package com.lotto.app.ui.components

import androidx.compose.foundation.background
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
