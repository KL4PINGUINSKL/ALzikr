package com.alzikr.app

import android.content.Context
import android.util.JsonReader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tafsir books from github.com/spa5k/tafsir_api (MIT; texts from quran.com and Tarteel's QUL).
 * A surah's tafsir is fetched the first time it is opened and kept on the phone; the short tafsirs
 * can also be downloaded whole for offline reading. Long tafsirs explain several verses together,
 * so a repeated text is stored once ("=n" points to verse n) and shown with its verse range.
 */
object Tafsir {
    data class Book(val id: String, val name: String, val about: String, val slug: String, val light: Boolean)
    data class Entry(val text: String, val from: Int, val to: Int)

    val books = listOf(
        Book("muyassar", "التفسير الميسر", "مختصر واضح، من مجمع الملك فهد", "ar-tafsir-muyassar", true),
        Book("mukhtasar", "المختصر في التفسير", "موجز معاصر، من مركز تفسير", "ar-tafsir-al-mukhtasar", true),
        Book("jalalayn", "تفسير الجلالين", "المحلي والسيوطي، موجز", "ar-tafsir-al-jalalayn", true),
        Book("saadi", "تفسير السعدي", "تيسير الكريم الرحمن", "ar-tafseer-al-saddi", true),
        Book("ibnkathir", "تفسير ابن كثير", "تفسير القرآن العظيم، بالمأثور", "ar-tafsir-ibn-kathir", false),
        Book("tabari", "تفسير الطبري", "جامع البيان، أوسع التفاسير بالمأثور", "ar-tafsir-al-tabari", false),
        Book("qurtubi", "تفسير القرطبي", "الجامع لأحكام القرآن", "ar-tafseer-al-qurtubi", false),
        Book("baghawi", "تفسير البغوي", "معالم التنزيل", "ar-tafsir-al-baghawi", false),
        Book("wasit", "التفسير الوسيط", "تفسير معاصر مفصّل", "ar-tafsir-al-wasit", false),
        Book("tanwir", "التحرير والتنوير", "ابن عاشور، لغة وبلاغة", "ar-tafseer-tahrir-al-tanwir", false),
        Book("irab", "إعراب القرآن الميسر", "إعراب الكلمات آية آية", "al-i-rab-al-muyassar", false),
    )
    fun book(id: String) = books.firstOrNull { it.id == id } ?: books.first()

    var selected: String
        get() = Store.prefs.getString("tafsir_sel", "muyassar") ?: "muyassar"
        set(v) { Store.prefs.edit().putString("tafsir_sel", v).apply(); Store.bump() }

    var busy by mutableStateOf<String?>(null)        // "<id>" while a whole book downloads
    var done by mutableIntStateOf(0)                 // surahs fetched in that download
    var version by mutableIntStateOf(0)

    private fun dir(c: Context, id: String) = File(c.filesDir, "tafsir/$id").apply { mkdirs() }
    private fun file(c: Context, id: String, s: Int) = File(dir(c, id), "$s.txt")
    fun has(c: Context, id: String, s: Int) = file(c, id, s).exists()
    fun count(c: Context, id: String) = (1..114).count { has(c, id, it) }
    fun bytes(c: Context, id: String) = dir(c, id).listFiles()?.sumOf { it.length() } ?: 0L
    fun delete(c: Context, id: String) { dir(c, id).deleteRecursively(); cache.keys.removeAll { it.startsWith("$id/") }; version++ }

    private val cache = object : LinkedHashMap<String, Map<Int, String>>(8, .75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, Map<Int, String>>?) = size > 6
    }

    private fun surahMap(c: Context, id: String, s: Int): Map<Int, String>? {
        cache["$id/$s"]?.let { return it }
        val f = file(c, id, s); if (!f.exists()) return null
        val m = HashMap<Int, String>()
        f.forEachLine { l -> val p = l.split('\t', limit = 2); if (p.size == 2) p[0].toIntOrNull()?.let { m[it] = p[1].replace("\\n", "\n") } }
        return m.also { cache["$id/$s"] = it }
    }

    /** The tafsir of a verse, or null when this surah is not on the phone yet. */
    fun get(c: Context, id: String, s: Int, a: Int): Entry? {
        val m = surahMap(c, id, s) ?: return null
        val raw = m[a] ?: return Entry("", a, a)
        val start = if (raw.startsWith("=")) raw.substring(1).toIntOrNull() ?: a else a
        val text = if (raw.startsWith("=")) m[start] ?: "" else raw
        var end = start
        while (m[end + 1] == "=$start") end++
        return Entry(text, start, end)
    }

    /** Fetches one surah's tafsir. Blocking: call off the main thread. */
    fun fetch(c: Context, id: String, s: Int): Boolean {
        if (has(c, id, s)) return true
        val b = book(id)
        return try {
            val conn = URL("https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/${b.slug}/$s.json").openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000; conn.readTimeout = 60_000
            if (conn.responseCode != 200) { conn.disconnect(); return false }
            val rows = ArrayList<Pair<Int, String>>()
            JsonReader(conn.inputStream.bufferedReader()).use { r ->
                r.beginArray()
                while (r.hasNext()) {
                    var a = 0; var text = ""
                    r.beginObject()
                    while (r.hasNext()) when (r.nextName()) {
                        "ayah" -> a = if (r.peek() == android.util.JsonToken.NULL) { r.nextNull(); 0 } else r.nextString().toDoubleOrNull()?.toInt() ?: 0
                        "text" -> text = if (r.peek() == android.util.JsonToken.NULL) { r.nextNull(); "" } else r.nextString()
                        else -> r.skipValue()
                    }
                    r.endObject()
                    if (a > 0) rows += a to text.trim()
                }
                r.endArray()
            }
            conn.disconnect()
            if (rows.isEmpty()) return false
            val tmp = File(file(c, id, s).path + ".part")
            tmp.bufferedWriter().use { out ->
                var lastText: String? = null; var lastStart = 0
                rows.sortedBy { it.first }.forEach { (a, t) ->
                    if (t.isNotEmpty() && t == lastText) out.write("$a\t=$lastStart\n")
                    else { out.write("$a\t${t.replace("\r", "").replace("\n", "\\n").replace('\t', ' ')}\n"); lastText = t; lastStart = a }
                }
            }
            tmp.renameTo(file(c, id, s)).also { version++ }
        } catch (e: Exception) {
            android.util.Log.w("ALzikr", "tafsir $id/$s", e); false
        }
    }

    /** Downloads every surah of a (short) tafsir. Blocking. */
    fun fetchAll(c: Context, id: String): Boolean {
        busy = id; done = count(c, id)
        try {
            for (s in 1..114) { if (!fetch(c, id, s)) return false; done = count(c, id) }
            return true
        } finally { busy = null }
    }

    /** The 1.1.0 single-file Jalalayn is replaced by the per-surah store. */
    fun cleanup(c: Context) { File(c.filesDir, "tafsir_jalalayn.txt").delete() }
}
