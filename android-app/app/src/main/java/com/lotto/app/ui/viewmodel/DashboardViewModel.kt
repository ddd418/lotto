package com.lotto.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotto.app.data.model.DashboardResponse
import com.lotto.app.data.repository.LottoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 대시보드 화면의 ViewModel
 */
class DashboardViewModel : ViewModel() {
    
    private val repository = LottoRepository()
    
    // UI 상태
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // 최근 회차 수
    private val _recentDraws = MutableStateFlow(20)
    val recentDraws: StateFlow<Int> = _recentDraws.asStateFlow()
    
    /**
     * 최근 회차 수 변경
     */
    fun setRecentDraws(draws: Int) {
        if (draws in 5..100) {
            _recentDraws.value = draws
            loadDashboard()
        }
    }
    
    /**
     * 대시보드 데이터 로드
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            
            repository.getDashboard(_recentDraws.value).fold(
                onSuccess = { response ->
                    _uiState.value = DashboardUiState.Success(response)
                },
                onFailure = { error ->
                    _uiState.value = DashboardUiState.Error(
                        error.message ?: "대시보드 데이터를 불러올 수 없습니다"
                    )
                }
            )
        }
    }
    
    /**
     * 재시도
     */
    fun retry() {
        loadDashboard()
    }
}

/**
 * 대시보드 UI 상태
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val data: DashboardResponse) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
