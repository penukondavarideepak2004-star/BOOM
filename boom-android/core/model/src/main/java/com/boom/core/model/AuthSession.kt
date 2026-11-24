package com.boom.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val sessionId: String,
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val deviceReference: String,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val riskLevel: String = "LOW",
    val requiresMfa: Boolean = false
)

@Serializable
data class MfaChallenge(
    val challengeId: String,
    val userId: String,
    val allowedFactors: List<String>,
    val expiresAtEpochMs: Long
)
