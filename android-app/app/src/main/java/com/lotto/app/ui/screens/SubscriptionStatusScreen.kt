package com.lotto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * 구독 상태 확인 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionStatusScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val status by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "구독 관리",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NotionColors.Background
                )
            )
        },
        containerColor = NotionColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 현재 상태 카드
            StatusCard(
                isPro = status.isPro,
                trialActive = status.trialActive,
                trialDaysRemaining = status.trialDaysRemaining,
                subscriptionEndDate = status.subscriptionEndDate
            )
            
            // 플랜 비교 테이블
            PlanComparisonCard(
                isPro = status.isPro,
                onUpgrade = onNavigateToSubscription
            )
            
            // 기능 접근 상태
            AccessStatusCard(hasAccess = status.hasAccess)
            
            // 체험 정보 (PRO 구독자가 아닐 때만 표시)
            if (!status.isPro && (status.trialActive || status.isTrialUsed)) {
                TrialInfoCard(
                    trialActive = status.trialActive,
                    trialDaysRemaining = status.trialDaysRemaining,
                    trialStartDate = status.trialStartDate,
                    trialEndDate = status.trialEndDate
                )
            }
            
            // 구독 정보 (PRO 구독자인 경우)
            if (status.isPro) {
                SubscriptionInfoCard(
                    subscriptionEndDate = status.subscriptionEndDate,
                    autoRenew = status.autoRenew,
                    onCancelSubscription = {
                        viewModel.cancelSubscription()
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    isPro: Boolean,
    trialActive: Boolean,
    trialDaysRemaining: Int,
    subscriptionEndDate: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPro -> Color(0xFF6366F1).copy(alpha = 0.1f)
                trialActive -> Color(0xFF10B981).copy(alpha = 0.1f)
                else -> Color(0xFFEF4444).copy(alpha = 0.1f)  // 체험 종료된 무료 플랜
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when {
                        isPro -> "✨"
                        trialActive -> "🎁"
                        else -> "⏰"  // 체험 종료
                    },
                    fontSize = 32.sp
                )
                
                Column {
                    Text(
                        text = when {
                            isPro -> "PRO 구독 중"
                            trialActive -> "무료 체험 중 (${trialDaysRemaining}일 남음)"
                            else -> "무료 체험 종료"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isPro -> Color(0xFF6366F1)
                            trialActive -> Color(0xFF10B981)
                            else -> Color(0xFFEF4444)
                        }
                    )
                    
                    Text(
                        text = when {
                            isPro -> subscriptionEndDate?.let { 
                                "다음 결제일: ${formatDate(it)}"
                            } ?: "구독 정보 로딩 중..."
                            trialActive -> "체험 기간이 끝나면 프로 기능이 제한됩니다"
                            else -> "프로 플랜 구독 후 모든 기능을 이용하세요"
                        },
                        fontSize = 13.sp,
                        color = NotionColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessStatusCard(hasAccess: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = NotionColors.Surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "기능 접근 권한",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NotionColors.TextPrimary
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (hasAccess) "✅" else "❌",
                    fontSize = 24.sp
                )
                Text(
                    text = if (hasAccess) {
                        "모든 프리미엄 기능을 사용할 수 있습니다"
                    } else {
                        "프리미엄 기능이 제한됩니다"
                    },
                    fontSize = 14.sp,
                    color = NotionColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TrialInfoCard(
    trialActive: Boolean,
    trialDaysRemaining: Int,
    trialStartDate: String?,
    trialEndDate: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = NotionColors.Surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎁 무료 체험 정보",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NotionColors.TextPrimary
            )
            
            if (trialActive) {
                InfoRow("상태", "체험 중")
                InfoRow("남은 기간", "${trialDaysRemaining}일")
                trialEndDate?.let {
                    InfoRow("종료일", formatDate(it))
                }
                
                if (trialDaysRemaining <= 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ 체험 기간이 곧 종료됩니다. PRO 구독을 고려해보세요!",
                        fontSize = 13.sp,
                        color = Color(0xFFFF9800),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFFF9800).copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    )
                }
            } else {
                InfoRow("상태", "체험 종료")
                trialStartDate?.let {
                    InfoRow("시작일", formatDate(it))
                }
                trialEndDate?.let {
                    InfoRow("종료일", formatDate(it))
                }
            }
        }
    }
}

@Composable
private fun SubscriptionInfoCard(
    subscriptionEndDate: String?,
    autoRenew: Boolean,
    onCancelSubscription: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = NotionColors.Surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "✨ PRO 구독 정보",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NotionColors.TextPrimary
            )
            
            InfoRow("플랜", "PRO (₩1,000/월)")
            subscriptionEndDate?.let {
                InfoRow("다음 결제일", formatDate(it))
            }
            InfoRow("자동 갱신", if (autoRenew) "활성화" else "비활성화")
            
            if (autoRenew) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NotionColors.Error
                    )
                ) {
                    Text("구독 취소")
                }
            }
        }
    }
    
    // 취소 확인 다이얼로그
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("구독 취소") },
            text = { 
                Text("정말로 구독을 취소하시겠습니까?\n\n현재 구독 기간까지는 PRO 기능을 계속 사용하실 수 있습니다.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelSubscription()
                        showCancelDialog = false
                    }
                ) {
                    Text("취소하기", color = NotionColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("돌아가기")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = NotionColors.TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NotionColors.TextPrimary
        )
    }
}

/**
 * 플랜 비교 카드
 */
@Composable
private fun PlanComparisonCard(
    isPro: Boolean,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "플랜 비교",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NotionColors.TextPrimary
            )
            
            Divider(color = NotionColors.Border)
            
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "기능",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NotionColors.TextSecondary
                )
                Text(
                    text = "무료 플랜",
                    modifier = Modifier.weight(0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NotionColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "프로 플랜",
                    modifier = Modifier.weight(0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1),
                    textAlign = TextAlign.Center
                )
            }
            
            Divider(color = NotionColors.Border)
            
            // 기능 비교 행들
            ComparisonRow("번호 추천 알고리즘", true, true)
            ComparisonRow("고급 분석 리포트", true, true)
            ComparisonRow("내 번호 저장 기능", true, true)
            ComparisonRow("프리미엄 환경", false, true)
            ComparisonRow("무료 사용 기간", "1개월", "제한 없음")
            ComparisonRow("월 구독료", "무료", "₩1,000")
            
            Divider(color = NotionColors.Border)
            
            // 업그레이드 버튼 (무료 사용자만)
            if (!isPro) {
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
                        "지금 업그레이드",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * 비교 행 (체크마크)
 */
@Composable
private fun ComparisonRow(
    feature: String,
    freeHas: Boolean,
    proHas: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = NotionColors.TextPrimary
        )
        Text(
            text = if (freeHas) "✓" else "✗",
            modifier = Modifier.weight(0.7f),
            fontSize = 18.sp,
            color = if (freeHas) Color(0xFF10B981) else Color(0xFFEF4444),
            textAlign = TextAlign.Center
        )
        Text(
            text = if (proHas) "✓" else "✗",
            modifier = Modifier.weight(0.7f),
            fontSize = 18.sp,
            color = if (proHas) Color(0xFF6366F1) else Color(0xFFEF4444),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 비교 행 (텍스트)
 */
@Composable
private fun ComparisonRow(
    feature: String,
    freeValue: String,
    proValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = NotionColors.TextPrimary
        )
        Text(
            text = freeValue,
            modifier = Modifier.weight(0.7f),
            fontSize = 13.sp,
            color = NotionColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Text(
            text = proValue,
            modifier = Modifier.weight(0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6366F1),
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        // ISO 8601 형식 시도
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
}
