package com.boom.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccountStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    LOCKED
}

@Serializable
enum class UserRole {
    GUEST,
    VERIFIED_USER,
    HOST,
    ADMIN
}

@Serializable
data class User(
    val userId: String,
    val email: String,
    val emailVerified: Boolean = false,
    val phoneNumber: String,
    val phoneVerified: Boolean = false,
    val displayName: String,
    val avatarUrl: String? = null,
    val accountStatus: AccountStatus = AccountStatus.PENDING_VERIFICATION,
    val role: UserRole = UserRole.VERIFIED_USER,
    val mfaEnabled: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
