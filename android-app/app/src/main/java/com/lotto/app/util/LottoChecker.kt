package com.lotto.app.util

import com.lotto.app.data.model.CheckResult
import com.lotto.app.data.model.LottoResult
import com.lotto.app.data.model.SavedLottoNumber
import com.lotto.app.data.model.WinningRank

/**
 * 당첨 확인 유틸리티
 */
object LottoChecker {
    
    /**
     * 저장된 번호와 당첨 번호 비교
     */
    fun checkWinning(
        savedNumber: SavedLottoNumber,
        result: LottoResult
    ): CheckResult {
        // 일치하는 번호 찾기
        val matchedNumbers = savedNumber.numbers.filter { it in result.winningNumbers }
        val matchCount = matchedNumbers.size
        
        // 보너스 번호 일치 확인
        val bonusMatch = result.bonusNumber in savedNumber.numbers
        
        // 등수 계산
        val rank = WinningRank.fromMatch(matchCount, bonusMatch)
        
        return CheckResult(
            savedNumber = savedNumber,
            result = result,
            matchedNumbers = matchedNumbers,
            matchCount = matchCount,
            bonusMatch = bonusMatch,
            rank = rank
        )
    }
    
    /**
     * 여러 저장된 번호들 일괄 확인
     */
    fun checkMultiple(
        savedNumbers: List<SavedLottoNumber>,
        result: LottoResult
    ): List<CheckResult> {
        return savedNumbers.map { checkWinning(it, result) }
    }
    
    /**
     * 당첨 여부 (3개 이상 맞춤)
     */
    fun isWinning(checkResult: CheckResult): Boolean {
        return checkResult.rank != WinningRank.NONE
    }
    
    /**
     * 고액 당첨 여부 (4등 이상)
     */
    fun isHighPrize(checkResult: CheckResult): Boolean {
        return checkResult.matchCount >= 4
    }
}
