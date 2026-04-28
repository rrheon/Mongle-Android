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
import kotlinx.coroutines.Job
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
    /** 이메일 로그인 플로우 (이메일/비밀번호 입력 → 서버 로그인) */
    data object EmailLogin : AppState()
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
    val pendingLegalVersions: LegalVersions = LegalVersions(terms = "", privacy = ""),
    /**
     * 토큰 만료(401+refresh 실패) 시 사용자에게 "다시 로그인이 필요해요" 안내를 노출.
     * iOS MG-33 패리티. 무음 로그아웃 회피.
     */
    val showSessionExpiredPopup: Boolean = false
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

    // 빠른 그룹 전환 시 이전 selectFamily/loadHomeData 흐름이 늦게 끝나
    // 서버 활성 그룹과 클라이언트 활성 그룹이 어긋나는 race 를 차단한다.
    private var groupSelectionJob: Job? = null

    // iOS MG-37 패리티 — scenePhase 빠른 반복(백그라운드↔포그라운드)으로 loadHomeData 가
    // 누적 호출되는 것을 차단. 다음 진입 시 이전 in-flight 호출 cancel.
    private var loadHomeDataJob: Job? = null

    init {
        checkAuthStatus()
        // 토큰 만료(401 + 갱신 실패) 이벤트 구독 → 안내 팝업 + 로그인 화면 (iOS MG-33 패리티)
        viewModelScope.launch {
            sessionExpiredNotifier.events.collect {
                _uiState.update {
                    RootUiState(
                        appState = AppState.Unauthenticated,
                        showSessionExpiredPopup = true
                    )
                }
            }
        }
    }

    /** sessionExpired 안내 팝업 닫기 */
    fun dismissSessionExpiredPopup() {
        _uiState.update { it.copy(showSessionExpiredPopup = false) }
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
        loadHomeDataJob?.cancel()
        loadHomeDataJob = viewModelScope.launch {
            try {
                val familyResult = runCatching { mongleRepository.getMyFamily() }.getOrNull()
                val family = familyResult?.first
                val members = familyResult?.second ?: emptyList()
                var question = runCatching { questionRepository.getTodayQuestion() }.getOrNull()
                val tree = runCatching { treeRepository.getMyTreeProgress() }.getOrElse { TreeProgress() }

                val allFamilies = runCatching { mongleRepository.getMyFamilies() }.getOrElse { emptyList() }

                // 오늘의 질문을 찾지 못한 경우 히스토리에서 가장 최근 질문을 fallback으로 사용
                if (question == null) {
                    val history = runCatching { questionRepository.getDailyHistory(page = 1, limit = 5) }.getOrElse { emptyList() }
                    val recent = history.firstOrNull()
                    if (recent != null) {
                        question = recent.question.copy(
                            dailyQuestionId = recent.id,
                            hasMyAnswer = recent.hasMyAnswer,
                            hasMySkipped = recent.hasMySkipped,
                            familyAnswerCount = recent.familyAnswerCount
                        )
                    }
                }

                if (family == null) {
                    // No active family → go to GroupSelection
                    _uiState.update { it.copy(appState = AppState.GroupSelection, allFamilies = allFamilies) }
                } else {
                    // 데일리 하트 지급 트리거 — 서버가 set-only 로 heartGrantedToday=true 를 응답.
                    // 클라 SharedPreferences 자체 카운터 방식은 다중 단말/그룹 전환 시 거짓 팝업이 발생해 폐기.
                    val refreshedUser = runCatching { authRepository.getCurrentUser(grantDailyHeart = true) }.getOrNull()
                    val heartGrantedToday = refreshedUser?.heartGrantedToday ?: false

                    // FCM 토큰 서버 등록
                    runCatching {
                        val fcmToken = context.getSharedPreferences("fcm", Context.MODE_PRIVATE)
                            .getString("token", null)
                        if (fcmToken != null) {
                            (userRepository as? com.mongle.android.data.remote.ApiUserRepository)
                                ?.registerFcmToken(fcmToken)
                        }
                    }

                    _uiState.update {
                        val serverMe = members.firstOrNull { m -> m.id == it.currentUser?.id }
                        // refreshedUser 우선 — 서버 grantDailyHeart 응답이 가장 최신.
                        // members 의 me 는 hearts 등 일부 필드만 동기화되며 heartGrantedToday 는 미포함.
                        val syncedUser = refreshedUser ?: serverMe ?: it.currentUser
                        it.copy(
                            appState = AppState.Authenticated,
                            todayQuestion = question,
                            lastQuestion = null,
                            familyTree = tree ?: TreeProgress(),
                            family = family,
                            familyMembers = members,
                            allFamilies = allFamilies,
                            hasAnsweredToday = question?.hasMyAnswer ?: false,
                            hasSkippedToday = question?.hasMySkipped ?: false,
                            // set-only — heartGrantedToday=false 응답일 땐 기존 값 유지(이미 본 팝업 재트리거 방지)
                            dailyHeartGranted = if (heartGrantedToday) 1 else it.dailyHeartGranted,
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
        groupSelectionJob?.cancel()
        groupSelectionJob = viewModelScope.launch {
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

    fun onEmailSignupRequested() {
        _uiState.update { it.copy(appState = AppState.EmailSignup) }
    }

    fun onEmailLoginRequested() {
        _uiState.update { it.copy(appState = AppState.EmailLogin) }
    }

    fun onEmailSignupCancelled() {
        _uiState.update { it.copy(appState = AppState.Unauthenticated) }
    }

    fun onEmailLoginCancelled() {
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
            // Unauthenticated 상태에서 도착한 푸시 tap / 딥링크로 설정된 stale pending 신호를
            // 새 사용자 화면이 자동 소비하지 않도록 명시 정리한다.
            _uiState.update {
                it.copy(
                    currentUser = user,
                    appState = AppState.Loading,
                    pendingNotificationType = null,
                    pendingInviteCode = null
                )
            }
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
        groupSelectionJob?.cancel()
        loadHomeDataJob?.cancel()
        viewModelScope.launch {
            runCatching { authRepository.logout() }
            clearUserScopedPrefs()
            // iOS MG-37 패리티 — pending 신호 명시 정리. RootUiState() 새 인스턴스로
            // default null 이 자동 적용되지만 명시적으로 의도를 드러낸다.
            _uiState.update {
                RootUiState(
                    appState = AppState.Unauthenticated,
                    pendingInviteCode = null,
                    pendingNotificationType = null,
                    pendingAppleCallbackUri = null
                )
            }
        }
    }

    /**
     * 다음 계정 로그인 시 이전 사용자의 그룹별 마커/푸시 토큰이 혼입되지 않도록
     * user-scoped SharedPreferences 를 일괄 정리한다.
     * mongle_auth 는 authRepository.logout() 에서 정리되고,
     * mongle_app_prefs (has_seen_onboarding, install sentinel) 는 보존한다.
     */
    private fun clearUserScopedPrefs() {
        context.getSharedPreferences("mongle_heart", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("fcm", Context.MODE_PRIVATE).edit().clear().apply()
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
