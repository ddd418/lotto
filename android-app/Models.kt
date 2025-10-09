package com.lotto.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 로또 번호 세트 (6개의 번호)
 */
data class LottoSet(
    @SerializedName("numbers")
    val numbers: List<Int>
)

/**
 * 번호 추천 요청
 */
data class RecommendRequest(
    @SerializedName("n_sets")
    val nSets: Int = 5,
    
    @SerializedName("seed")
    val seed: Int? = null
)

/**
 * 번호 추천 응답
 */
data class RecommendResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("last_draw")
    val lastDraw: Int,
    
    @SerializedName("generated_at")
    val generatedAt: String,
    
    @SerializedName("include_bonus")
    val includeBonus: Boolean,
    
    @SerializedName("sets")
    val sets: List<LottoSet>
)

/**
 * 통계 응답
 */
data class StatsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("last_draw")
    val lastDraw: Int,
    
    @SerializedName("generated_at")
    val generatedAt: String,
    
    @SerializedName("include_bonus")
    val includeBonus: Boolean,
    
    @SerializedName("frequency")
    val frequency: Map<String, Int>,
    
    @SerializedName("top_10")
    val top10: List<TopNumber>
)

/**
 * 상위 번호 정보
 */
data class TopNumber(
    @SerializedName("number")
    val number: Int,
    
    @SerializedName("count")
    val count: Int
)

/**
 * 헬스 체크 응답
 */
data class HealthResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("version")
    val version: String,
    
    @SerializedName("stats_available")
    val statsAvailable: Boolean,
    
    @SerializedName("last_draw")
    val lastDraw: Int?
)

/**
 * 최신 회차 응답
 */
data class LatestDrawResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("last_draw")
    val lastDraw: Int,
    
    @SerializedName("generated_at")
    val generatedAt: String,
    
    @SerializedName("include_bonus")
    val includeBonus: Boolean
)
