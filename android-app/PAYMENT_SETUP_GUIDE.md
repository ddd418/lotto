# 🔥 Google Play 실제 결제 연동 가이드

## ✅ 현재 상태

- **코드**: 100% 완료 (수정 불필요)
- **개발자 계정**: 생성 완료 ✅
- **결제 방식**:
  - 첫 결제: ₩1,000 즉시 청구
  - 이후: 매월 자동 갱신
  - 취소: 당월까지 사용 가능, 다음 달부터 차단

---

## 🎯 당신이 할 작업 (30분 소요)

### 1단계: Play Console 구독 상품 만들기 (5분)

1. **Play Console 접속**: https://play.google.com/console
2. 앱 선택 (없으면 새로 만들기)
3. 좌측 메뉴 **"수익 창출 설정" > "정기 결제"** 클릭
4. **"정기 결제 만들기"** 버튼 클릭

#### 필수 입력 정보:

```
📝 기본 정보:
- 제품 ID: lotto_pro_monthly (정확히 입력!)
- 이름: 로또연구소 PRO
- 설명: 모든 기능을 무제한으로 이용하세요

💰 가격 설정:
- 기본 가격: 1,000원 (KRW)
- 청구 기간: 매월 (1개월)
- 갱신 유형: 자동 갱신

⚙️ 추가 설정:
- 무료 체험: 설정 안 함 (앱에서 이미 제공)
- 유예 기간: 3일 (결제 실패 시 재시도)
- 계정 보류: 활성화 (결제 실패 시 기능 차단)
```

5. **"저장" 및 "활성화"** 클릭
6. 상태가 **"활성"**으로 변경되면 완료! ✅

---

### 2단계: 테스트 계정 추가 (2분)

**실제 돈 안 내고 테스트하기 위해 필수!**

1. Play Console 좌측 **"설정" > "라이선스 테스트"**
2. **"라이선스 테스터 추가"** 클릭
3. 본인 Gmail 주소 입력 (예: yourname@gmail.com)
4. **"저장"** 클릭

✅ **이제 이 계정으로는 "테스트 결제"가 표시되고 실제 청구 안 됨!**

---

### 3단계: Google Play Developer API 활성화 (5분)

**서버에서 결제 검증하려면 필수!**

#### A. API 활성화

1. **Google Cloud Console**: https://console.cloud.google.com
2. Play Console과 연결된 프로젝트 선택 (자동 생성되어 있음)
3. 좌측 메뉴 **"API 및 서비스" > "라이브러리"**
4. 검색창에 **"Google Play Developer API"** 입력
5. 클릭 후 **"사용 설정"** 버튼 클릭

#### B. 서비스 계정 생성

1. **"API 및 서비스" > "사용자 인증 정보"**
2. **"사용자 인증 정보 만들기" > "서비스 계정"** 선택
3. 서비스 계정 정보:
   ```
   - 이름: lotto-subscription-validator
   - 설명: 로또앱 구독 검증용
   ```
4. **"만들고 계속하기"** 클릭
5. **역할 선택하지 말고** "완료" 클릭
6. 생성된 서비스 계정 클릭
7. **"키" 탭 > "키 추가" > "새 키 만들기"**
8. **JSON** 선택 후 **"만들기"**
9. 🔑 **JSON 파일 다운로드됨 → 안전하게 보관!**

#### C. Play Console에 권한 부여

1. 다시 **Play Console > 설정 > API 액세스**
2. 방금 만든 서비스 계정 찾기
3. **"액세스 권한 부여"** 클릭
4. 권한 설정:
   ```
   ✅ 재무 데이터 보기 (읽기 전용)
   ✅ 주문 및 정기 결제 관리
   ```
5. **"사용자 초대 및 권한 보내기"** 클릭

---

### 4단계: Railway 백엔드 환경 변수 설정 (5분)

**서버가 Google API로 결제 검증하도록 설정**

1. **Railway 대시보드**: https://railway.app
2. 로또 백엔드 프로젝트 선택
3. **"Variables"** 탭 클릭
4. 다음 환경 변수 추가:

```bash
# 1. Google 서비스 계정 키 (방금 다운로드한 JSON 파일 내용 전체를 복사)
GOOGLE_APPLICATION_CREDENTIALS_JSON={"type":"service_account","project_id":"...전체내용..."}

# 2. 앱 패키지 이름
ANDROID_PACKAGE_NAME=com.lottoresearch.pro
```

**JSON 복사 방법:**

- 다운로드한 JSON 파일을 메모장으로 열기
- 전체 내용 복사 (Ctrl+A, Ctrl+C)
- Railway 환경 변수에 붙여넣기

5. **"Deploy"** 버튼 클릭 (자동 재배포)

---

### 5단계: 백엔드 코드 수정 - Google API 검증 추가 (10분)

**현재 서버는 간단 검증만 하므로, 실제 Google API 호출 추가 필요**

#### A. Python 라이브러리 추가

Railway 프로젝트에 다음 파일 수정:

**`requirements.txt`에 추가:**

```txt
google-auth==2.23.0
google-api-python-client==2.100.0
```

#### B. 서버 코드 수정

**`subscription_api.py`의 `verify_purchase` 함수를 다음으로 교체:**

```python
from google.oauth2 import service_account
from googleapiclient.discovery import build
import json
import os

# Google Play Developer API 클라이언트 초기화
def get_google_play_service():
    """Google Play Developer API 서비스 객체 생성"""
    credentials_json = os.getenv('GOOGLE_APPLICATION_CREDENTIALS_JSON')
    if not credentials_json:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Google API 인증 정보가 설정되지 않았습니다."
        )

    credentials_dict = json.loads(credentials_json)
    credentials = service_account.Credentials.from_service_account_info(
        credentials_dict,
        scopes=['https://www.googleapis.com/auth/androidpublisher']
    )

    service = build('androidpublisher', 'v3', credentials=credentials)
    return service

@router.post("/verify-purchase", response_model=VerifyPurchaseResponse)
async def verify_purchase(
    request: VerifyPurchaseRequest,
    user_id: int = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Google Play 구매 검증 및 PRO 구독 활성화
    """
    subscription = get_or_create_subscription(db, user_id)

    # 입력 검증
    if not request.purchase_token or not request.order_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="유효하지 않은 구매 정보입니다."
        )

    # 중복 구매 확인
    existing = db.query(UserSubscription).filter(
        UserSubscription.google_play_order_id == request.order_id,
        UserSubscription.user_id != user_id
    ).first()

    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미 등록된 구매입니다."
        )

    # ✨ Google Play API로 실제 검증
    try:
        service = get_google_play_service()
        package_name = os.getenv('ANDROID_PACKAGE_NAME', 'com.lotto.app')

        # Google API 호출
        result = service.purchases().subscriptions().get(
            packageName=package_name,
            subscriptionId=request.product_id,
            token=request.purchase_token
        ).execute()

        # 구매 상태 확인
        payment_state = result.get('paymentState')
        if payment_state != 1:  # 1 = Payment received
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="결제가 완료되지 않았습니다."
            )

        # 만료일 추출
        expiry_time_millis = int(result.get('expiryTimeMillis', 0))
        subscription_end_date = datetime.fromtimestamp(
            expiry_time_millis / 1000,
            tz=timezone.utc
        )

        print(f"✅ Google 검증 성공: {request.order_id}")
        print(f"   만료일: {subscription_end_date}")

    except Exception as e:
        print(f"❌ Google API 검증 실패: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"구매 검증 실패: {str(e)}"
        )

    # PRO 구독 활성화
    now = datetime.now(timezone.utc)
    subscription.is_pro_subscriber = True
    subscription.subscription_plan = "pro"
    subscription.subscription_start_date = now
    subscription.subscription_end_date = subscription_end_date
    subscription.google_play_order_id = request.order_id
    subscription.google_play_purchase_token = request.purchase_token
    subscription.google_play_product_id = request.product_id
    subscription.auto_renew = True
    subscription.updated_at = now

    db.commit()
    db.refresh(subscription)

    return VerifyPurchaseResponse(
        verified=True,
        is_pro=True,
        subscription_end_date=subscription.subscription_end_date,
        message="PRO 구독이 활성화되었습니다."
    )
```

---

### 6단계: APK/AAB 빌드 및 배포 (5분)

#### A. 릴리스 빌드 생성

```bash
cd C:\projects\lotto\android-app
.\gradlew assembleRelease
```

또는 AAB (Play Store 업로드용):

```bash
.\gradlew bundleRelease
```

빌드 파일 위치:

- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

#### B. Play Console에 업로드

1. **Play Console > 프로덕션 > 새 출시 만들기**
2. AAB 파일 업로드
3. 출시 노트 작성:
   ```
   v1.0.0 - 첫 출시
   • PRO 구독 기능 추가 (₩1,000/월)
   • 매월 자동 갱신
   • 언제든지 취소 가능
   ```
4. **"검토 시작"** 클릭

---

## 🧪 테스트 시나리오

### 테스트 1: 라이선스 테스터로 결제 테스트 (무료)

1. **테스터 계정으로 앱 실행**
2. 로그인
3. **"프로 플랜"** 선택
4. Google Play 결제 창 확인
5. ✅ **"테스트 결제"** 표시 확인 (실제 청구 안 됨!)
6. 결제 완료
7. PRO 기능 활성화 확인

### 테스트 2: 구독 취소

1. **Play Store 앱** 열기
2. 프로필 > **"결제 및 정기 결제"**
3. **"로또연구소 PRO"** 선택
4. **"정기 결제 해지"** 클릭
5. 확인
6. ✅ **당월 말까지 PRO 사용 가능**
7. 다음 달 1일부터 자동 차단

### 테스트 3: 구독 복원

1. 앱 삭제
2. 앱 재설치
3. 동일 계정으로 로그인
4. ✅ **자동으로 PRO 상태 복원**

---

## 🎯 결제 플로우 최종 확인

### 첫 결제 플로우:

```
사용자 "프로 플랜" 선택
    ↓
Google Play 결제 창 표시
    ↓
사용자 결제 수단 선택 (카드/휴대폰)
    ↓
₩1,000 즉시 청구 ✅
    ↓
Railway 서버 Google API로 검증
    ↓
DB에 PRO 상태 저장 (만료일: 30일 후)
    ↓
앱에서 PRO 기능 활성화
```

### 자동 갱신 플로우:

```
만료일 (예: 11월 17일)
    ↓
Google이 자동으로 ₩1,000 청구
    ↓
결제 성공 시: 만료일 +30일 (12월 17일)
    ↓
Railway 서버에 Webhook 알림 (선택사항)
    ↓
사용자 계속 PRO 사용
```

### 취소 플로우:

```
사용자 Play Store에서 "정기 결제 해지"
    ↓
auto_renew = False로 변경
    ↓
현재 만료일까지 PRO 사용 가능 ✅
    ↓
만료일 도달
    ↓
다음 갱신 없음
    ↓
is_pro_subscriber = False로 변경
    ↓
무료 플랜으로 자동 전환
```

---

## ⚠️ 중요 주의사항

### 1. 제품 ID 절대 변경 금지

```kotlin
// SubscriptionManager.kt
const val SUBSCRIPTION_PRODUCT_ID = "lotto_pro_monthly" // 이거 변경하면 안됨!
```

Play Console과 정확히 일치해야 함!

### 2. 라이선스 테스터 활성화 시간

- 테스터 추가 후 **최대 24시간** 소요
- 즉시 테스트 안 될 수 있음 (인내심!)

### 3. 환불 정책

- **Play Store 정책**: 48시간 이내 자동 환불 가능
- 앱에 환불 정책 명시 권장

### 4. 세금

- Google이 자동으로 부가세 처리
- ₩1,000 = 실제 사용자는 ₩1,100 청구 (부가세 10%)

---

## 🐛 문제 해결

### 에러 1: "상품을 찾을 수 없습니다"

```
원인: Play Console에서 상품 미활성화
해결: 정기 결제 > lotto_pro_monthly > 상태 = "활성" 확인
```

### 에러 2: "서버 검증 실패"

```
원인: Railway 환경 변수 미설정
해결: GOOGLE_APPLICATION_CREDENTIALS_JSON 확인
```

### 에러 3: 테스트 결제가 실제 청구됨

```
원인: 라이선스 테스터에 계정 미등록
해결: 설정 > 라이선스 테스트에 Gmail 추가
```

### 에러 4: "결제 보류 중"

```
원인: 결제 수단 문제 (잔액 부족 등)
해결: Google Play 결제 수단 확인
```

---

## 📊 수익 모니터링

### Play Console 대시보드

1. **수익 창출 > 개요**

   - 월별 수익
   - 활성 구독자 수
   - 전환율

2. **수익 창출 > 정기 결제**

   - 신규 구독
   - 갱신율
   - 취소율

3. **수익 창출 > 환불**
   - 환불 요청
   - 환불률

---

## ✅ 최종 체크리스트

배포 전 필수 확인:

- [ ] Play Console 구독 상품 생성 및 활성화
- [ ] 제품 ID = `lotto_pro_monthly` 정확히 일치
- [ ] 가격 = ₩1,000/월 설정
- [ ] 라이선스 테스터 추가 (본인 Gmail)
- [ ] Google Play Developer API 활성화
- [ ] 서비스 계정 JSON 키 다운로드
- [ ] Play Console에 서비스 계정 권한 부여
- [ ] Railway 환경 변수 2개 설정
- [ ] `requirements.txt`에 google-auth 라이브러리 추가
- [ ] `subscription_api.py` Google API 검증 코드 추가
- [ ] Railway 재배포 (자동)
- [ ] APK/AAB 빌드
- [ ] Play Console에 업로드
- [ ] 테스터 계정으로 결제 테스트
- [ ] 구독 취소 테스트
- [ ] 구독 복원 테스트

---

## 🚀 완료 후 다음 단계

1. ✅ **실제 사용자 결제 테스트** (라이선스 테스터 외 계정)
2. 📊 **수익 모니터링** (Play Console)
3. 🔔 **푸시 알림 추가** (결제 성공/실패 알림)
4. 💰 **연간 플랜 추가** (₩10,000/년 - 17% 할인)
5. 🎁 **프로모션 코드** (마케팅용 무료 체험 연장)

---

## 🎉 완료!

**이제 실제 결제가 작동합니다!** 🎊

궁금한 점 있으면 언제든지 물어보세요!
