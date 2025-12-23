package com.example.weatherapp.presentation.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import com.example.weatherapp.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.weatherapp.data.local.WeatherPreferences
import com.google.android.gms.location.LocationServices







@DrawableRes
fun getWeatherIconRes(iconCode: String?, description: String?): Int {
    val d = description?.lowercase() ?: ""
    val code = iconCode ?: ""

    return when {
        // ☀ Açık - Gündüz
        code.startsWith("01d") || ("açık" in d && !("gece" in d)) ->
            R.drawable.clear_day

        // 🌙 Açık - Gece
        code.startsWith("01n") || ("açık" in d && "gece" in d) ->
            R.drawable.clear_night

        // 🌤 Az bulutlu
        code.startsWith("02") || "az bulut" in d ->
            R.drawable.partly_cloudy_day

        // ☁ Bulutlu / Kapalı
        code.startsWith("03") || code.startsWith("04") ||
                "bulut" in d || "kapalı" in d ->
            R.drawable.cloudy

        // 🌧 Yağmurlu
        code.startsWith("09") || code.startsWith("10") || "yağmur" in d ->
            R.drawable.rain

        // 💦 Çiseleme / Drizzle
        "drizzle" in d ->
            R.drawable.raindrops

        // ⛈ Fırtına
        code.startsWith("11") || "fırtına" in d || "gök gürültülü" in d ->
            R.drawable.thunderstorms_1

        // ❄ Kar
        code.startsWith("13") || "kar" in d ->
            R.drawable.snow

        // 🌫 Sis
        code.startsWith("50") || "sis" in d || "fog" in d ->
            R.drawable.fog

        else -> R.drawable.cloudy
    }
}








private enum class TimeOfDay {
    MORNING, DAY, EVENING, NIGHT
}

private fun detectTimeOfDayByCity(timezoneOffsetSeconds: Int?): TimeOfDay {
    val offset = timezoneOffsetSeconds ?: 0

    val utcNow = java.time.Instant.now()
    val cityTime = utcNow
        .atOffset(java.time.ZoneOffset.UTC)
        .plusSeconds(offset.toLong())

    val hour = cityTime.hour

    return when (hour) {
        in 6..11 -> TimeOfDay.MORNING   // Sabah
        in 12..17 -> TimeOfDay.DAY      // Öğlen
        in 18..20 -> TimeOfDay.EVENING  // Akşam
        else -> TimeOfDay.NIGHT         // Gece
    }
}

fun pickBackgroundByTime(timezoneOffsetSeconds: Int?): Brush {
    return when (detectTimeOfDayByCity(timezoneOffsetSeconds)) {

        // 🌅 SABAH — Turuncu güneş dokunuşlu açık mavi gökyüzü
        TimeOfDay.MORNING -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFE0B2), // soft sunrise orange
                Color(0xFFFFCC80), // warm morning glow
                Color(0xFFB3E5FC)  // light morning sky blue
            )
        )

        // 🌤 ÖĞLEN — Parlak gökyüzü mavisi (net, temiz)
        TimeOfDay.DAY -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF42A5F5), // bright blue
                Color(0xFF1E88E5), // deep sky blue
                Color(0xFF00BCD4)  // cyan touch
            )
        )

        // 🌇 AKŞAM — Turuncu → pembe → mor dramatik sunset
        TimeOfDay.EVENING -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFB347), // sunset orange
                Color(0xFFFF5F6D), // sunset pink-orange
                Color(0xFF845EC2)  // twilight purple
            )
        )

        // 🌌 GECE — Galaksi / derin uzay tonları
        TimeOfDay.NIGHT -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D0B1A), // deep space
                Color(0xFF1B1C46), // night indigo
                Color(0xFF3A3F79)  // twilight blue
            )
        )
    }
}




@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState

    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val prefs = remember { WeatherPreferences(context) }

    var cityQuery by remember { mutableStateOf("") }
    var hasRequestedPermission by remember { mutableStateOf(false) }

    // İzin sonucu için launcher
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                requestLocation(fusedClient, viewModel)
            }
        }

    // Tek noktadan konum isteme fonksiyonu
    val requestLocationOrAskPermission: () -> Unit = {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            requestLocation(fusedClient, viewModel)
        } else if (!hasRequestedPermission) {
            hasRequestedPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // İlk açılışta:
    // 1) Son şehri göster
    // 2) Konumu otomatik dene
    LaunchedEffect(Unit) {
        val lastCity = prefs.getLastCity()
        if (lastCity != null) {
            viewModel.loadWeatherByCity(lastCity)
        } else {
            viewModel.loadWeatherByCity("Karaköy")
        }
        requestLocationOrAskPermission()
    }

    // Şehir değiştikçe son şehri DataStore'a kaydet
    LaunchedEffect(state.cityName) {
        state.cityName?.let { city ->
            prefs.saveLastCity(city)
        }
    }

    // konumun saatini almak için en mantıklısı ilk saatlik tahmin (veya ayrı currentTime field'ın varsa onu kullan)
    val currentLocationTime = state.hourly.firstOrNull()?.time

    // Eğer WeatherUiState içinde timezone offset alanın varsa:
    val bgBrush = pickBackgroundByTime(state.timezoneOffset)





    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // 🔎 Şehir arama + konum ikonu
            OutlinedTextField(
                value = cityQuery,
                onValueChange = { cityQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = {
                    Text(
                        text = "Şehir / ilçe giriniz...",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.9f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Black.copy(alpha = 0.18f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.12f)
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.loadWeatherByCity(cityQuery)
                    }
                ),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { requestLocationOrAskPermission() }) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "Konumuma dön",
                                tint = Color.White
                            )
                        }
                        TextButton(
                            onClick = { viewModel.loadWeatherByCity(cityQuery) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Ara",
                                color = Color.White
                            )
                        }
                    }
                }
            )


            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White
                        )
                    }

                    state.errorMessage != null -> {
                        Text(
                            text = state.errorMessage ?: "",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        WeatherContent(
                            state = state,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Konumu alma işini tek yere topladık
// ------------------------------------------------------------
@SuppressLint("MissingPermission")
private fun requestLocation(
    fusedClient: com.google.android.gms.location.FusedLocationProviderClient,
    viewModel: WeatherViewModel
) {
    fusedClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
            viewModel.loadWeather(loc.latitude, loc.longitude)
        } else {
            fusedClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                location?.let {
                    viewModel.loadWeather(it.latitude, it.longitude)
                }
            }
        }
    }
}

// ------------------------------------------------------------
//  UI İÇERİĞİ
// ------------------------------------------------------------

@Composable
private fun WeatherContent(
    state: WeatherUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                // Şehir adı
                Text(
                    text = state.cityName ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(Modifier.height(10.dp))

                // Ana hava durumu ikonu (şehir ile derece arasında)
                if (!state.iconCode.isNullOrBlank()) {
                    val mainIcon = getWeatherIconRes(state.iconCode, state.description)

                    Icon(
                        painter = painterResource(id = mainIcon),
                        contentDescription = state.description,
                        modifier = Modifier.size(72.dp),
                        tint = Color.Unspecified   // ❗ ikonları beyaza boyamasın
                    )

                    Spacer(Modifier.height(6.dp))
                }

            // Derece
                Text(
                    text = "${state.temp?.toInt() ?: 0}°C",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

            // Açıklama
                Text(
                    text = state.description ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )


                Spacer(Modifier.height(16.dp))

                // Hissedilen
                Text(
                    text = "Hissedilen: ${state.feelsLike?.toInt() ?: 0}°C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Spacer(Modifier.height(24.dp))

                // Min / Max / Nem
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip("Min", "${state.tempMin?.toInt() ?: 0}°")
                    InfoChip("Max", "${state.tempMax?.toInt() ?: 0}°")
                    InfoChipIcon(
                        iconRes = R.drawable.humidity,
                        text = "${state.humidity ?: 0}%"
                    )



                }

                Spacer(Modifier.height(12.dp))

                // Rüzgar / Basınç
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChipIcon(
                        iconRes = R.drawable.wind_1,
                        text = "${state.windSpeed ?: 0.0} m/s"
                    )



                    InfoChip(
                        title = "Basınç",
                        value = "${state.pressure ?: 0} hPa"
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Saatlik Tahmin
                if (state.hourly.isNotEmpty()) {
                    HourlyForecastSection(state)
                    Spacer(Modifier.height(24.dp))
                }

                // Günlük Tahmin
                if (state.daily.isNotEmpty()) {
                    DailyForecastSection(state)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}


@Composable
fun InfoChipIcon(
    @DrawableRes iconRes: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified   // ikon kendi rengini korusun
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}


@Composable
private fun HourlyForecastSection(
    state: WeatherUiState
) {
    Text(
        text = "Saatlik Tahmin",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(state.hourly.size) { index ->
            val item = state.hourly[index]
            HourlyCard(item)
        }
    }
}

@Composable
private fun HourlyCard(item: com.example.weatherapp.domain.model.HourlyForecast) {

    val timeText = remember(item.time) {
        val instant = java.time.Instant.ofEpochSecond(item.time)
        val localDateTime = java.time.LocalDateTime.ofInstant(
            instant,
            java.time.ZoneId.systemDefault()
        )
        val hour = localDateTime.hour
        String.format("%02d:00", hour)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.20f)
        ),
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Saat
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )

            Spacer(Modifier.height(6.dp))

            // ⭐ Meteocons ikonunu çağırıyoruz
            val hourlyIconRes =
                getWeatherIconRes(item.iconCode, item.description)

            Icon(
                painter = painterResource(id = hourlyIconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified   // İkon rengini koru!
            )

            Spacer(Modifier.height(6.dp))

            // Sıcaklık
            Text(
                text = "${item.temp.toInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}


@Composable
private fun DailyForecastSection(
    state: WeatherUiState
) {
    Text(
        text = "5 Günlük Tahmin",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        state.daily.forEach { item ->
            DailyRow(item)
        }
    }
}

@Composable
private fun DailyRow(item: com.example.weatherapp.domain.model.DailyForecast) {

    val dateText = remember(item.date) {
        val instant = java.time.Instant.ofEpochSecond(item.date)
        val localDate = java.time.LocalDateTime.ofInstant(
            instant,
            java.time.ZoneId.systemDefault()
        ).toLocalDate()

        val dayNameTr = when (localDate.dayOfWeek) {
            java.time.DayOfWeek.MONDAY    -> "Pazartesi"
            java.time.DayOfWeek.TUESDAY   -> "Salı"
            java.time.DayOfWeek.WEDNESDAY -> "Çarşamba"
            java.time.DayOfWeek.THURSDAY  -> "Perşembe"
            java.time.DayOfWeek.FRIDAY    -> "Cuma"
            java.time.DayOfWeek.SATURDAY  -> "Cumartesi"
            java.time.DayOfWeek.SUNDAY    -> "Pazar"
        }

        "$dayNameTr ${localDate.dayOfMonth}.${localDate.monthValue}"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.16f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                // ⭐ METEOCONS ICON — OpenWeather yerine yerel SVG/XML kullanıyoruz
                val dailyIconRes =
                    getWeatherIconRes(item.iconCode, item.description)

                Icon(
                    painter = painterResource(id = dailyIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.Unspecified  // İkonun kendi rengini korur
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "${item.min.toInt()}° / ${item.max.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

