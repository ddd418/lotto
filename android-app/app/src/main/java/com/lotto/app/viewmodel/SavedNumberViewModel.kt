package com.lotto.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotto.app.data.model.SavedNumberResponse
import com.lotto.app.data.remote.RetrofitClient
import com.lotto.app.data.repository.SavedNumberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 저장된 번호 ViewModel
 */
class SavedNumberViewModel : ViewModel() {
    
    private val TAG = "SavedNumberViewModel"
    private val repository = SavedNumberRepository(RetrofitClient.savedNumberApiService)
    
    // UI 상태
    private val _savedNumbers = MutableStateFlow<List<SavedNumberResponse>>(emptyList())
    val savedNumbers: StateFlow<List<SavedNumberResponse>> = _savedNumbers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        loadSavedNumbers()
    }
    
    /**
     * 저장된 번호 목록 로드
     */
    fun loadSavedNumbers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "저장된 번호 목록 로드 시작")
            
            repository.getSavedNumbers()
                .onSuccess { numbers ->
                    _savedNumbers.value = numbers
                    Log.d(TAG, "✅ 저장된 번호 ${numbers.size}개 로드 완료")
                }
                .onFailure { e ->
                    _error.value = "번호 목록을 불러오지 못했습니다: ${e.message}"
                    Log.e(TAG, "❌ 저장된 번호 로드 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 번호 저장
     */
    fun saveNumber(
        numbers: List<Int>,
        nickname: String? = null,
        memo: String? = null,
        isFavorite: Boolean = false,
        recommendationType: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "번호 저장 시작: $numbers")
            
            repository.saveNumber(
                numbers = numbers,
                nickname = nickname,
                memo = memo,
                isFavorite = isFavorite,
                recommendationType = recommendationType
            )
                .onSuccess {
                    _successMessage.value = "번호가 저장되었습니다"
                    Log.d(TAG, "✅ 번호 저장 성공")
                    loadSavedNumbers() // 목록 새로고침
                }
                .onFailure { e ->
                    _error.value = "번호 저장에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 번호 저장 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 번호 수정
     */
    fun updateNumber(
        id: Int,
        numbers: List<Int>,
        nickname: String? = null,
        memo: String? = null,
        isFavorite: Boolean = false,
        recommendationType: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "번호 수정 시작: ID $id")
            
            repository.updateNumber(
                id = id,
                numbers = numbers,
                nickname = nickname,
                memo = memo,
                isFavorite = isFavorite,
                recommendationType = recommendationType
            )
                .onSuccess {
                    _successMessage.value = "번호가 수정되었습니다"
                    Log.d(TAG, "✅ 번호 수정 성공")
                    loadSavedNumbers() // 목록 새로고침
                }
                .onFailure { e ->
                    _error.value = "번호 수정에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 번호 수정 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 번호 삭제
     */
    fun deleteNumber(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "번호 삭제 시작: ID $id")
            
            repository.deleteNumber(id)
                .onSuccess {
                    _successMessage.value = "번호가 삭제되었습니다"
                    Log.d(TAG, "✅ 번호 삭제 성공")
                    loadSavedNumbers() // 목록 새로고침
                }
                .onFailure { e ->
                    _error.value = "번호 삭제에 실패했습니다: ${e.message}"
                    Log.e(TAG, "❌ 번호 삭제 실패", e)
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * 즐겨찾기 토글
     */
    fun toggleFavorite(savedNumber: SavedNumberResponse) {
        updateNumber(
            id = savedNumber.id,
            numbers = savedNumber.numbers,
            nickname = savedNumber.nickname,
            memo = savedNumber.memo,
            isFavorite = !savedNumber.isFavorite,
            recommendationType = savedNumber.recommendationType
        )
    }
    
    /**
     * 메모 업데이트
     */
    fun updateMemo(savedNumber: SavedNumberResponse, newMemo: String) {
        updateNumber(
            id = savedNumber.id,
            numbers = savedNumber.numbers,
            nickname = savedNumber.nickname,
            memo = newMemo,
            isFavorite = savedNumber.isFavorite,
            recommendationType = savedNumber.recommendationType
        )
    }
    
    /**
     * 별칭 업데이트
     */
    fun updateNickname(savedNumber: SavedNumberResponse, newNickname: String) {
        updateNumber(
            id = savedNumber.id,
            numbers = savedNumber.numbers,
            nickname = newNickname,
            memo = savedNumber.memo,
            isFavorite = savedNumber.isFavorite,
            recommendationType = savedNumber.recommendationType
        )
    }
    
    /**
     * 에러 메시지 클리어
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
