package com.senda.lecturabiblica.ui

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val sendaShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
)

fun sendaTypography(scale: Float) = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = (38 * scale).sp, lineHeight = (44 * scale).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = (32 * scale).sp, lineHeight = (38 * scale).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = (27 * scale).sp, lineHeight = (33 * scale).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (21 * scale).sp, lineHeight = (27 * scale).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (16 * scale).sp, lineHeight = (22 * scale).sp),
    bodyLarge = TextStyle(fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp),
    bodyMedium = TextStyle(fontSize = (14 * scale).sp, lineHeight = (21 * scale).sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (12 * scale).sp, lineHeight = (17 * scale).sp),
)
