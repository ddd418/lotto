package com.lotto.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import com.lotto.app.data.local.SavedNumbersManager
import com.lotto.app.data.model.SavedLottoNumber
import com.lotto.app.ui.components.LoadingIndicator
import com.lotto.app.ui.components.LottoSetCard
import com.lotto.app.viewmodel.LottoViewModel
import com.lotto.app.viewmodel.UiState

/**
 * 추천 번호 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendScreen(
    viewModel: LottoViewModel,
    onNavigateBack: () -> Unit
) {
    val recommendState by viewModel.recommendState.collectAsStateWithLifecycle()
    var numberOfSets by remember { mutableIntStateOf(5) }
    var selectedMode by remember { mutableStateOf("ai") }
    
    // 화면 진입 시 자동으로 번호 추천
    LaunchedEffect(Unit) {
        viewModel.recommendNumbers(numberOfSets, selectedMode)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "로또 번호 추천",
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
                    // 재추천 버튼
                    IconButton(
                        onClick = { viewModel.recommendNumbers(numberOfSets, selectedMode) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "재추천"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = recommendState) {
                is UiState.Idle -> {
                    LoadingIndicator(message = "번호 추천 준비 중...")
                }
                
                is UiState.Loading -> {
                    LoadingIndicator(message = "AI가 번호를 분석하는 중...")
                }
                
                is UiState.Success -> {
                    val response = state.data
                    val context = LocalContext.current
                    val savedNumbersManager = remember { SavedNumbersManager(context) }
                    var showSaveDialog by remember { mutableStateOf<List<Int>?>(null) }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 모드 선택 카드
                        item {
                            ModeSelectionCard(
                                selectedMode = selectedMode,
                                onModeSelected = { mode ->
                                    selectedMode = mode
                                    viewModel.recommendNumbers(numberOfSets, mode)
                                }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        
                        // 헤더 정보
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
                                        text = "🎲 추천 번호",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "기준: ${response.lastDraw}회차까지",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        
                        // 추천 번호 세트들
                        itemsIndexed(response.sets) { index, lottoSet ->
                            Column {
                                LottoSetCard(
                                    setNumber = index + 1,
                                    lottoSet = lottoSet
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 세트별 저장 버튼
                                OutlinedButton(
                                    onClick = { showSaveDialog = lottoSet.numbers },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("이 번호 저장하기")
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        
                        // 카카오톡 공유 버튼
                        item {
                            Button(
                                onClick = {
                                    shareToKakao(context, response.sets, response.lastDraw)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFE812) // 카카오 옐로우
                                )
                            ) {
                                Text(
                                    text = "💬 카카오톡으로 공유",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3C1E1E)
                                )
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        
                        // 일반 공유 버튼
                        item {
                            OutlinedButton(
                                onClick = {
                                    // 공유할 텍스트 생성
                                    val shareText = buildShareText(response.sets, response.lastDraw)
                                    
                                    // Android 공유 Intent
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    
                                    val shareIntent = Intent.createChooser(sendIntent, "로또 번호 공유하기")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "다른 방법으로 공유",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    
                    // 저장 다이얼로그
                    showSaveDialog?.let { numbers ->
                        var memo by remember { mutableStateOf("") }
                        
                        AlertDialog(
                            onDismissRequest = { showSaveDialog = null },
                            title = { Text("번호 저장") },
                            text = {
                                Column {
                                    Text("이 번호를 저장하시겠습니까?")
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextField(
                                        value = memo,
                                        onValueChange = { memo = it },
                                        label = { Text("메모 (선택사항)") },
                                        placeholder = { Text("예: 생일, 행운의 번호") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val savedNumber = SavedLottoNumber(
                                            id = System.currentTimeMillis().toString(),
                                            numbers = numbers,
                                            savedAt = System.currentTimeMillis(),
                                            memo = memo,
                                            drawNumber = response.lastDraw
                                        )
                                        savedNumbersManager.saveNumber(savedNumber)
                                        Toast.makeText(context, "번호가 저장되었습니다!", Toast.LENGTH_SHORT).show()
                                        showSaveDialog = null
                                    }
                                ) {
                                    Text("저장")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSaveDialog = null }) {
                                    Text("취소")
                                }
                            }
                        )
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
                            text = "오류가 발생했습니다",
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
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.recommendNumbers(numberOfSets) }
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("다시 시도")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 공유할 텍스트 생성 함수
 */
private fun buildShareText(sets: List<com.lotto.app.data.model.LottoSet>, lastDraw: Int): String {
    val builder = StringBuilder()
    
    builder.append("🎰 AI 로또 번호 추천\n")
    builder.append("━━━━━━━━━━━━━━━━\n")
    builder.append("기준: ${lastDraw}회차까지\n\n")
    
    sets.forEachIndexed { index, lottoSet ->
        builder.append("세트 ${index + 1}: ")
        builder.append(lottoSet.numbers.joinToString(", ") { String.format("%02d", it) })
        builder.append("\n")
    }
    
    builder.append("\n━━━━━━━━━━━━━━━━\n")
    builder.append("💡 과거 출현 빈도 기반 AI 추천\n")
    builder.append("📱 로또 번호 추천 앱\n")
    
    return builder.toString()
}

/**
 * 카카오톡으로 공유하기
 */
private fun shareToKakao(
    context: Context,
    sets: List<com.lotto.app.data.model.LottoSet>,
    lastDraw: Int
) {
    // 추천 번호를 텍스트로 변환
    val numbersText = sets.mapIndexed { index, lottoSet ->
        "세트 ${index + 1}: ${lottoSet.numbers.joinToString(", ") { String.format("%02d", it) }}"
    }.joinToString("\n")
    
    // 카카오톡 피드 템플릿 생성
    val feedTemplate = FeedTemplate(
        content = Content(
            title = "🎰 AI 로또 번호 추천",
            description = "과거 ${lastDraw}회차 데이터 기반 분석\n\n$numbersText",
            imageUrl = "https://via.placeholder.com/400x200/FFD700/000000?text=LOTTO",
            link = Link(
                webUrl = "https://www.dhlottery.co.kr",
                mobileWebUrl = "https://m.dhlottery.co.kr"
            )
        ),
        buttons = listOf(
            Button(
                title = "로또 구매하러 가기",
                link = Link(
                    webUrl = "https://www.dhlottery.co.kr",
                    mobileWebUrl = "https://m.dhlottery.co.kr"
                )
            )
        )
    )
    
    // 카카오톡 설치 확인 및 공유
    if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
        // 카카오톡으로 공유
        ShareClient.instance.shareDefault(context, feedTemplate) { sharingResult, error ->
            if (error != null) {
                Toast.makeText(context, "카카오톡 공유 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            } else if (sharingResult != null) {
                Toast.makeText(context, "카카오톡 공유 성공!", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        // 카카오톡 미설치: 웹 공유 사용
        val sharerUrl = WebSharerClient.instance.makeDefaultUrl(feedTemplate)
        
        try {
            // CustomTabs으로 웹 공유
            val intent = Intent(Intent.ACTION_VIEW, sharerUrl)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "카카오톡 공유를 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * 추천 모드 선택 카드
 */
@Composable
fun ModeSelectionCard(
    selectedMode: String,
    onModeSelected: (String) -> Unit
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
                text = "🎯 추천 모드",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 모드 버튼들
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeButton(
                    mode = "ai",
                    icon = "🤖",
                    title = "AI 추천 (기본)",
                    description = "통계 기반 최적화 번호",
                    isSelected = selectedMode == "ai",
                    onClick = { onModeSelected("ai") }
                )
                
                ModeButton(
                    mode = "random",
                    icon = "🎲",
                    title = "랜덤",
                    description = "완전 랜덤 추천",
                    isSelected = selectedMode == "random",
                    onClick = { onModeSelected("random") }
                )
                
                ModeButton(
                    mode = "conservative",
                    icon = "🛡️",
                    title = "보수적",
                    description = "자주 나온 번호 위주",
                    isSelected = selectedMode == "conservative",
                    onClick = { onModeSelected("conservative") }
                )
                
                ModeButton(
                    mode = "aggressive",
                    icon = "⚡",
                    title = "공격적",
                    description = "넓은 범위 추천",
                    isSelected = selectedMode == "aggressive",
                    onClick = { onModeSelected("aggressive") }
                )
            }
        }
    }
}

/**
 * 개별 모드 버튼
 */
@Composable
fun ModeButton(
    mode: String,
    icon: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurface
        ),
        elevation = if (isSelected) 
            ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        else 
            ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
