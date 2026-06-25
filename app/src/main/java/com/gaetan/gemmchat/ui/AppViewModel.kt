package com.gaetan.gemmchat.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaetan.gemmchat.audio.AudioRecorder
import com.gaetan.gemmchat.data.ConversationStore
import com.gaetan.gemmchat.data.InstallResult
import com.gaetan.gemmchat.data.ModelDownloader
import com.gaetan.gemmchat.data.ModelPreferences
import com.gaetan.gemmchat.data.ModelRepository
import com.gaetan.gemmchat.data.ModelVariant
import com.gaetan.gemmchat.data.StoredConversation
import com.gaetan.gemmchat.data.StoredMessage
import com.gaetan.gemmchat.device.DeviceInfo
import com.gaetan.gemmchat.llm.BackendChoice
import com.gaetan.gemmchat.llm.ChatRepository
import com.gaetan.gemmchat.llm.EngineMode
import com.gaetan.gemmchat.llm.LlmEngine
import com.gaetan.gemmchat.llm.OutgoingMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val modelPreferences = ModelPreferences(application)
    private val modelRepository = ModelRepository(application, modelPreferences)
    private val modelDownloader = ModelDownloader(modelRepository)
    private val llmEngine = LlmEngine(application)
    private val chatRepository = ChatRepository(application, llmEngine)
    private val audioRecorder = AudioRecorder(application)
    private val conversationStore = ConversationStore(application)

    private var recordingTickerJob: Job? = null
    private var recordingStartedAtMs: Long = 0L
    private var generationJob: Job? = null

    private var currentTitle: String? = null
    private var currentCreatedAt: Long = 0L

    private val recommendedVariant = ModelVariant.bundled
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
            selectedModelVariant = recommendedVariant,
            backend = recommendedBackend,
            screen = AppScreen.LOADING,
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        modelRepository.setSelectedVariant(recommendedVariant)
        viewModelScope.launch { restoreConversations() }
        prepareBundledModelAndInitialize()
    }

    fun retryPrepare() {
        prepareBundledModelAndInitialize()
    }

    fun retryInitialize() {
        initializeEngine()
    }

    fun resetModelAndRetry() {
        modelRepository.deleteAllModels()
        llmEngine.close()
        prepareBundledModelAndInitialize()
    }

    fun setPreferredBackend(backend: BackendChoice) {
        if (_uiState.value.isInitializing || _uiState.value.isGenerating || _uiState.value.isExtractingModel) {
            return
        }
        _uiState.update { it.copy(backend = backend) }
        if (modelRepository.isModelReady(_uiState.value.selectedModelVariant)) {
            initializeEngine(preferredBackend = backend)
        }
    }

    fun updateInput(text: String) {
        // DEF-6 : borne l'entrée pour éviter de dépasser le contexte du modèle.
        _uiState.update { it.copy(inputText = text.take(MAX_INPUT_CHARS)) }
    }

    fun setPendingImage(uri: Uri?) {
        _uiState.update { it.copy(pendingImageUri = uri) }
    }

    fun setPendingAudio(uri: Uri?, label: String? = null) {
        _uiState.update {
            it.copy(
                pendingAudioUri = uri,
                pendingAudioFilePath = null,
                pendingAudioLabel = label,
            )
        }
    }

    fun clearAttachments() {
        _uiState.update {
            it.copy(
                pendingImageUri = null,
                pendingAudioUri = null,
                pendingAudioFilePath = null,
                pendingAudioLabel = null,
            )
        }
    }

    fun toggleAudioRecording() {
        if (_uiState.value.isGenerating) return

        if (audioRecorder.isRecording) {
            stopAudioRecording()
        } else {
            startAudioRecording()
        }
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
            Log.w(TAG, "Micro inaccessible", t)
            _uiState.update {
                it.copy(statusMessage = "Micro inaccessible. Vérifie l'autorisation micro dans les réglages.")
            }
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
        val pendingAudioUri = state.pendingAudioUri
        val pendingAudioFilePath = state.pendingAudioFilePath

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    text = "",
                    isStreaming = true,
                ),
                inputText = "",
                pendingImageUri = null,
                pendingAudioUri = null,
                pendingAudioFilePath = null,
                pendingAudioLabel = null,
                isGenerating = true,
                statusMessage = null,
            )
        }

        generationJob = viewModelScope.launch {
            val builder = StringBuilder()
            // DEF-4 : copier l'image jointe en stockage privé pour qu'elle
            // reste affichable après redémarrage (les URI content:// ne le sont pas).
            userMessage.imageUri?.let { src ->
                copyImageToStorage(src)?.let { persisted ->
                    _uiState.update { current ->
                        current.copy(
                            messages = current.messages.mapIndexed { index, message ->
                                if (index == assistantIndex - 1) message.copy(imageUri = persisted) else message
                            },
                        )
                    }
                }
            }
            try {
                val flow = chatRepository.sendMessage(
                    OutgoingMessage(
                        text = text,
                        imageUri = userMessage.imageUri,
                        audioUri = pendingAudioUri,
                        audioFilePath = pendingAudioFilePath,
                    ),
                )
                flow.collect { chunk ->
                    builder.append(chunk)
                    _uiState.update { current ->
                        current.copy(
                            messages = current.messages.mapIndexed { index, message ->
                                if (index == assistantIndex) {
                                    message.copy(text = builder.toString(), isStreaming = true)
                                } else {
                                    message
                                }
                            },
                        )
                    }
                }
                finalizeAssistant(assistantIndex)
                persistCurrentConversation()
            } catch (c: CancellationException) {
                if (builder.isEmpty()) {
                    // DEF-2 : stop avant le 1er token → on retire la bulle assistant vide.
                    _uiState.update { it.copy(isGenerating = false, messages = it.messages.dropLast(1)) }
                } else {
                    finalizeAssistant(assistantIndex)
                    // Persiste le partiel dans une coroutine NON annulée.
                    viewModelScope.launch { persistCurrentConversation() }
                }
                throw c
            } catch (t: Throwable) {
                Log.w(TAG, "Échec de génération", t)
                _uiState.update { current ->
                    current.copy(
                        isGenerating = false,
                        statusMessage = "Une erreur est survenue pendant la génération. Réessaie.",
                        messages = if (builder.isEmpty()) current.messages.dropLast(1) else current.messages,
                    )
                }
                if (builder.isNotEmpty()) persistCurrentConversation()
            }
        }
    }

    private suspend fun copyImageToStorage(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val app = getApplication<Application>()
            val dir = File(app.filesDir, "images").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.img")
            app.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            Uri.fromFile(file)
        }.onFailure { Log.w(TAG, "Copie image échouée", it) }.getOrNull()
    }

    private fun finalizeAssistant(assistantIndex: Int) {
        _uiState.update { current ->
            current.copy(
                messages = current.messages.mapIndexed { index, message ->
                    if (index == assistantIndex) message.copy(isStreaming = false) else message
                },
                isGenerating = false,
            )
        }
    }

    fun stopGeneration() {
        if (!_uiState.value.isGenerating) return
        generationJob?.cancel()
        generationJob = null
    }

    fun openDrawer() {
        _uiState.update { it.copy(isDrawerOpen = true) }
    }

    fun closeDrawer() {
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    fun newConversation() {
        if (_uiState.value.isGenerating || _uiState.value.isRecordingAudio) return
        viewModelScope.launch {
            persistCurrentConversation()
            currentTitle = null
            currentCreatedAt = 0L
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    currentConversationId = null,
                    isDrawerOpen = false,
                    inputText = "",
                    statusMessage = null,
                )
            }
            modelPreferences.saveLastActiveConversationId(null)
            clearAttachments()
            llmEngine.startNewConversation()
        }
    }

    fun openConversation(id: String) {
        if (_uiState.value.isGenerating || _uiState.value.isRecordingAudio) return
        if (id == _uiState.value.currentConversationId) {
            _uiState.update { it.copy(isDrawerOpen = false) }
            return
        }
        viewModelScope.launch {
            persistCurrentConversation()
            val record = conversationStore.load(id)
            if (record == null) {
                _uiState.update { it.copy(isDrawerOpen = false) }
                refreshConversations()
                return@launch
            }
            currentTitle = record.title
            currentCreatedAt = record.createdAt
            _uiState.update {
                it.copy(
                    messages = record.messages.map { stored -> stored.toChatMessage() },
                    currentConversationId = record.id,
                    isDrawerOpen = false,
                    inputText = "",
                    statusMessage = null,
                )
            }
            modelPreferences.saveLastActiveConversationId(record.id)
            clearAttachments()
            llmEngine.startNewConversation()
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationStore.delete(id)
            if (id == _uiState.value.currentConversationId) {
                currentTitle = null
                currentCreatedAt = 0L
                _uiState.update {
                    it.copy(
                        messages = emptyList(),
                        currentConversationId = null,
                        // DEF-3 : ferme le tiroir quand la conversation affichée disparaît
                        isDrawerOpen = false,
                    )
                }
                modelPreferences.saveLastActiveConversationId(null)
                llmEngine.startNewConversation()
            }
            refreshConversations()
        }
    }

    fun renameConversation(id: String, title: String) {
        viewModelScope.launch {
            val record = conversationStore.load(id) ?: return@launch
            val cleaned = title.trim().ifEmpty { record.title }
            conversationStore.save(record.copy(title = cleaned))
            if (id == _uiState.value.currentConversationId) currentTitle = cleaned
            refreshConversations()
        }
    }

    private suspend fun restoreConversations() {
        refreshConversations()
        val lastId = modelPreferences.getLastActiveConversationId() ?: return
        val record = conversationStore.load(lastId) ?: return
        currentTitle = record.title
        currentCreatedAt = record.createdAt
        _uiState.update {
            it.copy(
                currentConversationId = record.id,
                messages = record.messages.map { stored -> stored.toChatMessage() },
            )
        }
    }

    private suspend fun refreshConversations() {
        val summaries = conversationStore.listSummaries().map {
            ConversationSummary(id = it.id, title = it.title, updatedAt = it.updatedAt)
        }
        _uiState.update { it.copy(conversations = summaries) }
    }

    private suspend fun persistCurrentConversation() {
        val messages = _uiState.value.messages
            .filterNot { it.isStreaming && it.text.isEmpty() }
            .map { it.toStored() }
        if (messages.isEmpty()) return

        val now = System.currentTimeMillis()
        var id = _uiState.value.currentConversationId
        if (id == null) {
            id = UUID.randomUUID().toString()
            currentCreatedAt = now
            currentTitle = deriveTitle(_uiState.value.messages)
            val newId = id
            _uiState.update { it.copy(currentConversationId = newId) }
            modelPreferences.saveLastActiveConversationId(newId)
        }
        val record = StoredConversation(
            id = id,
            title = currentTitle ?: deriveTitle(_uiState.value.messages),
            createdAt = if (currentCreatedAt == 0L) now else currentCreatedAt,
            updatedAt = now,
            messages = messages,
        )
        conversationStore.save(record)
        refreshConversations()
    }

    private fun deriveTitle(messages: List<ChatMessage>): String {
        val firstUser = messages.firstOrNull { it.role == MessageRole.USER }?.text?.trim().orEmpty()
        if (firstUser.isEmpty()) return "Nouvelle conversation"
        return if (firstUser.length <= 40) firstUser else firstUser.take(40).trimEnd() + "…"
    }

    private fun ChatMessage.toStored(): StoredMessage = StoredMessage(
        role = role.name,
        text = text,
        imageUri = imageUri?.toString(),
        audioLabel = audioLabel,
        createdAt = createdAt,
    )

    private fun StoredMessage.toChatMessage(): ChatMessage = ChatMessage(
        role = if (role == MessageRole.USER.name) MessageRole.USER else MessageRole.ASSISTANT,
        text = text,
        imageUri = imageUri?.toUri(),
        audioLabel = audioLabel,
        createdAt = if (createdAt != 0L) createdAt else System.currentTimeMillis(),
    )

    private fun prepareBundledModelAndInitialize() {
        viewModelScope.launch {
            val variant = _uiState.value.selectedModelVariant
            _uiState.update {
                it.copy(
                    screen = AppScreen.LOADING,
                    isExtractingModel = true,
                    extractError = null,
                    extractProgress = null,
                    isInitializing = false,
                    initError = null,
                )
            }

            when (
                val result = modelDownloader.ensureDownloaded(
                    variant = variant,
                    onProgress = { progress ->
                        _uiState.update { state ->
                            state.copy(extractProgress = progress)
                        }
                    },
                )
            ) {
                is InstallResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExtractingModel = false,
                            extractProgress = null,
                            extractError = null,
                        )
                    }
                    initializeEngine()
                }
                is InstallResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExtractingModel = false,
                            extractError = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun initializeEngine(preferredBackend: BackendChoice = _uiState.value.backend) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    screen = AppScreen.LOADING,
                    isInitializing = true,
                    initError = null,
                )
            }

            val variant = _uiState.value.selectedModelVariant
            val status = llmEngine.initialize(
                modelFile = modelRepository.modelFile(variant),
                modelVariant = variant,
                preferredBackend = preferredBackend,
            )

            if (status.isReady) {
                _uiState.update {
                    it.copy(
                        screen = AppScreen.CHAT,
                        isInitializing = false,
                        engineMode = status.mode,
                        backend = status.backend,
                        initError = null,
                    )
                }
            } else {
                Log.w(TAG, "Échec d'initialisation du moteur: ${status.error}")
                _uiState.update {
                    it.copy(
                        screen = AppScreen.LOADING,
                        isInitializing = false,
                        initError = "Impossible de démarrer Gemma sur cet appareil. " +
                            "Réessaie, ou réinstalle le modèle.",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        recordingTickerJob?.cancel()
        generationJob?.cancel()
        audioRecorder.cancel()
        llmEngine.close()
        super.onCleared()
    }

    companion object {
        private const val TAG = "AppViewModel"
        private const val MAX_INPUT_CHARS = 8000
    }
}