package com.boom.feature.welcome

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boom.core.designsystem.components.BoomAudioWave
import com.boom.core.designsystem.components.BoomPrimaryButton
import com.boom.core.designsystem.theme.BoomDarkBackground
import com.boom.core.designsystem.theme.BoomDarkSurfaceElevated
import com.boom.core.designsystem.theme.BoomElectricIndigo
import com.boom.core.designsystem.theme.BoomNeonCoral
import com.boom.core.designsystem.theme.BoomNeonCyan
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onNavigateToDashboard: () -> Unit,
    autoTransitionDelayMs: Long = 2400L,
    forceStaticMotion: Boolean = false
) {
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        if (forceStaticMotion) true else {
            try {
                val scale = Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
                )
                scale == 0f
            } catch (e: Exception) {
                false
            }
        }
    }

    var isVisible by remember { mutableStateOf(isReducedMotion) }

    LaunchedEffect(Unit) {
        if (!isReducedMotion) {
            delay(100)
            isVisible = true
        }

        // Automatic smooth progression to Main Dashboard
        delay(if (isReducedMotion) 800L else autoTransitionDelayMs)
        onNavigateToDashboard()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BoomDarkBackground)
            .padding(24.dp)
            .semantics { contentDescription = "BOOM Welcome Screen" },
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BoomElectricIndigo.copy(alpha = 0.25f),
                            BoomNeonCoral.copy(alpha = 0.10f),
                            BoomDarkBackground
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Audio wave visual treatment (respects reduced-motion)
            BoomAudioWave(
                barCount = 9,
                maxHeight = 64.dp,
                barWidth = 6.dp,
                barSpacing = 7.dp,
                forceStatic = isReducedMotion
            )

            Spacer(modifier = Modifier.height(28.dp))

            // BOOM Wordmark with animated entrance
            Text(
                text = "BOOM",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = BoomTextPrimary,
                modifier = Modifier.alpha(if (isVisible) 1f else 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Working Tagline
            Text(
                text = "Your friends. Your music. Your BOOM.",
                style = MaterialTheme.typography.titleMedium,
                color = BoomNeonCyan,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(if (isVisible) 1f else 0.1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Synchronized Multi-Device Sound System",
                style = MaterialTheme.typography.bodyMedium,
                color = BoomTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Manual Skip / Fast Progression Button (Accessible for screen readers)
            BoomPrimaryButton(
                text = "Enter Dashboard",
                onClick = onNavigateToDashboard,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
