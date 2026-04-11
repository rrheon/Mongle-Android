package com.mongle.android.ui.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.data.remote.ApiUserRepository
import com.mongle.android.domain.model.BadgeDisplayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BadgesUiState(
    val items: List<BadgeDisplayItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val userRepository: ApiUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BadgesUiState())
    val uiState: StateFlow<BadgesUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        if (_uiState.value.loading) return
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { userRepository.getBadges() }
                .onSuccess { items ->
                    _uiState.update { it.copy(items = items, loading = false) }
                    // 진입 즉시 unseen 마킹: 팝업은 UI-7에서 처리하므로 여기서는 단순 마킹.
                    val unseen = items.mapNotNull { it.awarded }.filter { it.isUnseen }.map { it.code }
                    if (unseen.isNotEmpty()) {
                        runCatching { userRepository.markBadgesSeen(unseen) }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message) }
                }
        }
    }
}
