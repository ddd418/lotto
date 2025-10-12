# 🔧 백엔드 구독 시스템 구현 완료

## ✅ 구현된 기능

### 1. **데이터베이스 테이블**

- `user_subscriptions` 테이블 추가
- 사용자별 구독 정보 관리
- Google Play 구매 정보 저장

### 2. **API 엔드포인트**

#### 사용자 API

```
POST /api/subscription/start-trial        # 무료 체험 시작
GET  /api/subscription/status             # 구독 상태 조회
POST /api/subscription/verify-purchase    # Google Play 구매 검증
POST /api/subscription/cancel             # 구독 취소
```

#### 관리자 API

```
GET /api/subscription/admin/expiring-trials  # 만료 임박 사용자
GET /api/subscription/admin/stats            # 구독 통계
```

### 3. **안드로이드 앱 연동**

- 서버와 로컬 데이터 이중 관리
- 네트워크 오류 시 로컬 fallback
- 구매 시 서버 자동 검증

---

## 🚀 배포 방법

### Step 1: 데이터베이스 마이그레이션

```bash
cd c:\projects\lotto
python add_subscription_table.py
```

**예상 출력:**

```
🔄 구독 관리 테이블 마이그레이션 시작...
📋 기존 테이블: ['users', 'winning_numbers', 'saved_numbers', ...]
✅ user_subscriptions 테이블 생성 중...
✅ user_subscriptions 테이블 생성 완료!

📊 user_subscriptions 테이블 컬럼:
  - id: INTEGER
  - user_id: INTEGER
  - trial_start_date: DATETIME
  - trial_end_date: DATETIME
  - is_trial_used: BOOLEAN
  - subscription_plan: VARCHAR(20)
  - is_pro_subscriber: BOOLEAN
  ...

✅ 마이그레이션 완료!
🎉 구독 관리 시스템이 준비되었습니다!
```

### Step 2: 서버 재시작

```bash
python api_server.py
```

**확인:**

- http://localhost:8000/docs
- `/api/subscription/` 엔드포인트 확인

### Step 3: 안드로이드 앱 빌드

```bash
cd android-app
./gradlew assembleDebug
```

---

## 🧪 테스트 시나리오

### 1. 무료 체험 시작 테스트

**API 테스트 (cURL):**

```bash
# 1. 로그인 (토큰 받기)
curl -X POST "http://localhost:8000/auth/kakao/login" \
  -H "Content-Type: application/json" \
  -d '{"authorization_code": "YOUR_CODE"}'

# 2. 체험 시작
curl -X POST "http://localhost:8000/api/subscription/start-trial" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. 상태 확인
curl -X GET "http://localhost:8000/api/subscription/status" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**예상 응답:**

```json
{
  "is_pro": false,
  "trial_active": true,
  "trial_days_remaining": 30,
  "subscription_plan": "free",
  "has_access": true,
  "trial_start_date": "2025-10-12T10:00:00",
  "trial_end_date": "2025-11-11T10:00:00",
  "subscription_end_date": null,
  "auto_renew": true
}
```

### 2. Google Play 구매 검증 테스트

```bash
curl -X POST "http://localhost:8000/api/subscription/verify-purchase" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "purchase_token": "test_token_12345",
    "order_id": "GPA.1234-5678-9012-34567",
    "product_id": "lotto_pro_monthly"
  }'
```

**예상 응답:**

```json
{
  "verified": true,
  "is_pro": true,
  "subscription_end_date": "2025-11-12T10:00:00",
  "message": "PRO 구독이 활성화되었습니다."
}
```

### 3. 관리자 통계 조회

```bash
curl -X GET "http://localhost:8000/api/subscription/admin/stats" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**예상 응답:**

```json
{
  "total_users": 150,
  "pro_subscribers": 15,
  "active_trial_users": 45,
  "total_trial_used": 120,
  "conversion_rate": 12.5,
  "revenue_estimate_monthly": 28500
}
```

---

## 📊 데이터 흐름

### 사용자 체험 시작 시

```
Android App
    ↓ (1) POST /start-trial
Backend Server
    ↓ (2) DB에 체험 정보 저장
    ↓ (3) 30일 후 만료일 계산
    ↓ (4) 응답 반환
Android App
    ↓ (5) 로컬에도 저장 (이중 관리)
```

### Google Play 구독 시

```
Android App
    ↓ (1) Google Play Billing 실행
    ↓ (2) 구매 완료
    ↓ (3) purchase_token, order_id 받음
    ↓ (4) POST /verify-purchase
Backend Server
    ↓ (5) 구매 정보 검증
    ↓ (6) DB에 PRO 구독 저장
    ↓ (7) 구독 만료일 설정
    ↓ (8) 응답 반환
Android App
    ↓ (9) PRO 모드 활성화
```

---

## 🔒 보안 고려사항

### 1. **구매 검증 강화 (TODO)**

현재는 기본 검증만 구현되어 있습니다. 프로덕션에서는 Google Play Developer API를 사용해야 합니다:

```python
# subscription_api.py의 verify_purchase 함수 업데이트 필요

from google.oauth2 import service_account
from googleapiclient.discovery import build

def verify_with_google_play(purchase_token, product_id):
    """
    Google Play Developer API로 구매 검증
    """
    # 서비스 계정 인증
    credentials = service_account.Credentials.from_service_account_file(
        'google-play-service-account.json',
        scopes=['https://www.googleapis.com/auth/androidpublisher']
    )

    # API 클라이언트
    service = build('androidpublisher', 'v3', credentials=credentials)

    # 구매 검증
    result = service.purchases().subscriptions().get(
        packageName='com.lotto.app',
        subscriptionId=product_id,
        token=purchase_token
    ).execute()

    return result
```

### 2. **중복 구매 방지**

- ✅ `google_play_order_id` unique 제약
- ✅ 이미 등록된 주문 체크

### 3. **인증 필수**

- ✅ 모든 API에 `get_current_user` 의존성
- ✅ JWT 토큰 검증

---

## 📈 모니터링

### 주요 지표 추적

```sql
-- 오늘 신규 체험 시작 수
SELECT COUNT(*)
FROM user_subscriptions
WHERE DATE(trial_start_date) = CURRENT_DATE;

-- 이번 주 PRO 전환 수
SELECT COUNT(*)
FROM user_subscriptions
WHERE is_pro_subscriber = TRUE
  AND subscription_start_date >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY);

-- 만료 임박 사용자 (3일 이내)
SELECT u.email, u.nickname, us.trial_end_date
FROM user_subscriptions us
JOIN users u ON us.user_id = u.id
WHERE us.trial_end_date BETWEEN CURRENT_TIMESTAMP AND DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 3 DAY)
  AND us.is_pro_subscriber = FALSE;
```

---

## 🎯 다음 단계

### 즉시

- [x] DB 마이그레이션 실행
- [ ] 서버 재시작
- [ ] API 테스트
- [ ] 안드로이드 앱 빌드

### 출시 전

- [ ] Google Play Developer API 연동
- [ ] 구매 검증 강화
- [ ] 관리자 권한 체계 추가
- [ ] 푸시 알림 시스템 (만료 안내)

### 출시 후

- [ ] 대시보드 구축
- [ ] 자동 갱신 처리
- [ ] 환불 처리 로직
- [ ] A/B 테스트 (가격, 체험 기간)

---

## ✅ 체크리스트

- [x] DB 테이블 설계
- [x] API 엔드포인트 구현
- [x] 안드로이드 앱 연동
- [x] 서버-클라이언트 이중 관리
- [x] 네트워크 오류 처리
- [ ] 실제 Google Play API 연동
- [ ] 프로덕션 배포
- [ ] 모니터링 설정

**백엔드 구독 시스템 구현 완료! 🎉**
