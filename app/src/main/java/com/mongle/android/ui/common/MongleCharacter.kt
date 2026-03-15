package com.mongle.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mongle.android.domain.model.User
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow

private val characterColors = listOf(
    MongleMonggleGreenLight,
    MongleMonggleYellow,
    MongleMonggleBlue,
    MongleMongglePink,
    MongleMonggleOrange
)

@Composable
fun MongleCharacter(
    user: User,
    index: Int,
    size: Dp = 56.dp,
    hasAnswered: Boolean = false,
    showName: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bodyColor = characterColors[index % characterColors.size]
    val eyeSize = size * 0.18f
    val eyeOffset = size * 0.14f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = bodyColor.copy(alpha = 0.3f),
                    spotColor = bodyColor.copy(alpha = 0.3f)
                )
                .background(bodyColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // 왼쪽 눈
            Box(
                modifier = Modifier
                    .size(eyeSize)
                    .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
                    .background(Color.Black, CircleShape)
            )
            // 오른쪽 눈
            Box(
                modifier = Modifier
                    .size(eyeSize)
                    .offset(x = eyeOffset, y = -eyeSize * 0.3f)
                    .background(Color.Black, CircleShape)
            )

            // 답변 완료 시 초록 뺨 표시
            if (hasAnswered) {
                Box(
                    modifier = Modifier
                        .size(eyeSize * 0.9f)
                        .offset(x = -eyeOffset * 1.1f, y = eyeSize * 0.8f)
                        .background(Color(0xFF66BB6A).copy(alpha = 0.6f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(eyeSize * 0.9f)
                        .offset(x = eyeOffset * 1.1f, y = eyeSize * 0.8f)
                        .background(Color(0xFF66BB6A).copy(alpha = 0.6f), CircleShape)
                )
            }
        }

        if (showName) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.role.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MongleCharacterAvatar(
    name: String,
    index: Int,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val bodyColor = characterColors[index % characterColors.size]
    val eyeSize = size * 0.18f
    val eyeOffset = size * 0.14f

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = bodyColor.copy(alpha = 0.2f),
                spotColor = bodyColor.copy(alpha = 0.2f)
            )
            .background(bodyColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(eyeSize)
                .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
                .background(Color.Black, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(eyeSize)
                .offset(x = eyeOffset, y = -eyeSize * 0.3f)
                .background(Color.Black, CircleShape)
        )
    }
}
