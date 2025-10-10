"""
당첨 확인 내역 데이터 삭제 스크립트
"""
from database import SessionLocal
from models import WinningCheck

def clear_winning_history():
    """당첨 확인 내역 모두 삭제"""
    db = SessionLocal()
    try:
        # 모든 당첨 확인 내역 삭제
        deleted_count = db.query(WinningCheck).delete()
        db.commit()
        
        print(f"✅ 당첨 확인 내역 {deleted_count}개 삭제 완료!")
        return deleted_count
        
    except Exception as e:
        print(f"❌ 삭제 실패: {e}")
        db.rollback()
        return 0
    finally:
        db.close()

if __name__ == "__main__":
    print("🗑️  당첨 확인 내역 삭제 중...")
    count = clear_winning_history()
    print(f"✨ 완료! {count}개의 내역이 삭제되었습니다.")
