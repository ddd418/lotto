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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lotto.app.ui.screens.AnalysisDashboardScreen
import com.lotto.app.ui.screens.CheckWinningScreen
import com.lotto.app.ui.screens.LoginScreen
import com.lotto.app.ui.screens.MainScreen
import com.lotto.app.ui.screens.RecommendScreen
import com.lotto.app.ui.screens.SavedNumbersScreen
import com.lotto.app.ui.screens.SettingsScreen
import com.lotto.app.ui.screens.StatsScreen
import com.lotto.app.ui.screens.VirtualDrawScreen
import com.lotto.app.ui.theme.LottoAppTheme
import com.lotto.app.viewmodel.AuthViewModel
import com.lotto.app.viewmodel.AuthViewModelFactory
import com.lotto.app.viewmodel.LottoViewModel
import com.lotto.app.viewmodel.ThemeViewModel

/**
 * 네비게이션 라우트
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object Recommend : Screen("recommend")
    object Stats : Screen("stats")
    object SavedNumbers : Screen("saved_numbers")
    object CheckWinning : Screen("check_winning")
    object Settings : Screen("settings")
    object Analysis : Screen("analysis")
    object VirtualDraw : Screen("virtual_draw")
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
    
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    
    // 항상 메인 화면에서 시작 (로그인 선택 사항)
    val startDestination = Screen.Main.route
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 로그인 화면
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
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
            RecommendScreen(
                viewModel = viewModel,
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
            SavedNumbersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 당첨 확인 화면
        composable(Screen.CheckWinning.route) {
            CheckWinningScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
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
    }
}
