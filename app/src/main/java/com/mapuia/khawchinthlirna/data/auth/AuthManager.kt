package com.mapuia.khawchinthlirna.data.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    companion object {
        // Replace with your actual Web Client ID from Firebase Console
        private const val WEB_CLIENT_ID = "88630222212-ndtudd79n92emt0ptged8cnffdcp60gb.apps.googleusercontent.com"
        private const val USERS_COLLECTION = "users"
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
            val result = auth.signInAnonymously().await()
            result.user?.let {
                createUserProfile(it)
                Result.success(it)
            } ?: Result.failure(Exception("Anonymous sign-in failed"))
        } catch (e: Exception) {
            Result.failure(e)
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
    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "handleGoogleSignInResult called, data: ${data != null}")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            android.util.Log.d("AuthManager", "✅ Google account obtained: ${account.email}")
            android.util.Log.d("AuthManager", "ID Token available: ${account.idToken != null}")
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            // Status codes: https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes
            val errorMessage = when (e.statusCode) {
                12500 -> "Sign-in cancelled by user"
                12501 -> "Sign-in cancelled"
                12502 -> "Sign-in currently in progress"
                10 -> "Developer error: SHA-1 fingerprint not registered in Firebase Console. " +
                      "Run: keytool -list -v -keystore \"%USERPROFILE%\\.android\\debug.keystore\" -alias androiddebugkey -storepass android " +
                      "Then add the SHA-1 to Firebase Console → Project Settings → Your Android app"
                7 -> "Network error - check internet connection"
                8 -> "Internal error"
                else -> "Google sign-in failed (code: ${e.statusCode})"
            }
            android.util.Log.e("AuthManager", "❌ Google Sign-In ApiException: ${e.statusCode} - $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Google Sign-In Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Authenticate with Google credentials
     */
    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "🔐 Starting Firebase auth with Google...")
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            android.util.Log.d("AuthManager", "🔐 Credential created, checking current user...")
            
            // Check if we need to link anonymous account
            val currentUser = auth.currentUser
            android.util.Log.d("AuthManager", "🔐 Current user: ${currentUser?.uid}, isAnonymous: ${currentUser?.isAnonymous}")
            
            val result = if (currentUser != null && currentUser.isAnonymous) {
                // Link anonymous account to Google
                android.util.Log.d("AuthManager", "🔐 Linking anonymous account to Google...")
                currentUser.linkWithCredential(credential).await()
            } else {
                // Regular sign-in
                android.util.Log.d("AuthManager", "🔐 Regular signInWithCredential...")
                auth.signInWithCredential(credential).await()
            }
            
            android.util.Log.d("AuthManager", "🔐 Auth result user: ${result.user?.uid}, email: ${result.user?.email}")

            result.user?.let {
                android.util.Log.d("AuthManager", "✅ Firebase auth successful! Creating/updating profile...")
                createOrUpdateUserProfile(it, account)
                Result.success(it)
            } ?: run {
                android.util.Log.e("AuthManager", "❌ Firebase auth returned null user")
                Result.failure(Exception("Google authentication failed"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "❌ Firebase auth exception: ${e.message}", e)
            // If linking fails (account already exists), sign in directly
            if (e.message?.contains("already in use") == true) {
                android.util.Log.d("AuthManager", "🔄 Account already exists, trying direct sign-in...")
                try {
                    val result = auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(account.idToken, null)
                    ).await()
                    result.user?.let {
                        android.util.Log.d("AuthManager", "✅ Direct sign-in successful!")
                        createOrUpdateUserProfile(it, account)
                        Result.success(it)
                    } ?: Result.failure(e)
                } catch (e2: Exception) {
                    android.util.Log.e("AuthManager", "❌ Direct sign-in also failed: ${e2.message}", e2)
                    Result.failure(e2)
                }
            } else {
                Result.failure(e)
            }
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
            android.util.Log.d("AuthManager", "✅ User profile created successfully")
        } catch (e: Exception) {
            // Log the error but don't fail - user is still authenticated
            android.util.Log.w("AuthManager", "⚠️ Failed to create user profile in Firestore: ${e.message}", e)
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
            android.util.Log.d("AuthManager", "✅ User profile created/updated successfully")
        } catch (e: Exception) {
            // Log the error but don't fail the sign-in - user is still authenticated
            android.util.Log.w("AuthManager", "⚠️ Failed to create/update user profile in Firestore: ${e.message}", e)
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
            val doc = firestore.collection(USERS_COLLECTION).document(user.uid).get().await()
            if (doc.exists()) {
                doc.toObject(UserProfile::class.java)
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
            android.util.Log.w("AuthManager", "⚠️ Failed to get profile from Firestore, using Auth data: ${e.message}")
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
