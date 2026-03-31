package com.mongle.android.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mongle.android.ui.common.MonglePopup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleLogo
import com.mongle.android.ui.common.MongleLogoSize
import com.mongle.android.ui.common.MongleToastOverlay
import com.mongle.android.ui.common.MongleToastType
import com.mongle.android.ui.common.defaultMessage
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
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextSecondary
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
    var showGroupSelect by remember { mutableStateOf(false) }
    var groupLeftToast by remember { mutableStateOf(false) }
    var showAnswerSubmittedToast by remember { mutableStateOf(false) }
    var showHeartPopup by remember { mutableStateOf(false) }
    var answerSubmittedCount by remember { mutableIntStateOf(0) }

    // 답변 완료 토스트 3초 후 자동 닫기
    LaunchedEffect(showAnswerSubmittedToast) {
        if (showAnswerSubmittedToast) {
            delay(3000)
            showAnswerSubmittedToast = false
        }
    }

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
            // 일일 접속 하트 팝업
            if (uiState.dailyHeartGranted > 0) {
                Dialog(
                    onDismissRequest = { rootViewModel.dismissDailyHeartPopup() },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    MonglePopup(
                        title = "하트 획득",
                        description = "매일 첫 접속 보너스!\n하트 ${uiState.dailyHeartGranted}개를 받았어요.",
                        note = "현재 ${uiState.currentUser?.hearts ?: 0}개 보유 중이에요.",
                        primaryLabel = "확인",
                        onPrimary = { rootViewModel.dismissDailyHeartPopup() }
                    )
                }
            }

            // 답변 완료 하트 적립 팝업
            if (showHeartPopup) {
                HeartEarnedPopup(
                    hearts = uiState.currentUser?.hearts ?: 0,
                    onDismiss = { showHeartPopup = false }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    showQuestionDetail != null -> {
                        QuestionDetailScreen(
                            question = showQuestionDetail!!,
                            currentUser = uiState.currentUser,
                            familyMembers = uiState.familyMembers,
                            onAnswerSubmitted = { answer, isNewAnswer ->
                                rootViewModel.onAnswerSubmitted(answer, isNewAnswer)
                                showQuestionDetail = null
                                showAnswerSubmittedToast = true
                                if (isNewAnswer) showHeartPopup = true
                                answerSubmittedCount++
                            },
                            onClose = { showQuestionDetail = null }
                        )
                    }
                    showNotifications -> {
                        NotificationScreen(onBack = { showNotifications = false })
                    }
                    showNudgeTarget != null && adManager != null -> {
                        PeerNudgeScreen(
                            targetUser = showNudgeTarget!!,
                            currentUserHearts = uiState.currentUser?.hearts ?: 0,
                            adManager = adManager,
                            onBack = { showNudgeTarget = null },
                            onNudgeSent = { heartsRemaining ->
                                rootViewModel.updateHearts(heartsRemaining)
                            }
                        )
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
                    showGroupSelect -> {
                        GroupSelectScreen(
                            onBack = { showGroupSelect = false },
                            onGroupSelected = { familyId ->
                                showGroupSelect = false
                                rootViewModel.onGroupSelected(familyId)
                            },
                            onCreatedOrJoined = {
                                showGroupSelect = false
                                rootViewModel.onGroupCreatedOrJoined()
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
                            onNavigateToGroupSelect = { showGroupSelect = true },
                            onGroupSelected = { familyId -> rootViewModel.onGroupSelected(familyId) },
                            onLogout = { rootViewModel.logout() },
                            onGroupLeft = {
                                groupLeftToast = true
                                rootViewModel.loadHomeData()
                            },
                            answerSubmittedCount = answerSubmittedCount
                        )
                    }
                }

                // 답변 완료 토스트 (화면 하단) — iOS MongleToastView 스타일
                MongleToastOverlay(
                    message = if (showAnswerSubmittedToast && showQuestionDetail == null)
                        MongleToastType.ANSWER_SUBMITTED.defaultMessage else null,
                    type = MongleToastType.ANSWER_SUBMITTED,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun HeartEarnedPopup(hearts: Int, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        MonglePopup(
            title = "하트 1개를 받았어요!",
            description = "마음을 남겨서 하트 1개를 받았어요.",
            note = "현재 보유: $hearts 개",
            primaryLabel = "확인",
            onPrimary = onDismiss,
            extraContent = {
                Text(
                    text = "하트 사용처\n• 질문 다시받기 (1개)\n• 나만의 질문 작성 (1개)\n• 재촉하기 (1개)\n\n매일 오전 6시에 하트 1개가 충전돼요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextSecondary
                )
            }
        )
    }
}
