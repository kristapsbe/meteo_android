package lv.kristapsbe.meteo_android

import android.app.Activity.MODE_PRIVATE
import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.random.Random


@Serializable
data class ForecastData(val time: Long, val vals: List<Double>)

@Serializable
data class WarningData(
    val id: Int,
    val intensity: List<String>,
    val regions: List<String>,
    val type: List<String>,
    val time: List<Long>,
    val description: List<String>
)

@Serializable
data class CityForecastData(
    val hourly_params: List<List<String>>,
    val daily_params: List<List<String>>,
    val city: String,
    val hourly_forecast: List<ForecastData>,
    val daily_forecast: List<ForecastData>,
    val warnings: List<WarningData>,
    val last_updated: String,
    val last_downloaded: String
)


class CityForecastDataDownloader {
    companion object {
        const val RESPONSE_FILE = "response.json"

        fun downloadData(ctx: Context, lat: Double = 56.9730, lon: Double = 24.1327): CityForecastData? {
            try {
                val randTemp = String.format("%.1f", Random.nextInt(60)-30+ Random.nextDouble())
                // local ip 10.0.2.2
                var urlString = "https://meteo.kristapsbe.lv/api/v1/forecast/test_ctemp?temp=$randTemp"
                urlString = "https://meteo.kristapsbe.lv/api/v1/forecast/cities?lat=$lat&lon=$lon"
                val response = URL(urlString).readText()
                ctx.openFileOutput(RESPONSE_FILE, MODE_PRIVATE).use { fos ->
                    fos.write(response.toByteArray())
                }
            } catch (e: Exception) {
                Log.d("DEBUG", "DOWNLOAD FAILED $e")
            }

            var cityForecast: CityForecastData? = null
            try {
                val content = ctx.openFileInput(RESPONSE_FILE).bufferedReader().use { it.readText() }
                cityForecast = Json.decodeFromString<CityForecastData>(content)
            } catch (e: Exception) {
                Log.d("DEBUG", "LOADDATA FAILED $e")
            }
            return cityForecast
        }


        fun downloadData(ctx: Context, locationName: String = "Riga"): CityForecastData? {
            try {
                // local ip 10.0.2.2
                val urlString = "https://meteo.kristapsbe.lv/api/v1/forecast/cities/name?city_name=$locationName"
                val response = URL(urlString).readText()
                ctx.openFileOutput(RESPONSE_FILE, MODE_PRIVATE).use { fos ->
                    fos.write(response.toByteArray())
                }
            } catch (e: Exception) {
                Log.d("DEBUG", "DOWNLOAD locationName FAILED $e")
            }

            var cityForecast: CityForecastData? = null
            try {
                val content = ctx.openFileInput(RESPONSE_FILE).bufferedReader().use { it.readText() }
                cityForecast = Json.decodeFromString<CityForecastData>(content)
            } catch (e: Exception) {
                Log.d("DEBUG", "LOADDATA locationName FAILED $e")
            }
            return cityForecast
        }
    }
}