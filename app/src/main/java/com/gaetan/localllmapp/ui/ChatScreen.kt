package com.gaetan.localllmapp.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gaetan.localllmapp.data.Skill
import com.gaetan.localllmapp.data.Skills
import com.gaetan.localllmapp.llm.BackendChoice
import com.gaetan.localllmapp.llm.EngineMode
import com.gaetan.localllmapp.ui.components.GemmaInputBar
import com.gaetan.localllmapp.ui.components.GemmaLogo
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.components.StreamingIndicator
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.JetBrainsMonoFamily
import com.gaetan.localllmapp.ui.theme.ManropeFamily

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
    onBackToHub: () -> Unit,
    onRegenerate: () -> Unit,
    onSettings: () -> Unit,
    onClearCache: () -> Unit,
    onOpenHistory: () -> Unit,
    onConsumePendingAction: () -> Unit,
    onSelectSkill: (Skill) -> Unit,
    onClearSkill: () -> Unit,
) {
    val listState = rememberLazyListState()
    val multimodalEnabled = state.engineMode == EngineMode.MULTIMODAL
    val showWelcome = state.messages.isEmpty() && !state.isGenerating
    var menuOpen by remember { mutableStateOf(false) }
    var skillsOpen by remember { mutableStateOf(false) }
    val pickerContext = LocalContext.current

    // OpenDocument (au lieu de GetContent) + permission persistable → l'image reste
    // accessible après redémarrage / restauration depuis l'historique.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                pickerContext.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        onImageSelected(uri)
    }

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

    // NPU proposé uniquement là où le delegate fonctionne réellement (Tensor G5) —
    // ailleurs on resterait sur GPU, ce qui serait trompeur.
    val cycleBackend = remember(state.backend, state.supportsGemma4Npu) {
        {
            val order = buildList {
                if (state.supportsGemma4Npu) add(BackendChoice.NPU)
                add(BackendChoice.GPU)
                add(BackendChoice.CPU)
            }
            val nextIndex = (order.indexOf(state.backend) + 1) % order.size
            onBackendChange(order[nextIndex])
        }
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    // Cartes Hub « 1-tap » : ouvre directement le sélecteur d'image / l'enregistrement.
    LaunchedEffect(state.pendingChatAction) {
        when (state.pendingChatAction) {
            PendingChatAction.PICK_IMAGE -> {
                requestMediaPermissions()
                imagePicker.launch(arrayOf("image/*"))
                onConsumePendingAction()
            }
            PendingChatAction.START_RECORDING -> {
                requestMediaPermissions()
                if (!state.isRecordingAudio) onToggleRecording()
                onConsumePendingAction()
            }
            PendingChatAction.SHOW_SKILLS -> {
                skillsOpen = true
                onConsumePendingAction()
            }
            PendingChatAction.NONE -> Unit
        }
    }

    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                ChatHeader(
                    backend = state.backend,
                    variantLabel = state.selectedModelVariant.label,
                    onMenuClick = onBackToHub,
                    onBackendClick = cycleBackend,
                    onOverflowClick = { menuOpen = !menuOpen },
                    overflowActive = menuOpen,
                )

                if (showWelcome) {
                    ChatWelcome(onSuggestionClick = onInputChange, modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(17.dp),
                    ) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        // Clé = index (unique) — hashCode() peut collisionner entre messages identiques.
                        items(state.messages.size) { index ->
                            MessageBubble(message = state.messages[index], onRegenerate = onRegenerate)
                        }
                        if (state.isGenerating && state.messages.lastOrNull()?.isStreaming != true) {
                            item { AssistantStreamingRow() }
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
                    AttachmentChip(label = state.pendingAudioLabel ?: "Audio joint", onClear = onClearAttachments)
                }

                state.statusMessage?.let { message ->
                    Text(
                        text = message,
                        color = GemmaColors.AccentPurpleMid,
                        fontFamily = ManropeFamily,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }

                // footer télémétrie (06) + accès Skills — toujours visible
                TelemetryFooter(
                    skillsCount = state.skillsCount,
                    activeSkill = state.activeSkill,
                    tokensPerSec = state.liveTokensPerSec ?: state.messages.lastOrNull { it.tokensPerSec != null }?.tokensPerSec,
                    tempC = state.batteryTempC,
                    batteryPercent = state.batteryPercent,
                    onSkillsClick = { skillsOpen = true },
                )

                GemmaInputBar(
                    value = state.inputText,
                    onValueChange = onInputChange,
                    placeholder = if (showWelcome) "Dis bonjour à Gemma…" else "Écris à Gemma…",
                    onSend = onSend,
                    onAddImage = {
                        requestMediaPermissions()
                        imagePicker.launch(arrayOf("image/*"))
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

            if (menuOpen) {
                OverflowMenu(
                    onDismiss = { menuOpen = false },
                    onHistory = { menuOpen = false; onOpenHistory() },
                    onSettings = { menuOpen = false; onSettings() },
                    onClearCache = { menuOpen = false; onClearCache() },
                )
            }

            if (skillsOpen) {
                SkillsSheet(
                    active = state.activeSkill,
                    onSelect = { onSelectSkill(it); skillsOpen = false },
                    onClear = { onClearSkill(); skillsOpen = false },
                    onDismiss = { skillsOpen = false },
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    backend: BackendChoice,
    variantLabel: String,
    onMenuClick: () -> Unit,
    onBackendClick: () -> Unit,
    onOverflowClick: () -> Unit,
    overflowActive: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onMenuClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuOpen, "Hub", tint = GemmaColors.TextIcon, modifier = Modifier.size(23.dp))
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(GemmaColors.SurfacePill)
                    .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(999.dp))
                    .clickable(onClick = onBackendClick)
                    .padding(start = 9.dp, end = 11.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GemmaLogo(size = 15)
                Text(variantLabel, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GemmaColors.TextPrimary)
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GemmaColors.Success))
                Icon(Icons.Default.KeyboardArrowDown, null, tint = GemmaColors.IconMuted, modifier = Modifier.size(15.dp))
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(if (overflowActive) Modifier.background(GemmaColors.AccentPurple.copy(alpha = 0.12f)) else Modifier)
                    .clickable(onClick = onOverflowClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MoreVert, "Menu", tint = if (overflowActive) GemmaColors.AccentPurpleSoft else GemmaColors.TextIcon, modifier = Modifier.size(22.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GemmaColors.Success))
            Spacer(Modifier.width(5.dp))
            Text("Hors‑ligne", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            Sep()
            Text("100% local", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            Sep()
            Text(backend.name, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Sep() {
    Text("·", color = GemmaColors.TextStatus.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 9.dp), fontSize = 11.sp)
}

@Composable
private fun OverflowMenu(
    onDismiss: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onClearCache: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .padding(top = 52.dp, end = 12.dp)
                .align(Alignment.TopEnd)
                .width(208.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1C1E26))
                .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(14.dp))
                .padding(6.dp),
        ) {
            MenuItem(Icons.Default.History, "Historique", GemmaColors.TextMuted, onHistory)
            MenuItem(Icons.Default.Settings, "Paramètres", GemmaColors.TextMuted, onSettings)
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 8.dp).background(GemmaColors.BorderSubtle))
            MenuItem(Icons.Default.DeleteOutline, "Vider le cache modèle", Color(0xFFFF8585), onClearCache, danger = true)
        }
    }
}

@Composable
private fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit, danger: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = if (danger) Color(0xFFFF8585) else GemmaColors.TextSecondary)
    }
}

@Composable
private fun TelemetryFooter(
    skillsCount: Int,
    activeSkill: Skill?,
    tokensPerSec: Int?,
    tempC: Float?,
    batteryPercent: Int?,
    onSkillsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val skillActive = activeSkill != null
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (skillActive) GemmaColors.AccentPurple.copy(alpha = 0.16f) else GemmaColors.SurfaceCard)
                .border(
                    1.dp,
                    if (skillActive) GemmaColors.AccentPurple.copy(alpha = 0.5f) else GemmaColors.BorderLight,
                    RoundedCornerShape(999.dp),
                )
                .clickable(onClick = onSkillsClick)
                .padding(start = 9.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Star, null, tint = GemmaColors.StarGold, modifier = Modifier.size(13.dp))
            Text(
                activeSkill?.name ?: "Skills",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (skillActive) GemmaColors.AccentPurpleSoft else GemmaColors.TextIcon,
            )
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GemmaColors.AccentPurple).padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(if (skillActive) "1" else "$skillsCount", fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color.White)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            tokensPerSec?.let {
                Text("$it tok/s", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = GemmaColors.AccentPurpleSoft)
            }
            tempC?.let {
                Text("${it.toInt()} °C", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = GemmaColors.TextStats)
            }
            batteryPercent?.let {
                Text("$it%", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = GemmaColors.Success)
            }
        }
    }
}

@Composable
private fun ChatWelcome(onSuggestionClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        com.gaetan.localllmapp.ui.components.GemmaFloatingLogo(size = 56)
        Spacer(Modifier.height(18.dp))
        Text("Salut, moi c'est Gemma.", fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = GemmaColors.TextPrimary)
        Text(
            "Je tourne entièrement sur ton téléphone. Pour commencer, dis‑moi simplement bonjour.",
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = GemmaColors.TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(22.dp))
        val suggestions = listOf(
            "Résume‑moi ce texte en 3 points",
            "Analyse cette photo de plante",
            "Code une page HTML simple",
        )
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { s ->
                Text(
                    text = s,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = GemmaColors.TextIcon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(GemmaColors.SurfaceCard)
                        .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(13.dp))
                        .clickable { onSuggestionClick(s) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun AttachmentChip(label: String, onClear: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
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
    Row(modifier = Modifier.fillMaxWidth(0.97f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AssistantAvatar(pulsing = true)
        Column(modifier = Modifier.weight(1f)) { StreamingIndicator() }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onRegenerate: () -> Unit) {
    when (message.role) {
        MessageRole.USER -> UserBubble(message)
        MessageRole.ASSISTANT -> AssistantBubble(message, onRegenerate)
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        message.imageUri?.let { uri ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
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
                    Text(label, color = GemmaColors.TextMuted, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (message.text.isNotEmpty()) {
                    Text(message.text, color = GemmaColors.TextPrimary, fontFamily = ManropeFamily, fontSize = 14.5.sp, lineHeight = 21.sp)
                }
            }
        }
    }
}

@Composable
private fun AssistantBubble(message: ChatMessage, onRegenerate: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(0.97f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AssistantAvatar(pulsing = message.isStreaming)
        Column(modifier = Modifier.weight(1f)) {
            if (message.isStreaming && message.text.isEmpty()) StreamingIndicator()
            if (message.text.isNotEmpty()) {
                Text(renderMarkdown(message.text), color = GemmaColors.TextSecondary, fontFamily = ManropeFamily, fontSize = 14.5.sp, lineHeight = 23.sp)
            }
            // actions (07) — uniquement quand la réponse est terminée
            if (!message.isStreaming && message.text.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ActionPill(Icons.Default.ContentCopy, "Copier", filled = true) {
                        clipboard.setText(AnnotatedString(message.text))
                    }
                    ActionPill(Icons.Default.Autorenew, "Régénérer", filled = false, onClick = onRegenerate)
                    ActionPill(Icons.Default.Share, "Partager", filled = false) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message.text)
                        }
                        context.startActivity(Intent.createChooser(send, "Partager"))
                    }
                    Spacer(Modifier.weight(1f))
                    if (message.tokensPerSec != null && message.elapsedSec != null) {
                        Text(
                            text = String.format(java.util.Locale.FRANCE, "%.1f s · %d t/s", message.elapsedSec, message.tokensPerSec),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = GemmaColors.TextDisclaimer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, filled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .then(if (filled) Modifier.background(GemmaColors.SurfaceElevated).border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(9.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = if (filled) GemmaColors.TextIcon else GemmaColors.TextMuted, modifier = Modifier.size(14.dp))
        Text(label, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (filled) GemmaColors.TextIcon else GemmaColors.TextMuted)
    }
}

@Composable
private fun SkillsSheet(
    active: Skill?,
    onSelect: (Skill) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(GemmaColors.SurfaceCard)
                .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                // consomme les clics pour ne pas fermer la feuille
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GemmaColors.BorderLight),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Skills", fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = GemmaColors.TextPrimary)
                if (active != null) {
                    Text(
                        "Désactiver",
                        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = GemmaColors.AccentPurpleSoft,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClear).padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            Text(
                "Choisis un style appliqué à tes prochains messages.",
                fontFamily = ManropeFamily, fontSize = 12.sp, color = GemmaColors.TextMuted,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Skills.all.forEach { skill ->
                SkillRow(skill = skill, selected = active?.id == skill.id, onClick = { onSelect(skill) })
            }
        }
    }
}

@Composable
private fun SkillRow(skill: Skill, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) GemmaColors.AccentPurple.copy(alpha = 0.14f) else GemmaColors.SurfaceElevated)
            .border(
                1.dp,
                if (selected) GemmaColors.AccentPurple.copy(alpha = 0.5f) else GemmaColors.BorderSubtle,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(skill.emoji, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(skill.name, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GemmaColors.TextSecondary)
            Text(skill.description, fontFamily = ManropeFamily, fontSize = 11.5.sp, color = GemmaColors.TextFaint, modifier = Modifier.padding(top = 1.dp))
        }
        if (selected) {
            Icon(Icons.Default.Check, null, tint = GemmaColors.Success, modifier = Modifier.size(18.dp))
        }
    }
}

/** Rendu markdown léger : **gras** et `code` inline (le reste passe en texte brut). */
private fun renderMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = GemmaColors.TextPrimary)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text.substring(i)); i = text.length
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = JetBrainsMonoFamily, color = GemmaColors.AccentPurpleSoft)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            else -> { append(text[i]); i++ }
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
            .border(1.dp, GemmaColors.AccentPurpleSoft.copy(alpha = if (pulsing) 0.4f else 0.25f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        GemmaLogo(size = 15)
    }
}
