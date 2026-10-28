package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MayaPurple,
    onPrimary = Color.White,
    secondary = MayaNeonBlue,
    onSecondary = Color.White,
    tertiary = MayaPink,
    background = MayaBackgroundDark,
    onBackground = TextWhite,
    surface = MayaBackgroundSurface,
    onSurface = TextWhite,
    surfaceVariant = MayaCardDark,
    onSurfaceVariant = TextGray,
    outline = MayaBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
