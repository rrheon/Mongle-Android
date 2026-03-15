package com.mongle.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mongle.android.ui.theme.MongleGradientEnd
import com.mongle.android.ui.theme.MongleGradientStart
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleSpacing

enum class MongleButtonStyle {
    PRIMARY, SECONDARY
}

@Composable
fun MongleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MongleButtonStyle = MongleButtonStyle.PRIMARY,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(MongleRadius.full)
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(52.dp)

    when (style) {
        MongleButtonStyle.PRIMARY -> {
            val gradient = Brush.linearGradient(
                colors = if (enabled && !isLoading)
                    listOf(Color(0xFF6BBF93), Color(0xFF7BC8A0))
                else
                    listOf(MonglePrimary.copy(alpha = 0.4f), MonglePrimary.copy(alpha = 0.4f))
            )
            Button(
                onClick = onClick,
                modifier = buttonModifier
                    .shadow(
                        elevation = 8.dp,
                        shape = shape,
                        ambientColor = MonglePrimary.copy(alpha = 0.25f),
                        spotColor = MonglePrimary.copy(alpha = 0.25f)
                    )
                    .background(gradient, shape),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                contentPadding = PaddingValues(horizontal = MongleSpacing.lg)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = text, style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }

        MongleButtonStyle.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier
                    .border(1.5.dp, MonglePrimary, shape),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.8f),
                    contentColor = MonglePrimary,
                    disabledContainerColor = Color.White.copy(alpha = 0.4f),
                    disabledContentColor = MonglePrimary.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = MongleSpacing.lg)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MonglePrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = text, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
