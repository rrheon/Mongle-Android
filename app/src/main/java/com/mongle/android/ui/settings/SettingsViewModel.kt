package com.mongle.android.ui.settings

import android.app.Activity
import com.mongle.android.ui.common.AppError
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AuthRepository
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.UserRepository
import com.mongle.android.ui.login.revokeGoogleAccess
import com.mongle.android.ui.login.unlinkKakao
import com.mongle.android.util.ConsentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

data class SettingsUiState(
    val currentUser: User? = null,
    val loginProviderType: SocialProviderType? = null,
    val appVersion: String = "1.0.0",
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val showLogoutConfirmation: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val showDeleteAccountFinalConfirmation: Boolean = false,
    val showLeaveGroupConfirmation: Boolean = false,
    val showLeaveGroupFinalConfirmation: Boolean = false,
    val showTransferSheet: Boolean = false,
    val selectedTransferMemberId: java.util.UUID? = null,
    val showKickConfirmation: Boolean = false,
    val kickTargetMember: User? = null,
    val showEditProfile: Boolean = false,
    val editName: String = "",
    val editRole: FamilyRole = FamilyRole.OTHER,
    val family: MongleGroup? = null,
    val familyMembers: List<User> = emptyList(),
    val errorMessage: String? = null,
    /** UMP(GDPR/CCPA) 대상 사용자에게만 "광고 개인정보 옵션" 행을 노출한다. */
    val showPrivacyOptionsRow: Boolean = false
) {
    val isOwner: Boolean get() = family?.createdBy == currentUser?.id
    val transferCandidates: List<User> get() = familyMembers.filter { it.id != currentUser?.id }

    /** 이름 변경 가능 여부: 마지막 변경으로부터 7일 경과 시 true */
    val canChangeName: Boolean get() {
        val lastChanged = currentUser?.lastNameChangedAt ?: return true
        val daysSinceChange = (System.currentTimeMillis() - lastChanged) / (24L * 60 * 60 * 1000)
        return daysSinceChange >= 7
    }

    /** 다음 이름 변경 가능일까지 남은 일수 */
    val daysUntilNameChange: Int get() {
        val lastChanged = currentUser?.lastNameChangedAt ?: return 0
        val daysSinceChange = (System.currentTimeMillis() - lastChanged) / (24L * 60 * 60 * 1000)
        return (7 - daysSinceChange).coerceAtLeast(0).toInt()
    }
}

sealed class SettingsEvent {
    data object Logout : SettingsEvent()
    data object AccountDeleted : SettingsEvent()
    data object LeftGroup : SettingsEvent()
    data class CopiedInviteCode(val code: String) : SettingsEvent()
    /** 그룹장이 생성 후 3일(72시간)이 지나지 않아 나가기가 차단된 경우 */
    data class LeaveTooSoon(val daysLeft: Int) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val mongleRepository: MongleRepository,
    private val consentManager: ConsentManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // MG-97 collect 단절 사이 이벤트 유실 방지.
    private val _events = MutableSharedFlow<SettingsEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun initialize(user: User?, providerType: SocialProviderType?) {
        _uiState.update {
            it.copy(
                currentUser = user,
                loginProviderType = providerType,
                editName = user?.name ?: "",
                editRole = user?.role ?: FamilyRole.OTHER
            )
        }
        loadFamilyInfo()
    }

    private fun loadFamilyInfo() {
        viewModelScope.launch {
            val result = runCatching { mongleRepository.getMyFamily() }.getOrNull()
            _uiState.update {
                it.copy(
                    family = result?.first,
                    familyMembers = result?.second ?: emptyList()
                )
            }
        }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    // ── 프로필 편집 ──────────────────────────────────────────

    fun onEditProfileTapped() {
        val user = _uiState.value.currentUser ?: return
        _uiState.update {
            it.copy(showEditProfile = true, editName = user.name, editRole = user.role)
        }
    }

    fun onEditNameChanged(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun onEditRoleChanged(role: FamilyRole) {
        _uiState.update { it.copy(editRole = role) }
    }

    fun onEditProfileConfirmed() {
        val state = _uiState.value
        val user = state.currentUser ?: return
        val name = state.editName.trim()
        if (name.isBlank()) return

        val nameChanged = name != user.name
        if (nameChanged && !state.canChangeName) {
            _uiState.update {
                it.copy(errorMessage = "이름은 일주일에 한 번만 변경할 수 있어요. ${state.daysUntilNameChange}일 후에 다시 시도해 주세요.")
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, showEditProfile = false) }
        viewModelScope.launch {
            try {
                val nameChangedAt = if (nameChanged) System.currentTimeMillis() else user.lastNameChangedAt
                val updated = userRepository.update(
                    user.copy(name = name, role = state.editRole, lastNameChangedAt = nameChangedAt)
                )
                // moodId(캐릭터 색상)는 이름 변경 시 유지 - 서버 응답에 없을 경우 기존 값 보존
                _uiState.update {
                    it.copy(
                        currentUser = updated.copy(
                            moodId = updated.moodId ?: user.moodId,
                            lastNameChangedAt = nameChangedAt
                        ),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = AppError.from(e).toastMessage)
                }
            }
        }
    }

    fun onEditProfileCancelled() {
        _uiState.update { it.copy(showEditProfile = false) }
    }

    // ── 그룹 관리 ──────────────────────────────────────────

    fun onCopyInviteCode() {
        val code = _uiState.value.family?.inviteCode ?: return
        viewModelScope.launch {
            _events.emit(SettingsEvent.CopiedInviteCode(code))
        }
    }

    fun onLeaveGroupTapped() {
        val state = _uiState.value
        // 그룹장인 경우에만 생성 후 3일(72시간) 쿨다운 체크
        // (일반 멤버나 위임 후 나가기는 제한 없음)
        if (state.isOwner) {
            val family = state.family
            if (family != null) {
                val hoursSinceCreation =
                    (System.currentTimeMillis() - family.createdAt.time) / (1000.0 * 60 * 60)
                if (hoursSinceCreation < 72) {
                    val daysLeft = kotlin.math.ceil((72 - hoursSinceCreation) / 24.0).toInt()
                    viewModelScope.launch {
                        _events.emit(SettingsEvent.LeaveTooSoon(daysLeft))
                    }
                    return
                }
            }
        }
        _uiState.update { it.copy(showLeaveGroupConfirmation = true) }
    }

    fun onLeaveGroupFirstConfirmed() {
        _uiState.update { it.copy(showLeaveGroupConfirmation = false, showLeaveGroupFinalConfirmation = true) }
    }

    fun onLeaveGroupFinalCancelled() {
        _uiState.update { it.copy(showLeaveGroupFinalConfirmation = false) }
    }

    fun onLeaveGroupConfirmed() {
        val state = _uiState.value
        _uiState.update { it.copy(showLeaveGroupConfirmation = false, showLeaveGroupFinalConfirmation = false) }

        if (state.isOwner && state.transferCandidates.isNotEmpty()) {
            // 방장이고 다른 멤버가 있으면 위임 시트 표시
            _uiState.update { it.copy(showTransferSheet = true) }
            return
        }

        // 일반 멤버이거나 방장 혼자인 경우 바로 나가기
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                mongleRepository.leaveFamily()
                _uiState.update { it.copy(isLoading = false, family = null, familyMembers = emptyList()) }
                _events.emit(SettingsEvent.LeftGroup)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = AppError.from(e).toastMessage)
                }
            }
        }
    }

    fun onLeaveGroupCancelled() {
        _uiState.update { it.copy(showLeaveGroupConfirmation = false, showLeaveGroupFinalConfirmation = false) }
    }

    fun onTransferMemberSelected(userId: java.util.UUID) {
        _uiState.update { it.copy(selectedTransferMemberId = userId) }
    }

    fun onConfirmTransferAndLeave() {
        val newCreatorId = _uiState.value.selectedTransferMemberId ?: return
        _uiState.update { it.copy(showTransferSheet = false, isLoading = true) }
        viewModelScope.launch {
            try {
                mongleRepository.transferCreator(newCreatorId)
                mongleRepository.leaveFamily()
                _uiState.update { it.copy(isLoading = false, family = null, familyMembers = emptyList()) }
                _events.emit(SettingsEvent.LeftGroup)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = AppError.from(e).toastMessage)
                }
            }
        }
    }

    fun onDismissTransferSheet() {
        _uiState.update { it.copy(showTransferSheet = false, selectedTransferMemberId = null) }
    }

    fun onKickMemberTapped(member: User) {
        _uiState.update { it.copy(showKickConfirmation = true, kickTargetMember = member) }
    }

    fun onKickMemberConfirmed() {
        val member = _uiState.value.kickTargetMember ?: return
        _uiState.update { it.copy(showKickConfirmation = false, kickTargetMember = null) }
        viewModelScope.launch {
            try {
                mongleRepository.kickMember(member.id)
                _uiState.update { state ->
                    state.copy(familyMembers = state.familyMembers.filter { it.id != member.id })
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = AppError.from(e).toastMessage)
                }
            }
        }
    }

    fun onKickMemberCancelled() {
        _uiState.update { it.copy(showKickConfirmation = false, kickTargetMember = null) }
    }

    // ── 로그아웃 ──────────────────────────────────────────

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

    // ── 계정 삭제 ──────────────────────────────────────────

    fun onDeleteAccountTapped() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = true) }
    }

    fun onDeleteAccountFirstConfirmed() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = false, showDeleteAccountFinalConfirmation = true) }
    }

    fun onDeleteAccountFinalCancelled() {
        _uiState.update { it.copy(showDeleteAccountFinalConfirmation = false) }
    }

    fun onDeleteAccountConfirmed() {
        val providerType = _uiState.value.loginProviderType
        _uiState.update { it.copy(showDeleteAccountConfirmation = false, showDeleteAccountFinalConfirmation = false, isLoading = true) }
        viewModelScope.launch {
            try {
                // 소셜 연결 해제 (iOS의 revokeClientSocialAccess와 동일, 실패해도 계정 삭제 진행)
                when (providerType) {
                    SocialProviderType.KAKAO -> runCatching { unlinkKakao() }
                    SocialProviderType.GOOGLE -> runCatching { revokeGoogleAccess(context) }
                    else -> {}
                }
                authRepository.deleteAccount()
                _events.emit(SettingsEvent.AccountDeleted)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = AppError.from(e).toastMessage)
                }
            }
        }
    }

    fun onDeleteAccountCancelled() {
        _uiState.update { it.copy(showDeleteAccountConfirmation = false, showDeleteAccountFinalConfirmation = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── 광고 개인정보 옵션 (UMP) ──────────────────────────────

    /** UMP 대상 여부에 따라 "광고 개인정보 옵션" 행 노출 여부를 갱신한다. */
    fun refreshPrivacyOptionsVisibility(activity: Activity) {
        val required = consentManager.isPrivacyOptionsRequired(activity)
        _uiState.update { it.copy(showPrivacyOptionsRow = required) }
    }

    /** UMP 동의 재설정 폼을 노출한다. */
    fun onPrivacyOptionsTapped(activity: Activity) {
        consentManager.showPrivacyOptionsForm(activity) { errorMessage ->
            if (errorMessage != null) {
                _uiState.update { it.copy(errorMessage = errorMessage) }
            }
        }
    }
}
