"""
로또 앱 통합 테스트 (인증 필요한 기능들)
카카오 로그인 없이 테스트용 토큰으로 테스트
"""
import requests
import json
from datetime import datetime

BASE_URL = "http://localhost:8000"

# 테스트용 사용자 정보 (실제로는 카카오 로그인 필요)
# 여기서는 API 테스트만 확인
TEST_TOKEN = None  # 실제 로그인 후 받은 토큰 사용

def print_section(title):
    """섹션 제목 출력"""
    print(f"\n{'='*60}")
    print(f"=== {title} ===")
    print('='*60)

def print_response(response):
    """응답 출력"""
    print(f"상태 코드: {response.status_code}")
    try:
        data = response.json()
        print(f"응답: {json.dumps(data, indent=2, ensure_ascii=False)}")
        return data
    except:
        print(f"응답: {response.text}")
        return None

def get_headers():
    """인증 헤더 생성"""
    if TEST_TOKEN:
        return {
            "Authorization": f"Bearer {TEST_TOKEN}",
            "Content-Type": "application/json"
        }
    return {"Content-Type": "application/json"}

def test_health_check():
    """헬스 체크 테스트"""
    print_section("헬스 체크")
    response = requests.get(f"{BASE_URL}/api/health")
    return print_response(response)

def test_sync_winning_numbers():
    """당첨 번호 동기화 테스트"""
    print_section("당첨 번호 동기화 (최근 10개 회차)")
    
    # 최신 회차 확인
    health = requests.get(f"{BASE_URL}/api/health").json()
    latest_draw = health.get("last_draw", 1150)
    
    start_draw = max(1, latest_draw - 9)  # 최근 10개
    
    response = requests.post(
        f"{BASE_URL}/api/winning-numbers/sync",
        params={"start_draw": start_draw}
    )
    return print_response(response)

def test_get_latest_winning():
    """최신 당첨 번호 조회"""
    print_section("최신 당첨 번호 조회")
    response = requests.get(f"{BASE_URL}/api/winning-numbers/latest")
    data = print_response(response)
    
    if data:
        print(f"\n📊 {data['draw_number']}회차 당첨번호: {data['numbers']} + 보너스 {data['bonus_number']}")
        if data.get('prize_1st'):
            print(f"💰 1등 당첨금: {data['prize_1st']:,}원")
    
    return data

def test_get_winning_list():
    """당첨 번호 목록 조회"""
    print_section("최근 5개 회차 당첨 번호 목록")
    response = requests.get(f"{BASE_URL}/api/winning-numbers", params={"limit": 5})
    data = print_response(response)
    
    if data and data.get('winning_numbers'):
        print(f"\n총 {data['count']}개 회차")
        print(f"최신 회차: {data['latest_draw']}\n")
        print("당첨 번호 목록:")
        for w in data['winning_numbers']:
            print(f"  {w['draw_number']}회: {w['numbers']} + 보너스 {w['bonus_number']}")
    
    return data

def test_recommend_numbers():
    """번호 추천 테스트"""
    print_section("번호 추천 (AI 모드, 5세트)")
    response = requests.post(
        f"{BASE_URL}/api/recommend",
        json={
            "n_sets": 5,
            "mode": "ai"
        }
    )
    data = print_response(response)
    
    if data and data.get('sets'):
        print(f"\n✨ {data['last_draw']}회차 기준 추천 번호:")
        for i, s in enumerate(data['sets'], 1):
            print(f"  {i}번 세트: {s['numbers']}")
    
    return data

def test_saved_numbers_workflow():
    """저장된 번호 워크플로우 테스트 (인증 필요)"""
    if not TEST_TOKEN:
        print_section("저장된 번호 테스트 (건너뛰기 - 인증 필요)")
        print("⚠️  카카오 로그인 후 토큰이 필요합니다")
        return None
    
    print_section("저장된 번호 CRUD 테스트")
    headers = get_headers()
    
    # 1. 번호 저장
    print("\n1️⃣ 번호 저장")
    save_data = {
        "numbers": [7, 13, 21, 28, 35, 42],
        "nickname": "내 행운의 번호",
        "memo": "테스트용 번호",
        "is_favorite": True,
        "recommendation_type": "ai"
    }
    response = requests.post(
        f"{BASE_URL}/api/saved-numbers",
        headers=headers,
        json=save_data
    )
    saved = print_response(response)
    
    if not saved:
        return None
    
    saved_id = saved.get('id')
    
    # 2. 저장된 번호 조회
    print("\n2️⃣ 저장된 번호 목록 조회")
    response = requests.get(f"{BASE_URL}/api/saved-numbers", headers=headers)
    print_response(response)
    
    # 3. 번호 수정
    print("\n3️⃣ 번호 수정")
    update_data = {
        "numbers": [7, 13, 21, 28, 35, 43],  # 마지막 번호 변경
        "nickname": "수정된 행운의 번호",
        "memo": "번호 하나 변경",
        "is_favorite": True,
        "recommendation_type": "ai"
    }
    response = requests.put(
        f"{BASE_URL}/api/saved-numbers/{saved_id}",
        headers=headers,
        json=update_data
    )
    print_response(response)
    
    # 4. 번호 삭제
    print("\n4️⃣ 번호 삭제")
    response = requests.delete(
        f"{BASE_URL}/api/saved-numbers/{saved_id}",
        headers=headers
    )
    print_response(response)
    
    return True

def test_check_winning():
    """당첨 확인 테스트 (인증 필요)"""
    if not TEST_TOKEN:
        print_section("당첨 확인 테스트 (건너뛰기 - 인증 필요)")
        print("⚠️  카카오 로그인 후 토큰이 필요합니다")
        return None
    
    print_section("당첨 확인 테스트")
    headers = get_headers()
    
    # 최신 회차 번호 가져오기
    latest = requests.get(f"{BASE_URL}/api/winning-numbers/latest").json()
    draw_number = latest['draw_number']
    winning_numbers = latest['numbers']
    
    # 테스트 케이스 1: 3개 맞춤 (5등)
    print("\n1️⃣ 테스트: 3개 맞춤 (5등 예상)")
    test_numbers = winning_numbers[:3] + [1, 2, 3]
    check_data = {
        "numbers": test_numbers,
        "draw_number": draw_number
    }
    response = requests.post(
        f"{BASE_URL}/api/check-winning",
        headers=headers,
        json=check_data
    )
    result = print_response(response)
    
    if result:
        print(f"\n{result['message']}")
        print(f"맞춘 개수: {result['matched_count']}개")
        if result['rank']:
            print(f"등수: {result['rank']}등")
            if result['prize_amount']:
                print(f"당첨금: {result['prize_amount']:,}원")
    
    # 테스트 케이스 2: 완전 미당첨
    print("\n2️⃣ 테스트: 미당첨")
    test_numbers = [1, 2, 3, 4, 5, 6]
    check_data = {
        "numbers": test_numbers,
        "draw_number": draw_number
    }
    response = requests.post(
        f"{BASE_URL}/api/check-winning",
        headers=headers,
        json=check_data
    )
    result = print_response(response)
    
    if result:
        print(f"\n{result['message']}")
        print(f"맞춘 개수: {result['matched_count']}개")
    
    return True

def test_user_settings():
    """사용자 설정 테스트 (인증 필요)"""
    if not TEST_TOKEN:
        print_section("사용자 설정 테스트 (건너뛰기 - 인증 필요)")
        print("⚠️  카카오 로그인 후 토큰이 필요합니다")
        return None
    
    print_section("사용자 설정 테스트")
    headers = get_headers()
    
    # 1. 설정 조회
    print("\n1️⃣ 현재 설정 조회")
    response = requests.get(f"{BASE_URL}/api/settings", headers=headers)
    current_settings = print_response(response)
    
    # 2. 설정 업데이트
    print("\n2️⃣ 설정 업데이트")
    update_data = {
        "theme_mode": "dark",
        "lucky_numbers": [7, 13, 21, 28, 35],
        "exclude_numbers": [4, 14, 24],
        "enable_draw_notifications": True
    }
    response = requests.put(
        f"{BASE_URL}/api/settings",
        headers=headers,
        json=update_data
    )
    updated_settings = print_response(response)
    
    if updated_settings:
        print("\n✨ 설정 변경 사항:")
        print(f"  테마: {updated_settings['theme_mode']}")
        print(f"  행운의 번호: {updated_settings['lucky_numbers']}")
        print(f"  제외 번호: {updated_settings['exclude_numbers']}")
    
    return True

def main():
    """메인 테스트 실행"""
    print("="*60)
    print("로또 앱 통합 테스트 시작")
    print("="*60)
    
    results = {}
    
    # 인증 불필요한 테스트
    try:
        results['health'] = test_health_check()
        results['sync'] = test_sync_winning_numbers()
        results['latest_winning'] = test_get_latest_winning()
        results['winning_list'] = test_get_winning_list()
        results['recommend'] = test_recommend_numbers()
    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
    
    # 인증 필요한 테스트 (토큰 있을 때만)
    if TEST_TOKEN:
        try:
            results['saved_numbers'] = test_saved_numbers_workflow()
            results['check_winning'] = test_check_winning()
            results['user_settings'] = test_user_settings()
        except Exception as e:
            print(f"\n❌ 인증 테스트 오류: {e}")
    
    # 결과 요약
    print_section("테스트 결과 요약")
    print(f"헬스 체크: {'✅ 성공' if results.get('health') else '❌ 실패'}")
    print(f"당첨 번호 동기화: {'✅ 성공' if results.get('sync') else '❌ 실패'}")
    print(f"최신 당첨 번호 조회: {'✅ 성공' if results.get('latest_winning') else '❌ 실패'}")
    print(f"당첨 번호 목록: {'✅ 성공' if results.get('winning_list') else '❌ 실패'}")
    print(f"번호 추천: {'✅ 성공' if results.get('recommend') else '❌ 실패'}")
    
    if TEST_TOKEN:
        print(f"저장된 번호 CRUD: {'✅ 성공' if results.get('saved_numbers') else '❌ 실패'}")
        print(f"당첨 확인: {'✅ 성공' if results.get('check_winning') else '❌ 실패'}")
        print(f"사용자 설정: {'✅ 성공' if results.get('user_settings') else '❌ 실패'}")
    else:
        print("\n⚠️  인증 필요한 기능은 테스트하지 않았습니다")
        print("💡 카카오 로그인 후 TEST_TOKEN을 설정하면 전체 기능 테스트 가능")
    
    print("="*60)

if __name__ == "__main__":
    main()
