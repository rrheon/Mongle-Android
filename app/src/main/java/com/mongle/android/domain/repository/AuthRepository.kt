package com.mongle.android.domain.repository

import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.SocialLoginCredential
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): User
    suspend fun signup(name: String, email: String, password: String, role: FamilyRole): User
    suspend fun socialLogin(credential: SocialLoginCredential): User
    suspend fun logout()
    suspend fun deleteAccount()
    suspend fun getCurrentUser(): User?
}

sealed class AuthError(message: String) : Exception(message) {
    data object InvalidCredentials : AuthError("이메일 또는 비밀번호가 올바르지 않습니다.")
    data object EmailAlreadyExists : AuthError("이미 사용 중인 이메일입니다.")
    data object WeakPassword : AuthError("비밀번호는 최소 6자 이상이어야 합니다.")
    data object NetworkError : AuthError("네트워크 연결을 확인해주세요.")
    data object UserNotFound : AuthError("사용자를 찾을 수 없습니다.")
    data class SocialLoginFailed(val provider: SocialProviderType) :
        AuthError("${provider.value} 로그인에 실패했습니다.")
    data object AccountDeletionFailed : AuthError("계정 삭제에 실패했습니다. 다시 시도해주세요.")
    data class Unknown(val msg: String) : AuthError(msg)
}
