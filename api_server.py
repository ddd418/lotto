# api_server.py
"""
로또 번호 추천 FastAPI 백엔드 서버
Android 앱에서 호출할 수 있는 REST API 제공
"""
from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Annotated
from datetime import datetime, timezone
from contextlib import asynccontextmanager
import json
import logging
from pathlib import Path
from collections import Counter
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
import atexit
from sqlalchemy.orm import Session

# 데이터베이스 및 인증 관련 import
from database import get_db, engine
from models import Base, User, SavedNumber, WinningCheck, UserSettings, WinningNumber
from auth import TokenManager
from kakao_auth import KakaoAuth

# 기존 lott.py의 함수들 임포트
from lott import (
    collect_stats, save_stats, load_stats, recommend_sets,
    STATS_PATH, LOTTO_MIN, LOTTO_MAX
)

# 로또 크롤러 임포트
from lotto_crawler import (
    get_or_fetch_winning_number,
    sync_all_winning_numbers,
    get_latest_draw_number,
    get_latest_winning_numbers
)

# 로또 당첨 확인 임포트
from lotto_checker import (
    check_winning,
    get_rank_message,
    estimate_prize_amount
)

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 데이터베이스 테이블 생성
Base.metadata.create_all(bind=engine)

# 스케줄러 초기화
scheduler = BackgroundScheduler(timezone="Asia/Seoul")

# 보안 스키마
security = HTTPBearer()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    앱 생명주기 관리 (startup/shutdown)
    """
    # Startup
    setup_scheduler()
    yield
    # Shutdown
    if scheduler.running:
        scheduler.shutdown()
        logger.info("🛑 스케줄러 종료됨")

app = FastAPI(
    title="로또 번호 추천 API",
    description="AI 기반 로또 번호 추천 서비스 (카카오 로그인 지원)",
    version="2.0.0",
    lifespan=lifespan
)

# CORS 설정 (Android 앱에서 접근 허용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 특정 도메인만 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# -----------------------------
# Request/Response 모델
# -----------------------------
class RecommendRequest(BaseModel):
    n_sets: int = Field(default=5, ge=1, le=10, description="추천받을 번호 세트 개수 (1~10)")
    seed: Optional[int] = Field(default=None, description="랜덤 시드 (재현성을 위해)")
    mode: str = Field(default="ai", description="추천 모드: ai(기본), random(랜덤), conservative(보수적), aggressive(공격적)")

class LottoSet(BaseModel):
    numbers: List[int] = Field(description="정렬된 6개의 로또 번호")

class RecommendResponse(BaseModel):
    success: bool
    last_draw: int = Field(description="기준이 된 마지막 회차")
    generated_at: str = Field(description="추천 생성 시각")
    sets: List[LottoSet] = Field(description="추천 번호 세트 목록")
    include_bonus: bool = Field(description="보너스 번호 포함 여부")

class StatsResponse(BaseModel):
    success: bool
    last_draw: int
    generated_at: str
    include_bonus: bool
    frequency: Dict[str, int] = Field(description="각 번호의 출현 빈도")
    top_10: List[Dict[str, int]] = Field(description="상위 10개 번호")

class HealthResponse(BaseModel):
    status: str
    version: str
    stats_available: bool
    last_draw: Optional[int]
    scheduler_running: bool
    next_update: Optional[str]

class UpdateResponse(BaseModel):
    success: bool
    message: str
    last_draw: int
    updated_at: str
    new_data_count: int

# -----------------------------
# 인증 관련 모델
# -----------------------------
class KakaoLoginRequest(BaseModel):
    authorization_code: str = Field(description="카카오에서 받은 인증 코드")

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int

class UserProfile(BaseModel):
    id: int
    kakao_id: str
    email: Optional[str]
    nickname: str
    profile_image: Optional[str]
    created_at: datetime
    last_login_at: Optional[datetime]

class SavedNumberRequest(BaseModel):
    numbers: List[int] = Field(min_items=6, max_items=6, description="저장할 6개의 로또 번호")
    nickname: Optional[str] = Field(default=None, max_length=50, description="번호 별칭")
    memo: Optional[str] = Field(default=None, description="메모")
    is_favorite: bool = Field(default=False, description="즐겨찾기 여부")
    recommendation_type: Optional[str] = Field(default=None, description="추천 유형")

class SavedNumberResponse(BaseModel):
    id: int
    numbers: List[int]
    nickname: Optional[str]
    memo: Optional[str]
    is_favorite: bool
    recommendation_type: Optional[str]
    created_at: datetime

class WinningNumberResponse(BaseModel):
    """당첨 번호 응답 모델"""
    draw_number: int
    numbers: List[int] = Field(description="당첨 번호 6개 (정렬됨)")
    bonus_number: int
    draw_date: Optional[datetime]
    prize_1st: Optional[int]
    prize_2nd: Optional[int]
    prize_3rd: Optional[int]
    prize_4th: Optional[int]
    prize_5th: Optional[int]
    winners_1st: Optional[int]
    total_sales: Optional[int]

class WinningNumberListResponse(BaseModel):
    """당첨 번호 목록 응답"""
    success: bool
    count: int
    latest_draw: Optional[int]
    winning_numbers: List[WinningNumberResponse]

class SyncResponse(BaseModel):
    """동기화 결과 응답"""
    success: bool
    message: str
    success_count: int
    skip_count: int
    fail_count: int
    total: int

class CheckWinningRequest(BaseModel):
    """당첨 확인 요청 모델"""
    numbers: List[int] = Field(min_items=6, max_items=6, description="확인할 6개의 로또 번호")
    draw_number: int = Field(description="확인할 회차 번호")

class CheckWinningResponse(BaseModel):
    """당첨 확인 결과 응답"""
    success: bool
    draw_number: int
    user_numbers: List[int]
    winning_numbers: List[int]
    bonus_number: int
    matched_count: int
    has_bonus: bool
    rank: Optional[int] = Field(description="당첨 등수 (1~5등, None은 미당첨)")
    prize_amount: Optional[int] = Field(description="예상 당첨금 (실제 금액과 다를 수 있음)")
    message: str

class UserSettingsRequest(BaseModel):
    """사용자 설정 업데이트 요청"""
    enable_push_notifications: Optional[bool] = None
    enable_draw_notifications: Optional[bool] = None
    enable_winning_notifications: Optional[bool] = None
    theme_mode: Optional[str] = Field(None, description="light, dark, system")
    default_recommendation_type: Optional[str] = Field(None, description="balanced, hot, cold, random")
    lucky_numbers: Optional[List[int]] = Field(None, description="행운의 번호들")
    exclude_numbers: Optional[List[int]] = Field(None, description="제외할 번호들")

class UserSettingsResponse(BaseModel):
    """사용자 설정 응답"""
    user_id: int
    enable_push_notifications: bool
    enable_draw_notifications: bool
    enable_winning_notifications: bool
    theme_mode: str
    default_recommendation_type: str
    lucky_numbers: Optional[List[int]]
    exclude_numbers: Optional[List[int]]
    created_at: datetime
    updated_at: Optional[datetime]

# -----------------------------
# 인증 의존성 함수
# -----------------------------
async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
) -> User:
    """
    현재 인증된 사용자 반환
    """
    token = credentials.credentials
    user_id = TokenManager.get_user_id_from_token(token)
    
    user = db.query(User).filter(User.id == user_id, User.is_active == True).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="사용자를 찾을 수 없습니다"
        )
    
    return user

# -----------------------------
# API 엔드포인트
# -----------------------------

@app.get("/", response_model=Dict[str, str])
async def root():
    """
    루트 엔드포인트 - API 정보 제공
    """
    return {
        "service": "로또 번호 추천 API",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/api/health"
    }

@app.get("/api/health", response_model=HealthResponse)
async def health_check():
    """
    서버 상태 확인
    """
    stats = load_stats()
    
    # 다음 예정된 업데이트 시간 가져오기
    next_update = None
    if scheduler.running:
        jobs = scheduler.get_jobs()
        if jobs:
            next_run = jobs[0].next_run_time
            if next_run:
                next_update = next_run.isoformat()
    
    return HealthResponse(
        status="healthy",
        version="2.0.0",
        stats_available=stats is not None,
        last_draw=stats.get("last_draw") if stats else None,
        scheduler_running=scheduler.running,
        next_update=next_update
    )

# -----------------------------
# 인증 관련 엔드포인트
# -----------------------------

@app.post("/auth/kakao/login", response_model=TokenResponse)
async def kakao_login(
    request: KakaoLoginRequest,
    db: Session = Depends(get_db)
):
    """
    카카오 로그인 처리 (모바일 앱용)
    안드로이드 앱에서 카카오 SDK로 받은 액세스 토큰을 직접 사용
    """
    try:
        # 안드로이드에서 받은 authorization_code는 실제로는 액세스 토큰
        kakao_access_token = request.authorization_code
        
        # 액세스 토큰 유효성 검증 및 사용자 정보 가져오기
        kakao_user_info = await KakaoAuth.get_user_info(kakao_access_token)
        user_data = KakaoAuth.extract_user_data(kakao_user_info)
        
        # 데이터베이스에서 사용자 찾기 또는 생성
        user = db.query(User).filter(User.kakao_id == user_data["kakao_id"]).first()
        
        if not user:
            # 새 사용자 생성
            user = User(
                kakao_id=user_data["kakao_id"],
                email=user_data["email"],
                nickname=user_data["nickname"],
                profile_image=user_data["profile_image"],
                last_login_at=datetime.now(timezone.utc)
            )
            db.add(user)
            db.commit()
            db.refresh(user)
            
            # 기본 설정 생성
            user_settings = UserSettings(user_id=user.id)
            db.add(user_settings)
            db.commit()
        else:
            # 기존 사용자 정보 업데이트
            user.email = user_data["email"]
            user.nickname = user_data["nickname"]
            user.profile_image = user_data["profile_image"]
            user.last_login_at = datetime.now(timezone.utc)
            db.commit()
        
        # JWT 토큰 생성
        access_token = TokenManager.create_access_token(data={"sub": str(user.id)})
        refresh_token = TokenManager.create_refresh_token(data={"sub": str(user.id)})
        
        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token,
            expires_in=30 * 60  # 30분
        )
        
    except Exception as e:
        logger.error(f"카카오 로그인 오류: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="카카오 로그인에 실패했습니다"
        )

@app.get("/auth/me", response_model=UserProfile)
async def get_me(current_user: User = Depends(get_current_user)):
    """
    현재 로그인한 사용자 정보 조회
    """
    return UserProfile(
        id=current_user.id,
        kakao_id=current_user.kakao_id,
        email=current_user.email,
        nickname=current_user.nickname,
        profile_image=current_user.profile_image,
        created_at=current_user.created_at,
        last_login_at=current_user.last_login_at
    )

@app.post("/auth/logout")
async def logout(current_user: User = Depends(get_current_user)):
    """
    로그아웃 처리
    클라이언트에서 토큰을 삭제하도록 응답
    """
    return {
        "message": "로그아웃되었습니다",
        "user_id": current_user.id
    }

@app.get("/auth/profile", response_model=UserProfile)
async def get_user_profile(current_user: User = Depends(get_current_user)):
    """
    현재 사용자 프로필 조회
    """
    return UserProfile(
        id=current_user.id,
        kakao_id=current_user.kakao_id,
        email=current_user.email,
        nickname=current_user.nickname,
        profile_image=current_user.profile_image,
        created_at=current_user.created_at,
        last_login_at=current_user.last_login_at
    )

@app.get("/auth/kakao/url")
async def get_kakao_auth_url():
    """
    카카오 인증 URL 반환 (Android에서 사용)
    """
    return {"authorization_url": KakaoAuth.get_authorization_url()}

# -----------------------------
# 당첨 번호 관련 엔드포인트
# -----------------------------

@app.get("/api/winning-numbers/latest", response_model=WinningNumberResponse)
async def get_latest_winning_number(db: Session = Depends(get_db)):
    """
    최신 당첨 번호 조회 (DB 우선, 없으면 API에서 가져옴)
    """
    try:
        # 최신 회차 번호 추정
        latest_draw = get_latest_draw_number()
        
        if not latest_draw:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="최신 회차를 찾을 수 없습니다"
            )
        
        # DB 또는 API에서 가져오기
        winning = get_or_fetch_winning_number(db, latest_draw)
        
        if not winning:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{latest_draw}회차 당첨 번호를 찾을 수 없습니다"
            )
        
        return WinningNumberResponse(
            draw_number=winning.draw_number,
            numbers=[winning.number1, winning.number2, winning.number3,
                    winning.number4, winning.number5, winning.number6],
            bonus_number=winning.bonus_number,
            draw_date=winning.draw_date,
            prize_1st=winning.prize_1st,
            prize_2nd=winning.prize_2nd,
            prize_3rd=winning.prize_3rd,
            prize_4th=winning.prize_4th,
            prize_5th=winning.prize_5th,
            winners_1st=winning.winners_1st,
            total_sales=winning.total_sales
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"최신 당첨 번호 조회 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="당첨 번호 조회 중 오류가 발생했습니다"
        )

@app.get("/api/winning-numbers/{draw_number}", response_model=WinningNumberResponse)
async def get_winning_number_by_draw(
    draw_number: int,
    db: Session = Depends(get_db)
):
    """
    특정 회차의 당첨 번호 조회
    """
    if draw_number < 1:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="회차 번호는 1 이상이어야 합니다"
        )
    
    try:
        winning = get_or_fetch_winning_number(db, draw_number)
        
        if not winning:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{draw_number}회차 당첨 번호를 찾을 수 없습니다"
            )
        
        return WinningNumberResponse(
            draw_number=winning.draw_number,
            numbers=[winning.number1, winning.number2, winning.number3,
                    winning.number4, winning.number5, winning.number6],
            bonus_number=winning.bonus_number,
            draw_date=winning.draw_date,
            prize_1st=winning.prize_1st,
            prize_2nd=winning.prize_2nd,
            prize_3rd=winning.prize_3rd,
            prize_4th=winning.prize_4th,
            prize_5th=winning.prize_5th,
            winners_1st=winning.winners_1st,
            total_sales=winning.total_sales
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"당첨 번호 조회 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="당첨 번호 조회 중 오류가 발생했습니다"
        )

@app.get("/api/winning-numbers", response_model=WinningNumberListResponse)
async def get_winning_numbers(
    limit: int = 10,
    db: Session = Depends(get_db)
):
    """
    최신 당첨 번호 N개 조회
    """
    if limit < 1 or limit > 100:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="limit은 1~100 사이여야 합니다"
        )
    
    try:
        winnings = get_latest_winning_numbers(db, limit)
        
        latest_draw = winnings[0].draw_number if winnings else None
        
        return WinningNumberListResponse(
            success=True,
            count=len(winnings),
            latest_draw=latest_draw,
            winning_numbers=[
                WinningNumberResponse(
                    draw_number=w.draw_number,
                    numbers=[w.number1, w.number2, w.number3, w.number4, w.number5, w.number6],
                    bonus_number=w.bonus_number,
                    draw_date=w.draw_date,
                    prize_1st=w.prize_1st,
                    prize_2nd=w.prize_2nd,
                    prize_3rd=w.prize_3rd,
                    prize_4th=w.prize_4th,
                    prize_5th=w.prize_5th,
                    winners_1st=w.winners_1st,
                    total_sales=w.total_sales
                )
                for w in winnings
            ]
        )
        
    except Exception as e:
        logger.error(f"당첨 번호 목록 조회 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="당첨 번호 목록 조회 중 오류가 발생했습니다"
        )

@app.post("/api/winning-numbers/sync", response_model=SyncResponse)
async def sync_winning_numbers(
    start_draw: int = 1,
    end_draw: Optional[int] = None,
    db: Session = Depends(get_db)
):
    """
    당첨 번호 동기화 (관리자용 - 추후 인증 추가)
    start_draw부터 end_draw까지 (또는 최신까지) DB에 저장
    """
    if start_draw < 1:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="시작 회차는 1 이상이어야 합니다"
        )
    
    try:
        result = sync_all_winning_numbers(db, start_draw, end_draw)
        
        if not result.get("success"):
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=result.get("error", "동기화 실패")
            )
        
        return SyncResponse(
            success=True,
            message=f"{result['success_count']}개 회차 동기화 완료",
            success_count=result['success_count'],
            skip_count=result['skip_count'],
            fail_count=result['fail_count'],
            total=result['total']
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"당첨 번호 동기화 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="동기화 중 오류가 발생했습니다"
        )

# -----------------------------
# 사용자 데이터 관련 엔드포인트
# -----------------------------

@app.post("/api/saved-numbers", response_model=SavedNumberResponse)
async def save_number(
    request: SavedNumberRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    로또 번호 저장
    """
    # 번호 유효성 검사
    for num in request.numbers:
        if not (1 <= num <= 45):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="로또 번호는 1~45 사이여야 합니다"
            )
    
    # 중복 번호 확인
    if len(set(request.numbers)) != 6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="중복된 번호가 있습니다"
        )
    
    # 저장된 번호 생성
    saved_number = SavedNumber(
        user_id=current_user.id,
        number1=request.numbers[0],
        number2=request.numbers[1],
        number3=request.numbers[2],
        number4=request.numbers[3],
        number5=request.numbers[4],
        number6=request.numbers[5],
        nickname=request.nickname,
        memo=request.memo,
        is_favorite=request.is_favorite,
        recommendation_type=request.recommendation_type
    )
    
    db.add(saved_number)
    db.commit()
    db.refresh(saved_number)
    
    return SavedNumberResponse(
        id=saved_number.id,
        numbers=[saved_number.number1, saved_number.number2, saved_number.number3,
                saved_number.number4, saved_number.number5, saved_number.number6],
        nickname=saved_number.nickname,
        memo=saved_number.memo,
        is_favorite=saved_number.is_favorite,
        recommendation_type=saved_number.recommendation_type,
        created_at=saved_number.created_at
    )

@app.get("/api/saved-numbers", response_model=List[SavedNumberResponse])
async def get_saved_numbers(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    저장된 로또 번호 목록 조회
    """
    saved_numbers = db.query(SavedNumber).filter(
        SavedNumber.user_id == current_user.id
    ).order_by(SavedNumber.created_at.desc()).all()
    
    return [
        SavedNumberResponse(
            id=saved.id,
            numbers=[saved.number1, saved.number2, saved.number3,
                    saved.number4, saved.number5, saved.number6],
            nickname=saved.nickname,
            memo=saved.memo,
            is_favorite=saved.is_favorite,
            recommendation_type=saved.recommendation_type,
            created_at=saved.created_at
        )
        for saved in saved_numbers
    ]

@app.delete("/api/saved-numbers/{number_id}")
async def delete_saved_number(
    number_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    저장된 로또 번호 삭제
    """
    saved_number = db.query(SavedNumber).filter(
        SavedNumber.id == number_id,
        SavedNumber.user_id == current_user.id
    ).first()
    
    if not saved_number:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="저장된 번호를 찾을 수 없습니다"
        )
    
    db.delete(saved_number)
    db.commit()
    
    return {"message": "저장된 번호가 삭제되었습니다"}

@app.put("/api/saved-numbers/{number_id}", response_model=SavedNumberResponse)
async def update_saved_number(
    number_id: int,
    request: SavedNumberRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    저장된 로또 번호 업데이트
    """
    saved_number = db.query(SavedNumber).filter(
        SavedNumber.id == number_id,
        SavedNumber.user_id == current_user.id
    ).first()
    
    if not saved_number:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="저장된 번호를 찾을 수 없습니다"
        )
    
    # 번호 유효성 검사
    for num in request.numbers:
        if not (1 <= num <= 45):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="로또 번호는 1~45 사이여야 합니다"
            )
    
    # 중복 번호 확인
    if len(set(request.numbers)) != 6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="중복된 번호가 있습니다"
        )
    
    # 번호 업데이트
    saved_number.number1 = request.numbers[0]
    saved_number.number2 = request.numbers[1]
    saved_number.number3 = request.numbers[2]
    saved_number.number4 = request.numbers[3]
    saved_number.number5 = request.numbers[4]
    saved_number.number6 = request.numbers[5]
    saved_number.nickname = request.nickname
    saved_number.memo = request.memo
    saved_number.is_favorite = request.is_favorite
    saved_number.recommendation_type = request.recommendation_type
    
    db.commit()
    db.refresh(saved_number)
    
    return SavedNumberResponse(
        id=saved_number.id,
        numbers=[saved_number.number1, saved_number.number2, saved_number.number3,
                saved_number.number4, saved_number.number5, saved_number.number6],
        nickname=saved_number.nickname,
        memo=saved_number.memo,
        is_favorite=saved_number.is_favorite,
        recommendation_type=saved_number.recommendation_type,
        created_at=saved_number.created_at
    )

# -----------------------------
# 당첨 확인 엔드포인트
# -----------------------------

@app.post("/api/check-winning", response_model=CheckWinningResponse)
async def check_winning_numbers(
    request: CheckWinningRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    로또 번호 당첨 확인
    사용자의 번호와 해당 회차 당첨 번호를 비교하여 결과 반환
    """
    # 번호 유효성 검사
    for num in request.numbers:
        if not (1 <= num <= 45):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="로또 번호는 1~45 사이여야 합니다"
            )
    
    # 중복 번호 확인
    if len(set(request.numbers)) != 6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="중복된 번호가 있습니다"
        )
    
    # 회차 유효성 검사
    if request.draw_number < 1:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="회차 번호는 1 이상이어야 합니다"
        )
    
    try:
        # 당첨 번호 가져오기 (DB 우선, 없으면 API 호출)
        winning = get_or_fetch_winning_number(db, request.draw_number)
        
        if not winning:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"{request.draw_number}회차 당첨 번호를 찾을 수 없습니다"
            )
        
        # 당첨 번호 리스트로 변환
        winning_numbers = [
            winning.number1, winning.number2, winning.number3,
            winning.number4, winning.number5, winning.number6
        ]
        
        # 당첨 확인
        matched_count, has_bonus, rank = check_winning(
            request.numbers,
            winning_numbers,
            winning.bonus_number
        )
        
        # 당첨금 계산 (실제 당첨금 또는 평균값)
        actual_prize = None
        if rank == 1:
            actual_prize = winning.prize_1st
        elif rank == 2:
            actual_prize = winning.prize_2nd
        elif rank == 3:
            actual_prize = winning.prize_3rd
        elif rank == 4:
            actual_prize = winning.prize_4th
        elif rank == 5:
            actual_prize = winning.prize_5th
        
        prize_amount = estimate_prize_amount(rank, actual_prize)
        
        # 메시지 생성
        message = get_rank_message(rank, matched_count, has_bonus)
        
        # 당첨 내역 DB에 저장
        winning_check = WinningCheck(
            user_id=current_user.id,
            numbers=request.numbers,
            draw_number=request.draw_number,
            rank=rank,
            prize_amount=prize_amount,
            matched_count=matched_count,
            has_bonus=has_bonus
        )
        
        db.add(winning_check)
        db.commit()
        
        return CheckWinningResponse(
            success=True,
            draw_number=request.draw_number,
            user_numbers=request.numbers,
            winning_numbers=winning_numbers,
            bonus_number=winning.bonus_number,
            matched_count=matched_count,
            has_bonus=has_bonus,
            rank=rank,
            prize_amount=prize_amount,
            message=message
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"당첨 확인 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="당첨 확인 중 오류가 발생했습니다"
        )

@app.get("/api/winning-history", response_model=List[Dict])
async def get_winning_history(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    limit: int = 20
):
    """
    사용자의 당첨 확인 내역 조회
    """
    if limit < 1 or limit > 100:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="limit은 1~100 사이여야 합니다"
        )
    
    try:
        history = db.query(WinningCheck).filter(
            WinningCheck.user_id == current_user.id
        ).order_by(WinningCheck.checked_at.desc()).limit(limit).all()
        
        return [
            {
                "id": check.id,
                "numbers": check.numbers,
                "draw_number": check.draw_number,
                "rank": check.rank,
                "prize_amount": check.prize_amount,
                "matched_count": check.matched_count,
                "has_bonus": check.has_bonus,
                "checked_at": check.checked_at.isoformat()
            }
            for check in history
        ]
        
    except Exception as e:
        logger.error(f"당첨 내역 조회 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="당첨 내역 조회 중 오류가 발생했습니다"
        )

# -----------------------------
# 사용자 설정 엔드포인트
# -----------------------------

@app.get("/api/settings", response_model=UserSettingsResponse)
async def get_user_settings(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    현재 사용자의 설정 조회
    """
    try:
        settings = db.query(UserSettings).filter(
            UserSettings.user_id == current_user.id
        ).first()
        
        if not settings:
            # 설정이 없으면 기본 설정 생성
            settings = UserSettings(user_id=current_user.id)
            db.add(settings)
            db.commit()
            db.refresh(settings)
        
        return UserSettingsResponse(
            user_id=settings.user_id,
            enable_push_notifications=settings.enable_push_notifications,
            enable_draw_notifications=settings.enable_draw_notifications,
            enable_winning_notifications=settings.enable_winning_notifications,
            theme_mode=settings.theme_mode,
            default_recommendation_type=settings.default_recommendation_type,
            lucky_numbers=settings.lucky_numbers,
            exclude_numbers=settings.exclude_numbers,
            created_at=settings.created_at,
            updated_at=settings.updated_at
        )
        
    except Exception as e:
        logger.error(f"설정 조회 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="설정 조회 중 오류가 발생했습니다"
        )

@app.put("/api/settings", response_model=UserSettingsResponse)
async def update_user_settings(
    request: UserSettingsRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    사용자 설정 업데이트
    """
    try:
        settings = db.query(UserSettings).filter(
            UserSettings.user_id == current_user.id
        ).first()
        
        if not settings:
            # 설정이 없으면 새로 생성
            settings = UserSettings(user_id=current_user.id)
            db.add(settings)
        
        # 요청에 포함된 필드만 업데이트
        if request.enable_push_notifications is not None:
            settings.enable_push_notifications = request.enable_push_notifications
        if request.enable_draw_notifications is not None:
            settings.enable_draw_notifications = request.enable_draw_notifications
        if request.enable_winning_notifications is not None:
            settings.enable_winning_notifications = request.enable_winning_notifications
        if request.theme_mode is not None:
            if request.theme_mode not in ["light", "dark", "system"]:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="theme_mode는 light, dark, system 중 하나여야 합니다"
                )
            settings.theme_mode = request.theme_mode
        if request.default_recommendation_type is not None:
            if request.default_recommendation_type not in ["balanced", "hot", "cold", "random"]:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="default_recommendation_type는 balanced, hot, cold, random 중 하나여야 합니다"
                )
            settings.default_recommendation_type = request.default_recommendation_type
        if request.lucky_numbers is not None:
            # 행운의 번호 유효성 검사
            for num in request.lucky_numbers:
                if not (1 <= num <= 45):
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail="행운의 번호는 1~45 사이여야 합니다"
                    )
            settings.lucky_numbers = request.lucky_numbers
        if request.exclude_numbers is not None:
            # 제외할 번호 유효성 검사
            for num in request.exclude_numbers:
                if not (1 <= num <= 45):
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail="제외할 번호는 1~45 사이여야 합니다"
                    )
            settings.exclude_numbers = request.exclude_numbers
        
        db.commit()
        db.refresh(settings)
        
        return UserSettingsResponse(
            user_id=settings.user_id,
            enable_push_notifications=settings.enable_push_notifications,
            enable_draw_notifications=settings.enable_draw_notifications,
            enable_winning_notifications=settings.enable_winning_notifications,
            theme_mode=settings.theme_mode,
            default_recommendation_type=settings.default_recommendation_type,
            lucky_numbers=settings.lucky_numbers,
            exclude_numbers=settings.exclude_numbers,
            created_at=settings.created_at,
            updated_at=settings.updated_at
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"설정 업데이트 오류: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="설정 업데이트 중 오류가 발생했습니다"
        )

# -----------------------------
# 번호 추천 엔드포인트
# -----------------------------

@app.post("/api/recommend", response_model=RecommendResponse)
async def recommend_numbers(request: RecommendRequest):
    """
    로또 번호 추천 API
    
    - **n_sets**: 추천받을 번호 세트 개수 (기본 5, 최대 10)
    - **seed**: 랜덤 시드 (선택사항, 같은 시드면 같은 결과)
    
    Returns:
        추천된 로또 번호 세트들
    """
    # 저장된 통계 로드
    stats = load_stats()
    if not stats:
        raise HTTPException(
            status_code=404,
            detail="통계 데이터가 없습니다. /api/update를 먼저 호출하세요."
        )
    
    try:
        # 번호 추천 생성
        sets = recommend_sets(stats, n_sets=request.n_sets, seed=request.seed, mode=request.mode)
        
        return RecommendResponse(
            success=True,
            last_draw=stats.get("last_draw", 0),
            generated_at=datetime.now().isoformat(),
            sets=[LottoSet(numbers=s) for s in sets],
            include_bonus=stats.get("include_bonus", False)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"번호 생성 중 오류: {str(e)}")

@app.get("/api/stats", response_model=StatsResponse)
async def get_statistics():
    """
    로또 번호 통계 조회
    
    Returns:
        각 번호의 출현 빈도 및 상위 번호 정보
    """
    stats = load_stats()
    if not stats:
        raise HTTPException(
            status_code=404,
            detail="통계 데이터가 없습니다. /api/update를 먼저 호출하세요."
        )
    
    freq = stats.get("frequency", {})
    
    # 상위 10개 번호 추출
    freq_items = [(int(k) if k.isdigit() else k, v) for k, v in freq.items()]
    sorted_freq = sorted(freq_items, key=lambda x: (-x[1], x[0]))
    top_10 = [{"number": num, "count": count} for num, count in sorted_freq[:10]]
    
    return StatsResponse(
        success=True,
        last_draw=stats.get("last_draw", 0),
        generated_at=stats.get("generated_at", ""),
        include_bonus=stats.get("include_bonus", False),
        frequency=freq,
        top_10=top_10
    )

@app.get("/api/latest-draw")
async def get_latest_draw():
    """
    저장된 최신 회차 정보 조회
    """
    stats = load_stats()
    if not stats:
        raise HTTPException(status_code=404, detail="통계 데이터가 없습니다.")
    
    return {
        "success": True,
        "last_draw": stats.get("last_draw", 0),
        "generated_at": stats.get("generated_at", ""),
        "include_bonus": stats.get("include_bonus", False)
    }

# -----------------------------
# 자동 업데이트 함수
# -----------------------------
def auto_update_lotto_data():
    """
    자동으로 로또 데이터를 업데이트하는 함수
    매주 토요일 9시 30분에 실행됩니다
    """
    try:
        logger.info("🔄 자동 로또 데이터 업데이트 시작...")
        
        # 현재 저장된 마지막 회차 확인
        current_stats = load_stats()
        current_last_draw = current_stats.get("last_draw", 0) if current_stats else 0
        
        # 새로운 데이터 수집
        freq, last_draw, draws_store = collect_stats(max_draw=None, include_bonus=False)
        
        if last_draw > current_last_draw:
            # 새로운 데이터가 있으면 저장
            save_stats(freq, last_draw, False, draws_store)
            new_data_count = last_draw - current_last_draw
            logger.info(f"✅ 자동 업데이트 완료! 새로운 {new_data_count}개 회차 데이터 추가 (최신: {last_draw}회차)")
        else:
            logger.info(f"ℹ️ 새로운 데이터가 없습니다 (현재 최신: {last_draw}회차)")
            
    except Exception as e:
        logger.error(f"❌ 자동 업데이트 중 오류 발생: {str(e)}")

@app.post("/api/update", response_model=UpdateResponse)
async def manual_update():
    """
    수동으로 로또 데이터 업데이트
    
    Returns:
        업데이트 결과 및 상태 정보
    """
    try:
        logger.info("🔄 수동 로또 데이터 업데이트 시작...")
        
        # 현재 저장된 마지막 회차 확인
        current_stats = load_stats()
        current_last_draw = current_stats.get("last_draw", 0) if current_stats else 0
        
        # 새로운 데이터 수집
        freq, last_draw, draws_store = collect_stats(max_draw=None, include_bonus=False)
        
        # 데이터 저장
        save_stats(freq, last_draw, False, draws_store)
        new_data_count = last_draw - current_last_draw
        
        if new_data_count > 0:
            message = f"✅ {new_data_count}개의 새로운 회차 데이터가 업데이트되었습니다"
        else:
            message = "ℹ️ 이미 최신 데이터입니다"
        
        logger.info(f"✅ 수동 업데이트 완료! (최신: {last_draw}회차)")
        
        return UpdateResponse(
            success=True,
            message=message,
            last_draw=last_draw,
            updated_at=datetime.now().isoformat(),
            new_data_count=max(0, new_data_count)
        )
        
    except Exception as e:
        logger.error(f"❌ 수동 업데이트 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"업데이트 중 오류: {str(e)}")

# -----------------------------
# 스케줄러 설정
# -----------------------------
def setup_scheduler():
    """
    자동 업데이트 스케줄러 설정
    매주 토요일 9시 30분에 실행
    """
    try:
        # 기존 작업이 있으면 제거
        scheduler.remove_all_jobs()
        
        # 매주 토요일 9시 30분에 실행
        scheduler.add_job(
            func=auto_update_lotto_data,
            trigger=CronTrigger(
                day_of_week=5,  # 0=Monday, 5=Saturday
                hour=21,        # 9 PM
                minute=30,      # 30분
                timezone="Asia/Seoul"
            ),
            id="lotto_auto_update",
            name="로또 데이터 자동 업데이트",
            replace_existing=True
        )
        
        logger.info("📅 스케줄러 설정 완료: 매주 토요일 오후 9시 30분에 자동 업데이트")
        
        # 스케줄러 시작
        if not scheduler.running:
            scheduler.start()
            logger.info("🚀 스케줄러 시작됨")
            
    except Exception as e:
        logger.error(f"❌ 스케줄러 설정 중 오류: {str(e)}")

# -----------------------------
# 서버 실행 (개발용)
# -----------------------------
if __name__ == "__main__":
    import uvicorn
    
    # 시작 시 통계 파일이 없으면 자동 생성
    if not STATS_PATH.exists():
        print("📊 초기 데이터 수집 중...")
        freq, last_draw, draws_store = collect_stats(max_draw=None, include_bonus=False)
        save_stats(freq, last_draw, False, draws_store)
        print(f"✅ 1~{last_draw}회차 데이터 수집 완료!\n")
    
    # 스케줄러 정리를 위한 atexit 핸들러 등록
    atexit.register(lambda: scheduler.shutdown() if scheduler.running else None)
    
    print("🚀 로또 API 서버 시작...")
    print("📖 API 문서: http://localhost:8000/docs")
    print("🏥 헬스체크: http://localhost:8000/api/health")
    print("🔄 수동 업데이트: http://localhost:8000/api/update")
    print("📅 자동 업데이트: 매주 토요일 오후 9시 30분\n")
    
    try:
        uvicorn.run(app, host="0.0.0.0", port=8000)
    except KeyboardInterrupt:
        print("\n🛑 서버 종료 중...")
        if scheduler.running:
            scheduler.shutdown()
        print("✅ 서버가 안전하게 종료되었습니다.")
