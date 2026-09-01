package com.vcapp.voicechanger.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF7C3AED)
private val Cyan = Color(0xFF22D3EE)
private val Ink = Color(0xFF0B0B14)
private val Surface1 = Color(0xFF151424)

private val DarkColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = Color(0xFF07131A),
    background = Ink,
    onBackground = Color(0xFFEDEAF7),
    surface = Surface1,
    onSurface = Color(0xFFEDEAF7),
    surfaceVariant = Color(0xFF1F1D33),
    onSurfaceVariant = Color(0xFFBDB7D4),
    error = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary = Purple,
    secondary = Cyan
)

@Composable
fun VcAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else DarkColors,
        content = content
    )
}
