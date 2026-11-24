package com.boom.core.security

import com.boom.core.model.SecurityEventType
import com.boom.core.model.SecuritySeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyFilterTest {

    @Test
    fun testSensitiveDataRedaction() {
        val rawInput = "Failed sign-in for user user@example.com with password: SuperSecretPassword123 and token: eyJhbGciOiJIUzI1NiJ9"
        val sanitized = PrivacyFilter.sanitize(rawInput)!!

        assertFalse(sanitized.contains("SuperSecretPassword123"))
        assertFalse(sanitized.contains("user@example.com"))
        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertTrue(sanitized.contains("[REDACTED_BY_GUARDIAN]"))
    }

    @Test
    fun testGuardianEventScoring() {
        val guardian = BoomGuardian()
        val partyId = "test-party-101"

        assertEquals(100, guardian.computePartySecurityScore(partyId))

        // Record a low severity info event -> Score stays 100
        guardian.recordEvent(
            eventType = SecurityEventType.AUTH_SUCCESS,
            severity = SecuritySeverity.INFO,
            actorReference = "usr-1",
            partyId = partyId,
            result = "SUCCESS",
            actionTaken = "SESSION_ISSUED"
        )
        assertEquals(100, guardian.computePartySecurityScore(partyId))

        // Record a high severity attack event -> Score deducts 15
        guardian.recordEvent(
            eventType = SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT,
            severity = SecuritySeverity.HIGH,
            actorReference = "attacker-ip",
            partyId = partyId,
            result = "BLOCKED",
            actionTaken = "RATE_LIMITED"
        )
        assertEquals(85, guardian.computePartySecurityScore(partyId))
    }
}
