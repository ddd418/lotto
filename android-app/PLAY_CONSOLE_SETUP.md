# Google Play Console 구독 설정 가이드

## 1️⃣ Play Console 접속

1. https://play.google.com/console 접속
2. 로또연구소 앱 선택

## 2️⃣ 구독 상품 만들기

### 단계 1: 수익 창출 설정

1. 좌측 메뉴에서 **"수익 창출" > "정기 결제"** 선택
2. **"정기 결제 만들기"** 버튼 클릭

### 단계 2: 기본 정보 입력

- **제품 ID**: `lotto_pro_monthly` (코드와 정확히 일치해야 함)
- **이름**: 로또연구소 PRO (한국어)
- **설명**: 광고 없이 모든 기능을 무제한으로 이용하세요

### 단계 3: 가격 설정

1. **"가격 및 배포"** 섹션
2. **"가격 추가"** 클릭
3. **기본 가격**: 1,000원 (KRW)
4. **청구 기간**: 월간 (1개월마다)
5. **갱신 유형**: 자동 갱신

### 단계 4: 무료 체험 설정 (선택사항)

⚠️ **중요**: 앱에서 이미 30일 체험을 제공하고 있으므로, Play Console에서는 무료 체험을 설정하지 않아도 됩니다.

만약 Play Console 무료 체험을 사용하려면:

1. **"무료 체험"** 활성화
2. **기간**: 1개월
3. 단, 앱 코드에서 trial 로직을 제거해야 함

### 단계 5: 상품 활성화

1. 모든 정보 입력 완료 후 **"저장"** 클릭
2. **"활성화"** 버튼 클릭
3. 상태가 **"활성"**으로 변경되면 완료

## 3️⃣ 라이선스 테스터 추가 (테스트용)

### 테스트 계정 추가

1. **"설정" > "라이선스 테스트"** 이동
2. 테스트할 Google 계정 추가
3. 이 계정으로는 실제 결제 없이 테스트 가능

### 테스트 방법

1. 테스터 계정으로 앱 실행
2. 구독 시도
3. "테스트 결제" 카드가 표시됨
4. 실제 청구 없이 구독 완료

## 4️⃣ 서버 검증 설정 (중요)

### Google Play Developer API 활성화

1. **Google Cloud Console** (https://console.cloud.google.com) 접속
2. Play Console과 연결된 프로젝트 선택
3. **"API 및 서비스" > "라이브러리"** 이동
4. **"Google Play Developer API"** 검색 및 활성화

### 서비스 계정 생성

1. **"API 및 서비스" > "사용자 인증 정보"** 이동
2. **"사용자 인증 정보 만들기" > "서비스 계정"** 선택
3. 서비스 계정 이름: `lotto-subscription-validator`
4. **JSON 키 다운로드** (이 키를 Railway 백엔드에 업로드해야 함)

### Play Console에 서비스 계정 권한 부여

1. Play Console로 돌아가기
2. **"설정" > "API 액세스"** 이동
3. 생성한 서비스 계정 찾기
4. **"권한 부여"** 클릭
5. **"재무 데이터" > "주문 및 정기 결제 확인"** 권한 활성화
6. **"저장"** 클릭

## 5️⃣ Railway 백엔드 설정

### 환경 변수 추가

Railway 대시보드에서 다음 환경 변수 추가:

```bash
# Google Play Developer API 서비스 계정 키 (JSON 전체 내용)
GOOGLE_APPLICATION_CREDENTIALS_JSON={"type":"service_account",...}

# 또는 파일 경로 방식
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json

# 패키지 이름
ANDROID_PACKAGE_NAME=com.lotto.app
```

## 6️⃣ 검증 체크리스트

### 코드 확인

- [x] `SubscriptionManager.kt`의 `SUBSCRIPTION_PRODUCT_ID` = `"lotto_pro_monthly"`
- [x] `AndroidManifest.xml`에 `com.android.vending.BILLING` 권한 추가됨
- [x] `build.gradle`에 Billing 라이브러리 의존성 추가됨

### Play Console 확인

- [ ] 구독 상품 생성 및 활성화 완료
- [ ] 가격 ₩1,000/월 설정
- [ ] 자동 갱신 활성화
- [ ] 라이선스 테스터 추가 (본인 계정)

### 서버 확인

- [ ] Google Play Developer API 활성화
- [ ] 서비스 계정 생성 및 키 다운로드
- [ ] Play Console에 서비스 계정 권한 부여
- [ ] Railway에 서비스 계정 키 환경 변수 추가

## 7️⃣ 테스트 시나리오

### 테스트 1: 구독 시작

1. 앱에서 "프로 플랜으로 시작하기" 클릭
2. Google Play 결제 화면 표시 확인
3. 테스트 카드로 결제 진행
4. 구독 완료 후 PRO 기능 활성화 확인

### 테스트 2: 구독 복원

1. 앱 삭제 후 재설치
2. 동일 계정으로 로그인
3. 자동으로 PRO 상태 복원 확인

### 테스트 3: 구독 취소

1. Play Store > 내 정기 결제 이동
2. 로또연구소 PRO 취소
3. 앱에서 취소 반영 확인

## 📝 주의사항

1. **테스트 결제는 실제 청구되지 않습니다**

   - 라이선스 테스터로 추가된 계정만 해당
   - 일반 사용자는 실제 청구됨

2. **상품 ID는 절대 변경 불가**

   - 한 번 생성하면 수정 불가
   - 잘못 만들었으면 새로 생성 후 코드 수정

3. **서버 검증은 필수**

   - 클라이언트만으로는 해킹 가능
   - 반드시 백엔드에서 Google API로 검증해야 함

4. **환불 정책 명시**
   - Play Console에서 환불 정책 설정
   - 앱 내에도 명시 권장

## 🚀 다음 단계

1. ✅ Play Console에서 구독 상품 생성
2. ✅ 라이선스 테스터 추가
3. ✅ Google Play Developer API 설정
4. ✅ Railway 환경 변수 설정
5. ⏭️ 앱 테스트
6. ⏭️ 프로덕션 배포
