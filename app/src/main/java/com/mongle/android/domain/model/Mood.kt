package com.mongle.android.domain.model

import androidx.annotation.StringRes
import com.mongle.android.R

enum class Mood(
    val id: String,
    val displayName: String,
    val emoji: String,
    @StringRes val labelResId: Int
) {
    HAPPY("happy", "Happy", "😊", R.string.mood_happy),
    LOVED("loved", "Loved", "🥰", R.string.mood_loved),
    CALM("calm", "Calm", "😌", R.string.mood_calm),
    SAD("sad", "Sad", "😢", R.string.mood_sad),
    ANGRY("angry", "Angry", "😤", R.string.mood_angry),
    ANXIOUS("anxious", "Anxious", "😟", R.string.mood_anxious),
    EXCITED("excited", "Excited", "🤩", R.string.mood_excited),
    TIRED("tired", "Tired", "😴", R.string.mood_tired);

    companion object {
        fun fromId(id: String): Mood? = entries.firstOrNull { it.id == id }
    }
}
