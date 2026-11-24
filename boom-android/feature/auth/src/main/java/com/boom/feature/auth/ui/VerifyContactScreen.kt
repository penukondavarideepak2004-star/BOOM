package com.boom.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boom.core.designsystem.components.BoomCard
import com.boom.core.designsystem.components.BoomPrimaryButton
import com.boom.core.designsystem.components.BoomTextField
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomErrorRed
import com.boom.core.designsystem.theme.BoomSuccessGreen
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary
import com.boom.feature.auth.AuthViewModel

@Composable
fun VerifyContactScreen(
    viewModel: AuthViewModel,
    onAllVerified: () -> Unit
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
            text = "Verify Your Contact",
            style = MaterialTheme.typography.displayMedium,
            color = BoomTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "For security, verify both your email and phone number",
            style = MaterialTheme.typography.bodyMedium,
            color = BoomTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Email Verification Card
        BoomCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Email Verification",
                            style = MaterialTheme.typography.titleMedium,
                            color = BoomTextPrimary
                        )
                        Text(
                            text = state.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = BoomTextSecondary
                        )
                    }

                    if (state.isEmailVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = BoomSuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified", color = BoomSuccessGreen, fontSize = 12.sp)
                        }
                    }
                }

                if (!state.isEmailVerified) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BoomTextField(
                        value = state.emailOtp,
                        onValueChange = viewModel::updateEmailOtp,
                        label = "6-Digit Email Code",
                        placeholder = "123456"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BoomPrimaryButton(
                        text = "Verify Email",
                        onClick = viewModel::verifyEmail,
                        isLoading = state.isVerifyingContact
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Verification Card
        BoomCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Phone SMS Verification",
                            style = MaterialTheme.typography.titleMedium,
                            color = BoomTextPrimary
                        )
                        Text(
                            text = state.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = BoomTextSecondary
                        )
                    }

                    if (state.isPhoneVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = BoomSuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified", color = BoomSuccessGreen, fontSize = 12.sp)
                        }
                    }
                }

                if (!state.isPhoneVerified) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BoomTextField(
                        value = state.phoneOtp,
                        onValueChange = viewModel::updatePhoneOtp,
                        label = "6-Digit Phone OTP",
                        placeholder = "654321"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BoomPrimaryButton(
                        text = "Verify Phone OTP",
                        onClick = viewModel::verifyPhone,
                        isLoading = state.isVerifyingContact
                    )
                }
            }
        }

        if (state.verificationError != null) {
            Text(
                text = state.verificationError ?: "",
                color = BoomErrorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (state.resendCooldownSeconds > 0) {
            Text(
                text = "Resend code available in ${state.resendCooldownSeconds}s",
                color = BoomTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
