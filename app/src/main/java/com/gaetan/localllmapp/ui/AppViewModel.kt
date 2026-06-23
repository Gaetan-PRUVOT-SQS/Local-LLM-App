package com.gaetan.localllmapp.ui

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaetan.localllmapp.audio.AudioRecorder
import com.gaetan.localllmapp.data.BundledModelInstaller
import com.gaetan.localllmapp.data.InstallResult
import com.gaetan.localllmapp.data.ModelPreferences
import com.gaetan.localllmapp.data.ModelRepository
import com.gaetan.localllmapp.data.ModelVariant
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
    private val bundledModelInstaller = BundledModelInstaller(application, modelRepository)
    private val llmEngine = LlmEngine(application)
    private val chatRepository = ChatRepository(application, llmEngine)
    private val audioRecorder = AudioRecorder(application)

    private var recordingTickerJob: Job? = null
    private var recordingStartedAtMs: Long = 0L

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
        _uiState.update { it.copy(inputText = text) }
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
            _uiState.update {
                it.copy(statusMessage = "Micro inaccessible: ${t.message ?: "erreur"}")
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

        viewModelScope.launch {
            try {
                val flow = chatRepository.sendMessage(
                    OutgoingMessage(
                        text = text,
                        imageUri = userMessage.imageUri,
                        audioUri = pendingAudioUri,
                        audioFilePath = pendingAudioFilePath,
                    ),
                )
                val builder = StringBuilder()
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
                _uiState.update { current ->
                    current.copy(
                        messages = current.messages.mapIndexed { index, message ->
                            if (index == assistantIndex) {
                                message.copy(isStreaming = false)
                            } else {
                                message
                            }
                        },
                        isGenerating = false,
                    )
                }
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
                val result = bundledModelInstaller.ensureInstalled(
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
                _uiState.update {
                    it.copy(
                        screen = AppScreen.LOADING,
                        isInitializing = false,
                        initError = status.error,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        recordingTickerJob?.cancel()
        audioRecorder.cancel()
        llmEngine.close()
        super.onCleared()
    }
}