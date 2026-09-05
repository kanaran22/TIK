package com.geotask.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = PaperCard,
    onPrimaryContainer = Ink,
    secondary = Accent,
    onSecondary = Ink,
    secondaryContainer = Accent,
    onSecondaryContainer = Ink,
    tertiary = Accent,
    background = Paper,
    onBackground = Ink,
    surface = PaperCard,
    onSurface = Ink,
    surfaceVariant = PaperCard,
    onSurfaceVariant = Ink,
    outline = Ink,
    error = Error,
    errorContainer = Ink,
    onErrorContainer = Color.White
)

private val DarkColors = darkColorScheme(
    primary = InkDark,
    onPrimary = PaperDark,
    primaryContainer = PaperCardDark,
    onPrimaryContainer = InkDark,
    secondary = AccentDark,
    onSecondary = PaperDark,
    secondaryContainer = AccentDark,
    onSecondaryContainer = PaperDark,
    tertiary = AccentDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperCardDark,
    onSurface = InkDark,
    surfaceVariant = PaperCardDark,
    onSurfaceVariant = InkDark,
    outline = InkDark,
    error = Error,
    errorContainer = InkDark,
    onErrorContainer = Error
)

@Composable
fun TikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
