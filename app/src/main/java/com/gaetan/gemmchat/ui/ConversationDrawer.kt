package com.gaetan.gemmchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaetan.gemmchat.ui.components.GemmaLogo
import com.gaetan.gemmchat.ui.theme.GemmaColors
import com.gaetan.gemmchat.ui.theme.ManropeFamily

@Composable
fun ConversationDrawerContent(
    conversations: List<ConversationSummary>,
    currentConversationId: String?,
    onOpenConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
) {
    var renameTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.85f)
            .widthIn(max = 360.dp)
            .background(GemmaColors.Background)
            .safeDrawingPadding()
            .padding(horizontal = 12.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GemmaLogo(size = 22)
            Text(
                text = "Conversations",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GemmaColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GemmaColors.SurfaceCard)
                .border(1.dp, GemmaColors.BorderSubtle, RoundedCornerShape(14.dp))
                .clickable(onClick = onNewConversation)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = GemmaColors.AccentPurpleSoft,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Nouvelle conversation",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = GemmaColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Aucune conversation enregistrée",
                    fontFamily = ManropeFamily,
                    fontSize = 13.sp,
                    color = GemmaColors.TextDim,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        selected = conversation.id == currentConversationId,
                        onClick = { onOpenConversation(conversation.id) },
                        onRename = { renameTarget = conversation },
                        onDelete = { deleteTarget = conversation },
                    )
                }
            }
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            initialTitle = target.title,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle ->
                onRenameConversation(target.id, newTitle)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConversation(target.id)
                    deleteTarget = null
                }) {
                    Text("Supprimer", color = GemmaColors.AccentPurpleMid)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Annuler", color = GemmaColors.TextMuted)
                }
            },
            title = {
                Text(
                    text = "Supprimer la conversation ?",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    color = GemmaColors.TextPrimary,
                )
            },
            text = {
                Text(
                    text = "« ${target.title} » sera définitivement supprimée.",
                    fontFamily = ManropeFamily,
                    color = GemmaColors.TextMuted,
                )
            },
            containerColor = GemmaColors.SurfaceCard,
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) GemmaColors.SurfacePill else Color.Transparent)
            .then(
                if (selected) {
                    Modifier.border(1.dp, GemmaColors.BorderLight, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = if (selected) GemmaColors.AccentPurpleSoft else GemmaColors.IconMuted,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = conversation.title,
            fontFamily = ManropeFamily,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.5.sp,
            color = if (selected) GemmaColors.TextPrimary else GemmaColors.TextSecondary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRename, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Renommer la conversation",
                tint = GemmaColors.IconMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Supprimer la conversation",
                tint = GemmaColors.IconMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RenameDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Renommer", color = GemmaColors.AccentPurpleSoft)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = GemmaColors.TextMuted)
            }
        },
        title = {
            Text(
                text = "Renommer la conversation",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                color = GemmaColors.TextPrimary,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        containerColor = GemmaColors.SurfaceCard,
    )
}
