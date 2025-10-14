package com.lotto.app.di

import android.content.Context
import com.lotto.app.billing.SubscriptionManager
import com.lotto.app.data.local.TrialManager
import com.lotto.app.data.remote.RetrofitClient
import com.lotto.app.data.repository.AuthRepository
import com.lotto.app.data.repository.LottoRepository
import com.lotto.app.viewmodel.SubscriptionViewModel

/**
 * 의존성 주입을 위한 간단한 ServiceLocator
 */
object ServiceLocator {
    
    fun getLottoRepository(): LottoRepository {
        return LottoRepository()
    }
    
    fun getAuthRepository(context: Context): AuthRepository {
        return AuthRepository(
            context = context,
            authApiService = RetrofitClient.authApiService
        )
    }
    
    fun getSubscriptionManager(context: Context): SubscriptionManager {
        return SubscriptionManager(context)
    }
    
    fun getTrialManager(context: Context): TrialManager {
        return TrialManager(context)
    }
    
    fun getSubscriptionViewModel(context: Context): SubscriptionViewModel {
        return SubscriptionViewModel(
            subscriptionManager = getSubscriptionManager(context),
            trialManager = getTrialManager(context),
            context = context
        )
    }
    
    // RetrofitClient의 API 서비스들을 직접 노출
    val subscriptionApiService get() = RetrofitClient.subscriptionApiService
}