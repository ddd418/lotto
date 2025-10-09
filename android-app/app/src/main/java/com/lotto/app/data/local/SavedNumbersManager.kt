package com.lotto.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lotto.app.data.model.SavedLottoNumber

/**
 * 저장된 로또 번호를 관리하는 로컬 저장소
 */
class SavedNumbersManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "saved_lotto_numbers"
        private const val KEY_NUMBERS = "numbers_list"
    }
    
    /**
     * 모든 저장된 번호 조회
     */
    fun getSavedNumbers(): List<SavedLottoNumber> {
        val json = prefs.getString(KEY_NUMBERS, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedLottoNumber>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 번호 저장
     */
    fun saveNumber(number: SavedLottoNumber): Boolean {
        val currentList = getSavedNumbers().toMutableList()
        currentList.add(0, number)  // 최신이 맨 위로
        return saveList(currentList)
    }
    
    /**
     * 번호 삭제
     */
    fun deleteNumber(id: String): Boolean {
        val currentList = getSavedNumbers().toMutableList()
        val removed = currentList.removeIf { it.id == id }
        return if (removed) saveList(currentList) else false
    }
    
    /**
     * 번호 업데이트
     */
    fun updateNumber(number: SavedLottoNumber): Boolean {
        val currentList = getSavedNumbers().toMutableList()
        val index = currentList.indexOfFirst { it.id == number.id }
        return if (index >= 0) {
            currentList[index] = number
            saveList(currentList)
        } else {
            false
        }
    }
    
    /**
     * 즐겨찾기 토글
     */
    fun toggleFavorite(id: String): Boolean {
        val currentList = getSavedNumbers().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        return if (index >= 0) {
            currentList[index] = currentList[index].copy(
                isFavorite = !currentList[index].isFavorite
            )
            saveList(currentList)
        } else {
            false
        }
    }
    
    /**
     * 모든 번호 삭제
     */
    fun clearAll(): Boolean {
        return prefs.edit().remove(KEY_NUMBERS).commit()
    }
    
    /**
     * 리스트 저장 헬퍼
     */
    private fun saveList(list: List<SavedLottoNumber>): Boolean {
        val json = gson.toJson(list)
        return prefs.edit().putString(KEY_NUMBERS, json).commit()
    }
    
    /**
     * 저장된 번호 개수
     */
    fun getCount(): Int = getSavedNumbers().size
}
