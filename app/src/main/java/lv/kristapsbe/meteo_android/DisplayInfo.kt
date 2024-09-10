package lv.kristapsbe.meteo_android

import lv.kristapsbe.meteo_android.R
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

            1303 to R.drawable.cloud_lightning_sun,

            1504 to R.drawable.cloud_rain,
            1506 to R.drawable.cloud_rain_sun,

            2101 to R.drawable.moon,
            2102 to R.drawable.cloud_moon,
            2103 to R.drawable.cloud_moon,
            2104 to R.drawable.cloud,
            2105 to R.drawable.cloud, // TODO: should be a cloud with a second cloud in the background

            2303 to R.drawable.cloud_lightning_moon,

            2504 to R.drawable.cloud_rain,
            2506 to R.drawable.cloud_rain_moon
        )
    }

    fun getPictogram(): Int {
        return iconMapping[code] ?: R.drawable.example_battery
    }
}

class DailyForecast(
    private val date: LocalDateTime,
    val locationId: String,
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
    val locationId: String,
    val pictogram: WeatherPictogram
)

class Location(
    val id: String,
    val name: String
)

class DisplayInfo() {
    var location: Location = Location("", "")
    // Today
    private var hourlyForecasts: List<HourlyForecast> = emptyList()
    // Tomorrow onwards
    private var dailyForecasts: List<DailyForecast> = emptyList()

    private val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
    private var lastUpdated: LocalDateTime = LocalDateTime(1972, 1, 1, 0, 0)
    private var lastDownloaded: LocalDateTime = LocalDateTime(1972, 1, 1, 0, 0)

    constructor(cityForecastData: CityForecastData?) : this() {
        if (cityForecastData != null) {
            lastUpdated = stringToDatetime(cityForecastData.last_updated)
            lastDownloaded = stringToDatetime(cityForecastData.last_downloaded)
            location = Location(cityForecastData.cities[0].id, cityForecastData.cities[0].name)
            // TODO: I should get the ids dynamically
            hourlyForecasts = cityForecastData.hourly_forecast.map { e ->
                HourlyForecast(
                    e.vals[1].roundToInt(),
                    e.vals[2].roundToInt(),
                    e.id,
                    WeatherPictogram(e.vals[0].toInt())
                )
            }
            dailyForecasts = cityForecastData.daily_forecast.map { e ->
                DailyForecast(
                    stringToDatetime(e.time.toString()),
                    e.id,
                    e.vals[4].toInt(),
                    e.vals[5].toInt(),
                    e.vals[3].roundToInt(),
                    e.vals[2].roundToInt(),
                    WeatherPictogram(e.vals[7].toInt()),
                    WeatherPictogram(e.vals[6].toInt())
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
            return hourlyForecasts.filter { it.locationId == location.id }[0]
        }
        return HourlyForecast(0, 0, "", WeatherPictogram(0))
    }

    fun getCurrentDailyForecasts(): List<DailyForecast> {
        // TODO - filter based on the closest (and largest) town
        return dailyForecasts.filter { it.locationId == getTodayForecast().locationId }
    }

    fun getLastUpdated(): String {
        return format.format(lastUpdated)
    }

    fun getLastDownloaded(): String {
        return format.format(lastDownloaded)
    }
}