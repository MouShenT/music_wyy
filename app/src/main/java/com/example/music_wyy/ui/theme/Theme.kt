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
    onPrimary = TextPrimary,
    primaryContainer = NeteaseRedDark,
    onPrimaryContainer = TextPrimary,
    secondary = NeteaseRedLight,
    onSecondary = TextPrimary,
    secondaryContainer = CardDark,
    onSecondaryContainer = TextSecondary,
    tertiary = NeteaseRedLight,
    onTertiary = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    error = NeteaseRed,
    onError = TextPrimary,
    outline = DividerDark,
)

@Composable
fun MusicWyyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundDark.toArgb()
            window.navigationBarColor = BackgroundDark.toArgb()
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
