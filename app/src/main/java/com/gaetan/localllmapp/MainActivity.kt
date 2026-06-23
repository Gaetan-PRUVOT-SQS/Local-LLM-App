package com.gaetan.localllmapp

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
import com.gaetan.localllmapp.ui.AppScreen
import com.gaetan.localllmapp.ui.AppViewModel
import com.gaetan.localllmapp.ui.ChatScreen
import com.gaetan.localllmapp.ui.LoadingScreen
import com.gaetan.localllmapp.ui.theme.LocalLLMAppTheme
import com.gaetan.localllmapp.ui.theme.GemmaColors

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
                            onImageSelected = appViewModel::setPendingImage,
                            onAudioSelected = appViewModel::setPendingAudio,
                            onToggleRecording = appViewModel::toggleAudioRecording,
                            onCancelRecording = appViewModel::cancelAudioRecording,
                            onClearAttachments = appViewModel::clearAttachments,
                            onBackendChange = appViewModel::setPreferredBackend,
                        )
                    }
                }
            }
        }
    }
}