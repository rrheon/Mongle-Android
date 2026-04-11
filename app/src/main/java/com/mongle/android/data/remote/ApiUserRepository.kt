package com.mongle.android.data.remote

import com.mongle.android.domain.model.BadgeCategory
import com.mongle.android.domain.model.BadgeDefaults
import com.mongle.android.domain.model.BadgeDefinition
import com.mongle.android.domain.model.BadgeDisplayItem
import com.mongle.android.domain.model.CharacterStageInfo
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.User
import com.mongle.android.domain.model.UserBadge
import com.mongle.android.domain.repository.DailyHeartResult
import com.mongle.android.domain.repository.UserRepository
import retrofit2.HttpException
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiUserRepository @Inject constructor(
    private val api: MongleApiService
) : UserRepository {

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val raw = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(parseServerMessage(raw))
        }
    }

    private fun ApiUserResponse.toDomain(): User = User(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.randomUUID() },
        email = email,
        name = name,
        profileImageUrl = profileImageUrl,
        role = FamilyRole.entries.firstOrNull { it.name == role } ?: FamilyRole.OTHER,
        hearts = hearts,
        moodId = moodId,
        createdAt = Date()
    )

    override suspend fun get(id: UUID): User = safeCall {
        api.getMe().toDomain()
    }

    override suspend fun update(user: User): User = safeCall {
        val response = api.updateMe(
            UpdateUserRequest(
                name = user.name,
                role = user.role.name,
                moodId = user.moodId
            )
        )
        response.toDomain()
    }

    suspend fun getMe(): User = safeCall {
        api.getMe().toDomain()
    }

    suspend fun getMyStreak(): Int = safeCall {
        api.getMyStreak().streakDays
    }

    /**
     * 캐릭터 성장 스테이지 조회. 서버 API(MG Engine-4)가 아직 배포되지 않았으면
     * `/users/me/streak` 결과로 클라이언트에서 매핑한다. 서버 롤아웃 후 자연 전환.
     */
    suspend fun getMyCharacterStage(): CharacterStageInfo = safeCall {
        try {
            val r = api.getMyCharacterStage()
            CharacterStageInfo(
                stage = r.stage,
                stageKey = r.stageKey,
                streakDays = r.streakDays,
                nextStageStreak = r.nextStageStreak,
                sizeMultiplier = r.sizeMultiplier.toFloat()
            )
        } catch (e: HttpException) {
            // 404 등: 서버 미배포 시 streak 으로 폴백
            if (e.code() == 404) {
                val days = api.getMyStreak().streakDays
                CharacterStageInfo.fromStreak(days)
            } else throw e
        }
    }

    suspend fun grantAdHearts(amount: Int): Int = safeCall {
        api.grantAdHearts(AdHeartRewardRequest(amount)).heartsRemaining
    }

    suspend fun registerFcmToken(token: String) = safeCall {
        api.registerDeviceToken(RegisterDeviceTokenRequest(token))
    }

    /**
     * 배지 목록 조회. 서버 API(MG Engine-4)가 아직 없으면 정의만 [BadgeDefaults]에서 가져오고
     * 획득 목록은 빈 리스트로 둔다 — 화면이 "전부 잠금" 상태로 그려지도록 한다.
     */
    suspend fun getBadges(): List<BadgeDisplayItem> = safeCall {
        try {
            val resp = api.getMyBadges()
            val defs = resp.definitions.map { it.toDomain() }
            val owned = resp.badges.associateBy { it.code }
            defs.map { def ->
                val owner = owned[def.code]
                BadgeDisplayItem(
                    definition = def,
                    awarded = owner?.toDomain()
                )
            }
        } catch (e: HttpException) {
            if (e.code() == 404) {
                BadgeDefaults.seed.map { BadgeDisplayItem(definition = it, awarded = null) }
            } else throw e
        }
    }

    /**
     * 알림 옵트아웃 토글 저장. Engine-8 (`PUT /users/me`) 확장에 의존.
     * 미배포 환경에서는 silent 실패하여 로컬 SharedPreferences 만 유지된다.
     */
    suspend fun updateNotificationPrefs(streakRiskNotify: Boolean?, badgeEarnedNotify: Boolean?) {
        try {
            api.updateMe(
                UpdateUserRequest(
                    streakRiskNotify = streakRiskNotify,
                    badgeEarnedNotify = badgeEarnedNotify
                )
            )
        } catch (_: Exception) {
            // 서버 미배포/네트워크 오류는 silent. 로컬 토글 상태로 유지.
        }
    }

    suspend fun markBadgesSeen(codes: List<String>) {
        if (codes.isEmpty()) return
        try {
            api.markBadgesSeen(MarkBadgesSeenRequest(codes))
        } catch (_: Exception) {
            // 미배포 환경에서는 silent. 서버 도착 후 자연 동작.
        }
    }

    private fun BadgeDefinitionDto.toDomain(): BadgeDefinition = BadgeDefinition(
        code = code,
        category = runCatching { BadgeCategory.valueOf(category) }.getOrDefault(BadgeCategory.UNKNOWN),
        iconKey = iconKey
    )

    private fun UserBadgeDto.toDomain(): UserBadge = UserBadge(
        code = code,
        awardedAt = parseIso(awardedAt) ?: Date(),
        seenAt = seenAt?.let { parseIso(it) }
    )

    private fun parseIso(s: String): Date? = runCatching {
        // 서버는 ISO-8601 (UTC). java.time 사용으로 minSdk 26 OK.
        Date.from(java.time.OffsetDateTime.parse(s).toInstant())
    }.getOrNull()

    override suspend fun claimDailyHeart(): DailyHeartResult? = try {
        val resp = api.claimDailyHeart()
        if (resp.alreadyClaimed || resp.heartsGranted <= 0) null
        else DailyHeartResult(resp.heartsGranted, resp.heartsRemaining)
    } catch (e: HttpException) {
        // 409 Conflict = 이미 오늘 수령함
        if (e.code() == 409 || e.code() == 400) null else null
    } catch (e: Exception) {
        null
    }
}
