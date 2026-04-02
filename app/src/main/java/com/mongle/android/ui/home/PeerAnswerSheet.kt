package com.mongle.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleCharacter
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import androidx.compose.ui.res.stringResource
import com.mongle.android.R
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerAnswerSheet(
    member: User,
    memberIndex: Int,
    questionText: String,
    answer: Answer,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8F8F8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = MongleSpacing.xl)
        ) {
            // 헤더 (닫기 버튼)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MongleSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MongleTextHint
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MongleSpacing.md),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(MongleSpacing.md)
            ) {
                // 오늘의 질문 카드
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MonglePrimary, RoundedCornerShape(16.dp))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(MongleSpacing.md)
                ) {
                    Text(
                        text = stringResource(R.string.home_today_question),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MonglePrimary
                    )
                    Spacer(modifier = Modifier.height(MongleSpacing.xs))
                    Text(
                        text = questionText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MongleTextPrimary
                    )
                }

                // 멤버 답변 카드
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(MongleSpacing.md)
                ) {
                    // 멤버 정보 행
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MongleCharacter(
                            user = member,
                            index = memberIndex,
                            size = 36.dp,
                            showName = false
                        )
                        Spacer(modifier = Modifier.width(MongleSpacing.sm))
                        Column {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MongleTextPrimary
                            )
                            val timeStr = runCatching {
                                SimpleDateFormat("MM/dd HH:mm", Locale.KOREAN).format(answer.createdAt)
                            }.getOrElse { "" }
                            if (timeStr.isNotEmpty()) {
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MongleTextHint
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MongleSpacing.sm))

                    // 답변 내용
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MonglePrimaryLight.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(MongleSpacing.md)
                    ) {
                        Text(
                            text = answer.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MongleTextSecondary
                        )
                    }
                }
            }
        }
    }
}
