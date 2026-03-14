package com.mongle.android.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.mongle.android.domain.model.GoogleLoginCredential
import com.mongle.android.domain.model.KakaoLoginCredential
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ── 카카오 ─────────────────────────────────────────

suspend fun loginWithKakao(context: Context): KakaoLoginCredential =
    suspendCancellableCoroutine { cont ->
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                cont.resumeWithException(error)
            } else if (token != null) {
                // 사용자 정보 조회
                UserApiClient.instance.me { user, infoError ->
                    if (infoError != null) {
                        Log.w("KakaoLogin", "사용자 정보 조회 실패, 토큰만 사용", infoError)
                        cont.resume(KakaoLoginCredential(accessToken = token.accessToken))
                    } else {
                        cont.resume(
                            KakaoLoginCredential(
                                accessToken = token.accessToken,
                                name = user?.kakaoAccount?.profile?.nickname,
                                email = user?.kakaoAccount?.email
                            )
                        )
                    }
                }
            } else {
                cont.resumeWithException(Exception("카카오 로그인 실패: 토큰 없음"))
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    // 카카오톡 미설치 또는 사용자 취소가 아닌 경우 계정 로그인 시도
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        cont.resumeWithException(error)
                    } else {
                        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                    }
                } else {
                    callback(token, null)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }

// ── 구글 ────────────────────────────────────────────
// Google Sign-In Intent를 Activity Result로 처리하기 위한 헬퍼

fun getGoogleSignInIntent(context: Context, webClientId: String): Intent {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .requestProfile()
        .build()
    val client = GoogleSignIn.getClient(context, gso)
    return client.signInIntent
}

fun handleGoogleSignInResult(data: Intent?): GoogleLoginCredential {
    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
    val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
    val idToken = account.idToken ?: throw Exception("Google ID Token을 가져올 수 없습니다.")
    return GoogleLoginCredential(
        idToken = idToken,
        name = account.displayName,
        email = account.email
    )
}

fun signOutGoogle(context: Context, webClientId: String) {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    GoogleSignIn.getClient(context, gso).signOut()
}
