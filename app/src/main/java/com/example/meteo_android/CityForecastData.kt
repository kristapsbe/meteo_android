package com.example.meteo_android

import android.app.Activity.MODE_PRIVATE
import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.random.Random


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


class CityForecastDataDownloader {
    companion object {
        val responseFname = "response.json"

        fun downloadData(src: String, ctx: Context, lat: Double = 56.8750, lon: Double = 23.8658): CityForecastData? {
            Log.i("DL", "downloadData - $src")

            try {
                val randTemp = String.format("%.1f", Random.nextInt(60)-30+ Random.nextDouble())
                var urlString = "http://10.0.2.2:8000/api/v1/forecast/test_ctemp?temp=$randTemp"
                //urlString = "http://10.0.2.2:8000/api/v1/forecast/cities?lat=$lat&lon=$lon&radius=10"
                val response = URL(urlString).readText()
                Log.i("RRSP", "$response")
                ctx.openFileOutput(responseFname, MODE_PRIVATE).use { fos ->
                    fos.write(response.toByteArray())
                }
            } catch (e: Exception) {
                Log.d("DEBUG", "DOWNLOAD FAILED $e")
            }

            var cityForecast: CityForecastData? = null
            try {
                val content = ctx.openFileInput(responseFname).bufferedReader().use { it.readText() }
                Log.i("CRSP", "$content")
                cityForecast = Json.decodeFromString<CityForecastData>(content)
            } catch (e: Exception) {
                Log.d("DEBUG", "LOADDATA FAILED $e")
            }
            return cityForecast
        }
    }
}