package com.example.music_wyy.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NeteaseDarkColors = darkColorScheme(
    primary = NeteaseRed,
    onPrimary = TextOnPrimary,
    primaryContainer = NeteaseRedDark,
    onPrimaryContainer = TextOnPrimary,
    secondary = NeteaseRedLight,
    onSecondary = TextOnPrimary,
    secondaryContainer = SurfaceDark4,
    onSecondaryContainer = TextSecondary,
    tertiary = NeteaseRedLight,
    onTertiary = TextOnPrimary,
    background = SurfaceDark0,
    onBackground = TextPrimary,
    surface = SurfaceDark2,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark4,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = SurfaceDark1,
    surfaceContainerHigh = SurfaceDark4,
    surfaceContainerHighest = SurfaceDark5,
    error = ErrorRed,
    onError = TextOnPrimary,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = ErrorRed,
    outline = DividerDark,
    outlineVariant = DividerDark,
    scrim = ScrimDark,
)

@Composable
fun MusicWyyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceDark0.toArgb()
            window.navigationBarColor = SurfaceDark0.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = NeteaseDarkColors,
        typography = NeteaseTypography,
        content = content,
    )
}
