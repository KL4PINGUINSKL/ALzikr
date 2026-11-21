package com.alzikr.app

import android.content.Context
import android.util.JsonReader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
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
    fun firstOfJuz(juz: Int): Ayah = ayahs.first { it.juz >= juz }

    /** Same folding as the search column: no marks, one form of alef, ya and ta marbuta. */
    fun normalize(t: String): String = t
        .replace(Regex("[ؐ-ًؚ-ٰٟۖ-ۭ࣓-ࣿـ]"), "")
        .replace('ٱ', 'ا').replace(Regex("[آأإ]"), "ا")
        .replace('ى', 'ي').replace('ی', 'ي').replace('ة', 'ه').replace('ک', 'ك')
        .replace(Regex("\\s+"), " ").trim()

    fun search(q: String, limit: Int = 300): List<Ayah> {
        val n = normalize(q)
        if (n.length < 2) return emptyList()
        return ayahs.asSequence().filter { it.plain.contains(n) }.take(limit).toList()
    }

    fun digits(n: Int) = n.toString().map { "٠١٢٣٤٥٦٧٨٩"[it - '0'] }.joinToString("")

    /** One short verse per day, the same for everyone on that date. */
    fun ofTheDay(date: LocalDate = LocalDate.now()): Ayah? {
        if (!ready) return null
        val short = ayahs.filter { it.plain.length in 40..170 }
        return short[Math.floorMod(date.toEpochDay() * 7919, short.size.toLong()).toInt()]
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

    // ----------------------------------------------------------------- tafsir (downloaded)

    data class Tafsir(val id: String, val name: String, val edition: String)
    val tafasir = listOf(Tafsir("jalalayn", "تفسير الجلالين", "ara-jalaladdinalmah"))
    private val cache = HashMap<String, Map<Int, String>>()
    var downloading by mutableStateOf<String?>(null)
    var progress by mutableStateOf(0f)

    private fun file(c: Context, id: String) = File(c.filesDir, "tafsir_$id.txt")
    fun hasTafsir(c: Context, id: String) = file(c, id).exists()

    fun tafsir(c: Context, id: String, s: Int, a: Int): String? {
        val map = cache.getOrPut(id) {
            val f = file(c, id); if (!f.exists()) return null
            val m = HashMap<Int, String>(6300)
            f.forEachLine { l -> val p = l.split('\t', limit = 3); if (p.size == 3) m[p[0].toInt() * 1000 + p[1].toInt()] = p[2] }
            m
        }
        return map[s * 1000 + a]
    }

    /** Downloads a tafsir from quran-api (jsDelivr) and keeps only what the app needs. Blocking: call off the main thread. */
    fun downloadTafsir(c: Context, t: Tafsir): Boolean {
        downloading = t.id; progress = 0f
        val tmp = File(c.filesDir, "tafsir_${t.id}.part")
        return try {
            val conn = URL("https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/${t.edition}.min.json").openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000; conn.readTimeout = 60_000
            if (conn.responseCode != 200) return false
            var n = 0
            tmp.bufferedWriter().use { out ->
                JsonReader(conn.inputStream.bufferedReader()).use { r ->
                    r.beginObject()
                    while (r.hasNext()) {
                        if (r.nextName() != "quran") { r.skipValue(); continue }
                        r.beginArray()
                        while (r.hasNext()) {
                            var s = 0; var a = 0; var text = ""
                            r.beginObject()
                            while (r.hasNext()) when (r.nextName()) { "chapter" -> s = r.nextInt(); "verse" -> a = r.nextInt(); "text" -> text = r.nextString(); else -> r.skipValue() }
                            r.endObject()
                            out.write("$s\t$a\t${text.replace('\n', ' ').replace('\t', ' ')}\n")
                            if (++n % 200 == 0) progress = n / 6236f
                        }
                        r.endArray()
                    }
                    r.endObject()
                }
            }
            conn.disconnect()
            n > 6000 && tmp.renameTo(file(c, t.id))
        } catch (e: Exception) {
            android.util.Log.w("ALzikr", "tafsir download", e); false
        } finally { tmp.delete(); downloading = null; cache.remove(t.id) }
    }
}
