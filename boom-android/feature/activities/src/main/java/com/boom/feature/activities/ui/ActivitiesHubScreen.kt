package com.boom.feature.activities.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boom.core.audio.SyncQuality
import com.boom.core.designsystem.components.BoomAudioWave
import com.boom.core.designsystem.theme.BackgroundDark
import com.boom.core.designsystem.theme.ElectricIndigo
import com.boom.core.designsystem.theme.NeonCoral
import com.boom.core.designsystem.theme.NeonCyan
import com.boom.core.designsystem.theme.SurfaceDark
import com.boom.core.designsystem.theme.SurfaceVariantDark
import com.boom.core.model.ActivityType
import com.boom.core.model.AudioTrackInfo
import com.boom.core.model.PlaybackStatus
import com.boom.core.model.VibeActivityState
import com.boom.core.model.VibeVoteOption
import com.boom.feature.activities.ActivitiesViewModel
import com.boom.feature.activities.ActivityNavigationEvent
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesHubScreen(
    viewModel: ActivitiesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is ActivityNavigationEvent.NavigateBack -> onBack()
            }
        }
    }

    val selectedTabIndex = when (uiState.activeType) {
        ActivityType.MUSIC_PLAYER -> 0
        ActivityType.VIBE_VOTE -> 1
        ActivityType.PARTY_GAME -> 2
        ActivityType.NONE -> 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Party Activities",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (uiState.isHost) "Host Control • Sync Master" else "Guest Sync Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClicked() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row for Activity Switching (ACT-01)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SurfaceDark,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ElectricIndigo,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { if (uiState.isHost) viewModel.switchActivity(ActivityType.MUSIC_PLAYER) },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Music", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { if (uiState.isHost) viewModel.switchActivity(ActivityType.VIBE_VOTE) },
                    icon = { Icon(Icons.Default.HowToVote, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Vibe Vote", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { if (uiState.isHost) viewModel.switchActivity(ActivityType.PARTY_GAME) },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Quiz Game", fontSize = 12.sp) }
                )
            }

            // Host activity broadcast reminder
            if (uiState.isHost) {
                Surface(
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⭐ Host Mode: Selecting tabs switches active activity for all participants",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Active Activity Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (uiState.activeType) {
                    ActivityType.MUSIC_PLAYER -> {
                        MusicPlayerContent(
                            uiState = uiState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNextTrack() },
                            onSkipPrev = { viewModel.skipPreviousTrack() },
                            onSeek = { viewModel.seekTo(it) },
                            onVolumeChanged = { viewModel.setLocalVolume(it) },
                            onToggleMute = { viewModel.toggleLocalMute() },
                            onReaction = { viewModel.sendReaction(it) }
                        )
                    }
                    ActivityType.VIBE_VOTE -> {
                        VibeVoteContent(
                            vibeState = uiState.vibeState,
                            selectedOptionId = uiState.userSelectedVoteOptionId,
                            onVote = { viewModel.castVote(it) },
                            onReaction = { viewModel.sendReaction(it) }
                        )
                    }
                    ActivityType.PARTY_GAME, ActivityType.NONE -> {
                        PartyGameContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicPlayerContent(
    uiState: com.boom.feature.activities.ActivityUiState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onReaction: (String) -> Unit
) {
    val track = uiState.musicState.currentTrack
    val isPlaying = uiState.musicState.playbackStatus == PlaybackStatus.PLAYING
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album Art Box with animated Audio Wave (UI-02, FR-04)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .size(240.dp)
                .border(2.dp, ElectricIndigo.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BoomAudioWave(
                    isAnimating = isPlaying,
                    barCount = 9,
                    modifier = Modifier.size(160.dp, 80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Track Title & Artist
        Text(
            text = track?.title ?: "No Track Selected",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = track?.artist ?: "Unknown Artist",
            style = MaterialTheme.typography.bodyMedium,
            color = NeonCyan
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sync Health Badge (FR-04)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when (uiState.syncDiagnostics.quality) {
                SyncQuality.EXCELLENT -> NeonCyan.copy(alpha = 0.15f)
                SyncQuality.GOOD -> Color.Green.copy(alpha = 0.15f)
                SyncQuality.DEGRADED -> NeonCoral.copy(alpha = 0.15f)
                SyncQuality.OUT_OF_SYNC -> Color.Red.copy(alpha = 0.15f)
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isHost) {
                        "Clock Master • Broadcasting at 0ms reference"
                    } else {
                        "Sync: ${uiState.syncDiagnostics.quality.name} (±${kotlin.math.abs(uiState.syncDiagnostics.currentDriftMs)}ms drift • ${uiState.syncDiagnostics.playbackSpeedMultiplier}x)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seekbar & Timestamps
        val duration = track?.durationMs ?: 1L
        val currentPos = uiState.musicState.currentPositionMs.coerceIn(0L, duration)
        val progress = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

        Slider(
            value = progress,
            onValueChange = { newFrac ->
                if (uiState.isHost) {
                    onSeek((newFrac * duration).toLong())
                }
            },
            enabled = uiState.isHost,
            colors = SliderDefaults.colors(
                thumbColor = if (uiState.isHost) ElectricIndigo else Color.Transparent,
                activeTrackColor = ElectricIndigo,
                inactiveTrackColor = SurfaceVariantDark
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTimecode(currentPos), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            Text(text = formatTimecode(duration), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Host Playback Controls (Play/Pause, Prev, Next)
        if (uiState.isHost) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrev, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Previous Track", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(20.dp))

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ElectricIndigo)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.FastForward, contentDescription = "Next Track", tint = Color.White)
                }
            }
        } else {
            // Participant read-only playback banner
            Text(
                text = if (isPlaying) "Host is playing synchronously" else "Playback paused by Host",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Local Volume & Mute (FR-06)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Device Volume (Independent)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isMutedLocally) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (uiState.isMutedLocally) "Unmute" else "Mute",
                            tint = if (uiState.isMutedLocally) NeonCoral else NeonCyan
                        )
                    }
                }

                Slider(
                    value = if (uiState.isMutedLocally) 0f else uiState.localVolume,
                    onValueChange = onVolumeChanged,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Reaction Bar (FR-10)
        Text(
            text = "Tap to React with the Room",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("🔥", "💃", "🎧", "🚀", "❤️").forEach { emoji ->
                Surface(
                    shape = CircleShape,
                    color = SurfaceDark,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onReaction(emoji) }
                        .border(1.dp, SurfaceVariantDark, CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }
            }
        }

        // Live reactions bubble stream
        if (uiState.liveReactions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.liveReactions.takeLast(6).forEach { r ->
                    Text(text = r, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun VibeVoteContent(
    vibeState: VibeActivityState,
    selectedOptionId: String?,
    onVote: (String) -> Unit,
    onReaction: (String) -> Unit
) {
    val totalVotes = vibeState.options.sumOf { it.votes }.coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCoral.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = null,
                        tint = NeonCoral,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ROOM VIBE POLL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCoral
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = vibeState.activeQuestion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                vibeState.options.forEach { option ->
                    val isSelected = option.id == selectedOptionId
                    val percentage = (option.votes.toFloat() / totalVotes.toFloat())

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ElectricIndigo.copy(alpha = 0.2f) else SurfaceVariantDark)
                            .clickable { onVote(option.id) }
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) NeonCyan else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${option.votes} votes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Voted",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isSelected) NeonCyan else ElectricIndigo,
                            trackColor = SurfaceDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyGameContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = ElectricIndigo,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "BOOM Music Quiz & Trivia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Guess the track title and artist within 10 seconds! Synchronized round starting on Host cue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
