package com.mongle.android.ui.common
import com.ycompany.Monggle.BuildConfig

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "AdBanner"
private val BANNER_AD_UNIT_ID = BuildConfig.ADMOB_BANNER_ID

@Composable
fun AdBannerSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BANNER_AD_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d(TAG, "onAdLoaded: unitId=$BANNER_AD_UNIT_ID")
                        }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(
                                TAG,
                                "onAdFailedToLoad: code=${error.code} domain=${error.domain} " +
                                    "msg=${error.message} cause=${error.cause} unitId=$BANNER_AD_UNIT_ID"
                            )
                        }
                        override fun onAdOpened() { Log.d(TAG, "onAdOpened") }
                        override fun onAdClicked() { Log.d(TAG, "onAdClicked") }
                        override fun onAdImpression() { Log.d(TAG, "onAdImpression") }
                        override fun onAdClosed() { Log.d(TAG, "onAdClosed") }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
