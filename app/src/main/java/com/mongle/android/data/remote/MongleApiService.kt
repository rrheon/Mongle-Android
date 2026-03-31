package com.mongle.android.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// ── Auth 요청 ──────────────────────────────────────────

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

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    val refresh_token: String
)

// ── User 요청/응답 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiUserResponse(
    val id: String,
    val email: String,
    val name: String,
    @Json(name = "profileImageUrl") val profileImageUrl: String?,
    val role: String,
    val familyId: String?,
    val hearts: Int = 0,
    val moodId: String? = null,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val name: String? = null,
    val profileImageUrl: String? = null,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class StreakResponse(
    val streakDays: Int
)

// ── Auth 응답 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val user: ApiUserResponse,
    val token: String,
    val refresh_token: String?
)

@JsonClass(generateAdapter = true)
data class TokenRefreshResponse(
    val token: String,
    val refresh_token: String
)

// ── 질문 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class QuestionDto(
    val id: String,
    val content: String,
    val category: String,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class DailyQuestionResponse(
    val id: String,
    val question: QuestionDto,
    val date: String,
    val familyId: String,
    val isSkipped: Boolean = false,
    val skippedAt: String? = null,
    val hasMyAnswer: Boolean = false,
    val familyAnswerCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class HistoryAnswerSummaryDto(
    val id: String,
    val userId: String,
    val userName: String,
    val content: String,
    val imageUrl: String? = null,
    val moodId: String? = null
)

@JsonClass(generateAdapter = true)
data class DailyQuestionHistoryResponse(
    val id: String,
    val question: QuestionDto,
    val date: String,
    val familyId: String,
    val isSkipped: Boolean = false,
    val hasMyAnswer: Boolean = false,
    val familyAnswerCount: Int = 0,
    val answers: List<HistoryAnswerSummaryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PaginationDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

@JsonClass(generateAdapter = true)
data class QuestionHistoryPageResponse(
    val data: List<DailyQuestionHistoryResponse>,
    val pagination: PaginationDto
)

@JsonClass(generateAdapter = true)
data class CreateCustomQuestionRequest(
    val content: String
)

// ── 가족 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FamilyResponse(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdById: String,
    val members: List<ApiUserResponse>,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreateFamilyRequest(
    val name: String,
    val creatorRole: String
)

@JsonClass(generateAdapter = true)
data class TransferCreatorRequest(
    val newCreatorId: String
)

@JsonClass(generateAdapter = true)
data class JoinFamilyRequest(
    val inviteCode: String,
    val role: String
)

@JsonClass(generateAdapter = true)
data class FamiliesListResponse(
    val families: List<FamilyResponse>
)

// ── 나무 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class TreeProgressResponse(
    val id: String,
    val familyId: String,
    val stage: String,
    val totalAnswers: Int,
    val nextStageAt: Int,
    val progressPercent: Int
)

// ── 답변 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AnswerResponse(
    val id: String,
    val content: String,
    val imageUrl: String? = null,
    val moodId: String? = null,
    val user: ApiUserResponse,
    val questionId: String,
    val createdAt: String,
    val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class FamilyAnswersResponse(
    val answers: List<AnswerResponse>,
    val totalCount: Int,
    val myAnswer: AnswerResponse? = null
)

@JsonClass(generateAdapter = true)
data class CreateAnswerRequest(
    val questionId: String,
    val content: String,
    val imageUrl: String? = null,
    val moodId: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateAnswerRequest(
    val content: String? = null,
    val imageUrl: String? = null,
    val moodId: String? = null
)

// ── 알림 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class GetNotificationsResponse(
    val notifications: List<NotificationDto>
)

@JsonClass(generateAdapter = true)
data class MarkAllReadResponse(
    val count: Int
)

// ── 재촉하기 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class NudgeRequest(
    val targetUserId: String
)

@JsonClass(generateAdapter = true)
data class NudgeResponse(
    val heartsRemaining: Int
)

// ── 광고 보상 하트 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AdHeartRewardRequest(
    val amount: Int
)

@JsonClass(generateAdapter = true)
data class AdHeartRewardResponse(
    val heartsRemaining: Int
)

// ── 일일 접속 하트 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class DailyHeartResponse(
    val heartsGranted: Int,
    val heartsRemaining: Int,
    val alreadyClaimed: Boolean = false
)

// ── 질문 패스 응답 ──────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class SkipQuestionResponse(
    val heartsRemaining: Int
)

// ── Retrofit 인터페이스 ──────────────────────────────

interface MongleApiService {

    // Auth
    @POST("auth/social")
    suspend fun socialLogin(@Body body: SocialLoginRequest): AuthResponse

    @POST("auth/login")
    suspend fun emailLogin(@Body body: EmailLoginRequest): AuthResponse

    @POST("auth/signup")
    suspend fun emailSignup(@Body body: EmailSignupRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): TokenRefreshResponse

    @DELETE("auth/account")
    suspend fun deleteAccount()

    // Users
    @GET("users/me")
    suspend fun getMe(): ApiUserResponse

    @PUT("users/me")
    suspend fun updateMe(@Body body: UpdateUserRequest): ApiUserResponse

    @GET("users/me/streak")
    suspend fun getMyStreak(): StreakResponse

    @POST("users/me/hearts/ad-reward")
    suspend fun grantAdHearts(@Body body: AdHeartRewardRequest): AdHeartRewardResponse

    @POST("users/me/hearts/daily")
    suspend fun claimDailyHeart(): DailyHeartResponse

    // Questions
    @GET("questions/today")
    suspend fun getTodayQuestion(): DailyQuestionResponse

    @POST("questions/skip")
    suspend fun skipQuestion(): DailyQuestionResponse

    @GET("questions")
    suspend fun getQuestionHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): QuestionHistoryPageResponse

    @POST("questions/custom")
    suspend fun createCustomQuestion(@Body body: CreateCustomQuestionRequest): DailyQuestionResponse

    // Families
    @GET("families/my")
    suspend fun getMyFamily(): FamilyResponse

    @GET("families/all")
    suspend fun getMyFamilies(): FamiliesListResponse

    @POST("families/{familyId}/select")
    suspend fun selectFamily(@Path("familyId") familyId: String): FamilyResponse

    @POST("families")
    suspend fun createFamily(@Body body: CreateFamilyRequest): FamilyResponse

    @POST("families/join")
    suspend fun joinFamily(@Body body: JoinFamilyRequest): FamilyResponse

    @DELETE("families/leave")
    suspend fun leaveFamily()

    @DELETE("families/members/{memberId}")
    suspend fun kickMember(@Path("memberId") memberId: String)

    @PATCH("families/transfer-creator")
    suspend fun transferCreator(@Body body: TransferCreatorRequest)

    // Tree
    @GET("tree/progress")
    suspend fun getTreeProgress(): TreeProgressResponse

    // Answers
    @POST("answers")
    suspend fun createAnswer(@Body body: CreateAnswerRequest): AnswerResponse

    @GET("answers/my/{questionId}")
    suspend fun getMyAnswer(@Path("questionId") questionId: String): AnswerResponse

    @GET("answers/family/{questionId}")
    suspend fun getFamilyAnswers(@Path("questionId") questionId: String): FamilyAnswersResponse

    @PUT("answers/{answerId}")
    suspend fun updateAnswer(
        @Path("answerId") answerId: String,
        @Body body: UpdateAnswerRequest
    ): AnswerResponse

    // Nudge
    @POST("nudge")
    suspend fun sendNudge(@Body body: NudgeRequest): NudgeResponse

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(@Query("limit") limit: Int = 50): GetNotificationsResponse

    @PATCH("notifications/{notificationId}/read")
    suspend fun markNotificationRead(@Path("notificationId") notificationId: String): NotificationDto

    @PATCH("notifications/read-all")
    suspend fun markAllNotificationsRead(): MarkAllReadResponse
}
