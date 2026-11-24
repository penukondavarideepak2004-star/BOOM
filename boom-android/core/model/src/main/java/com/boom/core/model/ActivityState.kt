package com.boom.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ActivityType {
    MUSIC_PLAYER,
    VIBE_VOTE,
    PARTY_GAME,
    NONE
}

@Serializable
enum class PlaybackStatus {
    STOPPED,
    BUFFERING,
    PLAYING,
    PAUSED
}

@Serializable
data class AudioTrackInfo(
    val trackId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val sourceUri: String,
    val coverArtUrl: String? = null
)

@Serializable
data class MusicActivityState(
    val currentTrack: AudioTrackInfo? = null,
    val playbackStatus: PlaybackStatus = PlaybackStatus.STOPPED,
    val currentPositionMs: Long = 0L,
    val scheduledPlayAtMasterEpochMs: Long? = null,
    val playlist: List<AudioTrackInfo> = emptyList(),
    val queueIndex: Int = 0
)

@Serializable
data class VibeVoteOption(
    val id: String,
    val name: String,
    val votes: Int = 0
)

@Serializable
data class VibeActivityState(
    val activeQuestion: String,
    val options: List<VibeVoteOption> = emptyList(),
    val roundExpiresAtEpochMs: Long = 0L
)

@Serializable
data class ActivityState(
    val activityId: String,
    val partyId: String,
    val activeType: ActivityType = ActivityType.MUSIC_PLAYER,
    val musicState: MusicActivityState = MusicActivityState(),
    val vibeState: VibeActivityState? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
