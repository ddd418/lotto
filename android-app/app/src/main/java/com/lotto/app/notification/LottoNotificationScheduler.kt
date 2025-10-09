package com.lotto.app.notification

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 로또 추첨일 알림 스케줄 관리
 */
object LottoNotificationScheduler {
    
    private const val WORK_NAME = "lotto_draw_notification"
    
    /**
     * 매주 토요일 저녁 8시 알림 스케줄 설정
     */
    fun scheduleSaturdayNotification(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        // 다음 토요일 저녁 8시까지의 시간 계산
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 20) // 저녁 8시
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            // 현재 시간이 이미 토요일 8시를 지났다면 다음 주 토요일로
            if (timeInMillis <= currentTime.timeInMillis) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        
        val delay = targetTime.timeInMillis - currentTime.timeInMillis
        
        // 제약 조건 설정
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // 배터리 낮아도 실행
            .build()
        
        // 주기적 작업 요청 (매주 반복)
        val workRequest = PeriodicWorkRequestBuilder<LottoNotificationWorker>(
            7, TimeUnit.DAYS // 매주 반복
        )
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("lotto_notification")
            .build()
        
        // 기존 작업을 대체하면서 등록
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    /**
     * 알림 스케줄 취소
     */
    fun cancelNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
    
    /**
     * 알림 스케줄 상태 확인
     */
    fun isNotificationScheduled(context: Context): Boolean {
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        return workInfos.any { !it.state.isFinished }
    }
}
