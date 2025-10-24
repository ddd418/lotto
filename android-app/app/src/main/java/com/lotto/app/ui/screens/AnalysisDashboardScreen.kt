package com.lotto.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lotto.app.data.model.DashboardResponse
import com.lotto.app.ui.viewmodel.DashboardUiState
import com.lotto.app.ui.viewmodel.DashboardViewModel
import com.lotto.app.viewmodel.SubscriptionViewModel
import kotlin.math.roundToInt

/**
 * 로또 당첨번호 분석 대시보드 화면 (실제 DB 데이터 기반)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDashboardScreen(
    onNavigateBack: () -> Unit,
    subscriptionViewModel: SubscriptionViewModel,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentDraws by viewModel.recentDraws.collectAsState()
    
    // 드롭다운 메뉴 상태
    var expanded by remember { mutableStateOf(false) }
    val drawsOptions = listOf(10, 20, 30, 50)
    
    // 화면 진입 시 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "📊 당첨번호 통계 분석",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                actions = {
                    // 회차 선택 드롭다운
                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "최근 ${recentDraws}회차",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            drawsOptions.forEach { draws ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "최근 ${draws}회차",
                                            fontWeight = if (draws == recentDraws) FontWeight.Bold else FontWeight.Normal,
                                            color = if (draws == recentDraws) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setRecentDraws(draws)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    IconButton(onClick = { viewModel.retry() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("통계 데이터 로딩 중...")
                    }
                }
            }
            
            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as DashboardUiState.Error).message,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("재시도")
                        }
                    }
                }
            }
            
            is DashboardUiState.Success -> {
                val data = (uiState as DashboardUiState.Success).data
                DashboardContent(
                    data = data,
                    recentDraws = recentDraws,
                    paddingValues = paddingValues
                )
            }
        }
    }
}

/**
 * 대시보드 컨텐츠
 */
@Composable
fun DashboardContent(
    data: DashboardResponse,
    recentDraws: Int,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 요약 카드
        DashboardSummaryCard(data)
        
        // 핫 번호 Top 10 (최근 N회차 빈도 사용)
        HotNumbersCard(data.hotNumbers, data.recentFrequency, recentDraws)
        
        // 콜드 번호 (최근 N회차 빈도 사용)
        ColdNumbersCard(data.coldNumbers, data.recentFrequency, recentDraws)
        
        // 짝수/홀수 비율
        DashboardEvenOddCard(data.evenOddRatio)
        
        // 십의 자리 분포
        DecadeDistributionCard(data.decadeDistribution)
        
        // 추가 통계
        AdditionalDashboardStatsCard(data)
    }
}

/**
 * 요약 카드
 */
@Composable
fun DashboardSummaryCard(data: DashboardResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📈 전체 통계",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("분석 회차", "${data.totalDraws}회")
                SummaryItem("평균 합계", "${data.sumRange["avg"] ?: 0}")
                SummaryItem("합계 범위", "${data.sumRange["min"]}-${data.sumRange["max"]}")
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * 핫 번호 카드 (최근 N회차 기준)
 */
@Composable
fun HotNumbersCard(hotNumbers: List<Int>, frequency: List<com.lotto.app.data.model.NumberFrequency>, recentDraws: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🔥 핫 번호 Top 10 (최근 ${recentDraws}회차)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 번호들을 그리드로 표시
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hotNumbers.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { number ->
                            val freq = frequency.find { it.number == number }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFF5722)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = number.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    freq?.let {
                                        Text(
                                            text = "${it.count}회",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 콜드 번호 카드 (최근 N회차 기준)
 */
@Composable
fun ColdNumbersCard(coldNumbers: List<Int>, frequency: List<com.lotto.app.data.model.NumberFrequency>, recentDraws: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "❄️ 콜드 번호 Top 10 (최근 ${recentDraws}회차)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 번호들을 그리드로 표시
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                coldNumbers.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { number ->
                            val freq = frequency.find { it.number == number }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2196F3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = number.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    freq?.let {
                                        Text(
                                            text = "${it.count}회",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 짝수/홀수 비율 카드
 */
@Composable
fun DashboardEvenOddCard(evenOddRatio: Map<String, Float>) {
    val evenPercent = evenOddRatio["even"]?.roundToInt() ?: 0
    val oddPercent = evenOddRatio["odd"]?.roundToInt() ?: 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🎯 짝수/홀수 비율",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 짝수
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$evenPercent%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "짝수",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Text(
                    text = ":",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 홀수
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$oddPercent%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        text = "홀수",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 십의 자리 분포 카드
 */
@Composable
fun DecadeDistributionCard(decadeDistribution: List<com.lotto.app.data.model.DecadeDistribution>) {
    val maxCount = decadeDistribution.maxOfOrNull { it.count } ?: 1
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📍 십의 자리 분포",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            decadeDistribution.forEach { decade ->
                DecadeBar(decade.decade, decade.count, decade.percentage, maxCount)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DecadeBar(range: String, count: Int, percentage: Float, maxCount: Int) {
    val progress = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = range,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary)
            )
            
            Text(
                text = "${count}개 (${percentage.roundToInt()}%)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }
}

/**
 * 추가 통계 카드
 */
@Composable
fun AdditionalDashboardStatsCard(data: DashboardResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "💡 연속번호 통계",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 연속번호 통계 (더 명확하게 표시)
            val consecutiveLabels = mapOf(
                "none" to "연속 없음",
                "two" to "2개 연속 (예: 5,6)",
                "three" to "3개 연속 (예: 5,6,7)",
                "four_plus" to "4개 이상 연속"
            )
            
            val consecutiveOrder = listOf("none", "two", "three", "four_plus")
            consecutiveOrder.forEach { key ->
                val freq = data.consecutiveCount[key] ?: 0
                val label = consecutiveLabels[key] ?: key
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "${freq}회",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // 합계 범위
            Text(
                text = "📊 당첨번호 합계",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "최소: ${data.sumRange["min"]}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "평균: ${data.sumRange["avg"]}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "최대: ${data.sumRange["max"]}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
