package com.lotto.app.data.remote

import com.lotto.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 로또 API 서비스 인터페이스
 */
interface LottoApiService {
    
    /**
     * 헬스 체크
     */
    @GET("api/health")
    suspend fun getHealth(): Response<HealthResponse>
    
    /**
     * 로또 번호 추천
     */
    @POST("api/recommend")
    suspend fun recommendNumbers(
        @Body request: RecommendRequest
    ): Response<RecommendResponse>
    
    /**
     * 통계 조회
     */
    @GET("api/stats")
    suspend fun getStats(): Response<StatsResponse>
    
    /**
     * 최신 회차 조회
     */
    @GET("api/latest-draw")
    suspend fun getLatestDraw(): Response<LatestDrawResponse>
    
    /**
     * 대시보드 통계 조회
     * @param recentDraws 최근 N회차 (기본값: 20, 범위: 5-100)
     */
    @GET("api/dashboard")
    suspend fun getDashboard(
        @Query("recent_draws") recentDraws: Int = 20
    ): Response<DashboardResponse>
    
    /**
     * 저장된 번호 목록 조회 (인증 필요)
     */
    @GET("api/saved-numbers")
    suspend fun getSavedNumbers(): Response<List<SavedNumberApiResponse>>
}
