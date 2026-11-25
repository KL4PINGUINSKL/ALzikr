package com.alzikr.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/**
 * Searching for a hadith everywhere at once: the explained-hadith encyclopedia (an index of all its
 * titles, fetched once and kept) and the full text of every hadith book on the phone.
 */
object HadithSearch {
    data class Hit(val book: String, val bookTitle: String, val number: String, val text: String, val sharhId: String? = null)

    private fun indexFile(c: Context) = File(c.filesDir, "sharh/index.json")
    fun hasIndex(c: Context) = indexFile(c).exists()

    /** id → title of every hadith in HadeethEnc (one request per main category). Blocking. */
    fun sharhIndex(c: Context, refresh: Boolean = false): List<Sharh.Brief> {
        val f = indexFile(c)
        if (!refresh && f.exists() && System.currentTimeMillis() - f.lastModified() < 14 * 86_400_000L) return read(f)
        val roots = Sharh.categories(c).filter { it.parent == null }
        if (roots.isEmpty()) return if (f.exists()) read(f) else emptyList()
        val all = LinkedHashMap<String, String>()
        for (r in roots) {
            val text = runCatching {
                val conn = java.net.URL("https://hadeethenc.com/api/v1/hadeeths/list/?language=ar&category_id=${r.id}&page=1&per_page=5000").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15_000; conn.readTimeout = 60_000
                try { if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null } finally { conn.disconnect() }
            }.getOrNull() ?: continue
            val a = runCatching { org.json.JSONObject(text).getJSONArray("data") }.getOrNull() ?: continue
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { all[it.optString("id")] = it.optString("title") }
        }
        if (all.isEmpty()) return if (f.exists()) read(f) else emptyList()
        f.parentFile?.mkdirs()
        f.writeText(JSONArray().also { arr -> all.forEach { (id, t) -> arr.put(JSONArray().put(id).put(t)) } }.toString())
        return all.map { Sharh.Brief(it.key, it.value) }
    }

    private fun read(f: File): List<Sharh.Brief> = runCatching {
        val a = JSONArray(f.readText()); (0 until a.length()).map { a.getJSONArray(it).let { x -> Sharh.Brief(x.getString(0), x.getString(1)) } }
    }.getOrDefault(emptyList())

    /** Words people add around what they look for: «حديث إنما الأعمال», «ابحث عن …». */
    private val fillers = setOf("حديث", "الحديث", "احاديث", "الاحاديث", "ابحث", "عن", "نص", "شرح", "رقم")
    /** The words that matter in a query: no fillers, quotes or punctuation; marks and letter forms ignored. */
    fun words(q: String): List<String> {
        val all = Quran.normalize(Quran.latinDigits(q)).replace(Regex("[«»\"'()،,.؟?!:]"), " ").split(' ').filter { it.length >= 2 }
        return all.filter { it !in fillers }.ifEmpty { all }
    }
    /** Every word of the query must appear (in any order), marks and letter forms ignored. */
    fun matcher(q: String): (String) -> Boolean {
        val words = words(q)
        return { text -> val n = Quran.normalize(text); words.isNotEmpty() && words.all { n.contains(it) } }
    }

    /** A book the query names, with a hadith number if one is given: «صحيح البخاري», «مسلم 8», «الأربعين النووية». */
    fun books(c: Context, q: String): List<Pair<Hadith.Info, String?>> {
        val w = words(q)
        val num = w.firstOrNull { it.toIntOrNull() != null }
        val name = w.filter { it.toIntOrNull() == null && it !in setOf("صحيح", "سنن", "كتاب", "جامع") }
        if (name.isEmpty()) return emptyList()
        fun bare(t: String) = t.removePrefix("ال").replace("ين", "ون")
        return Hadith.books.filter { Hadith.available(c, it.id) }.filter { b ->
            val t = Quran.normalize(b.title + " " + b.author).split(' ').map { bare(it) }
            name.all { n -> t.any { it.startsWith(bare(n)) && bare(n).length >= 2 } }
        }.map { it to num }
    }

    /** Letters and spaces only (no marks, no punctuation), one form of alef / ya / ta marbuta. */
    private fun letters(t: String) = Quran.normalize(t).replace(Regex("[^\u0621-\u064A\\s]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Each title as words, its distinctive words, and the words too common to tell hadiths apart. */
    private class Titles(val list: List<Triple<String, List<String>, List<String>>>, val common: Set<String>)
    private var titles: Titles? = null

    private fun titles(c: Context): Titles = titles ?: run {
        val all = sharhIndex(c).map { it.id to letters(it.title).split(' ').filter { w -> w.isNotEmpty() } }
        // Words in more than 1% of the titles («رسول», «الله», «صلي», «قال», «كان»…) say nothing about which hadith it is.
        val df = HashMap<String, Int>()
        all.forEach { (_, ws) -> ws.toSet().forEach { df[it] = (df[it] ?: 0) + 1 } }
        val common = df.filter { it.value > all.size / 100 || it.key.length < 2 }.keys
        Titles(all.map { (id, ws) -> Triple(id, ws, ws.filter { it !in common }) }.filter { it.third.isNotEmpty() }, common)
    }.also { if (it.list.isNotEmpty()) titles = it }

    /**
     * The explanation for a hadith from its text. The encyclopedia's titles are the hadith's own key words, so a title
     * fits when its DISTINCTIVE words are in the text and several of them come in the same order. (It used to compare
     * the first words only, so a title opening with «سمعت رسول الله صلى الله عليه وسلم يقول» fitted any hadith that
     * opened the same way, and «إنما الأعمال بالنيات» opened another hadith's explanation.) 41 of the 42 Nawawi match,
     * and a hadith with no fitting title gets none rather than a wrong one. Blocking.
     */
    fun findSharh(c: Context, hadithText: String): String? {
        val t = titles(c)
        val tw = letters(hadithText).split(' ').filter { it.isNotEmpty() }
        val pos = HashMap<String, MutableList<Int>>()
        tw.forEachIndexed { k, w -> pos.getOrPut(w) { ArrayList() } += k }
        var best: String? = null; var bestScore = 0.0
        for ((id, all, distinct) in t.list) {
            val hit = distinct.count { it in pos }
            if (hit < 1 || hit < 2 && all.size > 2) continue
            val cov = hit.toDouble() / distinct.size
            // The longest stretch of the title found word for word in the text, and how many distinctive words it holds.
            var run = 0; var runDistinct = 0
            for (a in all.indices) for (p in pos[all[a]] ?: continue) {
                var n = 1
                while (a + n < all.size && p + n < tw.size && tw[p + n] == all[a + n]) n++
                val d = (a until a + n).count { all[it] !in t.common }
                if (d > runDistinct || d == runDistinct && n > run) { runDistinct = d; run = n }
            }
            val whole = run == all.size && all.size >= 2 && runDistinct >= 1
            if (!(whole || cov >= 0.6 && runDistinct >= 3 || cov >= 0.999 && hit >= 2)) continue
            val score = cov * 10 + runDistinct
            if (score > bestScore) { bestScore = score; best = id }
        }
        return best
    }

    /** A hadith with an explanation for each day (from the kept index; null until it exists). */
    fun ofTheDay(c: Context, date: java.time.LocalDate = java.time.LocalDate.now()): Sharh.Brief? {
        if (!hasIndex(c)) return null
        val all = sharhIndex(c).filter { it.title.length in 25..160 }
        if (all.isEmpty()) return null
        // Each person's own random order, none repeating until all have come (DailyBag).
        if (date == java.time.LocalDate.now()) runCatching { return all[DailyBag.index("hadith", all.size)] }
        return all[Math.floorMod(date.toEpochDay() * 31 + Data.dailyShift("hadith", date) * 7919L, all.size.toLong()).toInt()]
    }

    fun searchSharh(c: Context, q: String, limit: Int = 100): List<Hit> {
        val m = matcher(q)
        return sharhIndex(c).asSequence().filter { m(it.title) }.take(limit).map { Hit("sharh", "موسوعة الأحاديث المشروحة", "", it.title, it.id) }.toList()
    }

    /** The books on the phone, line by line (Bukhari takes a second or two). Blocking. */
    fun searchBooks(c: Context, q: String, limit: Int = 200): List<Hit> {
        val m = matcher(q)
        val hits = ArrayList<Hit>()
        for (b in Hadith.books) {
            if (!Hadith.available(c, b.id)) continue
            val input = runCatching { if (b.path == null) c.assets.open("hadith/${b.id}.txt") else File(File(c.filesDir, "hadith"), "${b.id}.txt").inputStream() }.getOrNull() ?: continue
            input.bufferedReader().useLines { lines ->
                for (l in lines) {
                    if (hits.size >= limit) break
                    if (l.isEmpty() || l[0] == '#' || l[0] == '@') continue
                    val p = l.split('\t', limit = 3); if (p.size < 3) continue
                    if (m(p[2])) hits += Hit(b.id, b.title, p[0], p[2].replace("\\n", "\n"))
                }
            }
            if (hits.size >= limit) break
        }
        return hits
    }
}

/** One search box for every hadith: the explained encyclopedia and the books on the phone. */
@Composable
fun HadithSearchPage(initial: String? = null, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf(initial ?: "") }
    var where by rememberSaveable { mutableStateOf(0) }       // 0 everywhere, 1 explained, 2 my books
    var busy by remember { mutableStateOf(false) }
    var sharh by remember { mutableStateOf<List<HadithSearch.Hit>>(emptyList()) }
    var named by remember { mutableStateOf<List<Pair<Hadith.Info, String?>>>(emptyList()) }
    var openBook by rememberSaveable { mutableStateOf<String?>(null) }
    var books by remember { mutableStateOf<List<HadithSearch.Hit>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var openSharh by rememberSaveable { mutableStateOf<String?>(null) }
    var openAt by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    openSharh?.let { SharhOpen(it) { openSharh = null }; return }
    openAt?.let { (b, n) -> HadithReader(b, 0, { openAt = null }, focus = n); return }
    openBook?.let { b -> HadithReader(b, 0, { openBook = null }); return }
    fun run() {
        if (query.isBlank() || busy) return
        keyboard?.hide()
        busy = true; searched = true
        named = HadithSearch.books(ctx, query)
        scope.launch {
            sharh = if (where != 2) withContext(Dispatchers.IO) { HadithSearch.searchSharh(ctx, query) } else emptyList()
            books = if (where != 1) withContext(Dispatchers.IO) { HadithSearch.searchBooks(ctx, query) } else emptyList()
            busy = false
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (!initial.isNullOrBlank()) run() }
    val onPhone = Hadith.books.filter { Hadith.available(ctx, it.id) }
    Subpage("البحث عن حديث", back) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Hint("hsearch", "🔍", "اكتب كلمات من الحديث (مثل: إنما الأعمال) أو اسم كتاب (مثل: صحيح مسلم، أو مسلم 8) ثم اضغط «ابحث». يبحث في موسوعة الأحاديث المشروحة وفي كل كتب الحديث على هاتفك، ولا يهم التشكيل.") }
            item {
                OutlinedTextField(query, { query = it }, placeholder = { Text("كلمات من الحديث…") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { run() }))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Choice("في كل مكان", where == 0) { where = 0 }
                    Choice("المشروحة", where == 1) { where = 1 }
                    Choice("كتبي", where == 2) { where = 2 }
                }
                Action(if (busy) "جارٍ البحث…" else "🔍 ابحث", Modifier.fillMaxWidth().padding(top = 8.dp)) { run() }
                Sub("الكتب التي يبحث فيها: " + onPhone.joinToString("، ") { it.title } + ". نزّل كتبًا أخرى من قسم «الحديث» لتشملها.", Modifier.padding(top = 6.dp), size = 11.sp)
            }
            if (busy) item { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent) } }
            if (named.isNotEmpty()) items(named, key = { "k" + it.first.id }) { (b, n) ->
                Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.accent.copy(alpha = .12f))
                    .clickable { if (n != null) openAt = b.id to n else openBook = b.id }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (n != null) "${b.title} · رقم ${Quran.digits(n.toInt())}" else b.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = Fonts.naskh)
                        Sub(b.author, size = 12.sp)
                    }
                    Text("افتح ›", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!busy && searched && sharh.isEmpty() && books.isEmpty() && named.isEmpty()) item { Sub("لا نتائج. جرّب كلمات أقل أو أقصر، أو تأكد من الإملاء.", Modifier.padding(8.dp)) }
            if (sharh.isNotEmpty()) {
                item { Title("في موسوعة الأحاديث المشروحة (${sharh.size})", Modifier.padding(top = 8.dp)) }
                items(sharh, key = { "s" + it.sharhId }) { h ->
                    Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card).clickable { openSharh = h.sharhId }.padding(14.dp)) {
                        Text(h.text, color = P.ink, fontSize = 16.sp, fontFamily = Fonts.naskh, lineHeight = 26.sp)
                        Sub("💡 مع الشرح والدرجة", size = 12.sp, color = P.gold)
                    }
                }
            }
            if (books.isNotEmpty()) {
                item { Title("في كتب الحديث (${books.size}${if (books.size >= 200) "+" else ""})", Modifier.padding(top = 8.dp)) }
                items(books, key = { "b" + it.book + it.number + it.text.length }) { h ->
                    Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, P.line, L.shape(14.dp))
                        .clickable { openAt = h.book to h.number }.padding(14.dp)) {
                        Text(h.text, color = P.ink, fontSize = 15.sp, fontFamily = Fonts.naskh, lineHeight = 25.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        Text("${h.bookTitle} · رقم ${h.number}", color = P.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
