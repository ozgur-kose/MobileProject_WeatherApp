package com.example.weatherapp.domain.model

data class ForecastInfo(
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>
)

data class HourlyForecast(
    val time: Long,       // epoch seconds
    val temp: Double,
    val iconCode: String,
    val description: String
)

data class DailyForecast(
    val date: Long,       // epoch seconds
    val min: Double,
    val max: Double,
    val iconCode: String,
    val description: String
)
