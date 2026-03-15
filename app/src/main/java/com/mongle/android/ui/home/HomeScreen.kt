package com.mongle.android.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToQuestionDetail -> onNavigateToQuestionDetail(event.question)
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
            onNotificationsTapped = onNavigateToNotifications,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeTopBar(
    familyName: String,
    onNotificationsTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.95f))
            .shadow(elevation = 2.dp)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = MongleSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        // 가족 이름 (중앙)
        Text(
            text = familyName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )

        // 우측 버튼들
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 하트 버튼
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MongleHeartRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "하트",
                    tint = MongleHeartRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
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
                        MongleCharacter(
                            user = member,
                            index = index,
                            size = 56.dp,
                            hasAnswered = false,
                            showName = true
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
