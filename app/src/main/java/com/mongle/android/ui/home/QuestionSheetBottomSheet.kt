package com.mongle.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mongle.android.domain.model.Question
import com.mongle.android.ui.common.MongleButton
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MongleHeartRed
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionSheetBottomSheet(
    question: Question,
    hasAnsweredToday: Boolean,
    onDismiss: () -> Unit,
    onAnswerTapped: () -> Unit,
    onWriteQuestionTapped: () -> Unit,
    onSkipTapped: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MongleSpacing.xl)
        ) {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MongleSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "오늘의 질문",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MonglePrimary
                    )
                    Text(
                        text = "무엇을 할까요?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MongleTextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = MongleTextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.md))

            // 질문 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MongleSpacing.md)
                    .background(
                        color = MonglePrimaryLight,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(MongleSpacing.md)
            ) {
                Column {
                    Text(
                        text = "# ${question.category.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MonglePrimary
                    )
                    Spacer(modifier = Modifier.height(MongleSpacing.xs))
                    Text(
                        text = question.content,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MongleTextPrimary
                    )
                    if (hasAnsweredToday) {
                        Spacer(modifier = Modifier.height(MongleSpacing.xs))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MonglePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "답변 완료",
                                style = MaterialTheme.typography.labelSmall,
                                color = MonglePrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.lg))

            // 답변하기 버튼
            MongleButton(
                text = if (hasAnsweredToday) "답변 수정하기" else "답변하기",
                onClick = {
                    onDismiss()
                    onAnswerTapped()
                },
                modifier = Modifier.padding(horizontal = MongleSpacing.md)
            )

            Spacer(modifier = Modifier.height(MongleSpacing.md))
            HorizontalDivider(color = MongleBorder)

            // 나만의 질문 작성하기 행
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onWriteQuestionTapped()
                    }
                    .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MongleTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(MongleSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "나만의 질문 작성하기",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MongleTextPrimary
                    )
                    Text(
                        text = "❤️ 하트 3개 소모",
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleHeartRed
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MongleTextHint,
                    modifier = Modifier.size(16.dp)
                )
            }

            HorizontalDivider(color = MongleBorder)

            // 질문 넘기기 행
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onSkipTapped()
                    }
                    .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MongleTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(MongleSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "질문 넘기기",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MongleTextPrimary
                    )
                    Text(
                        text = "❤️ 하트 3개 소모 · 다른 가족 답변 열람 가능",
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleHeartRed
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MongleTextHint,
                    modifier = Modifier.size(16.dp)
                )
            }

            HorizontalDivider(color = MongleBorder)
        }
    }
}
