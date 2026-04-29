package com.mongle.android.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "mongle_badge"
private const val KEY_UNREAD = "unread_count"

/**
 * iOS MG-39 패리티 — 앱 아이콘 배지의 미읽음 수 source-of-truth.
 *
 * Android 런처는 NotificationCompat.Builder.setNumber(n) 호출이 있어야 아이콘 위에
 * 숫자를 그린다. 이 store 는 FCM Service / NotificationViewModel / RootViewModel 사이에서
 * 공유되어 push 도착·mark-read·로그아웃 시점에 배지 숫자를 일관되게 동기화한다.
 *
 * 서버 unread-count API 도입 전까지는 클라 push 카운터로만 동작하며,
 * NotificationViewModel 가 알림 목록을 로드할 때 서버 기준 unread 수로 재정렬된다.
 */
@Singleton
class UnreadBadgeStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun get(): Int = prefs.getInt(KEY_UNREAD, 0).coerceAtLeast(0)

    fun set(count: Int) {
        prefs.edit().putInt(KEY_UNREAD, count.coerceAtLeast(0)).apply()
    }

    fun incrementAndGet(): Int {
        val next = (prefs.getInt(KEY_UNREAD, 0) + 1).coerceAtLeast(0)
        prefs.edit().putInt(KEY_UNREAD, next).apply()
        return next
    }

    fun clear() {
        prefs.edit().remove(KEY_UNREAD).apply()
    }
}
