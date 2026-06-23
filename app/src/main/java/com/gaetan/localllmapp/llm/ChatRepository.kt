package com.gaetan.localllmapp.llm

import android.content.Context
import android.net.Uri
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class OutgoingMessage(
    val text: String,
    val imageUri: Uri? = null,
    val audioUri: Uri? = null,
    val audioFilePath: String? = null,
)

class ChatRepository(
    private val context: Context,
    private val llmEngine: LlmEngine,
) {
    suspend fun sendMessage(message: OutgoingMessage): Flow<String> {
        val conversation = llmEngine.getConversation()
            ?: error("Le moteur n'est pas initialisé.")

        val contents = buildContents(message)
        return conversation.sendMessageAsync(Contents.of(contents))
            .map { chunk -> chunk.extractText() }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun buildContents(message: OutgoingMessage): List<Content> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<Content>()
            val prompt = message.text.ifBlank {
                when {
                    message.imageUri != null && hasAudio(message) ->
                        "Décris cette image et cet audio."
                    message.imageUri != null -> "Décris cette image."
                    hasAudio(message) -> "Transcris et résume cet audio."
                    else -> "Bonjour"
                }
            }
            items += Content.Text(prompt)

            message.imageUri?.let { uri ->
                val bytes = readUriBytes(uri)
                items += Content.ImageBytes(bytes)
            }

            when {
                message.audioFilePath != null -> {
                    items += Content.AudioFile(message.audioFilePath)
                }
                message.audioUri != null -> {
                    val bytes = readUriBytes(message.audioUri)
                    items += Content.AudioBytes(bytes)
                }
            }

            items
        }

    private fun hasAudio(message: OutgoingMessage): Boolean {
        return message.audioUri != null || message.audioFilePath != null
    }

    private fun Message.extractText(): String {
        return contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }
    }

    private fun readUriBytes(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Impossible de lire le fichier sélectionné.")
    }
}