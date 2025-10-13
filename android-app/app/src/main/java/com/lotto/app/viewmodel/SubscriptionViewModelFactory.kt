package com.lotto.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lotto.app.billing.SubscriptionManager
import com.lotto.app.data.local.TrialManager

/**
 * SubscriptionViewModel Factory
 */
class SubscriptionViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            val subscriptionManager = SubscriptionManager(context)
            val trialManager = TrialManager(context)
            return SubscriptionViewModel(subscriptionManager, trialManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
