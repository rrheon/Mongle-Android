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
    val errorMessage: String? = null
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
