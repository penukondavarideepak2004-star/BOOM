package com.boom.core.security

import com.boom.core.model.SecurityEvent
import com.boom.core.model.SecurityEventType
import com.boom.core.model.SecuritySeverity
import java.util.UUID

object PrivacyFilter {
    // Regex patterns for sensitive credentials that must NEVER appear in logs or diagnostics
    private val SENSITIVE_PATTERNS = listOf(
        "(?i)password[\\s:=]+[^\\s,;]+".toRegex(),
        "(?i)otp[\\s:=]+[0-9]{4,8}".toRegex(),
        "(?i)bearer[\\s]+[A-Za-z0-9-_=.]+".toRegex(),
        "(?i)token[\\s:=]+[A-Za-z0-9-_=.]+".toRegex(),
        "(?i)secret[\\s:=]+[^\\s,;]+".toRegex(),
        "(?i)key[\\s:=]+[^\\s,;]+".toRegex(),
        "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}".toRegex() // Raw email redaction
    )

    /**
     * Sanitizes any message or details string before it can be added to an event or log.
     */
    fun sanitize(input: String?): String? {
        if (input == null) return null
        var sanitized = input
        for (pattern in SENSITIVE_PATTERNS) {
            sanitized = pattern.replace(sanitized, "[REDACTED_BY_GUARDIAN]")
        }
        return sanitized
    }

    /**
     * Verifies whether an event is privacy-compliant before persistence or transmission.
     */
    fun isCompliant(event: SecurityEvent): Boolean {
        val details = event.details ?: return true
        for (pattern in SENSITIVE_PATTERNS) {
            if (pattern.containsMatchIn(details)) return false
        }
        return true
    }
}

class BoomGuardian {
    private val eventHistory = mutableListOf<SecurityEvent>()

    fun recordEvent(
        eventType: SecurityEventType,
        severity: SecuritySeverity,
        actorReference: String,
        partyId: String? = null,
        result: String,
        actionTaken: String,
        rawDetails: String? = null
    ): SecurityEvent {
        val sanitizedDetails = PrivacyFilter.sanitize(rawDetails)
        val event = SecurityEvent(
            eventId = "evt-" + UUID.randomUUID().toString().take(12),
            timestampEpochMs = System.currentTimeMillis(),
            eventType = eventType,
            severity = severity,
            actorReference = actorReference,
            partyId = partyId,
            result = result,
            actionTaken = actionTaken,
            details = sanitizedDetails
        )
        synchronized(eventHistory) {
            if (eventHistory.size >= 500) {
                eventHistory.removeAt(0) // Bound event queue in memory
            }
            eventHistory.add(event)
        }
        return event
    }

    fun recordEvent(event: SecurityEvent): SecurityEvent {
        val sanitizedDetails = PrivacyFilter.sanitize(event.details)
        val sanitizedEvent = event.copy(details = sanitizedDetails)
        synchronized(eventHistory) {
            if (eventHistory.size >= 500) {
                eventHistory.removeAt(0)
            }
            eventHistory.add(sanitizedEvent)
        }
        return sanitizedEvent
    }

    fun getRecentEvents(partyId: String? = null): List<SecurityEvent> {
        synchronized(eventHistory) {
            return if (partyId == null) {
                eventHistory.toList()
            } else {
                eventHistory.filter { it.partyId == partyId }
            }
        }
    }

    fun computePartySecurityScore(partyId: String): Int {
        val events = getRecentEvents(partyId)
        val criticalCount = events.count { it.severity == SecuritySeverity.CRITICAL }
        val highCount = events.count { it.severity == SecuritySeverity.HIGH }
        val mediumCount = events.count { it.severity == SecuritySeverity.MEDIUM }

        val deduction = (criticalCount * 30) + (highCount * 15) + (mediumCount * 5)
        return (100 - deduction).coerceIn(0, 100)
    }

    fun clearEvents() {
        synchronized(eventHistory) {
            eventHistory.clear()
        }
    }
}
