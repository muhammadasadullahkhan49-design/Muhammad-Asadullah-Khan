package com.example.prayer

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Punjab City Geographic Data
 */
data class City(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneOffset: Double = 5.0 // PKT (UTC+5)
)

/**
 * Calculated Prayer Times in HH:mm string format and Epoch Milliseconds
 */
data class PrayerTimes(
    val city: City,
    val date: Date,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val fajrMillis: Long,
    val dhuhrMillis: Long,
    val asrMillis: Long,
    val maghribMillis: Long,
    val ishaMillis: Long
)

/**
 * Astronomical Prayer Time Calculation Engine
 * Calculation Method: University of Islamic Sciences, Karachi (18° Fajr, 18° Isha)
 * Juristic Method: Hanafi (Shadow ratio factor = 2)
 */
object PrayerCalculator {

    val PUNJAB_CITIES = listOf(
        City("multan", "Multan", 30.1575, 71.5249),
        City("lahore", "Lahore", 31.5204, 74.3587),
        City("gujrat", "Gujrat", 32.5742, 74.0754),
        City("lalamusa", "Lalamusa", 32.7000, 73.9600),
        City("bahawalpur", "Bahawalpur", 29.3544, 71.6911),
        City("rawalpindi", "Rawalpindi", 33.5651, 73.0169),
        City("faisalabad", "Faisalabad", 31.4504, 73.1350),
        City("sialkot", "Sialkot", 32.4945, 74.5229),
        City("sargodha", "Sargodha", 32.0836, 72.6711),
        City("sheikhupura", "Sheikhupura", 31.7167, 73.9850),
        City("rahim_yar_khan", "Rahim Yar Khan", 28.4212, 70.2989),
        City("gujranwala", "Gujranwala", 32.1877, 74.1945),
        City("jhelum", "Jhelum", 32.9405, 73.7276),
        City("attock", "Attock", 33.7667, 72.3597),
        City("dg_khan", "Dera Ghazi Khan", 30.0561, 70.6348),
        City("sahiwal", "Sahiwal", 30.6682, 73.1014)
    )

    fun getDefaultCity(): City = PUNJAB_CITIES.first { it.id == "multan" }

    fun calculateTimes(city: City, date: Date = Date()): PrayerTimes {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi")).apply {
            time = date
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Julian Date Calculation
        val julianDate = getJulianDate(year, month, day)
        val d = julianDate - 2451545.0

        // Mean Solar Longitude and Anomaly
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val L = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))

        // Obliquity of Ecliptic & Solar Declination
        val e = 23.439 - 0.00000036 * d
        val declination = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(L))))

        // Equation of Time
        val ra = Math.toDegrees(Math.atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(L)), cos(Math.toRadians(L)))) / 15.0
        val fixRa = fixHour(ra)
        val eqOfTime = q / 15.0 - fixRa

        // Solar Noon (Dhuhr) in Hours
        val noon = 12.0 + city.timezoneOffset - (city.longitude / 15.0) - eqOfTime

        // Karachi Parameters: Fajr angle = 18°, Isha angle = 18°
        val fajrAngle = 18.0
        val ishaAngle = 18.0

        val fajrHour = noon - getHourAngle(-fajrAngle, city.latitude, declination)
        val sunriseHour = noon - getHourAngle(-0.833, city.latitude, declination)
        val dhuhrHour = noon + (2.0 / 60.0) // 2 min safety buffer
        val asrHour = noon + getAsrHourAngle(2.0, city.latitude, declination) // Hanafi shadow factor = 2
        val maghribHour = noon + getHourAngle(-0.833, city.latitude, declination)
        val ishaHour = noon + getHourAngle(-ishaAngle, city.latitude, declination)

        fun hourToMillis(hourDouble: Double): Long {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi")).apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val totalSeconds = (hourDouble * 3600).toLong()
            return cal.timeInMillis + (totalSeconds * 1000)
        }

        return PrayerTimes(
            city = city,
            date = date,
            fajr = formatTime(fajrHour),
            sunrise = formatTime(sunriseHour),
            dhuhr = formatTime(dhuhrHour),
            asr = formatTime(asrHour),
            maghrib = formatTime(maghribHour),
            isha = formatTime(ishaHour),
            fajrMillis = hourToMillis(fajrHour),
            dhuhrMillis = hourToMillis(dhuhrHour),
            asrMillis = hourToMillis(asrHour),
            maghribMillis = hourToMillis(maghribHour),
            ishaMillis = hourToMillis(ishaHour)
        )
    }

    private fun getHourAngle(alpha: Double, lat: Double, dec: Double): Double {
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(dec)
        val alphaRad = Math.toRadians(alpha)

        val top = sin(alphaRad) - sin(latRad) * sin(decRad)
        val bottom = cos(latRad) * cos(decRad)
        val cosH = top / bottom
        val clampedCosH = cosH.coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(clampedCosH)) / 15.0
    }

    private fun getAsrHourAngle(shadowFactor: Double, lat: Double, dec: Double): Double {
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(dec)
        val delta = abs(latRad - decRad)
        val shadowLen = shadowFactor + tan(delta)
        val angleRad = atan(1.0 / shadowLen)

        val top = sin(angleRad) - sin(latRad) * sin(decRad)
        val bottom = cos(latRad) * cos(decRad)
        val cosH = (top / bottom).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosH)) / 15.0
    }

    private fun getJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = Math.floor(y / 100.0)
        val b = 2 - a + Math.floor(a / 4.0)
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0) a += 360.0
        return a
    }

    private fun fixHour(hour: Double): Double {
        var h = hour % 24.0
        if (h < 0) h += 24.0
        return h
    }

    private fun formatTime(hourDouble: Double): String {
        val totalMinutes = (hourDouble * 60).toInt()
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val amPm = if (hours >= 12) "PM" else "AM"
        val displayHour = if (hours % 12 == 0) 12 else hours % 12
        return String.format("%02d:%02d %s", displayHour, minutes, amPm)
    }
}
