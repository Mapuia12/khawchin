package com.mapuia.khawchinthlirna.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room Database for Khawchin app offline caching
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
    exportSchema = false
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
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
