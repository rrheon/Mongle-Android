package com.mongle.android.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mongle.android.MainActivity
import com.ycompany.Monggle.R

class MongleFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 토큰 저장 실패가 silent 하게 묻히지 않도록 try-catch + 로그.
        // 저장 실패 시 RootViewModel.loadHomeData 의 서버 등록도 건너뛰게 됨.
        try {
            getSharedPreferences("fcm", Context.MODE_PRIVATE)
                .edit().putString("token", token).apply()
        } catch (e: Exception) {
            android.util.Log.e("MongleFcmService", "FCM 토큰 저장 실패", e)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: ""
        showNotification(title, body, type)
    }

    private fun showNotification(title: String, body: String, type: String = "") {
        val channelId = "mongle_default"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "몽글 알림", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "몽글 앱 알림" }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 앱이 foreground 일 때 heads-up 배너로 흐름을 끊지 않도록 priority 를 낮춘다.
        // 알림은 트레이/리스트로는 정상 노출되지만 헤드업 팝업은 표시되지 않는다.
        val isForeground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
        val priority = if (isForeground) NotificationCompat.PRIORITY_LOW
                       else NotificationCompat.PRIORITY_HIGH

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(priority)
            .setContentIntent(pending)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
