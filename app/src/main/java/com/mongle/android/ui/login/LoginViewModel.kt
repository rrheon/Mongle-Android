package com.mongle.android.ui.login

import android.util.Log
import com.mongle.android.ui.common.AppError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.LegalDocType
import com.mongle.android.domain.model.LegalVersions
import com.mongle.android.domain.model.SocialLoginCredential
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class LoginEvent {
    /**
     * 로그인 성공.
     * @param needsConsent true 면 RootViewModel 이 ConsentScreen 으로 라우팅한다.
     */
    data class LoggedIn(
        val user: User,
        val providerType: SocialProviderType?,
        val needsConsent: Boolean,
        val requiredConsents: List<LegalDocType>,
        val legalVersions: LegalVersions
    ) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // MG-97 collect 단절 사이 이벤트 유실 방지 (회전·메모리 부족).
    private val _events = MutableSharedFlow<LoginEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun loginWithSocial(credential: SocialLoginCredential) {
        Log.d("LoginViewModel", "소셜 로그인 시작 | provider=${credential.providerType.value} | fields=${credential.fields.keys}")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = authRepository.socialLogin(credential)
                Log.d("LoginViewModel", "소셜 로그인 성공 | userId=${result.user.id} | name=${result.user.name} | needsConsent=${result.needsConsent}")
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(
                    LoginEvent.LoggedIn(
                        user = result.user,
                        providerType = credential.providerType,
                        needsConsent = result.needsConsent,
                        requiredConsents = result.requiredConsents,
                        legalVersions = result.legalVersions
                    )
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "소셜 로그인 실패 | provider=${credential.providerType.value} | error=${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = AppError.from(e).toastMessage
                    )
                }
            }
        }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
