package com.mongle.android.domain.model

import java.util.Date
import java.util.UUID

data class Member(
    val id: UUID,
    val userId: UUID,
    val familyId: UUID,
    val role: FamilyRole,
    val joinedAt: Date,
    val isActive: Boolean = true
)
