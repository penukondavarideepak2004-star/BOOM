package com.boom.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageVisibility {
    EVERYONE,
    HOST_ONLY,
    HOST_ANNOUNCEMENT
}

@Serializable
enum class DeliveryState {
    PENDING,
    SENT,
    DELIVERED,
    FAILED
}

@Serializable
data class ChatMessage(
    val messageId: String,
    val partyId: String,
    val senderUserId: String,
    val senderDisplayName: String,
    val isFromHost: Boolean,
    val visibility: MessageVisibility = MessageVisibility.EVERYONE,
    val recipientUserId: String? = null, // Set when host privately replies or participant messages host
    val content: String,
    val deliveryState: DeliveryState = DeliveryState.SENT,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
