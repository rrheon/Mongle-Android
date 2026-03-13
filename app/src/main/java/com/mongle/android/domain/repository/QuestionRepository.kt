package com.mongle.android.domain.repository

import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.QuestionCategory
import java.util.UUID

interface QuestionRepository {
    suspend fun create(question: Question): Question
    suspend fun get(id: UUID): Question
    suspend fun getByOrder(order: Int): Question?
    suspend fun getByCategory(category: QuestionCategory): List<Question>
    suspend fun getAll(): List<Question>
    suspend fun update(question: Question): Question
    suspend fun delete(id: UUID)
    /** 오늘의 질문을 조회. 질문이 없으면 null. */
    suspend fun getTodayQuestion(): Question?
}

sealed class QuestionError(message: String) : Exception(message) {
    data object QuestionNotFound : QuestionError("질문을 찾을 수 없습니다.")
    data object NoQuestionToday : QuestionError("오늘의 질문이 아직 없습니다.")
    data object NetworkError : QuestionError("네트워크 연결을 확인해주세요.")
    data class Unknown(val msg: String) : QuestionError(msg)
}
