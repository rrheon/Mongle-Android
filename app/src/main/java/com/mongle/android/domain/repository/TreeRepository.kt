package com.mongle.android.domain.repository

import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.TreeStage
import java.util.UUID

interface TreeRepository {
    suspend fun create(treeProgress: TreeProgress): TreeProgress
    suspend fun get(id: UUID): TreeProgress
    suspend fun getByFamilyId(familyId: UUID): TreeProgress?
    suspend fun update(treeProgress: TreeProgress): TreeProgress
    suspend fun delete(id: UUID)
    suspend fun countByStage(stage: TreeStage): Int
    /** 현재 인증된 유저의 가족 나무 진행도 조회. 없으면 null. */
    suspend fun getMyTreeProgress(): TreeProgress?
}

sealed class TreeError(message: String) : Exception(message) {
    data object TreeNotFound : TreeError("나무 정보를 찾을 수 없습니다.")
    data object AlreadyExists : TreeError("이미 나무가 존재합니다.")
    data object NetworkError : TreeError("네트워크 연결을 확인해주세요.")
    data class Unknown(val msg: String) : TreeError(msg)
}
