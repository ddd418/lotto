# 로또 번호 추천 API 문서

## 📚 API 개요

이 API는 로또 번호 추천 서비스를 제공합니다. Android 앱에서 HTTP 요청을 통해 로또 번호를 추천받을 수 있습니다.

**베이스 URL**: `http://your-server-ip:8000`

---

## 🔌 API 엔드포인트

### 1. 헬스 체크

서버 상태 확인

**Endpoint**: `GET /api/health`

**Response**:

```json
{
  "status": "healthy",
  "version": "1.0.0",
  "stats_available": true,
  "last_draw": 1192
}
```

---

### 2. 로또 번호 추천 ⭐ (메인 기능)

AI 기반 로또 번호 추천

**Endpoint**: `POST /api/recommend`

**Request Body**:

```json
{
  "n_sets": 5,
  "seed": null
}
```

**Parameters**:

- `n_sets` (int, optional): 추천받을 번호 세트 개수 (기본값: 5, 범위: 1~10)
- `seed` (int, optional): 랜덤 시드 (재현성을 위해, 기본값: null)

**Response**:

```json
{
  "success": true,
  "last_draw": 1192,
  "generated_at": "2025-10-05T15:30:00",
  "include_bonus": false,
  "sets": [
    {
      "numbers": [3, 12, 17, 27, 34, 45]
    },
    {
      "numbers": [7, 13, 18, 24, 33, 40]
    },
    {
      "numbers": [1, 11, 20, 26, 37, 43]
    },
    {
      "numbers": [6, 14, 19, 31, 38, 44]
    },
    {
      "numbers": [10, 16, 21, 29, 36, 42]
    }
  ]
}
```

---

### 3. 통계 조회

로또 번호 출현 빈도 통계

**Endpoint**: `GET /api/stats`

**Response**:

```json
{
  "success": true,
  "last_draw": 1192,
  "generated_at": "2025-10-05T07:22:07",
  "include_bonus": false,
  "frequency": {
    "1": 161,
    "2": 150,
    "3": 165,
    ...
    "45": 168
  },
  "top_10": [
    {"number": 34, "count": 179},
    {"number": 12, "count": 173},
    {"number": 13, "count": 172},
    {"number": 27, "count": 171},
    {"number": 18, "count": 170},
    {"number": 14, "count": 169},
    {"number": 37, "count": 168},
    {"number": 40, "count": 168},
    {"number": 45, "count": 168},
    {"number": 17, "count": 166}
  ]
}
```

---

### 4. 데이터 업데이트

로또 회차 데이터 갱신 (관리자용, 시간 소요)

**Endpoint**: `POST /api/update`

**Request Body**:

```json
{
  "max_draw": null,
  "include_bonus": false
}
```

**Parameters**:

- `max_draw` (int, optional): 수집할 최대 회차 (null이면 최신까지 자동 수집)
- `include_bonus` (bool, optional): 보너스 번호 포함 여부 (기본값: false)

**Response**:

```json
{
  "success": true,
  "message": "1~1192회차 데이터 업데이트 완료",
  "last_draw": 1192,
  "updated_at": "2025-10-05T16:00:00"
}
```

---

### 5. 최신 회차 조회

저장된 최신 회차 정보

**Endpoint**: `GET /api/latest-draw`

**Response**:

```json
{
  "success": true,
  "last_draw": 1192,
  "generated_at": "2025-10-05T07:22:07",
  "include_bonus": false
}
```

---

## 📱 Android 앱 개발 가이드

### Retrofit 인터페이스 예제 (Kotlin)

```kotlin
// LottoApiService.kt
interface LottoApiService {

    @GET("api/health")
    suspend fun getHealth(): Response<HealthResponse>

    @POST("api/recommend")
    suspend fun recommendNumbers(
        @Body request: RecommendRequest
    ): Response<RecommendResponse>

    @GET("api/stats")
    suspend fun getStats(): Response<StatsResponse>

    @POST("api/update")
    suspend fun updateData(
        @Body request: UpdateRequest
    ): Response<UpdateResponse>

    @GET("api/latest-draw")
    suspend fun getLatestDraw(): Response<LatestDrawResponse>
}

// Data Classes
data class RecommendRequest(
    val n_sets: Int = 5,
    val seed: Int? = null
)

data class RecommendResponse(
    val success: Boolean,
    val last_draw: Int,
    val generated_at: String,
    val include_bonus: Boolean,
    val sets: List<LottoSet>
)

data class LottoSet(
    val numbers: List<Int>
)

data class HealthResponse(
    val status: String,
    val version: String,
    val stats_available: Boolean,
    val last_draw: Int?
)
```

### Retrofit 클라이언트 설정

```kotlin
// RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "http://your-server-ip:8000/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: LottoApiService by lazy {
        retrofit.create(LottoApiService::class.java)
    }
}
```

### 사용 예제

```kotlin
// MainActivity.kt
class MainActivity : AppCompatActivity() {

    private fun recommendLottoNumbers() {
        lifecycleScope.launch {
            try {
                val request = RecommendRequest(n_sets = 5)
                val response = RetrofitClient.apiService.recommendNumbers(request)

                if (response.isSuccessful) {
                    val data = response.body()
                    data?.sets?.forEachIndexed { index, set ->
                        Log.d("Lotto", "세트 ${index + 1}: ${set.numbers}")
                    }
                    // UI 업데이트
                    updateUI(data)
                } else {
                    Log.e("Lotto", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Lotto", "Exception: ${e.message}")
            }
        }
    }
}
```

### 필수 의존성 (build.gradle)

```gradle
dependencies {
    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // ViewModel & LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
}
```

### 권한 설정 (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:usesCleartextTraffic="true"
    ...>
```

---

## 🚀 서버 배포 가이드

### 로컬 테스트

```bash
python api_server.py
```

### 프로덕션 배포 (Ubuntu/Linux)

1. **서버에 파일 업로드**

```bash
scp -r lotto/ user@server-ip:/home/user/
```

2. **패키지 설치**

```bash
cd /home/user/lotto
pip install -r requirements.txt
```

3. **Systemd 서비스 등록** (`/etc/systemd/system/lotto-api.service`)

```ini
[Unit]
Description=Lotto API Service
After=network.target

[Service]
User=user
WorkingDirectory=/home/user/lotto
ExecStart=/usr/bin/python3 api_server.py
Restart=always

[Install]
WantedBy=multi-user.target
```

4. **서비스 시작**

```bash
sudo systemctl enable lotto-api
sudo systemctl start lotto-api
sudo systemctl status lotto-api
```

5. **방화벽 설정**

```bash
sudo ufw allow 8000/tcp
```

### Docker 배포 (선택사항)

**Dockerfile**:

```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY . .

RUN pip install --no-cache-dir -r requirements.txt

EXPOSE 8000

CMD ["python", "api_server.py"]
```

**실행**:

```bash
docker build -t lotto-api .
docker run -d -p 8000:8000 --name lotto-api lotto-api
```

---

## 🔒 보안 권장사항

1. **CORS 설정**: `api_server.py`에서 `allow_origins`를 특정 도메인으로 제한
2. **API 키**: 프로덕션에서는 API 키 인증 추가
3. **HTTPS**: 프로덕션에서는 SSL/TLS 인증서 적용 (Let's Encrypt)
4. **Rate Limiting**: 과도한 요청 방지를 위한 Rate Limit 설정

---

## 📞 문의 및 지원

- API 문서: `http://your-server-ip:8000/docs` (Swagger UI)
- Redoc: `http://your-server-ip:8000/redoc`

---

## 📝 버전 히스토리

- **v1.0.0** (2025-10-05): 초기 릴리스
  - 번호 추천 API
  - 통계 조회 API
  - 데이터 업데이트 API
