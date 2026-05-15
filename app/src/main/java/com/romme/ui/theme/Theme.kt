package com.romme.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkGreen = Color(0xFF1B5E20)
private val LightGreen = Color(0xFF4CAF50)
private val Gold = Color(0xFFFFD700)
private val DarkRed = Color(0xFFB71C1C)
private val Cream = Color(0xFFFFF8E1)

private val RommeColorScheme = darkColorScheme(
    primary = LightGreen,
    onPrimary = Color.White,
    primaryContainer = DarkGreen,
    secondary = Gold,
    onSecondary = Color.Black,
    background = DarkGreen,
    surface = Color(0xFF2E7D32),
    onSurface = Cream,
    error = DarkRed,
)

@Composable
fun RommeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RommeColorScheme,
        content = content
    )
}
