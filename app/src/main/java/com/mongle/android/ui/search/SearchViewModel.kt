package com.mongle.android.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ycompany.Monggle.BuildConfig
import com.mongle.android.domain.model.DailyQuestionHistory
import com.mongle.android.domain.model.HistoryAnswerSummary
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
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

    /** 현재 캐시가 어느 그룹의 데이터인지 — 그룹 전환 시 재로드에 사용 */
    private var loadedFamilyId: String? = null
    private var loadJob: Job? = null

    init {
        observeQuery()
    }

    /**
     * 화면이 포그라운드로 올라오거나, 현재 활성 familyId 가 전달될 때 호출.
     * - 같은 그룹이면 최신 상태 반영을 위해 재로드
     * - 그룹이 바뀌면 캐시를 비우고 재로드 (그룹별 독립 검색)
     * - familyId 가 null 이면 캐시만 비움
     */
    fun setActiveFamily(familyId: String?) {
        if (familyId == null) {
            allHistory = emptyList()
            loadedFamilyId = null
            _uiState.update { SearchUiState() }
            return
        }
        if (loadedFamilyId != familyId) {
            allHistory = emptyList()
            _uiState.update { it.copy(query = "", results = emptyList(), showMinLengthHint = false) }
            loadedFamilyId = familyId
        }
        loadHistory()
    }

    private fun loadHistory() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                allHistory = questionRepository.getDailyHistory(page = 1, limit = 100)
                _uiState.update { it.copy(isLoading = false) }
                // 로딩 중에 이미 입력해 둔 검색어가 있으면 재검색
                val pending = _uiState.value.query.trim()
                if (pending.length >= 2) {
                    performSearch(_uiState.value.query)
                }
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
        _uiState.update {
            it.copy(query = query, showMinLengthHint = query.isNotEmpty() && query.trim().length < 2)
        }
        _queryFlow.value = query
    }

    private fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(results = emptyList()) }
            return
        }
        // 아직 히스토리 로딩 중이면 결과를 비우지 않고 대기 (로드 완료 후 재검색)
        if (_uiState.value.isLoading && allHistory.isEmpty()) {
            return
        }

        // 한글 NFC/NFD 정규화 차이로 매칭 실패하는 것을 막기 위해 양쪽 모두 NFC + 소문자
        val normalizedQuery = trimmed.normalizedForSearch()
        val results = mutableListOf<SearchResultItem>()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "performSearch query=$trimmed allHistory.size=${allHistory.size} family=$loadedFamilyId")
        }

        for (history in allHistory) {
            // 오늘 질문: 내가 답변 or 스킵 하지 않았으면 검색 결과에서 제외
            if (isToday(history.date) && !(history.hasMyAnswer || history.hasMySkipped)) continue

            val questionMatches = history.question.content.normalizedForSearch().contains(normalizedQuery)
            val matchedAnswers = history.answers.filter { answer ->
                answer.content.normalizedForSearch().contains(normalizedQuery) ||
                    answer.userName.normalizedForSearch().contains(normalizedQuery)
            }

            if (questionMatches || matchedAnswers.isNotEmpty()) {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "match q=\"${history.question.content}\" answersCount=${history.answers.size} " +
                            "matched=${matchedAnswers.size} questionMatches=$questionMatches"
                    )
                }
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

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "total results: ${results.size}")
        }

        // 날짜 내림차순 정렬
        results.sortByDescending { it.date }

        _uiState.update { it.copy(results = results) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun isToday(date: Date): Boolean {
        val historyCal = Calendar.getInstance().apply { time = date }
        val todayCal = Calendar.getInstance()
        return historyCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            historyCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        private const val TAG = "SearchHistory"
    }
}

/**
 * 검색 매칭용 정규화: 한글 NFC 변환 + 소문자.
 * 입력기에 따라 NFD(자모 분해) 로 들어오는 케이스를 대비해 양쪽 모두 NFC 로 맞춰
 * substring 매칭을 안정화한다.
 */
private fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFC).lowercase()

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
