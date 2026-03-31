package com.mongle.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mongle.android.ui.theme.MonglePrimary

enum class MongleToastType { SUCCESS, ERROR, INFO }

@Composable
fun MongleToast(
    message: String,
    type: MongleToastType = MongleToastType.ERROR,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (type) {
        MongleToastType.SUCCESS -> MonglePrimary
        MongleToastType.ERROR -> Color(0xFFFF6B6B)
        MongleToastType.INFO -> Color(0xFF5C8FBE)
    }
    val icon: ImageVector = when (type) {
        MongleToastType.SUCCESS -> Icons.Default.Info
        MongleToastType.ERROR -> Icons.Default.Warning
        MongleToastType.INFO -> Icons.Default.Info
    }

    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun MongleToastOverlay(
    message: String?,
    type: MongleToastType = MongleToastType.ERROR,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        if (message != null) {
            MongleToast(message = message, type = type)
        }
    }
}
