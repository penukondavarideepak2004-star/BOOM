package com.boom.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ParticipantStatus {
    PENDING_APPROVAL,
    CONNECTED,
    RECONNECTING,
    DELAYED,
    MUTED,
    DISCONNECTED,
    REMOVED
}

@Serializable
data class AudioOutputRoute(
    val routeName: String = "Built-in Speaker",
    val isBluetooth: Boolean = false,
    val reportedOutputLatencyMs: Long = 0L
)

@Serializable
data class Participant(
    val participantId: String,
    val partyId: String,
    val userId: String,
    val displayName: String,
    val isHost: Boolean = false,
    val status: ParticipantStatus = ParticipantStatus.PENDING_APPROVAL,
    val localVolume: Float = 1.0f,
    val isMutedLocally: Boolean = false,
    val audioRoute: AudioOutputRoute = AudioOutputRoute(),
    val clockOffsetMs: Long = 0L,
    val roundTripTimeMs: Long = 0L,
    val joinedAtEpochMs: Long = System.currentTimeMillis(),
    val lastSeenEpochMs: Long = System.currentTimeMillis()
)
