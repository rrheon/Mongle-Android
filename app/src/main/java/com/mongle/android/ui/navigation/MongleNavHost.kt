package com.mongle.android.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleLogo
import com.mongle.android.ui.common.MongleLogoSize
import com.mongle.android.ui.groupselect.GroupSelectScreen
import com.mongle.android.ui.login.LoginScreen
import com.mongle.android.ui.main.MainTabScreen
import com.mongle.android.ui.notification.NotificationScreen
import com.mongle.android.ui.nudge.PeerNudgeScreen
import com.mongle.android.ui.onboarding.OnboardingScreen
import com.mongle.android.ui.question.QuestionDetailScreen
import com.mongle.android.ui.question.WriteQuestionScreen
import com.mongle.android.ui.root.AppState
import com.mongle.android.ui.root.RootViewModel
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.util.AdManager

@Composable
fun MongleNavHost(
    rootViewModel: RootViewModel = hiltViewModel(),
    adManager: AdManager? = null
) {
    val uiState by rootViewModel.uiState.collectAsState()
    var showQuestionDetail by remember { mutableStateOf<Question?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    var showNudgeTarget by remember { mutableStateOf<User?>(null) }
    var showWriteQuestion by remember { mutableStateOf(false) }
    var groupLeftToast by remember { mutableStateOf(false) }

    when (uiState.appState) {
        AppState.Onboarding -> {
            OnboardingScreen(
                onGetStarted = { rootViewModel.onOnboardingCompleted() },
                onNeverShowAgain = { rootViewModel.onOnboardingNeverShowAgain() }
            )
        }

        AppState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MongleSpacing.lg)
                ) {
                    MongleLogo(size = MongleLogoSize.LARGE)
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        AppState.Unauthenticated -> {
            LoginScreen(
                onLoggedIn = { user -> rootViewModel.onLoggedIn(user) },
                onBrowse = { rootViewModel.onBrowse() }
            )
        }

        AppState.GroupSelection -> {
            GroupSelectScreen(
                pendingInviteCode = uiState.pendingInviteCode,
                showGroupLeftToast = groupLeftToast,
                onGroupSelected = { familyId ->
                    groupLeftToast = false
                    rootViewModel.onGroupSelected(familyId)
                },
                onCreatedOrJoined = {
                    groupLeftToast = false
                    rootViewModel.onGroupCreatedOrJoined()
                },
                onPendingCodeConsumed = { rootViewModel.clearPendingInviteCode() }
            )
        }

        AppState.Authenticated -> {
            when {
                showQuestionDetail != null -> {
                    QuestionDetailScreen(
                        question = showQuestionDetail!!,
                        currentUser = uiState.currentUser,
                        familyMembers = uiState.familyMembers,
                        onAnswerSubmitted = {
                            rootViewModel.onAnswerSubmitted()
                            showQuestionDetail = null
                        },
                        onClose = { showQuestionDetail = null }
                    )
                }
                showNotifications -> {
                    NotificationScreen(onBack = { showNotifications = false })
                }
                showNudgeTarget != null -> {
                    adManager?.let {
                        PeerNudgeScreen(
                            targetUser = showNudgeTarget!!,
                            currentUserHearts = uiState.currentUser?.hearts ?: 0,
                            adManager = it,
                            onBack = { showNudgeTarget = null }
                        )
                    }
                }
                showWriteQuestion -> {
                    WriteQuestionScreen(
                        onClose = { showWriteQuestion = false },
                        onQuestionSubmitted = { question ->
                            showWriteQuestion = false
                            rootViewModel.onAnswerSubmitted()
                        }
                    )
                }
                else -> {
                    MainTabScreen(
                        rootUiState = uiState,
                        onNavigateToQuestionDetail = { question -> showQuestionDetail = question },
                        onNavigateToNotifications = { showNotifications = true },
                        onNavigateToNudge = { user -> showNudgeTarget = user },
                        onNavigateToWriteQuestion = { showWriteQuestion = true },
                        onLogout = { rootViewModel.logout() },
                        onGroupLeft = {
                            groupLeftToast = true
                            rootViewModel.loadHomeData()
                        }
                    )
                }
            }
        }
    }
}
