package com.lotto.app.data.repository

import android.util.Log
import com.lotto.app.data.model.*
import com.lotto.app.data.remote.SavedNumberApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 저장된 번호 Repository
 */
class SavedNumberRepository(
    private val api: SavedNumberApiService
) {
    private val TAG = "SavedNumberRepository"
    
    /**
     * 번호 저장
     */
    suspend fun saveNumber(
        numbers: List<Int>,
        nickname: String? = null,
        memo: String? = null,
        isFavorite: Boolean = false,
        recommendationType: String? = null
    ): Result<SavedNumberResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "번호 저장 시작: $numbers")
            val request = SavedNumberRequest(
                numbers = numbers,
                nickname = nickname,
                memo = memo,
                isFavorite = isFavorite,
                recommendationType = recommendationType
            )
            val response = api.saveNumber(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 번호 저장 성공: ID ${response.body()?.id}")
                Result.success(response.body()!!)
            } else {
                val error = "번호 저장 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 번호 저장 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 저장된 번호 목록 조회
     */
    suspend fun getSavedNumbers(): Result<List<SavedNumberResponse>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "저장된 번호 목록 조회 시작")
            val response = api.getSavedNumbers()
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 저장된 번호 조회 성공: ${response.body()?.size}개")
                Result.success(response.body()!!)
            } else {
                val error = "저장된 번호 조회 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 저장된 번호 조회 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 저장된 번호 수정
     */
    suspend fun updateNumber(
        id: Int,
        numbers: List<Int>,
        nickname: String? = null,
        memo: String? = null,
        isFavorite: Boolean = false,
        recommendationType: String? = null
    ): Result<SavedNumberResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "번호 수정 시작: ID $id")
            val request = SavedNumberRequest(
                numbers = numbers,
                nickname = nickname,
                memo = memo,
                isFavorite = isFavorite,
                recommendationType = recommendationType
            )
            val response = api.updateNumber(id, request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 번호 수정 성공: ID $id")
                Result.success(response.body()!!)
            } else {
                val error = "번호 수정 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 번호 수정 오류", e)
            Result.failure(e)
        }
    }
    
    /**
     * 저장된 번호 삭제
     */
    suspend fun deleteNumber(id: Int): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "번호 삭제 시작: ID $id")
            val response = api.deleteNumber(id)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ 번호 삭제 성공: ID $id")
                Result.success(response.body()!!)
            } else {
                val error = "번호 삭제 실패: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 번호 삭제 오류", e)
            Result.failure(e)
        }
    }
}
