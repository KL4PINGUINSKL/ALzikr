package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime

/**
 * «نور الليل»: a warm amber veil over the whole app (and an optional dimming) that comes on with Isha and goes with
 * Fajr, by the prayer times of the user's own city, so night reading of the Quran and the adhkar is gentle on the eyes.
 * It is drawn over the screens and lets every touch through.
 */
object Comfort {
    private val p get() = Store.prefs
    /** 0 = off, 1 = from Isha to Fajr, 2 = always. */
    var nightLight: Int
        get() = p.getInt("comfort_night", 0)
        set(v) { p.edit().putInt("comfort_night", v).apply(); Store.bump() }
    /** How warm: 1 light, 2 medium, 3 strong. */
    var warmth: Int
        get() = p.getInt("comfort_warmth", 2)
        set(v) { p.edit().putInt("comfort_warmth", v.coerceIn(1, 3)).apply(); Store.bump() }
    /** Extra dimming below the phone's lowest brightness: 0 none, 1 a little, 2 more. */
    var dim: Int
        get() = p.getInt("comfort_dim", 0)
        set(v) { p.edit().putInt("comfort_dim", v.coerceIn(0, 2)).apply(); Store.bump() }

    /** Isha and Fajr today in minutes after midnight (20:00 and 05:00 until a location is set). */
    private fun window(): Pair<Int, Int> = if (!Store.hasLocation) 20 * 60 to 5 * 60 else runCatching {
        val t = Store.times(LocalDate.now()); t.getValue(Prayer.ISHA) to t.getValue(Prayer.FAJR)
    }.getOrDefault(20 * 60 to 5 * 60)

    fun activeNow(): Boolean = when (nightLight) {
        2 -> true
        1 -> { val (isha, fajr) = window(); val m = LocalTime.now().let { it.hour * 60 + it.minute }; m >= isha || m < fajr }
        else -> false
    }

    /** «يعمل الآن حتى الفجر 05:12» or «يبدأ مع العشاء 19:40». */
    fun status(): String = when (nightLight) {
        0 -> "متوقف"
        2 -> "يعمل دائمًا"
        else -> { val (isha, fajr) = window(); if (activeNow()) "يعمل الآن حتى الفجر ${Alerts.timeText(fajr)}" else "يبدأ مع العشاء ${Alerts.timeText(isha)}" }
    }

    fun warmColor(): Color = Color(0xFFFF8A1F).copy(alpha = when (warmth) { 1 -> .10f; 3 -> .26f; else -> .17f })
    fun dimColor(): Color = Color.Black.copy(alpha = when (dim) { 1 -> .22f; 2 -> .40f; else -> 0f })
}

/** The veil itself; checked each minute so it comes on at Isha and goes at Fajr while the app is open. */
@Composable
fun ComfortOverlay() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000); tick++ } }
    @Suppress("UNUSED_EXPRESSION") tick
    if (!Comfort.activeNow()) return
    Box(Modifier.fillMaxSize().background(Comfort.warmColor()))
    if (Comfort.dim > 0) Box(Modifier.fillMaxSize().background(Comfort.dimColor()))
}
