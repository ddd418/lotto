package com.lotto.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lotto.app.viewmodel.UserSettingsViewModel

/**
 * 사용자 설정 화면 (백엔드 API 연동)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: com.lotto.app.viewmodel.ThemeViewModel,
    authViewModel: com.lotto.app.viewmodel.AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: UserSettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var showLuckyNumberDialog by remember { mutableStateOf(false) }
    var showExcludeNumberDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 에러 처리
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    // 성공 메시지
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }
    
    // 설정 로드 시 ThemeViewModel 동기화
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    LaunchedEffect(settings) {
        settings?.let { currentSettings ->
            themeViewModel.setThemeMode(context, currentSettings.themeMode, isSystemDark)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("설정") },
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && settings == null) {
                // 초기 로딩
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                settings?.let { currentSettings ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 번호 설정
                        item {
                            SectionTitle("번호 설정")
                        }
                        
                        item {
                            SettingCard(
                                title = "행운 번호",
                                subtitle = if (currentSettings.luckyNumbers.isNullOrEmpty()) {
                                    "설정된 행운 번호가 없습니다"
                                } else {
                                    currentSettings.luckyNumbers.joinToString(", ")
                                },
                                icon = Icons.Default.Star,
                                onClick = { showLuckyNumberDialog = true }
                            )
                        }
                        
                        item {
                            SettingCard(
                                title = "제외 번호",
                                subtitle = if (currentSettings.excludeNumbers.isNullOrEmpty()) {
                                    "설정된 제외 번호가 없습니다"
                                } else {
                                    currentSettings.excludeNumbers.joinToString(", ")
                                },
                                icon = Icons.Default.Clear,
                                onClick = { showExcludeNumberDialog = true }
                            )
                        }
                        
                        // 알림 설정
                        item {
                            SectionTitle("알림 설정")
                        }
                        
                        item {
                            SwitchSettingCard(
                                title = "추첨일 알림",
                                subtitle = "매주 토요일 추첨 전 알림",
                                icon = Icons.Default.Notifications,
                                checked = currentSettings.enableDrawNotifications,
                                onCheckedChange = { checked ->
                                    viewModel.updateNotificationSettings(
                                        drawNotification = checked,
                                        winningNotification = currentSettings.enableWinningNotifications,
                                        promotionNotification = currentSettings.enablePushNotifications
                                    )
                                }
                            )
                        }
                        
                        item {
                            SwitchSettingCard(
                                title = "당첨 결과 알림",
                                subtitle = "당첨 번호 발표 시 알림",
                                icon = Icons.Default.CheckCircle,
                                checked = currentSettings.enableWinningNotifications,
                                onCheckedChange = { checked ->
                                    viewModel.updateNotificationSettings(
                                        drawNotification = currentSettings.enableDrawNotifications,
                                        winningNotification = checked,
                                        promotionNotification = currentSettings.enablePushNotifications
                                    )
                                }
                            )
                        }
                        
                        item {
                            SwitchSettingCard(
                                title = "푸시 알림",
                                subtitle = "이벤트 및 프로모션 소식",
                                icon = Icons.Default.Info,
                                checked = currentSettings.enablePushNotifications,
                                onCheckedChange = { checked ->
                                    viewModel.updateNotificationSettings(
                                        drawNotification = currentSettings.enableDrawNotifications,
                                        winningNotification = currentSettings.enableWinningNotifications,
                                        promotionNotification = checked
                                    )
                                }
                            )
                        }
                        
                        // 화면 설정
                        item {
                            SectionTitle("화면 설정")
                        }
                        
                        item {
                            val context = LocalContext.current
                            val isSystemDark = isSystemInDarkTheme()
                            
                            ThemeSettingCard(
                                currentTheme = currentSettings.themeMode,
                                onThemeChange = { theme ->
                                    // 백엔드 API 업데이트
                                    viewModel.updateTheme(theme)
                                    
                                    // ThemeViewModel 업데이트 (새로운 메서드 사용)
                                    themeViewModel.setThemeMode(context, theme, isSystemDark)
                                }
                            )
                        }                        // 계정 설정
                        item {
                            SectionTitle("계정")
                        }
                        
                        item {
                            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                            val currentUser by authViewModel.currentUser.collectAsState()
                            
                            if (isLoggedIn && currentUser != null) {
                                // 로그인 상태
                                Column {
                                    SettingCard(
                                        title = "로그인 정보",
                                        subtitle = currentUser?.nickname ?: "사용자",
                                        icon = Icons.Default.Person,
                                        onClick = {}
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // 로그아웃 버튼
                                    Button(
                                        onClick = {
                                            authViewModel.logout()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("로그아웃")
                                    }
                                }
                            } else {
                                // 로그아웃 상태
                                Button(
                                    onClick = onNavigateToLogin,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("카카오 로그인")
                                }
                            }
                        }
                        
                        // 앱 정보
                        item {
                            SectionTitle("앱 정보")
                        }
                        
                        item {
                            SettingCard(
                                title = "버전 정보",
                                subtitle = "1.0.0",
                                icon = Icons.Default.Info,
                                onClick = {}
                            )
                        }
                    }
                }
            }
            
            // 로딩 오버레이 (설정 변경 중)
            if (isLoading && settings != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    
    // 행운 번호 선택 다이얼로그
    if (showLuckyNumberDialog) {
        NumberSelectionDialog(
            title = "행운 번호 선택",
            initialNumbers = settings?.luckyNumbers ?: emptyList(),
            maxSelection = 6,
            disabledNumbers = settings?.excludeNumbers ?: emptyList(), // 제외 번호는 선택 불가
            onDismiss = { showLuckyNumberDialog = false },
            onConfirm = { numbers ->
                viewModel.updateLuckyNumbers(numbers)
                showLuckyNumberDialog = false
            }
        )
    }
    
    // 제외 번호 선택 다이얼로그
    if (showExcludeNumberDialog) {
        NumberSelectionDialog(
            title = "제외 번호 선택",
            initialNumbers = settings?.excludeNumbers ?: emptyList(),
            maxSelection = 20,
            disabledNumbers = settings?.luckyNumbers ?: emptyList(), // 행운 번호는 선택 불가
            onDismiss = { showExcludeNumberDialog = false },
            onConfirm = { numbers ->
                viewModel.updateExcludeNumbers(numbers)
                showExcludeNumberDialog = false
            }
        )
    }
}

/**
 * 섹션 제목
 */
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * 설정 카드
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "열기",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * 스위치 설정 카드
 */
@Composable
fun SwitchSettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * 테마 설정 카드
 */
@Composable
fun ThemeSettingCard(
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "테마",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    label = "라이트",
                    value = "light",
                    isSelected = currentTheme == "light",
                    onClick = { onThemeChange("light") },
                    modifier = Modifier.weight(1f)
                )
                
                ThemeOption(
                    label = "다크",
                    value = "dark",
                    isSelected = currentTheme == "dark",
                    onClick = { onThemeChange("dark") },
                    modifier = Modifier.weight(1f)
                )
                
                ThemeOption(
                    label = "시스템",
                    value = "system",
                    isSelected = currentTheme == "system",
                    onClick = { onThemeChange("system") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 테마 옵션
 */
@Composable
fun ThemeOption(
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Text(
            text = label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 번호 선택 다이얼로그
 */
@Composable
fun NumberSelectionDialog(
    title: String,
    initialNumbers: List<Int>,
    maxSelection: Int,
    disabledNumbers: List<Int> = emptyList(), // 선택 불가능한 번호들
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    var selectedNumbers by remember { mutableStateOf(initialNumbers.toSet()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${selectedNumbers.size}/$maxSelection 선택",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 선택된 번호 표시
                if (selectedNumbers.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedNumbers.sorted().forEach { number ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                                MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = number.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 번호 그리드
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0..8) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..4) {
                                val number = row * 5 + col + 1
                                if (number <= 45) {
                                    val isSelected = selectedNumbers.contains(number)
                                    val isDisabled = disabledNumbers.contains(number)
                                    
                                    OutlinedButton(
                                        onClick = {
                                            if (!isDisabled) {
                                                selectedNumbers = if (isSelected) {
                                                    selectedNumbers - number
                                                } else {
                                                    if (selectedNumbers.size < maxSelection) {
                                                        selectedNumbers + number
                                                    } else {
                                                        selectedNumbers
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp),
                                        enabled = !isDisabled,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else if (isDisabled) {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            } else {
                                                Color.Transparent
                                            },
                                            contentColor = if (isSelected) {
                                                Color.White
                                            } else if (isDisabled) {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else if (isDisabled) {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            }
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = number.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
            Button(onClick = { onConfirm(selectedNumbers.sorted()) }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
