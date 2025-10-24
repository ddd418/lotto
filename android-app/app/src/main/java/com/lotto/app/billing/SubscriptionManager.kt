package com.lotto.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.lotto.app.data.model.VerifyPurchaseRequest
import com.lotto.app.data.remote.SubscriptionApiService
import com.lotto.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing 구독 관리 매니저 (서버 연동)
 */
class SubscriptionManager(
    private val context: Context
) {
    
    companion object {
        // 구독 상품 ID (Google Play Console에서 설정한 ID와 일치해야 함)
        const val SUBSCRIPTION_PRODUCT_ID = "lotto_pro_monthly"
    }
    
    private var billingClient: BillingClient? = null
    private val subscriptionApi: SubscriptionApiService = ServiceLocator.subscriptionApiService
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()
    
    private val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()
    
    /**
     * Billing Client 초기화
     */
    fun initialize() {
        Log.d("SubscriptionManager", "initialize() 시작")
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                Log.d("SubscriptionManager", "Purchase update: ${billingResult.responseCode}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d("SubscriptionManager", "Billing setup finished: ${billingResult.responseCode}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _subscriptionState.value = SubscriptionState.Ready
                    checkSubscriptionStatus()
                } else {
                    _subscriptionState.value = SubscriptionState.Error("Billing setup failed: ${billingResult.debugMessage}")
                    Log.e("SubscriptionManager", "Billing setup error: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.d("SubscriptionManager", "Billing service disconnected")
                _subscriptionState.value = SubscriptionState.Disconnected
            }
        })
    }
    
    /**
     * 구독 상태 확인
     */
    fun checkSubscriptionStatus() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val isSubscribed = purchases.any { purchase ->
                    purchase.products.contains(SUBSCRIPTION_PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isProUser.value = isSubscribed
            }
        }
    }
    
    /**
     * 구독 시작
     */
    fun launchSubscriptionFlow(activity: Activity) {
        Log.d("SubscriptionManager", "launchSubscriptionFlow 시작")
        
        if (billingClient == null) {
            Log.e("SubscriptionManager", "BillingClient가 null입니다!")
            _subscriptionState.value = SubscriptionState.Error("결제 시스템이 초기화되지 않았습니다")
            return
        }
        
        if (!billingClient!!.isReady) {
            Log.w("SubscriptionManager", "BillingClient가 준비되지 않았습니다. 재연결 시도...")
            _subscriptionState.value = SubscriptionState.Error("결제 시스템 연결 중입니다. 잠시 후 다시 시도해주세요.")
            
            // BillingClient 재연결 시도
            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("SubscriptionManager", "재연결 성공, 구독 흐름 다시 시작")
                        _subscriptionState.value = SubscriptionState.Ready
                        // 연결 성공 후 다시 시도
                        launchSubscriptionFlow(activity)
                    } else {
                        Log.e("SubscriptionManager", "재연결 실패: ${billingResult.debugMessage}")
                        _subscriptionState.value = SubscriptionState.Error("결제 시스템 연결 실패: ${billingResult.debugMessage}")
                    }
                }
                
                override fun onBillingServiceDisconnected() {
                    Log.w("SubscriptionManager", "재연결 중 연결 끊김")
                    _subscriptionState.value = SubscriptionState.Disconnected
                }
            })
            return
        }
        
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        Log.d("SubscriptionManager", "제품 정보 조회 시작: $SUBSCRIPTION_PRODUCT_ID")
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            Log.d("SubscriptionManager", "제품 정보 조회 결과: ${billingResult.responseCode}, 제품 수: ${productDetailsList.size}")
            
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList.first()
                
                Log.d("SubscriptionManager", "제품명: ${productDetails.name}, 제품 ID: ${productDetails.productId}")
                Log.d("SubscriptionManager", "구독 옵션 수: ${productDetails.subscriptionOfferDetails?.size ?: 0}")
                
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                
                if (offerToken == null) {
                    Log.e("SubscriptionManager", "구독 옵션을 찾을 수 없습니다!")
                    _subscriptionState.value = SubscriptionState.Error("구독 상품을 찾을 수 없습니다")
                    return@queryProductDetailsAsync
                }
                
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
                
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                
                Log.d("SubscriptionManager", "결제 화면 실행")
                val launchResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
                Log.d("SubscriptionManager", "결제 화면 실행 결과: ${launchResult?.responseCode}")
            } else {
                Log.e("SubscriptionManager", "제품 정보 조회 실패: ${billingResult.debugMessage}")
                _subscriptionState.value = SubscriptionState.Error("구독 상품을 불러올 수 없습니다: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * 구매 처리 및 서버 검증
     */
    private fun handlePurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    // 1. 서버에 구매 검증 요청
                    verifyPurchaseWithServer(purchase)
                    // 2. Google Play에 확인
                    acknowledgePurchase(purchase)
                }
                _isProUser.value = true
            }
        }
    }
    
    /**
     * 서버에 구매 검증 요청
     */
    private fun verifyPurchaseWithServer(purchase: Purchase) {
        scope.launch {
            try {
                val request = VerifyPurchaseRequest(
                    purchaseToken = purchase.purchaseToken,
                    orderId = purchase.orderId ?: "",
                    productId = SUBSCRIPTION_PRODUCT_ID
                )
                
                val response = subscriptionApi.verifyPurchase(request)
                if (response.isSuccessful && response.body()?.verified == true) {
                    _isProUser.value = true
                }
            } catch (e: Exception) {
                // 네트워크 오류 등은 로컬에서 처리
                _isProUser.value = true
            }
        }
    }
    
    /**
     * 구매 확인
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                checkSubscriptionStatus()
            }
        }
    }
    
    /**
     * 리소스 정리
     */
    fun destroy() {
        billingClient?.endConnection()
    }
}

/**
 * 구독 상태
 */
sealed class SubscriptionState {
    object Loading : SubscriptionState()
    object Ready : SubscriptionState()
    object Disconnected : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
}
