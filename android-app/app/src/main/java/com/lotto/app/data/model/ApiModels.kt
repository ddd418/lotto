package com.lotto.app.data.model

import com.google.gson.annotations.SerializedName

// ========================================
// 당첨 번호 관련 모델
// ========================================

/**
 * 당첨 번호 응답
 */
data class WinningNumberResponse(
    @SerializedName("draw_number")
    val drawNumber: Int,
    
    @SerializedName("numbers")
    val numbers: List<Int>,
    
    @SerializedName("bonus_number")
    val bonusNumber: Int,
    
    @SerializedName("draw_date")
    val drawDate: String?,
    
    @SerializedName("prize_1st")
    val prize1st: Long?,
    
    @SerializedName("prize_2nd")
    val prize2nd: Long?,
    
    @SerializedName("prize_3rd")
    val prize3rd: Long?,
    
    @SerializedName("prize_4th")
    val prize4th: Long?,
    
    @SerializedName("prize_5th")
    val prize5th: Long?,
    
    @SerializedName("winners_1st")
    val winners1st: Int?,
    
    @SerializedName("total_sales")
    val totalSales: Long?
)

/**
 * 당첨 번호 목록 응답
 */
data class WinningNumberListResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("latest_draw")
    val latestDraw: Int?,
    
    @SerializedName("winning_numbers")
    val winningNumbers: List<WinningNumberResponse>
)

/**
 * 동기화 응답
 */
data class SyncResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("success_count")
    val successCount: Int,
    
    @SerializedName("skip_count")
    val skipCount: Int,
    
    @SerializedName("fail_count")
    val failCount: Int,
    
    @SerializedName("total")
    val total: Int
)

// ========================================
// 저장된 번호 관련 모델
// ========================================

/**
 * 저장된 번호 요청
 */
data class SavedNumberRequest(
    @SerializedName("numbers")
    val numbers: List<Int>,
    
    @SerializedName("nickname")
    val nickname: String? = null,
    
    @SerializedName("memo")
    val memo: String? = null,
    
    @SerializedName("is_favorite")
    val isFavorite: Boolean = false,
    
    @SerializedName("recommendation_type")
    val recommendationType: String? = null
)

/**
 * 저장된 번호 응답
 */
data class SavedNumberResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("numbers")
    val numbers: List<Int>,
    
    @SerializedName("nickname")
    val nickname: String?,
    
    @SerializedName("memo")
    val memo: String?,
    
    @SerializedName("is_favorite")
    val isFavorite: Boolean,
    
    @SerializedName("recommendation_type")
    val recommendationType: String?,
    
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * 메시지 응답
 */
data class MessageResponse(
    @SerializedName("message")
    val message: String
)

// ========================================
// 당첨 확인 관련 모델
// ========================================

/**
 * 당첨 확인 요청
 */
data class CheckWinningRequest(
    @SerializedName("numbers")
    val numbers: List<Int>,
    
    @SerializedName("draw_number")
    val drawNumber: Int
)

/**
 * 당첨 확인 응답
 */
data class CheckWinningResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("draw_number")
    val drawNumber: Int,
    
    @SerializedName("user_numbers")
    val userNumbers: List<Int>,
    
    @SerializedName("winning_numbers")
    val winningNumbers: List<Int>,
    
    @SerializedName("bonus_number")
    val bonusNumber: Int,
    
    @SerializedName("matched_count")
    val matchedCount: Int,
    
    @SerializedName("has_bonus")
    val hasBonus: Boolean,
    
    @SerializedName("rank")
    val rank: Int?,
    
    @SerializedName("prize_amount")
    val prizeAmount: Long?,
    
    @SerializedName("message")
    val message: String
)

/**
 * 당첨 내역 항목
 */
data class WinningHistoryItem(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("numbers")
    val numbers: List<Int>,
    
    @SerializedName("draw_number")
    val drawNumber: Int,
    
    @SerializedName("rank")
    val rank: Int?,
    
    @SerializedName("prize_amount")
    val prizeAmount: Long?,
    
    @SerializedName("matched_count")
    val matchedCount: Int,
    
    @SerializedName("has_bonus")
    val hasBonus: Boolean,
    
    @SerializedName("checked_at")
    val checkedAt: String
)

// ========================================
// 사용자 설정 관련 모델
// ========================================

/**
 * 사용자 설정 요청
 */
data class UserSettingsRequest(
    @SerializedName("enable_push_notifications")
    val enablePushNotifications: Boolean? = null,
    
    @SerializedName("enable_draw_notifications")
    val enableDrawNotifications: Boolean? = null,
    
    @SerializedName("enable_winning_notifications")
    val enableWinningNotifications: Boolean? = null,
    
    @SerializedName("theme_mode")
    val themeMode: String? = null,
    
    @SerializedName("default_recommendation_type")
    val defaultRecommendationType: String? = null,
    
    @SerializedName("lucky_numbers")
    val luckyNumbers: List<Int>? = null,
    
    @SerializedName("exclude_numbers")
    val excludeNumbers: List<Int>? = null
)

/**
 * 사용자 설정 응답
 */
data class UserSettingsResponse(
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("enable_push_notifications")
    val enablePushNotifications: Boolean,
    
    @SerializedName("enable_draw_notifications")
    val enableDrawNotifications: Boolean,
    
    @SerializedName("enable_winning_notifications")
    val enableWinningNotifications: Boolean,
    
    @SerializedName("theme_mode")
    val themeMode: String,
    
    @SerializedName("default_recommendation_type")
    val defaultRecommendationType: String,
    
    @SerializedName("lucky_numbers")
    val luckyNumbers: List<Int>?,
    
    @SerializedName("exclude_numbers")
    val excludeNumbers: List<Int>?,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String?
)
