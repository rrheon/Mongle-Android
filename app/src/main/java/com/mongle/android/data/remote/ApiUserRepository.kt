package com.mongle.android.data.remote

import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.User
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
            val msg = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(msg)
        }
    }

    private fun ApiUserResponse.toDomain(): User = User(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.randomUUID() },
        email = email,
        name = name,
        profileImageUrl = profileImageUrl,
        role = FamilyRole.entries.firstOrNull { it.name == role } ?: FamilyRole.OTHER,
        createdAt = Date()
    )

    override suspend fun get(id: UUID): User = safeCall {
        api.getMe().toDomain()
    }

    override suspend fun update(user: User): User = safeCall {
        val response = api.updateMe(
            UpdateUserRequest(
                name = user.name,
                role = user.role.name
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
}
