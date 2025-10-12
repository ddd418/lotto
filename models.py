"""
데이터베이스 모델 정의
"""
from sqlalchemy import Column, Integer, BigInteger, String, DateTime, Boolean, Text, ForeignKey, JSON
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import Base

class User(Base):
    """
    사용자 모델
    """
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    kakao_id = Column(String(50), unique=True, index=True, nullable=False)  # 카카오 고유 ID
    email = Column(String(100), unique=True, index=True)
    nickname = Column(String(50), nullable=False)
    profile_image = Column(String(255))  # 프로필 이미지 URL
    
    # 메타데이터
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    last_login_at = Column(DateTime(timezone=True))
    is_active = Column(Boolean, default=True)
    
    # 관계
    saved_numbers = relationship("SavedNumber", back_populates="user", cascade="all, delete-orphan")
    winning_checks = relationship("WinningCheck", back_populates="user", cascade="all, delete-orphan")

class WinningNumber(Base):
    """
    로또 당첨 번호 (공식 당첨 번호 저장)
    """
    __tablename__ = "winning_numbers"
    
    id = Column(Integer, primary_key=True, index=True)
    draw_number = Column(Integer, unique=True, nullable=False, index=True)  # 회차
    
    # 당첨 번호 6개
    number1 = Column(Integer, nullable=False)
    number2 = Column(Integer, nullable=False)
    number3 = Column(Integer, nullable=False)
    number4 = Column(Integer, nullable=False)
    number5 = Column(Integer, nullable=False)
    number6 = Column(Integer, nullable=False)
    bonus_number = Column(Integer, nullable=False)  # 보너스 번호
    
    # 당첨 정보 (BigInteger: PostgreSQL에서 큰 숫자 지원)
    prize_1st = Column(BigInteger)  # 1등 당첨금
    prize_2nd = Column(BigInteger)  # 2등 당첨금
    prize_3rd = Column(BigInteger)  # 3등 당첨금
    prize_4th = Column(BigInteger)  # 4등 당첨금
    prize_5th = Column(BigInteger)  # 5등 당첨금
    
    winners_1st = Column(Integer)  # 1등 당첨자 수
    winners_2nd = Column(Integer)  # 2등 당첨자 수
    winners_3rd = Column(Integer)  # 3등 당첨자 수
    winners_4th = Column(Integer)  # 4등 당첨자 수
    winners_5th = Column(Integer)  # 5등 당첨자 수
    
    total_sales = Column(BigInteger)  # 총 판매액
    draw_date = Column(DateTime(timezone=True))  # 추첨일
    
    # 메타데이터
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

class SavedNumber(Base):
    """
    사용자가 저장한 로또 번호
    """
    __tablename__ = "saved_numbers"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    
    # 로또 번호 (1-45)
    number1 = Column(Integer, nullable=False)
    number2 = Column(Integer, nullable=False)
    number3 = Column(Integer, nullable=False)
    number4 = Column(Integer, nullable=False)
    number5 = Column(Integer, nullable=False)
    number6 = Column(Integer, nullable=False)
    
    # 메타데이터
    nickname = Column(String(50))  # 번호 별칭
    memo = Column(Text)  # 메모
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    is_favorite = Column(Boolean, default=False)  # 즐겨찾기
    recommendation_type = Column(String(20))  # 추천 유형 (hot, cold, balanced, random)
    
    # 관계
    user = relationship("User", back_populates="saved_numbers")

class WinningCheck(Base):
    """
    당첨 확인 기록
    """
    __tablename__ = "winning_checks"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    
    # 확인한 번호
    numbers = Column(JSON, nullable=False)  # [1, 2, 3, 4, 5, 6] 형태
    draw_number = Column(Integer, nullable=False)  # 몇 회차인지
    
    # 당첨 결과
    rank = Column(Integer)  # 1등, 2등, 3등, 4등, 5등, None(미당첨)
    prize_amount = Column(Integer)  # 당첨 금액
    matched_count = Column(Integer, nullable=False)  # 맞은 개수
    has_bonus = Column(Boolean, default=False)  # 보너스 번호 포함 여부
    
    # 메타데이터
    checked_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # 관계
    user = relationship("User", back_populates="winning_checks")

class UserSettings(Base):
    """
    사용자 설정
    """
    __tablename__ = "user_settings"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), unique=True, nullable=False)
    
    # 앱 설정
    theme_mode = Column(String(10), default="light")  # light, dark, system (기본값: light)
    default_recommendation_type = Column(String(20), default="balanced")
    
    # 알림 설정
    enable_push_notifications = Column(Boolean, default=False)  # 푸시 알림 설정
    enable_draw_notifications = Column(Boolean, default=False)  # 추첨일 알림
    enable_winning_notifications = Column(Boolean, default=False)  # 당첨 확인 알림
    
    # 개인화 설정
    lucky_numbers = Column(JSON)  # 행운의 번호들 [7, 13, 21, ...]
    exclude_numbers = Column(JSON)  # 제외할 번호들
    
    # 메타데이터
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

class AppStats(Base):
    """
    앱 사용 통계 (선택사항)
    """
    __tablename__ = "app_stats"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=True)  # 익명 통계 가능
    
    # 행동 통계
    action_type = Column(String(50), nullable=False)  # recommend, save, check_winning, etc.
    details = Column(JSON)  # 추가 상세 정보
    
    # 메타데이터
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    ip_address = Column(String(45))  # IPv6까지 지원
    user_agent = Column(Text)