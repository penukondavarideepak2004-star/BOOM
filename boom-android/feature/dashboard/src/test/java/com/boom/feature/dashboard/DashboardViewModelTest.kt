package com.boom.feature.dashboard

import com.boom.core.database.InMemorySessionStorage
import com.boom.core.model.AuthSession
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSessionStorage: InMemorySessionStorage
    private lateinit var mockGuardian: BoomGuardian
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockSessionStorage = InMemorySessionStorage()
        mockGuardian = BoomGuardian()

        // Seed with active authenticated session
        mockSessionStorage.saveSession(
            AuthSession(
                sessionId = "sess-101",
                userId = "usr-101",
                accessToken = "valid.jwt.token",
                refreshToken = "valid.refresh.token",
                deviceReference = "Pixel 8",
                issuedAtEpochMs = System.currentTimeMillis(),
                expiresAtEpochMs = System.currentTimeMillis() + 900000
            )
        )

        viewModel = DashboardViewModel(mockSessionStorage, mockGuardian)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dashboard initializes with user profile and 100% security score`() {
        val state = viewModel.uiState.value
        assertEquals("usr-101", state.userId)
        assertEquals(100, state.securityScore)
        assertEquals("Protected", state.securityStatusLabel)
    }

    @Test
    fun `host party action emits NavigateToHostParty event`() = runTest {
        viewModel.onHostPartyClicked()
        val event = viewModel.navigationEvents.first()
        assertEquals(DashboardNavigationEvent.NavigateToHostParty, event)
    }

    @Test
    fun `join party action emits NavigateToJoinParty event`() = runTest {
        viewModel.onJoinPartyClicked()
        val event = viewModel.navigationEvents.first()
        assertEquals(DashboardNavigationEvent.NavigateToJoinParty, event)
    }

    @Test
    fun `logout clears session and emits NavigateToSignIn`() = runTest {
        viewModel.logout()
        val event = viewModel.navigationEvents.first()
        assertEquals(DashboardNavigationEvent.NavigateToSignIn, event)
        assertNull(mockSessionStorage.getSession())
    }
}
