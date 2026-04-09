package com.mongle.android.domain.model

data class HistoryAnswerSummary(
    val id: String,
    val userId: String,
    val userName: String,
    val content: String,
    val imageUrl: String? = null,
    val moodId: String? = null,
    /** 사용자가 본인 캐릭터에 지정한 색상 (loved/happy/calm/sad/tired). 서버 memberAnswerStatuses 에서 조인. */
    val colorId: String? = null
)
