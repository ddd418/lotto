# lotto_cli.py
import json
import time
import random
import urllib.request
from collections import Counter
from typing import Optional, List, Dict, Tuple
from pathlib import Path
from datetime import datetime

# tqdm이 없으면 깔끔히 fallback
try:
    from tqdm import tqdm
except Exception:
    def tqdm(x, **kwargs):
        return x

API_URL = "https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={drw_no}"
STATS_PATH = Path("lotto_stats.json")   # 빈도/메타 저장
DRAWS_PATH = Path("lotto_draws.json")   # 회차별 원본 JSON 저장 (옵션)
LOTTO_MIN, LOTTO_MAX = 1, 45

# -----------------------------
# 1) 데이터 수집/파싱 유틸
# -----------------------------
def fetch_draw_json(drw_no: int) -> Optional[dict]:
    url = API_URL.format(drw_no=drw_no)
    try:
        with urllib.request.urlopen(url, timeout=5) as resp:
            data = resp.read().decode("utf-8")
            obj = json.loads(data)
            if obj.get("returnValue") == "success":
                return obj
            return None
    except Exception:
        return None

def extract_main_numbers(draw_obj: dict) -> List[int]:
    return [draw_obj.get(f"drwtNo{i}") for i in range(1, 7)]

def extract_bonus_number(draw_obj: dict) -> Optional[int]:
    return draw_obj.get("bnusNo")

def collect_stats(max_draw: Optional[int] = None,
                  include_bonus: bool = False,
                  rest_every: int = 50,
                  rest_sec: float = 0.3) -> Tuple[Counter, int, Dict[int, Dict]]:
    """
    max_draw=None이면 1회부터 '연속 실패 n회' 발생 시 종료(최신까지 추정) 방식.
    max_draw가 숫자면 1~max_draw까지 수집.
    """
    freq = Counter()
    draws_store: Dict[int, Dict] = {}
    last_success_draw = 0

    # 수집 범위 결정
    if max_draw is None:
        # 연속 실패가 몇 번 누적되면(예: 5회) 더 이상 회차가 없다고 판단
        fail_streak = 0
        drw_no = 1
        with tqdm(total=0, desc="로또 회차 자동 수집(최신까지 추정)") as _:
            while True:
                obj = fetch_draw_json(drw_no)
                if not obj:
                    fail_streak += 1
                    if fail_streak >= 5:
                        break
                else:
                    fail_streak = 0
                    last_success_draw = drw_no
                    draws_store[drw_no] = obj
                    freq.update(extract_main_numbers(obj))
                    if include_bonus:
                        bn = extract_bonus_number(obj)
                        if bn is not None:
                            freq.update([bn])

                if drw_no % rest_every == 0:
                    time.sleep(rest_sec)
                drw_no += 1
    else:
        for drw_no in tqdm(range(1, max_draw + 1), desc="로또 회차 수집 중"):
            obj = fetch_draw_json(drw_no)
            if not obj:
                continue
            last_success_draw = drw_no
            draws_store[drw_no] = obj
            freq.update(extract_main_numbers(obj))
            if include_bonus:
                bn = extract_bonus_number(obj)
                if bn is not None:
                    freq.update([bn])
            if drw_no % rest_every == 0:
                time.sleep(rest_sec)

    return freq, last_success_draw, draws_store

# -----------------------------
# 2) 저장/로드
# -----------------------------
def save_stats(freq: Counter, last_draw: int, include_bonus: bool, draws_store: Dict[int, Dict]):
    stats = {
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "include_bonus": include_bonus,
        "last_draw": last_draw,
        "frequency": dict(freq),  # {번호: 빈도}
    }
    STATS_PATH.write_text(json.dumps(stats, ensure_ascii=False, indent=2))

    # 원본 회차 데이터는 용량이 크면 생략 가능
    try:
        DRAWS_PATH.write_text(json.dumps(draws_store, ensure_ascii=False))
    except Exception:
        pass

def load_stats() -> Optional[dict]:
    if not STATS_PATH.exists():
        return None
    try:
        return json.loads(STATS_PATH.read_text(encoding="utf-8"))
    except Exception:
        return None

# -----------------------------
# 3) 추천 번호 생성 로직
# -----------------------------
def weighted_sample_without_replacement(population: List[int],
                                        weights: List[float],
                                        k: int) -> List[int]:
    """
    단순 가중치 비복원 샘플링: 한 개씩 뽑고 해당 항목 가중치를 0으로 설정.
    numpy 없이 구현.
    """
    assert len(population) == len(weights)
    chosen = []
    weights_work = weights[:]
    for _ in range(k):
        total = sum(weights_work)
        if total <= 0:
            # 모든 weight가 0이면 균등으로 대체
            candidates = [x for x, w in zip(population, weights_work) if w >= 0]
            pick = random.choice(candidates)
            chosen.append(pick)
            weights_work[population.index(pick)] = 0.0
            continue
        r = random.random() * total
        acc = 0.0
        idx = 0
        for i, w in enumerate(weights_work):
            acc += w
            if r <= acc:
                idx = i
                break
        pick = population[idx]
        chosen.append(pick)
        weights_work[idx] = 0.0
    return chosen

def is_plausible(nums: List[int]) -> bool:
    """
    '그럴듯함' 휴리스틱:
    - 정렬 후 연속 숫자 4개 이상 금지 (3개는 허용)
    - 동일 decade(1~10, 11~20, ...)에 4개 이상 몰림 금지
    - 짝수/홀수 균형: 짝수 2~4개 범위 선호
    - 합계 90~210 선호 (범위 확대)
    """
    s = sorted(nums)
    
    # 연속 4개 이상 금지
    streak = 1
    for i in range(1, len(s)):
        if s[i] == s[i-1] + 1:
            streak += 1
            if streak >= 4:  # 4개 이상 연속은 거부
                return False
        else:
            streak = 1

    # decade 몰림 체크 (4개 이상 금지)
    decades = Counter([(n-1)//10 for n in s])
    if any(v >= 4 for v in decades.values()):
        return False

    # 짝/홀 밸런스
    evens = sum(1 for n in s if n % 2 == 0)
    if not (2 <= evens <= 4):
        return False

    # 합계 범위 (좀 더 넓게)
    total = sum(s)
    if not (90 <= total <= 210):
        return False

    return True

def build_weights_from_frequency(freq_map: Dict[str, int]) -> List[float]:
    """
    과거 출현 빈도 → 가중치.
    개선: 상위 30개만 높은 가중치, 나머지는 매우 낮은 가중치
    """
    # 모든 번호의 빈도 가져오기
    freq_list = []
    for n in range(LOTTO_MIN, LOTTO_MAX+1):
        count = freq_map.get(str(n), 0) if isinstance(next(iter(freq_map.keys())), str) else freq_map.get(n, 0)
        freq_list.append((n, count))
    
    # 빈도 높은 순으로 정렬
    freq_list.sort(key=lambda x: x[1], reverse=True)
    
    # 상위 30개는 실제 빈도 기반, 나머지는 매우 낮은 가중치
    weights_dict = {}
    for i, (num, count) in enumerate(freq_list):
        if i < 30:  # 상위 30개
            # 빈도에 비례하는 가중치 (제곱으로 차이 극대화)
            weights_dict[num] = (count ** 1.5) + 10
        else:  # 하위 15개
            # 매우 낮은 가중치 (거의 선택되지 않음)
            weights_dict[num] = 0.1
    
    # 1~45 순서대로 가중치 리스트 반환
    weights = [weights_dict[n] for n in range(LOTTO_MIN, LOTTO_MAX+1)]
    return weights

def recommend_sets(stats: dict, n_sets: int = 5, seed: Optional[int] = None, mode: str = "ai") -> List[List[int]]:
    """
    빈도 기반 가중 샘플링 + 휴리스틱 필터로 6개 번호 x n_sets 추천.
    
    mode:
        - "ai": AI 추천 (기본, 상위 30개 가중치)
        - "random": 완전 랜덤
        - "conservative": 보수적 (상위 15개만)
        - "aggressive": 공격적 (하위 번호도 포함)
    """
    if seed is not None:
        random.seed(seed)

    freq_map = stats["frequency"]
    population = list(range(LOTTO_MIN, LOTTO_MAX+1))
    
    # 모드별 가중치 설정
    if mode == "random":
        # 완전 랜덤: 모든 번호 동일 가중치
        base_weights = [1.0] * 45
    elif mode == "conservative":
        # 보수적: 상위 15개만 높은 가중치
        freq_list = [(n, freq_map.get(str(n), 0) if isinstance(next(iter(freq_map.keys())), str) else freq_map.get(n, 0))
                     for n in range(LOTTO_MIN, LOTTO_MAX+1)]
        freq_list.sort(key=lambda x: x[1], reverse=True)
        weights_dict = {}
        for i, (num, count) in enumerate(freq_list):
            if i < 15:  # 상위 15개
                weights_dict[num] = (count ** 2) + 50
            else:
                weights_dict[num] = 0.01
        base_weights = [weights_dict[n] for n in range(LOTTO_MIN, LOTTO_MAX+1)]
    elif mode == "aggressive":
        # 공격적: 하위 번호도 적극 활용
        freq_list = [(n, freq_map.get(str(n), 0) if isinstance(next(iter(freq_map.keys())), str) else freq_map.get(n, 0))
                     for n in range(LOTTO_MIN, LOTTO_MAX+1)]
        freq_list.sort(key=lambda x: x[1], reverse=True)
        weights_dict = {}
        for i, (num, count) in enumerate(freq_list):
            if i < 35:  # 상위 35개
                weights_dict[num] = count + 5
            else:  # 하위 10개도 선택 가능
                weights_dict[num] = 3.0
        base_weights = [weights_dict[n] for n in range(LOTTO_MIN, LOTTO_MAX+1)]
    else:  # "ai" (기본)
        base_weights = build_weights_from_frequency(freq_map)

    results = []
    attempts_cap = 300  # 각 세트당 최대 시도
    for _ in range(n_sets):
        ok = False
        attempts = 0
        while not ok and attempts < attempts_cap:
            attempts += 1
            nums = weighted_sample_without_replacement(population, base_weights, 6)
            nums = sorted(nums)
            if is_plausible(nums):
                ok = True
                results.append(nums)
                break
        if not ok:
            # 필터 통과 실패 시 마지막 샘플이라도 채택
            results.append(sorted(nums))
    return results

# -----------------------------
# 4) 출력/정렬 유틸
# -----------------------------
def print_rank(freq: Counter, last_draw: int, include_bonus: bool, top_n: int = 20):
    ranked = sorted(freq.items(), key=lambda x: (-x[1], x[0]))
    limit = min(top_n, len(ranked))
    print(f"\n📊 1~{last_draw}회차 통계 (보너스 포함: {include_bonus}) 상위 {limit}개\n")
    for i, (num, count) in enumerate(ranked[:limit], start=1):
        print(f"{i:>2}위: {num:>2}번  - {count}회")

def print_recommendations(sets: List[List[int]], last_draw: int):
    print(f"\n🎯 {last_draw}회차까지 데이터 기반 추천 번호 (5세트)\n")
    for i, s in enumerate(sets,  start=1):
        print(f"{i}) " + " ".join(f"{n:02d}" for n in s))

# -----------------------------
# 5) 메뉴/메인 진입점
# -----------------------------
def run_collect_and_save(max_draw: Optional[int] = None, include_bonus: bool = False):
    freq, last_draw, draws_store = collect_stats(max_draw=max_draw, include_bonus=include_bonus)
    save_stats(freq, last_draw, include_bonus, draws_store)
    print_rank(freq, last_draw, include_bonus, top_n=20)
    print("\n✅ 수집/저장 완료:", STATS_PATH.resolve())

def run_recommend():
    stats = load_stats()
    if not stats:
        print("저장된 통계가 없습니다. 먼저 데이터를 수집하세요(메뉴 1).")
        return
    sets = recommend_sets(stats, n_sets=5)
    print_recommendations(sets, stats.get("last_draw", 0))

def main():
    # 첫 실행: 파일 없으면 자동 수집 유도
    if not STATS_PATH.exists():
        print("처음 실행으로 감지되었습니다. 회차 데이터를 수집/저장합니다.")
        # max_draw=None → 1회부터 최신까지 추정
        run_collect_and_save(max_draw=None, include_bonus=False)
        return

    # 이후 실행: 메뉴 제공
    print("저장된 통계를 확인했습니다.")
    print("1. 새로 수집/갱신")
    print("2. 로또번호 추천(파일 기반)")
    print("3. 종료")
    choice = input("선택(1/2/3): ").strip()

    if choice == "1":
        # 최신까지 갱신: None으로 두면 자동으로 이어서 최신 추정 수집
        run_collect_and_save(max_draw=None, include_bonus=False)
    elif choice == "2":
        run_recommend()
    else:
        print("종료합니다.")

if __name__ == "__main__":
    main()
