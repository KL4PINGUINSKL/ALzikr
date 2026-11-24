package com.alzikr.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/**
 * The Holy Quran, bundled: text by Khaled Hosny (for the Amiri Quran font) with page, juz and sajda
 * data from quran-api / Tanzil, and a plain copy for searching. Tafsir al-Jalalayn can be downloaded.
 * Source: tanzil.net (Quran text, used verbatim) via github.com/fawazahmed0/quran-api.
 */
object Quran {
    data class Surah(val n: Int, val name: String, val meccan: Boolean, val count: Int)
    data class Ayah(val s: Int, val a: Int, val page: Int, val juz: Int, val sajda: Boolean, val text: String, val plain: String)

    const val BASMALA = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ"
    var ready by mutableStateOf(false)
        private set
    lateinit var surahs: List<Surah>; private set
    lateinit var ayahs: List<Ayah>; private set
    private lateinit var start: IntArray // index of each surah's first ayah

    /** Loads the text once (about 2 MB), off the main thread. */
    fun load(c: Context) {
        if (ready) return
        synchronized(this) {
            if (ready) return
            surahs = c.assets.open("surahs.txt").bufferedReader().readLines().filter { it.isNotBlank() }.map {
                val p = it.split('\t'); Surah(p[0].toInt(), p[1], p[2] == "m", p[3].toInt())
            }
            val list = ArrayList<Ayah>(6236)
            c.assets.open("quran.txt").bufferedReader().forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val p = line.split('\t')
                list += Ayah(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4] == "1", p[5], p[6])
            }
            ayahs = list
            start = IntArray(115)
            list.forEachIndexed { i, a -> if (a.a == 1) start[a.s] = i }
            ready = true
        }
    }

    fun ofSurah(s: Int): List<Ayah> = ayahs.subList(start[s], if (s < 114) start[s + 1] else ayahs.size)
    fun at(s: Int, a: Int): Ayah? = ayahs.getOrNull(start[s] + a - 1)
    fun firstOfPage(page: Int): Ayah = ayahs.firstOrNull { it.page >= page } ?: ayahs.last()
    /** "s:a" of a page's first verse, loading the text if needed (for notifications). */
    fun firstOfPageSafe(c: Context, page: Int): String { load(c); return firstOfPage(page).let { "${it.s}:${it.a}" } }
    fun firstOfJuz(juz: Int): Ayah = ayahs.first { it.juz >= juz }

    /** Same folding as the search column: no marks, one form of alef, ya and ta marbuta. */
    fun normalize(t: String): String = t
        .replace(Regex("[ؐ-ًؚ-ٰٟۖ-ۭ࣓-ࣿـ]"), "")
        .replace('ٱ', 'ا').replace(Regex("[آأإ]"), "ا")
        .replace('ى', 'ي').replace('ی', 'ي').replace('ة', 'ه').replace('ک', 'ك')
        .replace(Regex("\\s+"), " ").trim()

    /** Western digits for Arabic-Indic and Persian ones. */
    fun latinDigits(t: String) = t.map { c -> when (c) { in '٠'..'٩' -> '0' + (c - '٠'); in '۰'..'۹' -> '0' + (c - '۰'); else -> c } }.joinToString("")

    /** A place a query names: a surah, a verse or a juz. */
    data class Jump(val label: String, val s: Int, val a: Int, val surahOnly: Boolean)
    private val fillers = setOf("سوره", "سورت", "سور", "السوره", "ايه", "الايه", "ايات", "الايات", "رقم", "اقرا", "افتح", "قراءه", "من")

    /**
     * Reads what people type: «سورة الكهف», «الكهف 10», «سورة البقرة آية 255», «2:255», «١٨»,
     * «آية الكرسي», «خواتيم البقرة», «جزء عم», «الجزء 30». The «ال» and the letter forms are optional.
     */
    fun jumps(q: String): List<Jump> {
        if (!ready) return emptyList()
        val n = normalize(latinDigits(q)).replace(Regex("[«»\"'()،,.؟?!]"), " ").replace(Regex("\\s+"), " ").trim()
        if (n.isEmpty()) return emptyList()
        val out = mutableListOf<Jump>()
        fun verse(s: Int, a: Int, label: String? = null) {
            val sur = surahs.getOrNull(s - 1) ?: return
            if (a in 1..sur.count) out += Jump(label ?: "سورة ${sur.name} · الآية ${digits(a)}", s, a, false)
        }
        Regex("^(\\d{1,3})\\s*[:/\\-]\\s*(\\d{1,3})$").find(n)?.let { m -> verse(m.groupValues[1].toInt(), m.groupValues[2].toInt()); return out }
        if (n.contains("الكرسي")) verse(2, 255, "آية الكرسي · البقرة ٢٥٥")
        if (n.contains("البقره") && (n.contains("خواتيم") || n.contains("اخر") || n.contains("خاتمه"))) verse(2, 285, "خواتيم سورة البقرة · ٢٨٥")
        Regex("(?:^|\\s)(?:ال)?جزء\\s*(\\d{1,2}|عم|تبارك)(?:\\s|$)").find(n)?.let { m ->
            val j = when (val g = m.groupValues[1]) { "عم" -> 30; "تبارك" -> 29; else -> g.toInt() }
            if (j in 1..30) firstOfJuz(j).let { a -> out += Jump("الجزء ${digits(j)} · ${surahs[a.s - 1].name} ${digits(a.a)}", a.s, a.a, false) }
            return out
        }
        val tokens = n.split(' ').filter { it !in fillers }
        val nums = tokens.mapNotNull { it.toIntOrNull() }
        val name = tokens.filter { it.toIntOrNull() == null }.joinToString(" ")
        fun bare(t: String) = t.removePrefix("ال").replace(" ", "")
        val found = when {
            name.isEmpty() -> nums.firstOrNull()?.takeIf { it in 1..114 }?.let { listOf(surahs[it - 1]) }.orEmpty()
            else -> {
                val b = bare(name)
                surahs.filter { bare(normalize(it.name)) == b }.ifEmpty {
                    if (b.length >= 2) surahs.filter { bare(normalize(it.name)).startsWith(b) } else emptyList()
                }
            }
        }
        val ayah = if (name.isEmpty()) nums.getOrNull(1) else nums.firstOrNull()
        for (s in found.take(6)) {
            if (ayah != null) verse(s.n, ayah) else out += Jump("سورة ${s.name}", s.n, 1, true)
        }
        return out.distinctBy { it.s to it.a }
    }

    fun search(q: String, limit: Int = 300): List<Ayah> {
        val n = normalize(q)
        if (n.length < 2) return emptyList()
        return ayahs.asSequence().filter { it.plain.contains(n) }.take(limit).toList()
    }

    /** The app's digits: ١٢٣ (Arabic-Indic, the default) or 123, chosen in Settings → Appearance. */
    var easternDigits: Boolean
        get() = Store.prefs.getBoolean("digits_eastern", true)
        set(v) { Store.prefs.edit().putBoolean("digits_eastern", v).apply(); Store.bump() }
    fun digits(n: Int) = if (easternDigits) n.toString().map { if (it in '0'..'9') "٠١٢٣٤٥٦٧٨٩"[it - '0'] else it }.joinToString("") else n.toString()
    /** Any text's digits in the chosen style. */
    fun numerals(t: String): String = if (easternDigits) { if (t.none { it in '0'..'9' }) t else String(CharArray(t.length) { i -> t[i].let { c -> if (c in '0'..'9') '٠' + (c - '0') else c } }) }
        else { if (t.none { it in '٠'..'٩' }) t else String(CharArray(t.length) { i -> t[i].let { c -> if (c in '٠'..'٩') '0' + (c - '٠') else c } }) }

    private val shortAyahs: List<Ayah> by lazy { ayahs.filter { it.plain.length in 40..170 } }
    /** One short verse a day, in each person's own random order: none repeats until all of them have come (DailyBag). */
    fun ofTheDay(date: LocalDate = LocalDate.now()): Ayah? {
        if (!ready) return null
        val short = shortAyahs
        if (date == LocalDate.now()) runCatching { return short[DailyBag.index("ayah", short.size)] }
        return short[Math.floorMod(date.toEpochDay() * 7919 + Data.dailyShift("ayah", date) * 104729L, short.size.toLong()).toInt()]
    }

    // ----------------------------------------------------------------- reading place and bookmarks

    var last: Pair<Int, Int>?
        get() = Store.prefs.getString("q_last", null)?.split(':')?.let { it[0].toInt() to it[1].toInt() }
        set(v) { Store.prefs.edit().putString("q_last", v?.let { "${it.first}:${it.second}" }).apply() }
    fun bookmarks(): List<Pair<Int, Int>> = (Store.prefs.getStringSet("q_marks", emptySet()) ?: emptySet())
        .map { it.split(':').let { p -> p[0].toInt() to p[1].toInt() } }.sortedWith(compareBy({ it.first }, { it.second }))
    fun marked(s: Int, a: Int) = "$s:$a" in (Store.prefs.getStringSet("q_marks", emptySet()) ?: emptySet())
    fun toggleMark(s: Int, a: Int) {
        val set = (Store.prefs.getStringSet("q_marks", emptySet()) ?: emptySet()).toMutableSet()
        if (!set.remove("$s:$a")) set += "$s:$a"
        Store.prefs.edit().putStringSet("q_marks", set).apply(); Store.bump()
    }
    var fontSize: Int
        get() = Store.prefs.getInt("q_size", 26)
        set(v) { Store.prefs.edit().putInt("q_size", v.coerceIn(18, 44)).apply(); Store.bump() }
}
