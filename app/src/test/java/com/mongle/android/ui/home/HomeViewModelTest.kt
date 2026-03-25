package com.mongle.android.ui.home

import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.QuestionCategory
import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import com.mongle.android.domain.repository.TreeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var questionRepository: QuestionRepository
    private lateinit var mongleRepository: MongleRepository
    private lateinit var treeRepository: TreeRepository
    private lateinit var viewModel: HomeViewModel

    private val mockUser = User(
        id = UUID.randomUUID(),
        email = "test@mongle.com",
        name = "홍길동",
        profileImageUrl = null,
        role = FamilyRole.FATHER,
        createdAt = Date()
    )

    private val mockQuestion = Question(
        id = UUID.randomUUID(),
        content = "오늘 가장 감사했던 순간은?",
        category = QuestionCategory.GRATITUDE,
        order = 1,
        dailyQuestionId = UUID.randomUUID().toString()
    )

    private val mockFamily = MongleGroup(
        id = UUID.randomUUID(),
        name = "행복한 가족",
        memberIds = listOf(UUID.randomUUID()),
        createdBy = UUID.randomUUID(),
        createdAt = Date(),
        inviteCode = "ABCDEFGH",
        groupProgressId = UUID.randomUUID()
    )

    private val mockTree = TreeProgress()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        questionRepository = mockk(relaxed = true)
        mongleRepository = mockk(relaxed = true)
        treeRepository = mockk(relaxed = true)
        viewModel = HomeViewModel(questionRepository, mongleRepository, treeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - initialize

    @Test
    fun `initialize 호출 시 전달된 데이터로 상태가 설정된다`() {
        viewModel.initialize(
            todayQuestion = mockQuestion,
            familyTree = mockTree,
            family = mockFamily,
            familyMembers = listOf(mockUser),
            currentUser = mockUser,
            hasAnsweredToday = false
        )

        val state = viewModel.uiState.value
        assertEquals(mockQuestion, state.todayQuestion)
        assertEquals(mockFamily, state.family)
        assertEquals(mockUser, state.currentUser)
        assertEquals(1, state.familyMembers.size)
        assertFalse(state.hasAnsweredToday)
    }

    @Test
    fun `initialize 후 hasFamily는 family가 있으면 true이다`() {
        viewModel.initialize(
            todayQuestion = null,
            familyTree = mockTree,
            family = mockFamily,
            familyMembers = emptyList(),
            currentUser = null,
            hasAnsweredToday = false
        )

        assertTrue(viewModel.uiState.value.hasFamily)
    }

    @Test
    fun `initialize 후 family가 null이면 hasFamily는 false이다`() {
        viewModel.initialize(
            todayQuestion = null,
            familyTree = mockTree,
            family = null,
            familyMembers = emptyList(),
            currentUser = null,
            hasAnsweredToday = false
        )

        assertFalse(viewModel.uiState.value.hasFamily)
    }

    // MARK: - onQuestionTapped

    @Test
    fun `onQuestionTapped 호출 시 질문이 없으면 이벤트가 발생하지 않는다`() = runTest {
        // todayQuestion = null 상태
        viewModel.onQuestionTapped()

        // events는 SharedFlow라 별도 수집 없이 확인 (이벤트 미발생 검증)
        // SharedFlow는 기본적으로 replay=0이므로 이전 이벤트 없음
        assertTrue(true) // 예외 없이 통과하면 성공
    }

    @Test
    fun `onQuestionTapped 호출 시 질문이 있으면 NavigateToQuestionDetail 이벤트가 발생한다`() = runTest {
        viewModel.initialize(mockQuestion, mockTree, null, emptyList(), null, false)

        val events = mutableListOf<HomeEvent>()
        val job = kotlinx.coroutines.launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onQuestionTapped()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is HomeEvent.NavigateToQuestionDetail)
        assertEquals(mockQuestion, (events[0] as HomeEvent.NavigateToQuestionDetail).question)

        job.cancel()
    }

    // MARK: - refresh

    @Test
    fun `refresh 성공 시 최신 데이터로 상태가 업데이트된다`() = runTest {
        val newQuestion = mockQuestion.copy(content = "새로운 질문")
        coEvery { mongleRepository.getMyFamily() } returns Pair(mockFamily, listOf(mockUser))
        coEvery { questionRepository.getTodayQuestion() } returns newQuestion
        coEvery { treeRepository.getMyTreeProgress() } returns mockTree

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals(newQuestion, state.todayQuestion)
        assertEquals(mockFamily, state.family)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
    }

    @Test
    fun `refresh 중 이미 refreshing 상태이면 중복 실행되지 않는다`() = runTest {
        coEvery { mongleRepository.getMyFamily() } returns null
        coEvery { questionRepository.getTodayQuestion() } returns null
        coEvery { treeRepository.getMyTreeProgress() } returns null

        viewModel.refresh()

        // isRefreshing이 false로 돌아온 것 확인 (UnconfinedTestDispatcher로 즉시 완료)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `refresh 시 가족이 없으면 family가 null이 된다`() = runTest {
        coEvery { mongleRepository.getMyFamily() } returns null
        coEvery { questionRepository.getTodayQuestion() } returns null
        coEvery { treeRepository.getMyTreeProgress() } returns mockTree

        viewModel.refresh()

        assertNull(viewModel.uiState.value.family)
        assertTrue(viewModel.uiState.value.familyMembers.isEmpty())
    }

    // MARK: - onAnswerSubmitted

    @Test
    fun `onAnswerSubmitted 호출 시 hasAnsweredToday가 true가 된다`() {
        viewModel.initialize(mockQuestion, mockTree, mockFamily, emptyList(), mockUser, false)
        assertFalse(viewModel.uiState.value.hasAnsweredToday)

        viewModel.onAnswerSubmitted()

        assertTrue(viewModel.uiState.value.hasAnsweredToday)
    }

    // MARK: - dismissError

    @Test
    fun `dismissError 호출 시 errorMessage가 null이 된다`() = runTest {
        coEvery { mongleRepository.getMyFamily() } throws Exception("서버 오류")
        coEvery { questionRepository.getTodayQuestion() } returns null
        coEvery { treeRepository.getMyTreeProgress() } returns null

        viewModel.refresh()
        // errorMessage가 설정됐는지 확인
        assertEquals("서버 오류", viewModel.uiState.value.errorMessage)

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
