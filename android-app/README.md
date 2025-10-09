# 🎰 로또 번호 추천 Android 앱

Jetpack Compose와 Material Design 3를 사용한 모던한 로또 번호 추천 앱

## 📱 앱 기능

- 🎲 AI 기반 로또 번호 자동 추천 (1~5세트)
- 📊 번호별 출현 빈도 통계 조회
- 🎨 Material Design 3 UI
- 🔄 최신 회차 정보 자동 표시

## 🏗️ 프로젝트 구조

```
app/
├── src/
│   └── main/
│       ├── java/com/lotto/app/
│       │   ├── data/
│       │   │   ├── model/          # 데이터 모델
│       │   │   │   ├── LottoSet.kt
│       │   │   │   ├── RecommendRequest.kt
│       │   │   │   ├── RecommendResponse.kt
│       │   │   │   └── StatsResponse.kt
│       │   │   ├── remote/         # API 통신
│       │   │   │   ├── LottoApiService.kt
│       │   │   │   └── RetrofitClient.kt
│       │   │   └── repository/     # 데이터 저장소
│       │   │       └── LottoRepository.kt
│       │   ├── ui/
│       │   │   ├── components/     # 재사용 UI 컴포넌트
│       │   │   │   ├── LottoNumberBall.kt
│       │   │   │   ├── LottoSetCard.kt
│       │   │   │   └── LoadingDialog.kt
│       │   │   ├── screens/        # 화면
│       │   │   │   ├── MainScreen.kt
│       │   │   │   ├── RecommendScreen.kt
│       │   │   │   └── StatsScreen.kt
│       │   │   └── theme/          # 테마 설정
│       │   │       ├── Color.kt
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   ├── viewmodel/          # ViewModel
│       │   │   └── LottoViewModel.kt
│       │   └── MainActivity.kt
│       ├── res/
│       │   ├── values/
│       │   │   ├── strings.xml
│       │   │   └── themes.xml
│       │   └── drawable/
│       └── AndroidManifest.xml
├── build.gradle (Module)
└── build.gradle (Project)
```

## 🚀 시작하기

### 1. 프로젝트 생성

Android Studio에서:

1. File → New → New Project
2. Empty Activity 선택
3. 프로젝트 이름: **LottoApp**
4. Package name: **com.lotto.app**
5. Language: **Kotlin**
6. Minimum SDK: **API 24 (Android 7.0)**

### 2. 의존성 추가

아래 코드들을 각 파일에 복사하세요.

### 3. API 서버 주소 설정

`RetrofitClient.kt` 파일에서 BASE_URL을 서버 주소로 변경:

```kotlin
private const val BASE_URL = "http://your-server-ip:8000/"
```

로컬 테스트 시:

- 에뮬레이터: `http://10.0.2.2:8000/`
- 실제 기기: `http://your-pc-ip:8000/`

## 📦 필수 의존성

모든 필수 의존성은 아래 `build.gradle` 파일들을 참고하세요.

## 🎨 UI 미리보기

### 메인 화면

- 로또 번호 추천 버튼
- 통계 보기 버튼
- 최신 회차 정보 표시

### 추천 화면

- 추천 번호 세트 표시 (로또 공 스타일)
- 재추천 버튼
- 공유 기능

### 통계 화면

- 상위 10개 번호 차트
- 각 번호별 출현 빈도

## 🔧 빌드 및 실행

### 1. Gradle Sync

```
File → Sync Project with Gradle Files
```

### 2. 실행

```
Run → Run 'app' (Shift + F10)
```

### 3. APK 빌드

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

## 🌐 네트워크 권한

AndroidManifest.xml에 이미 포함됨:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 📱 테스트 방법

### 1. 로컬 API 서버 실행

```bash
cd c:\projects\lotto
python api_server.py
```

### 2. 네트워크 연결 확인

- 에뮬레이터 사용 시: `http://10.0.2.2:8000/docs` 접속 확인
- 실제 기기 사용 시: PC와 같은 Wi-Fi 네트워크 연결

### 3. 앱 실행 및 테스트

- "번호 추천받기" 버튼 클릭
- 추천된 로또 번호 확인
- 통계 화면에서 출현 빈도 확인

## 🎯 주요 기능 설명

### MVVM 아키텍처

- **Model**: 데이터 모델 (API 응답 구조)
- **View**: Jetpack Compose UI
- **ViewModel**: 비즈니스 로직 및 상태 관리

### Retrofit + Coroutines

- 비동기 네트워크 통신
- suspend 함수로 깔끔한 코드

### Material Design 3

- 최신 디자인 시스템
- 다이나믹 컬러 지원

## 🐛 문제 해결

### API 연결 안 됨

1. 서버가 실행 중인지 확인
2. BASE_URL이 올바른지 확인
3. 방화벽 설정 확인
4. Cleartext Traffic 허용 확인 (AndroidManifest.xml)

### 빌드 에러

1. Gradle Sync 실행
2. Android Studio 재시작
3. Invalidate Caches / Restart

## 📄 라이선스

MIT License

## 🤝 기여

이슈 및 PR 환영합니다!

---

**Made with ❤️ using Jetpack Compose**
