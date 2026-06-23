package com.gaetan.localllmapp.ui

import android.net.Uri
import com.gaetan.localllmapp.data.ConversationSummary
import com.gaetan.localllmapp.data.DownloadProgress
import com.gaetan.localllmapp.data.ModelVariant
import com.gaetan.localllmapp.data.Skill
import com.gaetan.localllmapp.data.Skills
import com.gaetan.localllmapp.device.CompatibilityReport
import com.gaetan.localllmapp.device.TensorGeneration
import com.gaetan.localllmapp.llm.BackendChoice
import com.gaetan.localllmapp.llm.EngineMode

/** Les 7 écrans du parcours design. */
enum class AppScreen {
    COMPATIBILITY,   // 01
    MODEL_CHOICE,    // 02
    DOWNLOADING,     // 03
    LOADING,         // init moteur (transitoire, entre download et hub)
    HUB,             // 04
    CHAT,            // 05 / 06 / 07
    SETTINGS,        // paramètres
    HISTORY,         // historique complet des discussions
    LICENSE,         // acceptation licence Gemma avant téléchargement
}

data class ChatMessage(
    val role: MessageRole,
    val text: String,
    val imageUri: Uri? = null,
    val audioLabel: String? = null,
    val isStreaming: Boolean = false,
    val tokensPerSec: Int? = null,
    val elapsedSec: Float? = null,
)

enum class MessageRole {
    USER,
    ASSISTANT,
}

/** Action déclenchée automatiquement à l'ouverture du chat (cartes Hub « 1-tap »). */
enum class PendingChatAction {
    NONE,
    PICK_IMAGE,
    START_RECORDING,
    SHOW_SKILLS,
}

data class AppUiState(
    val screen: AppScreen = AppScreen.COMPATIBILITY,
    val deviceSocModel: String = "",
    val deviceSummary: String = "",
    val tensorGeneration: TensorGeneration = TensorGeneration.NONE,
    val isPixelTensor: Boolean = false,
    val supportsGemma4Npu: Boolean = false,
    val hasNpu: Boolean = false,
    val selectedModelVariant: ModelVariant = ModelVariant.Q4,

    // 01 — compatibilité
    val compatibilityReport: CompatibilityReport? = null,

    // 02 — choix modèle
    val textOnlySelected: Boolean = false,
    val wifiOnly: Boolean = true,

    // 03 — téléchargement
    val downloadProgress: DownloadProgress? = null,
    val downloadError: String? = null,
    val downloadWaitingNetwork: String? = null,
    val isDownloadPaused: Boolean = false,
    val isImportingModel: Boolean = false,

    // init moteur (écran LOADING transitoire)
    val isInitializing: Boolean = false,
    val initError: String? = null,
    val engineMode: EngineMode = EngineMode.MULTIMODAL,
    val backend: BackendChoice = BackendChoice.GPU,

    // 04 — hub / historique
    val recentConversations: List<ConversationSummary> = emptyList(),
    val allConversations: List<ConversationSummary> = emptyList(),

    // 05/06/07 — chat
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
    val pendingChatAction: PendingChatAction = PendingChatAction.NONE,

    // télémétrie chat (footer écran 06)
    val batteryPercent: Int? = null,
    val batteryTempC: Float? = null,
    val skillsCount: Int = Skills.all.size,
    val activeSkill: Skill? = null,
    val liveTokensPerSec: Int? = null,
)
