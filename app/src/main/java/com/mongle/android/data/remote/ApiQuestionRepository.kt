package com.mongle.android.data.remote

import com.mongle.android.domain.model.DailyQuestionHistory
import com.mongle.android.domain.model.HistoryAnswerSummary
import com.mongle.android.domain.model.HistorySkippedSummary
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
            val raw = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(parseServerMessage(raw))
        }
    }

    private fun QuestionDto.toDomain(
        dailyQuestionId: String? = null,
        hasMyAnswer: Boolean = false,
        hasMySkipped: Boolean = false,
        familyAnswerCount: Int = 0
    ): Question = Question(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.randomUUID() },
        content = content,
        category = category.toQuestionCategory(),
        order = 0,
        createdAt = Date(),
        dailyQuestionId = dailyQuestionId,
        hasMyAnswer = hasMyAnswer,
        hasMySkipped = hasMySkipped,
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
            hasMySkipped = response.hasMySkipped,
            familyAnswerCount = response.familyAnswerCount
        )
    }

    override suspend fun create(question: Question): Question =
        throw UnsupportedOperationException("서버에서 질문 생성을 지원하지 않습니다.")

    override suspend fun getTodayQuestionMemberStatuses(): List<Pair<String, String>> = safeCall {
        val response = api.getTodayQuestion()
        response.memberAnswerStatuses.map { it.userId to it.status }
    }

    override suspend fun skipQuestion(): Int = safeCall {
        val response = api.skipQuestion()
        response.heartsRemaining
    }

    override suspend fun createCustomQuestion(content: String): Question = safeCall {
        api.createCustomQuestion(CreateCustomQuestionRequest(content))
        getTodayQuestion() ?: throw Exception("등록된 질문을 불러올 수 없습니다.")
    }

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
            // memberAnswerStatuses 를 한 번만 인덱싱해 두고
            //  - 답변 카드의 colorId 조인
            //  - 스킵 멤버 섹션 추출
            // 두 가지 용도로 사용한다.
            val statusByUserId = item.memberAnswerStatuses.associateBy { it.userId }
            val skippedMembers = item.memberAnswerStatuses
                .filter { it.status == "skipped" }
                .map { s ->
                    HistorySkippedSummary(
                        userId = s.userId,
                        userName = s.userName,
                        colorId = s.colorId
                    )
                }
            DailyQuestionHistory(
                id = item.id,
                question = item.question.toDomain(
                    dailyQuestionId = item.id,
                    hasMyAnswer = item.hasMyAnswer,
                    familyAnswerCount = item.familyAnswerCount
                ),
                date = parseDate(item.date),
                hasMyAnswer = item.hasMyAnswer,
                hasMySkipped = item.hasMySkipped,
                familyAnswerCount = item.familyAnswerCount,
                answers = item.answers.map { a ->
                    HistoryAnswerSummary(
                        id = a.id,
                        userId = a.userId,
                        userName = a.userName,
                        content = a.content,
                        imageUrl = a.imageUrl,
                        moodId = a.moodId,
                        colorId = statusByUserId[a.userId]?.colorId
                    )
                },
                skippedMembers = skippedMembers
            )
        }
    }

    private fun parseDate(dateStr: String): java.util.Date {
        return runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr) ?: Date()
        }.getOrElse { Date() }
    }
}
