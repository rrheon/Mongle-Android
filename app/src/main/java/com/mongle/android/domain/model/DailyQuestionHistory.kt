package com.mongle.android.domain.model

import java.util.Date

data class DailyQuestionHistory(
    val id: String,
    val question: Question,
    val date: Date,
    val hasMyAnswer: Boolean,
    val isSkipped: Boolean = false,
    val familyAnswerCount: Int,
    val answers: List<HistoryAnswerSummary> = emptyList()
)
