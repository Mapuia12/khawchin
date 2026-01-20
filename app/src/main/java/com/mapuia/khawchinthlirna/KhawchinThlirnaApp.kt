package com.mapuia.khawchinthlirna

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.mapuia.khawchinthlirna.di.appModule
import com.mapuia.khawchinthlirna.service.NotificationChannels
import com.mapuia.khawchinthlirna.util.InterstitialAdManager
import com.mapuia.khawchinthlirna.worker.WorkScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KhawchinThlirnaApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Ads SDK init (safe to call once; required for consistent ad loading).
        MobileAds.initialize(this) {}
        
        // Initialize interstitial ads (preload first ad)
        InterstitialAdManager.init(this)

        // Initialize Koin DI
        startKoin {
            androidLogger()
            androidContext(this@KhawchinThlirnaApp)
            modules(appModule)
        }

        // Create notification channels
        NotificationChannels.createChannels(this)

        // Schedule periodic background work
        WorkScheduler.schedulePeriodicWeatherRefresh(this)
        WorkScheduler.scheduleDailyCleanup(this)
    }
}
