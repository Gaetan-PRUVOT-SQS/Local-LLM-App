package com.gaetan.gemmchat.data

import kotlinx.serialization.Serializable

@Serializable
data class StoredMessage(
    val role: String,
    val text: String,
    val imageUri: String? = null,
    val audioLabel: String? = null,
    val createdAt: Long = 0L,
)

@Serializable
data class StoredConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<StoredMessage> = emptyList(),
)
