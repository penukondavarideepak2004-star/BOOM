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
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boom.core.designsystem.components.BoomCard
import com.boom.core.designsystem.components.BoomPrimaryButton
import com.boom.core.designsystem.components.BoomSecondaryButton
import com.boom.core.designsystem.components.BoomTextField
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomDarkSurfaceElevated
import com.boom.core.designsystem.theme.BoomElectricIndigo
import com.boom.core.designsystem.theme.BoomErrorRed
import com.boom.core.designsystem.theme.BoomNeonCoral
import com.boom.core.designsystem.theme.BoomNeonCyan
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary
import com.boom.feature.party.PartyViewModel

@Composable
fun JoinPartyScreen(
    viewModel: PartyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoomDarkBackground)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BoomNeonCoral.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan QR",
                tint = BoomNeonCoral,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Join a BOOM Party",
            style = MaterialTheme.typography.displayMedium,
            color = BoomTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Enter party code or select a nearby detected session",
            style = MaterialTheme.typography.bodyMedium,
            color = BoomTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // 1. Enter 6-digit short code
        BoomTextField(
            value = state.joinCodeInput,
            onValueChange = viewModel::updateJoinCodeInput,
            label = "Party Join Code",
            placeholder = "e.g. BOOM42"
        )

        if (state.joinError != null) {
            Text(
                text = state.joinError ?: "",
                color = BoomErrorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        BoomPrimaryButton(
            text = "Connect with Code",
            onClick = { viewModel.joinParty() },
            isLoading = state.isJoiningParty
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(BoomDarkSurfaceElevated))
            Text(
                text = " OR NEARBY ",
                style = MaterialTheme.typography.labelSmall,
                color = BoomTextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(BoomDarkSurfaceElevated))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Nearby Discovered Parties List
        BoomSecondaryButton(
            text = if (state.isDiscoveringNearby) "Scanning Nearby Devices..." else "Scan for Nearby Parties",
            onClick = viewModel::toggleNearbyDiscovery
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.discoveredNearbyParties.isEmpty()) {
            if (state.isDiscoveringNearby) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = BoomNeonCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Searching for nearby BOOM hosts...",
                        style = MaterialTheme.typography.bodySmall,
                        color = BoomTextSecondary
                    )
                }
            }
        } else {
            state.discoveredNearbyParties.forEach { party ->
                BoomCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { viewModel.joinParty(party.shortCode) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BoomElectricIndigo.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speaker,
                                    contentDescription = "Speaker",
                                    tint = BoomElectricIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${party.hostDisplayName}'s Party",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BoomTextPrimary
                                )
                                Text(
                                    text = "Code: ${party.shortCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BoomNeonCyan
                                )
                            }
                        }

                        Text(
                            text = "Join →",
                            style = MaterialTheme.typography.titleMedium,
                            color = BoomElectricIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
