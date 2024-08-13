package com.example.meteo_android

import android.util.Log
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.byUnicodePattern


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
    val stormProb: Int,
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

class TodayForecast() {
    val currentTemp: Int = 0
    val feelsLikeTemp: Int = 0
    val pictogram: WeatherPictogram = WeatherPictogram(0)
}

class DisplayInfo() {
    // Today
    var hourlyForecasts: List<HourlyForecast> = emptyList()
    // Tomorrow onwards
    var dailyForecasts: List<DailyForecast> = emptyList()

    private val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
    var lastUpdated: LocalDateTime = LocalDateTime(1972, 1, 1, 0, 0)

    fun updateData(cityForecastData: CityForecastData?): Boolean {
        if (cityForecastData != null) {
            return true
        } else {
            return false
        }
        /*
        val currHForecast = currentInfo.value.hourlyForecast
        var currTempTmp: Double = currHForecast?.currTemp ?: -999.0
        var feelsLikeTmp: Double = currHForecast?.feelsLike ?: -999.0
        var pictogramTmp: Int = currHForecast?.pictogram?.code ?: 0
        if ((cityForecast?.hourly_forecast?.size ?: 0) > 0) {
            val currEntry = cityForecast?.hourly_forecast?.get(0)?.vals
            currTempTmp = currEntry?.get(1) ?: currTempTmp
            feelsLikeTmp = currEntry?.get(2) ?: feelsLikeTmp
            pictogramTmp = currEntry?.get(0)?.toInt() ?: pictogramTmp
        }
        var currDaily = currentInfo.value.dailyForecast
        if ((cityForecast?.daily_forecast?.size ?: 0) > 0) {
            val currEntry = cityForecast?.daily_forecast?.get(0)
            val tmpNewDaily = DailyForecast(
                currEntry?.time.toString(),
                currEntry?.vals?.get(4)?.toInt() ?: -999,
                currEntry?.vals?.get(5) ?: -999.0,
                currEntry?.vals?.get(3) ?: -999.0,
                currEntry?.vals?.get(2) ?: -999.0,
                WeatherPictogram((currEntry?.vals?.get(7) ?: -999).toInt()),
                WeatherPictogram((currEntry?.vals?.get(6) ?: -999).toInt())
            )
            currDaily = tmpNewDaily
        }
        Log.d("DEBUG", "DLOADED --- ${currTempTmp} | ${feelsLikeTmp} | ${pictogramTmp}")

        currentInfo.value = CurrentInfo(
            HourlyForecast(
                currTempTmp, feelsLikeTmp, WeatherPictogram(pictogramTmp)
            ),
            currDaily
        )

        var tmpDayList = dailyInfo.value.dailyForecasts
        if ((cityForecast?.daily_forecast?.size ?: 0) > 0) {
            val tmpNewList = cityForecast?.daily_forecast?.map {
                DailyForecast(
                    it.time.toString(),
                    it.vals.get(4).toInt(),
                    it.vals.get(5),
                    it.vals.get(3),
                    it.vals.get(2),
                    WeatherPictogram(it.vals.get(7).toInt()),
                    WeatherPictogram(it.vals.get(6).toInt())
                )
            } ?: emptyList()
            tmpDayList = tmpNewList
        }
        dailyInfo.value = DailyInfo(tmpDayList)

        var lastUpdatedTmp: LocalDateTime? = metadataInfo.value.lastUpdated
        if (cityForecast?.last_updated != null) {
            lastUpdatedTmp = stringToDateTime(cityForecast?.last_updated!!)
        }
        metadataInfo.value = MetadataInfo(lastUpdatedTmp)
         */
    }

    fun getTodayForecast(): TodayForecast {
        // TODO: fish out most recent relevant info
        return TodayForecast()
    }

    fun getLastUpdated(): String {
        return format.format(lastUpdated)
    }
}