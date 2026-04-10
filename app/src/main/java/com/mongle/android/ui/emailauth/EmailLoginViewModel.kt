package com.mongle.android.ui.emailauth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ycompany.Monggle.R
import com.mongle.android.domain.model.SocialLoginResult
import com.mongle.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EMAIL_REGEX = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

data class EmailLoginUiState(
    val email: String = "",
    val password: String = "",
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    /** 서버 에러(이미 i18n 처리됨) 또는 null */
    val errorMessage: String? = null
) {
    val isEmailValid: Boolean get() = EMAIL_REGEX.matches(email.trim())
    val isPasswordNonEmpty: Boolean get() = password.isNotEmpty()
    val canSubmit: Boolean get() = isEmailValid && isPasswordNonEmpty && !isSubmitting
}

sealed class EmailLoginEvent {
    data class Completed(val result: SocialLoginResult) : EmailLoginEvent()
    data object Cancelled : EmailLoginEvent()
}

@HiltViewModel
class EmailLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailLoginUiState())
    val uiState: StateFlow<EmailLoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EmailLoginEvent>()
    val events: SharedFlow<EmailLoginEvent> = _events.asSharedFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailErrorRes = null, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordErrorRes = null, errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        val emailErr = if (!state.isEmailValid) R.string.email_auth_email_invalid else null
        val pwErr = if (!state.isPasswordNonEmpty) R.string.email_auth_login_password_required else null
        if (emailErr != null || pwErr != null) {
            _uiState.update { it.copy(emailErrorRes = emailErr, passwordErrorRes = pwErr) }
            return
        }
        if (state.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = authRepository.emailLogin(
                    email = state.email.trim(),
                    password = state.password
                )
                _uiState.update { it.copy(isSubmitting = false) }
                _events.emit(EmailLoginEvent.Completed(result))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        // 서버가 i18n 처리한 메시지 우선; 없으면 화면에서 fallback 리소스 사용
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(EmailLoginEvent.Cancelled) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
