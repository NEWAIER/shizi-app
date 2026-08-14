package com.family.shizi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ShiziColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = CardWhite,
    secondary = SkyBlue,
    onSecondary = CardWhite,
    tertiary = ChallengePurple,
    onTertiary = CardWhite,
    background = WarmBackground,
    onBackground = PrimaryText,
    surface = CardWhite,
    onSurface = PrimaryText,
    errorContainer = GentleError,
    onErrorContainer = PrimaryText,
)

@Composable
fun ShiziTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShiziColorScheme,
        typography = ShiziTypography,
        content = content,
    )
}
