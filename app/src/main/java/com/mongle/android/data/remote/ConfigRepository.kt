package com.mongle.android.data.remote

import com.mongle.android.data.local.AdConfigStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 /config 호출 + AdConfigStore 갱신 (MG-132).
 *
 * 앱 부팅 시 1회 호출. 네트워크 실패 시 silent — 다음 부팅 또는 다음 호출에서 다시 시도.
 * 광고 토글은 즉시성보다 안전한 기본값(=현재 캐시 유지)이 우선.
 */
@Singleton
class ConfigRepository @Inject constructor(
    private val api: MongleApiService,
    private val adConfigStore: AdConfigStore
) {
    suspend fun refresh(): Result<Boolean> = runCatching {
        val response = api.getConfig()
        adConfigStore.set(response.isAdEnabled)
        response.isAdEnabled
    }
}
