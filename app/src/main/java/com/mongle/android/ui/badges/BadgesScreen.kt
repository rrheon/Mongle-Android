package com.mongle.android.ui.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ycompany.Monggle.R
import com.mongle.android.domain.model.BadgeCategory
import com.mongle.android.domain.model.BadgeDisplayItem
import com.mongle.android.ui.theme.MongleMonggleGreenLight
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BadgesScreen(
    onBack: () -> Unit,
    viewModel: BadgesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MongleTextPrimary
                )
            }
            Text(
                text = stringResource(R.string.badges_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MongleTextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        when {
            state.loading && state.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MongleMonggleGreenLight)
                }
            }
            state.items.isEmpty() && state.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.badges_load_error),
                        color = MongleTextSecondary
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.md)
                ) {
                    val ownedCount = state.items.count { it.isOwned }
                    Text(
                        text = stringResource(R.string.badges_summary, ownedCount, state.items.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextSecondary,
                        modifier = Modifier.padding(bottom = MongleSpacing.md)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(MongleSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(MongleSpacing.md),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items, key = { it.definition.code }) { item ->
                            BadgeCell(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCell(item: BadgeDisplayItem) {
    val owned = item.isOwned
    val nameRes = badgeNameRes(item.definition.code)
    val condRes = badgeConditionRes(item.definition.code)
    val ctx = LocalContext.current
    val name = if (nameRes != 0) ctx.getString(nameRes) else item.definition.code
    val condition = if (condRes != 0) ctx.getString(condRes) else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(vertical = MongleSpacing.md, horizontal = MongleSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (owned) categoryColor(item.definition.category)
                    else Color(0xFFE0E0E0),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (owned) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (owned) MongleTextPrimary else MongleTextHint,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            text = if (owned) {
                val date = item.awarded?.awardedAt?.let { dateFormatter.format(it) } ?: ""
                date
            } else condition,
            style = MaterialTheme.typography.labelSmall,
            color = MongleTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

private val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

private fun categoryColor(category: BadgeCategory): Color = when (category) {
    BadgeCategory.STREAK -> Color(0xFF7CC8A0)
    BadgeCategory.ANSWER_COUNT -> Color(0xFFEFB76A)
    BadgeCategory.UNKNOWN -> Color(0xFF9E9E9E)
}

private fun badgeNameRes(code: String): Int = when (code) {
    "STREAK_3" -> R.string.badge_streak_3_name
    "STREAK_7" -> R.string.badge_streak_7_name
    "STREAK_30" -> R.string.badge_streak_30_name
    "STREAK_100" -> R.string.badge_streak_100_name
    "ANSWERS_10" -> R.string.badge_answers_10_name
    "ANSWERS_50" -> R.string.badge_answers_50_name
    else -> 0
}

private fun badgeConditionRes(code: String): Int = when (code) {
    "STREAK_3" -> R.string.badge_streak_3_cond
    "STREAK_7" -> R.string.badge_streak_7_cond
    "STREAK_30" -> R.string.badge_streak_30_cond
    "STREAK_100" -> R.string.badge_streak_100_cond
    "ANSWERS_10" -> R.string.badge_answers_10_cond
    "ANSWERS_50" -> R.string.badge_answers_50_cond
    else -> 0
}

