package com.lotto.app.data.remote

import com.lotto.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 당첨 번호 API 서비스
 */
interface WinningNumberApiService {
    
    /**
     * 최신 당첨 번호 조회
     */
    @GET("api/winning-numbers/latest")
    suspend fun getLatestWinning(): Response<WinningNumberResponse>
    
    /**
     * 특정 회차 당첨 번호 조회
     */
    @GET("api/winning-numbers/{drawNumber}")
    suspend fun getWinningByDraw(
        @Path("drawNumber") drawNumber: Int
    ): Response<WinningNumberResponse>
    
    /**
     * 최근 N개 회차 당첨 번호 조회
     */
    @GET("api/winning-numbers")
    suspend fun getWinningList(
        @Query("limit") limit: Int = 10
    ): Response<WinningNumberListResponse>
    
    /**
     * 당첨 번호 동기화 (관리자용)
     */
    @POST("api/winning-numbers/sync")
    suspend fun syncWinningNumbers(
        @Query("start_draw") startDraw: Int,
        @Query("end_draw") endDraw: Int? = null
    ): Response<SyncResponse>
}
