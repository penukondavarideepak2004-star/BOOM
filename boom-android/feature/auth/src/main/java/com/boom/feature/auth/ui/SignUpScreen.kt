package com.boom.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boom.core.designsystem.components.BoomAudioWave
import com.boom.core.designsystem.components.BoomPrimaryButton
import com.boom.core.designsystem.components.BoomTextField
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomElectricIndigo
import com.boom.core.designsystem.theme.BoomErrorRed
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary
import com.boom.feature.auth.AuthViewModel

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit
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

        BoomAudioWave(
            barCount = 5,
            maxHeight = 44.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.displayMedium,
            color = BoomTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Join the next generation of synchronized listening",
            style = MaterialTheme.typography.bodyMedium,
            color = BoomTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Email Field
        BoomTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = "Email Address",
            placeholder = "alex@boom.party"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Phone Number Field
        BoomTextField(
            value = state.phoneNumber,
            onValueChange = viewModel::updatePhone,
            label = "Phone Number (E.164)",
            placeholder = "+1234567890"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Display Name Field
        BoomTextField(
            value = state.displayName,
            onValueChange = viewModel::updateDisplayName,
            label = "Display Name",
            placeholder = "Alex Sound"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Field
        BoomTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = "Password (min 8 characters)",
            placeholder = "••••••••"
        )

        if (state.registrationError != null) {
            Text(
                text = state.registrationError ?: "",
                color = BoomErrorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BoomPrimaryButton(
            text = "Continue to Verification",
            onClick = viewModel::register,
            isLoading = state.isRegistering
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Already have an account? ",
                color = BoomTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onNavigateToSignIn) {
                Text(
                    text = "Sign In",
                    color = BoomElectricIndigo,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
