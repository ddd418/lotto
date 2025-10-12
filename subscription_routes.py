"""
구독 관리 API 라우터
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import Optional
from datetime import datetime

from database import get_db
from models import User
from subscription_models import UserSubscription
from auth import get_current_user


router = APIRouter(prefix="/api/subscription", tags=["subscription"])


# ==================== Request/Response 모델 ====================

class TrialStartRequest(BaseModel):
    """무료 체험 시작 요청"""
    pass


class SubscriptionStatusResponse(BaseModel):
    """구독 상태 응답"""
    user_id: int
    subscription_plan: str
    is_pro_subscriber: bool
    has_access: bool
    trial_info: dict
    pro_info: dict


class VerifyPurchaseRequest(BaseModel):
    """구매 검증 요청"""
    order_id: str
    purchase_token: str
    product_id: str


class VerifyPurchaseResponse(BaseModel):
    """구매 검증 응답"""
    success: bool
    message: str
    subscription_status: Optional[dict] = None


# ==================== API 엔드포인트 ====================

@router.post("/start-trial", status_code=status.HTTP_201_CREATED)
async def start_trial(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    무료 체험 시작
    
    - 사용자당 1회만 가능
    - 30일 무료 체험 제공
    """
    # 기존 구독 정보 확인
    subscription = db.query(UserSubscription).filter(
        UserSubscription.user_id == current_user.id
    ).first()
    
    # 구독 정보가 없으면 생성
    if not subscription:
        subscription = UserSubscription(user_id=current_user.id)
        db.add(subscription)
    
    # 이미 체험을 사용한 경우
    if subscription.is_trial_used:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미 무료 체험을 사용하셨습니다."
        )
    
    # 무료 체험 시작
    try:
        subscription.start_trial()
        db.commit()
        db.refresh(subscription)
        
        return {
            "success": True,
            "message": "무료 체험이 시작되었습니다.",
            "trial_days_remaining": subscription.trial_days_remaining,
            "trial_end_date": subscription.trial_end_date.isoformat(),
            "subscription_status": subscription.to_dict()
        }
    except ValueError as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"무료 체험 시작 중 오류 발생: {str(e)}"
        )


@router.get("/status", response_model=SubscriptionStatusResponse)
async def get_subscription_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    현재 사용자의 구독 상태 조회
    
    - 무료 체험 정보
    - PRO 구독 정보
    - 접근 권한 여부
    """
    # 구독 정보 조회
    subscription = db.query(UserSubscription).filter(
        UserSubscription.user_id == current_user.id
    ).first()
    
    # 구독 정보가 없으면 기본값 생성
    if not subscription:
        subscription = UserSubscription(user_id=current_user.id)
        db.add(subscription)
        db.commit()
        db.refresh(subscription)
    
    return subscription.to_dict()


@router.post("/verify-purchase", response_model=VerifyPurchaseResponse)
async def verify_purchase(
    request: VerifyPurchaseRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Google Play 구매 검증 및 PRO 구독 활성화
    
    - Google Play 구매 토큰 검증
    - PRO 구독 활성화
    """
    # 구독 정보 조회 또는 생성
    subscription = db.query(UserSubscription).filter(
        UserSubscription.user_id == current_user.id
    ).first()
    
    if not subscription:
        subscription = UserSubscription(user_id=current_user.id)
        db.add(subscription)
    
    try:
        # TODO: Google Play Developer API로 실제 구매 검증
        # 현재는 간단히 활성화만 수행
        # 실제 구현 시:
        # 1. Google Play Developer API 호출
        # 2. purchase_token 검증
        # 3. 구매 정보 확인 (상품 ID, 만료일 등)
        
        # PRO 구독 활성화
        subscription.activate_pro_subscription(
            order_id=request.order_id,
            purchase_token=request.purchase_token,
            product_id=request.product_id
        )
        
        db.commit()
        db.refresh(subscription)
        
        return {
            "success": True,
            "message": "PRO 구독이 활성화되었습니다.",
            "subscription_status": subscription.to_dict()
        }
        
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"구매 검증 중 오류 발생: {str(e)}"
        )


@router.post("/cancel")
async def cancel_subscription(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    구독 취소 (현재 기간 종료까지는 사용 가능)
    """
    subscription = db.query(UserSubscription).filter(
        UserSubscription.user_id == current_user.id
    ).first()
    
    if not subscription or not subscription.is_pro_subscriber:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="활성화된 구독이 없습니다."
        )
    
    try:
        subscription.cancel_subscription()
        db.commit()
        db.refresh(subscription)
        
        return {
            "success": True,
            "message": "구독이 취소되었습니다. 현재 기간 종료까지는 사용 가능합니다.",
            "subscription_end_date": subscription.subscription_end_date.isoformat() if subscription.subscription_end_date else None
        }
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"구독 취소 중 오류 발생: {str(e)}"
        )


# ==================== 관리자 전용 ====================

@router.get("/admin/expiring-trials")
async def get_expiring_trials(
    days: int = 3,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    만료 임박 무료 체험 사용자 조회 (관리자 전용)
    
    Args:
        days: 며칠 이내 만료 (기본값: 3일)
    """
    # TODO: 관리자 권한 체크 추가
    
    from datetime import datetime, timedelta
    
    threshold_date = datetime.utcnow() + timedelta(days=days)
    
    subscriptions = db.query(UserSubscription).filter(
        UserSubscription.is_trial_used == True,
        UserSubscription.trial_end_date <= threshold_date,
        UserSubscription.trial_end_date >= datetime.utcnow(),
        UserSubscription.is_pro_subscriber == False
    ).all()
    
    result = []
    for sub in subscriptions:
        user = db.query(User).filter(User.id == sub.user_id).first()
        result.append({
            "user_id": sub.user_id,
            "email": user.email if user else None,
            "trial_end_date": sub.trial_end_date.isoformat(),
            "days_remaining": sub.trial_days_remaining
        })
    
    return {
        "total": len(result),
        "expiring_within_days": days,
        "users": result
    }


@router.get("/admin/stats")
async def get_subscription_stats(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    구독 통계 조회 (관리자 전용)
    """
    # TODO: 관리자 권한 체크 추가
    
    total_users = db.query(UserSubscription).count()
    pro_users = db.query(UserSubscription).filter(
        UserSubscription.is_pro_subscriber == True
    ).count()
    trial_users = db.query(UserSubscription).filter(
        UserSubscription.is_trial_active == True
    ).count()
    trial_used = db.query(UserSubscription).filter(
        UserSubscription.is_trial_used == True
    ).count()
    
    return {
        "total_users": total_users,
        "pro_subscribers": pro_users,
        "active_trials": trial_users,
        "trial_used": trial_used,
        "conversion_rate": round(pro_users / trial_used * 100, 2) if trial_used > 0 else 0
    }
