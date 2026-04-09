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
    object NavigateToWriteQuestion : HomeEvent()
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
                // 1-b) HISTORY 엔드포인트에서 오늘 항목의 답변을 가져와 fallback 으로 병합한다.
                //      getFamilyAnswers() 가 멤버 답변을 누락하는 백엔드 정합성 케이스가 있어,
                //      HISTORY 와 동일한 daily-history 응답을 두 번째 소스로 사용한다.
                //      (HISTORY 화면은 동일 엔드포인트로 동일 답변을 정상 노출하고 있음을 사용자가 확인)
                val historyAnswersForToday: List<Answer> = runCatching {
                    questionRepository.getDailyHistory(page = 1, limit = 10)
                }.getOrElse { emptyList() }
                    .firstOrNull { it.id == dailyQuestionId }
                    ?.answers
                    ?.mapNotNull { summary ->
                        val uid = runCatching { UUID.fromString(summary.userId) }.getOrNull() ?: return@mapNotNull null
                        val aid = runCatching { UUID.fromString(summary.id) }.getOrNull() ?: return@mapNotNull null
                        Answer(
                            id = aid,
                            dailyQuestionId = dailyQId,
                            userId = uid,
                            content = summary.content,
                            imageUrl = summary.imageUrl,
                            moodId = summary.moodId,
                            createdAt = java.util.Date()
                        )
                    } ?: emptyList()
                // 병합: 기존 allAnswers 우선, 없는 멤버만 history 답변으로 채움.
                val mergedById = allAnswers.associateBy { it.userId }.toMutableMap()
                historyAnswersForToday.forEach { hist ->
                    if (mergedById[hist.userId] == null) mergedById[hist.userId] = hist
                }
                val answersMap = mergedById.toMap()

                // 2) 멤버별 답변/스킵 상태 조회 (DailyQuestionResponse.memberAnswerStatuses)
                val todayStatuses = runCatching {
                    questionRepository.getTodayQuestionMemberStatuses()
                }.getOrElse { emptyList() }

                // 상태맵과 답변맵의 정합성 보장:
                // - answersMap에 답변이 있으면 무조건 "answered"로 간주 (서버 상태가 뒤처져도 UI 즉시 반영)
                // - todayStatuses의 "answered"/"skipped" 정보는 추가 병합
                val answeredFromAnswers = answersMap.mapValues { true }
                val answeredFromStatus = todayStatuses
                    .filter { it.second == "answered" }
                    .associate { (userId, _) -> UUID.fromString(userId) to true }
                val statusMap = answeredFromAnswers + answeredFromStatus
                val skipMap = todayStatuses
                    .filter { it.second == "skipped" }
                    .associate { (userId, _) -> UUID.fromString(userId) to true }

                android.util.Log.d(
                    "HomeVM",
                    "loadFamilyAnswers: answers=${allAnswers.size}, statuses=${todayStatuses.size}, statusMap=$statusMap, skipMap=$skipMap"
                )

                _uiState.update {
                    it.copy(
                        memberAnswerStatus = statusMap,
                        memberAnswers = answersMap,
                        memberSkipStatus = skipMap
                    )
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

    /**
     * 멤버별 답변/스킵 상태만 가볍게 재조회한다.
     * HOME 탭으로 (재)진입할 때마다 호출하여, 다른 멤버가 그 사이 답변/넘기기를 한 경우
     * UI 캐릭터 상태를 즉시 최신화한다. (HISTORY 다녀온 뒤에야 반영되던 회귀 수정)
     */
    fun refreshMemberStatuses() {
        val dailyQuestionId = _uiState.value.todayQuestion?.dailyQuestionId ?: return
        loadFamilyAnswers(dailyQuestionId)
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
            }
            if (dailyQId == null) {
                android.util.Log.w("HomeVM", "onViewAnswerTapped: dailyQuestionId 없음 member=${member.id}")
                _events.emit(HomeEvent.ShowError("아직 오늘의 질문 정보가 준비되지 않았어요."))
                return@launch
            }
            val freshAnswers = runCatching { answerRepository.getByDailyQuestion(dailyQId) }.getOrElse { emptyList() }
            var freshMap = freshAnswers.associateBy { it.userId }
            // 본인 답변은 backend에서 family-answers 응답에 포함되지 않을 수 있어
            // /me 전용 엔드포인트(getByUserAndDailyQuestion)로 한 번 더 시도한다.
            val isSelf = member.id == state.currentUser?.id
            if (isSelf && freshMap[member.id] == null) {
                val mine = runCatching {
                    answerRepository.getByUserAndDailyQuestion(dailyQId, member.id)
                }.getOrNull()
                if (mine != null) {
                    freshMap = freshMap + (mine.userId to mine)
                }
            }
            // 그래도 없으면 HISTORY 엔드포인트(daily-history)에서 오늘 항목을 찾아 본문을 보충한다.
            // HISTORY 화면이 동일 응답으로 모든 멤버 답변을 정상 노출하고 있음.
            if (freshMap[member.id] == null) {
                val histAnswers = runCatching {
                    questionRepository.getDailyHistory(page = 1, limit = 10)
                }.getOrElse { emptyList() }
                    .firstOrNull { it.id == _uiState.value.todayQuestion?.dailyQuestionId }
                    ?.answers
                    .orEmpty()
                val match = histAnswers.firstOrNull { it.userId == member.id.toString() }
                if (match != null) {
                    val converted = Answer(
                        id = runCatching { UUID.fromString(match.id) }.getOrNull() ?: UUID.randomUUID(),
                        dailyQuestionId = dailyQId,
                        userId = member.id,
                        content = match.content,
                        imageUrl = match.imageUrl,
                        moodId = match.moodId,
                        createdAt = java.util.Date()
                    )
                    freshMap = freshMap + (member.id to converted)
                }
            }
            _uiState.update { it.copy(memberAnswers = it.memberAnswers + freshMap) }
            val answer = freshMap[member.id]
            if (answer != null) {
                _events.emit(HomeEvent.ShowPeerAnswer(member, memberIndex.coerceAtLeast(0), answer))
            } else {
                // 캐시·서버 양쪽 모두 답변을 찾지 못한 경우 — 과거에는 silent return 으로
                // "터치 무반응" 회귀가 있었다. 최소한 토스트로 사용자에게 피드백을 준다.
                android.util.Log.w(
                    "HomeVM",
                    "onViewAnswerTapped: 답변 없음 member=${member.id} freshAnswersSize=${freshAnswers.size} statusBadgeAnswered=${state.memberAnswerStatus[member.id]}"
                )
                _events.emit(HomeEvent.ShowError("아직 ${member.name}님의 답변을 불러올 수 없어요."))
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

    fun watchAdForWrite(adManager: AdManager) {
        _uiState.update { it.copy(isLoading = true) }
        adManager.showRewardedAd(
            onRewarded = {
                viewModelScope.launch {
                    try {
                        val heartsAfterAd = userRepository.grantAdHearts(3)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentUser = it.currentUser?.copy(hearts = heartsAfterAd)
                            )
                        }
                        // 광고 시청 후 작성 화면으로 자동 이동
                        _events.emit(HomeEvent.NavigateToWriteQuestion)
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
