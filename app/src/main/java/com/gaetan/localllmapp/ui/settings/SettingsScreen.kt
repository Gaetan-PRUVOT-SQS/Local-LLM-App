package com.gaetan.localllmapp.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.localllmapp.BuildConfig
import com.gaetan.localllmapp.llm.BackendChoice
import com.gaetan.localllmapp.ui.AppUiState
import com.gaetan.localllmapp.ui.components.GemmaLogo
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.components.ToggleRow
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.ManropeFamily

@Composable
fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onBackendChange: (BackendChoice) -> Unit,
    onToggleWifi: (Boolean) -> Unit,
    onClearConversations: () -> Unit,
    onClearCache: () -> Unit,
) {
    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = GemmaColors.TextIcon, modifier = Modifier.size(22.dp))
                }
                Text("Paramètres", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GemmaColors.TextPrimary)
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                // Modèle
                SectionTitle("Modèle")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GemmaColors.SurfaceCard)
                        .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GemmaLogo(size = 32)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.selectedModelVariant.label, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GemmaColors.TextSecondary)
                        Text(
                            "${state.selectedModelVariant.sizeLabel} · installé",
                            fontFamily = ManropeFamily, fontSize = 11.5.sp, color = GemmaColors.TextFaint, modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GemmaColors.Success))
                }

                // Accélération
                SectionTitle("Accélération")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val backends = buildList {
                        if (state.supportsGemma4Npu) add(BackendChoice.NPU)
                        add(BackendChoice.GPU)
                        add(BackendChoice.CPU)
                    }
                    backends.forEach { b ->
                        BackendChip(
                            label = b.name,
                            selected = state.backend == b,
                            modifier = Modifier.weight(1f),
                            onClick = { onBackendChange(b) },
                        )
                    }
                }
                Text(
                    "Le moteur retombe automatiquement sur GPU/CPU si l'option n'est pas disponible.",
                    fontFamily = ManropeFamily, fontSize = 11.sp, lineHeight = 15.sp, color = GemmaColors.TextStatus,
                    modifier = Modifier.padding(top = 8.dp, start = 2.dp),
                )

                // Préférences
                SectionTitle("Préférences")
                ToggleRow(
                    title = "Wi‑Fi uniquement",
                    subtitle = "Télécharger le modèle seulement en Wi‑Fi",
                    checked = state.wifiOnly,
                    onToggle = onToggleWifi,
                    leadingIcon = Icons.Default.Wifi,
                )

                // Données
                SectionTitle("Données")
                DestructiveRow(Icons.Default.DeleteSweep, "Effacer les discussions", onClearConversations)
                Spacer(Modifier.height(8.dp))
                DestructiveRow(Icons.Default.DeleteOutline, "Vider le cache modèle", onClearCache)

                // À propos
                SectionTitle("À propos")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GemmaColors.SurfaceCard)
                        .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AboutRow("Version", BuildConfig.VERSION_NAME)
                    AboutRow("Exécution", "100% locale · hors‑ligne")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Lock, null, tint = GemmaColors.Success, modifier = Modifier.size(13.dp))
                        Text("Aucune donnée ne quitte l'appareil pendant l'inférence", fontFamily = ManropeFamily, fontSize = 11.5.sp, color = GemmaColors.TextMuted)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 0.7.sp,
        color = GemmaColors.TextStatus,
        modifier = Modifier.padding(top = 22.dp, bottom = 11.dp, start = 2.dp),
    )
}

@Composable
private fun BackendChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) GemmaColors.AccentPurple.copy(alpha = 0.16f) else GemmaColors.SurfaceCard)
            .border(
                1.dp,
                if (selected) GemmaColors.AccentPurple.copy(alpha = 0.6f) else GemmaColors.BorderSubtle,
                RoundedCornerShape(13.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = if (selected) GemmaColors.AccentPurpleSoft else GemmaColors.TextMuted,
        )
    }
}

@Composable
private fun DestructiveRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(GemmaColors.SurfaceCard)
            .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = Color(0xFFFF8585), modifier = Modifier.size(20.dp))
        Text(label, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color(0xFFFF8585))
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = ManropeFamily, fontSize = 13.sp, color = GemmaColors.TextMuted)
        Text(value, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GemmaColors.TextSecondary)
    }
}
