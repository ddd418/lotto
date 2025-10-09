package com.lotto.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lotto.app.MainActivity
import com.lotto.app.R

/**
 * 로또 추첨일 알림을 담당하는 Worker
 * 매주 토요일 저녁 8시에 알림 전송
 */
class LottoNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // 알림 전송
        sendNotification()
        return Result.success()
    }
    
    private fun sendNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Android 8.0 이상에서는 NotificationChannel 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "로또 추첨 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "매주 토요일 로또 추첨 알림"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // 앱 실행 Intent
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 알림 생성
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: 커스텀 아이콘으로 변경
            .setContentTitle("🎰 오늘은 로또 추첨일!")
            .setContentText("저녁 8시 45분 추첨 시작! 행운을 빌어요 🍀")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("오늘 저녁 8시 45분에 로또 추첨이 진행됩니다.\n구매하신 번호를 확인해보세요!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    companion object {
        const val CHANNEL_ID = "lotto_draw_channel"
        const val NOTIFICATION_ID = 1001
    }
}
