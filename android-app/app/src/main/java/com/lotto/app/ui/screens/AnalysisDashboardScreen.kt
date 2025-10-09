package com.lotto.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lotto.app.data.local.SavedNumbersManager
import com.lotto.app.data.model.NumberAnalyzer
import kotlin.math.roundToInt

/**
 * 번호 분석 대시보드 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDashboardScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val savedNumbersManager = remember { SavedNumbersManager(context) }
    val savedNumbers = remember { savedNumbersManager.getSavedNumbers() }
    val analysis = remember { NumberAnalyzer.analyze(savedNumbers) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "📊 번호 분석",
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
        if (analysis.totalSets == 0) {
            // 저장된 번호가 없을 때
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📭",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "저장된 번호가 없습니다",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "번호를 저장하면 패턴 분석 결과를 볼 수 있어요",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // 분석 결과 표시
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 요약 카드
                SummaryCard(analysis)
                
                // 번호 빈도 Top 10
                FrequencyCard(
                    title = "🔥 가장 많이 선택한 번호",
                    numbers = analysis.mostFrequentNumbers,
                    isAscending = false
                )
                
                // 짝수/홀수 비율
                EvenOddCard(analysis)
                
                // 구간별 분포
                RangeDistributionCard(analysis)
                
                // 추가 통계
                AdditionalStatsCard(analysis)
            }
        }
    }
}

/**
 * 요약 카드
 */
@Composable
fun SummaryCard(analysis: com.lotto.app.data.model.NumberAnalysis) {
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
                text = "📈 전체 요약",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("저장 세트", "${analysis.totalSets}개")
                SummaryItem("평균 합계", "${analysis.averageSum.roundToInt()}")
                SummaryItem("연속 번호", "${analysis.consecutiveCount}개")
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
            fontSize = 24.sp,
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
 * 번호 빈도 카드
 */
@Composable
fun FrequencyCard(
    title: String,
    numbers: List<Pair<Int, Int>>,
    isAscending: Boolean
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
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            numbers.take(10).forEach { (number, count) ->
                FrequencyBar(number, count, numbers.maxOf { it.second })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FrequencyBar(number: Int, count: Int, maxCount: Int) {
    val progress = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 번호
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
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
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            
            Text(
                text = "${count}회",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }
}

/**
 * 짝수/홀수 비율 카드
 */
@Composable
fun EvenOddCard(analysis: com.lotto.app.data.model.NumberAnalysis) {
    val (evenCount, oddCount) = analysis.evenOddRatio
    val total = evenCount + oddCount
    val evenPercent = if (total > 0) (evenCount * 100f / total).roundToInt() else 0
    val oddPercent = if (total > 0) (oddCount * 100f / total).roundToInt() else 0
    
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
                        text = "짝수 ${evenCount}개",
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
                        text = "홀수 ${oddCount}개",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 구간별 분포 카드
 */
@Composable
fun RangeDistributionCard(analysis: com.lotto.app.data.model.NumberAnalysis) {
    val maxCount = analysis.rangeDistribution.values.maxOrNull() ?: 1
    
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
                text = "📍 구간별 분포",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            listOf("1-10", "11-20", "21-30", "31-40", "41-45").forEach { range ->
                val count = analysis.rangeDistribution[range] ?: 0
                RangeBar(range, count, maxCount)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RangeBar(range: String, count: Int, maxCount: Int) {
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
                text = "${count}개",
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
fun AdditionalStatsCard(analysis: com.lotto.app.data.model.NumberAnalysis) {
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
                text = "💡 분석 인사이트",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val (evenCount, oddCount) = analysis.evenOddRatio
            val total = evenCount + oddCount
            val evenPercent = if (total > 0) (evenCount * 100 / total) else 0
            
            InsightRow(
                "• 평균 번호 합계: ${analysis.averageSum.roundToInt()} (권장: 90~210)"
            )
            InsightRow(
                "• 연속 번호 포함 세트: ${analysis.consecutiveCount}/${analysis.totalSets}"
            )
            InsightRow(
                "• 짝수 비율: $evenPercent% (권장: 40~60%)"
            )
        }
    }
}

@Composable
fun InsightRow(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
