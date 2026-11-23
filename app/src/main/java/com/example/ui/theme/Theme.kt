package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val StudioShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
  )

@Composable
fun KamarTajTheme(
  accentHex: String = "#82091c",
  themeMode: String = "auto",
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode.lowercase()) {
      "dark" -> true
      "light" -> false
      else -> isSystemInDarkTheme()
    }

  val accent = parseHexColorOrDefault(accentHex, DefaultKamarTajMaroon)
  val secondaryAccent = accent.blendWith(Color(0xFFB45309), 0.35f)

  val colorScheme =
    if (isDark) {
      val darkPrimary = accent.blendWith(Color.White, 0.28f)
      val darkContainer = accent.blendWith(DarkCharcoalBackground, 0.55f)
      darkColorScheme(
        primary = darkPrimary,
        onPrimary = darkPrimary.contrastingOnColor(),
        primaryContainer = darkContainer,
        onPrimaryContainer = Color(0xFFF9E6E8),
        secondary = secondaryAccent.blendWith(Color.White, 0.35f),
        onSecondary = Color(0xFF1A1415),
        secondaryContainer = Color(0xFF3A2D2A),
        onSecondaryContainer = Color(0xFFF3E2DC),
        background = DarkCharcoalBackground,
        onBackground = Color(0xFFF5ECE8),
        surface = DarkCharcoalSurface,
        onSurface = Color(0xFFF5ECE8),
        surfaceVariant = DarkCharcoalVariant,
        onSurfaceVariant = Color(0xFFCBBDB9),
        outline = Color(0xFF534547),
        outlineVariant = Color(0xFF3A2F31),
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A),
      )
    } else {
      val lightContainer = accent.blendWith(Color.White, 0.86f)
      val onLightContainer = accent.blendWith(Color.Black, 0.35f)
      lightColorScheme(
        primary = accent,
        onPrimary = accent.contrastingOnColor(),
        primaryContainer = lightContainer,
        onPrimaryContainer = onLightContainer,
        secondary = secondaryAccent,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF3E4DB),
        onSecondaryContainer = Color(0xFF42281D),
        background = WarmIvoryBackground,
        onBackground = Color(0xFF1E1718),
        surface = WarmIvorySurface,
        onSurface = Color(0xFF1E1718),
        surfaceVariant = WarmIvoryVariant,
        onSurfaceVariant = Color(0xFF665759),
        outline = Color(0xFFD1C2BC),
        outlineVariant = Color(0xFFEAE0DC),
        error = Color(0xFFDC2626),
        onError = Color.White,
      )
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = StudioTypography,
    shapes = StudioShapes,
    content = content,
  )
}
