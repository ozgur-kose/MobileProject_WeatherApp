package com.example.weatherapp.data.remote
import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val name: String,
    val coord: CoordDto,
    val main: MainDto,
    val weather: List<WeatherDescDto>,
    val wind: WindDto,
    @SerializedName("timezone")
    val timezone: Int? // saniye cinsinden offset (ör: 10800)
)


data class CoordDto(
    val lat: Double,
    val lon: Double
)

data class MainDto(
    val temp: Double,
    val feels_like: Double,
    val humidity: Int,
    val pressure: Int,
    val temp_min: Double,
    val temp_max: Double
)

data class WeatherDescDto(
    val description: String,
    val icon: String
)

data class WindDto(
    val speed: Double
)
