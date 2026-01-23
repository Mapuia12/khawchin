package com.mapuia.khawchinthlirna.data.network

/**
 * API Configuration for Khawchin Weather App
 * 
 * ARCHITECTURE OVERVIEW:
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Weather Data:  EC2 Backend → Firebase → Android App       │
 * │  Crowdsource:   Android App → EC2 API (direct)             │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * Weather data flows through Firebase (no direct EC2 calls needed).
 * Crowdsource features (reports, badges, leaderboard) call EC2 directly.
 */
object ApiConfig {
    
    // ═══════════════════════════════════════════════════════════════
    // CROWDSOURCE API CONFIGURATION
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * EC2 Crowdsource API Base URL
     * 
     * Replace "YOUR-ELASTIC-IP" with your actual AWS Elastic IP address.
     * Example: "http://13.234.56.78:8080"
     * 
     * To get your Elastic IP:
     * 1. Go to AWS EC2 Console → Elastic IPs
     * 2. Allocate new Elastic IP
     * 3. Associate with your instance
     * 4. Replace the placeholder below
     */
    const val CROWDSOURCE_BASE_URL = "http://13.234.127.71:8080"
    
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
        return "$CROWDSOURCE_BASE_URL$endpoint"
    }
    
    /**
     * Check if the API is configured (Elastic IP has been set)
     */
    fun isConfigured(): Boolean {
        return !CROWDSOURCE_BASE_URL.contains("13.234.127.71")
    }
}
