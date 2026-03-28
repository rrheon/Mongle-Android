package com.mongle.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AnswerRepository
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import com.mongle.android.domain.repository.TreeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val todayQuestion: Question? = null,
    val familyTree: TreeProgress = TreeProgress(),
    val family: MongleGroup? = null,
    val familyMembers: List<User> = emptyList(),
    val allFamilies: List<MongleGroup> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasAnsweredToday: Boolean = false,
    val hasSkippedToday: Boolean = false,
    val errorMessage: String? = null,
    /** 각 멤버 ID별 답변 여부 (userId → hasAnswered) */
    val memberAnswerStatus: Map<UUID, Boolean> = emptyMap(),
    /** 각 멤버 ID별 답변 내용 (userId → Answer) */
    val memberAnswers: Map<UUID, Answer> = emptyMap()
) {
    val hasFamily: Boolean get() = family != null
}

sealed class HomeEvent {
    data class NavigateToQuestionDetail(val question: Question) : HomeEvent()
    data class NavigateToNudge(val targetUser: User) : HomeEvent()
    data class ShowPeerAnswer(val member: User, val memberIndex: Int, val answer: Answer) : HomeEvent()
    data class ShowAnswerFirstToView(val memberName: String) : HomeEvent()
    data class ShowNudgeUnavailable(val memberName: String) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val mongleRepository: MongleRepository,
    private val treeRepository: TreeRepository,
    private val answerRepository: AnswerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun initialize(
        todayQuestion: Question?,
        familyTree: TreeProgress,
        family: MongleGroup?,
        familyMembers: List<User>,
        allFamilies: List<MongleGroup> = emptyList(),
        currentUser: User?,
        hasAnsweredToday: Boolean
    ) {
        _uiState.update {
            it.copy(
                todayQuestion = todayQuestion,
                familyTree = familyTree,
                family = family,
                familyMembers = familyMembers,
                allFamilies = allFamilies,
                currentUser = currentUser,
                hasAnsweredToday = hasAnsweredToday
            )
        }
        // 오늘의 질문이 있으면 가족 답변 상태 로드
        todayQuestion?.dailyQuestionId?.let { loadFamilyAnswers(it) }
    }

    private fun loadFamilyAnswers(dailyQuestionId: String) {
        viewModelScope.launch {
            runCatching {
                val dailyQId = UUID.fromString(dailyQuestionId)
                val answers = answerRepository.getByDailyQuestion(dailyQId)
                val statusMap = answers.associate { it.userId to true }
                val answersMap = answers.associateBy { it.userId }
                _uiState.update {
                    it.copy(
                        memberAnswerStatus = statusMap,
                        memberAnswers = answersMap
                    )
                }
            }
        }
    }

    fun onQuestionTapped() {
        val question = _uiState.value.todayQuestion ?: return
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToQuestionDetail(question))
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val familyResult = runCatching { mongleRepository.getMyFamily() }.getOrNull()
                val question = runCatching { questionRepository.getTodayQuestion() }.getOrNull()
                val tree = runCatching { treeRepository.getMyTreeProgress() }.getOrElse { TreeProgress() }

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        todayQuestion = question,
                        familyTree = tree ?: TreeProgress(),
                        family = familyResult?.first,
                        familyMembers = familyResult?.second ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isRefreshing = false, errorMessage = e.message)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onAnswerSubmitted() {
        _uiState.update { it.copy(hasAnsweredToday = true) }
    }

    fun onMemberTapped(member: User) {
        val state = _uiState.value
        val currentUser = state.currentUser ?: return
        if (member.id == currentUser.id) return

        viewModelScope.launch {
            val hasAnswered = state.memberAnswerStatus[member.id] == true
            if (hasAnswered) {
                val answer = state.memberAnswers[member.id]
                if (answer != null) {
                    val memberIndex = state.familyMembers.indexOfFirst { it.id == member.id }
                    _events.emit(HomeEvent.ShowPeerAnswer(member, memberIndex.coerceAtLeast(0), answer))
                } else {
                    _events.emit(HomeEvent.NavigateToNudge(member))
                }
            } else {
                _events.emit(HomeEvent.NavigateToNudge(member))
            }
        }
    }

    fun onViewAnswerTapped(member: User) {
        val state = _uiState.value
        viewModelScope.launch {
            val answer = state.memberAnswers[member.id]
            val memberIndex = state.familyMembers.indexOfFirst { it.id == member.id }
            if (answer != null) {
                _events.emit(HomeEvent.ShowPeerAnswer(member, memberIndex.coerceAtLeast(0), answer))
            }
        }
    }

    fun onNudgeTapped(member: User) {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToNudge(member))
        }
    }

    fun updateHearts(hearts: Int) {
        _uiState.update { state ->
            state.copy(currentUser = state.currentUser?.copy(hearts = hearts))
        }
    }
}
