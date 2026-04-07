package com.mongle.android.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.data.remote.AppNotification
import com.mongle.android.data.remote.ApiNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            withContext(NonCancellable) {
                runCatching { notificationRepository.markAllAsRead() }
            }
        }
    }

    fun onDeleteNotification(notificationId: String) {
        _uiState.update { state ->
            state.copy(notifications = state.notifications.filter { it.id != notificationId })
        }
        // 화면을 즉시 벗어나도 서버 삭제가 완료되도록 NonCancellable 컨텍스트에서 실행한다.
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching { notificationRepository.deleteNotification(notificationId) }
            }
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
        // 사용자가 모두제거 직후 다른 화면으로 이동하면 viewModelScope가 취소되어
        // 일부 삭제 API가 실행되지 못해, 재진입 시 서버에서 알림이 다시 내려오는 문제가 있었다.
        // NonCancellable 컨텍스트로 감싸 모든 삭제 호출이 끝까지 수행되도록 한다.
        viewModelScope.launch {
            withContext(NonCancellable) {
                toDelete.forEach { n ->
                    runCatching { notificationRepository.deleteNotification(n.id) }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
