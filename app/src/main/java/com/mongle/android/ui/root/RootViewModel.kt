package com.mongle.android.ui.root

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AuthRepository
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import com.mongle.android.domain.repository.TreeRepository
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
    val familyTree: TreeProgress = TreeProgress(),
    val family: MongleGroup? = null,
    val familyMembers: List<User> = emptyList(),
    val allFamilies: List<MongleGroup> = emptyList(),
    val hasAnsweredToday: Boolean = false,
    val errorMessage: String? = null,
    val pendingInviteCode: String? = null
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val mongleRepository: MongleRepository,
    private val questionRepository: QuestionRepository,
    private val treeRepository: TreeRepository,
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
                    _uiState.update {
                        it.copy(
                            appState = AppState.Authenticated,
                            todayQuestion = question,
                            familyTree = tree ?: TreeProgress(),
                            family = family,
                            familyMembers = members,
                            allFamilies = allFamilies,
                            hasAnsweredToday = question?.hasMyAnswer ?: false
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

    fun onAnswerSubmitted() {
        _uiState.update { it.copy(hasAnsweredToday = true) }
    }

    fun refreshData() {
        loadHomeData()
    }
}
