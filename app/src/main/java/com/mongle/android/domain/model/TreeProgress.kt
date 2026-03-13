package com.mongle.android.domain.model

import java.util.Date
import java.util.UUID

data class TreeProgress(
    val id: UUID = UUID.randomUUID(),
    val familyId: UUID = UUID.randomUUID(),
    val stage: TreeStage = TreeStage.SEED,
    val totalAnswers: Int = 0,
    val consecutiveDays: Int = 0,
    val badgeIds: List<UUID> = emptyList(),
    val lastUpdated: Date = Date()
)

enum class TreeStage(val value: Int, val displayName: String) {
    SEED(0, "씨앗"),
    SPROUT(1, "새싹"),
    SAPLING(2, "묘목"),
    YOUNG_TREE(3, "어린 나무"),
    MATURE_TREE(4, "큰 나무"),
    FLOWERING(5, "꽃 피는 나무"),
    BOUND(6, "열매 맺는 나무");

    companion object {
        fun fromValue(value: Int): TreeStage =
            entries.firstOrNull { it.value == value } ?: SEED
    }
}
