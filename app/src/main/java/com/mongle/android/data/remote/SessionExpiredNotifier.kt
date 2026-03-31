package com.mongle.android.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 세션 만료(401 + 토큰 갱신 실패) 이벤트를 앱 전역에 전달하는 싱글톤.
 * TokenAuthenticator에서 emit → RootViewModel에서 collect → 로그아웃 처리
 */
@Singleton
class SessionExpiredNotifier @Inject constructor() {

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifySessionExpired() {
        _events.tryEmit(Unit)
    }
}
