# Railway 배포 가이드

## 1. Railway 프로젝트 생성

1. [Railway](https://railway.app) 접속 및 로그인
2. "New Project" 클릭
3. "Deploy from GitHub repo" 선택
4. 이 저장소 선택

## 2. 환경 변수 설정

Railway 대시보드에서 다음 환경 변수들을 설정하세요:

### 필수 환경 변수

```
KAKAO_REST_API_KEY=your_kakao_rest_api_key
SECRET_KEY=your_secret_key_here
PORT=8000
```

### 카카오 Redirect URI

배포 후 Railway에서 제공하는 도메인을 확인하고 (예: `https://your-app.up.railway.app`)  
카카오 개발자 콘솔에서 Redirect URI를 업데이트하세요:

```
https://your-app.up.railway.app/auth/kakao/callback
```

## 3. 데이터베이스 초기화

Railway 배포 후 처음 한 번만 실행:

1. Railway 대시보드에서 터미널 열기
2. DB 마이그레이션 실행:

```bash
python migrate_db.py
```

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
├── database.py            # DB 연결
├── kakao_auth.py          # 카카오 인증
├── requirements.txt       # Python 패키지
├── Procfile              # Railway 시작 명령
├── runtime.txt           # Python 버전
├── .railwayignore        # 배포 제외 파일
├── lotto.db              # SQLite 데이터베이스 (자동 생성)
└── static/               # 정적 파일 (카카오 공유 이미지)
```

## 6. 주의사항

### SQLite 데이터 지속성

Railway는 ephemeral filesystem을 사용하므로 SQLite DB는 재배포 시 초기화됩니다.  
프로덕션에서는 Railway의 PostgreSQL 플러그인 사용을 권장합니다.

### Volume 마운트 (선택사항)

데이터를 유지하려면:

1. Railway 대시보드에서 "Volumes" 탭
2. `/app/data` 경로에 볼륨 추가
3. `database.py`에서 DB 경로를 `/app/data/lotto.db`로 변경

## 7. 배포 확인

배포 후 다음 엔드포인트로 확인:

```bash
# Health check
curl https://your-app.up.railway.app/api/health

# API 문서
https://your-app.up.railway.app/docs
```

## 8. 로그 확인

Railway 대시보드의 "Deployments" 탭에서 실시간 로그 확인 가능

## 9. 자동 배포

GitHub의 main 브랜치에 push하면 자동으로 재배포됩니다.

## 10. 비용

- Starter Plan: $5/월 (500시간 실행 시간 포함)
- Hobby Plan: 무료 체험 가능 (제한적)

## 문제 해결

### 배포 실패 시

1. Railway 로그 확인
2. `requirements.txt`의 패키지 버전 확인
3. 환경 변수가 올바르게 설정되었는지 확인

### DB 연결 오류

1. `migrate_db.py` 실행 확인
2. Volume이 올바르게 마운트되었는지 확인

### 카카오 로그인 실패

1. Redirect URI가 올바른지 확인
2. KAKAO_REST_API_KEY가 설정되었는지 확인
