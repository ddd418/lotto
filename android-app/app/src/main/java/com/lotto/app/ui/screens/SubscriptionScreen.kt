package com.lotto.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lotto.app.viewmodel.SubscriptionViewModel

/**
 * PRO 구독 안내 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit,
    onSubscribed: () -> Unit
) {
    val context = LocalContext.current
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val trialInfo by viewModel.trialInfo.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    
    // 화면 진입 시 구독 상태 새로고침
    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }
    
    // 디버깅: 구독 상태 로그
    LaunchedEffect(subscriptionStatus) {
        android.util.Log.d("SubscriptionScreen", """
            📊 구독 상태:
            - trialActive: ${subscriptionStatus.trialActive}
            - trialDaysRemaining: ${subscriptionStatus.trialDaysRemaining}
            - isPro: ${subscriptionStatus.isPro}
            - subscriptionPlan: ${subscriptionStatus.subscriptionPlan}
        """.trimIndent())
    }
    
    // 이미 구독 중이면 자동으로 돌아가기
    LaunchedEffect(isProUser) {
        if (isProUser) {
            onSubscribed()
        }
    }
    
    // 체험 만료 여부 체크 (실제로 0일 이하일 때만)
    // trialDaysRemaining == -1 은 아직 로드되지 않은 상태
    val isTrialExpired = subscriptionStatus.trialDaysRemaining != -1 && // 데이터 로드됨 (초기값 아님)
                         subscriptionStatus.trialDaysRemaining <= 0 &&  // 실제 만료 (0 이하, 음수 포함)
                         !subscriptionStatus.trialActive && 
                         !subscriptionStatus.isPro
    
    android.util.Log.d("SubscriptionScreen", "❗ isTrialExpired = $isTrialExpired (days=${subscriptionStatus.trialDaysRemaining})")
    
    // 체험 만료 시 시스템 백 버튼 차단
    BackHandler(enabled = isTrialExpired) {
        // 만료된 경우 뒤로가기 차단 (아무 동작 안 함)
        android.util.Log.d("SubscriptionScreen", "⛔ 체험 만료 - 뒤로가기 차단됨")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PRO 구독") },
                navigationIcon = {
                    // 체험 만료 시에는 뒤로가기 버튼 숨김
                    if (!isTrialExpired) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로 가기")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E3A8A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                Spacer(modifier = Modifier.height(20.dp))
                
                // ⚠️ 체험 만료 긴급 메시지
                if (isTrialExpired) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🚨",
                                fontSize = 48.sp
                            )
                            Text(
                                text = "무료 체험이 종료되었습니다",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "지금 구독하고 계속 이용하세요!",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // 타이틀
                Text(
                    text = "로또연구소",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "PRO",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFE812)
                )
                
                // 체험 기간 정보 (체험 중일 때만 - N일 남음)
                if (subscriptionStatus.trialActive && subscriptionStatus.trialDaysRemaining > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFE812).copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⏰ 무료 체험 남은 기간",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = "${subscriptionStatus.trialDaysRemaining}일",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }
                
                // PRO 기능 안내
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
                            text = "✨ PRO 회원 혜택",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        
                        ProFeatureItem(
                            icon = "🚫",
                            title = "광고 제거",
                            description = "깔끔한 환경에서 앱 사용"
                        )
                        
                        ProFeatureItem(
                            icon = "♾️",
                            title = "무제한 추천",
                            description = "하루에 원하는 만큼 번호 추천"
                        )
                        
                        ProFeatureItem(
                            icon = "☁️",
                            title = "클라우드 백업",
                            description = "저장한 번호 자동 백업"
                        )
                        
                        ProFeatureItem(
                            icon = "📊",
                            title = "상세 분석",
                            description = "심화 통계 및 확률 분석"
                        )
                        
                        ProFeatureItem(
                            icon = "🎨",
                            title = "커스텀 테마",
                            description = "다양한 테마 선택 가능"
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // 가격 정보
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "월 구독료",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "₩1,000",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "커피 한 잔 가격",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "언제든 해지 가능",
                                    fontSize = 12.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 구독 버튼
                Button(
                    onClick = {
                        viewModel.startSubscription(context as Activity)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTrialExpired) {
                            Color(0xFFEF4444)  // 만료 시 빨간색 강조
                        } else {
                            Color(0xFFFFE812)
                        }
                    ),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        if (isTrialExpired) {
                            Text(
                                text = "🚀 ",
                                fontSize = 20.sp
                            )
                        }
                        Text(
                            text = when {
                                isTrialExpired -> "지금 바로 구독하기"
                                trialInfo.isActive -> "지금 PRO로 업그레이드"
                                else -> "PRO 구독 시작하기"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTrialExpired) Color.White else Color(0xFF1E3A8A)
                        )
                    }
                }
                
                // 약관
                Text(
                    text = "구독 시 자동으로 매월 결제됩니다.\nGoogle Play 스토어에서 언제든 해지할 수 있습니다.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * PRO 기능 아이템
 */
@Composable
fun ProFeatureItem(
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
            fontSize = 24.sp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E3A8A)
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                lineHeight = 18.sp
            )
        }
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(20.dp)
        )
    }
}
