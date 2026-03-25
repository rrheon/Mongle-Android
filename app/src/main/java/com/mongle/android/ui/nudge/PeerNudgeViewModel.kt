package com.mongle.android.ui.nudge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.data.remote.ApiNudgeRepository
import com.mongle.android.data.remote.ApiUserRepository
import com.mongle.android.util.AdManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PeerNudgeUiState(
    val targetUserId: String = "",
    val targetUserName: String = "",
    val hearts: Int = 0,
    val isLoading: Boolean = false,
    val isSent: Boolean = false,
    val isWatchingAd: Boolean = false,
    val errorMessage: String? = null,
    val heartsRemaining: Int? = null
) {
    val hasEnoughHearts: Boolean get() = hearts >= 1
}

@HiltViewModel
class PeerNudgeViewModel @Inject constructor(
    private val nudgeRepository: ApiNudgeRepository,
    private val userRepository: ApiUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerNudgeUiState())
    val uiState: StateFlow<PeerNudgeUiState> = _uiState.asStateFlow()

    fun initialize(targetUserId: String, targetUserName: String, hearts: Int) {
        _uiState.update {
            it.copy(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                hearts = hearts
            )
        }
    }

    fun sendNudge() {
        val state = _uiState.value
        if (state.isLoading || state.isSent || state.targetUserId.isEmpty()) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val heartsRemaining = nudgeRepository.sendNudge(state.targetUserId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSent = true,
                        hearts = heartsRemaining,
                        heartsRemaining = heartsRemaining
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun watchAdForNudge(adManager: AdManager) {
        val state = _uiState.value
        if (state.isWatchingAd || state.isSent) return
        _uiState.update { it.copy(isWatchingAd = true, errorMessage = null) }
        adManager.showRewardedAd(
            onRewarded = {
                viewModelScope.launch {
                    try {
                        val heartsAfterAd = userRepository.grantAdHearts(1)
                        _uiState.update { it.copy(hearts = heartsAfterAd, isWatchingAd = false) }
                        val heartsRemaining = nudgeRepository.sendNudge(state.targetUserId)
                        _uiState.update {
                            it.copy(
                                isSent = true,
                                hearts = heartsRemaining,
                                heartsRemaining = heartsRemaining
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isWatchingAd = false, errorMessage = e.message) }
                    }
                }
            },
            onFailed = {
                _uiState.update { it.copy(isWatchingAd = false) }
            }
        )
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
