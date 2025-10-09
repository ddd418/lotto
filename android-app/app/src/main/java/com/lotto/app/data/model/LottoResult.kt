package com.lotto.app.data.model

/**
 * 당첨 결과
 */
data class LottoResult(
    val drawNumber: Int,           // 회차
    val winningNumbers: List<Int>,  // 당첨 번호 6개
    val bonusNumber: Int            // 보너스 번호
)

/**
 * 당첨 등수
 */
enum class WinningRank(
    val rank: String,
    val matchCount: Int,
    val hasBonus: Boolean,
    val description: String,
    val estimatedPrize: String
) {
    FIRST("1등", 6, false, "6개 일치", "약 20억원"),
    SECOND("2등", 5, true, "5개 + 보너스", "약 5천만원"),
    THIRD("3등", 5, false, "5개 일치", "약 150만원"),
    FOURTH("4등", 4, false, "4개 일치", "5만원"),
    FIFTH("5등", 3, false, "3개 일치", "5천원"),
    NONE("낙첨", 0, false, "미당첨", "0원");
    
    companion object {
        /**
         * 맞춘 개수와 보너스 일치 여부로 등수 계산
         */
        fun fromMatch(matchCount: Int, bonusMatch: Boolean): WinningRank {
            return when {
                matchCount == 6 -> FIRST
                matchCount == 5 && bonusMatch -> SECOND
                matchCount == 5 -> THIRD
                matchCount == 4 -> FOURTH
                matchCount == 3 -> FIFTH
                else -> NONE
            }
        }
    }
}

/**
 * 당첨 확인 결과
 */
data class CheckResult(
    val savedNumber: SavedLottoNumber,  // 저장된 번호
    val result: LottoResult,            // 당첨 결과
    val matchedNumbers: List<Int>,      // 일치한 번호들
    val matchCount: Int,                // 일치한 개수
    val bonusMatch: Boolean,            // 보너스 일치 여부
    val rank: WinningRank              // 당첨 등수
)
