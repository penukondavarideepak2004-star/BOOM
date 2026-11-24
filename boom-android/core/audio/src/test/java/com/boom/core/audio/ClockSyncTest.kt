package com.boom.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSyncTest {

    @Test
    fun testClockOffsetCalculation() {
        val filter = ClockSyncFilter(alpha = 1.0f) // No smoothing for deterministic test

        // Simulate client clock is 50ms behind host clock (host = client + 50ms)
        // Network delay in each direction: 10ms (10,000,000ns)
        val t0 = 100_000_000L // Client sends at 100ms
        val t1 = 160_000_000L // Host receives at 100 + 10 delay + 50 offset = 160ms
        val t2 = 165_000_000L // Host replies 5ms later at 165ms
        val t3 = 125_000_000L // Client receives at 100 + 10 + 5 + 10 = 125ms

        // RTT = (125 - 100) - (165 - 160) = 25 - 5 = 20ms
        // Offset = ((160 - 100) + (165 - 125)) / 2 = (60 + 40) / 2 = 50ms (50,000,000ns)
        val accepted = filter.ingestSample(t0, t1, t2, t3)

        assertTrue(accepted)
        assertEquals(50L, filter.getEstimatedOffsetMs())
        assertEquals(1, filter.getSampleCount())

        // Test host to local timestamp conversion
        val hostScheduledTimeMs = 1000L
        val expectedLocalTimeMs = 950L // 1000 - 50ms offset
        assertEquals(expectedLocalTimeMs, filter.hostToLocalTimestampMs(hostScheduledTimeMs))
    }

    @Test
    fun testAudioDriftCorrectionSpeed() {
        val engine = AudioSyncEngine()

        // 1. In sync (< 20ms drift) -> 1.0x speed
        val speedInSync = engine.evaluateDriftAndGetSpeedAdjustment(
            expectedHostPositionMs = 5010L,
            actualLocalPositionMs = 5000L
        )
        assertEquals(1.0f, speedInSync, 0.001f)
        assertEquals(SyncQuality.EXCELLENT, engine.diagnostics.value.quality)

        // 2. Client is lagging behind by 40ms -> speed up to 1.005x
        val speedLagging = engine.evaluateDriftAndGetSpeedAdjustment(
            expectedHostPositionMs = 5040L,
            actualLocalPositionMs = 5000L
        )
        assertEquals(1.005f, speedLagging, 0.001f)
        assertEquals(SyncQuality.GOOD, engine.diagnostics.value.quality)

        // 3. Client is ahead by 40ms -> slow down to 0.995x
        val speedAhead = engine.evaluateDriftAndGetSpeedAdjustment(
            expectedHostPositionMs = 5000L,
            actualLocalPositionMs = 5040L
        )
        assertEquals(0.995f, speedAhead, 0.001f)
        assertEquals(SyncQuality.GOOD, engine.diagnostics.value.quality)
    }
}
