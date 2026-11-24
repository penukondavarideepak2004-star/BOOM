package com.boom.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boom.core.designsystem.components.BoomCard
import com.boom.core.designsystem.components.BoomPrimaryButton
import com.boom.core.designsystem.components.BoomTextField
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomErrorRed
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary
import com.boom.feature.auth.AuthViewModel

@Composable
fun MfaChallengeScreen(
    viewModel: AuthViewModel,
    onMfaSuccess: () -> Unit
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
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Two-Factor Authentication",
            style = MaterialTheme.typography.displayMedium,
            color = BoomTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Enter the 6-digit code from your authenticator app",
            style = MaterialTheme.typography.bodyMedium,
            color = BoomTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        BoomCard {
            Column {
                BoomTextField(
                    value = state.mfaCode,
                    onValueChange = viewModel::updateMfaCode,
                    label = "6-Digit Authenticator Code",
                    placeholder = "123456"
                )

                if (state.mfaError != null) {
                    Text(
                        text = state.mfaError ?: "",
                        color = BoomErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                BoomPrimaryButton(
                    text = "Verify & Access BOOM",
                    onClick = viewModel::verifyMfa,
                    isLoading = state.isVerifyingMfa
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
