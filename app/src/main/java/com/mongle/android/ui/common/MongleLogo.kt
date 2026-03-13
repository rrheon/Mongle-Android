package com.mongle.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextOnPrimary

enum class MongleLogoSize {
    SMALL, MEDIUM, LARGE
}

@Composable
fun MongleLogo(
    size: MongleLogoSize = MongleLogoSize.MEDIUM,
    modifier: Modifier = Modifier
) {
    val (circleSize, fontSize, appNameSize) = when (size) {
        MongleLogoSize.SMALL -> Triple(48.dp, 20.sp, 14.sp)
        MongleLogoSize.MEDIUM -> Triple(72.dp, 28.sp, 18.sp)
        MongleLogoSize.LARGE -> Triple(96.dp, 36.sp, 22.sp)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 고슴도치 캐릭터 아이콘 (실제 앱에서는 이미지로 교체)
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🦔",
                fontSize = fontSize
            )
        }
        Spacer(modifier = Modifier.height(MongleSpacing.xs))
        Text(
            text = "몽글",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = appNameSize,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
