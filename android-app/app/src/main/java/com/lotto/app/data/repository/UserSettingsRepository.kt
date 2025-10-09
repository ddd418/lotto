package com.lotto.app.data.repository

import android.util.Log
import com.lotto.app.data.model.*
import com.lotto.app.data.remote.UserSettingsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 사용자 설정 Repository
 */
class UserSettingsRepository(
    private val api: UserSettingsApiService
) {
    private val TAG = "UserSettingsRepository"
    
    /**
     * 사용자 설정 조회
     */
    suspend fun getSettings(): Result<UserSettingsResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "사용자 설정 조회 시작")
            val response = api.getSettings()
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 사용자 설정 조회 성공")
                Result.success(response.body()!!)
            } else {
                val error = "사용자 설정 조회 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 사용자 설정 조회 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 사용자 설정 업데이트
     */
    suspend fun updateSettings(
        enablePushNotifications: Boolean? = null,
        enableDrawNotifications: Boolean? = null,
        enableWinningNotifications: Boolean? = null,
        themeMode: String? = null,
        defaultRecommendationType: String? = null,
        luckyNumbers: List<Int>? = null,
        excludeNumbers: List<Int>? = null
    ): Result<UserSettingsResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "사용자 설정 업데이트 시작")
            val request = UserSettingsRequest(
                enablePushNotifications = enablePushNotifications,
                enableDrawNotifications = enableDrawNotifications,
                enableWinningNotifications = enableWinningNotifications,
                themeMode = themeMode,
                defaultRecommendationType = defaultRecommendationType,
                luckyNumbers = luckyNumbers,
                excludeNumbers = excludeNumbers
            )
            val response = api.updateSettings(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 사용자 설정 업데이트 성공")
                Result.success(response.body()!!)
            } else {
                val error = "사용자 설정 업데이트 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 사용자 설정 업데이트 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 행운의 번호 업데이트
     */
    suspend fun updateLuckyNumbers(numbers: List<Int>): Result<UserSettingsResponse> {
        return updateSettings(luckyNumbers = numbers)
    }
    
    /**
     * 제외 번호 업데이트
     */
    suspend fun updateExcludeNumbers(numbers: List<Int>): Result<UserSettingsResponse> {
        return updateSettings(excludeNumbers = numbers)
    }
    
    /**
     * 테마 업데이트
     */
    suspend fun updateTheme(theme: String): Result<UserSettingsResponse> {
        return updateSettings(themeMode = theme)
    }
    
    /**
     * 알림 설정 업데이트
     */
    suspend fun updateNotificationSettings(
        push: Boolean? = null,
        draw: Boolean? = null,
        winning: Boolean? = null
    ): Result<UserSettingsResponse> {
        return updateSettings(
            enablePushNotifications = push,
            enableDrawNotifications = draw,
            enableWinningNotifications = winning
        )
    }
}
