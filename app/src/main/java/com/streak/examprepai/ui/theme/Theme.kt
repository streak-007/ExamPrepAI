package com.streak.examprepai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorWhite = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = MistBlue,
    onPrimary = Night,
    primaryContainer = SlateBlue,
    onPrimaryContainer = MistBlue,
    secondary = AquaSoft,
    onSecondary = Night,
    secondaryContainer = Aqua,
    onSecondaryContainer = MistBlue,
    tertiary = CoralSoft,
    onTertiary = Night,
    tertiaryContainer = Coral,
    onTertiaryContainer = ColorWhite,
    background = Night,
    onBackground = MistText,
    surface = NightSurface,
    onSurface = MistText,
    surfaceVariant = NightCard,
    onSurfaceVariant = Color(0xFFB5C1D4),
    outlineVariant = Color(0xFF31415B),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = SlateBlue,
    onPrimary = ColorWhite,
    primaryContainer = MistBlue,
    onPrimaryContainer = SlateBlue,
    secondary = Aqua,
    onSecondary = ColorWhite,
    secondaryContainer = AquaSoft,
    onSecondaryContainer = Ink,
    tertiary = Coral,
    onTertiary = ColorWhite,
    tertiaryContainer = CoralSoft,
    onTertiaryContainer = Coral,
    background = Canvas,
    onBackground = Ink,
    surface = ColorWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9EFF7),
    onSurfaceVariant = Color(0xFF56637A),
    outlineVariant = Color(0xFFD1DAE8),
    errorContainer = Color(0xFFFDE2E1),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun ExamPrepAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
