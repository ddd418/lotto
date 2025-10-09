# 로또 앱 API 사용 가이드

Android 앱에서 백엔드 API를 호출하는 방법을 설명합니다.

---

## 🔧 기본 설정

### Retrofit 설정 (Android)

```kotlin
// build.gradle.kts (Module: app)
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
}
```

### API 클라이언트

```kotlin
// network/ApiClient.kt
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"  // 에뮬레이터
    // private const val BASE_URL = "http://localhost:8000/"  // 실제 기기 (같은 네트워크)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = TokenManager.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
```

---

## 📱 API 인터페이스 정의

### 1. 당첨 번호 API

```kotlin
// network/WinningNumberApi.kt
interface WinningNumberApi {
    @GET("api/winning-numbers/latest")
    suspend fun getLatestWinning(): WinningNumberResponse

    @GET("api/winning-numbers/{drawNumber}")
    suspend fun getWinningByDraw(@Path("drawNumber") drawNumber: Int): WinningNumberResponse

    @GET("api/winning-numbers")
    suspend fun getWinningList(@Query("limit") limit: Int = 10): WinningNumberListResponse

    @POST("api/winning-numbers/sync")
    suspend fun syncWinningNumbers(
        @Query("start_draw") startDraw: Int,
        @Query("end_draw") endDraw: Int? = null
    ): SyncResponse
}

// 사용 예제
class WinningNumberRepository {
    private val api = ApiClient.retrofit.create(WinningNumberApi::class.java)

    suspend fun getLatestWinning(): Result<WinningNumberResponse> = try {
        val response = api.getLatestWinning()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getWinningHistory(limit: Int = 10): Result<WinningNumberListResponse> = try {
        val response = api.getWinningList(limit)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 2. 저장된 번호 API

```kotlin
// network/SavedNumberApi.kt
interface SavedNumberApi {
    @POST("api/saved-numbers")
    suspend fun saveNumber(@Body request: SavedNumberRequest): SavedNumberResponse

    @GET("api/saved-numbers")
    suspend fun getSavedNumbers(): List<SavedNumberResponse>

    @PUT("api/saved-numbers/{id}")
    suspend fun updateNumber(
        @Path("id") id: Int,
        @Body request: SavedNumberRequest
    ): SavedNumberResponse

    @DELETE("api/saved-numbers/{id}")
    suspend fun deleteNumber(@Path("id") id: Int): MessageResponse
}

// Data Models
data class SavedNumberRequest(
    val numbers: List<Int>,
    val nickname: String? = null,
    val memo: String? = null,
    val isFavorite: Boolean = false,
    val recommendationType: String? = null
)

data class SavedNumberResponse(
    val id: Int,
    val numbers: List<Int>,
    val nickname: String?,
    val memo: String?,
    val isFavorite: Boolean,
    val recommendationType: String?,
    val createdAt: String
)

// 사용 예제
class SavedNumberRepository(private val api: SavedNumberApi) {

    suspend fun saveNumber(numbers: List<Int>, nickname: String? = null): Result<SavedNumberResponse> = try {
        val request = SavedNumberRequest(
            numbers = numbers,
            nickname = nickname,
            isFavorite = false
        )
        val response = api.saveNumber(request)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllSavedNumbers(): Result<List<SavedNumberResponse>> = try {
        val response = api.getSavedNumbers()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateNumber(id: Int, numbers: List<Int>): Result<SavedNumberResponse> = try {
        val request = SavedNumberRequest(numbers = numbers)
        val response = api.updateNumber(id, request)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteNumber(id: Int): Result<MessageResponse> = try {
        val response = api.deleteNumber(id)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 3. 당첨 확인 API

```kotlin
// network/WinningCheckApi.kt
interface WinningCheckApi {
    @POST("api/check-winning")
    suspend fun checkWinning(@Body request: CheckWinningRequest): CheckWinningResponse

    @GET("api/winning-history")
    suspend fun getWinningHistory(@Query("limit") limit: Int = 20): List<WinningHistoryItem>
}

data class CheckWinningRequest(
    val numbers: List<Int>,
    val drawNumber: Int
)

data class CheckWinningResponse(
    val success: Boolean,
    val drawNumber: Int,
    val userNumbers: List<Int>,
    val winningNumbers: List<Int>,
    val bonusNumber: Int,
    val matchedCount: Int,
    val hasBonus: Boolean,
    val rank: Int?,
    val prizeAmount: Int?,
    val message: String
)

// 사용 예제
class WinningCheckRepository(private val api: WinningCheckApi) {

    suspend fun checkWinning(numbers: List<Int>, drawNumber: Int): Result<CheckWinningResponse> = try {
        val request = CheckWinningRequest(numbers, drawNumber)
        val response = api.checkWinning(request)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getHistory(limit: Int = 20): Result<List<WinningHistoryItem>> = try {
        val response = api.getWinningHistory(limit)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 4. 사용자 설정 API

```kotlin
// network/UserSettingsApi.kt
interface UserSettingsApi {
    @GET("api/settings")
    suspend fun getSettings(): UserSettingsResponse

    @PUT("api/settings")
    suspend fun updateSettings(@Body request: UserSettingsRequest): UserSettingsResponse
}

data class UserSettingsRequest(
    val enablePushNotifications: Boolean? = null,
    val enableDrawNotifications: Boolean? = null,
    val enableWinningNotifications: Boolean? = null,
    val themeMode: String? = null,  // "light", "dark", "system"
    val defaultRecommendationType: String? = null,
    val luckyNumbers: List<Int>? = null,
    val excludeNumbers: List<Int>? = null
)

data class UserSettingsResponse(
    val userId: Int,
    val enablePushNotifications: Boolean,
    val enableDrawNotifications: Boolean,
    val enableWinningNotifications: Boolean,
    val themeMode: String,
    val defaultRecommendationType: String,
    val luckyNumbers: List<Int>?,
    val excludeNumbers: List<Int>?,
    val createdAt: String,
    val updatedAt: String?
)

// 사용 예제
class SettingsRepository(private val api: UserSettingsApi) {

    suspend fun getSettings(): Result<UserSettingsResponse> = try {
        val response = api.getSettings()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateLuckyNumbers(numbers: List<Int>): Result<UserSettingsResponse> = try {
        val request = UserSettingsRequest(luckyNumbers = numbers)
        val response = api.updateSettings(request)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateTheme(theme: String): Result<UserSettingsResponse> = try {
        val request = UserSettingsRequest(themeMode = theme)
        val response = api.updateSettings(request)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 🎨 UI 구현 예제

### 1. 저장된 번호 화면

```kotlin
@Composable
fun SavedNumbersScreen(
    viewModel: SavedNumberViewModel = hiltViewModel()
) {
    val savedNumbers by viewModel.savedNumbers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(savedNumbers) { saved ->
                    SavedNumberCard(
                        saved = saved,
                        onEdit = { viewModel.editNumber(it) },
                        onDelete = { viewModel.deleteNumber(it.id) },
                        onCheckWinning = { viewModel.checkWinning(it) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.showAddDialog() }
        ) {
            Icon(Icons.Default.Add, "Add")
        }
    }
}

@Composable
fun SavedNumberCard(
    saved: SavedNumberResponse,
    onEdit: (SavedNumberResponse) -> Unit,
    onDelete: (SavedNumberResponse) -> Unit,
    onCheckWinning: (SavedNumberResponse) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 별칭
            if (saved.nickname != null) {
                Text(
                    text = saved.nickname,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // 번호 표시
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                saved.numbers.forEach { number ->
                    LottoNumberBall(number = number)
                }
            }

            // 메모
            if (saved.memo != null) {
                Text(
                    text = saved.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // 액션 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onCheckWinning(saved) }) {
                    Text("당첨 확인")
                }
                TextButton(onClick = { onEdit(saved) }) {
                    Text("수정")
                }
                TextButton(onClick = { onDelete(saved) }) {
                    Text("삭제")
                }
            }
        }
    }
}
```

### 2. 당첨 확인 화면

```kotlin
@Composable
fun CheckWinningScreen(
    numbers: List<Int>,
    viewModel: WinningCheckViewModel = hiltViewModel()
) {
    val result by viewModel.checkResult.collectAsState()
    val latestDraw by viewModel.latestDraw.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLatestDraw()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "당첨 확인",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 확인할 번호
        Text("확인할 번호", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            numbers.forEach { number ->
                LottoNumberBall(number = number)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 회차 선택
        Text("회차 선택", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = latestDraw?.toString() ?: "",
            onValueChange = { viewModel.setDrawNumber(it.toIntOrNull()) },
            label = { Text("회차 번호") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 확인 버튼
        Button(
            onClick = { viewModel.checkWinning(numbers) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("당첨 확인")
        }

        // 결과 표시
        result?.let { res ->
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (res.rank != null) {
                        Color(0xFF4CAF50)  // 당첨: 초록색
                    } else {
                        Color(0xFFE0E0E0)  // 미당첨: 회색
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = res.message,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (res.rank != null) Color.White else Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "맞춘 개수: ${res.matchedCount}개",
                        color = if (res.rank != null) Color.White else Color.Black
                    )

                    if (res.rank != null && res.prizeAmount != null) {
                        Text(
                            text = "예상 당첨금: ${formatPrize(res.prizeAmount)}원",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }

            // 당첨 번호 표시
            Spacer(modifier = Modifier.height(16.dp))
            Text("${res.drawNumber}회차 당첨번호")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                res.winningNumbers.forEach { number ->
                    LottoNumberBall(
                        number = number,
                        isMatched = number in numbers
                    )
                }
                Text("+", modifier = Modifier.align(Alignment.CenterVertically))
                LottoNumberBall(
                    number = res.bonusNumber,
                    isBonus = true,
                    isMatched = res.hasBonus
                )
            }
        }
    }
}

@Composable
fun LottoNumberBall(
    number: Int,
    isMatched: Boolean = false,
    isBonus: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = when {
                    isMatched -> Color(0xFF4CAF50)  // 맞춘 번호: 초록색
                    isBonus -> Color(0xFFFF9800)     // 보너스: 주황색
                    number <= 10 -> Color(0xFFFDD835)  // 1-10: 노란색
                    number <= 20 -> Color(0xFF42A5F5)  // 11-20: 파란색
                    number <= 30 -> Color(0xFFEF5350)  // 21-30: 빨간색
                    number <= 40 -> Color(0xFF66BB6A)  // 31-40: 초록색
                    else -> Color(0xFF9E9E9E)          // 41-45: 회색
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatPrize(amount: Int): String {
    return when {
        amount >= 100000000 -> {
            val eok = amount / 100000000
            "${eok}억"
        }
        amount >= 10000 -> {
            val man = amount / 10000
            "${man}만"
        }
        else -> amount.toString()
    }
}
```

### 3. 설정 화면

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            // 로그인 화면으로 이동
        } else {
            viewModel.loadSettings()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // 알림 설정
        item {
            SettingSection(title = "알림 설정")
        }
        item {
            SwitchSetting(
                title = "푸시 알림",
                checked = settings?.enablePushNotifications ?: true,
                onCheckedChange = { viewModel.updatePushNotifications(it) }
            )
        }
        item {
            SwitchSetting(
                title = "추첨일 알림",
                checked = settings?.enableDrawNotifications ?: true,
                onCheckedChange = { viewModel.updateDrawNotifications(it) }
            )
        }

        // 행운의 번호 설정
        item {
            SettingSection(title = "개인화")
        }
        item {
            ClickableSetting(
                title = "행운의 번호",
                value = settings?.luckyNumbers?.joinToString(", ") ?: "설정 안함",
                onClick = { viewModel.showLuckyNumbersDialog() }
            )
        }
        item {
            ClickableSetting(
                title = "제외할 번호",
                value = settings?.excludeNumbers?.joinToString(", ") ?: "설정 안함",
                onClick = { viewModel.showExcludeNumbersDialog() }
            )
        }

        // 테마 설정
        item {
            SettingSection(title = "화면")
        }
        item {
            ClickableSetting(
                title = "테마",
                value = when (settings?.themeMode) {
                    "light" -> "라이트"
                    "dark" -> "다크"
                    else -> "시스템 설정"
                },
                onClick = { viewModel.showThemeDialog() }
            )
        }

        // 로그아웃
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { authViewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("로그아웃")
            }
        }
    }
}
```

---

## 🔍 에러 처리

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

fun <T> handleApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: HttpException) {
        ApiResult.Error(
            message = e.message() ?: "알 수 없는 오류",
            code = e.code()
        )
    } catch (e: IOException) {
        ApiResult.Error("네트워크 오류가 발생했습니다")
    } catch (e: Exception) {
        ApiResult.Error("오류가 발생했습니다: ${e.message}")
    }
}

// 사용 예제
viewModelScope.launch {
    _uiState.value = ApiResult.Loading

    val result = handleApiCall {
        repository.getSavedNumbers()
    }

    _uiState.value = result
}
```

---

## 📝 요약

1. **Retrofit 설정**: OkHttp + Logging + Auth Interceptor
2. **API 인터페이스**: 각 기능별 인터페이스 정의
3. **Repository 패턴**: API 호출 로직 분리
4. **ViewModel**: UI 상태 관리
5. **Compose UI**: 선언형 UI 구현
6. **에러 처리**: 통일된 에러 핸들링

이제 이 가이드를 참고하여 Android 앱을 완성하세요! 🚀
