"""
데이터베이스 설정
"""
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os
from dotenv import load_dotenv

load_dotenv()

# 데이터베이스 URL 설정
# Railway private network 우선 사용 (egress 비용 절감)
DATABASE_URL = os.getenv(
    "DATABASE_PRIVATE_URL",  # Railway private network (우선)
    os.getenv(
        "DATABASE_URL",  # 기본 DATABASE_URL
        "sqlite:///./lotto_app.db"  # 개발용 SQLite
    )
)

# SQLite인 경우 connect_args 추가
if DATABASE_URL.startswith("sqlite"):
    engine = create_engine(
        DATABASE_URL,
        connect_args={"check_same_thread": False}
    )
else:
    # PostgreSQL, MySQL 등을 위한 설정
    engine = create_engine(DATABASE_URL)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

def get_db():
    """
    데이터베이스 세션 의존성
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()