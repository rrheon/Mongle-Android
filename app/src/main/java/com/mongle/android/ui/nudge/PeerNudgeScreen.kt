package com.mongle.android.ui.nudge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.mongle.android.ui.common.MongleToastData
import com.mongle.android.ui.common.MongleToastHost
import com.mongle.android.ui.common.MongleToastType
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mongle.android.ui.theme.pastelColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleCharacter
import com.mongle.android.ui.theme.MongleHeartRed
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import androidx.compose.ui.res.stringResource
import com.ycompany.Monggle.R
import com.mongle.android.util.AdManager

private val NudgeBgStart = pastelColor(0xFFFFF8F0)
private val NudgeBgEnd   = pastelColor(0xFFEFF8F1)

@Composable
fun PeerNudgeScreen(
    targetUser: User,
    currentUserHearts: Int,
    questionContent: String = "",
    adManager: AdManager,
    onBack: () -> Unit,
    onNudgeSent: (heartsRemaining: Int) -> Unit = {},
    viewModel: PeerNudgeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var toastData by remember { mutableStateOf<MongleToastData?>(null) }

    LaunchedEffect(targetUser) {
        viewModel.initialize(
            targetUserId = targetUser.id.toString(),
            targetUserName = targetUser.name,
            hearts = currentUserHearts
        )
    }

    // 재촉 전송 성공 시 하트 잔량을 상위에 알리고 토스트 노출
    val sentToastMessage = stringResource(R.string.nudge_sent)
    LaunchedEffect(uiState.sentCount) {
        val heartsRemaining = uiState.heartsRemaining
        if (uiState.sentCount > 0 && heartsRemaining != null) {
            onNudgeSent(heartsRemaining)
            toastData = MongleToastData(message = sentToastMessage, type = MongleToastType.SUCCESS)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toastData = MongleToastData(message = it, type = MongleToastType.ERROR)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NudgeBgStart, NudgeBgEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(MongleSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 내비게이션
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.nudge_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // 하트 수 표시
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MongleHeartRed.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MongleHeartRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${uiState.hearts}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MongleHeartRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.lg))

            // Scrollable 3-section layout (iOS 동일 구조)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MongleSpacing.lg)
            ) {
                // ── Section 1: 오늘의 질문 카드 ──
                if (questionContent.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(MongleSpacing.lg)) {
                            Text(
                                text = stringResource(R.string.nudge_today_question),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MonglePrimary
                            )
                            Spacer(modifier = Modifier.height(MongleSpacing.xs))
                            Text(
                                text = questionContent,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MongleTextPrimary
                            )
                        }
                    }
                }

                // ── Section 2: 빈 상태 (아직 답변 안 함) ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MongleSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
                ) {
                    MongleCharacter(
                        user = targetUser,
                        index = 0,
                        size = 60.dp,
                        showName = false
                    )
                    Spacer(modifier = Modifier.height(MongleSpacing.xs))
                    Text(
                        text = stringResource(R.string.nudge_not_answered, uiState.targetUserName),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MongleTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.nudge_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextHint,
                        textAlign = TextAlign.Center
                    )
                }

                // ── Section 3: 넛지 카드 ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(MongleSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(MongleSpacing.sm)
                    ) {
                        Text(
                            text = stringResource(R.string.nudge_prompt),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MongleTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.nudge_desc, uiState.targetUserName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MongleTextSecondary
                        )

                        // 보유 하트 + 비용 배지
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MongleSpacing.xs)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MongleHeartRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.nudge_hearts, uiState.hearts),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MongleTextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(MongleHeartRed.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "-1",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MongleHeartRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MongleSpacing.xs))

                        // 버튼 영역
                        when {
                            uiState.hasEnoughHearts -> {
                                Button(
                                    onClick = { viewModel.sendNudge() },
                                    enabled = !uiState.isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MonglePrimary)
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.nudge_send),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                            )
                                        )
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = stringResource(R.string.nudge_insufficient),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MongleTextSecondary
                                )
                                Spacer(modifier = Modifier.height(MongleSpacing.xxs))
                                Button(
                                    onClick = { viewModel.watchAdForNudge(adManager) },
                                    enabled = !uiState.isWatchingAd,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = pastelColor(0xFF7CC8A0))
                                ) {
                                    if (uiState.isWatchingAd) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.nudge_watch_ad),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        MongleToastHost(
            toastData = toastData,
            onDismiss = { toastData = null }
        )
    }
}
