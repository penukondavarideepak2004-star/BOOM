package com.boom.feature.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.core.audio.AudioSyncEngine
import com.boom.core.audio.SyncDiagnostics
import com.boom.core.audio.SyncQuality
import com.boom.core.connectivity.ConnectionEvent
import com.boom.core.connectivity.NearbyTransport
import com.boom.core.database.SessionStorage
import com.boom.core.model.ActivityState
import com.boom.core.model.ActivityType
import com.boom.core.model.AudioSyncPacket
import com.boom.core.model.AudioTrackInfo
import com.boom.core.model.MusicActivityState
import com.boom.core.model.PlaybackStatus
import com.boom.core.model.VibeActivityState
import com.boom.core.model.VibeVoteOption
import com.boom.core.network.api.BoomApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ActivityNavigationEvent {
    object NavigateBack : ActivityNavigationEvent()
}

data class ActivityUiState(
    val partyId: String = "",
    val isHost: Boolean = false,
    val currentUserId: String = "",
    val activeType: ActivityType = ActivityType.MUSIC_PLAYER,
    val musicState: MusicActivityState = MusicActivityState(),
    val vibeState: VibeActivityState = VibeActivityState(
        activeQuestion = "What vibe should we play next?",
        options = listOf(
            VibeVoteOption("opt-1", "🔥 Electronic & Cyberpunk", 4),
            VibeVoteOption("opt-2", "💃 Deep House Grooves", 6),
            VibeVoteOption("opt-3", "🎸 Neon Synthwave", 3),
            VibeVoteOption("opt-4", "🎧 Chill Lo-Fi Lounge", 2)
        )
    ),
    val userSelectedVoteOptionId: String? = null,
    val liveReactions: List<String> = emptyList(),
    val syncDiagnostics: SyncDiagnostics = SyncDiagnostics(),
    val localVolume: Float = 0.80f,
    val isMutedLocally: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ActivitiesViewModel(
    private val apiClient: BoomApiClient,
    private val sessionStorage: SessionStorage,
    private val nearbyTransport: NearbyTransport,
    private val audioSyncEngine: AudioSyncEngine,
    partyId: String = "",
    isHost: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ActivityUiState(
            partyId = partyId,
            isHost = isHost,
            musicState = MusicActivityState(
                currentTrack = AudioTrackInfo(
                    trackId = "trk-1",
                    title = "Neon Resonance",
                    artist = "HyperPulse",
                    durationMs = 214000L, // 3m 34s
                    sourceUri = "boom://audio/trk-1.mp3"
                ),
                playlist = listOf(
                    AudioTrackInfo("trk-1", "Neon Resonance", "HyperPulse", 214000L, "boom://audio/trk-1.mp3"),
                    AudioTrackInfo("trk-2", "Midnight Velocity", "CyberWave", 198000L, "boom://audio/trk-2.mp3"),
                    AudioTrackInfo("trk-3", "Sub-Bass Infinity", "Bassline Labs", 245000L, "boom://audio/trk-3.mp3")
                )
            )
        )
    )
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ActivityNavigationEvent>()
    val navigationEvents: SharedFlow<ActivityNavigationEvent> = _navigationEvents.asSharedFlow()

    private var playbackProgressJob: Job? = null

    init {
        sessionStorage.getSession()?.let { session ->
            _uiState.update { it.copy(currentUserId = session.userId) }
        }

        // Listen for transport packets
        viewModelScope.launch {
            nearbyTransport.events.collect { event ->
                when (event) {
                    is ConnectionEvent.PacketReceived -> handleIncomingPacket(event.packet)
                    else -> Unit
                }
            }
        }
    }

    fun setPartyContext(partyId: String, isHost: Boolean) {
        _uiState.update { it.copy(partyId = partyId, isHost = isHost) }
    }

    /**
     * Host switches the shared active activity across all devices (ACT-01)
     */
    fun switchActivity(newType: ActivityType) {
        if (!_uiState.value.isHost) return

        _uiState.update { it.copy(activeType = newType) }

        viewModelScope.launch {
            val state = ActivityState(
                activityId = "act-${UUID.randomUUID().toString().take(6)}",
                partyId = _uiState.value.partyId,
                activeType = newType,
                musicState = _uiState.value.musicState,
                vibeState = _uiState.value.vibeState
            )
            apiClient.updateActivityState(_uiState.value.partyId, state)
        }
    }

    /**
     * Host controls playback and synchronizes with all participants via AudioSyncEngine (FR-04)
     */
    fun togglePlayPause() {
        if (!_uiState.value.isHost) return

        val currentMusic = _uiState.value.musicState
        val isCurrentlyPlaying = currentMusic.playbackStatus == PlaybackStatus.PLAYING

        if (isCurrentlyPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val currentTrack = _uiState.value.musicState.currentTrack ?: return
        val currentPosition = _uiState.value.musicState.currentPositionMs

        // Schedule playback 250ms in future for network propagation
        val leadTimeMs = 250L
        val masterPlayAtEpochMs = System.currentTimeMillis() + leadTimeMs

        _uiState.update {
            it.copy(
                musicState = it.musicState.copy(
                    playbackStatus = PlaybackStatus.PLAYING,
                    scheduledPlayAtMasterEpochMs = masterPlayAtEpochMs
                )
            )
        }

        startProgressTimer()

        viewModelScope.launch {
            // Broadcast SchedulePlayback packet to all connected peers
            val packet = AudioSyncPacket.SchedulePlayback(
                trackId = currentTrack.trackId,
                trackUri = currentTrack.sourceUri,
                masterPlayAtEpochMs = masterPlayAtEpochMs,
                startOffsetMs = currentPosition
            )
            nearbyTransport.broadcastPacket(packet)
        }
    }

    private fun pausePlayback() {
        _uiState.update {
            it.copy(
                musicState = it.musicState.copy(
                    playbackStatus = PlaybackStatus.PAUSED
                )
            )
        }
        playbackProgressJob?.cancel()

        viewModelScope.launch {
            val packet = AudioSyncPacket.PlaybackControl(
                command = "PAUSE",
                positionMs = _uiState.value.musicState.currentPositionMs,
                effectiveMasterTimestampEpochMs = System.currentTimeMillis()
            )
            nearbyTransport.broadcastPacket(packet)
        }
    }

    fun seekTo(positionMs: Long) {
        if (!_uiState.value.isHost) return

        _uiState.update {
            it.copy(
                musicState = it.musicState.copy(currentPositionMs = positionMs)
            )
        }

        viewModelScope.launch {
            val packet = AudioSyncPacket.PlaybackControl(
                command = "SEEK",
                positionMs = positionMs,
                effectiveMasterTimestampEpochMs = System.currentTimeMillis()
            )
            nearbyTransport.broadcastPacket(packet)
        }
    }

    fun skipNextTrack() {
        if (!_uiState.value.isHost) return

        val state = _uiState.value.musicState
        val nextIndex = (state.queueIndex + 1) % state.playlist.size
        val nextTrack = state.playlist[nextIndex]

        _uiState.update {
            it.copy(
                musicState = it.musicState.copy(
                    currentTrack = nextTrack,
                    queueIndex = nextIndex,
                    currentPositionMs = 0L
                )
            )
        }
        startPlayback()
    }

    fun skipPreviousTrack() {
        if (!_uiState.value.isHost) return

        val state = _uiState.value.musicState
        val prevIndex = if (state.queueIndex > 0) state.queueIndex - 1 else state.playlist.size - 1
        val prevTrack = state.playlist[prevIndex]

        _uiState.update {
            it.copy(
                musicState = it.musicState.copy(
                    currentTrack = prevTrack,
                    queueIndex = prevIndex,
                    currentPositionMs = 0L
                )
            )
        }
        startPlayback()
    }

    /**
     * Vibe voting (ACT-02, FR-10)
     */
    fun castVote(optionId: String) {
        val currentVibe = _uiState.value.vibeState
        val previousSelectedId = _uiState.value.userSelectedVoteOptionId

        if (previousSelectedId == optionId) return // already voted for this

        val updatedOptions = currentVibe.options.map { option ->
            when (option.id) {
                optionId -> option.copy(votes = option.votes + 1)
                previousSelectedId -> option.copy(votes = maxOf(0, option.votes - 1))
                else -> option
            }
        }

        _uiState.update {
            it.copy(
                vibeState = currentVibe.copy(options = updatedOptions),
                userSelectedVoteOptionId = optionId
            )
        }
    }

    /**
     * Live emoji reaction (FR-10)
     */
    fun sendReaction(emoji: String) {
        _uiState.update {
            it.copy(
                liveReactions = (it.liveReactions + emoji).takeLast(10)
            )
        }
    }

    /**
     * Independent local volume & mute (FR-06)
     */
    fun setLocalVolume(volume: Float) {
        _uiState.update {
            it.copy(
                localVolume = volume.coerceIn(0.0f, 1.0f),
                isMutedLocally = volume == 0f
            )
        }
    }

    fun toggleLocalMute() {
        _uiState.update {
            it.copy(isMutedLocally = !it.isMutedLocally)
        }
    }

    private fun handleIncomingPacket(packet: AudioSyncPacket) {
        when (packet) {
            is AudioSyncPacket.SchedulePlayback -> {
                // Participant scheduled playback handling
                val now = System.currentTimeMillis()
                val delayMs = audioSyncEngine.schedulePlayAtMasterTime(packet.masterPlayAtEpochMs, now)

                _uiState.update {
                    it.copy(
                        musicState = it.musicState.copy(
                            playbackStatus = PlaybackStatus.PLAYING,
                            currentPositionMs = packet.startOffsetMs
                        )
                    )
                }

                startProgressTimer()
            }
            is AudioSyncPacket.PlaybackControl -> {
                when (packet.command) {
                    "PAUSE" -> {
                        _uiState.update {
                            it.copy(
                                musicState = it.musicState.copy(
                                    playbackStatus = PlaybackStatus.PAUSED,
                                    currentPositionMs = packet.positionMs
                                )
                            )
                        }
                        playbackProgressJob?.cancel()
                    }
                    "SEEK" -> {
                        _uiState.update {
                            it.copy(
                                musicState = it.musicState.copy(
                                    currentPositionMs = packet.positionMs
                                )
                            )
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    private fun startProgressTimer() {
        playbackProgressJob?.cancel()
        playbackProgressJob = viewModelScope.launch {
            while (true) {
                delay(500L)
                val current = _uiState.value.musicState
                if (current.playbackStatus == PlaybackStatus.PLAYING && current.currentTrack != null) {
                    val nextPosition = current.currentPositionMs + 500L
                    if (nextPosition >= current.currentTrack.durationMs) {
                        // Track completed, skip to next if host
                        if (_uiState.value.isHost) {
                            skipNextTrack()
                        } else {
                            _uiState.update {
                                it.copy(
                                    musicState = it.musicState.copy(
                                        playbackStatus = PlaybackStatus.STOPPED,
                                        currentPositionMs = 0L
                                    )
                                )
                            }
                        }
                        break
                    } else {
                        // Evaluate audio drift for participant
                        val speed = if (!_uiState.value.isHost) {
                            audioSyncEngine.evaluateDriftAndGetSpeedAdjustment(
                                expectedHostPositionMs = nextPosition,
                                actualLocalPositionMs = nextPosition - 12L // simulated slight offset
                            )
                        } else 1.0f

                        _uiState.update {
                            it.copy(
                                musicState = it.musicState.copy(currentPositionMs = nextPosition),
                                syncDiagnostics = audioSyncEngine.diagnostics.value
                            )
                        }
                    }
                }
            }
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            _navigationEvents.emit(ActivityNavigationEvent.NavigateBack)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackProgressJob?.cancel()
    }
}
