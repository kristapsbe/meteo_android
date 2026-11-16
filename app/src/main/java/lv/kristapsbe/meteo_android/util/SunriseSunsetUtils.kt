package lv.kristapsbe.meteo_android.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan


class SunRiseSunSet(val riseH: Int, val riseMin: String, val setH: Int, val setMin: String)

// https://gml.noaa.gov/grad/solcalc/main.js
class SunriseSunsetUtils {
    companion object {
        private fun calcTimeJulianCent(jd: Double): Double {
            return (jd - 2451545.0) / 36525.0
        }

        private fun radToDeg(angleRad: Double): Double {
            return (180.0 * angleRad / PI);
        }

        private fun degToRad(angleDeg: Double): Double {
            return (PI * angleDeg / 180.0);
        }

        private fun calculateJulianDay(t: LocalDateTime): Double {
            var year = t.year
            var month = t.month.number // 1..12 (Month.ordinal is 0-based)
            if (month <= 2) {
                year -= 1
                month += 12
            }
            val a = floor(((1.0f) * year) / 100)
            val b = 2 - a + floor(a / 4)
            return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + t.day + b - 1524.5
        }

        private fun calcMeanObliquityOfEcliptic(t: Double): Double {
            val seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * (0.001813)))
            return 23.0 + (26.0 + (seconds / 60.0)) / 60.0
        }

        private fun calcObliquityCorrection(t: Double): Double {
            val e0 = calcMeanObliquityOfEcliptic(t)
            val omega = 125.04 - 1934.136 * t
            return e0 + 0.00256 * cos(degToRad(omega))
        }

        private fun calcGeomMeanLongSun(t: Double): Double {
            var l0 = 280.46646 + t * (36000.76983 + t * (0.0003032))
            while (l0 > 360.0) {
                l0 -= 360.0
            }
            while (l0 < 0.0) {
                l0 += 360.0
            }
            return l0
        }

        private fun calcEccentricityEarthOrbit(t: Double): Double {
            return 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        }

        private fun calcGeomMeanAnomalySun(t: Double): Double {
            return 357.52911 + t * (35999.05029 - 0.0001537 * t)
        }

        private fun calcEquationOfTime(t: Double): Double {
            val epsilon = calcObliquityCorrection(t)
            val l0 = calcGeomMeanLongSun(t)
            val e = calcEccentricityEarthOrbit(t)
            val m = calcGeomMeanAnomalySun(t)
            val y = tan(degToRad(epsilon) / 2.0).pow(2)
            val sin2l0 = sin(2.0 * degToRad(l0))
            val sinm = sin(degToRad(m))
            val cos2l0 = cos(2.0 * degToRad(l0))
            val sin4l0 = sin(4.0 * degToRad(l0))
            val sin2m = sin(2.0 * degToRad(m))

            val eTime =
                y * sin2l0 - 2.0 * e * sinm + 4.0 * e * y * sinm * cos2l0 - 0.5 * y * y * sin4l0 - 1.25 * e * e * sin2m
            return radToDeg(eTime) * 4.0
        }

        private fun calcSunEqOfCenter(t: Double): Double {
            val mRad = degToRad(calcGeomMeanAnomalySun(t))
            return sin(mRad) * (1.914602 - t * (0.004817 + 0.000014 * t)) + sin(2 * mRad) * (0.019993 - 0.000101 * t) + sin(
                3 * mRad
            ) * 0.000289
        }

        private fun calcSunTrueLong(t: Double): Double {
            return calcGeomMeanLongSun(t) + calcSunEqOfCenter(t)
        }

        private fun calcSunApparentLong(t: Double): Double {
            val o = calcSunTrueLong(t)
            val omega = 125.04 - 1934.136 * t
            return o - 0.00569 - 0.00478 * sin(degToRad(omega))
        }

        private fun calcSunDeclination(t: Double): Double {
            return radToDeg(
                asin(
                    sin(degToRad(calcObliquityCorrection(t))) * sin(
                        degToRad(
                            calcSunApparentLong(t)
                        )
                    )
                )
            )
        }

        private fun calcHourAngleSunrise(lat: Double, solarDec: Double): Double {
            val latRad = degToRad(lat)
            val sdRad = degToRad(solarDec)
            return acos(cos(degToRad(90.833)) / (cos(latRad) * cos(sdRad)) - tan(latRad) * tan(sdRad))
        }

        private fun calcSunriseSetUTC(
            rise: Boolean,
            julianDay: Double,
            lat: Double,
            lon: Double
        ): Double {
            val t = calcTimeJulianCent(julianDay)
            val eqTime = calcEquationOfTime(t)
            val solarDec = calcSunDeclination(t)
            var hourAngle = calcHourAngleSunrise(lat, solarDec)
            if (!rise) hourAngle = -hourAngle
            val delta = lon + radToDeg(hourAngle)

            return 720 - (4.0 * delta) - eqTime
        }

        private fun calcSunriseSet(
            rise: Boolean,
            julianDay: Double,
            lat: Double,
            lon: Double,
            offsetMinutes: Double
        ): Double {
            val timeUTC = calcSunriseSetUTC(rise, julianDay, lat, lon)
            val newTimeUTC = calcSunriseSetUTC(rise, julianDay + timeUTC / 1440.0, lat, lon)

            return newTimeUTC + offsetMinutes
        }

        fun calculate(
            t: LocalDateTime,
            lat: Double,
            lon: Double,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): SunRiseSunSet {
            // Use the UTC offset in effect on the requested date (not today's), so days on the
            // other side of a DST change are shifted correctly.
            val offsetMinutes = zoneId.rules.getOffset(
                java.time.LocalDateTime.of(t.year, t.month.number, t.day, 12, 0)
            ).totalSeconds / 60.0

            val julianDay = calculateJulianDay(t)
            val rise = calcSunriseSet(true, julianDay, lat, lon, offsetMinutes).roundToInt()
            val set = calcSunriseSet(false, julianDay, lat, lon, offsetMinutes).roundToInt()

            return SunRiseSunSet(
                rise.floorDiv(60),
                rise.mod(60).toString().padStart(2, '0'),
                set.floorDiv(60),
                set.mod(60).toString().padStart(2, '0'),
            )
        }
    }
}