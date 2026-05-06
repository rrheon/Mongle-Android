package com.mongle.android.ui.nudge

import androidx.lifecycle.ViewModel
import com.mongle.android.ui.common.AppError
import androidx.lifecycle.viewModelScope
import com.mongle.android.data.remote.ApiNudgeRepository
import com.mongle.android.data.remote.ApiUserRepository
import com.mongle.android.util.AdManager
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

data class PeerNudgeUiState(
    val targetUserId: String = "",
    val targetUserName: String = "",
    val hearts: Int = 0,
    val isLoading: Boolean = false,
    val isWatchingAd: Boolean = false
) {
    val hasEnoughHearts: Boolean get() = hearts >= 1
}

// MG-116 — 토스트 트리거를 state(sentCount/errorMessage) 가 아닌 one-shot 이벤트로 발행한다.
// state 의존 LaunchedEffect 는 화면 재진입 시 stale 값으로 코루틴이 launch 되는 race 가 있어
// 직전 결과 토스트가 다시 노출되는 회귀를 야기했다. iOS PeerNudgeFeature.Delegate.nudgeSent 패턴 패리티.
sealed interface PeerNudgeEvent {
    data class NudgeSent(val heartsRemaining: Int) : PeerNudgeEvent
    data class Error(val message: String) : PeerNudgeEvent
}

@HiltViewModel
class PeerNudgeViewModel @Inject constructor(
    private val nudgeRepository: ApiNudgeRepository,
    private val userRepository: ApiUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerNudgeUiState())
    val uiState: StateFlow<PeerNudgeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PeerNudgeEvent>()
    val events: SharedFlow<PeerNudgeEvent> = _events.asSharedFlow()

    fun initialize(targetUserId: String, targetUserName: String, hearts: Int) {
        _uiState.update {
            PeerNudgeUiState(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                hearts = hearts
            )
        }
    }

    fun sendNudge() {
        val state = _uiState.value
        if (state.isLoading || state.targetUserId.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val heartsRemaining = nudgeRepository.sendNudge(state.targetUserId)
                _uiState.update { it.copy(isLoading = false, hearts = heartsRemaining) }
                _events.emit(PeerNudgeEvent.NudgeSent(heartsRemaining))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(PeerNudgeEvent.Error(AppError.from(e).toastMessage))
            }
        }
    }

    fun watchAdForNudge(adManager: AdManager) {
        val state = _uiState.value
        if (state.isWatchingAd) return
        _uiState.update { it.copy(isWatchingAd = true) }
        adManager.showRewardedAd(
            onRewarded = {
                viewModelScope.launch {
                    try {
                        // iOS MG-34 패리티 — AdRewardClient retry 적용
                        val heartsAfterAd = com.mongle.android.data.remote.AdRewardClient.grantAdHearts(userRepository, 1)
                        _uiState.update { it.copy(hearts = heartsAfterAd, isWatchingAd = false) }
                        val heartsRemaining = nudgeRepository.sendNudge(state.targetUserId)
                        _uiState.update { it.copy(hearts = heartsRemaining) }
                        _events.emit(PeerNudgeEvent.NudgeSent(heartsRemaining))
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isWatchingAd = false) }
                        _events.emit(PeerNudgeEvent.Error(AppError.from(e).toastMessage))
                    }
                }
            },
            onFailed = {
                _uiState.update { it.copy(isWatchingAd = false) }
            }
        )
    }
}
