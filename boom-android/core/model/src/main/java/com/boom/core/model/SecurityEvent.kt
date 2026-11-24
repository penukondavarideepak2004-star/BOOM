package com.boom.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SecuritySeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class SecurityEventType {
    AUTH_SUCCESS,
    AUTH_FAILURE,
    OTP_THROTTLED,
    INVALID_INVITATION,
    UNAUTHORIZED_ACCESS_ATTEMPT,
    REPLAY_DETECTED,
    DEVICE_DISCONNECTED,
    CLOCK_DRIFT_EXCEEDED,
    HIGH_RISK_NETWORK,
    PARTICIPANT_REMOVED
}

@Serializable
data class SecurityEvent(
    val eventId: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val eventType: SecurityEventType,
    val severity: SecuritySeverity,
    val actorReference: String, // Pseudonymous user or device ID (no PII)
    val partyId: String? = null,
    val result: String,
    val actionTaken: String,
    val details: String? = null
)
