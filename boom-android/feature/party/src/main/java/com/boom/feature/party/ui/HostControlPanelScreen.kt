package com.boom.feature.party.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.boom.core.designsystem.components.BoomAudioWave
import com.boom.core.designsystem.components.BoomCard
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
fun HostControlPanelScreen(
    viewModel: PartyViewModel,
    onOpenChat: (partyId: String) -> Unit,
    onOpenActivities: (partyId: String) -> Unit,
    onOpenSecurity: (partyId: String) -> Unit,
    onEndParty: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val party = state.activeParty

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoomDarkBackground)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // 1. Party Header & Invite Code Badge
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
                    text = "Host Control Panel • Authoritative Master Clock",
                    style = MaterialTheme.typography.bodySmall,
                    color = BoomNeonCyan
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BoomElectricIndigo)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = party?.shortCode ?: "BOOM",
                    color = BoomTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Quick Navigation Action Bar (Chat, Activities, Security, End)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Chat Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { party?.let { onOpenChat(it.partyId) } },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BoomDarkSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat", tint = BoomNeonCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Chat", style = MaterialTheme.typography.labelSmall, color = BoomTextPrimary)
                }
            }

            // Music / Activities Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { party?.let { onOpenActivities(it.partyId) } },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BoomDarkSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Music", tint = BoomNeonCoral)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Music", style = MaterialTheme.typography.labelSmall, color = BoomTextPrimary)
                }
            }

            // Guardian Security Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { party?.let { onOpenSecurity(it.partyId) } },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BoomDarkSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = BoomSuccessGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Guardian", style = MaterialTheme.typography.labelSmall, color = BoomTextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. PENDING JOIN REQUESTS (HOST-03)
        if (state.pendingApprovals.isNotEmpty()) {
            Text(
                text = "Join Requests (${state.pendingApprovals.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BoomAmberWarning
            )

            Spacer(modifier = Modifier.height(10.dp))

            state.pendingApprovals.forEach { pending ->
                BoomCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pending.displayName, color = BoomTextPrimary, fontWeight = FontWeight.Bold)
                            Text(pending.audioRoute.routeName, color = BoomTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }

                        Row {
                            IconButton(onClick = { viewModel.approveParticipant(pending.participantId) }) {
                                Icon(Icons.Default.Check, contentDescription = "Approve", tint = BoomSuccessGreen)
                            }
                            IconButton(onClick = { viewModel.removeParticipant(pending.participantId) }) {
                                Icon(Icons.Default.Close, contentDescription = "Reject", tint = BoomErrorRed)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. CONNECTED PARTICIPANTS ROSTER (HOST-04 & HOST-05)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connected Endpoints (${state.connectedParticipants.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BoomTextPrimary
            )
            BoomAudioWave(barCount = 3, maxHeight = 16.dp, barWidth = 3.dp, barSpacing = 3.dp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (state.connectedParticipants.isEmpty()) {
            BoomCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No guest phones connected yet",
                        color = BoomTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Share code ${party?.shortCode} to connect endpoints",
                        color = BoomNeonCyan,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            state.connectedParticipants.forEach { participant ->
                BoomCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = BoomElectricIndigo)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(participant.displayName, color = BoomTextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${participant.audioRoute.routeName} • Drift: ${participant.clockOffsetMs}ms",
                                    color = BoomTextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BoomStatusBadge(status = participant.status.name)
                            IconButton(onClick = { viewModel.removeParticipant(participant.participantId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Participant", tint = BoomErrorRed)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 5. Close / End Party Button
        OutlinedButton(
            onClick = {
                viewModel.leaveParty()
                onEndParty()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BoomErrorRed)
        ) {
            Text("End BOOM Party", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
