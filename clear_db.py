"""
데이터베이스 초기화 스크립트
당첨번호 데이터를 모두 삭제합니다.
"""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from models import WinningNumber

DATABASE_URL = "sqlite:///./lotto_app.db"

# DB 연결 설정
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def clear_winning_numbers():
    """당첨번호 테이블의 모든 데이터 삭제"""
    print("\n" + "=" * 70)
    print("🗑️  데이터베이스 초기화")
    print("=" * 70)
    
    db = SessionLocal()
    
    try:
        # 삭제 전 개수 확인
        count_before = db.query(WinningNumber).count()
        print(f"\n📊 현재 저장된 당첨번호: {count_before}개")
        
        if count_before == 0:
            print("\n✅ 이미 데이터베이스가 비어있습니다!")
            db.close()
            return
        
        # 확인 메시지
        print(f"\n⚠️  {count_before}개의 당첨번호 데이터를 모두 삭제합니다.")
        
        # 모든 당첨번호 삭제
        deleted = db.query(WinningNumber).delete()
        db.commit()
        
        # 삭제 후 개수 확인
        count_after = db.query(WinningNumber).count()
        
        print(f"\n✅ 삭제 완료!")
        print(f"   - 삭제됨: {deleted}개")
        print(f"   - 남은 데이터: {count_after}개")
        
    except Exception as e:
        print(f"\n❌ 에러 발생: {e}")
        db.rollback()
    finally:
        db.close()
    
    print("=" * 70)

if __name__ == "__main__":
    print("\n" + "🎰" * 35)
    print("🗑️  로또 당첨번호 데이터 초기화")
    print("🎰" * 35)
    
    clear_winning_numbers()
    
    print("\n💡 이제 'python test_full_sync.py'로 전체 동기화를 실행하세요!")
    print("\n" + "🎰" * 35)
