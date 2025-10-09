"""
API 서버 테스트 스크립트
로컬에서 API 엔드포인트를 테스트합니다.
"""
import requests
import json
from pprint import pprint

BASE_URL = "http://localhost:8000"

def test_health():
    """헬스 체크 테스트"""
    print("\n" + "="*50)
    print("1️⃣  헬스 체크 테스트")
    print("="*50)
    
    response = requests.get(f"{BASE_URL}/api/health")
    print(f"Status Code: {response.status_code}")
    pprint(response.json())
    return response.status_code == 200

def test_recommend():
    """번호 추천 테스트"""
    print("\n" + "="*50)
    print("2️⃣  로또 번호 추천 테스트")
    print("="*50)
    
    payload = {
        "n_sets": 5,
        "seed": None  # 랜덤
    }
    
    response = requests.post(f"{BASE_URL}/api/recommend", json=payload)
    print(f"Status Code: {response.status_code}")
    
    if response.status_code == 200:
        data = response.json()
        print(f"\n기준 회차: {data['last_draw']}회")
        print(f"생성 시각: {data['generated_at']}")
        print("\n추천 번호:")
        for i, set_data in enumerate(data['sets'], 1):
            numbers = set_data['numbers']
            print(f"  {i}) {' '.join(f'{n:02d}' for n in numbers)}")
    else:
        pprint(response.json())
    
    return response.status_code == 200

def test_stats():
    """통계 조회 테스트"""
    print("\n" + "="*50)
    print("3️⃣  통계 조회 테스트")
    print("="*50)
    
    response = requests.get(f"{BASE_URL}/api/stats")
    print(f"Status Code: {response.status_code}")
    
    if response.status_code == 200:
        data = response.json()
        print(f"\n기준 회차: {data['last_draw']}회")
        print(f"생성 시각: {data['generated_at']}")
        print("\n상위 10개 번호:")
        for i, item in enumerate(data['top_10'], 1):
            print(f"  {i:2d}위: {item['number']:2d}번 - {item['count']:3d}회")
    else:
        pprint(response.json())
    
    return response.status_code == 200

def test_latest_draw():
    """최신 회차 조회 테스트"""
    print("\n" + "="*50)
    print("4️⃣  최신 회차 조회 테스트")
    print("="*50)
    
    response = requests.get(f"{BASE_URL}/api/latest-draw")
    print(f"Status Code: {response.status_code}")
    pprint(response.json())
    return response.status_code == 200

def run_all_tests():
    """모든 테스트 실행"""
    print("\n" + "🚀 "*25)
    print(" "*20 + "API 테스트 시작")
    print("🚀 "*25)
    
    results = {
        "헬스 체크": test_health(),
        "번호 추천": test_recommend(),
        "통계 조회": test_stats(),
        "최신 회차": test_latest_draw(),
    }
    
    print("\n" + "="*50)
    print("📊 테스트 결과 요약")
    print("="*50)
    for name, result in results.items():
        status = "✅ 성공" if result else "❌ 실패"
        print(f"{name}: {status}")
    
    all_passed = all(results.values())
    print("\n" + ("🎉 "*25 if all_passed else "⚠️  "*25))
    print(" "*15 + ("모든 테스트 통과!" if all_passed else "일부 테스트 실패"))
    print(("🎉 "*25 if all_passed else "⚠️  "*25) + "\n")

if __name__ == "__main__":
    try:
        run_all_tests()
    except requests.exceptions.ConnectionError:
        print("\n❌ 서버에 연결할 수 없습니다!")
        print("먼저 API 서버를 실행하세요: python api_server.py")
    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
