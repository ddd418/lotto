package com.lotto.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 테마 설정 ViewModel
 */
class ThemeViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "ThemeViewModel"
    }
    
    // 테마 모드: "light", "dark", "system"
    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    /**
     * SharedPreferences에서 테마 설정 로드
     */
    fun loadThemePreference(context: Context) {
        val prefs = context.getSharedPreferences("lotto_settings", Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString("theme_mode", "system") ?: "system"
        _isDarkMode.value = prefs.getBoolean("dark_mode", false)
        Log.d(TAG, "테마 로드: mode=${_themeMode.value}, isDark=${_isDarkMode.value}")
    }
    
    /**
     * 테마 모드 설정 (light/dark/system)
     */
    fun setThemeMode(context: Context, mode: String, isSystemDark: Boolean = false) {
        Log.d(TAG, "테마 변경 요청: mode=$mode, isSystemDark=$isSystemDark")
        _themeMode.value = mode
        
        // 실제 다크 모드 여부 결정
        val isDark = when (mode) {
            "light" -> false
            "dark" -> true
            "system" -> isSystemDark
            else -> false
        }
        _isDarkMode.value = isDark
        
        Log.d(TAG, "테마 적용: mode=${_themeMode.value}, isDark=${_isDarkMode.value}")
        
        // SharedPreferences에 저장
        val prefs = context.getSharedPreferences("lotto_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("theme_mode", mode)
            .putBoolean("dark_mode", isDark)
            .apply()
    }
    
    /**
     * 다크 모드 토글
     */
    fun toggleDarkMode(context: Context) {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        _themeMode.value = if (newValue) "dark" else "light"
        
        // SharedPreferences에 저장
        val prefs = context.getSharedPreferences("lotto_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("theme_mode", _themeMode.value)
            .putBoolean("dark_mode", newValue)
            .apply()
    }
    
    /**
     * 다크 모드 설정 (하위 호환성)
     */
    fun setDarkMode(context: Context, enabled: Boolean) {
        _isDarkMode.value = enabled
        _themeMode.value = if (enabled) "dark" else "light"
        
        // SharedPreferences에 저장
        val prefs = context.getSharedPreferences("lotto_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("theme_mode", _themeMode.value)
            .putBoolean("dark_mode", enabled)
            .apply()
    }
}