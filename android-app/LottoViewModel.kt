package com.lotto.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotto.app.data.model.*
import com.lotto.app.data.repository.LottoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI 상태 관리를 위한 sealed class
 */
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/**
 * 로또 ViewModel
 * MVVM 패턴으로 비즈니스 로직과 UI 상태 관리
 */
class LottoViewModel : ViewModel() {
    
    private val repository = LottoRepository()
    
    // 추천 번호 상태
    private val _recommendState = MutableStateFlow<UiState<RecommendResponse>>(UiState.Idle)
    val recommendState: StateFlow<UiState<RecommendResponse>> = _recommendState.asStateFlow()
    
    // 통계 상태
    private val _statsState = MutableStateFlow<UiState<StatsResponse>>(UiState.Idle)
    val statsState: StateFlow<UiState<StatsResponse>> = _statsState.asStateFlow()
    
    // 최신 회차 상태
    private val _latestDrawState = MutableStateFlow<UiState<LatestDrawResponse>>(UiState.Idle)
    val latestDrawState: StateFlow<UiState<LatestDrawResponse>> = _latestDrawState.asStateFlow()
    
    // 서버 연결 상태
    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()
    
    init {
        // 앱 시작 시 서버 연결 확인 및 최신 회차 로드
        checkServerHealth()
        loadLatestDraw()
    }
    
    /**
     * 서버 헬스 체크
     */
    fun checkServerHealth() {
        viewModelScope.launch {
            repository.checkHealth()
                .onSuccess { 
                    _isServerConnected.value = true
                }
                .onFailure { 
                    _isServerConnected.value = false
                }
        }
    }
    
    /**
     * 로또연구소
     */
    fun recommendNumbers(nSets: Int = 5) {
        viewModelScope.launch {
            _recommendState.value = UiState.Loading
            
            repository.recommendNumbers(nSets)
                .onSuccess { response ->
                    _recommendState.value = UiState.Success(response)
                }
                .onFailure { error ->
                    _recommendState.value = UiState.Error(
                        error.message ?: "번호 추천 중 오류가 발생했습니다"
                    )
                }
        }
    }
    
    /**
     * 통계 조회
     */
    fun loadStats() {
        viewModelScope.launch {
            _statsState.value = UiState.Loading
            
            repository.getStats()
                .onSuccess { response ->
                    _statsState.value = UiState.Success(response)
                }
                .onFailure { error ->
                    _statsState.value = UiState.Error(
                        error.message ?: "통계 조회 중 오류가 발생했습니다"
                    )
                }
        }
    }
    
    /**
     * 최신 회차 조회
     */
    fun loadLatestDraw() {
        viewModelScope.launch {
            _latestDrawState.value = UiState.Loading
            
            repository.getLatestDraw()
                .onSuccess { response ->
                    _latestDrawState.value = UiState.Success(response)
                }
                .onFailure { error ->
                    _latestDrawState.value = UiState.Error(
                        error.message ?: "최신 회차 조회 중 오류가 발생했습니다"
                    )
                }
        }
    }
    
    /**
     * 추천 상태 초기화
     */
    fun resetRecommendState() {
        _recommendState.value = UiState.Idle
    }
    
    /**
     * 통계 상태 초기화
     */
    fun resetStatsState() {
        _statsState.value = UiState.Idle
    }
}
