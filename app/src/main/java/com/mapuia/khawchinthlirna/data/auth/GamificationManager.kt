package com.mapuia.khawchinthlirna.data.auth

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.ZoneId

/**
 * Gamification Manager - handles points and badge awarding
 */
class GamificationManager(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val USERS_COLLECTION = "users"
        
        // Points for different actions
        const val POINTS_FIRST_REPORT = 50
        const val POINTS_REPORT = 10
        const val POINTS_VERIFIED_REPORT = 25
        const val POINTS_EARLY_BIRD = 15
        const val POINTS_NIGHT_OWL = 15
        const val POINTS_STORM = 20
    }

    /**
     * Award points and check for badges after submitting a report
     */
    suspend fun onReportSubmitted(
        userId: String,
        rainIntensity: Int,
        lat: Double,
        lon: Double
    ): AwardResult {
        if (userId.isBlank()) return AwardResult()
        
        val userDoc = firestore.collection(USERS_COLLECTION).document(userId)
        val snapshot = userDoc.get().await()
        
        if (!snapshot.exists()) return AwardResult()

        val currentPoints = snapshot.getLong("points")?.toInt() ?: 0
        val totalReports = snapshot.getLong("total_reports")?.toInt() ?: 0
        val currentBadges = (snapshot.get("badges") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        var pointsEarned = 0
        val newBadges = mutableListOf<String>()

        // Base points for report
        pointsEarned += POINTS_REPORT

        // First report badge
        if (totalReports == 0 && !currentBadges.contains(Badges.FIRST_REPORT)) {
            newBadges.add(Badges.FIRST_REPORT)
            pointsEarned += POINTS_FIRST_REPORT
        }

        // Time-based badges
        val hour = LocalTime.now(ZoneId.of("Asia/Kolkata")).hour
        
        // Early Bird - before 6 AM
        if (hour < 6 && !currentBadges.contains(Badges.EARLY_BIRD)) {
            newBadges.add(Badges.EARLY_BIRD)
            pointsEarned += POINTS_EARLY_BIRD
        }
        
        // Night Owl - after 10 PM
        if (hour >= 22 && !currentBadges.contains(Badges.NIGHT_OWL)) {
            newBadges.add(Badges.NIGHT_OWL)
            pointsEarned += POINTS_NIGHT_OWL
        }

        // Storm Chaser - heavy rain (intensity 4-6)
        if (rainIntensity >= 4) {
            pointsEarned += POINTS_STORM
            // TODO: Track storm count for Storm Chaser badge
        }

        // Veteran badge - 100+ reports
        if (totalReports + 1 >= 100 && !currentBadges.contains(Badges.VETERAN)) {
            newBadges.add(Badges.VETERAN)
            pointsEarned += 100 // Bonus for veteran
        }

        // Update Firestore
        val updates = mutableMapOf<String, Any>(
            "points" to currentPoints + pointsEarned,
            "total_reports" to FieldValue.increment(1),
            "last_active" to System.currentTimeMillis(),
        )

        if (newBadges.isNotEmpty()) {
            updates["badges"] = FieldValue.arrayUnion(*newBadges.toTypedArray())
        }

        userDoc.update(updates).await()

        return AwardResult(
            pointsEarned = pointsEarned,
            newBadges = newBadges,
            totalPoints = currentPoints + pointsEarned
        )
    }

    /**
     * Award points when report is verified as accurate
     */
    suspend fun onReportVerified(userId: String) {
        if (userId.isBlank()) return
        
        val userDoc = firestore.collection(USERS_COLLECTION).document(userId)
        val snapshot = userDoc.get().await()
        
        if (!snapshot.exists()) return

        val currentBadges = (snapshot.get("badges") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val accurateReports = snapshot.getLong("accurate_reports")?.toInt() ?: 0
        val totalReports = snapshot.getLong("total_reports")?.toInt() ?: 0
        
        val updates = mutableMapOf<String, Any>(
            "points" to FieldValue.increment(POINTS_VERIFIED_REPORT.toLong()),
            "accurate_reports" to FieldValue.increment(1),
        )

        // Check accuracy star badge (90%+ accuracy with 10+ reports)
        val newAccurateReports = accurateReports + 1
        if (totalReports >= 10) {
            val accuracy = newAccurateReports.toDouble() / totalReports
            if (accuracy >= 0.9 && !currentBadges.contains(Badges.ACCURACY_STAR)) {
                updates["badges"] = FieldValue.arrayUnion(Badges.ACCURACY_STAR)
            }
        }

        userDoc.update(updates).await()
    }

    /**
     * Get user's gamification stats
     */
    suspend fun getUserStats(userId: String): UserStats? {
        if (userId.isBlank()) return null
        
        val snapshot = firestore.collection(USERS_COLLECTION).document(userId).get().await()
        if (!snapshot.exists()) return null

        return UserStats(
            points = snapshot.getLong("points")?.toInt() ?: 0,
            totalReports = snapshot.getLong("total_reports")?.toInt() ?: 0,
            accurateReports = snapshot.getLong("accurate_reports")?.toInt() ?: 0,
            badges = (snapshot.get("badges") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            trustLevel = snapshot.getLong("trust_level")?.toInt() ?: 1,
        )
    }
}

/**
 * Result of awarding points/badges
 */
data class AwardResult(
    val pointsEarned: Int = 0,
    val newBadges: List<String> = emptyList(),
    val totalPoints: Int = 0
)

/**
 * User's gamification stats
 */
data class UserStats(
    val points: Int,
    val totalReports: Int,
    val accurateReports: Int,
    val badges: List<String>,
    val trustLevel: Int
)
