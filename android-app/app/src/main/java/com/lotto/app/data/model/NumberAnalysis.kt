package com.lotto.app.data.model

/**
 * 번호 분석 결과
 */
data class NumberAnalysis(
    val totalSets: Int,                          // 총 저장된 세트 수
    val numberFrequency: Map<Int, Int>,          // 번호별 출현 횟수 (1~45)
    val mostFrequentNumbers: List<Pair<Int, Int>>, // 가장 많이 나온 번호 Top 10
    val leastFrequentNumbers: List<Pair<Int, Int>>, // 가장 적게 나온 번호 Top 10
    val evenOddRatio: Pair<Int, Int>,            // 짝수/홀수 비율
    val rangeDistribution: Map<String, Int>,     // 구간별 분포 (1-10, 11-20, ...)
    val averageSum: Double,                       // 번호 합계 평균
    val consecutiveCount: Int                     // 연속 번호 포함된 세트 수
)

/**
 * 번호 분석기
 */
object NumberAnalyzer {
    
    /**
     * 저장된 번호들을 분석
     */
    fun analyze(savedNumbers: List<com.lotto.app.data.model.SavedLottoNumber>): NumberAnalysis {
        if (savedNumbers.isEmpty()) {
            return NumberAnalysis(
                totalSets = 0,
                numberFrequency = emptyMap(),
                mostFrequentNumbers = emptyList(),
                leastFrequentNumbers = emptyList(),
                evenOddRatio = Pair(0, 0),
                rangeDistribution = mapOf(
                    "1-10" to 0,
                    "11-20" to 0,
                    "21-30" to 0,
                    "31-40" to 0,
                    "41-45" to 0
                ),
                averageSum = 0.0,
                consecutiveCount = 0
            )
        }
        
        val allNumbers = savedNumbers.flatMap { it.numbers }
        
        // 번호별 빈도 계산
        val frequency = mutableMapOf<Int, Int>()
        for (num in allNumbers) {
            frequency[num] = frequency.getOrDefault(num, 0) + 1
        }
        
        // 가장 많이/적게 나온 번호
        val sortedByFreq = frequency.toList().sortedByDescending { it.second }
        val mostFrequent = sortedByFreq.take(10)
        val leastFrequent = sortedByFreq.reversed().take(10)
        
        // 짝수/홀수 비율
        val evenCount = allNumbers.count { it % 2 == 0 }
        val oddCount = allNumbers.size - evenCount
        
        // 구간별 분포
        val rangeDistribution = mutableMapOf(
            "1-10" to 0,
            "11-20" to 0,
            "21-30" to 0,
            "31-40" to 0,
            "41-45" to 0
        )
        
        for (num in allNumbers) {
            when (num) {
                in 1..10 -> rangeDistribution["1-10"] = rangeDistribution["1-10"]!! + 1
                in 11..20 -> rangeDistribution["11-20"] = rangeDistribution["11-20"]!! + 1
                in 21..30 -> rangeDistribution["21-30"] = rangeDistribution["21-30"]!! + 1
                in 31..40 -> rangeDistribution["31-40"] = rangeDistribution["31-40"]!! + 1
                in 41..45 -> rangeDistribution["41-45"] = rangeDistribution["41-45"]!! + 1
            }
        }
        
        // 평균 합계
        val averageSum = savedNumbers.map { it.numbers.sum() }.average()
        
        // 연속 번호 포함된 세트 수
        val consecutiveCount = savedNumbers.count { hasConsecutiveNumbers(it.numbers) }
        
        return NumberAnalysis(
            totalSets = savedNumbers.size,
            numberFrequency = frequency,
            mostFrequentNumbers = mostFrequent,
            leastFrequentNumbers = leastFrequent,
            evenOddRatio = Pair(evenCount, oddCount),
            rangeDistribution = rangeDistribution,
            averageSum = averageSum,
            consecutiveCount = consecutiveCount
        )
    }
    
    /**
     * 연속 번호가 있는지 확인
     */
    private fun hasConsecutiveNumbers(numbers: List<Int>): Boolean {
        val sorted = numbers.sorted()
        for (i in 0 until sorted.size - 1) {
            if (sorted[i + 1] - sorted[i] == 1) {
                return true
            }
        }
        return false
    }
}
