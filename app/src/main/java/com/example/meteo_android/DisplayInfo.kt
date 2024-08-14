package com.example.meteo_android

import android.util.Log
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.byUnicodePattern
import kotlin.random.Random


class WeatherPictogram(
    val code: Int
) {
    fun getPictogram(): Int {
        // TODO: build dynamic icon based on the code
        return R.drawable.example_battery
    }
}

class DailyForecast(
    val date: LocalDateTime,
    val rainAmount: Int,
    val rainProb: Int,
    val tempMin: Int,
    val tempMax: Int,
    val pictogramDay: WeatherPictogram,
    val pictogramNight: WeatherPictogram
) {
    fun getDay(): String {
        return date.dayOfWeek.toString()
    }
}

class HourlyForecast(
    val currentTemp: Int,
    val feelsLikeTemp: Int,
    val pictogram: WeatherPictogram
)

class DisplayInfo() {
    // Today
    var hourlyForecasts: List<HourlyForecast> = emptyList()
    // Tomorrow onwards
    var dailyForecasts: List<DailyForecast> = emptyList()

    private val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
    var lastUpdated: LocalDateTime = LocalDateTime(1972, 1, 1, 0, 0)

    constructor(cityForecastData: CityForecastData?) : this() {
        if (cityForecastData != null) {
            lastUpdated = stringToDatetime(cityForecastData.last_updated)
            // TODO: I should get the ids dynamically
            hourlyForecasts = cityForecastData.hourly_forecast.map { e ->
                HourlyForecast(
                    e.vals.get(1).toInt(),
                    e.vals.get(2).toInt(),
                    WeatherPictogram(e.vals.get(0).toInt())
                )
            }
            dailyForecasts = cityForecastData.daily_forecast.map { e ->
                DailyForecast(
                    stringToDatetime(e.time.toString()),
                    e.vals.get(4).toInt(),
                    e.vals.get(5).toInt(),
                    e.vals.get(3).toInt(),
                    e.vals.get(2).toInt(),
                    WeatherPictogram(e.vals.get(7).toInt()),
                    WeatherPictogram(e.vals.get(6).toInt())
                )
            }
        }
    }

    private fun stringToDatetime(dateString: String): LocalDateTime {
        return LocalDateTime(
            dateString.substring(0, 4).toInt(),
            dateString.substring(4, 6).toInt(),
            dateString.substring(6, 8).toInt(),
            dateString.substring(8, 10).toInt(),
            dateString.substring(10, 12).toInt(),
            0, 0
        )
    }

    fun getTodayForecast(): HourlyForecast {
        // TODO: fish out most recent relevant info
        if (hourlyForecasts.isNotEmpty()) {
            return hourlyForecasts.get(0)
        }
        return HourlyForecast(0, 0, WeatherPictogram(0))
    }

    fun getLastUpdated(): String {
        return format.format(lastUpdated)
    }
}