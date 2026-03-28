package com.mongle.android.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
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
                Log.e("KakaoLogin", "❌ 카카오 로그인 실패: ${error.message}", error)
                cont.resumeWithException(Exception("카카오 로그인 실패: ${error.message}"))
            } else if (token != null) {
                Log.d("KakaoLogin", "✅ 카카오 토큰 획득 | token=${token.accessToken.take(20)}...")
                // 사용자 정보 조회
                UserApiClient.instance.me { user, infoError ->
                    if (infoError != null) {
                        Log.w("KakaoLogin", "⚠️ 사용자 정보 조회 실패, 토큰만 사용: ${infoError.message}")
                        cont.resume(KakaoLoginCredential(accessToken = token.accessToken))
                    } else {
                        Log.d("KakaoLogin", "✅ 사용자 정보 조회 성공 | name=${user?.kakaoAccount?.profile?.nickname}")
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
    return try {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
        val idToken = account.idToken
            ?: throw Exception("Google ID Token이 null입니다. Google Cloud Console에 Android OAuth 클라이언트 등록 필요 (패키지: com.mongle.android)")
        Log.d("GoogleLogin", "✅ 토큰 획득 성공 | email=${account.email} | token=${idToken.take(30)}...")
        GoogleLoginCredential(
            idToken = idToken,
            name = account.displayName,
            email = account.email
        )
    } catch (e: ApiException) {
        val desc = CommonStatusCodes.getStatusCodeString(e.statusCode)
        Log.e("GoogleLogin", "❌ Google Sign-In 실패 | statusCode=${e.statusCode} ($desc)")
        val hint = when (e.statusCode) {
            10 -> " → DEVELOPER_ERROR: SHA-1 미등록 또는 패키지명 불일치. Google Cloud Console에서 Android OAuth 클라이언트 확인 필요"
            12501 -> " → 사용자가 로그인을 취소했습니다"
            12500 -> " → Sign-In 실패 (일반). Google Play Services 버전 확인 필요"
            7 -> " → 네트워크 연결 오류"
            else -> ""
        }
        throw Exception("Google 로그인 실패 [${e.statusCode}: $desc]$hint")
    }
}

fun signOutGoogle(context: Context, webClientId: String) {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    GoogleSignIn.getClient(context, gso).signOut()
}
