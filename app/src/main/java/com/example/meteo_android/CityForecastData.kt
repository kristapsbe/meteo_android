package com.example.meteo_android

import kotlinx.serialization.Serializable


@Serializable
data class CoordinateData(val lat: Double, val lon: Double)

@Serializable
data class CityData(
    val id: String,
    val name: String,
    val type: String,
    val coords: CoordinateData
)

@Serializable
data class ForecastData(val id: String, val time: Long, val vals: List<Double>)

@Serializable
data class WarningData(val intensity: List<String>, val regions: List<String>, val type: List<String>)

@Serializable
data class CityForecastData(
    val hourly_params: List<List<String>>,
    val daily_params: List<List<String>>,
    val cities: List<CityData>,
    val hourly_forecast: List<ForecastData>,
    val daily_forecast: List<ForecastData>,
    val warnings: List<WarningData>,
    val all_warnings: HashMap<String, String>,
    val last_updated: String
)