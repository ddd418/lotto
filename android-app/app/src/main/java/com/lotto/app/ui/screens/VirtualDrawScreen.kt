package com.lotto.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lotto.app.data.local.SavedNumbersManager
import com.lotto.app.data.model.VirtualDrawResult
import com.lotto.app.data.model.WinningRank
import com.lotto.app.ui.components.LottoNumberBall
import com.lotto.app.util.VirtualDrawUtil
import kotlinx.coroutines.delay

/**
 * 반응형 로또 번호 볼 컴포넌트
 */
@Composable
fun ResponsiveLottoNumberBall(
    number: Int,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    
    // 화면 크기와 컨텍스트에 따라 공 크기 결정
    val ballSize = when {
        isCompact -> {
            val size = screenWidth / 10
            (if (size > 32) 32 else if (size < 24) 24 else size).dp
        }
        screenWidth < 360 -> {
            val size = screenWidth / 8
            (if (size > 48) 48 else if (size < 36) 36 else size).dp
        }
        screenWidth < 400 -> {
            val size = screenWidth / 7
            (if (size > 52) 52 else if (size < 40) 40 else size).dp
        }
        else -> {
            val size = (screenWidth / 6.5f).toInt()
            (if (size > 56) 56 else if (size < 44) 44 else size).dp
        }
    }
    
    LottoNumberBall(
        number = number,
        modifier = modifier,
        size = ballSize
    )
}

/**
 * 가상 추첨 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualDrawScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val savedNumbersManager = remember { SavedNumbersManager(context) }
    
    var isDrawing by remember { mutableStateOf(false) }
    var drawResult by remember { mutableStateOf<VirtualDrawResult?>(null) }
    var showResults by remember { mutableStateOf(false) }
    
    // 룰렛 애니메이션
    val rotationAngle by animateFloatAsState(
        targetValue = if (isDrawing) 360f * 5 else 0f,
        animationSpec = tween(
            durationMillis = 3000,
            easing = LinearOutSlowInEasing
        ),
        label = "roulette_rotation"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🎰 가상 추첨",
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
                    if (drawResult != null && !isDrawing) {
                        // 공유 버튼
                        IconButton(
                            onClick = {
                                drawResult?.let { result ->
                                    shareDrawResult(context, result)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "결과 공유"
                            )
                        }
                        
                        // 다시 추첨 버튼
                        IconButton(
                            onClick = {
                                drawResult = null
                                showResults = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "다시 추첨"
                            )
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 룰렛 애니메이션
            Card(
                modifier = Modifier
                    .size(200.dp)
                    .rotate(rotationAngle),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 추첨 버튼
            Button(
                onClick = {
                    if (!isDrawing) {
                        isDrawing = true
                        showResults = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = !isDrawing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDrawing) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                if (isDrawing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("추첨 중...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("🎲 가상 추첨 시작!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // 추첨 결과 표시
            drawResult?.let { result ->
                DrawnNumbersCard(result.drawnNumbers, result.bonusNumber)
                
                // 당첨 결과 표시
                if (showResults) {
                    if (result.userResults.isNotEmpty()) {
                        WinningResultsCard(result)
                    } else {
                        NoSavedNumbersCard()
                    }
                }
            }
            
            // 안내 문구
            if (drawResult == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "� 가상 추첨 안내",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 실제 로또 추첨과 동일한 방식으로 진행됩니다\n" +
                                  "• 저장된 번호가 있으면 자동으로 당첨 확인 및 상금 계산을 해드려요\n" +
                                  "• 결과를 친구들과 공유할 수 있습니다\n" +
                                  "• 재미를 위한 가상 추첨이므로 실제 당첨과는 무관합니다",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
    
    // 추첨 애니메이션 처리
    LaunchedEffect(isDrawing) {
        if (isDrawing) {
            delay(3000) // 룰렛 애니메이션 시간
            
            // 가상 추첨 실행
            val result = VirtualDrawUtil.performVirtualDraw(savedNumbersManager)
            drawResult = result
            
            delay(1000)
            isDrawing = false
            showResults = true
        }
    }
}

/**
 * 결과 공유 함수
 */
fun shareDrawResult(context: Context, result: VirtualDrawResult) {
    val shareText = result.toShareText()
    
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "가상 로또 추첨 결과")
    }
    
    context.startActivity(Intent.createChooser(intent, "결과 공유하기"))
}

/**
 * 추첨 번호 카드 (개선된 UI)
 */
@Composable
fun DrawnNumbersCard(numbers: List<Int>, bonusNumber: Int) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(500)
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(1000)
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4FC3F7).copy(alpha = 0.9f),
                                Color(0xFF29B6F6).copy(alpha = 0.8f),
                                Color(0xFF03A9F4).copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 헤더
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎊",
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "추첨 결과",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎊",
                            fontSize = 28.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 메인 번호들
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "당첨 번호",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                numbers.forEachIndexed { index, number ->
                                    // 애니메이션으로 번호 볼 나타내기
                                    var ballVisible by remember { mutableStateOf(false) }
                                    
                                    LaunchedEffect(Unit) {
                                        delay(1000 + (index * 150L))
                                        ballVisible = true
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = ballVisible,
                                        enter = fadeIn(animationSpec = tween(400)) +
                                                slideInVertically(initialOffsetY = { -it })
                                    ) {
                                        ResponsiveLottoNumberBall(number = number)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 보너스 번호
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌟",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "보너스 번호",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 보너스 번호 애니메이션
                            var bonusVisible by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                delay(2000) // 메인 번호들이 다 나타난 후
                                bonusVisible = true
                            }
                            
                            AnimatedVisibility(
                                visible = bonusVisible,
                                enter = fadeIn(animationSpec = tween(600)) +
                                        slideInVertically(initialOffsetY = { it })
                            ) {
                                ResponsiveLottoNumberBall(number = bonusNumber)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 당첨 결과 카드 (개선된 UI)
 */
@Composable
fun WinningResultsCard(result: VirtualDrawResult) {
    val winningResults = result.getWinningResults()
    val totalPrize = result.getTotalPrize()
    
    // 애니메이션 상태
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(800)
        )
    ) {
        if (winningResults.isNotEmpty()) {
            // 당첨된 경우
            WinningCard(result, winningResults, totalPrize)
        } else {
            // 당첨되지 않은 경우
            NoWinningCard(result)
        }
    }
}

/**
 * 당첨 카드
 */
@Composable
fun WinningCard(result: VirtualDrawResult, winningResults: List<com.lotto.app.data.model.UserDrawResult>, totalPrize: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.9f),
                            Color(0xFFFFA500).copy(alpha = 0.7f),
                            Color(0xFFFF8C00).copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                // 헤더 - 축하 메시지
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFFF6B00)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🎉 축하합니다! 🎉",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFFF6B00)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 총 당첨금 표시
                if (totalPrize > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color(0xFFFF6B00)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "총 당첨금",
                                fontSize = 16.sp,
                                color = Color(0xFF333333)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatPrize(totalPrize) + "원",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 개별 당첨 결과들
                winningResults.forEachIndexed { index, userResult ->
                    AnimatedWinningResultItem(
                        userResult = userResult,
                        delay = (index + 1) * 200L
                    )
                    if (index < winningResults.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 당첨 요약
                if (winningResults.size > 1) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "총 ${winningResults.size}개 번호 당첨!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                            }
                            Text(
                                text = "최고 ${result.getHighestRank().rank}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 애니메이션이 있는 당첨 결과 아이템
 */
@Composable
fun AnimatedWinningResultItem(
    userResult: com.lotto.app.data.model.UserDrawResult,
    delay: Long
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delay)
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(600)
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (userResult.rank) {
                    WinningRank.FIRST -> Color(0xFFFFD700)  // 금색
                    WinningRank.SECOND -> Color(0xFFC0C0C0) // 은색
                    WinningRank.THIRD -> Color(0xFFCD7F32)  // 동색
                    WinningRank.FOURTH -> Color(0xFF90EE90) // 연두색
                    WinningRank.FIFTH -> Color(0xFFDDA0DD)  // 자주색
                    else -> Color.White
                }
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 등수와 당첨금
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 등수별 아이콘
                        Text(
                            text = when (userResult.rank) {
                                WinningRank.FIRST -> "🥇"
                                WinningRank.SECOND -> "🥈"
                                WinningRank.THIRD -> "🥉"
                                WinningRank.FOURTH -> "🎁"
                                WinningRank.FIFTH -> "🎊"
                                else -> "📋"
                            },
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${userResult.rank.rank} 당첨!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                    
                    Text(
                        text = formatPrize(userResult.getPrizeAmount()) + "원",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 번호 정보
                Text(
                    text = userResult.memo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF444444)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 매치 상세 정보
                val (matched, notMatched) = userResult.getDetailedMatch()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 맞춘 번호들
                    Text(
                        text = "✅ ",
                        fontSize = 14.sp
                    )
                    matched.forEach { number ->
                        ResponsiveLottoNumberBall(
                            number = number,
                            modifier = Modifier.padding(end = 4.dp),
                            isCompact = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "(${matched.size}개 일치)",
                        fontSize = 13.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (userResult.hasBonus) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🌟 보너스 번호 일치!",
                            fontSize = 13.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 낙첨 카드 (개선된 UI)
 */
@Composable
fun NoWinningCard(result: VirtualDrawResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF93D9F7).copy(alpha = 0.7f),
                            Color(0xFFB3E5FC).copy(alpha = 0.5f),
                            Color(0xFFE1F5FE).copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 아쉬움 표현
                Text(
                    text = "😅",
                    fontSize = 64.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "아쉽게도 당첨되지 않았습니다",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF546E7A),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "다음 기회에 도전해보세요!",
                    fontSize = 15.sp,
                    color = Color(0xFF78909C),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 근사한 결과가 있으면 표시
                val nearMisses = result.userResults.filter { it.matchCount >= 2 && it.rank == WinningRank.NONE }
                if (nearMisses.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💫",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "아까운 결과",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF546E7A)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            nearMisses.forEach { userResult ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = userResult.memo,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF546E7A)
                                            )
                                            Text(
                                                text = "${userResult.matchCount}개 일치",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF26A69A)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // 맞춘 번호들 표시
                                        val matchedNumbers = userResult.getDetailedMatch().first
                                        Text(
                                            text = "✅ 맞춘 번호: ${matchedNumbers.joinToString(", ")}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 격려 메시지
                Text(
                    text = "포기하지 마세요! 다음번엔 분명 좋은 결과가 있을 거예요 💪",
                    fontSize = 14.sp,
                    color = Color(0xFF607D8B),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * 저장된 번호 없음 카드 (개선된 UI)
 */
@Composable
fun NoSavedNumbersCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3E5F5).copy(alpha = 0.8f),
                            Color(0xFFE8EAF6).copy(alpha = 0.6f),
                            Color(0xFFE3F2FD).copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📭",
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "저장된 번호가 없습니다",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5E35B1),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "번호를 저장하면 자동으로 당첨 확인을 해드려요!",
                    fontSize = 15.sp,
                    color = Color(0xFF7986CB),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "� 번호 추천 화면에서 마음에 드는 번호를 저장해보세요!",
                        fontSize = 14.sp,
                        color = Color(0xFF5E35B1),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * 당첨금 포맷팅
 */
fun formatPrize(amount: Long): String {
    return when {
        amount >= 100000000 -> String.format("%,d", amount / 100000000) + "억"
        amount >= 10000 -> String.format("%,d", amount / 10000) + "만"
        else -> String.format("%,d", amount)
    }
}

/**
 * 추첨 애니메이션 (기존 유지)
 */
@Composable
fun DrawingAnimation(isDrawing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "drawing")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (isDrawing) {
            Text(
                text = "🎰",
                fontSize = 80.sp,
                modifier = Modifier.rotate(rotation)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎲",
                    fontSize = 80.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "추첨 준비 완료!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
