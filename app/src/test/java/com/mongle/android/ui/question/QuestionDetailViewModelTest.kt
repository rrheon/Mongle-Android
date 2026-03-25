package com.mongle.android.ui.question

import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.QuestionCategory
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AnswerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var answerRepository: AnswerRepository
    private lateinit var viewModel: QuestionDetailViewModel

    private val dailyQId = UUID.randomUUID()

    private val mockUser = User(
        id = UUID.randomUUID(),
        email = "test@mongle.com",
        name = "홍길동",
        profileImageUrl = null,
        role = FamilyRole.FATHER,
        createdAt = Date()
    )

    private val mockOtherUser = User(
        id = UUID.randomUUID(),
        email = "other@mongle.com",
        name = "김영희",
        profileImageUrl = null,
        role = FamilyRole.MOTHER,
        createdAt = Date()
    )

    private val mockQuestion = Question(
        id = UUID.randomUUID(),
        content = "오늘 가장 감사했던 순간은?",
        category = QuestionCategory.GRATITUDE,
        order = 1,
        dailyQuestionId = dailyQId.toString()
    )

    private val mockAnswer = Answer(
        id = UUID.randomUUID(),
        dailyQuestionId = dailyQId,
        userId = mockUser.id,
        content = "가족과 함께한 저녁",
        imageUrl = null,
        createdAt = Date()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        answerRepository = mockk(relaxed = true)
        viewModel = QuestionDetailViewModel(answerRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - initialize

    @Test
    fun `initialize 호출 시 질문과 사용자 정보가 설정된다`() = runTest {
        coEvery { answerRepository.getByDailyQuestion(any()) } returns emptyList()
        coEvery { answerRepository.getByUserAndDailyQuestion(any(), any()) } returns null

        viewModel.initialize(mockQuestion, mockUser, listOf(mockUser))

        assertEquals(mockQuestion, viewModel.uiState.value.question)
        assertEquals(mockUser, viewModel.uiState.value.currentUser)
    }

    @Test
    fun `initialize 시 이미 답변이 있으면 answerText가 기존 답변 내용으로 설정된다`() = runTest {
        coEvery { answerRepository.getByDailyQuestion(any()) } returns listOf(mockAnswer)
        coEvery { answerRepository.getByUserAndDailyQuestion(dailyQId, mockUser.id) } returns mockAnswer

        viewModel.initialize(mockQuestion, mockUser, listOf(mockUser))

        assertEquals(mockAnswer.content, viewModel.uiState.value.answerText)
        assertNotNull(viewModel.uiState.value.myAnswer)
        assertTrue(viewModel.uiState.value.hasMyAnswer)
    }

    @Test
    fun `initialize 시 다른 가족 구성원의 답변은 familyAnswers에 포함된다`() = runTest {
        val otherAnswer = mockAnswer.copy(id = UUID.randomUUID(), userId = mockOtherUser.id)
        coEvery { answerRepository.getByDailyQuestion(any()) } returns listOf(otherAnswer)
        coEvery { answerRepository.getByUserAndDailyQuestion(any(), mockUser.id) } returns null

        viewModel.initialize(mockQuestion, mockUser, listOf(mockUser, mockOtherUser))

        assertEquals(1, viewModel.uiState.value.familyAnswers.size)
        assertEquals(mockOtherUser.id, viewModel.uiState.value.familyAnswers[0].user.id)
    }

    @Test
    fun `dailyQuestionId가 없는 질문은 로딩 없이 초기화된다`() = runTest {
        val questionWithoutId = mockQuestion.copy(dailyQuestionId = null)

        viewModel.initialize(questionWithoutId, mockUser)

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.myAnswer)
    }

    // MARK: - onAnswerTextChanged

    @Test
    fun `onAnswerTextChanged 호출 시 answerText가 업데이트된다`() {
        viewModel.onAnswerTextChanged("새로운 답변 내용")

        assertEquals("새로운 답변 내용", viewModel.uiState.value.answerText)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // MARK: - isValidAnswer 계산 프로퍼티

    @Test
    fun `빈 텍스트이면 isValidAnswer가 false이다`() {
        viewModel.onAnswerTextChanged("   ")

        assertFalse(viewModel.uiState.value.isValidAnswer)
    }

    @Test
    fun `텍스트가 있으면 isValidAnswer가 true이다`() {
        viewModel.onAnswerTextChanged("답변 내용")

        assertTrue(viewModel.uiState.value.isValidAnswer)
    }

    // MARK: - submitAnswer

    @Test
    fun `submitAnswer 시 빈 텍스트이면 errorMessage가 설정된다`() = runTest {
        coEvery { answerRepository.getByDailyQuestion(any()) } returns emptyList()
        coEvery { answerRepository.getByUserAndDailyQuestion(any(), any()) } returns null

        viewModel.initialize(mockQuestion, mockUser)
        viewModel.onAnswerTextChanged("")

        viewModel.submitAnswer()

        assertEquals("답변을 입력해주세요.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `submitAnswer 시 첫 답변이면 create가 호출된다`() = runTest {
        coEvery { answerRepository.getByDailyQuestion(any()) } returns emptyList()
        coEvery { answerRepository.getByUserAndDailyQuestion(any(), any()) } returns null
        coEvery { answerRepository.create(any()) } returns mockAnswer

        viewModel.initialize(mockQuestion, mockUser)
        viewModel.onAnswerTextChanged("처음 작성하는 답변")

        viewModel.submitAnswer()

        coVerify { answerRepository.create(any()) }
        coVerify(exactly = 0) { answerRepository.update(any()) }
    }

    @Test
    fun `submitAnswer 시 기존 답변이 있으면 update가 호출된다`() = runTest {
        coEvery { answerRepository.getByDailyQuestion(any()) } returns listOf(mockAnswer)
        coEvery { answerRepository.getByUserAndDailyQuestion(dailyQId, mockUser.id) } returns mockAnswer
        coEvery { answerRepository.update(any()) } returns mockAnswer.copy(content = "수정된 답변")

        viewModel.initialize(mockQuestion, mockUser)
        viewModel.onAnswerTextChanged("수정된 답변")

        viewModel.submitAnswer()

        coVerify { answerRepository.update(any()) }
        coVerify(exactly = 0) { answerRepository.create(any()) }
    }

    @Test
    fun `submitAnswer 성공 시 AnswerSubmitted 이벤트가 발생한다`() = runTest {
        coEvery { answerRepository.getByDailyQuestion(any()) } returns emptyList()
        coEvery { answerRepository.getByUserAndDailyQuestion(any(), any()) } returns null
        coEvery { answerRepository.create(any()) } returns mockAnswer

        viewModel.initialize(mockQuestion, mockUser)
        viewModel.onAnswerTextChanged("새로운 답변")

        val events = mutableListOf<QuestionDetailEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.submitAnswer()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(events.any { it is QuestionDetailEvent.AnswerSubmitted })
        job.cancel()
    }

    @Test
    fun `submitAnswer 실패 시 errorMessage가 설정된다`() = runTest {
        coEvery { answerRepository.getByDailyQuestion(any()) } returns emptyList()
        coEvery { answerRepository.getByUserAndDailyQuestion(any(), any()) } returns null
        coEvery { answerRepository.create(any()) } throws Exception("서버 오류")

        viewModel.initialize(mockQuestion, mockUser)
        viewModel.onAnswerTextChanged("답변 내용")

        viewModel.submitAnswer()

        assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `submitAnswer 시 dailyQuestionId가 없으면 에러를 설정한다`() = runTest {
        val questionWithoutId = mockQuestion.copy(dailyQuestionId = null)
        viewModel.initialize(questionWithoutId, mockUser)
        viewModel.onAnswerTextChanged("답변 내용")

        viewModel.submitAnswer()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    // MARK: - dismissError

    @Test
    fun `dismissError 호출 시 errorMessage가 null이 된다`() {
        viewModel.onAnswerTextChanged("")
        viewModel.submitAnswer() // errorMessage 설정

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
