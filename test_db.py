"""
데이터베이스 테스트 스크립트
"""
import requests
import json

BASE_URL = "http://localhost:8000"

def test_health():
    """헬스 체크"""
    print("\n=== 헬스 체크 ===")
    response = requests.get(f"{BASE_URL}/api/health")
    print(f"상태 코드: {response.status_code}")
    print(f"응답: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
    return response.status_code == 200

def test_sync_winning_numbers():
    """당첨 번호 동기화 (최근 10개 회차)"""
    print("\n=== 당첨 번호 동기화 (1140~1150회차) ===")
    response = requests.post(f"{BASE_URL}/api/winning-numbers/sync?start_draw=1140&end_draw=1150")
    print(f"상태 코드: {response.status_code}")
    result = response.json()
    print(f"응답: {json.dumps(result, indent=2, ensure_ascii=False)}")
    return response.status_code == 200

def test_get_latest_winning():
    """최신 당첨 번호 조회"""
    print("\n=== 최신 당첨 번호 조회 ===")
    response = requests.get(f"{BASE_URL}/api/winning-numbers/latest")
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print(f"응답: {json.dumps(result, indent=2, ensure_ascii=False)}")
        print(f"\n📊 {result['draw_number']}회차 당첨번호: {result['numbers']} + 보너스 {result['bonus_number']}")
        if result.get('prize_1st'):
            print(f"💰 1등 당첨금: {result['prize_1st']:,}원")
    return response.status_code == 200

def test_get_winning_by_draw():
    """특정 회차 당첨 번호 조회"""
    draw_no = 1145
    print(f"\n=== {draw_no}회차 당첨 번호 조회 ===")
    response = requests.get(f"{BASE_URL}/api/winning-numbers/{draw_no}")
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print(f"응답: {json.dumps(result, indent=2, ensure_ascii=False)}")
        print(f"\n📊 {result['draw_number']}회차 당첨번호: {result['numbers']} + 보너스 {result['bonus_number']}")
    return response.status_code == 200

def test_get_winning_list():
    """당첨 번호 목록 조회"""
    print("\n=== 최근 5개 회차 당첨 번호 목록 ===")
    response = requests.get(f"{BASE_URL}/api/winning-numbers?limit=5")
    print(f"상태 코드: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print(f"총 {result['count']}개 회차")
        print(f"최신 회차: {result['latest_draw']}")
        print("\n당첨 번호 목록:")
        for winning in result['winning_numbers']:
            print(f"  {winning['draw_number']}회: {winning['numbers']} + 보너스 {winning['bonus_number']}")
    return response.status_code == 200

def main():
    """테스트 실행"""
    print("=" * 60)
    print("로또 데이터베이스 테스트 시작")
    print("=" * 60)
    
    tests = [
        ("헬스 체크", test_health),
        ("당첨 번호 동기화", test_sync_winning_numbers),
        ("최신 당첨 번호 조회", test_get_latest_winning),
        ("특정 회차 조회", test_get_winning_by_draw),
        ("당첨 번호 목록", test_get_winning_list),
    ]
    
    results = []
    for name, test_func in tests:
        try:
            result = test_func()
            results.append((name, "✅ 성공" if result else "❌ 실패"))
        except Exception as e:
            print(f"오류 발생: {e}")
            results.append((name, f"❌ 오류: {str(e)}"))
    
    print("\n" + "=" * 60)
    print("테스트 결과 요약")
    print("=" * 60)
    for name, result in results:
        print(f"{name}: {result}")
    print("=" * 60)

if __name__ == "__main__":
    main()
