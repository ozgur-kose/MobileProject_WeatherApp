package com.example.weatherapp.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.weatherapp.MainActivity
import com.example.weatherapp.R

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Her widget örneği için ayrı ayrı güncelle
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Widget layoutunu bağla
            val views = RemoteViews(context.packageName, R.layout.weather_widget)

            // Şimdilik statik text – sonra istersen WeatherPreferences ile son şehri basarız
            views.setTextViewText(R.id.textCity, "Hava Durumu")
            views.setTextViewText(R.id.textTemp, "--°C")
            views.setTextViewText(R.id.textDesc, "Uygulamayı açmak için dokun")

            // Widget'e tıklayınca uygulamayı aç
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Widget'i güncelle
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
