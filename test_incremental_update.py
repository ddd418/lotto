"""
증분 업데이트 테스트 스크립트
기존 데이터는 유지하고 새로운 회차만 추가되는지 확인
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
        db.close()
        return min_draw, max_draw, count
    else:
        print(f"📊 현재 DB: 비어있음")
        db.close()
        return None, None, 0

def test_incremental_update():
    """증분 업데이트 API 테스트"""
    print("\n" + "=" * 70)
    print("🔄 증분 업데이트 API 호출")
    print("=" * 70)
    
    try:
        print("\n📡 POST /api/update 요청 중...")
        print("⏳ 이미 있는 회차는 스킵하고 새로운 회차만 가져옵니다...")
        
        start_time = time.time()
        response = requests.post(f"{BASE_URL}/api/update", timeout=60)
        elapsed = time.time() - start_time
        
        print(f"\n✅ 상태 코드: {response.status_code}")
        print(f"⏱️  소요 시간: {elapsed:.2f}초")
        
        if response.status_code == 200:
            result = response.json()
            print(f"\n📊 업데이트 결과:")
            print(f"  - 성공 여부: {result['success']}")
            print(f"  - 메시지: {result['message']}")
            print(f"  - 최신 회차: {result['last_draw']}회")
            print(f"  - 새 데이터 개수: {result['new_data_count']}개")
            print(f"  - 업데이트 시각: {result['updated_at']}")
            return True, result['new_data_count']
        else:
            print(f"\n❌ 업데이트 실패: {response.text}")
            return False, 0
            
    except Exception as e:
        print(f"\n❌ 에러 발생: {e}")
        return False, 0

def main():
    print("\n" + "⚡" * 35)
    print("⚡ 증분 업데이트 테스트 (리소스 효율성 확인)")
    print("⚡" * 35)
    
    # 1. 업데이트 전 상태
    print("\n📋 [업데이트 전]")
    min_before, max_before, count_before = check_db_summary()
    
    # 2. 증분 업데이트 실행
    time.sleep(1)
    success, new_count = test_incremental_update()
    
    if success:
        # 3. 업데이트 후 상태
        print("\n📋 [업데이트 후]")
        time.sleep(1)
        min_after, max_after, count_after = check_db_summary()
        
        # 결과 분석
        print("\n" + "=" * 70)
        print("📈 증분 업데이트 결과 분석")
        print("=" * 70)
        
        if count_after > 0:
            print(f"\n  업데이트 전: {count_before}개 ({min_before}~{max_before}회)")
            print(f"  업데이트 후: {count_after}개 ({min_after}~{max_after}회)")
            print(f"  증가량: +{count_after - count_before}개")
            
            if new_count == 0:
                print(f"\n✅ 효율성 확인 완료!")
                print(f"   - 새로운 회차가 없어서 API 호출만 하고 DB는 변경 안됨")
                print(f"   - 기존 {count_before}개 회차를 다시 크롤링하지 않음")
                print(f"   - 리소스 낭비 없음! 🎉")
            elif new_count == (count_after - count_before):
                print(f"\n✅ 완벽한 증분 업데이트!")
                print(f"   - 새로운 {new_count}개 회차만 추가됨")
                print(f"   - 기존 {count_before}개 회차는 스킵됨")
                print(f"   - 리소스 효율적! 🎉")
            else:
                print(f"\n⚠️ 불일치 발견:")
                print(f"   - API 보고: {new_count}개 추가")
                print(f"   - 실제 DB 증가: {count_after - count_before}개")
        
        # 최근 추가된 데이터 확인
        if count_after > count_before:
            print("\n📋 새로 추가된 회차:")
            print("-" * 70)
            
            db = SessionLocal()
            new_draws = db.query(WinningNumber).filter(
                WinningNumber.draw_number > max_before
            ).order_by(WinningNumber.draw_number).all()
            
            for draw in new_draws:
                numbers = f"{draw.number1:2d}, {draw.number2:2d}, {draw.number3:2d}, {draw.number4:2d}, {draw.number5:2d}, {draw.number6:2d}"
                draw_date = draw.draw_date.strftime('%Y-%m-%d') if draw.draw_date else '날짜 없음'
                print(f"  {draw.draw_number:4d}회 ({draw_date}): [{numbers}] + 보너스 {draw.bonus_number:2d}")
            
            db.close()
    
    print("\n" + "⚡" * 35)

if __name__ == "__main__":
    main()
