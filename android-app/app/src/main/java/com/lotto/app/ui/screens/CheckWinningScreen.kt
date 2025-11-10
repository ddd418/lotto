package com.lotto.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lotto.app.viewmodel.WinningCheckViewModel
import com.lotto.app.viewmodel.SavedNumberViewModel
import com.lotto.app.data.model.SavedNumberResponse
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.delay

/**
 * 당첨 결과 데이터 클래스
 */
data class DrawResult(
    val nickname: String,
    val numbers: List<Int>,
    val matchedCount: Int,
    val hasBonus: Boolean,
    val rank: Int?
)

/**
 * 당첨 확인 화면 (백엔드 API 연동)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckWinningScreen(
    onNavigateBack: () -> Unit,
    viewModel: WinningCheckViewModel = viewModel(),
    savedNumberViewModel: SavedNumberViewModel = viewModel()
) {
    val checkResult by viewModel.checkResult.collectAsState()
    val latestWinning by viewModel.latestWinning.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // 저장된 번호 목록
    val savedNumbers by savedNumberViewModel.savedNumbers.collectAsState()
    val savedNumbersLoading by savedNumberViewModel.isLoading.collectAsState()
    
    var selectedNumbers by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showHistory by remember { mutableStateOf(false) }
    var showSavedNumbersDialog by remember { mutableStateOf(false) }
    var showDrawAnimation by remember { mutableStateOf(false) }
    var selectedSavedNumbers by remember { mutableStateOf<List<SavedNumberResponse>>(emptyList()) }
    var currentAnimationIndex by remember { mutableIntStateOf(0) }
    var showResultsSummary by remember { mutableStateOf(false) }
    var drawResults by remember { mutableStateOf<List<DrawResult>>(emptyList()) }
    
    // 저장된 번호 로드
    LaunchedEffect(Unit) {
        savedNumberViewModel.loadSavedNumbers()
    }
    
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
                        .verticalScroll(rememberScrollState())
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
                    
                    // 저장번호 불러오기
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "저장된 번호로 당첨 확인",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 내 저장번호 불러오기 버튼
                            Button(
                                onClick = {
                                    savedNumberViewModel.loadSavedNumbers()
                                    showSavedNumbersDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.List, "내 번호")
                                Spacer(Modifier.width(8.dp))
                                Text("내 저장번호 불러오기 (${selectedSavedNumbers.size}개 선택)")
                            }
                            
                            if (selectedSavedNumbers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // 선택된 저장번호 목록
                                Text(
                                    text = "선택된 번호 목록",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                selectedSavedNumbers.forEach { savedNumber ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = savedNumber.nickname ?: "번호 ${savedNumber.id}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    savedNumber.numbers.forEach { number ->
                                                        LottoBall(
                                                            number = number,
                                                            size = 28.dp
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    selectedSavedNumbers = selectedSavedNumbers.filter { it.id != savedNumber.id }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    "제거",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        android.util.Log.d("CheckWinningScreen", "🎬 순차 추첨 시작: ${selectedSavedNumbers.size}개 번호")
                                        selectedSavedNumbers.forEachIndexed { index, number ->
                                            android.util.Log.d("CheckWinningScreen", "  [$index] ${number.nickname}: ${number.numbers}")
                                        }
                                        drawResults = emptyList()
                                        currentAnimationIndex = 0
                                        showDrawAnimation = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading && latestWinning != null
                                ) {
                                    Icon(Icons.Default.PlayArrow, "추첨")
                                    Spacer(Modifier.width(8.dp))
                                    Text("${selectedSavedNumbers.size}개 번호 순차 추첨 시작!")
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
    
    // 저장된 번호 선택 다이얼로그
    if (showSavedNumbersDialog) {
        SavedNumbersSelectionDialog(
            savedNumbers = savedNumbers,
            selectedSavedNumbers = selectedSavedNumbers,
            isLoading = savedNumbersLoading,
            onSelectionChanged = { selected ->
                selectedSavedNumbers = selected
            },
            onDismiss = { showSavedNumbersDialog = false }
        )
    }
    
    // 가상 추첨 애니메이션 다이얼로그
    if (showDrawAnimation && selectedSavedNumbers.isNotEmpty() && currentAnimationIndex < selectedSavedNumbers.size) {
        val currentSavedNumber = selectedSavedNumbers[currentAnimationIndex]
        // key를 사용해서 매번 새로운 인스턴스로 생성 (애니메이션 초기화)
        key(currentAnimationIndex) {
            VirtualDrawAnimationDialog(
                userNumbers = currentSavedNumber.numbers.sorted(),
                winningNumbers = latestWinning?.numbers ?: emptyList(),
                bonusNumber = latestWinning?.bonusNumber ?: 0,
                savedNumberNickname = currentSavedNumber.nickname,
                currentIndex = currentAnimationIndex + 1,
                totalCount = selectedSavedNumbers.size,
                onAnimationComplete = { matchedCount, hasBonus, rank ->
                    // 백엔드에 당첨 확인 결과 저장
                    latestWinning?.let { winning ->
                        viewModel.checkWinning(
                            numbers = currentSavedNumber.numbers,
                            drawNumber = winning.drawNumber
                        )
                        android.util.Log.d("CheckWinningScreen", "💾 백엔드에 당첨 결과 저장: ${currentSavedNumber.nickname}")
                    }
                    
                    // 결과 저장
                    val newResult = DrawResult(
                        nickname = currentSavedNumber.nickname ?: "번호 ${currentSavedNumber.id}",
                        numbers = currentSavedNumber.numbers,
                        matchedCount = matchedCount,
                        hasBonus = hasBonus,
                        rank = rank
                    )
                    drawResults = drawResults + newResult
                    
                    android.util.Log.d("CheckWinningScreen", "🎯 결과 추가: ${newResult.nickname}, matched=${matchedCount}, total results=${drawResults.size}")
                    
                    // 다음 번호로 이동 또는 완료
                    if (currentAnimationIndex + 1 < selectedSavedNumbers.size) {
                        currentAnimationIndex = currentAnimationIndex + 1
                        android.util.Log.d("CheckWinningScreen", "➡️ 다음 번호로 이동: ${currentAnimationIndex + 1}/${selectedSavedNumbers.size}")
                    } else {
                        android.util.Log.d("CheckWinningScreen", "✅ 모든 추첨 완료! 총 ${drawResults.size}개 결과")
                        showDrawAnimation = false
                        currentAnimationIndex = 0
                        showResultsSummary = true
                        // 내역 새로고침
                        viewModel.loadHistory()
                    }
                },
                onDismiss = {
                    android.util.Log.d("CheckWinningScreen", "❌ 애니메이션 취소됨")
                    showDrawAnimation = false
                    currentAnimationIndex = 0
                    drawResults = emptyList()
                }
            )
        }
    }
    
    // 전체 결과 요약 다이얼로그
    if (showResultsSummary && drawResults.isNotEmpty()) {
        DrawResultsSummaryDialog(
            results = drawResults,
            onDismiss = {
                showResultsSummary = false
                drawResults = emptyList()
            }
        )
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
            
            // 일반 번호
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                numbers.forEach { number ->
                    LottoBall(number = number, size = 38.dp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 보너스 번호
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "보너스",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 8.dp)
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
 * 로또 번호 공
 */
@Composable
fun LottoBall(
    number: Int,
    size: Dp = 40.dp,
    isBonus: Boolean = false,
    modifier: Modifier = Modifier
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
        modifier = modifier
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
    
    // 일치 개수 많은 순으로 정렬
    val sortedHistory = remember(history) {
        history.sortedWith(
            compareByDescending<com.lotto.app.data.model.WinningHistoryItem> { it.matchedCount }
                .thenByDescending { it.hasBonus }
                .thenByDescending { it.checkedAt }
        ).also {
            android.util.Log.d("WinningHistoryView", "📊 당첨 내역 정렬: ${history.size}개 -> 일치 개수 많은 순")
        }
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
                items(sortedHistory) { item ->
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

/**
 * 저장된 번호 선택 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedNumbersSelectionDialog(
    savedNumbers: List<SavedNumberResponse>,
    selectedSavedNumbers: List<SavedNumberResponse>,
    isLoading: Boolean,
    onSelectionChanged: (List<SavedNumberResponse>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "💾 저장된 번호 선택 (${selectedSavedNumbers.size}개)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (savedNumbers.isEmpty()) {
                Text(
                    text = "저장된 번호가 없습니다.\n추천 화면에서 번호를 저장해보세요!",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = savedNumbers,
                        key = { it.id }
                    ) { savedNumber ->
                        val isSelected = selectedSavedNumbers.any { it.id == savedNumber.id }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val newSelection = if (isSelected) {
                                    selectedSavedNumbers.filter { it.id != savedNumber.id }
                                } else {
                                    selectedSavedNumbers + savedNumber
                                }
                                onSelectionChanged(newSelection)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null
                                        )
                                        Text(
                                            text = savedNumber.nickname ?: "번호 ${savedNumber.id}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (savedNumber.isFavorite) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "즐겨찾기",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    savedNumber.numbers.forEach { number ->
                                        LottoBall(number = number, size = 30.dp)
                                    }
                                }
                                
                                savedNumber.memo?.let { memo ->
                                    if (memo.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = memo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                android.util.Log.d("SavedNumbersSelectionDialog", "✅ 선택 완료: ${selectedSavedNumbers.size}개")
                selectedSavedNumbers.forEachIndexed { index, number ->
                    android.util.Log.d("SavedNumbersSelectionDialog", "  [$index] ${number.nickname}: ${number.numbers}")
                }
                onDismiss()
            }) {
                Text("완료")
            }
        },
        dismissButton = {
            if (selectedSavedNumbers.isNotEmpty()) {
                TextButton(
                    onClick = { onSelectionChanged(emptyList()) }
                ) {
                    Text("전체 해제")
                }
            }
        }
    )
}

/**
 * 가상 추첨 애니메이션 다이얼로그
 */
@Composable
fun VirtualDrawAnimationDialog(
    userNumbers: List<Int>,
    winningNumbers: List<Int>,
    bonusNumber: Int,
    savedNumberNickname: String?,
    currentIndex: Int = 1,
    totalCount: Int = 1,
    onAnimationComplete: (matchedCount: Int, hasBonus: Boolean, rank: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var revealedNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showResult by remember { mutableStateOf(false) }
    
    val matchedNumbers = userNumbers.filter { it in winningNumbers }
    val hasBonus = bonusNumber in userNumbers && bonusNumber !in winningNumbers
    val matchCount = matchedNumbers.size
    
    // 등수 계산
    val rank = when {
        matchCount == 6 -> 1
        matchCount == 5 && hasBonus -> 2
        matchCount == 5 -> 3
        matchCount == 4 -> 4
        matchCount == 3 -> 5
        else -> null
    }
    
    // 애니메이션 진행 (빠른 모드)
    LaunchedEffect(Unit) {
        // 1. 준비 단계 (0.3초)
        delay(300)
        
        // 2. 당첨 번호 하나씩 공개 (각 0.15초)
        winningNumbers.forEach { number ->
            revealedNumbers = revealedNumbers + number
            delay(150)
        }
        
        // 3. 보너스 번호 공개 (0.3초)
        delay(150)
        revealedNumbers = revealedNumbers + bonusNumber
        delay(300)
        
        // 4. 결과 표시
        showResult = true
        
        // 5. 결과 표시 후 자동 진행 (0.8초)
        delay(800)
        onAnimationComplete(matchCount, hasBonus, rank)
    }
    
    AlertDialog(
        onDismissRequest = { },  // 애니메이션 중에는 닫기 불가
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 진행 상황 표시
                if (totalCount > 1) {
                    Text(
                        text = "$currentIndex / $totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 제목
                Text(
                    text = if (showResult) "🎊 추첨 결과" else "🎰 추첨 중...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                savedNumberNickname?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$it\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 내 번호
                Text(
                    text = "내 번호",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val ballCount = userNumbers.size
                    val spacing = 6.dp
                    val totalSpacing = spacing * (ballCount - 1)
                    val ballSizePx = ((maxWidth - totalSpacing) / ballCount).coerceAtMost(56.dp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        userNumbers.forEach { number ->
                            val isMatched = showResult && number in matchedNumbers
                            val isBonusMatch = showResult && hasBonus && number == bonusNumber
                            AnimatedLottoBall(
                                number = number,
                                size = ballSizePx,
                                isHighlighted = isMatched || isBonusMatch,
                                highlightColor = if (isBonusMatch) Color(0xFFFF6B6B) else Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 당첨 번호
                Text(
                    text = "당첨 번호",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val ballCount = if (revealedNumbers.size > winningNumbers.size) winningNumbers.size + 1 else winningNumbers.size
                    val spacing = 6.dp
                    val totalSpacing = spacing * (ballCount - 1) + if (revealedNumbers.size > winningNumbers.size) 16.dp else 0.dp
                    val ballSizePx = ((maxWidth - totalSpacing) / ballCount).coerceAtMost(56.dp)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        winningNumbers.forEach { number ->
                            AnimatedRevealBall(
                                number = number,
                                isRevealed = number in revealedNumbers,
                                size = ballSizePx
                            )
                        }
                    
                        if (revealedNumbers.size > winningNumbers.size) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "보너스",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            
                            AnimatedRevealBall(
                                number = bonusNumber,
                                isRevealed = bonusNumber in revealedNumbers,
                                size = ballSizePx,
                                isBonus = true
                            )
                        }
                    }
                }
                
                // 결과 메시지
                AnimatedVisibility(
                    visible = showResult,
                    enter = fadeIn() + scaleIn()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val resultMessage = when {
                            matchCount == 6 -> "🎉🎉🎉 1등 당첨! 🎉🎉🎉"
                            matchCount == 5 && hasBonus -> "🎊 2등 당첨! 🎊"
                            matchCount == 5 -> "🎁 3등 당첨! 🎁"
                            matchCount == 4 -> "✨ 4등 당첨! ✨"
                            matchCount == 3 -> "🌟 5등 당첨! 🌟"
                            else -> "아쉽게도 낙첨입니다 😢"
                        }
                        
                        Text(
                            text = resultMessage,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (matchCount >= 3) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${matchCount}개 일치" + if (hasBonus) " + 보너스" else "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            // 자동 진행으로 버튼 제거
        },
        dismissButton = {
            if (totalCount > 1) {
                TextButton(onClick = onDismiss) {
                    Text("전체 중단")
                }
            }
        }
    )
}

/**
 * 애니메이션 하이라이트 공
 */
@Composable
fun AnimatedLottoBall(
    number: Int,
    size: Dp = 40.dp,
    isHighlighted: Boolean = false,
    highlightColor: Color = Color(0xFF4CAF50)
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ball_scale"
    )
    
    val color = when {
        isHighlighted -> highlightColor
        number <= 10 -> Color(0xFFFBC02D)
        number <= 20 -> Color(0xFF42A5F5)
        number <= 30 -> Color(0xFFEF5350)
        number <= 40 -> Color(0xFFBDBDBD)
        else -> Color(0xFF66BB6A)
    }
    
    Box(
        modifier = Modifier
            .size(size * scale)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

/**
 * 공개 애니메이션 공
 */
@Composable
fun AnimatedRevealBall(
    number: Int,
    isRevealed: Boolean,
    size: Dp = 40.dp,
    isBonus: Boolean = false
) {
    val alpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0.3f,
        animationSpec = tween(durationMillis = 500),
        label = "ball_alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ball_reveal_scale"
    )
    
    val color = when {
        isBonus -> Color(0xFFFF6B6B)
        number <= 10 -> Color(0xFFFBC02D)
        number <= 20 -> Color(0xFF42A5F5)
        number <= 30 -> Color(0xFFEF5350)
        number <= 40 -> Color(0xFFBDBDBD)
        else -> Color(0xFF66BB6A)
    }
    
    Box(
        modifier = Modifier
            .size(size * scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        if (isRevealed) {
            Text(
                text = number.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        } else {
            Text(
                text = "?",
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

/**
 * 전체 당첨 결과 요약 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawResultsSummaryDialog(
    results: List<DrawResult>,
    onDismiss: () -> Unit
) {
    // 명확한 정렬: matchedCount 내림차순 -> hasBonus 내림차순 -> rank 오름차순 (null은 뒤로)
    val sortedResults = remember(results) {
        android.util.Log.d("DrawResults", "📊 정렬 시작 - 총 ${results.size}개 결과")
        android.util.Log.d("DrawResults", "📊 정렬 전 결과:")
        results.forEachIndexed { index, result ->
            android.util.Log.d("DrawResults", "  [$index] ${result.nickname}: matched=${result.matchedCount}, bonus=${result.hasBonus}, rank=${result.rank}")
        }
        
        val sorted = results.sortedWith(
            compareByDescending<DrawResult> { it.matchedCount }
                .thenByDescending { it.hasBonus }
                .thenBy { it.rank ?: Int.MAX_VALUE }
        )
        
        android.util.Log.d("DrawResults", "📊 정렬 후 결과:")
        sorted.forEachIndexed { index, result ->
            android.util.Log.d("DrawResults", "  [$index] ${result.nickname}: matched=${result.matchedCount}, bonus=${result.hasBonus}, rank=${result.rank}")
        }
        
        sorted
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🎉 전체 당첨 결과",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = sortedResults,
                    key = { "${it.nickname}_${it.numbers.joinToString()}" }
                ) { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (result.rank) {
                                1 -> Color(0xFFFFD700).copy(alpha = 0.3f)
                                2, 3 -> Color(0xFFC0C0C0).copy(alpha = 0.3f)
                                4, 5 -> Color(0xFFCD7F32).copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // 번호 이름
                            Text(
                                text = result.nickname,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 번호
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                result.numbers.forEach { number ->
                                    LottoBall(number = number, size = 28.dp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Divider()
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 결과
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${result.matchedCount}개 일치" + 
                                           if (result.hasBonus) " + 보너스" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                result.rank?.let { rank ->
                                    val emoji = when (rank) {
                                        1 -> "🎉"
                                        2 -> "🎊"
                                        3 -> "🎁"
                                        4 -> "✨"
                                        5 -> "🌟"
                                        else -> ""
                                    }
                                    Text(
                                        text = "$emoji ${rank}등",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (rank) {
                                            1 -> Color(0xFFFF6B35)
                                            2, 3 -> Color(0xFF4CAF50)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                } ?: run {
                                    Text(
                                        text = "낙첨",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}
