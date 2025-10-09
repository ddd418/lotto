package com.lotto.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 카카오 로그인 관련 모델
 */
data class KakaoLoginRequest(
    @SerializedName("authorization_code")
    val authorizationCode: String
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    
    @SerializedName("refresh_token")
    val refreshToken: String,
    
    @SerializedName("token_type")
    val tokenType: String = "bearer",
    
    @SerializedName("expires_in")
    val expiresIn: Int
)

data class UserProfile(
    val id: Int,
    
    @SerializedName("kakao_id")
    val kakaoId: String,
    
    val email: String?,
    val nickname: String,
    
    @SerializedName("profile_image")
    val profileImage: String?,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("last_login_at")
    val lastLoginAt: String?
)