package com.mongle.android.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.HistoryAnswerSummary
import com.mongle.android.domain.model.HistorySkippedSummary
import com.mongle.android.ui.common.MongleCharacterAvatar
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MonglePrimaryLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import androidx.compose.ui.res.stringResource
import com.ycompany.Monggle.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DAY_NAME_RES_IDS = listOf(
    R.string.history_day_sun, R.string.history_day_mon, R.string.history_day_tue,
    R.string.history_day_wed, R.string.history_day_thu, R.string.history_day_fri,
    R.string.history_day_sat
)

// Mood labels resolved at composition time via moodLabelResources()
private val moodLabelKeys = listOf(R.string.mood_calm, R.string.mood_happy, R.string.mood_loved, R.string.mood_sad, R.string.mood_tired)
private val moodIds = listOf("calm", "happy", "loved", "sad", "tired")
// iOS monggleColor(for:) 순서: [Green, Yellow, Pink, Blue, Orange]
private val moodCharacterColors = listOf(
    MongleMonggleGreenLight, MongleMonggleYellow, MongleMongglePink, MongleMonggleBlue, MongleMonggleOrange
)

private val selectedDateFormatter = java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG)

@Composable
fun HistoryScreen(
    onNavigateToQuestionDetail: (Question) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 화면 진입/탭 전환 시마다 최신 상태 재조회
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.NavigateToQuestionDetail ->
                    onNavigateToQuestionDetail(event.question)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF8))
    ) {
        // ── 헤더: "기록" + 월 네비게이션 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MongleTextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previousMonth, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.history_prev_month),
                        tint = MongleTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = uiState.monthTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleTextPrimary
                )
                IconButton(onClick = viewModel::nextMonth, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.history_next_month),
                        tint = MongleTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MonglePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── 달력 ──
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // 요일 헤더
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DAY_NAME_RES_IDS.forEachIndexed { idx, resId ->
                            Text(
                                text = stringResource(resId),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = when (idx) {
                                    0 -> MaterialTheme.colorScheme.error
                                    6 -> Color(0xFF1565C0)
                                    else -> MongleTextPrimary
                                }
                            )
                        }
                    }

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
                                    hasRecord = date?.let {
                                        val cal = Calendar.getInstance().apply {
                                            time = it
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        uiState.historyItems[cal.timeInMillis] != null
                                    } ?: false,
                                    onDateSelected = { d -> d?.let { viewModel.onDateSelected(it) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ── 최근 14일 기분 ──
                MoodTimelineSection(
                    moodCounts = uiState.moodCounts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // ── 선택된 날짜의 질문 카드 또는 빈 카드 ──
                val selectedItem = uiState.selectedItem
                if (selectedItem != null) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HistoryQuestionCard(
                            item = selectedItem,
                            selectedDate = uiState.selectedDate,
                            onClick = { viewModel.onItemTapped(selectedItem) }
                        )
                        // 내가 답변했거나 넘기기한 경우에만 다른 멤버 답변/넘김 노출
                        if (selectedItem.userAnswered || selectedItem.isSkipped) {
                            selectedItem.memberAnswers.forEach { answer ->
                                FamilyAnswerCard(answer = answer)
                            }
                            // 질문 넘긴 멤버는 답변자와 별개 섹션으로 노출 (캐릭터 색상은 colorId 기반)
                            selectedItem.skippedMembers.forEach { skipped ->
                                FamilySkippedCard(skipped = skipped)
                            }
                        }
                    }
                } else {
                    EmptyDateCard(
                        selectedDate = uiState.selectedDate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 24.dp)
                    )
                }
            }
        }
    }
}

// ── 달력 날짜 셀 ──

@Composable
private fun CalendarDayCell(
    date: Date?,
    currentMonth: Date,
    selectedDate: Date,
    hasRecord: Boolean,
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

    val dayOfWeek = date?.let { Calendar.getInstance().apply { time = it }.get(Calendar.DAY_OF_WEEK) }

    val numColor: Color = when {
        isToday -> Color.White
        !isCurrentMonth -> MongleTextHint.copy(alpha = 0.4f)
        dayOfWeek == Calendar.SUNDAY -> MaterialTheme.colorScheme.error
        dayOfWeek == Calendar.SATURDAY -> Color(0xFF1565C0)
        else -> MongleTextPrimary
    }

    Column(
        modifier = modifier
            .height(54.dp)
            .clickable(enabled = date != null && isCurrentMonth) { onDateSelected(date) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    when {
                        isToday -> MonglePrimary
                        isSelected -> MonglePrimaryLight
                        else -> Color.Transparent
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date?.let {
                    Calendar.getInstance().apply { time = it }.get(Calendar.DAY_OF_MONTH).toString()
                } ?: "",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (hasRecord) FontWeight.Medium else FontWeight.Normal
                ),
                color = numColor
            )
        }
        Box(modifier = Modifier.height(8.dp), contentAlignment = Alignment.Center) {
            if (hasRecord && isCurrentMonth) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MonglePrimary, CircleShape)
                )
            }
        }
    }
}

// ── 최근 14일 기분 섹션 ──

@Composable
private fun MoodTimelineSection(
    moodCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.history_recent_mood),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MongleTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            moodCharacterColors.forEachIndexed { index, color ->
                val count = moodCounts[moodIds[index]] ?: 0
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // iOS: ZStack(alignment: .topTrailing) + badge offset(x: 6, y: -6)
                    Box {
                        MongleCharacterAvatar(
                            name = stringResource(moodLabelKeys[index]),
                            index = index,
                            size = 44.dp,
                            color = color
                        )
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .background(MonglePrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(moodLabelKeys[index]),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MongleTextSecondary
                    )
                }
            }
        }
    }
}

// ── 가족 답변 카드 ──

/**
 * 사용자가 본인 캐릭터에 지정한 colorId 기준으로 캐릭터 색상을 결정한다.
 * (서버 memberAnswerStatuses.colorId)
 * colorId 가 비어 있으면 답변의 moodId 로 폴백.
 */
private fun characterColorFor(colorId: String?, moodId: String?): Pair<Int, Color?> {
    val key = colorId ?: moodId
    val idx = key?.let { moodIds.indexOf(it).takeIf { i -> i >= 0 } }
    return (idx ?: 0) to idx?.let { moodCharacterColors[it] }
}

@Composable
private fun FamilyAnswerCard(answer: HistoryAnswerSummary) {
    val (charIdx, charColor) = characterColorFor(answer.colorId, answer.moodId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, MongleBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        MongleCharacterAvatar(
            name = answer.userName,
            index = charIdx,
            size = 36.dp,
            color = charColor
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = answer.userName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = answer.content,
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextSecondary
            )
        }
    }
}

// ── 질문 넘긴 멤버 카드 ──

@Composable
private fun FamilySkippedCard(skipped: HistorySkippedSummary) {
    val (charIdx, charColor) = characterColorFor(skipped.colorId, null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, MongleBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        MongleCharacterAvatar(
            name = skipped.userName,
            index = charIdx,
            size = 36.dp,
            color = charColor
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = skipped.userName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.history_skipped_label),
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextHint
            )
        }
    }
}

// ── 질문 카드 ──

@Composable
private fun HistoryQuestionCard(
    item: HistoryItem,
    selectedDate: Date,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.5.dp, MonglePrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MonglePrimary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = selectedDateFormatter.format(selectedDate),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MonglePrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.history_answer_count, item.answerCount, item.totalMembers),
                style = MaterialTheme.typography.labelSmall,
                color = MongleTextHint
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.question.content,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextPrimary
        )
    }
}

// ── 빈 날짜 카드 ──

@Composable
private fun EmptyDateCard(selectedDate: Date, modifier: Modifier = Modifier) {
    // 서버 스케줄러가 KST 정오에 새 질문을 배정한다.
    // 해당 날짜에 기록이 없는 경우엔 일괄 "기록 없음" 안내.
    val message = stringResource(R.string.history_no_record)

    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextHint
        )
        Text(
            text = selectedDateFormatter.format(selectedDate),
            style = MaterialTheme.typography.labelSmall,
            color = MongleTextHint.copy(alpha = 0.7f)
        )
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

    repeat(firstDayOfWeek) { days.add(null) }

    repeat(daysInMonth) { i ->
        days.add(Calendar.getInstance().apply {
            time = month
            set(Calendar.DAY_OF_MONTH, i + 1)
        }.time)
    }

    while (days.size < 42) days.add(null)
    return days
}
