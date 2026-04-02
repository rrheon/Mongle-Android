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
    /** 오늘의 질문이 아직 도착하지 않았을 때 보여줄 전날 질문 */
    val lastQuestion: Question? = null,
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
    val memberAnswers: Map<UUID, Answer> = emptyMap(),
    val hasUnreadNotifications: Boolean = false
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
    private val answerRepository: AnswerRepository,
    private val notificationRepository: com.mongle.android.data.remote.ApiNotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    /** 질문 넘기기 성공 시 남은 하트 수를 상위로 전파 */
    private val _skipEvents = MutableSharedFlow<Int>()
    val skipEvents: SharedFlow<Int> = _skipEvents.asSharedFlow()

    fun initialize(
        todayQuestion: Question?,
        lastQuestion: Question? = null,
        familyTree: TreeProgress,
        family: MongleGroup?,
        familyMembers: List<User>,
        allFamilies: List<MongleGroup> = emptyList(),
        currentUser: User?,
        hasAnsweredToday: Boolean,
        hasSkippedToday: Boolean = false
    ) {
        _uiState.update {
            it.copy(
                todayQuestion = todayQuestion,
                lastQuestion = lastQuestion,
                familyTree = familyTree,
                family = family,
                familyMembers = familyMembers,
                allFamilies = allFamilies,
                currentUser = currentUser,
                hasAnsweredToday = hasAnsweredToday,
                hasSkippedToday = hasSkippedToday,
                memberAnswerStatus = emptyMap(),
                memberAnswers = emptyMap()
            )
        }
        // 오늘의 질문이 있으면 가족 답변 상태 로드
        todayQuestion?.dailyQuestionId?.let { loadFamilyAnswers(it) }
        // 미읽은 알림 확인
        checkUnreadNotifications()
    }

    private fun checkUnreadNotifications() {
        viewModelScope.launch {
            val hasUnread = runCatching {
                notificationRepository.getNotifications(limit = 20).any { !it.isRead }
            }.getOrElse { false }
            _uiState.update { it.copy(hasUnreadNotifications = hasUnread) }
        }
    }

    private fun loadFamilyAnswers(dailyQuestionId: String) {
        viewModelScope.launch {
            runCatching {
                val dailyQId = UUID.fromString(dailyQuestionId)
                val familyAnswers = answerRepository.getByDailyQuestion(dailyQId)
                // getFamilyAnswers API는 가족 답변만 반환하므로 현재 사용자의 답변도 별도 로드
                val currentUserId = _uiState.value.currentUser?.id
                val myAnswer = currentUserId?.let {
                    runCatching { answerRepository.getByUserAndDailyQuestion(dailyQId, it) }.getOrNull()
                }
                val allAnswers = if (myAnswer != null) familyAnswers + myAnswer else familyAnswers
                val statusMap = allAnswers.associate { it.userId to true }
                val answersMap = allAnswers.associateBy { it.userId }
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

    fun skipQuestion() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { questionRepository.skipQuestion() }
                .onSuccess { heartsRemaining ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasSkippedToday = true,
                            currentUser = it.currentUser?.copy(hearts = heartsRemaining)
                        )
                    }
                    // 넘기기 후 다른 가족 답변을 볼 수 있도록 답변 로드
                    _uiState.value.todayQuestion?.dailyQuestionId?.let { loadFamilyAnswers(it) }
                    _skipEvents.emit(heartsRemaining)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "질문 넘기기에 실패했습니다.") }
                }
        }
    }

    fun updateHearts(hearts: Int) {
        _uiState.update { state ->
            state.copy(currentUser = state.currentUser?.copy(hearts = hearts))
        }
    }
}
