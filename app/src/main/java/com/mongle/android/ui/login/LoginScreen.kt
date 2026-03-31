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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
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
import com.mongle.android.ui.theme.MongleGoogleBlue
import com.mongle.android.ui.theme.MongleGoogleGreen
import com.mongle.android.ui.theme.MongleGoogleRed
import com.mongle.android.ui.theme.MongleGoogleYellow
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
                    SocialProvider.GOOGLE -> GoogleLogoIcon(size = 20.dp)
                    SocialProvider.APPLE -> AppleLogoIcon(size = 20.dp, tint = provider.contentColor)
                    SocialProvider.NAVER -> NaverLogoIcon(size = 20.dp)
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

// MARK: - Google 로고 아이콘 (공식 SVG 기반, viewBox 0 0 48 48)

@Composable
private fun GoogleLogoIcon(size: Dp = 20.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 48f

        // Red — top arc (#EA4335)
        val redPath = Path()
        redPath.moveTo(24f * s, 9.5f * s)
        redPath.cubicTo(27.54f * s, 9.5f * s, 30.71f * s, 10.72f * s, 33.21f * s, 13.1f * s)
        redPath.lineTo(40.06f * s, 6.25f * s)
        redPath.cubicTo(35.9f * s, 2.38f * s, 30.47f * s, 0f, 24f * s, 0f)
        redPath.cubicTo(14.62f * s, 0f, 6.51f * s, 5.38f * s, 2.56f * s, 13.22f * s)
        redPath.lineTo(10.54f * s, 19.41f * s)
        redPath.cubicTo(12.43f * s, 13.72f * s, 17.74f * s, 9.5f * s, 24f * s, 9.5f * s)
        redPath.close()
        drawPath(redPath, color = MongleGoogleRed)

        // Blue — right arc (#4285F4)
        val bluePath = Path()
        bluePath.moveTo(46.98f * s, 24.55f * s)
        bluePath.cubicTo(46.98f * s, 22.98f * s, 46.83f * s, 21.46f * s, 46.6f * s, 20f * s)
        bluePath.lineTo(24f * s, 20f * s)
        bluePath.lineTo(24f * s, 29.02f * s)
        bluePath.lineTo(36.94f * s, 29.02f * s)
        bluePath.cubicTo(36.36f * s, 31.98f * s, 34.68f * s, 34.5f * s, 32.16f * s, 36.2f * s)
        bluePath.lineTo(39.89f * s, 42.2f * s)
        bluePath.cubicTo(44.4f * s, 38.02f * s, 46.98f * s, 31.84f * s, 46.98f * s, 24.55f * s)
        bluePath.close()
        drawPath(bluePath, color = MongleGoogleBlue)

        // Yellow — left arc (#FBBC05)
        val yellowPath = Path()
        yellowPath.moveTo(10.53f * s, 28.59f * s)
        yellowPath.cubicTo(10.05f * s, 27.14f * s, 9.77f * s, 25.6f * s, 9.77f * s, 24f * s)
        yellowPath.cubicTo(9.77f * s, 22.4f * s, 10.04f * s, 20.86f * s, 10.53f * s, 19.41f * s)
        yellowPath.lineTo(2.55f * s, 13.22f * s)
        yellowPath.cubicTo(0.92f * s, 16.46f * s, 0f, 20.12f * s, 0f, 24f * s)
        yellowPath.cubicTo(0f, 27.88f * s, 0.92f * s, 31.54f * s, 2.56f * s, 34.78f * s)
        yellowPath.lineTo(10.53f * s, 28.59f * s)
        yellowPath.close()
        drawPath(yellowPath, color = MongleGoogleYellow)

        // Green — bottom arc (#34A853)
        val greenPath = Path()
        greenPath.moveTo(24f * s, 48f * s)
        greenPath.cubicTo(30.48f * s, 48f * s, 35.93f * s, 45.87f * s, 39.89f * s, 42.19f * s)
        greenPath.lineTo(32.16f * s, 36.19f * s)
        greenPath.cubicTo(30.01f * s, 37.64f * s, 27.24f * s, 38.49f * s, 24f * s, 38.49f * s)
        greenPath.cubicTo(17.74f * s, 38.49f * s, 12.43f * s, 34.27f * s, 10.53f * s, 28.58f * s)
        greenPath.lineTo(2.55f * s, 34.77f * s)
        greenPath.cubicTo(6.51f * s, 42.62f * s, 14.62f * s, 48f * s, 24f * s, 48f * s)
        greenPath.close()
        drawPath(greenPath, color = MongleGoogleGreen)
    }
}

// MARK: - Apple 로고 아이콘 (간소화된 사과 모양)

@Composable
private fun AppleLogoIcon(size: Dp = 20.dp, tint: Color = Color.White) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        drawApplePath(tint)
    }
}

private fun DrawScope.drawApplePath(color: Color) {
    val w = size.width
    val h = size.height

    // Apple 로고 근사 (간소화된 버전)
    // 줄기
    val stemPath = Path()
    stemPath.moveTo(w * 0.52f, h * 0.06f)
    stemPath.cubicTo(w * 0.52f, h * 0.06f, w * 0.56f, h * 0.0f, w * 0.65f, h * 0.05f)
    stemPath.cubicTo(w * 0.73f, h * 0.10f, w * 0.62f, h * 0.20f, w * 0.52f, h * 0.20f)
    stemPath.close()
    drawPath(stemPath, color = color)

    // 사과 본체
    val bodyPath = Path()
    bodyPath.moveTo(w * 0.50f, h * 0.22f)
    // 왼쪽 위 노치
    bodyPath.cubicTo(w * 0.38f, h * 0.22f, w * 0.18f, h * 0.28f, w * 0.14f, h * 0.48f)
    // 왼쪽 아래
    bodyPath.cubicTo(w * 0.09f, h * 0.68f, w * 0.18f, h * 0.88f, w * 0.32f, h * 0.94f)
    // 아래 중앙
    bodyPath.cubicTo(w * 0.40f, h * 0.98f, w * 0.44f, h * 0.96f, w * 0.50f, h * 0.94f)
    // 오른쪽 아래
    bodyPath.cubicTo(w * 0.56f, h * 0.96f, w * 0.60f, h * 0.98f, w * 0.68f, h * 0.94f)
    // 오른쪽 위
    bodyPath.cubicTo(w * 0.82f, h * 0.88f, w * 0.91f, h * 0.68f, w * 0.86f, h * 0.48f)
    // 오른쪽 위 노치
    bodyPath.cubicTo(w * 0.82f, h * 0.28f, w * 0.62f, h * 0.22f, w * 0.50f, h * 0.22f)
    bodyPath.close()
    drawPath(bodyPath, color = color)
}

// MARK: - 네이버 로고 아이콘 ("N" 텍스트 기반)

@Composable
private fun NaverLogoIcon(size: Dp = 20.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.16f

        // N 글자를 Path로 그리기
        val path = Path()
        // 왼쪽 세로선
        path.moveTo(w * 0.15f, h * 0.10f)
        path.lineTo(w * 0.15f, h * 0.90f)
        path.lineTo(w * 0.15f + stroke, h * 0.90f)
        path.lineTo(w * 0.15f + stroke, h * 0.10f)
        path.close()

        // 대각선
        path.moveTo(w * 0.15f, h * 0.10f)
        path.lineTo(w * 0.15f + stroke, h * 0.10f)
        path.lineTo(w * 0.85f, h * 0.90f)
        path.lineTo(w * 0.85f - stroke, h * 0.90f)
        path.close()

        // 오른쪽 세로선
        path.moveTo(w * 0.85f - stroke, h * 0.10f)
        path.lineTo(w * 0.85f, h * 0.10f)
        path.lineTo(w * 0.85f, h * 0.90f)
        path.lineTo(w * 0.85f - stroke, h * 0.90f)
        path.close()

        drawPath(path, color = Color.White)
    }
}
