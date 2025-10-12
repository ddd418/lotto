#!/usr/bin/env python3
"""
Railway 배포 시 실행할 초기화 스크립트
- 데이터베이스 테이블 생성
- 최신 당첨 번호 크롤링 및 저장
"""
import sys
from pathlib import Path

# 프로젝트 루트를 Python 경로에 추가
sys.path.insert(0, str(Path(__file__).parent))

from database import engine, SessionLocal
from models import Base, WinningNumber
from lotto_crawler import sync_all_winning_numbers, get_latest_draw_number
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def init_database():
    """데이터베이스 초기화"""
    try:
        # 1. 모든 테이블 생성
        logger.info("📊 데이터베이스 테이블 생성 중...")
        Base.metadata.create_all(bind=engine)
        logger.info("✅ 테이블 생성 완료")
        
        # 2. 당첨 번호 동기화
        db = SessionLocal()
        try:
            # 이미 데이터가 있는지 확인
            existing_count = db.query(WinningNumber).count()
            
            if existing_count == 0:
                logger.info("🔄 당첨 번호 데이터 크롤링 중...")
                # 최신 회차 확인
                latest = get_latest_draw_number()
                if latest:
                    # 최근 100회차만 크롤링 (최신 - 99부터 최신까지)
                    start = max(1, latest - 99)
                    sync_all_winning_numbers(db, start_draw=start, end_draw=latest)
                    logger.info("✅ 당첨 번호 데이터 저장 완료")
                else:
                    logger.warning("⚠️ 최신 회차를 확인할 수 없습니다")
            else:
                logger.info(f"ℹ️ 이미 {existing_count}개의 당첨 번호가 존재합니다")
                # 최신 데이터만 업데이트
                logger.info("🔄 최신 당첨 번호 업데이트 중...")
                latest = get_latest_draw_number()
                if latest:
                    # 최근 10회차만 업데이트
                    start = max(1, latest - 9)
                    sync_all_winning_numbers(db, start_draw=start, end_draw=latest)
                    logger.info("✅ 최신 데이터 업데이트 완료")
        finally:
            db.close()
        
        logger.info("🎉 데이터베이스 초기화 완료!")
        return True
        
    except Exception as e:
        logger.error(f"❌ 초기화 실패: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = init_database()
    sys.exit(0 if success else 1)
