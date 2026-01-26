package com.mapuia.khawchinthlirna.data.network

import com.mapuia.khawchinthlirna.BuildConfig

/**
 * API Configuration for Khawchin Weather App
 * 
 * ARCHITECTURE OVERVIEW:
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Weather Data:  EC2 Backend → Firebase → Android App       │
 * │  Crowdsource:   Android App → Firebase (direct)            │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * Weather data flows through Firebase (no direct EC2 calls needed).
 * Crowdsource reports are also stored in Firebase directly from the app.
 * 
 * NOTE: This config is currently UNUSED. All data flows through Firebase.
 * Keeping for potential future EC2 API integration (gamification, etc.)
 * 
 * @deprecated Currently unused - all features use Firebase directly.
 *             Remove or implement when EC2 crowdsource API is needed.
 */
@Deprecated("Currently unused - all features use Firebase directly")
object ApiConfig {
    
    // ═══════════════════════════════════════════════════════════════
    // CROWDSOURCE API CONFIGURATION
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * EC2 Crowdsource API Base URL
     * 
     * SECURITY: URL is configured via BuildConfig to avoid hardcoding in source.
     * Set in local.properties (git-ignored):
     *   CROWDSOURCE_API_URL=https://your-domain.com:8080
     * 
     * For HTTPS (recommended for production):
     * - Use AWS ACM + ALB or Let's Encrypt on EC2
     * - Never use HTTP in production
     */
    val CROWDSOURCE_BASE_URL: String
        get() = BuildConfig.CROWDSOURCE_API_URL.ifEmpty { 
            // Fallback for development only
            if (BuildConfig.DEBUG) "http://10.0.2.2:8080" else ""
        }
    
    /**
     * API Version prefix for all crowdsource endpoints
     */
    const val API_VERSION = "/api/v1"
    
    // ═══════════════════════════════════════════════════════════════
    // CROWDSOURCE ENDPOINTS
    // ═══════════════════════════════════════════════════════════════
    
    object Endpoints {
        // Reports
        const val SUBMIT_REPORT = "$API_VERSION/reports"
        const val NEARBY_REPORTS = "$API_VERSION/reports/nearby"
        const val MY_REPORTS = "$API_VERSION/reports/user"
        
        // Gamification
        const val USER_BADGES = "$API_VERSION/badges"
        const val LEADERBOARD = "$API_VERSION/leaderboard"
        const val USER_STATS = "$API_VERSION/stats"
        
        // Health check
        const val HEALTH = "/health"
    }
    
    // ═══════════════════════════════════════════════════════════════
    // NETWORK CONFIGURATION
    // ═══════════════════════════════════════════════════════════════
    
    object Timeouts {
        const val CONNECT_SECONDS = 30L
        const val READ_SECONDS = 30L
        const val WRITE_SECONDS = 30L
    }
    
    // ═══════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Build full URL for an endpoint
     */
    fun buildUrl(endpoint: String): String {
        require(isConfigured()) { "Crowdsource API URL not configured" }
        return "$CROWDSOURCE_BASE_URL$endpoint"
    }
    
    /**
     * Check if the API is configured with a valid URL
     */
    fun isConfigured(): Boolean {
        return CROWDSOURCE_BASE_URL.isNotEmpty() && 
               CROWDSOURCE_BASE_URL.startsWith("http") &&
               !CROWDSOURCE_BASE_URL.contains("10.0.2.2") // Exclude emulator localhost
    }
}
