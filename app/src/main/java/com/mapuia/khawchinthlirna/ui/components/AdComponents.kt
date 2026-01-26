package com.mapuia.khawchinthlirna.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.mapuia.khawchinthlirna.R
import com.mapuia.khawchinthlirna.util.AppLog

/**
 * Shared Banner Ad composable that can be used across all screens.
 * Uses adaptive banner size for best fit on different screen sizes.
 *
 * NOTE: Banner and native ads often don't show for unpublished apps.
 * AdMob limits ad fill rates during testing. This is expected behavior.
 * Real ads will serve once the app is published on Play Store.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bannerAdUnitId = remember { context.getString(R.string.admob_banner_unit_id) }
    var adLoaded by remember { mutableStateOf(false) }
    var adError by remember { mutableStateOf<String?>(null) }

    // Calculate adaptive banner size based on screen width
    val density = context.resources.displayMetrics.density
    val screenWidthDp = (context.resources.displayMetrics.widthPixels / density).toInt()

    // Log the ad unit ID being used
    LaunchedEffect(Unit) {
        AppLog.d("BannerAd", "Loading banner ad with unit ID: $bannerAdUnitId")
        AppLog.d("BannerAd", "Screen width for adaptive size: ${screenWidthDp}dp")
    }

    Box(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(vertical = 8.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    // Use adaptive banner for better fit across devices
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, screenWidthDp))
                    adUnitId = bannerAdUnitId

                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            adLoaded = true
                            adError = null
                            AppLog.d("BannerAd", "✅ Banner ad loaded successfully!")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            adLoaded = false
                            adError = error.message
                            // Common error codes:
                            // 0 = ERROR_CODE_INTERNAL_ERROR
                            // 1 = ERROR_CODE_INVALID_REQUEST
                            // 2 = ERROR_CODE_NETWORK_ERROR
                            // 3 = ERROR_CODE_NO_FILL (most common for unpublished apps)
                            val errorDetail = when (error.code) {
                                0 -> "Internal error"
                                1 -> "Invalid request - check ad unit ID"
                                2 -> "Network error - check internet connection"
                                3 -> "No fill - normal for unpublished apps, ads will serve after app is live"
                                else -> "Unknown error"
                            }
                            AppLog.w("BannerAd", "❌ Failed to load: $errorDetail (code: ${error.code}, msg: ${error.message})")
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

                    loadAd(AdRequest.Builder().build())
                }
            },
        )

        // Show placeholder when ad fails to load
        if (adError != null && !adLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📢",
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 16.sp
                )
            }
        }
    }
}
