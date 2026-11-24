package com.alzikr.app

import android.content.Context
import android.util.JsonReader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mushaf editions for the reader: the built-in text, or a text from quran.com downloaded once (whole Quran, 1.5–4 MB):
 * the Uthmani script, the simple (إملائي) script, and the Uthmani script with the tajweed rules in colour.
 * Also: showing or hiding the diacritics.
 */
object Mushafs {
    /** [field]: a quran.com text field; [fawaz]: an edition of fawazahmed0/quran-api (other readings, IndoPak), same verse numbering. */
    data class Edition(val id: String, val name: String, val about: String, val field: String?, val fawaz: String? = null)
    val editions = listOf(
        Edition("app", "مصحف التطبيق", "الرسم العثماني المضمّن، يعمل بلا إنترنت", null),
        Edition("tajweed", "مصحف التجويد الملوّن", "الرسم العثماني مع تلوين أحكام التجويد", "uthmani_tajweed"),
        Edition("uthmani", "الرسم العثماني (quran.com)", "رسم مصحف المدينة", "uthmani"),
        Edition("imlaei", "الرسم الإملائي", "بالإملاء المعتاد، أسهل لمن يتعلّم القراءة", "imlaei"),
        Edition("indopak", "الرسم الهندي", "كما في مصاحف شبه القارة الهندية", null, "ara-quranindopak"),
        // Other readings (القراءات), with their own spelling and marks.
        Edition("warsh", "رواية ورش عن نافع", "المنتشرة في المغرب العربي وغرب إفريقيا", null, "ara-quranwarsh"),
        Edition("qaloon", "رواية قالون عن نافع", "المنتشرة في ليبيا وتونس", null, "ara-quranqaloon"),
        Edition("shouba", "رواية شعبة عن عاصم", "الرواية الثانية عن عاصم", null, "ara-quranshouba"),
        Edition("doori", "رواية الدوري عن أبي عمرو", "المنتشرة في السودان", null, "ara-qurandoori"),
        Edition("soosi", "رواية السوسي عن أبي عمرو", "عن أبي عمرو البصري", null, "ara-quransoosi"),
        Edition("bazzi", "رواية البزي عن ابن كثير", "قراءة أهل مكة", null, "ara-quranbazzi"),
        Edition("qumbul", "رواية قنبل عن ابن كثير", "قراءة أهل مكة", null, "ara-quranqumbul"),
    )
    var current: String
        get() = Store.prefs.getString("mushaf_ed", "app") ?: "app"
        set(v) { Store.prefs.edit().putString("mushaf_ed", v).apply(); Store.bump() }
    var tashkeel: Boolean
        get() = Store.prefs.getBoolean("mushaf_tashkeel", true)
        set(v) { Store.prefs.edit().putBoolean("mushaf_tashkeel", v).apply(); Store.bump() }
    var busy by mutableStateOf<String?>(null)
    var version by mutableIntStateOf(0)

    private fun file(c: Context, id: String) = File(c.filesDir, "mushaf/$id.txt")
    fun has(c: Context, id: String) = id == "app" || file(c, id).exists()
    private val cache = HashMap<String, Map<String, String>>()

    private fun map(c: Context, id: String): Map<String, String>? {
        cache[id]?.let { return it }
        val f = file(c, id); if (!f.exists()) return null
        val m = HashMap<String, String>(6300)
        f.forEachLine { l -> val p = l.split('\t', limit = 2); if (p.size == 2) m[p[0]] = p[1] }
        return m.also { cache[id] = it }
    }

    /** Downloads a whole edition. Blocking: call off the main thread. */
    fun fetch(c: Context, id: String): Boolean {
        val ed = editions.firstOrNull { it.id == id } ?: return false
        ed.fawaz?.let { return fetchFawaz(c, id, it) }
        val field = ed.field ?: return true
        return try {
            val conn = URL("https://api.quran.com/api/v4/quran/verses/$field").openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000; conn.readTimeout = 90_000
            if (conn.responseCode != 200) { conn.disconnect(); return false }
            val rows = ArrayList<Pair<String, String>>(6300)
            JsonReader(conn.inputStream.bufferedReader()).use { r ->
                r.beginObject()
                while (r.hasNext()) {
                    if (r.nextName() != "verses") { r.skipValue(); continue }
                    r.beginArray()
                    while (r.hasNext()) {
                        var key = ""; var text = ""
                        r.beginObject()
                        while (r.hasNext()) when (r.nextName()) { "verse_key" -> key = r.nextString(); "text_$field" -> text = r.nextString(); else -> r.skipValue() }
                        r.endObject()
                        if (key.isNotEmpty()) rows += key to text.replace('\n', ' ').replace('\t', ' ')
                    }
                    r.endArray()
                }
                r.endObject()
            }
            conn.disconnect()
            if (rows.size < 6000) return false
            val f = file(c, id).apply { parentFile?.mkdirs() }
            val tmp = File(f.path + ".part")
            tmp.bufferedWriter().use { w -> rows.forEach { (k, t) -> w.write("$k\t$t\n") } }
            tmp.renameTo(f).also { cache.remove(id); version++ }
        } catch (e: Exception) { false }
    }

    /** {"quran":[{"chapter":1,"verse":1,"text":"…"}]} from jsDelivr, or GitHub when jsDelivr refuses a large file. */
    private fun fetchFawaz(c: Context, id: String, ed: String): Boolean {
        for (u in listOf("https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$ed.min.json", "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/$ed.min.json")) {
            try {
                val conn = URL(u).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000; conn.readTimeout = 90_000
                if (conn.responseCode != 200) { conn.disconnect(); continue }
                val rows = ArrayList<Pair<String, String>>(6300)
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
                            if (s > 0 && a > 0) rows += "$s:$a" to text.replace('\n', ' ').replace('\t', ' ')
                        }
                        r.endArray()
                    }
                    r.endObject()
                }
                conn.disconnect()
                if (rows.size < 6000) continue
                val f = file(c, id).apply { parentFile?.mkdirs() }
                val tmp = File(f.path + ".part")
                tmp.bufferedWriter().use { w -> rows.forEach { (k, t) -> w.write("$k\t$t\n") } }
                if (tmp.renameTo(f)) { cache.remove(id); version++; return true }
            } catch (e: Exception) { }
        }
        return false
    }

    // Tajweed colours (the usual mushaf-al-tajweed palette, as quran.com uses).
    val tajweedColors = linkedMapOf(
        "ghunnah" to (Color(0xFFFF7E1E) to "غنّة"),
        "ikhafa" to (Color(0xFF9400A8) to "إخفاء"), "ikhafa_shafawi" to (Color(0xFFD500B7) to "إخفاء شفوي"),
        "iqlab" to (Color(0xFF26BFFD) to "إقلاب"),
        "idgham_ghunnah" to (Color(0xFF169777) to "إدغام بغنّة"), "idgham_wo_ghunnah" to (Color(0xFF169200) to "إدغام بلا غنّة"),
        "idgham_shafawi" to (Color(0xFF58B800) to "إدغام شفوي"), "idgham_mutajanisayn" to (Color(0xFFA1A1A1) to "إدغام متجانسين"),
        "idgham_mutaqaribayn" to (Color(0xFFA1A1A1) to "إدغام متقاربين"),
        "qalaqah" to (Color(0xFFDD0008) to "قلقلة"),
        "madda_normal" to (Color(0xFF537FFF) to "مدّ طبيعي"), "madda_permissible" to (Color(0xFF4050FF) to "مدّ جائز"),
        "madda_obligatory" to (Color(0xFF2144C1) to "مدّ واجب"), "madda_necessary" to (Color(0xFF000EBC) to "مدّ لازم"),
        "ham_wasl" to (Color(0xFFAAAAAA) to "همزة وصل"), "laam_shamsiyah" to (Color(0xFFAAAAAA) to "لام شمسية"), "slnt" to (Color(0xFFAAAAAA) to "لا يُنطق"),
    )
    private val tag = Regex("""<tajweed class=([a-z_]+)>(.*?)</tajweed>|<span class=end>.*?</span>|<[^>]+>""")
    private val marks = Regex("[\\u064B-\\u0652\\u0670\\u06D6-\\u06ED]")

    /** The verse's text in the chosen edition (and without diacritics when they are hidden). */
    fun verse(c: Context, a: Quran.Ayah): AnnotatedString {
        val id = current
        val raw = if (id == "app") null else map(c, id)?.get("${a.s}:${a.a}")
        val bare = !tashkeel
        fun t(x: String) = if (bare) x.replace(marks, "") else x
        if (raw == null) return AnnotatedString(t(a.text))
        if (id != "tajweed") return AnnotatedString(t(raw).trim())
        return buildAnnotatedString {
            var at = 0
            for (m in tag.findAll(raw)) {
                append(t(raw.substring(at, m.range.first)))
                val cls = m.groupValues[1]
                if (cls.isNotEmpty()) { val col = tajweedColors[cls]?.first; if (col != null) withStyle(SpanStyle(color = col)) { append(t(m.groupValues[2])) } else append(t(m.groupValues[2])) }
                at = m.range.last + 1
            }
            append(t(raw.substring(at)).trimEnd())
        }
    }
}
