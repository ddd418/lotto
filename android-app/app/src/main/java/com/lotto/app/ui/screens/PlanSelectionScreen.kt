package com.lotto.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lotto.app.ui.theme.NotionColors
import com.lotto.app.viewmodel.SubscriptionViewModel

/**
 * 첫 가입자를 위한 플랜 선택 화면
 */
@Composable
fun PlanSelectionScreen(
    onFreePlanSelected: () -> Unit,
    onProPlanSelected: () -> Unit,
    subscriptionViewModel: SubscriptionViewModel,
    activity: Activity
) {
    var selectedPlan by remember { mutableStateOf<String?>(null) }
    val isProUser by subscriptionViewModel.isProUser.collectAsStateWithLifecycle()
    
    // PRO 구독 완료 시 자동으로 메인으로 이동
    LaunchedEffect(isProUser) {
        if (isProUser) {
            onFreePlanSelected() // 메인 화면으로 이동 (트라이얼은 서버에서 자동 처리)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NotionColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // 헤더
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🎱",
                    fontSize = 64.sp
                )
                Text(
                    text = "로또연구소에 오신 것을\n환영합니다!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NotionColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "시작하기 전에 플랜을 선택해주세요",
                    fontSize = 15.sp,
                    color = NotionColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 무료 플랜 카드
            PlanCard(
                title = "무료 플랜",
                subtitle = "30일 무료 체험",
                price = "무료",
                features = listOf(
                    "번호 추천 알고리즘" to true,
                    "고급 분석 리포트" to true,
                    "내 번호 저장 기능" to true,
                    "광고 제거" to false
                ),
                isSelected = selectedPlan == "free",
                isPremium = false,
                onClick = { selectedPlan = "free" }
            )
            
            // 프로 플랜 카드
            PlanCard(
                title = "프로 플랜",
                subtitle = "자동 갱신 구독",
                price = "₩1,000/월",
                features = listOf(
                    "번호 추천 알고리즘" to true,
                    "고급 분석 리포트" to true,
                    "내 번호 저장 기능" to true,
                    "광고 없음" to true,
                    "언제든지 취소 가능" to true
                ),
                isSelected = selectedPlan == "pro",
                isPremium = true,
                onClick = { selectedPlan = "pro" }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 시작하기 버튼
            Button(
                onClick = {
                    when (selectedPlan) {
                        "free" -> onFreePlanSelected()
                        "pro" -> {
                            // 바로 Google Play 결제 시작
                            subscriptionViewModel.startSubscription(activity)
                        }
                    }
                },
                enabled = selectedPlan != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when (selectedPlan) {
                        "free" -> "30일 무료로 시작하기"
                        "pro" -> "₩1,000/월 구독하기"
                        else -> "플랜을 선택해주세요"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // 결제 안내 문구
            if (selectedPlan == "pro") {
                Text(
                    text = "• Google Play를 통해 안전하게 결제됩니다\n• 매달 자동으로 갱신되며, 언제든지 취소 가능합니다",
                    fontSize = 12.sp,
                    color = NotionColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    price: String,
    features: List<Pair<String, Boolean>>,
    isSelected: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected && isPremium -> Color(0xFF6366F1)
        isSelected -> Color(0xFF10B981)
        else -> NotionColors.Border
    }
    
    val backgroundColor = when {
        isSelected && isPremium -> Color(0xFF6366F1).copy(alpha = 0.05f)
        isSelected -> Color(0xFF10B981).copy(alpha = 0.05f)
        else -> Color.White
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NotionColors.TextPrimary
                        )
                        if (isPremium) {
                            Text(
                                text = "추천",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFF6366F1),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = NotionColors.TextSecondary
                    )
                }
                
                // 선택 인디케이터
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isPremium) Color(0xFF6366F1) else Color(0xFF10B981)
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // 가격
            Text(
                text = price,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPremium) Color(0xFF6366F1) else Color(0xFF10B981)
            )
            
            Divider(color = NotionColors.Border)
            
            // 기능 목록
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                features.forEach { (feature, available) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (available) "✓" else "✗",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (available) {
                                if (isPremium) Color(0xFF6366F1) else Color(0xFF10B981)
                            } else {
                                Color(0xFFBDBDBD)
                            }
                        )
                        Text(
                            text = feature,
                            fontSize = 14.sp,
                            color = if (available) NotionColors.TextPrimary else NotionColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
