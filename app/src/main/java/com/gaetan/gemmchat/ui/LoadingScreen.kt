package com.gaetan.gemmchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.gemmchat.ui.components.GemmaLogo
import com.gaetan.gemmchat.ui.components.GemmaScreenBackground
import com.gaetan.gemmchat.ui.theme.GemmaColors
import com.gaetan.gemmchat.ui.theme.ManropeFamily

@Composable
fun LoadingScreen(
    state: AppUiState,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    onRetryExtract: () -> Unit,
) {
    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GemmaLogo(size = 58)
            Spacer(modifier = Modifier.height(24.dp))

            when {
                state.isExtractingModel -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = GemmaColors.AccentPurpleSoft,
                        strokeWidth = 3.dp,
                    )
                    Text(
                        text = "Préparation de Gemma 4 E2B Q4…",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GemmaColors.TextPrimary,
                        modifier = Modifier.padding(top = 20.dp),
                        textAlign = TextAlign.Center,
                    )
                    state.extractProgress?.let { progress ->
                        LinearProgressIndicator(
                            progress = { progress.percent.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = GemmaColors.AccentPurpleSoft,
                            trackColor = GemmaColors.SurfaceElevated,
                        )
                        Text(
                            text = "${(progress.percent * 100).toInt()}% — première installation uniquement",
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = GemmaColors.TextMuted,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    } ?: Text(
                        text = "Assemblage depuis l'APK (~2,4 Go, une fois)…",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = GemmaColors.TextMuted,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                state.extractError != null -> {
                    Text(
                        text = state.extractError,
                        fontFamily = ManropeFamily,
                        fontSize = 15.sp,
                        color = GemmaColors.AccentPurpleMid,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ActionButton("Réessayer", onRetryExtract)
                }

                state.isInitializing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = GemmaColors.AccentPurpleSoft,
                        strokeWidth = 3.dp,
                    )
                    Text(
                        text = "Chargement de Gemma 4 E2B Q4…",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GemmaColors.TextPrimary,
                        modifier = Modifier.padding(top = 20.dp),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Premier chargement : ~10 secondes",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = GemmaColors.TextMuted,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                else -> {
                    Text(
                        text = state.initError ?: "Erreur d'initialisation",
                        fontFamily = ManropeFamily,
                        fontSize = 15.sp,
                        color = GemmaColors.AccentPurpleMid,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ActionButton("Réessayer", onRetry)
                    Spacer(modifier = Modifier.height(10.dp))
                    SecondaryButton("Réinstaller le modèle", onReset)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(
                    colors = listOf(GemmaColors.AccentPurple, GemmaColors.AccentPurpleMid),
                ),
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(24.dp))
            .background(GemmaColors.SurfaceCard)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = GemmaColors.TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}