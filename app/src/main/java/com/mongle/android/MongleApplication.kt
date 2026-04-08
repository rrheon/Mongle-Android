package com.mongle.android

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MongleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_APP_KEY)
        // AdMob 초기화는 ConsentManager 가 MainActivity.onCreate 에서 수행한다.
        // (UMP 동의 폼은 Activity context 를 필요로 하므로 Application 에서 초기화하지 않음)
    }
}
