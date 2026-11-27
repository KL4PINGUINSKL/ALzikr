package com.alzikr.app

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** The five prayers plus sunrise (shown, never alerted as a prayer). */
enum class Prayer(val base: String) { FAJR("الفجر"), SUNRISE("الشروق"), DHUHR("الظهر"), ASR("العصر"), MAGHRIB("المغرب"), ISHA("العشاء");
    val alertable get() = this != SUNRISE
    /** Today's name: on Friday the midday prayer is «الجمعة». */
    val ar: String get() = on(java.time.LocalDate.now())
    fun on(d: java.time.LocalDate): String = if (this == DHUHR && d.dayOfWeek == java.time.DayOfWeek.FRIDAY) "الجمعة" else base
}

/**
 * A calculation method: the sun angle below the horizon for Fajr and Isha, or Isha as minutes after Maghrib.
 * The angles are the ones published by each authority.
 */
enum class Method(val ar: String, val fajr: Double, val isha: Double, val ishaMinutes: Int = 0) {
    MAKKAH("أم القرى (مكة المكرمة)", 18.5, 0.0, 90),
    EGYPT("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    MWL("رابطة العالم الإسلامي", 18.0, 17.0),
    KARACHI("جامعة العلوم الإسلامية، كراتشي", 18.0, 18.0),
    ISNA("أمريكا الشمالية (ISNA)", 15.0, 15.0),
    DUBAI("دبي", 18.2, 18.2),
    QATAR("قطر", 18.0, 0.0, 90),
    KUWAIT("الكويت", 18.0, 17.5),
    TURKEY("رئاسة الشؤون الدينية التركية", 18.0, 17.0),
    JORDAN("الأردن", 18.0, 18.0),
}

/**
 * Prayer times from the sun's position (the PrayTimes.org equations), fully offline.
 * [asrFactor] 1 = Shafi'i, Maliki, Hanbali (shadow = length); 2 = Hanafi (twice the length).
 * Times are minutes after local midnight in the phone's time zone for that date.
 */
object PrayTimes {
    fun compute(date: LocalDate, lat: Double, lng: Double, method: Method, asrFactor: Int, zone: ZoneId = ZoneId.systemDefault()): Map<Prayer, Int> {
        val tz = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15 * 24)

        fun sun(t: Double): Pair<Double, Double> { // (declination, equation of time)
            val d = jd + t - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15
            val eqt = q / 15 - fixHour(ra)
            val decl = darcsin(dsin(e) * dsin(l))
            return decl to eqt
        }
        fun midDay(t: Double) = fixHour(12 - sun(t).second)
        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(t).first
            val noon = midDay(t)
            val x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            if (x < -1 || x > 1) return Double.NaN // the sun never reaches that angle today (far north/south)
            val time = darccos(x) / 15
            return noon + if (ccw) -time else time
        }
        fun asr(t: Double): Double {
            val decl = sun(t).first
            val angle = -darccot(asrFactor + dtan(abs(lat - decl)))
            return angleTime(angle, t, false)
        }

        // Two rounds: first from rough guesses, then from the first answers (more exact).
        var f = 5.0; var sr = 6.0; var dh = 12.0; var a = 13.0; var ss = 18.0; var ish = 18.0
        repeat(2) {
            val nf = angleTime(method.fajr, f / 24, true)
            val nsr = angleTime(0.833, sr / 24, true)
            val ndh = midDay(dh / 24)
            val na = asr(a / 24)
            val nss = angleTime(0.833, ss / 24, false)
            val nish = if (method.ishaMinutes > 0) Double.NaN else angleTime(method.isha, ish / 24, false)
            f = nf; sr = nsr; dh = ndh; a = na; ss = nss; ish = nish
        }
        val shift = tz - lng / 15
        f += shift; sr += shift; dh += shift; a += shift; ss += shift
        ish = if (method.ishaMinutes > 0) ss + method.ishaMinutes / 60.0 else ish + shift

        // High latitudes: when Fajr/Isha are missing or too far, use the "angle-based" portion of the night.
        val night = 24 - (ss - sr)
        if (f.isNaN() || sr - f > method.fajr / 60 * night) f = sr - method.fajr / 60 * night
        if (method.ishaMinutes == 0 && (ish.isNaN() || ish - ss > method.isha / 60 * night)) ish = ss + method.isha / 60 * night

        fun min(h: Double) = Math.round(h * 60).toInt()
        return linkedMapOf(
            Prayer.FAJR to min(f), Prayer.SUNRISE to min(sr), Prayer.DHUHR to min(dh) + 1, // Dhuhr a minute after the sun's zenith
            Prayer.ASR to min(a), Prayer.MAGHRIB to min(ss) + 1, Prayer.ISHA to min(ish),
        )
    }

    /** Direction of the Kaaba from north, clockwise, in degrees. */
    fun qibla(lat: Double, lng: Double): Double {
        val kLat = 21.4225; val kLng = 39.8262
        val y = dsin(kLng - lng)
        val x = dcos(lat) * dtan(kLat) - dsin(lat) * dcos(kLng - lng)
        return fixAngle(darctan2(y, x))
    }

    private fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0; var m = m0
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun dsin(d: Double) = sin(rad(d))
    private fun dcos(d: Double) = cos(rad(d))
    private fun dtan(d: Double) = tan(rad(d))
    private fun darcsin(x: Double) = deg(asin(x))
    private fun darccos(x: Double) = deg(acos(x))
    private fun darctan2(y: Double, x: Double) = deg(atan2(y, x))
    private fun darccot(x: Double) = deg(atan(1 / x))
    private fun fixAngle(a: Double) = a - 360 * floor(a / 360)
    private fun fixHour(a: Double) = a - 24 * floor(a / 24)
}
