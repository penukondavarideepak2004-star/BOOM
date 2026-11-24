package com.boom.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PartyState {
    INITIALIZING,
    DISCOVERABLE,
    ACTIVE,
    PAUSED,
    ENDED
}

@Serializable
data class PartySettings(
    val maxParticipants: Int = 10,
    val requireHostApproval: Boolean = true,
    val allowGuestRequests: Boolean = true,
    val isDiscoverable: Boolean = true,
    val chatEnabled: Boolean = true
)

@Serializable
data class Party(
    val partyId: String,
    val hostUserId: String,
    val hostDisplayName: String,
    val title: String,
    val shortCode: String,
    val qrPayload: String,
    val state: PartyState = PartyState.INITIALIZING,
    val settings: PartySettings = PartySettings(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val expiresAtEpochMs: Long = System.currentTimeMillis() + (4 * 3600 * 1000) // 4 hours lifetime
)

@Serializable
data class PartyInvitation(
    val invitationId: String,
    val partyId: String,
    val tokenHash: String,
    val shortCode: String,
    val expiresAtEpochMs: Long,
    val isRevoked: Boolean = false
)
