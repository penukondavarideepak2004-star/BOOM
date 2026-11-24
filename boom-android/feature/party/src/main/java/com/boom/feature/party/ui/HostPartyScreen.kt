package com.boom.feature.party.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boom.core.designsystem.components.BoomAudioWave
import com.boom.core.designsystem.components.BoomCard
import com.boom.core.designsystem.components.BoomPrimaryButton
import com.boom.core.designsystem.components.BoomTextField
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomElectricIndigo
import com.boom.core.designsystem.theme.BoomErrorRed
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary
import com.boom.feature.party.PartyViewModel

@Composable
fun HostPartyScreen(
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        BoomAudioWave(barCount = 7, maxHeight = 40.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create BOOM Party",
            style = MaterialTheme.typography.displayMedium,
            color = BoomTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Your phone coordinates the session and master clock",
            style = MaterialTheme.typography.bodyMedium,
            color = BoomTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        BoomTextField(
            value = state.createPartyTitle,
            onValueChange = viewModel::updatePartyTitle,
            label = "Party Name",
            placeholder = "Friday Night Boom"
        )

        Spacer(modifier = Modifier.height(16.dp))

        BoomCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Require Host Approval",
                        style = MaterialTheme.typography.titleMedium,
                        color = BoomTextPrimary
                    )
                    Text(
                        text = "Approve each friend before audio connects",
                        style = MaterialTheme.typography.bodySmall,
                        color = BoomTextSecondary
                    )
                }

                Switch(
                    checked = state.requireHostApproval,
                    onCheckedChange = { /* Toggle */ },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BoomTextPrimary,
                        checkedTrackColor = BoomElectricIndigo
                    )
                )
            }
        }

        if (state.partyCreationError != null) {
            Text(
                text = state.partyCreationError ?: "",
                color = BoomErrorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        BoomPrimaryButton(
            text = "Launch Party",
            onClick = viewModel::createParty,
            isLoading = state.isCreatingParty
        )

        Spacer(modifier = Modifier.height(36.dp))
    }
}
