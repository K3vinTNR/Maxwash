package org.umn.maxwash.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun washStyle(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = washStyle(36, 42, FontWeight.Bold),
    displayMedium = washStyle(32, 38, FontWeight.Bold),
    displaySmall = washStyle(28, 34, FontWeight.Bold),
    headlineLarge = washStyle(28, 34, FontWeight.Bold),
    headlineMedium = washStyle(24, 30, FontWeight.Bold),
    headlineSmall = washStyle(22, 28, FontWeight.Bold),
    titleLarge = washStyle(20, 26, FontWeight.SemiBold),
    titleMedium = washStyle(16, 22, FontWeight.SemiBold),
    titleSmall = washStyle(14, 20, FontWeight.SemiBold),
    bodyLarge = washStyle(16, 23), bodyMedium = washStyle(14, 20),
    bodySmall = washStyle(12, 17), labelLarge = washStyle(14, 20, FontWeight.SemiBold),
    labelMedium = washStyle(12, 16, FontWeight.SemiBold),
    labelSmall = washStyle(10, 14, FontWeight.SemiBold)
)
