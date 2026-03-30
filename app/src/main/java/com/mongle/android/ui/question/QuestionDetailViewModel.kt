package com.mongle.android.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AnswerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val familyMembers: List<User> = emptyList(),
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

    fun initialize(question: Question, currentUser: User?, familyMembers: List<User> = emptyList()) {
        _uiState.update {
            it.copy(
                question = question,
                currentUser = currentUser,
                familyMembers = familyMembers,
                isLoading = true
            )
        }
        loadAnswers(question, currentUser)
    }

    private fun loadAnswers(question: Question, currentUser: User?) {
        val dailyQId = question.dailyQuestionId
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return
            }

        viewModelScope.launch {
            try {
                val allAnswers = answerRepository.getByDailyQuestion(dailyQId)
                val myAnswer = currentUser?.id?.let {
                    runCatching { answerRepository.getByUserAndDailyQuestion(dailyQId, it) }.getOrNull()
                }

                val members = _uiState.value.familyMembers
                val familyAnswers = allAnswers
                    .filter { it.userId != currentUser?.id }
                    .mapNotNull { answer ->
                        val user = members.firstOrNull { it.id == answer.userId }
                        user?.let { FamilyAnswer(user = it, answer = answer) }
                    }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        myAnswer = myAnswer,
                        familyAnswers = familyAnswers,
                        answerText = myAnswer?.content ?: ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
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
        val question = state.question ?: run {
            _uiState.update { it.copy(errorMessage = "질문 정보를 불러올 수 없습니다.") }
            return
        }
        val userId = state.currentUser?.id ?: run {
            _uiState.update { it.copy(errorMessage = "로그인 정보를 확인해주세요.") }
            return
        }
        val dailyQId = question.dailyQuestionId
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: run {
                _uiState.update { it.copy(errorMessage = "질문 정보를 불러올 수 없습니다.") }
                return
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val answer = Answer(
                    id = state.myAnswer?.id ?: UUID.randomUUID(),
                    dailyQuestionId = dailyQId,
                    userId = userId,
                    content = state.answerText.trim(),
                    imageUrl = null,
                    createdAt = Date(),
                    updatedAt = if (state.myAnswer != null) Date() else null
                )
                val savedAnswer = if (state.myAnswer != null) {
                    answerRepository.update(answer)
                } else {
                    answerRepository.create(answer)
                }
                _uiState.update { it.copy(isSubmitting = false, myAnswer = savedAnswer) }
                _events.emit(QuestionDetailEvent.AnswerSubmitted(savedAnswer))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "답변 제출에 실패했습니다.")
                }
            }
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
