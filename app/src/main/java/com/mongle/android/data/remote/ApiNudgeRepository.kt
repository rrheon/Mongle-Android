package com.mongle.android.data.remote

import com.mongle.android.domain.repository.NudgeRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiNudgeRepository @Inject constructor(
    private val api: MongleApiService
) : NudgeRepository {

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val raw = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(parseServerMessage(raw))
        }
    }

    override suspend fun sendNudge(targetUserId: String): Int = safeCall {
        api.sendNudge(NudgeRequest(targetUserId)).heartsRemaining
    }
}
