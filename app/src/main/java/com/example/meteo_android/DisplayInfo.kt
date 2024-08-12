package com.example.meteo_android

import kotlinx.datetime.LocalDateTime


class WeatherPictogram(
    val code: Int
) {
    fun getPictogram(): Int {
        // TODO: build dynamic icon based on the code
        return R.drawable.example_battery
    }
}

class DailyForecast(
    val day: String,
    val rainAmount: Int,
    val stormProb: Double,
    val tempMin: Double,
    val tempMax: Double,
    val pictogramDay: WeatherPictogram,
    val pictogramNight: WeatherPictogram
)

class HourlyForecast(
    val currTemp: Double,
    val feelsLike: Double,
    val pictogram: WeatherPictogram
)

class CurrentInfo(
    var hourlyForecast: HourlyForecast?,
    var dailyForecast: DailyForecast?
)

class DailyInfo(
    var dailyForecasts: List<DailyForecast>
)

class MetadataInfo(
    var lastUpdated: LocalDateTime?
)