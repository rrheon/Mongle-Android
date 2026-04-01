package com.mongle.android.data.remote

import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.DailyHeartResult
import com.mongle.android.domain.repository.UserRepository
import retrofit2.HttpException
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiUserRepository @Inject constructor(
    private val api: MongleApiService
) : UserRepository {

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val raw = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(parseServerMessage(raw))
        }
    }

    private fun ApiUserResponse.toDomain(): User = User(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.randomUUID() },
        email = email,
        name = name,
        profileImageUrl = profileImageUrl,
        role = FamilyRole.entries.firstOrNull { it.name == role } ?: FamilyRole.OTHER,
        hearts = hearts,
        moodId = moodId,
        createdAt = Date()
    )

    override suspend fun get(id: UUID): User = safeCall {
        api.getMe().toDomain()
    }

    override suspend fun update(user: User): User = safeCall {
        val response = api.updateMe(
            UpdateUserRequest(
                name = user.name,
                role = user.role.name,
                moodId = user.moodId
            )
        )
        response.toDomain()
    }

    suspend fun getMe(): User = safeCall {
        api.getMe().toDomain()
    }

    suspend fun getMyStreak(): Int = safeCall {
        api.getMyStreak().streakDays
    }

    suspend fun grantAdHearts(amount: Int): Int = safeCall {
        api.grantAdHearts(AdHeartRewardRequest(amount)).heartsRemaining
    }

    suspend fun registerFcmToken(token: String) = safeCall {
        api.registerFcmToken(RegisterDeviceTokenRequest(token))
    }

    override suspend fun claimDailyHeart(): DailyHeartResult? = try {
        val resp = api.claimDailyHeart()
        if (resp.alreadyClaimed || resp.heartsGranted <= 0) null
        else DailyHeartResult(resp.heartsGranted, resp.heartsRemaining)
    } catch (e: HttpException) {
        // 409 Conflict = 이미 오늘 수령함
        if (e.code() == 409 || e.code() == 400) null else null
    } catch (e: Exception) {
        null
    }
}
