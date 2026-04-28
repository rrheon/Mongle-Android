package com.mongle.android.ui.question

import androidx.lifecycle.ViewModel
import com.mongle.android.ui.common.AppError
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WriteQuestionUiState(
    val questionText: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean get() = questionText.trim().isNotEmpty() && !isSubmitting
}

sealed class WriteQuestionEvent {
    data class QuestionSubmitted(val question: Question) : WriteQuestionEvent()
}

@HiltViewModel
class WriteQuestionViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WriteQuestionUiState())
    val uiState: StateFlow<WriteQuestionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WriteQuestionEvent>()
    val events: SharedFlow<WriteQuestionEvent> = _events.asSharedFlow()

    fun onQuestionTextChanged(text: String) {
        _uiState.update { it.copy(questionText = text, errorMessage = null) }
    }

    fun onSubmit() {
        val content = _uiState.value.questionText.trim()
        if (content.isEmpty() || _uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val question = questionRepository.createCustomQuestion(content)
                _events.emit(WriteQuestionEvent.QuestionSubmitted(question))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = AppError.from(e).toastMessage
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
