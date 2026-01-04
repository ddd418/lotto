"""
로또 당첨 번호 크롤링 및 DB 저장 유틸리티
"""
import requests
import json
import logging
import time
import re
from datetime import datetime
from typing import Optional, Dict, List
from sqlalchemy.orm import Session

from models import WinningNumber

logger = logging.getLogger(__name__)

API_URL = "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={drw_no}"
MAIN_PAGE_URL = "https://www.dhlottery.co.kr/common.do?method=main"
NAVER_SEARCH_URL = "https://search.naver.com/search.naver?query=로또+{draw_no}회+당첨번호"

# 세션 재사용 (연결 풀링 및 쿠키 유지)
_session = None
_session_initialized = False
_last_request_time = 0

# Selenium 드라이버 (필요 시 lazy 초기화)
_driver = None
_use_selenium = False  # 봇 차단 시 자동으로 True로 전환
_use_main_page_scraping = False  # API 차단 시 메인 페이지 스크래핑 사용
_use_naver_search = False  # Selenium 실패 시 네이버 검색 사용

def get_session():
    """HTTP 세션 가져오기 (싱글톤)"""
    global _session, _session_initialized
    if _session is None:
        _session = requests.Session()
        _session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7',
            'Accept-Encoding': 'gzip, deflate, br, zstd',
            'Connection': 'keep-alive',
        })
    
    # 첫 API 호출 전에 메인 페이지 방문하여 쿠키 받기
    if not _session_initialized:
        try:
            logger.info("🔄 동행복권 사이트 초기 연결 중...")
            _session.get('https://www.dhlottery.co.kr/', timeout=10)
            time.sleep(0.5)
            _session_initialized = True
            logger.info("✅ 사이트 연결 완료 (쿠키 설정됨)")
        except Exception as e:
            logger.warning(f"⚠️ 사이트 초기 연결 실패 (계속 진행): {e}")
            _session_initialized = True
    
    return _session

def reset_session():
    """세션을 초기화하여 새로운 연결 시도 (봇 차단 해결용)"""
    global _session, _session_initialized, _use_selenium, _use_main_page_scraping, _use_naver_search
    _session = None
    _session_initialized = False
    _use_selenium = False
    _use_main_page_scraping = False
    _use_naver_search = False
    logger.info("🔄 세션 초기화됨")

def fetch_from_naver_search(draw_no: int) -> Optional[Dict]:
    """
    네이버 검색에서 로또 당첨번호 가져오기 (Selenium 없이 requests 사용)
    동행복권 API 차단 시 대안으로 사용
    
    Args:
        draw_no: 로또 회차 번호
        
    Returns:
        당첨 번호 정보 딕셔너리 또는 None
    """
    try:
        logger.info(f"🔍 네이버 검색에서 {draw_no}회차 당첨번호 조회 중...")
        
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7',
            'Referer': 'https://www.naver.com/',
        }
        
        # 네이버 검색 URL - "로또 당첨번호" 또는 "복권 당첨번호"
        search_queries = [
            "로또 당첨번호",
            "복권 당첨번호", 
            f"로또 {draw_no}회 당첨번호"
        ]
        
        html = None
        for query in search_queries:
            try:
                url = f"https://search.naver.com/search.naver?query={requests.utils.quote(query)}"
                response = requests.get(url, headers=headers, timeout=15)
                response.raise_for_status()
                html = response.text
                
                # win_number_box가 있으면 성공
                if 'win_number_box' in html or 'winning_number' in html:
                    logger.info(f"✅ 네이버 검색 성공: {query}")
                    break
            except Exception as e:
                logger.warning(f"⚠️ 검색 실패 ({query}): {e}")
                continue
        
        if not html:
            logger.warning("⚠️ 네이버 검색 결과를 가져올 수 없음")
            return None
        
        # 방법 1: 새로운 네이버 검색 결과 패턴 (win_number_box 구조)
        # <div class="win_number_box">
        #   <div class="winning_number"> <span class="ball type1">1</span>... </div>
        #   <div class="bonus_number"> <span class="ball type1">2</span> </div>
        #   <p class="win_text">1등 당첨금 <strong>3,226,386,263</strong>원 (당첨 복권수 10개)</p>
        # </div>
        
        numbers = []
        bonus = None
        prize_1st = None
        winners_1st = None
        
        # 당첨번호 추출 - winning_number 영역
        winning_pattern = r'winning_number["\']?\s*>([^<]*(?:<[^>]*>[^<]*)*?)</div>'
        winning_match = re.search(winning_pattern, html, re.DOTALL | re.IGNORECASE)
        
        if winning_match:
            winning_html = winning_match.group(1)
            # ball 클래스에서 숫자 추출: <span class="ball type1">1</span>
            ball_pattern = r'ball[^"]*"[^>]*>(\d+)</span>'
            balls = re.findall(ball_pattern, winning_html, re.IGNORECASE)
            numbers = [int(b) for b in balls if 1 <= int(b) <= 45]
            logger.info(f"📊 winning_number에서 추출: {numbers}")
        
        # 보너스 번호 추출 - bonus_number 영역
        bonus_pattern = r'bonus_number["\']?\s*>([^<]*(?:<[^>]*>[^<]*)*?)</div>'
        bonus_match = re.search(bonus_pattern, html, re.DOTALL | re.IGNORECASE)
        
        if bonus_match:
            bonus_html = bonus_match.group(1)
            ball_pattern = r'ball[^"]*"[^>]*>(\d+)</span>'
            bonus_balls = re.findall(ball_pattern, bonus_html, re.IGNORECASE)
            if bonus_balls:
                bonus = int(bonus_balls[0])
                logger.info(f"📊 bonus_number에서 추출: {bonus}")
        
        # 당첨금 및 당첨자 수 추출
        # "1등 당첨금 <strong class="emphasis">3,226,386,263</strong>원 (당첨 복권수 10개)"
        prize_pattern = r'1등\s*당첨금[^<]*<strong[^>]*>([0-9,]+)</strong>원[^(]*\(당첨[^0-9]*(\d+)'
        prize_match = re.search(prize_pattern, html, re.IGNORECASE)
        
        if prize_match:
            prize_str = prize_match.group(1).replace(',', '')
            prize_1st = int(prize_str)
            winners_1st = int(prize_match.group(2))
            logger.info(f"💰 당첨금: {prize_1st:,}원, 당첨자: {winners_1st}명")
        
        # 방법 2: 번호를 찾지 못했으면 더 유연한 패턴 시도
        if len(numbers) < 6 or bonus is None:
            logger.info("🔄 대체 패턴으로 번호 추출 시도...")
            
            # win_ball 전체 영역에서 ball 태그 찾기
            win_ball_pattern = r'win_ball["\']?\s*>([^<]*(?:<[^>]*>[^<]*)*?)</div>\s*</div>'
            win_ball_match = re.search(win_ball_pattern, html, re.DOTALL | re.IGNORECASE)
            
            if win_ball_match:
                win_ball_html = win_ball_match.group(1)
                ball_pattern = r'ball[^"]*"[^>]*>(\d+)</span>'
                all_balls = re.findall(ball_pattern, win_ball_html, re.IGNORECASE)
                all_balls = [int(b) for b in all_balls if 1 <= int(b) <= 45]
                
                if len(all_balls) >= 7:
                    numbers = all_balls[:6]
                    bonus = all_balls[6]
                    logger.info(f"📊 win_ball에서 추출: {numbers} + 보너스 {bonus}")
        
        # 방법 3: 여전히 못 찾았으면 전체 HTML에서 ball 클래스 찾기
        if len(numbers) < 6 or bonus is None:
            ball_pattern = r'<span[^>]*class="ball[^"]*"[^>]*>(\d+)</span>'
            all_balls = re.findall(ball_pattern, html, re.IGNORECASE)
            all_balls = [int(b) for b in all_balls if 1 <= int(b) <= 45]
            
            if len(all_balls) >= 7:
                numbers = all_balls[:6]
                bonus = all_balls[6]
                logger.info(f"📊 전체 HTML에서 추출: {numbers} + 보너스 {bonus}")
        
        # 유효성 검사
        if len(numbers) < 6:
            logger.warning(f"⚠️ 당첨번호 부족: {numbers}")
            return None
        
        if bonus is None:
            logger.warning("⚠️ 보너스 번호를 찾을 수 없음")
            return None
        
        if not all(1 <= n <= 45 for n in numbers) or not (1 <= bonus <= 45):
            logger.warning(f"⚠️ 파싱된 번호가 유효하지 않음: {numbers} + {bonus}")
            return None
        
        # 회차 번호 확인 (선택적) - 네이버 검색 결과에 회차가 표시될 수 있음
        draw_found = draw_no
        draw_date = None
        
        # 회차 패턴: "1205회" 또는 "1205회차"
        draw_no_pattern = r'(\d{4})회'
        draw_no_match = re.search(draw_no_pattern, html)
        if draw_no_match:
            draw_found = int(draw_no_match.group(1))
            logger.info(f"📊 검색결과 회차: {draw_found}회")
        
        # 날짜 패턴: "2026.01.03" 또는 "(2026.01.03 추첨)"
        date_pattern = r'\((\d{4}\.\d{2}\.\d{2})\s*추첨?\)'
        date_match = re.search(date_pattern, html)
        if date_match:
            draw_date = date_match.group(1).replace('.', '-')
        else:
            # 날짜가 없으면 오늘 또는 직전 토요일 계산
            from datetime import datetime, timedelta
            today = datetime.now()
            days_since_saturday = (today.weekday() + 2) % 7
            last_saturday = today - timedelta(days=days_since_saturday)
            draw_date = last_saturday.strftime('%Y-%m-%d')
        
        result = {
            'drwNo': draw_found,
            'drwNoDate': draw_date,
            'drwtNo1': numbers[0],
            'drwtNo2': numbers[1],
            'drwtNo3': numbers[2],
            'drwtNo4': numbers[3],
            'drwtNo5': numbers[4],
            'drwtNo6': numbers[5],
            'bnusNo': bonus,
            'returnValue': 'success'
        }
        
        # 당첨금 정보 추가 (있으면)
        if prize_1st:
            result['firstWinamnt'] = prize_1st
        if winners_1st:
            result['firstPrzwnerCo'] = winners_1st
        
        logger.info(f"✅ {draw_found}회차 네이버에서 가져오기 성공: {numbers} + 보너스 {bonus}")
        return result
        
    except requests.exceptions.RequestException as e:
        logger.error(f"❌ 네이버 검색 네트워크 오류: {e}")
        return None
    except Exception as e:
        logger.error(f"❌ 네이버 검색 파싱 오류: {e}")
        return None

def fetch_from_main_page() -> List[Dict]:
    """
    동행복권 메인 페이지에서 최근 당첨 번호 스크래핑 (Selenium 사용)
    2026년부터 API 차단으로 인한 대안
    
    Returns:
        최근 회차 당첨 정보 리스트 (최신순)
    """
    try:
        from selenium import webdriver
        from selenium.webdriver.chrome.options import Options
        from selenium.webdriver.chrome.service import Service
        
        logger.info("🌐 Selenium으로 메인 페이지 스크래핑 시작...")
        
        options = Options()
        # Docker/Railway 환경 필수 옵션들
        options.add_argument('--headless=new')  # 새로운 headless 모드
        options.add_argument('--no-sandbox')
        options.add_argument('--disable-dev-shm-usage')
        options.add_argument('--disable-gpu')
        options.add_argument('--disable-extensions')
        options.add_argument('--disable-software-rasterizer')
        options.add_argument('--single-process')  # Docker에서 중요
        options.add_argument('--disable-setuid-sandbox')
        options.add_argument('--disable-background-networking')
        options.add_argument('--disable-default-apps')
        options.add_argument('--disable-sync')
        options.add_argument('--disable-translate')
        options.add_argument('--metrics-recording-only')
        options.add_argument('--no-first-run')
        options.add_argument('--safebrowsing-disable-auto-update')
        options.add_argument('--window-size=1920,1080')
        options.add_argument('user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36')
        
        # 서버 환경에서 Chrome 경로 설정
        import os
        chrome_path = None
        if os.path.exists('/usr/bin/google-chrome'):
            chrome_path = '/usr/bin/google-chrome'
            options.binary_location = chrome_path
        elif os.path.exists('/usr/bin/google-chrome-stable'):
            chrome_path = '/usr/bin/google-chrome-stable'
            options.binary_location = chrome_path
        
        # ChromeDriver 서비스 설정
        service = None
        chromedriver_path = '/usr/local/bin/chromedriver'
        if os.path.exists(chromedriver_path):
            service = Service(executable_path=chromedriver_path)
            logger.info(f"🔧 ChromeDriver 경로: {chromedriver_path}")
        
        if chrome_path:
            logger.info(f"🔧 Chrome 경로: {chrome_path}")
        
        # WebDriver 생성 (타임아웃 연장)
        if service:
            driver = webdriver.Chrome(service=service, options=options)
        else:
            driver = webdriver.Chrome(options=options)
        
        driver.set_page_load_timeout(60)  # 페이지 로드 타임아웃 60초
        driver.get(MAIN_PAGE_URL)
        time.sleep(3)  # JavaScript 렌더링 대기
        
        html = driver.page_source
        driver.quit()
        
        results = []
        
        # 회차별로 데이터 추출
        # 패턴: 1205회</div><div class="lt645-date">2026.01.03</div>...lt-ball num-Xn>숫자</span>
        draw_pattern = r'(\d{4})회</div><div class="lt645-date">(\d{4}\.\d{2}\.\d{2})</div>.*?lt645-numBox.*?(<div class="lt645-list">.*?</div>)</div>'
        
        matches = re.finditer(draw_pattern, html, re.DOTALL)
        
        for match in matches:
            draw_no = int(match.group(1))
            draw_date = match.group(2).replace('.', '-')
            ball_section = match.group(3)
            
            # 번호 추출: <span class="lt-ball num-Xn">숫자</span>
            # 플러스 이미지 전후로 메인 번호와 보너스 번호 구분
            ball_pattern = r'lt-ball num-\d+n">(\d+)</span>'
            balls = re.findall(ball_pattern, ball_section)
            
            if len(balls) >= 7:
                result = {
                    'drwNo': draw_no,
                    'drwNoDate': draw_date,
                    'drwtNo1': int(balls[0]),
                    'drwtNo2': int(balls[1]),
                    'drwtNo3': int(balls[2]),
                    'drwtNo4': int(balls[3]),
                    'drwtNo5': int(balls[4]),
                    'drwtNo6': int(balls[5]),
                    'bnusNo': int(balls[6]),
                    'returnValue': 'success'
                }
                results.append(result)
                logger.info(f"✅ {draw_no}회차 추출: [{balls[0]}, {balls[1]}, {balls[2]}, {balls[3]}, {balls[4]}, {balls[5]}] + 보너스 {balls[6]}")
        
        if not results:
            logger.warning("⚠️ 메인 페이지에서 당첨번호를 찾지 못했습니다")
        
        return results
        
    except ImportError:
        logger.error("❌ Selenium이 설치되지 않았습니다. pip install selenium 실행 필요")
        return []
    except Exception as e:
        logger.error(f"❌ 메인 페이지 스크래핑 실패: {e}")
        return []

def fetch_winning_number(draw_no: int) -> Optional[Dict]:
    """
    로또 당첨 번호 가져오기
    2026년부터 동행복권 API 차단으로 네이버 검색을 기본으로 사용
    
    Args:
        draw_no: 로또 회차 번호
        
    Returns:
        당첨 번호 정보 딕셔너리 또는 None (실패 시)
    """
    # 네이버 검색으로 당첨번호 가져오기 (기본 방식)
    result = fetch_from_naver_search(draw_no)
    if result:
        return result
    
    # 네이버 실패 시 동행복권 API 시도 (백업)
    logger.info(f"🔄 네이버 검색 실패 - 동행복권 API 시도 중...")
    url = API_URL.format(drw_no=draw_no)
    try:
        session = get_session()
        response = session.get(url, timeout=10)
        response.raise_for_status()
        
        # 응답이 JSON인지 확인
        content_type = response.headers.get('Content-Type', '')
        if 'application/json' not in content_type and not response.text.strip().startswith('{'):
            logger.warning(f"⚠️ 동행복권 API 차단됨")
            return None
        
        obj = response.json()
        
        if obj.get("returnValue") == "success":
            logger.info(f"✅ {draw_no}회차 당첨 번호 가져오기 성공 (동행복권 API)")
            return obj
        else:
            logger.warning(f"❌ {draw_no}회차 당첨 번호 없음 (아직 추첨 전이거나 잘못된 회차)")
            return None
    except requests.exceptions.RequestException as e:
        logger.error(f"❌ {draw_no}회차 네트워크 오류: {e}")
        return None
    except json.JSONDecodeError as e:
        logger.warning(f"⚠️ 동행복권 API JSON 파싱 실패")
        return None
    except Exception as e:
        logger.error(f"❌ {draw_no}회차 API 호출 실패: {e}")
        return None

# 메인 페이지에서 가져온 당첨번호 캐시
_main_page_cache = {}
_main_page_cache_time = 0

def fetch_winning_number_from_cache(draw_no: int) -> Optional[Dict]:
    """
    메인 페이지 스크래핑 캐시에서 당첨번호 조회
    캐시가 없거나 오래되면 새로 스크래핑
    Selenium 실패 시 네이버 검색으로 폴백
    """
    global _main_page_cache, _main_page_cache_time, _use_naver_search
    
    # 네이버 검색 모드가 활성화되어 있으면 바로 네이버 사용
    if _use_naver_search:
        return fetch_from_naver_search(draw_no)
    
    current_time = time.time()
    
    # 캐시가 5분 이상 오래되었으면 새로 가져오기
    if current_time - _main_page_cache_time > 300 or draw_no not in _main_page_cache:
        logger.info("🔄 메인 페이지에서 최신 당첨번호 스크래핑 중...")
        results = fetch_from_main_page()
        
        # Selenium 실패 시 네이버 검색으로 폴백
        if not results:
            logger.warning("⚠️ Selenium 스크래핑 실패 - 네이버 검색으로 전환합니다")
            _use_naver_search = True
            return fetch_from_naver_search(draw_no)
        
        # 캐시 업데이트
        _main_page_cache = {r['drwNo']: r for r in results}
        _main_page_cache_time = current_time
        
        if results:
            logger.info(f"✅ 메인 페이지에서 {len(results)}개 회차 정보 획득")
    
    # 캐시에서 조회
    if draw_no in _main_page_cache:
        logger.info(f"✅ {draw_no}회차 당첨 번호 가져오기 성공 (메인페이지)")
        return _main_page_cache[draw_no]
    else:
        # 캐시에 없으면 네이버 검색 시도
        logger.info(f"ℹ️ {draw_no}회차가 캐시에 없음 - 네이버 검색 시도")
        return fetch_from_naver_search(draw_no)

def save_winning_number_to_db(db: Session, draw_data: Dict) -> Optional[WinningNumber]:
    """
    동행복권 API 응답을 DB에 저장
    
    Args:
        db: SQLAlchemy DB 세션
        draw_data: API에서 받은 회차 정보
        
    Returns:
        저장된 WinningNumber 객체 또는 None
    """
    try:
        draw_no = draw_data.get("drwNo")
        
        # 이미 DB에 있는지 확인
        existing = db.query(WinningNumber).filter(
            WinningNumber.draw_number == draw_no
        ).first()
        
        if existing:
            logger.info(f"⏭️  {draw_no}회차는 이미 DB에 저장되어 있음")
            return existing
        
        # 추첨일 파싱
        draw_date_str = draw_data.get("drwNoDate")  # "2024-01-06" 형식
        draw_date = None
        if draw_date_str:
            try:
                draw_date = datetime.strptime(draw_date_str, "%Y-%m-%d")
            except:
                pass
        
        # WinningNumber 객체 생성
        winning_number = WinningNumber(
            draw_number=draw_no,
            number1=draw_data.get("drwtNo1"),
            number2=draw_data.get("drwtNo2"),
            number3=draw_data.get("drwtNo3"),
            number4=draw_data.get("drwtNo4"),
            number5=draw_data.get("drwtNo5"),
            number6=draw_data.get("drwtNo6"),
            bonus_number=draw_data.get("bnusNo"),
            prize_1st=draw_data.get("firstWinamnt"),
            prize_2nd=draw_data.get("secondWinamnt"),
            prize_3rd=draw_data.get("thirdWinamnt"),
            prize_4th=draw_data.get("fourthWinamnt"),
            prize_5th=draw_data.get("fifthWinamnt"),
            winners_1st=draw_data.get("firstPrzwnerCo"),
            winners_2nd=draw_data.get("secondPrzwnerCo"),
            winners_3rd=draw_data.get("thirdPrzwnerCo"),
            winners_4th=draw_data.get("fourthPrzwnerCo"),
            winners_5th=draw_data.get("fifthPrzwnerCo"),
            total_sales=draw_data.get("totSellamnt"),
            draw_date=draw_date
        )
        
        db.add(winning_number)
        db.commit()
        db.refresh(winning_number)
        
        logger.info(f"💾 {draw_no}회차 DB 저장 완료: [{winning_number.number1}, {winning_number.number2}, {winning_number.number3}, {winning_number.number4}, {winning_number.number5}, {winning_number.number6}] + 보너스 {winning_number.bonus_number}")
        
        return winning_number
        
    except Exception as e:
        logger.error(f"❌ DB 저장 실패: {e}")
        db.rollback()
        return None

def get_latest_draw_number(start_from: Optional[int] = None) -> Optional[int]:
    """
    현재 최신 회차 번호 추정 (연속 실패 방식)
    lott.py의 collect_stats 로직 참고
    
    Args:
        start_from: 검색 시작 회차 (None이면 1회차부터, 값이 있으면 해당 회차+1부터 검색하여 최신 회차 찾기)
    
    Returns:
        최신 회차 번호 또는 None
    """
    # start_from이 있으면 그 다음 회차부터 검색 (증분 업데이트용)
    if start_from and start_from > 0:
        start_draw = start_from + 1
        logger.info(f"🔍 최신 회차 검색 시작 ({start_draw}회차부터, 연속 실패 5회까지)")
    else:
        start_draw = 1
        logger.info(f"🔍 최신 회차 검색 시작 (1회차부터 연속 실패 5회까지)")
    
    fail_streak = 0
    last_success_draw = start_from if start_from else 0  # start_from을 초기값으로 설정
    drw_no = start_draw
    
    # 2026년부터 동행복권 API 차단으로 인한 특별 처리
    consecutive_api_errors = 0
    
    while True:
        data = fetch_winning_number(drw_no)
        if not data:
            fail_streak += 1
            consecutive_api_errors += 1
            logger.debug(f"  {drw_no}회차 실패 (연속 실패: {fail_streak}회)")
            
            # API가 완전히 차단된 경우 (연속 3회 이상 실패)
            if consecutive_api_errors >= 3 and last_success_draw == start_from:
                logger.error("❌ 동행복권 API 접근이 차단되었습니다")
                logger.error("💡 2026년부터 동행복권이 외부 접근을 차단한 것으로 보입니다")
                logger.info(f"📦 DB에 저장된 최신 회차({start_from}회)를 계속 사용합니다")
                return start_from  # 기존 회차 반환
            
            if fail_streak >= 5:
                if last_success_draw > 0:
                    logger.info(f"🎯 최신 회차 확정: {last_success_draw}회 (연속 실패 5회 도달)")
                else:
                    logger.warning(f"⚠️ 최신 회차를 찾을 수 없습니다 (API 차단 가능성)")
                break
        else:
            fail_streak = 0
            consecutive_api_errors = 0
            last_success_draw = drw_no
            if drw_no % 100 == 0 or (start_from and drw_no == start_draw):
                logger.info(f"  ✅ {drw_no}회차 확인됨...")
        
        drw_no += 1
        
        # 무한루프 방지 (2030년까지 약 1500회차 예상)
        if drw_no > 2000:
            logger.warning(f"⚠️ 2000회차 도달, 검색 중단")
            break
    
    return last_success_draw if last_success_draw > 0 else None

def sync_all_winning_numbers(db: Session, start_draw: int = 1, end_draw: Optional[int] = None) -> Dict:
    """
    특정 범위의 당첨 번호를 모두 DB에 동기화
    
    Args:
        db: SQLAlchemy DB 세션
        start_draw: 시작 회차
        end_draw: 종료 회차 (None이면 최신 회차까지)
        
    Returns:
        통계 정보 딕셔너리
    """
    if end_draw is None:
        end_draw = get_latest_draw_number()
        if end_draw is None:
            logger.error("❌ 최신 회차를 찾을 수 없습니다")
            return {"success": False, "error": "최신 회차를 찾을 수 없음"}
    
    logger.info(f"🔄 {start_draw}회 ~ {end_draw}회 동기화 시작")
    
    success_count = 0
    skip_count = 0
    fail_count = 0
    
    for draw_no in range(start_draw, end_draw + 1):
        # DB에 이미 있는지 확인
        existing = db.query(WinningNumber).filter(
            WinningNumber.draw_number == draw_no
        ).first()
        
        if existing:
            skip_count += 1
            if draw_no % 100 == 0:
                logger.info(f"⏭️  {draw_no}회차 스킵 (이미 존재)")
            continue
        
        # API에서 가져오기
        draw_data = fetch_winning_number(draw_no)
        if not draw_data:
            fail_count += 1
            continue
        
        # DB에 저장
        result = save_winning_number_to_db(db, draw_data)
        if result:
            success_count += 1
        else:
            fail_count += 1
    
    logger.info(f"✅ 동기화 완료: 성공 {success_count}개, 스킵 {skip_count}개, 실패 {fail_count}개")
    
    return {
        "success": True,
        "success_count": success_count,
        "skip_count": skip_count,
        "fail_count": fail_count,
        "total": end_draw - start_draw + 1
    }

def get_or_fetch_winning_number(db: Session, draw_no: int) -> Optional[WinningNumber]:
    """
    DB에서 당첨 번호 조회, 없으면 API에서 가져와서 저장
    
    Args:
        db: SQLAlchemy DB 세션
        draw_no: 회차 번호
        
    Returns:
        WinningNumber 객체 또는 None
    """
    # 1. DB 조회
    winning = db.query(WinningNumber).filter(
        WinningNumber.draw_number == draw_no
    ).first()
    
    if winning:
        logger.info(f"📦 {draw_no}회차 DB에서 조회 성공")
        return winning
    
    # 2. API에서 가져오기
    logger.info(f"🌐 {draw_no}회차 API에서 가져오는 중...")
    draw_data = fetch_winning_number(draw_no)
    
    if not draw_data:
        return None
    
    # 3. DB에 저장
    return save_winning_number_to_db(db, draw_data)

def get_latest_winning_numbers(db: Session, count: int = 10) -> List[WinningNumber]:
    """
    최신 당첨 번호 N개 조회
    
    Args:
        db: SQLAlchemy DB 세션
        count: 조회할 개수
        
    Returns:
        WinningNumber 리스트 (최신순)
    """
    return db.query(WinningNumber).order_by(
        WinningNumber.draw_number.desc()
    ).limit(count).all()
