package com.mongle.android.ui.question

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mongle.android.ui.common.MonglePopup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleCharacterAvatar
import com.mongle.android.ui.common.MongleTextField
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary

private data class MoodOption(val id: String, val label: String, val color: Color)

private val moodOptions = listOf(
    MoodOption("happy",  "행복",  MongleMonggleYellow),
    MoodOption("calm",   "평온",  MongleMonggleGreenLight),
    MoodOption("loved",  "사랑",  MongleMongglePink),
    MoodOption("sad",    "슬픔",  MongleMonggleBlue),
    MoodOption("tired",  "피곤",  MongleMonggleOrange)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    question: Question,
    currentUser: User?,
    familyMembers: List<User> = emptyList(),
    onAnswerSubmitted: (Answer, Boolean) -> Unit,
    onClose: () -> Unit,
    viewModel: QuestionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(question, currentUser) {
        viewModel.initialize(question, currentUser, familyMembers)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuestionDetailEvent.AnswerSubmitted -> onAnswerSubmitted(event.answer, event.isNewAnswer)
                QuestionDetailEvent.Closed -> onClose()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "마음 남기기",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAF8)
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MonglePrimary)
            }
        } else {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(MongleSpacing.md)
                ) {
                    // 질문 카드
                    QuestionCard(question = question)

                    // 기분 선택 카드
                    MoodPickerCard(
                        selectedIndex = uiState.selectedMoodIndex,
                        onMoodSelected = viewModel::onMoodSelected
                    )

                    // 답변 입력 카드
                    AnswerInputCard(
                        answerText = uiState.answerText,
                        onTextChange = viewModel::onAnswerTextChanged,
                        hasMyAnswer = uiState.hasMyAnswer
                    )

                    // 가족 답변 섹션
                    if (uiState.familyAnswers.isNotEmpty()) {
                        FamilyAnswersSection(familyAnswers = uiState.familyAnswers)
                    }

                    Spacer(modifier = Modifier.height(MongleSpacing.xl))
                }

                // CTA 버튼
                AnswerCtaButton(
                    hasMyAnswer = uiState.hasMyAnswer,
                    isSubmitting = uiState.isSubmitting,
                    enabled = uiState.answerText.trim().isNotEmpty(),
                    onClick = viewModel::submitAnswer
                )
            }
        }
    }

    // 기분 미선택 알림
    if (uiState.showMoodRequiredAlert) {
        Dialog(
            onDismissRequest = viewModel::dismissMoodRequiredAlert,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = "오늘의 몽글을 선택해주세요",
                description = "지금 기분과 가장 비슷한 몽글 캐릭터를 골라보세요",
                primaryLabel = "확인",
                onPrimary = viewModel::dismissMoodRequiredAlert
            )
        }
    }

    // 답변 수정 확인 (하트 소모 안내)
    if (uiState.showEditConfirmDialog) {
        Dialog(
            onDismissRequest = viewModel::dismissEditConfirmDialog,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = "답변을 수정할까요?",
                description = "답변을 수정하면 하트 1개가 소모돼요.",
                primaryLabel = "수정하기",
                onPrimary = viewModel::confirmEditAnswer,
                secondaryLabel = "취소",
                onSecondary = viewModel::dismissEditConfirmDialog
            )
        }
    }
}

// ─── 질문 카드 ───────────────────────────────────────────────────────────────

@Composable
private fun QuestionCard(question: Question) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(MongleSpacing.lg)
    ) {
        Text(
            text = "오늘의 질문",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MonglePrimary
        )
        Spacer(modifier = Modifier.height(MongleSpacing.xs))
        Text(
            text = question.content,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = 26.sp
            ),
            color = MongleTextPrimary
        )
    }
}

// ─── 기분 선택 카드 ───────────────────────────────────────────────────────────

@Composable
private fun MoodPickerCard(
    selectedIndex: Int?,
    onMoodSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(MongleSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "오늘의 몽글",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextSecondary
            )
            if (selectedIndex == null) {
                Text(
                    text = "하나를 선택해주세요",
                    style = MaterialTheme.typography.labelSmall,
                    color = MongleTextHint
                )
            }
        }

        Spacer(modifier = Modifier.height(MongleSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            moodOptions.forEachIndexed { index, mood ->
                MoodCell(
                    mood = mood,
                    isSelected = selectedIndex == index,
                    noneSelected = selectedIndex == null,
                    onClick = { onMoodSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MoodCell(
    mood: MoodOption,
    isSelected: Boolean,
    noneSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "mood_scale"
    )
    val dotColor by animateColorAsState(
        targetValue = if (isSelected) MonglePrimary else Color(0xFFE0E0E0),
        label = "mood_dot"
    )
    val alpha = if (noneSelected || isSelected) 1f else 0.45f

    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 몽글 캐릭터 (원형) — iOS MongleMonggle 스타일
        Box(
            modifier = Modifier
                .scale(scale)
                .size(36.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = mood.color.copy(alpha = 0.3f),
                    spotColor = mood.color.copy(alpha = 0.3f)
                )
                .background(mood.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val eyeSize = 36.dp * 0.18f
            val eyeOffset = 36.dp * 0.14f
            // 왼쪽 눈 (흰 테두리)
            Box(
                modifier = Modifier
                    .size(eyeSize + 2.dp)
                    .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(eyeSize)
                        .background(Color.Black, CircleShape)
                )
            }
            // 오른쪽 눈 (흰 테두리)
            Box(
                modifier = Modifier
                    .size(eyeSize + 2.dp)
                    .offset(x = eyeOffset, y = -eyeSize * 0.3f)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(eyeSize)
                        .background(Color.Black, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = mood.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MongleTextPrimary.copy(alpha = alpha),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dotColor, CircleShape)
        )
    }
}

// ─── 답변 입력 카드 ───────────────────────────────────────────────────────────

@Composable
private fun AnswerInputCard(
    answerText: String,
    onTextChange: (String) -> Unit,
    hasMyAnswer: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(MongleSpacing.md)
    ) {
        MongleTextField(
            value = answerText,
            onValueChange = onTextChange,
            placeholder = "오늘의 감정을 자유롭게 적어보세요.\n어떤 이야기든 좋아요.",
            modifier = Modifier.fillMaxWidth(),
            maxLines = 10,
            minLines = 5
        )
        Spacer(modifier = Modifier.height(MongleSpacing.xs))
        Text(
            text = "${answerText.length}/200",
            style = MaterialTheme.typography.labelSmall,
            color = if (answerText.length >= 200) MaterialTheme.colorScheme.error else MongleTextHint,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

// ─── 가족 답변 섹션 ───────────────────────────────────────────────────────────

@Composable
private fun FamilyAnswersSection(familyAnswers: List<FamilyAnswer>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
    ) {
        Text(
            text = "가족의 답변 (${familyAnswers.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextPrimary
        )
        familyAnswers.forEach { familyAnswer ->
            FamilyAnswerItem(familyAnswer = familyAnswer)
        }
    }
}

@Composable
private fun FamilyAnswerItem(familyAnswer: FamilyAnswer) {
    val index = familyAnswer.user.role.ordinal
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(MongleSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MongleCharacterAvatar(name = familyAnswer.user.name, index = index, size = 36.dp)
            Spacer(modifier = Modifier.width(MongleSpacing.sm))
            Text(
                text = familyAnswer.user.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(MongleSpacing.sm))
        Text(
            text = familyAnswer.answer.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MongleTextSecondary
        )
    }
}

// ─── CTA 버튼 ────────────────────────────────────────────────────────────────

@Composable
private fun AnswerCtaButton(
    hasMyAnswer: Boolean,
    isSubmitting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAF8))
            .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) Brush.linearGradient(
                        colors = listOf(Color(0xFF8FD5A6), Color(0xFF7CC8A0))
                    ) else Brush.linearGradient(
                        colors = listOf(Color(0xFFC5DFC8), Color(0xFFB8D8BB))
                    )
                )
                .clickable(enabled = enabled && !isSubmitting) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (hasMyAnswer) "답변 수정하기" else "답변 남기기",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
