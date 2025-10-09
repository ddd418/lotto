@echo off
REM 로또 API 서버 실행 스크립트 (Windows)

echo ====================================
echo  로또 번호 추천 API 서버 시작
echo ====================================
echo.

REM 가상환경 확인 (선택사항)
if exist venv\Scripts\activate.bat (
    echo [1/3] 가상환경 활성화...
    call venv\Scripts\activate.bat
) else (
    echo [!] 가상환경이 없습니다. 전역 Python을 사용합니다.
)

REM 필수 패키지 설치 확인
echo [2/3] 패키지 설치 확인 중...
pip install -r requirements.txt --quiet

REM 서버 실행
echo [3/3] API 서버 실행 중...
echo.
echo ====================================
echo  서버 주소: http://localhost:8000
echo  API 문서: http://localhost:8000/docs
echo  헬스체크: http://localhost:8000/api/health
echo ====================================
echo.
echo 서버를 종료하려면 Ctrl+C를 누르세요.
echo.

python api_server.py

pause
