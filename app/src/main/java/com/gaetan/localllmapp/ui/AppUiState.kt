package com.gaetan.localllmapp.ui

import android.net.Uri
import com.gaetan.localllmapp.data.ExtractProgress
import com.gaetan.localllmapp.data.ModelVariant
import com.gaetan.localllmapp.device.TensorGeneration
import com.gaetan.localllmapp.llm.BackendChoice
import com.gaetan.localllmapp.llm.EngineMode

enum class AppScreen {
    LOADING,
    CHAT,
}

data class ChatMessage(
    val role: MessageRole,
    val text: String,
    val imageUri: Uri? = null,
    val audioLabel: String? = null,
    val isStreaming: Boolean = false,
)

enum class MessageRole {
    USER,
    ASSISTANT,
}

data class AppUiState(
    val screen: AppScreen = AppScreen.LOADING,
    val deviceSocModel: String = "",
    val deviceSummary: String = "",
    val tensorGeneration: TensorGeneration = TensorGeneration.NONE,
    val isPixelTensor: Boolean = false,
    val supportsGemma4Npu: Boolean = false,
    val selectedModelVariant: ModelVariant = ModelVariant.Q4,
    val isExtractingModel: Boolean = false,
    val extractProgress: ExtractProgress? = null,
    val extractError: String? = null,
    val isInitializing: Boolean = false,
    val initError: String? = null,
    val engineMode: EngineMode = EngineMode.MULTIMODAL,
    val backend: BackendChoice = BackendChoice.GPU,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val pendingImageUri: Uri? = null,
    val pendingAudioUri: Uri? = null,
    val pendingAudioFilePath: String? = null,
    val pendingAudioLabel: String? = null,
    val isRecordingAudio: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val isGenerating: Boolean = false,
    val statusMessage: String? = null,
)