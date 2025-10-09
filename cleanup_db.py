"""
데이터베이스 정리 스크립트
테스트 데이터 삭제
"""
from database import SessionLocal, engine
from models import User, SavedNumber, WinningNumber, WinningCheck

def cleanup_test_data():
    """테스트 데이터 정리"""
    db = SessionLocal()
    
    try:
        print("=" * 70)
        print("🧹 데이터베이스 정리 시작")
        print("=" * 70)
        
        # SavedNumber 테스트 데이터 확인
        saved_numbers = db.query(SavedNumber).all()
        print(f"\n📊 현재 SavedNumber 레코드: {len(saved_numbers)}개")
        
        if saved_numbers:
            print("\n삭제할 레코드:")
            for sn in saved_numbers:
                print(f"  - ID {sn.id}: {sn.nickname or '(이름없음)'} [{sn.number1}, {sn.number2}, {sn.number3}, {sn.number4}, {sn.number5}, {sn.number6}]")
            
            # SavedNumber 모두 삭제
            deleted_count = db.query(SavedNumber).delete()
            db.commit()
            print(f"\n✅ SavedNumber {deleted_count}개 삭제 완료")
        else:
            print("\n✅ 삭제할 SavedNumber 없음")
        
        # WinningCheck 데이터 확인
        winning_checks = db.query(WinningCheck).all()
        print(f"\n📊 현재 WinningCheck 레코드: {len(winning_checks)}개")
        
        if winning_checks:
            deleted_count = db.query(WinningCheck).delete()
            db.commit()
            print(f"✅ WinningCheck {deleted_count}개 삭제 완료")
        else:
            print("✅ 삭제할 WinningCheck 없음")
        
        # User 정보 확인 (삭제하지 않음 - 로그인 테스트용으로 유지)
        users = db.query(User).all()
        print(f"\n👥 현재 User 레코드: {len(users)}개 (유지)")
        for user in users:
            print(f"  - ID {user.id}: {user.nickname} (kakao_id: {user.kakao_id})")
        
        # WinningNumber 정보 확인 (삭제하지 않음)
        winning_count = db.query(WinningNumber).count()
        print(f"\n🎰 WinningNumber 레코드: {winning_count}개 (유지)")
        if winning_count > 0:
            latest = db.query(WinningNumber).order_by(WinningNumber.draw_number.desc()).first()
            print(f"  - 최신 회차: {latest.draw_number}회 ({latest.draw_date})")
        
        print("\n" + "=" * 70)
        print("✅ 데이터베이스 정리 완료!")
        print("=" * 70)
        print("\n📋 정리 결과:")
        print("  ✅ SavedNumber: 모두 삭제")
        print("  ✅ WinningCheck: 모두 삭제")
        print("  ✅ User: 유지 (테스트 로그인용)")
        print("  ✅ WinningNumber: 유지 (당첨 번호 데이터)")
        print("\n🎯 Android 앱 통합 테스트 준비 완료!")
        
    except Exception as e:
        print(f"\n❌ 에러 발생: {e}")
        db.rollback()
        import traceback
        traceback.print_exc()
    finally:
        db.close()

if __name__ == "__main__":
    cleanup_test_data()
