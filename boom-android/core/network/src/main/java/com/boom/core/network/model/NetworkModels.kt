package com.boom.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: String? = null
)

@Serializable
data class RegisterRequest(
    val email: String,
    val phoneNumber: String,
    val displayName: String,
    val passwordHash: String // Pre-hashed or TLS-transmitted credential
)

@Serializable
data class VerifyContactRequest(
    val contactType: String, // "EMAIL" or "PHONE"
    val identifier: String,
    val code: String
)

@Serializable
data class LoginRequest(
    val identifier: String, // email or phone
    val password: String,
    val deviceReference: String
)

@Serializable
data class MfaVerifyRequest(
    val challengeId: String,
    val factorType: String, // "TOTP" or "SMS_OTP"
    val code: String
)

@Serializable
data class CreatePartyRequest(
    val title: String,
    val maxParticipants: Int = 10,
    val requireHostApproval: Boolean = true
)

@Serializable
data class JoinPartyRequest(
    val shortCode: String,
    val userDisplayName: String,
    val reportedOutputRoute: String = "Built-in Speaker"
)

@Serializable
data class SendChatMessageRequest(
    val partyId: String,
    val content: String,
    val visibility: String, // "EVERYONE" or "HOST_ONLY"
    val recipientUserId: String? = null
)
