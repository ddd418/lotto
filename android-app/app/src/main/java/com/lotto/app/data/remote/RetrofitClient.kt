package com.lotto.app.data.remote

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 클라이언트 싱글톤
 */
object RetrofitClient {
    
    // ⚠️ 여기를 서버 주소로 변경하세요!
    // Railway 프로덕션 서버
    private const val BASE_URL = "https://web-production-43fb4.up.railway.app/"
    // 로컬 테스트:
    // - 에뮬레이터: "http://10.0.2.2:8000/"
    // - 실제 기기: "http://192.168.0.6:8000/"
    
    private var applicationContext: Context? = null
    
    /**
     * Context 초기화 (Application에서 호출)
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
    
    /**
     * HTTP 로깅 인터셉터 (디버깅용)
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /**
     * 인증 토큰 인터셉터
     */
    private val authInterceptor = Interceptor { chain ->
        val token = getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }
    
    /**
     * SharedPreferences에서 액세스 토큰 가져오기
     */
    private fun getAccessToken(): String? {
        return applicationContext?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            ?.getString("access_token", null)
    }
    
    /**
     * OkHttp 클라이언트
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Retrofit 인스턴스
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * API 서비스들
     */
    val lottoApiService: LottoApiService by lazy {
        retrofit.create(LottoApiService::class.java)
    }
    
    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
    
    val winningNumberApiService: WinningNumberApiService by lazy {
        retrofit.create(WinningNumberApiService::class.java)
    }
    
    val savedNumberApiService: SavedNumberApiService by lazy {
        retrofit.create(SavedNumberApiService::class.java)
    }
    
    val winningCheckApiService: WinningCheckApiService by lazy {
        retrofit.create(WinningCheckApiService::class.java)
    }
    
    val userSettingsApiService: UserSettingsApiService by lazy {
        retrofit.create(UserSettingsApiService::class.java)
    }
    
    val subscriptionApiService: SubscriptionApiService by lazy {
        retrofit.create(SubscriptionApiService::class.java)
    }
}
