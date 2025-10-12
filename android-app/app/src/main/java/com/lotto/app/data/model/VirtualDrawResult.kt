package com.lotto.app.data.model

/**
 * 가상 추첨 결과 데이터 클래스
 */
data class VirtualDrawResult(
    val drawnNumbers: List<Int>,
    val bonusNumber: Int,
    val drawDateTime: String,
    val userResults: List<UserDrawResult>
) {
    /**
     * 총 당첨금 계산
     */
    fun getTotalPrize(): Long {
        return userResults.sumOf { it.getPrizeAmount() }
    }
    
    /**
     * 당첨된 결과만 필터링
     */
    fun getWinningResults(): List<UserDrawResult> {
        return userResults.filter { it.rank != WinningRank.NONE }
    }
    
    /**
     * 가장 높은 등수 반환
     */
    fun getHighestRank(): WinningRank {
        return getWinningResults().minByOrNull { it.rank.ordinal }?.rank ?: WinningRank.NONE
    }
    
    /**
     * 공유용 텍스트 생성
     */
    fun toShareText(): String {
        val drawnNumbersText = drawnNumbers.joinToString(", ")
        val bonusText = bonusNumber.toString()
        
        val resultText = if (getWinningResults().isNotEmpty()) {
            val winningText = getWinningResults().joinToString("\n") { result ->
                "• ${result.memo}: ${result.rank.rank} (${result.rank.estimatedPrize})"
            }
            val totalPrize = getTotalPrize()
            val totalPrizeText = if (totalPrize > 0) {
                "\n💰 총 당첨금: ${formatPrize(totalPrize)}원"
            } else {
                ""
            }
            
            "🎊 당첨 결과:\n$winningText$totalPrizeText"
        } else {
            "😅 아쉽게도 당첨되지 않았습니다"
        }
        
        return """
🎰 가상 로또 추첨 결과

🎲 당첨 번호: $drawnNumbersText
🔹 보너스: $bonusText

$resultText

builder.append("\n━━━━━━━━━━━━━━━━\n")
        builder.append("📱 로또연구소로 더 나은 번호를 찾아보세요!\n")
        builder.append("🍀 행운을 빕니다!")
        """.trimIndent()
    }
    
    private fun formatPrize(amount: Long): String {
        return when {
            amount >= 100000000 -> "${amount / 100000000}억"
            amount >= 10000 -> "${amount / 10000}만"
            else -> amount.toString()
        }
    }
}

/**
 * 사용자별 추첨 결과
 */
data class UserDrawResult(
    val memo: String,
    val userNumbers: List<Int>,
    val matchedNumbers: List<Int>,
    val matchCount: Int,
    val hasBonus: Boolean,
    val rank: WinningRank
) {
    /**
     * 당첨금 계산 (실제 당첨금의 추정치)
     */
    fun getPrizeAmount(): Long {
        return when (rank) {
            WinningRank.FIRST -> 2000000000L  // 20억 (평균)
            WinningRank.SECOND -> 50000000L   // 5천만원
            WinningRank.THIRD -> 1500000L     // 150만원
            WinningRank.FOURTH -> 50000L      // 5만원
            WinningRank.FIFTH -> 5000L        // 5천원
            WinningRank.NONE -> 0L
        }
    }
    
    /**
     * 매치된 번호와 매치되지 않은 번호 분석
     */
    fun getDetailedMatch(): Pair<List<Int>, List<Int>> {
        val matched = userNumbers.filter { it in matchedNumbers }
        val notMatched = userNumbers.filter { it !in matchedNumbers }
        return matched to notMatched
    }
}