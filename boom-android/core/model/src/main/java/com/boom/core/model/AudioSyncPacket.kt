package com.boom.core.model

import kotlinx.serialization.Serializable

@Serializable
sealed class AudioSyncPacket {
    @Serializable
    data class TimeSyncRequest(
        val clientSendTimestampNs: Long
    ) : AudioSyncPacket()

    @Serializable
    data class TimeSyncResponse(
        val clientSendTimestampNs: Long,
        val hostReceiveTimestampNs: Long,
        val hostTransmitTimestampNs: Long
    ) : AudioSyncPacket()

    @Serializable
    data class SchedulePlayback(
        val trackId: String,
        val trackUri: String,
        val masterPlayAtEpochMs: Long,
        val startOffsetMs: Long = 0L
    ) : AudioSyncPacket()

    @Serializable
    data class PlaybackControl(
        val command: String, // "PLAY", "PAUSE", "SEEK", "STOP"
        val positionMs: Long,
        val effectiveMasterTimestampEpochMs: Long
    ) : AudioSyncPacket()

    @Serializable
    data class PlaybackHeadReport(
        val participantId: String,
        val currentPositionMs: Long,
        val measuredOffsetNs: Long,
        val bufferHealthMs: Long
    ) : AudioSyncPacket()
}
