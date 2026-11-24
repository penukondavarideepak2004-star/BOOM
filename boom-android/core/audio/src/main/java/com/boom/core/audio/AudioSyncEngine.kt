package com.boom.core.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class SyncQuality {
    EXCELLENT, // < 25ms error
    GOOD,      // 25ms - 60ms error
    DEGRADED,  // 60ms - 120ms error
    OUT_OF_SYNC// > 120ms error
}

data class SyncDiagnostics(
    val currentDriftMs: Long = 0L,
    val clockOffsetMs: Long = 0L,
    val playbackSpeedMultiplier: Float = 1.0f,
    val quality: SyncQuality = SyncQuality.EXCELLENT,
    val isActivelyPlaying: Boolean = false
)

class AudioSyncEngine(
    private val clockSyncFilter: ClockSyncFilter = ClockSyncFilter()
) {
    private val _diagnostics = MutableStateFlow(SyncDiagnostics())
    val diagnostics: StateFlow<SyncDiagnostics> = _diagnostics.asStateFlow()

    private var targetMasterPlayTimeMs: Long? = null
    private var localPlaybackStartTimeMs: Long? = null

    /**
     * Schedules synchronized playback. Returns milliseconds until playback starts on local clock.
     */
    fun schedulePlayAtMasterTime(masterTimestampMs: Long, currentLocalTimeMs: Long): Long {
        targetMasterPlayTimeMs = masterTimestampMs
        val localTargetTimeMs = clockSyncFilter.hostToLocalTimestampMs(masterTimestampMs)
        localPlaybackStartTimeMs = localTargetTimeMs

        val delayMs = localTargetTimeMs - currentLocalTimeMs
        return if (delayMs > 0) delayMs else 0L
    }

    /**
     * Evaluates current playback drift against host timeline.
     * @param expectedHostPositionMs Host reported current track position
     * @param actualLocalPositionMs Client actual current track position
     * @return recommended playback speed adjustment (e.g. 0.995 to 1.005)
     */
    fun evaluateDriftAndGetSpeedAdjustment(
        expectedHostPositionMs: Long,
        actualLocalPositionMs: Long
    ): Float {
        val driftMs = expectedHostPositionMs - actualLocalPositionMs
        val absDrift = abs(driftMs)

        val quality = when {
            absDrift <= 25 -> SyncQuality.EXCELLENT
            absDrift <= 60 -> SyncQuality.GOOD
            absDrift <= 120 -> SyncQuality.DEGRADED
            else -> SyncQuality.OUT_OF_SYNC
        }

        // Smooth drift correction without audible pitch shift
        val speed = when {
            absDrift <= 20 -> 1.0f // In tolerance
            driftMs > 20 && driftMs <= 120 -> 1.005f // Slightly accelerate to catch up
            driftMs < -20 && driftMs >= -120 -> 0.995f // Slightly decelerate to let host catch up
            else -> 1.0f // Beyond 120ms, caller should execute a hard seek
        }

        _diagnostics.value = SyncDiagnostics(
            currentDriftMs = driftMs,
            clockOffsetMs = clockSyncFilter.getEstimatedOffsetMs(),
            playbackSpeedMultiplier = speed,
            quality = quality,
            isActivelyPlaying = true
        )

        return speed
    }

    fun getClockFilter(): ClockSyncFilter = clockSyncFilter
}
