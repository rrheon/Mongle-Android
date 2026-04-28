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
import com.ycompany.Monggle.R
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
    allFamilies: List<com.mongle.android.domain.model.MongleGroup> = emptyList(),
    currentFamilyId: java.util.UUID? = null,
    onNotificationTap: (AppNotification) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var toastData by remember { mutableStateOf<MongleToastData?>(null) }

    BackHandler { onBack() }

    // 화면 진입/복귀 시 최신 알림 재로딩 (FCM 푸시 수신 후 즉시 반영).
    // currentFamilyId 를 ViewModel scope 로 설정 → 이후 markAllAsRead/deleteAll 이
    // 자동으로 해당 그룹 스코프만 처리한다.
    LaunchedEffect(currentFamilyId) {
        viewModel.loadNotifications(currentFamilyId?.toString())
    }

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
                        TextButton(onClick = { viewModel.onDeleteAll(currentFamilyId?.toString()) }) {
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
                // 서버가 이미 group_id 로 필터링해서 내려주지만, 안전망으로 클라도 필터.
                // 각 그룹 화면에서는 해당 그룹 알림만 노출 — 그룹 무관(familyId==null) 알림도 제외.
                val filtered = if (currentFamilyId != null) {
                    uiState.notifications.filter { it.familyId == currentFamilyId.toString() }
                } else {
                    uiState.notifications
                }

                if (filtered.isEmpty()) {
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
                        }
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadNotifications(currentFamilyId?.toString()) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (currentFamilyId == null && allFamilies.isNotEmpty()) {
                                // 그룹별 섹션으로 표시
                                val grouped = filtered.groupBy { it.familyId }
                                for (family in allFamilies) {
                                    val familyNotifs = grouped[family.id.toString()] ?: continue
                                    if (familyNotifs.isEmpty()) continue
                                    // 섹션 헤더
                                    item(key = "header_${family.id}") {
                                        Text(
                                            text = family.name,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MongleTextSecondary,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF2F2F2))
                                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                        )
                                    }
                                    items(familyNotifs, key = { it.id }) { notification ->
                                        NotificationSwipeItem(notification, viewModel, onNotificationTap)
                                    }
                                }
                                // familyId가 null인 알림
                                val ungrouped = grouped[null]
                                if (!ungrouped.isNullOrEmpty()) {
                                    items(ungrouped, key = { it.id }) { notification ->
                                        NotificationSwipeItem(notification, viewModel, onNotificationTap)
                                    }
                                }
                            } else {
                                // 단일 그룹 또는 그룹 정보 없을 때 플랫 리스트
                                items(filtered, key = { it.id }) { notification ->
                                    NotificationSwipeItem(notification, viewModel, onNotificationTap)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSwipeItem(
    notification: AppNotification,
    viewModel: NotificationViewModel,
    onNotificationTap: (AppNotification) -> Unit
) {
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
                onClick = {
                    viewModel.onMarkAsRead(notification.id)
                    onNotificationTap(notification)
                }
            )
        }
    )
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        color = Color(0xFFE0E0E0)
    )
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
    val size = 44.dp
    when (type.lowercase()) {
        "member_answered" -> {
            // 몽글 캐릭터 — 답변자의 colorId(moodId)에 따른 색상
            val characterColor = moodColorForNotification(colorId)
            val eyeSize = size * 0.18f
            val eyeHOffset = size * 0.14f
            val eyeVOffset = size * 0.07f
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(characterColor),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(eyeSize + 2.dp)
                        .offset(x = -eyeHOffset, y = eyeVOffset)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(eyeSize).background(Color.Black, CircleShape)) }
                Box(
                    modifier = Modifier
                        .size(eyeSize + 2.dp)
                        .offset(x = eyeHOffset, y = eyeVOffset)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(eyeSize).background(Color.Black, CircleShape)) }
            }
        }
        else -> {
            val (bg, icon, tint) = notificationTypeStyle(type)
            Box(
                modifier = Modifier
                    .size(size)
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

// iOS NotificationCard 아이콘 스타일 기준
private fun notificationTypeStyle(type: String): NotifStyle = when (type.lowercase()) {
    "new_question" -> NotifStyle(Color(0xFFE8F2FD), Icons.Default.QuestionMark, Color(0xFF42A5F5))
    "all_answered" -> NotifStyle(Color(0xFFE8F6EA), Icons.Default.CheckCircle, Color(0xFF4CAF50))
    "answer_request" -> NotifStyle(Color(0xFFFFF1E2), Icons.Default.Campaign, Color(0xFFFF9800))
    "badge_earned" -> NotifStyle(Color(0xFFFDDDD8), Icons.Default.CardGiftcard, Color(0xFFFF8A80))
    else -> NotifStyle(Color(0xFFF5F5F5), Icons.Default.QuestionMark, Color(0xFF9E9E9E))
}

// 서버가 보내는 createdAt은 UTC 기준 ISO 8601 문자열.
// 'Z'는 SimpleDateFormat에서 리터럴로 취급되므로 반드시 TimeZone을 UTC로 명시해야
// KST(+9) 환경에서 9시간 어긋난 값을 피할 수 있다.
private val isoFormatZ = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
    timeZone = java.util.TimeZone.getTimeZone("UTC")
}
private val isoFormatMillisZ = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
    timeZone = java.util.TimeZone.getTimeZone("UTC")
}
// 타임존 표기가 없는 경우도 UTC로 해석 (서버가 항상 UTC를 내려준다고 가정)
private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
    timeZone = java.util.TimeZone.getTimeZone("UTC")
}

private fun timeAgo(createdAt: String, res: Resources): String {
    val date = runCatching { isoFormatMillisZ.parse(createdAt) }.getOrNull()
        ?: runCatching { isoFormatZ.parse(createdAt) }.getOrNull()
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
        else -> {
            // iOS MG-38 패리티 — 7일 이상 fallback 도 로케일 분기
            val locale = Locale.getDefault()
            val pattern = when (locale.language) {
                "ko" -> "M월 d일"
                "ja" -> "M月d日"
                else -> "M/d"
            }
            SimpleDateFormat(pattern, locale).format(date)
        }
    }
}
