package com.mongle.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.mongle.android.ui.navigation.MongleNavHost
import com.mongle.android.ui.root.RootViewModel
import com.mongle.android.ui.theme.MongleTheme
import com.mongle.android.util.AdManager
import com.mongle.android.util.ConsentManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var consentManager: ConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        adManager.setActivity(this)
        // GDPR/CCPA 동의 흐름(UMP). KR·JP 는 건너뛰고 즉시 AdMob 초기화,
        // 그 외 지역은 동의 폼 표시 후 AdMob 초기화한다.
        consentManager.gatherConsent(this)
        intent?.data?.let { uri -> rootViewModel.handleDeepLink(uri) }
        intent?.getStringExtra("notification_type")?.let { rootViewModel.handleNotificationTap(it) }
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
        intent.getStringExtra("notification_type")?.let { rootViewModel.handleNotificationTap(it) }
    }
}
