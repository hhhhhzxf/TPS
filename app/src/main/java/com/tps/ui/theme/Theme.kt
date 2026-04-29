package com.tps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF5A1F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1D2),
    onPrimaryContainer = Color(0xFF8A2400),
    secondary = Color(0xFF1F8A70),
    secondaryContainer = Color(0xFFD9F2EA),
    tertiary = Color(0xFFFFB000),
    background = Color(0xFFFFF5ED),
    surface = Color.White,
    surfaceVariant = Color(0xFFFFE9DC),
    onSurface = Color(0xFF241A16),
    onSurfaceVariant = Color(0xFF7B6257),
    outline = Color(0xFFE7C8B8),
    error = Color(0xFFD93025),
)

@Composable
fun TpsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
