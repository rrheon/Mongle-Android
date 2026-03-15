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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleButton
import com.mongle.android.ui.common.MongleButtonStyle
import com.mongle.android.ui.common.MongleLogo
import com.mongle.android.ui.common.MongleLogoSize
import com.mongle.android.ui.common.MongleTextField
import com.mongle.android.ui.theme.MongleKakao
import com.mongle.android.ui.theme.MongleKakaoText
import com.mongle.android.ui.theme.MongleSpacing
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions as KeyboardOptionsCompose

// Google Web Client ID (Firebase Console → OAuth 2.0 클라이언트 ID)
private const val GOOGLE_WEB_CLIENT_ID = "YOUR_GOOGLE_WEB_CLIENT_ID"

@Composable
fun LoginScreen(
    onLoggedIn: (User) -> Unit,
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MongleSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            MongleLogo(size = MongleLogoSize.LARGE)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "가족과 함께 매일 나누는 이야기",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(MongleSpacing.xxl))

            // 이메일/비밀번호 입력 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(MongleSpacing.lg)
            ) {
                if (uiState.isSignUp) {
                    MongleTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChange,
                        placeholder = "이름",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(MongleSpacing.sm))
                }

                MongleTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = "이메일",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptionsCompose(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                MongleTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    placeholder = "비밀번호",
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptionsCompose(keyboardType = KeyboardType.Password)
                )

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(MongleSpacing.xs))
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(MongleSpacing.md))

                MongleButton(
                    text = if (uiState.isSignUp) "회원가입" else "로그인",
                    onClick = {
                        if (uiState.isSignUp) viewModel.signupWithEmail()
                        else viewModel.loginWithEmail()
                    },
                    isLoading = uiState.isLoading
                )

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isSignUp) "이미 계정이 있으신가요?" else "계정이 없으신가요?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = viewModel::toggleSignUp) {
                        Text(
                            text = if (uiState.isSignUp) "로그인" else "회원가입",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MongleSpacing.lg))

            // 소셜 로그인 구분선
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.4f))
                Text(
                    text = "  소셜 로그인  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.4f))
            }

            Spacer(modifier = Modifier.height(MongleSpacing.md))

            // 카카오 로그인
            SocialLoginButton(
                text = "카카오로 계속하기",
                backgroundColor = MongleKakao,
                contentColor = MongleKakaoText,
                emoji = "💬",
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
                onClick = {
                    val intent = getGoogleSignInIntent(context, GOOGLE_WEB_CLIENT_ID)
                    googleLauncher.launch(intent)
                }
            )

            Spacer(modifier = Modifier.height(MongleSpacing.xxl))
        }
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    emoji: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(MongleSpacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}
