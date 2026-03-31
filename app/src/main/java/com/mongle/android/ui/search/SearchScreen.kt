package com.mongle.android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.HistoryAnswerSummary
import com.mongle.android.ui.common.MongleCharacter
import com.mongle.android.ui.common.MongleCard
import com.mongle.android.ui.theme.MongleMonggleBlue
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleMonggleOrange
import com.mongle.android.ui.theme.MongleMongglePink
import com.mongle.android.ui.theme.MongleMonggleYellow
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import java.util.Calendar
import java.util.Date

private val SearchBg = Color(0xFFF8FAF8)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // 탭 전환 시 키보드 자동 포커스
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBg)
            // 검색창 외부 탭 시 키보드 내리기
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // ── 검색 헤더 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .shadow(elevation = 1.dp)
                .statusBarsPadding()
                .padding(horizontal = MongleSpacing.md, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F0F0))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 몽글 아이콘 (검색바)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(MongleMonggleGreenLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val eyeSize = 3.dp
                    val eyeOffset = 2.5.dp
                    Box(
                        modifier = Modifier
                            .size(eyeSize + 1.dp)
                            .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(eyeSize).background(Color.Black, CircleShape))
                    }
                    Box(
                        modifier = Modifier
                            .size(eyeSize + 1.dp)
                            .offset(x = eyeOffset, y = -eyeSize * 0.3f)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(eyeSize).background(Color.Black, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MongleTextPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MonglePrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    decorationBox = { innerTextField ->
                        if (uiState.query.isEmpty()) {
                            Text(
                                text = "답변이나 질문 검색",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MongleTextHint
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // ── 본문 ──
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MonglePrimary)
                }
            }

            uiState.showMinLengthHint -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "2글자 이상 입력해 주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextHint
                    )
                }
            }

            uiState.query.trim().length >= 2 && uiState.results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔍", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"${uiState.query.trim()}\"에 맞는 기록이 없어요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MongleTextHint
                        )
                    }
                }
            }

            uiState.results.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MongleSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(MongleSpacing.md))
                        Text(
                            text = "${uiState.resultCount}개의 기록을 찾았어요",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MongleTextHint
                        )
                        Spacer(modifier = Modifier.height(MongleSpacing.md))
                    }

                    // 날짜별 그룹핑
                    val grouped = uiState.results.groupBy { it.date.toDateKey() }
                    grouped.entries.sortedByDescending { it.key }.forEach { (_, items) ->
                        item {
                            DateGroupHeader(date = items.first().date)
                            Spacer(modifier = Modifier.height(MongleSpacing.sm))
                        }
                        items(items, key = { it.dailyQuestionId }) { result ->
                            SearchResultCard(
                                result = result,
                                query = uiState.query.trim()
                            )
                            Spacer(modifier = Modifier.height(MongleSpacing.sm))
                        }
                        item { Spacer(modifier = Modifier.height(MongleSpacing.xs)) }
                    }
                    item { Spacer(modifier = Modifier.height(MongleSpacing.xl)) }
                }
            }

            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🌿", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "가족의 소중한 기록을 검색해보세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MongleTextHint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateGroupHeader(date: Date) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MongleTextHint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = date.toDisplayLabel(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MongleTextHint
        )
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResultItem,
    query: String
) {
    MongleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(MongleSpacing.md)) {
            // 질문 텍스트 (검색어 하이라이팅)
            Text(
                text = buildHighlightedText(result.questionContent, query),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )

            if (result.matchedAnswers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MongleSpacing.sm))
                result.matchedAnswers.take(3).forEachIndexed { index, answer ->
                    AnswerRow(
                        answer = answer,
                        query = query
                    )
                    if (index < (result.matchedAnswers.take(3).size - 1)) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerRow(
    answer: HistoryAnswerSummary,
    query: String
) {
    val moodColor = moodColorForSearch(answer.moodId)
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 몽글 캐릭터 아바타 (답변 시 선택한 감정 색상)
        MiniMongleAvatar(bodyColor = moodColor)

        Column(modifier = Modifier.weight(1f)) {
            // 답변 내용 (검색어 하이라이팅)
            Text(
                text = buildHighlightedText(answer.content, query),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MongleTextSecondary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            // 멤버 이름 뱃지
            MemberBadge(name = answer.userName, bgColor = moodColor.copy(alpha = 0.15f))
        }
    }
}

private fun moodColorForSearch(moodId: String?): Color = when (moodId) {
    "happy" -> MongleMonggleYellow
    "calm"  -> MongleMonggleGreenLight
    "loved" -> MongleMongglePink
    "sad"   -> MongleMonggleBlue
    "tired" -> MongleMonggleOrange
    else    -> MongleMonggleGreenLight
}

@Composable
private fun MiniMongleAvatar(bodyColor: Color) {
    val eyeSize = 36.dp * 0.18f
    val eyeOffset = 36.dp * 0.14f

    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(4.dp, CircleShape, ambientColor = bodyColor.copy(0.3f), spotColor = bodyColor.copy(0.3f))
            .background(bodyColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(eyeSize + 2.dp)
                .offset(x = -eyeOffset, y = -eyeSize * 0.3f)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(eyeSize).background(Color.Black, CircleShape))
        }
        Box(
            modifier = Modifier
                .size(eyeSize + 2.dp)
                .offset(x = eyeOffset, y = -eyeSize * 0.3f)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(eyeSize).background(Color.Black, CircleShape))
        }
    }
}

@Composable
private fun MemberBadge(name: String, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MongleTextSecondary
        )
    }
}

/** 검색어 부분을 MonglePrimary 색상 + Bold로 하이라이팅 */
private fun buildHighlightedText(text: String, query: String) = buildAnnotatedString {
    if (query.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var start = 0
    while (start < text.length) {
        val idx = lowerText.indexOf(lowerQuery, start)
        if (idx == -1) {
            append(text.substring(start))
            break
        }
        if (idx > start) append(text.substring(start, idx))
        withStyle(SpanStyle(color = Color(0xFF56A96B), fontWeight = FontWeight.Bold)) {
            append(text.substring(idx, idx + query.length))
        }
        start = idx + query.length
    }
}

private fun Date.toDateKey(): Long {
    val cal = Calendar.getInstance().apply {
        time = this@toDateKey
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
