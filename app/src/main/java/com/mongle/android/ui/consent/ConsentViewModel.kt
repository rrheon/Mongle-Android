package com.mongle.android.ui.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.LegalDocType
import com.mongle.android.domain.model.LegalVersions
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

data class ConsentUiState(
    val ageAgreed: Boolean = false,
    val termsAgreed: Boolean = false,
    val privacyAgreed: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val requiredConsents: List<LegalDocType> = listOf(LegalDocType.TERMS, LegalDocType.PRIVACY),
    val legalVersions: LegalVersions = LegalVersions(terms = "1.0.0", privacy = "1.0.0"),
    /** preSignup 모드: 이메일 회원가입 이전 단계 — 서버 호출 없이 버전만 수집해 상위로 전달 */
    val preSignup: Boolean = false
) {
    val allAgreed: Boolean get() = ageAgreed && termsAgreed && privacyAgreed
    val canSubmit: Boolean get() = allAgreed && !isSubmitting
}

sealed class ConsentEvent {
    data object Completed : ConsentEvent()
    /** preSignup 완료 → 수집된 버전을 상위 composable 로 전달 */
    data class PreSignupCompleted(val termsVersion: String, val privacyVersion: String) : ConsentEvent()
}

@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ConsentEvent>()
    val events: SharedFlow<ConsentEvent> = _events.asSharedFlow()

    fun setContext(
        requiredConsents: List<LegalDocType>,
        legalVersions: LegalVersions,
        preSignup: Boolean = false
    ) {
        _uiState.update {
            it.copy(
                requiredConsents = requiredConsents,
                legalVersions = legalVersions,
                preSignup = preSignup
            )
        }
    }

    fun toggleAll(on: Boolean) {
        _uiState.update { it.copy(ageAgreed = on, termsAgreed = on, privacyAgreed = on) }
    }

    fun toggleAge() = _uiState.update { it.copy(ageAgreed = !it.ageAgreed) }
    fun toggleTerms() = _uiState.update { it.copy(termsAgreed = !it.termsAgreed) }
    fun togglePrivacy() = _uiState.update { it.copy(privacyAgreed = !it.privacyAgreed) }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        // preSignup 모드: 아직 계정이 없으므로 서버 호출 없이 버전만 상위로 전달
        if (state.preSignup) {
            viewModelScope.launch {
                _events.emit(
                    ConsentEvent.PreSignupCompleted(
                        termsVersion = state.legalVersions.terms,
                        privacyVersion = state.legalVersions.privacy
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                authRepository.submitConsent(
                    termsVersion = state.legalVersions.terms,
                    privacyVersion = state.legalVersions.privacy
                )
                _uiState.update { it.copy(isSubmitting = false) }
                _events.emit(ConsentEvent.Completed)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "동의 저장에 실패했습니다."
                    )
                }
            }
        }
    }
}
