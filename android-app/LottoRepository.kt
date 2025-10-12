package com.lotto.app.data.repository

import com.lotto.app.data.model.*
import com.lotto.app.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 로또 데이터 저장소
 * API 호출을 관리하고 UI 레이어에 데이터 제공
 */
class LottoRepository {
    
    private val api = RetrofitClient.apiService
    
    /**
     * 헬스 체크
     */
    suspend fun checkHealth(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHealth()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("서버 응답 오류: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 로또연구소
     */
    suspend fun recommendNumbers(nSets: Int = 5): Result<RecommendResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RecommendRequest(nSets = nSets)
            val response = api.recommendNumbers(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("추천 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 통계 조회
     */
    suspend fun getStats(): Result<StatsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getStats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("통계 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 최신 회차 조회
     */
    suspend fun getLatestDraw(): Result<LatestDrawResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLatestDraw()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("최신 회차 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
