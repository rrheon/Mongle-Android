package com.mongle.android.ui.notification

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import com.mongle.android.ui.common.MongleToastData
import com.mongle.android.ui.common.MongleToastHost
import com.mongle.android.ui.common.MongleToastType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.R
import com.mongle.android.data.remote.AppNotification
import com.mongle.android.ui.theme.MongleAccentCoralLight
import com.mongle.android.ui.theme.MongleInfo
import com.mongle.android.ui.theme.MongleMoodHappy
import com.mongle.android.ui.theme.MongleMoodHappyLight
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow
import com.mongle.android.ui.theme.MongleMoodLovedLight
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleSuccessLight
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import com.mongle.android.ui.theme.MongleWarning
import android.content.res.Resources
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var toastData by remember { mutableStateOf<MongleToastData?>(null) }

    BackHandler { onBack() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toastData = MongleToastData(message = it, type = MongleToastType.ERROR)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notif_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState.notifications.isNotEmpty()) {
                        TextButton(onClick = viewModel::onMarkAllAsRead) {
                            Text(
                                text = stringResource(R.string.notif_mark_all_read),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MongleTextSecondary
                            )
                        }
                        TextButton(onClick = viewModel::onDeleteAll) {
                            Text(
                                text = stringResource(R.string.notif_delete_all),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAF8),
        snackbarHost = { MongleToastHost(toastData = toastData, onDismiss = { toastData = null }) }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MonglePrimary)
                        Spacer(modifier = Modifier.height(MongleSpacing.sm))
                        Text(
                            text = stringResource(R.string.notif_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MongleTextSecondary
                        )
                    }
                }
            }
            uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MongleTextHint
                        )
                        Spacer(modifier = Modifier.height(MongleSpacing.sm))
                        Text(
                            text = stringResource(R.string.notif_empty_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MongleTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.notif_empty_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MongleTextSecondary
                        )
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.loadNotifications() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.notifications, key = { it.id }) { notification ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.onDeleteNotification(notification.id)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                            MaterialTheme.colorScheme.error
                                        else Color.Transparent,
                                        label = "swipe_bg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color)
                                            .padding(end = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = stringResource(R.string.common_delete),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                },
                                content = {
                                    NotificationCard(
                                        notification = notification,
                                        onClick = { viewModel.onMarkAsRead(notification.id) }
                                    )
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp),
                                color = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 타입별 아이콘 (iOS 동일)
        NotificationTypeIcon(type = notification.type, colorId = notification.colorId)

        Spacer(modifier = Modifier.width(12.dp))

        // 내용
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
                ),
                color = MongleTextPrimary,
                maxLines = 1
            )
            Text(
                text = timeAgo(notification.createdAt, androidx.compose.ui.platform.LocalContext.current.resources),
                style = MaterialTheme.typography.labelSmall,
                color = MongleTextHint
            )
        }

        // 안 읽음 점
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MonglePrimary)
            )
        }
    }
}

// colorId(moodId)에 따른 몽글 캐릭터 색상
private fun moodColorForNotification(colorId: String?): Color = when (colorId) {
    "happy" -> MongleMonggleYellow
    "calm" -> MongleMonggleGreenLight
    "loved" -> MongleMongglePink
    "sad" -> MongleMonggleBlue
    "tired" -> MongleMonggleOrange
    else -> MongleMoodHappy // 기본값
}

// iOS 기준: 타입별 아이콘 + 배경색 분리
@Composable
private fun NotificationTypeIcon(type: String, colorId: String? = null) {
    when (type) {
        "member_answered" -> {
            // 몽글 캐릭터 — 답변자의 colorId(moodId)에 따른 색상
            val characterColor = moodColorForNotification(colorId)
            val eyeSize = 44.dp * 0.18f
            val eyeOffset = 44.dp * 0.14f
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(characterColor),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(eyeSize + 2.dp)
                        .offset(x = -eyeOffset, y = -(eyeSize * 0.3f))
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(eyeSize).background(Color.Black, CircleShape)) }
                Box(
                    modifier = Modifier
                        .size(eyeSize + 2.dp)
                        .offset(x = eyeOffset, y = -(eyeSize * 0.3f))
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(eyeSize).background(Color.Black, CircleShape)) }
            }
        }
        else -> {
            val (bg, icon, tint) = notificationTypeStyle(type)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class NotifStyle(val bg: Color, val icon: ImageVector, val tint: Color)

private fun notificationTypeStyle(type: String): NotifStyle = when (type) {
    "new_question" -> NotifStyle(Color(0xFFE3F2FD), Icons.Default.QuestionMark, MongleInfo)
    "all_answered" -> NotifStyle(Color(0xFFEDF7F0), Icons.Default.CheckCircle, MongleSuccessLight)
    "answer_request" -> NotifStyle(Color(0xFFFFF3E0), Icons.Default.Campaign, MongleWarning)
    "badge_earned" -> NotifStyle(MongleMoodLovedLight, Icons.Default.CardGiftcard, MongleAccentCoralLight)
    else -> NotifStyle(Color(0xFFF5F5F5), Icons.Default.QuestionMark, Color(0xFF9E9E9E))
}

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
private val isoFormatZ = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

private fun timeAgo(createdAt: String, res: Resources): String {
    val date = runCatching { isoFormatZ.parse(createdAt) }.getOrNull()
        ?: runCatching { isoFormat.parse(createdAt) }.getOrNull()
        ?: return ""
    val diffMs = Date().time - date.time
    val diffMin = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        diffMin < 1 -> res.getString(R.string.notif_time_now)
        diffMin < 60 -> res.getString(R.string.notif_time_min, diffMin.toInt())
        diffHours < 24 -> res.getString(R.string.notif_time_hour, diffHours.toInt())
        diffDays < 7 -> res.getString(R.string.notif_time_day, diffDays.toInt())
        else -> SimpleDateFormat("M/d", Locale.getDefault()).format(date)
    }
}
