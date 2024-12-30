package lv.kristapsbe.meteo_android

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import lv.kristapsbe.meteo_android.LangStrings.Companion.getDirectionString
import lv.kristapsbe.meteo_android.LangStrings.Companion.getShortenedDayString
import lv.kristapsbe.meteo_android.MainActivity.Companion.AURORA_NOTIFICATION_THRESHOLD
import lv.kristapsbe.meteo_android.MainActivity.Companion.CELSIUS
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_EN
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_LV
import lv.kristapsbe.meteo_android.MainActivity.Companion.convertFromCtoDisplayTemp
import lv.kristapsbe.meteo_android.SunriseSunsetUtils.Companion.calculate
import lv.kristapsbe.meteo_android.IconMapping.Companion.rainCodes
import lv.kristapsbe.meteo_android.IconMapping.Companion.iconMapping
import lv.kristapsbe.meteo_android.IconMapping.Companion.alternateIconMapping
import lv.kristapsbe.meteo_android.IconMapping.Companion.alternateAnimatedIconMapping
import lv.kristapsbe.meteo_android.MainActivity.Companion.DEFAULT_LAT
import lv.kristapsbe.meteo_android.MainActivity.Companion.DEFAULT_LON
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.roundToInt


class WeatherPictogram(
    val code: Int
) {
    fun getPictogram(): Int {
        return iconMapping[code] ?: R.drawable.unknown
    }

    private fun getPictogram(currH: Int, riseH: Int, setH: Int): Int {
        return iconMapping[code.mod(1000) + (if (currH in (riseH + 1)..setH) 1000 else 2000)] ?: R.drawable.unknown
    }

    fun getPictogram(t: LocalDateTime, sunTimes: SunRiseSunSet): Int {
        return getPictogram(t.hour, sunTimes.riseH, sunTimes.setH)
    }

    fun getAlternatePictogram(): Int {
        return alternateIconMapping[code] ?: R.raw.not_available
    }

    private fun getAlternatePictogram(currH: Int, riseH: Int, setH: Int): Int {
        return alternateIconMapping[code.mod(1000) + (if (currH in (riseH + 1)..setH) 1000 else 2000)] ?: R.raw.not_available
    }

    fun getAlternatePictogram(t: LocalDateTime, sunTimes: SunRiseSunSet): Int {
        return getAlternatePictogram(t.hour, sunTimes.riseH, sunTimes.setH)
    }

    fun getAlternateAnimatedPictogram(): Int {
        return alternateAnimatedIconMapping[code] ?: R.raw.not_available
    }

    private fun getAlternateAnimatedPictogram(currH: Int, riseH: Int, setH: Int): Int {
        return alternateAnimatedIconMapping[code.mod(1000) + (if (currH in (riseH + 1)..setH) 1000 else 2000)] ?: R.raw.not_available
    }

    fun getAlternateAnimatedPictogram(t: LocalDateTime, sunTimes: SunRiseSunSet): Int {
        return getAlternateAnimatedPictogram(t.hour, sunTimes.riseH, sunTimes.setH)
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
    val stormProb: Int,
    val windSpeed: Int,
    private val windDirection: Int,
    val currentTemp: Int,
    val feelsLikeTemp: Int,
    val uvIndex: Int,
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
    val ids: List<Int>,
    val intensity: String,
    val type: HashMap<String, String>,
    private val description: HashMap<String, List<String>>,
) {
    fun getFullDescription(lang: String): String {
        return description[lang]?.joinToString(separator = "\n\n") ?: ""
    }
}

class Aurora(
    val prob: Int,
    val time: String
)

class DisplayInfo() {
    companion object {
        fun updateWidget(context: Context, displayInfo: DisplayInfo) {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val prefs = AppPreferences(context)

            val currentLocale: Locale = Locale.getDefault()
            val language: String = currentLocale.language

            val lang = prefs.getString(Preference.LANG, if (language == LANG_LV) LANG_LV else LANG_EN)
            val selectedTemp = prefs.getString(Preference.TEMP_UNIT, CELSIUS)
            val useAltLayout = prefs.getBoolean(Preference.USE_ALT_LAYOUT, false)
            val doShowWidgetBackground = prefs.getBoolean(Preference.DO_SHOW_WIDGET_BACKGROUND, true)
            val doShowAurora = prefs.getBoolean(Preference.DO_SHOW_AURORA, true)
            val doFixIconDayNight = prefs.getBoolean(Preference.DO_FIX_ICON_DAY_NIGHT, true)
            val useAnimatedIcons = prefs.getBoolean(Preference.USE_ANIMATED_ICONS, false)

            // Retrieve the widget IDs
            val widget = ComponentName(context, ForecastWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widget)

            // Create an intent to update the widget
            val intent = Intent(context, ForecastWidget::class.java)
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)

            intent.putExtra("widget_text", convertFromCtoDisplayTemp(displayInfo.getTodayForecast().currentTemp, selectedTemp))
            intent.putExtra("widget_location", displayInfo.city)
            intent.putExtra("widget_feelslike", "${LangStrings.getTranslationString(lang, Translation.FEELS_LIKE)} ${convertFromCtoDisplayTemp(displayInfo.getTodayForecast().feelsLikeTemp, selectedTemp)}")

            intent.putExtra("do_show_widget_background", doShowWidgetBackground)
            if (doFixIconDayNight) {
                val sunTimes: SunRiseSunSet = calculate(
                    displayInfo.getTodayForecast().date,
                    prefs.getFloat(Preference.LAST_LAT, DEFAULT_LAT).toDouble(),
                    prefs.getFloat(Preference.LAST_LON, DEFAULT_LON).toDouble(),
                    ZonedDateTime.now().offset.totalSeconds / 3600
                )

                intent.putExtra("icon_image", if (useAnimatedIcons) displayInfo.getTodayForecast().pictogram.getAlternatePictogram(displayInfo.getTodayForecast().date, sunTimes) else displayInfo.getTodayForecast().pictogram.getPictogram(displayInfo.getTodayForecast().date, sunTimes))
            } else {
                intent.putExtra("icon_image", if (useAnimatedIcons) displayInfo.getTodayForecast().pictogram.getAlternatePictogram() else displayInfo.getTodayForecast().pictogram.getPictogram())
            }
            intent.putExtra("warning_red", displayInfo.warnings.any { it.intensity == "Red" })
            intent.putExtra("warning_orange", displayInfo.warnings.any { it.intensity == "Orange" })
            intent.putExtra("warning_yellow", displayInfo.warnings.any { it.intensity == "Yellow" })
            intent.putExtra("rain_image", displayInfo.getRainIconId(useAnimatedIcons))
            intent.putExtra("rain", displayInfo.getWhenRainExpected(lang))
            intent.putExtra("uv_index", displayInfo.getTodayForecast().uvIndex.toString())
            intent.putExtra("do_show_aurora", (doShowAurora && (displayInfo.aurora.prob >= AURORA_NOTIFICATION_THRESHOLD)))
            intent.putExtra("do_show_uv", (displayInfo.getTodayForecast().uvIndex > 0))
            intent.putExtra("aurora", "${displayInfo.aurora.prob}% (${displayInfo.aurora.time})")
            intent.putExtra("use_alt_layout", useAltLayout)

            context.sendBroadcast(intent)
        }
    }

    var city: String = ""
    var lat: Double = 0.0
    var lon: Double = 0.0
    // Today
    private var hourlyForecasts: List<HourlyForecast> = emptyList()
    // Tomorrow onwards
    var dailyForecasts: List<DailyForecast> = emptyList()

    private val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
    private var lastUpdated: LocalDateTime = LocalDateTime(1970, 1, 1, 0, 0)
    private var lastDownloaded: LocalDateTime = LocalDateTime(1970, 1, 1, 0, 0)
    private var lastDownloadedNoSkip: LocalDateTime = LocalDateTime(1970, 1, 1, 0, 0)

    var warnings: List<Warning> = emptyList()
    var aurora: Aurora = Aurora(0, "")

    constructor(cityForecastData: CityForecastData?) : this() {
        if (cityForecastData != null) {
            lastUpdated = stringToDatetime(cityForecastData.last_updated)
            lastDownloaded = stringToDatetime(cityForecastData.last_downloaded)
            lastDownloadedNoSkip = stringToDatetime(cityForecastData.last_downloaded_no_skip)
            city = cityForecastData.city
            lat = cityForecastData.lat
            lon = cityForecastData.lon
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
                    e.vals[7].roundToInt(),
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
                    e.ids,
                    e.intensity[1],
                    hashMapOf(
                        LANG_LV to e.type[0],
                        LANG_EN to e.type[1]
                    ),
                    hashMapOf(
                        LANG_LV to e.description_lv,
                        LANG_EN to e.description_en
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
        return HourlyForecast(LocalDateTime(1970, 1, 1, 0, 0),"", 0, 0, 0, 0, 0, 0, 0, WeatherPictogram(0))
    }

    // TODO: can this be merged into getWhenRainExpected (?)
    fun getRainIconId(useAlternativeIcon: Boolean): Int {
        val hForecasts = getHourlyForecasts()
        val hourlyRain = hForecasts.filter { it.rainAmount > 0 || rainCodes.contains(it.pictogram.code) }
        if (hourlyRain.isNotEmpty()) {
            return if (useAlternativeIcon) hourlyRain[0].pictogram.getAlternatePictogram() else hourlyRain[0].pictogram.getPictogram()
        }
        return -1
    }

    fun getWhenRainExpected(lang: String): String {
        val hForecasts = getHourlyForecasts()
        val hourlyRain = hForecasts.filter { it.rainAmount > 0 || rainCodes.contains(it.pictogram.code) }
        if (hourlyRain.isNotEmpty() && hourlyRain[0].date != hForecasts[0].date) {
            val dt = convertTimestampToLocalDateTime(System.currentTimeMillis())
            return if (hourlyRain[0].date.dayOfMonth == dt.date.dayOfMonth) {
                "${LangStrings.getTranslationString(lang, Translation.RAIN_EXPECTED_TODAY)} ${hourlyRain[0].date.hour}:00"
            } else {
                "${LangStrings.getTranslationString(lang, Translation.RAIN_EXPECTED_TOMORROW)} ${hourlyRain[0].date.hour}:00"
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

    fun getLastDownloadedNoSkip(): String {
        return format.format(lastDownloadedNoSkip)
    }
}