package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GrandmasterSuiteColorScheme = darkColorScheme(
    primary = PlatinumLight,
    onPrimary = SurfaceGround,
    primaryContainer = WoodGrainMid,
    onPrimaryContainer = TextPrimary,
    secondary = GoldAccentRestrained,
    onSecondary = SurfaceGround,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = ColdIceBlue,
    onTertiary = SurfaceGround,
    tertiaryContainer = WoodGrainDark,
    onTertiaryContainer = GoldAccentRestrained,
    error = StateDanger,
    onError = Color.White,
    background = SurfaceGround,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSlate
)

@Composable
fun ChessWithFriendsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GrandmasterSuiteColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ChessWithFriendsTheme(darkTheme = darkTheme, content = content)
}
