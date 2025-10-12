# Railway 배포 가이드

## 1. Railway 프로젝트 및 PostgreSQL 생성

1. [Railway](https://railway.app) 접속 및 로그인
2. "New Project" 클릭
3. "Deploy from GitHub repo" 선택
4. 이 저장소 선택
5. **PostgreSQL 추가:**
   - 프로젝트 대시보드에서 "New" 클릭
   - "Database" → "Add PostgreSQL" 선택
   - 자동으로 `DATABASE_URL` 환경변수 생성됨

## 2. 환경 변수 설정

Railway 대시보드의 **백엔드 서비스**에서 다음 환경 변수들을 설정하세요:

### 필수 환경 변수

```env
KAKAO_REST_API_KEY=your_kakao_rest_api_key
SECRET_KEY=your_secret_key_here
DATABASE_URL=${{Postgres.DATABASE_URL}}
```

> **참고:** `DATABASE_URL`은 PostgreSQL 서비스의 변수를 참조합니다.  
> Railway에서 `${{Postgres.DATABASE_URL}}` 형식으로 자동 연결됩니다.

### 카카오 Redirect URI

배포 후 Railway에서 제공하는 도메인을 확인하고 (예: `https://your-app.up.railway.app`)  
카카오 개발자 콘솔에서 Redirect URI를 업데이트하세요:

```
https://your-app.up.railway.app/auth/kakao/callback
```

## 3. 데이터베이스 자동 초기화

배포 시 `init_db.py` 스크립트가 자동으로 실행되어:
- ✅ 모든 테이블 생성
- ✅ 최근 100회차 당첨 번호 크롤링 및 저장
- ✅ 이후 배포 시에는 최신 데이터만 업데이트

> 수동으로 재초기화가 필요한 경우:
> ```bash
> python init_db.py
> ```

## 4. Android 앱 설정 업데이트

배포 완료 후 Android 앱의 `ServiceLocator.kt` 수정:

```kotlin
private const val BASE_URL = "https://your-app.up.railway.app/"
```

## 5. 배포 파일 구조

```
lotto/
├── api_server.py          # FastAPI 메인 서버
├── models.py              # DB 모델
├── database.py            # DB 연결 (PostgreSQL/SQLite 자동 감지)
├── kakao_auth.py          # 카카오 인증
├── init_db.py            # DB 초기화 스크립트 (자동 실행)
├── requirements.txt       # Python 패키지
├── Procfile              # Railway 시작 명령
├── runtime.txt           # Python 버전
├── railway.json          # Railway 설정
├── .railwayignore        # 배포 제외 파일
└── static/               # 정적 파일 (카카오 공유 이미지)
```

## 6. 배포 확인

배포 후 다음 엔드포인트로 확인:

```bash
# Health check
curl https://your-app.up.railway.app/api/health

# API 문서
https://your-app.up.railway.app/docs

# 당첨 번호 조회
curl https://your-app.up.railway.app/api/stats
```

## 7. 로그 확인

Railway 대시보드의 "Deployments" 탭에서 실시간 로그 확인 가능

## 8. 자동 배포

GitHub의 main 브랜치에 push하면 자동으로 재배포됩니다.

## 9. 비용

- **Hobby Plan**: $5/월 크레딧 (개발용)
- **PostgreSQL**: 사용량 기반 ($5/월 크레딧에 포함)
- **예상 비용**: 소규모 앱의 경우 월 $5 이내

## 문제 해결

### 배포 실패 시

1. Railway 로그 확인
2. `requirements.txt`의 패키지 버전 확인
3. 환경 변수가 올바르게 설정되었는지 확인
4. PostgreSQL 서비스가 실행 중인지 확인

### DB 연결 오류

1. `DATABASE_URL` 환경변수 확인
2. PostgreSQL 서비스가 정상 작동하는지 확인
3. Railway 대시보드에서 PostgreSQL 로그 확인

### 초기 데이터 로딩 실패

1. Railway 로그에서 `init_db.py` 실행 로그 확인
2. 수동으로 다시 실행:
   ```bash
   railway run python init_db.py
   ```

1. `migrate_db.py` 실행 확인
2. Volume이 올바르게 마운트되었는지 확인

### 카카오 로그인 실패

1. Redirect URI가 올바른지 확인
2. KAKAO_REST_API_KEY가 설정되었는지 확인
