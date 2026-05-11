package com.mongle.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.mongle.android.data.remote.ConfigRepository
import com.mongle.android.ui.navigation.MongleNavHost
import com.mongle.android.ui.root.RootViewModel
import com.mongle.android.ui.theme.MongleTheme
import com.mongle.android.util.AdManager
import com.mongle.android.util.ConsentManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var configRepository: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        adManager.setActivity(this)
        // 서버 /config (MG-132) — 광고 토글 캐시 갱신. 동의 흐름보다 먼저 시작해
        // 다음 부팅부터는 최신값으로 ConsentManager / AdBannerSection 분기가 동작.
        // 실패는 silent — AdConfigStore 의 기본값(true) 또는 직전 캐시 유지.
        lifecycleScope.launch { configRepository.refresh() }
        // GDPR/CCPA 동의 흐름(UMP). KR·JP 는 건너뛰고 즉시 AdMob 초기화,
        // 그 외 지역은 동의 폼 표시 후 AdMob 초기화한다.
        consentManager.gatherConsent(this)
        intent?.data?.let { uri -> rootViewModel.handleDeepLink(uri) }
        rootViewModel.handleNotificationTap(
            intent?.getStringExtra("notification_type"),
            intent?.getStringExtra("notification_id")
        )
        setContent {
            MongleTheme {
                MongleNavHost(
                    rootViewModel = rootViewModel,
                    adManager = adManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adManager.setActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        adManager.setActivity(null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri -> rootViewModel.handleDeepLink(uri) }
        rootViewModel.handleNotificationTap(
            intent.getStringExtra("notification_type"),
            intent.getStringExtra("notification_id")
        )
    }
}
