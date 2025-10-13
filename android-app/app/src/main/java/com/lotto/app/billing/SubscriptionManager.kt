package com.lotto.app.billing

import android.app.Activity
import android.content.Context
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
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _subscriptionState.value = SubscriptionState.Ready
                    checkSubscriptionStatus()
                } else {
                    _subscriptionState.value = SubscriptionState.Error("Billing setup failed")
                }
            }
            
            override fun onBillingServiceDisconnected() {
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
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList.first()
                
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return@queryProductDetailsAsync
                
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
                
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                
                billingClient?.launchBillingFlow(activity, billingFlowParams)
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
