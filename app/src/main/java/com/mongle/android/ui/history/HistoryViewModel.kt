package com.mongle.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.QuestionCategory
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
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class HistoryItem(
    val id: UUID = UUID.randomUUID(),
    val date: Date,
    val question: Question,
    val answerCount: Int,
    val totalMembers: Int,
    val isCompleted: Boolean,
    val userAnswered: Boolean
)

data class HistoryUiState(
    val selectedDate: Date = Date(),
    val currentMonth: Date = Date(),
    val historyItems: Map<Long, HistoryItem> = emptyMap(),
    val selectedItem: HistoryItem? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val monthTitle: String
        get() {
            val cal = Calendar.getInstance().apply { time = currentMonth }
            return "${cal.get(Calendar.YEAR)}년 ${cal.get(Calendar.MONTH) + 1}월"
        }
}

sealed class HistoryEvent {
    data class NavigateToQuestionDetail(val question: Question) : HistoryEvent()
}

@HiltViewModel
class HistoryViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events: SharedFlow<HistoryEvent> = _events.asSharedFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(500)
            val mockData = generateMockData()
            _uiState.update {
                it.copy(isLoading = false, historyItems = mockData)
            }
        }
    }

    fun onDateSelected(date: Date) {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayKey = cal.timeInMillis
        val item = _uiState.value.historyItems[dayKey]
        _uiState.update { it.copy(selectedDate = date, selectedItem = item) }
    }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply { time = _uiState.value.currentMonth }
        cal.add(Calendar.MONTH, -1)
        _uiState.update { it.copy(currentMonth = cal.time) }
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply { time = _uiState.value.currentMonth }
        cal.add(Calendar.MONTH, 1)
        _uiState.update { it.copy(currentMonth = cal.time) }
    }

    fun onItemTapped(item: HistoryItem) {
        viewModelScope.launch {
            _events.emit(HistoryEvent.NavigateToQuestionDetail(item.question))
        }
    }

    private fun generateMockData(): Map<Long, HistoryItem> {
        val questions = listOf(
            Question(UUID.randomUUID(), "오늘 가장 감사했던 순간은 언제인가요?", QuestionCategory.GRATITUDE, 1),
            Question(UUID.randomUUID(), "어릴 때 가장 좋아했던 놀이는 무엇이었나요?", QuestionCategory.MEMORY, 2),
            Question(UUID.randomUUID(), "요즘 가장 관심 있는 것은 무엇인가요?", QuestionCategory.DAILY, 3),
            Question(UUID.randomUUID(), "10년 후 어떤 모습이고 싶은가요?", QuestionCategory.FUTURE, 4),
            Question(UUID.randomUUID(), "가족에게 가장 고마웠던 순간은?", QuestionCategory.GRATITUDE, 5)
        )
        val result = mutableMapOf<Long, HistoryItem>()
        val cal = Calendar.getInstance()
        repeat(30) { dayOffset ->
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (Math.random() < 0.7) {
                val q = questions[dayOffset % questions.size]
                val totalMembers = 4
                val answerCount = (Math.random() * totalMembers).toInt()
                result[cal.timeInMillis] = HistoryItem(
                    date = cal.time,
                    question = q,
                    answerCount = answerCount,
                    totalMembers = totalMembers,
                    isCompleted = answerCount == totalMembers,
                    userAnswered = Math.random() < 0.8
                )
            }
        }
        return result
    }
}
