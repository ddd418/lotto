"""
구독 API 테스트 스크립트
"""
import requests
import json

BASE_URL = "http://localhost:8000"

# 테스트용 사용자 토큰 (실제로는 로그인 후 받은 토큰 사용)
# 먼저 카카오 로그인을 해서 토큰을 받아야 합니다
TEST_TOKEN = None


def set_token(token):
    """테스트 토큰 설정"""
    global TEST_TOKEN
    TEST_TOKEN = token


def get_headers():
    """인증 헤더"""
    if not TEST_TOKEN:
        raise ValueError("먼저 set_token()으로 토큰을 설정하세요")
    return {"Authorization": f"Bearer {TEST_TOKEN}"}


def test_subscription_status():
    """구독 상태 조회 테스트"""
    print("\n🔍 1. 구독 상태 조회")
    print("=" * 60)
    
    response = requests.get(
        f"{BASE_URL}/api/subscription/status",
        headers=get_headers()
    )
    
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(json.dumps(data, indent=2, ensure_ascii=False))
    else:
        print(f"오류: {response.text}")


def test_start_trial():
    """무료 체험 시작 테스트"""
    print("\n🎁 2. 무료 체험 시작")
    print("=" * 60)
    
    response = requests.post(
        f"{BASE_URL}/api/subscription/start-trial",
        headers=get_headers()
    )
    
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 201 or response.status_code == 200:
        data = response.json()
        print(json.dumps(data, indent=2, ensure_ascii=False))
    else:
        print(f"오류: {response.text}")


def test_verify_purchase():
    """구매 검증 테스트"""
    print("\n💳 3. 구매 검증 (PRO 구독)")
    print("=" * 60)
    
    payload = {
        "order_id": "TEST_ORDER_12345",
        "purchase_token": "TEST_TOKEN_ABC123",
        "product_id": "lotto_pro_monthly"
    }
    
    response = requests.post(
        f"{BASE_URL}/api/subscription/verify-purchase",
        headers=get_headers(),
        json=payload
    )
    
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(json.dumps(data, indent=2, ensure_ascii=False))
    else:
        print(f"오류: {response.text}")


def test_cancel_subscription():
    """구독 취소 테스트"""
    print("\n❌ 4. 구독 취소")
    print("=" * 60)
    
    response = requests.post(
        f"{BASE_URL}/api/subscription/cancel",
        headers=get_headers()
    )
    
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(json.dumps(data, indent=2, ensure_ascii=False))
    else:
        print(f"오류: {response.text}")


def test_admin_expiring_trials():
    """만료 임박 사용자 조회 (관리자)"""
    print("\n⏰ 5. 만료 임박 사용자 조회 (관리자)")
    print("=" * 60)
    
    response = requests.get(
        f"{BASE_URL}/api/subscription/admin/expiring-trials?days=3",
        headers=get_headers()
    )
    
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(json.dumps(data, indent=2, ensure_ascii=False))
    else:
        print(f"오류: {response.text}")


def test_admin_stats():
    """구독 통계 조회 (관리자)"""
    print("\n📊 6. 구독 통계 조회 (관리자)")
    print("=" * 60)
    
    response = requests.get(
        f"{BASE_URL}/api/subscription/admin/stats",
        headers=get_headers()
    )
    
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(json.dumps(data, indent=2, ensure_ascii=False))
    else:
        print(f"오류: {response.text}")


def run_all_tests():
    """모든 테스트 실행"""
    print("\n" + "=" * 60)
    print("🧪 구독 API 테스트 시작")
    print("=" * 60)
    
    try:
        # 1. 구독 상태 조회
        test_subscription_status()
        
        # 2. 무료 체험 시작
        test_start_trial()
        
        # 3. 구독 상태 다시 조회
        test_subscription_status()
        
        # 4. 구매 검증 (PRO 전환)
        test_verify_purchase()
        
        # 5. 구독 상태 다시 조회
        test_subscription_status()
        
        # 6. 관리자 기능
        test_admin_expiring_trials()
        test_admin_stats()
        
        print("\n" + "=" * 60)
        print("✅ 모든 테스트 완료!")
        print("=" * 60)
        
    except ValueError as e:
        print(f"\n❌ 오류: {e}")
        print("\n사용 방법:")
        print("1. 먼저 카카오 로그인으로 토큰을 받으세요")
        print("2. set_token('your_token_here')로 토큰 설정")
        print("3. run_all_tests() 실행")


if __name__ == "__main__":
    print("\n📱 로또연구소 구독 API 테스트")
    print("\n사용 방법:")
    print("1. Python 인터프리터에서 실행:")
    print("   >>> from test_subscription_api import *")
    print("   >>> set_token('your_jwt_token_here')")
    print("   >>> run_all_tests()")
    print("\n2. 또는 개별 테스트:")
    print("   >>> test_subscription_status()")
    print("   >>> test_start_trial()")
    print("   >>> test_verify_purchase()")
    print("\n⚠️ 참고: 먼저 /auth/kakao/login으로 로그인하여 JWT 토큰을 받으세요!")
