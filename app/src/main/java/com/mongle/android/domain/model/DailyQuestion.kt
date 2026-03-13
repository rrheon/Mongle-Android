package com.mongle.android.domain.model

import java.util.Date
import java.util.UUID

data class DailyQuestion(
    val id: UUID,
    val familyId: UUID,
    val questionId: UUID,
    val questionOrder: Int,
    val date: Date,
    val isCompleted: Boolean,
    val answerIds: List<UUID>,
    val createdAt: Date,
    val completedAt: Date?
)
