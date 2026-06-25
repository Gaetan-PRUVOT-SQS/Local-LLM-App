package com.gaetan.gemmchat.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gaetan.gemmchat.llm.BackendChoice
import com.gaetan.gemmchat.llm.EngineMode
import com.gaetan.gemmchat.ui.components.GemmaHeader
import com.gaetan.gemmchat.ui.components.MarkdownText
import com.gaetan.gemmchat.ui.components.GemmaInputBar
import com.gaetan.gemmchat.ui.components.GemmaLogo
import com.gaetan.gemmchat.ui.components.GemmaScreenBackground
import com.gaetan.gemmchat.ui.components.StreamingIndicator
import com.gaetan.gemmchat.ui.components.WelcomeContent
import com.gaetan.gemmchat.ui.theme.GemmaColors
import com.gaetan.gemmchat.ui.theme.ManropeFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatScreen(
    state: AppUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onImageSelected: (Uri?) -> Unit,
    onAudioSelected: (Uri?, String?) -> Unit,
    onToggleRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onClearAttachments: () -> Unit,
    onBackendChange: (BackendChoice) -> Unit,
    onOpenDrawer: () -> Unit,
    onCloseDrawer: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(state.isDrawerOpen) {
        if (state.isDrawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen && state.isDrawerOpen) onCloseDrawer()
    }

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
                if (state.supportsGemma4Npu && state.selectedModelVariant.supportsNpu) {
                    add(BackendChoice.NPU)
                }
                add(BackendChoice.GPU)
                add(BackendChoice.CPU)
            }
            val nextIndex = (order.indexOf(state.backend) + 1) % order.size
            onBackendChange(order[nextIndex])
        }
    }

    // Vrai bas : plus rien à scroller vers le bas (fiable même quand le dernier
    // message en streaming est plus haut que l'écran).
    val isAtBottom by remember {
        derivedStateOf { !listState.canScrollForward }
    }

    // Suivi auto activé par défaut ; mis en pause si l'utilisateur attrape la
    // liste pour remonter, réactivé quand il revient en bas / lance une génération.
    var autoFollow by remember { mutableStateOf(true) }

    // Un drag utilisateur (et non un scroll programmatique) suspend le suivi.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) autoFollow = false
        }
    }
    // Revenu en bas → on re-suit. Nouvelle génération → on re-suit.
    LaunchedEffect(isAtBottom) { if (isAtBottom) autoFollow = true }
    LaunchedEffect(state.isGenerating) { if (state.isGenerating) autoFollow = true }

    // Colle au bas à chaque token : on vise le DERNIER item lazy (le Spacer de
    // fin) ; Compose clampe au max de scroll = bas réel du contenu. scrollToItem
    // (non animé) pour rester fluide token par token. La clé `isGenerating`
    // déclenche un dernier snap à la finalisation : la fin de génération ajoute
    // la rangée « Copier / horodatage » sous le message, qui sinon resterait
    // sous le pli (texte/taille inchangés → l'effet ne se redéclencherait pas).
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text, state.isGenerating, autoFollow) {
        if (autoFollow && state.messages.isNotEmpty()) {
            val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            listState.scrollToItem(lastIndex)
        }
    }

    val canSend = state.inputText.isNotBlank() ||
        state.pendingImageUri != null ||
        state.pendingAudioUri != null ||
        state.pendingAudioFilePath != null

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawerContent(
                conversations = state.conversations,
                currentConversationId = state.currentConversationId,
                onOpenConversation = onOpenConversation,
                onNewConversation = onNewConversation,
                onRenameConversation = onRenameConversation,
                onDeleteConversation = onDeleteConversation,
            )
        },
    ) {
    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                // Gère TOUS les insets : status bar, barre de nav (y compris à
                // droite en paysage / la taskbar grand écran) et le clavier.
                .safeDrawingPadding(),
        ) {
            GemmaHeader(
                backend = state.backend,
                onBackendClick = cycleBackend,
                onMenuClick = onOpenDrawer,
                onNewChatClick = onNewConversation,
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
                val scope = rememberCoroutineScope()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(17.dp),
                        reverseLayout = false,
                    ) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }
                        if (state.isGenerating && state.messages.lastOrNull()?.isStreaming != true) {
                            item {
                                AssistantStreamingRow()
                            }
                        }
                        item { Spacer(modifier = Modifier.height(6.dp)) }
                    }

                    if (!isAtBottom && state.messages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GemmaColors.SurfaceBubble)
                                .border(1.dp, GemmaColors.BorderLight, CircleShape)
                                .clickable(onClickLabel = "Descendre en bas") {
                                    autoFollow = true
                                    scope.launch {
                                        val lastIndex = (listState.layoutInfo.totalItemsCount - 1)
                                            .coerceAtLeast(0)
                                        listState.animateScrollToItem(lastIndex)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "↓",
                                color = GemmaColors.TextIcon,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
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
                onStop = onStop,
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
                canSend = canSend,
                multimodalEnabled = multimodalEnabled,
                showDisclaimer = true,
            )
        }
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
                    SelectionContainer {
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
        Text(
            text = timeLabel(message.createdAt),
            color = GemmaColors.TextDisclaimer,
            fontFamily = ManropeFamily,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp, end = 2.dp),
        )
    }
}

@Composable
private fun AssistantBubble(message: ChatMessage) {
    val clipboard = LocalClipboardManager.current
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
                SelectionContainer {
                    MarkdownText(
                        markdown = message.text,
                        color = GemmaColors.TextSecondary,
                    )
                }
                if (!message.isStreaming) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClickLabel = "Copier la réponse") {
                                    clipboard.setText(AnnotatedString(message.text))
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copier la réponse",
                                tint = GemmaColors.TextMuted,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Copier",
                                color = GemmaColors.TextMuted,
                                fontFamily = ManropeFamily,
                                fontSize = 11.sp,
                            )
                        }
                        Text(
                            text = timeLabel(message.createdAt),
                            color = GemmaColors.TextDisclaimer,
                            fontFamily = ManropeFamily,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun timeLabel(epochMs: Long): String = remember(epochMs) {
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(epochMs))
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

