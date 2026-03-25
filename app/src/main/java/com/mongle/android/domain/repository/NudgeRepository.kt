package com.mongle.android.domain.repository

interface NudgeRepository {
    suspend fun sendNudge(targetUserId: String): Int
}
