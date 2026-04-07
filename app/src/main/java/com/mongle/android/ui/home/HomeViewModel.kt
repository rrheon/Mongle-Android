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
        val prev = _uiState.value
        val questionChanged = prev.todayQuestion?.dailyQuestionId != todayQuestion?.dailyQuestionId
        val answeredTransition = !prev.hasAnsweredToday && hasAnsweredToday
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
        // 본인 답변 직후(false → true 전환)에는 서버에서 오늘의 질문도 함께 재조회하여
        // 백엔드가 본인 답변 존재 여부로 게이트하는 가족 답변을 최신 상태로 가져온다.
        todayQuestion?.dailyQuestionId?.let { loadFamilyAnswers(it, forceRefreshQuestion = answeredTransition) }
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

    private fun loadFamilyAnswers(dailyQuestionId: String, forceRefreshQuestion: Boolean = false) {
        viewModelScope.launch {
            try {
                val dailyQId = UUID.fromString(dailyQuestionId)

                // 본인 답변 직후 백엔드 게이팅 우회를 위해 오늘의 질문을 먼저 재조회 (동기화 목적)
                if (forceRefreshQuestion) {
                    runCatching { questionRepository.getTodayQuestion() }.getOrNull()?.let { freshQ ->
                        _uiState.update { it.copy(todayQuestion = freshQ, hasAnsweredToday = freshQ.hasMyAnswer) }
                    }
                }

                // 1) 가족 답변 로드 (Answer 목록)
                val familyAnswers = runCatching { answerRepository.getByDailyQuestion(dailyQId) }.getOrElse { emptyList() }
                val currentUserId = _uiState.value.currentUser?.id
                val myAnswer = currentUserId?.let {
                    runCatching { answerRepository.getByUserAndDailyQuestion(dailyQId, it) }.getOrNull()
                }
                val allAnswers = if (myAnswer != null && familyAnswers.none { it.userId == myAnswer.userId }) {
                    familyAnswers + myAnswer
                } else {
                    familyAnswers
                }
                val answersMap = allAnswers.associateBy { it.userId }

                // 2) 멤버별 답변/스킵 상태 조회 (DailyQuestionResponse.memberAnswerStatuses)
                val todayStatuses = runCatching {
                    questionRepository.getTodayQuestionMemberStatuses()
                }.getOrElse { emptyList() }

                android.util.Log.d("HomeVM", "loadFamilyAnswers: answers=${allAnswers.size}, statuses=${todayStatuses.size}, statuses=$todayStatuses")

                if (todayStatuses.isNotEmpty()) {
                    val statusMap = todayStatuses.associate { (userId, status) ->
                        UUID.fromString(userId) to (status == "answered")
                    }
                    val skipMap = todayStatuses.associate { (userId, status) ->
                        UUID.fromString(userId) to (status == "skipped")
                    }
                    android.util.Log.d("HomeVM", "statusMap=$statusMap, skipMap=$skipMap")
                    _uiState.update {
                        it.copy(
                            memberAnswerStatus = statusMap,
                            memberAnswers = answersMap,
                            memberSkipStatus = skipMap
                        )
                    }
                } else {
                    // fallback: 답변 있는 멤버는 answered, 나머지는 상태 없음
                    val statusMap = answersMap.mapValues { true }
                    android.util.Log.d("HomeVM", "fallback statusMap=$statusMap")
                    _uiState.update {
                        it.copy(
                            memberAnswerStatus = statusMap,
                            memberAnswers = answersMap
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "loadFamilyAnswers failed", e)
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
        viewModelScope.launch {
            val state = _uiState.value
            val memberIndex = state.familyMembers.indexOfFirst { it.id == member.id }
            val cached = state.memberAnswers[member.id]
            if (cached != null) {
                _events.emit(HomeEvent.ShowPeerAnswer(member, memberIndex.coerceAtLeast(0), cached))
                return@launch
            }
            // 캐시에 없으면 서버에서 최신 가족 답변을 즉시 재조회 후 재시도
            val dailyQId = _uiState.value.todayQuestion?.dailyQuestionId?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            } ?: return@launch
            val freshAnswers = runCatching { answerRepository.getByDailyQuestion(dailyQId) }.getOrElse { emptyList() }
            val freshMap = freshAnswers.associateBy { it.userId }
            _uiState.update { it.copy(memberAnswers = it.memberAnswers + freshMap) }
            val answer = freshMap[member.id]
            if (answer != null) {
                _events.emit(HomeEvent.ShowPeerAnswer(member, memberIndex.coerceAtLeast(0), answer))
            } else {
                // 서버에도 없으면 에러 토스트 대신 일단 동작 안내 없이 조용히 리턴
                android.util.Log.w("HomeVM", "onViewAnswerTapped: no answer for ${member.id}")
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
