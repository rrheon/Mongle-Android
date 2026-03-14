package com.mongle.android.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

// ── 요청 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class SocialLoginRequest(
    val provider: String,
    val access_token: String? = null,
    val id_token: String? = null,
    val identity_token: String? = null,
    val authorization_code: String? = null,
    val name: String? = null,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class EmailLoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class EmailSignupRequest(
    val name: String,
    val email: String,
    val password: String
)

// ── 응답 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiUserResponse(
    val id: String,
    val email: String,
    val name: String,
    @Json(name = "profileImageUrl") val profileImageUrl: String?,
    val role: String,
    val familyId: String?,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val user: ApiUserResponse,
    val token: String,
    val refresh_token: String?
)

// ── Retrofit 인터페이스 ──────────────────────────────

interface MongleApiService {

    @POST("auth/social")
    suspend fun socialLogin(@Body body: SocialLoginRequest): AuthResponse

    @POST("auth/email/login")
    suspend fun emailLogin(@Body body: EmailLoginRequest): AuthResponse

    @POST("auth/email/signup")
    suspend fun emailSignup(@Body body: EmailSignupRequest): AuthResponse
}
