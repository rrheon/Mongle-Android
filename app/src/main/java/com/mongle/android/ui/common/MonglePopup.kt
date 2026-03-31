package com.mongle.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mongle.android.ui.theme.MongleError
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary

// MARK: - MonglePopup
// iOS MonglePopupView와 동일한 구조:
// - 어두운 오버레이 (45% 불투명)
// - 중앙 흰색 카드 (최대 344dp)
// - 타이틀, 설명, 노트 텍스트
// - 기본/파괴적 액션 버튼 + 선택적 보조 버튼
// - 선택적 추가 콘텐츠 슬롯

data class MonglePopupIcon(
    val imageVector: ImageVector,
    val tint: Color,
    val backgroundColor: Color
)

@Composable
fun MonglePopup(
    title: String,
    description: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    icon: MonglePopupIcon? = null,
    note: String? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    isPrimaryEnabled: Boolean = true,
    isDestructive: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 344.dp)
                .padding(horizontal = MongleSpacing.lg)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(MongleRadius.xl))
                .background(Color.White, RoundedCornerShape(MongleRadius.xl))
                .padding(MongleSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MongleSpacing.lg)
        ) {
            // 텍스트 섹션
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
            ) {
                // 아이콘 (선택적)
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(icon.backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon.imageVector,
                            contentDescription = null,
                            tint = icon.tint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MongleTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )

                if (note != null) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MongleTextHint,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 추가 콘텐츠 슬롯
            extraContent?.invoke()

            // 버튼 섹션
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
            ) {
                if (isDestructive) {
                    Button(
                        onClick = onPrimary,
                        enabled = isPrimaryEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(MongleRadius.full),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MongleError,
                            contentColor = Color.White,
                            disabledContainerColor = MongleError.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        contentPadding = PaddingValues(horizontal = MongleSpacing.lg)
                    ) {
                        Text(text = primaryLabel, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    MongleButton(
                        text = primaryLabel,
                        onClick = onPrimary,
                        enabled = isPrimaryEnabled
                    )
                }

                if (secondaryLabel != null && onSecondary != null) {
                    TextButton(
                        onClick = onSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = secondaryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MongleTextHint
                        )
                    }
                }
            }
        }
    }
}
