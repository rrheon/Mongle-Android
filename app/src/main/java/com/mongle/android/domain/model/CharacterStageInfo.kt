package com.mongle.android.domain.model

/**
 * 몽글캐릭터 성장 스테이지 (PRD v2 §2.2).
 * 단일 진실 소스는 서버이지만, 미배포 기간에는 [fromStreak] 폴백을 쓴다.
 */
data class CharacterStageInfo(
    val stage: Int,
    val stageKey: String,
    val streakDays: Int,
    val nextStageStreak: Int?,
    val sizeMultiplier: Float
) {
    companion object {
        private val table = listOf(
            Triple(0, "SEED", 1.00f),
            Triple(3, "SPROUT", 1.10f),
            Triple(7, "LEAF", 1.20f),
            Triple(14, "BUD", 1.32f),
            Triple(30, "BLOOM", 1.45f),
            Triple(100, "RADIANCE", 1.60f)
        )

        fun fromStreak(streakDays: Int): CharacterStageInfo {
            var stageIdx = 0
            for (i in table.indices) {
                if (streakDays >= table[i].first) stageIdx = i
            }
            val (_, key, mult) = table[stageIdx]
            val next = if (stageIdx + 1 < table.size) table[stageIdx + 1].first else null
            return CharacterStageInfo(
                stage = stageIdx,
                stageKey = key,
                streakDays = streakDays,
                nextStageStreak = next,
                sizeMultiplier = mult
            )
        }

        val ZERO = fromStreak(0)
    }
}
