"""
SavedNumber 테이블 데이터 확인
"""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from models import SavedNumber, User

DATABASE_URL = "sqlite:///./lotto_app.db"

# DB 연결
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

db = SessionLocal()

print("=" * 70)
print("📊 SavedNumber 테이블 데이터 확인")
print("=" * 70)

# 전체 저장번호 조회
saved_numbers = db.query(SavedNumber).all()

print(f"\n총 {len(saved_numbers)}개의 저장된 번호:")

for saved in saved_numbers:
    print(f"\n[ID: {saved.id}] User: {saved.user_id}")
    print(f"  Numbers: {saved.number1}, {saved.number2}, {saved.number3}, {saved.number4}, {saved.number5}, {saved.number6}")
    print(f"  Nickname: {saved.nickname}")
    print(f"  Memo: {saved.memo}")
    print(f"  Favorite: {saved.is_favorite}")
    print(f"  Type: {saved.recommendation_type}")
    print(f"  Created: {saved.created_at}")

# 문제가 있는 레코드 찾기
print("\n" + "=" * 70)
print("🔍 NULL 값 확인")
print("=" * 70)

for saved in saved_numbers:
    issues = []
    if saved.number1 is None: issues.append("number1")
    if saved.number2 is None: issues.append("number2")
    if saved.number3 is None: issues.append("number3")
    if saved.number4 is None: issues.append("number4")
    if saved.number5 is None: issues.append("number5")
    if saved.number6 is None: issues.append("number6")
    if saved.created_at is None: issues.append("created_at")
    
    if issues:
        print(f"\n❌ ID {saved.id}: NULL 필드 발견 - {', '.join(issues)}")

# User 테이블 확인
print("\n" + "=" * 70)
print("👥 User 테이블 확인")
print("=" * 70)

users = db.query(User).all()
for user in users:
    count = db.query(SavedNumber).filter(SavedNumber.user_id == user.id).count()
    print(f"User {user.id} ({user.nickname}): {count}개 번호 저장")

db.close()

print("\n" + "=" * 70)
print("💡 문제 해결:")
print("   - NULL 값이 있는 레코드는 삭제하거나 수정해야 합니다")
print("   - DELETE FROM saved_numbers WHERE number1 IS NULL;")
print("=" * 70)
