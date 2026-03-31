package com.mongle.android.ui.groupselect

import androidx.lifecycle.ViewModel
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

enum class GroupSelectStep { SELECT, CREATE, CREATED, JOIN }

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
    val selectedColorId: String = "calm"
)

@HiltViewModel
class GroupSelectViewModel @Inject constructor(
    private val familyRepository: ApiFamilyRepository,
    private val userRepository: ApiUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupSelectUiState())
    val uiState: StateFlow<GroupSelectUiState> = _uiState.asStateFlow()

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val groups = runCatching { familyRepository.getMyFamilies() }.getOrElse { emptyList() }
            _uiState.update { it.copy(groups = groups, isLoading = false) }
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
                // 닉네임을 사용자 이름으로 업데이트
                runCatching {
                    val me = userRepository.getMe()
                    userRepository.update(me.copy(name = state.nickname))
                }
                _uiState.update {
                    it.copy(inviteCode = group.inviteCode, step = GroupSelectStep.CREATED, isLoading = false)
                }
            }.onFailure { e ->
                val msg = e.message ?: "그룹 생성에 실패했어요"
                if (msg.contains("최대") || msg.contains("3개")) {
                    _uiState.update { it.copy(showMaxGroupsAlert = true, isLoading = false) }
                } else {
                    _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
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
                // 닉네임을 사용자 이름으로 업데이트
                runCatching {
                    val me = userRepository.getMe()
                    userRepository.update(me.copy(name = state.nickname))
                }
                _uiState.update { it.copy(isLoading = false) }
                onJoined()
            }.onFailure { e ->
                val msg = e.message ?: "참여에 실패했어요"
                if (msg.contains("최대") || msg.contains("3개")) {
                    _uiState.update { it.copy(showMaxGroupsAlert = true, isLoading = false) }
                } else if (msg.contains("유효하지") || msg.contains("찾을 수 없")) {
                    _uiState.update { it.copy(errorMessage = "유효하지 않은 초대 코드예요. 다시 확인해 주세요.", isLoading = false) }
                } else {
                    _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                }
            }
        }
    }

    fun goToCreate() { _uiState.update { it.copy(step = GroupSelectStep.CREATE) } }
    fun goToJoin(prefillCode: String = "") {
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
    fun dismissMaxGroupsAlert() { _uiState.update { it.copy(showMaxGroupsAlert = false) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
