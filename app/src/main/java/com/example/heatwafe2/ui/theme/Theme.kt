package com.example.heatwafe2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),      // Bright Blue
    secondary = Color(0xFFFF9800),    // Orange
    background = Color(0xFFFAFAFA),   // Off-white
    surface = Color(0xFFFFFFFF),      // White
    onPrimary = Color(0xFFFFFFFF),    // White
    onSecondary = Color(0xFFFFFFFF),  // White
    onBackground = Color(0xFF000000), // Black
    onSurface = Color(0xFF000000),     // Black


    // Warna tambahan
    tertiary = Color(0xFF2196F3),
    error = Color(0xFFF44336)
)

@Composable
fun HeatWafeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}