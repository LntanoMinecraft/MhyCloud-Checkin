package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Expressive Indigo-Violet Fallback Palette (API < 31 or Dynamic Color Off)
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
