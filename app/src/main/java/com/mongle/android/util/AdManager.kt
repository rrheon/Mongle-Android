package com.mongle.android.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val REWARDED_AD_UNIT_ID = com.mongle.android.BuildConfig.ADMOB_REWARDED_ID

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var currentActivity: Activity? = null
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        MobileAds.initialize(context)
        loadRewardedAd()
    }

    fun setActivity(activity: Activity?) {
        currentActivity = activity
    }

    private fun loadRewardedAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showRewardedAd(
        onRewarded: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val activity = currentActivity
        val ad = rewardedAd
        if (activity == null || ad == null) {
            onFailed()
            loadRewardedAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                onFailed()
                loadRewardedAd()
            }
        }

        ad.show(activity) { _ ->
            onRewarded()
        }
    }

    fun preload() {
        loadRewardedAd()
    }
}
