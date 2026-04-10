package com.mongle.android.ui.consent

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mongle.android.R
import com.mongle.android.domain.model.LegalDocType
import com.mongle.android.domain.model.LegalVersions
import com.mongle.android.ui.theme.MongleBackgroundLight
import com.mongle.android.ui.theme.MongleBorder
import com.mongle.android.ui.theme.MongleCardBackgroundSolid
import com.mongle.android.ui.theme.MonglePrimary
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleSpacing
import com.mongle.android.ui.theme.MongleTextHint
import com.mongle.android.ui.theme.MongleTextPrimary
import com.mongle.android.ui.theme.MongleTextSecondary

@Composable
fun ConsentScreen(
    requiredConsents: List<LegalDocType>,
    legalVersions: LegalVersions,
    onCompleted: () -> Unit,
    onBack: () -> Unit,
    /** 이메일 회원가입 이전 단계에서 호출될 때 true. 서버 호출 없이 버전만 상위로 전달. */
    preSignup: Boolean = false,
    onPreSignupCompleted: ((termsVersion: String, privacyVersion: String) -> Unit)? = null,
    viewModel: ConsentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    LaunchedEffect(requiredConsents, legalVersions, preSignup) {
        viewModel.setContext(requiredConsents, legalVersions, preSignup = preSignup)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConsentEvent.Completed -> onCompleted()
                is ConsentEvent.PreSignupCompleted ->
                    onPreSignupCompleted?.invoke(event.termsVersion, event.privacyVersion)
            }
        }
    }

    fun openUrl(url: String) {
        runCatching {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        }.onFailure {
            // Custom Tabs 미설치 시 일반 브라우저 폴백 (브라우저도 없으면 조용히 무시)
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MongleBackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 상단 바 (뒤로가기)
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MongleSpacing.xl)
            ) {

            Text(
                text = stringResource(R.string.consent_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MongleTextPrimary
            )
            Spacer(Modifier.height(MongleSpacing.sm))
            Text(
                text = stringResource(R.string.consent_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextSecondary
            )

            Spacer(Modifier.height(MongleSpacing.lg))

            // 동의 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MongleCardBackgroundSolid, RoundedCornerShape(MongleRadius.large))
                    .padding(vertical = MongleSpacing.md)
            ) {
                AllAgreeRow(
                    checked = uiState.allAgreed,
                    onClick = { viewModel.toggleAll(!uiState.allAgreed) }
                )

                Divider(
                    modifier = Modifier.padding(
                        horizontal = MongleSpacing.md,
                        vertical = MongleSpacing.sm
                    ),
                    color = MongleBorder
                )

                ConsentRow(
                    title = stringResource(R.string.consent_age),
                    checked = uiState.ageAgreed,
                    onTap = { viewModel.toggleAge() },
                    onLink = null
                )

                ConsentRow(
                    title = stringResource(R.string.consent_terms),
                    checked = uiState.termsAgreed,
                    onTap = { viewModel.toggleTerms() },
                    onLink = { openUrl(LegalLinks.termsUrl()) }
                )

                ConsentRow(
                    title = stringResource(R.string.consent_privacy),
                    checked = uiState.privacyAgreed,
                    onTap = { viewModel.togglePrivacy() },
                    onLink = { openUrl(LegalLinks.privacyUrl()) }
                )
            }

            Spacer(Modifier.weight(1f))

            uiState.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = MongleSpacing.sm)
                )
            }

            Button(
                onClick = { viewModel.submit() },
                enabled = uiState.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
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
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.consent_submit),
                        fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
            } // 내부 padding Column 닫기
        }
    }
}

@Composable
private fun AllAgreeRow(checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckIcon(checked = checked)
        Spacer(Modifier.width(MongleSpacing.sm))
        Text(
            text = stringResource(R.string.consent_all),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MongleTextPrimary
        )
    }
}

@Composable
private fun ConsentRow(
    title: String,
    checked: Boolean,
    onTap: () -> Unit,
    onLink: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MongleSpacing.md, vertical = MongleSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onTap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckIcon(checked = checked)
            Spacer(Modifier.width(MongleSpacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MongleTextPrimary
            )
        }

        if (onLink != null) {
            TextButton(onClick = onLink, contentPadding = PaddingValues(horizontal = MongleSpacing.xs)) {
                Text(
                    text = stringResource(R.string.consent_view),
                    style = MaterialTheme.typography.bodySmall,
                    color = MongleTextHint
                )
            }
        }
    }
}

@Composable
private fun CheckIcon(checked: Boolean) {
    if (checked) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MonglePrimary,
            modifier = Modifier.size(24.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Circle,
            contentDescription = null,
            tint = MongleBorder,
            modifier = Modifier.size(24.dp)
        )
    }
}

