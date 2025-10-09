package com.lotto.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 테마 설정 ViewModel
 */
class ThemeViewModel : ViewModel() {
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    /**
     * SharedPreferences에서 테마 설정 로드
     */
    fun loadThemePreference(context: Context) {
        val prefs = context.getSharedPreferences("lotto_settings", Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean("dark_mode", false)
    }
    
    /**
     * 다크 모드 토글
     */
    fun toggleDarkMode(context: Context) {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        
        // SharedPreferences에 저장
        val prefs = context.getSharedPreferences("lotto_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", newValue).apply()
    }
    
    /**
     * 다크 모드 설정
     */
    fun setDarkMode(context: Context, enabled: Boolean) {
        _isDarkMode.value = enabled
        
        // SharedPreferences에 저장
        val prefs = context.getSharedPreferences("lotto_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }
}
