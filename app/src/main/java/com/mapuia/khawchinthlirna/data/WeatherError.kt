package com.mapuia.khawchinthlirna.data

sealed class WeatherError(val message: String) {
    object NetworkError : WeatherError("Network connection failed")
    object LocationError : WeatherError("Location access failed")
    object DataError : WeatherError("Invalid weather data")
    data class ApiError(val code: Int, val details: String) : WeatherError("API Error: $code - $details")
    data class UnknownError(val details: String) : WeatherError("Unknown error: $details")
}

enum class LoadingState { Idle, Loading, Success, Error }

data class AsyncResult<T>(
    val state: LoadingState = LoadingState.Idle,
    val data: T? = null,
    val error: WeatherError? = null
)