package com.alzikr.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Prints the times for a few cities so they can be compared with published timetables. */
class PrayTimesTest {
    private fun hm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
    private fun show(name: String, lat: Double, lng: Double, method: Method, zone: String, asr: Int = 1): Map<Prayer, Int> {
        val t = PrayTimes.compute(LocalDate.of(2026, 11, 21), lat, lng, method, asr, ZoneId.of(zone))
        println("$name: " + t.entries.joinToString("  ") { "${it.key.name}=${hm(it.value)}" })
        return t
    }

    @Test fun cities() {
        val r = show("Riyadh", 24.7136, 46.6753, Method.MAKKAH, "Asia/Riyadh")
        show("Makkah", 21.4225, 39.8262, Method.MAKKAH, "Asia/Riyadh")
        show("Cairo", 30.0444, 31.2357, Method.EGYPT, "Africa/Cairo")
        show("Istanbul", 41.0082, 28.9784, Method.TURKEY, "Europe/Istanbul")
        show("Berlin", 52.52, 13.405, Method.MWL, "Europe/Berlin")
        println("Qibla Berlin=" + PrayTimes.qibla(52.52, 13.405) + " Cairo=" + PrayTimes.qibla(30.0444, 31.2357))
        val order = listOf(Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA).map { r.getValue(it) }
        assertTrue(order.zipWithNext().all { (a, b) -> a < b })
    }
}
