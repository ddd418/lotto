# 🎰 로또 번호 추천 FastAPI 서버

기존 Python CLI 로또 추천 프로그램을 Android 앱에서 사용할 수 있도록 REST API 서버로 변환한 프로젝트입니다.

## 📂 프로젝트 구조

```
lotto/
├── lott.py              # 기존 로또 로직 (데이터 수집, 추천 알고리즘)
├── api_server.py        # FastAPI REST API 서버 ⭐
├── requirements.txt     # Python 패키지 의존성
├── start_server.bat     # 서버 실행 스크립트 (Windows)
├── test_api.py          # API 테스트 스크립트
├── API_GUIDE.md         # 상세 API 문서 (Android 개발자용)
├── lotto_stats.json     # 로또 통계 데이터
└── lotto_draws.json     # 회차별 원본 데이터
```

---

## 🚀 빠른 시작

### 1. 패키지 설치

```bash
pip install -r requirements.txt
```

### 2. 서버 실행

**방법 1: 배치 스크립트 사용 (Windows)**

```bash
start_server.bat
```

**방법 2: 직접 실행**

```bash
python api_server.py
```

서버가 시작되면:

- 🌐 API 서버: http://localhost:8000
- 📖 API 문서: http://localhost:8000/docs
- 🏥 헬스체크: http://localhost:8000/api/health

### 3. API 테스트

```bash
python test_api.py
```

---

## 🎯 주요 API 엔드포인트

### 1️⃣ 로또 번호 추천 (메인 기능)

```http
POST /api/recommend
Content-Type: application/json

{
  "n_sets": 5
}
```

**응답 예시:**

```json
{
  "success": true,
  "last_draw": 1192,
  "sets": [
    { "numbers": [3, 12, 17, 27, 34, 45] },
    { "numbers": [7, 13, 18, 24, 33, 40] }
  ]
}
```

### 2️⃣ 통계 조회

```http
GET /api/stats
```

### 3️⃣ 헬스 체크

```http
GET /api/health
```

### 4️⃣ 데이터 업데이트

```http
POST /api/update
Content-Type: application/json

{
  "max_draw": null,
  "include_bonus": false
}
```

자세한 API 문서는 **[API_GUIDE.md](API_GUIDE.md)** 참조

---

## 📱 Android 앱 연동 예제

### Retrofit 설정 (Kotlin)

```kotlin
// 1. API 인터페이스 정의
interface LottoApiService {
    @POST("api/recommend")
    suspend fun recommendNumbers(
        @Body request: RecommendRequest
    ): Response<RecommendResponse>
}

// 2. Retrofit 클라이언트
object RetrofitClient {
    private const val BASE_URL = "http://your-server-ip:8000/"

    val apiService: LottoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LottoApiService::class.java)
    }
}

// 3. 사용 예제
lifecycleScope.launch {
    val response = RetrofitClient.apiService.recommendNumbers(
        RecommendRequest(n_sets = 5)
    )
    if (response.isSuccessful) {
        val numbers = response.body()?.sets
        // UI 업데이트
    }
}
```

전체 Android 예제 코드는 **[API_GUIDE.md](API_GUIDE.md#-android-앱-개발-가이드)** 참조

---

## 🔧 기술 스택

### 백엔드

- **FastAPI**: 고성능 Python 웹 프레임워크
- **Uvicorn**: ASGI 서버
- **Pydantic**: 데이터 검증

### 알고리즘

- 과거 출현 빈도 기반 가중치 샘플링
- 휴리스틱 필터링 (연속 숫자, 짝/홀 균형, 합계 범위)

---

## 📊 데이터 수집 방식

- **출처**: 동행복권 공식 API (`https://www.dhlottery.co.kr`)
- **수집 범위**: 1회차 ~ 최신 회차
- **자동 갱신**: `/api/update` 엔드포인트로 데이터 업데이트

---

## 🌐 서버 배포

### 로컬 테스트

```bash
python api_server.py
```

### 프로덕션 (Linux/Ubuntu)

```bash
# 1. 패키지 설치
pip install -r requirements.txt

# 2. Systemd 서비스 등록
sudo nano /etc/systemd/system/lotto-api.service

# 3. 서비스 시작
sudo systemctl enable lotto-api
sudo systemctl start lotto-api
```

### Docker (선택사항)

```bash
docker build -t lotto-api .
docker run -d -p 8000:8000 lotto-api
```

자세한 배포 가이드는 **[API_GUIDE.md](API_GUIDE.md#-서버-배포-가이드)** 참조

---

## 🔒 보안 권장사항

- [ ] CORS 설정: 특정 도메인만 허용
- [ ] API 키 인증 추가
- [ ] HTTPS (SSL/TLS) 적용
- [ ] Rate Limiting 설정

---

## 📝 라이선스

MIT License

---

## 🤝 기여

이슈 및 PR 환영합니다!

---

## 📧 문의

문제가 있으면 Issue를 등록해주세요.

---

**Made with ❤️ for Android Developers**
