package com.lotto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 온보딩 화면 - 첫 실행 시 표시
 */
@Composable
fun OnboardingScreen(
    onStartTrial: () -> Unit,
    onStartPro: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A),
                        Color(0xFF3B82F6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // 로고
            Text(
                text = "🔬",
                fontSize = 80.sp
            )
            
            Text(
                text = "로또연구소",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "AI 기반 데이터 분석으로\n최적의 번호를 찾아보세요",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 주요 기능
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "✨ 주요 기능",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    
                    FeatureItem(
                        icon = "🤖",
                        title = "AI 번호 추천",
                        description = "4가지 전략 기반 번호 분석"
                    )
                    
                    FeatureItem(
                        icon = "📊",
                        title = "통계 분석",
                        description = "과거 출현 빈도 기반 데이터"
                    )
                    
                    FeatureItem(
                        icon = "🏆",
                        title = "당첨 확인",
                        description = "저장한 번호 자동 당첨 체크"
                    )
                    
                    FeatureItem(
                        icon = "💾",
                        title = "번호 저장",
                        description = "관심 번호 메모와 함께 저장"
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 무료 체험 시작 버튼
            Button(
                onClick = onStartTrial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFE812)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "30일 무료 체험 시작하기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "이후 월 1,000원",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A8A).copy(alpha = 0.7f)
                    )
                }
            }
            
            // PRO로 바로 시작
            OutlinedButton(
                onClick = onStartPro,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, Color.White)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "PRO로 바로 시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // 건너뛰기
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "나중에 하기",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * 기능 아이템
 */
@Composable
fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E3A8A)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                lineHeight = 20.sp
            )
        }
    }
}
