package com.mongle.android.domain.repository

import com.mongle.android.domain.model.SocialLoginCredential
import com.mongle.android.domain.model.SocialLoginResult
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User

interface AuthRepository {
    /**
     * 소셜 로그인. 반환값에 needsConsent 가 포함되며, true 면
     * 클라이언트는 ConsentScreen 으로 라우팅 후 submitConsent 를 호출해야 한다.
     */
    suspend fun socialLogin(credential: SocialLoginCredential): SocialLoginResult
    suspend fun logout()
    suspend fun deleteAccount()

    /**
     * 현재 사용자 정보 조회.
     * @param grantDailyHeart true 면 서버가 데일리 하트를 지급(또는 이미 받음) 후
     *                        heartGrantedToday 플래그를 응답에 포함. RootViewModel.loadHomeData
     *                        같은 단일 진입점에서만 true 로 호출해야 한다.
     */
    suspend fun getCurrentUser(grantDailyHeart: Boolean = false): User?

    /**
     * 약관/개인정보 동의 저장.
     * @param termsVersion null 이면 약관 동의 미갱신
     * @param privacyVersion null 이면 개인정보 동의 미갱신
     */
    suspend fun submitConsent(termsVersion: String?, privacyVersion: String?)

    // region Email Auth

    /**
     * 이메일 회원가입 6자리 인증코드 발송.
     * 이미 가입된 이메일이면 예외 발생.
     */
    suspend fun requestEmailSignupCode(email: String)

    /**
     * 이메일 회원가입 완료. 인증코드 + 약관 버전 검증 후 유저 생성.
     */
    suspend fun emailSignup(
        email: String,
        password: String,
        code: String,
        name: String?,
        termsVersion: String,
        privacyVersion: String
    ): SocialLoginResult

    /** 이메일/비밀번호 로그인 (기존 회원) */
    suspend fun emailLogin(email: String, password: String): SocialLoginResult

    // endregion
}

sealed class AuthError(message: String) : Exception(message) {
    data object NetworkError : AuthError("네트워크 연결을 확인해주세요.")
    data object UserNotFound : AuthError("사용자를 찾을 수 없습니다.")
    data class SocialLoginFailed(val provider: SocialProviderType) :
        AuthError("${provider.value} 로그인에 실패했습니다.")
    data object AccountDeletionFailed : AuthError("계정 삭제에 실패했습니다. 다시 시도해주세요.")
    data class Unknown(val msg: String) : AuthError(msg)
}
