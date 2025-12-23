package com.example.weatherapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object WeatherBackgrounds {

    val Clear = Brush.verticalGradient(
        listOf(
            Color(0xFF4FACFE),
            Color(0xFF00F2FE)
        )
    )

    val Clouds = Brush.verticalGradient(
        listOf(
            Color(0xFF89F7FE),
            Color(0xFF66A6FF)
        )
    )

    val Rain = Brush.verticalGradient(
        listOf(
            Color(0xFF373B44),
            Color(0xFF4286F4)
        )
    )

    val Snow = Brush.verticalGradient(
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE0EAFC)
        )
    )

    val Night = Brush.verticalGradient(
        listOf(
            Color(0xFF232526),
            Color(0xFF414345)
        )
    )
}
