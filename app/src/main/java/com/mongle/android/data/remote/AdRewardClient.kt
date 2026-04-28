package com.mongle.android.data.remote

import kotlinx.coroutines.delay

/**
 * 광고 보상 grantAdHearts 호출에 exponential backoff retry 적용 (iOS MG-34 패리티).
 *
 * 사용자가 광고를 끝까지 시청했음에도 transient 네트워크 오류로 보상이 누락되는 것을
 * 방지하기 위해 최대 3회 재시도(500ms / 1s / 2s).
 *
 * 사용처: HomeViewModel.watchAdForWrite/Skip, QuestionDetailViewModel.watchAdForEdit,
 * PeerNudgeViewModel.watchAdForNudge.
 */
object AdRewardClient {
    suspend fun grantAdHearts(
        repository: ApiUserRepository,
        amount: Int,
        maxRetries: Int = 3
    ): Int {
        var lastError: Throwable? = null
        for (attempt in 0 until maxRetries) {
            try {
                return repository.grantAdHearts(amount)
            } catch (e: Throwable) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    val backoffMs = 500L * (1 shl attempt) // 500, 1000, 2000
                    delay(backoffMs)
                }
            }
        }
        throw lastError ?: Exception("ad reward grant failed")
    }
}
