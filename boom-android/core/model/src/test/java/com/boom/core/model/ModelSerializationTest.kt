package com.boom.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSerializationTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun testUserSerialization() {
        val user = User(
            userId = "usr-123",
            email = "test@boom.party",
            phoneNumber = "+1234567890",
            displayName = "Alex Sound",
            accountStatus = AccountStatus.ACTIVE,
            role = UserRole.HOST
        )
        val serialized = json.encodeToString(user)
        val deserialized = json.decodeFromString<User>(serialized)

        assertEquals("usr-123", deserialized.userId)
        assertEquals("test@boom.party", deserialized.email)
        assertEquals(UserRole.HOST, deserialized.role)
        assertEquals(AccountStatus.ACTIVE, deserialized.accountStatus)
    }

    @Test
    fun testChatMessageVisibilityRules() {
        val publicMsg = ChatMessage(
            messageId = "msg-pub-1",
            partyId = "party-001",
            senderUserId = "usr-1",
            senderDisplayName = "DJ Sam",
            isFromHost = true,
            visibility = MessageVisibility.EVERYONE,
            content = "Welcome to the party!"
        )

        val privateMsg = ChatMessage(
            messageId = "msg-priv-1",
            partyId = "party-001",
            senderUserId = "usr-2",
            senderDisplayName = "Guest Charlie",
            isFromHost = false,
            visibility = MessageVisibility.HOST_ONLY,
            content = "Can you play track 4 next?"
        )

        assertEquals(MessageVisibility.EVERYONE, publicMsg.visibility)
        assertEquals(MessageVisibility.HOST_ONLY, privateMsg.visibility)
        assertFalse(privateMsg.isFromHost)
        assertTrue(publicMsg.isFromHost)
    }

    @Test
    fun testSecurityEventModel() {
        val event = SecurityEvent(
            eventId = "sec-999",
            eventType = SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT,
            severity = SecuritySeverity.HIGH,
            actorReference = "anon-device-hash-442",
            result = "BLOCKED",
            actionTaken = "TOKEN_REVOKED"
        )
        val serialized = json.encodeToString(event)
        val deserialized = json.decodeFromString<SecurityEvent>(serialized)

        assertEquals(SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT, deserialized.eventType)
        assertEquals(SecuritySeverity.HIGH, deserialized.severity)
        assertEquals("BLOCKED", deserialized.result)
    }
}
