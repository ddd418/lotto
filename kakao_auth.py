"""
카카오 OAuth2 인증 처리
"""
import httpx
from typing import Optional, Dict, Any
from fastapi import HTTPException, status
import os
from dotenv import load_dotenv

load_dotenv()

# 카카오 OAuth 설정
KAKAO_CLIENT_ID = os.getenv("KAKAO_CLIENT_ID", "your-kakao-app-key")
KAKAO_CLIENT_SECRET = os.getenv("KAKAO_CLIENT_SECRET", "your-kakao-secret")
KAKAO_REDIRECT_URI = os.getenv("KAKAO_REDIRECT_URI", "http://localhost:8000/auth/kakao/callback")

# 카카오 API 엔드포인트
KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token"
KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me"

class KakaoAuth:
    """
    카카오 OAuth2 인증 처리 클래스
    """
    
    @staticmethod
    async def get_access_token_from_code(authorization_code: str) -> Dict[str, Any]:
        """
        인증 코드로 액세스 토큰 가져오기 (웹 로그인용)
        """
        async with httpx.AsyncClient() as client:
            data = {
                "grant_type": "authorization_code",
                "client_id": KAKAO_CLIENT_ID,
                "client_secret": KAKAO_CLIENT_SECRET,
                "redirect_uri": KAKAO_REDIRECT_URI,
                "code": authorization_code,
            }
            
            response = await client.post(KAKAO_TOKEN_URL, data=data)
            
            if response.status_code != 200:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="카카오 토큰 요청에 실패했습니다"
                )
            
            return response.json()
    
    @staticmethod
    async def validate_access_token(access_token: str) -> bool:
        """
        액세스 토큰 유효성 검증 (모바일 앱용)
        카카오 SDK에서 직접 받은 액세스 토큰을 검증
        """
        try:
            # 토큰으로 사용자 정보를 가져올 수 있으면 유효한 토큰
            await KakaoAuth.get_user_info(access_token)
            return True
        except:
            return False
    
    @staticmethod
    async def get_user_info(access_token: str) -> Dict[str, Any]:
        """
        액세스 토큰으로 사용자 정보 가져오기
        """
        async with httpx.AsyncClient() as client:
            headers = {
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/x-www-form-urlencoded;charset=utf-8"
            }
            
            response = await client.get(KAKAO_USER_INFO_URL, headers=headers)
            
            if response.status_code != 200:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="카카오 사용자 정보 요청에 실패했습니다"
                )
            
            return response.json()
    
    @staticmethod
    def extract_user_data(kakao_user_info: Dict[str, Any]) -> Dict[str, Any]:
        """
        카카오 사용자 정보에서 필요한 데이터 추출
        """
        import json
        
        # 디버깅용 로그 추가
        print("=" * 80)
        print("🔍 카카오 사용자 정보 원본:")
        print(json.dumps(kakao_user_info, indent=2, ensure_ascii=False))
        print("=" * 80)
        
        kakao_account = kakao_user_info.get("kakao_account", {})
        profile = kakao_account.get("profile", {})
        
        print(f"📋 kakao_account 존재 여부: {bool(kakao_account)}")
        print(f"📋 profile 존재 여부: {bool(profile)}")
        print(f"📋 profile 내용: {profile}")
        
        extracted_data = {
            "kakao_id": str(kakao_user_info.get("id")),
            "email": kakao_account.get("email"),
            "nickname": profile.get("nickname", "카카오사용자"),
            "profile_image": profile.get("profile_image_url"),
        }
        
        print(f"✅ 추출된 사용자 데이터:")
        print(json.dumps(extracted_data, indent=2, ensure_ascii=False))
        print("=" * 80)
        
        return extracted_data
    
    @staticmethod
    def get_authorization_url() -> str:
        """
        카카오 인증 URL 생성 (Android에서 사용)
        """
        base_url = "https://kauth.kakao.com/oauth/authorize"
        params = {
            "client_id": KAKAO_CLIENT_ID,
            "redirect_uri": KAKAO_REDIRECT_URI,
            "response_type": "code",
            "scope": "profile_nickname,profile_image,account_email"
        }
        
        query_string = "&".join([f"{k}={v}" for k, v in params.items()])
        return f"{base_url}?{query_string}"