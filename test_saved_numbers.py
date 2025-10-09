"""
사용자 및 저장 번호 테스트 스크립트
1. 카카오 로그인 (테스트용 Mock)
2. 로또 번호 저장
3. 저장한 번호 조회
4. 번호 수정
5. 번호 삭제
"""
import requests
import json
from datetime import datetime

BASE_URL = "http://localhost:8000"

# 테스트용 가짜 카카오 토큰 (실제로는 카카오 로그인 필요)
# 실제 환경에서는 카카오 로그인 후 받은 code를 사용해야 함

class LottoAPITester:
    def __init__(self):
        self.access_token = None
        self.user_info = None
        
    def print_section(self, title):
        """섹션 구분선 출력"""
        print("\n" + "=" * 70)
        print(f"📌 {title}")
        print("=" * 70)
    
    def print_result(self, response):
        """API 응답 출력"""
        print(f"\n✅ 상태 코드: {response.status_code}")
        try:
            result = response.json()
            print(f"📄 응답 데이터:")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            return result
        except:
            print(f"📄 응답: {response.text}")
            return None
    
    def test_health_check(self):
        """서버 상태 확인"""
        self.print_section("서버 상태 확인")
        response = requests.get(f"{BASE_URL}/api/health")
        result = self.print_result(response)
        return response.status_code == 200
    
    def test_kakao_login(self, code="test_code_12345"):
        """카카오 로그인 테스트"""
        self.print_section("카카오 로그인")
        print(f"📱 카카오 인증 코드: {code}")
        print("⚠️  실제로는 카카오 로그인 페이지에서 받은 code를 사용해야 합니다")
        print("⚠️  현재는 테스트용 코드를 사용합니다 (실제로는 실패할 수 있음)")
        
        response = requests.post(
            f"{BASE_URL}/auth/kakao/login",
            json={"code": code}
        )
        
        result = self.print_result(response)
        
        if response.status_code == 200 and result:
            self.access_token = result.get("access_token")
            print(f"\n🔑 Access Token: {self.access_token[:50]}...")
            return True
        else:
            print(f"\n❌ 로그인 실패 - 아래 '토큰 직접 생성' 섹션을 사용하세요")
            return False
    
    def create_test_token_manually(self):
        """테스트용 토큰 수동 생성"""
        self.print_section("테스트용 JWT 토큰 생성")
        print("카카오 로그인이 실패했으므로 테스트용 토큰을 생성합니다.")
        print("⚠️  이 토큰은 실제 환경에서는 사용할 수 없습니다.")
        
        # JWT 토큰 생성 (실제로는 서버의 TokenManager를 사용)
        from auth import TokenManager
        from models import User
        from database import SessionLocal
        
        db = SessionLocal()
        
        # 테스트 유저 찾기 또는 생성
        test_user = db.query(User).filter(User.kakao_id == "test_kakao_id_001").first()
        
        if not test_user:
            print("\n👤 테스트 유저 생성 중...")
            test_user = User(
                kakao_id="test_kakao_id_001",
                nickname="테스트유저",
                email="test@example.com"
            )
            db.add(test_user)
            db.commit()
            db.refresh(test_user)
            print(f"✅ 테스트 유저 생성 완료: {test_user.nickname} (ID: {test_user.id})")
        else:
            print(f"\n✅ 기존 테스트 유저 사용: {test_user.nickname} (ID: {test_user.id})")
        
        # JWT 토큰 생성
        token_manager = TokenManager()
        access_token = token_manager.create_access_token(data={"sub": str(test_user.id)})
        refresh_token = token_manager.create_refresh_token(data={"sub": str(test_user.id)})
        
        self.access_token = access_token
        self.user_info = {
            "id": test_user.id,
            "kakao_id": test_user.kakao_id,
            "nickname": test_user.nickname,
            "email": test_user.email
        }
        
        print(f"\n🔑 Access Token 생성 완료")
        print(f"   - User ID: {test_user.id}")
        print(f"   - Nickname: {test_user.nickname}")
        print(f"   - Token: {self.access_token[:50]}...")
        
        db.close()
        return True
    
    def get_auth_headers(self):
        """인증 헤더 생성"""
        if not self.access_token:
            raise Exception("로그인이 필요합니다!")
        return {"Authorization": f"Bearer {self.access_token}"}
    
    def test_save_number(self, numbers, nickname="테스트번호", memo=""):
        """로또 번호 저장"""
        self.print_section(f"로또 번호 저장: {nickname}")
        
        response = requests.post(
            f"{BASE_URL}/api/saved-numbers",
            headers=self.get_auth_headers(),
            json={
                "numbers": numbers,
                "nickname": nickname,
                "memo": memo
            }
        )
        
        result = self.print_result(response)
        
        if response.status_code == 200 and result:
            saved_id = result.get("id")
            print(f"\n💾 번호 저장 완료! (ID: {saved_id})")
            print(f"   - 번호: {result.get('numbers')}")
            print(f"   - 별칭: {result.get('nickname')}")
            return saved_id
        else:
            print(f"\n❌ 번호 저장 실패")
            return None
    
    def test_get_saved_numbers(self):
        """저장한 번호 목록 조회"""
        self.print_section("저장한 번호 목록 조회")
        
        response = requests.get(
            f"{BASE_URL}/api/saved-numbers",
            headers=self.get_auth_headers()
        )
        
        result = self.print_result(response)
        
        if response.status_code == 200 and result:
            # 응답이 리스트로 직접 반환됨
            numbers_list = result if isinstance(result, list) else result.get("numbers", [])
            print(f"\n📋 총 {len(numbers_list)}개의 저장된 번호:")
            for num in numbers_list:
                print(f"\n  [ID: {num['id']}]")
                print(f"  번호: {num['numbers']}")
                if num.get('nickname'):
                    print(f"  이름: {num['nickname']}")
                if num.get('memo'):
                    print(f"  메모: {num['memo']}")
                print(f"  생성: {num['created_at'][:19]}")
            return numbers_list
        else:
            print(f"\n❌ 조회 실패")
            return []
    
    def test_update_number(self, number_id, new_numbers=None, new_nickname=None, new_memo=None):
        """저장한 번호 수정"""
        self.print_section(f"번호 수정 (ID: {number_id})")
        
        update_data = {}
        if new_numbers:
            update_data["numbers"] = new_numbers
        if new_nickname:
            update_data["nickname"] = new_nickname
        if new_memo is not None:
            update_data["memo"] = new_memo
        
        response = requests.put(
            f"{BASE_URL}/api/saved-numbers/{number_id}",
            headers=self.get_auth_headers(),
            json=update_data
        )
        
        result = self.print_result(response)
        
        if response.status_code == 200 and result:
            print(f"\n✅ 번호 수정 완료!")
            print(f"   - 번호: {result.get('numbers')}")
            print(f"   - 별칭: {result.get('nickname')}")
            return True
        else:
            print(f"\n❌ 수정 실패")
            return False
    
    def test_delete_number(self, number_id):
        """저장한 번호 삭제"""
        self.print_section(f"번호 삭제 (ID: {number_id})")
        
        response = requests.delete(
            f"{BASE_URL}/api/saved-numbers/{number_id}",
            headers=self.get_auth_headers()
        )
        
        result = self.print_result(response)
        
        if response.status_code == 200:
            print(f"\n✅ 번호 삭제 완료!")
            return True
        else:
            print(f"\n❌ 삭제 실패")
            return False

def main():
    print("\n" + "🎰" * 35)
    print("🎲 로또 번호 저장 기능 테스트")
    print("🎰" * 35)
    
    tester = LottoAPITester()
    
    # 1. 서버 상태 확인
    if not tester.test_health_check():
        print("\n❌ 서버가 실행 중이지 않습니다!")
        print("💡 'python api_server.py'로 서버를 먼저 실행해주세요!")
        return
    
    # 2. 로그인 (카카오 로그인은 실패할 수 있으므로 수동 토큰 생성)
    print("\n⚠️  카카오 로그인은 실제 인증 코드가 필요하므로")
    print("⚠️  테스트용 JWT 토큰을 직접 생성합니다.")
    
    if not tester.create_test_token_manually():
        print("\n❌ 토큰 생성 실패!")
        return
    
    # 3. 로또 번호 저장 (여러 개)
    print("\n")
    saved_ids = []
    
    id1 = tester.test_save_number(
        numbers=[1, 7, 14, 21, 28, 35],
        nickname="행운의 7배수",
        memo="7의 배수로 구성된 번호"
    )
    if id1:
        saved_ids.append(id1)
    
    id2 = tester.test_save_number(
        numbers=[3, 9, 15, 21, 27, 33],
        nickname="3의 배수 조합",
        memo="3의 배수 번호들"
    )
    if id2:
        saved_ids.append(id2)
    
    id3 = tester.test_save_number(
        numbers=[2, 11, 19, 26, 34, 42],
        nickname="랜덤 추천",
        memo="AI 추천 번호"
    )
    if id3:
        saved_ids.append(id3)
    
    # 4. 저장한 번호 목록 조회
    import time
    time.sleep(1)
    numbers_list = tester.test_get_saved_numbers()
    
    # 5. 번호 수정
    if saved_ids:
        time.sleep(1)
        tester.test_update_number(
            number_id=saved_ids[0],
            new_nickname="수정된 행운의 번호",
            new_memo="이번 주 1등 예감!"
        )
    
    # 6. 수정 후 다시 조회
    time.sleep(1)
    tester.test_get_saved_numbers()
    
    # 7. 번호 삭제
    if len(saved_ids) >= 2:
        time.sleep(1)
        tester.test_delete_number(saved_ids[1])
    
    # 8. 삭제 후 최종 조회
    time.sleep(1)
    final_list = tester.test_get_saved_numbers()
    
    # 결과 요약
    print("\n" + "=" * 70)
    print("📊 테스트 결과 요약")
    print("=" * 70)
    print(f"\n  ✅ 총 {len(saved_ids)}개 번호 저장 테스트")
    print(f"  ✅ 1개 번호 수정 테스트")
    print(f"  ✅ 1개 번호 삭제 테스트")
    print(f"  📋 최종 저장된 번호: {len(final_list)}개")
    
    print("\n🎉 모든 테스트 완료!")
    print("\n" + "🎰" * 35)

if __name__ == "__main__":
    main()
