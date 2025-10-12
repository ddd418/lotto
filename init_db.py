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
                # DB가 비어있음 → 전체 크롤링
                logger.info("🔄 당첨 번호 데이터 크롤링 중 (전체 데이터 없음)")
                latest = 1193  # 현재 알려진 최신 회차 (수동 업데이트 필요)
                logger.info(f"📊 1회 ~ {latest}회 전체 크롤링 시작 (약 5-10분 소요)")
                sync_all_winning_numbers(db, start_draw=1, end_draw=latest)
                logger.info("✅ 당첨 번호 데이터 저장 완료")
            else:
                # DB에 데이터가 있음 → 마지막 데이터의 날짜 확인
                from sqlalchemy import func, desc
                from datetime import datetime, timezone
                
                last_draw = db.query(WinningNumber).order_by(desc(WinningNumber.draw_number)).first()
                
                if last_draw and last_draw.draw_date:
                    # 마지막 추첨일과 현재 날짜 비교
                    days_diff = (datetime.now(timezone.utc) - last_draw.draw_date).days
                    
                    logger.info(f"ℹ️ 이미 {existing_count}개의 당첨 번호가 존재합니다")
                    logger.info(f"📅 마지막 회차: {last_draw.draw_number}회 ({last_draw.draw_date.strftime('%Y-%m-%d')})")
                    logger.info(f"⏱️ 경과 일수: {days_diff}일")
                    
                    if days_diff >= 8:
                        # 8일 이상 차이 → 마지막 회차+1부터 크롤링
                        logger.info(f"🔄 마지막 회차 이후 데이터 크롤링 중 ({last_draw.draw_number + 1}회부터)")
                        latest = get_latest_draw_number()
                        if latest and latest > last_draw.draw_number:
                            sync_all_winning_numbers(db, start_draw=last_draw.draw_number + 1, end_draw=latest)
                            logger.info(f"✅ {last_draw.draw_number + 1}회 ~ {latest}회 데이터 저장 완료")
                        else:
                            logger.info("ℹ️ 새로운 회차가 없습니다")
                    else:
                        # 8일 미만 → 최근 2회차만 업데이트
                        logger.info("🔄 최신 당첨 번호 업데이트 중 (최근 2회차)")
                        latest = get_latest_draw_number()
                        if latest:
                            start = max(1, latest - 1)
                            sync_all_winning_numbers(db, start_draw=start, end_draw=latest)
                            logger.info(f"✅ {start}회 ~ {latest}회 데이터 업데이트 완료")
                else:
                    logger.warning("⚠️ 마지막 회차의 날짜 정보가 없습니다")
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
