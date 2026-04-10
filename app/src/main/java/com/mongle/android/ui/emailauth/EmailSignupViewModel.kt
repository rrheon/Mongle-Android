package com.mongle.android.ui.emailauth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.R
import com.mongle.android.domain.model.LegalDocType
import com.mongle.android.domain.model.LegalVersions
import com.mongle.android.domain.model.SocialLoginResult
import com.mongle.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 이메일 회원가입 플로우.
 *
 * phase: CONSENT → INPUT_FORM → VERIFY_CODE → (서버 signup) → EmailSignupEvent.Completed
 */
enum class EmailSignupPhase { CONSENT, INPUT_FORM, VERIFY_CODE }

private val EMAIL_REGEX = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
private val PASSWORD_SPECIAL_REGEX = Regex("""[^A-Za-z0-9]""")

data class EmailSignupUiState(
    val phase: EmailSignupPhase = EmailSignupPhase.CONSENT,
    val legalVersions: LegalVersions = LegalVersions(terms = "1.0.0", privacy = "1.0.0"),
    val requiredConsents: List<LegalDocType> = listOf(LegalDocType.TERMS, LegalDocType.PRIVACY),

    // 수집된 약관 버전 (consent 완료 후)
    val acceptedTermsVersion: String = "",
    val acceptedPrivacyVersion: String = "",

    // Input form
    val email: String = "",
    val password: String = "",
    /** 클라이언트 유효성 에러는 string resource ID — 화면에서 해석 */
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    val isSendingCode: Boolean = false,

    // Verify code
    val code: String = "",
    /** 서버 에러(이미 i18n 처리됨) 또는 null */
    val codeError: String? = null,
    val isVerifying: Boolean = false,
    val resendCooldownSec: Int = 0,

    val errorMessage: String? = null
) {
    val isEmailValid: Boolean get() = EMAIL_REGEX.matches(email.trim())
    val isPasswordValid: Boolean get() = password.length >= 10 && PASSWORD_SPECIAL_REGEX.containsMatchIn(password)
    val canSendCode: Boolean get() = isEmailValid && isPasswordValid && !isSendingCode
    val canSubmitCode: Boolean get() = code.length == 6 && !isVerifying
}

sealed class EmailSignupEvent {
    data class Completed(val result: SocialLoginResult) : EmailSignupEvent()
    data object Cancelled : EmailSignupEvent()
}

@HiltViewModel
class EmailSignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailSignupUiState())
    val uiState: StateFlow<EmailSignupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EmailSignupEvent>()
    val events: SharedFlow<EmailSignupEvent> = _events.asSharedFlow()

    private var resendTimerJob: Job? = null

    // region Input updates

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailErrorRes = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordErrorRes = null) }
    }

    fun onCodeChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(code = digits, codeError = null) }
    }

    // endregion

    fun onConsentCompleted(termsVersion: String, privacyVersion: String) {
        _uiState.update {
            it.copy(
                phase = EmailSignupPhase.INPUT_FORM,
                acceptedTermsVersion = termsVersion,
                acceptedPrivacyVersion = privacyVersion
            )
        }
    }

    fun sendCode() {
        val state = _uiState.value
        val emailErrRes = if (!state.isEmailValid) R.string.email_auth_email_invalid else null
        val pwErrRes = if (!state.isPasswordValid) R.string.email_auth_password_invalid else null
        if (emailErrRes != null || pwErrRes != null) {
            _uiState.update { it.copy(emailErrorRes = emailErrRes, passwordErrorRes = pwErrRes) }
            return
        }
        if (state.isSendingCode) return

        _uiState.update { it.copy(isSendingCode = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                authRepository.requestEmailSignupCode(email = state.email.trim())
                _uiState.update {
                    it.copy(
                        isSendingCode = false,
                        phase = EmailSignupPhase.VERIFY_CODE,
                        resendCooldownSec = 30
                    )
                }
                startResendTimer()
            } catch (e: Exception) {
                // 서버가 i18n 처리한 메시지 우선, 없으면 null 로 두고 Screen 에서 fallback 리소스 사용
                _uiState.update {
                    it.copy(
                        isSendingCode = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun resendCode() {
        val state = _uiState.value
        if (state.resendCooldownSec > 0 || state.isSendingCode) return
        _uiState.update { it.copy(isSendingCode = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                authRepository.requestEmailSignupCode(email = state.email.trim())
                _uiState.update { it.copy(isSendingCode = false, resendCooldownSec = 30) }
                startResendTimer()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSendingCode = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    private fun startResendTimer() {
        resendTimerJob?.cancel()
        resendTimerJob = viewModelScope.launch {
            while (_uiState.value.resendCooldownSec > 0) {
                delay(1000)
                _uiState.update { it.copy(resendCooldownSec = (it.resendCooldownSec - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun verifyAndSignup() {
        val state = _uiState.value
        if (!state.canSubmitCode) return
        _uiState.update { it.copy(isVerifying = true, codeError = null) }
        viewModelScope.launch {
            try {
                val result = authRepository.emailSignup(
                    email = state.email.trim(),
                    password = state.password,
                    code = state.code,
                    name = null,
                    termsVersion = state.acceptedTermsVersion,
                    privacyVersion = state.acceptedPrivacyVersion
                )
                resendTimerJob?.cancel()
                _uiState.update { it.copy(isVerifying = false) }
                _events.emit(EmailSignupEvent.Completed(result))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        codeError = e.message
                    )
                }
            }
        }
    }

    fun onBack() {
        val state = _uiState.value
        when (state.phase) {
            EmailSignupPhase.CONSENT -> {
                viewModelScope.launch { _events.emit(EmailSignupEvent.Cancelled) }
            }
            EmailSignupPhase.INPUT_FORM -> {
                _uiState.update { it.copy(phase = EmailSignupPhase.CONSENT) }
            }
            EmailSignupPhase.VERIFY_CODE -> {
                resendTimerJob?.cancel()
                _uiState.update { it.copy(phase = EmailSignupPhase.INPUT_FORM, code = "", codeError = null) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        resendTimerJob?.cancel()
        super.onCleared()
    }
}
