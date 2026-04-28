package com.mongle.android.ui.groupselect

import androidx.lifecycle.ViewModel
import com.mongle.android.ui.common.AppError
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.data.remote.ApiFamilyRepository
import com.mongle.android.data.remote.ApiUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GroupSelectStep { SELECT, CREATE, NOTIFICATION_PERMISSION, QUIET_HOURS, CREATED, JOIN }

data class GroupSelectUiState(
    val step: GroupSelectStep = GroupSelectStep.SELECT,
    val groups: List<MongleGroup> = emptyList(),
    val isLoading: Boolean = false,
    val groupName: String = "",
    val nickname: String = "",
    val joinCode: String = "",
    val inviteCode: String = "",
    val groupNameError: Boolean = false,
    val nicknameError: Boolean = false,
    val joinCodeError: Boolean = false,
    val showMaxGroupsAlert: Boolean = false,
    val errorMessage: String? = null,
    val selectedColorId: String = "calm",
    /** iOS MG-28 패리티 — long-click 으로 set 된 leave 대상 그룹 */
    val pendingLeaveGroup: MongleGroup? = null,
    /** 1차 확인 다이얼로그 노출 여부 */
    val showLeaveGroupConfirmation: Boolean = false,
    /** 2차 (마지막) 확인 다이얼로그 노출 여부 */
    val showLeaveGroupFinalConfirmation: Boolean = false
)

@HiltViewModel
class GroupSelectViewModel @Inject constructor(
    private val familyRepository: ApiFamilyRepository,
    private val userRepository: ApiUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupSelectUiState())
    val uiState: StateFlow<GroupSelectUiState> = _uiState.asStateFlow()

    // ─── 그룹 나가기 (iOS MG-28 패리티: long-click + 2단계 확인) ───────────────

    fun onGroupLongPressed(group: MongleGroup) {
        _uiState.update {
            it.copy(pendingLeaveGroup = group, showLeaveGroupConfirmation = true)
        }
    }

    fun dismissLeaveGroupConfirmation() {
        _uiState.update {
            it.copy(showLeaveGroupConfirmation = false, pendingLeaveGroup = null)
        }
    }

    fun onLeaveGroupFirstConfirmed() {
        _uiState.update {
            it.copy(showLeaveGroupConfirmation = false, showLeaveGroupFinalConfirmation = true)
        }
    }

    fun dismissLeaveGroupFinalConfirmation() {
        _uiState.update {
            it.copy(showLeaveGroupFinalConfirmation = false, pendingLeaveGroup = null)
        }
    }

    fun onLeaveGroupConfirmed(onLeft: () -> Unit) {
        val target = _uiState.value.pendingLeaveGroup ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    showLeaveGroupFinalConfirmation = false,
                    errorMessage = null
                )
            }
            runCatching {
                // 활성 family 를 leave 대상 그룹으로 전환 후 leave (서버 leaveFamily 는 active 그룹 대상)
                familyRepository.selectFamily(target.id)
                familyRepository.leaveFamily()
            }.onSuccess {
                _uiState.update {
                    it.copy(isLoading = false, pendingLeaveGroup = null)
                }
                loadGroups()
                onLeft()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingLeaveGroup = null,
                        errorMessage = AppError.from(e).toastMessage
                    )
                }
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { familyRepository.getMyFamilies() }
                .onSuccess { groups ->
                    _uiState.update { it.copy(groups = groups, isLoading = false) }
                }
                .onFailure { e ->
                    // 이전엔 emptyList() 로 silent fallback 하여 사용자가 "그룹이 없음"으로 오해.
                    // 빈 목록 + errorMessage 로 명확히 안내한다.
                    _uiState.update {
                        it.copy(groups = emptyList(), isLoading = false, errorMessage = AppError.from(e).toastMessage)
                    }
                }
        }
    }

    fun onGroupNameChanged(name: String) {
        _uiState.update { it.copy(groupName = name, groupNameError = false) }
    }

    fun onNicknameChanged(nickname: String) {
        _uiState.update { it.copy(nickname = nickname, nicknameError = false) }
    }

    fun onJoinCodeChanged(code: String) {
        _uiState.update { it.copy(joinCode = code, joinCodeError = false) }
    }

    fun onColorChanged(colorId: String) {
        _uiState.update { it.copy(selectedColorId = colorId) }
    }

    fun createGroup(onCreated: () -> Unit) {
        val state = _uiState.value
        if (state.groups.size >= 3) {
            _uiState.update { it.copy(showMaxGroupsAlert = true) }
            return
        }
        val nameEmpty = state.groupName.isBlank()
        val nickEmpty = state.nickname.isBlank()
        if (nameEmpty || nickEmpty) {
            _uiState.update { it.copy(groupNameError = nameEmpty, nicknameError = nickEmpty) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                familyRepository.createFamily(state.groupName, FamilyRole.OTHER)
            }.onSuccess { group ->
                // 닉네임과 캐릭터 색상을 사용자 정보에 업데이트
                runCatching {
                    val me = userRepository.getMe()
                    userRepository.update(me.copy(name = state.nickname, moodId = state.selectedColorId))
                }
                _uiState.update {
                    it.copy(inviteCode = group.inviteCode, step = GroupSelectStep.NOTIFICATION_PERMISSION, isLoading = false)
                }
            }.onFailure { e ->
                val appError = AppError.from(e)
                val rawMsg = (appError as? AppError.Domain)?.message ?: e.message ?: ""
                if (rawMsg.contains("최대") || rawMsg.contains("3개")) {
                    _uiState.update { it.copy(showMaxGroupsAlert = true, isLoading = false) }
                } else {
                    _uiState.update { it.copy(errorMessage = appError.toastMessage, isLoading = false) }
                }
            }
        }
    }

    fun joinWithCode(onJoined: () -> Unit) {
        val state = _uiState.value
        if (state.groups.size >= 3) {
            _uiState.update { it.copy(showMaxGroupsAlert = true) }
            return
        }
        val codeEmpty = state.joinCode.isBlank()
        val nickEmpty = state.nickname.isBlank()
        if (codeEmpty || nickEmpty) {
            _uiState.update { it.copy(joinCodeError = codeEmpty, nicknameError = nickEmpty) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                familyRepository.joinFamily(state.joinCode.uppercase(), FamilyRole.OTHER)
            }.onSuccess {
                // 닉네임과 캐릭터 색상을 사용자 정보에 업데이트
                runCatching {
                    val me = userRepository.getMe()
                    userRepository.update(me.copy(name = state.nickname, moodId = state.selectedColorId))
                }
                _uiState.update { it.copy(isLoading = false) }
                onJoined()
            }.onFailure { e ->
                val appError = AppError.from(e)
                val rawMsg = (appError as? AppError.Domain)?.message ?: e.message ?: ""
                if (rawMsg.contains("최대") || rawMsg.contains("3개")) {
                    _uiState.update { it.copy(showMaxGroupsAlert = true, isLoading = false) }
                } else if (rawMsg.contains("유효하지") || rawMsg.contains("찾을 수 없")) {
                    _uiState.update { it.copy(errorMessage = "유효하지 않은 초대 코드예요. 다시 확인해 주세요.", isLoading = false) }
                } else {
                    _uiState.update { it.copy(errorMessage = appError.toastMessage, isLoading = false) }
                }
            }
        }
    }

    fun goToCreate() {
        if (_uiState.value.groups.size >= 3) {
            _uiState.update { it.copy(showMaxGroupsAlert = true) }
            return
        }
        _uiState.update { it.copy(step = GroupSelectStep.CREATE) }
    }
    fun goToJoin(prefillCode: String = "") {
        if (_uiState.value.groups.size >= 3) {
            _uiState.update { it.copy(showMaxGroupsAlert = true) }
            return
        }
        _uiState.update { it.copy(step = GroupSelectStep.JOIN, joinCode = prefillCode) }
    }
    fun goBack() {
        _uiState.update {
            it.copy(
                step = GroupSelectStep.SELECT,
                groupName = "", nickname = "", joinCode = "",
                groupNameError = false, nicknameError = false, joinCodeError = false,
                errorMessage = null
            )
        }
    }
    fun resetToSelect() {
        _uiState.update {
            it.copy(
                step = GroupSelectStep.SELECT,
                groupName = "", nickname = "", joinCode = "", inviteCode = "",
                groupNameError = false, nicknameError = false, joinCodeError = false,
                errorMessage = null
            )
        }
    }
    // ─── Notification permission & quiet hours ────────────────────────────────

    fun goToNotificationPermission() {
        _uiState.update { it.copy(step = GroupSelectStep.NOTIFICATION_PERMISSION) }
    }

    fun onNotificationPermissionAllowed() {
        _uiState.update { it.copy(step = GroupSelectStep.QUIET_HOURS) }
    }

    fun onNotificationPermissionSkipped() {
        _uiState.update { it.copy(step = GroupSelectStep.QUIET_HOURS) }
    }

    fun onQuietHoursAccepted() {
        _uiState.update { it.copy(step = GroupSelectStep.CREATED) }
    }

    fun onQuietHoursSkipped() {
        _uiState.update { it.copy(step = GroupSelectStep.CREATED) }
    }

    fun dismissMaxGroupsAlert() { _uiState.update { it.copy(showMaxGroupsAlert = false) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
