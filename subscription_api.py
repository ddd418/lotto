"""
구독 관리 API 엔드포인트
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from datetime import datetime, timedelta
from typing import Optional
from pydantic import BaseModel

from database import get_db
from models import User, UserSubscription
from auth import get_current_user

router = APIRouter(prefix="/api/subscription", tags=["subscription"])


# ==================== Request/Response 모델 ====================

class TrialStartRequest(BaseModel):
    """체험 시작 요청"""
    pass

class SubscriptionStatusResponse(BaseModel):
    """구독 상태 응답"""
    is_pro: bool
    trial_active: bool
    trial_days_remaining: int
    subscription_plan: str
    has_access: bool
    trial_start_date: Optional[datetime] = None
    trial_end_date: Optional[datetime] = None
    subscription_end_date: Optional[datetime] = None
    auto_renew: bool

class VerifyPurchaseRequest(BaseModel):
    """Google Play 구매 검증 요청"""
    purchase_token: str
    order_id: str
    product_id: str

class VerifyPurchaseResponse(BaseModel):
    """구매 검증 응답"""
    verified: bool
    is_pro: bool
    subscription_end_date: Optional[datetime] = None
    message: str


# ==================== Helper 함수 ====================

def get_or_create_subscription(db: Session, user: User) -> UserSubscription:
    """구독 정보 조회 또는 생성"""
    subscription = db.query(UserSubscription).filter(
        UserSubscription.user_id == user.id
    ).first()
    
    if not subscription:
        subscription = UserSubscription(
            user_id=user.id,
            subscription_plan="free",
            is_pro_subscriber=False,
            is_trial_used=False
        )
        db.add(subscription)
        db.commit()
        db.refresh(subscription)
    
    return subscription


def calculate_trial_days_remaining(subscription: UserSubscription) -> int:
    """남은 체험 기간 계산"""
    if not subscription.trial_start_date or not subscription.trial_end_date:
        return 0
    
    now = datetime.now()
    if now > subscription.trial_end_date:
        return 0
    
    remaining = (subscription.trial_end_date - now).days
    return max(0, remaining)


def is_subscription_valid(subscription: UserSubscription) -> bool:
    """구독이 유효한지 확인"""
    if not subscription.is_pro_subscriber:
        return False
    
    if not subscription.subscription_end_date:
        return False
    
    now = datetime.now()
    return now < subscription.subscription_end_date


# ==================== API 엔드포인트 ====================

@router.post("/start-trial", response_model=SubscriptionStatusResponse)
async def start_trial(
    request: TrialStartRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    무료 체험 시작
    
    - 사용자당 1회만 가능
    - 30일 무료 체험 제공
    """
    subscription = get_or_create_subscription(db, current_user)
    
    # 이미 체험을 사용한 경우
    if subscription.is_trial_used:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미 무료 체험을 사용하셨습니다."
        )
    
    # 이미 PRO 구독자인 경우
    if subscription.is_pro_subscriber:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미 PRO 구독 중입니다."
        )
    
    # 체험 시작
    now = datetime.now()
    subscription.trial_start_date = now
    subscription.trial_end_date = now + timedelta(days=30)
    subscription.is_trial_used = True
    subscription.updated_at = now
    
    db.commit()
    db.refresh(subscription)
    
    # 응답 생성
    trial_days_remaining = calculate_trial_days_remaining(subscription)
    trial_active = trial_days_remaining > 0
    
    return SubscriptionStatusResponse(
        is_pro=subscription.is_pro_subscriber,
        trial_active=trial_active,
        trial_days_remaining=trial_days_remaining,
        subscription_plan=subscription.subscription_plan,
        has_access=subscription.is_pro_subscriber or trial_active,
        trial_start_date=subscription.trial_start_date,
        trial_end_date=subscription.trial_end_date,
        subscription_end_date=subscription.subscription_end_date,
        auto_renew=subscription.auto_renew
    )


@router.get("/status", response_model=SubscriptionStatusResponse)
async def get_subscription_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    현재 구독 상태 조회
    
    - PRO 구독 여부
    - 체험 기간 남은 일수
    - 접근 권한 여부
    """
    subscription = get_or_create_subscription(db, current_user)
    
    # 구독 만료 확인
    if subscription.is_pro_subscriber:
        if not is_subscription_valid(subscription):
            # 만료된 구독
            subscription.is_pro_subscriber = False
            subscription.subscription_plan = "free"
            db.commit()
            db.refresh(subscription)
    
    # 체험 기간 계산
    trial_days_remaining = calculate_trial_days_remaining(subscription)
    trial_active = trial_days_remaining > 0 and subscription.is_trial_used
    
    return SubscriptionStatusResponse(
        is_pro=subscription.is_pro_subscriber,
        trial_active=trial_active,
        trial_days_remaining=trial_days_remaining,
        subscription_plan=subscription.subscription_plan,
        has_access=subscription.is_pro_subscriber or trial_active,
        trial_start_date=subscription.trial_start_date,
        trial_end_date=subscription.trial_end_date,
        subscription_end_date=subscription.subscription_end_date,
        auto_renew=subscription.auto_renew
    )


@router.post("/verify-purchase", response_model=VerifyPurchaseResponse)
async def verify_purchase(
    request: VerifyPurchaseRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Google Play 구매 검증 및 PRO 구독 활성화
    
    - Google Play에서 구매한 구독을 검증
    - 검증 성공 시 PRO 구독 활성화
    """
    subscription = get_or_create_subscription(db, current_user)
    
    # TODO: 실제 Google Play API로 구매 검증
    # 현재는 간단히 구매 토큰 존재 여부만 확인
    # 프로덕션에서는 Google Play Developer API 사용 필요
    
    if not request.purchase_token or not request.order_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="유효하지 않은 구매 정보입니다."
        )
    
    # 이미 같은 주문 ID가 등록되어 있는지 확인
    existing = db.query(UserSubscription).filter(
        UserSubscription.google_play_order_id == request.order_id,
        UserSubscription.user_id != current_user.id
    ).first()
    
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미 등록된 구매입니다."
        )
    
    # PRO 구독 활성화
    now = datetime.now()
    subscription.is_pro_subscriber = True
    subscription.subscription_plan = "pro"
    subscription.subscription_start_date = now
    subscription.subscription_end_date = now + timedelta(days=30)  # 월 구독
    subscription.google_play_order_id = request.order_id
    subscription.google_play_purchase_token = request.purchase_token
    subscription.google_play_product_id = request.product_id
    subscription.auto_renew = True
    subscription.updated_at = now
    
    db.commit()
    db.refresh(subscription)
    
    return VerifyPurchaseResponse(
        verified=True,
        is_pro=True,
        subscription_end_date=subscription.subscription_end_date,
        message="PRO 구독이 활성화되었습니다."
    )


@router.post("/cancel")
async def cancel_subscription(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    구독 취소
    
    - 자동 갱신 비활성화
    - 현재 구독 기간까지는 PRO 사용 가능
    """
    subscription = get_or_create_subscription(db, current_user)
    
    if not subscription.is_pro_subscriber:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="활성화된 구독이 없습니다."
        )
    
    # 자동 갱신 비활성화
    subscription.auto_renew = False
    subscription.cancelled_at = datetime.now()
    subscription.updated_at = datetime.now()
    
    db.commit()
    
    return {
        "success": True,
        "message": "구독이 취소되었습니다. 현재 구독 기간까지는 PRO 기능을 사용하실 수 있습니다.",
        "subscription_end_date": subscription.subscription_end_date
    }


# ==================== 관리자 API ====================

@router.get("/admin/expiring-trials")
async def get_expiring_trials(
    days: int = 3,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    만료 임박 체험 사용자 조회 (관리자용)
    
    - 지정된 일수 이내 만료 예정인 체험 사용자 리스트
    """
    # TODO: 관리자 권한 체크 추가
    
    now = datetime.now()
    target_date = now + timedelta(days=days)
    
    expiring_subscriptions = db.query(UserSubscription, User).join(
        User, UserSubscription.user_id == User.id
    ).filter(
        UserSubscription.is_trial_used == True,
        UserSubscription.is_pro_subscriber == False,
        UserSubscription.trial_end_date <= target_date,
        UserSubscription.trial_end_date > now
    ).all()
    
    result = []
    for subscription, user in expiring_subscriptions:
        remaining_days = calculate_trial_days_remaining(subscription)
        result.append({
            "user_id": user.id,
            "email": user.email,
            "nickname": user.nickname,
            "trial_end_date": subscription.trial_end_date,
            "days_remaining": remaining_days
        })
    
    return {
        "total": len(result),
        "users": result
    }


@router.get("/admin/stats")
async def get_subscription_stats(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    구독 통계 조회 (관리자용)
    
    - 전체 사용자 수
    - PRO 구독자 수
    - 체험 중 사용자 수
    - 전환율 등
    """
    # TODO: 관리자 권한 체크 추가
    
    total_users = db.query(User).count()
    
    pro_subscribers = db.query(UserSubscription).filter(
        UserSubscription.is_pro_subscriber == True
    ).count()
    
    trial_users = db.query(UserSubscription).filter(
        UserSubscription.is_trial_used == True,
        UserSubscription.is_pro_subscriber == False,
        UserSubscription.trial_end_date > datetime.now()
    ).count()
    
    trial_used = db.query(UserSubscription).filter(
        UserSubscription.is_trial_used == True
    ).count()
    
    conversion_rate = (pro_subscribers / trial_used * 100) if trial_used > 0 else 0
    
    return {
        "total_users": total_users,
        "pro_subscribers": pro_subscribers,
        "active_trial_users": trial_users,
        "total_trial_used": trial_used,
        "conversion_rate": round(conversion_rate, 2),
        "revenue_estimate_monthly": pro_subscribers * 1900  # ₩1,900 × 구독자 수
    }
