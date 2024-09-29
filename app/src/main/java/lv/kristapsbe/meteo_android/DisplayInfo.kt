package lv.kristapsbe.meteo_android

import android.app.Activity.MODE_PRIVATE
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.loadStringFromStorage
import lv.kristapsbe.meteo_android.LangStrings.Companion.getDirectionString
import lv.kristapsbe.meteo_android.LangStrings.Companion.getShortenedDayString
import lv.kristapsbe.meteo_android.MainActivity.Companion.AURORA_NOTIFICATION_THRESHOLD
import lv.kristapsbe.meteo_android.MainActivity.Companion.DO_ALWAYS_SHOW_AURORA
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_EN
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_LV
import lv.kristapsbe.meteo_android.MainActivity.Companion.SELECTED_LANG
import lv.kristapsbe.meteo_android.MainActivity.Companion.SELECTED_TEMP_FILE
import lv.kristapsbe.meteo_android.MainActivity.Companion.USE_ALT_LAYOUT
import lv.kristapsbe.meteo_android.MainActivity.Companion.WIDGET_TRANSPARENT
import lv.kristapsbe.meteo_android.MainActivity.Companion.convertFromCtoDisplayTemp
import lv.kristapsbe.meteo_android.WeatherPictogram.Companion.rainPictograms
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

        val rainPictograms: List<Int> = listOf(
            R.drawable.tshower, R.drawable.tshower0, R.drawable.tshower1,
            R.drawable.shower, R.drawable.shower0, R.drawable.shower1,
            R.drawable.rain, R.drawable.rain0, R.drawable.rain1,
        )

        // TODO: make warning specific icons
        val warningIconMapping: HashMap<String, Int> = hashMapOf(
            "Yellow" to R.drawable.baseline_warning_yellow_24,
            "Orange" to R.drawable.baseline_warning_orange_24,
            "Red" to R.drawable.baseline_warning_24_red
        )
    }

    fun getPictogram(): Int {
        return iconMapping[code] ?: R.drawable.clear1
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
    fun getDayOfWeek(lang: String): String {
        return getShortenedDayString(lang, date.dayOfWeek.toString())
    }
}

class HourlyForecast(
    val date: LocalDateTime,
    val time: String,
    val rainAmount: Int,
    val rainProb: Int,
    val windSpeed: Int,
    private val windDirection: Int,
    val currentTemp: Int,
    val feelsLikeTemp: Int,
    val pictogram: WeatherPictogram
) {
    fun getDayOfWeek(): String {
        return date.dayOfWeek.toString()
    }

    fun getDirection(lang: String): String {
        return getDirectionString(lang, windDirection/10)
    }
}

class Warning(
    val id: Int,
    val intensity: String,
    val type: HashMap<String, String>,
    val description: HashMap<String, String>,
)

class Aurora(
    val prob: Int,
    val time: String
)

class DisplayInfo() {
    companion object {
        fun updateWidget(context: Context, displayInfo: DisplayInfo) {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val prefs = AppPreferences(context)

            val lang = loadStringFromStorage(context, SELECTED_LANG)
            val selectedTemp = loadStringFromStorage(context, SELECTED_TEMP_FILE)
            val useAltLayout = loadStringFromStorage(context, USE_ALT_LAYOUT)
            val isWidgetTransparent = loadStringFromStorage(context, WIDGET_TRANSPARENT)
            val doAlwaysShowAurora = loadStringFromStorage(context, DO_ALWAYS_SHOW_AURORA)

            // Retrieve the widget IDs
            val widget = ComponentName(context, ForecastWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widget)

            // Create an intent to update the widget
            val intent = Intent(context, ForecastWidget::class.java)
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)

            intent.putExtra("widget_text", convertFromCtoDisplayTemp(displayInfo.getTodayForecast().currentTemp, selectedTemp))
            intent.putExtra("widget_location", displayInfo.city)
            intent.putExtra("widget_feelslike", "${LangStrings.getTranslationString(lang, Translations.FEELS_LIKE)} ${convertFromCtoDisplayTemp(displayInfo.getTodayForecast().feelsLikeTemp, selectedTemp)}")

            intent.putExtra("is_widget_transparent", (isWidgetTransparent != ""))
            intent.putExtra("icon_image", displayInfo.getTodayForecast().pictogram.getPictogram())
            intent.putExtra("warning_red", displayInfo.warnings.any { it.intensity == "Red" })
            intent.putExtra("warning_orange", displayInfo.warnings.any { it.intensity == "Orange" })
            intent.putExtra("warning_yellow", displayInfo.warnings.any { it.intensity == "Yellow" })
            intent.putExtra("rain", displayInfo.getWhenRainExpected(lang))
            intent.putExtra("use_alt_layout", (useAltLayout != ""))

            if (doAlwaysShowAurora != "" || displayInfo.aurora.prob > AURORA_NOTIFICATION_THRESHOLD) {
                if (lang == LANG_EN) {
                    intent.putExtra("aurora", "Aurora ${displayInfo.aurora.prob}% at ${displayInfo.aurora.time}")
                } else {
                    intent.putExtra("aurora", "Ziemeļblāzma ${displayInfo.aurora.prob}% plkst. ${displayInfo.aurora.time}")
                }
            } else {
                intent.putExtra("aurora", "")
            }

            context.sendBroadcast(intent)
        }
    }

    var city: String = ""
    // Today
    private var hourlyForecasts: List<HourlyForecast> = emptyList()
    // Tomorrow onwards
    var dailyForecasts: List<DailyForecast> = emptyList()

    private val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
    private var lastUpdated: LocalDateTime = LocalDateTime(1972, 1, 1, 0, 0)
    private var lastDownloaded: LocalDateTime = LocalDateTime(1972, 1, 1, 0, 0)

    var warnings: List<Warning> = emptyList()
    var aurora: Aurora = Aurora(0, "")

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
                    hashMapOf(
                        LANG_LV to e.type[0],
                        LANG_EN to e.type[1]
                    ),
                    hashMapOf(
                        LANG_LV to e.description[0],
                        LANG_EN to e.description[1]
                    )
                )
            }

            aurora = Aurora(
                cityForecastData.aurora_probs.prob,
                "${cityForecastData.aurora_probs.time.takeLast(4).take(2)}:${cityForecastData.aurora_probs.time.takeLast(2)}"
            )
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

    private fun convertTimestampToLocalDateTime(timestampMillis: Long): LocalDateTime {
        val instant = Instant.fromEpochMilliseconds(timestampMillis)
        val timeZone = TimeZone.currentSystemDefault()
        val localDateTime = instant.toLocalDateTime(timeZone)

        return localDateTime
    }

    fun getHourlyForecasts(): List<HourlyForecast> {
        val dt = convertTimestampToLocalDateTime(System.currentTimeMillis())
        return hourlyForecasts.filter { it.date >= dt }
    }

    fun getTodayForecast(): HourlyForecast {
        val currHourlyForecasts = getHourlyForecasts()
        if (currHourlyForecasts.isNotEmpty()) {
            return currHourlyForecasts[0]
        }
        return HourlyForecast(LocalDateTime(1972, 1, 1, 0, 0),"", 0, 0, 0, 0, 0, 0, WeatherPictogram(0))
    }

    fun getWhenRainExpected(lang: String): String {
        val hForecasts = getHourlyForecasts()
        val hourlyRain = hForecasts.filter { it.rainAmount > 0 || rainPictograms.contains(it.pictogram.getPictogram()) }
        if (hourlyRain.isNotEmpty() && hourlyRain[0].date != hForecasts[0].date) {
            return if (hourlyRain[0].date.dayOfMonth == getTodayForecast().date.dayOfMonth) {
                "${LangStrings.getTranslationString(lang, Translations.RAIN_EXPECTED_TODAY)} ${hourlyRain[0].date.hour}:00"
            } else {
                "${LangStrings.getTranslationString(lang, Translations.RAIN_EXPECTED_TOMORROW)} ${hourlyRain[0].date.hour}:00"
            }
        }
        return ""
    }

    fun getLastUpdated(): String {
        return format.format(lastUpdated)
    }

    fun getLastDownloaded(): String {
        return format.format(lastDownloaded)
    }
}