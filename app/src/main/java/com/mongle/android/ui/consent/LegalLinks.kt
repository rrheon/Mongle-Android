package com.mongle.android.ui.consent

import java.util.Locale

/**
 * 약관/개인정보 처리방침 노션 링크.
 *
 * 한국어(ko) / 영어(en) / 일본어(ja) 별 개별 페이지가 Notion 에 정리되어 있으며,
 * 기기 [Locale] 에 따라 자동 선택된다. 지원하지 않는 언어는 영어로 폴백.
 *
 * 호출 예:
 * ```
 * openLegalUrl(context, LegalLinks.termsUrl())
 * openLegalUrl(context, LegalLinks.privacyUrl())
 * ```
 */
object LegalLinks {

    // ── Terms of Service ─────────────────────────────────────
    private const val TERMS_KO = "https://bedecked-latency-99c.notion.site/terms-ko-33c4d36af6f68054a527c510d4f98b7f"
    private const val TERMS_EN = "https://bedecked-latency-99c.notion.site/terms-en-33c4d36af6f6807eae71c6f530a4457d"
    private const val TERMS_JA = "https://bedecked-latency-99c.notion.site/terms-ja-33c4d36af6f680478c85e99a04fa629c"

    // ── Privacy Policy ───────────────────────────────────────
    private const val PRIVACY_KO = "https://bedecked-latency-99c.notion.site/privacy-policy-ko-33c4d36af6f680ffb927c10bc5d7bd1b"
    private const val PRIVACY_EN = "https://bedecked-latency-99c.notion.site/privacy-policy-en-33c4d36af6f680c1a224efb392dc488e"
    private const val PRIVACY_JA = "https://bedecked-latency-99c.notion.site/privacy-policy-ja-33c4d36af6f680e89cfbec8fb659d491"

    /** 현재 기기 언어에 맞는 서비스 이용약관 URL. */
    fun termsUrl(): String = when (currentLegalLang()) {
        LegalLang.KO -> TERMS_KO
        LegalLang.JA -> TERMS_JA
        LegalLang.EN -> TERMS_EN
    }

    /** 현재 기기 언어에 맞는 개인정보 처리방침 URL. */
    fun privacyUrl(): String = when (currentLegalLang()) {
        LegalLang.KO -> PRIVACY_KO
        LegalLang.JA -> PRIVACY_JA
        LegalLang.EN -> PRIVACY_EN
    }

    private enum class LegalLang { KO, EN, JA }

    private fun currentLegalLang(): LegalLang =
        when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
            "ko" -> LegalLang.KO
            "ja" -> LegalLang.JA
            else -> LegalLang.EN
        }
}
