package com.mongle.android.util

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.mongle.android.BuildConfig
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google UMP (User Messaging Platform) 래퍼.
 *
 * GDPR(EEA/UK) · CCPA/CPRA(US) 등 동의 수집이 필요한 지역에서 AdMob 개인화 광고 노출 전
 * 동의 팝업을 표시한다. 한국·일본 사용자에 대해서는 UMP 흐름을 건너뛰고 AdMob 을 즉시
 * 초기화한다 (해당 지역은 UMP 동의 요구 대상이 아니며, 불필요한 네트워크 호출/지연을 방지).
 *
 * MainActivity.onCreate 에서 [gatherConsent] 를 호출하면 된다. 이 메서드는 내부에서
 * AdManager.initialize() 를 호출하므로, MongleApplication 에서 AdManager 를 초기화하던
 * 기존 흐름은 ConsentManager 경로로 일원화된다.
 */
@Singleton
class ConsentManager @Inject constructor(
    private val adManager: AdManager
) {
    companion object {
        /** UMP 동의 흐름을 건너뛰는 지역(ISO 3166-1 alpha-2). */
        private val EXEMPTED_REGIONS = setOf("KR", "JP")
        private const val TAG = "ConsentManager"

        // ── 디버그 설정 (DEBUG 빌드에서만 활성화) ─────────────────────────
        //
        // 사용법:
        // 1. [DEBUG_GEOGRAPHY_ENABLED] 를 true 로 변경
        // 2. [DEBUG_GEOGRAPHY] 를 원하는 값으로 변경
        //    (DEBUG_GEOGRAPHY_EEA, DEBUG_GEOGRAPHY_REGULATED_US_STATE, DEBUG_GEOGRAPHY_DISABLED)
        // 3. [DEBUG_TEST_DEVICE_HASHES] 에 실기기 해시 추가
        //    (앱 첫 실행 시 Logcat 에 "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId(...)" 메시지로 출력됨)
        // 4. 앱 삭제 후 재설치해야 이전 동의 상태가 초기화되어 폼이 다시 표시됨
        //
        // 배포 전 반드시 DEBUG_GEOGRAPHY_ENABLED = false 로 되돌릴 것.
        private const val DEBUG_GEOGRAPHY_ENABLED = false
        private const val DEBUG_GEOGRAPHY = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
        private val DEBUG_TEST_DEVICE_HASHES = listOf<String>(
            // 예: "33BE2250-B28F-4B5C-8A96-AB7E5DC7E5E7"
        )
    }

    @Volatile
    private var isMobileAdsStarted = false

    /**
     * 앱 진입 시 한 번 호출한다.
     * - 기기 지역이 KR/JP 이면 UMP 를 건너뛰고 즉시 AdMob 초기화
     * - 그 외 지역이면 UMP 로 동의 상태를 갱신하고, 필요 시 동의 폼을 표시한 뒤 AdMob 초기화
     *
     * 동의 거부/오류 상황에서도 AdMob 은 반드시 초기화한다 (UMP 가 비개인화 광고로 자동 전환).
     */
    fun gatherConsent(activity: Activity) {
        // DEBUG 빌드에서 디버그 지역 시뮬레이션이 활성화된 경우, KR/JP 면제 로직을 건너뛴다.
        val debugActive = BuildConfig.DEBUG && DEBUG_GEOGRAPHY_ENABLED
        if (!debugActive && isExemptedRegion()) {
            startMobileAdsIfNeeded()
            return
        }

        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (debugActive) {
            val debugBuilder = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(DEBUG_GEOGRAPHY)
            DEBUG_TEST_DEVICE_HASHES.forEach { debugBuilder.addTestDeviceHashedId(it) }
            paramsBuilder.setConsentDebugSettings(debugBuilder.build())
        }

        val params = paramsBuilder.build()

        val consentInformation: ConsentInformation =
            UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // 상태 업데이트 성공 → 필요 시 동의 폼 노출
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "loadAndShowConsentFormIfRequired: ${formError.message}")
                    }
                    // 동의 결과와 무관하게 AdMob 초기화.
                    if (consentInformation.canRequestAds()) {
                        startMobileAdsIfNeeded()
                    } else {
                        // 거부 시에도 비개인화 광고로 동작시키기 위해 초기화는 수행.
                        startMobileAdsIfNeeded()
                    }
                }
            },
            { requestError ->
                // 네트워크 오류 등으로 상태 조회 실패 → 서비스 연속성을 위해 초기화는 수행.
                Log.w(TAG, "requestConsentInfoUpdate: ${requestError.message}")
                startMobileAdsIfNeeded()
            }
        )
    }

    /**
     * 앱 내 "개인정보 보호 옵션 / Privacy options" 재동의 버튼에서 호출한다.
     * UMP 가 폼 재표시를 허용하는 경우에만 실제로 폼이 열린다.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onComplete: (errorMessage: String?) -> Unit = {}
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            onComplete(error?.message)
        }
    }

    /**
     * 현재 설정에서 "Privacy options" 버튼을 보여줘야 하는지 여부.
     * (GDPR/CCPA 대상 사용자에게만 true)
     */
    fun isPrivacyOptionsRequired(activity: Activity): Boolean {
        return UserMessagingPlatform.getConsentInformation(activity)
            .privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    // region Private

    @Synchronized
    private fun startMobileAdsIfNeeded() {
        if (isMobileAdsStarted) return
        isMobileAdsStarted = true
        adManager.initialize()
    }

    private fun isExemptedRegion(): Boolean {
        val country = Locale.getDefault().country.uppercase(Locale.ROOT)
        return country in EXEMPTED_REGIONS
    }

    // endregion
}
