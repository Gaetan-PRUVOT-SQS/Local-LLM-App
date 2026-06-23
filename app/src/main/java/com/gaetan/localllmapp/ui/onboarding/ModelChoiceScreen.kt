package com.gaetan.localllmapp.ui.onboarding

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.localllmapp.data.ModelVariant
import com.gaetan.localllmapp.device.CompatibilityReport
import com.gaetan.localllmapp.ui.components.GemmaPrimaryButton
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.components.ModelCard
import com.gaetan.localllmapp.ui.components.StorageBar
import com.gaetan.localllmapp.ui.components.ToggleRow
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.ManropeFamily

@Composable
fun ModelChoiceScreen(
    selectedVariant: ModelVariant,
    textOnly: Boolean,
    wifiOnly: Boolean,
    report: CompatibilityReport?,
    onBack: () -> Unit,
    onToggleTextOnly: (Boolean) -> Unit,
    onToggleWifi: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onSelectSecondary: () -> Unit,
    onImportModel: (Uri) -> Unit,
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImportModel) }
    val free = report?.freeStorageBytes ?: 12_400_000_000L
    val modelBytes = selectedVariant.expectedSizeBytes
    val modelFraction = (modelBytes.toFloat() / free).coerceIn(0.02f, 0.9f)

    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = GemmaColors.TextIcon, modifier = Modifier.size(22.dp))
                }
                Text(
                    "Installer Gemma",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = GemmaColors.TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "2 / 3",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GemmaColors.TextStatus,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                Spacer(Modifier.height(6.dp))
                ModelCard(variant = selectedVariant, selected = true, onClick = {})

                Spacer(Modifier.height(12.dp))
                ToggleRow(
                    title = "Version texte seule",
                    subtitle = "Désactive image & audio · plus rapide",
                    checked = textOnly,
                    onToggle = onToggleTextOnly,
                )

                Spacer(Modifier.height(14.dp))
                StorageBar(
                    usedFraction = 0.02f,
                    modelFraction = modelFraction,
                    leftLabel = "Stockage de l'appareil",
                    rightLabel = "${selectedVariant.sizeLabel} / ${ModelVariant.formatBytes(free)}",
                    footnote = "Reste ${ModelVariant.formatBytes((free - modelBytes).coerceAtLeast(0))} après installation",
                )

                Spacer(Modifier.height(14.dp))
                ToggleRow(
                    title = "Wi‑Fi uniquement",
                    subtitle = "Évite les frais de données mobiles",
                    checked = wifiOnly,
                    onToggle = onToggleWifi,
                    leadingIcon = Icons.Default.Wifi,
                )
                Spacer(Modifier.height(20.dp))
            }

            Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 14.dp)) {
                GemmaPrimaryButton(
                    label = "Télécharger · ${selectedVariant.sizeLabel}",
                    onClick = onDownload,
                    leadingIcon = Icons.Default.Download,
                )
                // Charger un modèle déjà présent sur le téléphone (pas de téléchargement).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(GemmaColors.SurfaceCard)
                        .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(15.dp))
                        .clickable { filePicker.launch(arrayOf("*/*")) }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = GemmaColors.AccentPurpleSoft, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Charger un modèle déjà présent (.litertlm)",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = GemmaColors.TextSecondary,
                    )
                }
                Text(
                    text = "Choisir ${ModelVariant.E4B.label} · qualité supérieure",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = GemmaColors.AccentPurpleSoft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 11.dp)
                        .clickable(onClick = onSelectSecondary),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
