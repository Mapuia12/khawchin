package com.mapuia.khawchinthlirna.di

import com.google.firebase.firestore.FirebaseFirestore
import com.mapuia.khawchinthlirna.WeatherViewModel
import com.mapuia.khawchinthlirna.data.LocationProvider
import com.mapuia.khawchinthlirna.data.ReverseGeocoder
import com.mapuia.khawchinthlirna.data.WeatherCache
import com.mapuia.khawchinthlirna.data.WeatherRepository
import com.mapuia.khawchinthlirna.data.auth.AuthManager
import com.mapuia.khawchinthlirna.data.auth.GamificationManager
import com.mapuia.khawchinthlirna.data.local.KhawchinDatabase
import com.mapuia.khawchinthlirna.data.preferences.PreferencesManager
import com.mapuia.khawchinthlirna.data.verification.ReportVerificationService
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase
    single { FirebaseFirestore.getInstance() }

    // Database
    single { KhawchinDatabase.getInstance(androidContext()) }
    single { get<KhawchinDatabase>().weatherCacheDao() }
    single { get<KhawchinDatabase>().reportDao() }
    single { get<KhawchinDatabase>().hourlyForecastDao() }
    single { get<KhawchinDatabase>().favoriteLocationDao() }
    single { get<KhawchinDatabase>().notificationDao() }
    single { get<KhawchinDatabase>().userPreferencesDao() }

    // Cache and Repository
    single { WeatherCache(appContext = androidContext()) }
    single { WeatherRepository(db = get(), cache = get()) }

    // Location
    single { LocationProvider(androidContext()) }
    single { ReverseGeocoder(androidContext()) }

    // Auth
    single { AuthManager(androidContext(), get()) }
    
    // Gamification
    single { GamificationManager(get()) }

    // Preferences
    single { PreferencesManager(androidContext()) }

    // Verification
    single { ReportVerificationService(get()) }

    // ViewModel
    viewModel {
        val app = androidContext().applicationContext as android.app.Application
        WeatherViewModel(
            app = app,
            repository = get(),
            locationProvider = get(),
            reverseGeocoder = get(),
        )
    }
}

