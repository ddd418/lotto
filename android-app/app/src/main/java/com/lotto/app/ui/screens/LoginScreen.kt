package com.lotto.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lotto.app.R
import com.lotto.app.ui.theme.NotionColors
import com.lotto.app.ui.components.*
import com.lotto.app.viewmodel.AuthViewModel
import com.lotto.app.viewmodel.SubscriptionViewModel
import com.lotto.app.viewmodel.ThemeViewModel
import com.lotto.app.viewmodel.UserSettingsViewModel
import com.lotto.app.viewmodel.UiState

/**
 * 로그인 화면 (노션 스타일)
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean) -> Unit,  // Boolean: 신규 가입자 여부
    viewModel: AuthViewModel = viewModel(),
    subscriptionViewModel: SubscriptionViewModel? = null,
    themeViewModel: ThemeViewModel? = null,
    userSettingsViewModel: UserSettingsViewModel? = null
) {
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val isNewUser by viewModel.isNewUser.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    
    // 테마 적용 완료 여부를 추적
    var themeApplied by remember { mutableStateOf(false) }
    
    // 로그인 성공 시 구독 상태 및 테마 설정 서버와 동기화
    LaunchedEffect(loginState) {
        if (loginState is UiState.Success && !themeApplied) {
            // 구독 상태를 서버와 동기화
            subscriptionViewModel?.syncWithServer()
            
            // 사용자 설정(테마 포함)을 서버에서 로드
            userSettingsViewModel?.loadSettings()
        }
    }
    
    // 서버에서 로드한 테마 설정을 ThemeViewModel에 적용
    val settings by userSettingsViewModel?.settings?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    LaunchedEffect(settings) {
        if (settings != null && !themeApplied && loginState is UiState.Success) {
            settings?.let { currentSettings ->
                // 서버에서 받은 테마 설정 적용
                themeViewModel?.setThemeMode(context, currentSettings.themeMode, isSystemDark)
                android.util.Log.d("LoginScreen", "✅ 서버 테마 적용: ${currentSettings.themeMode}")
                
                themeApplied = true
                // 테마 적용 후 화면 이동
                onLoginSuccess(isNewUser)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NotionColors.Gray50),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            // 앱 로고 및 제목
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 로고 (로봇 + 복권)
                Text(
                    text = "🎫",
                    fontSize = 64.sp
                )
                
                Text(
                    text = "로또연구소",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NotionColors.TextPrimary
                )
                
                Text(
                    text = "AI 기반 데이터 분석으로\n최적의 번호를 찾아보세요",
                    fontSize = 16.sp,
                    color = NotionColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
            
            // 기능 소개
            NotionCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "✨ 주요 기능",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NotionColors.TextPrimary
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureItem(
                            icon = "🤖",
                            title = "AI 번호 추천",
                            description = "과거 데이터 분석 기반 4가지 전략"
                        )
                        FeatureItem(
                            icon = "💾",
                            title = "번호 저장",
                            description = "관심 번호 저장 및 관리"
                        )
                        FeatureItem(
                            icon = "🏆",
                            title = "당첨 확인",
                            description = "자동 당첨 여부 확인"
                        )
                        FeatureItem(
                            icon = "📊",
                            title = "통계 분석",
                            description = "번호별 출현 빈도 분석"
                        )
                    }
                }
            }
            
            // 로그인 버튼
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 카카오 로그인 버튼
                Button(
                    onClick = { 
                        viewModel.loginWithKakao(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFE812), // 카카오 노란색
                        contentColor = Color(0xFF3C1E1E)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = loginState !is UiState.Loading
                ) {
                    if (loginState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF3C1E1E)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "💬", fontSize = 20.sp)
                            Text(
                                text = "카카오 로그인",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // 오류 메시지
            when (val currentState = loginState) {
                is UiState.Error -> {
                    NotionCard(backgroundColor = NotionColors.Red100) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "⚠️", fontSize = 16.sp)
                            Text(
                                text = currentState.message,
                                color = NotionColors.Error,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

