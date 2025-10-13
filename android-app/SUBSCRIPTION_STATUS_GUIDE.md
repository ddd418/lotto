# 📱 구독 상태 확인 기능 추가 완료!

## ✅ 생성된 파일

### 1. SubscriptionStatusScreen.kt

**위치:** `app/src/main/java/com/lotto/app/ui/screens/SubscriptionStatusScreen.kt`

**기능:**

- 현재 구독 상태 표시 (PRO / 무료 체험 / 무료 플랜)
- 기능 접근 권한 표시
- 무료 체험 정보 (남은 기간, 시작/종료일)
- PRO 구독 정보 (다음 결제일, 자동 갱신 여부)
- 구독 취소 버튼

---

## 🔧 수정된 파일

### 1. SubscriptionViewModel.kt

**추가된 기능:**

```kotlin
// 서버 구독 상태
val subscriptionStatus: StateFlow<SubscriptionStatus>

// 서버에서 구독 상태 조회
fun refreshStatus()

// 구독 취소
fun cancelSubscription()
```

### 2. MainActivity.kt

**추가된 라우트:**

```kotlin
object Onboarding : Screen("onboarding")
object Subscription : Screen("subscription")
object SubscriptionStatus : Screen("subscription_status")
```

---

## 🎯 사용 방법

### MainActivity.kt의 NavHost에 추가:

```kotlin
// NavHost 내부에 추가
composable(Screen.SubscriptionStatus.route) {
    val subscriptionViewModel = remember {
        ServiceLocator.getSubscriptionViewModel(context)
    }

    SubscriptionStatusScreen(
        viewModel = subscriptionViewModel,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToSubscription = {
            navController.navigate(Screen.Subscription.route)
        }
    )
}

composable(Screen.Subscription.route) {
    val subscriptionViewModel = remember {
        ServiceLocator.getSubscriptionViewModel(context)
    }

    SubscriptionScreen(
        viewModel = subscriptionViewModel,
        onNavigateBack = { navController.popBackStack() },
        onStartTrial = {
            subscriptionViewModel.startTrial()
            navController.popBackStack()
        },
        onSubscribe = { activity ->
            subscriptionViewModel.startSubscription(activity)
        }
    )
}

composable(Screen.Onboarding.route) {
    val subscriptionViewModel = remember {
        ServiceLocator.getSubscriptionViewModel(context)
    }

    OnboardingScreen(
        viewModel = subscriptionViewModel,
        onStartTrial = {
            subscriptionViewModel.startTrial()
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        },
        onSubscribe = {
            navController.navigate(Screen.Subscription.route)
        },
        onSkip = {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        }
    )
}
```

---

## 🧭 네비게이션 플로우

### 1. 메인 화면에서 구독 상태 확인

```kotlin
// MainScreen.kt의 AppBar 또는 Settings에 추가
IconButton(onClick = {
    navController.navigate(Screen.SubscriptionStatus.route)
}) {
    Icon(Icons.Default.Star, "구독 관리")
}
```

### 2. 설정 화면에 구독 관리 메뉴 추가

```kotlin
// Settings 화면에 추가
SettingsItem(
    icon = "💎",
    title = "구독 관리",
    description = "PRO 구독 및 체험 정보 확인",
    onClick = {
        navController.navigate(Screen.SubscriptionStatus.route)
    }
)
```

---

## 📊 구독 상태 화면 구성

### 1. 상태 카드

- ✨ PRO 구독 중
- 🎁 무료 체험 중
- 📱 무료 플랜

### 2. 접근 권한 카드

- ✅ 모든 프리미엄 기능 사용 가능
- ❌ 프리미엄 기능 제한

### 3. 체험 정보 카드 (체험 중인 경우)

- 상태: 체험 중
- 남은 기간: N일
- 종료일: YYYY년 MM월 DD일
- ⚠️ 만료 임박 경고 (3일 이하)

### 4. 구독 정보 카드 (PRO 구독자)

- 플랜: PRO (₩1,900/월)
- 다음 결제일: YYYY년 MM월 DD일
- 자동 갱신: 활성화/비활성화
- 🔴 구독 취소 버튼

---

## 🔔 구독 취소 플로우

1. "구독 취소" 버튼 클릭
2. 확인 다이얼로그 표시
   - "정말로 구독을 취소하시겠습니까?"
   - "현재 구독 기간까지는 PRO 기능을 계속 사용하실 수 있습니다."
3. "취소하기" 확인
4. 서버 API 호출: `POST /api/subscription/cancel`
5. 자동 갱신 비활성화
6. 상태 새로고침

---

## 🎨 UI 특징

### 카드 디자인

- 둥근 모서리 (12.dp)
- 노션 스타일 색상
- PRO/체험 중일 때 초록색 배경

### 이모지 사용

- ✨ PRO 구독
- 🎁 무료 체험
- 📱 무료 플랜
- ✅ 접근 가능
- ❌ 접근 불가
- ⚠️ 경고

### 날짜 포맷

- "YYYY년 MM월 DD일" (한국어)
- ISO 8601 형식 자동 파싱

---

## 🧪 테스트 시나리오

### 1. 무료 플랜 사용자

```
상태: 📱 무료 플랜
접근 권한: ❌ 프리미엄 기능 제한
버튼: "PRO 구독하기"
```

### 2. 체험 중 사용자 (7일 남음)

```
상태: 🎁 무료 체험 중 (남은 기간: 7일)
접근 권한: ✅ 모든 기능 사용 가능
체험 정보: 시작일, 종료일 표시
```

### 3. 체험 만료 임박 (2일 남음)

```
상태: 🎁 무료 체험 중 (남은 기간: 2일)
경고: ⚠️ 체험 기간이 곧 종료됩니다
버튼: "PRO 구독하기" 강조
```

### 4. PRO 구독자 (자동 갱신)

```
상태: ✨ PRO 구독 중
플랜: PRO (₩1,900/월)
다음 결제일: 2025년 11월 13일
자동 갱신: 활성화
버튼: 🔴 "구독 취소"
```

### 5. PRO 구독자 (취소됨)

```
상태: ✨ PRO 구독 중
만료일: 2025년 11월 13일
자동 갱신: 비활성화
정보: 만료일까지 사용 가능
```

---

## 🚀 다음 단계

### 1. MainActivity.kt 수정

- NavHost에 3개 화면 추가 (Onboarding, Subscription, SubscriptionStatus)

### 2. MainScreen 또는 Settings에 버튼 추가

```kotlin
"구독 관리" 메뉴 → SubscriptionStatusScreen
```

### 3. 빌드 및 테스트

```bash
.\gradlew assembleDebug
.\gradlew installDebug
```

### 4. 서버 연동 테스트

- 무료 체험 시작
- PRO 구독 결제
- 구독 상태 조회
- 구독 취소

---

## 📝 API 연동

### 사용하는 엔드포인트:

```
GET  /api/subscription/status       # 구독 상태 조회
POST /api/subscription/start-trial  # 체험 시작
POST /api/subscription/cancel       # 구독 취소
```

### 응답 데이터:

```json
{
  "is_pro": true,
  "trial_active": false,
  "trial_days_remaining": 0,
  "subscription_plan": "pro",
  "has_access": true,
  "trial_start_date": "2025-09-13T00:00:00",
  "trial_end_date": "2025-10-13T00:00:00",
  "subscription_end_date": "2025-11-13T00:00:00",
  "auto_renew": true
}
```

---

## ✅ 완료!

**구독 상태 확인 화면이 준비되었습니다!** 🎉

이제 MainActivity.kt에 NavHost 라우트만 추가하면 됩니다!
