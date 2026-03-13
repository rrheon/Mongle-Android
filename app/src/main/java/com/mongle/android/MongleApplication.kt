package com.mongle.android

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MongleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 카카오 SDK 초기화 (실제 앱 키로 교체 필요)
        KakaoSdk.init(this, "YOUR_KAKAO_NATIVE_APP_KEY")
    }
}
