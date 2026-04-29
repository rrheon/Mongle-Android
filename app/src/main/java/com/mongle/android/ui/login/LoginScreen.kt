package com.mongle.android.ui.login

import android.app.Activity
import android.net.Uri
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ycompany.Monggle.R
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.domain.model.LegalDocType
import com.mongle.android.domain.model.LegalVersions
import com.mongle.android.domain.model.User
import com.mongle.android.ui.common.MongleLogo
import com.mongle.android.ui.common.MongleLogoSize
import com.mongle.android.ui.theme.MongleAppleLight
import com.mongle.android.ui.theme.MongleAppleTextLight
import com.mongle.android.ui.theme.MongleGoogleBorder
import com.mongle.android.ui.theme.MongleKakao
import com.mongle.android.ui.theme.MongleKakaoText
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleBackgroundLight
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoggedIn: (User, Boolean, List<LegalDocType>, LegalVersions) -> Unit,
    onBrowse: () -> Unit = {},
    onEmailLoginSelected: () -> Unit = {},
    onEmailSignupSelected: () -> Unit = {},
    pendingAppleCallbackUri: Uri? = null,
    onAppleCallbackConsumed: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 이메일로 계속하기 → 로그인/회원가입 선택 바텀시트 (iOS 와 동일 패턴)
    var showEmailChoiceSheet by remember { mutableStateOf(false) }
    val emailChoiceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    // Apple Sign-In 콜백 처리: 서버가 monggle://apple-callback 으로 리다이렉트하면 여기서 수신
    LaunchedEffect(pendingAppleCallbackUri) {
        val uri = pendingAppleCallbackUri ?: return@LaunchedEffect
        onAppleCallbackConsumed()
        try {
            // MG-99 handleAppleCallback 가 state 일치 검증을 위해 context 를 받음.
            val credential = handleAppleCallback(context, uri)
            Log.d("LoginScreen", "Apple 토큰 파싱 성공 → 서버 로그인 요청")
            viewModel.loginWithSocial(credential)
        } catch (e: Exception) {
            Log.e("LoginScreen", "Apple 로그인 실패: ${e.message}")
            viewModel.setError(e.message ?: "Apple 로그인에 실패했습니다.")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LoggedIn -> onLoggedIn(
                    event.user,
                    event.needsConsent,
                    event.requiredConsents,
                    event.legalVersions
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MongleBackgroundLight) // iOS 기준: 단색 background
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
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MongleTextPrimary
                    )

                    Spacer(modifier = Modifier.height(MongleSpacing.xs))

                    Text(
                        text = stringResource(R.string.login_subtitle),
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

                Spacer(modifier = Modifier.height(MongleSpacing.md))

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
                        Log.d("LoginScreen", "Apple 로그인 버튼 클릭 → Custom Tab 실행")
                        launchAppleSignIn(context)
                    }
                )

                Spacer(modifier = Modifier.height(MongleSpacing.sm))

                // 이메일로 계속하기 — Apple 버튼 아래
                Button(
                    onClick = { showEmailChoiceSheet = true },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, MongleGoogleBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MongleTextPrimary,
                        disabledContainerColor = Color.White.copy(alpha = 0.5f),
                        disabledContentColor = MongleTextPrimary.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = MongleSpacing.md)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.login_email),
                            style = MaterialTheme.typography.labelLarge,
                            color = MongleTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MongleSpacing.md))

                // 둘러보기
                TextButton(
                    onClick = onBrowse,
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = stringResource(R.string.login_browse),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MongleTextHint
                    )
                }
            }
        }

        // 에러 토스트 (iOS 기준: mongleErrorToast 방식)
        val errorMessage = uiState.errorMessage
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.xl)
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm)
                )
            }
            LaunchedEffect(uiState.errorMessage) {
                kotlinx.coroutines.delay(2000)
                viewModel.dismissError()
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

        // 이메일 로그인/회원가입 선택 바텀시트
        if (showEmailChoiceSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEmailChoiceSheet = false },
                sheetState = emailChoiceSheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MongleSpacing.xl)
                        .padding(top = MongleSpacing.sm, bottom = MongleSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.email_auth_choice_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MongleTextPrimary
                    )
                    Spacer(modifier = Modifier.height(MongleSpacing.xs))
                    Text(
                        text = stringResource(R.string.email_auth_choice_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextSecondary
                    )

                    Spacer(modifier = Modifier.height(MongleSpacing.lg))

                    // 로그인 (Primary)
                    Button(
                        onClick = {
                            showEmailChoiceSheet = false
                            onEmailLoginSelected()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.email_auth_choice_login),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(MongleSpacing.sm))

                    // 회원가입 (Outlined)
                    OutlinedButton(
                        onClick = {
                            showEmailChoiceSheet = false
                            onEmailSignupSelected()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, MongleGoogleBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = MongleTextPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.email_auth_choice_signup),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Social Login Provider

enum class SocialProvider {
    KAKAO, GOOGLE, APPLE;

    val title: String
        get() = when (this) {
            KAKAO -> "카카오로 계속하기"
            GOOGLE -> "Google로 계속하기"
            APPLE -> "Apple로 계속하기"
        }

    /** 카카오 공식 가이드: 12dp, 그 외: 16dp */
    val cornerRadius: Dp
        get() = when (this) {
            KAKAO -> 12.dp
            GOOGLE, APPLE -> 16.dp
        }

    val backgroundColor: Color
        get() = when (this) {
            KAKAO -> MongleKakao
            GOOGLE -> Color.White
            APPLE -> MongleAppleLight
        }

    val contentColor: Color
        get() = when (this) {
            KAKAO -> MongleKakaoText
            GOOGLE -> MongleTextPrimary
            APPLE -> MongleAppleTextLight
        }
}

@Composable
private fun SocialProvider.localizedTitle(): String = when (this) {
    SocialProvider.KAKAO -> stringResource(R.string.login_kakao)
    SocialProvider.GOOGLE -> stringResource(R.string.login_google)
    SocialProvider.APPLE -> stringResource(R.string.login_apple)
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
                }
            }
            Spacer(modifier = Modifier.width(MongleSpacing.sm))
            Text(
                text = provider.localizedTitle(),
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


