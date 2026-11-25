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
    var daily by mutableStateOf(5)   // Daily.TODAY
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
    /** Today's wird: its mushaf pages on black, full screen. */
    var wird by mutableStateOf(false)
    /** A day's card (virtue and sunan), over any page. */
    var daySheet by mutableStateOf<java.time.LocalDate?>(null)
    /** The counter on screen now (adhkar): the volume keys tap it. */
    var volumeTap: (() -> Unit)? = null
    /** Inner pages open now (innermost last). While any is open the app header and the tab bar hide. */
    val pages = mutableStateListOf<Any>()
    /** Pages opened on top of the current tab (back returns to it): an explained hadith, a book hadith, a verse. */
    var overlaySharh by mutableStateOf<String?>(null)
    var overlayHadith by mutableStateOf<Pair<String, String>?>(null)
    var overlayQuran by mutableStateOf<Pair<Int, Int>?>(null)
    /** The mushaf in reading mode (surah, verse), full screen over everything. */
    var reading by mutableStateOf<Pair<Int, Int>?>(null)

    /** The tabs visited before this one, newest last: back walks them in reverse. */
    val history = mutableStateListOf<String>()
    /** Go to a tab (from the bar or a shortcut), remembering where we were. */
    /** The first tab: the kids section in the theme for children, the prayer page otherwise. */
    val home: String get() = if (Looks.byId(Data.look).kids) "kids" else "prayer"
    fun go(t: String) {
        if (t == tab) return
        openedFrom = null   // moving on by hand: «back» from a verse stays where it is
        history.remove(t); history += tab
        if (history.size > 12) history.removeAt(0)
        tab = t
    }
    /** Back to the previous tab; false when there is none. */
    fun back(): Boolean {
        if (history.isNotEmpty()) { tab = history.removeAt(history.lastIndex); return true }
        // Nothing before: the home page is the last stop before leaving the app.
        if (tab != home) { tab = home; return true }
        return false
    }

    /** [sheet]: also open the verse's sheet; [tadabbur]: with its reflections already open. */
    var quranSheet by mutableStateOf(false)
    var openTadabbur = false
    /** The tab a verse or hadith was opened from (e.g. «ختمتك» in «يومي»): «back» from it returns there, not to the library. */
    var openedFrom: String? = null
    fun openQuran(surah: Int, ayah: Int, sheet: Boolean = false, tadabbur: Boolean = false) { quranSheet = sheet; openTadabbur = tadabbur; quran = surah to ayah; openedFrom = tab.takeIf { it != "library" }; tab = "library" }
    fun openHadith(book: String, number: Int) { hadith = book to number; openedFrom = tab.takeIf { it != "library" }; tab = "library" }
    fun openBook(b: Books.Book) { book = b; openedFrom = tab.takeIf { it != "library" }; tab = "library" }
    /** A book opened from the global search. */
    var book by mutableStateOf<Books.Book?>(null)
    /** The global search page is open (🔍 in the header). */
    var search by mutableStateOf(false)
    /** A settings section asked for from outside the settings (the global search). */
    var settingsReq by mutableStateOf<String?>(null)
    /** Closing a verse or hadith opened from another tab goes back to that tab. */
    fun closeOpened() { openedFrom?.let { tab = it }; openedFrom = null }
}
