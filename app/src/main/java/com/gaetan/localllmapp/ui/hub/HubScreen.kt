package com.gaetan.localllmapp.ui.hub

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.localllmapp.data.ConversationKind
import com.gaetan.localllmapp.data.ConversationSummary
import com.gaetan.localllmapp.ui.components.GemmaLogo
import com.gaetan.localllmapp.ui.components.GemmaPrimaryButton
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.ManropeFamily

@Composable
fun HubScreen(
    recent: List<ConversationSummary>,
    onNewChat: () -> Unit,
    onCapability: (String) -> Unit,
    onAnalyse: () -> Unit,
    onTranscribe: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onShowAll: () -> Unit,
) {
    GemmaScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    GemmaLogo(size = 25)
                    Text("Gemma", fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = GemmaColors.TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(GemmaColors.Success.copy(alpha = 0.12f))
                            .border(1.dp, GemmaColors.Success.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Default.Lock, null, tint = GemmaColors.SuccessBright, modifier = Modifier.size(12.dp))
                        Text("Privé", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GemmaColors.SuccessBright)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Settings, "Paramètres", tint = GemmaColors.TextIcon, modifier = Modifier.size(21.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(10.dp))
                Text("Bonjour.", fontFamily = ManropeFamily, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, color = GemmaColors.TextPrimary)
                Text(
                    "Que veux‑tu faire en local aujourd'hui ?",
                    fontFamily = ManropeFamily,
                    fontSize = 13.5.sp,
                    color = GemmaColors.TextMuted,
                    modifier = Modifier.padding(top = 5.dp),
                )

                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    HubCard(
                        icon = Icons.AutoMirrored.Filled.Chat, iconTint = GemmaColors.AccentPurpleSoft, iconBg = Color(0x266E78FF),
                        title = "Discuter", subtitle = "Pose n'importe quelle question", badge = HubBadge.OFFLINE,
                        modifier = Modifier.weight(1f), onClick = onNewChat,
                    )
                    HubCard(
                        icon = Icons.Default.Image, iconTint = GemmaColors.CameraPurpleSoft, iconBg = Color(0x29A78BFA),
                        title = "Analyser", subtitle = "Décris ou explique une photo", badge = HubBadge.OFFLINE,
                        modifier = Modifier.weight(1f), onClick = onAnalyse,
                    )
                }
                Spacer(Modifier.height(11.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    HubCard(
                        icon = Icons.Default.GraphicEq, iconTint = GemmaColors.SuccessBright, iconBg = Color(0x2634D39A),
                        title = "Transcrire", subtitle = "Voix → texte en direct", badge = HubBadge.OFFLINE,
                        modifier = Modifier.weight(1f), onClick = onTranscribe,
                    )
                    HubCard(
                        icon = Icons.Default.Star, iconTint = GemmaColors.StarGold, iconBg = Color(0x29F2B45C),
                        title = "Agent & Skills", subtitle = "Styles de réponse prêts", badge = HubBadge.NEW,
                        modifier = Modifier.weight(1f), onClick = onOpenSkills,
                    )
                }

                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Discussions récentes", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = GemmaColors.TextMuted)
                    Text(
                        "Tout voir",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = GemmaColors.AccentPurple,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onShowAll)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (recent.isEmpty()) {
                    Text(
                        "Aucune discussion pour l'instant. Lance‑toi !",
                        fontFamily = ManropeFamily,
                        fontSize = 12.sp,
                        color = GemmaColors.TextStatus,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recent.forEach { RecentRow(it, onClick = { onOpenConversation(it.id) }) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)) {
                GemmaPrimaryButton(label = "Nouvelle discussion", onClick = onNewChat, leadingIcon = Icons.Default.Add)
            }
        }
    }
}

private enum class HubBadge { OFFLINE, NEW }

@Composable
private fun HubCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    badge: HubBadge,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GemmaColors.SurfaceCard)
            .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(15.dp),
    ) {
        when (badge) {
            HubBadge.OFFLINE -> Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(GemmaColors.Success))
                Text("Hors‑ligne", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = GemmaColors.SuccessBright)
            }
            HubBadge.NEW -> Text(
                "NOUVEAU",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.5.sp,
                color = GemmaColors.StarGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GemmaColors.StarGold.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GemmaColors.TextSecondary)
                Text(subtitle, fontFamily = ManropeFamily, fontSize = 11.5.sp, lineHeight = 15.sp, color = GemmaColors.TextFaint, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun RecentRow(item: ConversationSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF131319))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val (icon, tint, bg) = when (item.kind) {
            ConversationKind.IMAGE -> Triple(Icons.Default.Image, GemmaColors.CameraPurpleSoft, Color(0x24A78BFA))
            ConversationKind.AUDIO -> Triple(Icons.Default.GraphicEq, GemmaColors.SuccessBright, Color(0x2234D39A))
            ConversationKind.CHAT -> Triple(Icons.AutoMirrored.Filled.Chat, GemmaColors.AccentPurpleSoft, Color(0x216E78FF))
        }
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GemmaColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.messageCount} messages", fontFamily = ManropeFamily, fontSize = 10.5.sp, color = GemmaColors.TextStatus, modifier = Modifier.padding(top = 1.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = GemmaColors.TextStatus, modifier = Modifier.size(16.dp))
    }
}
