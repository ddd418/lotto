package com.lotto.app.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotto.app.billing.SubscriptionManager
import com.lotto.app.data.local.TrialManager
import com.lotto.app.data.remote.SubscriptionApiService
import com.lotto.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 구독 및 체험 관리 ViewModel (서버 연동)
 */
class SubscriptionViewModel(
    private val subscriptionManager: SubscriptionManager,
    private val trialManager: TrialManager,
    private val context: Context
) : ViewModel() {
    
    private val subscriptionApi: SubscriptionApiService = ServiceLocator.subscriptionApiService
    
    // 체험 종료 알림 표시 여부
    private val _shouldShowTrialWarning = MutableStateFlow(false)
    val shouldShowTrialWarning: StateFlow<Boolean> = _shouldShowTrialWarning.asStateFlow()
    
    private val _trialWarningDays = MutableStateFlow(0)
    val trialWarningDays: StateFlow<Int> = _trialWarningDays.asStateFlow()
    
    // 구독 상태
    val isProUser: StateFlow<Boolean> = subscriptionManager.isProUser
    
    // 무료 체험 상태
    private val _trialInfo = MutableStateFlow(TrialInfo())
    val trialInfo: StateFlow<TrialInfo> = _trialInfo.asStateFlow()
    
    // 사용자 접근 권한
    private val _hasAccess = MutableStateFlow(false)
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
        refreshStatus()  // 서버에서 최신 구독 상태 가져오기
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
                updateAccessStatus()
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
     * 서버와 동기화 (public - 로그인 시 호출)
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
                            - trialActive: ${it.trialActive}
                            - trialDaysRemaining: ${it.trialDaysRemaining}
                            - hasAccess: ${it.hasAccess}
                        """.trimIndent())
                        
                        // subscriptionStatus 업데이트
                        _subscriptionStatus.value = SubscriptionStatus(
                            isPro = it.isPro,
                            trialActive = it.trialActive,
                            trialDaysRemaining = it.trialDaysRemaining,
                            subscriptionPlan = it.subscriptionPlan,
                            hasAccess = it.hasAccess,
                            trialStartDate = it.trialStartDate,
                            trialEndDate = it.trialEndDate,
                            subscriptionEndDate = it.subscriptionEndDate,
                            autoRenew = it.autoRenew,
                            isTrialUsed = it.trialActive || it.trialDaysRemaining >= 0
                        )
                        
                        // hasAccess 즉시 업데이트
                        _hasAccess.value = it.hasAccess
                        
                        _trialInfo.value = TrialInfo(
                            isStarted = it.trialActive || it.trialDaysRemaining >= 0,
                            isActive = it.trialActive,
                            remainingDays = it.trialDaysRemaining.toLong()
                        )
                        
                        // 체험 종료 임박 알림 체크
                        checkTrialWarning(it.trialActive, it.trialDaysRemaining)
                        
                        Log.d("SubscriptionViewModel", "✅ syncWithServer 완료 - hasAccess: ${_hasAccess.value}")
                    }
                } else {
                    Log.e("SubscriptionViewModel", "❌ 서버 오류: ${response.code()}")
                    // 서버 오류 시 로컬 데이터 사용
                    updateTrialInfo()
                }
            } catch (e: Exception) {
                Log.e("SubscriptionViewModel", "❌ 네트워크 오류: ${e.message}")
                // 네트워크 오류 시 로컬 데이터 사용
                updateTrialInfo()
            }
        }
    }
    
    /**
     * 체험 종료 임박 알림 체크
     */
    private fun checkTrialWarning(trialActive: Boolean, daysRemaining: Int) {
        // PRO 구독자는 알림 불필요
        if (isProUser.value) {
            return
        }
        
        // 체험 중이고, 15일/5일/2일 남았을 때만 알림
        if (trialActive && (daysRemaining == 15 || daysRemaining == 5 || daysRemaining == 2)) {
            val prefs = context.getSharedPreferences("trial_warnings", Context.MODE_PRIVATE)
            val key = "warning_shown_$daysRemaining"
            val alreadyShown = prefs.getBoolean(key, false)
            
            if (!alreadyShown) {
                _trialWarningDays.value = daysRemaining
                _shouldShowTrialWarning.value = true
            }
        }
    }
    
    /**
     * 알림 확인 (다시 표시하지 않도록 저장)
     */
    fun dismissTrialWarning() {
        val days = _trialWarningDays.value
        if (days > 0) {
            val prefs = context.getSharedPreferences("trial_warnings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("warning_shown_$days", true).apply()
        }
        _shouldShowTrialWarning.value = false
    }
    
    /**
     * 무료 체험 정보 업데이트
     */
    fun updateTrialInfo() {
        _trialInfo.value = TrialInfo(
            isStarted = trialManager.isTrialStarted(),
            isActive = trialManager.isTrialActive(),
            remainingDays = trialManager.getRemainingTrialDays()
        )
        updateAccessStatus()
    }
    
    /**
     * 무료 체험 시작 (서버 연동)
     */
    fun startTrial() {
        viewModelScope.launch {
            try {
                // 1. 서버에 체험 시작 요청
                val response = subscriptionApi.startTrial()
                if (response.isSuccessful) {
                    val status = response.body()
                    status?.let {
                        _trialInfo.value = TrialInfo(
                            isStarted = true,
                            isActive = it.trialActive,
                            remainingDays = it.trialDaysRemaining.toLong()
                        )
                    }
                }
            } catch (e: Exception) {
                // 네트워크 오류 시 로컬에서 처리
                trialManager.startTrial()
            }
            
            // 2. 로컬에도 저장
            trialManager.startTrial()
            updateTrialInfo()
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
                            trialActive = status.trialActive,
                            trialDaysRemaining = status.trialDaysRemaining,
                            subscriptionPlan = status.subscriptionPlan,
                            hasAccess = status.hasAccess,
                            trialStartDate = status.trialStartDate,
                            trialEndDate = status.trialEndDate,
                            subscriptionEndDate = status.subscriptionEndDate,
                            autoRenew = status.autoRenew,
                            isTrialUsed = status.trialActive || status.trialDaysRemaining >= 0
                        )
                        _hasAccess.value = status.hasAccess
                        
                        // 체험 종료 임박 알림 체크
                        checkTrialWarning(status.trialActive, status.trialDaysRemaining)
                    }
                }
            } catch (e: Exception) {
                // 네트워크 오류 시 로컬 상태 사용
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
     * 접근 권한 업데이트
     */
    private fun updateAccessStatus() {
        val newAccess = isProUser.value || trialInfo.value.isActive
        Log.d("SubscriptionViewModel", """
            🔐 접근 권한 업데이트:
            - isProUser: ${isProUser.value}
            - trialActive: ${trialInfo.value.isActive}
            - 이전 hasAccess: ${_hasAccess.value}
            - 새로운 hasAccess: $newAccess
        """.trimIndent())
        _hasAccess.value = newAccess
    }
    
    /**
     * 오늘 사용 가능 여부
     */
    fun canUseToday(): Boolean {
        return hasAccess.value
    }
    
    override fun onCleared() {
        super.onCleared()
        subscriptionManager.destroy()
    }
}

/**
 * 무료 체험 정보
 */
data class TrialInfo(
    val isStarted: Boolean = false,
    val isActive: Boolean = false,
    val remainingDays: Long = 30
)

/**
 * 서버 구독 상태
 */
data class SubscriptionStatus(
    val isPro: Boolean = false,
    val trialActive: Boolean = false,
    val trialDaysRemaining: Int = -1,  // -1 = 아직 로드되지 않음
    val subscriptionPlan: String = "free",
    val hasAccess: Boolean = false,
    val trialStartDate: String? = null,
    val trialEndDate: String? = null,
    val subscriptionEndDate: String? = null,
    val autoRenew: Boolean = false,
    val isTrialUsed: Boolean = false
)
