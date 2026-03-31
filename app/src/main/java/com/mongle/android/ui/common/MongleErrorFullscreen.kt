package com.mongle.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextSecondary

/**
 * iOS MongleErrorFullscreen과 동일.
 * 첫 로딩 실패 시 전체 화면을 대체하는 에러 뷰.
 */
@Composable
fun MongleErrorFullscreen(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = MongleSpacing.xl)
        ) {
            Icon(
                imageVector = error.icon,
                contentDescription = null,
                tint = MongleTextHint,
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.height(MongleSpacing.lg))

            Text(
                text = error.userMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextSecondary,
                textAlign = TextAlign.Center
            )

            if (error.isRetryable && onRetry != null) {
                Spacer(modifier = Modifier.height(MongleSpacing.lg))
                MongleButton(
                    text = "다시 시도",
                    onClick = onRetry
                )
            }
        }
    }
}
