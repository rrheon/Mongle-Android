package com.mongle.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleSceneView
import com.mongle.android.ui.common.SceneMemberInfo
import com.mongle.android.ui.theme.MongleHeartRed
import com.mongle.android.ui.theme.MongleHeartRedLight
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

private val sceneCharacterColors = listOf(
    MongleMonggleGreenLight,
    MongleMonggleYellow,
    MongleMonggleBlue,
    MongleMongglePink,
    MongleMonggleOrange
)

@Composable
fun HomeScreen(
    onNavigateToQuestionDetail: (Question) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToNudge: (User) -> Unit,
    onNavigateToWriteQuestion: () -> Unit,
    onNavigateToGroupSelect: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // QuestionSheet / PeerAnswerSheet 상태
    var showQuestionSheet by remember { mutableStateOf(false) }
    var peerAnswerData by remember { mutableStateOf<Triple<User, Int, Answer>?>(null) }

    // ViewModel 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToQuestionDetail -> onNavigateToQuestionDetail(event.question)
                is HomeEvent.NavigateToNudge -> onNavigateToNudge(event.targetUser)
                is HomeEvent.ShowPeerAnswer ->
                    peerAnswerData = Triple(event.member, event.memberIndex, event.answer)
            }
        }
    }

    // 씬 멤버 목록
    val sceneMembers = uiState.familyMembers.mapIndexed { index, user ->
        SceneMemberInfo(
            id = user.id,
            name = user.name,
            color = sceneCharacterColors[index % sceneCharacterColors.size],
            hasAnswered = uiState.memberAnswerStatus[user.id] == true
        )
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
        Column(modifier = Modifier.fillMaxSize()) {
            // ── TopBar (그룹명 + 하트 + 알림 + 오늘의 질문 카드) ──
            HomeTopBar(
                groupName = uiState.family?.name ?: "몽글",
                currentFamilyId = uiState.family?.id,
                allFamilies = uiState.allFamilies,
                hearts = uiState.currentUser?.hearts ?: 0,
                hasNotification = false,
                todayQuestion = uiState.todayQuestion,
                hasAnsweredToday = uiState.hasAnsweredToday,
                onQuestionTap = { if (uiState.todayQuestion != null) showQuestionSheet = true },
                onNotificationTap = onNavigateToNotifications,
                onWriteQuestionTap = onNavigateToWriteQuestion,
                onGroupManage = onNavigateToGroupSelect
            )

            // ── MongleScene (나머지 공간 전체) ──
            MongleSceneView(
                members = sceneMembers,
                currentUserId = uiState.currentUser?.id,
                hasCurrentUserAnswered = uiState.hasAnsweredToday,
                onMemberTapped = { info ->
                    val user = uiState.familyMembers.find { it.id == info.id }
                    user?.let { viewModel.onMemberTapped(it) }
                },
                modifier = Modifier.fillMaxSize()
            )
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
                androidx.compose.material3.CircularProgressIndicator(color = MonglePrimary)
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
                onNavigateToWriteQuestion()
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
}

// ─── HomeTopBar ──────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(
    groupName: String,
    currentFamilyId: java.util.UUID?,
    allFamilies: List<MongleGroup>,
    hearts: Int,
    hasNotification: Boolean,
    todayQuestion: Question?,
    hasAnsweredToday: Boolean,
    onQuestionTap: () -> Unit,
    onNotificationTap: () -> Unit,
    onWriteQuestionTap: () -> Unit,
    onGroupManage: () -> Unit = {}
) {
    var showHeartMenu by remember { mutableStateOf(false) }
    var showGroupDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 1단: 그룹명 + 하트 + 알림
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 그룹명 드롭다운
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showGroupDropdown = true }
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
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showGroupDropdown,
                    onDismissRequest = { showGroupDropdown = false }
                ) {
                    // 그룹 목록: 현재 그룹에 체크마크
                    if (allFamilies.isNotEmpty()) {
                        allFamilies.forEach { family ->
                            val isSelected = family.id == currentFamilyId
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = family.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MonglePrimary else MongleTextPrimary
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
                                },
                                onClick = { showGroupDropdown = false }
                            )
                        }
                    } else {
                        DropdownMenuItem(
                            text = { Text(groupName, style = MaterialTheme.typography.bodyMedium) },
                            onClick = { showGroupDropdown = false }
                        )
                    }
                    HorizontalDivider()
                    // 그룹 관리 버튼
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MongleTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "그룹 관리",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MongleTextSecondary
                                )
                            }
                        },
                        onClick = {
                            showGroupDropdown = false
                            onGroupManage()
                        }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 하트 버튼
                Box {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MongleHeartRedLight)
                            .clickable { showHeartMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
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
                    DropdownMenu(
                        expanded = showHeartMenu,
                        onDismissRequest = { showHeartMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "현재 보유 ${hearts}개 ❤️",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MongleHeartRed
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🔄 질문 다시받기  하트 1개", style = MaterialTheme.typography.bodySmall)
                                    Text("✏️ 나만의 질문 작성  하트 3개", style = MaterialTheme.typography.bodySmall)
                                    Text("📣 재촉하기  하트 1개", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "☀️ 매일 오전 +1 · 답변 완료 +3",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MongleTextHint
                                    )
                                }
                            },
                            onClick = { showHeartMenu = false }
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
                        contentDescription = "알림",
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

        // 2단: 오늘의 질문 카드
        if (todayQuestion != null) {
            TodayQuestionCard(
                question = todayQuestion,
                hasAnswered = hasAnsweredToday,
                onTap = onQuestionTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MongleSpacing.md)
                    .padding(bottom = MongleSpacing.sm)
            )
        }
    }
}

// ─── 오늘의 질문 카드 ─────────────────────────────────────────────────────────

@Composable
private fun TodayQuestionCard(
    question: Question,
    hasAnswered: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onTap() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "오늘의 질문",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MonglePrimary
                    )
                    if (hasAnswered) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MonglePrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = question.content,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MongleTextPrimary,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MongleTextHint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
