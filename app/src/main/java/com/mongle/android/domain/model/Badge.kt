package com.mongle.android.domain.model

import java.util.Date

/**
 * 배지 정의 (서버 BadgeDefinition).
 * 이름/조건 텍스트는 클라이언트 strings.xml 에서 [code] 를 키로 가져온다.
 */
data class BadgeDefinition(
    val code: String,
    val category: BadgeCategory,
    val iconKey: String
)

enum class BadgeCategory { STREAK, ANSWER_COUNT, UNKNOWN }

/** 사용자가 획득한 배지 1개 */
data class UserBadge(
    val code: String,
    val awardedAt: Date,
    val seenAt: Date?
) {
    val isUnseen: Boolean get() = seenAt == null
}

/**
 * 화면에 그릴 배지 1개의 표시 상태. 정의 + 획득 정보 병합.
 */
data class BadgeDisplayItem(
    val definition: BadgeDefinition,
    val awarded: UserBadge?
) {
    val isOwned: Boolean get() = awarded != null
}

object BadgeDefaults {
    /** 서버 미배포 시 클라이언트에서 동일하게 보여주는 6개 정의 (PRD §4.2) */
    val seed: List<BadgeDefinition> = listOf(
        BadgeDefinition("STREAK_3", BadgeCategory.STREAK, "badge_streak_3"),
        BadgeDefinition("STREAK_7", BadgeCategory.STREAK, "badge_streak_7"),
        BadgeDefinition("STREAK_30", BadgeCategory.STREAK, "badge_streak_30"),
        BadgeDefinition("STREAK_100", BadgeCategory.STREAK, "badge_streak_100"),
        BadgeDefinition("ANSWERS_10", BadgeCategory.ANSWER_COUNT, "badge_answers_10"),
        BadgeDefinition("ANSWERS_50", BadgeCategory.ANSWER_COUNT, "badge_answers_50")
    )
}
