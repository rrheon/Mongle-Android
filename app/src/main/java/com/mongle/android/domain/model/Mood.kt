package com.mongle.android.domain.model

enum class Mood(
    val id: String,
    val displayName: String,
    val emoji: String
) {
    HAPPY("happy", "행복", "😊"),
    LOVED("loved", "사랑", "🥰"),
    CALM("calm", "평온", "😌"),
    SAD("sad", "슬픔", "😢"),
    ANGRY("angry", "화남", "😤"),
    ANXIOUS("anxious", "불안", "😟"),
    EXCITED("excited", "신남", "🤩"),
    TIRED("tired", "피곤", "😴");

    companion object {
        fun fromId(id: String): Mood? = entries.firstOrNull { it.id == id }
    }
}
