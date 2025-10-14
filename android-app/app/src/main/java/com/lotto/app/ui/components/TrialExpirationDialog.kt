package com.lotto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lotto.app.ui.theme.NotionColors

/**
 * 무료 체험 종료 임박 알림 다이얼로그
 */
@Composable
fun TrialExpirationDialog(
    daysRemaining: Int,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 아이콘
                Text(
                    text = when {
                        daysRemaining <= 2 -> "🚨"
                        daysRemaining <= 5 -> "⏰"
                        else -> "📢"
                    },
                    fontSize = 64.sp
                )
                
                // 제목
                Text(
                    text = "무료 체험 종료 임박",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NotionColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                
                // 메시지
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "무료 체험 기간이",
                        fontSize = 16.sp,
                        color = NotionColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "${daysRemaining}일",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            daysRemaining <= 2 -> Color(0xFFEF4444)
                            daysRemaining <= 5 -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }
                    )
                    
                    Text(
                        text = "남았습니다",
                        fontSize = 16.sp,
                        color = NotionColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                
                Divider(color = NotionColors.Border)
                
                // 안내 메시지
                Text(
                    text = when {
                        daysRemaining <= 2 -> "체험 종료 후에는 고급 분석 리포트와 광고 제거 기능을 사용할 수 없습니다."
                        daysRemaining <= 5 -> "프로 플랜으로 업그레이드하여 계속 모든 기능을 이용하세요."
                        else -> "지금 프로 플랜으로 업그레이드하면 계속해서 모든 기능을 제한 없이 이용할 수 있습니다."
                    },
                    fontSize = 14.sp,
                    color = NotionColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 버튼
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "프로 플랜 구독하기",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            "나중에",
                            fontSize = 15.sp,
                            color = NotionColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
