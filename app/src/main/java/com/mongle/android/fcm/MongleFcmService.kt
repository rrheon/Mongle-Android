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
import com.mongle.android.data.local.UnreadBadgeStore
import com.mongle.android.util.AppForegroundTracker
import com.ycompany.Monggle.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MongleFcmService : FirebaseMessagingService() {

    companion object {
        // MG-96 currentTimeMillis().toInt() 는 49.7일 wrap-around + 동일 millisecond 충돌 가능.
        // AtomicInteger 단조 증가로 알림 ID 충돌을 원천 차단.
        private val notificationIdSeq = java.util.concurrent.atomic.AtomicInteger(1000)
    }

    @Inject lateinit var unreadBadgeStore: UnreadBadgeStore
    @Inject lateinit var appForegroundTracker: AppForegroundTracker

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

        // iOS MG-55 패리티 — 로그인/온보딩/약관 도중에는 트레이 배너로 흐름을 끊지 않는다.
        // unread 카운터는 그대로 증가시켜 사용자가 인증 후 알림 화면에서 누적 확인 가능.
        if (appForegroundTracker.isInAuthFlow()) {
            unreadBadgeStore.incrementAndGet()
            return
        }

        val unread = unreadBadgeStore.incrementAndGet()
        showNotification(title, body, type, unread)
    }

    private fun showNotification(title: String, body: String, type: String, unread: Int) {
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
            // iOS MG-39 패리티 — 런처 아이콘 배지에 미읽음 숫자 표시. Pixel/One UI 등 setNumber
            // 를 지원하는 런처에서 active. 미지원 런처에서는 dot 만 표시되며 이 값은 무시된다.
            .setNumber(unread)
            .setContentIntent(pending)
            .build()

        manager.notify(notificationIdSeq.incrementAndGet(), notification)
    }
}
