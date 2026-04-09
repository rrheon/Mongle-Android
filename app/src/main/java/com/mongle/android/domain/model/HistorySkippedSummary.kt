package com.mongle.android.domain.model

/**
 * HISTORY 화면에서 "이 날 질문을 넘긴(skip)" 멤버 1명을 표현한다.
 * daily-history 응답의 memberAnswerStatuses 항목 중 status == "skipped" 인 것들이 매핑된다.
 */
data class HistorySkippedSummary(
    val userId: String,
    val userName: String,
    /** 사용자가 본인 캐릭터에 지정한 색상 (loved/happy/calm/sad/tired). */
    val colorId: String? = null
)
