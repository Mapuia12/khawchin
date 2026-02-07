package com.mapuia.khawchinthlirna.util

/**
 * Simple no-fill backoff to reduce repeated AdMob requests.
 * Backoff is kept in-memory and resets on app restart.
 */
object BannerAdBackoff {
    private const val NO_FILL_BACKOFF_MS = 10 * 60 * 1000L
    private var lastNoFillAtMs: Long = 0L

    fun shouldBackoff(nowMs: Long = System.currentTimeMillis()): Boolean {
        return lastNoFillAtMs > 0L && (nowMs - lastNoFillAtMs) < NO_FILL_BACKOFF_MS
    }

    fun markNoFill(nowMs: Long = System.currentTimeMillis()) {
        lastNoFillAtMs = nowMs
    }
}

object NativeAdBackoff {
    private const val NO_FILL_BACKOFF_MS = 10 * 60 * 1000L
    private var lastNoFillAtMs: Long = 0L

    fun shouldBackoff(nowMs: Long = System.currentTimeMillis()): Boolean {
        return lastNoFillAtMs > 0L && (nowMs - lastNoFillAtMs) < NO_FILL_BACKOFF_MS
    }

    fun markNoFill(nowMs: Long = System.currentTimeMillis()) {
        lastNoFillAtMs = nowMs
    }
}
