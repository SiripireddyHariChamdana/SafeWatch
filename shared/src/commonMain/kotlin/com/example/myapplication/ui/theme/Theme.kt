package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class UserTheme {
    FUTURISTIC_DARK,
    STEALTH_BLACK,
    MIDNIGHT_PURPLE,
    EMERGENCY_RED
}

private val FuturisticColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonPurple,
    background = DarkNavy,
    surface = CardNavy,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val StealthColorScheme = darkColorScheme(
    primary = StealthAccent,
    secondary = Color.Gray,
    background = StealthBlack,
    surface = StealthGrey,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightAccent,
    secondary = NeonBlue,
    background = MidnightBackground,
    surface = MidnightCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val EmergencyColorScheme = darkColorScheme(
    primary = RedSOS,
    secondary = Color.White,
    background = EmergencyBackground,
    surface = EmergencyCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ShadowGuardTheme(
    userTheme: UserTheme = UserTheme.FUTURISTIC_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (userTheme) {
        UserTheme.FUTURISTIC_DARK -> FuturisticColorScheme
        UserTheme.STEALTH_BLACK -> StealthColorScheme
        UserTheme.MIDNIGHT_PURPLE -> MidnightColorScheme
        UserTheme.EMERGENCY_RED -> EmergencyColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
