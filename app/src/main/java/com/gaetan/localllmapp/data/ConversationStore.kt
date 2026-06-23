package com.gaetan.localllmapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Résumé d'une discussion affiché dans le Hub (écran 04). */
data class ConversationSummary(
    val id: String,
    val title: String,
    val messageCount: Int,
    val updatedAtMs: Long,
    val kind: ConversationKind,
)

enum class ConversationKind { CHAT, IMAGE, AUDIO }

/** Message persisté (sérialisable) d'une discussion. */
data class StoredMessage(
    val role: String, // "USER" | "ASSISTANT"
    val text: String,
    val imageUri: String? = null,
    val audioLabel: String? = null,
)

private data class StoredConversation(
    val id: String,
    val title: String,
    val updatedAtMs: Long,
    val kind: ConversationKind,
    val messages: List<StoredMessage>,
)

/**
 * Persistance des discussions (JSON dans filesDir) — **contenu complet** des messages,
 * pour pouvoir rouvrir une discussion archivée et non en créer une vide.
 */
class ConversationStore(context: Context) {
    private val file = File(context.filesDir, "conversations.json")

    fun recent(limit: Int = 10): List<ConversationSummary> =
        load().sortedByDescending { it.updatedAtMs }.take(limit).map { it.toSummary() }

    fun all(): List<ConversationSummary> =
        load().sortedByDescending { it.updatedAtMs }.map { it.toSummary() }

    /** Messages d'une discussion donnée (vide si inconnue). */
    fun messages(id: String): List<StoredMessage> =
        load().firstOrNull { it.id == id }?.messages ?: emptyList()

    /** Crée ou met à jour une discussion avec son contenu complet. */
    fun save(id: String, title: String, kind: ConversationKind, updatedAtMs: Long, messages: List<StoredMessage>) {
        val others = load().filterNot { it.id == id }
        val conv = StoredConversation(id, title.ifBlank { "Discussion" }, updatedAtMs, kind, messages)
        persist(others + conv)
    }

    fun delete(id: String) {
        persist(load().filterNot { it.id == id })
    }

    fun clear() {
        if (file.exists()) file.delete()
    }

    private fun StoredConversation.toSummary() =
        ConversationSummary(id, title, messages.size, updatedAtMs, kind)

    private fun load(): List<StoredConversation> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val msgsArray = o.optJSONArray("messages") ?: JSONArray()
                    val messages = buildList {
                        for (j in 0 until msgsArray.length()) {
                            val m = msgsArray.getJSONObject(j)
                            add(
                                StoredMessage(
                                    role = m.optString("role", "USER"),
                                    text = m.optString("text", ""),
                                    imageUri = m.optString("imageUri", "").ifBlank { null },
                                    audioLabel = m.optString("audioLabel", "").ifBlank { null },
                                ),
                            )
                        }
                    }
                    add(
                        StoredConversation(
                            id = o.getString("id"),
                            title = o.optString("title", "Discussion"),
                            updatedAtMs = o.optLong("updatedAtMs", 0L),
                            kind = runCatching { ConversationKind.valueOf(o.optString("kind", "CHAT")) }
                                .getOrDefault(ConversationKind.CHAT),
                            messages = messages,
                        ),
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun persist(items: List<StoredConversation>) {
        val array = JSONArray()
        items.forEach { c ->
            val msgs = JSONArray()
            c.messages.forEach { m ->
                msgs.put(
                    JSONObject()
                        .put("role", m.role)
                        .put("text", m.text)
                        .put("imageUri", m.imageUri ?: "")
                        .put("audioLabel", m.audioLabel ?: ""),
                )
            }
            array.put(
                JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("updatedAtMs", c.updatedAtMs)
                    .put("kind", c.kind.name)
                    .put("messages", msgs),
            )
        }
        file.writeText(array.toString())
    }
}
