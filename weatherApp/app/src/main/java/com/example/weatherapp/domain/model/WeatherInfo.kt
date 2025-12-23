package com.example.weatherapp.domain.model

data class WeatherInfo(
    val cityName: String,
    val temp: Double,
    val feelsLike: Double,
    val description: String,
    val humidity: Int,
    val iconCode: String,
    val pressure: Int,
    val tempMin: Double,
    val tempMax: Double,
    val windSpeed: Double,
    val latitude: Double,        // 👈 YENİ
    val longitude: Double,       // 👈 YENİ

    val timezoneOffset: Int? = null
)
