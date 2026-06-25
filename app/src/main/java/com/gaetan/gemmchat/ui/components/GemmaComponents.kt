package com.gaetan.gemmchat.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.gemmchat.R
import com.gaetan.gemmchat.llm.BackendChoice
import com.gaetan.gemmchat.ui.theme.GemmaColors
import com.gaetan.gemmchat.ui.theme.JetBrainsMonoFamily
import com.gaetan.gemmchat.ui.theme.ManropeFamily

@Composable
fun GemmaLogo(
    modifier: Modifier = Modifier,
    size: Int = 24,
) {
    Icon(
        painter = painterResource(R.drawable.ic_gemma_logo),
        contentDescription = "Gemma",
        modifier = modifier.size(size.dp),
        tint = Color.Unspecified,
    )
}

@Composable
fun GemmaHeader(
    backend: BackendChoice,
    onBackendClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    showBorder: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = 1.dp,
                        color = GemmaColors.BorderSubtle,
                        shape = RoundedCornerShape(0.dp),
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuOpen,
                    contentDescription = "Menu",
                    tint = GemmaColors.TextIcon,
                    modifier = Modifier.size(23.dp),
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(GemmaColors.SurfacePill)
                    .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GemmaLogo(size = 15)
                Text(
                    text = "Gemma 4 E2B Q4",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GemmaColors.TextPrimary,
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GemmaColors.Success),
                )
            }

            IconButton(onClick = onNewChatClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Nouvelle conversation",
                    tint = GemmaColors.TextIcon,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (showBorder) 9.dp else 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot()
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Hors‑ligne",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
            StatusSeparator()
            Text(
                text = "100% local",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
            StatusSeparator()
            Row(
                modifier = Modifier.clickable(
                    onClickLabel = "Changer de moteur (actuel : ${backend.name})",
                    onClick = onBackendClick,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = GemmaColors.TextStatus,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = backend.name,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun StatusDot() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(GemmaColors.Success),
    )
}

@Composable
private fun StatusSeparator() {
    Text(
        text = "·",
        color = GemmaColors.TextStatus.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 9.dp),
        fontSize = 11.sp,
    )
}

@Composable
fun WelcomeContent(
    onSuggestionClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.padding(bottom = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x4D6E78FF),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.minDimension / 2f,
                            ),
                        )
                    },
            )
            GemmaLogo(size = 58)
        }

        Text(
            text = "Salut, moi c'est Gemma.",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Je tourne entièrement sur ton téléphone. Aucune donnée ne quitte ton appareil.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 11.dp),
        )

        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            FeaturePill(Icons.Default.Lock, "100% local", GemmaColors.AccentPurpleSoft)
            FeaturePill(Icons.Default.WifiOff, "Hors‑ligne", GemmaColors.Success)
            FeaturePill(Icons.Default.Lock, "Chiffré", GemmaColors.CameraPurple)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CapabilityCard(
                    icon = Icons.Default.PhotoCamera,
                    iconTint = GemmaColors.AccentPurpleSoft,
                    iconBg = Color(0x266E78FF),
                    title = "Analyse une photo",
                    subtitle = "Décris ou explique une image, hors‑ligne",
                    modifier = Modifier.weight(1f),
                    onClick = { onSuggestionClick("Décris cette image en détail.") },
                )
                CapabilityCard(
                    icon = Icons.Default.Mic,
                    iconTint = GemmaColors.CameraPurpleSoft,
                    iconBg = Color(0x29A78BFA),
                    title = "Transcris un audio",
                    subtitle = "Voix → texte en temps réel",
                    modifier = Modifier.weight(1f),
                    onClick = { onSuggestionClick("Transcris l'audio que je vais t'envoyer.") },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CapabilityCard(
                    icon = Icons.Default.Code,
                    iconTint = GemmaColors.SuccessBright,
                    iconBg = Color(0x2634D39A),
                    title = "Code une mini‑app",
                    subtitle = "Génère un script ou une page HTML",
                    modifier = Modifier.weight(1f),
                    onClick = { onSuggestionClick("Génère une mini page HTML interactive.") },
                )
                CapabilityCard(
                    icon = Icons.Default.Summarize,
                    iconTint = GemmaColors.StarGold,
                    iconBg = Color(0x29F2B45C),
                    title = "Résume un texte",
                    subtitle = "Colle un texte, j'en fais une synthèse",
                    modifier = Modifier.weight(1f),
                    onClick = { onSuggestionClick("Résume le texte suivant :\n") },
                )
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    label: String,
    iconTint: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GemmaColors.SurfaceCard)
            .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(13.dp))
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            color = GemmaColors.TextIcon,
        )
    }
}

@Composable
private fun CapabilityCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GemmaColors.SurfaceCard)
            .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
        }
        Text(
            text = title,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = GemmaColors.TextSecondary,
        )
        Text(
            text = subtitle,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun StreamingIndicator() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { index ->
                val animatedOffset by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, delayMillis = index * 160),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dotOffset$index",
                )
                val alpha by transition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, delayMillis = index * 160),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dotAlpha$index",
                )
                Box(
                    modifier = Modifier
                        .offset(y = animatedOffset.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(GemmaColors.AccentPurpleSoft.copy(alpha = alpha)),
                )
            }
        }
        Text(
            text = "Gemma écrit…",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = GemmaColors.AccentPurpleSoft,
        )
    }
}

@Composable
fun GemmaInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAddImage: () -> Unit,
    onRecordAudio: () -> Unit,
    isRecording: Boolean,
    isGenerating: Boolean,
    canSend: Boolean,
    multimodalEnabled: Boolean,
    showDisclaimer: Boolean = true,
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(GemmaColors.SurfaceElevated)
                .border(1.dp, Color(0x17FFFFFF), RoundedCornerShape(26.dp))
                .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Ajouter",
                tint = GemmaColors.TextMuted,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(enabled = multimodalEnabled && !isGenerating) { onAddImage() },
            )

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    color = GemmaColors.TextPrimary,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(GemmaColors.AccentPurpleSoft),
                enabled = !isGenerating && !isRecording,
                singleLine = false,
                maxLines = 4,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 15.sp,
                                color = GemmaColors.TextDim,
                            )
                        }
                        inner()
                    }
                },
            )

            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Image",
                tint = GemmaColors.TextIcon,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(enabled = multimodalEnabled && !isGenerating && !isRecording) {
                        onAddImage()
                    },
            )

            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Arrêter l'enregistrement" else "Enregistrer un audio",
                tint = if (isRecording) GemmaColors.Success else GemmaColors.TextIcon,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(enabled = multimodalEnabled && !isGenerating) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRecordAudio()
                    },
            )

            val sendEnabled = if (isGenerating) true else canSend && !isRecording
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .then(
                        when {
                            isGenerating -> Modifier.background(GemmaColors.SurfaceInput)
                            !sendEnabled -> Modifier.background(GemmaColors.SurfaceInput)
                            else -> Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        GemmaColors.AccentPurple,
                                        GemmaColors.AccentPurpleMid,
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(100f, 100f),
                                ),
                            )
                        },
                    )
                    .clickable(
                        enabled = sendEnabled,
                        onClickLabel = if (isGenerating) "Arrêter la génération" else "Envoyer le message",
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isGenerating) onStop() else onSend()
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isGenerating) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Arrêter la génération",
                        tint = GemmaColors.TextIcon,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = "↑",
                        color = if (sendEnabled) Color.White else GemmaColors.TextDim,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (showDisclaimer) {
            Text(
                text = "Gemma peut se tromper. Tout reste sur ton appareil.",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp),
            )
        }
    }
}

@Composable
fun GemmaScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GemmaColors.Background),
    ) {
        content()
    }
}

@Composable
fun MonoStatsText(
    text: String,
    color: Color = GemmaColors.TextStats,
) {
    Text(
        text = text,
        fontFamily = JetBrainsMonoFamily,
        fontSize = 10.5.sp,
        color = color,
        letterSpacing = 0.2.sp,
    )
}