package com.gaetan.localllmapp.ui

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaetan.localllmapp.audio.AudioRecorder
import com.gaetan.localllmapp.data.ConversationKind
import com.gaetan.localllmapp.data.ConversationStore
import com.gaetan.localllmapp.data.ConversationSummary
import com.gaetan.localllmapp.data.DownloadResult
import com.gaetan.localllmapp.data.ModelDownloader
import com.gaetan.localllmapp.data.ModelPreferences
import com.gaetan.localllmapp.data.ModelRepository
import com.gaetan.localllmapp.data.ModelVariant
import com.gaetan.localllmapp.data.Skill
import com.gaetan.localllmapp.data.StoredMessage
import com.gaetan.localllmapp.device.DeviceInfo
import com.gaetan.localllmapp.llm.BackendChoice
import com.gaetan.localllmapp.llm.ChatRepository
import com.gaetan.localllmapp.llm.EngineMode
import com.gaetan.localllmapp.llm.LlmEngine
import com.gaetan.localllmapp.llm.OutgoingMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val modelPreferences = ModelPreferences(application)
    private val modelRepository = ModelRepository(application, modelPreferences)
    private val modelDownloader = ModelDownloader(application, modelRepository)
    private val conversationStore = ConversationStore(application)
    private val llmEngine = LlmEngine(application)
    private val chatRepository = ChatRepository(application, llmEngine)
    private val audioRecorder = AudioRecorder(application)

    private var recordingTickerJob: Job? = null
    private var recordingStartedAtMs: Long = 0L
    private var downloadJob: Job? = null
    private var currentConversationId: String? = null

    private val recommendedVariant = DeviceInfo.recommendedModelVariant()
    private val recommendedBackend = when {
        DeviceInfo.supportsGemma4Npu -> BackendChoice.NPU
        else -> BackendChoice.GPU
    }

    private val _uiState = MutableStateFlow(
        AppUiState(
            deviceSocModel = DeviceInfo.socModel,
            deviceSummary = DeviceInfo.deviceSummary(),
            tensorGeneration = DeviceInfo.tensorGeneration,
            isPixelTensor = DeviceInfo.isPixelTensor,
            supportsGemma4Npu = DeviceInfo.supportsGemma4Npu,
            hasNpu = DeviceInfo.hasNpu,
            selectedModelVariant = recommendedVariant,
            backend = recommendedBackend,
            wifiOnly = modelPreferences.isWifiOnly(),
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        // Un modèle déjà présent dans le stockage de l'app (téléchargé, importé, ou poussé
        // via adb) → on saute l'onboarding et on va directement au Hub.
        val present = ModelVariant.entries.firstOrNull { modelRepository.isModelReady(it) }
        if (present != null) {
            modelRepository.setSelectedVariant(present)
            modelPreferences.setOnboardingComplete(true)
            _uiState.update {
                it.copy(
                    selectedModelVariant = present,
                    screen = AppScreen.HUB,
                    recentConversations = conversationStore.recent(),
                )
            }
            initializeEngine(goTo = AppScreen.HUB)
        } else {
            modelRepository.setSelectedVariant(recommendedVariant)
            runCompatibilityScan()
        }
    }

    // ---------- 01 · Compatibilité ----------
    private fun runCompatibilityScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(screen = AppScreen.COMPATIBILITY) }
            val report = DeviceInfo.compatibilityReport(
                getApplication(),
                _uiState.value.selectedModelVariant,
            )
            _uiState.update { it.copy(compatibilityReport = report) }
        }
    }

    fun onCompatibilityContinue() {
        _uiState.update { it.copy(screen = AppScreen.MODEL_CHOICE) }
    }

    // ---------- 02 · Choix du modèle ----------
    fun setTextOnly(textOnly: Boolean) {
        _uiState.update { it.copy(textOnlySelected = textOnly) }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        modelPreferences.setWifiOnly(wifiOnly)
        _uiState.update { it.copy(wifiOnly = wifiOnly) }
    }

    fun selectSecondaryModel() {
        // Gemma 4 E4B — pas encore packagé en .litertlm public.
        toast("${ModelVariant.E4B.label} sera bientôt disponible.")
    }

    private fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    fun backToCompatibility() {
        _uiState.update { it.copy(screen = AppScreen.COMPATIBILITY) }
    }

    // ---------- 03 · Téléchargement ----------
    /** Bouton « Télécharger » : passe par l'acceptation de licence Gemma si nécessaire. */
    fun requestDownload() {
        if (modelPreferences.isLicenseAccepted()) {
            startDownload()
        } else {
            _uiState.update { it.copy(screen = AppScreen.LICENSE) }
        }
    }

    fun acceptLicense() {
        modelPreferences.setLicenseAccepted(true)
        startDownload()
    }

    fun declineLicense() {
        _uiState.update { it.copy(screen = AppScreen.MODEL_CHOICE) }
    }

    fun startDownload() {
        val variant = _uiState.value.selectedModelVariant
        // Garde-fou stockage : besoin de la taille du modèle + 10% de marge.
        val free = DeviceInfo.freeStorageBytes(getApplication())
        val needed = (variant.expectedSizeBytes * 1.1).toLong()
        if (free < needed) {
            _uiState.update {
                it.copy(
                    screen = AppScreen.DOWNLOADING,
                    isDownloadPaused = true,
                    downloadError = "Stockage insuffisant : ${ModelVariant.formatBytes(free)} libres, " +
                        "${ModelVariant.formatBytes(needed)} requis. Libère de l'espace puis réessaie.",
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                screen = AppScreen.DOWNLOADING,
                isDownloadPaused = false,
                downloadError = null,
                downloadWaitingNetwork = null,
            )
        }
        launchDownload(variant)
    }

    private fun launchDownload(variant: ModelVariant) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            when (
                val result = modelDownloader.download(
                    variant = variant,
                    wifiOnly = _uiState.value.wifiOnly,
                    onProgress = { progress ->
                        _uiState.update { it.copy(downloadProgress = progress, downloadError = null) }
                    },
                )
            ) {
                is DownloadResult.Success -> onModelInstalled(variant)
                is DownloadResult.WaitingForNetwork ->
                    _uiState.update { it.copy(downloadWaitingNetwork = result.message, isDownloadPaused = true) }
                is DownloadResult.Error ->
                    _uiState.update { it.copy(downloadError = result.message, isDownloadPaused = true) }
            }
        }
    }

    fun pauseResumeDownload() {
        val state = _uiState.value
        if (state.isDownloadPaused || state.downloadError != null || state.downloadWaitingNetwork != null) {
            _uiState.update { it.copy(isDownloadPaused = false, downloadError = null, downloadWaitingNetwork = null) }
            launchDownload(state.selectedModelVariant)
        } else {
            downloadJob?.cancel() // pause coopérative — le .partial est conservé
            downloadJob = null
            _uiState.update { it.copy(isDownloadPaused = true) }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        modelRepository.deleteModel(_uiState.value.selectedModelVariant)
        _uiState.update {
            it.copy(
                screen = AppScreen.MODEL_CHOICE,
                downloadProgress = null,
                downloadError = null,
                downloadWaitingNetwork = null,
                isDownloadPaused = false,
                isImportingModel = false,
            )
        }
    }

    /** Charge un modèle déjà présent sur le téléphone (sélecteur de fichiers). */
    fun importLocalModel(uri: Uri) {
        val variant = _uiState.value.selectedModelVariant
        _uiState.update {
            it.copy(
                screen = AppScreen.DOWNLOADING,
                isImportingModel = true,
                isDownloadPaused = false,
                downloadError = null,
                downloadWaitingNetwork = null,
                downloadProgress = null,
            )
        }
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            when (
                val result = modelDownloader.importLocal(
                    variant = variant,
                    uri = uri,
                    onProgress = { p -> _uiState.update { it.copy(downloadProgress = p) } },
                )
            ) {
                is DownloadResult.Success -> {
                    _uiState.update { it.copy(isImportingModel = false) }
                    onModelInstalled(variant)
                }
                is DownloadResult.Error ->
                    _uiState.update { it.copy(downloadError = result.message, isImportingModel = false, isDownloadPaused = true) }
                is DownloadResult.WaitingForNetwork -> Unit // sans objet pour un import local
            }
        }
    }

    private fun onModelInstalled(variant: ModelVariant) {
        modelPreferences.setOnboardingComplete(true)
        modelRepository.setSelectedVariant(variant)
        _uiState.update {
            it.copy(downloadProgress = null, recentConversations = conversationStore.recent())
        }
        initializeEngine(goTo = AppScreen.HUB)
    }

    // ---------- init moteur ----------
    private fun initializeEngine(
        goTo: AppScreen,
        preferredBackend: BackendChoice = _uiState.value.backend,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(screen = AppScreen.LOADING, isInitializing = true, initError = null) }
            val variant = _uiState.value.selectedModelVariant
            val status = llmEngine.initialize(
                modelFile = modelRepository.modelFile(variant),
                modelVariant = variant,
                preferredBackend = preferredBackend,
            )
            if (status.isReady) {
                val mode = if (_uiState.value.textOnlySelected) EngineMode.TEXT_ONLY else status.mode
                _uiState.update {
                    it.copy(
                        screen = goTo,
                        isInitializing = false,
                        engineMode = mode,
                        backend = status.backend,
                        initError = null,
                    )
                }
            } else {
                _uiState.update { it.copy(screen = AppScreen.LOADING, isInitializing = false, initError = status.error) }
            }
        }
    }

    fun retryInitialize() = initializeEngine(goTo = AppScreen.HUB)

    fun resetModelAndRetry() {
        modelRepository.deleteAllModels()
        modelPreferences.setOnboardingComplete(false)
        llmEngine.close()
        runCompatibilityScan()
    }

    fun setPreferredBackend(backend: BackendChoice) {
        val s = _uiState.value
        if (s.isInitializing || s.isGenerating || backend == s.backend) return
        val current = s.screen
        _uiState.update { it.copy(backend = backend) }
        if (modelRepository.isModelReady(s.selectedModelVariant)) {
            initializeEngine(goTo = current, preferredBackend = backend)
        }
    }

    // ---------- 04 · Hub ----------
    fun openNewChat() {
        currentConversationId = null
        _uiState.update {
            it.copy(
                screen = AppScreen.CHAT,
                messages = emptyList(),
                inputText = "",
                statusMessage = null,
                pendingChatAction = PendingChatAction.NONE,
                batteryPercent = readBattery().first,
                batteryTempC = readBattery().second,
            )
        }
    }

    fun openCapability(prompt: String) {
        openChatWith(prompt, PendingChatAction.NONE)
    }

    /** Carte « Analyser » : ouvre le chat ET le sélecteur d'image (1-tap). */
    fun openAnalyse() {
        openChatWith("Analyse cette photo.", PendingChatAction.PICK_IMAGE)
    }

    /** Carte « Transcrire » : ouvre le chat ET démarre l'enregistrement (1-tap). */
    fun openTranscribe() {
        openChatWith("Transcris cet audio.", PendingChatAction.START_RECORDING)
    }

    private fun openChatWith(prompt: String, action: PendingChatAction) {
        currentConversationId = null
        _uiState.update {
            it.copy(
                screen = AppScreen.CHAT,
                messages = emptyList(),
                inputText = prompt,
                statusMessage = null,
                pendingChatAction = action,
                batteryPercent = readBattery().first,
                batteryTempC = readBattery().second,
            )
        }
    }

    fun consumePendingChatAction() {
        if (_uiState.value.pendingChatAction != PendingChatAction.NONE) {
            _uiState.update { it.copy(pendingChatAction = PendingChatAction.NONE) }
        }
    }

    /** Carte « Agent & Skills » : ouvre le chat et affiche le sélecteur de skills. */
    fun openSkills() {
        openChatWith("", PendingChatAction.SHOW_SKILLS)
    }

    fun selectSkill(skill: Skill) {
        _uiState.update { it.copy(activeSkill = skill) }
        toast("Skill « ${skill.name} » activée")
    }

    fun clearSkill() {
        _uiState.update { it.copy(activeSkill = null) }
    }

    /** Rouvre une discussion archivée en restaurant ses messages. */
    fun openConversation(id: String) {
        val stored = conversationStore.messages(id)
        if (stored.isEmpty()) {
            openNewChat()
            return
        }
        currentConversationId = id
        val restored = stored.map { s ->
            ChatMessage(
                role = if (s.role == MessageRole.ASSISTANT.name) MessageRole.ASSISTANT else MessageRole.USER,
                text = s.text,
                imageUri = s.imageUri?.let { runCatching { it.toUri() }.getOrNull() },
                audioLabel = s.audioLabel,
            )
        }
        _uiState.update {
            it.copy(
                screen = AppScreen.CHAT,
                messages = restored,
                inputText = "",
                statusMessage = null,
                pendingChatAction = PendingChatAction.NONE,
                batteryPercent = readBattery().first,
                batteryTempC = readBattery().second,
            )
        }
    }

    // Écran d'origine pour revenir après Paramètres / Historique.
    private var subReturn: AppScreen = AppScreen.HUB

    fun openSettings() {
        subReturn = _uiState.value.screen
        _uiState.update { it.copy(screen = AppScreen.SETTINGS) }
    }

    fun openHistory() {
        subReturn = _uiState.value.screen
        _uiState.update {
            it.copy(screen = AppScreen.HISTORY, allConversations = conversationStore.all())
        }
    }

    fun backFromSub() {
        _uiState.update {
            it.copy(
                screen = if (subReturn == AppScreen.SETTINGS || subReturn == AppScreen.HISTORY) AppScreen.HUB else subReturn,
                recentConversations = conversationStore.recent(),
            )
        }
    }

    fun deleteConversation(id: String) {
        conversationStore.delete(id)
        _uiState.update {
            it.copy(
                allConversations = conversationStore.all(),
                recentConversations = conversationStore.recent(),
            )
        }
    }

    fun clearAllConversations() {
        conversationStore.clear()
        toast("Discussions effacées.")
        _uiState.update {
            it.copy(allConversations = emptyList(), recentConversations = emptyList())
        }
    }

    /** « Vider le cache modèle » : supprime le modèle et relance l'onboarding. */
    fun clearModelCache() {
        toast("Cache modèle vidé.")
        resetModelAndRetry()
    }

    fun backToHub() {
        _uiState.update {
            it.copy(screen = AppScreen.HUB, recentConversations = conversationStore.recent())
        }
    }

    // ---------- 05/06/07 · Chat ----------
    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun setPendingImage(uri: Uri?) = _uiState.update { it.copy(pendingImageUri = uri) }

    fun setPendingAudio(uri: Uri?, label: String? = null) {
        _uiState.update {
            it.copy(pendingAudioUri = uri, pendingAudioFilePath = null, pendingAudioLabel = label)
        }
    }

    fun clearAttachments() {
        _uiState.update {
            it.copy(pendingImageUri = null, pendingAudioUri = null, pendingAudioFilePath = null, pendingAudioLabel = null)
        }
    }

    fun toggleAudioRecording() {
        if (_uiState.value.isGenerating) return
        if (audioRecorder.isRecording) stopAudioRecording() else startAudioRecording()
    }

    private fun startAudioRecording() {
        try {
            val file = audioRecorder.start()
            recordingStartedAtMs = System.currentTimeMillis()
            recordingTickerJob?.cancel()
            recordingTickerJob = viewModelScope.launch {
                while (audioRecorder.isRecording) {
                    val elapsed = System.currentTimeMillis() - recordingStartedAtMs
                    _uiState.update {
                        it.copy(
                            isRecordingAudio = true,
                            recordingElapsedMs = elapsed,
                            pendingAudioUri = null,
                            pendingAudioFilePath = file.absolutePath,
                            pendingAudioLabel = "Enregistrement…",
                            statusMessage = null,
                        )
                    }
                    delay(200)
                }
            }
            _uiState.update {
                it.copy(
                    isRecordingAudio = true,
                    recordingElapsedMs = 0L,
                    pendingAudioUri = null,
                    pendingAudioFilePath = file.absolutePath,
                    pendingAudioLabel = "Enregistrement…",
                )
            }
        } catch (t: Throwable) {
            _uiState.update { it.copy(statusMessage = "Micro inaccessible: ${t.message ?: "erreur"}") }
        }
    }

    private fun stopAudioRecording() {
        recordingTickerJob?.cancel()
        recordingTickerJob = null
        val file = audioRecorder.stop()
        val elapsedSec = ((System.currentTimeMillis() - recordingStartedAtMs) / 1000).coerceAtLeast(1)
        _uiState.update {
            it.copy(
                isRecordingAudio = false,
                recordingElapsedMs = 0L,
                pendingAudioUri = file?.toUri(),
                pendingAudioFilePath = file?.absolutePath,
                pendingAudioLabel = file?.let { "Voix (${elapsedSec}s)" },
                statusMessage = if (file == null) "Enregistrement trop court ou invalide." else null,
            )
        }
    }

    fun cancelAudioRecording() {
        recordingTickerJob?.cancel()
        recordingTickerJob = null
        audioRecorder.cancel()
        _uiState.update {
            it.copy(
                isRecordingAudio = false,
                recordingElapsedMs = 0L,
                pendingAudioUri = null,
                pendingAudioFilePath = null,
                pendingAudioLabel = null,
            )
        }
    }

    /** Régénère la dernière réponse : on **réutilise** le dernier message utilisateur
     *  (sans le dupliquer) et on remplace uniquement la réponse de l'assistant. */
    fun regenerateLast() {
        val state = _uiState.value
        if (state.isGenerating || state.isRecordingAudio) return
        val lastUser = state.messages.lastOrNull { it.role == MessageRole.USER } ?: return

        val trimmed = state.messages.dropLastWhile { it.role == MessageRole.ASSISTANT }
        val assistantIndex = trimmed.size
        val outgoing = OutgoingMessage(
            text = lastUser.text.takeIf { it != "(message multimédia)" } ?: "",
            imageUri = lastUser.imageUri,
            audioUri = null,
            audioFilePath = null,
        )
        _uiState.update {
            it.copy(
                messages = trimmed + ChatMessage(MessageRole.ASSISTANT, "", isStreaming = true),
                isGenerating = true,
                statusMessage = null,
                liveTokensPerSec = null,
                batteryPercent = readBattery().first,
                batteryTempC = readBattery().second,
            )
        }
        streamResponse(outgoing, assistantIndex, lastUser)
    }

    private var generationStartNanos: Long = 0L

    fun sendMessage() {
        val state = _uiState.value
        if (state.isGenerating || state.isRecordingAudio) return
        val text = state.inputText.trim()
        val hasMedia = state.pendingImageUri != null ||
            state.pendingAudioUri != null ||
            state.pendingAudioFilePath != null
        if (text.isEmpty() && !hasMedia) return

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            text = text.ifEmpty { "(message multimédia)" },
            imageUri = state.pendingImageUri,
            audioLabel = state.pendingAudioLabel,
        )
        val assistantIndex = state.messages.size + 1
        // Skill active → on préfixe le prompt envoyé au modèle (la bulle affichée reste brute).
        val outgoing = OutgoingMessage(
            text = state.activeSkill?.let { if (text.isNotEmpty()) it.promptPrefix + text else text } ?: text,
            imageUri = state.pendingImageUri,
            audioUri = state.pendingAudioUri,
            audioFilePath = state.pendingAudioFilePath,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + ChatMessage(MessageRole.ASSISTANT, "", isStreaming = true),
                inputText = "",
                pendingImageUri = null,
                pendingAudioUri = null,
                pendingAudioFilePath = null,
                pendingAudioLabel = null,
                isGenerating = true,
                statusMessage = null,
                liveTokensPerSec = null,
                batteryPercent = readBattery().first,
                batteryTempC = readBattery().second,
            )
        }
        streamResponse(outgoing, assistantIndex, userMessage)
    }

    /** Lance l'inférence en streaming dans le message assistant à [assistantIndex]. */
    private fun streamResponse(outgoing: OutgoingMessage, assistantIndex: Int, firstUser: ChatMessage) {
        viewModelScope.launch {
            generationStartNanos = System.nanoTime()
            try {
                val flow = chatRepository.sendMessage(outgoing)
                val builder = StringBuilder()
                flow.collect { chunk ->
                    builder.append(chunk)
                    val elapsedSec = (System.nanoTime() - generationStartNanos) / 1_000_000_000f
                    val tps = if (elapsedSec > 0.2f) (estimateTokens(builder.length) / elapsedSec).toInt() else null
                    _uiState.update { current ->
                        current.copy(
                            liveTokensPerSec = tps,
                            messages = current.messages.mapIndexed { index, message ->
                                if (index == assistantIndex) message.copy(text = builder.toString(), isStreaming = true)
                                else message
                            },
                        )
                    }
                }
                val elapsedSec = (System.nanoTime() - generationStartNanos) / 1_000_000_000f
                val finalTps = if (elapsedSec > 0.05f) (estimateTokens(builder.length) / elapsedSec).toInt() else null
                _uiState.update { current ->
                    current.copy(
                        isGenerating = false,
                        liveTokensPerSec = null,
                        batteryPercent = readBattery().first,
                        batteryTempC = readBattery().second,
                        messages = current.messages.mapIndexed { index, message ->
                            if (index == assistantIndex)
                                message.copy(isStreaming = false, tokensPerSec = finalTps, elapsedSec = elapsedSec)
                            else message
                        },
                    )
                }
                persistConversation(firstUser)
            } catch (t: Throwable) {
                _uiState.update { current ->
                    current.copy(
                        isGenerating = false,
                        statusMessage = t.message ?: "Erreur d'inférence.",
                        messages = current.messages.dropLast(1),
                    )
                }
            }
        }
    }

    private fun persistConversation(firstUser: ChatMessage) {
        val state = _uiState.value
        val id = currentConversationId ?: System.currentTimeMillis().toString().also { currentConversationId = it }
        val title = state.messages.firstOrNull { it.role == MessageRole.USER }?.text
            ?.take(48)?.ifBlank { "Discussion" } ?: "Discussion"
        val kind = when {
            state.messages.any { it.imageUri != null } -> ConversationKind.IMAGE
            state.messages.any { it.audioLabel != null } -> ConversationKind.AUDIO
            else -> ConversationKind.CHAT
        }
        // Persiste le contenu complet pour pouvoir rouvrir la discussion.
        val stored = state.messages.map { m ->
            StoredMessage(
                role = m.role.name,
                text = m.text,
                imageUri = m.imageUri?.toString(),
                audioLabel = m.audioLabel,
            )
        }
        conversationStore.save(id, title, kind, System.currentTimeMillis(), stored)
        _uiState.update { it.copy(recentConversations = conversationStore.recent()) }
    }

    private fun estimateTokens(chars: Int): Int = (chars / 4f).toInt().coerceAtLeast(1)

    private fun readBattery(): Pair<Int?, Float?> {
        val intent: Intent? = getApplication<Application>()
            .registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else null
        val tempDeci = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val tempC = if (tempDeci != Int.MIN_VALUE && tempDeci != 0) tempDeci / 10f else null
        return pct to tempC
    }

    override fun onCleared() {
        recordingTickerJob?.cancel()
        downloadJob?.cancel()
        audioRecorder.cancel()
        llmEngine.close()
        super.onCleared()
    }
}
