package com.mongle.android.ui.notification

import com.mongle.android.data.remote.AppNotification
import com.mongle.android.data.remote.ApiNotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: ApiNotificationRepository
    private lateinit var viewModel: NotificationViewModel

    private val sampleNotifications = listOf(
        AppNotification("n1", "NEW_QUESTION", "오늘의 질문", "새 질문이 도착했어요", isRead = false, createdAt = "2026-03-24T00:00:00Z"),
        AppNotification("n2", "MEMBER_ANSWERED", "가족 답변", "아빠가 답변했어요", isRead = true, createdAt = "2026-03-23T00:00:00Z"),
        AppNotification("n3", "BADGE_EARNED", "배지 획득", "새 배지를 받았어요", isRead = false, createdAt = "2026-03-22T00:00:00Z"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - init / loadNotifications

    @Test
    fun `init 시 알림 목록을 자동으로 로드한다`() = runTest {
        coEvery { repository.getNotifications() } returns sampleNotifications

        viewModel = NotificationViewModel(repository)

        assertEquals(3, viewModel.uiState.value.notifications.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadNotifications 성공 시 isLoading이 false가 된다`() = runTest {
        coEvery { repository.getNotifications() } returns sampleNotifications

        viewModel = NotificationViewModel(repository)
        viewModel.loadNotifications()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `loadNotifications 실패 시 errorMessage가 설정된다`() = runTest {
        coEvery { repository.getNotifications() } throws Exception("네트워크 오류")

        viewModel = NotificationViewModel(repository)

        assertEquals("네트워크 오류", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `빈 알림 목록도 정상 처리된다`() = runTest {
        coEvery { repository.getNotifications() } returns emptyList()

        viewModel = NotificationViewModel(repository)

        assertTrue(viewModel.uiState.value.notifications.isEmpty())
        assertEquals(0, viewModel.uiState.value.unreadCount)
    }

    // MARK: - unreadCount 계산 프로퍼티

    @Test
    fun `unreadCount는 읽지 않은 알림 수를 반환한다`() = runTest {
        coEvery { repository.getNotifications() } returns sampleNotifications

        viewModel = NotificationViewModel(repository)

        // n1, n3이 unread → 2개
        assertEquals(2, viewModel.uiState.value.unreadCount)
    }

    // MARK: - onMarkAsRead

    @Test
    fun `onMarkAsRead 호출 시 낙관적 업데이트로 즉시 읽음 처리된다`() = runTest {
        coEvery { repository.getNotifications() } returns sampleNotifications
        coEvery { repository.markAsRead(any()) } returns sampleNotifications[0].copy(isRead = true)

        viewModel = NotificationViewModel(repository)
        viewModel.onMarkAsRead("n1")

        val n1 = viewModel.uiState.value.notifications.find { it.id == "n1" }
        assertTrue(n1!!.isRead)
        // unread는 n3 하나만 남음
        assertEquals(1, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun `onMarkAsRead는 다른 알림에 영향을 주지 않는다`() = runTest {
        coEvery { repository.getNotifications() } returns sampleNotifications
        coEvery { repository.markAsRead(any()) } returns sampleNotifications[0].copy(isRead = true)

        viewModel = NotificationViewModel(repository)
        viewModel.onMarkAsRead("n1")

        val n2 = viewModel.uiState.value.notifications.find { it.id == "n2" }
        val n3 = viewModel.uiState.value.notifications.find { it.id == "n3" }
        assertTrue(n2!!.isRead) // 원래 읽음
        assertFalse(n3!!.isRead) // 원래 미읽음, 변경 없음
    }

    @Test
    fun `onMarkAsRead API 호출 실패해도 낙관적 업데이트는 유지된다`() = runTest {
        coEvery { repository.getNotifications() } returns sampleNotifications
        coEvery { repository.markAsRead(any()) } throws Exception("서버 오류")

        viewModel = NotificationViewModel(repository)
        viewModel.onMarkAsRead("n1")

        // 낙관적 업데이트이므로 UI는 이미 읽음 처리됨
        val n1 = viewModel.uiState.value.notifications.find { it.id == "n1" }
        assertTrue(n1!!.isRead)
    }

    // MARK: - onMarkAllAsRead

    @Test
    fun `onMarkAllAsRead 호출 시 모든 알림이 읽음 처리된다`() = runTest {
        coEvery { repository.getNotifications() } returns sampleNotifications
        coEvery { repository.markAllAsRead() } returns 2

        viewModel = NotificationViewModel(repository)
        viewModel.onMarkAllAsRead()

        val allRead = viewModel.uiState.value.notifications.all { it.isRead }
        assertTrue(allRead)
        assertEquals(0, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun `onMarkAllAsRead는 API를 호출한다`() = runTest {
        coEvery { repository.getNotifications() } returns sampleNotifications
        coEvery { repository.markAllAsRead() } returns 2

        viewModel = NotificationViewModel(repository)
        viewModel.onMarkAllAsRead()

        coVerify { repository.markAllAsRead() }
    }

    // MARK: - dismissError

    @Test
    fun `dismissError 호출 시 errorMessage가 null이 된다`() = runTest {
        coEvery { repository.getNotifications() } throws Exception("오류")

        viewModel = NotificationViewModel(repository)
        // 에러 상태 확인
        assertEquals("오류", viewModel.uiState.value.errorMessage)

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
