package com.boom.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BoomDarkColorScheme = darkColorScheme(
    primary = BoomElectricIndigo,
    onPrimary = BoomTextPrimary,
    primaryContainer = BoomIndigoDark,
    onPrimaryContainer = BoomTextPrimary,
    secondary = BoomNeonCoral,
    onSecondary = BoomTextPrimary,
    tertiary = BoomNeonCyan,
    onTertiary = BoomDarkBackground,
    background = BoomDarkBackground,
    onBackground = BoomTextPrimary,
    surface = BoomDarkSurface,
    onSurface = BoomTextPrimary,
    surfaceVariant = BoomDarkSurfaceElevated,
    onSurfaceVariant = BoomTextSecondary,
    outline = BoomDarkBorder,
    error = BoomErrorRed,
    onError = BoomTextPrimary
)

@Composable
fun BoomTheme(
    darkTheme: Boolean = true, // BOOM is dark-themed by design for music & party vibe
    content: @Composable () -> Unit
) {
    val colorScheme = BoomDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BoomTypography,
        shapes = BoomShapes,
        content = content
    )
}
