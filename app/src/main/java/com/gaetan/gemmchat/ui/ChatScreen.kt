package com.gaetan.gemmchat.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gaetan.gemmchat.llm.BackendChoice
import com.gaetan.gemmchat.llm.EngineMode
import com.gaetan.gemmchat.ui.components.GemmaHeader
import com.gaetan.gemmchat.ui.components.GemmaInputBar
import com.gaetan.gemmchat.ui.components.GemmaLogo
import com.gaetan.gemmchat.ui.components.GemmaScreenBackground
import com.gaetan.gemmchat.ui.components.StreamingIndicator
import com.gaetan.gemmchat.ui.components.WelcomeContent
import com.gaetan.gemmchat.ui.theme.GemmaColors
import com.gaetan.gemmchat.ui.theme.ManropeFamily

@Composable
fun ChatScreen(
    state: AppUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageSelected: (Uri?) -> Unit,
    onAudioSelected: (Uri?, String?) -> Unit,
    onToggleRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onClearAttachments: () -> Unit,
    onBackendChange: (BackendChoice) -> Unit,
) {
    val listState = rememberLazyListState()
    val multimodalEnabled = state.engineMode == EngineMode.MULTIMODAL
    val showWelcome = state.messages.isEmpty() && !state.isGenerating

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> onImageSelected(uri) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    val requestMediaPermissions = remember {
        {
            val permissions = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                    add(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    val cycleBackend = remember(state.backend, state.supportsGemma4Npu, state.selectedModelVariant) {
        {
            val order = buildList {
                if (state.supportsGemma4Npu) {
                    add(BackendChoice.NPU)
                }
                add(BackendChoice.GPU)
                add(BackendChoice.CPU)
            }
            val nextIndex = (order.indexOf(state.backend) + 1) % order.size
            onBackendChange(order[nextIndex])
        }
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            GemmaHeader(
                backend = state.backend,
                onBackendClick = cycleBackend,
                showBorder = !showWelcome,
            )

            if (showWelcome) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    WelcomeContent(
                        onSuggestionClick = onInputChange,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(17.dp),
                    reverseLayout = false,
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    items(state.messages, key = { it.hashCode() }) { message ->
                        MessageBubble(message = message)
                    }
                    if (state.isGenerating && state.messages.lastOrNull()?.isStreaming != true) {
                        item {
                            AssistantStreamingRow()
                        }
                    }
                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }
            }

            if (state.isRecordingAudio) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Enregistrement ${state.recordingElapsedMs / 1000}s",
                        color = GemmaColors.Success,
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Annuler",
                        color = GemmaColors.TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCancelRecording)
                            .background(GemmaColors.SurfaceCard)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            state.pendingImageUri?.let {
                AttachmentChip(label = "Image jointe", onClear = onClearAttachments)
            }
            if (state.pendingAudioUri != null || state.pendingAudioFilePath != null) {
                AttachmentChip(
                    label = state.pendingAudioLabel ?: "Audio joint",
                    onClear = onClearAttachments,
                )
            }

            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    color = GemmaColors.AccentPurpleMid,
                    fontFamily = ManropeFamily,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }

            GemmaInputBar(
                value = state.inputText,
                onValueChange = onInputChange,
                placeholder = if (showWelcome) "Dis bonjour à Gemma…" else "Écris à Gemma…",
                onSend = onSend,
                onAddImage = {
                    requestMediaPermissions()
                    imagePicker.launch("image/*")
                },
                onRecordAudio = {
                    requestMediaPermissions()
                    onToggleRecording()
                },
                isRecording = state.isRecordingAudio,
                isGenerating = state.isGenerating,
                multimodalEnabled = multimodalEnabled,
                showDisclaimer = showWelcome,
            )
        }
    }
}

@Composable
private fun AttachmentChip(
    label: String,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$label · toucher pour retirer",
            color = GemmaColors.AccentPurpleSoft,
            fontFamily = ManropeFamily,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClear)
                .background(GemmaColors.SurfaceCard)
                .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun AssistantStreamingRow() {
    Row(
        modifier = Modifier.fillMaxWidth(0.97f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AssistantAvatar(pulsing = true)
        Column(modifier = Modifier.weight(1f)) {
            StreamingIndicator()
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    when (message.role) {
        MessageRole.USER -> UserBubble(message)
        MessageRole.ASSISTANT -> AssistantBubble(message)
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        message.imageUri?.let { uri ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Image envoyée",
                modifier = Modifier
                    .widthIn(max = 172.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (message.text.isNotEmpty() || message.audioLabel != null) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp))
                    .background(GemmaColors.SurfaceBubble)
                    .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                message.audioLabel?.let { label ->
                    Text(
                        text = label,
                        color = GemmaColors.TextMuted,
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        color = GemmaColors.TextPrimary,
                        fontFamily = ManropeFamily,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(0.97f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AssistantAvatar(pulsing = message.isStreaming)
        Column(modifier = Modifier.weight(1f)) {
            if (message.isStreaming && message.text.isEmpty()) {
                StreamingIndicator()
            }
            if (message.text.isNotEmpty()) {
                Text(
                    text = message.text,
                    color = GemmaColors.TextSecondary,
                    fontFamily = ManropeFamily,
                    fontSize = 14.5.sp,
                    lineHeight = 23.sp,
                )
            }
        }
    }
}

@Composable
private fun AssistantAvatar(pulsing: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(GemmaColors.AccentPurple.copy(alpha = if (pulsing) 0.16f else 0.14f))
            .border(
                width = 1.dp,
                color = GemmaColors.AccentPurpleSoft.copy(alpha = if (pulsing) 0.4f else 0.25f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GemmaLogo(size = 15)
    }
}