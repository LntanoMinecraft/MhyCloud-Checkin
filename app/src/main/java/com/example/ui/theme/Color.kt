package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.pow

// Expressive Indigo-Violet Fallback Palette (API < 31 or Dynamic Color Off without seed)
val IndigoPrimaryLight = Color(0xFF4338CA)
val OnIndigoPrimaryLight = Color(0xFFFFFFFF)
val IndigoPrimaryContainerLight = Color(0xFFE0E7FF)
val OnIndigoPrimaryContainerLight = Color(0xFF1E1B4B)

val VioletSecondaryLight = Color(0xFF6D28D9)
val OnVioletSecondaryLight = Color(0xFFFFFFFF)
val VioletSecondaryContainerLight = Color(0xFFEDE9FE)
val OnVioletSecondaryContainerLight = Color(0xFF2E1065)

val CyanTertiaryLight = Color(0xFF0E7490)
val OnCyanTertiaryLight = Color(0xFFFFFFFF)
val CyanTertiaryContainerLight = Color(0xFFCFFAFE)
val OnCyanTertiaryContainerLight = Color(0xFF083344)

val BackgroundLight = Color(0xFFF8F9FC)
val SurfaceLight = Color(0xFFFDFDFF)
val SurfaceContainerLowLight = Color(0xFFF3F4FA)
val SurfaceContainerLight = Color(0xFFECEEF7)
val SurfaceContainerHighLight = Color(0xFFE4E7F2)
val OnSurfaceLight = Color(0xFF191B24)
val OnSurfaceVariantLight = Color(0xFF454857)
val OutlineLight = Color(0xFF767989)
val OutlineVariantLight = Color(0xFFC6C8D6)

// Dark Expressive Palette
val IndigoPrimaryDark = Color(0xFFBFC4FF)
val OnIndigoPrimaryDark = Color(0xFF18155B)
val IndigoPrimaryContainerDark = Color(0xFF312E81)
val OnIndigoPrimaryContainerDark = Color(0xFFE0E7FF)

val VioletSecondaryDark = Color(0xFFD5BBFF)
val OnVioletSecondaryDark = Color(0xFF3D0C87)
val VioletSecondaryContainerDark = Color(0xFF5421A3)
val OnVioletSecondaryContainerDark = Color(0xFFEDE9FE)

val CyanTertiaryDark = Color(0xFF67E8F9)
val OnCyanTertiaryDark = Color(0xFF003642)
val CyanTertiaryContainerDark = Color(0xFF004F60)
val OnCyanTertiaryContainerDark = Color(0xFFCFFAFE)

val BackgroundDark = Color(0xFF11121A)
val SurfaceDark = Color(0xFF14151E)
val SurfaceContainerLowDark = Color(0xFF1B1D28)
val SurfaceContainerDark = Color(0xFF20222F)
val SurfaceContainerHighDark = Color(0xFF2A2C3B)
val OnSurfaceDark = Color(0xFFE6E7F2)
val OnSurfaceVariantDark = Color(0xFFC5C7D6)
val OutlineDark = Color(0xFF8F92A2)
val OutlineVariantDark = Color(0xFF454857)

@Immutable
data class ThemeSeedPreset(
  val name: String,
  val hex: String,
  val color: Color,
)

val ThemeSeedPresets = listOf(
  ThemeSeedPreset(name = "星辰紫", hex = "#6750A4", color = Color(0xFF6750A4)),
  ThemeSeedPreset(name = "琉璃蓝", hex = "#2563EB", color = Color(0xFF2563EB)),
  ThemeSeedPreset(name = "翡翠绿", hex = "#059669", color = Color(0xFF059669)),
  ThemeSeedPreset(name = "琥珀金", hex = "#D97706", color = Color(0xFFD97706)),
  ThemeSeedPreset(name = "绯樱红", hex = "#E11D48", color = Color(0xFFE11D48)),
  ThemeSeedPreset(name = "青黛青", hex = "#0D9488", color = Color(0xFF0D9488)),
  ThemeSeedPreset(name = "赤霞橙", hex = "#EA580C", color = Color(0xFFEA580C)),
)

fun parseThemeSeedColorOrNull(hex: String?): Color? {
  if (hex.isNullOrBlank()) return null
  val cleaned = hex.trim().removePrefix("#")
  val value = cleaned.toLongOrNull(16) ?: return null
  return when (cleaned.length) {
    6 -> Color((0xFF000000L or value).toInt())
    8 -> Color(value.toInt())
    else -> null
  }
}

/**
 * Maps an MD3 perceptual Tone (CIE L* in 0..100) to WCAG relative luminance Y in 0..1,
 * then binary-searches HSL lightness at fixed (hue, saturation) so colors remain vivid and
 * strictly preserve MD3 contrast guarantees across all hues.
 */
private fun toneToRelativeLuminance(tone: Float): Double {
  val t = tone.coerceIn(0f, 100f).toDouble()
  return if (t <= 8.0) {
    t / 903.2962962962963
  } else {
    ((t + 16.0) / 116.0).pow(3.0)
  }
}

private fun tonalColor(hue: Float, saturation: Float, tone: Float): Color {
  if (tone <= 0.5f) return Color.Black
  if (tone >= 99.5f) return Color.White

  val targetY = toneToRelativeLuminance(tone)
  val hsl = floatArrayOf(hue.mod(360f), saturation.coerceIn(0f, 1f), 0.5f)
  var low = 0f
  var high = 1f
  repeat(14) {
    val mid = (low + high) * 0.5f
    hsl[2] = mid
    val lum = ColorUtils.calculateLuminance(ColorUtils.HSLToColor(hsl))
    if (lum < targetY) {
      low = mid
    } else {
      high = mid
    }
  }
  hsl[2] = (low + high) * 0.5f
  return Color(ColorUtils.HSLToColor(hsl))
}

fun generateSeededColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
  val baseHsl = FloatArray(3)
  ColorUtils.colorToHSL(seedColor.toArgb(), baseHsl)

  val hue = baseHsl[0]
  val baseSat = baseHsl[1].coerceAtLeast(0.35f)

  val primarySat = baseSat.coerceIn(0.45f, 0.88f)
  val secondarySat = (baseSat * 0.36f).coerceIn(0.14f, 0.34f)
  val tertiaryHue = (hue + 56f).mod(360f)
  val tertiarySat = (baseSat * 0.52f).coerceIn(0.26f, 0.58f)
  val neutralSat = (baseSat * 0.08f).coerceIn(0.03f, 0.08f)
  val neutralVariantSat = (baseSat * 0.16f).coerceIn(0.06f, 0.14f)

  return if (isDark) {
    darkColorScheme(
      primary = tonalColor(hue, primarySat, 80f),
      onPrimary = tonalColor(hue, primarySat, 20f),
      primaryContainer = tonalColor(hue, primarySat, 30f),
      onPrimaryContainer = tonalColor(hue, primarySat, 90f),
      secondary = tonalColor(hue, secondarySat, 80f),
      onSecondary = tonalColor(hue, secondarySat, 20f),
      secondaryContainer = tonalColor(hue, secondarySat, 30f),
      onSecondaryContainer = tonalColor(hue, secondarySat, 90f),
      tertiary = tonalColor(tertiaryHue, tertiarySat, 80f),
      onTertiary = tonalColor(tertiaryHue, tertiarySat, 20f),
      tertiaryContainer = tonalColor(tertiaryHue, tertiarySat, 30f),
      onTertiaryContainer = tonalColor(tertiaryHue, tertiarySat, 90f),
      background = tonalColor(hue, neutralSat, 6f),
      onBackground = tonalColor(hue, neutralSat, 90f),
      surface = tonalColor(hue, neutralSat, 6f),
      onSurface = tonalColor(hue, neutralSat, 90f),
      surfaceVariant = tonalColor(hue, neutralVariantSat, 30f),
      onSurfaceVariant = tonalColor(hue, neutralVariantSat, 80f),
      surfaceContainerLowest = tonalColor(hue, neutralSat, 4f),
      surfaceContainerLow = tonalColor(hue, neutralSat, 10f),
      surfaceContainer = tonalColor(hue, neutralSat, 12f),
      surfaceContainerHigh = tonalColor(hue, neutralSat, 17f),
      surfaceContainerHighest = tonalColor(hue, neutralSat, 22f),
      outline = tonalColor(hue, neutralVariantSat, 60f),
      outlineVariant = tonalColor(hue, neutralVariantSat, 30f),
      inverseSurface = tonalColor(hue, neutralSat, 90f),
      inverseOnSurface = tonalColor(hue, neutralSat, 20f),
      inversePrimary = tonalColor(hue, primarySat, 40f),
    )
  } else {
    lightColorScheme(
      primary = tonalColor(hue, primarySat, 40f),
      onPrimary = tonalColor(hue, primarySat, 100f),
      primaryContainer = tonalColor(hue, primarySat, 90f),
      onPrimaryContainer = tonalColor(hue, primarySat, 10f),
      secondary = tonalColor(hue, secondarySat, 40f),
      onSecondary = tonalColor(hue, secondarySat, 100f),
      secondaryContainer = tonalColor(hue, secondarySat, 90f),
      onSecondaryContainer = tonalColor(hue, secondarySat, 10f),
      tertiary = tonalColor(tertiaryHue, tertiarySat, 40f),
      onTertiary = tonalColor(tertiaryHue, tertiarySat, 100f),
      tertiaryContainer = tonalColor(tertiaryHue, tertiarySat, 90f),
      onTertiaryContainer = tonalColor(tertiaryHue, tertiarySat, 10f),
      background = tonalColor(hue, neutralSat, 99f),
      onBackground = tonalColor(hue, neutralSat, 10f),
      surface = tonalColor(hue, neutralSat, 99f),
      onSurface = tonalColor(hue, neutralSat, 10f),
      surfaceVariant = tonalColor(hue, neutralVariantSat, 90f),
      onSurfaceVariant = tonalColor(hue, neutralVariantSat, 30f),
      surfaceContainerLowest = tonalColor(hue, neutralSat, 100f),
      surfaceContainerLow = tonalColor(hue, neutralSat, 96f),
      surfaceContainer = tonalColor(hue, neutralSat, 94f),
      surfaceContainerHigh = tonalColor(hue, neutralSat, 92f),
      surfaceContainerHighest = tonalColor(hue, neutralSat, 90f),
      outline = tonalColor(hue, neutralVariantSat, 50f),
      outlineVariant = tonalColor(hue, neutralVariantSat, 80f),
      inverseSurface = tonalColor(hue, neutralSat, 20f),
      inverseOnSurface = tonalColor(hue, neutralSat, 95f),
      inversePrimary = tonalColor(hue, primarySat, 80f),
    )
  }
}

@Immutable
data class ExpressiveStatusColors(
  val successContainer: Color,
  val onSuccessContainer: Color,
  val successAccent: Color,
  val warningContainer: Color,
  val onWarningContainer: Color,
  val warningAccent: Color,
  val genshinAccent: Color,
  val genshinContainer: Color,
  val onGenshinContainer: Color,
  val zzzAccent: Color,
  val zzzContainer: Color,
  val onZzzContainer: Color,
)

val LightStatusColors = ExpressiveStatusColors(
  successContainer = Color(0xFFD1FAE5),
  onSuccessContainer = Color(0xFF064E3B),
  successAccent = Color(0xFF059669),
  warningContainer = Color(0xFFFEF3C7),
  onWarningContainer = Color(0xFF78350F),
  warningAccent = Color(0xFFD97706),
  genshinAccent = Color(0xFF0D9488),
  genshinContainer = Color(0xFFCCFBF1),
  onGenshinContainer = Color(0xFF134E4A),
  zzzAccent = Color(0xFFEA580C),
  zzzContainer = Color(0xFFFFEDD5),
  onZzzContainer = Color(0xFF7C2D12),
)

val DarkStatusColors = ExpressiveStatusColors(
  successContainer = Color(0xFF064E3B),
  onSuccessContainer = Color(0xFFA7F3D0),
  successAccent = Color(0xFF34D399),
  warningContainer = Color(0xFF78350F),
  onWarningContainer = Color(0xFFFDE68A),
  warningAccent = Color(0xFFFBBF24),
  genshinAccent = Color(0xFF2DD4BF),
  genshinContainer = Color(0xFF115E59),
  onGenshinContainer = Color(0xFF99F6E4),
  zzzAccent = Color(0xFFFB923C),
  zzzContainer = Color(0xFF7C2D12),
  onZzzContainer = Color(0xFFFED7AA),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
