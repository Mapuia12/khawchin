package com.mapuia.khawchinthlirna.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mapuia.khawchinthlirna.R

/**
 * Shared Banner Ad composable that can be used across all screens.
 * Uses adaptive banner size for best fit on different screen sizes.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bannerAdUnitId = remember { context.getString(R.string.admob_banner_unit_id) }
    
    // Calculate adaptive banner size based on screen width
    val density = context.resources.displayMetrics.density
    val screenWidthDp = (context.resources.displayMetrics.widthPixels / density).toInt()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(vertical = 8.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                // Use adaptive banner for better fit across devices
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, screenWidthDp))
                adUnitId = bannerAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
