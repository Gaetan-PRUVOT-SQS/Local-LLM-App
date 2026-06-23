package com.gaetan.localllmapp.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.localllmapp.data.ModelVariant
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.JetBrainsMonoFamily
import com.gaetan.localllmapp.ui.theme.ManropeFamily

/** CTA principal dégradé violet, réutilisé sur tous les écrans onboarding/hub. */
@Composable
fun GemmaPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (enabled) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(GemmaColors.AccentPurple, GemmaColors.AccentPurpleMid),
                        ),
                    )
                } else {
                    Modifier.background(GemmaColors.SurfaceInput)
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let {
            Icon(it, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp,
            color = if (enabled) Color.White else GemmaColors.TextFaint,
        )
        trailingIcon?.let {
            Spacer(Modifier.width(8.dp))
            Icon(it, null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
    }
}

/** Logo Gemma flottant avec halo radial (écrans 01/03/05). */
@Composable
fun GemmaFloatingLogo(size: Int = 52) {
    val transition = rememberInfiniteTransition(label = "float")
    val dy by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "dy",
    )
    Box(
        modifier = Modifier.offset(y = dy.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size((size * 2).dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x4D6E78FF), Color.Transparent),
                            center = center,
                            radius = this.size.minDimension / 2f,
                        ),
                    )
                },
        )
        GemmaLogo(size = size)
    }
}

/** Pastille de verdict de compatibilité (barres + libellé). */
@Composable
fun CompatibilityBadge(label: String, tint: Color = GemmaColors.SuccessBright) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GemmaColors.Success.copy(alpha = 0.12f))
            .border(1.dp, GemmaColors.Success.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(start = 11.dp, end = 15.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(7, 11, 15).forEach { h ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(GemmaColors.Success),
                )
            }
        }
        Text(
            text = label,
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = tint,
        )
    }
}

/** Ligne du scan de compatibilité : icône + titre + détail + check (écran 01). */
@Composable
fun ScanRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    detail: String,
    passed: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GemmaColors.SurfaceCard)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = GemmaColors.TextSecondary,
            )
            Text(
                text = detail,
                fontFamily = ManropeFamily,
                fontSize = 11.5.sp,
                color = GemmaColors.TextFaint,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Icon(
            Icons.Default.Check,
            null,
            tint = if (passed) GemmaColors.Success else GemmaColors.TextFaint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Carte modèle de l'écran 02. */
@Composable
fun ModelCard(
    variant: ModelVariant,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF1A1B27), Color(0xFF141420))),
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) GemmaColors.AccentPurpleLight.copy(alpha = 0.55f)
                else GemmaColors.AccentPurpleLight.copy(alpha = 0.22f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(enabled = variant.available, onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GemmaLogo(size = 38)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = variant.label,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = GemmaColors.TextPrimary,
                )
                Text(
                    text = variant.description,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = GemmaColors.AccentPurpleSoft,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (variant.recommended) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(GemmaColors.Success.copy(alpha = 0.13f))
                        .border(1.dp, GemmaColors.Success.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(Icons.Default.Star, null, tint = GemmaColors.SuccessBright, modifier = Modifier.size(11.dp))
                    Text(
                        "Recommandé",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = GemmaColors.SuccessBright,
                    )
                }
            } else if (!variant.available) {
                Text(
                    "Bientôt",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = GemmaColors.StarGold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GemmaColors.StarGold.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Tag(variant.sizeLabel)
            variant.tags.take(2).forEach { Tag(it) }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text = text,
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        color = GemmaColors.TextIcon,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x0FFFFFFF))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

/** Ligne avec libellé + interrupteur (toggle), écran 02. */
@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    leadingIcon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(GemmaColors.SurfaceCard)
            .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(15.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leadingIcon?.let {
            Icon(it, null, tint = GemmaColors.AccentPurpleSoft, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = GemmaColors.TextSecondary,
            )
            Text(
                text = subtitle,
                fontFamily = ManropeFamily,
                fontSize = 11.5.sp,
                color = GemmaColors.TextFaint,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Switch(checked)
    }
}

@Composable
private fun Switch(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) GemmaColors.AccentPurple.copy(alpha = 0.9f) else Color(0xFF2A2C36))
            .border(
                1.dp,
                if (checked) GemmaColors.AccentPurple.copy(alpha = 0.5f) else GemmaColors.BorderLight,
                RoundedCornerShape(999.dp),
            ),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else GemmaColors.TextStatus),
        )
    }
}

/** Barre de stockage à deux segments (écran 02). */
@Composable
fun StorageBar(
    usedFraction: Float,
    modelFraction: Float,
    leftLabel: String,
    rightLabel: String,
    footnote: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                leftLabel,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = GemmaColors.TextMuted,
            )
            Text(
                rightLabel,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.5.sp,
                color = GemmaColors.TextStats,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF23252F)),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(usedFraction.coerceIn(0.01f, 1f))
                        .height(7.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(GemmaColors.AccentPurple, GemmaColors.AccentPurpleMid),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .weight(modelFraction.coerceIn(0.01f, 1f))
                        .height(7.dp)
                        .background(GemmaColors.AccentPurple.copy(alpha = 0.3f)),
                )
                Box(modifier = Modifier.weight((1f - usedFraction - modelFraction).coerceIn(0.01f, 1f)))
            }
        }
        Text(
            footnote,
            fontFamily = ManropeFamily,
            fontSize = 10.5.sp,
            color = GemmaColors.TextStatus,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Rail d'étapes "Téléchargement → En mémoire" (écran 03). */
@Composable
fun StepRail(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StepChip(index = 1, label = "Téléchargement", active = currentStep >= 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(GemmaColors.AccentPurple, GemmaColors.AccentPurple.copy(alpha = 0.2f)),
                    ),
                ),
        )
        StepChip(index = 2, label = "En mémoire", active = currentStep >= 2)
    }
}

@Composable
private fun StepChip(index: Int, label: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (active) GemmaColors.AccentPurple else Color(0xFF23252F)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$index",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                color = if (active) Color.White else GemmaColors.TextFaint,
            )
        }
        Text(
            label,
            fontFamily = ManropeFamily,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 11.5.sp,
            color = if (active) GemmaColors.AccentPurpleLight else GemmaColors.TextStatus,
        )
    }
}
