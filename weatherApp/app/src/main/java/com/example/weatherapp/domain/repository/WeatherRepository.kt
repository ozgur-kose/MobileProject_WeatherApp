package com.example.weatherapp.domain.repository

import com.example.weatherapp.domain.model.WeatherInfo
import com.example.weatherapp.domain.model.ForecastInfo

interface WeatherRepository {
    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherInfo
    suspend fun getCurrentWeatherByCity(city: String): WeatherInfo
    suspend fun getForecast(lat: Double, lon: Double): ForecastInfo   // 👈 YENİ
}
