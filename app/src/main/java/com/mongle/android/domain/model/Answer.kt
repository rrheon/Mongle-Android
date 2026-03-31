package com.mongle.android.domain.model

import java.util.Date
import java.util.UUID

data class Answer(
    val id: UUID,
    val dailyQuestionId: UUID,
    val userId: UUID,
    val content: String,
    val imageUrl: String?,
    val moodId: String? = null,
    val createdAt: Date,
    val updatedAt: Date? = null,
    val reactionIds: List<UUID> = emptyList(),
    val commentIds: List<UUID> = emptyList()
)
