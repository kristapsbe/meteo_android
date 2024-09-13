package lv.kristapsbe.meteo_android

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.byUnicodePattern
import kotlin.math.roundToInt


class WeatherPictogram(
    private val code: Int
) {
    companion object {
        val iconMapping: HashMap<Int, Int> = hashMapOf(
            1101 to R.drawable.sun,
            1102 to R.drawable.cloud_sun,
            1103 to R.drawable.cloud_sun,
            1104 to R.drawable.cloud,
            1105 to R.drawable.cloud, // TODO: should be a cloud with a second cloud in the background

            1301 to R.drawable.cloud_lightning,
            1303 to R.drawable.cloud_lightning_sun,

            1504 to R.drawable.cloud_rain,
            1506 to R.drawable.cloud_rain_sun,

            2101 to R.drawable.moon,
            2102 to R.drawable.cloud_moon,
            2103 to R.drawable.cloud_moon,
            2104 to R.drawable.cloud,
            2105 to R.drawable.cloud, // TODO: should be a cloud with a second cloud in the background

            2303 to R.drawable.cloud_lightning_moon,

            2401 to R.drawable.cloud_fog,
            2403 to R.drawable.cloud_fog_moon,

            2504 to R.drawable.cloud_rain,
            2506 to R.drawable.cloud_rain_moon
        )

        val warningIconMapping: HashMap<String, Int> = hashMapOf(
            "Yellow" to R.drawable.alert_yellow,
            "Orange" to R.drawable.alert_orange,
            "Red" to R.drawable.alert_red
        )
    }

    fun getPictogram(): Int {
        return iconMapping[code] ?: R.drawable.example_battery
    }
}

class DailyForecast(
    private val date: LocalDateTime,
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

class Warning(
    val id: Int,
    val intensity: String,
    val type: String,
    val description: String,
)

class DisplayInfo() {
    var city: String = ""
    // Today
    private var hourlyForecasts: List<HourlyForecast> = emptyList()
    // Tomorrow onwards
    var dailyForecasts: List<DailyForecast> = emptyList()

    private val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
    private var lastUpdated: LocalDateTime = LocalDateTime(1972, 1, 1, 0, 0)
    private var lastDownloaded: LocalDateTime = LocalDateTime(1972, 1, 1, 0, 0)

    var warnings: List<Warning> = emptyList()

    constructor(cityForecastData: CityForecastData?) : this() {
        if (cityForecastData != null) {
            lastUpdated = stringToDatetime(cityForecastData.last_updated)
            lastDownloaded = stringToDatetime(cityForecastData.last_downloaded)
            city = cityForecastData.city
            // TODO: I should get the ids dynamically
            hourlyForecasts = cityForecastData.hourly_forecast.map { e ->
                HourlyForecast(
                    e.vals[1].roundToInt(),
                    e.vals[2].roundToInt(),
                    WeatherPictogram(e.vals[0].toInt())
                )
            }
            dailyForecasts = cityForecastData.daily_forecast.map { e ->
                DailyForecast(
                    stringToDatetime(e.time.toString()),
                    e.vals[4].toInt(),
                    e.vals[5].toInt(),
                    e.vals[3].roundToInt(),
                    e.vals[2].roundToInt(),
                    WeatherPictogram(e.vals[7].toInt()),
                    WeatherPictogram(e.vals[6].toInt())
                )
            }

            warnings = cityForecastData.warnings.map { e ->
                Warning(
                    e.id,
                    e.intensity[1],
                    e.type[0],
                    e.description[0]
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
            return hourlyForecasts[0]
        }
        return HourlyForecast(0, 0, WeatherPictogram(0))
    }

    fun getLastUpdated(): String {
        return format.format(lastUpdated)
    }

    fun getLastDownloaded(): String {
        return format.format(lastDownloaded)
    }
}