package com.alzikr.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App-wide navigation state, so one screen can open another (the khatma opens the mushaf, etc.). */
object Nav {
    var tab by mutableStateOf("prayer")
    /** Open the mushaf at (surah, ayah). */
    var quran by mutableStateOf<Pair<Int, Int>?>(null)
    /** Open a hadith book at a hadith number. */
    var hadith by mutableStateOf<Pair<String, Int>?>(null)
    /** Open an explained hadith (HadeethEnc id). */
    var sharh by mutableStateOf<String?>(null)
    /** Sub-section inside the daily tab (0 tasks, 1 habits, 2 focus, 3 khatma, 4 notes). */
    var daily by mutableStateOf(0)
    /** Library section to show (0 Quran … 6 favorites), a "More" page, and a Quran story by index. */
    var library by mutableStateOf<Int?>(null)
    var morePage by mutableStateOf<String?>(null)
    var story by mutableStateOf<Int?>(null)
    /** Open the hadith search, searching this right away. */
    var hadithQuery by mutableStateOf<String?>(null)
    /** Adhkar tab section to open (0 my awrad, 1 adhkar, 2 tasbih). */
    var adhkar by mutableStateOf<Int?>(null)
    /** Open one adhkar collection by id (morning, evening, sleep…). */
    var collection by mutableStateOf<String?>(null)
    /** Open one prayer's own page on the prayer tab. */
    var prayerSheet by mutableStateOf<Prayer?>(null)
    /** Inner pages open now (innermost last). While any is open the app header and the tab bar hide. */
    val pages = mutableStateListOf<Any>()
    /** Pages opened on top of the current tab (back returns to it): an explained hadith, a book hadith, a verse. */
    var overlaySharh by mutableStateOf<String?>(null)
    var overlayHadith by mutableStateOf<Pair<String, String>?>(null)
    var overlayQuran by mutableStateOf<Pair<Int, Int>?>(null)
    /** The mushaf in reading mode (surah, verse), full screen over everything. */
    var reading by mutableStateOf<Pair<Int, Int>?>(null)

    /** The tab a shortcut came from: back returns there (cleared when a tab is chosen in the bar). */
    var backTo by mutableStateOf<String?>(null)
    /** Go to a tab from a shortcut, remembering where we were. */
    fun go(t: String) { if (t != tab) backTo = tab; tab = t }
    /** Back to where the shortcut came from; false when there is nowhere to return. */
    fun back(): Boolean { val t = backTo ?: return false; backTo = null; tab = t; return true }

    fun openQuran(surah: Int, ayah: Int) { quran = surah to ayah; tab = "library" }
    fun openHadith(book: String, number: Int) { hadith = book to number; tab = "library" }
}
