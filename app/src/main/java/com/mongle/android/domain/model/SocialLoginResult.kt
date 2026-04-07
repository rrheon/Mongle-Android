package com.mongle.android.domain.model

/**
 * 소셜 로그인 응답.
 * 사용자 정보 + 약관 동의 필요 여부 + 현재 약관 버전.
 *
 * needsConsent=true 면 RootViewModel 이 ConsentScreen 으로 라우팅하고,
 * 사용자가 동의를 마친 후 AuthRepository.submitConsent() 를 호출한다.
 */
data class SocialLoginResult(
    val user: User,
    val needsConsent: Boolean,
    val requiredConsents: List<LegalDocType>,
    val legalVersions: LegalVersions
)

enum class LegalDocType(val key: String) {
    TERMS("terms"),
    PRIVACY("privacy");

    companion object {
        fun fromKey(key: String?): LegalDocType? =
            entries.firstOrNull { it.key == key }
    }
}

data class LegalVersions(
    val terms: String,
    val privacy: String
)
