package com.lotto.app.util

import com.lotto.app.data.local.SavedNumbersManager
import com.lotto.app.data.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 가상 추첨 유틸리티
 */
object VirtualDrawUtil {
    
    /**
     * 가상 추첨 실행
     */
    fun performVirtualDraw(savedNumbersManager: SavedNumbersManager): VirtualDrawResult {
        // 1~45에서 6개 번호 무작위 선택
        val drawnNumbers = (1..45).shuffled().take(6).sorted()
        
        // 보너스 번호 (선택된 6개 제외)
        val remainingNumbers = (1..45).filter { it !in drawnNumbers }
        val bonusNumber = remainingNumbers.random()
        
        // 현재 시간
        val drawDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        // 저장된 번호들과 비교
        val savedNumbers = savedNumbersManager.getSavedNumbers()
        val userResults = savedNumbers.map { savedNumber ->
            analyzeUserResult(savedNumber, drawnNumbers, bonusNumber)
        }
        
        return VirtualDrawResult(
            drawnNumbers = drawnNumbers,
            bonusNumber = bonusNumber,
            drawDateTime = drawDateTime,
            userResults = userResults
        )
    }
    
    /**
     * 사용자 번호 분석
     */
    private fun analyzeUserResult(
        savedNumber: SavedLottoNumber,
        drawnNumbers: List<Int>,
        bonusNumber: Int
    ): UserDrawResult {
        // 매치된 번호 찾기
        val matchedNumbers = savedNumber.numbers.filter { it in drawnNumbers }
        val matchCount = matchedNumbers.size
        
        // 보너스 번호 매치 확인
        val hasBonus = bonusNumber in savedNumber.numbers
        
        // 당첨 등수 계산
        val rank = calculateRank(matchCount, hasBonus)
        
        return UserDrawResult(
            memo = savedNumber.memo.ifEmpty { "번호 ${savedNumber.id}" },
            userNumbers = savedNumber.numbers,
            matchedNumbers = matchedNumbers,
            matchCount = matchCount,
            hasBonus = hasBonus,
            rank = rank
        )
    }
    
    /**
     * 당첨 등수 계산
     */
    private fun calculateRank(matchCount: Int, hasBonus: Boolean): WinningRank {
        return when {
            matchCount == 6 -> WinningRank.FIRST
            matchCount == 5 && hasBonus -> WinningRank.SECOND
            matchCount == 5 -> WinningRank.THIRD
            matchCount == 4 -> WinningRank.FOURTH
            matchCount == 3 -> WinningRank.FIFTH
            else -> WinningRank.NONE
        }
    }
    
    /**
     * 상세 매치 정보 생성
     */
    fun getMatchDetails(userResult: UserDrawResult, drawnNumbers: List<Int>, bonusNumber: Int): String {
        val (matched, notMatched) = userResult.getDetailedMatch()
        
        return buildString {
            append("📊 상세 분석\n")
            append("• 선택한 번호: ${userResult.userNumbers.joinToString(", ")}\n")
            append("• 당첨 번호: ${drawnNumbers.joinToString(", ")}\n")
            append("• 보너스: $bonusNumber\n\n")
            
            if (matched.isNotEmpty()) {
                append("✅ 맞춘 번호: ${matched.joinToString(", ")} (${matched.size}개)\n")
            }
            
            if (notMatched.isNotEmpty()) {
                append("❌ 틀린 번호: ${notMatched.joinToString(", ")} (${notMatched.size}개)\n")
            }
            
            if (userResult.hasBonus) {
                append("🔹 보너스 번호 일치!\n")
            }
            
            append("\n🏆 결과: ${userResult.rank.rank}")
            
            if (userResult.rank != WinningRank.NONE) {
                append("\n💰 예상 당첨금: ${formatPrize(userResult.getPrizeAmount())}원")
            }
        }
    }
    
    /**
     * 당첨금 포맷팅
     */
    private fun formatPrize(amount: Long): String {
        return when {
            amount >= 100000000 -> String.format("%,d", amount / 100000000) + "억"
            amount >= 10000 -> String.format("%,d", amount / 10000) + "만"
            else -> String.format("%,d", amount)
        }
    }
}