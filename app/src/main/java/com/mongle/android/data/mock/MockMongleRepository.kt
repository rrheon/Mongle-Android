package com.mongle.android.data.mock

import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.Member
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.MongleRepository
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockMongleRepository @Inject constructor() : MongleRepository {

    private val familyId = UUID.fromString("00000000-0000-0000-0000-000000000010")
    private val treeProgressId = UUID.fromString("00000000-0000-0000-0000-000000000020")

    private val mockMembers = listOf(
        User(UUID.fromString("00000000-0000-0000-0000-000000000001"), "dad@mongle.com", "아빠", null, FamilyRole.FATHER, Date()),
        User(UUID.fromString("00000000-0000-0000-0000-000000000002"), "mom@mongle.com", "엄마", null, FamilyRole.MOTHER, Date()),
        User(UUID.fromString("00000000-0000-0000-0000-000000000003"), "son@mongle.com", "아들", null, FamilyRole.SON, Date()),
        User(UUID.fromString("00000000-0000-0000-0000-000000000004"), "daughter@mongle.com", "딸", null, FamilyRole.DAUGHTER, Date())
    )

    private val mockFamily = MongleGroup(
        id = familyId,
        name = "행복한 우리 가족",
        memberIds = mockMembers.map { it.id },
        createdBy = mockMembers[0].id,
        createdAt = Date(),
        inviteCode = "MONGLE123",
        groupProgressId = treeProgressId
    )

    override suspend fun create(family: MongleGroup): MongleGroup {
        delay(500)
        return family
    }

    override suspend fun get(id: UUID): MongleGroup {
        delay(300)
        return mockFamily
    }

    override suspend fun findByInviteCode(inviteCode: String): MongleGroup? {
        delay(300)
        return if (inviteCode == mockFamily.inviteCode) mockFamily else null
    }

    override suspend fun getFamiliesByUserId(userId: UUID): List<MongleGroup> {
        delay(300)
        return listOf(mockFamily)
    }

    override suspend fun update(family: MongleGroup): MongleGroup {
        delay(400)
        return family
    }

    override suspend fun delete(id: UUID) {
        delay(400)
    }

    override suspend fun addMember(member: Member) {
        delay(300)
    }

    override suspend fun removeMember(userId: UUID, familyId: UUID) {
        delay(300)
    }

    override suspend fun getMembers(familyId: UUID): List<Member> {
        delay(300)
        return mockMembers.mapIndexed { index, user ->
            Member(
                id = UUID.randomUUID(),
                userId = user.id,
                familyId = familyId,
                role = user.role,
                joinedAt = Date(),
                isActive = true
            )
        }
    }

    override suspend fun isMember(userId: UUID, familyId: UUID): Boolean {
        delay(200)
        return mockMembers.any { it.id == userId }
    }

    override suspend fun getMyFamily(): Pair<MongleGroup, List<User>> {
        delay(500)
        return Pair(mockFamily, mockMembers)
    }
}
