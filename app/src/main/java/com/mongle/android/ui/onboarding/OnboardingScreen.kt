package com.mongle.android.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mongle.android.ui.common.MongleButton
import com.mongle.android.ui.common.MongleLogo
import com.mongle.android.ui.common.MongleLogoSize
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        eyebrow = "환영해요 🌿",
        title = "몽글에 오신 걸\n환영해요",
        description = "가족, 친구와 매일 마음을 나누는\n따뜻한 소통 공간"
    ),
    OnboardingPage(
        eyebrow = "함께 만들기",
        title = "나만의 공간을\n만들어보세요",
        description = "가족, 친구, 커플 등\n함께하고 싶은 사람들을 초대해요"
    ),
    OnboardingPage(
        eyebrow = "매일 함께",
        title = "매일 함께\n마음을 나눠요",
        description = "매일 새로운 질문에 답하고\n서로의 마음을 들여다보세요"
    )
)

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onNeverShowAgain: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF8F0), Color(0xFFFFF2EB), Color(0xFFEFF8F1))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = onboardingPages[page])
            }

            // 하단 바
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 페이지 인디케이터
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onboardingPages.forEachIndexed { index, _ ->
                        val isSelected = index == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            label = "indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MonglePrimary else Color(0xFFD0D0D0)
                                )
                        )
                    }
                }

                MongleButton(
                    text = when {
                        pagerState.currentPage == 0 -> "시작하기"
                        isLastPage -> "몽글 시작하기 🌿"
                        else -> "다음"
                    },
                    onClick = {
                        if (isLastPage) {
                            onGetStarted()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )

                TextButton(onClick = onNeverShowAgain) {
                    Text(
                        text = "다시 보지않기",
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextHint
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MongleLogo(size = MongleLogoSize.LARGE)

        Spacer(modifier = Modifier.height(44.dp))

        Text(
            text = page.eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MonglePrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MongleTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MongleTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
