package com.mapuia.khawchinthlirna.util

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.mapuia.khawchinthlirna.BuildConfig

/**
 * Force-update controller:
 * 1) Read minimum supported version from Remote Config.
 * 2) If current version is below minimum, enforce update.
 * 3) Prefer Play immediate in-app update; fallback to Play Store dialog.
 */
class ForceUpdateManager(
    private val activity: ComponentActivity,
) {
    private val remoteConfig = Firebase.remoteConfig
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    private val updateFlowLauncher: ActivityResultLauncher<IntentSenderRequestHolder> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                showFallbackDialog()
            }
        }

    fun checkOnStart() {
        configureRemoteConfig()
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener {
                val minSupportedVersion = remoteConfig.getLong(KEY_MIN_SUPPORTED_VERSION).toInt()
                val shouldForceUpdate = BuildConfig.VERSION_CODE < minSupportedVersion
                logDebugStatus(minSupportedVersion, shouldForceUpdate)
                if (!shouldForceUpdate) return@addOnCompleteListener
                tryImmediateUpdate()
            }
            .addOnFailureListener {
                // Fail closed only when app is already below last cached minimum.
                val minSupportedVersion = remoteConfig.getLong(KEY_MIN_SUPPORTED_VERSION).toInt()
                AppLog.w(TAG, "Remote Config fetch failed. Using cached min version=$minSupportedVersion")
                if (BuildConfig.VERSION_CODE < minSupportedVersion) {
                    showFallbackDialog()
                }
            }
    }

    private fun configureRemoteConfig() {
        val settings = remoteConfigSettings {
            // Keep production reasonably fresh so forecast JSON URLs can be
            // moved to a new EC2 IP without waiting half a day.
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 60 * 60
        }
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_MIN_SUPPORTED_VERSION to BuildConfig.VERSION_CODE.toLong(),
                KEY_FORECAST_JSON_URL to DEFAULT_FORECAST_JSON_URL,
                KEY_CURRENT_JSON_URL to DEFAULT_CURRENT_JSON_URL,
                KEY_FORECAST_JSON_BACKUP_URL to DEFAULT_FORECAST_JSON_BACKUP_URL,
                KEY_CURRENT_JSON_BACKUP_URL to DEFAULT_CURRENT_JSON_BACKUP_URL,
                KEY_APP_ANNOUNCEMENTS_URL to DEFAULT_APP_ANNOUNCEMENTS_URL,
                KEY_APP_STATUS_URL to DEFAULT_APP_STATUS_URL,
                KEY_FIRESTORE_PUBLIC_READS_ENABLED to false,
            ),
        )
    }

    private fun tryImmediateUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val canUseImmediate =
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

                AppLog.d(TAG, "Immediate update available=$canUseImmediate, availability=${info.updateAvailability()}")

                if (!canUseImmediate) {
                    showFallbackDialog()
                    return@addOnSuccessListener
                }

                runCatching {
                    val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateFlowLauncher,
                        options,
                    )
                }.onFailure {
                    showFallbackDialog()
                }
            }
            .addOnFailureListener {
                showFallbackDialog()
            }
    }

    private fun showFallbackDialog() {
        if (activity.isFinishing || activity.isDestroyed) return

        AlertDialog.Builder(activity)
            .setTitle("Update Required")
            .setMessage("A newer version of Khawchin Thlirna is required to continue.")
            .setCancelable(false)
            .setPositiveButton("Update") { _, _ ->
                openPlayStore()
                activity.finishAffinity()
            }
            .show()
    }

    private fun openPlayStore() {
        val packageName = activity.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            activity.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            )
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(webIntent)
        }
    }

    private fun logDebugStatus(minSupportedVersion: Int, shouldForceUpdate: Boolean) {
        AppLog.d(
            TAG,
            "Version check current=${BuildConfig.VERSION_CODE}, min=$minSupportedVersion, force=$shouldForceUpdate",
        )
        if (!BuildConfig.DEBUG) return
        Toast.makeText(
            activity,
            "Update policy: current=${BuildConfig.VERSION_CODE}, min=$minSupportedVersion, force=$shouldForceUpdate",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private companion object {
        private const val TAG = "ForceUpdate"
        private const val KEY_MIN_SUPPORTED_VERSION = "min_supported_version_code"
        private const val KEY_FORECAST_JSON_URL = "forecast_json_url"
        private const val KEY_CURRENT_JSON_URL = "current_json_url"
        private const val KEY_FORECAST_JSON_BACKUP_URL = "forecast_json_backup_url"
        private const val KEY_CURRENT_JSON_BACKUP_URL = "current_json_backup_url"
        private const val KEY_APP_ANNOUNCEMENTS_URL = "app_announcements_url"
        private const val KEY_APP_STATUS_URL = "app_status_url"
        private const val KEY_FIRESTORE_PUBLIC_READS_ENABLED = "firestore_public_reads_enabled"
        private const val DEFAULT_FORECAST_JSON_URL = "https://khawchin.me/forecast/khawchin_forecast.json"
        private const val DEFAULT_CURRENT_JSON_URL = "https://khawchin.me/forecast/khawchin_current.json"
        private const val DEFAULT_FORECAST_JSON_BACKUP_URL = "https://khawchin.me/forecast/khawchin_forecast_backup.json"
        private const val DEFAULT_CURRENT_JSON_BACKUP_URL = "https://khawchin.me/forecast/khawchin_current_backup.json"
        private const val DEFAULT_APP_ANNOUNCEMENTS_URL = "https://khawchin.me/app/announcements.json"
        private const val DEFAULT_APP_STATUS_URL = "https://khawchin.me/app/status.json"
    }
}

/**
 * Play Core launcher API expects IntentSenderRequest. Use a typealias to keep imports local.
 */
private typealias IntentSenderRequestHolder = androidx.activity.result.IntentSenderRequest
