package com.lotto.app.data.repository

import android.util.Log
import com.lotto.app.data.model.*
import com.lotto.app.data.remote.WinningNumberApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 당첨 번호 Repository
 */
class WinningNumberRepository(
    private val api: WinningNumberApiService
) {
    private val TAG = "WinningNumberRepository"
    
    /**
     * 최신 당첨 번호 조회
     */
    suspend fun getLatestWinning(): Result<WinningNumberResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "최신 당첨 번호 조회 시작")
            val response = api.getLatestWinning()
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 최신 당첨 번호 조회 성공: ${response.body()?.drawNumber}회차")
                Result.success(response.body()!!)
            } else {
                val error = "최신 당첨 번호 조회 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 최신 당첨 번호 조회 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 특정 회차 당첨 번호 조회
     */
    suspend fun getWinningByDraw(drawNumber: Int): Result<WinningNumberResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "${drawNumber}회차 당첨 번호 조회 시작")
            val response = api.getWinningByDraw(drawNumber)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ ${drawNumber}회차 당첨 번호 조회 성공")
                Result.success(response.body()!!)
            } else {
                val error = "${drawNumber}회차 조회 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ${drawNumber}회차 조회 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 최근 N개 회차 당첨 번호 목록 조회
     */
    suspend fun getWinningList(limit: Int = 10): Result<WinningNumberListResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "최근 ${limit}개 회차 당첨 번호 목록 조회 시작")
            val response = api.getWinningList(limit)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 당첨 번호 목록 조회 성공: ${response.body()?.count}개")
                Result.success(response.body()!!)
            } else {
                val error = "당첨 번호 목록 조회 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 당첨 번호 목록 조회 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 당첨 번호 동기화 (관리자용)
     */
    suspend fun syncWinningNumbers(
        startDraw: Int,
        endDraw: Int? = null
    ): Result<SyncResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "당첨 번호 동기화 시작: ${startDraw}회 ~ ${endDraw ?: "최신"}회")
            val response = api.syncWinningNumbers(startDraw, endDraw)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 당첨 번호 동기화 성공: ${response.body()?.message}")
                Result.success(response.body()!!)
            } else {
                val error = "당첨 번호 동기화 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 당첨 번호 동기화 오류", e)
            Result.failure(e)
        }
    }
}
