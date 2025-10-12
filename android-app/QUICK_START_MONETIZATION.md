# 🚀 수익화 빠른 시작 가이드

## 📝 5분 안에 시작하기

### 1단계: 현재 상태 확인 ✅

다음 파일들이 생성되었습니다:

- ✅ Google Play Billing 구독 시스템
- ✅ 무료 30일 체험 관리
- ✅ AdMob 광고 통합
- ✅ 온보딩 화면
- ✅ PRO 구독 화면

### 2단계: 즉시 해야 할 일 🔥

#### A. Google Play Console 설정 (필수)

1. **Google Play Console** 접속

   - https://play.google.com/console

2. **앱 만들기** 클릭

   ```
   앱 이름: 로또연구소
   기본 언어: 한국어
   앱 또는 게임: 앱
   무료 또는 유료: 무료 (구독 포함)
   ```

3. **수익 창출 > 구독 만들기**

   ```
   상품 ID: lotto_pro_monthly
   이름: 로또연구소 PRO
   설명: 광고 없이 무제한으로 사용하세요

   요금제 설정:
   - 결제 주기: 1개월
   - 가격: ₩1,900
   - 무료 체험: 30일
   ```

4. **구독 상품 활성화** ✅

#### B. AdMob 설정 (필수)

1. **AdMob 콘솔** 접속

   - https://apps.admob.com

2. **앱 추가**

   ```
   플랫폼: Android
   앱 이름: 로또연구소
   패키지 이름: com.lotto.app
   ```

3. **광고 단위 만들기**

   ```
   형식: 배너
   이름: 메인 배너
   ```

4. **발급받은 ID 복사** 📋
   - App ID: `ca-app-pub-XXXXX~XXXXX`
   - 광고 단위 ID: `ca-app-pub-XXXXX/XXXXX`

#### C. 코드에 ID 적용 (필수)

**AndroidManifest.xml**

```xml
<!-- 16번째 줄 - App ID 교체 -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXX~XXXXX" />
```

**BannerAd.kt**

```kotlin
// 13번째 줄 - 광고 단위 ID 교체
adUnitId: String = "ca-app-pub-XXXXX/XXXXX"
```

### 3단계: 테스트 앱 빌드 🔧

```bash
# 프로젝트 루트로 이동
cd android-app

# 릴리즈 AAB 빌드
./gradlew bundleRelease

# 생성된 파일 위치
app/build/outputs/bundle/release/app-release.aab
```

### 4단계: 내부 테스트 시작 🧪

1. **Google Play Console > 테스트 > 내부 테스트**
2. **새 버전 만들기**
3. **AAB 파일 업로드**
4. **테스터 추가** (본인 Gmail)
5. **검토 후 출시**

### 5단계: 테스트 시나리오 확인 ✔️

1. **무료 체험 시작**

   - 앱 설치
   - "30일 무료 체험 시작" 클릭
   - 모든 기능 사용 가능 확인

2. **광고 확인**

   - 메인 화면 하단 배너 광고 표시
   - PRO 사용자는 광고 없음

3. **구독 결제**
   - "PRO로 바로 시작" 클릭
   - Google Play 결제 화면
   - 테스트 결제 진행 (실제 과금 안됨)

---

## ⚠️ 중요 체크리스트

출시 전 반드시 확인:

- [ ] Google Play Console 앱 등록
- [ ] 구독 상품 생성 (`lotto_pro_monthly`)
- [ ] AdMob 앱 및 광고 단위 생성
- [ ] 실제 AdMob ID로 코드 수정
- [ ] 내부 테스트 완료
- [ ] 스크린샷 준비 (최소 2개)
- [ ] 개인정보 처리방침 URL
- [ ] 앱 설명 작성

---

## 🆘 자주 묻는 질문

**Q: 구독 테스트 시 실제로 결제되나요?**
A: 아니요. 라이선스 테스터로 등록된 계정은 실제 결제 없이 테스트 가능합니다.

**Q: 광고가 표시되지 않아요**
A:

1. AdMob App ID가 올바른지 확인
2. 테스트 광고 ID 사용 중인지 확인
3. 인터넷 연결 확인

**Q: 구독 상품 ID를 변경해도 되나요?**
A: 가능하지만, 다음 파일도 함께 수정해야 합니다:

- `SubscriptionManager.kt` (상품 ID)
- Google Play Console (실제 상품 ID)

**Q: 무료 체험 기간을 변경하고 싶어요**
A: `TrialManager.kt` 파일에서:

```kotlin
private const val TRIAL_PERIOD_DAYS = 30L  // 원하는 일수로 변경
```

---

## 📚 상세 문서

더 자세한 내용은 다음 문서를 참고하세요:

- **MONETIZATION_GUIDE.md**: 전체 수익화 가이드
- **PLAY*STORE*배포\_가이드.md**: 플레이 스토어 출시 가이드

---

## 🎯 현재 구현 상태

```
✅ Google Play Billing Library 통합
✅ 구독 관리 시스템
✅ 무료 30일 체험 시스템
✅ AdMob 광고 통합
✅ 온보딩 화면
✅ PRO 구독 화면
✅ 배너 광고 컴포넌트
✅ 체험 기간 표시 배너

⏳ Google Play Console 설정 필요
⏳ AdMob 실제 ID 적용 필요
⏳ 테스트 필요
```

---

## 💡 다음 단계

1. Google Play Console 계정 생성/로그인
2. 앱 등록 및 구독 상품 설정
3. AdMob 가입 및 광고 단위 생성
4. 발급받은 ID로 코드 수정
5. 테스트 빌드 및 업로드
6. 내부 테스트 진행

**준비 완료! 지금 시작하세요! 🚀**
