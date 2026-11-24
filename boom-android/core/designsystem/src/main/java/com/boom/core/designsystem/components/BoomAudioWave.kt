package com.boom.core.designsystem.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boom.core.designsystem.theme.BoomElectricIndigo
import com.boom.core.designsystem.theme.BoomNeonCoral
import com.boom.core.designsystem.theme.BoomNeonCyan

/**
 * Animated Audio Waveform component.
 * Respects Android system reduced-motion and accessibility animation duration scale settings.
 */
@Composable
fun BoomAudioWave(
    modifier: Modifier = Modifier,
    barCount: Int = 7,
    maxHeight: Dp = 64.dp,
    barWidth: Dp = 6.dp,
    barSpacing: Dp = 6.dp,
    gradientColors: List<Color> = listOf(BoomElectricIndigo, BoomNeonCoral, BoomNeonCyan),
    forceStatic: Boolean = false
) {
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        if (forceStatic) true else {
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

    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave_transition")

    val animations = (0 until barCount).map { index ->
        if (isReducedMotion) {
            0.5f // Static calm waveform height when reduced-motion is requested
        } else {
            val duration = 450 + (index * 90)
            val delay = index * 70
            val fraction by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            fraction
        }
    }

    Row(
        modifier = modifier
            .semantics { contentDescription = "Sound wave animation indicator" },
        horizontalArrangement = Arrangement.spacedBy(barSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val brush = Brush.verticalGradient(gradientColors)
        animations.forEach { scaleFraction ->
            val height = maxHeight * scaleFraction
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(height)
                    .background(brush = brush, shape = RoundedCornerShape(percent = 50))
            )
        }
    }
}
