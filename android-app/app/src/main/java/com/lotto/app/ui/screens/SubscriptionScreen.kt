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
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    // 에러 다이얼로그 표시
    var showErrorDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showErrorDialog = true
        }
    }
    
    if (showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.clearError()
            },
            title = { Text("구독 오류") },
            text = { Text(errorMessage ?: "알 수 없는 오류가 발생했습니다") },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    viewModel.clearError()
                }) {
                    Text("확인")
                }
            }
        )
    }
    
    // 화면 진입 시 구독 상태 새로고침
    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }
    
    // 이미 구독 중이면 자동으로 돌아가기
    LaunchedEffect(isProUser) {
        if (isProUser) {
            onSubscribed()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PRO 구독") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로 가기")
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
                
                Text(
                    text = "PRO",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFE812)
                )
                
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
                            icon = "💾",
                            title = "번호 저장",
                            description = "내 번호를 저장하고 관리"
                        )
                        
                        ProFeatureItem(
                            icon = "✅",
                            title = "당첨 확인",
                            description = "저장한 번호 자동 당첨 확인"
                        )
                        
                        ProFeatureItem(
                            icon = "🎲",
                            title = "가상 추첨",
                            description = "재미있는 추첨 시뮬레이션"
                        )
                        
                        ProFeatureItem(
                            icon = "📊",
                            title = "고급 분석",
                            description = "심화 패턴 및 확률 분석"
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
                                    text = "₩500",
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 구독 버튼
                Button(
                    onClick = {
                        viewModel.startSubscription(context as Activity)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFE812)
                    ),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "PRO 구독 시작하기",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
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
