package com.gaetan.localllmapp.ui.history

import android.text.format.DateUtils
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.localllmapp.data.ConversationKind
import com.gaetan.localllmapp.data.ConversationSummary
import com.gaetan.localllmapp.ui.components.GemmaScreenBackground
import com.gaetan.localllmapp.ui.theme.GemmaColors
import com.gaetan.localllmapp.ui.theme.ManropeFamily

@Composable
fun HistoryScreen(
    conversations: List<ConversationSummary>,
    nowMs: Long,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
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
                Text("Historique", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GemmaColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                if (conversations.isNotEmpty()) {
                    Text(
                        "Effacer tout",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = Color(0xFFFF8585),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClearAll)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }

            if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.History, null, tint = GemmaColors.TextStatus, modifier = Modifier.size(44.dp))
                    Text(
                        "Aucune discussion enregistrée",
                        fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = GemmaColors.TextMuted, modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(conversations, key = { it.id }) { c ->
                        HistoryRow(c, nowMs, onOpen = { onOpen(c.id) }, onDelete = { onDelete(c.id) })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: ConversationSummary, nowMs: Long, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(GemmaColors.SurfaceCard)
            .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(13.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val (icon, tint, bg) = when (item.kind) {
            ConversationKind.IMAGE -> Triple(Icons.Default.Image, GemmaColors.CameraPurpleSoft, Color(0x24A78BFA))
            ConversationKind.AUDIO -> Triple(Icons.Default.GraphicEq, GemmaColors.SuccessBright, Color(0x2234D39A))
            ConversationKind.CHAT -> Triple(Icons.AutoMirrored.Filled.Chat, GemmaColors.AccentPurpleSoft, Color(0x216E78FF))
        }
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = GemmaColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${item.messageCount} messages · ${relative(item.updatedAtMs, nowMs)}",
                fontFamily = ManropeFamily, fontSize = 10.5.sp, color = GemmaColors.TextStatus, modifier = Modifier.padding(top = 1.dp),
            )
        }
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, "Supprimer", tint = GemmaColors.TextStatus, modifier = Modifier.size(16.dp))
        }
    }
}

private fun relative(ms: Long, nowMs: Long): String =
    DateUtils.getRelativeTimeSpanString(ms, nowMs, DateUtils.MINUTE_IN_MILLIS).toString()
