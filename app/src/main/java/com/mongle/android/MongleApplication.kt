package com.mongle.android
import com.ycompany.Monggle.BuildConfig

import android.app.Application
import android.content.Context
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

private const val APP_PREFS_NAME = "mongle_app_prefs"
private const val INSTALL_SENTINEL_KEY = "mongle.installSentinel"

@HiltAndroidApp
class MongleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 재설치 감지 — 일부 환경에서는 SharedPreferences 가 즉시 비워지지 않을 수 있어
        // 신규 설치 시점에 이전 사용자 토큰/하트 마커/FCM 토큰을 명시 폐기한다.
        val appPrefs = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        if (!appPrefs.getBoolean(INSTALL_SENTINEL_KEY, false)) {
            getSharedPreferences("mongle_auth", Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("mongle_heart", Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("fcm", Context.MODE_PRIVATE).edit().clear().apply()
            appPrefs.edit().putBoolean(INSTALL_SENTINEL_KEY, true).apply()
        }
        KakaoSdk.init(this, BuildConfig.KAKAO_APP_KEY)
        // AdMob 초기화는 ConsentManager 가 MainActivity.onCreate 에서 수행한다.
        // (UMP 동의 폼은 Activity context 를 필요로 하므로 Application 에서 초기화하지 않음)
    }
}
