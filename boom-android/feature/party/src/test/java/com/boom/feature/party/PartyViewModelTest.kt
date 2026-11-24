package com.boom.feature.party

import com.boom.core.audio.AudioSyncEngine
import com.boom.core.connectivity.LocalLoopbackTransport
import com.boom.core.connectivity.TransportState
import com.boom.core.database.InMemorySessionStorage
import com.boom.core.model.AccountStatus
import com.boom.core.model.ActivityState
import com.boom.core.model.AuthSession
import com.boom.core.model.ChatMessage
import com.boom.core.model.Participant
import com.boom.core.model.ParticipantStatus
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
import kotlinx.coroutines.flow.first
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
class PartyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiClient: MockPartyApiClient
    private lateinit var mockSessionStorage: InMemorySessionStorage
    private lateinit var transport: LocalLoopbackTransport
    private lateinit var audioSyncEngine: AudioSyncEngine
    private lateinit var guardian: BoomGuardian
    private lateinit var viewModel: PartyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApiClient = MockPartyApiClient()
        mockSessionStorage = InMemorySessionStorage()
        transport = LocalLoopbackTransport()
        audioSyncEngine = AudioSyncEngine()
        guardian = BoomGuardian()

        viewModel = PartyViewModel(
            apiClient = mockApiClient,
            sessionStorage = mockSessionStorage,
            nearbyTransport = transport,
            audioSyncEngine = audioSyncEngine,
            guardian = guardian
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `HOST-02 Create party starts nearby advertising and navigates to HostControlPanel`() = runTest {
        viewModel.updatePartyTitle("Friday Mega Boom")
        viewModel.createParty()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.activeParty)
        assertEquals("Friday Mega Boom", viewModel.uiState.value.activeParty?.title)
        assertTrue(viewModel.uiState.value.isHost)
        assertEquals(TransportState.ADVERTISING, transport.transportState.value)
    }

    @Test
    fun `HOST-03 & HOST-04 Host approves join request moving participant to connected roster`() = runTest {
        viewModel.createParty()
        testDispatcher.scheduler.advanceUntilIdle()

        val guest = Participant(
            participantId = "guest-1",
            partyId = "pty-test",
            userId = "usr-guest-1",
            displayName = "Charlie",
            status = ParticipantStatus.PENDING_APPROVAL
        )

        viewModel.simulateIncomingJoinRequest(guest)
        assertEquals(1, viewModel.uiState.value.pendingApprovals.size)

        viewModel.approveParticipant("guest-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.pendingApprovals.size)
        assertEquals(1, viewModel.uiState.value.connectedParticipants.size)
        assertEquals("Charlie", viewModel.uiState.value.connectedParticipants[0].displayName)
        assertEquals(ParticipantStatus.CONNECTED, viewModel.uiState.value.connectedParticipants[0].status)
    }

    @Test
    fun `HOST-05 Host removes participant and disconnects endpoint`() = runTest {
        viewModel.createParty()
        testDispatcher.scheduler.advanceUntilIdle()

        val guest = Participant(
            participantId = "guest-bad",
            partyId = "pty-test",
            userId = "usr-guest-bad",
            displayName = "Bad Actor",
            status = ParticipantStatus.CONNECTED
        )
        viewModel.simulateIncomingJoinRequest(guest)
        viewModel.approveParticipant("guest-bad")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.connectedParticipants.size)

        viewModel.removeParticipant("guest-bad")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.connectedParticipants.size)
    }

    @Test
    fun `FR-06 Local volume adjustment affects only local endpoint`() {
        viewModel.setLocalVolume(0.40f)
        assertEquals(0.40f, viewModel.uiState.value.localVolume, 0.001f)
        assertFalse(viewModel.uiState.value.isMutedLocally)

        viewModel.toggleLocalMute()
        assertTrue(viewModel.uiState.value.isMutedLocally)
    }

    @Test
    fun `JOIN-04 & FR-06 Participant can leave party immediately without host approval`() = runTest {
        viewModel.joinParty("BOOM99")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.leaveParty()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = viewModel.navigationEvents.first()
        assertEquals(PartyNavigationEvent.NavigateToDashboard, event)
        assertNull(viewModel.uiState.value.activeParty)
    }
}

class MockPartyApiClient : BoomApiClient {
    override suspend fun register(request: RegisterRequest): ApiResponse<User> = ApiResponse(success = true)
    override suspend fun verifyContact(request: VerifyContactRequest): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun login(request: LoginRequest): ApiResponse<AuthSession> = ApiResponse(success = true)
    override suspend fun verifyMfa(request: MfaVerifyRequest): ApiResponse<AuthSession> = ApiResponse(success = true)
    override suspend fun logout(sessionId: String): ApiResponse<Boolean> = ApiResponse(success = true)

    override suspend fun createParty(request: CreatePartyRequest): ApiResponse<Party> {
        return ApiResponse(
            success = true,
            data = Party(
                partyId = "pty-test-101",
                hostUserId = "usr-host",
                hostDisplayName = "DJ Sam",
                title = request.title,
                shortCode = "BOOM77",
                qrPayload = "boom://party?id=pty-test-101&code=BOOM77"
            )
        )
    }

    override suspend fun joinParty(request: JoinPartyRequest): ApiResponse<Party> {
        return ApiResponse(
            success = true,
            data = Party(
                partyId = "pty-joined-202",
                hostUserId = "usr-other-host",
                hostDisplayName = "DJ Apex",
                title = "Joined Party",
                shortCode = request.shortCode,
                qrPayload = "boom://party"
            )
        )
    }

    override suspend fun approveParticipant(partyId: String, participantId: String): ApiResponse<Boolean> = ApiResponse(success = true, data = true)
    override suspend fun removeParticipant(partyId: String, participantId: String): ApiResponse<Boolean> = ApiResponse(success = true, data = true)
    override suspend fun leaveParty(partyId: String): ApiResponse<Boolean> = ApiResponse(success = true, data = true)

    override suspend fun getMessages(partyId: String): ApiResponse<List<ChatMessage>> = ApiResponse(success = true, data = emptyList())
    override suspend fun sendMessage(request: SendChatMessageRequest): ApiResponse<ChatMessage> = ApiResponse(success = false)
    override suspend fun getActivityState(partyId: String): ApiResponse<ActivityState> = ApiResponse(success = false)
    override suspend fun updateActivityState(partyId: String, state: ActivityState): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun reportSecurityEvent(event: SecurityEvent): ApiResponse<Boolean> = ApiResponse(success = true)
}
