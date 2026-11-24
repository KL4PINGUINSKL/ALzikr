package com.alzikr.app

import android.content.Context
import android.util.JsonReader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Hadith books. The Forty of an-Nawawi and the Forty Qudsi come with the app (fawazahmed0/hadith-api,
 * public domain). The others are downloaded once from github.com/AhmedBaset/hadith-json (Arabic text and
 * Arabic chapter names) and kept on the phone in a compact form; the English is skipped while reading.
 */
object Hadith {
    data class Info(val id: String, val title: String, val author: String, val path: String?, val mb: Int)
    data class Item(val number: String, val chapter: String, val text: String)
    class Book(val info: Info, val chapters: Map<String, String>, val items: List<Item>)

    val books = listOf(
        Info("nawawi40", "الأربعون النووية", "يحيى بن شرف النووي", null, 0),
        Info("qudsi40", "الأحاديث القدسية الأربعون", "جمع من أهل العلم", null, 0),
        Info("riyad_assalihin", "رياض الصالحين", "يحيى بن شرف النووي", "other_books/riyad_assalihin", 2),
        Info("bukhari", "صحيح البخاري", "محمد بن إسماعيل البخاري", "the_9_books/bukhari", 13),
        Info("muslim", "صحيح مسلم", "مسلم بن الحجاج النيسابوري", "the_9_books/muslim", 11),
        Info("abudawud", "سنن أبي داود", "أبو داود السجستاني", "the_9_books/abudawud", 8),
        Info("tirmidhi", "جامع الترمذي", "محمد بن عيسى الترمذي", "the_9_books/tirmidhi", 8),
        Info("nasai", "سنن النسائي", "أحمد بن شعيب النسائي", "the_9_books/nasai", 8),
        Info("ibnmajah", "سنن ابن ماجه", "محمد بن يزيد ابن ماجه", "the_9_books/ibnmajah", 6),
        Info("malik", "موطأ مالك", "مالك بن أنس", "the_9_books/malik", 3),
        Info("darimi", "سنن الدارمي", "عبد الله بن عبد الرحمن الدارمي", "the_9_books/darimi", 3),
        Info("bulugh_almaram", "بلوغ المرام", "ابن حجر العسقلاني", "other_books/bulugh_almaram", 2),
        Info("aladab_almufrad", "الأدب المفرد", "محمد بن إسماعيل البخاري", "other_books/aladab_almufrad", 2),
        Info("shamail_muhammadiyah", "الشمائل المحمدية", "محمد بن عيسى الترمذي", "other_books/shamail_muhammadiyah", 1),
        Info("mishkat_almasabih", "مشكاة المصابيح", "الخطيب التبريزي", "other_books/mishkat_almasabih", 5),
    )
    fun info(id: String) = books.firstOrNull { it.id == id }

    var downloading by mutableStateOf<String?>(null)
    var progress by mutableStateOf(0f)
    var version by mutableStateOf(0)
    private val cache = HashMap<String, Book>()

    private fun file(c: Context, id: String) = File(File(c.filesDir, "hadith").apply { mkdirs() }, "$id.txt")
    fun available(c: Context, id: String) = info(id)?.path == null || file(c, id).exists()
    fun delete(c: Context, id: String) { file(c, id).delete(); cache.remove(id); version++ }

    /** Reads a book in the compact format (bundled asset or downloaded file). Off the main thread for big books. */
    fun load(c: Context, id: String): Book? {
        cache[id]?.let { return it }
        val info = info(id) ?: return null
        val input: InputStream = if (info.path == null) c.assets.open("hadith/$id.txt") else file(c, id).takeIf { it.exists() }?.inputStream() ?: return null
        val chapters = LinkedHashMap<String, String>(); val items = ArrayList<Item>()
        input.bufferedReader().useLines { lines ->
            lines.forEach { l ->
                when {
                    l.isBlank() || l.startsWith("#") -> {}
                    l.startsWith("@") -> l.substring(1).split('\t', limit = 2).let { if (it.size == 2) chapters[it[0]] = it[1] }
                    else -> l.split('\t', limit = 3).let { if (it.size == 3) items += Item(it[0], it[1], it[2].replace("\\n", "\n")) }
                }
            }
        }
        return Book(info, chapters, items).also { cache[id] = it }
    }

    /** Streams the hadith-json file (Arabic only) into the compact format. Blocking: call off the main thread. */
    fun download(c: Context, id: String): Boolean {
        val info = info(id) ?: return false
        val path = info.path ?: return true
        downloading = id; progress = 0f
        val tmp = File(file(c, id).path + ".part")
        return try {
            val conn = URL("https://cdn.jsdelivr.net/gh/AhmedBaset/hadith-json@main/db/by_book/$path.json").openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000; conn.readTimeout = 60_000
            if (conn.responseCode != 200) return false
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: (info.mb * 1_000_000L)
            val counted = object : java.io.FilterInputStream(conn.inputStream) {
                var read = 0L
                override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { if (it > 0) { read += it; progress = (read.toFloat() / total).coerceAtMost(1f) } }
            }
            var n = 0
            tmp.bufferedWriter().use { out ->
                JsonReader(counted.bufferedReader()).use { r ->
                    var title = info.title; var author = info.author
                    r.beginObject()
                    while (r.hasNext()) when (r.nextName()) {
                        "metadata" -> {
                            r.beginObject()
                            while (r.hasNext()) if (r.nextName() == "arabic") {
                                r.beginObject(); while (r.hasNext()) when (r.nextName()) { "title" -> title = r.nextString().ifBlank { title }; "author" -> author = r.nextString().ifBlank { author }; else -> r.skipValue() }; r.endObject()
                            } else r.skipValue()
                            r.endObject()
                            out.write("#$title\t$author\n")
                        }
                        "chapters" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                var cid = ""; var name = ""
                                r.beginObject(); while (r.hasNext()) when (r.nextName()) { "id" -> cid = if (r.peek() == android.util.JsonToken.NULL) { r.nextNull(); "" } else r.nextString(); "arabic" -> name = if (r.peek() == android.util.JsonToken.NULL) { r.nextNull(); "" } else r.nextString(); else -> r.skipValue() }; r.endObject()
                                out.write("@$cid\t${name.replace('\t', ' ').replace('\n', ' ').trim()}\n")
                            }
                            r.endArray()
                        }
                        "hadiths" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                var num = ""; var cid = ""; var text = ""
                                r.beginObject()
                                while (r.hasNext()) when (r.nextName()) {
                                    "idInBook" -> num = if (r.peek() == android.util.JsonToken.NULL) { r.nextNull(); "" } else r.nextString()
                                    "chapterId" -> cid = if (r.peek() == android.util.JsonToken.NULL) { r.nextNull(); "" } else r.nextString()
                                    "arabic" -> text = if (r.peek() == android.util.JsonToken.NULL) { r.nextNull(); "" } else r.nextString()
                                    else -> r.skipValue()
                                }
                                r.endObject()
                                val clean = text.replace(Regex("[ \u00A0]+"), " ").replace(" ،", "،").replace(" :", ":").trim()
                                if (clean.isNotEmpty()) { out.write("$num\t$cid\t${clean.replace("\r", "").replace("\n", "\\n").replace('\t', ' ')}\n"); n++ }
                            }
                            r.endArray()
                        }
                        else -> r.skipValue()
                    }
                    r.endObject()
                }
            }
            conn.disconnect()
            (n > 0 && tmp.renameTo(file(c, id))).also { cache.remove(id); version++ }
        } catch (e: Exception) {
            android.util.Log.w("ALzikr", "hadith download $id", e); false
        } finally { tmp.delete(); downloading = null }
    }

    // ----------------------------------------------------------------- favorites and the daily hadith

    fun favorites(): List<Pair<String, String>> = (Store.prefs.getStringSet("h_fav", emptySet()) ?: emptySet()).map { it.substringBefore('|') to it.substringAfter('|') }
    fun isFavorite(book: String, number: String) = "$book|$number" in (Store.prefs.getStringSet("h_fav", emptySet()) ?: emptySet())
    fun toggleFavorite(book: String, number: String) {
        val set = (Store.prefs.getStringSet("h_fav", emptySet()) ?: emptySet()).toMutableSet()
        if (!set.remove("$book|$number")) set += "$book|$number"
        Store.prefs.edit().putStringSet("h_fav", set).apply(); Store.bump()
    }

    /** A hadith a day from the two bundled forties. */
    fun ofTheDay(c: Context, date: LocalDate = LocalDate.now()): Pair<Info, Item>? {
        if (date == LocalDate.now()) runCatching {
            // Both forties as one list, in each person's own random order (DailyBag).
            val all = listOf("nawawi40", "qudsi40").mapNotNull { load(c, it) }.flatMap { b -> b.items.map { b.info to it } }
            if (all.isNotEmpty()) return all[DailyBag.index("hadith40", all.size)]
        }
        val k = date.toEpochDay() + Data.dailyShift("hadith", date)
        val id = if (k % 2 == 0L) "nawawi40" else "qudsi40"
        val book = load(c, id) ?: return null
        return book.info to book.items[Math.floorMod(k / 2 * 7 + Data.dailyShift("hadith", date), book.items.size.toLong()).toInt()]
    }

    fun normalize(t: String) = Quran.normalize(t)
}
