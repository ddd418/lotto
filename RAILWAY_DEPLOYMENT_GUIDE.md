# 🚀 Railway 배포 업데이트 가이드

## 📋 현재 상황

- ✅ 로컬에서 구독 시스템 코드 완성
- ✅ `UserSubscription` 모델 추가
- ✅ 구독 API 엔드포인트 추가
- ⏳ Railway 서버에 배포 필요

---

## 🎯 Railway 배포 자동화

### Railway가 자동으로 처리하는 것:

1. ✅ Git push 감지
2. ✅ requirements.txt 설치
3. ✅ DB 테이블 자동 생성 (`Base.metadata.create_all`)
4. ✅ 서버 재시작

**즉, Git push만 하면 모든 것이 자동으로 처리됩니다!** 🎉

---

## 📝 Step 1: Git 커밋 & 푸시

```bash
# 프로젝트 루트로 이동
cd c:\projects\lotto

# 변경사항 확인
git status

# 모든 변경사항 스테이징
git add .

# 커밋
git commit -m "feat: 구독 관리 시스템 추가

- UserSubscription 모델 추가
- 무료 30일 체험 기능
- PRO 월 구독 기능 (₩1,900)
- Google Play 구매 검증 API
- 관리자 통계 대시보드
- 만료 임박 사용자 조회"

# GitHub에 푸시
git push origin main
```

---

## 🔍 Step 2: Railway 배포 확인

### 배포 진행 상황 확인

1. **Railway 대시보드** 접속

   - https://railway.app

2. **프로젝트 선택**

   - lotto 프로젝트 클릭

3. **Deployments** 탭에서 배포 로그 확인

   ```
   ✓ Building...
   ✓ Installing dependencies from requirements.txt
   ✓ Starting server
   ✓ Deployment successful
   ```

4. **배포 시간**: 약 3-5분 소요

---

## ✅ Step 3: 배포 완료 후 확인

### 1. API 문서 확인

```
https://web-production-43fb4.up.railway.app/docs
```

**확인할 것:**

- `/api/subscription/start-trial` 엔드포인트 존재
- `/api/subscription/status` 엔드포인트 존재
- `/api/subscription/verify-purchase` 엔드포인트 존재

### 2. 테이블 생성 확인

Railway 대시보드에서:

1. **프로젝트 > PostgreSQL** (또는 SQLite)
2. **Data** 탭 클릭
3. `user_subscriptions` 테이블 확인

**확인할 컬럼:**

```sql
- id
- user_id
- trial_start_date
- trial_end_date
- is_trial_used
- subscription_plan
- is_pro_subscriber
- google_play_order_id
- ...
```

### 3. API 테스트

**Postman 또는 curl로 테스트:**

```bash
# 1. 헬스 체크
curl https://web-production-43fb4.up.railway.app/api/health

# 2. 구독 API (인증 필요)
# 먼저 카카오 로그인으로 토큰 받기
curl -X POST https://web-production-43fb4.up.railway.app/auth/kakao/login \
  -H "Content-Type: application/json" \
  -d '{"access_token": "kakao_token"}'

# 3. 구독 상태 조회
curl https://web-production-43fb4.up.railway.app/api/subscription/status \
  -H "Authorization: Bearer <jwt_token>"
```

---

## 🐛 문제 발생 시 해결 방법

### 문제 1: "user_subscriptions 테이블이 없습니다"

**원인:** 모델이 제대로 임포트되지 않음

**해결:**

```python
# api_server.py 확인
from models import ..., UserSubscription  # ✅ 추가되었는지 확인

# Base.metadata.create_all(bind=engine)  # ✅ 이 라인 존재 확인
```

### 문제 2: 배포 실패

**Railway 로그 확인:**

1. Deployments > 최신 배포 클릭
2. Logs 확인
3. 오류 메시지 확인

**일반적인 원인:**

- requirements.txt 패키지 누락
- Python 버전 문제
- DB 연결 오류

### 문제 3: API가 404 응답

**확인 사항:**

```python
# api_server.py에서 라우터 등록 확인
app.include_router(subscription_router)  # ✅ 있는지 확인
```

---

## 📊 배포 후 모니터링

### Railway 로그 확인

```bash
# Railway CLI로 실시간 로그 보기
railway logs
```

또는 Railway 대시보드:

1. 프로젝트 선택
2. **Observability** 탭
3. 실시간 로그 확인

### 주요 로그 메시지

```
✅ 정상:
- INFO: Application startup complete
- INFO: Uvicorn running on http://0.0.0.0:8000
- 📊 최신 회차 정보: 1192회

❌ 오류:
- ERROR: Could not connect to database
- ERROR: Module not found: subscription_api
```

---

## 🎉 배포 완료 체크리스트

배포 후 다음을 확인하세요:

- [ ] Git push 성공
- [ ] Railway 빌드 성공
- [ ] 서버 재시작 완료
- [ ] `/docs` 페이지에서 구독 API 확인
- [ ] 테스트 API 호출 성공
- [ ] `user_subscriptions` 테이블 생성 확인
- [ ] 로그에 오류 없음

---

## 🔄 업데이트 후 Android 앱 연동

배포 완료 후 Android 앱에서:

### 1. ServiceLocator.kt 확인

```kotlin
// Railway 프로덕션 서버 URL
private const val BASE_URL = "https://web-production-43fb4.up.railway.app/"
```

### 2. 구독 API 테스트

```kotlin
// 앱 실행 시 서버에서 구독 상태 확인
val status = subscriptionRepository.getStatus()
if (status.has_access) {
    // 앱 사용 가능
} else {
    // 온보딩 화면
}
```

---

## 💡 팁: 빠른 배포 확인

### Railway CLI 사용 (선택사항)

```bash
# Railway CLI 설치
npm install -g @railway/cli

# 로그인
railway login

# 프로젝트 연결
railway link

# 실시간 로그 보기
railway logs

# 서버 상태 확인
railway status
```

---

## 📞 도움말

### Railway 공식 문서

- https://docs.railway.app/

### 배포 트러블슈팅

- https://docs.railway.app/troubleshoot/

### PostgreSQL 설정

- https://docs.railway.app/databases/postgresql

---

## 🚀 지금 바로 배포하기

### 명령어 한 줄로 배포:

```bash
cd c:\projects\lotto && git add . && git commit -m "feat: 구독 시스템 추가" && git push origin main
```

**배포 시작! 3-5분 후 완료됩니다!** 🎉

---

## ✅ 최종 확인

배포 완료 후:

```
1. https://web-production-43fb4.up.railway.app/docs
   → 구독 API 엔드포인트 확인

2. Android 앱 실행
   → 서버 연결 확인
   → 온보딩 화면 테스트

3. 무료 체험 시작
   → 서버 DB에 기록 확인
   → 30일 카운트다운 확인

4. PRO 구독 테스트
   → Google Play 테스트 결제
   → 서버 검증 확인
```

**모든 준비 완료! 이제 플레이스토어에 출시하세요! 🚀**
