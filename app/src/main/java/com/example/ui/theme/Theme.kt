package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioDarkColorScheme = darkColorScheme(
  primary = StudioAmber,
  onPrimary = Color.Black,
  primaryContainer = StudioAmberDim,
  onPrimaryContainer = Color.White,
  secondary = StudioCyan,
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF0E7490),
  onSecondaryContainer = Color.White,
  tertiary = StudioEmerald,
  onTertiary = Color.Black,
  background = StudioVoid,
  onBackground = TextPrimary,
  surface = StudioSurface,
  onSurface = TextPrimary,
  surfaceVariant = StudioSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = StudioBorder,
  error = StudioCrimson,
  onError = Color.White
)

@Composable
fun GarageStudioTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = StudioDarkColorScheme,
    typography = Typography,
    content = content
  )
}
