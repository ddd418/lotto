"""
구독 관리 API 엔드포인트
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from datetime import datetime, timedelta, timezone
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
    
    class Config:
        from_attributes = True  # Pydantic v2
        json_encoders = {
            datetime: lambda v: v.isoformat() if v else None
        }

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

def get_or_create_subscription(db: Session, user_id: int) -> UserSubscription:
    """구독 정보 조회 또는 생성"""
    # 사용자 존재 여부 확인
    from models import User
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        print(f"❌ 사용자 없음: user_id={user_id}")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"사용자를 찾을 수 없습니다 (user_id: {user_id})"
        )
    
    print(f"✅ 사용자 확인: user_id={user_id}, nickname={user.nickname}")
    
    subscription = db.query(UserSubscription).filter(
        UserSubscription.user_id == user_id
    ).first()
    
    if not subscription:
        print(f"📝 새 구독 정보 생성: user_id={user_id}")
        subscription = UserSubscription(
            user_id=user_id,
            subscription_plan="free",
            is_pro_subscriber=False,
            is_trial_used=False
        )
        db.add(subscription)
        db.commit()
        db.refresh(subscription)
        print(f"✅ 구독 정보 생성 완료: subscription_id={subscription.id}")
    else:
        print(f"✅ 기존 구독 정보 사용: subscription_id={subscription.id}")
    
    return subscription


def calculate_trial_days_remaining(subscription: UserSubscription) -> int:
    """남은 체험 기간 계산"""
    if not subscription.trial_start_date or not subscription.trial_end_date:
        return 0
    
    now = datetime.now(timezone.utc)
    # trial_end_date가 naive datetime이면 UTC로 간주
    trial_end = subscription.trial_end_date
    if trial_end.tzinfo is None:
        trial_end = trial_end.replace(tzinfo=timezone.utc)
    
    if now > trial_end:
        return 0
    
    remaining = (trial_end - now).days
    return max(0, remaining)


def is_subscription_valid(subscription: UserSubscription) -> bool:
    """구독이 유효한지 확인"""
    if not subscription.is_pro_subscriber:
        return False
    
    if not subscription.subscription_end_date:
        return False
    
    now = datetime.now(timezone.utc)
    subscription_end = subscription.subscription_end_date
    if subscription_end.tzinfo is None:
        subscription_end = subscription_end.replace(tzinfo=timezone.utc)
    return now < subscription_end


# ==================== API 엔드포인트 ====================

@router.post("/start-trial", response_model=SubscriptionStatusResponse)
async def start_trial(
    user_id: int = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    무료 체험 시작
    
    - 사용자당 1회만 가능
    - 30일 무료 체험 제공
    """
    print(f"🎯 무료 체험 시작 요청 받음!")
    print(f"   user_id: {user_id}")
    
    subscription = get_or_create_subscription(db, user_id)
    
    print(f"📊 현재 구독 상태:")
    print(f"   is_trial_used: {subscription.is_trial_used}")
    print(f"   is_pro_subscriber: {subscription.is_pro_subscriber}")
    
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
    now = datetime.now(timezone.utc)
    subscription.trial_start_date = now
    subscription.trial_end_date = now + timedelta(days=30)
    subscription.is_trial_used = True
    subscription.subscription_plan = "free_trial"
    subscription.updated_at = now
    
    print(f"✅ 무료 체험 시작:")
    print(f"   trial_start_date: {subscription.trial_start_date}")
    print(f"   trial_end_date: {subscription.trial_end_date}")
    print(f"   is_trial_used: {subscription.is_trial_used}")
    print(f"   subscription_plan: {subscription.subscription_plan}")
    
    db.commit()
    db.refresh(subscription)
    
    # 응답 생성
    try:
        print("📦 응답 생성 시작...")
        trial_days_remaining = calculate_trial_days_remaining(subscription)
        print(f"   trial_days_remaining: {trial_days_remaining}")
        trial_active = trial_days_remaining > 0
        print(f"   trial_active: {trial_active}")
        
        print("📝 SubscriptionStatusResponse 생성 중...")
        response = SubscriptionStatusResponse(
            is_pro=subscription.is_pro_subscriber,
            trial_active=trial_active,
            trial_days_remaining=trial_days_remaining,
            subscription_plan=subscription.subscription_plan or "free",
            has_access=subscription.is_pro_subscriber or trial_active,
            trial_start_date=subscription.trial_start_date,
            trial_end_date=subscription.trial_end_date,
            subscription_end_date=subscription.subscription_end_date,
            auto_renew=subscription.auto_renew if subscription.auto_renew is not None else False
        )
        print("✅ 응답 생성 완료!")
        return response
    except Exception as e:
        print(f"❌ 응답 생성 실패: {type(e).__name__}: {str(e)}")
        import traceback
        traceback.print_exc()
        raise


@router.get("/status", response_model=SubscriptionStatusResponse)
async def get_subscription_status(
    user_id: int = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    현재 구독 상태 조회
    
    - PRO 구독 여부
    - 체험 기간 남은 일수
    - 접근 권한 여부
    """
    print(f"📊 구독 상태 조회 요청 받음!")
    print(f"   user_id: {user_id}")
    
    subscription = get_or_create_subscription(db, user_id)
    
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
        subscription_plan=subscription.subscription_plan or "free",
        has_access=subscription.is_pro_subscriber or trial_active,
        trial_start_date=subscription.trial_start_date,
        trial_end_date=subscription.trial_end_date,
        subscription_end_date=subscription.subscription_end_date,
        auto_renew=subscription.auto_renew if subscription.auto_renew is not None else False
    )


@router.post("/verify-purchase", response_model=VerifyPurchaseResponse)
async def verify_purchase(
    request: VerifyPurchaseRequest,
    user_id: int = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Google Play 구매 검증 및 PRO 구독 활성화
    
    - Google Play에서 구매한 구독을 검증
    - 검증 성공 시 PRO 구독 활성화
    """
    subscription = get_or_create_subscription(db, user_id)
    
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
        UserSubscription.user_id != user_id
    ).first()
    
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미 등록된 구매입니다."
        )
    
    # PRO 구독 활성화
    now = datetime.now(timezone.utc)
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
    user_id: int = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    구독 취소
    
    - 자동 갱신 비활성화
    - 현재 구독 기간까지는 PRO 사용 가능
    """
    subscription = get_or_create_subscription(db, user_id)
    
    if not subscription.is_pro_subscriber:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="활성화된 구독이 없습니다."
        )
    
    # 자동 갱신 비활성화
    subscription.auto_renew = False
    subscription.cancelled_at = datetime.now(timezone.utc)
    subscription.updated_at = datetime.now(timezone.utc)
    
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
    user_id: int = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    만료 임박 체험 사용자 조회 (관리자용)
    
    - 지정된 일수 이내 만료 예정인 체험 사용자 리스트
    """
    # TODO: 관리자 권한 체크 추가
    
    now = datetime.now(timezone.utc)
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
    user_id: int = Depends(get_current_user),
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
        UserSubscription.trial_end_date > datetime.now(timezone.utc)
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
