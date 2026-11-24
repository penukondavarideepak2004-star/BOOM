package com.boom.core.audio

import kotlin.math.abs

data class ClockOffsetEstimate(
    val offsetNs: Long,
    val roundTripTimeNs: Long,
    val sampleCount: Int
)

class ClockSyncFilter(
    private val alpha: Float = 0.2f, // Smoothing factor for exponential moving average
    private val maxAcceptedRttNs: Long = 100_000_000L // 100ms max acceptable RTT for clock sync packet
) {
    private var smoothedOffsetNs: Long? = null
    private var sampleCount: Int = 0

    /**
     * Ingest a 4-timestamp NTP packet exchange.
     * t0: Client send timestamp
     * t1: Host receive timestamp
     * t2: Host transmit timestamp
     * t3: Client receive timestamp
     *
     * Returns true if sample was valid and accepted.
     */
    fun ingestSample(t0Ns: Long, t1Ns: Long, t2Ns: Long, t3Ns: Long): Boolean {
        val rtt = (t3Ns - t0Ns) - (t2Ns - t1Ns)
        if (rtt < 0 || rtt > maxAcceptedRttNs) {
            return false // Outlier or invalid measurement
        }

        val rawOffset = ((t1Ns - t0Ns) + (t2Ns - t3Ns)) / 2

        smoothedOffsetNs = when (val prev = smoothedOffsetNs) {
            null -> rawOffset
            else -> (alpha * rawOffset + (1.0f - alpha) * prev).toLong()
        }
        sampleCount++
        return true
    }

    fun getEstimatedOffsetNs(): Long = smoothedOffsetNs ?: 0L

    fun getEstimatedOffsetMs(): Long = getEstimatedOffsetNs() / 1_000_000L

    fun getSampleCount(): Int = sampleCount

    fun reset() {
        smoothedOffsetNs = null
        sampleCount = 0
    }

    /**
     * Converts a Host master timestamp to local device timestamp.
     */
    fun hostToLocalTimestampMs(hostTimestampMs: Long): Long {
        return hostTimestampMs - getEstimatedOffsetMs()
    }
}
