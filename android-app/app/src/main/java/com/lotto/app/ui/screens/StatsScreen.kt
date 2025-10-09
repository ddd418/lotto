package com.lotto.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lotto.app.ui.components.LoadingIndicator
import com.lotto.app.ui.components.SmallLottoNumberBall
import com.lotto.app.viewmodel.LottoViewModel
import com.lotto.app.viewmodel.UiState

/**
 * 통계 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: LottoViewModel,
    onNavigateBack: () -> Unit
) {
    val statsState by viewModel.statsState.collectAsStateWithLifecycle()
    
    // 화면 진입 시 통계 로드
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "번호 출현 통계",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = statsState) {
                is UiState.Idle, is UiState.Loading -> {
                    LoadingIndicator(message = "통계 데이터 로딩 중...")
                }
                
                is UiState.Success -> {
                    val stats = state.data
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 헤더
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "📊 출현 빈도 통계",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "1~${stats.lastDraw}회차 기준",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🏆 상위 10개 번호",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        // 차트 형태로 상위 10개 번호 표시
                        item {
                            TopNumbersChart(
                                topNumbers = stats.top10,
                                maxCount = stats.top10.firstOrNull()?.count ?: 1
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        item {
                            Text(
                                text = "📋 상세 순위",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        // 상위 10개 번호 (기존 카드 형태)
                        itemsIndexed(stats.top10) { index, topNumber ->
                            StatItemCard(
                                rank = index + 1,
                                number = topNumber.number,
                                count = topNumber.count,
                                totalDraws = stats.lastDraw
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        
                        // 안내 문구
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "💡 통계 안내",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• 과거 출현 빈도가 높다고 미래에도 자주 나온다는 보장은 없습니다.\n" +
                                              "• 모든 번호는 동일한 확률로 추첨됩니다.\n" +
                                              "• 통계는 참고용으로만 활용하세요.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
                
                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "통계를 불러올 수 없습니다",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 통계 항목 카드
 */
@Composable
fun StatItemCard(
    rank: Int,
    number: Int,
    count: Int,
    totalDraws: Int
) {
    val percentage = (count.toFloat() / totalDraws * 100)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 순위
            Text(
                text = "${rank}위",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(50.dp)
            )
            
            // 로또 번호 공
            SmallLottoNumberBall(number = number)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 출현 횟수 및 비율
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${count}회",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format("%.1f%%", percentage),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 상위 번호 차트
 */
@Composable
fun TopNumbersChart(
    topNumbers: List<com.lotto.app.data.model.TopNumber>,
    maxCount: Int
) {
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
            topNumbers.forEachIndexed { index, topNumber ->
                NumberBarChart(
                    rank = index + 1,
                    number = topNumber.number,
                    count = topNumber.count,
                    maxCount = maxCount
                )
                if (index < topNumbers.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 개별 번호 바 차트
 */
@Composable
fun NumberBarChart(
    rank: Int,
    number: Int,
    count: Int,
    maxCount: Int
) {
    val progress = if (maxCount > 0) count.toFloat() / maxCount else 0f
    
    // 애니메이션을 위한 상태
    var startAnimation by remember { mutableStateOf(false) }
    
    // 애니메이션 트리거
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) progress else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = rank * 80),
        label = "bar_progress"
    )
    
    // 순위별 색상
    val barColor = when (rank) {
        1 -> Color(0xFFFFD700) // 금색
        2 -> Color(0xFFC0C0C0) // 은색
        3 -> Color(0xFFCD7F32) // 동색
        else -> MaterialTheme.colorScheme.primary
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위 배지
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (rank <= 3) barColor else MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 번호
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 진행 바
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 애니메이션 바
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(16.dp))
                    .background(barColor.copy(alpha = 0.7f))
            )
            
            // 출현 횟수 텍스트
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${count}회",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
