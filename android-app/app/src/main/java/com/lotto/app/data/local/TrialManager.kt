package com.lotto.app.data.local

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

/**
 * 무료 체험 관리 매니저
 */
class TrialManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "trial_prefs"
        private const val KEY_TRIAL_START_TIME = "trial_start_time"
        private const val KEY_IS_TRIAL_STARTED = "is_trial_started"
        private const val TRIAL_PERIOD_DAYS = 30L
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * 무료 체험 시작
     */
    fun startTrial() {
        if (!isTrialStarted()) {
            prefs.edit()
                .putLong(KEY_TRIAL_START_TIME, System.currentTimeMillis())
                .putBoolean(KEY_IS_TRIAL_STARTED, true)
                .apply()
        }
    }
    
    /**
     * 무료 체험 시작 여부
     */
    fun isTrialStarted(): Boolean {
        return prefs.getBoolean(KEY_IS_TRIAL_STARTED, false)
    }
    
    /**
     * 무료 체험 활성 상태 확인
     */
    fun isTrialActive(): Boolean {
        if (!isTrialStarted()) return false
        
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(currentTime - startTime)
        
        return elapsedDays < TRIAL_PERIOD_DAYS
    }
    
    /**
     * 남은 체험 기간 (일)
     */
    fun getRemainingTrialDays(): Long {
        if (!isTrialStarted()) return TRIAL_PERIOD_DAYS
        
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(currentTime - startTime)
        
        return (TRIAL_PERIOD_DAYS - elapsedDays).coerceAtLeast(0)
    }
    
    /**
     * 체험 시작 시간
     */
    fun getTrialStartTime(): Long {
        return prefs.getLong(KEY_TRIAL_START_TIME, 0)
    }
    
    /**
     * 체험 정보 초기화 (테스트용)
     */
    fun resetTrial() {
        prefs.edit()
            .remove(KEY_TRIAL_START_TIME)
            .remove(KEY_IS_TRIAL_STARTED)
            .apply()
    }
}
