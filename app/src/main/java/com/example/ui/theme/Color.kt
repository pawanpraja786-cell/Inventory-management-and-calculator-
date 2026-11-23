package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val DefaultKamarTajMaroon = Color(0xFF82091C)
val WarmIvoryBackground = Color(0xFFFAF7F4)
val WarmIvorySurface = Color(0xFFFFFFFF)
val WarmIvoryVariant = Color(0xFFF2EAE5)
val DarkCharcoalBackground = Color(0xFF151112)
val DarkCharcoalSurface = Color(0xFF201A1B)
val DarkCharcoalVariant = Color(0xFF2D2527)
val SuccessGreen = Color(0xFF15803D)
val WarningAmber = Color(0xFFB45309)

fun parseHexColorOrDefault(hex: String, fallback: Color = DefaultKamarTajMaroon): Color {
  return runCatching {
    val clean = if (hex.startsWith("#")) hex else "#$hex"
    Color(android.graphics.Color.parseColor(clean))
  }.getOrDefault(fallback)
}

fun Color.blendWith(other: Color, ratio: Float): Color {
  val inv = 1f - ratio
  return Color(
    red = red * inv + other.red * ratio,
    green = green * inv + other.green * ratio,
    blue = blue * inv + other.blue * ratio,
    alpha = 1f,
  )
}

fun Color.contrastingOnColor(): Color = if (luminance() > 0.42f) Color(0xFF1A1415) else Color.White
