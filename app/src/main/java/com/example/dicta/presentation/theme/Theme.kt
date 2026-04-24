package com.example.dicta.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Hairline,
    onPrimaryContainer = Ink,
    secondary = InkMuted,
    onSecondary = Paper,
    tertiary = Ember,
    onTertiary = Paper,
    tertiaryContainer = EmberSoft,
    onTertiaryContainer = Ember,
    background = Paper,
    onBackground = Ink,
    surface = PaperSurface,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = InkSubtle,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Ember,
    onError = Paper,
    errorContainer = EmberSoft,
    onErrorContainer = Ember,
)

private val DarkColors = darkColorScheme(
    primary = Parchment,
    onPrimary = InkDark,
    primaryContainer = HairlineDark,
    onPrimaryContainer = Parchment,
    secondary = ParchmentMuted,
    onSecondary = InkDark,
    tertiary = EmberDark,
    onTertiary = Parchment,
    tertiaryContainer = EmberSoftDark,
    onTertiaryContainer = EmberDark,
    background = InkDark,
    onBackground = Parchment,
    surface = InkDarkSurface,
    onSurface = Parchment,
    surfaceVariant = InkDark,
    onSurfaceVariant = ParchmentSubtle,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    error = EmberDark,
    onError = Parchment,
    errorContainer = EmberSoftDark,
    onErrorContainer = EmberDark,
)

@Composable
fun DictaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
