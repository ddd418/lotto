package com.lotto.app.data.remote

import com.lotto.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
}
