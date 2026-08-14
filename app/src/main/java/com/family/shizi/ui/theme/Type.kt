package com.family.shizi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ShiziTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    headlineMedium = Typography().headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 18.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 16.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
)
