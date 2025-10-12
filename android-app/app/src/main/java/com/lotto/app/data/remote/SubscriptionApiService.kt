package com.lotto.app.data.remote

import com.lotto.app.data.model.*
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

/**
 * 구매 검증 요청
 */
data class VerifyPurchaseRequest(
    val purchase_token: String,
    val order_id: String,
    val product_id: String
)

/**
 * 구매 검증 응답
 */
data class VerifyPurchaseResponse(
    val verified: Boolean,
    val is_pro: Boolean,
    val subscription_end_date: String?,
    val message: String
)

/**
 * 구독 취소 응답
 */
data class CancelSubscriptionResponse(
    val success: Boolean,
    val message: String,
    val subscription_end_date: String?
)
