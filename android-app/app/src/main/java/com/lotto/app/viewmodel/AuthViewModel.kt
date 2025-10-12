package com.lotto.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotto.app.data.model.UserProfile
import com.lotto.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 인증 관련 ViewModel
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    // 로그인 상태
    private val _loginState = MutableStateFlow<UiState<UserProfile>>(UiState.Idle)
    val loginState: StateFlow<UiState<UserProfile>> = _loginState.asStateFlow()
    
    // 현재 사용자 정보
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
    
    // 로그인 여부
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        checkLoginStatus()
    }
    
    /**
     * 로그인 상태 확인
     */
    private fun checkLoginStatus() {
        _isLoggedIn.value = authRepository.isLoggedIn()
        
        if (_isLoggedIn.value) {
            // 로그인된 상태면 사용자 정보 로드
            getCurrentUser()
        }
    }
    
    /**
     * 카카오 로그인
     */
    fun loginWithKakao(context: Context) {
        _loginState.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔑 카카오 로그인 시작")
                val result = authRepository.loginWithKakao()
                
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    Log.d(TAG, "✅ 카카오 로그인 성공:")
                    Log.d(TAG, "   id: ${userProfile.id}")
                    Log.d(TAG, "   nickname: ${userProfile.nickname}")
                    Log.d(TAG, "   email: ${userProfile.email}")
                    
                    _currentUser.value = userProfile
                    _isLoggedIn.value = true
                    _loginState.value = UiState.Success(userProfile)
                    
                    Log.d(TAG, "✅ ViewModel 상태 업데이트 완료")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "❌ 카카오 로그인 실패: ${error?.message}")
                    _loginState.value = UiState.Error(
                        error?.message ?: "로그인에 실패했습니다"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 카카오 로그인 예외 발생", e)
                _loginState.value = UiState.Error(
                    e.message ?: "로그인 중 오류가 발생했습니다"
                )
            }
        }
    }
    
    /**
     * 현재 사용자 정보 조회
     */
    fun getCurrentUser() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 AuthViewModel.getCurrentUser() 호출")
                val result = authRepository.getCurrentUser()
                
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    Log.d(TAG, "✅ 사용자 정보 조회 성공:")
                    Log.d(TAG, "   id: ${userProfile.id}")
                    Log.d(TAG, "   nickname: ${userProfile.nickname}")
                    Log.d(TAG, "   email: ${userProfile.email}")
                    
                    _currentUser.value = userProfile
                    
                    Log.d(TAG, "✅ _currentUser.value 업데이트 완료: ${_currentUser.value?.nickname}")
                } else {
                    Log.e(TAG, "❌ 사용자 정보 조회 실패 - 로그아웃 처리")
                    // 사용자 정보 조회 실패 시 로그아웃 처리
                    logout()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 사용자 정보 조회 예외 발생 - 로그아웃 처리", e)
                // 오류 발생 시 로그아웃 처리
                logout()
            }
        }
    }
    
    /**
     * 로그아웃
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _currentUser.value = null
                _isLoggedIn.value = false
                _loginState.value = UiState.Idle
            } catch (e: Exception) {
                // 로그아웃 실패해도 로컬 상태는 초기화
                _currentUser.value = null
                _isLoggedIn.value = false
                _loginState.value = UiState.Idle
            }
        }
    }
    
    /**
     * 로그인 상태 초기화
     */
    fun clearLoginState() {
        _loginState.value = UiState.Idle
    }
}