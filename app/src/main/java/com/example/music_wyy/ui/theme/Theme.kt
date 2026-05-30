package com.example.music_wyy.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MonochromeDarkColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray15,
    onPrimaryContainer = White,
    secondary = Gray60,
    onSecondary = Black,
    secondaryContainer = Gray12,
    onSecondaryContainer = Gray80,
    tertiary = Gray40,
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = Gray08,
    onSurface = White,
    surfaceVariant = Gray12,
    onSurfaceVariant = Gray60,
    surfaceContainerLow = Gray05,
    surfaceContainerHigh = Gray15,
    surfaceContainerHighest = Gray20,
    error = Gray80,
    onError = Black,
    errorContainer = Gray20,
    onErrorContainer = Gray90,
    outline = Gray20,
    outlineVariant = Gray15,
    scrim = ScrimDark,
)

@Composable
fun MusicWyyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Black.toArgb()
            window.navigationBarColor = Black.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = MonochromeDarkColors,
        typography = NeteaseTypography,
        content = content,
    )
}
