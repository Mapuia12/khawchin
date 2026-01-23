# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ═══════════════════════════════════════════════════════════════════════════════
# KEEP LINE NUMBERS FOR CRASH REPORTS
# ═══════════════════════════════════════════════════════════════════════════════
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ═══════════════════════════════════════════════════════════════════════════════
# FIREBASE / FIRESTORE
# ═══════════════════════════════════════════════════════════════════════════════
# Keep Firestore model classes
-keep class com.mapuia.khawchinthlirna.data.model.** { *; }

# Firebase Firestore uses reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Firebase Auth
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# GSON (for WeatherCache JSON serialization)
# ═══════════════════════════════════════════════════════════════════════════════
-keepattributes Signature
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

# Keep fields with @SerializedName annotation
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic type info for Gson
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ═══════════════════════════════════════════════════════════════════════════════
# KOTLIN
# ═══════════════════════════════════════════════════════════════════════════════
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ═══════════════════════════════════════════════════════════════════════════════
# ROOM DATABASE
# ═══════════════════════════════════════════════════════════════════════════════
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ═══════════════════════════════════════════════════════════════════════════════
# KOIN DEPENDENCY INJECTION
# ═══════════════════════════════════════════════════════════════════════════════
-keep class org.koin.** { *; }
-keepclassmembers class * { public <init>(...); }

# ═══════════════════════════════════════════════════════════════════════════════
# JETPACK COMPOSE
# ═══════════════════════════════════════════════════════════════════════════════
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ═══════════════════════════════════════════════════════════════════════════════
# COIL (Image Loading)
# ═══════════════════════════════════════════════════════════════════════════════
-keep class coil.** { *; }
-dontwarn coil.**

# ═══════════════════════════════════════════════════════════════════════════════
# GOOGLE ADS
# ═══════════════════════════════════════════════════════════════════════════════
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ═══════════════════════════════════════════════════════════════════════════════
# GLANCE (App Widgets)
# ═══════════════════════════════════════════════════════════════════════════════
-keep class androidx.glance.** { *; }
-keep class com.mapuia.khawchinthlirna.widget.** { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# WORK MANAGER
# ═══════════════════════════════════════════════════════════════════════════════
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# ═══════════════════════════════════════════════════════════════════════════════
# ENUMS (used in data models)
# ═══════════════════════════════════════════════════════════════════════════════
-keepclassmembers enum * { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# R8 FULL MODE COMPATIBILITY
# ═══════════════════════════════════════════════════════════════════════════════
-dontwarn java.lang.invoke.StringConcatFactory