"""
로또 데이터 업데이트 테스트 스크립트
1. DB 상태 확인 (업데이트 전)
2. 수동 업데이트 API 호출
3. DB 상태 확인 (업데이트 후)
"""
from sqlalchemy import create_engine, func
from sqlalchemy.orm import sessionmaker
from models import WinningNumber
import requests
import json
import time

BASE_URL = "http://localhost:8000"
DATABASE_URL = "sqlite:///./lotto_app.db"

# DB 연결 설정
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def check_db_status(label=""):
    """데이터베이스 상태 확인"""
    print("\n" + "=" * 70)
    print(f"📊 데이터베이스 상태 확인 {label}")
    print("=" * 70)
    
    try:
        db = SessionLocal()
        
        # 당첨번호 개수 확인
        count = db.query(func.count(WinningNumber.id)).scalar()
        print(f'\n✅ 저장된 당첨번호 개수: {count}개')
        
        if count > 0:
            # 최근 5개 회차 확인
            recent_draws = db.query(WinningNumber).order_by(
                WinningNumber.draw_number.desc()
            ).limit(5).all()
            
            print('\n📋 최근 5개 회차:')
            print('-' * 70)
            for draw in recent_draws:
                numbers = f"{draw.number1:2d}, {draw.number2:2d}, {draw.number3:2d}, {draw.number4:2d}, {draw.number5:2d}, {draw.number6:2d}"
                draw_date = draw.draw_date.strftime('%Y-%m-%d') if draw.draw_date else '날짜 없음'
                print(f'  {draw.draw_number:4d}회 ({draw_date}): [{numbers}] + 보너스 {draw.bonus_number:2d}')
            
            # 가장 오래된 회차와 최신 회차
            min_draw = db.query(func.min(WinningNumber.draw_number)).scalar()
            max_draw = db.query(func.max(WinningNumber.draw_number)).scalar()
            print(f'\n📅 데이터 범위: {min_draw}회 ~ {max_draw}회 (총 {count}개)')
        else:
            print('\n⚠️ 데이터베이스가 비어있습니다!')
        
        db.close()
        return count
        
    except Exception as e:
        print(f'\n❌ 에러: {e}')
        print('💡 서버를 먼저 실행해서 테이블을 생성해주세요!')
        return 0

def test_manual_update():
    """수동 업데이트 API 테스트"""
    print("\n" + "=" * 70)
    print("🔄 수동 업데이트 API 호출")
    print("=" * 70)
    
    try:
        print("\n📡 POST /api/update 요청 중...")
        print("⏳ 첫 실행 시 모든 회차 데이터를 가져오므로 1~3분 정도 소요될 수 있습니다...")
        response = requests.post(f"{BASE_URL}/api/update", timeout=300)  # 5분으로 증가
        
        print(f"\n✅ 상태 코드: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"\n응답 데이터:")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            
            print(f"\n📊 업데이트 결과:")
            print(f"  - 성공 여부: {result['success']}")
            print(f"  - 메시지: {result['message']}")
            print(f"  - 최신 회차: {result['last_draw']}회")
            print(f"  - 새 데이터 개수: {result['new_data_count']}개")
            print(f"  - 업데이트 시각: {result['updated_at']}")
            
            return True
        else:
            print(f"\n❌ 업데이트 실패: {response.text}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("\n❌ 서버에 연결할 수 없습니다!")
        print("💡 'python api_server.py'로 서버를 먼저 실행해주세요!")
        return False
    except Exception as e:
        print(f"\n❌ 에러 발생: {e}")
        return False

def main():
    """메인 테스트 함수"""
    print("\n" + "🎯" * 35)
    print("🎰 로또 데이터 업데이트 테스트 시작")
    print("🎯" * 35)
    
    # 1. 업데이트 전 DB 상태
    count_before = check_db_status("(업데이트 전)")
    
    # 2. 수동 업데이트 실행
    print("\n")
    time.sleep(1)
    success = test_manual_update()
    
    if success:
        # 3. 업데이트 후 DB 상태
        print("\n")
        time.sleep(1)
        count_after = check_db_status("(업데이트 후)")
        
        # 결과 요약
        print("\n" + "=" * 70)
        print("📈 테스트 결과 요약")
        print("=" * 70)
        print(f"\n  업데이트 전: {count_before}개")
        print(f"  업데이트 후: {count_after}개")
        print(f"  증가량: +{count_after - count_before}개")
        
        if count_after > count_before:
            print("\n✅ 테스트 성공! 새로운 데이터가 DB에 저장되었습니다!")
        elif count_after == count_before and count_before > 0:
            print("\n✅ 테스트 성공! 이미 최신 데이터를 보유하고 있습니다!")
        else:
            print("\n⚠️ 데이터가 증가하지 않았습니다.")
    
    print("\n" + "🎯" * 35)

if __name__ == "__main__":
    main()
