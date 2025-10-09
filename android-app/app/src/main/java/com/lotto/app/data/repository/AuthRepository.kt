package com.lotto.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.lotto.app.data.model.*
import com.lotto.app.data.remote.AuthApiService
import com.lotto.app.data.remote.UserDataApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 인증 리포지토리
 * 카카오 로그인과 JWT 토큰 관리
 */
class AuthRepository(
    private val context: Context,
    private val authApiService: AuthApiService,
    private val userDataApiService: UserDataApiService
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "nickname"
    }
    
    /**
     * 카카오 로그인 처리
     */
    suspend fun loginWithKakao(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "카카오 로그인 시작")
            
            // 1. 카카오 로그인으로 인증 코드 획득
            val authCode = getKakaoAuthCode()
            Log.d(TAG, "카카오 인증 코드 획득 성공")
            
            // 2. 백엔드 서버에 인증 코드 전송하여 JWT 토큰 획득
            val loginRequest = KakaoLoginRequest(authCode)
            Log.d(TAG, "백엔드 서버에 로그인 요청 전송")
            val response = authApiService.kakaoLogin(loginRequest)
            
            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                Log.d(TAG, "로그인 성공 - 토큰 저장")
                
                // 3. 토큰 저장
                saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                
                // 4. 사용자 정보 가져오기
                val userProfile = getCurrentUser()
                
                if (userProfile.isSuccess) {
                    Result.success(userProfile.getOrThrow())
                } else {
                    Result.failure(userProfile.exceptionOrNull() ?: Exception("사용자 정보 조회 실패"))
                }
            } else {
                Log.e(TAG, "로그인 실패: ${response.message()}")
                Result.failure(Exception("로그인 실패: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그인 중 오류 발생", e)
            Result.failure(e)
        }
    }
    
    /**
     * 카카오에서 인증 코드 획득
     */
    private suspend fun getKakaoAuthCode(): String = suspendCoroutine { continuation ->
        Log.d(TAG, "카카오 인증 코드 획득 시작")
        
        // 카카오톡 설치 여부 확인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            Log.d(TAG, "카카오톡 앱으로 로그인 시도")
            // 카카오톡으로 로그인
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    Log.e(TAG, "카카오톡 로그인 오류", error)
                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        continuation.resumeWithException(Exception("로그인 취소"))
                    } else {
                        // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                        Log.d(TAG, "카카오계정으로 로그인 재시도")
                        loginWithKakaoAccount(continuation)
                    }
                } else if (token != null) {
                    Log.d(TAG, "카카오톡 로그인 성공")
                    continuation.resume(token.accessToken)
                }
            }
        } else {
            Log.d(TAG, "카카오톡 미설치 - 카카오계정으로 로그인")
            // 카카오계정으로 로그인
            loginWithKakaoAccount(continuation)
        }
    }
    
    /**
     * 카카오계정으로 로그인
     */
    private fun loginWithKakaoAccount(continuation: kotlin.coroutines.Continuation<String>) {
        Log.d(TAG, "카카오계정 웹뷰 로그인 시작")
        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
            if (error != null) {
                Log.e(TAG, "카카오계정 로그인 실패", error)
                continuation.resumeWithException(Exception("카카오계정 로그인 실패: ${error.message}"))
            } else if (token != null) {
                Log.d(TAG, "카카오계정 로그인 성공")
                continuation.resume(token.accessToken)
            } else {
                Log.e(TAG, "토큰 획득 실패")
                continuation.resumeWithException(Exception("토큰 획득 실패"))
            }
        }
    }
    
    /**
     * 현재 사용자 정보 조회
     */
    suspend fun getCurrentUser(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("로그인이 필요합니다"))
            }
            
            val response = authApiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful) {
                val userProfile = response.body()!!
                saveUserInfo(userProfile)
                Result.success(userProfile)
            } else {
                Result.failure(Exception("사용자 정보 조회 실패: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 토큰 저장
     */
    private fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }
    
    /**
     * 사용자 정보 저장
     */
    private fun saveUserInfo(userProfile: UserProfile) {
        prefs.edit()
            .putInt(KEY_USER_ID, userProfile.id)
            .putString(KEY_NICKNAME, userProfile.nickname)
            .apply()
    }
    
    /**
     * 토큰 삭제
     */
    private fun clearTokens() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 액세스 토큰 가져오기
     */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    
    /**
     * 로그인 상태 확인
     */
    fun isLoggedIn(): Boolean = getAccessToken() != null
    
    /**
     * 저장된 사용자 닉네임 가져오기
     */
    fun getUserNickname(): String? = prefs.getString(KEY_NICKNAME, null)
    
    /**
     * 로그아웃 처리
     */
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "로그아웃 시작")
            
            // 1. 백엔드 서버에 로그아웃 요청
            val token = getAccessToken()
            if (token != null) {
                try {
                    authApiService.logout("Bearer $token")
                    Log.d(TAG, "백엔드 로그아웃 성공")
                } catch (e: Exception) {
                    Log.e(TAG, "백엔드 로그아웃 실패 (무시): ${e.message}")
                    // 백엔드 로그아웃 실패해도 로컬 토큰은 삭제
                }
            }
            
            // 2. 카카오 로그아웃
            suspendCoroutine<Unit> { continuation ->
                UserApiClient.instance.logout { error ->
                    if (error != null) {
                        Log.e(TAG, "카카오 로그아웃 실패 (무시): ${error.message}")
                        // 카카오 로그아웃 실패해도 계속 진행
                    } else {
                        Log.d(TAG, "카카오 로그아웃 성공")
                    }
                    continuation.resume(Unit)
                }
            }
            
            // 3. 로컬 토큰 삭제
            clearTokens()
            Log.d(TAG, "로컬 토큰 삭제 완료")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "로그아웃 오류: ${e.message}", e)
            // 오류가 발생해도 로컬 토큰은 삭제
            clearTokens()
            Result.failure(e)
        }
    }
}