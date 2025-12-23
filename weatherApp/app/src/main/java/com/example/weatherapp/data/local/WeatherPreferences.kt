package com.example.weatherapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Context'e extension:
private val Context.dataStore by preferencesDataStore("weather_prefs")

class WeatherPreferences(private val context: Context) {

    companion object {
        private val KEY_LAST_CITY = stringPreferencesKey("last_city")
    }

    suspend fun saveLastCity(city: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_CITY] = city
        }
    }

    suspend fun getLastCity(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_LAST_CITY]
    }
}
