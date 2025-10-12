"""
로또 당첨 번호 크롤링 및 DB 저장 유틸리티
"""
import urllib.request
import json
import logging
from datetime import datetime
from typing import Optional, Dict, List
from sqlalchemy.orm import Session

from models import WinningNumber

logger = logging.getLogger(__name__)

API_URL = "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={drw_no}"

def fetch_winning_number(draw_no: int) -> Optional[Dict]:
    """
    동행복권 API에서 특정 회차의 당첨 번호 가져오기
    
    Args:
        draw_no: 로또 회차 번호
        
    Returns:
        당첨 번호 정보 딕셔너리 또는 None (실패 시)
    """
    url = API_URL.format(drw_no=draw_no)
    try:
        with urllib.request.urlopen(url, timeout=10) as resp:
            data = resp.read().decode("utf-8")
            obj = json.loads(data)
            
            if obj.get("returnValue") == "success":
                logger.info(f"✅ {draw_no}회차 당첨 번호 가져오기 성공")
                return obj
            else:
                logger.warning(f"❌ {draw_no}회차 당첨 번호 없음 (아직 추첨 전이거나 잘못된 회차)")
                return None
    except Exception as e:
        logger.error(f"❌ {draw_no}회차 API 호출 실패: {e}")
        return None

def save_winning_number_to_db(db: Session, draw_data: Dict) -> Optional[WinningNumber]:
    """
    동행복권 API 응답을 DB에 저장
    
    Args:
        db: SQLAlchemy DB 세션
        draw_data: API에서 받은 회차 정보
        
    Returns:
        저장된 WinningNumber 객체 또는 None
    """
    try:
        draw_no = draw_data.get("drwNo")
        
        # 이미 DB에 있는지 확인
        existing = db.query(WinningNumber).filter(
            WinningNumber.draw_number == draw_no
        ).first()
        
        if existing:
            logger.info(f"⏭️  {draw_no}회차는 이미 DB에 저장되어 있음")
            return existing
        
        # 추첨일 파싱
        draw_date_str = draw_data.get("drwNoDate")  # "2024-01-06" 형식
        draw_date = None
        if draw_date_str:
            try:
                draw_date = datetime.strptime(draw_date_str, "%Y-%m-%d")
            except:
                pass
        
        # WinningNumber 객체 생성
        winning_number = WinningNumber(
            draw_number=draw_no,
            number1=draw_data.get("drwtNo1"),
            number2=draw_data.get("drwtNo2"),
            number3=draw_data.get("drwtNo3"),
            number4=draw_data.get("drwtNo4"),
            number5=draw_data.get("drwtNo5"),
            number6=draw_data.get("drwtNo6"),
            bonus_number=draw_data.get("bnusNo"),
            prize_1st=draw_data.get("firstWinamnt"),
            prize_2nd=draw_data.get("secondWinamnt"),
            prize_3rd=draw_data.get("thirdWinamnt"),
            prize_4th=draw_data.get("fourthWinamnt"),
            prize_5th=draw_data.get("fifthWinamnt"),
            winners_1st=draw_data.get("firstPrzwnerCo"),
            winners_2nd=draw_data.get("secondPrzwnerCo"),
            winners_3rd=draw_data.get("thirdPrzwnerCo"),
            winners_4th=draw_data.get("fourthPrzwnerCo"),
            winners_5th=draw_data.get("fifthPrzwnerCo"),
            total_sales=draw_data.get("totSellamnt"),
            draw_date=draw_date
        )
        
        db.add(winning_number)
        db.commit()
        db.refresh(winning_number)
        
        logger.info(f"💾 {draw_no}회차 DB 저장 완료: [{winning_number.number1}, {winning_number.number2}, {winning_number.number3}, {winning_number.number4}, {winning_number.number5}, {winning_number.number6}] + 보너스 {winning_number.bonus_number}")
        
        return winning_number
        
    except Exception as e:
        logger.error(f"❌ DB 저장 실패: {e}")
        db.rollback()
        return None

def get_latest_draw_number(start_from: Optional[int] = None) -> Optional[int]:
    """
    현재 최신 회차 번호 추정 (연속 실패 방식)
    lott.py의 collect_stats 로직 참고
    
    Args:
        start_from: 검색 시작 회차 (None이면 1회차부터, 값이 있으면 해당 회차+1부터 검색하여 최신 회차 찾기)
    
    Returns:
        최신 회차 번호 또는 None
    """
    # start_from이 있으면 그 다음 회차부터 검색 (증분 업데이트용)
    if start_from and start_from > 0:
        start_draw = start_from + 1
        logger.info(f"🔍 최신 회차 검색 시작 ({start_draw}회차부터, 연속 실패 5회까지)")
    else:
        start_draw = 1
        logger.info(f"🔍 최신 회차 검색 시작 (1회차부터 연속 실패 5회까지)")
    
    fail_streak = 0
    last_success_draw = start_from if start_from else 0  # start_from을 초기값으로 설정
    drw_no = start_draw
    
    while True:
        data = fetch_winning_number(drw_no)
        if not data:
            fail_streak += 1
            logger.debug(f"  {drw_no}회차 실패 (연속 실패: {fail_streak}회)")
            if fail_streak >= 5:
                logger.info(f"🎯 최신 회차 확정: {last_success_draw}회 (연속 실패 5회 도달)")
                break
        else:
            fail_streak = 0
            last_success_draw = drw_no
            if drw_no % 100 == 0 or (start_from and drw_no == start_draw):
                logger.info(f"  ✅ {drw_no}회차 확인됨...")
        
        drw_no += 1
        
        # 무한루프 방지 (2030년까지 약 1500회차 예상)
        if drw_no > 2000:
            logger.warning(f"⚠️ 2000회차 도달, 검색 중단")
            break
    
    return last_success_draw if last_success_draw > 0 else None

def sync_all_winning_numbers(db: Session, start_draw: int = 1, end_draw: Optional[int] = None) -> Dict:
    """
    특정 범위의 당첨 번호를 모두 DB에 동기화
    
    Args:
        db: SQLAlchemy DB 세션
        start_draw: 시작 회차
        end_draw: 종료 회차 (None이면 최신 회차까지)
        
    Returns:
        통계 정보 딕셔너리
    """
    if end_draw is None:
        end_draw = get_latest_draw_number()
        if end_draw is None:
            logger.error("❌ 최신 회차를 찾을 수 없습니다")
            return {"success": False, "error": "최신 회차를 찾을 수 없음"}
    
    logger.info(f"🔄 {start_draw}회 ~ {end_draw}회 동기화 시작")
    
    success_count = 0
    skip_count = 0
    fail_count = 0
    
    for draw_no in range(start_draw, end_draw + 1):
        # DB에 이미 있는지 확인
        existing = db.query(WinningNumber).filter(
            WinningNumber.draw_number == draw_no
        ).first()
        
        if existing:
            skip_count += 1
            if draw_no % 100 == 0:
                logger.info(f"⏭️  {draw_no}회차 스킵 (이미 존재)")
            continue
        
        # API에서 가져오기
        draw_data = fetch_winning_number(draw_no)
        if not draw_data:
            fail_count += 1
            continue
        
        # DB에 저장
        result = save_winning_number_to_db(db, draw_data)
        if result:
            success_count += 1
        else:
            fail_count += 1
    
    logger.info(f"✅ 동기화 완료: 성공 {success_count}개, 스킵 {skip_count}개, 실패 {fail_count}개")
    
    return {
        "success": True,
        "success_count": success_count,
        "skip_count": skip_count,
        "fail_count": fail_count,
        "total": end_draw - start_draw + 1
    }

def get_or_fetch_winning_number(db: Session, draw_no: int) -> Optional[WinningNumber]:
    """
    DB에서 당첨 번호 조회, 없으면 API에서 가져와서 저장
    
    Args:
        db: SQLAlchemy DB 세션
        draw_no: 회차 번호
        
    Returns:
        WinningNumber 객체 또는 None
    """
    # 1. DB 조회
    winning = db.query(WinningNumber).filter(
        WinningNumber.draw_number == draw_no
    ).first()
    
    if winning:
        logger.info(f"📦 {draw_no}회차 DB에서 조회 성공")
        return winning
    
    # 2. API에서 가져오기
    logger.info(f"🌐 {draw_no}회차 API에서 가져오는 중...")
    draw_data = fetch_winning_number(draw_no)
    
    if not draw_data:
        return None
    
    # 3. DB에 저장
    return save_winning_number_to_db(db, draw_data)

def get_latest_winning_numbers(db: Session, count: int = 10) -> List[WinningNumber]:
    """
    최신 당첨 번호 N개 조회
    
    Args:
        db: SQLAlchemy DB 세션
        count: 조회할 개수
        
    Returns:
        WinningNumber 리스트 (최신순)
    """
    return db.query(WinningNumber).order_by(
        WinningNumber.draw_number.desc()
    ).limit(count).all()
