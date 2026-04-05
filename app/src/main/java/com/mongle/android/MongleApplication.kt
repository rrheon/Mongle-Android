package com.mongle.android

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.mongle.android.util.AdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MongleApplication : Application() {

    @Inject
    lateinit var adManager: AdManager

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_APP_KEY)
        // AdMob 초기화
        adManager.initialize()
    }
}
