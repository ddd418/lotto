"""
구독 관리 모델
"""
from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime, timedelta
from database import Base


class UserSubscription(Base):
    """사용자 구독 정보"""
    __tablename__ = "user_subscriptions"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), unique=True, nullable=False)
    
    # 무료 체험 관리
    trial_start_date = Column(DateTime, nullable=True)
    trial_end_date = Column(DateTime, nullable=True)
    is_trial_used = Column(Boolean, default=False)
    
    # 구독 상태
    subscription_plan = Column(String(20), default="free")  # free, pro
    is_pro_subscriber = Column(Boolean, default=False)
    subscription_start_date = Column(DateTime, nullable=True)
    subscription_end_date = Column(DateTime, nullable=True)
    
    # Google Play 연동
    google_play_order_id = Column(String(100), nullable=True)
    google_play_purchase_token = Column(Text, nullable=True)
    google_play_product_id = Column(String(50), nullable=True)
    
    # 자동 갱신
    auto_renew = Column(Boolean, default=True)
    cancelled_at = Column(DateTime, nullable=True)
    
    # 메타데이터
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # 관계
    user = relationship("User", back_populates="subscription")
    
    @property
    def is_trial_active(self) -> bool:
        """무료 체험 활성 상태"""
        if not self.is_trial_used or not self.trial_end_date:
            return False
        return datetime.utcnow() < self.trial_end_date
    
    @property
    def trial_days_remaining(self) -> int:
        """남은 체험 기간 (일)"""
        if not self.is_trial_active:
            return 0
        delta = self.trial_end_date - datetime.utcnow()
        return max(0, delta.days)
    
    @property
    def has_access(self) -> bool:
        """앱 사용 권한 (PRO 또는 체험 중)"""
        return self.is_pro_subscriber or self.is_trial_active
    
    @property
    def should_show_trial_warning(self) -> bool:
        """체험 종료 경고 표시 여부 (3일 이내)"""
        return self.is_trial_active and self.trial_days_remaining <= 3
    
    def start_trial(self):
        """무료 체험 시작"""
        if self.is_trial_used:
            raise ValueError("이미 무료 체험을 사용했습니다.")
        
        self.trial_start_date = datetime.utcnow()
        self.trial_end_date = datetime.utcnow() + timedelta(days=30)
        self.is_trial_used = True
        self.subscription_plan = "trial"
        self.updated_at = datetime.utcnow()
    
    def activate_pro_subscription(
        self,
        order_id: str,
        purchase_token: str,
        product_id: str
    ):
        """PRO 구독 활성화"""
        self.is_pro_subscriber = True
        self.subscription_plan = "pro"
        self.subscription_start_date = datetime.utcnow()
        self.subscription_end_date = datetime.utcnow() + timedelta(days=30)  # 월 구독
        self.google_play_order_id = order_id
        self.google_play_purchase_token = purchase_token
        self.google_play_product_id = product_id
        self.auto_renew = True
        self.cancelled_at = None
        self.updated_at = datetime.utcnow()
    
    def cancel_subscription(self):
        """구독 취소 (현재 기간 종료까지는 사용 가능)"""
        self.auto_renew = False
        self.cancelled_at = datetime.utcnow()
        self.updated_at = datetime.utcnow()
    
    def expire_subscription(self):
        """구독 만료 처리"""
        self.is_pro_subscriber = False
        self.subscription_plan = "free"
        self.subscription_end_date = None
        self.updated_at = datetime.utcnow()
    
    def to_dict(self):
        """딕셔너리 변환"""
        return {
            "user_id": self.user_id,
            "subscription_plan": self.subscription_plan,
            "is_pro_subscriber": self.is_pro_subscriber,
            "has_access": self.has_access,
            "trial_info": {
                "is_used": self.is_trial_used,
                "is_active": self.is_trial_active,
                "start_date": self.trial_start_date.isoformat() if self.trial_start_date else None,
                "end_date": self.trial_end_date.isoformat() if self.trial_end_date else None,
                "days_remaining": self.trial_days_remaining,
                "show_warning": self.should_show_trial_warning
            },
            "pro_info": {
                "start_date": self.subscription_start_date.isoformat() if self.subscription_start_date else None,
                "end_date": self.subscription_end_date.isoformat() if self.subscription_end_date else None,
                "auto_renew": self.auto_renew,
                "cancelled_at": self.cancelled_at.isoformat() if self.cancelled_at else None
            }
        }
