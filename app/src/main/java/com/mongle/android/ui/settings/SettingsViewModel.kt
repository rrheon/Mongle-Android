package com.mongle.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AuthRepository
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.UserRepository
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
    val showLeaveGroupConfirmation: Boolean = false,
    val showTransferSheet: Boolean = false,
    val selectedTransferMemberId: java.util.UUID? = null,
    val showEditProfile: Boolean = false,
    val editName: String = "",
    val editRole: FamilyRole = FamilyRole.OTHER,
    val family: MongleGroup? = null,
    val familyMembers: List<User> = emptyList(),
    val errorMessage: String? = null
) {
    val isOwner: Boolean get() = family?.createdBy == currentUser?.id
    val transferCandidates: List<User> get() = familyMembers.filter { it.id != currentUser?.id }
}

sealed class SettingsEvent {
    data object Logout : SettingsEvent()
    data object AccountDeleted : SettingsEvent()
    data object LeftGroup : SettingsEvent()
    data class CopiedInviteCode(val code: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val mongleRepository: MongleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
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
        val user = _uiState.value.currentUser ?: return
        val name = _uiState.value.editName.trim()
        if (name.isBlank()) return

        _uiState.update { it.copy(isLoading = true, showEditProfile = false) }
        viewModelScope.launch {
            try {
                val updated = userRepository.update(
                    user.copy(name = name, role = _uiState.value.editRole)
                )
                _uiState.update { it.copy(currentUser = updated, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "프로필 업데이트에 실패했습니다.")
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
        _uiState.update { it.copy(showLeaveGroupConfirmation = true) }
    }

    fun onLeaveGroupConfirmed() {
        val state = _uiState.value
        _uiState.update { it.copy(showLeaveGroupConfirmation = false) }

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
                    it.copy(isLoading = false, errorMessage = e.message ?: "그룹 탈퇴에 실패했습니다.")
                }
            }
        }
    }

    fun onLeaveGroupCancelled() {
        _uiState.update { it.copy(showLeaveGroupConfirmation = false) }
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
                    it.copy(isLoading = false, errorMessage = e.message ?: "그룹 탈퇴에 실패했습니다.")
                }
            }
        }
    }

    fun onDismissTransferSheet() {
        _uiState.update { it.copy(showTransferSheet = false, selectedTransferMemberId = null) }
    }

    fun onKickMember(member: User) {
        viewModelScope.launch {
            try {
                mongleRepository.kickMember(member.id)
                _uiState.update { state ->
                    state.copy(familyMembers = state.familyMembers.filter { it.id != member.id })
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "멤버 내보내기에 실패했습니다.")
                }
            }
        }
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
