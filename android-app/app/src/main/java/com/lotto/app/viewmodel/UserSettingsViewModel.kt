package com.lotto.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotto.app.data.model.UserSettingsResponse
import com.lotto.app.data.remote.RetrofitClient
import com.lotto.app.data.repository.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 사용자 설정 ViewModel
 */
class UserSettingsViewModel : ViewModel() {
    
    private val TAG = "UserSettingsViewModel"
    private val repository = UserSettingsRepository(RetrofitClient.userSettingsApiService)
    
    // UI 상태
    private val _settings = MutableStateFlow<UserSettingsResponse?>(null)
    val settings: StateFlow<UserSettingsResponse?> = _settings.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * 설정 로드
     */
    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "사용자 설정 로드 시작")
            
            repository.getSettings()
                .onSuccess { settings ->
                    _settings.value = settings
                    Log.d(TAG, "✅ 설정 로드 완료")
                }
                .onFailure { e ->
                    _error.value = "설정을 불러오지 못했습니다: ${e.message}"
                    Log.e(TAG, "❌ 설정 로드 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 행운 번호 업데이트
     */
    fun updateLuckyNumbers(numbers: List<Int>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "행운 번호 업데이트: $numbers")
            
            repository.updateLuckyNumbers(numbers)
                .onSuccess { settings ->
                    _settings.value = settings
                    _successMessage.value = "행운 번호가 저장되었습니다"
                    Log.d(TAG, "✅ 행운 번호 업데이트 완료")
                }
                .onFailure { e ->
                    _error.value = "행운 번호 저장에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 행운 번호 업데이트 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 제외 번호 업데이트
     */
    fun updateExcludeNumbers(numbers: List<Int>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "제외 번호 업데이트: $numbers")
            
            repository.updateExcludeNumbers(numbers)
                .onSuccess { settings ->
                    _settings.value = settings
                    _successMessage.value = "제외 번호가 저장되었습니다"
                    Log.d(TAG, "✅ 제외 번호 업데이트 완료")
                }
                .onFailure { e ->
                    _error.value = "제외 번호 저장에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 제외 번호 업데이트 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 테마 업데이트
     */
    fun updateTheme(theme: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "테마 업데이트: $theme")
            
            repository.updateTheme(theme)
                .onSuccess { settings ->
                    _settings.value = settings
                    _successMessage.value = "테마가 변경되었습니다"
                    Log.d(TAG, "✅ 테마 업데이트 완료")
                }
                .onFailure { e ->
                    _error.value = "테마 변경에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 테마 업데이트 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 알림 설정 업데이트
     */
    fun updateNotificationSettings(
        drawNotification: Boolean,
        winningNotification: Boolean,
        promotionNotification: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "알림 설정 업데이트: draw=$drawNotification, winning=$winningNotification, promotion=$promotionNotification")
            
            repository.updateNotificationSettings(
                push = promotionNotification,
                draw = drawNotification,
                winning = winningNotification
            )
                .onSuccess { settings ->
                    _settings.value = settings
                    _successMessage.value = "알림 설정이 저장되었습니다"
                    Log.d(TAG, "✅ 알림 설정 업데이트 완료")
                }
                .onFailure { e ->
                    _error.value = "알림 설정 저장에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 알림 설정 업데이트 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 전체 설정 업데이트
     */
    fun updateAllSettings(
        luckyNumbers: List<Int>?,
        excludeNumbers: List<Int>?,
        themeMode: String?,
        drawNotification: Boolean?,
        winningNotification: Boolean?,
        promotionNotification: Boolean?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "전체 설정 업데이트")
            
            repository.updateSettings(
                luckyNumbers = luckyNumbers,
                excludeNumbers = excludeNumbers,
                themeMode = themeMode,
                enableDrawNotifications = drawNotification,
                enableWinningNotifications = winningNotification,
                enablePushNotifications = promotionNotification
            )
                .onSuccess { settings ->
                    _settings.value = settings
                    _successMessage.value = "설정이 저장되었습니다"
                    Log.d(TAG, "✅ 전체 설정 업데이트 완료")
                }
                .onFailure { e ->
                    _error.value = "설정 저장에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 전체 설정 업데이트 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 에러 클리어
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 성공 메시지 클리어
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
