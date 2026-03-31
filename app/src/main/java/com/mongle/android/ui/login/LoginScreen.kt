package com.mongle.android.ui.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.mongle.android.R
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleLogo
import com.mongle.android.ui.common.MongleLogoSize
import com.mongle.android.ui.theme.MongleAppleLight
import com.mongle.android.ui.theme.MongleAppleTextLight
import com.mongle.android.ui.theme.MongleGoogleBorder
import com.mongle.android.ui.theme.MongleKakao
import com.mongle.android.ui.theme.MongleKakaoText
import com.mongle.android.ui.theme.MongleNaver
import com.mongle.android.ui.theme.MongleNaverText
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import kotlinx.coroutines.launch


@Composable
fun LoginScreen(
    onLoggedIn: (User) -> Unit,
    onBrowse: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreen", "Google 결과 수신 | resultCode=${result.resultCode} (OK=${Activity.RESULT_OK})")
        try {
            val credential = handleGoogleSignInResult(result.data)
            Log.d("LoginScreen", "Google 토큰 파싱 성공 → 서버 로그인 요청")
            viewModel.loginWithSocial(credential)
        } catch (e: Exception) {
            Log.e("LoginScreen", "Google 로그인 실패: ${e.message}")
            viewModel.setError(e.message ?: "Google 로그인에 실패했습니다.")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LoggedIn -> onLoggedIn(event.user)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8F0),
                        Color(0xFFFFF2EB),
                        Color(0xFFEFF8F1)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 상단 로고 영역 (화면 약 55%) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MongleLogo(size = MongleLogoSize.LARGE)

                    Spacer(modifier = Modifier.height(MongleSpacing.md))

                    Text(
                        text = "몽글",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MongleTextPrimary
                    )

                    Spacer(modifier = Modifier.height(MongleSpacing.xs))

                    Text(
                        text = "오늘의 마음은 어떤 색인가요?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.05f))

            // ── 소셜 로그인 버튼 영역 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MongleSpacing.xl)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 에러 메시지
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(MongleSpacing.sm))
                }

                // 카카오 로그인
                SocialLoginButton(
                    provider = SocialProvider.KAKAO,
                    enabled = !uiState.isLoading,
                    onClick = {
                        Log.d("LoginScreen", "카카오 로그인 버튼 클릭")
                        scope.launch {
                            try {
                                val credential = loginWithKakao(context)
                                Log.d("LoginScreen", "카카오 credential 획득 → 서버 로그인 요청")
                                viewModel.loginWithSocial(credential)
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "카카오 로그인 실패: ${e.message}")
                                viewModel.setError(e.message ?: "카카오 로그인에 실패했습니다.")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                // Google 로그인
                SocialLoginButton(
                    provider = SocialProvider.GOOGLE,
                    enabled = !uiState.isLoading,
                    onClick = {
                        Log.d("LoginScreen", "Google 로그인 버튼 클릭 | clientId=$GOOGLE_WEB_CLIENT_ID")
                        val intent = getGoogleSignInIntent(context, GOOGLE_WEB_CLIENT_ID)
                        googleLauncher.launch(intent)
                    }
                )

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                // Apple 로그인
                SocialLoginButton(
                    provider = SocialProvider.APPLE,
                    enabled = !uiState.isLoading,
                    onClick = {
                        viewModel.setError("Apple 로그인은 준비 중입니다.")
                    }
                )

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                // 네이버 로그인
                SocialLoginButton(
                    provider = SocialProvider.NAVER,
                    enabled = !uiState.isLoading,
                    onClick = {
                        viewModel.setError("네이버 로그인은 준비 중입니다.")
                    }
                )

                Spacer(modifier = Modifier.height(MongleSpacing.md))

                // 둘러보기
                TextButton(
                    onClick = onBrowse,
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = "둘러보기",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextHint
                    )
                }
            }
        }

        // 로딩 오버레이
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// MARK: - Social Login Provider

enum class SocialProvider {
    KAKAO, GOOGLE, APPLE, NAVER;

    val title: String
        get() = when (this) {
            KAKAO -> "카카오로 계속하기"
            GOOGLE -> "Google로 계속하기"
            APPLE -> "Apple로 계속하기"
            NAVER -> "네이버로 계속하기"
        }

    /** 카카오 공식 가이드: 12dp, 그 외: 16dp */
    val cornerRadius: Dp
        get() = when (this) {
            KAKAO -> 12.dp
            else -> 16.dp
        }

    val backgroundColor: Color
        get() = when (this) {
            KAKAO -> MongleKakao
            GOOGLE -> Color.White
            APPLE -> MongleAppleLight
            NAVER -> MongleNaver
        }

    val contentColor: Color
        get() = when (this) {
            KAKAO -> MongleKakaoText
            GOOGLE -> MongleTextPrimary
            APPLE -> MongleAppleTextLight
            NAVER -> MongleNaverText
        }
}

// MARK: - SocialLoginButton
// iOS SocialLoginButton과 동일: 52dp 높이, 프로바이더별 아이콘 + 텍스트

@Composable
private fun SocialLoginButton(
    provider: SocialProvider,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(provider.cornerRadius)
    val modifier = if (provider == SocialProvider.GOOGLE) {
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, MongleGoogleBorder, shape)
    } else {
        Modifier
            .fillMaxWidth()
            .height(52.dp)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = provider.backgroundColor,
            contentColor = provider.contentColor,
            disabledContainerColor = provider.backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = provider.contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = MongleSpacing.md)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로바이더 아이콘
            Box(modifier = Modifier.size(20.dp)) {
                when (provider) {
                    SocialProvider.KAKAO -> KakaoLogoIcon(size = 20.dp)
                    SocialProvider.GOOGLE -> Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    SocialProvider.APPLE -> Icon(
                        painter = painterResource(id = R.drawable.ic_apple_logo),
                        contentDescription = null,
                        tint = provider.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    SocialProvider.NAVER -> Icon(
                        painter = painterResource(id = R.drawable.ic_naver_logo),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(MongleSpacing.sm))
            Text(
                text = provider.title,
                style = MaterialTheme.typography.labelLarge,
                color = provider.contentColor
            )
        }
    }
}

// MARK: - 카카오 로고 아이콘 (공식 SVG 기반, viewBox 0 0 32 32)

@Composable
private fun KakaoLogoIcon(size: Dp = 20.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 32f
        val path = Path()
        path.moveTo(16f * s, 4f * s)
        path.cubicTo(9.373f * s, 4f * s, 4f * s, 8.283f * s, 4f * s, 13.565f * s)
        path.cubicTo(4f * s, 16.892f * s, 6.152f * s, 19.820f * s, 9.378f * s, 21.522f * s)
        path.lineTo(8.301f * s, 25.452f * s)
        path.cubicTo(8.187f * s, 25.869f * s, 8.671f * s, 26.218f * s, 9.031f * s, 25.970f * s)
        path.lineTo(13.506f * s, 22.920f * s)
        path.cubicTo(14.663f * s, 23.093f * s, 15.872f * s, 23.186f * s, 17.112f * s, 23.186f * s)
        path.cubicTo(23.739f * s, 23.186f * s, 29.112f * s, 18.903f * s, 29.112f * s, 13.621f * s)
        path.cubicTo(28f * s, 8.283f * s, 22.627f * s, 4f * s, 16f * s, 4f * s)
        path.close()
        drawPath(path, color = Color.Black)
    }
}


