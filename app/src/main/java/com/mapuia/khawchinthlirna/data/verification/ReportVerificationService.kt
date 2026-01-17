package com.mapuia.khawchinthlirna.data.verification

import com.google.firebase.firestore.FirebaseFirestore
import com.mapuia.khawchinthlirna.data.WeatherConstants
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

/**
 * Handles verification of user-submitted weather reports.
 * Compares reports with official weather API data and nearby reports.
 */
class ReportVerificationService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val USERS_COLLECTION = "users"
        // Use centralized constant for reports collection
        private const val REPORTS_COLLECTION = "crowdsource_reports"
        private const val WEATHER_GRID_COLLECTION = "weather_v69_grid"
        
        // Tolerance thresholds
        private const val TEMP_TOLERANCE_CELSIUS = 5.0
        private const val HUMIDITY_TOLERANCE_PERCENT = 20
        private const val VERIFICATION_TIME_WINDOW_HOURS = 2
        private const val MIN_NEARBY_REPORTS_FOR_CONSENSUS = 3
        
        // Reputation change values
        private const val ACCURATE_REPORT_BONUS = 0.02
        private const val INACCURATE_REPORT_PENALTY = 0.05
        private const val VERIFIED_REPORT_BONUS = 0.03
        
        // Points
        private const val REPORT_SUBMISSION_POINTS = 5
        private const val ACCURATE_REPORT_POINTS = 10
        private const val VERIFIED_REPORT_POINTS = 15
    }

    /**
     * Verify a report against official weather data
     */
    suspend fun verifyAgainstOfficialData(
        reportId: String,
        gridId: String,
        reportedTemp: Int?,
        reportedHumidity: Int?,
        reportedCondition: String
    ): VerificationResult {
        return try {
            // Get official weather data for the grid
            val officialDoc = firestore.collection(WEATHER_GRID_COLLECTION)
                .document(gridId)
                .get()
                .await()

            if (!officialDoc.exists()) {
                return VerificationResult(
                    isVerified = false,
                    confidence = 0.0,
                    reason = "No official data available for comparison"
                )
            }

            val officialTemp = officialDoc.getDouble("current.temp_c")?.toInt()
            val officialHumidity = officialDoc.getLong("current.humidity")?.toInt()
            val officialConditionCode = officialDoc.getLong("current.condition.code")?.toInt()

            var matchScore = 0
            var totalChecks = 0

            // Check temperature
            if (reportedTemp != null && officialTemp != null) {
                totalChecks++
                if (abs(reportedTemp - officialTemp) <= TEMP_TOLERANCE_CELSIUS) {
                    matchScore++
                }
            }

            // Check humidity
            if (reportedHumidity != null && officialHumidity != null) {
                totalChecks++
                if (abs(reportedHumidity - officialHumidity) <= HUMIDITY_TOLERANCE_PERCENT) {
                    matchScore++
                }
            }

            // Check condition
            totalChecks++
            if (isConditionMatch(reportedCondition, officialConditionCode)) {
                matchScore++
            }

            val confidence = if (totalChecks > 0) matchScore.toDouble() / totalChecks else 0.0
            val isVerified = confidence >= 0.5

            VerificationResult(
                isVerified = isVerified,
                confidence = confidence,
                reason = if (isVerified) "Report matches official data" else "Report differs from official data"
            )
        } catch (e: Exception) {
            VerificationResult(
                isVerified = false,
                confidence = 0.0,
                reason = "Verification failed: ${e.message}"
            )
        }
    }

    /**
     * Verify a report against nearby user reports (consensus-based)
     */
    suspend fun verifyAgainstNearbyReports(
        reportId: String,
        lat: Double,
        lng: Double,
        reportedCondition: String,
        reportedTemp: Int?,
        timestamp: Long
    ): VerificationResult {
        return try {
            val timeWindowStart = timestamp - (VERIFICATION_TIME_WINDOW_HOURS * 60 * 60 * 1000)
            val timeWindowEnd = timestamp + (VERIFICATION_TIME_WINDOW_HOURS * 60 * 60 * 1000)

            // Query nearby reports (simplified - in production use GeoFirestore or similar)
            val nearbyReports = firestore.collection(REPORTS_COLLECTION)
                .whereGreaterThan("timestamp", timeWindowStart)
                .whereLessThan("timestamp", timeWindowEnd)
                .get()
                .await()

            if (nearbyReports.size() < MIN_NEARBY_REPORTS_FOR_CONSENSUS) {
                return VerificationResult(
                    isVerified = false,
                    confidence = 0.0,
                    reason = "Not enough nearby reports for consensus"
                )
            }

            var matchingConditions = 0
            var matchingTemps = 0
            var totalNearby = 0

            for (doc in nearbyReports.documents) {
                if (doc.id == reportId) continue // Skip self

                val docLat = doc.getDouble("lat") ?: continue
                val docLng = doc.getDouble("lng") ?: continue
                
                // Simple distance check (approx 20km)
                if (abs(docLat - lat) > 0.2 || abs(docLng - lng) > 0.2) continue

                totalNearby++
                
                val docCondition = doc.getString("condition") ?: ""
                if (isSimilarCondition(reportedCondition, docCondition)) {
                    matchingConditions++
                }

                if (reportedTemp != null) {
                    val docTemp = doc.getLong("temperature")?.toInt()
                    if (docTemp != null && abs(reportedTemp - docTemp) <= TEMP_TOLERANCE_CELSIUS) {
                        matchingTemps++
                    }
                }
            }

            if (totalNearby < MIN_NEARBY_REPORTS_FOR_CONSENSUS) {
                return VerificationResult(
                    isVerified = false,
                    confidence = 0.0,
                    reason = "Not enough nearby reports for consensus"
                )
            }

            val conditionConsensus = matchingConditions.toDouble() / totalNearby
            val tempConsensus = if (reportedTemp != null) matchingTemps.toDouble() / totalNearby else 0.5
            val overallConsensus = (conditionConsensus + tempConsensus) / 2

            VerificationResult(
                isVerified = overallConsensus >= 0.6,
                confidence = overallConsensus,
                reason = if (overallConsensus >= 0.6) 
                    "Report matches community consensus" 
                else 
                    "Report differs from community reports"
            )
        } catch (e: Exception) {
            VerificationResult(
                isVerified = false,
                confidence = 0.0,
                reason = "Verification failed: ${e.message}"
            )
        }
    }

    /**
     * Update user reputation and points based on verification
     */
    suspend fun updateUserStats(
        userId: String,
        isAccurate: Boolean
    ): Result<Unit> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            
            firestore.runTransaction { transaction ->
                val userDoc = transaction.get(userRef)
                
                var reputation = userDoc.getDouble("reputation") ?: 0.5
                var points = userDoc.getLong("points")?.toInt() ?: 0
                var totalReports = userDoc.getLong("total_reports")?.toInt() ?: 0
                var accurateReports = userDoc.getLong("accurate_reports")?.toInt() ?: 0
                var trustLevel = userDoc.getLong("trust_level")?.toInt() ?: 1
                val badges = userDoc.get("badges") as? List<String> ?: emptyList()

                // Update stats
                totalReports++
                points += REPORT_SUBMISSION_POINTS

                if (isAccurate) {
                    accurateReports++
                    reputation = (reputation + ACCURATE_REPORT_BONUS).coerceAtMost(1.0)
                    points += ACCURATE_REPORT_POINTS
                } else {
                    reputation = (reputation - INACCURATE_REPORT_PENALTY).coerceAtLeast(0.0)
                }

                // Update trust level
                trustLevel = calculateTrustLevel(totalReports, reputation, accurateReports)

                // Check for new badges
                val newBadges = checkForNewBadges(
                    currentBadges = badges,
                    totalReports = totalReports,
                    accurateReports = accurateReports,
                    reputation = reputation,
                    trustLevel = trustLevel
                )

                transaction.update(userRef, mapOf(
                    "reputation" to reputation,
                    "points" to points,
                    "total_reports" to totalReports,
                    "accurate_reports" to accurateReports,
                    "trust_level" to trustLevel,
                    "badges" to (badges + newBadges).distinct(),
                    "last_active" to System.currentTimeMillis()
                ))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateTrustLevel(totalReports: Int, reputation: Double, accurateReports: Int): Int {
        val accuracyRate = if (totalReports > 0) accurateReports.toDouble() / totalReports else 0.0
        
        return when {
            totalReports >= 100 && accuracyRate >= 0.95 && reputation >= 0.9 -> 5
            totalReports >= 50 && accuracyRate >= 0.85 && reputation >= 0.75 -> 4
            totalReports >= 20 && accuracyRate >= 0.75 && reputation >= 0.6 -> 3
            totalReports >= 10 && accuracyRate >= 0.6 && reputation >= 0.5 -> 2
            else -> 1
        }
    }

    private fun checkForNewBadges(
        currentBadges: List<String>,
        totalReports: Int,
        accurateReports: Int,
        reputation: Double,
        trustLevel: Int
    ): List<String> {
        val newBadges = mutableListOf<String>()
        
        if (totalReports == 1 && !currentBadges.contains("first_report")) {
            newBadges.add("first_report")
        }
        
        if (totalReports >= 100 && !currentBadges.contains("veteran")) {
            newBadges.add("veteran")
        }
        
        val accuracyRate = if (totalReports > 0) accurateReports.toDouble() / totalReports else 0.0
        if (accuracyRate >= 0.9 && totalReports >= 10 && !currentBadges.contains("accuracy_star")) {
            newBadges.add("accuracy_star")
        }
        
        if (trustLevel >= 3 && !currentBadges.contains("trusted")) {
            newBadges.add("trusted")
        }
        
        return newBadges
    }

    private fun isConditionMatch(reportedCondition: String, officialCode: Int?): Boolean {
        if (officialCode == null) return false
        
        val normalizedReported = reportedCondition.lowercase()
        
        return when {
            // Clear/Sunny
            officialCode == 1000 && (normalizedReported.contains("clear") || normalizedReported.contains("sunny")) -> true
            // Cloudy
            officialCode in 1003..1009 && normalizedReported.contains("cloud") -> true
            // Rain
            officialCode in 1063..1201 && (normalizedReported.contains("rain") || normalizedReported.contains("drizzle")) -> true
            // Storm
            officialCode in 1273..1282 && (normalizedReported.contains("storm") || normalizedReported.contains("thunder")) -> true
            // Fog/Mist
            officialCode in 1030..1147 && (normalizedReported.contains("fog") || normalizedReported.contains("mist")) -> true
            else -> false
        }
    }

    private fun isSimilarCondition(condition1: String, condition2: String): Boolean {
        val normalized1 = condition1.lowercase()
        val normalized2 = condition2.lowercase()
        
        // Same condition
        if (normalized1 == normalized2) return true
        
        // Similar conditions
        val rainConditions = listOf("rain", "drizzle", "shower", "ruah")
        val cloudConditions = listOf("cloud", "overcast", "hnim")
        val clearConditions = listOf("clear", "sunny", "chhum")
        val stormConditions = listOf("storm", "thunder", "lightning", "thlipui")
        
        val groups = listOf(rainConditions, cloudConditions, clearConditions, stormConditions)
        
        for (group in groups) {
            val in1 = group.any { normalized1.contains(it) }
            val in2 = group.any { normalized2.contains(it) }
            if (in1 && in2) return true
        }
        
        return false
    }
}

/**
 * Result of verification
 */
data class VerificationResult(
    val isVerified: Boolean,
    val confidence: Double, // 0.0 to 1.0
    val reason: String
)
