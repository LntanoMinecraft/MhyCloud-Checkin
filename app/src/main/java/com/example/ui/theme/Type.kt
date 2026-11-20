package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexFontFamily = FontFamily(
  Font(
    resId = R.font.google_sans_flex,
    weight = FontWeight.Normal,
    variationSettings = FontVariation.Settings(
      FontVariation.weight(400),
      FontVariation.Setting("ROND", 100f),
    ),
  ),
  Font(
    resId = R.font.google_sans_flex,
    weight = FontWeight.Medium,
    variationSettings = FontVariation.Settings(
      FontVariation.weight(500),
      FontVariation.Setting("ROND", 100f),
    ),
  ),
  Font(
    resId = R.font.google_sans_flex,
    weight = FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(
      FontVariation.weight(600),
      FontVariation.Setting("ROND", 100f),
    ),
  ),
  Font(
    resId = R.font.google_sans_flex,
    weight = FontWeight.Bold,
    variationSettings = FontVariation.Settings(
      FontVariation.weight(700),
      FontVariation.Setting("ROND", 100f),
    ),
  ),
)

val JetBrainsMonoFontFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
)

val ExpressiveTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
  ),
  headlineMedium = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.25).sp,
    fontFeatureSettings = "tnum",
  ),
  headlineSmall = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    fontFeatureSettings = "tnum",
  ),
  titleLarge = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontFeatureSettings = "tnum",
  ),
  titleMedium = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.1.sp,
    fontFeatureSettings = "tnum",
  ),
  bodyLarge = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    fontFeatureSettings = "tnum",
  ),
  bodyMedium = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp,
    lineHeight = 20.sp,
    fontFeatureSettings = "tnum",
  ),
  bodySmall = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum",
  ),
  labelLarge = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.5.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum",
  ),
  labelMedium = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum",
  ),
  labelSmall = TextStyle(
    fontFamily = GoogleSansFlexFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 15.sp,
    fontFeatureSettings = "tnum",
  ),
)
