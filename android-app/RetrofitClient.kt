package com.lotto.app.data.remote

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
    // 로컬 테스트:
    // - 에뮬레이터: "http://10.0.2.2:8000/"
    // - 실제 기기: "http://your-pc-ip:8000/"
    // 프로덕션: "http://your-server-ip:8000/"
    private const val BASE_URL = "http://10.0.2.2:8000/"
    
    /**
     * HTTP 로깅 인터셉터 (디버깅용)
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /**
     * OkHttp 클라이언트
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
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
     * API 서비스
     */
    val apiService: LottoApiService by lazy {
        retrofit.create(LottoApiService::class.java)
    }
}
