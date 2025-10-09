package com.lotto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lotto.app.ui.screens.MainScreen
import com.lotto.app.ui.screens.RecommendScreen
import com.lotto.app.ui.screens.StatsScreen
import com.lotto.app.ui.theme.LottoAppTheme
import com.lotto.app.viewmodel.LottoViewModel

/**
 * 네비게이션 라우트
 */
sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Recommend : Screen("recommend")
    object Stats : Screen("stats")
}

/**
 * 메인 액티비티
 */
class MainActivity : ComponentActivity() {
    private val viewModel: LottoViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LottoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LottoApp(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * 앱 메인 컴포저블 (네비게이션)
 */
@Composable
fun LottoApp(viewModel: LottoViewModel) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        // 메인 화면
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToRecommend = {
                    navController.navigate(Screen.Recommend.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
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
    }
}
