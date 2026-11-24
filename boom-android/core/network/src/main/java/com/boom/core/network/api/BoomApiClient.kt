package com.boom.core.network.api

import com.boom.core.model.ActivityState
import com.boom.core.model.AuthSession
import com.boom.core.model.ChatMessage
import com.boom.core.model.Party
import com.boom.core.model.SecurityEvent
import com.boom.core.model.User
import com.boom.core.network.model.ApiResponse
import com.boom.core.network.model.CreatePartyRequest
import com.boom.core.network.model.JoinPartyRequest
import com.boom.core.network.model.LoginRequest
import com.boom.core.network.model.MfaVerifyRequest
import com.boom.core.network.model.RegisterRequest
import com.boom.core.network.model.SendChatMessageRequest
import com.boom.core.network.model.VerifyContactRequest

interface BoomApiClient {
    // Auth
    suspend fun register(request: RegisterRequest): ApiResponse<User>
    suspend fun verifyContact(request: VerifyContactRequest): ApiResponse<Boolean>
    suspend fun login(request: LoginRequest): ApiResponse<AuthSession>
    suspend fun verifyMfa(request: MfaVerifyRequest): ApiResponse<AuthSession>
    suspend fun logout(sessionId: String): ApiResponse<Boolean>

    // Party
    suspend fun createParty(request: CreatePartyRequest): ApiResponse<Party>
    suspend fun joinParty(request: JoinPartyRequest): ApiResponse<Party>
    suspend fun approveParticipant(partyId: String, participantId: String): ApiResponse<Boolean>
    suspend fun removeParticipant(partyId: String, participantId: String): ApiResponse<Boolean>
    suspend fun leaveParty(partyId: String): ApiResponse<Boolean>

    // Chat
    suspend fun getMessages(partyId: String): ApiResponse<List<ChatMessage>>
    suspend fun sendMessage(request: SendChatMessageRequest): ApiResponse<ChatMessage>

    // Activities
    suspend fun getActivityState(partyId: String): ApiResponse<ActivityState>
    suspend fun updateActivityState(partyId: String, state: ActivityState): ApiResponse<Boolean>

    // Guardian
    suspend fun reportSecurityEvent(event: SecurityEvent): ApiResponse<Boolean>
}
