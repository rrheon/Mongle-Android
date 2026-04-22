package com.mongle.android.data.remote

import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: String,
    val colorId: String? = null,
    val familyId: String? = null
)

@Singleton
class ApiNotificationRepository @Inject constructor(
    private val api: MongleApiService
) {
    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val raw = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(parseServerMessage(raw))
        }
    }

    suspend fun getNotifications(limit: Int = 50, familyId: String? = null): List<AppNotification> = safeCall {
        api.getNotifications(limit, familyId).notifications.map { it.toApp() }
    }

    suspend fun markAsRead(notificationId: String): AppNotification = safeCall {
        api.markNotificationRead(notificationId).toApp()
    }

    suspend fun markAllAsRead(familyId: String? = null): Int = safeCall {
        api.markAllNotificationsRead(familyId).count
    }

    suspend fun deleteNotification(notificationId: String) = safeCall {
        api.deleteNotification(notificationId)
    }

    suspend fun deleteAll(familyId: String? = null): Int = safeCall {
        api.deleteAllNotifications(familyId).count
    }

    private fun NotificationDto.toApp() = AppNotification(
        id = id,
        type = type,
        title = title,
        body = body,
        isRead = isRead,
        createdAt = createdAt,
        colorId = colorId,
        familyId = familyId
    )
}
