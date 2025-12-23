package com.example.weatherapp.presentation.weather

import com.example.weatherapp.domain.model.HourlyForecast
import com.example.weatherapp.domain.model.DailyForecast

data class WeatherUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val cityName: String? = null,
    val temp: Double? = null,
    val feelsLike: Double? = null,
    val description: String? = null,
    val humidity: Int? = null,
    val iconCode: String? = null,
    val pressure: Int? = null,
    val tempMin: Double? = null,
    val tempMax: Double? = null,
    val windSpeed: Double? = null,
    val searchText: String = "",
    val timezoneOffset: Int? = null ,

    val hourly: List<HourlyForecast> = emptyList(),   // 👈 YENİ
    val daily: List<DailyForecast> = emptyList()      // 👈 YENİ
)
