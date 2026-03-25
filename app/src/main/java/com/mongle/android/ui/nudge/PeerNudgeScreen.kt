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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.mongle.android.ui.theme.MongleTextSecondary
import com.mongle.android.util.AdManager

private val NudgeBgStart = Color(0xFFFFF8F0)
private val NudgeBgEnd   = Color(0xFFEFF8F1)

@Composable
fun PeerNudgeScreen(
    targetUser: User,
    currentUserHearts: Int,
    adManager: AdManager,
    onBack: () -> Unit,
    viewModel: PeerNudgeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(targetUser) {
        viewModel.initialize(
            targetUserId = targetUser.id.toString(),
            targetUserName = targetUser.name,
            hearts = currentUserHearts
        )
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
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
                        contentDescription = "뒤로가기",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "재촉하기",
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

            Spacer(modifier = Modifier.height(40.dp))

            // 대상 멤버 캐릭터
            MongleCharacter(
                user = targetUser,
                index = 0,
                size = 80.dp,
                showName = false
            )

            Spacer(modifier = Modifier.height(MongleSpacing.md))

            Text(
                text = "${uiState.targetUserName}에게",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "재촉 메시지를 보낼까요?",
                style = MaterialTheme.typography.bodyLarge,
                color = MongleTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 하트 비용 안내
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(MongleHeartRed.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MongleHeartRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "하트 1개 소모",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MongleHeartRed
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            when {
                uiState.isSent -> {
                    // 전송 완료
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MonglePrimary.copy(alpha = 0.1f))
                            .padding(MongleSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✅ 재촉 메시지를 보냈어요!",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MonglePrimary
                        )
                    }
                }
                uiState.hasEnoughHearts -> {
                    // 하트 충분 — 재촉하기 버튼
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
                                text = "재촉하기 💌",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
                else -> {
                    // 하트 부족 — 광고 버튼
                    Text(
                        text = "하트가 부족해요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextSecondary
                    )
                    Spacer(modifier = Modifier.height(MongleSpacing.sm))
                    Button(
                        onClick = { viewModel.watchAdForNudge(adManager) },
                        enabled = !uiState.isWatchingAd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        if (uiState.isWatchingAd) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "광고 보고 재촉하기 💚",
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
