package com.mongle.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.TreeStage
import com.mongle.android.domain.model.User
import java.util.UUID
import com.mongle.android.domain.model.Answer
import com.mongle.android.ui.common.MongleCard
import com.mongle.android.ui.common.MongleCharacter
import com.mongle.android.ui.theme.MongleHeartRed
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextSecondary

// 홈 배경 그라디언트 색상 (iOS: FFF8F0 → FFF2EB → EFF8F1)
private val HomeBgStart = Color(0xFFFFF8F0)
private val HomeBgMid   = Color(0xFFFFF2EB)
private val HomeBgEnd   = Color(0xFFEFF8F1)

@Composable
fun HomeScreen(
    onNavigateToQuestionDetail: (Question) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToNudge: (User) -> Unit = {},
    onNavigateToWriteQuestion: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var peerAnswerTarget by remember {
        mutableStateOf<Triple<User, Int, com.mongle.android.domain.model.Answer>?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToQuestionDetail -> onNavigateToQuestionDetail(event.question)
                is HomeEvent.NavigateToNudge -> onNavigateToNudge(event.targetUser)
                is HomeEvent.ShowPeerAnswer -> {
                    peerAnswerTarget = Triple(event.member, event.memberIndex, event.answer)
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(HomeBgStart, HomeBgMid, HomeBgEnd))
            )
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MonglePrimary
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 상단 여백 (TopBar 높이만큼)
                Spacer(modifier = Modifier.height(72.dp))

                // 오늘의 질문 카드
                TodayQuestionCard(
                    question = uiState.todayQuestion,
                    hasAnsweredToday = uiState.hasAnsweredToday,
                    onTap = { viewModel.onQuestionTapped() },
                    modifier = Modifier
                        .padding(horizontal = MongleSpacing.md)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MongleSpacing.md))

                // 몽글 씬 (가족 캐릭터들)
                MongleSceneSection(
                    familyMembers = uiState.familyMembers,
                    currentUserId = uiState.currentUser?.id,
                    memberAnswerStatus = uiState.memberAnswerStatus,
                    onMemberTapped = { member -> viewModel.onMemberTapped(member) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MongleSpacing.md)
                )

                Spacer(modifier = Modifier.height(MongleSpacing.md))

                // 나무 진행도 (작은 뱃지)
                TreeProgressBadge(
                    treeProgress = uiState.familyTree,
                    modifier = Modifier
                        .padding(horizontal = MongleSpacing.md)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MongleSpacing.xl))
            }
        }

        // 상단 앱바 (floating)
        HomeTopBar(
            familyName = uiState.family?.name ?: "몽글",
            hearts = uiState.currentUser?.hearts ?: 0,
            onNotificationsTapped = onNavigateToNotifications,
            onWriteQuestionTapped = onNavigateToWriteQuestion,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 가족 답변 보기 BottomSheet
    peerAnswerTarget?.let { (member, memberIndex, answer) ->
        PeerAnswerSheet(
            member = member,
            memberIndex = memberIndex,
            questionText = uiState.todayQuestion?.content ?: "",
            answer = answer,
            onDismiss = { peerAnswerTarget = null }
        )
    }
}

@Composable
private fun HomeTopBar(
    familyName: String,
    hearts: Int,
    onNotificationsTapped: () -> Unit,
    onWriteQuestionTapped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showGroupDropdown by remember { mutableStateOf(false) }
    var showHeartMenu by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (showGroupDropdown) 180f else 0f,
        label = "arrow_rotation"
    )

    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.95f))
            .shadow(elevation = 2.dp)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = MongleSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        // 가족 이름 (좌측 드롭다운 버튼)
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showGroupDropdown = true }
            ) {
                Text(
                    text = familyName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "그룹 선택",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(arrowRotation)
                )
            }

            DropdownMenu(
                expanded = showGroupDropdown,
                onDismissRequest = { showGroupDropdown = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = familyName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MonglePrimary
                        )
                    },
                    onClick = { showGroupDropdown = false }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "그룹 관리",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { showGroupDropdown = false }
                )
            }
        }

        // 우측 버튼들
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 하트 뱃지 (탭 시 드롭다운)
            Box {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MongleHeartRed.copy(alpha = 0.12f))
                        .clickable { showHeartMenu = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "하트",
                        tint = MongleHeartRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$hearts",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MongleHeartRed
                    )
                }
                DropdownMenu(
                    expanded = showHeartMenu,
                    onDismissRequest = { showHeartMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "현재 보유 ${hearts}개 ❤️",
                                    style = MaterialTheme.typography.labelMedium.copy(FontWeight.SemiBold),
                                    color = MongleHeartRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("✏️ 나만의 질문 작성  하트 3개", style = MaterialTheme.typography.bodySmall)
                                Text("📣 재촉하기  하트 1개", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("🌅 매일 오전 +1 · 답변 완료 +3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {}
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("✏️ 나만의 질문 작성하기", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            showHeartMenu = false
                            onWriteQuestionTapped()
                        }
                    )
                }
            }
            // 알림 버튼
            IconButton(onClick = onNotificationsTapped) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MonglePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "알림",
                        tint = MonglePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayQuestionCard(
    question: Question?,
    hasAnsweredToday: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    MongleCard(modifier = modifier, onClick = onTap) {
        Column(modifier = Modifier.padding(MongleSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "오늘의 질문",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MonglePrimary
                )
                if (hasAnsweredToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MonglePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "답변 완료",
                            style = MaterialTheme.typography.labelSmall,
                            color = MonglePrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(MongleSpacing.xs))
            if (question != null) {
                Text(
                    text = question.content,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(MongleSpacing.xs))
                Text(
                    text = "# ${question.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextSecondary
                )
            } else {
                Text(
                    text = "오늘의 질문을 불러오는 중...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MongleTextSecondary
                )
            }
        }
    }
}

@Composable
private fun MongleSceneSection(
    familyMembers: List<User>,
    currentUserId: UUID?,
    memberAnswerStatus: Map<UUID, Boolean>,
    onMemberTapped: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    MongleCard(modifier = modifier) {
        Column(modifier = Modifier.padding(MongleSpacing.md)) {
            Text(
                text = "우리 가족",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MonglePrimary
            )
            Spacer(modifier = Modifier.height(MongleSpacing.sm))

            if (familyMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "가족을 초대해보세요 👨‍👩‍👧‍👦",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MongleSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    familyMembers.take(5).forEachIndexed { index, member ->
                        val isMe = member.id == currentUserId
                        val hasAnswered = memberAnswerStatus[member.id] == true
                        MongleCharacter(
                            user = member,
                            index = index,
                            size = 56.dp,
                            hasAnswered = hasAnswered,
                            showName = true,
                            modifier = if (!isMe) {
                                Modifier.clickable { onMemberTapped(member) }
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeProgressBadge(
    treeProgress: TreeProgress,
    modifier: Modifier = Modifier
) {
    val emoji = when (treeProgress.stage) {
        TreeStage.SEED        -> "🌱"
        TreeStage.SPROUT      -> "🌿"
        TreeStage.SAPLING     -> "🌲"
        TreeStage.YOUNG_TREE  -> "🌳"
        TreeStage.MATURE_TREE -> "🌴"
        TreeStage.FLOWERING   -> "🌸"
        TreeStage.BOUND       -> "🍎"
    }

    MongleCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MongleSpacing.xs)
            ) {
                Text(text = emoji, fontSize = 28.sp)
                Column {
                    Text(
                        text = treeProgress.stage.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MonglePrimary
                    )
                    Text(
                        text = "총 ${treeProgress.totalAnswers}개 답변",
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextSecondary
                    )
                }
            }
            if (treeProgress.consecutiveDays > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF5978E), Color(0xFFF7B4A0))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔥 ${treeProgress.consecutiveDays}일 연속",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
