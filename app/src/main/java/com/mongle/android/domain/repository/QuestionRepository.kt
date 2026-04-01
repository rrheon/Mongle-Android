package com.mongle.android.domain.repository

import com.mongle.android.domain.model.DailyQuestionHistory
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

    /** 히스토리 목록 조회. */
    suspend fun getDailyHistory(page: Int = 1, limit: Int = 50): List<DailyQuestionHistory>

    /** 나만의 질문 작성 (하트 3개 차감). */
    suspend fun createCustomQuestion(content: String): Question

    /** 오늘의 질문 넘기기 (하트 3개 차감). 답변 없이 다른 가족 답변 열람 가능. 차감 후 남은 하트 수 반환. */
    suspend fun skipQuestion(): Int
}

sealed class QuestionError(message: String) : Exception(message) {
    data object QuestionNotFound : QuestionError("질문을 찾을 수 없습니다.")
    data object NoQuestionToday : QuestionError("오늘의 질문이 아직 없습니다.")
    data object NetworkError : QuestionError("네트워크 연결을 확인해주세요.")
    data class Unknown(val msg: String) : QuestionError(msg)
}
