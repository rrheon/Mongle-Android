package com.mongle.android.data.remote
import com.ycompany.Monggle.BuildConfig

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTH_PREF_NAME = "mongle_auth"
private const val AUTH_KEY_TOKEN = "auth_token"
private const val AUTH_KEY_REFRESH_TOKEN = "refresh_token"

/**
 * 401 Unauthorized 응답 시 refresh_token으로 액세스 토큰을 갱신한다.
 * 갱신 성공 → 원래 요청을 새 토큰으로 재시도
 * 갱신 실패 → 세션 삭제 + SessionExpiredNotifier 이벤트 전파 → 로그인 화면
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val sessionExpiredNotifier: SessionExpiredNotifier
) : Authenticator {

    private val baseUrl = BuildConfig.BASE_URL

    private val prefs by lazy {
        context.getSharedPreferences(AUTH_PREF_NAME, Context.MODE_PRIVATE)
    }

    // Authenticator 전용 clean client (인터셉터/authenticator 없음, 무한루프 방지)
    private val refreshClient = OkHttpClient.Builder().build()

    private val refreshAdapter by lazy {
        moshi.adapter(TokenRefreshResponse::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 한 번 재시도했으면 포기 (무한루프 방지)
        if (response.priorResponse != null) {
            notifyAndClear()
            return null
        }

        // 첫 설치 직후 또는 로그아웃 직후처럼 refresh 토큰이 SharedPreferences 에 한 번도
        // 들어간 적 없는 상태에서는 sessionExpired 신호를 발화하지 않는다.
        // 발화하면 RootViewModel 이 "세션 만료"로 라우팅돼 사용자에게 불필요한 재로그인 안내가 노출됨.
        val refreshToken = prefs.getString(AUTH_KEY_REFRESH_TOKEN, null) ?: run {
            if (prefs.contains(AUTH_KEY_REFRESH_TOKEN)) {
                notifyAndClear()
            } else {
                prefs.edit().clear().apply()
            }
            return null
        }

        return try {
            val body = """{"refresh_token":"$refreshToken"}"""
                .toRequestBody("application/json".toMediaType())

            val refreshRequest = Request.Builder()
                .url("${baseUrl}auth/refresh")
                .post(body)
                .build()

            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (!refreshResponse.isSuccessful) {
                notifyAndClear()
                return null
            }

            val bodyStr = refreshResponse.body?.string() ?: run {
                notifyAndClear()
                return null
            }

            val tokenResponse = refreshAdapter.fromJson(bodyStr) ?: run {
                notifyAndClear()
                return null
            }

            // 새 토큰 저장
            prefs.edit()
                .putString(AUTH_KEY_TOKEN, tokenResponse.token)
                .putString(AUTH_KEY_REFRESH_TOKEN, tokenResponse.refresh_token)
                .apply()

            // 원래 요청을 새 토큰으로 재시도
            response.request.newBuilder()
                .header("Authorization", "Bearer ${tokenResponse.token}")
                .build()

        } catch (e: Exception) {
            notifyAndClear()
            null
        }
    }

    private fun notifyAndClear() {
        prefs.edit().clear().apply()
        sessionExpiredNotifier.notifySessionExpired()
    }
}
