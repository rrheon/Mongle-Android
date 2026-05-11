package com.mongle.android.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "mongle_ad_config"
private const val KEY_IS_AD_ENABLED = "is_ad_enabled"

/**
 * 서버 /config 응답을 캐시하는 source-of-truth (MG-132).
 *
 * 부팅 시 ConfigRepository 가 서버에서 최신값을 받아 [set] 으로 갱신한다.
 * AdBannerSection 과 ConsentManager 는 [isAdEnabled] 를 읽어 광고 렌더링 / AdMob
 * 초기화 여부를 결정.
 *
 * 기본값은 true (광고 표시) — 첫 부팅 / 네트워크 실패 시 운영 사고 없이 기존 동작 유지.
 * 운영자가 서버 환경변수 ADS_ENABLED=false 로 OFF 한 뒤 다음 부팅부터 반영된다.
 */
@Singleton
class AdConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isAdEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_AD_ENABLED, true)

    fun set(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AD_ENABLED, enabled).apply()
    }

    companion object {
        /**
         * Composable 등 Hilt 주입이 어려운 곳에서 Context 만으로 읽기 위한 정적 진입점.
         * 인스턴스의 [isAdEnabled] 와 같은 SharedPrefs 를 본다.
         */
        fun read(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_AD_ENABLED, true)
    }
}
