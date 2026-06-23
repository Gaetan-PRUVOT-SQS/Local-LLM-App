package com.gaetan.localllmapp.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.gaetan.localllmapp.ui.components.GemmaFloatingLogo
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.ManropeFamily

@Composable
fun LoadingScreen(
    state: AppUiState,
    onRetry: () -> Unit,
    onReset: () -> Unit,
) {
    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GemmaFloatingLogo(size = 52)
            Spacer(modifier = Modifier.height(24.dp))

            if (state.initError == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = GemmaColors.AccentPurpleSoft,
                    strokeWidth = 3.dp,
                )
                Text(
                    text = "Chargement de ${state.selectedModelVariant.label}…",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GemmaColors.TextPrimary,
                    modifier = Modifier.padding(top = 20.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Mise en mémoire du modèle…",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = GemmaColors.TextMuted,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = state.initError,
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

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(listOf(GemmaColors.AccentPurple, GemmaColors.AccentPurpleMid)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(24.dp))
            .background(GemmaColors.SurfaceCard),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GemmaColors.TextMuted)
    }
}
