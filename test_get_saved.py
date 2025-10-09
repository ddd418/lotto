"""
SavedNumber GET 엔드포인트 테스트
"""
import requests
import json
from auth import TokenManager

# 테스트 유저 ID (DB에 존재하는 ID)
test_user_id = 2

# JWT 토큰 생성
token = TokenManager.create_access_token({"sub": str(test_user_id)})
print(f"🔑 생성된 토큰: {token[:50]}...")

# GET 요청 테스트
headers = {
    "Authorization": f"Bearer {token}"
}

print("\n" + "="*70)
print("📥 GET /api/saved-numbers 테스트")
print("="*70)

try:
    response = requests.get(
        "http://localhost:8000/api/saved-numbers",
        headers=headers,
        timeout=10
    )
    
    print(f"\n✅ 상태 코드: {response.status_code}")
    
    if response.status_code == 200:
        data = response.json()
        print(f"✅ 저장된 번호 개수: {len(data)}")
        for item in data:
            print(f"\n[ID: {item['id']}] {item.get('nickname', 'No Name')}")
            numbers = item['numbers']
            print(f"  Numbers: {', '.join(map(str, numbers))}")
            print(f"  Memo: {item.get('memo', 'N/A')}")
            print(f"  Favorite: {item.get('is_favorite', 'N/A')}")
            print(f"  Type: {item.get('recommendation_type', 'N/A')}")
            print(f"  Created: {item.get('created_at', 'N/A')}")
    else:
        print(f"❌ 에러 응답:")
        print(response.text)
        
except Exception as e:
    print(f"❌ 예외 발생: {e}")
    import traceback
    traceback.print_exc()

print("\n" + "="*70)
