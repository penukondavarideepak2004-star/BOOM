package com.boom.feature.party.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boom.core.audio.SyncQuality
import com.boom.core.designsystem.components.BoomAudioWave
import com.boom.core.designsystem.components.BoomCard
import com.boom.core.designsystem.components.BoomPrimaryButton
import com.boom.core.designsystem.components.BoomStatusBadge
import com.boom.core.designsystem.theme.BoomAmberWarning
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomDarkSurface
import com.boom.core.designsystem.theme.BoomDarkSurfaceElevated
import com.boom.core.designsystem.theme.BoomElectricIndigo
import com.boom.core.designsystem.theme.BoomErrorRed
import com.boom.core.designsystem.theme.BoomNeonCoral
import com.boom.core.designsystem.theme.BoomNeonCyan
import com.boom.core.designsystem.theme.BoomSuccessGreen
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary
import com.boom.feature.party.PartyViewModel

@Composable
fun ParticipantDashboardScreen(
    viewModel: PartyViewModel,
    onOpenChat: (partyId: String) -> Unit,
    onOpenActivities: (partyId: String) -> Unit = {},
    onLeaveParty: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val party = state.activeParty
    val syncDiag = state.syncDiagnostics

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoomDarkBackground)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // 1. Host Session Identity Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = party?.title ?: "BOOM Party",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = BoomTextPrimary
                )
                Text(
                    text = "Host: ${party?.hostDisplayName ?: "DJ Host"} • Endpoint Active",
                    style = MaterialTheme.typography.bodySmall,
                    color = BoomNeonCyan
                )
            }

            BoomStatusBadge(status = state.joinStatus)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. SYNCHRONIZED AUDIO STATUS CARD (FR-04, FR-07)
        BoomCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BoomNeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = BoomNeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Audio Synchronization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BoomTextPrimary
                            )
                            Text(
                                text = "Offset: ${syncDiag.clockOffsetMs}ms • Drift: ${syncDiag.currentDriftMs}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = BoomTextSecondary
                            )
                        }
                    }

                    val (syncColor, syncLabel) = when (syncDiag.quality) {
                        SyncQuality.EXCELLENT -> BoomSuccessGreen to "Sync <25ms"
                        SyncQuality.GOOD -> BoomNeonCyan to "Sync <60ms"
                        SyncQuality.DEGRADED -> BoomAmberWarning to "Compensating"
                        SyncQuality.OUT_OF_SYNC -> BoomErrorRed to "Resyncing"
                    }

                    Text(
                        text = syncLabel,
                        color = syncColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                BoomAudioWave(
                    barCount = 11,
                    maxHeight = 36.dp,
                    barWidth = 4.dp,
                    barSpacing = 4.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. INDEPENDENT LOCAL VOLUME CONTROLS (FR-06)
        BoomCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Local Endpoint Volume",
                        style = MaterialTheme.typography.titleMedium,
                        color = BoomTextPrimary
                    )

                    IconButton(onClick = viewModel::toggleLocalMute) {
                        Icon(
                            imageVector = if (state.isMutedLocally) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute / Unmute",
                            tint = if (state.isMutedLocally) BoomErrorRed else BoomTextPrimary
                        )
                    }
                }

                Text(
                    text = "Controls only this phone's speaker without affecting host or friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = BoomTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeDown, contentDescription = null, tint = BoomTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = state.localVolume,
                        onValueChange = viewModel::setLocalVolume,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = BoomElectricIndigo,
                            activeTrackColor = BoomElectricIndigo,
                            inactiveTrackColor = BoomDarkSurfaceElevated
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(state.localVolume * 100).toInt()}%",
                        color = BoomTextPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Quick Actions: Activities Hub & Chat
        BoomPrimaryButton(
            text = "Activities & Vibe Voting",
            onClick = { party?.let { onOpenActivities(it.partyId) } }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { party?.let { onOpenChat(it.partyId) } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BoomNeonCyan)
        ) {
            Text(
                text = "Open Party Chat",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. LEAVE PARTY BUTTON (FR-06 & JOIN-04: immediate exit without host approval)
        OutlinedButton(
            onClick = {
                viewModel.leaveParty()
                onLeaveParty()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BoomErrorRed)
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Leave Party",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
