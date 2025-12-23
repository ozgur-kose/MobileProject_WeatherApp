package com.example.weatherapp.data.remote

data class Forecast5DayResponse(
    val list: List<ForecastItem>,
)

data class ForecastItem(
    val dt: Long,
    val main: MainData,
    val weather: List<WeatherDesc>,
)

data class MainData(
    val temp: Double,
    val temp_min: Double,
    val temp_max: Double
)

data class WeatherDesc(
    val description: String,
    val icon: String
)

