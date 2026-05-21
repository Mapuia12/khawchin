package com.mapuia.khawchinthlirna.util

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.mapuia.khawchinthlirna.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Interstitial strategy optimized for retention-first monetization.
 *
 * Policy:
 * - Action-triggered only (no aggressive auto popups from refresh loops)
 * - 10-minute minimum spacing
 * - 1 interstitial per app session
 * - 2 interstitials per day max
 */
object InterstitialAdManager {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var appStartTime: Long = 0L
    private var lastShownTime: Long = 0L
    private var actionCount: Int = 0
    private var pendingContinuation: (() -> Unit)? = null

    private var appContext: Context? = null
    private var sessionImpressionCount: Int = 0
    private var currentDayKey: String = ""
    private var dailyImpressionCount: Int = 0

    private const val ACTION_THRESHOLD = 2
    private const val INITIAL_DELAY_MS = 90_000L
    private const val MIN_SHOW_INTERVAL_MS = 10 * 60_000L
    private const val MAX_SESSION_IMPRESSIONS = 1
    private const val MAX_DAILY_IMPRESSIONS = 2

    private const val PREF_NAME = "interstitial_ad_prefs"
    private const val KEY_DAY = "day_key"
    private const val KEY_DAILY_COUNT = "daily_count"

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady = _isAdReady.asStateFlow()

    private var adUnitId: String = ""

    fun init(context: Context) {
        appContext = context.applicationContext
        adUnitId = context.getString(R.string.admob_interstitial_unit_id)
        appStartTime = System.currentTimeMillis()
        lastShownTime = appStartTime
        actionCount = 0
        sessionImpressionCount = 0
        restoreDailyCounters()
        loadAd(context)
    }

    fun onSessionStart() {
        actionCount = 0
        sessionImpressionCount = 0
        restoreDailyCounters()
    }

    /**
     * Call this after meaningful completed actions (e.g., successful report submit).
     */
    fun trackAction(activity: Activity): Boolean {
        actionCount += 1
        if (actionCount < ACTION_THRESHOLD) return false

        actionCount = 0
        return showIfReady(activity)
    }

    fun trackLocationSwitch(activity: Activity, onContinue: () -> Unit) {
        preload(activity)
        if (showIfReady(activity, onContinue, ignorePolicy = true)) return
        onContinue()
    }

    private fun dayKeyNow(): String {
        val cal = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun restoreDailyCounters() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val today = dayKeyNow()
        val savedDay = prefs.getString(KEY_DAY, "") ?: ""

        if (savedDay != today) {
            currentDayKey = today
            dailyImpressionCount = 0
            prefs.edit().putString(KEY_DAY, today).putInt(KEY_DAILY_COUNT, 0).apply()
            return
        }

        currentDayKey = today
        dailyImpressionCount = prefs.getInt(KEY_DAILY_COUNT, 0)
    }

    private fun incrementDailyCounter() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val today = dayKeyNow()

        if (currentDayKey != today) {
            currentDayKey = today
            dailyImpressionCount = 0
        }

        dailyImpressionCount += 1
        prefs.edit()
            .putString(KEY_DAY, currentDayKey)
            .putInt(KEY_DAILY_COUNT, dailyImpressionCount)
            .apply()
    }

    private fun canShowNow(now: Long): Boolean {
        restoreDailyCounters()

        if (now - appStartTime < INITIAL_DELAY_MS) {
            AppLog.d("InterstitialAdManager", "Blocked: startup delay")
            return false
        }
        if (now - lastShownTime < MIN_SHOW_INTERVAL_MS) {
            AppLog.d("InterstitialAdManager", "Blocked: min interval")
            return false
        }
        if (sessionImpressionCount >= MAX_SESSION_IMPRESSIONS) {
            AppLog.d("InterstitialAdManager", "Blocked: session cap")
            return false
        }
        if (dailyImpressionCount >= MAX_DAILY_IMPRESSIONS) {
            AppLog.d("InterstitialAdManager", "Blocked: daily cap")
            return false
        }

        return true
    }

    private fun loadAd(context: Context) {
        if (isLoading || interstitialAd != null) return

        isLoading = true
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    _isAdReady.value = true

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdImpression() {
                            appContext?.let { AdRevenueTracker.onInterstitialImpression(it) }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            val continuation = pendingContinuation
                            pendingContinuation = null
                            interstitialAd = null
                            _isAdReady.value = false
                            lastShownTime = System.currentTimeMillis()
                            sessionImpressionCount += 1
                            incrementDailyCounter()
                            loadAd(context)
                            continuation?.invoke()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            val continuation = pendingContinuation
                            pendingContinuation = null
                            interstitialAd = null
                            _isAdReady.value = false
                            loadAd(context)
                            continuation?.invoke()
                        }
                    }

                    AppLog.d("InterstitialAdManager", "Ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    _isAdReady.value = false
                    AppLog.w("InterstitialAdManager", "Load failed: ${error.message}")
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadAd(context)
                    }, 30_000)
                }
            },
        )
    }

    fun showIfReady(activity: Activity): Boolean {
        return showIfReady(activity, onFinished = null)
    }

    fun showIfReady(
        activity: Activity,
        onFinished: (() -> Unit)?,
        ignorePolicy: Boolean = false,
    ): Boolean {
        val now = System.currentTimeMillis()
        if (!ignorePolicy && !canShowNow(now)) return false

        val ad = interstitialAd
        if (ad == null) {
            loadAd(activity)
            return false
        }

        pendingContinuation = onFinished
        ad.show(activity)
        AppLog.d("InterstitialAdManager", "Showing interstitial")
        return true
    }

    fun preload(context: Context) {
        if (interstitialAd == null && !isLoading) {
            loadAd(context)
        }
    }
}
