package com.mapuia.khawchinthlirna.data

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.mapuia.khawchinthlirna.data.model.CurrentWeather
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import com.mapuia.khawchinthlirna.data.model.SkillReport
import com.mapuia.khawchinthlirna.data.model.ImergDoc
import com.mapuia.khawchinthlirna.data.model.ForecastSnapshot
import com.mapuia.khawchinthlirna.util.AppLog
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuth
import com.mapuia.khawchinthlirna.BuildConfig
import com.mapuia.khawchinthlirna.data.model.AppAnnouncement
import com.mapuia.khawchinthlirna.data.model.AppStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.util.Locale
import kotlin.math.*

/**
 * Repository that encapsulates Firestore access.
 *
 * Contract:
 * - Read: `weather_v69_grid/{gridId}`
 * - Write: `crowd_reports` (must match Firestore security rules)
 * 
 * ROBUST FALLBACK STRATEGY:
 * 1. Try exact grid ID from user location (rounded to 2 decimals)
 * 2. If not found, use Firestore whereIn query with batch of nearby candidates
 * 3. Return the closest valid document by distance
 */
class WeatherRepository(
    private val db: FirebaseFirestore,
    private val cache: WeatherCache? = null,
    private val httpClient: OkHttpClient? = null,
) {

    private data class TimedValue<T>(val value: T, val cachedAtMs: Long)
    private data class CombinedForecastEnvelope(
        val schemaVersion: Int? = null,
        val runId: String? = null,
        val runTimeUtc: String? = null,
        val generatedUtc: String? = null,
        val expiresUtc: String? = null,
        val gridCount: Int? = null,
        val grid: Map<String, WeatherDoc>? = null,
    )
    private data class CurrentForecastEnvelope(
        val schemaVersion: Int? = null,
        val generatedUtc: String? = null,
        val expiresUtc: String? = null,
        val gridCount: Int? = null,
        val grid: Map<String, CurrentWeather>? = null,
    )

    private companion object {
        private const val DEFAULT_EC2_FORECAST_URL = "https://khawchin.me/forecast/khawchin_forecast.json"
        private const val DEFAULT_EC2_CURRENT_URL = "https://khawchin.me/forecast/khawchin_current.json"
        private const val DEFAULT_EC2_FORECAST_BACKUP_URL = "https://khawchin.me/forecast/khawchin_forecast_backup.json"
        private const val DEFAULT_EC2_CURRENT_BACKUP_URL = "https://khawchin.me/forecast/khawchin_current_backup.json"
        private const val DEFAULT_APP_ANNOUNCEMENTS_URL = "https://khawchin.me/app/announcements.json"
        private const val DEFAULT_APP_STATUS_URL = "https://khawchin.me/app/status.json"
        private const val REMOTE_CONFIG_FORECAST_URL_KEY = "forecast_json_url"
        private const val REMOTE_CONFIG_CURRENT_URL_KEY = "current_json_url"
        private const val REMOTE_CONFIG_FORECAST_BACKUP_URL_KEY = "forecast_json_backup_url"
        private const val REMOTE_CONFIG_CURRENT_BACKUP_URL_KEY = "current_json_backup_url"
        private const val REMOTE_CONFIG_APP_ANNOUNCEMENTS_URL_KEY = "app_announcements_url"
        private const val REMOTE_CONFIG_APP_STATUS_URL_KEY = "app_status_url"
        private const val REMOTE_CONFIG_FIRESTORE_PUBLIC_READS_ENABLED_KEY = "firestore_public_reads_enabled"
        private const val FORECAST_HTTP_CACHE_TTL_MS = 30 * 60 * 1000L
        private const val CURRENT_HTTP_CACHE_TTL_MS = 10 * 60 * 1000L
        private const val APP_CONTROL_CACHE_TTL_MS = 10 * 60 * 1000L
        private const val MIN_EC2_GRID_POINTS = 100
        private const val MAX_HTTP_NEAREST_DISTANCE_KM = 60.0
        private const val MAX_EC2_JSON_AGE_HOURS = 20L
        private const val MAX_EC2_CURRENT_AGE_MINUTES = 120L
    }

    private val nowMs: Long
        get() = System.currentTimeMillis()

    private val skillReportTtlMs = 2 * 60 * 60 * 1000L
    private val satelliteTtlMs = 20 * 60 * 1000L

    @Volatile
    private var skillReportCache: TimedValue<SkillReport?>? = null
    @Volatile
    private var combinedForecastCache: TimedValue<Map<String, WeatherDoc>?>? = null
    @Volatile
    private var currentForecastCache: TimedValue<Map<String, CurrentWeather>?>? = null
    @Volatile
    private var appAnnouncementCache: TimedValue<AppAnnouncement?>? = null
    @Volatile
    private var appStatusCache: TimedValue<AppStatus?>? = null
    private val imergCache = mutableMapOf<String, TimedValue<ImergDoc?>>()
    private val forecastCache = mutableMapOf<String, TimedValue<ForecastSnapshot?>>()
    private val httpGson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private fun parseInstantOrNull(value: String?): Instant? {
        return value?.let {
            runCatching { Instant.parse(it) }.getOrElse {
                runCatching { java.time.OffsetDateTime.parse(value).toInstant() }.getOrNull()
            }
        }
    }

    private fun isCombinedForecastFresh(envelope: CombinedForecastEnvelope): Boolean {
        val now = Instant.now()
        parseInstantOrNull(envelope.expiresUtc)?.let { expires ->
            if (now.isAfter(expires)) {
                AppLog.w("WeatherRepo", "EC2 forecast expired at ${envelope.expiresUtc}; using Firestore fallback")
                return false
            }
        }

        parseInstantOrNull(envelope.generatedUtc)?.let { generated ->
            if (now.isAfter(generated.plusSeconds(MAX_EC2_JSON_AGE_HOURS * 3600))) {
                AppLog.w("WeatherRepo", "EC2 forecast too old (${envelope.generatedUtc}); using Firestore fallback")
                return false
            }
        }

        return true
    }

    private fun isCurrentForecastFresh(envelope: CurrentForecastEnvelope): Boolean {
        val now = Instant.now()
        parseInstantOrNull(envelope.expiresUtc)?.let { expires ->
            if (now.isAfter(expires)) {
                AppLog.w("WeatherRepo", "EC2 current forecast expired at ${envelope.expiresUtc}; skipping current merge")
                return false
            }
        }

        parseInstantOrNull(envelope.generatedUtc)?.let { generated ->
            if (now.isAfter(generated.plusSeconds(MAX_EC2_CURRENT_AGE_MINUTES * 60))) {
                AppLog.w("WeatherRepo", "EC2 current forecast too old (${envelope.generatedUtc}); skipping current merge")
                return false
            }
        }

        return true
    }

    private fun remoteUrl(key: String, default: String): String {
        val configured = Firebase.remoteConfig
            .getString(key)
            .trim()

        return configured
            .takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: default
    }

    private fun combinedForecastUrls(): List<String> =
        listOf(
            remoteUrl(REMOTE_CONFIG_FORECAST_URL_KEY, DEFAULT_EC2_FORECAST_URL),
            remoteUrl(REMOTE_CONFIG_FORECAST_BACKUP_URL_KEY, DEFAULT_EC2_FORECAST_BACKUP_URL),
        ).distinct()

    private fun currentForecastUrls(): List<String> =
        listOf(
            remoteUrl(REMOTE_CONFIG_CURRENT_URL_KEY, DEFAULT_EC2_CURRENT_URL),
            remoteUrl(REMOTE_CONFIG_CURRENT_BACKUP_URL_KEY, DEFAULT_EC2_CURRENT_BACKUP_URL),
        ).distinct()

    private fun appAnnouncementsUrl(): String =
        remoteUrl(REMOTE_CONFIG_APP_ANNOUNCEMENTS_URL_KEY, DEFAULT_APP_ANNOUNCEMENTS_URL)

    private fun appStatusUrl(): String =
        remoteUrl(REMOTE_CONFIG_APP_STATUS_URL_KEY, DEFAULT_APP_STATUS_URL)

    private fun firestorePublicReadsEnabled(): Boolean =
        Firebase.remoteConfig.getBoolean(REMOTE_CONFIG_FIRESTORE_PUBLIC_READS_ENABLED_KEY)

    private fun cacheBustedUrl(url: String, forceRefresh: Boolean): String {
        if (!forceRefresh) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}_=${nowMs}"
    }

    private fun jsonRequest(url: String, forceRefresh: Boolean): Request {
        val builder = Request.Builder()
            .url(cacheBustedUrl(url, forceRefresh))
            .header("Accept", "application/json")

        if (forceRefresh) {
            builder
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
        }

        return builder.build()
    }

    private fun isWithinControlWindow(startAt: String?, endAt: String?): Boolean {
        val now = Instant.now()
        parseInstantOrNull(startAt)?.let { start ->
            if (now.isBefore(start)) return false
        }
        parseInstantOrNull(endAt)?.let { end ->
            if (now.isAfter(end)) return false
        }
        return true
    }

    private fun isVersionAllowed(minVersionCode: Int?, maxVersionCode: Int?): Boolean {
        val current = BuildConfig.VERSION_CODE
        if (minVersionCode != null && current < minVersionCode) return false
        if (maxVersionCode != null && current > maxVersionCode) return false
        return true
    }

    /** Get latest skill report (global, not grid-specific). */
    suspend fun getLatestSkillReport(): SkillReport? {
        skillReportCache?.let { cached ->
            if (nowMs - cached.cachedAtMs <= skillReportTtlMs) {
                return cached.value
            }
        }

        if (!firestorePublicReadsEnabled()) {
            AppLog.d("WeatherRepo", "Skipping Firestore skill report read; JSON migration mode")
            skillReportCache = TimedValue(null, nowMs)
            return null
        }

        fun mapSkillReport(doc: com.google.firebase.firestore.DocumentSnapshot): SkillReport? {
            return try {
                val perModelMae = (doc.get("per_model_mae") as? Map<*, *>)
                    ?.mapNotNull { (k, v) ->
                        val key = k as? String
                        val value = (v as? Number)?.toDouble()
                        if (key != null && value != null) key to value else null
                    }
                    ?.toMap()
                val perModelCount = (doc.get("per_model_count") as? Map<*, *>)
                    ?.mapNotNull { (k, v) ->
                        val key = k as? String
                        val value = (v as? Number)?.toLong()
                        if (key != null && value != null) key to value else null
                    }
                    ?.toMap()

                SkillReport(
                    periodStart = doc.getString("period_start"),
                    periodEnd = doc.getString("period_end"),
                    sampleCount = doc.getLong("sample_count")?.toInt() ?: 0,
                    overallMae = (doc.get("overall_mae") as? Number)?.toDouble(),
                    overallBrier = (doc.get("overall_brier") as? Number)?.toDouble(),
                    overallBias = (doc.get("overall_bias") as? Number)?.toDouble(),
                    hitRate = (doc.get("hit_rate") as? Number)?.toDouble(),
                    falseAlarmRate = (doc.get("false_alarm_rate") as? Number)?.toDouble(),
                    perModelMae = perModelMae,
                    perModelCount = perModelCount,
                    ts = doc.getString("ts"),
                )
            } catch (_: Exception) {
                null
            }
        }
        suspend fun fetchFallback(): SkillReport? {
            return try {
                val fallback = db.collection(WeatherConstants.SKILL_REPORT_COLLECTION)
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()

                fallback.documents.firstOrNull()?.let { mapSkillReport(it) }
            } catch (e2: Exception) {
                AppLog.e("WeatherRepo", "Skill report fetch (docId) failed: ${e2.message}")
                try {
                    val anyDocs = db.collection(WeatherConstants.SKILL_REPORT_COLLECTION)
                        .limit(10)
                        .get()
                        .await()

                    anyDocs.documents
                        .mapNotNull { doc ->
                            val ts = doc.getString("ts")
                            val score = ts ?: doc.id
                            mapSkillReport(doc)?.let { it to score }
                        }
                        .maxByOrNull { it.second }
                        ?.first
                } catch (e3: Exception) {
                    AppLog.e("WeatherRepo", "Skill report fetch (fallback) failed: ${e3.message}")
                    null
                }
            }
        }

        val fetched = try {
            AppLog.d("WeatherRepo", "Fetching skill report from collection: ${WeatherConstants.SKILL_REPORT_COLLECTION}")
            val snapshot = db.collection(WeatherConstants.SKILL_REPORT_COLLECTION)
                .orderBy("ts", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            AppLog.d(
                "WeatherRepo",
                "Skill report query (ts) docs=${snapshot.size()} ids=${snapshot.documents.joinToString { it.id }}"
            )

            snapshot.documents.firstOrNull()?.let { mapSkillReport(it) }
                ?: fetchFallback()
        } catch (e: Exception) {
            AppLog.e("WeatherRepo", "Skill report fetch (ts) failed: ${e.message}")
            fetchFallback()
        }

        skillReportCache = TimedValue(fetched, nowMs)
        return fetched
    }

    /** Get latest IMERG satellite precipitation for a grid ID (if available). */
    suspend fun getImergByGridId(gridId: String, forceServer: Boolean = false): ImergDoc? {
        imergCache[gridId]?.let { cached ->
            if (!forceServer && nowMs - cached.cachedAtMs <= satelliteTtlMs) {
                return cached.value
            }
        }

        if (!firestorePublicReadsEnabled()) {
            AppLog.d("WeatherRepo", "Skipping Firestore IMERG read for $gridId; JSON migration mode")
            imergCache[gridId] = TimedValue(null, nowMs)
            return null
        }

        return try {
            val doc = db.collection(WeatherConstants.IMERG_COLLECTION)
                .document(gridId)
                .get(if (forceServer) Source.SERVER else Source.DEFAULT)
                .await()
            val fetched = doc.toObject(ImergDoc::class.java)?.also {
                if (it.gridId.isNullOrBlank()) it.gridId = doc.id
            }
            imergCache[gridId] = TimedValue(fetched, nowMs)
            fetched
        } catch (e: Exception) {
            AppLog.e("WeatherRepo", "IMERG fetch failed for $gridId: ${e.message}")
            null
        }
    }

    /** Get latest forecast snapshot for a grid ID (if available). */
    suspend fun getForecastSnapshotByGridId(gridId: String, forceServer: Boolean = false): ForecastSnapshot? {
        forecastCache[gridId]?.let { cached ->
            if (!forceServer && nowMs - cached.cachedAtMs <= satelliteTtlMs) {
                return cached.value
            }
        }

        if (!firestorePublicReadsEnabled()) {
            AppLog.d("WeatherRepo", "Skipping Firestore forecast snapshot read for $gridId; JSON migration mode")
            forecastCache[gridId] = TimedValue(null, nowMs)
            return null
        }

        return try {
            val doc = db.collection(WeatherConstants.FORECAST_SNAPSHOT_COLLECTION)
                .document(gridId)
                .get(if (forceServer) Source.SERVER else Source.DEFAULT)
                .await()
            val fetched = doc.toObject(ForecastSnapshot::class.java)?.also {
                if (it.gridId.isNullOrBlank()) it.gridId = doc.id
                if (it.runTime == null) it.runTime = doc.get("run_time")
            }
            forecastCache[gridId] = TimedValue(fetched, nowMs)
            fetched
        } catch (e: Exception) {
            AppLog.e("WeatherRepo", "Forecast snapshot fetch failed for $gridId: ${e.message}")
            null
        }
    }

    private suspend fun fetchCombinedForecastFromEc2(forceRefresh: Boolean = false): Map<String, WeatherDoc>? {
        combinedForecastCache?.let { cached ->
            if (!forceRefresh && nowMs - cached.cachedAtMs <= FORECAST_HTTP_CACHE_TTL_MS) {
                return cached.value
            }
        }

        val client = httpClient ?: return null
        return withContext(Dispatchers.IO) {
            try {
                for (url in combinedForecastUrls()) {
                    val request = jsonRequest(url, forceRefresh)

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            AppLog.w("WeatherRepo", "EC2 forecast HTTP ${response.code} for ${request.url}")
                            return@use
                        }

                        val body = response.body?.string()
                        if (body.isNullOrBlank()) {
                            AppLog.w("WeatherRepo", "EC2 forecast response empty for $url")
                            return@use
                        }

                        val envelope = httpGson.fromJson(body, CombinedForecastEnvelope::class.java)
                        if (!isCombinedForecastFresh(envelope)) {
                            return@use
                        }

                        val rawGrid = envelope.grid.orEmpty()
                        val validGrid = rawGrid.mapNotNull { (gid, doc) ->
                            val normalized = doc.apply {
                                if (gridId.isNullOrBlank()) gridId = gid
                            }
                            if (normalized.isValid()) gid to normalized else null
                        }.toMap()

                        if (validGrid.size < MIN_EC2_GRID_POINTS) {
                            AppLog.w(
                                "WeatherRepo",
                                "EC2 forecast too small (${validGrid.size}/${envelope.gridCount ?: 0}) for $url"
                            )
                            return@use
                        }

                        combinedForecastCache = TimedValue(validGrid, nowMs)
                        AppLog.d(
                            "WeatherRepo",
                            "EC2 forecast loaded: ${validGrid.size} grid points, run=${envelope.runId ?: "unknown"}, url=${request.url}"
                        )
                        return@withContext validGrid
                    }
                }
                AppLog.w("WeatherRepo", "EC2 forecast primary+backup unavailable, using Firestore fallback")
                null
            } catch (e: Exception) {
                AppLog.e("WeatherRepo", "EC2 forecast fetch failed: ${e.message}")
                null
            }
        }
    }

    private suspend fun fetchCurrentForecastFromEc2(forceRefresh: Boolean = false): Map<String, CurrentWeather>? {
        currentForecastCache?.let { cached ->
            if (!forceRefresh && nowMs - cached.cachedAtMs <= CURRENT_HTTP_CACHE_TTL_MS) {
                return cached.value
            }
        }

        val client = httpClient ?: return null
        return withContext(Dispatchers.IO) {
            try {
                for (url in currentForecastUrls()) {
                    val request = jsonRequest(url, forceRefresh)

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            AppLog.w("WeatherRepo", "EC2 current HTTP ${response.code} for ${request.url}")
                            return@use
                        }

                        val body = response.body?.string()
                        if (body.isNullOrBlank()) {
                            AppLog.w("WeatherRepo", "EC2 current response empty for $url")
                            return@use
                        }

                        val envelope = httpGson.fromJson(body, CurrentForecastEnvelope::class.java)
                        if (!isCurrentForecastFresh(envelope)) {
                            return@use
                        }

                        val validGrid = envelope.grid.orEmpty()
                            .filterValues { it.temp > -100.0 && it.temp < 100.0 }

                        if (validGrid.size < MIN_EC2_GRID_POINTS) {
                            AppLog.w(
                                "WeatherRepo",
                                "EC2 current too small (${validGrid.size}/${envelope.gridCount ?: 0}) for $url"
                            )
                            return@use
                        }

                        currentForecastCache = TimedValue(validGrid, nowMs)
                        AppLog.d("WeatherRepo", "EC2 current loaded: ${validGrid.size} grid points, url=$url")
                        return@withContext validGrid
                    }
                }
                AppLog.w("WeatherRepo", "EC2 current primary+backup unavailable; keeping forecast current block")
                null
            } catch (e: Exception) {
                AppLog.e("WeatherRepo", "EC2 current fetch failed: ${e.message}")
                null
            }
        }
    }

    suspend fun getAppAnnouncement(forceRefresh: Boolean = false): AppAnnouncement? {
        appAnnouncementCache?.let { cached ->
            if (!forceRefresh && nowMs - cached.cachedAtMs <= APP_CONTROL_CACHE_TTL_MS) {
                return cached.value
            }
        }

        val client = httpClient ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val request = jsonRequest(appAnnouncementsUrl(), forceRefresh)

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        AppLog.w("WeatherRepo", "Announcement HTTP ${response.code}")
                        appAnnouncementCache = TimedValue(null, nowMs)
                        return@withContext null
                    }
                    val body = response.body?.string()
                    if (body.isNullOrBlank()) {
                        appAnnouncementCache = TimedValue(null, nowMs)
                        return@withContext null
                    }
                    val announcement = httpGson.fromJson(body, AppAnnouncement::class.java)
                    val visible = announcement
                        ?.takeIf { it.enabled }
                        ?.takeIf { it.id.isNotBlank() }
                        ?.takeIf { it.title(true).isNotBlank() || it.body(true).isNotBlank() }
                        ?.takeIf { isWithinControlWindow(it.startAt, it.endAt) }
                        ?.takeIf { isVersionAllowed(it.minVersionCode, it.maxVersionCode) }

                    appAnnouncementCache = TimedValue(visible, nowMs)
                    visible
                }
            } catch (e: Exception) {
                AppLog.e("WeatherRepo", "Announcement fetch failed: ${e.message}")
                null
            }
        }
    }

    suspend fun getAppStatus(forceRefresh: Boolean = false): AppStatus? {
        appStatusCache?.let { cached ->
            if (!forceRefresh && nowMs - cached.cachedAtMs <= APP_CONTROL_CACHE_TTL_MS) {
                return cached.value
            }
        }

        val client = httpClient ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val request = jsonRequest(appStatusUrl(), forceRefresh)

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        AppLog.w("WeatherRepo", "App status HTTP ${response.code}")
                        appStatusCache = TimedValue(null, nowMs)
                        return@withContext null
                    }
                    val body = response.body?.string()
                    val status = body
                        ?.takeIf { it.isNotBlank() }
                        ?.let { httpGson.fromJson(it, AppStatus::class.java) }
                    appStatusCache = TimedValue(status, nowMs)
                    status
                }
            } catch (e: Exception) {
                AppLog.e("WeatherRepo", "App status fetch failed: ${e.message}")
                null
            }
        }
    }

    private fun findNearestInCombinedForecast(
        userLat: Double,
        userLon: Double,
        gridMap: Map<String, WeatherDoc>,
    ): WeatherDoc? {
        val nearest = gridMap.values
            .asSequence()
            .filter { it.isValid() }
            .map { doc -> doc to haversineKm(userLat, userLon, doc.lat, doc.lon) }
            .minByOrNull { it.second }

        if (nearest == null || nearest.second > MAX_HTTP_NEAREST_DISTANCE_KM) {
            return null
        }

        AppLog.d(
            "WeatherRepo",
            "EC2 forecast nearest=${nearest.first.gridId} distance=${String.format(Locale.US, "%.1f", nearest.second)}km"
        )
        return nearest.first
    }

    private fun findNearestCurrentInMap(
        userLat: Double,
        userLon: Double,
        currentMap: Map<String, CurrentWeather>,
    ): CurrentWeather? {
        val nearest = currentMap
            .asSequence()
            .mapNotNull { (gid, current) ->
                val (lat, lon) = parseGridId(gid) ?: return@mapNotNull null
                current to haversineKm(userLat, userLon, lat, lon)
            }
            .minByOrNull { it.second }

        return nearest
            ?.takeIf { it.second <= MAX_HTTP_NEAREST_DISTANCE_KM }
            ?.first
    }

    private suspend fun mergeFreshCurrent(
        gridId: String,
        userLat: Double,
        userLon: Double,
        doc: WeatherDoc,
        forceServer: Boolean,
    ): WeatherDoc {
        val currentMap = fetchCurrentForecastFromEc2(forceRefresh = forceServer) ?: return doc
        val current = currentMap[gridId] ?: findNearestCurrentInMap(userLat, userLon, currentMap) ?: return doc
        return doc.copy(current = current)
    }

    /**
     * Get weather by grid ID. If document doesn't exist, automatically
     * searches for nearest available grid point in Firestore using batch queries.
     */
    suspend fun getWeatherByGridId(gridId: String, forceServer: Boolean = false): WeatherDoc? {
        AppLog.d("WeatherRepo", "getWeatherByGridId called with: $gridId")
        
        // Validate input
        if (!gridId.isValidGridId()) {
            AppLog.e("WeatherRepo", "Invalid grid ID format: $gridId")
            throw IllegalArgumentException("Invalid grid ID format: $gridId")
        }

        val (userLat, userLon) = parseGridId(gridId) ?: return getCachedWeatherFallback(gridId)

        // Preferred path for the new app: one public JSON fetch from EC2, then
        // all nearest-grid matching happens locally. Firestore remains as a safe fallback.
        val combinedForecast = fetchCombinedForecastFromEc2(forceRefresh = forceServer)
        if (combinedForecast != null) {
            combinedForecast[gridId]?.takeIf { it.isValid() }?.let { exact ->
                val merged = mergeFreshCurrent(gridId, userLat, userLon, exact, forceServer)
                runCatching { cache?.save(gridId = gridId, doc = merged) }
                AppLog.d("WeatherRepo", "Found exact EC2 forecast for: $gridId")
                return merged
            }

            findNearestInCombinedForecast(userLat, userLon, combinedForecast)?.let { nearest ->
                val merged = mergeFreshCurrent(gridId, userLat, userLon, nearest, forceServer)
                runCatching { cache?.save(gridId = gridId, doc = merged) }
                return merged
            }
        } else {
            AppLog.w("WeatherRepo", "EC2 forecast unavailable, using Firestore fallback")
        }

        if (!firestorePublicReadsEnabled()) {
            AppLog.w("WeatherRepo", "Firestore weather fallback disabled; using local cache only")
            return getCachedWeatherFallback(gridId)
        }

        // First try the exact grid ID
        AppLog.d("WeatherRepo", "Trying exact grid ID: $gridId")
        val exactDoc = getWeatherWithRetry(gridId, forceServer = forceServer)
        if (exactDoc != null) {
            AppLog.d("WeatherRepo", "Found exact document for: $gridId")
            return mergeFreshCurrent(gridId, userLat, userLon, exactDoc, forceServer)
        }

        AppLog.d("WeatherRepo", "Exact doc not found, trying fallback search")
        // Document not found - try robust fallback search
        return findNearestAvailableWeather(gridId, forceServer = forceServer)
            ?.let { mergeFreshCurrent(gridId, userLat, userLon, it, forceServer) }
    }

    /**
     * ROBUST FALLBACK: Find nearest available weather document.
     * 
     * Strategy:
     * 1. Generate candidate grid IDs in expanding rings (0.01 to 0.30 degree radius)
     * 2. Use Firestore whereIn queries (batch of 10) to efficiently check multiple IDs
     * 3. Return the closest valid document by geographic distance
     * 
     * This handles the case where user is at 23.19_94.01 but Firebase has 23.19_94.05
     */
    private suspend fun findNearestAvailableWeather(originalGridId: String, forceServer: Boolean = false): WeatherDoc? {
        val (userLat, userLon) = parseGridId(originalGridId) ?: return getCachedWeatherFallback(originalGridId)
        
        AppLog.d("WeatherRepo", "Starting robust fallback search from: $originalGridId (user: $userLat, $userLon)")

        // Generate ALL possible nearby grid IDs sorted by distance (uses 0.50° radius = ~55km)
        val candidates = generateNearbyCandidates(userLat, userLon, maxRadiusDegrees = 0.50)
        AppLog.d("WeatherRepo", "Generated ${candidates.size} fallback candidates, first 10: ${candidates.take(10)}")

        // Query in batches of 10 (Firestore whereIn limit)
        val foundDocs = mutableListOf<Pair<WeatherDoc, Double>>() // doc to distance
        var batchesQueried = 0
        val maxBatches = 50 // Query up to 500 candidates before giving up
        
        for (batch in candidates.chunked(10)) {
            if (batch.isEmpty()) continue
            if (batchesQueried >= maxBatches) break
            batchesQueried++
            
            try {
                AppLog.d("WeatherRepo", "Querying batch $batchesQueried: ${batch.joinToString()}")
                
                // Use FieldPath.documentId() since document IDs ARE the grid IDs
                val snapshot = db.collection(WeatherConstants.WEATHER_COLLECTION)
                    .whereIn(FieldPath.documentId(), batch)
                    .get(if (forceServer) Source.SERVER else Source.DEFAULT)
                    .await()
                
                AppLog.d("WeatherRepo", "Batch $batchesQueried returned ${snapshot.documents.size} documents")
                
                for (document in snapshot.documents) {
                    val doc = document.toObject(WeatherDoc::class.java)
                    if (doc != null && doc.isValid()) {
                        val distance = haversineKm(userLat, userLon, doc.lat, doc.lon)
                        foundDocs.add(Pair(doc, distance))
                        AppLog.d("WeatherRepo", "Found valid doc: ${doc.gridId ?: document.id} at ${String.format("%.1f", distance)}km")
                    }
                }
                
                // If we found any valid document, we can stop after checking a few more batches
                // to ensure we have the closest one
                if (foundDocs.isNotEmpty() && batchesQueried >= 5) {
                    AppLog.d("WeatherRepo", "Found ${foundDocs.size} docs after $batchesQueried batches, selecting closest")
                    break
                }
            } catch (e: Exception) {
                AppLog.e("WeatherRepo", "Batch $batchesQueried query failed: ${e.message}")
                // Continue with next batch
            }
        }
        
        AppLog.d("WeatherRepo", "Total found: ${foundDocs.size} documents after $batchesQueried batches")
        
        // Return closest valid document
        val closest = foundDocs.minByOrNull { it.second }?.first
        if (closest != null) {
            AppLog.d("WeatherRepo", "Fallback success! Using: ${closest.gridId} (distance: ${foundDocs.minByOrNull { it.second }?.second?.let { String.format("%.1f", it) }}km)")
            runCatching { cache?.save(gridId = originalGridId, doc = closest) }
            return closest
        }
        
        AppLog.w("WeatherRepo", "No fallback documents found after $batchesQueried batches, trying cache")
        return getCachedWeatherFallback(originalGridId)
    }

    /**
     * Generate nearby grid ID candidates sorted by distance.
     * Uses 0.01 degree step to ensure ALL possible 2-decimal grid IDs are covered.
     * Backend uses various grids (0.25 coarse, 0.10 refined, POI-based).
     * 
     * FIXED: Use finer step (0.01) and larger radius (0.50) for comprehensive coverage.
     */
    private fun generateNearbyCandidates(lat: Double, lon: Double, maxRadiusDegrees: Double): List<String> {
        val candidates = mutableSetOf<String>()
        
        // Use 0.01 degree step to cover ALL possible 2-decimal grid points
        // This ensures we don't miss any grid like 23.48_93.24
        // For 0.50° radius: generates ~10,000 candidates but Firestore batch queries handle it
        val step = 0.01
        val searchRadius = maxOf(maxRadiusDegrees, 0.50) // At least 0.50° (~55km) search radius
        
        var dLat = -searchRadius
        while (dLat <= searchRadius) {
            var dLon = -searchRadius
            while (dLon <= searchRadius) {
                // Round to 2 decimals exactly as backend stores them
                val gLat = ((lat + dLat) * 100).roundToInt() / 100.0
                val gLon = ((lon + dLon) * 100).roundToInt() / 100.0
                
                // Only add if within valid coordinate range (Mizoram + Myanmar area)
                if (gLat in 21.0..25.0 && gLon in 91.5..95.0) {
                    candidates.add(String.format(Locale.US, "%.2f_%.2f", gLat, gLon))
                }
                dLon += step
            }
            dLat += step
        }
        
        AppLog.d("WeatherRepo", "Generated ${candidates.size} total candidates for $lat, $lon")
        
        // Sort by distance from user location and take reasonable limit
        return candidates
            .map { id ->
                val parts = id.split("_")
                val gLat = parts[0].toDouble()
                val gLon = parts[1].toDouble()
                Pair(id, haversineKm(lat, lon, gLat, gLon))
            }
            .sortedBy { it.second }
            .take(500) // Limit to closest 500 to avoid excessive queries
            .map { it.first }
    }

    /**
     * Haversine formula for distance between two coordinates in km
     */
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Parse grid ID like "23.50_93.30" into (lat, lon) pair.
     */
    private fun parseGridId(gridId: String): Pair<Double, Double>? {
        return try {
            val parts = gridId.split("_")
            if (parts.size == 2) {
                Pair(parts[0].toDouble(), parts[1].toDouble())
            } else null
        } catch (e: Exception) {
            null
        }
    }


    private suspend fun getWeatherWithRetry(
        gridId: String,
        maxRetries: Int = WeatherConstants.MAX_RETRY_ATTEMPTS,
        forceServer: Boolean = false,
    ): WeatherDoc? {
        repeat(maxRetries) { attempt ->
            try {
                val doc = db.collection(WeatherConstants.WEATHER_COLLECTION)
                    .document(gridId)
                    .get(if (forceServer) Source.SERVER else Source.DEFAULT)
                    .await()
                    .toObject(WeatherDoc::class.java)

                if (doc != null && doc.isValid()) {
                    // Cache successful, valid docs
                    runCatching { cache?.save(gridId = gridId, doc = doc) }
                    return doc
                } else {
                    // Document doesn't exist or invalid - return null to trigger fallback
                    return null
                }
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    // Last attempt failed, return null
                    return null
                }
                // Exponential backoff
                delay(1000L * (attempt + 1))
            }
        }
        return null
    }

    private suspend fun getCachedWeatherFallback(gridId: String): WeatherDoc? {
        val cached = cache?.cachedWeather?.firstOrNull()
        if (cached == null) return null
        
        // Only use cache if it matches the requested gridId OR is very close
        // Don't use cache from unrelated locations
        if (!cached.isExpired()) {
            val cachedGridId = cached.gridId ?: return null
            val (reqLat, reqLon) = parseGridId(gridId) ?: return null
            val (cachedLat, cachedLon) = parseGridId(cachedGridId) ?: return null
            
            val distanceDegrees = kotlin.math.sqrt(
                (reqLat - cachedLat) * (reqLat - cachedLat) + 
                (reqLon - cachedLon) * (reqLon - cachedLon)
            )
            
            // Only use cache if within 0.1 degrees (~11km) of requested location
            if (distanceDegrees <= 0.1) {
                AppLog.d("WeatherRepo", "Using cached weather from $cachedGridId for $gridId (dist: ${"%.3f".format(distanceDegrees)}°, age: ${cached.getAgeMinutes()} min)")
                return cached.doc
            } else {
                AppLog.d("WeatherRepo", "Cache too far: $cachedGridId vs $gridId (dist: ${"%.3f".format(distanceDegrees)}°)")
                return null
            }
        }
        
        AppLog.d("WeatherRepo", "Cache expired (age: ${cached.getAgeMinutes()} min > ${WeatherConstants.CACHE_EXPIRY_MINUTES} min)")
        return null
    }

    /**
     * Writes a report document that matches the backend function contract.
     *
     * Backend reads:
     * - lat/lon
     * - accuracy_m (default 150)
     * - severity (default 3)
     * - timestamp_auto (iso string)
     */
    suspend fun submitCrowdReport(
        optionMizo: String,
        gridId: String?,
        userLat: Double?,
        userLon: Double?,
        accuracyMeters: Double = 150.0,
        severity: Int = 3,
        rainIntensity: Int? = null,
        windStrength: Int? = null,
        skyCondition: Int? = null,
        reportSource: String? = null,
    ) {
        // Keep payload minimal and aligned with backend function.
        // Validate inputs
        if (userLat == null || userLon == null) {
            throw IllegalArgumentException("Location coordinates required")
        }
        if (userLat !in -90.0..90.0 || userLon !in -180.0..180.0) {
            throw IllegalArgumentException("Invalid coordinates")
        }

        // Firestore rules require request.auth != null. If the user did not explicitly
        // sign in, prepare a silent anonymous account so guest reports still work.
        val currentUser = ensureReportUser()

        val severityClamped = severity.coerceIn(1, 5)

        val rainIntensityValue = (rainIntensity ?: severityClamped).coerceIn(0, 6)
        val data = hashMapOf<String, Any>(
            "lat" to userLat,
            "lon" to userLon,
            "accuracy_m" to accuracyMeters.coerceIn(1.0, 10000.0),
            // Firestore rule expects severity to be integer
            "severity" to severityClamped,
            // Must use 'timestamp_auto' to match Firestore security rules
            "timestamp_auto" to Instant.now().toString(),
            "report_type" to optionMizo.sanitizeInput(),
            // Required by Firestore rules
            "user_id" to currentUser.uid,
            // rain_intensity is required by rules (use severity as approximation)
            "rain_intensity" to rainIntensityValue,
        )

        if (!gridId.isNullOrBlank()) {
            data["grid_id"] = gridId
        }
        windStrength?.let { data["wind_strength"] = it.coerceIn(0, 4) }
        skyCondition?.let { data["sky_condition"] = it.coerceIn(0, 4) }
        reportSource?.takeIf { it.isNotBlank() }?.let { data["report_source"] = it }

        db.collection(WeatherConstants.REPORTS_COLLECTION).add(data).await()
    }

    private suspend fun ensureReportUser() =
        FirebaseAuth.getInstance().let { auth ->
            auth.currentUser ?: try {
                auth.signInAnonymously().await().user
            } catch (e: Exception) {
                AppLog.e("WeatherRepo", "Guest auth failed before report submit: ${e.message}", e)
                throw IllegalStateException(reportAuthFailureMessage(e), e)
            }
        } ?: throw IllegalStateException("Report submit nan guest sign-in a hlawhchham. Firebase auth setup check rawh.")

    private fun reportAuthFailureMessage(e: Exception): String {
        val message = e.message.orEmpty()
        return when {
            message.contains("Requests from this Android client application", ignoreCase = true) ||
                message.contains("blocked", ignoreCase = true) ->
                "Report submit nan guest sign-in hi Firebase-in a block. Play signing SHA/API key/App Check setup check rawh."
            message.contains("network", ignoreCase = true) ->
                "Network a buai avangin report submit theih loh. Internet check la, beih leh rawh."
            else ->
                "Report submit nan guest sign-in a hlawhchham. Firebase auth setup check rawh."
        }
    }
}
