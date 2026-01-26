package com.mapuia.khawchinthlirna.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mapuia.khawchinthlirna.BuildConfig

/**
 * Room Database for Khawchin app offline caching
 * 
 * MIGRATION STRATEGY:
 * - Cache tables (weather, hourly, reports): Destructive migration OK (will refresh)
 * - User data tables (favorites, notifications, preferences): Requires proper migration
 * 
 * When adding schema changes:
 * 1. Increment version number
 * 2. Add migration object (MIGRATION_X_Y)
 * 3. Add migration to builder
 * 
 * For cache-only changes, fallbackToDestructiveMigration handles it safely.
 */
@Database(
    entities = [
        CachedWeatherEntity::class,
        CachedReportEntity::class,
        CachedHourlyEntity::class,
        FavoriteLocationEntity::class,
        NotificationEntity::class,
        UserPreferencesEntity::class
    ],
    version = 1,
    exportSchema = false  // Disable for now - enable when adding migrations
)
@TypeConverters(Converters::class)
abstract class KhawchinDatabase : RoomDatabase() {

    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun reportDao(): ReportDao
    abstract fun hourlyForecastDao(): HourlyForecastDao
    abstract fun favoriteLocationDao(): FavoriteLocationDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        private const val DATABASE_NAME = "khawchin_database"

        @Volatile
        private var INSTANCE: KhawchinDatabase? = null

        // Define migrations here as schema evolves
        // Example:
        // private val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         database.execSQL("ALTER TABLE favorites ADD COLUMN notes TEXT DEFAULT ''")
        //     }
        // }

        fun getInstance(context: Context): KhawchinDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): KhawchinDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                KhawchinDatabase::class.java,
                DATABASE_NAME
            )
                // Add migrations here as schema evolves:
                // .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                
                // Fallback for cache tables - user data should have proper migrations
                // In debug builds, allow destructive migration for faster iteration
                .apply {
                    if (BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration()
                    } else {
                        // In release, only allow destructive migration from specific versions
                        // where we know it's safe (e.g., cache-only schema changes)
                        fallbackToDestructiveMigrationOnDowngrade()
                    }
                }
                .build()
        }
    }
}
