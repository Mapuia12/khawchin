package com.mapuia.khawchinthlirna.util

import android.content.Context
import java.util.Calendar

/**
 * Lightweight local ad metrics so we can tune placements without extra backend cost.
 */
object AdRevenueTracker {
    private const val PREF_NAME = "ad_revenue_tracker"
    private const val KEY_DAY = "day"

    private const val KEY_APP_OPEN_IMPRESSIONS = "app_open_impressions"
    private const val KEY_INTERSTITIAL_IMPRESSIONS = "interstitial_impressions"
    private const val KEY_REWARDED_IMPRESSIONS = "rewarded_impressions"
    private const val KEY_BANNER_IMPRESSIONS = "banner_impressions"
    private const val KEY_NATIVE_IMPRESSIONS = "native_impressions"
    private const val KEY_REWARDED_EARNED = "rewarded_earned"
    private const val KEY_REPORT_SUBMITS = "report_submits"

    private fun todayKey(): String {
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

    private fun ensureDay(context: Context) {
        val p = prefs(context)
        val today = todayKey()
        val existing = p.getString(KEY_DAY, null)
        if (existing == today) return

        p.edit()
            .putString(KEY_DAY, today)
            .putInt(KEY_APP_OPEN_IMPRESSIONS, 0)
            .putInt(KEY_INTERSTITIAL_IMPRESSIONS, 0)
            .putInt(KEY_REWARDED_IMPRESSIONS, 0)
            .putInt(KEY_BANNER_IMPRESSIONS, 0)
            .putInt(KEY_NATIVE_IMPRESSIONS, 0)
            .putInt(KEY_REWARDED_EARNED, 0)
            .putInt(KEY_REPORT_SUBMITS, 0)
            .apply()
    }

    private fun inc(context: Context, key: String) {
        ensureDay(context)
        val p = prefs(context)
        val next = p.getInt(key, 0) + 1
        p.edit().putInt(key, next).apply()
    }

    fun onAppOpenImpression(context: Context) = inc(context, KEY_APP_OPEN_IMPRESSIONS)
    fun onInterstitialImpression(context: Context) = inc(context, KEY_INTERSTITIAL_IMPRESSIONS)
    fun onRewardedImpression(context: Context) = inc(context, KEY_REWARDED_IMPRESSIONS)
    fun onBannerImpression(context: Context) = inc(context, KEY_BANNER_IMPRESSIONS)
    fun onNativeImpression(context: Context) = inc(context, KEY_NATIVE_IMPRESSIONS)
    fun onRewardEarned(context: Context) = inc(context, KEY_REWARDED_EARNED)
    fun onReportSubmit(context: Context) = inc(context, KEY_REPORT_SUBMITS)
}
