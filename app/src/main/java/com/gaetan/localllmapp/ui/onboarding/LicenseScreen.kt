package com.gaetan.localllmapp.ui.onboarding

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.gaetan.localllmapp.ui.components.GemmaPrimaryButton
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.ManropeFamily

private const val GEMMA_TERMS_URL = "https://ai.google.dev/gemma/terms"
private const val GEMMA_PROHIBITED_URL = "https://ai.google.dev/gemma/prohibited_use_policy"

@Composable
fun LicenseScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val context = LocalContext.current
    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GemmaColors.AccentPurple.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Lock, null, tint = GemmaColors.AccentPurpleSoft, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Licence du modèle Gemma",
                    fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = GemmaColors.TextPrimary,
                )
                Text(
                    "Le modèle Gemma 4 E2B est fourni par Google sous les Gemma Terms of Use. " +
                        "En téléchargeant et en utilisant ce modèle, vous acceptez ces conditions, " +
                        "y compris la politique d'utilisation interdite.",
                    fontFamily = ManropeFamily, fontSize = 14.sp, lineHeight = 21.sp, color = GemmaColors.TextMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )

                Spacer(Modifier.height(18.dp))
                LinkRow("Gemma Terms of Use", GEMMA_TERMS_URL) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, GEMMA_TERMS_URL.toUri()))
                }
                Spacer(Modifier.height(8.dp))
                LinkRow("Politique d'utilisation interdite", GEMMA_PROHIBITED_URL) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, GEMMA_PROHIBITED_URL.toUri()))
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "Le modèle s'exécute ensuite entièrement sur votre appareil. Aucune donnée " +
                        "d'inférence n'est envoyée à Google ni à un tiers.",
                    fontFamily = ManropeFamily, fontSize = 12.5.sp, lineHeight = 18.sp, color = GemmaColors.TextStatus,
                )
                Spacer(Modifier.height(20.dp))
            }

            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 16.dp)) {
                GemmaPrimaryButton(label = "Accepter et télécharger", onClick = onAccept)
                Text(
                    "Refuser",
                    fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GemmaColors.TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDecline)
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LinkRow(label: String, url: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GemmaColors.SurfaceCard)
            .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = GemmaColors.AccentPurpleSoft)
            Text(url, fontFamily = ManropeFamily, fontSize = 10.5.sp, color = GemmaColors.TextStatus, modifier = Modifier.padding(top = 1.dp))
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = GemmaColors.TextStatus, modifier = Modifier.size(18.dp))
    }
}
