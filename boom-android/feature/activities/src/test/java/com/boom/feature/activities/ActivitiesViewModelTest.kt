package com.boom.feature.activities

import com.boom.core.audio.AudioSyncEngine
import com.boom.core.connectivity.LocalLoopbackTransport
import com.boom.core.database.InMemorySessionStorage
import com.boom.core.model.AccountStatus
import com.boom.core.model.ActivityState
import com.boom.core.model.ActivityType
import com.boom.core.model.AudioSyncPacket
import com.boom.core.model.AuthSession
import com.boom.core.model.ChatMessage
import com.boom.core.model.Party
import com.boom.core.model.PlaybackStatus
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
class ActivitiesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiClient: MockActivitiesApiClient
    private lateinit var hostSessionStorage: InMemorySessionStorage
    private lateinit var guestSessionStorage: InMemorySessionStorage
    private lateinit var transport: LocalLoopbackTransport
    private lateinit var audioSyncEngine: AudioSyncEngine

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApiClient = MockActivitiesApiClient()
        transport = LocalLoopbackTransport()
        audioSyncEngine = AudioSyncEngine()

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

        guestSessionStorage = InMemorySessionStorage().apply {
            saveSession(
                AuthSession(
                    sessionId = "sess-guest",
                    userId = "usr-guest",
                    token = "token-guest",
                    user = User(
                        userId = "usr-guest",
                        email = "guest@boom.app",
                        phoneNumber = "+1000000001",
                        displayName = "Guest Jane",
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
    fun `ACT-01 Host switches activity to Vibe Vote`() = runTest {
        val hostVm = ActivitiesViewModel(
            apiClient = mockApiClient,
            sessionStorage = hostSessionStorage,
            nearbyTransport = transport,
            audioSyncEngine = audioSyncEngine,
            partyId = "pty-100",
            isHost = true
        )

        assertEquals(ActivityType.MUSIC_PLAYER, hostVm.uiState.value.activeType)

        hostVm.switchActivity(ActivityType.VIBE_VOTE)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ActivityType.VIBE_VOTE, hostVm.uiState.value.activeType)
    }

    @Test
    fun `ACT-01 Participant cannot switch activity`() = runTest {
        val guestVm = ActivitiesViewModel(
            apiClient = mockApiClient,
            sessionStorage = guestSessionStorage,
            nearbyTransport = transport,
            audioSyncEngine = audioSyncEngine,
            partyId = "pty-100",
            isHost = false
        )

        guestVm.switchActivity(ActivityType.PARTY_GAME)
        testDispatcher.scheduler.advanceUntilIdle()

        // Remains on MUSIC_PLAYER
        assertEquals(ActivityType.MUSIC_PLAYER, guestVm.uiState.value.activeType)
    }

    @Test
    fun `FR-04 Host starts playback schedules master timestamp and triggers broadcast`() = runTest {
        val hostVm = ActivitiesViewModel(
            apiClient = mockApiClient,
            sessionStorage = hostSessionStorage,
            nearbyTransport = transport,
            audioSyncEngine = audioSyncEngine,
            partyId = "pty-100",
            isHost = true
        )

        hostVm.togglePlayPause()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PlaybackStatus.PLAYING, hostVm.uiState.value.musicState.playbackStatus)
        assertNotNull(hostVm.uiState.value.musicState.scheduledPlayAtMasterEpochMs)
        assertTrue(hostVm.uiState.value.musicState.scheduledPlayAtMasterEpochMs!! > System.currentTimeMillis())
    }

    @Test
    fun `ACT-02 & FR-10 Vibe voting updates option tally correctly`() {
        val guestVm = ActivitiesViewModel(
            apiClient = mockApiClient,
            sessionStorage = guestSessionStorage,
            nearbyTransport = transport,
            audioSyncEngine = audioSyncEngine,
            partyId = "pty-100",
            isHost = false
        )

        val initialOption = guestVm.uiState.value.vibeState.options[0]
        val initialVotes = initialOption.votes

        guestVm.castVote(initialOption.id)

        assertEquals(initialOption.id, guestVm.uiState.value.userSelectedVoteOptionId)
        val updatedOption = guestVm.uiState.value.vibeState.options.first { it.id == initialOption.id }
        assertEquals(initialVotes + 1, updatedOption.votes)

        // Switching vote changes counts accordingly
        val secondOption = guestVm.uiState.value.vibeState.options[1]
        val secondInitialVotes = secondOption.votes
        guestVm.castVote(secondOption.id)

        val option1AfterSwitch = guestVm.uiState.value.vibeState.options.first { it.id == initialOption.id }
        val option2AfterSwitch = guestVm.uiState.value.vibeState.options.first { it.id == secondOption.id }
        assertEquals(initialVotes, option1AfterSwitch.votes)
        assertEquals(secondInitialVotes + 1, option2AfterSwitch.votes)
    }

    @Test
    fun `FR-06 Local volume changes do not impact host or other settings`() {
        val guestVm = ActivitiesViewModel(
            apiClient = mockApiClient,
            sessionStorage = guestSessionStorage,
            nearbyTransport = transport,
            audioSyncEngine = audioSyncEngine,
            partyId = "pty-100",
            isHost = false
        )

        guestVm.setLocalVolume(0.35f)
        assertEquals(0.35f, guestVm.uiState.value.localVolume, 0.001f)
        assertFalse(guestVm.uiState.value.isMutedLocally)

        guestVm.toggleLocalMute()
        assertTrue(guestVm.uiState.value.isMutedLocally)
    }
}

class MockActivitiesApiClient : BoomApiClient {
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
    override suspend fun sendMessage(request: SendChatMessageRequest): ApiResponse<ChatMessage> = ApiResponse(success = false)

    override suspend fun getActivityState(partyId: String): ApiResponse<ActivityState> {
        return ApiResponse(
            success = true,
            data = ActivityState(
                activityId = "act-mock",
                partyId = partyId,
                activeType = ActivityType.MUSIC_PLAYER
            )
        )
    }

    override suspend fun updateActivityState(partyId: String, state: ActivityState): ApiResponse<Boolean> {
        return ApiResponse(success = true, data = true)
    }

    override suspend fun reportSecurityEvent(event: SecurityEvent): ApiResponse<Boolean> = ApiResponse(success = true)
}
