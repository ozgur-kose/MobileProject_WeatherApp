package com.example.weatherapp.data.repository

import com.example.weatherapp.data.remote.Forecast5DayResponse
import com.example.weatherapp.data.remote.RetrofitInstance
import com.example.weatherapp.data.remote.WeatherResponse
import com.example.weatherapp.domain.model.DailyForecast
import com.example.weatherapp.domain.model.ForecastInfo
import com.example.weatherapp.domain.model.HourlyForecast
import com.example.weatherapp.domain.model.WeatherInfo
import com.example.weatherapp.domain.repository.WeatherRepository

class WeatherRepositoryImpl : WeatherRepository {

    // KENDİ API KEY'İN:
    private val apiKey = "277e0cc965d668aa929d1b8bded93496"

    override suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherInfo {
        val response = RetrofitInstance.api.getCurrentWeather(
            lat = lat,
            lon = lon,
            apiKey = apiKey
        )
        return response.toWeatherInfo()
    }

    override suspend fun getCurrentWeatherByCity(city: String): WeatherInfo {
        val response = RetrofitInstance.api.getCurrentWeatherByCity(
            city = city,
            apiKey = apiKey
        )
        return response.toWeatherInfo()
    }

    override suspend fun getForecast(lat: Double, lon: Double): ForecastInfo {
        val response = RetrofitInstance.api.getFiveDayForecast(
            lat = lat,
            lon = lon,
            apiKey = apiKey
        )

        return response.toForecastInfo()
    }


    // DTO -> Domain dönüşümü tek yerde
    private fun WeatherResponse.toWeatherInfo(): WeatherInfo {
        val main = this.main
        val desc = this.weather.firstOrNull()
        return WeatherInfo(
            cityName  = this.name,
            temp      = main.temp,
            feelsLike = main.feels_like,
            description = desc?.description ?: "",
            humidity  = main.humidity,
            iconCode  = desc?.icon ?: "",
            pressure  = main.pressure,
            tempMin   = main.temp_min,
            tempMax   = main.temp_max,
            windSpeed = this.wind.speed,
            latitude  = this.coord.lat,       // 👈 BURADAN
            longitude = this.coord.lon  ,      // 👈 BURADAN
            timezoneOffset = this.timezone

        )
    }
    private fun Forecast5DayResponse.toForecastInfo(): ForecastInfo {

        // 3 saatlik veriden hourly list
        val hourlyDomain = list.take(8).map { item ->
            val w = item.weather.firstOrNull()
            HourlyForecast(
                time = item.dt,
                temp = item.main.temp,
                iconCode = w?.icon ?: "",
                description = w?.description ?: ""
            )
        }

        // Günlük tahmin: her günün ilk (veya ortalama) verisini alacağız
        val dailyGrouped = list.groupBy { item ->
            java.time.Instant.ofEpochSecond(item.dt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }

        val dailyDomain = dailyGrouped.entries.take(5).map { entry ->
            val items = entry.value
            val first = items.first()
            val w = first.weather.firstOrNull()

            DailyForecast(
                date = first.dt,
                min = items.minOf { it.main.temp_min },
                max = items.maxOf { it.main.temp_max },
                iconCode = w?.icon ?: "",
                description = w?.description ?: ""
            )
        }

        return ForecastInfo(
            hourly = hourlyDomain,
            daily = dailyDomain
        )
    }


}
