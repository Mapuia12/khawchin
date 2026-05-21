package com.mapuia.khawchinthlirna.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.mapuia.khawchinthlirna.R
import java.util.Calendar

/**
 * Rewarded ads are explicitly user-consented and heavily capped to protect retention.
 */
object RewardedAdManager {
    private const val PREF_NAME = "rewarded_ad_prefs"
    private const val KEY_DAY = "day_key"
    private const val KEY_DAILY_COUNT = "daily_count"
    private const val KEY_LAST_SHOWN_MS = "last_shown_ms"

    private const val MAX_DAILY_IMPRESSIONS = 1
    private const val MIN_INTERVAL_MS = 8 * 60 * 60 * 1000L // 8 hours

    private var appContext: Context? = null
    private var adUnitId: String = ""
    private var rewardedAd: RewardedAd? = null
    private var isLoading: Boolean = false

    fun init(context: Context) {
        appContext = context.applicationContext
        adUnitId = context.getString(R.string.admob_rewarded_unit_id)
        preload(context)
    }

    private fun dayKeyNow(): String {
        val c = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun normalizeDay(context: Context) {
        val p = prefs(context)
        val today = dayKeyNow()
        val savedDay = p.getString(KEY_DAY, "") ?: ""
        if (savedDay == today) return

        p.edit()
            .putString(KEY_DAY, today)
            .putInt(KEY_DAILY_COUNT, 0)
            .apply()
    }

    private fun dailyCount(context: Context): Int {
        normalizeDay(context)
        return prefs(context).getInt(KEY_DAILY_COUNT, 0)
    }

    private fun incrementDailyCount(context: Context) {
        normalizeDay(context)
        val p = prefs(context)
        p.edit().putInt(KEY_DAILY_COUNT, p.getInt(KEY_DAILY_COUNT, 0) + 1).apply()
    }

    private fun getLastShownMs(context: Context): Long = prefs(context).getLong(KEY_LAST_SHOWN_MS, 0L)

    private fun setLastShownMs(context: Context, ms: Long) {
        prefs(context).edit().putLong(KEY_LAST_SHOWN_MS, ms).apply()
    }

    fun isEligible(context: Context): Boolean {
        val now = System.currentTimeMillis()
        if (dailyCount(context) >= MAX_DAILY_IMPRESSIONS) return false
        if (now - getLastShownMs(context) < MIN_INTERVAL_MS) return false
        return true
    }

    fun isReady(): Boolean = rewardedAd != null

    fun preload(context: Context) {
        if (rewardedAd != null || isLoading) return
        isLoading = true

        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    AppLog.w("RewardedAdManager", "Rewarded load failed: ${error.message}")
                }
            },
        )
    }

    fun show(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit,
        onClosed: () -> Unit,
    ): Boolean {
        val context = appContext ?: return false
        if (!isEligible(context)) return false

        val ad = rewardedAd
        if (ad == null) {
            preload(context)
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload(context)
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                preload(context)
                onClosed()
            }

            override fun onAdImpression() {
                AdRevenueTracker.onRewardedImpression(context)
            }
        }

        ad.show(activity) { rewardItem ->
            setLastShownMs(context, System.currentTimeMillis())
            incrementDailyCount(context)
            AdRevenueTracker.onRewardEarned(context)
            onRewardEarned(rewardItem)
        }

        return true
    }
}
