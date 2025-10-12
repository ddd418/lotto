package com.lotto.app.di

import android.content.Context
import com.lotto.app.data.remote.AuthApiService
import com.lotto.app.data.remote.LottoApiService
import com.lotto.app.data.remote.UserDataApiService
import com.lotto.app.data.repository.AuthRepository
import com.lotto.app.data.repository.LottoRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 의존성 주입을 위한 간단한 ServiceLocator
 */
object ServiceLocator {
    
    // Railway 프로덕션 서버
    private const val BASE_URL = "https://web-production-43fb4.up.railway.app/"
    // private const val BASE_URL = "http://10.0.2.2:8000/" // Android 에뮬레이터용 (로컬)
    // private const val BASE_URL = "http://192.168.0.6:8000/" // 실제 기기용 (로컬)
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val lottoApiService: LottoApiService by lazy {
        retrofit.create(LottoApiService::class.java)
    }
    
    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
    
    val userDataApiService: UserDataApiService by lazy {
        retrofit.create(UserDataApiService::class.java)
    }
    
    fun getLottoRepository(): LottoRepository {
        return LottoRepository()
    }
    
    fun getAuthRepository(context: Context): AuthRepository {
        return AuthRepository(
            context = context,
            authApiService = authApiService,
            userDataApiService = userDataApiService
        )
    }
}