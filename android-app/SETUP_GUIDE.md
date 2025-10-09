# 🎰 로또 번호 추천 Android 앱 설치 가이드

## 📋 준비사항

- Android Studio (최신 버전 권장)
- JDK 17 이상
- 최소 SDK 24 (Android 7.0)

## 🚀 프로젝트 생성 단계별 가이드

### 1단계: Android Studio에서 새 프로젝트 생성

1. Android Studio 실행
2. `File` → `New` → `New Project`
3. `Empty Activity` 선택
4. 프로젝트 설정:
   - **Name**: LottoApp
   - **Package name**: com.lotto.app
   - **Save location**: 원하는 위치
   - **Language**: Kotlin
   - **Minimum SDK**: API 24 (Android 7.0)
   - **Build configuration language**: Kotlin DSL (build.gradle.kts) 또는 Groovy (build.gradle)

### 2단계: 파일 복사

생성된 Android 프로젝트의 다음 위치에 파일들을 복사하세요:

#### Gradle 설정 파일

```
프로젝트_루트/build.gradle               ← build.gradle 복사
프로젝트_루트/app/build.gradle           ← app-build.gradle 복사 (이름을 build.gradle로 변경)
```

#### Kotlin 소스 파일 (패키지 구조대로)

```
app/src/main/java/com/lotto/app/
├── data/
│   ├── model/
│   │   └── Models.kt                    ← Models.kt 복사
│   ├── remote/
│   │   ├── LottoApiService.kt          ← LottoApiService.kt 복사
│   │   └── RetrofitClient.kt           ← RetrofitClient.kt 복사
│   └── repository/
│       └── LottoRepository.kt          ← LottoRepository.kt 복사
├── ui/
│   ├── components/
│   │   ├── LoadingDialog.kt            ← LoadingDialog.kt 복사
│   │   ├── LottoNumberBall.kt          ← LottoNumberBall.kt 복사
│   │   └── LottoSetCard.kt             ← LottoSetCard.kt 복사
│   ├── screens/
│   │   ├── MainScreen.kt               ← MainScreen.kt 복사
│   │   ├── RecommendScreen.kt          ← RecommendScreen.kt 복사
│   │   └── StatsScreen.kt              ← StatsScreen.kt 복사
│   └── theme/
│       ├── Color.kt                     ← Color.kt 복사
│       ├── Theme.kt                     ← Theme.kt 복사
│       └── Type.kt                      ← Type.kt 복사
├── viewmodel/
│   └── LottoViewModel.kt               ← LottoViewModel.kt 복사
└── MainActivity.kt                      ← MainActivity.kt 복사 (기존 파일 덮어쓰기)
```

#### 리소스 파일

```
app/src/main/res/values/strings.xml     ← strings.xml 복사 (기존 파일 덮어쓰기)
app/src/main/AndroidManifest.xml        ← AndroidManifest.xml 복사 (기존 파일 덮어쓰기)
```

### 3단계: 서버 주소 설정

`RetrofitClient.kt` 파일을 열고 BASE_URL 수정:

```kotlin
// 로컬 테스트 (에뮬레이터)
private const val BASE_URL = "http://10.0.2.2:8000/"

// 로컬 테스트 (실제 기기) - PC의 IP 주소로 변경
private const val BASE_URL = "http://192.168.x.x:8000/"

// 프로덕션 서버
private const val BASE_URL = "http://your-server-ip:8000/"
```

**PC IP 주소 확인 방법 (Windows):**

```cmd
ipconfig
```

→ `IPv4 주소` 찾기 (예: 192.168.0.100)

### 4단계: Gradle Sync

1. Android Studio에서 `File` → `Sync Project with Gradle Files` 클릭
2. 모든 의존성이 다운로드될 때까지 대기

### 5단계: 빌드 및 실행

1. **에뮬레이터 실행** 또는 **실제 기기 연결**
2. `Run` → `Run 'app'` (단축키: Shift + F10)
3. 앱이 설치되고 실행됩니다!

## 🔧 문제 해결

### Gradle Sync 실패

```
File → Invalidate Caches / Restart → Invalidate and Restart
```

### 패키지 임포트 오류

- 각 파일 상단의 `package` 선언이 올바른지 확인
- `Alt + Enter`로 자동 임포트

### 서버 연결 안 됨

1. Python API 서버가 실행 중인지 확인:

   ```bash
   python api_server.py
   ```

2. 방화벽 설정 확인 (Windows):

   ```
   제어판 → Windows Defender 방화벽 → 고급 설정
   → 인바운드 규칙 → 새 규칙 → 포트 → TCP 8000 허용
   ```

3. 에뮬레이터의 경우: `http://10.0.2.2:8000/` 사용
4. 실제 기기의 경우: PC와 같은 Wi-Fi 네트워크에 연결

### Cleartext Traffic 오류

`AndroidManifest.xml`에 다음이 포함되어 있는지 확인:

```xml
android:usesCleartextTraffic="true"
```

## 📱 APK 빌드

### Debug APK

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

→ `app/build/outputs/apk/debug/app-debug.apk` 생성

### Release APK (배포용)

```
Build → Generate Signed Bundle / APK → APK
```

## 🎯 테스트 체크리스트

- [ ] API 서버 실행 중
- [ ] 앱 실행 성공
- [ ] "서버 연결됨" 상태 확인
- [ ] 최신 회차 정보 표시
- [ ] "로또 번호 추천받기" 버튼 클릭
- [ ] 5개 세트 번호 표시 확인
- [ ] "번호 출현 통계 보기" 버튼 클릭
- [ ] 상위 10개 번호 표시 확인

## 📚 추가 리소스

- [Android 공식 문서](https://developer.android.com)
- [Jetpack Compose 가이드](https://developer.android.com/jetpack/compose)
- [Retrofit 문서](https://square.github.io/retrofit/)

## 💡 팁

1. **로그 확인**: Android Studio 하단의 `Logcat` 탭에서 실시간 로그 확인
2. **네트워크 디버깅**: `HttpLoggingInterceptor`가 활성화되어 있어 API 요청/응답 로그 확인 가능
3. **UI 미리보기**: Compose 함수에 `@Preview` 추가하여 UI 미리보기

---

**문제가 발생하면 Logcat을 확인하거나 이슈를 등록해주세요!** 🚀
