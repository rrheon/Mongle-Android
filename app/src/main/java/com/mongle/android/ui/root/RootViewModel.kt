package com.mongle.android.ui.root

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.LegalDocType
import com.mongle.android.domain.model.LegalVersions
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
    /** 로그인은 됐지만 약관 동의가 필요한 상태 */
    data object ConsentRequired : AppState()
    /** 이메일 회원가입 플로우 (Consent → 입력 → 인증코드) */
    data object EmailSignup : AppState()
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
    val hasSkippedToday: Boolean = false,
    val errorMessage: String? = null,
    val pendingInviteCode: String? = null,
    val dailyHeartGranted: Int = 0,
    val pendingNotificationType: String? = null,
    val pendingAppleCallbackUri: android.net.Uri? = null,
    /** 동의 화면에 전달할 컨텍스트 (로그인 응답에서 받음) */
    val pendingConsentRequired: List<LegalDocType> = emptyList(),
    val pendingLegalVersions: LegalVersions = LegalVersions(terms = "", privacy = "")
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
                    // 하루 첫 접속 하트 팝업 체크 (그룹별, iOS와 동일 로직)
                    // 서버의 auth middleware recordAccess가 자동으로 하트를 증가시킴
                    val heartPopupKey = "mongle.lastHeartPopupDate.${family.id}"
                    val heartPrefs = context.getSharedPreferences("mongle_heart", Context.MODE_PRIVATE)
                    val todayStart = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val lastPopupTime = heartPrefs.getLong(heartPopupKey, 0L)
                    val isFirstAccessToday = lastPopupTime < todayStart
                    if (isFirstAccessToday) {
                        heartPrefs.edit().putLong(heartPopupKey, todayStart).apply()
                    }

                    // FCM 토큰 서버 등록
                    runCatching {
                        val fcmToken = context.getSharedPreferences("fcm", Context.MODE_PRIVATE)
                            .getString("token", null)
                        if (fcmToken != null) {
                            (userRepository as? com.mongle.android.data.remote.ApiUserRepository)
                                ?.registerFcmToken(fcmToken)
                        }
                    }

                    // 서버가 KST 정오에 새 질문을 배정하므로 별도의 "최근 질문 폴백" 은 불필요.
                    val lastQ: Question? = null

                    _uiState.update {
                        val serverMe = members.firstOrNull { m -> m.id == it.currentUser?.id }
                        val syncedUser = serverMe ?: it.currentUser
                        it.copy(
                            appState = AppState.Authenticated,
                            todayQuestion = question,
                            lastQuestion = lastQ,
                            familyTree = tree ?: TreeProgress(),
                            family = family,
                            familyMembers = members,
                            allFamilies = allFamilies,
                            hasAnsweredToday = question?.hasMyAnswer ?: false,
                            hasSkippedToday = question?.hasMySkipped ?: false,
                            dailyHeartGranted = if (isFirstAccessToday) 1 else 0,
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
        // Apple Sign-In 콜백: monggle://apple-callback?id_token=...&code=...
        if (uri.scheme == "monggle" && uri.host == "apple-callback") {
            _uiState.update { it.copy(pendingAppleCallbackUri = uri) }
            return
        }

        val code = when {
            uri.scheme == "monggle" && uri.host == "join" -> uri.pathSegments.firstOrNull()?.uppercase()
            uri.host == "monggle.app" && uri.pathSegments.size >= 2 && uri.pathSegments[0] == "join" -> uri.pathSegments[1].uppercase()
            else -> null
        }
        if (code != null) {
            _uiState.update { it.copy(pendingInviteCode = code) }
        }
    }

    fun clearPendingAppleCallback() {
        _uiState.update { it.copy(pendingAppleCallbackUri = null) }
    }

    fun clearPendingInviteCode() {
        _uiState.update { it.copy(pendingInviteCode = null) }
    }

    fun handleNotificationTap(type: String) {
        _uiState.update { it.copy(pendingNotificationType = type) }
    }

    fun clearPendingNotification() {
        _uiState.update { it.copy(pendingNotificationType = null) }
    }

    fun onEmailFlowRequested() {
        _uiState.update { it.copy(appState = AppState.EmailSignup) }
    }

    fun onEmailSignupCancelled() {
        _uiState.update { it.copy(appState = AppState.Unauthenticated) }
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

    fun onLoggedIn(
        user: User,
        needsConsent: Boolean = false,
        requiredConsents: List<LegalDocType> = emptyList(),
        legalVersions: LegalVersions = LegalVersions(terms = "", privacy = "")
    ) {
        if (needsConsent) {
            // 동의 필요 → ConsentScreen 으로 라우팅
            _uiState.update {
                it.copy(
                    currentUser = user,
                    appState = AppState.ConsentRequired,
                    pendingConsentRequired = requiredConsents,
                    pendingLegalVersions = legalVersions
                )
            }
            return
        }
        // 로그인 직후엔 항상 그룹선택화면으로 이동 (기존 그룹 있어도 선택하게)
        viewModelScope.launch {
            _uiState.update { it.copy(currentUser = user, appState = AppState.Loading) }
            val allFamilies = runCatching { mongleRepository.getMyFamilies() }.getOrElse { emptyList() }
            _uiState.update { it.copy(appState = AppState.GroupSelection, allFamilies = allFamilies) }
        }
    }

    /** ConsentScreen 에서 뒤로가기 → 세션 정리하고 로그인 화면으로 */
    fun onConsentCancelled() {
        viewModelScope.launch {
            runCatching { authRepository.logout() }
            _uiState.update {
                RootUiState(appState = AppState.Unauthenticated)
            }
        }
    }

    /** ConsentScreen 에서 동의 완료 → 일반 로그인 흐름으로 진입 */
    fun onConsentCompleted() {
        val user = _uiState.value.currentUser ?: run {
            _uiState.update { it.copy(appState = AppState.Unauthenticated) }
            return
        }
        _uiState.update {
            it.copy(
                pendingConsentRequired = emptyList(),
                pendingLegalVersions = LegalVersions(terms = "", privacy = "")
            )
        }
        onLoggedIn(user)
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
        // 서버에 moodId 동기화 (그룹선택/홈/My 화면 모두 반영)
        if (answer?.moodId != null) {
            viewModelScope.launch {
                val user = _uiState.value.currentUser ?: return@launch
                runCatching { userRepository.update(user) }
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

    fun onQuestionSkipped(heartsRemaining: Int) {
        _uiState.update {
            it.copy(
                hasSkippedToday = true,
                currentUser = it.currentUser?.copy(hearts = heartsRemaining)
            )
        }
    }

    fun onWriteQuestionSubmitted(question: Question) {
        _uiState.update {
            it.copy(
                todayQuestion = question,
                hasAnsweredToday = false,
                currentUser = it.currentUser?.copy(
                    hearts = (it.currentUser.hearts - 3).coerceAtLeast(0)
                )
            )
        }
    }

    fun refreshData() {
        loadHomeData()
    }

    /** 그룹 탈퇴 후 즉시 GroupSelection 화면으로 전환 */
    fun onGroupLeft() {
        viewModelScope.launch {
            val allFamilies = runCatching { mongleRepository.getMyFamilies() }.getOrElse { emptyList() }
            _uiState.update {
                it.copy(
                    appState = AppState.GroupSelection,
                    family = null,
                    familyMembers = emptyList(),
                    allFamilies = allFamilies,
                    todayQuestion = null,
                    hasAnsweredToday = false,
                    hasSkippedToday = false
                )
            }
        }
    }
}
