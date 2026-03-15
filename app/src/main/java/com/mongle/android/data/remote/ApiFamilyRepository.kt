package com.mongle.android.data.remote

import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.Member
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.MongleRepository
import retrofit2.HttpException
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiFamilyRepository @Inject constructor(
    private val api: MongleApiService
) : MongleRepository {

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val msg = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(msg)
        }
    }

    private fun ApiUserResponse.toDomain(): User = User(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.randomUUID() },
        email = email,
        name = name,
        profileImageUrl = profileImageUrl,
        role = FamilyRole.entries.firstOrNull { it.name == role } ?: FamilyRole.OTHER,
        createdAt = Date()
    )

    private fun FamilyResponse.toGroup(): MongleGroup = MongleGroup(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.randomUUID() },
        name = name,
        memberIds = members.map { runCatching { UUID.fromString(it.id) }.getOrElse { UUID.randomUUID() } },
        createdBy = runCatching { UUID.fromString(createdById) }.getOrElse { UUID.randomUUID() },
        createdAt = Date(),
        inviteCode = inviteCode,
        groupProgressId = UUID.randomUUID()
    )

    override suspend fun getMyFamily(): Pair<MongleGroup, List<User>>? = safeCall {
        try {
            val response = api.getMyFamily()
            Pair(response.toGroup(), response.members.map { it.toDomain() })
        } catch (e: Exception) {
            // 가족이 없는 경우 null 반환
            null
        }
    }

    override suspend fun create(family: MongleGroup): MongleGroup =
        throw UnsupportedOperationException("createFamily(name, role) 사용")

    suspend fun createFamily(name: String, creatorRole: FamilyRole): MongleGroup = safeCall {
        val response = api.createFamily(CreateFamilyRequest(name, creatorRole.name))
        response.toGroup()
    }

    suspend fun joinFamily(inviteCode: String, role: FamilyRole): MongleGroup = safeCall {
        val response = api.joinFamily(JoinFamilyRequest(inviteCode, role.name))
        response.toGroup()
    }

    suspend fun leaveFamily() = safeCall {
        api.leaveFamily()
    }

    suspend fun kickMember(memberId: String) = safeCall {
        api.kickMember(memberId)
    }

    override suspend fun get(id: UUID): MongleGroup =
        throw UnsupportedOperationException()

    override suspend fun findByInviteCode(inviteCode: String): MongleGroup? = null

    override suspend fun getFamiliesByUserId(userId: UUID): List<MongleGroup> = emptyList()

    override suspend fun update(family: MongleGroup): MongleGroup =
        throw UnsupportedOperationException()

    override suspend fun delete(id: UUID) =
        throw UnsupportedOperationException()

    override suspend fun addMember(member: Member) =
        throw UnsupportedOperationException()

    override suspend fun removeMember(userId: UUID, familyId: UUID) = safeCall {
        api.leaveFamily()
    }

    override suspend fun getMembers(familyId: UUID): List<Member> = emptyList()

    override suspend fun isMember(userId: UUID, familyId: UUID): Boolean = false

    override suspend fun kickMember(memberId: UUID) = safeCall {
        api.kickMember(memberId.toString())
    }
}
