package com.redx.codex.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Material3 Dark Color Scheme ───────────────────────────────────────────────
// Evidence: OpenCode only ships dark theme (no light theme resources found in Phase 2)
private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = ColorOnPrimary,
    primaryContainer = AccentBlueDim,
    onPrimaryContainer = AccentBlue,

    secondary = AccentGreen,
    onSecondary = Background,
    secondaryContainer = AccentGreenDim,
    onSecondaryContainer = AccentGreen,

    tertiary = AccentAmber,
    onTertiary = Background,

    error = AccentRed,
    onError = ColorOnPrimary,
    errorContainer = AccentRed.copy(alpha = 0.15f),
    onErrorContainer = AccentRed,

    background = Background,
    onBackground = OnBackground,

    surface = Surface,
    onSurface = OnBackground,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = OnBackgroundSecondary,

    surfaceContainerLowest = Background,
    surfaceContainerLow = Surface,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh,

    outline = DividerColor,
    outlineVariant = OnBackgroundTertiary,

    inverseSurface = OnBackground,
    inverseOnSurface = Background,
    inversePrimary = AccentBlue,

    scrim = Background.copy(alpha = 0.6f),
)

@Composable
fun RedXTheme(
    content: @Composable () -> Unit
) {
    // Always dark theme — same as OpenCode
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            window.navigationBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RedXTypography,
        content = content,
    )
}
