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
    
    private val api = RetrofitClient.lottoApiService
    
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
     * 번호 추천
     */
    suspend fun recommendNumbers(nSets: Int = 5, mode: String = "ai"): Result<RecommendResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RecommendRequest(nSets = nSets, mode = mode)
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
            android.util.Log.d("LottoRepository", "📡 API 호출: /api/latest-draw")
            val response = api.getLatestDraw()
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                android.util.Log.d("LottoRepository", "✅ API 응답 성공:")
                android.util.Log.d("LottoRepository", "   lastDraw: ${data.lastDraw}")
                android.util.Log.d("LottoRepository", "   generatedAt: ${data.generatedAt}")
                
                Result.success(data)
            } else {
                android.util.Log.e("LottoRepository", "❌ API 응답 실패: ${response.code()}")
                Result.failure(Exception("최신 회차 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("LottoRepository", "❌ API 호출 예외: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    
    /**
     * 대시보드 통계 조회
     * @param recentDraws 최근 N회차 (기본값: 20, 범위: 5-100)
     */
    suspend fun getDashboard(recentDraws: Int = 20): Result<DashboardResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDashboard(recentDraws)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("대시보드 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 저장된 번호 목록 조회 (서버에서)
     */
    suspend fun getSavedNumbersFromServer(): Result<List<SavedNumberApiResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSavedNumbers()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("저장된 번호 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

