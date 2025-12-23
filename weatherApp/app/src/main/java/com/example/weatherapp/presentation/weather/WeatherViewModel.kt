package com.example.weatherapp.presentation.weather

import retrofit2.HttpException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.repository.WeatherRepositoryImpl
import com.example.weatherapp.domain.model.WeatherInfo
import com.example.weatherapp.domain.repository.WeatherRepository
import kotlinx.coroutines.launch
import java.net.UnknownHostException


class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepositoryImpl()
) : ViewModel() {

    var uiState by mutableStateOf(WeatherUiState())
        private set

    fun loadWeather(lat: Double, lon: Double) {
        fetchWeather { repository.getCurrentWeather(lat, lon) }
    }

    fun loadWeatherByCity(city: String) {
        val normalized = city.normalizeCityInput()
        if (normalized.isBlank()) return

        fetchWeather { repository.getCurrentWeatherByCity(normalized) }
    }

    private fun String.normalizeCityInput(): String {
        val trimmed = this.trim()
        if (trimmed.isEmpty()) return trimmed

        val map = mapOf(
            'ç' to 'c', 'Ç' to 'C',
            'ğ' to 'g', 'Ğ' to 'G',
            'ı' to 'i', 'İ' to 'I',
            'ö' to 'o', 'Ö' to 'O',
            'ş' to 's', 'Ş' to 'S',
            'ü' to 'u', 'Ü' to 'U'
        )

        val sb = StringBuilder()
        for (ch in trimmed) {
            sb.append(map[ch] ?: ch)
        }
        return sb.toString()
    }

    private fun translateDescription(desc: String): String {
        val map = mapOf(
            "clear sky" to "Açık",
            "few clouds" to "Az bulutlu",
            "scattered clouds" to "Parçalı bulutlu",
            "broken clouds" to "Çok bulutlu",
            "overcast clouds" to "Kapalı",
            "light rain" to "Hafif yağmur",
            "moderate rain" to "Yağmurlu",
            "heavy intensity rain" to "Şiddetli yağmur",
            "thunderstorm" to "Gök gürültülü fırtına",
            "snow" to "Karlı",
            "light snow" to "Hafif kar",
            "mist" to "Puslu",
            "fog" to "Sisli",
            "haze" to "Dumanlı",
            "smoke" to "Duman",
            "dust" to "Tozlu",
            "sand" to "Kum fırtınası",
            "drizzle" to "Çiseleme"
        )

        return map[desc.lowercase()] ?: desc.replaceFirstChar { it.uppercase() }
    }

    private fun fetchWeather(block: suspend () -> WeatherInfo) {
        uiState = uiState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // 1) Önce anlık hava bilgisi
                val info = block()

                // 2) Forecast iste (opsiyonel)
                val forecastResult = try {
                    val result = repository.getForecast(
                        lat = info.latitude,
                        lon = info.longitude
                    )
                    android.util.Log.d("ForecastTest", "Forecast geldi: hourly=${result.hourly.size} daily=${result.daily.size}")
                    result
                } catch (e: Exception) {
                    android.util.Log.e("ForecastTest", "Forecast HATA", e)
                    null
                }



                // 3) UI state’i güncelle ve ARAMA KUTUSUNU TEMİZLE
                uiState = uiState.copy(
                    isLoading = false,
                    cityName = info.cityName,
                    temp = info.temp,
                    feelsLike = info.feelsLike,
                    description = translateDescription(info.description),
                    humidity = info.humidity,
                    iconCode = info.iconCode,
                    pressure = info.pressure,
                    tempMin = info.tempMin,
                    tempMax = info.tempMax,
                    windSpeed = info.windSpeed,
                    searchText = "",              // 🔹 TextField buradan temizleniyor
                    hourly = forecastResult?.hourly?.map {
                        it.copy(description = translateDescription(it.description))
                    } ?: emptyList(),
                    daily = forecastResult?.daily?.map {
                        it.copy(description = translateDescription(it.description))
                    } ?: emptyList(),
                    timezoneOffset = info.timezoneOffset,
                )
            } catch (e: Exception) {
                val message = when {
                    e is HttpException && e.code() == 404 ->
                        "Bu isimde bir şehir bulunamadı.\nLütfen şehir adını kontrol edip tekrar deneyin."

                    e is UnknownHostException ->
                        "İnternet bağlantısı yok gibi görünüyor.\nLütfen bağlantınızı kontrol edin."

                    else ->
                        "Beklenmeyen bir hata oluştu.\nLütfen daha sonra tekrar deneyin."
                }

                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = message
                )
            }
        }
    }
}
