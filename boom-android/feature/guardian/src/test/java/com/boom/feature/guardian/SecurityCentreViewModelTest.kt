package com.boom.feature.guardian

import com.boom.core.model.SecurityEvent
import com.boom.core.model.SecurityEventType
import com.boom.core.model.SecuritySeverity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityCentreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var guardian: BoomGuardian
    private lateinit var viewModel: SecurityCentreViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        guardian = BoomGuardian()
        viewModel = SecurityCentreViewModel(guardian = guardian, partyId = "pty-sec-test")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SEC-01 Baseline security score is 100 percent when all defenses active`() {
        guardian.recordEvent(
            eventType = SecurityEventType.AUTH_SUCCESS,
            severity = SecuritySeverity.INFO,
            actorReference = "usr-test",
            partyId = "pty-sec-test",
            result = "SUCCESS",
            actionTaken = "SESSION_GRANTED"
        )
        viewModel.refreshSecurityState()

        assertEquals(100, viewModel.uiState.value.overallScore)
        assertEquals("ALL DEFENSES ACTIVE", viewModel.uiState.value.protectionStatus)
    }

    @Test
    fun `SEC-01 High and critical security events reduce security score proportionately`() {
        guardian.recordEvent(
            eventType = SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT,
            severity = SecuritySeverity.HIGH, // -15
            actorReference = "usr-bad-actor",
            partyId = "pty-sec-test",
            result = "BLOCKED",
            actionTaken = "DROPPED_PACKET"
        )
        guardian.recordEvent(
            eventType = SecurityEventType.REPLAY_DETECTED,
            severity = SecuritySeverity.CRITICAL, // -30
            actorReference = "usr-attacker",
            partyId = "pty-sec-test",
            result = "REJECTED",
            actionTaken = "DISCONNECTED"
        )
        viewModel.refreshSecurityState()

        // 100 - (15 + 30) = 55
        assertEquals(55, viewModel.uiState.value.overallScore)
        assertEquals("THREATS MITIGATED", viewModel.uiState.value.protectionStatus)
    }

    @Test
    fun `SEC-02 PrivacyFilter guarantees credentials never appear in audit events`() {
        guardian.recordEvent(
            eventType = SecurityEventType.AUTH_FAILURE,
            severity = SecuritySeverity.MEDIUM,
            actorReference = "usr-test",
            partyId = "pty-sec-test",
            result = "REJECTED",
            actionTaken = "THROTTLED",
            rawDetails = "Failed login with password=SuperSecret123 and otp: 987654 for user@example.com"
        )
        viewModel.refreshSecurityState()

        val event = viewModel.uiState.value.recentEvents.first()
        val details = event.details ?: ""

        assertFalse(details.contains("SuperSecret123"))
        assertFalse(details.contains("987654"))
        assertFalse(details.contains("user@example.com"))
        assertTrue(details.contains("[REDACTED_BY_GUARDIAN]"))
    }

    @Test
    fun `SEC-03 All defense layers report active status with standard specifications`() {
        val defenses = viewModel.uiState.value.activeDefenses
        assertEquals(4, defenses.size)
        assertTrue(defenses.all { it.isActive })
        assertTrue(defenses.any { it.title.contains("Hardware Keystore") })
        assertTrue(defenses.any { it.title.contains("Transport Layer Security") })
        assertTrue(defenses.any { it.title.contains("Object-Level Chat Privacy") })
        assertTrue(defenses.any { it.title.contains("Abuse Throttling") })
    }

    @Test
    fun `SEC-04 Non-intrusive privacy assurances are declared`() {
        val assurances = viewModel.uiState.value.privacyAssurances
        assertTrue(assurances.isNotEmpty())
        assertTrue(assurances.any { it.contains("Zero Device File Inspection") })
        assertTrue(assurances.any { it.contains("Zero Background Surveillance") })
    }
}
