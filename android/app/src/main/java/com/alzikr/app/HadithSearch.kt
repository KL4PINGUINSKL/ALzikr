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
import androidx.compose.material3.Text
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

    /** Every word of the query must appear (in any order), marks and letter forms ignored. */
    private fun matcher(q: String): (String) -> Boolean {
        val words = Quran.normalize(q).split(' ').filter { it.length >= 2 }
        return { text -> val n = Quran.normalize(text); words.isNotEmpty() && words.all { n.contains(it) } }
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
    var books by remember { mutableStateOf<List<HadithSearch.Hit>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var openSharh by rememberSaveable { mutableStateOf<String?>(null) }
    var openAt by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    openSharh?.let { SharhOpen(it) { openSharh = null }; return }
    openAt?.let { (b, n) -> HadithReader(b, 0, { openAt = null }, focus = n); return }
    fun run() {
        if (query.isBlank() || busy) return
        keyboard?.hide()
        busy = true; searched = true
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
            item { Hint("hsearch", "🔍", "اكتب كلمات من الحديث (مثل: إنما الأعمال) ثم اضغط «ابحث». يبحث في موسوعة الأحاديث المشروحة وفي كل كتب الحديث على هاتفك، ولا يهم التشكيل.") }
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
            if (!busy && searched && sharh.isEmpty() && books.isEmpty()) item { Sub("لا نتائج. جرّب كلمات أقل أو أقصر، أو تأكد من الإملاء.", Modifier.padding(8.dp)) }
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
