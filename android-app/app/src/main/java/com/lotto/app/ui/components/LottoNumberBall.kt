package com.lotto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lotto.app.ui.theme.getLottoBallColor
import com.lotto.app.ui.theme.NotionColors
import com.lotto.app.ui.theme.getNotionLottoBallColor

/**
 * 로또 번호 공 컴포넌트
 */
@Composable
fun LottoNumberBall(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape
            )
            .background(
                color = getLottoBallColor(number),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = (size.value / 2.5).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * 노션 스타일 로또 번호 공
 */
@Composable
fun NotionLottoNumberBall(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isSelected: Boolean = false
) {
    val backgroundColor = if (isSelected) {
        getNotionLottoBallColor(number)
    } else {
        NotionColors.Surface
    }
    
    val textColor = if (isSelected) {
        Color.White
    } else {
        getNotionLottoBallColor(number)
    }
    
    val borderColor = if (isSelected) {
        getNotionLottoBallColor(number)
    } else {
        NotionColors.Border
    }
    
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = (size.value / 2.8).sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            letterSpacing = (-0.5).sp
        )
    }
}

/**
 * 작은 로또 번호 공
 */
@Composable
fun SmallLottoNumberBall(
    number: Int,
    modifier: Modifier = Modifier
) {
    LottoNumberBall(
        number = number,
        modifier = modifier,
        size = 40.dp
    )
}

/**
 * 노션 스타일 작은 로또 번호 공
 */
@Composable
fun SmallNotionLottoNumberBall(
    number: Int,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    NotionLottoNumberBall(
        number = number,
        modifier = modifier,
        size = 32.dp,
        isSelected = isSelected
    )
}
