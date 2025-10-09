"""
JWT 토큰 인증 시스템
"""
from datetime import datetime, timedelta
from typing import Optional, Union
from jose import JWTError, jwt
from passlib.context import CryptContext
from fastapi import HTTPException, status
import os
from dotenv import load_dotenv

load_dotenv()

# JWT 설정
SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-change-this-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30
REFRESH_TOKEN_EXPIRE_DAYS = 7

# 패스워드 해싱 (카카오 로그인에서는 직접 사용 안함)
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

class TokenManager:
    """
    JWT 토큰 관리 클래스
    """
    
    @staticmethod
    def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
        """
        액세스 토큰 생성
        """
        to_encode = data.copy()
        if expires_delta:
            expire = datetime.utcnow() + expires_delta
        else:
            expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
        
        to_encode.update({"exp": expire})
        encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
        return encoded_jwt
    
    @staticmethod
    def create_refresh_token(data: dict):
        """
        리프레시 토큰 생성
        """
        to_encode = data.copy()
        expire = datetime.utcnow() + timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS)
        to_encode.update({"exp": expire})
        encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
        return encoded_jwt
    
    @staticmethod
    def verify_token(token: str) -> dict:
        """
        토큰 검증 및 페이로드 반환
        """
        try:
            payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
            return payload
        except JWTError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="토큰이 유효하지 않습니다",
                headers={"WWW-Authenticate": "Bearer"},
            )
    
    @staticmethod
    def get_user_id_from_token(token: str) -> int:
        """
        토큰에서 사용자 ID 추출
        """
        payload = TokenManager.verify_token(token)
        user_id = payload.get("sub")
        if user_id is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="토큰에서 사용자 정보를 찾을 수 없습니다",
                headers={"WWW-Authenticate": "Bearer"},
            )
        # sub는 문자열로 저장되므로 정수로 변환
        return int(user_id)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """
    비밀번호 검증 (카카오 로그인에서는 사용 안함)
    """
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password: str) -> str:
    """
    비밀번호 해싱 (카카오 로그인에서는 사용 안함)
    """
    return pwd_context.hash(password)