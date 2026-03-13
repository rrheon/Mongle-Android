package com.mongle.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.MongleRepository
import com.mongle.android.domain.repository.QuestionRepository
import com.mongle.android.domain.repository.TreeRepository
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

data class HomeUiState(
    val todayQuestion: Question? = null,
    val familyTree: TreeProgress = TreeProgress(),
    val family: MongleGroup? = null,
    val familyMembers: List<User> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasAnsweredToday: Boolean = false,
    val errorMessage: String? = null
) {
    val hasFamily: Boolean get() = family != null
}

sealed class HomeEvent {
    data class NavigateToQuestionDetail(val question: Question) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val mongleRepository: MongleRepository,
    private val treeRepository: TreeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun initialize(
        todayQuestion: Question?,
        familyTree: TreeProgress,
        family: MongleGroup?,
        familyMembers: List<User>,
        currentUser: User?,
        hasAnsweredToday: Boolean
    ) {
        _uiState.update {
            it.copy(
                todayQuestion = todayQuestion,
                familyTree = familyTree,
                family = family,
                familyMembers = familyMembers,
                currentUser = currentUser,
                hasAnsweredToday = hasAnsweredToday
            )
        }
    }

    fun onQuestionTapped() {
        val question = _uiState.value.todayQuestion ?: return
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToQuestionDetail(question))
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val familyResult = runCatching { mongleRepository.getMyFamily() }.getOrNull()
                val question = runCatching { questionRepository.getTodayQuestion() }.getOrNull()
                val tree = runCatching { treeRepository.getMyTreeProgress() }.getOrElse { TreeProgress() }

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        todayQuestion = question,
                        familyTree = tree ?: TreeProgress(),
                        family = familyResult?.first,
                        familyMembers = familyResult?.second ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isRefreshing = false, errorMessage = e.message)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onAnswerSubmitted() {
        _uiState.update { it.copy(hasAnsweredToday = true) }
    }
}
