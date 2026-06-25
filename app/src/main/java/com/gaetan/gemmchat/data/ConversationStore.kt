package com.gaetan.gemmchat.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persistance simple des conversations : un fichier JSON par conversation dans
 * filesDir/conversations/<id>.json. Pas de base de données — le volume reste petit.
 */
class ConversationStore(context: Context) {
    private val dir: File =
        File(context.filesDir, "conversations").also { it.mkdirs() }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Résumés de toutes les conversations, triés du plus récent au plus ancien.
     * Ne lit que les métadonnées nécessaires à la liste (pas les messages) pour
     * limiter le coût quand le nombre de conversations grandit.
     */
    suspend fun listSummaries(): List<StoredConversation> = withContext(Dispatchers.IO) {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?: return@withContext emptyList()
        files.mapNotNull { file ->
            runCatching {
                val full = json.decodeFromString<StoredConversation>(file.readText())
                // on n'a besoin que de id/titre/updatedAt pour la liste
                full.copy(messages = emptyList())
            }
                .onFailure { Log.w(TAG, "Conversation illisible: ${file.name}", it) }
                .getOrNull()
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun load(id: String): StoredConversation? = withContext(Dispatchers.IO) {
        val file = fileFor(id)
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString<StoredConversation>(file.readText()) }
            .onFailure { Log.w(TAG, "Échec de lecture conversation $id", it) }
            .getOrNull()
    }

    suspend fun save(conversation: StoredConversation) = withContext(Dispatchers.IO) {
        runCatching { fileFor(conversation.id).writeText(json.encodeToString(conversation)) }
            .onFailure { Log.w(TAG, "Échec de sauvegarde conversation ${conversation.id}", it) }
        Unit
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        fileFor(id).delete()
        Unit
    }

    private fun fileFor(id: String): File = File(dir, "$id.json")

    companion object {
        private const val TAG = "ConversationStore"
    }
}
