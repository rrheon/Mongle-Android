package com.mongle.android.ui.question

import androidx.lifecycle.ViewModel
import com.mongle.android.ui.common.AppError
import androidx.lifecycle.viewModelScope
import com.mongle.android.data.remote.AdRewardClient
import com.mongle.android.data.remote.ApiUserRepository
import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AnswerRepository
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
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class FamilyAnswer(
    val id: UUID = UUID.randomUUID(),
    val user: User,
    val answer: Answer
)

data class QuestionDetailUiState(
    val question: Question? = null,
    val currentUser: User? = null,
    val familyMembers: List<User> = emptyList(),
    val myAnswer: Answer? = null,
    val familyAnswers: List<FamilyAnswer> = emptyList(),
    val answerText: String = "",
    val selectedMoodIndex: Int? = null,
    val showMoodRequiredAlert: Boolean = false,
    val showEditConfirmDialog: Boolean = false,
    val showEditAdDialog: Boolean = false,
    val hearts: Int = 0,
    val isWatchingAd: Boolean = false,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val hasMyAnswer: Boolean get() = myAnswer != null
    val isValidAnswer: Boolean get() = answerText.trim().isNotEmpty() && selectedMoodIndex != null
    val hasEnoughHeartsForEdit: Boolean get() = hearts >= 1
}

sealed class QuestionDetailEvent {
    data class AnswerSubmitted(val answer: Answer, val isNewAnswer: Boolean) : QuestionDetailEvent()
    data object Closed : QuestionDetailEvent()
}

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    private val answerRepository: AnswerRepository,
    private val userRepository: ApiUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuestionDetailEvent>()
    val events: SharedFlow<QuestionDetailEvent> = _events.asSharedFlow()

    private var initialized = false

    fun initialize(question: Question, currentUser: User?, familyMembers: List<User> = emptyList(), hearts: Int = 0) {
        if (initialized) return
        initialized = true
        _uiState.update {
            it.copy(
                question = question,
                currentUser = currentUser,
                familyMembers = familyMembers,
                hearts = hearts,
                isLoading = true
            )
        }
        loadAnswers(question, currentUser)
        // 부모에서 캡처한 hearts 는 stale 일 수 있음(다른 단말에서 답변/스킵/edit 으로 변경됨).
        // 진입 시 서버에서 최신 값을 가져와 editCostPopup/adReward 분기가 올바른 잔량으로 동작하게 한다.
        refreshHearts()
    }

    /**
     * 화면 진입 또는 재진입(onResume) 시 서버에서 최신 hearts 를 받아 갱신.
     * 다중 단말 동기화 필요 (iOS MG-49 패리티).
     */
    fun refreshHearts() {
        viewModelScope.launch {
            runCatching { userRepository.getMe() }
                .onSuccess { me -> _uiState.update { it.copy(hearts = me.hearts) } }
        }
    }

    private fun loadAnswers(question: Question, currentUser: User?) {
        viewModelScope.launch {
            try {
                // 서버 Answer 테이블은 question(콘텐츠) ID로 연결되므로 question.id 사용
                // (iOS와 동일 — dailyQuestionId가 아닌 question.id를 전송해야 함)
                val questionId = question.id
                val allAnswers = answerRepository.getByDailyQuestion(questionId)
                val myAnswer = currentUser?.id?.let {
                    runCatching { answerRepository.getByUserAndDailyQuestion(questionId, it) }.getOrNull()
                }

                val members = _uiState.value.familyMembers
                // O(F + A) — answer 마다 O(F) firstOrNull 선형 탐색하던 nested loop 를
                // dict 인덱스 1회 구축으로 대체 (iOS MG-68 패리티).
                val memberById = members.associateBy { it.id }
                val familyAnswers = allAnswers
                    .filter { it.userId != currentUser?.id }
                    .mapNotNull { answer ->
                        val user = memberById[answer.userId]
                        user?.let { FamilyAnswer(user = it, answer = answer) }
                    }

                val moodIds = listOf("happy", "calm", "loved", "sad", "tired")
                val moodIndex = myAnswer?.moodId?.let { moodIds.indexOf(it).takeIf { idx -> idx >= 0 } }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        myAnswer = myAnswer,
                        familyAnswers = familyAnswers,
                        answerText = myAnswer?.content ?: "",
                        selectedMoodIndex = moodIndex
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = AppError.from(e).toastMessage) }
            }
        }
    }

    fun onAnswerTextChanged(text: String) {
        if (text.length > 200) return
        _uiState.update { it.copy(answerText = text, errorMessage = null) }
    }

    fun onMoodSelected(index: Int) {
        _uiState.update { it.copy(selectedMoodIndex = index, showMoodRequiredAlert = false) }
    }

    fun dismissMoodRequiredAlert() {
        _uiState.update { it.copy(showMoodRequiredAlert = false) }
    }

    fun dismissEditConfirmDialog() {
        _uiState.update { it.copy(showEditConfirmDialog = false) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        if (state.answerText.trim().isEmpty()) {
            _uiState.update { it.copy(errorMessage = "답변을 입력해주세요.") }
            return
        }
        if (state.selectedMoodIndex == null) {
            _uiState.update { it.copy(showMoodRequiredAlert = true) }
            return
        }
        // 기존 답변이 있으면 하트 확인 후 수정 확인 팝업 표시
        if (state.myAnswer != null) {
            if (state.hasEnoughHeartsForEdit) {
                _uiState.update { it.copy(showEditConfirmDialog = true) }
            } else {
                // 하트 부족 → 광고 보기 팝업
                _uiState.update { it.copy(showEditAdDialog = true) }
            }
            return
        }
        doSubmitAnswer()
    }

    fun confirmEditAnswer() {
        // iOS MG-34 패리티 — 수정 비용 1 하트를 옵티미스틱 차감해 UI 즉시 피드백.
        // doSubmitAnswer() catch 블록에서 실패 시 +1 rollback.
        _uiState.update {
            it.copy(
                showEditConfirmDialog = false,
                hearts = (it.hearts - 1).coerceAtLeast(0)
            )
        }
        doSubmitAnswer(isEditOptimistic = true)
    }

    fun dismissEditAdDialog() {
        _uiState.update { it.copy(showEditAdDialog = false) }
    }

    fun watchAdForEdit(adManager: AdManager) {
        val state = _uiState.value
        if (state.isWatchingAd) return
        _uiState.update { it.copy(isWatchingAd = true, showEditAdDialog = false, errorMessage = null) }
        adManager.showRewardedAd(
            onRewarded = {
                viewModelScope.launch {
                    try {
                        // iOS MG-34 패리티 — transient 오류 시에도 보상 누락 방지 (3회 retry)
                        val heartsAfterAd = AdRewardClient.grantAdHearts(userRepository, 1)
                        // iOS MG-34 패리티 — 다른 단말에서 동시에 하트를 사용한 경우를 대비해 재검증.
                        if (heartsAfterAd >= 1) {
                            // 옵티미스틱 차감 후 수정 실행
                            _uiState.update { it.copy(hearts = heartsAfterAd - 1, isWatchingAd = false) }
                            doSubmitAnswer(isEditOptimistic = true)
                        } else {
                            _uiState.update {
                                it.copy(
                                    hearts = heartsAfterAd,
                                    isWatchingAd = false,
                                    errorMessage = "다른 단말에서 하트를 사용했어요. 다시 시도해주세요."
                                )
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isWatchingAd = false, errorMessage = AppError.from(e).toastMessage) }
                    }
                }
            },
            onFailed = {
                _uiState.update { it.copy(isWatchingAd = false, errorMessage = "광고를 불러올 수 없습니다.") }
            }
        )
    }

    /**
     * @param isEditOptimistic confirmEditAnswer / watchAdForEdit 진입 시 hearts 를 미리
     *   -1 차감했음을 알리는 플래그. 실패 시 +1 rollback.
     */
    private fun doSubmitAnswer(isEditOptimistic: Boolean = false) {
        val state = _uiState.value
        val question = state.question ?: return
        val userId = state.currentUser?.id ?: return

        val moodIds = listOf("happy", "calm", "loved", "sad", "tired")
        val moodId = state.selectedMoodIndex?.let { moodIds.getOrNull(it) }
        val isNewAnswer = state.myAnswer == null

        // 서버는 question(콘텐츠) ID를 기대함 (iOS와 동일)
        val questionId = question.id

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val answer = Answer(
                    id = state.myAnswer?.id ?: UUID.randomUUID(),
                    dailyQuestionId = questionId,
                    userId = userId,
                    content = state.answerText.trim(),
                    imageUrl = null,
                    moodId = moodId,
                    createdAt = Date(),
                    updatedAt = if (state.myAnswer != null) Date() else null
                )
                val savedAnswer = if (state.myAnswer != null) {
                    answerRepository.update(answer)
                } else {
                    answerRepository.create(answer)
                }
                _uiState.update { it.copy(isSubmitting = false, myAnswer = savedAnswer) }
                _events.emit(QuestionDetailEvent.AnswerSubmitted(savedAnswer, isNewAnswer))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = AppError.from(e).toastMessage,
                        // 옵티미스틱 차감했던 hearts 복구 (iOS MG-34 패리티)
                        hearts = if (isEditOptimistic) it.hearts + 1 else it.hearts
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun close() {
        viewModelScope.launch {
            _events.emit(QuestionDetailEvent.Closed)
        }
    }
}
