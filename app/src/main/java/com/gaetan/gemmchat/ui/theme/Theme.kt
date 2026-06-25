package com.gaetan.gemmchat.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GemmaDarkScheme = darkColorScheme(
    primary = GemmaColors.AccentPurpleSoft,
    onPrimary = GemmaColors.Background,
    primaryContainer = GemmaColors.AccentPurple,
    onPrimaryContainer = GemmaColors.TextPrimary,
    secondary = GemmaColors.Success,
    onSecondary = GemmaColors.Background,
    background = GemmaColors.Background,
    onBackground = GemmaColors.TextPrimary,
    surface = GemmaColors.SurfaceCard,
    onSurface = GemmaColors.TextPrimary,
    surfaceVariant = GemmaColors.SurfaceElevated,
    onSurfaceVariant = GemmaColors.TextMuted,
    outline = GemmaColors.BorderSubtle,
    error = GemmaColors.AccentPurpleMid,
)

@Composable
fun GemmaChatTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GemmaColors.Background.toArgb()
            window.navigationBarColor = GemmaColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = GemmaDarkScheme,
        typography = GemmaTypography,
        content = content,
    )
}