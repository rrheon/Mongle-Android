package com.mongle.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class SettingsUiState(
    val currentUser: User? = null,
    val loginProviderType: SocialProviderType? = null,
    val appVersion: String = "1.0.0",
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val showLogoutConfirmation: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val errorMessage: String? = null
)

sealed class SettingsEvent {
    data object Logout : SettingsEvent()
    data object AccountDeleted : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun initialize(user: User?, providerType: SocialProviderType?) {
        _uiState.update { it.copy(currentUser = user, loginProviderType = providerType) }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun onLogoutTapped() {
        _uiState.update { it.copy(showLogoutConfirmation = true) }
    }

    fun onLogoutConfirmed() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
        viewModelScope.launch {
            _events.emit(SettingsEvent.Logout)
        }
    }

    fun onLogoutCancelled() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    fun onDeleteAccountTapped() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = true) }
    }

    fun onDeleteAccountConfirmed() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = false, isLoading = true) }
        viewModelScope.launch {
            try {
                authRepository.deleteAccount()
                _events.emit(SettingsEvent.AccountDeleted)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "계정 삭제에 실패했습니다.")
                }
            }
        }
    }

    fun onDeleteAccountCancelled() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
