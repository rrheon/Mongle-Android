package com.mongle.android.ui.root

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.User
import com.mongle.android.data.remote.SessionExpiredNotifier
import com.mongle.android.domain.repository.AuthRepository
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import com.mongle.android.domain.repository.TreeRepository
import com.mongle.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AppState {
    data object Loading : AppState()
    data object Onboarding : AppState()
    data object Unauthenticated : AppState()
    data object GroupSelection : AppState()
    data object Authenticated : AppState()
}

private const val PREFS_NAME = "mongle_app_prefs"
private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"

data class RootUiState(
    val appState: AppState = AppState.Loading,
    val currentUser: User? = null,
    val todayQuestion: Question? = null,
    /** 오늘의 질문이 아직 도착하지 않았을 때 보여줄 전날 질문 */
    val lastQuestion: Question? = null,
    val familyTree: TreeProgress = TreeProgress(),
    val family: MongleGroup? = null,
    val familyMembers: List<User> = emptyList(),
    val allFamilies: List<MongleGroup> = emptyList(),
    val hasAnsweredToday: Boolean = false,
    val errorMessage: String? = null,
    val pendingInviteCode: String? = null,
    val dailyHeartGranted: Int = 0
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val mongleRepository: MongleRepository,
    private val questionRepository: QuestionRepository,
    private val treeRepository: TreeRepository,
    private val userRepository: UserRepository,
    private val sessionExpiredNotifier: SessionExpiredNotifier,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun hasSeenOnboarding(): Boolean =
        prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)

    fun onOnboardingCompleted() {
        _uiState.update { it.copy(appState = AppState.Unauthenticated) }
    }

    fun onOnboardingNeverShowAgain() {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
        _uiState.update { it.copy(appState = AppState.Unauthenticated) }
    }

    private val _uiState = MutableStateFlow(RootUiState())
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
        // 토큰 만료(401 + 갱신 실패) 이벤트 구독 → 로그인 화면으로 이동
        viewModelScope.launch {
            sessionExpiredNotifier.events.collect {
                _uiState.update { RootUiState(appState = AppState.Unauthenticated) }
            }
        }
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val user = runCatching { authRepository.getCurrentUser() }.getOrNull()
            if (user != null) {
                _uiState.update { it.copy(currentUser = user) }
                // 서버에 실제 토큰 유효성 검증
                val familiesResult = runCatching { mongleRepository.getMyFamilies() }
                familiesResult.onSuccess { allFamilies ->
                    _uiState.update { it.copy(appState = AppState.GroupSelection, allFamilies = allFamilies) }
                }.onFailure { e ->
                    val msg = e.message ?: ""
                    val isAuthError = msg.contains("401") ||
                        msg.contains("token", ignoreCase = true) ||
                        msg.contains("unauthorized", ignoreCase = true) ||
                        msg.contains("invalid", ignoreCase = true)
                    if (isAuthError) {
                        // 토큰 만료/무효 → 세션 초기화 후 로그인 화면
                        runCatching { authRepository.logout() }
                        _uiState.update { RootUiState(appState = AppState.Unauthenticated) }
                    } else {
                        // 네트워크 오류 등 → 빈 목록으로 그룹선택화면
                        _uiState.update { it.copy(appState = AppState.GroupSelection, allFamilies = emptyList()) }
                    }
                }
            } else if (!hasSeenOnboarding()) {
                _uiState.update { it.copy(appState = AppState.Onboarding) }
            } else {
                _uiState.update { it.copy(appState = AppState.Unauthenticated) }
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            try {
                val familyResult = runCatching { mongleRepository.getMyFamily() }.getOrNull()
                val family = familyResult?.first
                val members = familyResult?.second ?: emptyList()
                val question = runCatching { questionRepository.getTodayQuestion() }.getOrNull()
                val tree = runCatching { treeRepository.getMyTreeProgress() }.getOrElse { TreeProgress() }

                val allFamilies = runCatching { mongleRepository.getMyFamilies() }.getOrElse { emptyList() }

                if (family == null) {
                    // No active family → go to GroupSelection
                    _uiState.update { it.copy(appState = AppState.GroupSelection, allFamilies = allFamilies) }
                } else {
                    // 일일 접속 하트 획득 시도
                    val dailyHeart = runCatching { userRepository.claimDailyHeart() }.getOrNull()

                    // 오늘의 질문이 없으면 히스토리에서 가장 최근 질문을 가져와 표시
                    val lastQ = if (question == null) {
                        runCatching {
                            questionRepository.getDailyHistory(page = 1, limit = 1).firstOrNull()?.question
                        }.getOrNull()
                    } else null

                    _uiState.update {
                        // 서버의 familyMembers에서 현재 유저의 최신 정보를 동기화 (그룹별 닉네임 포함)
                        val serverMe = members.firstOrNull { m -> m.id == it.currentUser?.id }
                        val syncedUser = if (serverMe != null) {
                            serverMe.copy(
                                hearts = if (dailyHeart != null) dailyHeart.heartsRemaining else serverMe.hearts
                            )
                        } else {
                            it.currentUser?.copy(
                                hearts = if (dailyHeart != null) dailyHeart.heartsRemaining else (it.currentUser.hearts)
                            )
                        }
                        it.copy(
                            appState = AppState.Authenticated,
                            todayQuestion = question,
                            lastQuestion = lastQ,
                            familyTree = tree ?: TreeProgress(),
                            family = family,
                            familyMembers = members,
                            allFamilies = allFamilies,
                            hasAnsweredToday = question?.hasMyAnswer ?: false,
                            dailyHeartGranted = dailyHeart?.heartsGranted ?: 0,
                            currentUser = syncedUser
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        appState = AppState.Authenticated,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun onGroupSelected(familyId: java.util.UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(appState = AppState.Loading) }
            runCatching { mongleRepository.selectFamily(familyId) }
            loadHomeData()
        }
    }

    fun onGroupCreatedOrJoined() {
        loadHomeData()
    }

    fun handleDeepLink(uri: android.net.Uri) {
        val code = when {
            uri.scheme == "monggle" && uri.host == "join" -> uri.pathSegments.firstOrNull()?.uppercase()
            uri.host == "monggle.app" && uri.pathSegments.size >= 2 && uri.pathSegments[0] == "join" -> uri.pathSegments[1].uppercase()
            else -> null
        }
        if (code != null) {
            _uiState.update { it.copy(pendingInviteCode = code) }
        }
    }

    fun clearPendingInviteCode() {
        _uiState.update { it.copy(pendingInviteCode = null) }
    }

    fun onBrowse() {
        // 게스트 모드: 빈 데이터로 인증 상태 진입
        _uiState.update {
            it.copy(
                appState = AppState.Authenticated,
                currentUser = null
            )
        }
    }

    fun onLoggedIn(user: User) {
        // 로그인 직후엔 항상 그룹선택화면으로 이동 (기존 그룹 있어도 선택하게)
        viewModelScope.launch {
            _uiState.update { it.copy(currentUser = user, appState = AppState.Loading) }
            val allFamilies = runCatching { mongleRepository.getMyFamilies() }.getOrElse { emptyList() }
            _uiState.update { it.copy(appState = AppState.GroupSelection, allFamilies = allFamilies) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { authRepository.logout() }
            _uiState.update {
                RootUiState(appState = AppState.Unauthenticated)
            }
        }
    }

    fun onAnswerSubmitted(answer: Answer? = null, isNewAnswer: Boolean = true) {
        _uiState.update { state ->
            if (answer != null) {
                val heartsChange = if (isNewAnswer) 1 else -1
                val updatedUser = state.currentUser?.copy(
                    moodId = answer.moodId,
                    hearts = (state.currentUser.hearts) + heartsChange
                )
                val updatedMembers = state.familyMembers.map { member ->
                    if (member.id == answer.userId) member.copy(moodId = answer.moodId) else member
                }
                state.copy(
                    hasAnsweredToday = true,
                    currentUser = updatedUser,
                    familyMembers = updatedMembers
                )
            } else {
                state.copy(hasAnsweredToday = true)
            }
        }
    }

    fun dismissDailyHeartPopup() {
        _uiState.update { it.copy(dailyHeartGranted = 0) }
    }

    fun updateHearts(hearts: Int) {
        _uiState.update { state ->
            state.copy(currentUser = state.currentUser?.copy(hearts = hearts))
        }
    }

    fun onQuestionSkipped(newQuestion: Question) {
        _uiState.update {
            it.copy(
                todayQuestion = newQuestion,
                hasAnsweredToday = false
            )
        }
    }

    fun refreshData() {
        loadHomeData()
    }
}
