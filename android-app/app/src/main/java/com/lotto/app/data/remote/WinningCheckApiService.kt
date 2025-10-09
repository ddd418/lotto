package com.lotto.app.data.remote

import com.lotto.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 당첨 확인 API 서비스
 */
interface WinningCheckApiService {
    
    /**
     * 로또 번호 당첨 확인
     */
    @POST("api/check-winning")
    suspend fun checkWinning(
        @Body request: CheckWinningRequest
    ): Response<CheckWinningResponse>
    
    /**
     * 당첨 확인 내역 조회
     */
    @GET("api/winning-history")
    suspend fun getWinningHistory(
        @Query("limit") limit: Int = 20
    ): Response<List<WinningHistoryItem>>
}
