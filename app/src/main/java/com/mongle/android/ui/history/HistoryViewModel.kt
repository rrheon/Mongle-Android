package com.mongle.android.ui.history

import androidx.lifecycle.ViewModel
import com.mongle.android.ui.common.AppError
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

/**
 * 달력 한 셀에 필요한 사전 계산 정보. iOS MG-59 패리티.
 * 매 composable 재실행마다 Calendar.getInstance() 4~5회 호출하던 비용을 없애기 위해
 * monthly transition 시점(loadHistory/previousMonth/nextMonth)에 1회만 계산해 stored 한다.
 */
data class CalendarDayInfo(
    val date: Date,
    val dayString: String,
    val weekday: Int,            // Calendar.DAY_OF_WEEK (1=일~7=토)
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

data class HistoryUiState(
    val selectedDate: Date = Date(),
    val currentMonth: Date = Date(),
    val historyItems: Map<Long, HistoryItem> = emptyMap(),
    val selectedItem: HistoryItem? = null,
    val moodCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** 6주×7일 = 42 셀. null = 빈 칸. iOS MG-59 stored property 캐시. */
    val calendarDays: List<CalendarDayInfo?> = emptyList(),
    val monthTitle: String = ""
)

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
        // 초기 monthTitle/calendarDays 도 stored 로 미리 계산
        val initialMonth = _uiState.value.currentMonth
        _uiState.update {
            it.copy(
                monthTitle = computeMonthTitle(initialMonth),
                calendarDays = computeCalendarDays(initialMonth)
            )
        }
        loadHistory()
    }

    /** iOS MG-59 패리티 — 42 셀 사전 계산 */
    private fun computeCalendarDays(month: Date): List<CalendarDayInfo?> {
        val firstOfMonth = Calendar.getInstance().apply {
            time = month
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val firstDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 1   // 0=일요일
        val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val days = ArrayList<CalendarDayInfo?>(42)
        repeat(firstDayOfWeek) { days.add(null) }
        val cal = Calendar.getInstance().apply {
            time = firstOfMonth.time
        }
        repeat(daysInMonth) { i ->
            cal.set(Calendar.DAY_OF_MONTH, i + 1)
            val dayMillis = cal.timeInMillis
            days.add(
                CalendarDayInfo(
                    date = cal.time,
                    dayString = (i + 1).toString(),
                    weekday = cal.get(Calendar.DAY_OF_WEEK),
                    isCurrentMonth = true,
                    isToday = dayMillis == today
                )
            )
        }
        while (days.size < 42) days.add(null)
        return days
    }

    private fun computeMonthTitle(month: Date): String {
        val cal = Calendar.getInstance().apply { time = month }
        return "${cal.get(Calendar.YEAR)}년 ${cal.get(Calendar.MONTH) + 1}월"
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
                    it.copy(isLoading = false, errorMessage = AppError.from(e).toastMessage)
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
        val newMonth = cal.time
        _uiState.update {
            it.copy(
                currentMonth = newMonth,
                monthTitle = computeMonthTitle(newMonth),
                calendarDays = computeCalendarDays(newMonth)
            )
        }
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply { time = _uiState.value.currentMonth }
        cal.add(Calendar.MONTH, 1)
        val newMonth = cal.time
        _uiState.update {
            it.copy(
                currentMonth = newMonth,
                monthTitle = computeMonthTitle(newMonth),
                calendarDays = computeCalendarDays(newMonth)
            )
        }
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
