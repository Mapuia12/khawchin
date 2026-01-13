package com.mapuia.khawchinthlirna.di

import com.google.firebase.firestore.FirebaseFirestore
import com.mapuia.khawchinthlirna.WeatherViewModel
import com.mapuia.khawchinthlirna.data.LocationProvider
import com.mapuia.khawchinthlirna.data.ReverseGeocoder
import com.mapuia.khawchinthlirna.data.WeatherCache
import com.mapuia.khawchinthlirna.data.WeatherRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseFirestore.getInstance() }

    single { WeatherCache(appContext = androidContext()) }
    single { WeatherRepository(db = get(), cache = get()) }

    single { LocationProvider(androidContext()) }
    single { ReverseGeocoder(androidContext()) }

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
