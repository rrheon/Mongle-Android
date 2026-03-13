package com.mongle.android.domain.repository

import com.mongle.android.domain.model.User
import java.util.UUID

interface UserRepository {
    suspend fun get(id: UUID): User
    suspend fun update(user: User): User
}

sealed class UserError(message: String) : Exception(message) {
    data object UserNotFound : UserError("사용자를 찾을 수 없습니다.")
    data object UpdateFailed : UserError("사용자 정보 업데이트에 실패했습니다.")
    data object NetworkError : UserError("네트워크 연결을 확인해주세요.")
    data class Unknown(val msg: String) : UserError(msg)
}
