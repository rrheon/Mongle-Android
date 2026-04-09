package com.mongle.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.HistoryAnswerSummary
import com.mongle.android.domain.model.HistorySkippedSummary
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.repository.AuthRepository
import com.mongle.android.domain.repository.MongleRepository
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
    val userAnswered: Boolean,
    val isSkipped: Boolean = false,
    val memberAnswers: List<HistoryAnswerSummary> = emptyList(),
    val skippedMembers: List<HistorySkippedSummary> = emptyList()
)

data class HistoryUiState(
    val selectedDate: Date = Date(),
    val currentMonth: Date = Date(),
    val historyItems: Map<Long, HistoryItem> = emptyMap(),
    val selectedItem: HistoryItem? = null,
    val moodCounts: Map<String, Int> = emptyMap(),
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
class HistoryViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val mongleRepository: MongleRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events: SharedFlow<HistoryEvent> = _events.asSharedFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedItem = null) }
            try {
                val familyResult = runCatching { mongleRepository.getMyFamily() }.getOrNull()
                val totalMembers = familyResult?.second?.size ?: 1
                val currentUser = runCatching { authRepository.getCurrentUser() }.getOrNull()

                val history = questionRepository.getDailyHistory(page = 1, limit = 50)

                val historyMap = mutableMapOf<Long, HistoryItem>()
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                history.forEach { item ->
                    val cal = Calendar.getInstance().apply {
                        time = item.date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    // 본인이 넘겼는데 서버 응답에 본인이 포함되지 않은 경우 직접 추가
                    val mergedSkipped = if (item.hasMySkipped && currentUser != null &&
                        item.skippedMembers.none { it.userId == currentUser.id.toString() }
                    ) {
                        item.skippedMembers + HistorySkippedSummary(
                            userId = currentUser.id.toString(),
                            userName = currentUser.name,
                            colorId = currentUser.moodId
                        )
                    } else {
                        item.skippedMembers
                    }

                    historyMap[cal.timeInMillis] = HistoryItem(
                        id = runCatching { UUID.fromString(item.id) }.getOrElse { UUID.randomUUID() },
                        date = item.date,
                        question = item.question,
                        answerCount = item.familyAnswerCount,
                        totalMembers = totalMembers,
                        isCompleted = item.familyAnswerCount >= totalMembers,
                        userAnswered = item.hasMyAnswer,
                        isSkipped = item.hasMySkipped,
                        memberAnswers = item.answers,
                        skippedMembers = mergedSkipped
                    )
                }

                val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -14) }.time
                val moodCounts = mutableMapOf<String, Int>()
                historyMap.values.forEach { item ->
                    if (item.date >= cutoff) {
                        item.memberAnswers.forEach { answer ->
                            answer.moodId?.let { mood ->
                                moodCounts[mood] = (moodCounts[mood] ?: 0) + 1
                            }
                        }
                    }
                }

                // 오늘 날짜에 해당하는 아이템 자동 선택
                val todayItem = historyMap[todayCal.timeInMillis]

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        historyItems = historyMap,
                        moodCounts = moodCounts,
                        selectedDate = Date(),
                        selectedItem = todayItem
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message)
                }
            }
        }
    }

    fun onDateSelected(date: Date) {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
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

    fun refresh() {
        loadHistory()
    }
}
