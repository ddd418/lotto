package com.lotto.app.utils

import com.lotto.app.viewmodel.SubscriptionStatus

/**
 * 광고 표시 전략 헬퍼
 * 
 * 전략:
 * - 20일 이상: 완전 무료 (광고 없음)
 * - 10-19일: 핵심 기능 광고 없음
 * - 1-9일: 점진적으로 광고 증가
 * - PRO: 모든 광고 제거
 */
object AdStrategy {
    
    /**
     * 메인 화면 광고 표시 여부
     * - 핵심 첫인상 보호
     * - 10일 미만부터 광고
     */
    fun shouldShowAdInMain(subscriptionStatus: SubscriptionStatus): Boolean {
        return when {
            subscriptionStatus.isPro -> false
            subscriptionStatus.trialActive -> {
                subscriptionStatus.trialDaysRemaining < 10  // 10일 미만만 광고
            }
            else -> false  // 체험 종료는 차단됨
        }
    }
    
    /**
     * 추천 화면 광고 표시 여부
     * - 핵심 기능 보호
     * - 10일 미만부터 광고
     */
    fun shouldShowAdInRecommend(subscriptionStatus: SubscriptionStatus): Boolean {
        return when {
            subscriptionStatus.isPro -> false
            subscriptionStatus.trialActive -> {
                subscriptionStatus.trialDaysRemaining < 10  // 10일 미만만 광고
            }
            else -> false
        }
    }
    
    /**
     * 통계 화면 광고 표시 여부
     * - 분석 기능 (중요도 중간)
     * - 15일 미만부터 광고
     */
    fun shouldShowAdInStats(subscriptionStatus: SubscriptionStatus): Boolean {
        return when {
            subscriptionStatus.isPro -> false
            subscriptionStatus.trialActive -> {
                subscriptionStatus.trialDaysRemaining < 15  // 15일 미만만 광고
            }
            else -> false
        }
    }
    
    /**
     * 당첨 확인 화면 광고 표시 여부
     * - 부가 기능 (덜 중요)
     * - 체험 중 항상 광고
     */
    fun shouldShowAdInCheckWinning(subscriptionStatus: SubscriptionStatus): Boolean {
        return when {
            subscriptionStatus.isPro -> false
            subscriptionStatus.trialActive -> true  // 항상 광고
            else -> false
        }
    }
    
    /**
     * 저장 번호 화면 광고 표시 여부
     * - 부가 기능 (덜 중요)
     * - 체험 중 항상 광고
     */
    fun shouldShowAdInSavedNumbers(subscriptionStatus: SubscriptionStatus): Boolean {
        return when {
            subscriptionStatus.isPro -> false
            subscriptionStatus.trialActive -> true  // 항상 광고
            else -> false
        }
    }
    
    /**
     * 가상 추첨 화면 광고 표시 여부
     * - 재미 기능 (덜 중요)
     * - 체험 중 항상 광고
     */
    fun shouldShowAdInVirtualDraw(subscriptionStatus: SubscriptionStatus): Boolean {
        return when {
            subscriptionStatus.isPro -> false
            subscriptionStatus.trialActive -> true  // 항상 광고
            else -> false
        }
    }
    
    /**
     * 분석 대시보드 화면 광고 표시 여부
     * - 고급 기능
     * - 15일 미만부터 광고
     */
    fun shouldShowAdInAnalysis(subscriptionStatus: SubscriptionStatus): Boolean {
        return when {
            subscriptionStatus.isPro -> false
            subscriptionStatus.trialActive -> {
                subscriptionStatus.trialDaysRemaining < 15  // 15일 미만만 광고
            }
            else -> false
        }
    }
}
