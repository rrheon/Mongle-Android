package com.mongle.android.ui.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleLogo
import com.mongle.android.ui.common.MongleLogoSize
import com.mongle.android.ui.theme.MongleAppleLight
import com.mongle.android.ui.theme.MongleAppleTextLight
import com.mongle.android.ui.theme.MongleKakao
import com.mongle.android.ui.theme.MongleKakaoText
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import kotlinx.coroutines.launch

// Google Web Client ID: Google Cloud Console → APIs & Services → Credentials
// → OAuth 2.0 클라이언트 ID 중 "웹 애플리케이션" 타입의 ID 사용
private const val GOOGLE_WEB_CLIENT_ID = "YOUR_GOOGLE_WEB_CLIENT_ID"

@Composable
fun LoginScreen(
    onLoggedIn: (User) -> Unit,
    onBrowse: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Google Sign-In Activity Result 처리
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = handleGoogleSignInResult(result.data)
                viewModel.loginWithSocial(credential)
            } catch (e: Exception) {
                viewModel.setError("Google 로그인에 실패했습니다: ${e.message}")
            }
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
                    text = "카카오로 계속하기",
                    backgroundColor = MongleKakao,
                    contentColor = MongleKakaoText,
                    emoji = "💬",
                    enabled = !uiState.isLoading,
                    onClick = {
                        scope.launch {
                            try {
                                val credential = loginWithKakao(context)
                                viewModel.loginWithSocial(credential)
                            } catch (e: Exception) {
                                viewModel.setError("카카오 로그인에 실패했습니다: ${e.message}")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                // Google 로그인
                SocialLoginButton(
                    text = "Google로 계속하기",
                    backgroundColor = Color.White,
                    contentColor = Color(0xFF1A1A1A),
                    emoji = "G",
                    enabled = !uiState.isLoading,
                    onClick = {
                        val intent = getGoogleSignInIntent(context, GOOGLE_WEB_CLIENT_ID)
                        googleLauncher.launch(intent)
                    }
                )

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                // Apple 로그인
                SocialLoginButton(
                    text = "Apple로 계속하기",
                    backgroundColor = MongleAppleLight,
                    contentColor = MongleAppleTextLight,
                    emoji = "",
                    enabled = !uiState.isLoading,
                    onClick = {
                        viewModel.setError("Apple 로그인은 준비 중입니다.")
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

@Composable
private fun SocialLoginButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    emoji: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (emoji.isNotEmpty()) {
                Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(MongleSpacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}
