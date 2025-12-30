package lv.kristapsbe.meteo_android.util

import lv.kristapsbe.meteo_android.R

/**
 * Holds all resource IDs for a specific weather condition variant (Day or Night)
 */
data class WeatherResources(
    val standard: Int,
    val alternate: Int,
    val animated: Int,
    val isRainy: Boolean = false
)

class IconMapping {
    companion object {
        /**
         * Maps the condition suffix (the last 3 digits, e.g., 101, 302) to its resource sets
         */
        private val conditionMap = mapOf(
            // Clear / Cloudy
            101 to Pair(
                WeatherResources(R.drawable.clear, R.drawable.clear_day, R.raw.clear_day),
                WeatherResources(R.drawable.clear0, R.drawable.clear_night, R.raw.clear_night)
            ),
            102 to Pair(
                WeatherResources(
                    R.drawable.pcloudy,
                    R.drawable.partly_cloudy_day,
                    R.raw.partly_cloudy_day
                ),
                WeatherResources(
                    R.drawable.pcloudy0,
                    R.drawable.partly_cloudy_night,
                    R.raw.partly_cloudy_night
                )
            ),
            103 to Pair(
                WeatherResources(R.drawable.mcloudy1, R.drawable.overcast_day, R.raw.overcast_day),
                WeatherResources(
                    R.drawable.mcloudy0,
                    R.drawable.overcast_night,
                    R.raw.overcast_night
                )
            ),
            104 to Pair(
                WeatherResources(R.drawable.mcloudy, R.drawable.extreme_day, R.raw.extreme_day),
                WeatherResources(R.drawable.mcloudy, R.drawable.extreme_night, R.raw.extreme_night)
            ),
            105 to Pair(
                WeatherResources(R.drawable.mcloudy, R.drawable.extreme, R.raw.extreme),
                WeatherResources(R.drawable.mcloudy, R.drawable.extreme, R.raw.extreme)
            ),

            // Sleet (Rain + Snow)
            201 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                )
            ),
            202 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                )
            ),
            203 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.partly_cloudy_day_sleet,
                    R.raw.partly_cloudy_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.partly_cloudy_night_sleet,
                    R.raw.partly_cloudy_night_sleet,
                    true
                )
            ),
            // ... (Consolidating similar sleet codes)
            204 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                )
            ),
            205 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                )
            ),
            206 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.partly_cloudy_day_sleet,
                    R.raw.partly_cloudy_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.partly_cloudy_night_sleet,
                    R.raw.partly_cloudy_night_sleet,
                    true
                )
            ),
            207 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                )
            ),
            208 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.partly_cloudy_day_sleet,
                    R.raw.partly_cloudy_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.partly_cloudy_night_sleet,
                    R.raw.partly_cloudy_night_sleet,
                    true
                )
            ),

            // Thunderstorms
            301 to Pair(
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_extreme_rain,
                    R.raw.thunderstorms_extreme_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_extreme_rain,
                    R.raw.thunderstorms_extreme_rain,
                    true
                )
            ),
            302 to Pair(
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_overcast_rain,
                    R.raw.thunderstorms_overcast_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_overcast_rain,
                    R.raw.thunderstorms_overcast_rain,
                    true
                )
            ),
            303 to Pair(
                WeatherResources(
                    R.drawable.tshower1,
                    R.drawable.thunderstorms_day_rain,
                    R.raw.thunderstorms_day_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower0,
                    R.drawable.thunderstorms_night_rain,
                    R.raw.thunderstorms_night_rain,
                    true
                )
            ),
            304 to Pair(
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_extreme_rain,
                    R.raw.thunderstorms_extreme_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_extreme_rain,
                    R.raw.thunderstorms_extreme_rain,
                    true
                )
            ),
            305 to Pair(
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_overcast_rain,
                    R.raw.thunderstorms_overcast_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_overcast_rain,
                    R.raw.thunderstorms_overcast_rain,
                    true
                )
            ),
            306 to Pair(
                WeatherResources(
                    R.drawable.tshower1,
                    R.drawable.thunderstorms_day_rain,
                    R.raw.thunderstorms_day_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower0,
                    R.drawable.thunderstorms_night_rain,
                    R.raw.thunderstorms_night_rain,
                    true
                )
            ),
            307 to Pair(
                WeatherResources(
                    R.drawable.tsnow,
                    R.drawable.thunderstorms_extreme_snow,
                    R.raw.thunderstorms_extreme_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.tsnow,
                    R.drawable.thunderstorms_extreme_snow,
                    R.raw.thunderstorms_extreme_snow,
                    true
                )
            ),
            308 to Pair(
                WeatherResources(
                    R.drawable.tsnow,
                    R.drawable.thunderstorms_overcast_snow,
                    R.raw.thunderstorms_overcast_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.tsnow,
                    R.drawable.thunderstorms_overcast_snow,
                    R.raw.thunderstorms_overcast_snow,
                    true
                )
            ),
            309 to Pair(
                WeatherResources(
                    R.drawable.tsnow1,
                    R.drawable.thunderstorms_day_snow,
                    R.raw.thunderstorms_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.tsnow0,
                    R.drawable.thunderstorms_night_snow,
                    R.raw.thunderstorms_night_snow,
                    true
                )
            ),
            310 to Pair(
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_extreme_rain,
                    R.raw.thunderstorms_extreme_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_extreme_rain,
                    R.raw.thunderstorms_extreme_rain,
                    true
                )
            ),
            311 to Pair(
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_overcast_rain,
                    R.raw.thunderstorms_overcast_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower,
                    R.drawable.thunderstorms_overcast_rain,
                    R.raw.thunderstorms_overcast_rain,
                    true
                )
            ),
            312 to Pair(
                WeatherResources(
                    R.drawable.tshower1,
                    R.drawable.thunderstorms_day_rain,
                    R.raw.thunderstorms_day_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.tshower0,
                    R.drawable.thunderstorms_night_rain,
                    R.raw.thunderstorms_night_rain,
                    true
                )
            ),
            313 to Pair(
                WeatherResources(
                    R.drawable.tsleet,
                    R.drawable.thunderstorms_extreme_snow,
                    R.raw.thunderstorms_extreme_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.tsleet,
                    R.drawable.thunderstorms_extreme_snow,
                    R.raw.thunderstorms_extreme_snow,
                    true
                )
            ),
            314 to Pair(
                WeatherResources(
                    R.drawable.tsleet1,
                    R.drawable.thunderstorms_day_snow,
                    R.raw.thunderstorms_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.tsleet0,
                    R.drawable.thunderstorms_night_snow,
                    R.raw.thunderstorms_night_snow,
                    true
                )
            ),
            315 to Pair(
                WeatherResources(
                    R.drawable.tsleet,
                    R.drawable.thunderstorms_extreme_snow,
                    R.raw.thunderstorms_extreme_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.tsleet,
                    R.drawable.thunderstorms_extreme_snow,
                    R.raw.thunderstorms_extreme_snow,
                    true
                )
            ),
            316 to Pair(
                WeatherResources(
                    R.drawable.tsleet1,
                    R.drawable.thunderstorms_day_extreme_snow,
                    R.raw.thunderstorms_day_extreme_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.tsleet0,
                    R.drawable.thunderstorms_night_extreme_snow,
                    R.raw.thunderstorms_night_extreme_snow,
                    true
                )
            ),
            317 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                )
            ),
            318 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                )
            ),
            319 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.partly_cloudy_day_sleet,
                    R.raw.partly_cloudy_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.partly_cloudy_night_sleet,
                    R.raw.partly_cloudy_night_sleet,
                    true
                )
            ),
            320 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                )
            ),
            321 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                )
            ),
            322 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.partly_cloudy_day_sleet,
                    R.raw.partly_cloudy_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.partly_cloudy_night_sleet,
                    R.raw.partly_cloudy_night_sleet,
                    true
                )
            ),
            323 to Pair(
                WeatherResources(
                    R.drawable.tstorm,
                    R.drawable.thunderstorms_extreme,
                    R.raw.thunderstorms_extreme,
                    true
                ),
                WeatherResources(
                    R.drawable.tstorm,
                    R.drawable.thunderstorms_extreme,
                    R.raw.thunderstorms_extreme,
                    true
                )
            ),
            324 to Pair(
                WeatherResources(
                    R.drawable.tstorm,
                    R.drawable.thunderstorms,
                    R.raw.thunderstorms,
                    true
                ),
                WeatherResources(
                    R.drawable.tstorm,
                    R.drawable.thunderstorms,
                    R.raw.thunderstorms,
                    true
                )
            ),
            325 to Pair(
                WeatherResources(
                    R.drawable.tstorm1,
                    R.drawable.thunderstorms_day,
                    R.raw.thunderstorms_day,
                    true
                ),
                WeatherResources(
                    R.drawable.tstorm0,
                    R.drawable.thunderstorms_night,
                    R.raw.thunderstorms_night,
                    true
                )
            ),

            // Fog
            401 to Pair(
                WeatherResources(R.drawable.fog, R.drawable.extreme_fog, R.raw.extreme_fog),
                WeatherResources(R.drawable.fog, R.drawable.extreme_fog, R.raw.extreme_fog)
            ),
            402 to Pair(
                WeatherResources(
                    R.drawable.fog,
                    R.drawable.overcast_fog,
                    R.raw.overcast_fog
                ), WeatherResources(R.drawable.fog, R.drawable.overcast_fog, R.raw.overcast_fog)
            ),
            403 to Pair(
                WeatherResources(
                    R.drawable.fog1,
                    R.drawable.overcast_fog,
                    R.raw.overcast_fog
                ), WeatherResources(R.drawable.fog0, R.drawable.overcast_fog, R.raw.overcast_fog)
            ),
            404 to Pair(
                WeatherResources(
                    R.drawable.fog1,
                    R.drawable.overcast_fog,
                    R.raw.overcast_fog
                ), WeatherResources(R.drawable.fog0, R.drawable.overcast_fog, R.raw.overcast_fog)
            ),
            405 to Pair(
                WeatherResources(
                    R.drawable.fog,
                    R.drawable.extreme_fog,
                    R.raw.extreme_fog,
                    true
                ), WeatherResources(R.drawable.fog, R.drawable.extreme_fog, R.raw.extreme_fog, true)
            ),
            406 to Pair(
                WeatherResources(
                    R.drawable.fog,
                    R.drawable.overcast_fog,
                    R.raw.overcast_fog,
                    true
                ),
                WeatherResources(R.drawable.fog, R.drawable.overcast_fog, R.raw.overcast_fog, true)
            ),
            407 to Pair(
                WeatherResources(
                    R.drawable.fog,
                    R.drawable.extreme_fog,
                    R.raw.extreme_fog,
                    true
                ), WeatherResources(R.drawable.fog, R.drawable.extreme_fog, R.raw.extreme_fog, true)
            ),
            408 to Pair(
                WeatherResources(
                    R.drawable.fog1,
                    R.drawable.extreme_day_fog,
                    R.raw.extreme_day_fog,
                    true
                ),
                WeatherResources(
                    R.drawable.fog0,
                    R.drawable.extreme_night_fog,
                    R.raw.extreme_night_fog,
                    true
                )
            ),
            409 to Pair(
                WeatherResources(
                    R.drawable.fog,
                    R.drawable.overcast_fog,
                    R.raw.overcast_fog,
                    true
                ),
                WeatherResources(R.drawable.fog, R.drawable.overcast_fog, R.raw.overcast_fog, true)
            ),

            // Snow / Wind
            410 to Pair(
                WeatherResources(
                    R.drawable.lsnow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.lsnow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                )
            ),
            411 to Pair(
                WeatherResources(
                    R.drawable.lsnow1,
                    R.drawable.partly_cloudy_day_snow,
                    R.raw.partly_cloudy_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.lsnow0,
                    R.drawable.partly_cloudy_night_snow,
                    R.raw.partly_cloudy_night_snow,
                    true
                )
            ),
            412 to Pair(
                WeatherResources(
                    R.drawable.lsnow1,
                    R.drawable.overcast_day_snow,
                    R.raw.overcast_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.lsnow0,
                    R.drawable.overcast_night_snow,
                    R.raw.overcast_night_snow,
                    true
                )
            ),
            413 to Pair(
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.extreme_snow,
                    R.raw.extreme_snow,
                    true
                ),
                WeatherResources(R.drawable.snow, R.drawable.extreme_snow, R.raw.extreme_snow, true)
            ),
            414 to Pair(
                WeatherResources(
                    R.drawable.snow1,
                    R.drawable.overcast_day_snow,
                    R.raw.overcast_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.snow0,
                    R.drawable.overcast_night_snow,
                    R.raw.overcast_night_snow,
                    true
                )
            ),
            415 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                )
            ),
            416 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.overcast_day_sleet,
                    R.raw.overcast_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.overcast_night_sleet,
                    R.raw.overcast_night_sleet,
                    true
                )
            ),

            // Rain / Showers
            501 to Pair(
                WeatherResources(
                    R.drawable.shower,
                    R.drawable.extreme_drizzle,
                    R.raw.extreme_drizzle,
                    true
                ),
                WeatherResources(
                    R.drawable.shower,
                    R.drawable.extreme_drizzle,
                    R.raw.extreme_drizzle,
                    true
                )
            ),
            502 to Pair(
                WeatherResources(
                    R.drawable.shower,
                    R.drawable.overcast_drizzle,
                    R.raw.overcast_drizzle,
                    true
                ),
                WeatherResources(
                    R.drawable.shower,
                    R.drawable.overcast_drizzle,
                    R.raw.overcast_drizzle,
                    true
                )
            ),
            503 to Pair(
                WeatherResources(
                    R.drawable.shower1,
                    R.drawable.partly_cloudy_day_drizzle,
                    R.raw.partly_cloudy_day_drizzle,
                    true
                ),
                WeatherResources(
                    R.drawable.shower0,
                    R.drawable.partly_cloudy_night_drizzle,
                    R.raw.partly_cloudy_night_drizzle,
                    true
                )
            ),
            504 to Pair(
                WeatherResources(
                    R.drawable.rain,
                    R.drawable.extreme_rain,
                    R.raw.extreme_rain,
                    true
                ),
                WeatherResources(R.drawable.rain, R.drawable.extreme_rain, R.raw.extreme_rain, true)
            ),
            505 to Pair(
                WeatherResources(
                    R.drawable.rain,
                    R.drawable.overcast_rain,
                    R.raw.overcast_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.rain,
                    R.drawable.overcast_rain,
                    R.raw.overcast_rain,
                    true
                )
            ),
            506 to Pair(
                WeatherResources(
                    R.drawable.rain1,
                    R.drawable.partly_cloudy_day_rain,
                    R.raw.partly_cloudy_day_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.rain0,
                    R.drawable.partly_cloudy_night_rain,
                    R.raw.partly_cloudy_night_rain,
                    true
                )
            ),
            507 to Pair(
                WeatherResources(
                    R.drawable.rain,
                    R.drawable.extreme_rain,
                    R.raw.extreme_rain,
                    true
                ),
                WeatherResources(R.drawable.rain, R.drawable.extreme_rain, R.raw.extreme_rain, true)
            ),
            508 to Pair(
                WeatherResources(
                    R.drawable.rain,
                    R.drawable.overcast_rain,
                    R.raw.overcast_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.rain,
                    R.drawable.overcast_rain,
                    R.raw.overcast_rain,
                    true
                )
            ),
            509 to Pair(
                WeatherResources(
                    R.drawable.rain1,
                    R.drawable.partly_cloudy_day_rain,
                    R.raw.partly_cloudy_day_rain,
                    true
                ),
                WeatherResources(
                    R.drawable.rain0,
                    R.drawable.partly_cloudy_night_rain,
                    R.raw.partly_cloudy_night_rain,
                    true
                )
            ),

            // Snow
            601 to Pair(
                WeatherResources(
                    R.drawable.lsnow,
                    R.drawable.extreme_snow,
                    R.raw.extreme_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.lsnow,
                    R.drawable.extreme_snow,
                    R.raw.extreme_snow,
                    true
                )
            ),
            602 to Pair(
                WeatherResources(
                    R.drawable.lsnow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.lsnow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                )
            ),
            603 to Pair(
                WeatherResources(
                    R.drawable.lsnow1,
                    R.drawable.partly_cloudy_day_snow,
                    R.raw.partly_cloudy_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.lsnow0,
                    R.drawable.partly_cloudy_night_snow,
                    R.raw.partly_cloudy_night_snow,
                    true
                )
            ),
            604 to Pair(
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.extreme_snow,
                    R.raw.extreme_snow,
                    true
                ),
                WeatherResources(R.drawable.snow, R.drawable.extreme_snow, R.raw.extreme_snow, true)
            ),
            605 to Pair(
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                )
            ),
            606 to Pair(
                WeatherResources(
                    R.drawable.snow1,
                    R.drawable.partly_cloudy_day_snow,
                    R.raw.partly_cloudy_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.snow0,
                    R.drawable.partly_cloudy_night_snow,
                    R.raw.partly_cloudy_night_snow,
                    true
                )
            ),
            607 to Pair(
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.extreme_snow,
                    R.raw.extreme_snow,
                    true
                ),
                WeatherResources(R.drawable.snow, R.drawable.extreme_snow, R.raw.extreme_snow, true)
            ),
            608 to Pair(
                WeatherResources(
                    R.drawable.snow1,
                    R.drawable.overcast_day_snow,
                    R.raw.overcast_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.snow0,
                    R.drawable.overcast_night_snow,
                    R.raw.overcast_night_snow,
                    true
                )
            ),
            609 to Pair(
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                )
            ),
            610 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.partly_cloudy_day_sleet,
                    R.raw.partly_cloudy_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.partly_cloudy_night_sleet,
                    R.raw.partly_cloudy_night_sleet,
                    true
                )
            ),
            611 to Pair(
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.extreme_snow,
                    R.raw.extreme_snow,
                    true
                ),
                WeatherResources(R.drawable.snow, R.drawable.extreme_snow, R.raw.extreme_snow, true)
            ),
            612 to Pair(
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.snow,
                    R.drawable.overcast_snow,
                    R.raw.overcast_snow,
                    true
                )
            ),
            613 to Pair(
                WeatherResources(
                    R.drawable.snow1,
                    R.drawable.partly_cloudy_day_snow,
                    R.raw.partly_cloudy_day_snow,
                    true
                ),
                WeatherResources(
                    R.drawable.snow0,
                    R.drawable.partly_cloudy_night_snow,
                    R.raw.partly_cloudy_night_snow,
                    true
                )
            ),
            614 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                )
            ),
            615 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                )
            ),
            616 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.partly_cloudy_day_sleet,
                    R.raw.partly_cloudy_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.partly_cloudy_night_sleet,
                    R.raw.partly_cloudy_night_sleet,
                    true
                )
            ),
            617 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.extreme_sleet,
                    R.raw.extreme_sleet,
                    true
                )
            ),
            618 to Pair(
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet,
                    R.drawable.overcast_sleet,
                    R.raw.overcast_sleet,
                    true
                )
            ),
            619 to Pair(
                WeatherResources(
                    R.drawable.sleet1,
                    R.drawable.partly_cloudy_day_sleet,
                    R.raw.partly_cloudy_day_sleet,
                    true
                ),
                WeatherResources(
                    R.drawable.sleet0,
                    R.drawable.partly_cloudy_night_sleet,
                    R.raw.partly_cloudy_night_sleet,
                    true
                )
            ),
        )

        private fun getResources(code: Int): WeatherResources {
            val type = code % 1000
            val isNight = code / 1000 == 2
            val pair = conditionMap[type] ?: Pair(
                WeatherResources(R.drawable.unknown, R.raw.not_available, R.raw.not_available),
                WeatherResources(R.drawable.unknown, R.raw.not_available, R.raw.not_available)
            )
            return if (isNight) pair.second else pair.first
        }

        // --- Compatibility Getters for existing code ---

        val iconMapping = object : HashMap<Int, Int>() {
            override fun get(key: Int): Int = getResources(key).standard
        }

        val alternateIconMapping = object : HashMap<Int, Int>() {
            override fun get(key: Int): Int = getResources(key).alternate
        }

        val alternateAnimatedIconMapping = object : HashMap<Int, Int>() {
            override fun get(key: Int): Int = getResources(key).animated
        }

        val rainCodes: List<Int> by lazy {
            // Automatically calculate rain codes from the map
            val codes = mutableListOf<Int>()
            conditionMap.forEach { (type, pair) ->
                if (pair.first.isRainy) codes.add(1000 + type)
                if (pair.second.isRainy) codes.add(2000 + type)
            }
            codes
        }

        val warningIconMapping: HashMap<String, Int> = hashMapOf(
            "Yellow" to R.drawable.baseline_warning_yellow_24,
            "Orange" to R.drawable.baseline_warning_orange_24,
            "Red" to R.drawable.baseline_warning_24_red
        )
    }
}
