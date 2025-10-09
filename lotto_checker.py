"""
로또 당첨 확인 유틸리티
"""
from typing import List, Tuple, Optional, Dict
import logging

logger = logging.getLogger(__name__)

def calculate_rank(matched_count: int, has_bonus: bool) -> Optional[int]:
    """
    맞춘 번호 개수와 보너스 번호 포함 여부로 등수 계산
    
    Args:
        matched_count: 맞춘 번호 개수 (0~6)
        has_bonus: 보너스 번호 포함 여부
        
    Returns:
        등수 (1~5) 또는 None (미당첨)
    """
    if matched_count == 6:
        return 1  # 1등: 6개 모두 맞음
    elif matched_count == 5 and has_bonus:
        return 2  # 2등: 5개 + 보너스
    elif matched_count == 5:
        return 3  # 3등: 5개
    elif matched_count == 4:
        return 4  # 4등: 4개
    elif matched_count == 3:
        return 5  # 5등: 3개
    else:
        return None  # 미당첨

def check_winning(
    user_numbers: List[int],
    winning_numbers: List[int],
    bonus_number: int
) -> Tuple[int, bool, Optional[int]]:
    """
    사용자 번호와 당첨 번호 비교
    
    Args:
        user_numbers: 사용자 번호 6개
        winning_numbers: 당첨 번호 6개
        bonus_number: 보너스 번호
        
    Returns:
        (맞춘 개수, 보너스 포함 여부, 등수)
    """
    # 집합으로 변환하여 교집합 계산
    user_set = set(user_numbers)
    winning_set = set(winning_numbers)
    
    # 맞춘 개수
    matched_count = len(user_set & winning_set)
    
    # 보너스 번호 확인 (5개 맞았을 때만 의미 있음)
    has_bonus = bonus_number in user_set and matched_count == 5
    
    # 등수 계산
    rank = calculate_rank(matched_count, has_bonus)
    
    logger.info(f"당첨 확인: {user_numbers} vs {winning_numbers}+{bonus_number} → {matched_count}개 맞음, 보너스={has_bonus}, 등수={rank}")
    
    return matched_count, has_bonus, rank

def get_rank_message(rank: Optional[int], matched_count: int, has_bonus: bool) -> str:
    """
    등수에 따른 메시지 생성
    
    Args:
        rank: 등수 (1~5 또는 None)
        matched_count: 맞춘 개수
        has_bonus: 보너스 포함 여부
        
    Returns:
        결과 메시지
    """
    if rank == 1:
        return "🎉 축하합니다! 1등 당첨입니다! (6개 숫자 일치)"
    elif rank == 2:
        return "🎊 축하합니다! 2등 당첨입니다! (5개 숫자 + 보너스 번호 일치)"
    elif rank == 3:
        return "🎈 축하합니다! 3등 당첨입니다! (5개 숫자 일치)"
    elif rank == 4:
        return "👏 축하합니다! 4등 당첨입니다! (4개 숫자 일치)"
    elif rank == 5:
        return "✨ 축하합니다! 5등 당첨입니다! (3개 숫자 일치)"
    else:
        if matched_count == 2:
            return "아쉽게도 당첨되지 않았습니다. (2개 일치)"
        elif matched_count == 1:
            return "아쉽게도 당첨되지 않았습니다. (1개 일치)"
        else:
            return "아쉽게도 당첨되지 않았습니다."

def estimate_prize_amount(rank: Optional[int], actual_prize: Optional[int] = None) -> Optional[int]:
    """
    등수별 예상 당첨금 계산
    
    Args:
        rank: 등수 (1~5)
        actual_prize: 실제 당첨금 (DB에 저장된 값)
        
    Returns:
        예상 당첨금 또는 None
    """
    if actual_prize:
        return actual_prize
    
    # 평균 당첨금 (실제와 다를 수 있음)
    average_prizes = {
        1: 2000000000,  # 1등: 약 20억
        2: 50000000,    # 2등: 약 5천만
        3: 1500000,     # 3등: 약 150만
        4: 50000,       # 4등: 약 5만
        5: 5000,        # 5등: 고정 5천원
    }
    
    return average_prizes.get(rank)

def format_prize_amount(amount: Optional[int]) -> str:
    """
    당첨금을 읽기 쉬운 형식으로 변환
    
    Args:
        amount: 당첨금액
        
    Returns:
        포맷된 문자열 (예: "2,000,000,000원")
    """
    if amount is None:
        return "미당첨"
    
    if amount >= 100000000:  # 1억 이상
        eok = amount // 100000000
        remainder = amount % 100000000
        if remainder >= 10000000:  # 천만 이상
            cheonman = remainder // 10000000
            return f"{eok}억 {cheonman}천만원"
        else:
            return f"{eok}억원"
    elif amount >= 10000:  # 1만 이상
        man = amount // 10000
        remainder = amount % 10000
        if remainder >= 1000:  # 천 이상
            cheon = remainder // 1000
            return f"{man}만 {cheon}천원"
        else:
            return f"{man}만원"
    else:
        return f"{amount:,}원"
