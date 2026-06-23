package com.gaetan.localllmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gaetan.localllmapp.ui.AppScreen
import com.gaetan.localllmapp.ui.AppViewModel
import com.gaetan.localllmapp.ui.ChatScreen
import com.gaetan.localllmapp.ui.LoadingScreen
import com.gaetan.localllmapp.ui.history.HistoryScreen
import com.gaetan.localllmapp.ui.hub.HubScreen
import com.gaetan.localllmapp.ui.onboarding.DownloadProgressScreen
import com.gaetan.localllmapp.ui.onboarding.LicenseScreen
import com.gaetan.localllmapp.ui.onboarding.ModelChoiceScreen
import com.gaetan.localllmapp.ui.onboarding.OnboardingCompatibilityScreen
import com.gaetan.localllmapp.ui.settings.SettingsScreen
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.LocalLLMAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appViewModel: AppViewModel = viewModel()
            val state by appViewModel.uiState.collectAsState()

            LocalLLMAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GemmaColors.Background,
                ) {
                  Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    // Navigation arrière cohérente avec le parcours.
                    when (state.screen) {
                        AppScreen.MODEL_CHOICE -> BackHandler { appViewModel.backToCompatibility() }
                        AppScreen.LICENSE -> BackHandler { appViewModel.declineLicense() }
                        AppScreen.DOWNLOADING -> BackHandler { appViewModel.cancelDownload() }
                        AppScreen.CHAT -> BackHandler { appViewModel.backToHub() }
                        AppScreen.SETTINGS, AppScreen.HISTORY -> BackHandler { appViewModel.backFromSub() }
                        else -> {}
                    }

                    when (state.screen) {
                        AppScreen.COMPATIBILITY -> OnboardingCompatibilityScreen(
                            report = state.compatibilityReport,
                            onContinue = appViewModel::onCompatibilityContinue,
                        )

                        AppScreen.MODEL_CHOICE -> ModelChoiceScreen(
                            selectedVariant = state.selectedModelVariant,
                            textOnly = state.textOnlySelected,
                            wifiOnly = state.wifiOnly,
                            report = state.compatibilityReport,
                            onBack = appViewModel::backToCompatibility,
                            onToggleTextOnly = appViewModel::setTextOnly,
                            onToggleWifi = appViewModel::setWifiOnly,
                            onDownload = appViewModel::requestDownload,
                            onSelectSecondary = appViewModel::selectSecondaryModel,
                            onImportModel = appViewModel::importLocalModel,
                        )

                        AppScreen.LICENSE -> LicenseScreen(
                            onAccept = appViewModel::acceptLicense,
                            onDecline = appViewModel::declineLicense,
                        )

                        AppScreen.DOWNLOADING -> DownloadProgressScreen(
                            variant = state.selectedModelVariant,
                            progress = state.downloadProgress,
                            paused = state.isDownloadPaused,
                            waitingNetwork = state.downloadWaitingNetwork,
                            error = state.downloadError,
                            wifiOnly = state.wifiOnly,
                            importing = state.isImportingModel,
                            onPauseToggle = appViewModel::pauseResumeDownload,
                            onCancel = appViewModel::cancelDownload,
                        )

                        AppScreen.LOADING -> LoadingScreen(
                            state = state,
                            onRetry = appViewModel::retryInitialize,
                            onReset = appViewModel::resetModelAndRetry,
                        )

                        AppScreen.HUB -> HubScreen(
                            recent = state.recentConversations,
                            onNewChat = appViewModel::openNewChat,
                            onCapability = appViewModel::openCapability,
                            onAnalyse = appViewModel::openAnalyse,
                            onTranscribe = appViewModel::openTranscribe,
                            onOpenSkills = appViewModel::openSkills,
                            onOpenConversation = appViewModel::openConversation,
                            onOpenSettings = appViewModel::openSettings,
                            onShowAll = appViewModel::openHistory,
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            state = state,
                            onBack = appViewModel::backFromSub,
                            onBackendChange = appViewModel::setPreferredBackend,
                            onToggleWifi = appViewModel::setWifiOnly,
                            onClearConversations = appViewModel::clearAllConversations,
                            onClearCache = appViewModel::clearModelCache,
                        )

                        AppScreen.HISTORY -> HistoryScreen(
                            conversations = state.allConversations,
                            nowMs = System.currentTimeMillis(),
                            onBack = appViewModel::backFromSub,
                            onOpen = appViewModel::openConversation,
                            onDelete = appViewModel::deleteConversation,
                            onClearAll = appViewModel::clearAllConversations,
                        )

                        AppScreen.CHAT -> ChatScreen(
                            state = state,
                            onInputChange = appViewModel::updateInput,
                            onSend = appViewModel::sendMessage,
                            onImageSelected = appViewModel::setPendingImage,
                            onAudioSelected = appViewModel::setPendingAudio,
                            onToggleRecording = appViewModel::toggleAudioRecording,
                            onCancelRecording = appViewModel::cancelAudioRecording,
                            onClearAttachments = appViewModel::clearAttachments,
                            onBackendChange = appViewModel::setPreferredBackend,
                            onBackToHub = appViewModel::backToHub,
                            onRegenerate = appViewModel::regenerateLast,
                            onSettings = appViewModel::openSettings,
                            onClearCache = appViewModel::clearModelCache,
                            onOpenHistory = appViewModel::openHistory,
                            onConsumePendingAction = appViewModel::consumePendingChatAction,
                            onSelectSkill = appViewModel::selectSkill,
                            onClearSkill = appViewModel::clearSkill,
                        )
                    }
                  }
                }
            }
        }
    }
}
