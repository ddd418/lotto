package com.lotto.app.viewmodel

import android.app.Activity
import android.content.Context
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
    
    init {
        initializeSubscription()
        syncWithServer()
    }
    
    /**
     * 구독 시스템 초기화
     */
    private fun initializeSubscription() {
        subscriptionManager.initialize()
        
        viewModelScope.launch {
            subscriptionManager.isProUser.collect {
                updateAccessStatus()
            }
        }
    }
    
    /**
     * 서버와 동기화 (public - 로그인 시 호출)
     */
    fun syncWithServer() {
        viewModelScope.launch {
            try {
                val response = subscriptionApi.getSubscriptionStatus()
                if (response.isSuccessful) {
                    val status = response.body()
                    status?.let {
                        _trialInfo.value = TrialInfo(
                            isStarted = it.trialActive || it.trialDaysRemaining >= 0,
                            isActive = it.trialActive,
                            remainingDays = it.trialDaysRemaining.toLong()
                        )
                        updateAccessStatus()
                        
                        // 체험 종료 임박 알림 체크
                        checkTrialWarning(it.trialActive, it.trialDaysRemaining)
                    }
                } else {
                    // 서버 오류 시 로컬 데이터 사용
                    updateTrialInfo()
                }
            } catch (e: Exception) {
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
        subscriptionManager.launchSubscriptionFlow(activity)
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
        _hasAccess.value = isProUser.value || trialInfo.value.isActive
    }
    
    /**
     * 오늘 사용 가능 여부 (광고 시청 옵션 포함)
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
    val trialDaysRemaining: Int = 0,
    val subscriptionPlan: String = "free",
    val hasAccess: Boolean = false,
    val trialStartDate: String? = null,
    val trialEndDate: String? = null,
    val subscriptionEndDate: String? = null,
    val autoRenew: Boolean = false,
    val isTrialUsed: Boolean = false
)
