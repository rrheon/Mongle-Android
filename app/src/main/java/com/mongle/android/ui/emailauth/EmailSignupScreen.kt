package com.mongle.android.ui.emailauth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ycompany.Monggle.R
import com.mongle.android.domain.model.LegalVersions
import com.mongle.android.domain.model.SocialLoginResult
import com.mongle.android.ui.common.MongleToastOverlay
import com.mongle.android.ui.common.MongleToastType
import com.mongle.android.ui.consent.ConsentScreen
import com.mongle.android.ui.theme.MongleBackgroundLight
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary

@Composable
fun EmailSignupScreen(
    onCompleted: (SocialLoginResult) -> Unit,
    onCancelled: () -> Unit,
    initialLegalVersions: LegalVersions = LegalVersions(terms = "1.0.0", privacy = "1.0.0"),
    viewModel: EmailSignupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { viewModel.onBack() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EmailSignupEvent.Completed -> onCompleted(event.result)
                is EmailSignupEvent.Cancelled -> onCancelled()
            }
        }
    }

    when (uiState.phase) {
        EmailSignupPhase.CONSENT -> {
            ConsentScreen(
                requiredConsents = uiState.requiredConsents,
                legalVersions = initialLegalVersions,
                onCompleted = { /* preSignup 모드에선 호출되지 않음 */ },
                onBack = { viewModel.onBack() },
                preSignup = true,
                onPreSignupCompleted = { terms, privacy ->
                    viewModel.onConsentCompleted(terms, privacy)
                }
            )
        }

        EmailSignupPhase.INPUT_FORM -> InputFormView(
            uiState = uiState,
            onEmailChanged = viewModel::onEmailChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onSendCode = viewModel::sendCode,
            onBack = viewModel::onBack,
            onDismissError = viewModel::dismissError
        )

        EmailSignupPhase.VERIFY_CODE -> VerifyCodeView(
            uiState = uiState,
            onCodeChanged = viewModel::onCodeChanged,
            onVerify = viewModel::verifyAndSignup,
            onResend = viewModel::resendCode,
            onBack = viewModel::onBack
        )
    }
}

@Composable
private fun InputFormView(
    uiState: EmailSignupUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MongleBackgroundLight)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        TopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MongleSpacing.xl)
        ) {
            Text(
                text = stringResource(R.string.email_auth_signup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MongleTextPrimary
            )
            Spacer(Modifier.height(MongleSpacing.xs))
            Text(
                text = stringResource(R.string.email_auth_signup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextSecondary
            )

            Spacer(Modifier.height(MongleSpacing.lg))

            LabeledField(
                label = stringResource(R.string.email_auth_email_label),
                placeholder = stringResource(R.string.email_auth_email_placeholder),
                value = uiState.email,
                onValueChange = onEmailChanged,
                keyboardType = KeyboardType.Email,
                isError = uiState.emailErrorRes != null,
                errorMessage = uiState.emailErrorRes?.let { stringResource(it) }
            )

            Spacer(Modifier.height(MongleSpacing.md))

            LabeledField(
                label = stringResource(R.string.email_auth_password_label),
                placeholder = stringResource(R.string.email_auth_password_placeholder),
                value = uiState.password,
                onValueChange = onPasswordChanged,
                keyboardType = KeyboardType.Password,
                isError = uiState.passwordErrorRes != null,
                errorMessage = uiState.passwordErrorRes?.let { stringResource(it) },
                hint = stringResource(R.string.email_auth_password_hint),
                isPassword = true
            )
        }

        Button(
            onClick = onSendCode,
            enabled = uiState.canSendCode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MongleSpacing.xl)
                .padding(bottom = MongleSpacing.xl)
                .height(56.dp),
            shape = RoundedCornerShape(MongleRadius.large),
            colors = ButtonDefaults.buttonColors(
                containerColor = MonglePrimary,
                disabledContainerColor = MongleBorder
            )
        ) {
            if (uiState.isSendingCode) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.email_auth_send_code),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }

    // 서버 에러 토스트 (중복 이메일 등)
    MongleToastOverlay(
        message = uiState.errorMessage,
        type = MongleToastType.ERROR,
        onDismiss = onDismissError
    )
    }
}

@Composable
private fun VerifyCodeView(
    uiState: EmailSignupUiState,
    onCodeChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MongleBackgroundLight)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        TopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MongleSpacing.xl)
        ) {
            Text(
                text = stringResource(R.string.email_auth_verify_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MongleTextPrimary
            )
            Spacer(Modifier.height(MongleSpacing.xs))
            Text(
                text = stringResource(R.string.email_auth_verify_subtitle, uiState.email),
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextSecondary
            )

            Spacer(Modifier.height(MongleSpacing.lg))

            LabeledField(
                label = stringResource(R.string.email_auth_code_label),
                placeholder = stringResource(R.string.email_auth_code_placeholder),
                value = uiState.code,
                onValueChange = onCodeChanged,
                keyboardType = KeyboardType.NumberPassword,
                isError = uiState.codeError != null,
                errorMessage = uiState.codeError
            )

            Spacer(Modifier.height(MongleSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (uiState.resendCooldownSec > 0) {
                    Text(
                        text = stringResource(R.string.email_auth_resend_cooldown, uiState.resendCooldownSec),
                        style = MaterialTheme.typography.bodySmall,
                        color = MongleTextHint
                    )
                } else {
                    TextButton(
                        onClick = onResend,
                        enabled = !uiState.isSendingCode,
                        contentPadding = PaddingValues(horizontal = MongleSpacing.xs)
                    ) {
                        Text(
                            text = stringResource(R.string.email_auth_resend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MonglePrimary
                        )
                    }
                }
            }
        }

        Button(
            onClick = onVerify,
            enabled = uiState.canSubmitCode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MongleSpacing.xl)
                .padding(bottom = MongleSpacing.xl)
                .height(56.dp),
            shape = RoundedCornerShape(MongleRadius.large),
            colors = ButtonDefaults.buttonColors(
                containerColor = MonglePrimary,
                disabledContainerColor = MongleBorder
            )
        ) {
            if (uiState.isVerifying) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.email_auth_verify_submit),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MongleSpacing.xs, vertical = MongleSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MongleTextPrimary
            )
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    isError: Boolean,
    errorMessage: String?,
    hint: String? = null,
    isPassword: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MongleTextSecondary
        )
        Spacer(Modifier.height(MongleSpacing.xs))

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MongleTextHint) },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(MongleRadius.large),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isError) 1.5.dp else 1.dp,
                    color = if (isError) MaterialTheme.colorScheme.error else MongleBorder,
                    shape = RoundedCornerShape(MongleRadius.large)
                )
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(MongleSpacing.xxs))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (hint != null) {
            Spacer(Modifier.height(MongleSpacing.xxs))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MongleTextHint
            )
        }
    }
}
