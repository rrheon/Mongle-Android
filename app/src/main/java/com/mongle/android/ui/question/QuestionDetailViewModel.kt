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
    val selectedMoodIndex: Int? = null,
    val showMoodRequiredAlert: Boolean = false,
    val showEditConfirmDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val hasMyAnswer: Boolean get() = myAnswer != null
    val isValidAnswer: Boolean get() = answerText.trim().isNotEmpty() && selectedMoodIndex != null
}

sealed class QuestionDetailEvent {
    data class AnswerSubmitted(val answer: Answer, val isNewAnswer: Boolean) : QuestionDetailEvent()
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
        viewModelScope.launch {
            try {
                val allAnswers = answerRepository.getByDailyQuestion(question.id)
                val myAnswer = currentUser?.id?.let {
                    runCatching { answerRepository.getByUserAndDailyQuestion(question.id, it) }.getOrNull()
                }

                val members = _uiState.value.familyMembers
                val familyAnswers = allAnswers
                    .filter { it.userId != currentUser?.id }
                    .mapNotNull { answer ->
                        val user = members.firstOrNull { it.id == answer.userId }
                        user?.let { FamilyAnswer(user = it, answer = answer) }
                    }

                val moodIds = listOf("happy", "calm", "loved", "sad", "tired")
                val moodIndex = myAnswer?.moodId?.let { moodIds.indexOf(it).takeIf { idx -> idx >= 0 } }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        myAnswer = myAnswer,
                        familyAnswers = familyAnswers,
                        answerText = myAnswer?.content ?: "",
                        selectedMoodIndex = moodIndex
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onAnswerTextChanged(text: String) {
        if (text.length > 200) return
        _uiState.update { it.copy(answerText = text, errorMessage = null) }
    }

    fun onMoodSelected(index: Int) {
        _uiState.update { it.copy(selectedMoodIndex = index, showMoodRequiredAlert = false) }
    }

    fun dismissMoodRequiredAlert() {
        _uiState.update { it.copy(showMoodRequiredAlert = false) }
    }

    fun dismissEditConfirmDialog() {
        _uiState.update { it.copy(showEditConfirmDialog = false) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        if (state.answerText.trim().isEmpty()) {
            _uiState.update { it.copy(errorMessage = "답변을 입력해주세요.") }
            return
        }
        if (state.selectedMoodIndex == null) {
            _uiState.update { it.copy(showMoodRequiredAlert = true) }
            return
        }
        // 기존 답변이 있으면 수정 확인 팝업 먼저 표시
        if (state.myAnswer != null) {
            _uiState.update { it.copy(showEditConfirmDialog = true) }
            return
        }
        doSubmitAnswer()
    }

    fun confirmEditAnswer() {
        _uiState.update { it.copy(showEditConfirmDialog = false) }
        doSubmitAnswer()
    }

    private fun doSubmitAnswer() {
        val state = _uiState.value
        val question = state.question ?: return
        val userId = state.currentUser?.id ?: return

        val moodIds = listOf("happy", "calm", "loved", "sad", "tired")
        val moodId = state.selectedMoodIndex?.let { moodIds.getOrNull(it) }
        val isNewAnswer = state.myAnswer == null

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val answer = Answer(
                    id = state.myAnswer?.id ?: UUID.randomUUID(),
                    dailyQuestionId = question.id,
                    userId = userId,
                    content = state.answerText.trim(),
                    imageUrl = null,
                    moodId = moodId,
                    createdAt = Date(),
                    updatedAt = if (state.myAnswer != null) Date() else null
                )
                val savedAnswer = if (state.myAnswer != null) {
                    answerRepository.update(answer)
                } else {
                    answerRepository.create(answer)
                }
                _uiState.update { it.copy(isSubmitting = false, myAnswer = savedAnswer) }
                _events.emit(QuestionDetailEvent.AnswerSubmitted(savedAnswer, isNewAnswer))
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
