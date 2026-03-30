package com.mongle.android.ui.question

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleButton
import com.mongle.android.ui.common.MongleCard
import com.mongle.android.ui.common.MongleCharacterAvatar
import com.mongle.android.ui.common.MongleTextField
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    question: Question,
    currentUser: User?,
    familyMembers: List<User> = emptyList(),
    onAnswerSubmitted: (Answer) -> Unit,
    onClose: () -> Unit,
    viewModel: QuestionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(question, currentUser) {
        viewModel.initialize(question, currentUser, familyMembers)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuestionDetailEvent.AnswerSubmitted -> onAnswerSubmitted(event.answer)
                QuestionDetailEvent.Closed -> onClose()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "오늘의 질문",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            ) {
                // 스크롤 가능한 컨텐츠 영역
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(MongleSpacing.md)
                ) {
                    // 질문 카드
                    MongleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(MongleSpacing.lg)) {
                            Text(
                                text = "# ${question.category.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(MongleSpacing.xs))
                            Text(
                                text = question.content,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MongleSpacing.lg))

                    // 내 답변 입력 섹션
                    if (!uiState.hasMyAnswer) {
                        Text(
                            text = "내 답변",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(MongleSpacing.sm))
                        MongleTextField(
                            value = uiState.answerText,
                            onValueChange = viewModel::onAnswerTextChanged,
                            placeholder = "오늘의 질문에 답해보세요...",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 6,
                            minLines = 3,
                            isError = uiState.errorMessage != null,
                            errorMessage = uiState.errorMessage
                        )
                    } else {
                        Text(
                            text = "내 답변",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(MongleSpacing.sm))
                        MongleCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(MongleSpacing.lg)
                            ) {
                                Text(
                                    text = uiState.myAnswer!!.content,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    // 가족 답변 섹션
                    if (uiState.familyAnswers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(MongleSpacing.lg))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(MongleSpacing.md))
                        Text(
                            text = "가족의 답변 (${uiState.familyAnswers.size})",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(MongleSpacing.sm))
                        uiState.familyAnswers.forEach { familyAnswer ->
                            FamilyAnswerItem(familyAnswer = familyAnswer)
                            Spacer(modifier = Modifier.height(MongleSpacing.sm))
                        }
                    }
                }

                // 고정 하단 버튼 (답변 미제출 시)
                if (!uiState.hasMyAnswer) {
                    HorizontalDivider()
                    MongleButton(
                        text = "답변 제출",
                        onClick = viewModel::submitAnswer,
                        isLoading = uiState.isSubmitting,
                        enabled = uiState.isValidAnswer,
                        modifier = Modifier
                            .imePadding()
                            .padding(MongleSpacing.md)
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyAnswerItem(familyAnswer: FamilyAnswer) {
    val index = familyAnswer.user.role.ordinal
    MongleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MongleSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MongleCharacterAvatar(
                    name = familyAnswer.user.name,
                    index = index,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(MongleSpacing.sm))
                Column {
                    Text(
                        text = familyAnswer.user.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = familyAnswer.user.role.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(MongleSpacing.sm))
            Text(
                text = familyAnswer.answer.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
