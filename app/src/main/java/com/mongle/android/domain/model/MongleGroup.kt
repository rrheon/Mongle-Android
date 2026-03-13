package com.mongle.android.domain.model

import java.util.Date
import java.util.UUID

data class MongleGroup(
    val id: UUID,
    val name: String,
    val memberIds: List<UUID>,
    val createdBy: UUID,
    val createdAt: Date,
    val inviteCode: String,
    val groupProgressId: UUID
)
