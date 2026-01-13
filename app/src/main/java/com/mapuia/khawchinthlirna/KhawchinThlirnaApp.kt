package com.mapuia.khawchinthlirna

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.mapuia.khawchinthlirna.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KhawchinThlirnaApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Ads SDK init (safe to call once; required for consistent ad loading).
        MobileAds.initialize(this) {}

        startKoin {
            androidLogger()
            androidContext(this@KhawchinThlirnaApp)
            modules(appModule)
        }
    }
}
