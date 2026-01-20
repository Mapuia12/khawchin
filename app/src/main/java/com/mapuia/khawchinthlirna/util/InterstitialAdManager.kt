package com.mapuia.khawchinthlirna.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.mapuia.khawchinthlirna.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages interstitial ads with multiple trigger strategies:
 * 
 * 1. Time-based: Show after X minutes of app usage
 * 2. Action-based: Show after X user actions (reports, refreshes)
 * 3. Session-based: Show once per session after first content load
 * 
 * Usage:
 * 1. Initialize with context: InterstitialAdManager.init(context)
 * 2. Show on action: InterstitialAdManager.showIfReady(activity)
 * 3. Show time-based: InterstitialAdManager.showIfTimeElapsed(activity, intervalMs = 180000) // 3 minutes
 * 4. Auto-trigger: InterstitialAdManager.checkAutoTrigger(activity) // Call periodically
 */
object InterstitialAdManager {
    
    private var interstitialAd: InterstitialAd? = null
    private var lastShownTime: Long = 0
    private var isLoading = false
    private var appStartTime: Long = 0
    private var actionCount: Int = 0
    private var hasShownSessionAd = false
    
    // Configuration
    private const val AUTO_TRIGGER_INTERVAL_MS = 180_000L // 3 minutes between auto-triggered ads
    private const val INITIAL_DELAY_MS = 60_000L // Wait 1 minute before first auto ad
    private const val ACTION_THRESHOLD = 5 // Show ad after every 5 actions
    
    private val _isAdReady = MutableStateFlow(false)
    val isAdReady = _isAdReady.asStateFlow()
    
    private var adUnitId: String = ""
    
    /**
     * Initialize the manager and start loading the first ad.
     * Call this once in Application.onCreate() or MainActivity.onCreate()
     */
    fun init(context: Context) {
        adUnitId = context.getString(R.string.admob_interstitial_unit_id)
        appStartTime = System.currentTimeMillis()
        lastShownTime = appStartTime // Don't show immediately on app start
        hasShownSessionAd = false
        actionCount = 0
        loadAd(context)
    }
    
    /**
     * Reset session tracking (call when app comes to foreground after background)
     */
    fun onSessionStart() {
        hasShownSessionAd = false
        actionCount = 0
    }
    
    /**
     * Track user action (report submission, refresh, etc.)
     * Returns true if ad was triggered
     */
    fun trackAction(activity: Activity): Boolean {
        actionCount++
        
        // Show ad after threshold actions
        if (actionCount >= ACTION_THRESHOLD) {
            actionCount = 0
            return showIfTimeElapsed(activity, 60_000) // At least 1 minute between action-triggered ads
        }
        return false
    }
    
    /**
     * Check if auto-trigger conditions are met.
     * Call this periodically (e.g., on screen transitions, pull-to-refresh)
     */
    fun checkAutoTrigger(activity: Activity): Boolean {
        val now = System.currentTimeMillis()
        val appUsageTime = now - appStartTime
        val timeSinceLastAd = now - lastShownTime
        
        // Don't auto-trigger if:
        // 1. App just started (wait INITIAL_DELAY_MS)
        // 2. Recently showed an ad (wait AUTO_TRIGGER_INTERVAL_MS)
        if (appUsageTime < INITIAL_DELAY_MS) {
            AppLog.d("InterstitialAdManager", "Skipping auto-trigger: app just started (${appUsageTime}ms)")
            return false
        }
        
        if (timeSinceLastAd < AUTO_TRIGGER_INTERVAL_MS) {
            AppLog.d("InterstitialAdManager", "Skipping auto-trigger: recently shown (${timeSinceLastAd}ms ago)")
            return false
        }
        
        // Auto-trigger!
        return showIfReady(activity)
    }
    
    /**
     * Load an interstitial ad (preload for instant showing)
     */
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
                    
                    // Set up full screen callback
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            _isAdReady.value = false
                            lastShownTime = System.currentTimeMillis()
                            hasShownSessionAd = true
                            // Preload next ad
                            loadAd(context)
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            _isAdReady.value = false
                            // Try loading again
                            loadAd(context)
                        }
                    }
                    
                    AppLog.d("InterstitialAdManager", "Ad loaded successfully")
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    _isAdReady.value = false
                    AppLog.w("InterstitialAdManager", "Failed to load ad: ${error.message}")
                    
                    // Retry after delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadAd(context)
                    }, 30_000) // Retry after 30 seconds
                }
            }
        )
    }
    
    /**
     * Show interstitial ad if ready.
     * Returns true if ad was shown.
     */
    fun showIfReady(activity: Activity): Boolean {
        val ad = interstitialAd
        return if (ad != null) {
            ad.show(activity)
            AppLog.d("InterstitialAdManager", "Showing interstitial ad")
            true
        } else {
            // Ad not ready, try loading
            loadAd(activity)
            false
        }
    }
    
    /**
     * Show interstitial ad if enough time has passed since last shown.
     * 
     * @param activity The activity to show the ad in
     * @param intervalMs Minimum time between ads in milliseconds (default: 3 minutes)
     * @return true if ad was shown
     */
    fun showIfTimeElapsed(activity: Activity, intervalMs: Long = 180_000): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = now - lastShownTime
        
        return if (elapsed >= intervalMs) {
            showIfReady(activity)
        } else {
            AppLog.d("InterstitialAdManager", "Not showing ad, only ${elapsed}ms elapsed (need ${intervalMs}ms)")
            false
        }
    }
    
    /**
     * Show interstitial ad after a certain number of actions.
     * Call this with each action (e.g., each report submission).
     * 
     * @param activity The activity to show the ad in
     * @param actionCount Current action count
     * @param threshold Show ad after this many actions (default: 5)
     * @return true if ad was shown
     */
    fun showAfterActions(activity: Activity, actionCount: Int, threshold: Int = 5): Boolean {
        return if (actionCount > 0 && actionCount % threshold == 0) {
            showIfReady(activity)
        } else {
            false
        }
    }
    
    /**
     * Force preload an ad if none is loaded.
     */
    fun preload(context: Context) {
        if (interstitialAd == null && !isLoading) {
            loadAd(context)
        }
    }
}
