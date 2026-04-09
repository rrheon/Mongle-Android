package com.mongle.android.domain.model

import java.util.Date

data class DailyQuestionHistory(
    val id: String,
    val question: Question,
    val date: Date,
    val hasMyAnswer: Boolean,
    /** 현재 사용자가 이 날의 질문을 "건너뛰기(skip)" 했는지 여부 */
    val hasMySkipped: Boolean = false,
    val familyAnswerCount: Int,
    val answers: List<HistoryAnswerSummary> = emptyList()
)
