# Railway용 Dockerfile - 경량 버전 (네이버 검색 기반, Selenium 불필요)
FROM python:3.11-slim

# 기본 도구만 설치 (Chrome 불필요)
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# 의존성 파일 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 코드 복사
COPY . .

# 환경 변수 설정
ENV PYTHONUNBUFFERED=1
ENV PORT=8000

# 포트 노출
EXPOSE 8000

# 시작 명령 - Python에서 직접 PORT 환경변수 처리 (uvicorn 직접 호출 아님)
CMD ["python", "api_server.py"]
