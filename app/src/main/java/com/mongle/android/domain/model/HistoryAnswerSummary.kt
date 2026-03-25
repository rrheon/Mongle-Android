package com.mongle.android.domain.model

data class HistoryAnswerSummary(
    val id: String,
    val userId: String,
    val userName: String,
    val content: String,
    val imageUrl: String? = null,
    val moodId: String? = null
)
