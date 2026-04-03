package com.mongle.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.User
import com.mongle.android.data.remote.ApiUserRepository
import com.mongle.android.domain.repository.AnswerRepository
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import com.mongle.android.domain.repository.TreeRepository
import com.mongle.android.util.AdManager
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
    /** 각 멤버 ID별 스킵 여부 (userId → hasSkipped) */
    val memberSkipStatus: Map<UUID, Boolean> = emptyMap(),
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
    data class ShowError(val message: String) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val mongleRepository: MongleRepository,
    private val treeRepository: TreeRepository,
    private val answerRepository: AnswerRepository,
    private val userRepository: ApiUserRepository,
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
        val questionChanged = _uiState.value.todayQuestion?.dailyQuestionId != todayQuestion?.dailyQuestionId
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
                // 질문이 바뀐 경우에만 리셋, 아니면 기존 답변 상태 유지
                memberAnswerStatus = if (questionChanged) emptyMap() else it.memberAnswerStatus,
                memberAnswers = if (questionChanged) emptyMap() else it.memberAnswers
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

                // 1) 가족 답변 로드
                val response = (answerRepository as? com.mongle.android.data.remote.ApiAnswerRepository)
                    ?.getFamilyAnswersWithStatuses(dailyQId)

                val answersMap = if (response != null) {
                    response.answers.associateBy { UUID.fromString(it.user.id) }
                        .mapValues { (_, ar) ->
                            com.mongle.android.domain.model.Answer(
                                id = UUID.fromString(ar.id),
                                dailyQuestionId = UUID.fromString(ar.questionId),
                                userId = UUID.fromString(ar.user.id),
                                content = ar.content,
                                imageUrl = ar.imageUrl,
                                moodId = ar.moodId,
                                createdAt = java.util.Date(),
                                updatedAt = java.util.Date()
                            )
                        }
                } else {
                    val familyAnswers = answerRepository.getByDailyQuestion(dailyQId)
                    val currentUserId = _uiState.value.currentUser?.id
                    val myAnswer = currentUserId?.let {
                        runCatching { answerRepository.getByUserAndDailyQuestion(dailyQId, it) }.getOrNull()
                    }
                    val allAnswers = if (myAnswer != null) familyAnswers + myAnswer else familyAnswers
                    allAnswers.associateBy { it.userId }
                }

                // 2) 멤버별 답변/스킵 상태: FamilyAnswersResponse.memberStatuses 우선, 없으면 DailyQuestionResponse.memberAnswerStatuses 활용
                val memberStatuses = response?.memberStatuses?.takeIf { it.isNotEmpty() }
                if (memberStatuses != null) {
                    val statusMap = memberStatuses.associate { ms ->
                        UUID.fromString(ms.userId) to (ms.status == "answered")
                    }
                    val skipMap = memberStatuses.associate { ms ->
                        UUID.fromString(ms.userId) to (ms.status == "skipped")
                    }
                    _uiState.update {
                        it.copy(
                            memberAnswerStatus = statusMap,
                            memberAnswers = answersMap,
                            memberSkipStatus = skipMap
                        )
                    }
                } else {
                    // FamilyAnswersResponse에 memberStatuses가 없으면 DailyQuestionResponse에서 가져옴
                    val todayStatuses = runCatching {
                        questionRepository.getTodayQuestionMemberStatuses()
                    }.getOrElse { emptyList() }

                    if (todayStatuses.isNotEmpty()) {
                        val statusMap = todayStatuses.associate { (userId, status) ->
                            UUID.fromString(userId) to (status == "answered")
                        }
                        val skipMap = todayStatuses.associate { (userId, status) ->
                            UUID.fromString(userId) to (status == "skipped")
                        }
                        _uiState.update {
                            it.copy(
                                memberAnswerStatus = statusMap,
                                memberAnswers = answersMap,
                                memberSkipStatus = skipMap
                            )
                        }
                    } else {
                        // 최종 fallback: 답변 있는 멤버만 answered로 처리
                        val statusMap = answersMap.mapValues { true }
                        _uiState.update {
                            it.copy(
                                memberAnswerStatus = statusMap,
                                memberAnswers = answersMap
                            )
                        }
                    }
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
                        hasAnsweredToday = question?.hasMyAnswer ?: false,
                        hasSkippedToday = question?.hasMySkipped ?: false,
                        familyTree = tree ?: TreeProgress(),
                        family = familyResult?.first,
                        familyMembers = familyResult?.second ?: emptyList()
                    )
                }
                // 답변 상태도 함께 새로고침
                question?.dailyQuestionId?.let { loadFamilyAnswers(it) }
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
        // 답변 제출 후 가족 답변 목록 새로고침
        _uiState.value.todayQuestion?.dailyQuestionId?.let { loadFamilyAnswers(it) }
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
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(HomeEvent.ShowError(e.message ?: ""))
                }
        }
    }

    fun watchAdForSkip(adManager: AdManager) {
        _uiState.update { it.copy(isLoading = true) }
        adManager.showRewardedAd(
            onRewarded = {
                viewModelScope.launch {
                    try {
                        val heartsAfterAd = userRepository.grantAdHearts(3)
                        _uiState.update {
                            it.copy(currentUser = it.currentUser?.copy(hearts = heartsAfterAd))
                        }
                        skipQuestion()
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.emit(HomeEvent.ShowError(e.message ?: "ad_reward_failed"))
                    }
                }
            },
            onFailed = {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(HomeEvent.ShowError("ad_load_failed"))
                }
            }
        )
    }

    fun updateHearts(hearts: Int) {
        _uiState.update { state ->
            state.copy(currentUser = state.currentUser?.copy(hearts = hearts))
        }
    }
}
