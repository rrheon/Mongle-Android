package com.mongle.android.ui.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import com.mongle.android.ui.common.AppError
import androidx.lifecycle.viewModelScope
import com.mongle.android.data.local.UnreadBadgeStore
import com.mongle.android.data.remote.AppNotification
import com.mongle.android.data.remote.ApiNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val errorMessage: String? = null,
    /**
     * 미읽음 수. computed property 대신 stored property 로 두고
     * notifications mutation 시점에만 갱신해 매 composable 재실행마다
     * O(n) count 가 반복되는 비용을 제거한다.
     */
    val unreadCount: Int = 0
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: ApiNotificationRepository,
    private val unreadBadgeStore: UnreadBadgeStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** 미읽음 수를 notifications 기준으로 재계산해 stored 값으로 반영한다. */
    private fun refreshDerived(state: NotificationUiState): NotificationUiState {
        val next = state.copy(unreadCount = state.notifications.count { !it.isRead })
        // iOS MG-39 패리티 — 알림 mutation/load 직후 OS 아이콘 배지 숫자를 동기화한다.
        // setNumber() 가 적용된 활성 알림에는 영향이 없지만, 다음 push 도착 시점부터
        // 사용자가 mark-read 한 상태에 맞춰 정확한 카운터로 시작한다.
        unreadBadgeStore.set(next.unreadCount)
        return next
    }

    /**
     * 인앱에서 알림을 모두 읽음/삭제 처리한 시점에 OS 알림 트레이의 푸시도 정리한다.
     * 시스템 배지(채널 dot/숫자)는 OS 가 활성 알림 기준으로 자동 갱신하므로
     * cancel 만으로 OS 배지 ↔ 인앱 미읽음 카운트 분리가 해소된다.
     */
    private fun cancelOsNotifications() {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    /**
     * 알림 스코프.
     * - null: 그룹선택 화면에서 진입한 전역 목록
     * - not null: 특정 그룹 내부에서 진입한 그룹 한정 목록
     *
     * loadNotifications / markAllAsRead / deleteAll 이 이 스코프를 서버에 전달한다.
     */
    private var scopeFamilyId: String? = null

    init {
        // ViewModel 생성 즉시 기본 로드(scopeFamilyId=null, 전역).
        // Screen 의 LaunchedEffect(currentFamilyId) 가 이후 올바른 스코프로 재호출한다.
        // 딥링크·푸시 탭 → 재진입 경로에서 Compose 의 LaunchedEffect key 가 변하지 않아
        // 재실행되지 않는 케이스를 커버하기 위한 안전망.
        loadNotifications()
    }

    fun loadNotifications(familyId: String? = scopeFamilyId) {
        scopeFamilyId = familyId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val items = notificationRepository.getNotifications(familyId = familyId)
                _uiState.update { refreshDerived(it.copy(isLoading = false, notifications = items)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = AppError.from(e).toastMessage) }
            }
        }
    }

    fun onMarkAsRead(notificationId: String) {
        // 낙관적 업데이트
        _uiState.update { state ->
            refreshDerived(
                state.copy(
                    notifications = state.notifications.map { n ->
                        if (n.id == notificationId) n.copy(isRead = true) else n
                    }
                )
            )
        }
        viewModelScope.launch {
            runCatching { notificationRepository.markAsRead(notificationId) }
                .onFailure { e ->
                    // optimistic update 는 유지(rollback 안 함). 실패 시 사용자 안내만 추가.
                    _uiState.update { it.copy(errorMessage = AppError.from(e).toastMessage) }
                }
        }
    }

    fun onMarkAllAsRead() {
        val familyId = scopeFamilyId
        _uiState.update { state ->
            // 스코프가 지정되어 있으면 해당 그룹 알림만, 아니면 전체를 읽음 처리
            val updated = state.notifications.map { n ->
                if (familyId == null || n.familyId == familyId) n.copy(isRead = true) else n
            }
            refreshDerived(state.copy(notifications = updated))
        }
        cancelOsNotifications()
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching { notificationRepository.markAllAsRead(familyId) }
                    .onFailure { e ->
                        _uiState.update { it.copy(errorMessage = AppError.from(e).toastMessage) }
                    }
            }
        }
    }

    fun onDeleteNotification(notificationId: String) {
        _uiState.update { state ->
            refreshDerived(
                state.copy(notifications = state.notifications.filter { it.id != notificationId })
            )
        }
        // 화면을 즉시 벗어나도 서버 삭제가 완료되도록 NonCancellable 컨텍스트에서 실행한다.
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching { notificationRepository.deleteNotification(notificationId) }
                    .onFailure { e ->
                        _uiState.update { it.copy(errorMessage = AppError.from(e).toastMessage) }
                    }
            }
        }
    }

    /**
     * 주어진 familyId 범위의 알림만 제거한다.
     * - familyId == null: 전체 알림 제거 (그룹 선택 화면에서 진입한 경우)
     * - familyId != null: 해당 그룹에 속한 알림만 제거. 그룹 무관(familyId == null)
     *   알림은 다른 그룹에서도 계속 노출되어야 하므로 남겨둔다.
     *
     * 파라미터 미지정 시 현재 스코프(scopeFamilyId)를 사용한다.
     */
    fun onDeleteAll(familyId: String? = scopeFamilyId) {
        val current = _uiState.value.notifications
        val toDelete = if (familyId == null) {
            current
        } else {
            current.filter { it.familyId == familyId }
        }
        if (toDelete.isEmpty()) return
        val remaining = current - toDelete.toSet()
        _uiState.update { refreshDerived(it.copy(notifications = remaining)) }
        cancelOsNotifications()
        // 서버 일괄 삭제 API 사용 — 이전에는 개별 삭제를 반복 호출했지만
        // 서버에 DELETE /notifications?group_id= 가 추가되어 1회 호출로 처리한다.
        // NonCancellable 로 화면 전환 시에도 요청 완료 보장.
        viewModelScope.launch {
            withContext(NonCancellable) {
                runCatching { notificationRepository.deleteAll(familyId) }
                    .onFailure { e ->
                        _uiState.update { it.copy(errorMessage = AppError.from(e).toastMessage) }
                    }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
