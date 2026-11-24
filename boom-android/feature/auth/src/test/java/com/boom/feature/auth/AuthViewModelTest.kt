package com.boom.feature.auth

import com.boom.core.database.InMemorySessionStorage
import com.boom.core.model.AccountStatus
import com.boom.core.model.ActivityState
import com.boom.core.model.AuthSession
import com.boom.core.model.ChatMessage
import com.boom.core.model.Party
import com.boom.core.model.SecurityEvent
import com.boom.core.model.User
import com.boom.core.network.api.BoomApiClient
import com.boom.core.network.model.ApiError
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiClient: MockBoomApiClient
    private lateinit var mockSessionStorage: InMemorySessionStorage
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApiClient = MockBoomApiClient()
        mockSessionStorage = InMemorySessionStorage()
        viewModel = AuthViewModel(mockApiClient, mockSessionStorage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `registration rejects invalid email`() = runTest {
        viewModel.updateEmail("invalid-email")
        viewModel.updatePhone("+15551234567")
        viewModel.updateDisplayName("Alex")
        viewModel.updatePassword("ValidPass123!")

        viewModel.register()

        assertEquals("Please enter a valid email address", viewModel.uiState.value.registrationError)
    }

    @Test
    fun `registration rejects password shorter than 8 characters`() = runTest {
        viewModel.updateEmail("alex@boom.party")
        viewModel.updatePhone("+15551234567")
        viewModel.updateDisplayName("Alex")
        viewModel.updatePassword("short")

        viewModel.register()

        assertEquals("Password must be at least 8 characters long", viewModel.uiState.value.registrationError)
    }

    @Test
    fun `successful login stores token and navigates to welcome`() = runTest {
        viewModel.updateLoginIdentifier("alex@boom.party")
        viewModel.updateLoginPassword("SuperSecret123!")

        mockApiClient.mockLoginSession = AuthSession(
            sessionId = "sess-1",
            userId = "usr-1",
            accessToken = "jwt.valid.token",
            refreshToken = "jwt.refresh.token",
            deviceReference = "Pixel 8",
            issuedAtEpochMs = System.currentTimeMillis(),
            expiresAtEpochMs = System.currentTimeMillis() + 900000
        )

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("jwt.valid.token", mockSessionStorage.getAccessToken())
        assertNull(viewModel.uiState.value.loginError)
    }

    @Test
    fun `failed login surfaces generic error without account enumeration`() = runTest {
        viewModel.updateLoginIdentifier("stranger@unknown.com")
        viewModel.updateLoginPassword("BadPassword!")

        mockApiClient.mockLoginError = ApiError(code = "INVALID_CREDENTIALS", message = "Invalid credentials")

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Invalid credentials", viewModel.uiState.value.loginError)
        assertNull(mockSessionStorage.getAccessToken())
    }
}

class MockBoomApiClient : BoomApiClient {
    var mockLoginSession: AuthSession? = null
    var mockLoginError: ApiError? = null

    override suspend fun register(request: RegisterRequest): ApiResponse<User> {
        return ApiResponse(
            success = true,
            data = User(
                userId = "usr-new",
                email = request.email,
                phoneNumber = request.phoneNumber,
                displayName = request.displayName,
                accountStatus = AccountStatus.PENDING_VERIFICATION
            )
        )
    }

    override suspend fun verifyContact(request: VerifyContactRequest): ApiResponse<Boolean> {
        return ApiResponse(success = true, data = true)
    }

    override suspend fun login(request: LoginRequest): ApiResponse<AuthSession> {
        return if (mockLoginSession != null) {
            ApiResponse(success = true, data = mockLoginSession)
        } else {
            ApiResponse(success = false, error = mockLoginError ?: ApiError("INVALID_CREDENTIALS", "Invalid credentials"))
        }
    }

    override suspend fun verifyMfa(request: MfaVerifyRequest): ApiResponse<AuthSession> = ApiResponse(success = true)
    override suspend fun logout(sessionId: String): ApiResponse<Boolean> = ApiResponse(success = true, data = true)
    override suspend fun createParty(request: CreatePartyRequest): ApiResponse<Party> = ApiResponse(success = false)
    override suspend fun joinParty(request: JoinPartyRequest): ApiResponse<Party> = ApiResponse(success = false)
    override suspend fun approveParticipant(partyId: String, participantId: String): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun removeParticipant(partyId: String, participantId: String): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun leaveParty(partyId: String): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun getMessages(partyId: String): ApiResponse<List<ChatMessage>> = ApiResponse(success = true, data = emptyList())
    override suspend fun sendMessage(request: SendChatMessageRequest): ApiResponse<ChatMessage> = ApiResponse(success = false)
    override suspend fun getActivityState(partyId: String): ApiResponse<ActivityState> = ApiResponse(success = false)
    override suspend fun updateActivityState(partyId: String, state: ActivityState): ApiResponse<Boolean> = ApiResponse(success = true)
    override suspend fun reportSecurityEvent(event: SecurityEvent): ApiResponse<Boolean> = ApiResponse(success = true, data = true)
}
