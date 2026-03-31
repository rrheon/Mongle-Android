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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mongle.android.domain.model.Question
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "오늘의 질문",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MonglePrimary
                    )
                    if (hasAnswered) {
                        Spacer(modifier = Modifier.width(MongleSpacing.xs))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MonglePrimary,
                            modifier = Modifier.size(12.dp)
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
                            colors = listOf(Color(0xFF6BBF93), Color(0xFF7BC8A0))
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
                enabled = true,
                onClick = onWriteQuestionTap
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MongleSpacing.md),
                color = Color(0xFFEEEEEE)
            )

            // ── 질문 넘기기 ──
            QuestionSheetActionRow(
                icon = Icons.Default.Forward,
                title = "질문 넘기기",
                subtitle = "하트 3개 소모 · 다른 가족 답변 열람 가능",
                enabled = true,
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
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (enabled) MonglePrimaryLight else Color(0xFFF0F0F0),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MonglePrimary else MongleTextHint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(MongleSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary.copy(alpha = contentAlpha)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextSecondary.copy(alpha = contentAlpha)
            )
        }
    }
}
