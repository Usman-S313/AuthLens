package com.authlens.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val default = Typography()

val AppTypography = Typography(
    displayLarge = default.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = default.displayMedium.copy(fontWeight = FontWeight.Bold),
    headlineLarge = default.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = default.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = default.bodyLarge.copy(lineHeight = 24.sp),
    labelLarge = default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
