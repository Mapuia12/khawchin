package com.mapuia.khawchinthlirna.data.auth

import com.google.firebase.firestore.PropertyName

/**
 * User profile data model for Firestore
 */
data class UserProfile(
    val uid: String = "",
    @get:PropertyName("display_name")
    @set:PropertyName("display_name")
    var displayName: String = "Mizo User",
    val email: String? = null,
    @get:PropertyName("photo_url")
    @set:PropertyName("photo_url")
    var photoUrl: String? = null,
    @get:PropertyName("is_anonymous")
    @set:PropertyName("is_anonymous")
    var isAnonymous: Boolean = true,
    var reputation: Double = 0.5,
    @get:PropertyName("total_reports")
    @set:PropertyName("total_reports")
    var totalReports: Int = 0,
    @get:PropertyName("accurate_reports")
    @set:PropertyName("accurate_reports")
    var accurateReports: Int = 0,
    @get:PropertyName("trust_level")
    @set:PropertyName("trust_level")
    var trustLevel: Int = 1,
    var points: Int = 0,
    var badges: List<String> = emptyList(),
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = 0L,
    @get:PropertyName("last_active")
    @set:PropertyName("last_active")
    var lastActive: Long = 0L,
    @get:PropertyName("preferred_language")
    @set:PropertyName("preferred_language")
    var preferredLanguage: String = "mz", // "mz" or "en"
    @get:PropertyName("notification_enabled")
    @set:PropertyName("notification_enabled")
    var notificationEnabled: Boolean = true,
    @get:PropertyName("severe_weather_alerts")
    @set:PropertyName("severe_weather_alerts")
    var severeWeatherAlerts: Boolean = true,
    @get:PropertyName("home_location")
    @set:PropertyName("home_location")
    var homeLocation: String? = null, // Grid ID for home location
    @get:PropertyName("favorite_locations")
    @set:PropertyName("favorite_locations")
    var favoriteLocations: List<String> = emptyList() // List of grid IDs
) {
    /**
     * Get reputation as percentage
     */
    val reputationPercent: Int
        get() = (reputation * 100).toInt()

    /**
     * Get trust level name
     */
    val trustLevelName: String
        get() = when (trustLevel) {
            1 -> "Beginner"
            2 -> "Contributor"
            3 -> "Trusted"
            4 -> "Expert"
            5 -> "Verified"
            else -> "Unknown"
        }

    /**
     * Get trust level name in Mizo
     */
    val trustLevelNameMz: String
        get() = when (trustLevel) {
            1 -> "Thar"
            2 -> "Thawktu"
            3 -> "Rinawm"
            4 -> "Thiamna nei"
            5 -> "Pawmzui"
            else -> "Hriat loh"
        }

    /**
     * Check if user has specific badge
     */
    fun hasBadge(badgeId: String): Boolean = badges.contains(badgeId)

    /**
     * Calculate accuracy rate
     */
    val accuracyRate: Double
        get() = if (totalReports > 0) {
            accurateReports.toDouble() / totalReports.toDouble()
        } else 0.0

    /**
     * Get accuracy as percentage
     */
    val accuracyPercent: Int
        get() = (accuracyRate * 100).toInt()
}

/**
 * Badge definitions
 */
object Badges {
    const val FIRST_REPORT = "first_report"
    const val EARLY_BIRD = "early_bird"         // Report before 6 AM
    const val NIGHT_OWL = "night_owl"           // Report after 10 PM
    const val STORM_CHASER = "storm_chaser"     // Report 5 storms
    const val ACCURACY_STAR = "accuracy_star"   // 90%+ accuracy
    const val TRUSTED_REPORTER = "trusted"      // Trust level 3+
    const val WEEKLY_WARRIOR = "weekly_warrior" // Report 7 days in a row
    const val HELPER = "helper"                 // Get 50 upvotes
    const val VETERAN = "veteran"               // 100+ reports
    const val MIZORAM_EXPLORER = "explorer"     // Report from 10+ locations

    /**
     * Get badge display name
     */
    fun getName(badgeId: String): String = when (badgeId) {
        FIRST_REPORT -> "First Report"
        EARLY_BIRD -> "Early Bird"
        NIGHT_OWL -> "Night Owl"
        STORM_CHASER -> "Storm Chaser"
        ACCURACY_STAR -> "Accuracy Star"
        TRUSTED_REPORTER -> "Trusted Reporter"
        WEEKLY_WARRIOR -> "Weekly Warrior"
        HELPER -> "Community Helper"
        VETERAN -> "Weather Veteran"
        MIZORAM_EXPLORER -> "Mizoram Explorer"
        else -> badgeId
    }

    /**
     * Get badge display name in Mizo
     */
    fun getNameMz(badgeId: String): String = when (badgeId) {
        FIRST_REPORT -> "Report Hmasa Ber"
        EARLY_BIRD -> "Zing Thawktu"
        NIGHT_OWL -> "Zan Thawktu"
        STORM_CHASER -> "Thlipui Zawntu"
        ACCURACY_STAR -> "Dikna Star"
        TRUSTED_REPORTER -> "Rinawm Reporter"
        WEEKLY_WARRIOR -> "Kar Tin Thawktu"
        HELPER -> "Puitu"
        VETERAN -> "Kawng Lama"
        MIZORAM_EXPLORER -> "Mizoram Zawntu"
        else -> badgeId
    }

    /**
     * Get badge emoji
     */
    fun getEmoji(badgeId: String): String = when (badgeId) {
        FIRST_REPORT -> "🌟"
        EARLY_BIRD -> "🌅"
        NIGHT_OWL -> "🦉"
        STORM_CHASER -> "⛈️"
        ACCURACY_STAR -> "⭐"
        TRUSTED_REPORTER -> "✅"
        WEEKLY_WARRIOR -> "🔥"
        HELPER -> "🤝"
        VETERAN -> "🏆"
        MIZORAM_EXPLORER -> "🗺️"
        else -> "🎖️"
    }

    /**
     * Get badge description
     */
    fun getDescription(badgeId: String): String = when (badgeId) {
        FIRST_REPORT -> "Submit your first weather report"
        EARLY_BIRD -> "Report weather before 6 AM"
        NIGHT_OWL -> "Report weather after 10 PM"
        STORM_CHASER -> "Report 5 storms or heavy rain events"
        ACCURACY_STAR -> "Maintain 90%+ accuracy rating"
        TRUSTED_REPORTER -> "Reach Trust Level 3"
        WEEKLY_WARRIOR -> "Report 7 consecutive days"
        HELPER -> "Receive 50 upvotes from community"
        VETERAN -> "Submit 100+ weather reports"
        MIZORAM_EXPLORER -> "Report from 10+ different locations"
        else -> ""
    }

    /**
     * Get all badge IDs
     */
    val allBadges = listOf(
        FIRST_REPORT, EARLY_BIRD, NIGHT_OWL, STORM_CHASER, ACCURACY_STAR,
        TRUSTED_REPORTER, WEEKLY_WARRIOR, HELPER, VETERAN, MIZORAM_EXPLORER
    )
}
