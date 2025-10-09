package com.lotto.app.data.remote

import com.lotto.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 인증 관련 API 서비스
 */
interface AuthApiService {
    
    @POST("auth/kakao/login")
    suspend fun kakaoLogin(
        @Body request: KakaoLoginRequest
    ): Response<TokenResponse>
    
    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserProfile>
    
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body refreshToken: String
    ): Response<TokenResponse>
    
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>
}

/**
 * 사용자 데이터 관련 API 서비스
 */
interface UserDataApiService {
    
    // 저장된 번호 관리
    @POST("api/saved-numbers")
    suspend fun saveNumbers(
        @Header("Authorization") token: String,
        @Body request: SavedNumberRequest
    ): Response<SavedNumberResponse>
    
    @GET("api/saved-numbers")
    suspend fun getSavedNumbers(
        @Header("Authorization") token: String
    ): Response<List<SavedNumberResponse>>
    
    @PUT("api/saved-numbers/{id}")
    suspend fun updateSavedNumber(
        @Path("id") id: Int,
        @Header("Authorization") token: String,
        @Body request: SavedNumberRequest
    ): Response<SavedNumberResponse>
    
    @DELETE("api/saved-numbers/{id}")
    suspend fun deleteSavedNumber(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<Unit>
}