package com.lotto.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotto.app.data.model.CheckWinningResponse
import com.lotto.app.data.model.WinningHistoryItem
import com.lotto.app.data.model.WinningNumberResponse
import com.lotto.app.data.remote.RetrofitClient
import com.lotto.app.data.repository.WinningCheckRepository
import com.lotto.app.data.repository.WinningNumberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 당첨 확인 ViewModel
 */
class WinningCheckViewModel : ViewModel() {
    
    private val TAG = "WinningCheckViewModel"
    private val checkRepository = WinningCheckRepository(RetrofitClient.winningCheckApiService)
    private val winningRepository = WinningNumberRepository(RetrofitClient.winningNumberApiService)
    
    // UI 상태
    private val _checkResult = MutableStateFlow<CheckWinningResponse?>(null)
    val checkResult: StateFlow<CheckWinningResponse?> = _checkResult.asStateFlow()
    
    private val _latestWinning = MutableStateFlow<WinningNumberResponse?>(null)
    val latestWinning: StateFlow<WinningNumberResponse?> = _latestWinning.asStateFlow()
    
    private val _history = MutableStateFlow<List<WinningHistoryItem>>(emptyList())
    val history: StateFlow<List<WinningHistoryItem>> = _history.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadLatestWinning()
    }
    
    /**
     * 최신 당첨 번호 로드
     */
    fun loadLatestWinning() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "최신 당첨 번호 로드 시작")
            
            winningRepository.getLatestWinning()
                .onSuccess { winning ->
                    _latestWinning.value = winning
                    Log.d(TAG, "✅ 최신 당첨 번호 로드: ${winning.drawNumber}회차")
                }
                .onFailure { e ->
                    _error.value = "최신 당첨 번호를 불러오지 못했습니다: ${e.message}"
                    Log.e(TAG, "❌ 최신 당첨 번호 로드 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 당첨 확인
     */
    fun checkWinning(numbers: List<Int>, drawNumber: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _checkResult.value = null
            
            Log.d(TAG, "당첨 확인 시작: $numbers vs ${drawNumber}회차")
            
            checkRepository.checkWinning(numbers, drawNumber)
                .onSuccess { result ->
                    _checkResult.value = result
                    Log.d(TAG, "✅ 당첨 확인 완료: ${result.matchedCount}개 맞음, 등수=${result.rank}")
                }
                .onFailure { e ->
                    _error.value = "당첨 확인에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 당첨 확인 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 당첨 확인 내역 로드
     */
    fun loadHistory(limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "당첨 확인 내역 로드 시작")
            
            checkRepository.getWinningHistory(limit)
                .onSuccess { history ->
                    _history.value = history
                    Log.d(TAG, "✅ 당첨 확인 내역 ${history.size}개 로드")
                }
                .onFailure { e ->
                    _error.value = "당첨 확인 내역을 불러오지 못했습니다: ${e.message}"
                    Log.e(TAG, "❌ 당첨 확인 내역 로드 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 결과 클리어
     */
    fun clearResult() {
        _checkResult.value = null
    }
    
    /**
     * 에러 클리어
     */
    fun clearError() {
        _error.value = null
    }
}
