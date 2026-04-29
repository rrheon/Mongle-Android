package com.mongle.android.data.remote

import android.content.SharedPreferences
import android.util.Log
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.LegalDocType
import com.mongle.android.domain.model.LegalVersions
import com.mongle.android.domain.model.SocialLoginCredential
import com.mongle.android.domain.model.SocialLoginResult
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AuthRepository
import retrofit2.HttpException
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val KEY_TOKEN = "auth_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_ID = "user_id"
private const val KEY_USER_EMAIL = "user_email"
private const val KEY_USER_NAME = "user_name"
private const val KEY_USER_ROLE = "user_role"

@Singleton
class ApiAuthRepository @Inject constructor(
    private val api: MongleApiService,
    // MG-95 EncryptedSharedPreferences 인스턴스를 SecurityModule 에서 주입.
    @Named("auth") private val prefs: SharedPreferences
) : AuthRepository {

    private fun saveSession(user: ApiUserResponse, token: String, refreshToken: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .also { editor -> refreshToken?.let { editor.putString(KEY_REFRESH_TOKEN, it) } }
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_ROLE, user.role)
            .apply()
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun ApiUserResponse.toDomain(): User = User(
        id = UUID.fromString(id),
        email = email,
        name = name,
        profileImageUrl = profileImageUrl,
        role = FamilyRole.entries.firstOrNull { it.name == role } ?: FamilyRole.OTHER,
        hearts = hearts,
        moodId = moodId,
        createdAt = Date(),
        heartGrantedToday = heartGrantedToday ?: false,
        heartsWriteCost = heartsWriteCost,
        heartsSkipCost = heartsSkipCost
    )

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string() ?: e.message()
            Log.e("MongleApi", "❌ HTTP ${e.code()}")
            // MG-102 cause 로 HttpException 보존 → RootViewModel 등이 e.cause.code() 로 401 분기 가능.
            throw Exception(parseServerMessage(body), e)
        } catch (e: Exception) {
            Log.e("MongleApi", "❌ API 호출 실패: ${e.message}", e)
            throw e
        }
    }

    override suspend fun socialLogin(credential: SocialLoginCredential): SocialLoginResult {
        return safeCall {
            val request = when (credential.providerType) {
                SocialProviderType.KAKAO -> SocialLoginRequest(
                    provider = "kakao",
                    access_token = credential.fields["access_token"],
                    name = credential.fields["name"],
                    email = credential.fields["email"]
                )
                SocialProviderType.GOOGLE -> SocialLoginRequest(
                    provider = "google",
                    id_token = credential.fields["id_token"],
                    name = credential.fields["name"],
                    email = credential.fields["email"]
                )
                SocialProviderType.APPLE -> SocialLoginRequest(
                    provider = "apple",
                    identity_token = credential.fields["identity_token"],
                    authorization_code = credential.fields["authorization_code"],
                    name = credential.fields["name"],
                    email = credential.fields["email"]
                )
            }
            val response = api.socialLogin(request)
            saveSession(response.user, response.token, response.refresh_token)
            val required = response.requiredConsents.orEmpty()
                .mapNotNull { LegalDocType.fromKey(it) }
            // 서버가 legalVersions 를 안 내려주면 빈 문자열로 두고 needsConsent=false 로 안전 동작
            val versions = LegalVersions(
                terms = response.legalVersions?.terms.orEmpty(),
                privacy = response.legalVersions?.privacy.orEmpty()
            )
            SocialLoginResult(
                user = response.user.toDomain(),
                needsConsent = response.needsConsent ?: false,
                requiredConsents = required,
                legalVersions = versions
            )
        }
    }

    override suspend fun submitConsent(termsVersion: String?, privacyVersion: String?) {
        safeCall {
            api.submitConsent(ConsentRequest(termsVersion = termsVersion, privacyVersion = privacyVersion))
        }
    }

    // region Email Auth

    override suspend fun requestEmailSignupCode(email: String) {
        safeCall { api.requestEmailCode(EmailRequestCodeBody(email = email)) }
    }

    override suspend fun emailSignup(
        email: String,
        password: String,
        code: String,
        name: String?,
        termsVersion: String,
        privacyVersion: String
    ): SocialLoginResult {
        return safeCall {
            val response = api.emailSignup(
                EmailSignupBody(
                    email = email,
                    password = password,
                    code = code,
                    name = name,
                    termsVersion = termsVersion,
                    privacyVersion = privacyVersion
                )
            )
            saveSession(response.user, response.token, response.refresh_token)
            val required = response.requiredConsents.orEmpty()
                .mapNotNull { LegalDocType.fromKey(it) }
            val versions = LegalVersions(
                terms = response.legalVersions?.terms.orEmpty(),
                privacy = response.legalVersions?.privacy.orEmpty()
            )
            SocialLoginResult(
                user = response.user.toDomain(),
                needsConsent = response.needsConsent ?: false,
                requiredConsents = required,
                legalVersions = versions
            )
        }
    }

    override suspend fun emailLogin(email: String, password: String): SocialLoginResult {
        return safeCall {
            val response = api.emailLogin(EmailLoginBody(email = email, password = password))
            saveSession(response.user, response.token, response.refresh_token)
            val required = response.requiredConsents.orEmpty()
                .mapNotNull { LegalDocType.fromKey(it) }
            val versions = LegalVersions(
                terms = response.legalVersions?.terms.orEmpty(),
                privacy = response.legalVersions?.privacy.orEmpty()
            )
            SocialLoginResult(
                user = response.user.toDomain(),
                needsConsent = response.needsConsent ?: false,
                requiredConsents = required,
                legalVersions = versions
            )
        }
    }

    // endregion

    override suspend fun logout() {
        clearSession()
    }

    override suspend fun deleteAccount() {
        // iOS MG-35 패리티 — API 호출 성공/실패와 무관하게 로컬 세션 정리.
        // 이전에는 safeCall 이 throw 하면 clearSession() 이 실행되지 않아
        // 잔존 토큰으로 다음 사용자가 이전 계정 데이터에 접근할 위험이 있었음.
        try {
            safeCall { api.deleteAccount() }
        } finally {
            clearSession()
        }
    }

    override suspend fun getCurrentUser(grantDailyHeart: Boolean): User? {
        // 저장된 토큰이 없으면 미로그인 상태
        val token = prefs.getString(KEY_TOKEN, null) ?: return null

        return try {
            // 서버에서 최신 사용자 정보를 가져와 토큰 유효성 검증.
            // grantDailyHeart=true 호출은 RootViewModel.loadHomeData 에서만 사용하여
            // 데일리 하트 지급/heartGrantedToday 플래그를 set-only 트리거로 받는다.
            val apiUser = api.getMe(grantDailyHeart = grantDailyHeart)
            saveSession(apiUser, token, prefs.getString(KEY_REFRESH_TOKEN, null))
            apiUser.toDomain()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                // 토큰 만료 + 갱신 실패 → TokenAuthenticator가 이미 세션을 삭제함
                null
            } else {
                // 서버 오류 등 → 캐시된 사용자 정보로 오프라인 진행
                buildUserFromCache()
            }
        } catch (e: Exception) {
            // 네트워크 없음 → 캐시된 사용자 정보로 오프라인 진행
            buildUserFromCache()
        }
    }

    private fun buildUserFromCache(): User? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, null) ?: return null
        val role = prefs.getString(KEY_USER_ROLE, null)
        return User(
            id = UUID.fromString(id),
            email = email,
            name = name,
            profileImageUrl = null,
            role = FamilyRole.entries.firstOrNull { it.name == role } ?: FamilyRole.OTHER,
            createdAt = Date()
        )
    }
}
