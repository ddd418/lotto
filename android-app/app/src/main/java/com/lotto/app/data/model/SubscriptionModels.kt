package com.lotto.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 구독 상태 응답
 */
data class SubscriptionStatusResponse(
    @SerializedName("is_pro")
    val isPro: Boolean,
    
    @SerializedName("trial_active")
    val trialActive: Boolean,
    
    @SerializedName("trial_days_remaining")
    val trialDaysRemaining: Int,
    
    @SerializedName("subscription_plan")
    val subscriptionPlan: String,
    
    @SerializedName("has_access")
    val hasAccess: Boolean,
    
    @SerializedName("trial_start_date")
    val trialStartDate: String?,
    
    @SerializedName("trial_end_date")
    val trialEndDate: String?,
    
    @SerializedName("subscription_end_date")
    val subscriptionEndDate: String?,
    
    @SerializedName("auto_renew")
    val autoRenew: Boolean
)
