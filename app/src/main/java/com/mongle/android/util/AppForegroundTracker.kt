package com.mongle.android.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS MG-55 패리티 — 인증 흐름(로그인/온보딩/약관) 화면에서는
 * MongleFcmService 가 트레이 배너를 노출하지 않도록 현재 화면 상태를 알려주는,
 * 프로세스 수명 동안 살아있는 신호 채널.
 *
 * Service 컨텍스트는 Activity/Composition 스택과 분리되어 있어 직접 현재 화면을
 * 알 수 없다. RootViewModel 이 자신의 uiState.appState 를 이 tracker 에 반영하고,
 * FCM Service 는 onMessageReceived 진입부에 이 값을 읽어 배너 노출 여부를 결정한다.
 */
@Singleton
class AppForegroundTracker @Inject constructor() {

    @Volatile
    var currentScreen: Screen = Screen.LOADING

    @Volatile
    var isAuthenticated: Boolean = false

    /**
     * 인증 흐름 도중(로그인/온보딩/약관) 인지 여부.
     * 이 동안 도착한 푸시는 트레이 배너 대신 unread store 만 갱신한다.
     */
    fun isInAuthFlow(): Boolean = !isAuthenticated && currentScreen.isAuthFlow

    enum class Screen(val isAuthFlow: Boolean) {
        LOADING(true),
        ONBOARDING(true),
        UNAUTHENTICATED(true),
        EMAIL_LOGIN(true),
        EMAIL_SIGNUP(true),
        CONSENT_REQUIRED(true),
        GROUP_SELECTION(false),
        AUTHENTICATED(false)
    }
}
