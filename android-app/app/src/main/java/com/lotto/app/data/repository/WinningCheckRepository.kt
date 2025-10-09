package com.lotto.app.data.repository

import android.util.Log
import com.lotto.app.data.model.*
import com.lotto.app.data.remote.WinningCheckApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 당첨 확인 Repository
 */
class WinningCheckRepository(
    private val api: WinningCheckApiService
) {
    private val TAG = "WinningCheckRepository"
    
    /**
     * 로또 번호 당첨 확인
     */
    suspend fun checkWinning(
        numbers: List<Int>,
        drawNumber: Int
    ): Result<CheckWinningResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "당첨 확인 시작: $numbers vs ${drawNumber}회차")
            val request = CheckWinningRequest(
                numbers = numbers,
                drawNumber = drawNumber
            )
            val response = api.checkWinning(request)
            
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "✅ 당첨 확인 성공: ${result.matchedCount}개 맞음, 등수=${result.rank}")
                Result.success(result)
            } else {
                val error = "당첨 확인 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 당첨 확인 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 당첨 확인 내역 조회
     */
    suspend fun getWinningHistory(limit: Int = 20): Result<List<WinningHistoryItem>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "당첨 확인 내역 조회 시작: 최근 ${limit}개")
            val response = api.getWinningHistory(limit)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 당첨 확인 내역 조회 성공: ${response.body()?.size}개")
                Result.success(response.body()!!)
            } else {
                val error = "당첨 확인 내역 조회 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 당첨 확인 내역 조회 오류", e)
            Result.failure(e)
        }
    }
}
