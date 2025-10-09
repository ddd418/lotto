package com.lotto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lotto.app.ui.theme.NotionColors

/**
 * 노션 스타일 카드
 */
@Composable
fun NotionCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NotionColors.Surface,
    borderColor: Color = NotionColors.Border,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * 노션 스타일 섹션 헤더
 */
@Composable
fun NotionSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: String? = null
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Text(
                    text = icon,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = NotionColors.TextPrimary,
                letterSpacing = (-0.5).sp
            )
        }
        
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = NotionColors.TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * 노션 스타일 버튼
 */
@Composable
fun NotionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NotionButtonVariant = NotionButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: String? = null
) {
    val (backgroundColor, textColor, borderColor) = when (variant) {
        NotionButtonVariant.Primary -> Triple(
            NotionColors.Primary,
            Color.White,
            NotionColors.Primary
        )
        NotionButtonVariant.Secondary -> Triple(
            NotionColors.Surface,
            NotionColors.TextPrimary,
            NotionColors.Border
        )
        NotionButtonVariant.Ghost -> Triple(
            Color.Transparent,
            NotionColors.TextSecondary,
            Color.Transparent
        )
    }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(36.dp)
            .border(
                width = 1.dp,
                color = if (enabled) borderColor else NotionColors.Gray300,
                shape = RoundedCornerShape(6.dp)
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) backgroundColor else NotionColors.Gray100,
            contentColor = if (enabled) textColor else NotionColors.Gray400
        ),
        shape = RoundedCornerShape(6.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Text(
                    text = leadingIcon,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class NotionButtonVariant {
    Primary,
    Secondary,
    Ghost
}

/**
 * 노션 스타일 구분선
 */
@Composable
fun NotionDivider(
    modifier: Modifier = Modifier,
    color: Color = NotionColors.Divider
) {
    Divider(
        modifier = modifier,
        thickness = 1.dp,
        color = color
    )
}

/**
 * 노션 스타일 뱃지
 */
@Composable
fun NotionBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NotionColors.Gray100,
    textColor: Color = NotionColors.TextSecondary
) {
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}