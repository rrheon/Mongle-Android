package com.mongle.android.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AnswerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class FamilyAnswer(
    val id: UUID = UUID.randomUUID(),
    val user: User,
    val answer: Answer
)

data class QuestionDetailUiState(
    val question: Question? = null,
    val currentUser: User? = null,
    val myAnswer: Answer? = null,
    val familyAnswers: List<FamilyAnswer> = emptyList(),
    val answerText: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val hasMyAnswer: Boolean get() = myAnswer != null
    val isValidAnswer: Boolean get() = answerText.trim().isNotEmpty()
}

sealed class QuestionDetailEvent {
    data class AnswerSubmitted(val answer: Answer) : QuestionDetailEvent()
    data object Closed : QuestionDetailEvent()
}

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    private val answerRepository: AnswerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuestionDetailEvent>()
    val events: SharedFlow<QuestionDetailEvent> = _events.asSharedFlow()

    fun initialize(question: Question, currentUser: User?) {
        _uiState.update {
            it.copy(
                question = question,
                currentUser = currentUser,
                isLoading = true
            )
        }
        loadFamilyAnswers(question)
    }

    private fun loadFamilyAnswers(question: Question) {
        viewModelScope.launch {
            delay(500) // 네트워크 시뮬레이션
            val mockUsers = listOf(
                User(UUID.randomUUID(), "dad@example.com", "아빠", null, FamilyRole.FATHER, Date()),
                User(UUID.randomUUID(), "mom@example.com", "엄마", null, FamilyRole.MOTHER, Date())
            )
            val mockFamilyAnswers = mockUsers.map { user ->
                FamilyAnswer(
                    user = user,
                    answer = Answer(
                        id = UUID.randomUUID(),
                        dailyQuestionId = question.id,
                        userId = user.id,
                        content = "${user.name}의 답변입니다. 오늘 하루도 감사한 일이 많았어요.",
                        imageUrl = null,
                        createdAt = Date()
                    )
                )
            }
            _uiState.update {
                it.copy(isLoading = false, familyAnswers = mockFamilyAnswers)
            }
        }
    }

    fun onAnswerTextChanged(text: String) {
        _uiState.update { it.copy(answerText = text, errorMessage = null) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        if (!state.isValidAnswer) {
            _uiState.update { it.copy(errorMessage = "답변을 입력해주세요.") }
            return
        }
        val question = state.question ?: return
        val userId = state.currentUser?.id ?: UUID.randomUUID()

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            delay(1000) // 네트워크 시뮬레이션
            val answer = Answer(
                id = state.myAnswer?.id ?: UUID.randomUUID(),
                dailyQuestionId = question.id,
                userId = userId,
                content = state.answerText.trim(),
                imageUrl = null,
                createdAt = Date(),
                updatedAt = if (state.myAnswer != null) Date() else null
            )
            val savedAnswer = runCatching { answerRepository.create(answer) }.getOrDefault(answer)
            _uiState.update {
                it.copy(isSubmitting = false, myAnswer = savedAnswer)
            }
            _events.emit(QuestionDetailEvent.AnswerSubmitted(savedAnswer))
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun close() {
        viewModelScope.launch {
            _events.emit(QuestionDetailEvent.Closed)
        }
    }
}
