package com.mongle.android.domain.repository

import com.mongle.android.domain.model.DailyQuestion
import java.util.Date
import java.util.UUID

interface DailyQuestionRepository {
    suspend fun create(dailyQuestion: DailyQuestion): DailyQuestion
    suspend fun get(id: UUID): DailyQuestion
    suspend fun getByFamilyAndDate(familyId: UUID, date: Date): DailyQuestion?
    suspend fun getHistoryByFamily(familyId: UUID, limit: Int? = null): List<DailyQuestion>
    suspend fun getLastQuestionOrder(familyId: UUID): Int?
    suspend fun update(dailyQuestion: DailyQuestion): DailyQuestion
    suspend fun delete(id: UUID)
    suspend fun getCompletedByFamily(familyId: UUID): List<DailyQuestion>
    suspend fun getIncompleteByFamily(familyId: UUID): List<DailyQuestion>
}

sealed class DailyQuestionError(message: String) : Exception(message) {
    data object DailyQuestionNotFound : DailyQuestionError("오늘의 질문을 찾을 수 없습니다.")
    data object AlreadyExists : DailyQuestionError("오늘의 질문이 이미 존재합니다.")
    data object NetworkError : DailyQuestionError("네트워크 연결을 확인해주세요.")
    data class Unknown(val msg: String) : DailyQuestionError(msg)
}
