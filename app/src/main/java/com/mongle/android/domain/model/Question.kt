package com.mongle.android.domain.model

import java.util.Date
import java.util.UUID

data class Question(
    val id: UUID,
    val content: String,
    val category: QuestionCategory,
    val order: Int,
    val createdAt: Date = Date(),
    val dailyQuestionId: String? = null,
    val hasMyAnswer: Boolean = false,
    val familyAnswerCount: Int = 0
)

enum class QuestionCategory(val displayName: String) {
    DAILY("일상 & 취미"),
    MEMORY("추억 & 과거"),
    VALUES("가치관 & 생각"),
    FUTURE("미래 & 계획"),
    GRATITUDE("감사 & 애정")
}
