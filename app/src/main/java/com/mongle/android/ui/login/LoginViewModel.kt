package com.mongle.android.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.SocialLoginCredential
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
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

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val isSignUp: Boolean = false
)

sealed class LoginEvent {
    data class LoggedIn(val user: User, val providerType: SocialProviderType?) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun toggleSignUp() {
        _uiState.update { it.copy(isSignUp = !it.isSignUp, errorMessage = null) }
    }

    fun loginWithEmail() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "이메일과 비밀번호를 입력해주세요.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = authRepository.login(state.email, state.password)
                _events.emit(LoginEvent.LoggedIn(user, null))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "로그인에 실패했습니다.") }
            }
        }
    }

    fun signupWithEmail() {
        val state = _uiState.value
        if (state.name.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "모든 항목을 입력해주세요.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = authRepository.signup(state.name, state.email, state.password, com.mongle.android.domain.model.FamilyRole.OTHER)
                _events.emit(LoginEvent.LoggedIn(user, null))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "회원가입에 실패했습니다.") }
            }
        }
    }

    fun loginWithSocial(credential: SocialLoginCredential) {
        Log.d("LoginViewModel", "소셜 로그인 시작 | provider=${credential.providerType.value} | fields=${credential.fields.keys}")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = authRepository.socialLogin(credential)
                Log.d("LoginViewModel", "소셜 로그인 성공 | userId=${user.id} | name=${user.name}")
                _events.emit(LoginEvent.LoggedIn(user, credential.providerType))
            } catch (e: Exception) {
                Log.e("LoginViewModel", "소셜 로그인 실패 | provider=${credential.providerType.value} | error=${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "${credential.providerType.value} 로그인에 실패했습니다."
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
