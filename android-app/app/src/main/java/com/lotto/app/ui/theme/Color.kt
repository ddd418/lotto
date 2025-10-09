package com.lotto.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors
val LottoPurple = Color(0xFF6750A4)
val LottoPurpleLight = Color(0xFF9A82DB)
val LottoPurpleDark = Color(0xFF4F378B)

// Secondary Colors
val LottoGold = Color(0xFFFFB74D)
val LottoGoldLight = Color(0xFFFFE57F)
val LottoGoldDark = Color(0xFFF57C00)

// Number Ball Colors (로또 공 색상)
val BallYellow = Color(0xFFFFC107)  // 1~10
val BallBlue = Color(0xFF2196F3)    // 11~20
val BallRed = Color(0xFFF44336)     // 21~30
val BallGray = Color(0xFF9E9E9E)    // 31~40
val BallGreen = Color(0xFF4CAF50)   // 41~45

// Background Colors
val BackgroundLight = Color(0xFFFFFBFE)
val BackgroundDark = Color(0xFF1C1B1F)

// Surface Colors
val SurfaceLight = Color(0xFFF5F5F5)
val SurfaceDark = Color(0xFF2B2930)

// Text Colors
val TextPrimary = Color(0xFF1C1B1F)
val TextSecondary = Color(0xFF49454F)
val TextOnPrimary = Color(0xFFFFFFFF)

/**
 * 로또 번호에 따라 공 색상 반환
 */
fun getLottoBallColor(number: Int): Color {
    return when (number) {
        in 1..10 -> BallYellow
        in 11..20 -> BallBlue
        in 21..30 -> BallRed
        in 31..40 -> BallGray
        else -> BallGreen
    }
}
