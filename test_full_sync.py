"""
전체 로또 당첨번호 동기화 테스트
"""
from sqlalchemy import create_engine, func
from sqlalchemy.orm import sessionmaker
from models import WinningNumber
import requests
import time

BASE_URL = "http://localhost:8000"
DATABASE_URL = "sqlite:///./lotto_app.db"

# DB 연결 설정
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def check_db_summary():
    """DB 요약 정보"""
    db = SessionLocal()
    count = db.query(func.count(WinningNumber.id)).scalar()
    
    if count > 0:
        min_draw = db.query(func.min(WinningNumber.draw_number)).scalar()
        max_draw = db.query(func.max(WinningNumber.draw_number)).scalar()
        print(f"📊 현재 DB: {min_draw}회 ~ {max_draw}회 (총 {count}개)")
    else:
        print(f"📊 현재 DB: 비어있음")
    
    db.close()
    return count

def sync_all_draws():
    """전체 회차 동기화"""
    print("\n" + "=" * 70)
    print("🔄 전체 회차 동기화 시작")
    print("=" * 70)
    
    try:
        # 1회부터 최신까지 동기화 (end_draw를 지정하지 않으면 자동으로 최신까지)
        print("\n📡 POST /api/winning-numbers/sync?start_draw=1 요청 중...")
        print("⏳ 전체 회차를 동기화하므로 1~3분 정도 소요될 수 있습니다...")
        
        response = requests.post(
            f"{BASE_URL}/api/winning-numbers/sync",
            params={"start_draw": 1},
            timeout=300
        )
        
        print(f"\n✅ 상태 코드: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"\n📊 동기화 결과:")
            print(f"  - 성공 여부: {result['success']}")
            print(f"  - 메시지: {result['message']}")
            print(f"  - 새로 추가: {result['success_count']}개")
            print(f"  - 이미 존재: {result['skip_count']}개")
            print(f"  - 실패: {result['fail_count']}개")
            print(f"  - 전체 처리: {result['total']}개")
            return True
        else:
            print(f"\n❌ 동기화 실패: {response.text}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("\n❌ 서버에 연결할 수 없습니다!")
        return False
    except Exception as e:
        print(f"\n❌ 에러 발생: {e}")
        return False

def main():
    print("\n" + "🎰" * 35)
    print("🎲 로또 당첨번호 전체 동기화 테스트")
    print("🎰" * 35)
    
    # 동기화 전 상태
    print("\n📋 [동기화 전]")
    count_before = check_db_summary()
    
    # 동기화 실행
    time.sleep(1)
    success = sync_all_draws()
    
    if success:
        # 동기화 후 상태
        print("\n📋 [동기화 후]")
        time.sleep(1)
        count_after = check_db_summary()
        
        # 최근 5개 회차 확인
        print("\n📋 최근 5개 회차:")
        print("-" * 70)
        
        db = SessionLocal()
        recent_draws = db.query(WinningNumber).order_by(
            WinningNumber.draw_number.desc()
        ).limit(5).all()
        
        for draw in recent_draws:
            numbers = f"{draw.number1:2d}, {draw.number2:2d}, {draw.number3:2d}, {draw.number4:2d}, {draw.number5:2d}, {draw.number6:2d}"
            draw_date = draw.draw_date.strftime('%Y-%m-%d') if draw.draw_date else '날짜 없음'
            print(f"  {draw.draw_number:4d}회 ({draw_date}): [{numbers}] + 보너스 {draw.bonus_number:2d}")
        
        db.close()
        
        # 결과 요약
        print("\n" + "=" * 70)
        print("📈 동기화 결과 요약")
        print("=" * 70)
        print(f"\n  동기화 전: {count_before}개")
        print(f"  동기화 후: {count_after}개")
        print(f"  증가량: +{count_after - count_before}개")
        
        if count_after > count_before:
            print(f"\n✅ 성공! {count_after - count_before}개의 새로운 회차가 추가되었습니다!")
        else:
            print("\n✅ 이미 모든 데이터가 동기화되어 있습니다!")
    
    print("\n" + "🎰" * 35)

if __name__ == "__main__":
    main()
