package com.mapuia.khawchinthlirna.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.mapuia.khawchinthlirna.R
import com.mapuia.khawchinthlirna.util.AppLog
import com.mapuia.khawchinthlirna.util.BannerAdBackoff

/**
 * Shared Banner Ad composable that can be used across all screens.
 * Uses adaptive banner size for best fit on different screen sizes.
 *
 * NOTE: Ad fill depends on AdMob inventory and policy checks.
 * It's normal to see "no fill" even in production.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bannerAdUnitId = remember { context.getString(R.string.admob_banner_unit_id) }
    // Calculate adaptive banner size based on screen width
    val density = context.resources.displayMetrics.density
    val screenWidthDp = (context.resources.displayMetrics.widthPixels / density).toInt()
    val adView = remember(bannerAdUnitId, screenWidthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp))
            adUnitId = bannerAdUnitId
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    LaunchedEffect(adView) {
        if (BannerAdBackoff.shouldBackoff()) {
            AppLog.w("BannerAd", "Backoff active after no-fill. Skipping ad request.")
            return@LaunchedEffect
        }

        AppLog.d("BannerAd", "Loading banner ad with unit ID: $bannerAdUnitId")
        AppLog.d("BannerAd", "Screen width for adaptive size: ${screenWidthDp}dp")
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                AppLog.d("BannerAd", "Banner ad loaded successfully")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                val errorDetail = when (error.code) {
                    0 -> "Internal error"
                    1 -> "Invalid request - check ad unit ID"
                    2 -> "Network error - check internet connection"
                    3 -> "No fill"
                    else -> "Unknown error"
                }
                if (error.code == 3) {
                    BannerAdBackoff.markNoFill()
                }
                AppLog.w("BannerAd", "Failed to load: $errorDetail (code: ${error.code}, msg: ${error.message})")
            }

            override fun onAdOpened() {
                AppLog.d("BannerAd", "Ad opened")
            }

            override fun onAdClicked() {
                AppLog.d("BannerAd", "Ad clicked")
            }

            override fun onAdImpression() {
                AppLog.d("BannerAd", "Ad impression recorded")
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    }

    Box(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(vertical = 8.dp),
            factory = { adView },
        )

    }
}
