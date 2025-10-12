"""
구독 관리 테이블 추가 마이그레이션

실행 방법:
python add_subscription_table.py
"""
from database import engine, SessionLocal
from models import Base, UserSubscription
from sqlalchemy import inspect

def migrate():
    """구독 테이블 추가"""
    print("🔄 구독 관리 테이블 마이그레이션 시작...")
    
    # Inspector로 기존 테이블 확인
    inspector = inspect(engine)
    existing_tables = inspector.get_table_names()
    
    print(f"📋 기존 테이블: {existing_tables}")
    
    # user_subscriptions 테이블이 없으면 생성
    if "user_subscriptions" not in existing_tables:
        print("✅ user_subscriptions 테이블 생성 중...")
        UserSubscription.__table__.create(engine)
        print("✅ user_subscriptions 테이블 생성 완료!")
    else:
        print("ℹ️  user_subscriptions 테이블이 이미 존재합니다.")
    
    # 테이블 확인
    inspector = inspect(engine)
    if "user_subscriptions" in inspector.get_table_names():
        columns = inspector.get_columns("user_subscriptions")
        print(f"\n📊 user_subscriptions 테이블 컬럼:")
        for col in columns:
            print(f"  - {col['name']}: {col['type']}")
        
        print("\n✅ 마이그레이션 완료!")
        return True
    else:
        print("\n❌ 마이그레이션 실패!")
        return False

if __name__ == "__main__":
    try:
        success = migrate()
        if success:
            print("\n🎉 구독 관리 시스템이 준비되었습니다!")
            print("\n📚 다음 단계:")
            print("1. 서버 재시작: python api_server.py")
            print("2. API 문서 확인: http://localhost:8000/docs")
            print("3. 구독 API 테스트:")
            print("   - POST /api/subscription/start-trial")
            print("   - GET  /api/subscription/status")
            print("   - POST /api/subscription/verify-purchase")
    except Exception as e:
        print(f"\n❌ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
