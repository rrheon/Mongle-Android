package com.mongle.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mongle.android.domain.model.Question
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionSheetBottomSheet(
    question: Question,
    hasAnswered: Boolean,
    onDismiss: () -> Unit,
    onAnswerTap: () -> Unit,
    onWriteQuestionTap: () -> Unit,
    onSkipTapped: () -> Unit = {}
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
                .padding(bottom = 32.dp)
        ) {
            // ── 헤더: 제목 + X 버튼 ──
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

            // ── 질문 카드 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MongleSpacing.md)
                    .border(
                        width = 1.5.dp,
                        color = MonglePrimary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        color = MonglePrimaryLight.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(MongleSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "오늘의 질문",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MonglePrimary
                    )
                    if (hasAnswered) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MonglePrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "답변 완료",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MonglePrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MongleSpacing.xs))
                Text(
                    text = question.content,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MongleTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(MongleSpacing.md))

            // ── 답변하기 버튼 (그라디언트) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MongleSpacing.md)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8FD5A6), Color(0xFFA5E0BD))
                        )
                    )
                    .clickable { onAnswerTap() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasAnswered) "답변 수정하기" else "답변하기",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(MongleSpacing.md))

            HorizontalDivider(color = Color(0xFFEEEEEE))

            Spacer(modifier = Modifier.height(MongleSpacing.xs))

            // ── 나만의 질문 작성하기 ──
            QuestionSheetActionRow(
                icon = Icons.Default.Edit,
                title = "나만의 질문 작성하기",
                subtitle = "하트 3개 소모",
                onClick = onWriteQuestionTap
            )

            Spacer(modifier = Modifier.height(MongleSpacing.xs))

            // ── 질문 넘기기 ──
            QuestionSheetActionRow(
                icon = Icons.Default.Forward,
                title = "질문 넘기기",
                subtitle = "하트 3개 소모 · 다른 가족 답변 열람 가능",
                onClick = onSkipTapped
            )
        }
    }
}

@Composable
private fun QuestionSheetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MongleSpacing.md)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(MongleRadius.medium))
            .clip(RoundedCornerShape(MongleRadius.medium))
            .background(color = MonglePrimaryLight.copy(alpha = 0.3f))
            .border(width = 1.dp, color = MongleBorder, shape = RoundedCornerShape(MongleRadius.medium))
            .clickable { onClick() }
            .padding(vertical = MongleSpacing.sm, horizontal = MongleSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MonglePrimary,
            modifier = Modifier
                .size(22.dp)
                .width(36.dp)
        )
        Spacer(modifier = Modifier.width(MongleSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextHint
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MongleTextHint,
            modifier = Modifier.size(16.dp)
        )
    }
}
