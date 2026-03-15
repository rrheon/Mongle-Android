package com.mongle.android.data.mock

import com.mongle.android.domain.model.DailyQuestionHistory
import com.mongle.android.domain.model.Question
import com.mongle.android.domain.model.QuestionCategory
import com.mongle.android.domain.repository.QuestionRepository
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockQuestionRepository @Inject constructor() : QuestionRepository {

    private val mockQuestions = listOf(
        Question(UUID.randomUUID(), "오늘 가장 감사했던 순간은 언제인가요?", QuestionCategory.GRATITUDE, 1),
        Question(UUID.randomUUID(), "어릴 때 가장 좋아했던 놀이는 무엇이었나요?", QuestionCategory.MEMORY, 2),
        Question(UUID.randomUUID(), "요즘 가장 관심 있는 것은 무엇인가요?", QuestionCategory.DAILY, 3),
        Question(UUID.randomUUID(), "가족에게 가장 고마웠던 순간은 언제인가요?", QuestionCategory.GRATITUDE, 4),
        Question(UUID.randomUUID(), "10년 후 어떤 모습이고 싶은가요?", QuestionCategory.FUTURE, 5),
        Question(UUID.randomUUID(), "가장 기억에 남는 가족 여행은 언제인가요?", QuestionCategory.MEMORY, 6),
        Question(UUID.randomUUID(), "요즘 읽고 있는 책이나 보고 있는 영상이 있나요?", QuestionCategory.DAILY, 7),
        Question(UUID.randomUUID(), "오늘 기분은 어때요? 이유도 알려주세요.", QuestionCategory.DAILY, 8),
        Question(UUID.randomUUID(), "나에게 가장 영향을 준 가족 구성원은 누구인가요?", QuestionCategory.VALUES, 9),
        Question(UUID.randomUUID(), "올해 꼭 이루고 싶은 목표가 있나요?", QuestionCategory.FUTURE, 10)
    )

    override suspend fun create(question: Question): Question {
        delay(300)
        return question
    }

    override suspend fun get(id: UUID): Question {
        delay(200)
        return mockQuestions.firstOrNull { it.id == id }
            ?: throw Exception("질문을 찾을 수 없습니다.")
    }

    override suspend fun getByOrder(order: Int): Question? {
        delay(200)
        return mockQuestions.firstOrNull { it.order == order }
    }

    override suspend fun getByCategory(category: QuestionCategory): List<Question> {
        delay(300)
        return mockQuestions.filter { it.category == category }
    }

    override suspend fun getAll(): List<Question> {
        delay(300)
        return mockQuestions
    }

    override suspend fun update(question: Question): Question {
        delay(300)
        return question
    }

    override suspend fun delete(id: UUID) {
        delay(200)
    }

    override suspend fun getTodayQuestion(): Question? {
        delay(400)
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return mockQuestions[dayOfYear % mockQuestions.size]
    }

    override suspend fun getDailyHistory(page: Int, limit: Int): List<DailyQuestionHistory> {
        delay(400)
        return emptyList()
    }
}
