package com.lotto.app.data.remote

import com.lotto.app.data.model.SubscriptionStatusResponse
import com.lotto.app.data.model.VerifyPurchaseRequest
import com.lotto.app.data.model.VerifyPurchaseResponse
import com.lotto.app.data.model.CancelSubscriptionResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * 구독 관리 API 서비스
 */
interface SubscriptionApiService {
    
    /**
     * 무료 체험 시작
     */
    @POST("api/subscription/start-trial")
    suspend fun startTrial(): Response<SubscriptionStatusResponse>
    
    /**
     * 구독 상태 조회
     */
    @GET("api/subscription/status")
    suspend fun getSubscriptionStatus(): Response<SubscriptionStatusResponse>
    
    /**
     * Google Play 구매 검증
     */
    @POST("api/subscription/verify-purchase")
    suspend fun verifyPurchase(
        @Body request: VerifyPurchaseRequest
    ): Response<VerifyPurchaseResponse>
    
    /**
     * 구독 취소
     */
    @POST("api/subscription/cancel")
    suspend fun cancelSubscription(): Response<CancelSubscriptionResponse>
}
