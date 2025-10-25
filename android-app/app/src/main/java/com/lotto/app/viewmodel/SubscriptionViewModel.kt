package com.lotto.app.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotto.app.billing.SubscriptionManager
import com.lotto.app.data.remote.SubscriptionApiService
import com.lotto.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 구독 관리 ViewModel (프리미엄 모델)
 * Free: AI 추천(보기만), 통계 무제한
 * Pro: 모든 기능 (저장, 당첨확인, 가상추첨, 고급분석)
 */
class SubscriptionViewModel(
    private val subscriptionManager: SubscriptionManager,
    private val context: Context
) : ViewModel() {
    
    private val subscriptionApi: SubscriptionApiService = ServiceLocator.subscriptionApiService
    
    // 구독 상태
    val isProUser: StateFlow<Boolean> = subscriptionManager.isProUser
    
    // 사용자 접근 권한 (모든 유저가 앱 사용 가능)
    private val _hasAccess = MutableStateFlow(true)  // 항상 true
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()
    
    // 서버 구독 상태
    private val _subscriptionStatus = MutableStateFlow(SubscriptionStatus())
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    // 에러 메시지
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        initializeSubscription()
        syncWithServer()
        refreshStatus()
    }
    
    /**
     * 구독 시스템 초기화
     */
    private fun initializeSubscription() {
        Log.d("SubscriptionViewModel", "initializeSubscription 시작")
        subscriptionManager.initialize()
        
        viewModelScope.launch {
            subscriptionManager.isProUser.collect {
                Log.d("SubscriptionViewModel", "isProUser 변경: $it")
            }
        }
        
        // 에러 상태 모니터링
        viewModelScope.launch {
            subscriptionManager.subscriptionState.collect { state ->
                Log.d("SubscriptionViewModel", "구독 상태: $state")
                if (state is com.lotto.app.billing.SubscriptionState.Error) {
                    _errorMessage.value = state.message
                }
            }
        }
    }
    
    /**
     * 서버와 동기화 (로그인 시 호출)
     */
    fun syncWithServer() {
        Log.d("SubscriptionViewModel", "🔄 syncWithServer 시작")
        viewModelScope.launch {
            try {
                val response = subscriptionApi.getSubscriptionStatus()
                Log.d("SubscriptionViewModel", "서버 응답: ${response.code()}")
                
                if (response.isSuccessful) {
                    val status = response.body()
                    status?.let {
                        Log.d("SubscriptionViewModel", """
                            📡 서버 구독 상태:
                            - isPro: ${it.isPro}
                            - hasAccess: ${it.hasAccess}
                        """.trimIndent())
                        
                        _subscriptionStatus.value = SubscriptionStatus(
                            isPro = it.isPro,
                            subscriptionPlan = it.subscriptionPlan,
                            hasAccess = true,  // 모든 유저 접근 가능
                            subscriptionEndDate = it.subscriptionEndDate,
                            autoRenew = it.autoRenew
                        )
                        
                        // 🔥 중요: 서버의 isPro 상태로 SubscriptionManager 업데이트
                        subscriptionManager.updateProStatusFromServer(it.isPro)
                        
                        _hasAccess.value = true  // 항상 true
                        
                        Log.d("SubscriptionViewModel", "✅ syncWithServer 완료 - Pro 상태: ${it.isPro}")
                    }
                } else {
                    Log.e("SubscriptionViewModel", "❌ 서버 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SubscriptionViewModel", "❌ 네트워크 오류: ${e.message}")
            }
        }
    }
    
    /**
     * 구독 결제 시작
     */
    fun startSubscription(activity: Activity) {
        Log.d("SubscriptionViewModel", "startSubscription 호출됨")
        _errorMessage.value = null  // 이전 에러 메시지 초기화
        subscriptionManager.launchSubscriptionFlow(activity)
    }
    
    /**
     * 에러 메시지 초기화
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 구독 상태 새로고침
     */
    fun refreshSubscriptionStatus() {
        subscriptionManager.checkSubscriptionStatus()
    }
    
    /**
     * 서버에서 구독 상태 조회
     */
    fun refreshStatus() {
        viewModelScope.launch {
            try {
                val response = subscriptionApi.getSubscriptionStatus()
                if (response.isSuccessful) {
                    response.body()?.let { status ->
                        _subscriptionStatus.value = SubscriptionStatus(
                            isPro = status.isPro,
                            subscriptionPlan = status.subscriptionPlan,
                            hasAccess = true,  // 모든 유저 접근 가능
                            subscriptionEndDate = status.subscriptionEndDate,
                            autoRenew = status.autoRenew
                        )
                        
                        // 🔥 중요: 서버의 isPro 상태로 SubscriptionManager 업데이트
                        subscriptionManager.updateProStatusFromServer(status.isPro)
                        
                        _hasAccess.value = true
                        
                        Log.d("SubscriptionViewModel", "✅ refreshStatus 완료 - Pro 상태: ${status.isPro}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SubscriptionViewModel", "상태 갱신 오류: ${e.message}")
            }
        }
    }
    
    /**
     * 구독 취소
     */
    fun cancelSubscription() {
        viewModelScope.launch {
            try {
                val response = subscriptionApi.cancelSubscription()
                if (response.isSuccessful) {
                    // 상태 새로고침
                    refreshStatus()
                }
            } catch (e: Exception) {
                // 오류 처리
            }
        }
    }
    
    /**
     * Pro 전용 기능 사용 가능 여부
     */
    fun canUseProFeature(): Boolean = isProUser.value
    
    /**
     * 번호 저장 가능 여부 (Pro 전용)
     */
    fun canSaveNumbers(): Boolean = isProUser.value
    
    /**
     * 당첨 확인 가능 여부 (Pro 전용)
     */
    fun canCheckWinning(): Boolean = isProUser.value
    
    /**
     * 가상 추첨 가능 여부 (Pro 전용)
     */
    fun canUseVirtualDraw(): Boolean = isProUser.value
    
    /**
     * 고급 분석 가능 여부 (Pro 전용)
     */
    fun canUseAdvancedAnalysis(): Boolean = isProUser.value
    
    override fun onCleared() {
        super.onCleared()
        subscriptionManager.destroy()
    }
}

/**
 * 서버 구독 상태 (프리미엄 모델)
 */
data class SubscriptionStatus(
    val isPro: Boolean = false,
    val subscriptionPlan: String = "free",
    val hasAccess: Boolean = true,  // 항상 true
    val subscriptionEndDate: String? = null,
    val autoRenew: Boolean = false
)
