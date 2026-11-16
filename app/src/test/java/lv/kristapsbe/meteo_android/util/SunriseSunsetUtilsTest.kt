package lv.kristapsbe.meteo_android.util

import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import kotlin.math.abs

class SunriseSunsetUtilsTest {
    private val rigaLat = 56.9496
    private val rigaLon = 24.1052
    private val riga = ZoneId.of("Europe/Riga")

    private fun assertSunTimes(
        date: LocalDateTime,
        expectedRise: String,
        expectedSet: String
    ) {
        val sun = SunriseSunsetUtils.calculate(date, rigaLat, rigaLon, riga)
        assertWithinAMinute("$date sunrise", expectedRise, sun.riseH, sun.riseMin)
        assertWithinAMinute("$date sunset", expectedSet, sun.setH, sun.setMin)
    }

    private fun assertWithinAMinute(label: String, expected: String, h: Int, min: String) {
        val (eh, em) = expected.split(":").map { it.toInt() }
        val actual = h * 60 + min.toInt()
        assertTrue(
            "$label: expected $expected, got $h:$min",
            abs(actual - (eh * 60 + em)) <= 1
        )
    }

    @Test
    fun springEquinoxPeriod() = assertSunTimes(LocalDateTime(2026, 3, 15, 12, 0), "06:40", "18:27")

    @Test
    fun dayAfterDstStarts() = assertSunTimes(LocalDateTime(2026, 3, 30, 12, 0), "06:58", "19:58")

    @Test
    fun summerSolstice() = assertSunTimes(LocalDateTime(2026, 6, 21, 12, 0), "04:29", "22:22")

    @Test
    fun autumnEquinoxPeriod() = assertSunTimes(LocalDateTime(2026, 9, 15, 12, 0), "06:54", "19:42")

    @Test
    fun november() = assertSunTimes(LocalDateTime(2026, 11, 16, 12, 0), "08:07", "16:09")

    @Test
    fun winterSolstice() = assertSunTimes(LocalDateTime(2026, 12, 21, 12, 0), "09:00", "15:43")

    @Test
    fun january() = assertSunTimes(LocalDateTime(2026, 1, 15, 12, 0), "08:50", "16:15")
}
