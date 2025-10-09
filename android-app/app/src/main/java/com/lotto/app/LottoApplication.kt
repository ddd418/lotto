package com.lotto.app

import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Base64
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.lotto.app.data.remote.RetrofitClient
import java.security.MessageDigest

class LottoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 디버그 모드에서 키해시 출력 (ApplicationInfo 사용)
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            printKeyHash()
        }
        
        // 카카오 SDK 초기화
        KakaoSdk.init(this, "6b46608ffd2d4f1d912db2ac6c7e9802")
        
        // Retrofit 초기화
        RetrofitClient.init(this)
        
        Log.d("LottoApplication", "✅ 앱 초기화 완료")
    }
    
    /**
     * 카카오 개발자 콘솔에 등록할 키해시를 출력하는 함수
     */
    private fun printKeyHash() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature: Signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d("KAKAO_KEY_HASH", "KeyHash: $keyHash")
                Log.d("KAKAO_KEY_HASH", "Package Name: $packageName")
            }
        } catch (e: Exception) {
            Log.e("KAKAO_KEY_HASH", "Error getting key hash", e)
        }
    }
}
