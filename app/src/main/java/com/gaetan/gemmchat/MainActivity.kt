package com.gaetan.gemmchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gaetan.gemmchat.ui.AppScreen
import com.gaetan.gemmchat.ui.AppViewModel
import com.gaetan.gemmchat.ui.ChatScreen
import com.gaetan.gemmchat.ui.LoadingScreen
import com.gaetan.gemmchat.ui.theme.GemmaChatTheme
import com.gaetan.gemmchat.ui.theme.GemmaColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appViewModel: AppViewModel = viewModel()
            val state by appViewModel.uiState.collectAsState()

            GemmaChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GemmaColors.Background,
                ) {
                    when (state.screen) {
                        AppScreen.LOADING -> LoadingScreen(
                            state = state,
                            onRetry = appViewModel::retryInitialize,
                            onReset = appViewModel::resetModelAndRetry,
                            onRetryExtract = appViewModel::retryPrepare,
                        )

                        AppScreen.CHAT -> ChatScreen(
                            state = state,
                            onInputChange = appViewModel::updateInput,
                            onSend = appViewModel::sendMessage,
                            onStop = appViewModel::stopGeneration,
                            onImageSelected = appViewModel::setPendingImage,
                            onAudioSelected = appViewModel::setPendingAudio,
                            onToggleRecording = appViewModel::toggleAudioRecording,
                            onCancelRecording = appViewModel::cancelAudioRecording,
                            onClearAttachments = appViewModel::clearAttachments,
                            onBackendChange = appViewModel::setPreferredBackend,
                            onOpenDrawer = appViewModel::openDrawer,
                            onCloseDrawer = appViewModel::closeDrawer,
                            onNewConversation = appViewModel::newConversation,
                            onOpenConversation = appViewModel::openConversation,
                            onRenameConversation = appViewModel::renameConversation,
                            onDeleteConversation = appViewModel::deleteConversation,
                        )
                    }
                }
            }
        }
    }
}