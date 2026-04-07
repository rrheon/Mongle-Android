package com.mongle.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mongle.android.R
import com.mongle.android.ui.theme.MongleAccentOrange
import com.mongle.android.ui.theme.MongleError
import com.mongle.android.ui.theme.MongleHeartRed
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextPrimary
import kotlinx.coroutines.delay

// MARK: - Toast Types (iOS ToastType 기준)

enum class MongleToastType {
    // Success
    REFRESH_QUESTION,    // 질문 다시받기
    WRITE_QUESTION,      // 나만의 질문 작성
    NUDGE,               // 재촉하기
    EDIT_ANSWER,         // 답변 수정
    ANSWER_SUBMITTED,    // 답변 작성
    GROUP_LEFT,          // 그룹 나가기
    INVITE_CODE_COPIED,  // 초대 코드 복사
    // Error
    MAX_GROUPS_REACHED,  // 그룹 3개 한도 초과
    ALREADY_MEMBER,      // 이미 속해있는 그룹
    INVALID_INVITE_CODE, // 유효하지 않은 초대코드
    // Generic
    SUCCESS,
    ERROR,
    INFO;

    companion object {
        /** 서버 에러 메시지에서 적합한 ToastType을 추론 */
        fun fromErrorMessage(message: String): MongleToastType = when {
            message.contains("최대") || message.contains("3개") -> MAX_GROUPS_REACHED
            message.contains("이미") && message.contains("속해") -> ALREADY_MEMBER
            message.contains("유효하지") || message.contains("초대코드") || message.contains("찾을 수 없") -> INVALID_INVITE_CODE
            else -> ERROR
        }
    }
}

private val MongleToastType.icon: ImageVector
    get() = when (this) {
        MongleToastType.REFRESH_QUESTION -> Icons.Default.Refresh
        MongleToastType.WRITE_QUESTION -> Icons.Default.CheckCircle
        MongleToastType.NUDGE -> Icons.Default.Favorite
        MongleToastType.EDIT_ANSWER -> Icons.Default.Check
        MongleToastType.ANSWER_SUBMITTED -> Icons.Default.Send
        MongleToastType.GROUP_LEFT -> Icons.Default.CheckCircle
        MongleToastType.INVITE_CODE_COPIED -> Icons.Default.ContentCopy
        MongleToastType.MAX_GROUPS_REACHED -> Icons.Default.Warning
        MongleToastType.ALREADY_MEMBER -> Icons.Default.Warning
        MongleToastType.INVALID_INVITE_CODE -> Icons.Default.Error
        MongleToastType.SUCCESS -> Icons.Default.CheckCircle
        MongleToastType.ERROR -> Icons.Default.Warning
        MongleToastType.INFO -> Icons.Default.Info
    }

private val MongleToastType.iconColor: Color
    get() = when (this) {
        MongleToastType.REFRESH_QUESTION -> MonglePrimary
        MongleToastType.WRITE_QUESTION -> MongleAccentOrange
        MongleToastType.NUDGE -> MongleHeartRed
        MongleToastType.EDIT_ANSWER -> MonglePrimary
        MongleToastType.ANSWER_SUBMITTED -> MonglePrimary
        MongleToastType.GROUP_LEFT -> MonglePrimary
        MongleToastType.INVITE_CODE_COPIED -> MonglePrimary
        MongleToastType.MAX_GROUPS_REACHED -> MongleError
        MongleToastType.ALREADY_MEMBER -> MongleError
        MongleToastType.INVALID_INVITE_CODE -> MongleError
        MongleToastType.SUCCESS -> MonglePrimary
        MongleToastType.ERROR -> MongleError
        MongleToastType.INFO -> Color(0xFF42A5F5)
    }

// MARK: - Default messages (iOS ToastType.message 기준)

val MongleToastType.defaultMessage: String
    get() = when (this) {
        MongleToastType.REFRESH_QUESTION -> "질문을 넘겼어요. 다른 멤버 답변을 확인해보세요!"
        MongleToastType.WRITE_QUESTION -> "나만의 질문을 등록했어요!"
        MongleToastType.NUDGE -> "재촉 메시지를 보냈어요!"
        MongleToastType.EDIT_ANSWER -> "답변을 수정했어요!"
        MongleToastType.ANSWER_SUBMITTED -> "마음을 남겼어요!"
        MongleToastType.GROUP_LEFT -> "그룹에서 나왔어요"
        MongleToastType.INVITE_CODE_COPIED -> "초대 코드가 복사되었습니다"
        MongleToastType.MAX_GROUPS_REACHED -> "그룹은 최대 3개까지 만들 수 있어요"
        MongleToastType.ALREADY_MEMBER -> "이미 속해있는 그룹이에요"
        MongleToastType.INVALID_INVITE_CODE -> "초대코드를 다시 확인해주세요"
        MongleToastType.SUCCESS -> ""
        MongleToastType.ERROR -> ""
        MongleToastType.INFO -> ""
    }

@Composable
fun MongleToastType.localizedMessage(): String = when (this) {
    MongleToastType.REFRESH_QUESTION -> stringResource(R.string.toast_skip)
    MongleToastType.WRITE_QUESTION -> stringResource(R.string.toast_write)
    MongleToastType.NUDGE -> stringResource(R.string.toast_nudge)
    MongleToastType.EDIT_ANSWER -> stringResource(R.string.toast_edit)
    MongleToastType.ANSWER_SUBMITTED -> stringResource(R.string.toast_answer)
    MongleToastType.GROUP_LEFT -> stringResource(R.string.toast_group_left)
    MongleToastType.INVITE_CODE_COPIED -> stringResource(R.string.toast_code_copied)
    MongleToastType.MAX_GROUPS_REACHED -> stringResource(R.string.toast_max_groups)
    MongleToastType.ALREADY_MEMBER -> stringResource(R.string.toast_already_member)
    MongleToastType.INVALID_INVITE_CODE -> stringResource(R.string.toast_invalid_code)
    MongleToastType.SUCCESS -> ""
    MongleToastType.ERROR -> ""
    MongleToastType.INFO -> ""
}

// MARK: - MongleToast Component
// iOS MongleToastView와 동일: 흰 배경 캡슐, 컬러 아이콘 + 어두운 텍스트

@Composable
fun MongleToast(
    message: String,
    type: MongleToastType = MongleToastType.ERROR,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(50.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = type.iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
        }
    }
}

@Composable
fun MongleToastOverlay(
    message: String?,
    type: MongleToastType = MongleToastType.ERROR,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 3초 후 자동 dismiss
    LaunchedEffect(message) {
        if (message != null && onDismiss != null) {
            delay(3000L)
            onDismiss()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .padding(horizontal = MongleSpacing.md)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
        ) {
            if (message != null) {
                MongleToast(message = message, type = type)
            }
        }
    }
}

// MARK: - Auto-dismiss Toast Host (iOS mongleErrorToast modifier 대응)
// 화면 하단에 토스트를 표시하고 3초 후 자동으로 사라짐

data class MongleToastData(
    val message: String,
    val type: MongleToastType = MongleToastType.ERROR
)

/**
 * 화면 최상위에 배치하면 토스트를 자동으로 표시/dismiss하는 오버레이.
 * iOS mongleErrorToast modifier와 동일한 동작.
 */
@Composable
fun MongleToastHost(
    toastData: MongleToastData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 3초 후 자동 dismiss
    LaunchedEffect(toastData) {
        if (toastData != null) {
            delay(3000L)
            onDismiss()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toastData != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .padding(horizontal = MongleSpacing.md)
                .padding(bottom = MongleSpacing.lg)
                .navigationBarsPadding()
        ) {
            if (toastData != null) {
                MongleToast(
                    message = toastData.message,
                    type = toastData.type
                )
            }
        }
    }
}
