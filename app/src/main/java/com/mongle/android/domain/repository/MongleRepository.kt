package com.mongle.android.domain.repository

import com.mongle.android.domain.model.Member
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.User
import java.util.UUID

interface MongleRepository {
    suspend fun create(family: MongleGroup): MongleGroup
    suspend fun get(id: UUID): MongleGroup
    suspend fun findByInviteCode(inviteCode: String): MongleGroup?
    suspend fun getFamiliesByUserId(userId: UUID): List<MongleGroup>
    suspend fun update(family: MongleGroup): MongleGroup
    suspend fun delete(id: UUID)
    suspend fun addMember(member: Member)
    suspend fun removeMember(userId: UUID, familyId: UUID)
    suspend fun getMembers(familyId: UUID): List<Member>
    suspend fun isMember(userId: UUID, familyId: UUID): Boolean

    /** 현재 인증된 유저의 가족을 구성원 목록과 함께 조회. 가족이 없으면 null. */
    suspend fun getMyFamily(): Pair<MongleGroup, List<User>>?

    /** 내 모든 가족 목록 조회 (최대 3개) */
    suspend fun getMyFamilies(): List<MongleGroup>

    /** 활성 가족 전환 */
    suspend fun selectFamily(familyId: java.util.UUID): MongleGroup

    /** 방장이 멤버를 강제 탈퇴. */
    suspend fun kickMember(memberId: java.util.UUID)

    /** 현재 활성 가족에서 나가기. DELETE /families/leave */
    suspend fun leaveFamily()

    /** 방장 위임 — 현재 방장이 다른 멤버에게 방장 권한을 넘김. PATCH /families/transfer-creator */
    suspend fun transferCreator(newCreatorId: java.util.UUID)
}

sealed class MongleError(message: String) : Exception(message) {
    data object FamilyNotFound : MongleError("가족을 찾을 수 없습니다.")
    data object InvalidInviteCode : MongleError("유효하지 않은 초대 코드입니다.")
    data object AlreadyMember : MongleError("이미 가족 구성원입니다.")
    data object NotMember : MongleError("가족 구성원이 아닙니다.")
    data object CannotLeaveAsOnlyAdmin : MongleError("유일한 관리자는 가족을 떠날 수 없습니다.")
    data object NetworkError : MongleError("네트워크 연결을 확인해주세요.")
    data class Unknown(val msg: String) : MongleError(msg)
}
