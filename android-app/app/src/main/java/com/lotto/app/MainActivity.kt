package com.lotto.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lotto.app.ui.screens.AnalysisDashboardScreen
import com.lotto.app.ui.screens.CheckWinningScreen
import com.lotto.app.ui.screens.LoginScreen
import com.lotto.app.ui.screens.MainScreen
import com.lotto.app.ui.screens.PlanSelectionScreen
import com.lotto.app.ui.screens.RecommendScreen
import com.lotto.app.ui.screens.SavedNumbersScreen
import com.lotto.app.ui.screens.SettingsScreen
import com.lotto.app.ui.screens.StatsScreen
import com.lotto.app.ui.screens.VirtualDrawScreen
import com.lotto.app.ui.theme.LottoAppTheme
import com.lotto.app.viewmodel.AuthViewModel
import com.lotto.app.viewmodel.AuthViewModelFactory
import com.lotto.app.viewmodel.LottoViewModel
import com.lotto.app.viewmodel.SavedNumberViewModel
import com.lotto.app.viewmodel.SubscriptionViewModel
import com.lotto.app.viewmodel.SubscriptionViewModelFactory
import com.lotto.app.viewmodel.ThemeViewModel

/**
 * 네비게이션 라우트
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object PlanSelection : Screen("plan_selection")
    object Main : Screen("main")
    object Recommend : Screen("recommend")
    object Stats : Screen("stats")
    object SavedNumbers : Screen("saved_numbers")
    object CheckWinning : Screen("check_winning")
    object Settings : Screen("settings")
    object Analysis : Screen("analysis")
    object VirtualDraw : Screen("virtual_draw")
    object Onboarding : Screen("onboarding")
    object Subscription : Screen("subscription")
    object SubscriptionStatus : Screen("subscription_status")
}

/**
 * 메인 액티비티
 */
class MainActivity : ComponentActivity() {
    private val viewModel: LottoViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 테마 설정 로드
        themeViewModel.loadThemePreference(this)
        
        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            
            // 디버깅 로그
            android.util.Log.d("MainActivity", "Recomposition: isDarkMode=$isDarkMode, themeMode=$themeMode")
            
            // 시스템 테마 모드일 때 시스템 다크 모드 변경 감지
            LaunchedEffect(isSystemDark, themeMode) {
                if (themeMode == "system") {
                    themeViewModel.setThemeMode(this@MainActivity, "system", isSystemDark)
                }
            }
            
            LottoAppTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LottoApp(
                        viewModel = viewModel, 
                        themeViewModel = themeViewModel,
                        context = this@MainActivity
                    )
                }
            }
        }
    }
}

/**
 * 앱 메인 컴포저블 (네비게이션)
 */
@Composable
fun LottoApp(
    viewModel: LottoViewModel, 
    themeViewModel: ThemeViewModel,
    context: Context
) {
    val navController = rememberNavController()
    
    // AuthViewModel을 Composable 내에서 생성
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context)
    )
    
    // SubscriptionViewModel 생성 (Factory 사용)
    val subscriptionViewModel: SubscriptionViewModel = viewModel(
        factory = SubscriptionViewModelFactory(context)
    )
    
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    
    // 체험 종료 임박 알림
    val shouldShowTrialWarning by subscriptionViewModel.shouldShowTrialWarning.collectAsStateWithLifecycle()
    val trialWarningDays by subscriptionViewModel.trialWarningDays.collectAsStateWithLifecycle()
    
    // 알림 다이얼로그 표시
    if (shouldShowTrialWarning && trialWarningDays > 0) {
        com.lotto.app.ui.components.TrialExpirationDialog(
            daysRemaining = trialWarningDays,
            onDismiss = {
                subscriptionViewModel.dismissTrialWarning()
            },
            onUpgrade = {
                subscriptionViewModel.dismissTrialWarning()
                navController.navigate(Screen.Subscription.route)
            }
        )
    }
    
    // 로그인 상태에 따라 시작 화면 결정
    val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Login.route
    
    // 로그인 상태 변경 감지하여 자동 네비게이션
    LaunchedEffect(isLoggedIn) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        
        when {
            // 로그인 성공 시 메인 화면으로 이동
            isLoggedIn && currentRoute == Screen.Login.route -> {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            // 로그아웃 시 로그인 화면으로 이동
            !isLoggedIn && currentRoute != Screen.Login.route -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 로그인 화면
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                subscriptionViewModel = subscriptionViewModel,
                onLoginSuccess = { isNewUser ->
                    if (isNewUser) {
                        // 신규 가입자는 플랜 선택 화면으로
                        navController.navigate(Screen.PlanSelection.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        // 기존 사용자는 메인 화면으로
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // 플랜 선택 화면 (첫 가입자용)
        composable(Screen.PlanSelection.route) {
            PlanSelectionScreen(
                onFreePlanSelected = {
                    // 무료 플랜 선택 - 트라이얼 시작
                    subscriptionViewModel.startTrial()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.PlanSelection.route) { inclusive = true }
                    }
                },
                onProPlanSelected = {
                    // 프로 플랜 선택 - 구독 화면으로 이동
                    navController.navigate(Screen.Subscription.route) {
                        popUpTo(Screen.PlanSelection.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 메인 화면
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToRecommend = {
                    navController.navigate(Screen.Recommend.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToSavedNumbers = {
                    navController.navigate(Screen.SavedNumbers.route)
                },
                onNavigateToCheckWinning = {
                    navController.navigate(Screen.CheckWinning.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAnalysis = {
                    navController.navigate(Screen.Analysis.route)
                },
                onNavigateToVirtualDraw = {
                    navController.navigate(Screen.VirtualDraw.route)
                }
            )
        }
        
        // 추천 화면
        composable(Screen.Recommend.route) {
            val savedNumberViewModel: SavedNumberViewModel = viewModel()
            
            RecommendScreen(
                viewModel = viewModel,
                savedNumberViewModel = savedNumberViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 통계 화면
        composable(Screen.Stats.route) {
            StatsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 저장된 번호 화면
        composable(Screen.SavedNumbers.route) {
            // 로그인 체크
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SavedNumbers.route) { inclusive = true }
                    }
                }
            }
            
            if (isLoggedIn) {
                SavedNumbersScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        // 당첨 확인 화면
        composable(Screen.CheckWinning.route) {
            // 로그인 체크
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.CheckWinning.route) { inclusive = true }
                    }
                }
            }
            
            if (isLoggedIn) {
                // SavedNumberViewModel 인스턴스 생성
                val savedNumberViewModel: SavedNumberViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return SavedNumberViewModel() as T
                        }
                    }
                )
                
                CheckWinningScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    savedNumberViewModel = savedNumberViewModel
                )
            }
        }
        
        // 설정 화면
        composable(Screen.Settings.route) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToSubscription = {
                    navController.navigate(Screen.SubscriptionStatus.route)
                }
            )
        }
        
        // 번호 분석 화면
        composable(Screen.Analysis.route) {
            AnalysisDashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 가상 추첨 화면
        composable(Screen.VirtualDraw.route) {
            VirtualDrawScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 구독 상태 화면
        composable(Screen.SubscriptionStatus.route) {
            com.lotto.app.ui.screens.SubscriptionStatusScreen(
                viewModel = subscriptionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSubscription = {
                    // 구독 화면으로 이동 (있다면)
                    // navController.navigate(Screen.Subscription.route)
                }
            )
        }
    }
}
