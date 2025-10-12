# 🚀 로또연구소 앱 수익화 구현 가이드

## 📋 목차

1. [수익화 구조 개요](#수익화-구조-개요)
2. [구현된 기능](#구현된-기능)
3. [Google Play Console 설정](#google-play-console-설정)
4. [AdMob 설정](#admob-설정)
5. [테스트 방법](#테스트-방법)
6. [출시 전 체크리스트](#출시-전-체크리스트)

---

## 💰 수익화 구조 개요

### 핵심 전략

- **무료 체험**: 30일 무료 체험 제공
- **유료 전환**: 월 1,900원 구독
- **광고**: 무료 사용자에게 배너 광고 표시 (살짝만)

### 사용자 흐름

```
1단계: 첫 실행
   ↓
[온보딩 화면]
- 30일 무료 체험 시작
- PRO로 바로 시작
- 나중에 하기
   ↓
2단계: 무료 체험 중 (30일)
   ↓
[모든 기능 사용 가능]
+ 하단 배너 광고 1개
+ 상단에 "남은 기간: X일" 표시
   ↓
3단계: 체험 종료 직전 (27~30일)
   ↓
[PRO 전환 안내]
"곧 종료됩니다. 월 1,900원으로 계속 이용하세요"
   ↓
4단계: 체험 종료 후
   ↓
[기능 제한 모드]
- 하루 1회 추천만 가능
- "광고 보고 1회 더 사용" 옵션
- PRO 구독 유도 화면
```

---

## ✨ 구현된 기능

### 1. 구독 관리 시스템

#### 파일 위치

- `app/src/main/java/com/lotto/app/billing/SubscriptionManager.kt`
- `app/src/main/java/com/lotto/app/data/local/TrialManager.kt`
- `app/src/main/java/com/lotto/app/viewmodel/SubscriptionViewModel.kt`

#### 주요 기능

```kotlin
// 구독 상태 확인
val isProUser: StateFlow<Boolean>

// 무료 체험 정보
val trialInfo: StateFlow<TrialInfo>

// 접근 권한 (PRO 또는 체험 중)
val hasAccess: StateFlow<Boolean>

// 구독 시작
fun startSubscription(activity: Activity)

// 무료 체험 시작
fun startTrial()
```

### 2. UI 화면

#### OnboardingScreen.kt

- 첫 실행 시 표시되는 온보딩 화면
- 무료 체험 / PRO 구독 선택

#### SubscriptionScreen.kt

- PRO 구독 안내 및 결제 화면
- 기능 상세 설명
- 가격 정보 (월 1,900원)

#### 배너 컴포넌트

- `BannerAd.kt`: AdMob 배너 광고
- `SubscriptionComponents.kt`: 체험 기간 배너, PRO 배지

### 3. 광고 시스템

#### AdMob 통합

```kotlin
// 무료 사용자에게만 광고 표시
ConditionalBannerAd(
    showAd = !isProUser,
    modifier = Modifier.fillMaxWidth()
)
```

---

## 🎮 Google Play Console 설정

### 1. 인앱 상품 생성

1. **Google Play Console** 접속
2. **수익 창출 > 인앱 상품 및 구독** 메뉴
3. **구독 만들기** 클릭

### 2. 구독 상품 설정

```
상품 ID: lotto_pro_monthly
이름: 로또연구소 PRO
설명: 광고 없이 무제한으로 번호 추천을 받으세요

기본 요금제:
  - 결제 주기: 1개월
  - 가격: ₩1,900

무료 체험:
  - 체험 기간: 30일
  - 적격성: 신규 구독자만
```

### 3. 중요 설정

#### 구독 옵션

- ✅ 자동 갱신 활성화
- ✅ 무료 체험 제공
- ✅ 유예 기간: 3일
- ✅ 결제 보류 기간: 7일

#### 취소 정책

```
"구독을 언제든지 취소할 수 있습니다.
취소 시 현재 결제 주기가 끝날 때까지 PRO 기능을 사용할 수 있습니다."
```

---

## 📱 AdMob 설정

### 1. AdMob 계정 생성

1. **AdMob 콘솔** (https://apps.admob.com) 접속
2. **앱 추가** 클릭
3. **Android** 선택

### 2. 앱 정보 입력

```
앱 이름: 로또연구소
패키지 이름: com.lotto.app
앱 스토어 URL: (출시 후 입력)
```

### 3. 광고 단위 생성

#### 배너 광고 단위

```
광고 형식: 배너
이름: 메인 배너
크기: 320x50 (표준 배너)
```

### 4. 광고 ID 적용

**테스트 단계** (현재 코드에 적용됨)

```kotlin
// 테스트 광고 ID
adUnitId = "ca-app-pub-3940256099942544/6300978111"
```

**실제 출시 시** (AdMob에서 발급받은 ID로 교체)

```kotlin
// BannerAd.kt 파일 수정
adUnitId = "ca-app-pub-XXXXXXXXXXXXX/YYYYYYYYYY"
```

**AndroidManifest.xml 수정**

```xml
<!-- 테스트 ID를 실제 AdMob App ID로 교체 -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXX~YYYYYYYYYY" />
```

### 5. 광고 배치 전략

```
📍 광고 위치:
- 메인 화면: 하단 배너 1개
- 추천 결과 화면: 하단 배너 1개
- 통계 화면: 하단 배너 1개

⚠️ 광고 제한:
- PRO 사용자: 광고 없음
- 무료 체험 중: 배너 광고만 표시
- 체험 종료 후: 배너 + 전면 광고 옵션
```

---

## 🧪 테스트 방법

### 1. 로컬 테스트

#### 무료 체험 테스트

```kotlin
// TrialManager.kt 에서 기간 변경
private const val TRIAL_PERIOD_DAYS = 1L  // 1일로 변경하여 빠른 테스트
```

#### 구독 테스트

1. Google Play Console에서 **라이선스 테스터** 추가
2. 테스트 계정 Gmail 등록
3. 실제 결제 없이 구독 플로우 테스트 가능

### 2. 내부 테스트 트랙

```bash
# 내부 테스트 AAB 빌드
./gradlew bundleRelease

# 생성된 파일 위치
android-app/app/build/outputs/bundle/release/app-release.aab
```

**Google Play Console 업로드**

1. **테스트 > 내부 테스트** 메뉴
2. **새 버전 만들기**
3. AAB 파일 업로드
4. 테스터 이메일 추가

### 3. 구독 테스트 시나리오

#### ✅ 시나리오 1: 무료 체험 시작

1. 앱 첫 실행
2. "30일 무료 체험 시작" 버튼 클릭
3. 모든 기능 사용 가능 확인
4. 배너 광고 표시 확인

#### ✅ 시나리오 2: PRO 구독

1. "PRO로 바로 시작" 버튼 클릭
2. Google Play 결제 화면 표시
3. 테스트 결제 진행
4. 광고 제거 확인

#### ✅ 시나리오 3: 체험 종료

1. 무료 체험 기간 종료 대기
2. 경고 배너 표시 확인 (27~30일)
3. 종료 후 기능 제한 확인

---

## ✅ 출시 전 체크리스트

### 코드 수정 필수 항목

- [ ] **AdMob App ID** 교체 (AndroidManifest.xml)
- [ ] **배너 광고 ID** 교체 (BannerAd.kt)
- [ ] **구독 상품 ID** 확인 (SubscriptionManager.kt)
  ```kotlin
  const val SUBSCRIPTION_PRODUCT_ID = "lotto_pro_monthly"
  ```
- [ ] **체험 기간** 확인 (TrialManager.kt)
  ```kotlin
  private const val TRIAL_PERIOD_DAYS = 30L
  ```

### Google Play Console

- [ ] 앱 등록 및 스토어 등록정보 작성
- [ ] 인앱 구독 상품 생성 (lotto_pro_monthly)
- [ ] 가격 설정: ₩1,900/월
- [ ] 무료 체험 설정: 30일
- [ ] 스크린샷 업로드 (최소 2개)
- [ ] 앱 아이콘 등록
- [ ] 개인정보 처리방침 URL 등록

### AdMob 설정

- [ ] AdMob 앱 등록
- [ ] 배너 광고 단위 생성
- [ ] 앱 ID 및 광고 ID 발급
- [ ] 코드에 실제 ID 적용
- [ ] 결제 정보 등록

### 법적 요구사항

- [ ] 개인정보 처리방침 작성
- [ ] 이용약관 작성
- [ ] 구독 취소 및 환불 정책 명시
- [ ] 광고 표시 동의 (필요시)

### 앱 품질

- [ ] 모든 기능 정상 작동 확인
- [ ] 결제 플로우 테스트
- [ ] 광고 표시 테스트
- [ ] 다양한 기기에서 테스트
- [ ] 크래시 없음 확인

---

## 📊 수익 예상

### 예상 수익 모델

```
월 사용자 1,000명 기준:

구독 전환율 10%:
  - 구독자: 100명
  - 구독 수익: 100명 × ₩1,900 = ₩190,000/월

광고 수익 (무료 사용자 900명):
  - 일 광고 노출: 900명 × 5회 = 4,500회
  - 월 광고 노출: 4,500 × 30 = 135,000회
  - eCPM ₩500 기준: ₩67,500/월

총 예상 수익: ₩257,500/월
```

### 최적화 전략

1. **구독 전환율 향상**

   - 무료 체험 중 적극적 기능 사용 유도
   - 체험 종료 3일 전부터 안내 배너
   - PRO 혜택 강조

2. **광고 수익 최적화**

   - 적절한 광고 위치 선정
   - 사용자 경험 해치지 않는 선에서 배치
   - eCPM 높은 광고 네트워크 활용

3. **리텐션 향상**
   - 푸시 알림 (당첨 번호 발표일)
   - 주간 통계 리포트
   - 개인화된 번호 추천

---

## 🔧 주요 파일 위치

```
android-app/
├── app/
│   ├── build.gradle                    # 의존성 설정
│   ├── src/main/
│   │   ├── AndroidManifest.xml         # AdMob App ID
│   │   └── java/com/lotto/app/
│   │       ├── billing/
│   │       │   └── SubscriptionManager.kt      # 구독 관리
│   │       ├── data/local/
│   │       │   └── TrialManager.kt            # 무료 체험 관리
│   │       ├── viewmodel/
│   │       │   └── SubscriptionViewModel.kt   # 구독 ViewModel
│   │       ├── ui/
│   │       │   ├── screens/
│   │       │   │   ├── OnboardingScreen.kt    # 온보딩
│   │       │   │   └── SubscriptionScreen.kt  # 구독 화면
│   │       │   └── components/
│   │       │       ├── BannerAd.kt           # 광고 컴포넌트
│   │       │       └── SubscriptionComponents.kt
│   │       └── di/
│   │           └── ServiceLocator.kt          # 의존성 주입
```

---

## 📞 문의 및 지원

구현 중 문제가 발생하면:

1. **Google Play 구독 문서**: https://developer.android.com/google/play/billing
2. **AdMob 시작 가이드**: https://developers.google.com/admob/android/quick-start
3. **Billing Library v6**: https://developer.android.com/google/play/billing/migrate-gpblv6

---

## 🎯 다음 단계

1. ✅ 코드 구현 완료
2. ⏳ Google Play Console 설정
3. ⏳ AdMob 계정 생성 및 설정
4. ⏳ 내부 테스트
5. ⏳ 비공개 테스트
6. ⏳ 정식 출시

**성공적인 런칭을 기원합니다! 🚀**
