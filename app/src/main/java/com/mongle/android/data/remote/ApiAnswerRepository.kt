package com.mongle.android.data.remote

import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.repository.AnswerRepository
import retrofit2.HttpException
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiAnswerRepository @Inject constructor(
    private val api: MongleApiService
) : AnswerRepository {

    override suspend fun create(answer: Answer): Answer = safeCall {
        val req = CreateAnswerRequest(
            questionId = answer.dailyQuestionId.toString(),
            content = answer.content,
            imageUrl = answer.imageUrl,
            moodId = answer.moodId
        )
        api.createAnswer(req).toDomain()
    }

    override suspend fun get(id: UUID): Answer = throw UnsupportedOperationException()

    override suspend fun getByDailyQuestion(dailyQuestionId: UUID): List<Answer> = safeCall {
        api.getFamilyAnswers(dailyQuestionId.toString()).answers.map { it.toDomain() }
    }

    /** 가족 답변 + 각 멤버의 답변/스킵/미답변 상태 반환 */
    suspend fun getFamilyAnswersWithStatuses(dailyQuestionId: UUID): FamilyAnswersResponse = safeCall {
        api.getFamilyAnswers(dailyQuestionId.toString())
    }

    override suspend fun getByUserAndDailyQuestion(dailyQuestionId: UUID, userId: UUID): Answer? = safeCall {
        runCatching { api.getMyAnswer(dailyQuestionId.toString()).toDomain() }.getOrNull()
    }

    override suspend fun hasUserAnswered(dailyQuestionId: UUID, userId: UUID): Boolean =
        getByUserAndDailyQuestion(dailyQuestionId, userId) != null

    override suspend fun getByUser(userId: UUID): List<Answer> = throw UnsupportedOperationException()

    override suspend fun update(answer: Answer): Answer = safeCall {
        val req = UpdateAnswerRequest(content = answer.content, imageUrl = answer.imageUrl, moodId = answer.moodId)
        api.updateAnswer(answer.id.toString(), req).toDomain()
    }

    override suspend fun delete(id: UUID) = throw UnsupportedOperationException()

    private fun AnswerResponse.toDomain() = Answer(
        id = UUID.fromString(id),
        dailyQuestionId = UUID.fromString(questionId),
        userId = UUID.fromString(user.id),
        content = content,
        imageUrl = imageUrl,
        moodId = moodId,
        createdAt = Date(),
        updatedAt = Date()
    )

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val raw = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(parseServerMessage(raw))
        }
    }
}

private val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()
internal fun parseServerMessage(raw: String?): String {
    if (raw == null) return "알 수 없는 오류가 발생했습니다."
    return messageRegex.find(raw)?.groupValues?.get(1) ?: raw
}
