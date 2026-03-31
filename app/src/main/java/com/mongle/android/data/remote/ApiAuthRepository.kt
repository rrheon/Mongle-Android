package com.mongle.android.data.remote

import android.content.Context
import android.util.Log
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.SocialLoginCredential
import com.mongle.android.domain.model.SocialProviderType
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_NAME = "mongle_auth"
private const val KEY_TOKEN = "auth_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_ID = "user_id"
private const val KEY_USER_EMAIL = "user_email"
private const val KEY_USER_NAME = "user_name"
private const val KEY_USER_ROLE = "user_role"

@Singleton
class ApiAuthRepository @Inject constructor(
    private val api: MongleApiService,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

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
        createdAt = Date()
    )

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string() ?: e.message()
            Log.e("MongleApi", "❌ HTTP ${e.code()} | body=$body")
            throw Exception(parseServerMessage(body))
        } catch (e: Exception) {
            Log.e("MongleApi", "❌ API 호출 실패: ${e.message}", e)
            throw e
        }
    }

    override suspend fun login(email: String, password: String): User {
        return safeCall {
            val response = api.emailLogin(EmailLoginRequest(email, password))
            saveSession(response.user, response.token, response.refresh_token)
            response.user.toDomain()
        }
    }

    override suspend fun signup(name: String, email: String, password: String, role: FamilyRole): User {
        return safeCall {
            val response = api.emailSignup(EmailSignupRequest(name, email, password))
            saveSession(response.user, response.token, response.refresh_token)
            response.user.toDomain()
        }
    }

    override suspend fun socialLogin(credential: SocialLoginCredential): User {
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
                SocialProviderType.NAVER -> SocialLoginRequest(
                    provider = "naver",
                    access_token = credential.fields["access_token"]
                )
            }
            val response = api.socialLogin(request)
            saveSession(response.user, response.token, response.refresh_token)
            response.user.toDomain()
        }
    }

    override suspend fun logout() {
        clearSession()
    }

    override suspend fun deleteAccount() {
        safeCall { api.deleteAccount() }
        clearSession()
    }

    override suspend fun getCurrentUser(): User? {
        // 저장된 토큰이 없으면 미로그인 상태
        val token = prefs.getString(KEY_TOKEN, null) ?: return null

        return try {
            // 서버에서 최신 사용자 정보를 가져와 토큰 유효성 검증
            val apiUser = api.getMe()
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
