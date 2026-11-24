package com.boom.feature.chat

import com.boom.core.database.InMemorySessionStorage
import com.boom.core.model.AccountStatus
import com.boom.core.model.ActivityState
import com.boom.core.model.AuthSession
import com.boom.core.model.ChatMessage
import com.boom.core.model.DeliveryState
import com.boom.core.model.MessageVisibility
import com.boom.core.model.Participant
import com.boom.core.model.Party
import com.boom.core.model.SecurityEvent
import com.boom.core.model.User
import com.boom.core.network.api.BoomApiClient
import com.boom.core.network.model.ApiResponse
import com.boom.core.network.model.CreatePartyRequest
import com.boom.core.network.model.JoinPartyRequest
import com.boom.core.network.model.LoginRequest
import com.boom.core.network.model.MfaVerifyRequest
import com.boom.core.network.model.RegisterRequest
import com.boom.core.network.model.SendChatMessageRequest
import com.boom.core.network.model.VerifyContactRequest
import com.boom.core.security.BoomGuardian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiClient: MockChatApiClient
    private lateinit var hostSessionStorage: InMemorySessionStorage
    private lateinit var aliceSessionStorage: InMemorySessionStorage
    private lateinit var bobSessionStorage: InMemorySessionStorage
    private lateinit var guardian: BoomGuardian

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApiClient = MockChatApiClient()
        guardian = BoomGuardian()

        hostSessionStorage = InMemorySessionStorage().apply {
            saveSession(
                AuthSession(
                    sessionId = "sess-host",
                    userId = "usr-host",
                    token = "token-host",
                    user = User(
                        userId = "usr-host",
                        email = "host@boom.app",
                        phoneNumber = "+1000000000",
                        displayName = "Host Sam",
                        accountStatus = AccountStatus.ACTIVE
                    )
                )
            )
        }

        aliceSessionStorage = InMemorySessionStorage().apply {
            saveSession(
                AuthSession(
                    sessionId = "sess-alice",
                    userId = "usr-alice",
                    token = "token-alice",
                    user = User(
                        userId = "usr-alice",
                        email = "alice@boom.app",
                        phoneNumber = "+1000000001",
                        displayName = "Alice Guest",
                        accountStatus = AccountStatus.ACTIVE
                    )
                )
            )
        }

        bobSessionStorage = InMemorySessionStorage().apply {
            saveSession(
                AuthSession(
                    sessionId = "sess-bob",
                    userId = "usr-bob",
                    token = "token-bob",
                    user = User(
                        userId = "usr-bob",
                        email = "bob@boom.app",
                        phoneNumber = "+1000000002",
                        displayName = "Bob Guest",
                        accountStatus = AccountStatus.ACTIVE
                    )
                )
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `CHAT-01 Public message sent is delivered and visible to everyone`() = runTest {
        val aliceVm = ChatViewModel(
            apiClient = mockApiClient,
            sessionStorage = aliceSessionStorage,
            guardian = guardian,
            partyId = "pty-100",
            isHost = false,
            hostUserId = "usr-host"
        )

        aliceVm.updateInputText("Hello everyone! Great music!")
        aliceVm.setSelectedVisibility(MessageVisibility.EVERYONE)
        aliceVm.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, aliceVm.uiState.value.messages.size)
        val sentMsg = aliceVm.uiState.value.messages[0]
        assertEquals("Hello everyone! Great music!", sentMsg.content)
        assertEquals(DeliveryState.DELIVERED, sentMsg.deliveryState)
        assertEquals(MessageVisibility.EVERYONE, sentMsg.visibility)
    }

    @Test
    fun `CHAT-03 and CHAT-04 Participant private message to Host is hidden from other participants`() = runTest {
        val aliceVm = ChatViewModel(
            apiClient = mockApiClient,
            sessionStorage = aliceSessionStorage,
            guardian = guardian,
            partyId = "pty-100",
            isHost = false,
            hostUserId = "usr-host"
        )

        val bobVm = ChatViewModel(
            apiClient = mockApiClient,
            sessionStorage = bobSessionStorage,
            guardian = guardian,
            partyId = "pty-100",
            isHost = false,
            hostUserId = "usr-host"
        )

        val hostVm = ChatViewModel(
            apiClient = mockApiClient,
            sessionStorage = hostSessionStorage,
            guardian = guardian,
            partyId = "pty-100",
            isHost = true,
            hostUserId = "usr-host"
        )

        // Alice sends private message to host
        aliceVm.updateInputText("Can you please turn down the bass a bit?")
        aliceVm.setSelectedVisibility(MessageVisibility.HOST_ONLY)
        aliceVm.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        val alicePrivateMessage = aliceVm.uiState.value.messages[0]
        assertEquals(MessageVisibility.HOST_ONLY, alicePrivateMessage.visibility)

        // Simulate incoming message to Host
        hostVm.receiveIncomingMessage(alicePrivateMessage)
        assertEquals(1, hostVm.uiState.value.filteredMessages.size)
        assertEquals("Can you please turn down the bass a bit?", hostVm.uiState.value.filteredMessages[0].content)

        // Simulate message arrival attempt at Bob's device (defense-in-depth)
        bobVm.receiveIncomingMessage(alicePrivateMessage)
        // Bob should drop the message, NOT adding it to messages
        assertEquals(0, bobVm.uiState.value.filteredMessages.size)
    }

    @Test
    fun `CHAT-05 Host can reply privately to participant`() = runTest {
        val hostVm = ChatViewModel(
            apiClient = mockApiClient,
            sessionStorage = hostSessionStorage,
            guardian = guardian,
            partyId = "pty-100",
            isHost = true,
            hostUserId = "usr-host"
        )

        val aliceMsg = ChatMessage(
            messageId = "msg-alice-priv",
            partyId = "pty-100",
            senderUserId = "usr-alice",
            senderDisplayName = "Alice Guest",
            isFromHost = false,
            visibility = MessageVisibility.HOST_ONLY,
            content = "Is the volume good for you?"
        )
        hostVm.receiveIncomingMessage(aliceMsg)

        hostVm.startReplyingTo(aliceMsg)
        assertEquals(MessageVisibility.HOST_ONLY, hostVm.uiState.value.selectedVisibility)
        assertEquals(aliceMsg, hostVm.uiState.value.replyingToMessage)

        hostVm.updateInputText("Adjusted the EQ! Enjoy!")
        hostVm.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        val hostReply = hostVm.uiState.value.messages.last()
        assertEquals(MessageVisibility.HOST_ONLY, hostReply.visibility)
        assertEquals("usr-alice", hostReply.recipientUserId)
    }

    @Test
    fun `CHAT-06 Host can post public announcements`() = runTest {
        val hostVm = ChatViewModel(
            apiClient = mockApiClient,
            sessionStorage = hostSessionStorage,
            guardian = guardian,
            partyId = "pty-100",
            isHost = true,
            hostUserId = "usr-host"
        )

        hostVm.setSelectedVisibility(MessageVisibility.HOST_ANNOUNCEMENT)
        hostVm.updateInputText("Pizza has arrived in the main room!")
        hostVm.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        val announcement = hostVm.uiState.value.messages[0]
        assertEquals(MessageVisibility.HOST_ANNOUNCEMENT, announcement.visibility)
        assertTrue(announcement.isFromHost)
    }

    @Test
    fun `CHAT-08 Filter chips correctly isolate channel views`() = runTest {
        val hostVm = ChatViewModel(
            apiClient = mockApiClient,
            sessionStorage = hostSessionStorage,
            guardian = guardian,
            partyId = "pty-100",
            isHost = true,
            hostUserId = "usr-host"
        )

        val publicMsg = ChatMessage(
            messageId = "m1",
            partyId = "pty-100",
            senderUserId = "usr-bob",
            senderDisplayName = "Bob",
            isFromHost = false,
            visibility = MessageVisibility.EVERYONE,
            content = "Great vibe!"
        )
        val privateMsg = ChatMessage(
            messageId = "m2",
            partyId = "pty-100",
            senderUserId = "usr-alice",
            senderDisplayName = "Alice",
            isFromHost = false,
            visibility = MessageVisibility.HOST_ONLY,
            content = "Private question"
        )
        val announceMsg = ChatMessage(
            messageId = "m3",
            partyId = "pty-100",
            senderUserId = "usr-host",
            senderDisplayName = "Host",
            isFromHost = true,
            visibility = MessageVisibility.HOST_ANNOUNCEMENT,
            content = "Announcement"
        )

        hostVm.receiveIncomingMessage(publicMsg)
        hostVm.receiveIncomingMessage(privateMsg)
        hostVm.receiveIncomingMessage(announceMsg)

        // ALL filter: all 3 visible to host
        hostVm.setFilter(ChatFilter.ALL)
        assertEquals(3, hostVm.uiState.value.filteredMessages.size)

        // HOST_ONLY filter: only private message
        hostVm.setFilter(ChatFilter.HOST_ONLY)
        assertEquals(1, hostVm.uiState.value.filteredMessages.size)
        assertEquals("Private question", hostVm.uiState.value.filteredMessages[0].content)

        // ANNOUNCEMENTS filter: only announcement
        hostVm.setFilter(ChatFilter.ANNOUNCEMENTS)
        assertEquals(1, hostVm.uiState.value.filteredMessages.size)
        assertEquals("Announcement", hostVm.uiState.value.filteredMessages[0].content)
    }
}

class MockChatApiClient : BoomApiClient {
    override suspend fun register(request: RegisterRequest): ApiResponse<User> = ApiResponse(success = true)
    override suspend fun verifyContact(request: VerifyContactRequest): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun login(request: LoginRequest): ApiResponse<AuthSession> = ApiResponse(success = true)
    override suspend fun verifyMfa(request: MfaVerifyRequest): ApiResponse<AuthSession> = ApiResponse(success = true)
    override suspend fun logout(sessionId: String): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun createParty(request: CreatePartyRequest): ApiResponse<Party> = ApiResponse(success = false)
    override suspend fun joinParty(request: JoinPartyRequest): ApiResponse<Party> = ApiResponse(success = false)
    override suspend fun approveParticipant(partyId: String, participantId: String): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun removeParticipant(partyId: String, participantId: String): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun leaveParty(partyId: String): ApiResponse<Boolean> = ApiResponse(success = true)

    override suspend fun getMessages(partyId: String): ApiResponse<List<ChatMessage>> = ApiResponse(success = true, data = emptyList())

    override suspend fun sendMessage(request: SendChatMessageRequest): ApiResponse<ChatMessage> {
        val vis = MessageVisibility.valueOf(request.visibility)
        return ApiResponse(
            success = true,
            data = ChatMessage(
                messageId = "msg-server-${System.currentTimeMillis()}",
                partyId = request.partyId,
                senderUserId = "usr-sender",
                senderDisplayName = "Sender",
                isFromHost = vis == MessageVisibility.HOST_ANNOUNCEMENT,
                visibility = vis,
                recipientUserId = request.recipientUserId,
                content = request.content,
                deliveryState = DeliveryState.DELIVERED
            )
        )
    }

    override suspend fun getActivityState(partyId: String): ApiResponse<ActivityState> = ApiResponse(success = false)
    override suspend fun updateActivityState(partyId: String, state: ActivityState): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun reportSecurityEvent(event: SecurityEvent): ApiResponse<Boolean> = ApiResponse(success = true)
}
