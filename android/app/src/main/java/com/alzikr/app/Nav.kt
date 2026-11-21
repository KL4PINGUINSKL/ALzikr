package com.alzikr.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App-wide navigation state, so one screen can open another (the khatma opens the mushaf, etc.). */
object Nav {
    var tab by mutableStateOf("prayer")
    /** Open the mushaf at (surah, ayah). */
    var quran by mutableStateOf<Pair<Int, Int>?>(null)
    /** Open a hadith book at a hadith number. */
    var hadith by mutableStateOf<Pair<String, Int>?>(null)
    /** Sub-section inside the daily tab (0 tasks, 1 habits, 2 focus, 3 khatma, 4 notes). */
    var daily by mutableStateOf(0)

    fun openQuran(surah: Int, ayah: Int) { quran = surah to ayah; tab = "library" }
    fun openHadith(book: String, number: Int) { hadith = book to number; tab = "library" }
}
