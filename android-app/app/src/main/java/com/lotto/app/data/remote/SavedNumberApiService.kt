package com.lotto.app.data.remote

import com.lotto.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 저장된 번호 API 서비스
 */
interface SavedNumberApiService {
    
    /**
     * 번호 저장
     */
    @POST("api/saved-numbers")
    suspend fun saveNumber(
        @Body request: SavedNumberRequest
    ): Response<SavedNumberResponse>
    
    /**
     * 저장된 번호 목록 조회
     */
    @GET("api/saved-numbers")
    suspend fun getSavedNumbers(): Response<List<SavedNumberResponse>>
    
    /**
     * 저장된 번호 수정
     */
    @PUT("api/saved-numbers/{id}")
    suspend fun updateNumber(
        @Path("id") id: Int,
        @Body request: SavedNumberRequest
    ): Response<SavedNumberResponse>
    
    /**
     * 저장된 번호 삭제
     */
    @DELETE("api/saved-numbers/{id}")
    suspend fun deleteNumber(
        @Path("id") id: Int
    ): Response<MessageResponse>
}
