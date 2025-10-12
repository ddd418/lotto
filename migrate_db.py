"""
DB 마이그레이션: 전체 테이블 재생성
"""
from database import engine, Base
from models import User, SavedNumber, WinningCheck, UserSettings, WinningNumber

def migrate_db():
    print("📊 데이터베이스 마이그레이션 시작...")
    
    try:
        # 모든 테이블 생성 (이미 존재하면 무시됨)
        Base.metadata.create_all(bind=engine)
        print("✅ 모든 테이블 생성/업데이트 완료")
        
        # 기존 user_settings 데이터의 theme_mode를 light로 업데이트
        from sqlalchemy.orm import Session
        with Session(engine) as session:
            from sqlalchemy import text
            
            # theme_mode를 light로 업데이트
            result = session.execute(
                text("UPDATE user_settings SET theme_mode = 'light' WHERE theme_mode = 'system' OR theme_mode = 'dark'")
            )
            session.commit()
            print(f"✅ {result.rowcount}개 사용자의 테마를 light로 업데이트")
        
        print("\n🎉 마이그레이션 완료!")
        
    except Exception as e:
        print(f"❌ 마이그레이션 실패: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    migrate_db()
