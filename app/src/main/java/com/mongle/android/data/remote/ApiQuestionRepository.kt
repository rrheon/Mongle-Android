package com.mongle.android.data.remote

import com.mongle.android.domain.model.DailyQuestionHistory
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.QuestionCategory
import com.mongle.android.domain.repository.QuestionRepository
import retrofit2.HttpException
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiQuestionRepository @Inject constructor(
    private val api: MongleApiService
) : QuestionRepository {

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val msg = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(msg)
        }
    }

    private fun QuestionDto.toDomain(
        dailyQuestionId: String? = null,
        hasMyAnswer: Boolean = false,
        familyAnswerCount: Int = 0
    ): Question = Question(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.randomUUID() },
        content = content,
        category = category.toQuestionCategory(),
        order = 0,
        createdAt = Date(),
        dailyQuestionId = dailyQuestionId,
        hasMyAnswer = hasMyAnswer,
        familyAnswerCount = familyAnswerCount
    )

    private fun String.toQuestionCategory(): QuestionCategory = when (this) {
        "DAILY" -> QuestionCategory.DAILY
        "MEMORY" -> QuestionCategory.MEMORY
        "VALUE" -> QuestionCategory.VALUES
        "DREAM" -> QuestionCategory.FUTURE
        "GRATITUDE" -> QuestionCategory.GRATITUDE
        else -> QuestionCategory.DAILY
    }

    override suspend fun getTodayQuestion(): Question? = safeCall {
        val response = api.getTodayQuestion()
        response.question.toDomain(
            dailyQuestionId = response.id,
            hasMyAnswer = response.hasMyAnswer,
            familyAnswerCount = response.familyAnswerCount
        )
    }

    override suspend fun create(question: Question): Question =
        throw UnsupportedOperationException("서버에서 질문 생성을 지원하지 않습니다.")

    override suspend fun get(id: UUID): Question =
        throw UnsupportedOperationException()

    override suspend fun getByOrder(order: Int): Question? = null

    override suspend fun getByCategory(category: QuestionCategory): List<Question> = emptyList()

    override suspend fun getAll(): List<Question> = emptyList()

    override suspend fun update(question: Question): Question =
        throw UnsupportedOperationException()

    override suspend fun delete(id: UUID) =
        throw UnsupportedOperationException()

    override suspend fun getDailyHistory(page: Int, limit: Int): List<DailyQuestionHistory> = safeCall {
        api.getQuestionHistory(page, limit).data.map { item ->
            DailyQuestionHistory(
                id = item.id,
                question = item.question.toDomain(
                    dailyQuestionId = item.id,
                    hasMyAnswer = item.hasMyAnswer,
                    familyAnswerCount = item.familyAnswerCount
                ),
                date = parseDate(item.date),
                hasMyAnswer = item.hasMyAnswer,
                familyAnswerCount = item.familyAnswerCount
            )
        }
    }

    private fun parseDate(dateStr: String): java.util.Date {
        return runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr) ?: Date()
        }.getOrElse { Date() }
    }
}
