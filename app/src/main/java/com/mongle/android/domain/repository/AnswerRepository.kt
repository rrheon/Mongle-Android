package com.mongle.android.domain.repository

import com.mongle.android.domain.model.Answer
import java.util.UUID

interface AnswerRepository {
    suspend fun create(answer: Answer): Answer
    suspend fun get(id: UUID): Answer
    suspend fun getByDailyQuestion(dailyQuestionId: UUID): List<Answer>
    suspend fun getByUserAndDailyQuestion(dailyQuestionId: UUID, userId: UUID): Answer?
    suspend fun hasUserAnswered(dailyQuestionId: UUID, userId: UUID): Boolean
    suspend fun getByUser(userId: UUID): List<Answer>
    suspend fun update(answer: Answer): Answer
    suspend fun delete(id: UUID)
}

sealed class AnswerError(message: String) : Exception(message) {
    data object AnswerNotFound : AnswerError("답변을 찾을 수 없습니다.")
    data object AlreadyAnswered : AnswerError("이미 답변을 작성했습니다.")
    data object CannotModify : AnswerError("답변을 수정할 수 없습니다.")
    data object NetworkError : AnswerError("네트워크 연결을 확인해주세요.")
    data class Unknown(val msg: String) : AnswerError(msg)
}
