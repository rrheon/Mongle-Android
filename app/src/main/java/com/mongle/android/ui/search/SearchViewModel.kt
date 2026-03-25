package com.mongle.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.DailyQuestionHistory
import com.mongle.android.domain.model.HistoryAnswerSummary
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

data class SearchResultItem(
    val dailyQuestionId: String,
    val date: Date,
    val questionContent: String,
    /** 검색어와 일치하는 답변들만 (또는 질문이 일치할 경우 전체 답변) */
    val matchedAnswers: List<HistoryAnswerSummary>,
    val totalAnswerCount: Int
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList(),
    val isLoading: Boolean = false,
    val showMinLengthHint: Boolean = false,
    val errorMessage: String? = null
) {
    val resultCount: Int get() = results.size
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val mongleRepository: MongleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var allHistory: List<DailyQuestionHistory> = emptyList()

    init {
        loadHistory()
        observeQuery()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                allHistory = questionRepository.getDailyHistory(page = 1, limit = 100)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun observeQuery() {
        viewModelScope.launch {
            _queryFlow
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    performSearch(query)
                }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, showMinLengthHint = query.isNotEmpty() && query.trim().length < 2) }
        _queryFlow.value = query
    }

    private fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(results = emptyList()) }
            return
        }

        val results = mutableListOf<SearchResultItem>()
        val lowerQuery = trimmed.lowercase()

        for (history in allHistory) {
            val questionMatches = history.question.content.lowercase().contains(lowerQuery)
            val matchedAnswers = history.answers.filter { answer ->
                answer.content.lowercase().contains(lowerQuery) ||
                    answer.userName.lowercase().contains(lowerQuery)
            }

            if (questionMatches || matchedAnswers.isNotEmpty()) {
                results.add(
                    SearchResultItem(
                        dailyQuestionId = history.id,
                        date = history.date,
                        questionContent = history.question.content,
                        matchedAnswers = if (questionMatches) history.answers else matchedAnswers,
                        totalAnswerCount = history.familyAnswerCount
                    )
                )
            }
        }

        // 날짜 내림차순 정렬
        results.sortByDescending { it.date }

        _uiState.update { it.copy(results = results) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/** Date를 표시 문자열로 포맷 (오늘/어제/날짜) */
fun Date.toDisplayLabel(): String {
    val cal = Calendar.getInstance().apply { time = this@toDisplayLabel }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) ->
            "${cal.get(Calendar.MONTH) + 1}월 ${cal.get(Calendar.DAY_OF_MONTH)}일 · 오늘"
        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
            "${cal.get(Calendar.MONTH) + 1}월 ${cal.get(Calendar.DAY_OF_MONTH)}일 · 어제"
        else -> "${cal.get(Calendar.MONTH) + 1}월 ${cal.get(Calendar.DAY_OF_MONTH)}일"
    }
}
