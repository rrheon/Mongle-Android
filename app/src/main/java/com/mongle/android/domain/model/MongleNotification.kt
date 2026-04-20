package com.mongle.android.domain.model

import java.util.Date
import java.util.UUID

data class MongleNotification(
    val id: UUID,
    val userId: UUID,
    val type: NotificationType,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: Date
)

enum class NotificationType {
    NEW_QUESTION,
    MEMBER_ANSWERED,   // 가족 구성원이 답변했을 때
    ALL_ANSWERED,      // 모든 구성원이 답변 완료했을 때
    ANSWER_REQUEST,    // 누군가 나에게 답변 요청할 때
    TREE_GROWTH,
    BADGE_EARNED,
    REMINDER           // 서버 스케줄러가 발송하는 일반 리마인더 (답변 독려 등)
}
