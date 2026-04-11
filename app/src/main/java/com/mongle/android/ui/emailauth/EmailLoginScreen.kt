package com.mongle.android.ui.emailauth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ycompany.Monggle.R
import com.mongle.android.domain.model.SocialLoginResult
import com.mongle.android.ui.theme.MongleBackgroundLight
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary

@Composable
fun EmailLoginScreen(
    onCompleted: (SocialLoginResult) -> Unit,
    onCancelled: () -> Unit,
    viewModel: EmailLoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    BackHandler { viewModel.onBack() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EmailLoginEvent.Completed -> onCompleted(event.result)
                is EmailLoginEvent.Cancelled -> onCancelled()
            }
        }
    }

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
        TopBar(onBack = viewModel::onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MongleSpacing.xl)
        ) {
            Text(
                text = stringResource(R.string.email_auth_login_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MongleTextPrimary
            )
            Spacer(Modifier.height(MongleSpacing.xs))
            Text(
                text = stringResource(R.string.email_auth_login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextSecondary
            )

            Spacer(Modifier.height(MongleSpacing.lg))

            LoginField(
                label = stringResource(R.string.email_auth_email_label),
                placeholder = stringResource(R.string.email_auth_email_placeholder),
                value = uiState.email,
                onValueChange = viewModel::onEmailChanged,
                keyboardType = KeyboardType.Email,
                isError = uiState.emailErrorRes != null,
                errorMessage = uiState.emailErrorRes?.let { stringResource(it) }
            )

            Spacer(Modifier.height(MongleSpacing.md))

            LoginField(
                label = stringResource(R.string.email_auth_password_label),
                placeholder = stringResource(R.string.email_auth_password_placeholder),
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                keyboardType = KeyboardType.Password,
                isError = uiState.passwordErrorRes != null,
                errorMessage = uiState.passwordErrorRes?.let { stringResource(it) },
                isPassword = true
            )

            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.height(MongleSpacing.sm))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Button(
            onClick = viewModel::submit,
            enabled = uiState.canSubmit,
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
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.email_auth_login_submit),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
private fun LoginField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    isError: Boolean,
    errorMessage: String?,
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
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
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
        }
    }
}
