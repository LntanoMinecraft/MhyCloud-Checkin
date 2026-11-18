package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.ThemePreference

private val ExpressiveLightColorScheme = lightColorScheme(
  primary = IndigoPrimaryLight,
  onPrimary = OnIndigoPrimaryLight,
  primaryContainer = IndigoPrimaryContainerLight,
  onPrimaryContainer = OnIndigoPrimaryContainerLight,
  secondary = VioletSecondaryLight,
  onSecondary = OnVioletSecondaryLight,
  secondaryContainer = VioletSecondaryContainerLight,
  onSecondaryContainer = OnVioletSecondaryContainerLight,
  tertiary = CyanTertiaryLight,
  onTertiary = OnCyanTertiaryLight,
  tertiaryContainer = CyanTertiaryContainerLight,
  onTertiaryContainer = OnCyanTertiaryContainerLight,
  background = BackgroundLight,
  onBackground = OnSurfaceLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceContainerLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  surfaceContainerLow = SurfaceContainerLowLight,
  surfaceContainer = SurfaceContainerLight,
  surfaceContainerHigh = SurfaceContainerHighLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,
)

private val ExpressiveDarkColorScheme = darkColorScheme(
  primary = IndigoPrimaryDark,
  onPrimary = OnIndigoPrimaryDark,
  primaryContainer = IndigoPrimaryContainerDark,
  onPrimaryContainer = OnIndigoPrimaryContainerDark,
  secondary = VioletSecondaryDark,
  onSecondary = OnVioletSecondaryDark,
  secondaryContainer = VioletSecondaryContainerDark,
  onSecondaryContainer = OnVioletSecondaryContainerDark,
  tertiary = CyanTertiaryDark,
  onTertiary = OnCyanTertiaryDark,
  tertiaryContainer = CyanTertiaryContainerDark,
  onTertiaryContainer = OnCyanTertiaryContainerDark,
  background = BackgroundDark,
  onBackground = OnSurfaceDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceContainerDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  surfaceContainerLow = SurfaceContainerLowDark,
  surfaceContainer = SurfaceContainerDark,
  surfaceContainerHigh = SurfaceContainerHighDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,
)

val ExpressiveShapes = Shapes(
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CloudCheckinTheme(
  themePreference: ThemePreference = ThemePreference.SYSTEM,
  useDynamicColor: Boolean = true,
  themeSeedColorHex: String? = null,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark = when (themePreference) {
    ThemePreference.SYSTEM -> systemDark
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
  }

  val seedColor = parseThemeSeedColorOrNull(themeSeedColorHex)
  val colorScheme = when {
    useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    seedColor != null -> generateSeededColorScheme(seedColor = seedColor, isDark = isDark)
    isDark -> ExpressiveDarkColorScheme
    else -> ExpressiveLightColorScheme
  }

  val statusColors = if (isDark) DarkStatusColors else LightStatusColors

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
      }
    }
  }

  CompositionLocalProvider(LocalStatusColors provides statusColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = ExpressiveTypography,
      shapes = ExpressiveShapes,
      content = content,
    )
  }
}
