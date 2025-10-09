package com.lotto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lotto.app.data.model.SavedNumberResponse
import com.lotto.app.ui.components.LottoNumberBall
import com.lotto.app.viewmodel.SavedNumberViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 저장된 번호 화면 (백엔드 API 연동)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedNumbersScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavedNumberViewModel = viewModel()
) {
    val savedNumbers by viewModel.savedNumbers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<SavedNumberResponse?>(null) }
    var showEditDialog by remember { mutableStateOf<SavedNumberResponse?>(null) }
    
    // 에러 메시지 표시
    LaunchedEffect(error) {
        error?.let {
            // Snackbar나 Toast로 에러 표시 (여기서는 로그로만)
            android.util.Log.e("SavedNumbersScreen", it)
        }
    }
    
    // 성공 메시지 표시
    LaunchedEffect(successMessage) {
        successMessage?.let {
            android.util.Log.d("SavedNumbersScreen", it)
            viewModel.clearSuccessMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "내 저장 번호",
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
                    // 새로고침 버튼
                    IconButton(onClick = { viewModel.loadSavedNumbers() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
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
            when {
                isLoading -> {
                    // 로딩 중
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                savedNumbers.isEmpty() -> {
                    // 빈 상태
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "저장된 번호가 없습니다",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "번호를 추천받고 저장해보세요!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
                
                else -> {
                    // 저장된 번호 목록
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "총 ${savedNumbers.size}개",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(savedNumbers, key = { it.id }) { number ->
                            SavedNumberCard(
                                number = number,
                                onDelete = { showDeleteDialog = number },
                                onToggleFavorite = { viewModel.toggleFavorite(number) },
                                onEdit = { showEditDialog = number }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
            
            // 에러 메시지 표시
            error?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("확인")
                        }
                    }
                ) {
                    Text(errorMsg)
                }
            }
        }
    }
    
    // 삭제 확인 다이얼로그
    showDeleteDialog?.let { number ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("번호 삭제") },
            text = { Text("이 번호를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNumber(number.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("취소")
                }
            }
        )
    }
    
    // 편집 다이얼로그
    showEditDialog?.let { number ->
        EditNumberDialog(
            number = number,
            onDismiss = { showEditDialog = null },
            onSave = { nickname, memo ->
                viewModel.updateNickname(number, nickname)
                viewModel.updateMemo(number, memo)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun SavedNumberCard(
    number: SavedNumberResponse,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (number.isFavorite) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 헤더: 별칭 또는 날짜 + 액션 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (number.nickname != null) {
                        Text(
                            text = number.nickname,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = formatDate(number.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    number.recommendationType?.let {
                        Text(
                            text = getRecommendationTypeText(it),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Row {
                    // 즐겨찾기 버튼
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (number.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "즐겨찾기",
                            tint = if (number.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    
                    // 편집 버튼
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "편집",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    
                    // 삭제 버튼
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 번호 표시
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                number.numbers.sorted().forEach { num ->
                    LottoNumberBall(number = num, size = 40.dp)
                }
            }
            
            // 메모 표시
            if (number.memo != null && number.memo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = number.memo,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EditNumberDialog(
    number: SavedNumberResponse,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var nicknameText by remember { mutableStateOf(number.nickname ?: "") }
    var memoText by remember { mutableStateOf(number.memo ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("번호 편집") },
        text = {
            Column {
                OutlinedTextField(
                    value = nicknameText,
                    onValueChange = { nicknameText = it },
                    label = { Text("별칭") },
                    placeholder = { Text("별칭을 입력하세요") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("메모") },
                    placeholder = { Text("메모를 입력하세요") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(nicknameText, memoText) }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

private fun getRecommendationTypeText(type: String): String {
    return when (type) {
        "ai" -> "AI 추천"
        "hot" -> "핫 번호"
        "cold" -> "콜드 번호"
        "balanced" -> "균형"
        "random" -> "랜덤"
        else -> type
    }
}
