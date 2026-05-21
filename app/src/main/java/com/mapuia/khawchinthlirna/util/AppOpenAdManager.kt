package com.mapuia.khawchinthlirna.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.mapuia.khawchinthlirna.R
import java.util.Calendar

object AppOpenAdManager {
    private const val PREF_NAME = "app_open_ad_prefs"
    private const val KEY_DAY = "day"
    private const val KEY_DAILY_COUNT = "daily_count"

    private const val MAX_DAILY_IMPRESSIONS = 1
    private const val MIN_STARTUP_DELAY_MS = 30_000L

    private var appContext: Context? = null
    private var adUnitId: String = ""
    private var appOpenAd: AppOpenAd? = null
    private var isLoading: Boolean = false
    private var appStartMs: Long = 0L
    private var isShowing: Boolean = false

    fun init(context: Context) {
        appContext = context.applicationContext
        adUnitId = context.getString(R.string.admob_app_open_unit_id)
        appStartMs = System.currentTimeMillis()
        loadAd(context)
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

    private fun getDailyCount(context: Context): Int {
        val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val day = dayKeyNow()
        val stored = p.getString(KEY_DAY, "") ?: ""
        if (stored != day) {
            p.edit().putString(KEY_DAY, day).putInt(KEY_DAILY_COUNT, 0).apply()
            return 0
        }
        return p.getInt(KEY_DAILY_COUNT, 0)
    }

    private fun incrementDailyCount(context: Context) {
        val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val day = dayKeyNow()
        val stored = p.getString(KEY_DAY, "") ?: ""
        val count = if (stored == day) p.getInt(KEY_DAILY_COUNT, 0) else 0
        p.edit().putString(KEY_DAY, day).putInt(KEY_DAILY_COUNT, count + 1).apply()
    }

    private fun canShow(context: Context): Boolean {
        if (System.currentTimeMillis() - appStartMs < MIN_STARTUP_DELAY_MS) return false
        if (isShowing) return false
        return getDailyCount(context) < MAX_DAILY_IMPRESSIONS
    }

    private fun loadAd(context: Context) {
        if (isLoading || appOpenAd != null) return
        isLoading = true

        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoading = false
                    AppLog.w("AppOpenAdManager", "App open load failed: ${error.message}")
                }
            },
        )
    }

    fun showIfEligible(activity: Activity): Boolean {
        val context = appContext ?: return false
        if (!canShow(context)) return false

        val ad = appOpenAd
        if (ad == null) {
            loadAd(context)
            return false
        }

        isShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                appOpenAd = null
                loadAd(context)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                isShowing = false
                appOpenAd = null
                loadAd(context)
            }

            override fun onAdImpression() {
                incrementDailyCount(context)
                AdRevenueTracker.onAppOpenImpression(context)
            }
        }

        ad.show(activity)
        return true
    }
}
