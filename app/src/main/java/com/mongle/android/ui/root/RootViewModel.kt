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
    val familyTree: TreeProgress = TreeProgress(),
    val family: MongleGroup? = null,
    val familyMembers: List<User> = emptyList(),
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
                loadHomeData()
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

                if (family == null) {
                    // No active family → go to GroupSelection
                    _uiState.update { it.copy(appState = AppState.GroupSelection) }
                } else {
                    // 일일 접속 하트 획득 시도
                    val dailyHeart = runCatching { userRepository.claimDailyHeart() }.getOrNull()

                    _uiState.update {
                        it.copy(
                            appState = AppState.Authenticated,
                            todayQuestion = question,
                            familyTree = tree ?: TreeProgress(),
                            family = family,
                            familyMembers = members,
                            hasAnsweredToday = question?.hasMyAnswer ?: false,
                            dailyHeartGranted = dailyHeart?.heartsGranted ?: 0,
                            currentUser = if (dailyHeart != null) {
                                it.currentUser?.copy(hearts = dailyHeart.heartsRemaining)
                            } else {
                                it.currentUser
                            }
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

    fun onLoggedIn(user: User) {
        _uiState.update { it.copy(currentUser = user) }
        loadHomeData()
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

    fun dismissDailyHeartPopup() {
        _uiState.update { it.copy(dailyHeartGranted = 0) }
    }

    fun refreshData() {
        loadHomeData()
    }
}
