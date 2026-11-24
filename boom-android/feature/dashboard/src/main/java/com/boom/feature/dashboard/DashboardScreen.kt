package com.boom.feature.dashboard

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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boom.core.designsystem.components.BoomAudioWave
import com.boom.core.designsystem.components.BoomCard
import com.boom.core.designsystem.components.BoomPrimaryButton
import com.boom.core.designsystem.components.BoomSecondaryButton
import com.boom.core.designsystem.components.BoomStatusBadge
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomDarkSurface
import com.boom.core.designsystem.theme.BoomDarkSurfaceElevated
import com.boom.core.designsystem.theme.BoomElectricIndigo
import com.boom.core.designsystem.theme.BoomIndigoDark
import com.boom.core.designsystem.theme.BoomNeonCoral
import com.boom.core.designsystem.theme.BoomNeonCyan
import com.boom.core.designsystem.theme.BoomSuccessGreen
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToHost: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoomDarkBackground)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "BOOM Main Dashboard" }
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Top App Bar: Greeting & Security Badge & Logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(BoomElectricIndigo, BoomNeonCoral))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.displayName.take(1).uppercase(),
                        color = BoomTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Hey, ${state.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BoomTextPrimary
                    )
                    Text(
                        text = "Ready to BOOM?",
                        style = MaterialTheme.typography.bodySmall,
                        color = BoomTextSecondary
                    )
                }
            }

            IconButton(onClick = viewModel::logout) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = BoomTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. PRIMARY ACTION: HOST A PARTY CARD (DASH-01)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToHost() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BoomDarkSurfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(BoomElectricIndigo.copy(alpha = 0.35f), BoomDarkSurfaceElevated)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(BoomElectricIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speaker,
                                contentDescription = "Host Party Icon",
                                tint = BoomTextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        BoomAudioWave(
                            barCount = 4,
                            maxHeight = 24.dp,
                            barWidth = 4.dp,
                            barSpacing = 3.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Host a Party",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BoomTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Lead a synchronized listening session. Share your QR code or 6-digit party code with nearby friends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BoomTextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    BoomPrimaryButton(
                        text = "Start Party as Host",
                        onClick = onNavigateToHost
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. PRIMARY ACTION: JOIN A PARTY CARD (DASH-01)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToJoin() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BoomDarkSurfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(BoomNeonCoral.copy(alpha = 0.25f), BoomDarkSurfaceElevated)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BoomNeonCoral),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Join Party Icon",
                            tint = BoomTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Join a Party",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BoomTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Scan a host's QR code or enter a short code to become a synchronized audio endpoint.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BoomTextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    BoomSecondaryButton(
                        text = "Scan QR or Enter Code",
                        onClick = onNavigateToJoin
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. SECURITY STATUS CARD (Unobtrusive status per Section 4.2)
        BoomCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSecurity() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BoomSuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Status",
                            tint = BoomSuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "BOOM Guardian",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BoomTextPrimary
                        )
                        Text(
                            text = "Score ${state.securityScore}% • ${state.securityStatusLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BoomSuccessGreen
                        )
                    }
                }

                BoomStatusBadge(status = "CONNECTED")
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
