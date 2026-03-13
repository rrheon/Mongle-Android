package com.mongle.android.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Question
import com.mongle.android.ui.common.MongleCard
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import java.util.Calendar
import java.util.Date

private val DAY_NAMES = listOf("일", "월", "화", "수", "목", "금", "토")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToQuestionDetail: (Question) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.NavigateToQuestionDetail ->
                    onNavigateToQuestionDetail(event.question)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "히스토리",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(MongleSpacing.md)
            ) {
                // 달력 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "이전 달")
                    }
                    Text(
                        text = uiState.monthTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "다음 달")
                    }
                }

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                // 요일 헤더
                Row(modifier = Modifier.fillMaxWidth()) {
                    DAY_NAMES.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = when (day) {
                                "일" -> MaterialTheme.colorScheme.error
                                "토" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MongleSpacing.xs))

                // 달력 그리드
                val calendarDays = generateCalendarDays(uiState.currentMonth)
                val weeks = calendarDays.chunked(7)
                weeks.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            CalendarDayCell(
                                date = date,
                                currentMonth = uiState.currentMonth,
                                selectedDate = uiState.selectedDate,
                                historyItem = date?.let {
                                    val cal = Calendar.getInstance().apply {
                                        time = it
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    uiState.historyItems[cal.timeInMillis]
                                },
                                onDateSelected = { d -> d?.let { viewModel.onDateSelected(it) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 선택된 날짜의 히스토리 아이템
                uiState.selectedItem?.let { item ->
                    Spacer(modifier = Modifier.height(MongleSpacing.lg))
                    MongleCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.onItemTapped(item) }
                    ) {
                        Column(modifier = Modifier.padding(MongleSpacing.md)) {
                            Text(
                                text = "# ${item.question.category.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.question.content,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(MongleSpacing.sm))
                            Text(
                                text = "${item.answerCount}/${item.totalMembers}명 답변",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: Date?,
    currentMonth: Date,
    selectedDate: Date,
    historyItem: HistoryItem?,
    onDateSelected: (Date?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentMonth = date?.let {
        val dateCal = Calendar.getInstance().apply { time = it }
        val monthCal = Calendar.getInstance().apply { time = currentMonth }
        dateCal.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH) &&
            dateCal.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR)
    } ?: false

    val isSelected = date?.let {
        val dateCal = Calendar.getInstance().apply { time = it }
        val selCal = Calendar.getInstance().apply { time = selectedDate }
        dateCal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR) &&
            dateCal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR)
    } ?: false

    val isToday = date?.let {
        val dateCal = Calendar.getInstance().apply { time = it }
        val todayCal = Calendar.getInstance()
        dateCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
            dateCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
    } ?: false

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = date != null) { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date?.let {
                    Calendar.getInstance().apply { time = it }.get(Calendar.DAY_OF_MONTH).toString()
                } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (historyItem != null) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MonglePrimary
                        )
                )
            }
        }
    }
}

private fun generateCalendarDays(month: Date): List<Date?> {
    val cal = Calendar.getInstance().apply {
        time = month
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=일
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = mutableListOf<Date?>()

    // 이전 달 빈 칸
    repeat(firstDayOfWeek) { days.add(null) }

    // 이번 달 날짜
    repeat(daysInMonth) { i ->
        days.add(Calendar.getInstance().apply {
            time = month
            set(Calendar.DAY_OF_MONTH, i + 1)
        }.time)
    }

    // 다음 달 빈 칸으로 6주 채우기
    while (days.size < 42) days.add(null)
    return days
}
