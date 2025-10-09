package com.lotto.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lotto.app.viewmodel.WinningCheckViewModel
import java.text.NumberFormat
import java.util.*

/**
 * 당첨 확인 화면 (백엔드 API 연동)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckWinningScreen(
    onNavigateBack: () -> Unit,
    viewModel: WinningCheckViewModel = viewModel()
) {
    val checkResult by viewModel.checkResult.collectAsState()
    val latestWinning by viewModel.latestWinning.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedNumbers by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showHistory by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 에러 처리
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("당첨 확인") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(
                            imageVector = if (showHistory) Icons.Default.Close else Icons.Default.History,
                            contentDescription = "내역"
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
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (showHistory) {
                // 당첨 확인 내역
                WinningHistoryView(
                    history = history,
                    isLoading = isLoading,
                    onRefresh = { viewModel.loadHistory() }
                )
            } else {
                // 당첨 확인 메인 화면
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 최신 당첨 정보
                    latestWinning?.let { winning ->
                        LatestWinningCard(
                            drawNumber = winning.drawNumber,
                            numbers = winning.numbers,
                            bonusNumber = winning.bonusNumber,
                            drawDate = winning.drawDate
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // 번호 선택기
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "확인할 번호 선택 (6개)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${selectedNumbers.size}/6 선택됨",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 번호 그리드
                            NumberGrid(
                                selectedNumbers = selectedNumbers,
                                onNumberClick = { number ->
                                    selectedNumbers = if (selectedNumbers.contains(number)) {
                                        selectedNumbers - number
                                    } else {
                                        if (selectedNumbers.size < 6) {
                                            selectedNumbers + number
                                        } else {
                                            selectedNumbers
                                        }
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { selectedNumbers = emptySet() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Clear, "초기화")
                                    Spacer(Modifier.width(4.dp))
                                    Text("초기화")
                                }
                                
                                Button(
                                    onClick = {
                                        latestWinning?.let {
                                            viewModel.checkWinning(
                                                selectedNumbers.sorted(),
                                                it.drawNumber
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = selectedNumbers.size == 6 && !isLoading && latestWinning != null
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(Icons.Default.CheckCircle, "확인")
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("당첨 확인")
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 결과 표시
                    AnimatedVisibility(
                        visible = checkResult != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        checkResult?.let { result ->
                            // 일치하는 번호 계산
                            val matchedNums = result.userNumbers.filter { it in result.winningNumbers }
                            
                            WinningResultCard(
                                matchedCount = result.matchedCount,
                                matchedNumbers = matchedNums,
                                bonusMatched = result.hasBonus,
                                rank = result.rank,
                                prizeAmount = result.prizeAmount,
                                onDismiss = { viewModel.clearResult() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 최신 당첨 정보 카드
 */
@Composable
fun LatestWinningCard(
    drawNumber: Int,
    numbers: List<Int>,
    bonusNumber: Int,
    drawDate: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${drawNumber}회 당첨 번호",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                drawDate?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                numbers.forEach { number ->
                    LottoBall(number = number, size = 38.dp)
                }
                
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "보너스",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
                
                LottoBall(
                    number = bonusNumber,
                    size = 38.dp,
                    isBonus = true
                )
            }
        }
    }
}

/**
 * 번호 그리드 (1-45)
 */
@Composable
fun NumberGrid(
    selectedNumbers: Set<Int>,
    onNumberClick: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0..8) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0..4) {
                    val number = row * 5 + col + 1
                    if (number <= 45) {
                        NumberButton(
                            number = number,
                            isSelected = selectedNumbers.contains(number),
                            onClick = { onNumberClick(number) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 번호 버튼
 */
@Composable
fun NumberButton(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 로또 번호 공
 */
@Composable
fun LottoBall(
    number: Int,
    size: Dp = 40.dp,
    isBonus: Boolean = false
) {
    val color = when {
        isBonus -> Color(0xFFFF9800) // 보너스는 오렌지색
        number <= 10 -> Color(0xFFFFC107) // 노랑
        number <= 20 -> Color(0xFF2196F3) // 파랑
        number <= 30 -> Color(0xFFF44336) // 빨강
        number <= 40 -> Color(0xFF9E9E9E) // 회색
        else -> Color(0xFF4CAF50) // 초록
    }
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        color
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * 당첨 결과 카드
 */
@Composable
fun WinningResultCard(
    matchedCount: Int,
    matchedNumbers: List<Int>,
    bonusMatched: Boolean,
    rank: Int?,
    prizeAmount: Long?,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (rank) {
                1 -> Color(0xFFFFD700) // 금색
                2, 3 -> Color(0xFFC0C0C0) // 은색
                4, 5 -> Color(0xFFCD7F32) // 동색
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "당첨 결과",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "닫기")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 맞은 개수
            Text(
                text = "${matchedCount}개 일치",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (bonusMatched) {
                Text(
                    text = "+ 보너스 번호 일치",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 맞은 번호
            if (matchedNumbers.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    matchedNumbers.forEach { number ->
                        LottoBall(number = number, size = 32.dp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 등수 및 당첨금
            if (rank != null && rank <= 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${rank}등 당첨!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (rank) {
                            1 -> Color(0xFFB8860B)
                            2, 3 -> Color(0xFF696969)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    prizeAmount?.let { amount ->
                        Text(
                            text = formatCurrency(amount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "🎉 축하합니다! 🎉",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "아쉽지만 낙첨입니다",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "다음 기회에 도전하세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 당첨 확인 내역 뷰
 */
@Composable
fun WinningHistoryView(
    history: List<com.lotto.app.data.model.WinningHistoryItem>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "당첨 확인 내역",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "당첨 확인 내역이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { item ->
                    HistoryItemCard(item)
                }
            }
        }
    }
}

/**
 * 내역 아이템 카드
 */
@Composable
fun HistoryItemCard(item: com.lotto.app.data.model.WinningHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.rank != null && item.rank <= 5) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.drawNumber}회차",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = item.checkedAt.split("T")[0],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.numbers.forEach { number ->
                    LottoBall(number = number, size = 32.dp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${item.matchedCount}개 일치",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (item.hasBonus) {
                        Text(
                            text = "+ 보너스",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (item.rank != null && item.rank <= 5) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${item.rank}등",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        item.prizeAmount?.let { amount ->
                            Text(
                                text = formatCurrency(amount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "낙첨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 금액 포맷
 */
fun formatCurrency(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
    return formatter.format(amount)
}
