package lv.kristapsbe.meteo_android

import android.app.Activity.MODE_PRIVATE
import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL


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
data class AuroraProbs(
    val prob: Int,
    val time: String
)

@Serializable
data class CityForecastData(
    val city: String,
    val hourly_forecast: List<ForecastData>,
    val daily_forecast: List<ForecastData>,
    val warnings: List<WarningData>,
    val last_updated: String,
    val last_downloaded: String,
    val aurora_probs: AuroraProbs
)


class CityForecastDataDownloader {
    companion object {
        const val RESPONSE_FILE = "response.json"
        //private const val BASE_URL = "http://10.0.2.2:8000/api/v1/forecast/cities"
        private const val BASE_URL = "https://meteo.kristapsbe.lv/api/v1/forecast/cities"

        private fun downloadData(ctx: Context, urlString: String, doDL: Boolean): CityForecastData? {
            var cityForecast: CityForecastData? = null
            if (doDL) {
                try {
                    val response = URL(urlString).readText()
                    cityForecast = Json.decodeFromString<CityForecastData>(response)
                    if (cityForecast.city != "") {
                        ctx.openFileOutput(RESPONSE_FILE, MODE_PRIVATE).use { fos ->
                            fos.write(response.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ERROR", "Failed to download forecast data: $e")
                }
            }

            if (cityForecast == null || cityForecast.city == "") {
                try {
                    val content = loadStringFromStorage(ctx, RESPONSE_FILE)
                    cityForecast = Json.decodeFromString<CityForecastData>(content)
                } catch (e: Exception) {
                    Log.e("ERROR", "Failed to load forecast data from storage: $e")
                }
            }
            return cityForecast
        }

        fun downloadDataLatLon(ctx: Context, lat: Double = 56.9730, lon: Double = 24.1327, doDL: Boolean = true): CityForecastData? {
            return downloadData(ctx, "$BASE_URL?lat=$lat&lon=$lon&add_params=false&add_aurora=true", doDL)
        }

        fun downloadDataCityName(ctx: Context, locationName: String = "Riga", doDL: Boolean = true): CityForecastData? {
            return downloadData(ctx, "$BASE_URL/name?city_name=$locationName&add_params=false&add_aurora=true", doDL)
        }

        fun loadStringFromStorage(ctx: Context, fileName: String): String {
            for (f in ctx.fileList()) {
                if (f.equals(fileName)) {
                    return ctx.openFileInput(fileName).bufferedReader().use { it.readText() }
                }
            }
            return ""
        }
    }
}
