package com.lotto.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 노션 스타일 색상 팔레트
 */
object NotionColors {
    // 기본 색상
    val Background = Color(0xFFFFFFFF)
    val BackgroundDark = Color(0xFF191919)
    
    // 그레이 스케일 (노션 스타일)
    val Gray50 = Color(0xFFFAFAFA)
    val Gray100 = Color(0xFFF5F5F5)
    val Gray200 = Color(0xFFEBEBEB)
    val Gray300 = Color(0xFFD1D1D1)
    val Gray400 = Color(0xFFB8B8B8)
    val Gray500 = Color(0xFF9E9E9E)
    val Gray600 = Color(0xFF6B6B6B)
    val Gray700 = Color(0xFF4A4A4A)
    val Gray800 = Color(0xFF2E2E2E)
    val Gray900 = Color(0xFF1A1A1A)
    
    // 텍스트 색상
    val TextPrimary = Color(0xFF37352F)
    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF6B6B6B)
    val TextSecondaryDark = Color(0xFFA3A3A3)
    val TextTertiary = Color(0xFF9E9E9E)
    val TextTertiaryDark = Color(0xFF707070)
    
    // 색상 변형 (추가)
    val Red100 = Color(0xFFFFEBEE)
    val Blue50 = Color(0xFFE3F2FD)
    
    // 아센트 색상 (노션 블루)
    val Primary = Color(0xFF2383E2)
    val PrimaryVariant = Color(0xFF1A6BC7)
    val Secondary = Color(0xFF6B73FF)
    
    // 상태 색상
    val Success = Color(0xFF0F7B0F)
    val Warning = Color(0xFFE16259)
    val Error = Color(0xFFE03E3E)
    val Info = Color(0xFF0F7B0F)
    
    // 카드와 서피스
    val Surface = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF202020)
    val SurfaceVariant = Color(0xFFF7F6F3)
    val SurfaceVariantDark = Color(0xFF2A2A2A)
    
    // 테두리
    val Border = Color(0xFFEBEBEB)
    val BorderDark = Color(0xFF373737)
    val Divider = Color(0xFFF1F1F0)
    val DividerDark = Color(0xFF333333)
    
    // 특별한 색상
    val Highlight = Color(0xFFFEF3C7)
    val HighlightDark = Color(0xFF3A3A1F)
    val Selection = Color(0xFFE3F2FD)
    val SelectionDark = Color(0xFF1F2937)
}

/**
 * 노션 스타일 로또 번호 색상
 */
fun getNotionLottoBallColor(number: Int): Color {
    return when (number) {
        in 1..9 -> Color(0xFFE16259)    // 따뜻한 빨강
        in 10..19 -> Color(0xFFD9730D)  // 오렌지
        in 20..29 -> Color(0xFF0F7B0F)  // 초록
        in 30..39 -> Color(0xFF2383E2)  // 노션 블루
        in 40..45 -> Color(0xFF6B73FF)  // 보라
        else -> Color(0xFF6B6B6B)       // 기본 그레이
    }
}