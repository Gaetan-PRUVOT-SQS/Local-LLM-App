package com.gaetan.localllmapp.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.localllmapp.device.CompatibilityReport
import com.gaetan.localllmapp.ui.components.CompatibilityBadge
import com.gaetan.localllmapp.ui.components.GemmaFloatingLogo
import com.gaetan.localllmapp.ui.components.GemmaPrimaryButton
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.components.ScanRow
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.ManropeFamily

@Composable
fun OnboardingCompatibilityScreen(
    report: CompatibilityReport?,
    onContinue: () -> Unit,
) {
    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // top: étape + privé
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ÉTAPE 1 / 3",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GemmaColors.TextStatus,
                    letterSpacing = 0.3.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.Lock, null, tint = GemmaColors.Success, modifier = Modifier.size(12.dp))
                    Text(
                        "100% privé",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = GemmaColors.Success,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(14.dp))
                GemmaFloatingLogo(size = 52)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Votre téléphone est prêt",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 23.sp,
                    color = GemmaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "On a vérifié la mémoire, le processeur et l'espace libre. Gemma tournera en natif, hors‑ligne.",
                    fontFamily = ManropeFamily,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = GemmaColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
                CompatibilityBadge(report?.verdictLabel ?: "Analyse en cours…")

                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    val checks = report?.checks.orEmpty()
                    val icons = listOf(Icons.Default.Memory, Icons.Default.DeveloperBoard, Icons.Default.Storage)
                    val bgs = listOf(Color(0x246E78FF), Color(0x29A78BFA), Color(0x2434D39A))
                    val tints = listOf(GemmaColors.AccentPurpleSoft, GemmaColors.CameraPurpleSoft, GemmaColors.SuccessBright)
                    checks.forEachIndexed { i, c ->
                        ScanRow(
                            icon = icons.getOrElse(i) { Icons.Default.Memory },
                            iconTint = tints.getOrElse(i) { GemmaColors.AccentPurpleSoft },
                            iconBg = bgs.getOrElse(i) { Color(0x246E78FF) },
                            title = c.title,
                            detail = c.detail,
                            passed = c.passed,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 16.dp)) {
                GemmaPrimaryButton(
                    label = "Choisir mon modèle",
                    onClick = onContinue,
                    trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                )
            }
        }
    }
}
