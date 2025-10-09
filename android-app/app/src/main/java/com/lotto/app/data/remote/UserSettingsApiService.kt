package com.lotto.app.data.remote

import com.lotto.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 사용자 설정 API 서비스
 */
interface UserSettingsApiService {
    
    /**
     * 사용자 설정 조회
     */
    @GET("api/settings")
    suspend fun getSettings(): Response<UserSettingsResponse>
    
    /**
     * 사용자 설정 업데이트
     */
    @PUT("api/settings")
    suspend fun updateSettings(
        @Body request: UserSettingsRequest
    ): Response<UserSettingsResponse>
}
