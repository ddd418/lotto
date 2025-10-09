"""
JWT 토큰 생성 및 검증 테스트
"""
from auth import TokenManager
from models import User
from database import SessionLocal

# 1. 테스트 유저 찾기
db = SessionLocal()
test_user = db.query(User).filter(User.kakao_id == "test_kakao_id_001").first()

if not test_user:
    print("❌ 테스트 유저가 없습니다!")
    print("먼저 test_saved_numbers.py를 한번 실행해서 유저를 생성하세요.")
    exit()

print(f"✅ 테스트 유저: {test_user.nickname} (ID: {test_user.id})")

# 2. 토큰 생성
print("\n🔑 JWT 토큰 생성 중...")
token_manager = TokenManager()

# sub를 문자열로 변환해서 시도
access_token = token_manager.create_access_token(data={"sub": str(test_user.id)})

print(f"✅ Access Token 생성 완료")
print(f"   Token: {access_token[:100]}...")

# 3. 토큰 검증
print("\n🔍 토큰 검증 중...")
try:
    payload = token_manager.verify_token(access_token)
    print(f"✅ 토큰 검증 성공!")
    print(f"   Payload: {payload}")
    
    # 4. 사용자 ID 추출
    user_id = token_manager.get_user_id_from_token(access_token)
    print(f"\n👤 사용자 ID 추출: {user_id} (타입: {type(user_id)})")
    
    # 5. DB에서 유저 조회
    found_user = db.query(User).filter(User.id == int(user_id), User.is_active == True).first()
    if found_user:
        print(f"✅ DB에서 유저 찾기 성공: {found_user.nickname}")
    else:
        print(f"❌ DB에서 유저를 찾을 수 없습니다!")
        print(f"   검색한 ID: {user_id} (타입: {type(user_id)})")
        print(f"   실제 ID: {test_user.id} (타입: {type(test_user.id)})")
        
except Exception as e:
    print(f"❌ 에러 발생: {e}")
    import traceback
    traceback.print_exc()

db.close()

print("\n" + "=" * 70)
print("💡 해결 방법:")
print("   - JWT payload에서 'sub'를 문자열로 저장하고")
print("   - 검증 시 int()로 변환해야 할 수 있습니다")
print("=" * 70)
