package lv.kristapsbe.meteo_android

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.byUnicodePattern
import kotlin.math.roundToInt


class WeatherPictogram(
    private val code: Int
) {
    companion object {
        val iconMapping: HashMap<Int, Int> = hashMapOf(
            1101 to R.drawable.clear,
            1102 to R.drawable.pcloudy,
            1103 to R.drawable.mcloudy1, // single fulled out cloud with sun
            1104 to R.drawable.mcloudy, // single filled out cloud
            1105 to R.drawable.mcloudy, // two filled out clouds

            // 12xx - sleet (?)

            1301 to R.drawable.tshower, // two filled out clouds, with lightning and rain
            1302 to R.drawable.tshower, // single filled out cloud, with lightning and rain
            1303 to R.drawable.tshower1, // single filled out cloud with sun, lightning and rain
            1304 to R.drawable.tshower, // two filled out clouds, with lightning and rain (downpour)
            1305 to R.drawable.tshower, // single filled out cloud, with lightning and rain (downpour)
            1306 to R.drawable.tshower, // single filled out cloud with sun, lightning and rain (downpour)
            1307 to R.drawable.tshower, // two filled out clouds, with lightning and snow or sleet
            1308 to R.drawable.tshower, // single filled out cloud, with lightning and snow or sleet
            1309 to R.drawable.tshower1, // single filled out cloud with sun, lightning and snow or sleet

            1401 to R.drawable.fog, // two clouds with fog
            1402 to R.drawable.fog, // cloud with fog
            1403 to R.drawable.fog1, // single filled out cloud with fog
            1404 to R.drawable.fog1, // single unfilled cloud with fog
            1405 to R.drawable.fog, // two clouds with fog and rain (drizzle)
            1406 to R.drawable.fog, // cloud with fog and rain (drizzle)
            1407 to R.drawable.fog, // two clouds with fog and rain
            1408 to R.drawable.fog1, // two clouds with fog, sun and rain
            1409 to R.drawable.fog, // cloud with fog and snow or sleet

            1501 to R.drawable.shower, // two clouds and drizzle
            1502 to R.drawable.shower, // cloud and drizzle
            1503 to R.drawable.shower1, // cloud, sun and drizzle
            1504 to R.drawable.rain, // two clouds and rain
            1505 to R.drawable.rain, // cloud and rain
            1506 to R.drawable.rain1, // cloud, sun and rain
            1507 to R.drawable.rain, // two clouds and rain (downpour)
            1508 to R.drawable.rain, // cloud and rain (downpour)
            1509 to R.drawable.rain1, // cloud, sun and rain (downpour)

            1601 to R.drawable.lsnow, // two clouds and snow
            1602 to R.drawable.lsnow, // cloud and snow
            1603 to R.drawable.lsnow1, // cloud, sun and snow
            1604 to R.drawable.snow, // two clouds and more snow
            1605 to R.drawable.snow, // cloud and more snow
            1606 to R.drawable.snow1, // cloud, sun and more snow
            1607 to R.drawable.snow, // two clouds, snow and wind
            1608 to R.drawable.snow1, // cloud, sun, snow and wind
            1609 to R.drawable.snow, // cloud, sun, more snow and wind

            2101 to R.drawable.clear0,
            2102 to R.drawable.pcloudy0,
            2103 to R.drawable.mcloudy0,
            2104 to R.drawable.mcloudy,
            2105 to R.drawable.mcloudy,

            // 22xx - sleet (?)

            2301 to R.drawable.tshower, // two filled out clouds, with lightning and rain
            2302 to R.drawable.tshower, // single filled out cloud, with lightning and rain
            2303 to R.drawable.tshower0, // single filled out cloud with moon, lightning and rain
            2304 to R.drawable.tshower, // two filled out clouds, with lightning and rain (downpour)
            2305 to R.drawable.tshower, // single filled out cloud, with lightning and rain (downpour)
            2306 to R.drawable.tshower0, // single filled out cloud with moon, lightning and rain (downpour)
            2307 to R.drawable.tshower, // two filled out clouds, with lightning and snow or sleet
            2308 to R.drawable.tshower, // single filled out cloud, with lightning and snow or sleet
            2309 to R.drawable.tshower0, // single filled out cloud with moon, and lightning

            2401 to R.drawable.fog, // two clouds with fog
            2402 to R.drawable.fog, // cloud with fog
            2403 to R.drawable.fog0, // single filled out cloud with fog
            2404 to R.drawable.fog0, // single unfilled cloud with fog
            2405 to R.drawable.fog, // two clouds with fog and rain (drizzle)
            2406 to R.drawable.fog, // cloud with fog and rain (drizzle)
            2407 to R.drawable.fog, // two clouds with fog and rain
            2408 to R.drawable.fog0, // two clouds with fog, moon and rain
            2409 to R.drawable.fog, // cloud with fog and snow or sleet

            2501 to R.drawable.shower, // two clouds and drizzle
            2502 to R.drawable.shower, // cloud and drizzle
            2503 to R.drawable.shower0, // cloud, moon and drizzle
            2504 to R.drawable.rain, // two clouds and rain
            2505 to R.drawable.rain, // cloud and rain
            2506 to R.drawable.rain0, // cloud, moon and rain
            2507 to R.drawable.rain, // two clouds and rain (downpour)
            2508 to R.drawable.rain, // cloud and rain (downpour)
            2509 to R.drawable.rain0, // cloud, moon and rain (downpour)

            2601 to R.drawable.lsnow, // two clouds and snow
            2602 to R.drawable.lsnow, // cloud and snow
            2603 to R.drawable.lsnow0, // cloud, moon and snow
            2604 to R.drawable.snow, // two clouds and more snow
            2605 to R.drawable.snow, // cloud and more snow
            2606 to R.drawable.snow0, // cloud, moon and more snow
            2607 to R.drawable.snow, // two clouds, snow and wind
            2608 to R.drawable.snow, // cloud, moon, snow and wind
            2609 to R.drawable.snow0, // cloud, sun, more snow and wind
        )

        // TODO: make warning specific icons
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
    val date: LocalDateTime,
    val rainAmount: Int,
    val rainProb: Int,
    val averageWind: Int,
    val maxWind: Int,
    val tempMin: Int,
    val tempMax: Int,
    val pictogramDay: WeatherPictogram,
    val pictogramNight: WeatherPictogram
) {
    companion object {
        val dayMapping = hashMapOf(
            "MONDAY" to "P.",
            "TUESDAY" to "O.",
            "WEDNESDAY" to "T.",
            "THURSDAY" to "C.",
            "FRIDAY" to "Pk.",
            "SATURDAY" to "S.",
            "SUNDAY" to "Sv."
        )
    }

    fun getDayOfWeek(): String {
        val dow = date.dayOfWeek.toString()
        return dayMapping[dow] ?: dow
    }
}

class HourlyForecast(
    private val date: LocalDateTime,
    val time: String,
    val rainAmount: Int,
    val rainProb: Int,
    val windSpeed: Int,
    val windDirection: Int,
    val currentTemp: Int,
    val feelsLikeTemp: Int,
    val pictogram: WeatherPictogram
) {
    companion object {
        //https://uni.edu/storm/Wind%20Direction%20slide.pdf
        var directions = hashMapOf(
            35 to "Z",
            36 to "Z",
            0 to "Z",
            1 to "Z",
            2 to "Z/ZA",
            3 to "Z/ZA",
            4 to "ZA",
            5 to "ZA",
            6 to "A/ZA",
            7 to "A/ZA",
            8 to "A",
            9 to "A",
            10 to "A",
            11 to "A/DA",
            12 to "A/DA",
            13 to "DA",
            14 to "DA",
            15 to "D/DA",
            16 to "D/DA",
            17 to "D",
            18 to "D",
            19 to "D",
            20 to "D/DR",
            21 to "D/DR",
            22 to "DR",
            23 to "DR",
            24 to "R/DR",
            25 to "R/DR",
            26 to "R",
            27 to "R",
            28 to "R",
            29 to "R/ZR",
            30 to "R/ZR",
            31 to "ZR",
            32 to "ZR",
            33 to "Z/ZR",
            34 to "Z/ZR",
        )
    }

    fun getDayOfWeek(): String {
        return date.dayOfWeek.toString()
    }

    fun getDirection(): String {
        return directions[(windDirection/10)] ?: ""
    }
}

class Warning(
    val id: Int,
    val intensity: String,
    val type: String,
    val description: String,
)

class DisplayInfo() {
    var city: String = ""
    // Today
    var hourlyForecasts: List<HourlyForecast> = emptyList()
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
                    stringToDatetime(e.time.toString()),
                    e.time.toString().takeLast(4),
                    e.vals[6].roundToInt(),
                    e.vals[8].roundToInt(),
                    e.vals[3].roundToInt(),
                    e.vals[4].roundToInt(),
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
                    e.vals[0].roundToInt(),
                    e.vals[1].roundToInt(),
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
        return HourlyForecast(LocalDateTime(1972, 1, 1, 0, 0),"", 0, 0, 0, 0, 0, 0, WeatherPictogram(0))
    }

    fun getLastUpdated(): String {
        return format.format(lastUpdated)
    }

    fun getLastDownloaded(): String {
        return format.format(lastDownloaded)
    }
}