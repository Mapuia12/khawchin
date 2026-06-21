package com.mapuia.khawchinthlirna.data.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.mapuia.khawchinthlirna.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale

/**
 * Manages Firebase Authentication with Google Sign-In and Anonymous auth.
 * Supports upgrading anonymous accounts to Google accounts.
 */
class AuthManager(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient
    private val webClientId: String

    init {
        webClientId = runCatching { context.getString(R.string.default_web_client_id) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: WEB_CLIENT_ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    companion object {
        // Replace with your actual Web Client ID from Firebase Console
        private const val WEB_CLIENT_ID = "88630222212-ndtudd79n92emt0ptged8cnffdcp60gb.apps.googleusercontent.com"
        private const val USERS_COLLECTION = "users"
        private val BUNDLED_ANDROID_SHA1 = setOf(
            "6B:11:4B:17:1A:40:E0:F0:C7:99:70:92:74:6B:AB:E4:CF:7A:EB:42",
            "D7:48:A1:C5:79:C1:59:32:79:73:F3:CD:85:49:57:A2:26:7C:12:DC",
            "AB:E3:0E:BA:05:1D:2D:B9:A8:25:D1:62:EF:39:14:93:7A:FC:5B:3E",
        )
    }

    /**
     * Current authenticated user
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * User ID (anonymous or authenticated)
     */
    val userId: String
        get() = auth.currentUser?.uid ?: ""

    /**
     * Check if user is signed in (including anonymous)
     */
    val isSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Check if user needs to sign in (null or anonymous)
     * Used to show sign-in button - true if user is null OR anonymous
     */
    val isAnonymous: Boolean
        get() = auth.currentUser == null || auth.currentUser?.isAnonymous == true

    /**
     * Flow of auth state changes
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Sign in anonymously - for users who don't want to create account
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "Starting anonymous Firebase auth...")
            val result = auth.signInAnonymously().await()
            result.user?.let {
                createUserProfile(it)
                Result.success(it)
            } ?: Result.failure(Exception("Anonymous sign-in failed"))
        } catch (e: Exception) {
            if (isFirebaseClientBlocked(e)) {
                logFirebaseClientDiagnostics("Anonymous auth blocked", e)
            } else {
                android.util.Log.e("AuthManager", "Anonymous auth exception: ${e.message}", e)
            }
            Result.failure(Exception(friendlyFirebaseAuthError(e, "Guest sign-in"), e))
        }
    }

    /**
     * Get Google Sign-In intent
     */
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handle Google Sign-In result
     */
    suspend fun handleGoogleSignInResult(data: Intent?, resultCode: Int? = null): Result<FirebaseUser> {
        return try {
            android.util.Log.d(
                "AuthManager",
                "handleGoogleSignInResult called resultCode=$resultCode data=${data != null}"
            )
            if (data == null) {
                val message = if (resultCode != null && resultCode != Activity.RESULT_OK) {
                    "Google sign-in cancelled"
                } else {
                    "Google sign-in returned no data"
                }
                return Result.failure(Exception(message))
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            android.util.Log.d("AuthManager", "Google account obtained: ${account.email}")
            android.util.Log.d("AuthManager", "ID Token available: ${account.idToken != null}")
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            // Status codes: https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes
            val errorMessage = when (e.statusCode) {
                12500 -> "Google sign-in setup error. Check Firebase SHA keys and Web Client ID."
                12501 -> "Sign-in cancelled"
                12502 -> "Sign-in currently in progress"
                10 -> "Developer error: SHA-1 fingerprint not registered in Firebase Console. " +
                      "Run: keytool -list -v -keystore \"%USERPROFILE%\\.android\\debug.keystore\" -alias androiddebugkey -storepass android " +
                      "Then add the SHA-1 to Firebase Console > Project Settings > Your Android app"
                7 -> "Network error - check internet connection"
                8 -> "Internal error"
                else -> "Google sign-in failed (code: ${e.statusCode})"
            }
            android.util.Log.e("AuthManager", "Google Sign-In ApiException: ${e.statusCode} - $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Google Sign-In Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Authenticate with Google credentials
     */
    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        val idToken = account.idToken
            ?: return Result.failure(Exception("Google sign-in did not return an ID token"))
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        return try {
            android.util.Log.d("AuthManager", "Starting Firebase auth with Google...")
            val currentUser = auth.currentUser
            android.util.Log.d(
                "AuthManager",
                "Current user: ${currentUser?.uid}, isAnonymous: ${currentUser?.isAnonymous}"
            )

            val result = if (currentUser != null && currentUser.isAnonymous) {
                try {
                    currentUser.linkWithCredential(credential).await()
                } catch (collision: FirebaseAuthUserCollisionException) {
                    android.util.Log.d(
                        "AuthManager",
                        "Anonymous upgrade collided with existing Google account, signing in directly..."
                    )
                    auth.signInWithCredential(credential).await()
                }
            } else {
                auth.signInWithCredential(credential).await()
            }

            completeGoogleAuth(result.user, account)
        } catch (e: FirebaseAuthUserCollisionException) {
            android.util.Log.d("AuthManager", "Google account already exists, retrying direct sign-in...")
            try {
                val result = auth.signInWithCredential(credential).await()
                completeGoogleAuth(result.user, account)
            } catch (retryError: Exception) {
                android.util.Log.e("AuthManager", "Direct Google sign-in retry failed: ${retryError.message}", retryError)
                Result.failure(Exception(friendlyFirebaseAuthError(retryError, "Google sign-in"), retryError))
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            android.util.Log.e("AuthManager", "Invalid Google credential: ${e.message}", e)
            Result.failure(Exception("Google sign-in credential became invalid. Please try again."))
        } catch (e: Exception) {
            if (isFirebaseClientBlocked(e)) {
                logFirebaseClientDiagnostics("Google Firebase auth blocked", e)
            } else {
                android.util.Log.e("AuthManager", "Firebase auth exception: ${e.message}", e)
            }
            Result.failure(Exception(friendlyFirebaseAuthError(e, "Google sign-in"), e))
        }
    }

    private fun friendlyFirebaseAuthError(e: Exception, action: String): String {
        val message = e.message.orEmpty()
        return when {
            message.contains("Requests from this Android client application", ignoreCase = true) ||
                message.contains("blocked", ignoreCase = true) ->
                "$action is blocked for this Play Store build. Add the installed app-signing SHA-1/SHA-256 to Firebase and Google Cloud API key Android restrictions, then rebuild with a fresh google-services.json."

            message.contains("network", ignoreCase = true) ||
                message.contains("unreachable", ignoreCase = true) ->
                "Network error while signing in. Please check internet connection."

            message.contains("Too many requests", ignoreCase = true) ->
                "Too many sign-in attempts. Please wait a little and try again."

            message.contains("API key", ignoreCase = true) ||
                message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "$action Firebase setup is incomplete. Check google-services.json, package name, SHA keys, and enabled sign-in providers."

            else -> message.ifBlank { "$action failed. Please try again." }
        }
    }

    private fun isFirebaseClientBlocked(e: Exception): Boolean {
        val message = e.message.orEmpty()
        return message.contains("Requests from this Android client application", ignoreCase = true) ||
            message.contains("blocked", ignoreCase = true)
    }

    private fun logFirebaseClientDiagnostics(reason: String, e: Exception? = null) {
        val fingerprints = installedSigningFingerprints()
        val installedSha1 = fingerprints.joinToString { it.sha1 }.ifBlank { "unknown" }
        val installedSha256 = fingerprints.joinToString { it.sha256 }.ifBlank { "unknown" }
        val sha1InBundledConfig = fingerprints.any { it.sha1 in BUNDLED_ANDROID_SHA1 }

        Log.e(
            "AuthManager",
            "$reason. package=${context.packageName}; " +
                "installedSha1=$installedSha1; installedSha256=$installedSha256; " +
                "sha1InBundledGoogleServices=$sha1InBundledConfig; " +
                "bundledGoogleServicesSha1=${BUNDLED_ANDROID_SHA1.joinToString()}; " +
                "webClientId=$webClientId. " +
                "If sha1InBundledGoogleServices=false, download a fresh google-services.json after adding the Play App Signing cert. " +
                "If true, also add the same package/SHA-1 pair to the Google Cloud API key Android restrictions and verify Firebase App Check Play Integrity.",
            e,
        )
    }

    private data class AppSigningFingerprint(
        val sha1: String,
        val sha256: String,
    )

    private fun installedSigningFingerprints(): List<AppSigningFingerprint> {
        return try {
            val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
                val signingInfo = packageInfo.signingInfo ?: return emptyList()
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES,
                ).signatures
            } ?: emptyArray()

            signatures
                .distinctBy { it.toCharsString() }
                .map {
                    AppSigningFingerprint(
                        sha1 = signingDigest(it, "SHA-1"),
                        sha256 = signingDigest(it, "SHA-256"),
                    )
                }
        } catch (error: Exception) {
            Log.w("AuthManager", "Could not read installed signing certificate: ${error.message}", error)
            emptyList()
        }
    }

    private fun signingDigest(signature: Signature, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm).digest(signature.toByteArray())
        return digest.joinToString(":") { byte ->
            String.format(Locale.US, "%02X", byte.toInt() and 0xff)
        }
    }

    private suspend fun completeGoogleAuth(
        user: FirebaseUser?,
        account: GoogleSignInAccount,
    ): Result<FirebaseUser> {
        return user?.let {
            android.util.Log.d("AuthManager", "Google auth successful. Creating/updating profile...")
            createOrUpdateUserProfile(it, account)
            Result.success(it)
        } ?: run {
            android.util.Log.e("AuthManager", "Firebase auth returned null user")
            Result.failure(Exception("Google authentication failed"))
        }
    }

    /**
     * Create user profile in Firestore.
     * Handles Firestore permission errors gracefully.
     */
    private suspend fun createUserProfile(user: FirebaseUser) {
        try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(user.uid)
            val existingDoc = userDoc.get().await()

            if (!existingDoc.exists()) {
                val profile = hashMapOf(
                    "uid" to user.uid,
                    "display_name" to (user.displayName ?: "Mizo User"),
                    "email" to user.email,
                    "photo_url" to user.photoUrl?.toString(),
                    "is_anonymous" to user.isAnonymous,
                    "reputation" to 0.5, // Start at 50%
                    "total_reports" to 0,
                    "accurate_reports" to 0,
                    "trust_level" to 1,
                    "points" to 0,
                    "badges" to emptyList<String>(),
                    "created_at" to System.currentTimeMillis(),
                    "last_active" to System.currentTimeMillis(),
                )
                userDoc.set(profile).await()
            }
            android.util.Log.d("AuthManager", "User profile created successfully")
        } catch (e: Exception) {
            // Log the error but don't fail - user is still authenticated
            android.util.Log.w("AuthManager", "Failed to create user profile in Firestore: ${e.message}", e)
        }
    }

    /**
     * Create or update user profile after Google sign-in.
     * Handles Firestore permission errors gracefully - user is still signed in even if profile creation fails.
     */
    private suspend fun createOrUpdateUserProfile(user: FirebaseUser, account: GoogleSignInAccount) {
        try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(user.uid)
            val existingDoc = userDoc.get().await()

            if (existingDoc.exists()) {
                // Update existing profile
                userDoc.update(
                    mapOf(
                        "display_name" to (account.displayName ?: user.displayName),
                        "email" to account.email,
                        "photo_url" to account.photoUrl?.toString(),
                        "is_anonymous" to false,
                        "last_active" to System.currentTimeMillis(),
                    )
                ).await()
            } else {
                // Create new profile
                val profile = hashMapOf(
                    "uid" to user.uid,
                    "display_name" to (account.displayName ?: "Mizo User"),
                    "email" to account.email,
                    "photo_url" to account.photoUrl?.toString(),
                    "is_anonymous" to false,
                    "reputation" to 0.5,
                    "total_reports" to 0,
                    "accurate_reports" to 0,
                    "trust_level" to 1,
                    "points" to 0,
                    "badges" to emptyList<String>(),
                    "created_at" to System.currentTimeMillis(),
                    "last_active" to System.currentTimeMillis(),
                )
                userDoc.set(profile).await()
            }
            android.util.Log.d("AuthManager", "User profile created/updated successfully")
        } catch (e: Exception) {
            // Log the error but don't fail the sign-in - user is still authenticated
            android.util.Log.w("AuthManager", "Failed to create/update user profile in Firestore: ${e.message}", e)
            // Profile creation failed but user is signed in - they can still use the app
        }
    }

    /**
     * Get current user profile from Firestore.
     * Falls back to Firebase Auth user data if Firestore fails.
     */
    suspend fun getUserProfile(): UserProfile? {
        val user = auth.currentUser ?: return null
        return try {
            val doc = try {
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .get(Source.SERVER)
                    .await()
            } catch (e: Exception) {
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .get(Source.CACHE)
                    .await()
            }
            if (doc.exists()) {
                val badges = (doc.get("badges") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val favoriteLocations = (doc.get("favorite_locations") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val totalReports = doc.getLong("total_reports")?.toInt()
                    ?: doc.getLong("reports")?.toInt()
                    ?: 0
                val points = doc.getLong("points")?.toInt()
                    ?: doc.getLong("total_points")?.toInt()
                    ?: 0
                UserProfile(
                    uid = doc.getString("uid") ?: user.uid,
                    displayName = doc.getString("display_name") ?: (user.displayName ?: "Mizo User"),
                    email = doc.getString("email") ?: user.email,
                    photoUrl = doc.getString("photo_url") ?: user.photoUrl?.toString(),
                    isAnonymous = doc.getBoolean("is_anonymous") ?: user.isAnonymous,
                    reputation = doc.getDouble("reputation") ?: 0.5,
                    totalReports = totalReports,
                    accurateReports = doc.getLong("accurate_reports")?.toInt() ?: 0,
                    trustLevel = doc.getLong("trust_level")?.toInt() ?: 1,
                    points = points,
                    badges = badges,
                    createdAt = doc.getLong("created_at") ?: 0L,
                    lastActive = doc.getLong("last_active") ?: 0L,
                    preferredLanguage = doc.getString("preferred_language") ?: "mz",
                    notificationEnabled = doc.getBoolean("notification_enabled") ?: true,
                    severeWeatherAlerts = doc.getBoolean("severe_weather_alerts") ?: true,
                    homeLocation = doc.getString("home_location"),
                    favoriteLocations = favoriteLocations
                )
            } else {
                // Create profile if doesn't exist
                createUserProfile(user)
                UserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: "Mizo User",
                    email = user.email,
                    photoUrl = user.photoUrl?.toString(),
                    isAnonymous = user.isAnonymous
                )
            }
        } catch (e: Exception) {
            // Firestore failed (permission denied, network error, etc.)
            // Return profile from Firebase Auth data instead
            android.util.Log.w("AuthManager", "Failed to get profile from Firestore, using Auth data: ${e.message}")
            UserProfile(
                uid = user.uid,
                displayName = user.displayName ?: "Mizo User",
                email = user.email,
                photoUrl = user.photoUrl?.toString(),
                isAnonymous = user.isAnonymous
            )
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut() {
        googleSignInClient.signOut().await()
        auth.signOut()
    }

    /**
     * Delete account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
            
            // Delete user data from Firestore
            firestore.collection(USERS_COLLECTION).document(user.uid).delete().await()
            
            // Delete Firebase Auth account
            user.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
