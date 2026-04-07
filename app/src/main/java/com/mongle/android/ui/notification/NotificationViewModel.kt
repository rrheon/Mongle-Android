package com.mongle.android.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.data.remote.AppNotification
import com.mongle.android.data.remote.ApiNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: ApiNotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val items = notificationRepository.getNotifications()
                _uiState.update { it.copy(isLoading = false, notifications = items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onMarkAsRead(notificationId: String) {
        // 낙관적 업데이트
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map { n ->
                    if (n.id == notificationId) n.copy(isRead = true) else n
                }
            )
        }
        viewModelScope.launch {
            runCatching { notificationRepository.markAsRead(notificationId) }
        }
    }

    fun onMarkAllAsRead() {
        _uiState.update { state ->
            state.copy(notifications = state.notifications.map { it.copy(isRead = true) })
        }
        viewModelScope.launch {
            runCatching { notificationRepository.markAllAsRead() }
        }
    }

    fun onDeleteNotification(notificationId: String) {
        _uiState.update { state ->
            state.copy(notifications = state.notifications.filter { it.id != notificationId })
        }
        viewModelScope.launch {
            runCatching { notificationRepository.deleteNotification(notificationId) }
        }
    }

    /**
     * 주어진 familyId 범위의 알림만 제거한다.
     * - familyId == null: 전체 알림 제거 (그룹 선택 화면에서 진입한 경우)
     * - familyId != null: 해당 그룹에 속한 알림만 제거. 그룹 무관(familyId == null)
     *   알림은 다른 그룹에서도 계속 노출되어야 하므로 남겨둔다.
     */
    fun onDeleteAll(familyId: String? = null) {
        val current = _uiState.value.notifications
        val toDelete = if (familyId == null) {
            current
        } else {
            current.filter { it.familyId == familyId }
        }
        if (toDelete.isEmpty()) return
        val remaining = current - toDelete.toSet()
        _uiState.update { it.copy(notifications = remaining) }
        viewModelScope.launch {
            toDelete.forEach { n ->
                runCatching { notificationRepository.deleteNotification(n.id) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
