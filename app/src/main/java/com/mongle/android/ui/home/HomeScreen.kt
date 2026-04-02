package com.mongle.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mongle.android.ui.common.MonglePopup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleSceneView
import com.mongle.android.ui.common.SceneMemberInfo
import androidx.compose.ui.graphics.vector.ImageVector
import com.mongle.android.ui.theme.MongleAccentOrange
import com.mongle.android.ui.theme.MongleHeartRed
import com.mongle.android.ui.theme.MongleBgNeutral
import com.mongle.android.ui.theme.MongleHeartRedLight
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import androidx.compose.ui.res.stringResource
import com.mongle.android.R

// iOS monggleColor(for:) 순서와 동일: [Green, Yellow, Pink, Blue, Orange]
private val sceneCharacterColors = listOf(
    MongleMonggleGreenLight,
    MongleMonggleYellow,
    MongleMongglePink,
    MongleMonggleBlue,
    MongleMonggleOrange
)

private fun moodColor(moodId: String?, fallback: Color): Color = when (moodId) {
    "happy" -> MongleMonggleYellow
    "calm" -> MongleMonggleGreenLight
    "loved" -> MongleMongglePink
    "sad" -> MongleMonggleBlue
    "tired" -> MongleMonggleOrange
    else -> fallback
}

@Composable
fun HomeScreen(
    onNavigateToQuestionDetail: (Question) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToNudge: (User) -> Unit,
    onNavigateToWriteQuestion: () -> Unit,
    onNavigateToGroupSelect: () -> Unit = {},
    onGroupSelected: (java.util.UUID) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // QuestionSheet / PeerAnswerSheet 상태
    var showQuestionSheet by remember { mutableStateOf(false) }
    var peerAnswerData by remember { mutableStateOf<Triple<User, Int, Answer>?>(null) }

    // 그룹 드롭다운 상태 (호이스팅)
    var showGroupDropdown by remember { mutableStateOf(false) }
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var overlayHeightPx by remember { mutableIntStateOf(0) }

    // 다이얼로그 상태
    var showAnswerFirstDialog by remember { mutableStateOf<String?>(null) }
    var showNudgeUnavailableDialog by remember { mutableStateOf<String?>(null) }
    var showSkipConfirmDialog by remember { mutableStateOf(false) }
    var showWriteConfirmDialog by remember { mutableStateOf(false) }

    // ViewModel 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToQuestionDetail -> onNavigateToQuestionDetail(event.question)
                is HomeEvent.NavigateToNudge -> onNavigateToNudge(event.targetUser)
                is HomeEvent.ShowPeerAnswer ->
                    peerAnswerData = Triple(event.member, event.memberIndex, event.answer)
                is HomeEvent.ShowAnswerFirstToView -> showAnswerFirstDialog = event.memberName
                is HomeEvent.ShowNudgeUnavailable -> showNudgeUnavailableDialog = event.memberName
            }
        }
    }

    // 씬 멤버 목록 (둘러보기 모드에서는 데모 캐릭터 표시)
    val sceneMembers = if (uiState.familyMembers.isNotEmpty()) {
        uiState.familyMembers.mapIndexed { index, user ->
            val fallbackColor = sceneCharacterColors[index % sceneCharacterColors.size]
            val answer = uiState.memberAnswers[user.id]
            val isCurrentUser = user.id == uiState.currentUser?.id
            val hasAnswered = if (isCurrentUser) uiState.hasAnsweredToday else uiState.memberAnswerStatus[user.id] == true
            SceneMemberInfo(
                id = user.id,
                name = user.name,
                color = if (hasAnswered) {
                    val moodId = answer?.moodId ?: user.moodId
                    moodColor(moodId, fallbackColor)
                } else fallbackColor,
                hasAnswered = hasAnswered
            )
        }
    } else {
        // 둘러보기 모드: 5명의 데모 캐릭터로 애니메이션 씬 표시
        listOf(
            stringResource(R.string.demo_char_1),
            stringResource(R.string.demo_char_2),
            stringResource(R.string.demo_char_3),
            stringResource(R.string.demo_char_4),
            stringResource(R.string.demo_char_5)
        ).mapIndexed { index, demoName ->
            SceneMemberInfo(
                id = java.util.UUID.nameUUIDFromBytes("demo_$index".toByteArray()),
                name = demoName,
                color = sceneCharacterColors[index % sceneCharacterColors.size],
                hasAnswered = index % 2 == 0
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEDF7F0),
                        Color(0xFFF5FAF0),
                        Color(0xFFFFF8F2)
                    )
                )
            )
    ) {
        // ── MongleScene (전체 화면) ──
        MongleSceneView(
            members = sceneMembers,
            currentUserId = uiState.currentUser?.id,
            hasCurrentUserAnswered = uiState.hasAnsweredToday,
            hasCurrentUserSkipped = uiState.hasSkippedToday,
            topInsetPx = overlayHeightPx,
            onViewAnswer = { info ->
                val user = uiState.familyMembers.find { it.id == info.id }
                user?.let { viewModel.onViewAnswerTapped(it) }
            },
            onNudge = { info ->
                val user = uiState.familyMembers.find { it.id == info.id }
                user?.let { viewModel.onNudgeTapped(it) }
            },
            onSelfTap = { if (uiState.todayQuestion != null) showQuestionSheet = true },
            onAnswerFirstToView = { name -> showAnswerFirstDialog = name },
            onAnswerFirstToNudge = { name -> showNudgeUnavailableDialog = name },
            modifier = Modifier.fillMaxSize()
        )

        // ── 상단 오버레이 (TopBar + 오늘의 질문 카드) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { overlayHeightPx = it.size.height }
        ) {
            HomeTopBar(
                groupName = uiState.family?.name ?: stringResource(R.string.home_default_group),
                currentFamilyId = uiState.family?.id,
                allFamilies = uiState.allFamilies,
                hearts = uiState.currentUser?.hearts ?: 0,
                hasNotification = uiState.hasUnreadNotifications,
                showGroupDropdown = showGroupDropdown,
                onGroupDropdownToggle = { showGroupDropdown = !showGroupDropdown },
                onTopBarMeasured = { topBarHeightPx = it },
                onNotificationTap = onNavigateToNotifications,
                onGroupManage = onNavigateToGroupSelect
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 오늘의 질문 카드 (씬 안쪽 오버레이)
            if (uiState.todayQuestion != null) {
                TodayQuestionCard(
                    question = uiState.todayQuestion!!,
                    hasAnswered = uiState.hasAnsweredToday,
                    onTap = { showQuestionSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MongleSpacing.md)
                        .padding(bottom = MongleSpacing.sm)
                )
            } else if (uiState.lastQuestion != null) {
                // 오늘의 질문이 아직 도착하지 않았을 때 전날 질문을 읽기 전용으로 표시
                TodayQuestionCard(
                    question = uiState.lastQuestion!!,
                    hasAnswered = true,
                    onTap = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MongleSpacing.md)
                        .padding(bottom = MongleSpacing.sm)
                )
            }
        }

        // 로딩
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MonglePrimary)
            }
        }

        // 그룹 드롭다운 오버레이
        if (showGroupDropdown) {
            val density = LocalDensity.current
            val topBarHeightDp = with(density) { topBarHeightPx.toDp() }
            val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

            // 반투명 배경 (탭하면 닫힘)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showGroupDropdown = false }
            )

            // 드롭다운 패널 (좌상단, 화면 절반 너비)
            Box(
                modifier = Modifier
                    .padding(top = topBarHeightDp, start = 16.dp)
                    .width(screenWidthDp / 2)
                    .wrapContentHeight()
            ) {
                HomeGroupDropdownPanel(
                    allFamilies = uiState.allFamilies,
                    currentFamilyId = uiState.family?.id,
                    onGroupSelected = { group ->
                        showGroupDropdown = false
                        onGroupSelected(group.id)
                    },
                    onGroupManage = { showGroupDropdown = false; onNavigateToGroupSelect() }
                )
            }
        }
    }

    // QuestionSheet
    if (showQuestionSheet && uiState.todayQuestion != null) {
        QuestionSheetBottomSheet(
            question = uiState.todayQuestion!!,
            hasAnswered = uiState.hasAnsweredToday,
            onDismiss = { showQuestionSheet = false },
            onAnswerTap = {
                showQuestionSheet = false
                onNavigateToQuestionDetail(uiState.todayQuestion!!)
            },
            onWriteQuestionTap = {
                showQuestionSheet = false
                showWriteConfirmDialog = true
            },
            onSkipTapped = {
                showQuestionSheet = false
                showSkipConfirmDialog = true
            }
        )
    }

    // PeerAnswerSheet
    peerAnswerData?.let { (member, index, answer) ->
        PeerAnswerSheet(
            member = member,
            memberIndex = index,
            questionText = uiState.todayQuestion?.content ?: "",
            answer = answer,
            onDismiss = { peerAnswerData = null }
        )
    }

    // 먼저 답변 완료 팝업 (답변 보기 시도)
    showAnswerFirstDialog?.let { name ->
        Dialog(
            onDismissRequest = { showAnswerFirstDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.home_answer_first_title),
                description = stringResource(R.string.home_answer_first_view_desc, name),
                primaryLabel = stringResource(R.string.home_answer_btn),
                onPrimary = {
                    showAnswerFirstDialog = null
                    if (uiState.todayQuestion != null) showQuestionSheet = true
                },
                secondaryLabel = stringResource(R.string.common_cancel),
                onSecondary = { showAnswerFirstDialog = null }
            )
        }
    }

    // 먼저 답변 완료 팝업 (재촉하기 시도)
    showNudgeUnavailableDialog?.let { name ->
        Dialog(
            onDismissRequest = { showNudgeUnavailableDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.home_answer_first_title),
                description = stringResource(R.string.home_answer_first_nudge_desc, name),
                primaryLabel = stringResource(R.string.home_answer_btn),
                onPrimary = {
                    showNudgeUnavailableDialog = null
                    if (uiState.todayQuestion != null) showQuestionSheet = true
                },
                secondaryLabel = stringResource(R.string.common_cancel),
                onSecondary = { showNudgeUnavailableDialog = null }
            )
        }
    }

    // 나만의 질문 작성하기 확인 팝업
    if (showWriteConfirmDialog) {
        val currentHearts = uiState.currentUser?.hearts ?: 0
        Dialog(
            onDismissRequest = { showWriteConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            if (currentHearts >= 3) {
                MonglePopup(
                    title = stringResource(R.string.home_write_question_title),
                    description = stringResource(R.string.home_write_question_desc),
                    primaryLabel = stringResource(R.string.home_write_btn),
                    onPrimary = {
                        showWriteConfirmDialog = false
                        onNavigateToWriteQuestion()
                    },
                    secondaryLabel = stringResource(R.string.common_cancel),
                    onSecondary = { showWriteConfirmDialog = false }
                )
            } else {
                MonglePopup(
                    title = stringResource(R.string.home_hearts_insufficient_title),
                    description = stringResource(R.string.home_hearts_insufficient_write, currentHearts),
                    primaryLabel = stringResource(R.string.common_confirm),
                    onPrimary = { showWriteConfirmDialog = false }
                )
            }
        }
    }

    // 질문 넘기기 확인 팝업
    if (showSkipConfirmDialog) {
        val currentHearts = uiState.currentUser?.hearts ?: 0
        Dialog(
            onDismissRequest = { showSkipConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            if (currentHearts >= 3) {
                MonglePopup(
                    title = stringResource(R.string.home_skip_question_title),
                    description = stringResource(R.string.home_skip_question_desc),
                    primaryLabel = stringResource(R.string.home_skip_btn),
                    onPrimary = {
                        showSkipConfirmDialog = false
                        viewModel.skipQuestion()
                    },
                    secondaryLabel = stringResource(R.string.common_cancel),
                    onSecondary = { showSkipConfirmDialog = false }
                )
            } else {
                MonglePopup(
                    title = stringResource(R.string.home_hearts_insufficient_title),
                    description = stringResource(R.string.home_hearts_insufficient_skip, currentHearts),
                    primaryLabel = stringResource(R.string.common_confirm),
                    onPrimary = { showSkipConfirmDialog = false }
                )
            }
        }
    }
}

// ─── HomeTopBar ──────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(
    groupName: String,
    currentFamilyId: java.util.UUID?,
    allFamilies: List<MongleGroup>,
    hearts: Int,
    hasNotification: Boolean,
    showGroupDropdown: Boolean,
    onGroupDropdownToggle: () -> Unit,
    onTopBarMeasured: (Int) -> Unit = {},
    onNotificationTap: () -> Unit,
    onGroupManage: () -> Unit = {}
) {
    var showHeartMenu by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (showGroupDropdown) 180f else 0f,
        animationSpec = tween(200),
        label = "chevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 그룹명 + 하트 + 알림
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates -> onTopBarMeasured(coordinates.size.height) }
                .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 그룹명 드롭다운 토글
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onGroupDropdownToggle() }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MongleTextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MongleTextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 하트 버튼
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MongleBgNeutral) // iOS 기준: bgNeutral
                        .clickable { showHeartMenu = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MongleHeartRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = hearts.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MongleTextPrimary
                    )
                }

                // 하트 정보 말풍선 (iOS HeartCalloutView 동일)
                if (showHeartMenu) {
                    Popup(
                        onDismissRequest = { showHeartMenu = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        HeartCalloutBubble(
                            hearts = hearts,
                            onDismiss = { showHeartMenu = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(MongleSpacing.sm))

                // 알림 버튼
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFF2F2F2))
                        .clickable { onNotificationTap() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.home_notifications),
                        tint = MonglePrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    if (hasNotification) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Red, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }

    }
}

// ─── 오늘의 질문 카드 ─────────────────────────────────────────────────────────

@Composable
private fun TodayQuestionCard(
    question: Question,
    hasAnswered: Boolean,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isTappable = onTap != null
    Box(
        modifier = modifier
            .then(if (isTappable) Modifier.clickable { onTap?.invoke() } else Modifier)
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_today_question),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MonglePrimary
                    )
                    if (hasAnswered) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MonglePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = question.content,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isTappable) MongleTextPrimary else MongleTextSecondary,
                    maxLines = 2
                )
            }
            if (isTappable) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MongleTextHint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── 오전 12시 전 플레이스홀더 카드 ──────────────────────────────────────────

@Composable
private fun TodayQuestionPlaceholderCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_today_question),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MonglePrimary.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.home_question_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary,
                    maxLines = 2
                )
            }
        }
    }
}

// ─── 하트 정보 말풍선 (iOS HeartCalloutView 동일) ────────────────────────────

@Composable
private fun HeartCalloutBubble(hearts: Int, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
    ) {
        // 말풍선 카드 — 우측 상단 하트 버튼 아래에 위치
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = MongleSpacing.md)
                .width(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {} // 내부 클릭 dismiss 방지
                )
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 보유 하트
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MongleHeartRed,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = stringResource(R.string.home_heart_count, hearts),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleHeartRed
                )
            }

            HorizontalDivider(color = Color(0xFFE0E0E0))

            // 사용처 목록
            HeartCalloutRow(icon = Icons.Default.Refresh, color = MonglePrimary, label = stringResource(R.string.home_heart_replace), cost = "1")
            HeartCalloutRow(icon = Icons.Default.Create, color = MongleAccentOrange, label = stringResource(R.string.home_heart_write), cost = "3")
            HeartCalloutRow(icon = Icons.Default.Campaign, color = MongleHeartRed, label = stringResource(R.string.home_heart_nudge), cost = "1")

            HorizontalDivider(color = Color(0xFFE0E0E0))

            // 충전 안내
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = MonglePrimary,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = stringResource(R.string.home_heart_earn_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MongleTextSecondary
                )
            }
        }
    }
}

@Composable
private fun HeartCalloutRow(icon: ImageVector, color: Color, label: String, cost: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MongleTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = cost,
            style = MaterialTheme.typography.labelSmall,
            color = MongleHeartRed
        )
    }
}

// ─── 그룹 드롭다운 패널 ───────────────────────────────────────────────────────

@Composable
private fun HomeGroupDropdownPanel(
    allFamilies: List<MongleGroup>,
    currentFamilyId: java.util.UUID?,
    onGroupSelected: (MongleGroup) -> Unit,
    onGroupManage: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            allFamilies.forEachIndexed { index, family ->
                val isSelected = family.id == currentFamilyId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupSelected(family) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = family.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MonglePrimary else MongleTextPrimary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MonglePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (index < allFamilies.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            if (allFamilies.isNotEmpty()) {
                HorizontalDivider()
            }
            // 그룹 관리 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroupManage() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = MongleTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    stringResource(R.string.home_group_manage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MongleTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
