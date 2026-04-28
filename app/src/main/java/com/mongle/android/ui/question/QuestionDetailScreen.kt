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
import com.mongle.android.ui.common.MongleToastData
import com.mongle.android.ui.common.MongleToastHost
import com.mongle.android.ui.common.MongleToastType
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryGradientEnd
import com.mongle.android.ui.theme.MonglePrimaryGradientStart
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import androidx.compose.ui.res.stringResource
import com.ycompany.Monggle.R
import com.mongle.android.util.AdManager

private data class MoodOption(val id: String, val labelResId: Int, val color: Color)

private val moodOptions = listOf(
    MoodOption("happy",  R.string.mood_happy,  MongleMonggleYellow),
    MoodOption("calm",   R.string.mood_calm,   MongleMonggleGreenLight),
    MoodOption("loved",  R.string.mood_loved,  MongleMongglePink),
    MoodOption("sad",    R.string.mood_sad,    MongleMonggleBlue),
    MoodOption("tired",  R.string.mood_tired,  MongleMonggleOrange)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    question: Question,
    currentUser: User?,
    familyMembers: List<User> = emptyList(),
    currentUserHearts: Int = 0,
    adManager: AdManager? = null,
    onAnswerSubmitted: (Answer, Boolean) -> Unit,
    onClose: () -> Unit,
    viewModel: QuestionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var toastData by remember { mutableStateOf<MongleToastData?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(question, currentUser) {
        viewModel.initialize(question, currentUser, familyMembers, currentUserHearts)
    }
    // onResume 동등 — 화면 재진입 시 서버에서 hearts 재조회 (다중 단말 동기화)
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHearts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            focusManager.clearFocus()
            toastData = MongleToastData(message = it, type = MongleToastType.ERROR)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.detail_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // 답변 제출 중에는 close 처리하지 않는다.
                            // 부모의 path.removeLast() 와 race 가 발생할 수 있음 (iOS MG-56 패리티).
                            if (!uiState.isSubmitting) onClose()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAF8),
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        ) { focusManager.clearFocus() }
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

                    Spacer(modifier = Modifier.height(MongleSpacing.xl))
                }

                // CTA 버튼
                AnswerCtaButton(
                    hasMyAnswer = uiState.hasMyAnswer,
                    isSubmitting = uiState.isSubmitting,
                    enabled = uiState.answerText.trim().isNotEmpty(),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submitAnswer()
                    }
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
                title = stringResource(R.string.detail_mood_required_title),
                description = stringResource(R.string.detail_mood_required_desc),
                primaryLabel = stringResource(R.string.common_confirm),
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
                title = stringResource(R.string.detail_edit_confirm_title),
                description = stringResource(R.string.detail_edit_confirm_desc),
                primaryLabel = stringResource(R.string.detail_edit_btn),
                onPrimary = viewModel::confirmEditAnswer,
                secondaryLabel = stringResource(R.string.common_cancel),
                onSecondary = viewModel::dismissEditConfirmDialog
            )
        }
    }

    // 하트 부족 시 광고 보고 수정하기 팝업
    if (uiState.showEditAdDialog) {
        Dialog(
            onDismissRequest = viewModel::dismissEditAdDialog,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MonglePopup(
                title = stringResource(R.string.detail_edit_ad_title),
                description = stringResource(R.string.detail_edit_ad_desc),
                primaryLabel = stringResource(R.string.detail_edit_ad_btn),
                onPrimary = {
                    adManager?.let { viewModel.watchAdForEdit(it) }
                        ?: viewModel.dismissEditAdDialog()
                },
                secondaryLabel = stringResource(R.string.common_cancel),
                onSecondary = viewModel::dismissEditAdDialog
            )
        }
    }

    MongleToastHost(
        toastData = toastData,
        onDismiss = { toastData = null }
    )
}

// ─── 질문 카드 ───────────────────────────────────────────────────────────────

@Composable
private fun QuestionCard(question: Question) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(MongleSpacing.xl))
            .border(1.dp, MongleBorder, RoundedCornerShape(MongleSpacing.xl))
            .padding(MongleSpacing.lg)
    ) {
        Text(
            text = stringResource(R.string.detail_today_question),
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
                text = stringResource(R.string.detail_today_mongle),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextSecondary
            )
            if (selectedIndex == null) {
                Text(
                    text = stringResource(R.string.detail_select_mood),
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
            text = stringResource(mood.labelResId),
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
            placeholder = stringResource(R.string.detail_answer_placeholder),
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
            text = stringResource(R.string.detail_family_answers, familyAnswers.size),
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
    val moodColor = when (familyAnswer.answer.moodId) {
        "happy" -> MongleMonggleYellow
        "calm" -> MongleMonggleGreenLight
        "loved" -> MongleMongglePink
        "sad" -> MongleMonggleBlue
        "tired" -> MongleMonggleOrange
        else -> MongleMongglePink
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(MongleSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MongleCharacterAvatar(name = familyAnswer.user.name, index = 0, size = 36.dp, color = moodColor)
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
                        colors = listOf(MonglePrimaryGradientStart, MonglePrimaryGradientEnd)
                    ) else Brush.linearGradient(
                        colors = listOf(MonglePrimaryGradientStart.copy(alpha = 0.5f), MonglePrimaryGradientEnd.copy(alpha = 0.5f))
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
                        text = stringResource(if (hasMyAnswer) R.string.detail_edit_submit else R.string.detail_submit),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
