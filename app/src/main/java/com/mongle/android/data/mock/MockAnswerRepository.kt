package com.mongle.android.data.mock

import com.mongle.android.domain.model.Answer
import com.mongle.android.domain.repository.AnswerRepository
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAnswerRepository @Inject constructor() : AnswerRepository {

    private val answers = mutableListOf<Answer>()

    override suspend fun create(answer: Answer): Answer {
        delay(500)
        answers.add(answer)
        return answer
    }

    override suspend fun get(id: UUID): Answer {
        delay(200)
        return answers.firstOrNull { it.id == id }
            ?: throw Exception("답변을 찾을 수 없습니다.")
    }

    override suspend fun getByDailyQuestion(dailyQuestionId: UUID): List<Answer> {
        delay(300)
        return answers.filter { it.dailyQuestionId == dailyQuestionId }
    }

    override suspend fun getByUserAndDailyQuestion(dailyQuestionId: UUID, userId: UUID): Answer? {
        delay(200)
        return answers.firstOrNull { it.dailyQuestionId == dailyQuestionId && it.userId == userId }
    }

    override suspend fun hasUserAnswered(dailyQuestionId: UUID, userId: UUID): Boolean {
        delay(200)
        return answers.any { it.dailyQuestionId == dailyQuestionId && it.userId == userId }
    }

    override suspend fun getByUser(userId: UUID): List<Answer> {
        delay(300)
        return answers.filter { it.userId == userId }
    }

    override suspend fun update(answer: Answer): Answer {
        delay(400)
        val index = answers.indexOfFirst { it.id == answer.id }
        if (index != -1) {
            answers[index] = answer
        }
        return answer
    }

    override suspend fun delete(id: UUID) {
        delay(300)
        answers.removeAll { it.id == id }
    }
}
