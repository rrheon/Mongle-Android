package com.mongle.android.domain.model

import java.util.Date
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val name: String,
    val profileImageUrl: String?,
    val role: FamilyRole,
    val hearts: Int = 0,
    val moodId: String? = null,
    val createdAt: Date,
    val lastNameChangedAt: Long? = null,
    /** users/me?grantDailyHeart=true 응답에서만 true 가 올 수 있음 */
    val heartGrantedToday: Boolean = false
)

enum class FamilyRole(val displayName: String) {
    FATHER("아빠"),
    MOTHER("엄마"),
    SON("아들"),
    DAUGHTER("딸"),
    OTHER("기타");

    companion object {
        fun fromDisplayName(name: String): FamilyRole =
            entries.firstOrNull { it.displayName == name } ?: OTHER
    }
}
