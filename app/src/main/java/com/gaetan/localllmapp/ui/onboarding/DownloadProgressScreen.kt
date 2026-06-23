package com.gaetan.localllmapp.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.localllmapp.data.DownloadProgress
import com.gaetan.localllmapp.data.ModelVariant
import com.gaetan.localllmapp.ui.components.GemmaFloatingLogo
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.components.StepRail
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.JetBrainsMonoFamily
import com.gaetan.localllmapp.ui.theme.ManropeFamily

@Composable
fun DownloadProgressScreen(
    variant: ModelVariant,
    progress: DownloadProgress?,
    paused: Boolean,
    waitingNetwork: String?,
    error: String?,
    wifiOnly: Boolean,
    importing: Boolean,
    onPauseToggle: () -> Unit,
    onCancel: () -> Unit,
) {
    val percent = ((progress?.percent ?: 0f) * 100).toInt().coerceIn(0, 100)
    val statusLine = when {
        error != null -> error
        importing -> "Import du modèle local…"
        waitingNetwork != null -> waitingNetwork
        paused -> "En pause"
        wifiOnly -> "Téléchargement sur Wi‑Fi…"
        else -> "Téléchargement…"
    }

    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Installation", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = GemmaColors.TextPrimary)
                Text("3 / 3", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GemmaColors.TextStatus)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(10.dp))
                StepRail(currentStep = 1)
                Spacer(Modifier.height(20.dp))
                GemmaFloatingLogo(size = 46)
                Spacer(Modifier.height(14.dp))
                Text(variant.label, fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = GemmaColors.TextPrimary)
                Text(
                    statusLine,
                    fontFamily = ManropeFamily,
                    fontSize = 12.5.sp,
                    color = if (error != null) GemmaColors.AccentPurpleMid else GemmaColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 3.dp),
                )

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$percent",
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(GemmaColors.AccentPurpleSoft, GemmaColors.AccentPurplePale)),
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 46.sp,
                        ),
                    )
                    Text("%", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = GemmaColors.AccentPurpleSoft, modifier = Modifier.padding(bottom = 6.dp))
                }

                Spacer(Modifier.height(16.dp))
                ShimmerProgressBar(fraction = (progress?.percent ?: 0f).coerceIn(0f, 1f), animate = !paused && error == null)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (progress != null)
                            "${ModelVariant.formatBytes(progress.bytesCopied)} / ${ModelVariant.formatBytes(progress.totalBytes)}"
                        else "— / ${variant.sizeLabel}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.5.sp,
                        color = GemmaColors.TextStats,
                    )
                    Text(
                        text = progress?.let { formatEta(it.etaSeconds) } ?: "",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.5.sp,
                        color = GemmaColors.AccentPurpleSoft,
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(GemmaColors.AccentPurple.copy(alpha = 0.08f))
                        .border(1.dp, GemmaColors.AccentPurple.copy(alpha = 0.18f), RoundedCornerShape(13.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(Icons.Default.Info, null, tint = GemmaColors.AccentPurpleSoft, modifier = Modifier.size(18.dp))
                    Text(
                        if (importing) "Copie du fichier depuis le stockage de l'appareil. Garde l'app ouverte."
                        else "Garde l'app ouverte. La connexion peut couper — le téléchargement reprendra tout seul.",
                        fontFamily = ManropeFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = GemmaColors.TextIcon,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Pas de pause/reprise pour un import local (copie atomique).
                if (!importing) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(GemmaColors.SurfaceElevated)
                            .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(15.dp))
                            .clickable(onClick = onPauseToggle),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            null,
                            tint = GemmaColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (paused) "Reprendre" else "Pause",
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = GemmaColors.TextSecondary,
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(GemmaColors.SurfaceElevated)
                        .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(15.dp))
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Annuler", tint = GemmaColors.TextMuted, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

@Composable
private fun ShimmerProgressBar(fraction: Float, animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "shift",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF1C1E27)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(9.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    Brush.horizontalGradient(listOf(GemmaColors.AccentPurple, GemmaColors.AccentPurpleMid)),
                ),
        ) {
            if (animate && fraction > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(9.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0x73FFFFFF), Color.Transparent),
                                startX = shift * 600f - 200f,
                                endX = shift * 600f,
                            ),
                        ),
                )
            }
        }
    }
}

private fun formatEta(seconds: Long): String = when {
    seconds <= 0 -> ""
    seconds < 60 -> "≈ ${seconds} s restantes"
    else -> "≈ ${seconds / 60} min restantes"
}
