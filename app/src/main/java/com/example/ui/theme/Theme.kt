package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
  primary = PoliceRedPrimary,
  onPrimary = Color.White,
  primaryContainer = PoliceRedContainer,
  onPrimaryContainer = PoliceRedOnContainer,
  secondary = PoliceGold,
  onSecondary = Color.White,
  secondaryContainer = PoliceGoldContainer,
  onSecondaryContainer = PoliceGoldOnContainer,
  tertiary = LeaderCyan,
  onTertiary = Color.White,
  background = SlateBackground,
  onBackground = SlateTextPrimary,
  surface = SlateSurface,
  onSurface = SlateTextPrimary,
  surfaceVariant = SlateSurfaceVariant,
  onSurfaceVariant = SlateTextSecondary,
  outline = SlateBorder,
  error = Color(0xFFDC2626),
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFFF87171),
  onPrimary = Color(0xFF450A0A),
  primaryContainer = PoliceRedDark,
  onPrimaryContainer = Color(0xFFFEE2E2),
  secondary = PoliceGoldLight,
  onSecondary = Color(0xFF451A03),
  secondaryContainer = Color(0xFF78350F),
  onSecondaryContainer = Color(0xFFFEF3C7),
  tertiary = Color(0xFF22D3EE),
  onTertiary = Color(0xFF083344),
  background = Color(0xFF0F172A),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF1E293B),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF475569),
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve authoritative Police red/gold scheme
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
