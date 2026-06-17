package com.example.eggtimer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.eggtimer.R

val EterminationSans = FontFamily(
    Font(R.font.eterminationsans)
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = EterminationSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = EterminationSans,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = EterminationSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    displayLarge = TextStyle(fontFamily = EterminationSans),
    displayMedium = TextStyle(fontFamily = EterminationSans),
    displaySmall = TextStyle(fontFamily = EterminationSans),
    headlineLarge = TextStyle(fontFamily = EterminationSans),
    headlineMedium = TextStyle(fontFamily = EterminationSans),
    headlineSmall = TextStyle(fontFamily = EterminationSans),
    titleMedium = TextStyle(fontFamily = EterminationSans),
    titleSmall = TextStyle(fontFamily = EterminationSans),
    bodyMedium = TextStyle(fontFamily = EterminationSans),
    bodySmall = TextStyle(fontFamily = EterminationSans),
    labelLarge = TextStyle(fontFamily = EterminationSans),
    labelMedium = TextStyle(fontFamily = EterminationSans)
)