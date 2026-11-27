package com.alzikr.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * «الموسوعة القرآنية الشاملة» (quranpedia.app), read at the moment it is asked for through its open MCP endpoint
 * (no key, no account; the site invites apps to read its data). Nothing of it is bundled in the APK: the book texts
 * belong to their authors, so they are fetched from the site, cached on the phone for 30 days, and shown with the
 * source named and linked. Asked for by the user on 2026-11-27 («نضيف المهم ونضع زرًا لمن يريد الاستزادة»).
 */
object Qp {
    private const val ENDPOINT = "https://mcp.quranpedia.workers.dev/mcp"
    const val SITE = "https://quranpedia.app"
    private const val KEEP_MS = 30L * 24 * 3600 * 1000

    private fun dir(c: Context) = File(c.filesDir, "quranpedia").apply { mkdirs() }

    /** One tool call; the text it returns, or null when offline. Cached per tool and arguments. Blocking. */
    fun call(c: Context, tool: String, args: Map<String, Any>): String? {
        val key = (tool + "_" + args.entries.sortedBy { it.key }.joinToString("_") { "${it.key}-${it.value}" }).replace(Regex("[^A-Za-z0-9_\\-]"), "")
        val f = File(dir(c), "$key.txt")
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < KEEP_MS) return f.readText()
        val text = runCatching {
            val body = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call")
                .put("params", JSONObject().put("name", tool).put("arguments", JSONObject(args))).toString()
            val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"; conn.doOutput = true; conn.connectTimeout = 15000; conn.readTimeout = 45000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json, text/event-stream")
            conn.setRequestProperty("User-Agent", "ALzikr")
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode != 200) return@runCatching null
            var raw = conn.inputStream.bufferedReader().readText()
            // A streamed answer: the JSON is on the «data:» lines.
            if (!raw.trimStart().startsWith("{")) raw = raw.lines().filter { it.startsWith("data:") }.joinToString("") { it.removePrefix("data:").trim() }
            val res = JSONObject(raw).optJSONObject("result") ?: return@runCatching null
            if (res.optBoolean("isError")) return@runCatching null
            val content = res.optJSONArray("content") ?: JSONArray()
            (0 until content.length()).mapNotNull { content.optJSONObject(it)?.optString("text") }.joinToString("\n")
        }.getOrNull()
        if (!text.isNullOrBlank()) f.writeText(text)
        return text?.takeIf { it.isNotBlank() } ?: f.takeIf { it.exists() }?.readText()   // stale copy rather than nothing
    }

    class Book(val slug: String, val name: String)
    class Group(val title: String, val books: List<Book>)

    /** From the «aya» answer: the tafsirs (by era) and the other books (by kind) that have material on this verse. */
    fun groups(ayaText: String): Pair<List<Group>, List<Group>> {
        val tafsir = ArrayList<Group>(); val other = ArrayList<Group>()
        var into: ArrayList<Group>? = null
        val item = Regex("([^،\\[\\]]+?)\\s*\\[([A-Za-z0-9\\-]+)\\]")
        for (line in ayaText.lines()) {
            when {
                line.startsWith("## التفاسير") -> into = tafsir
                line.startsWith("## كتب لها مادة") -> into = other
                line.startsWith("## ") -> into = null
                line.startsWith("- ") && into != null -> {
                    val body = line.removePrefix("- ")
                    val cut = body.indexOf(':')
                    if (cut < 0) continue
                    val books = item.findAll(body.substring(cut + 1)).map { Book(it.groupValues[2], it.groupValues[1].trim()) }.toList()
                    if (books.isNotEmpty()) into.add(Group(body.substring(0, cut).trim(), books))
                }
            }
        }
        return tafsir to other
    }

    val riwayat = listOf("warsh" to "ورش عن نافع", "qaloon" to "قالون عن نافع", "shouba" to "شعبة عن عاصم", "bazzi" to "البزي عن ابن كثير",
        "qumbul" to "قنبل عن ابن كثير", "doori" to "الدوري عن أبي عمرو", "soosi" to "السوسي عن أبي عمرو")

    fun open(c: Context, url: String) { runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

/**
 * Under the verse's tafsir: one folded line «📚 المزيد من الموسوعة القرآنية». Opened, it offers the reasons of revelation,
 * hadiths, i'rab, qira'at and the other kinds of books that speak about this verse, the word-by-word i'rab, the verses
 * that resemble it (for memorizing), the other riwayat, and every tafsir that covers it. Each loads only when tapped.
 */
@Composable
fun QuranpediaCard(s: Int, a: Int) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    if (!open) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(14.dp)).background(P.card).clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            GText("📚 المزيد من الموسوعة القرآنية", color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("إظهار ▾", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        return
    }
    var ayaText by remember(s, a) { mutableStateOf<String?>(null) }
    var failed by remember(s, a) { mutableStateOf(false) }
    LaunchedEffect(s, a) { failed = false; ayaText = withContext(Dispatchers.IO) { Qp.call(ctx, "aya", mapOf("sura" to s, "aya" to a)) }; failed = ayaText == null }
    // What is shown: a kind of books, or one of the special views.
    var section by remember(s, a) { mutableStateOf<String?>(null) }
    var book by remember(s, a) { mutableStateOf<Qp.Book?>(null) }
    var riwaya by remember(s, a) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(14.dp)).background(P.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GText("📚 الموسوعة القرآنية", color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("إخفاء ▴", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(L.shape(8.dp)).clickable { open = false }.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        val text = ayaText
        when {
            text == null && !failed -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), color = P.accent, strokeWidth = 2.dp); Sub("  جارٍ التحميل…") }
            text == null -> Sub("تعذّر التحميل، تحقق من الإنترنت.")
            else -> {
                val (tafsirs, others) = remember(text) { Qp.groups(text) }
                // The kinds of material, as one sideways row: the most asked first.
                val order = listOf("سبب النزول", "الأحاديث الواردة في الآية", "الإعراب", "القراءات", "التدبر")
                val kinds = others.sortedBy { g -> order.indexOfFirst { g.title.startsWith(it) }.let { if (it < 0) 99 else it } }
                val tabs = kinds.map { it.title } + listOf("إعراب الكلمات", "المتشابهات", "الروايات", "التفاسير (${tafsirs.sumOf { it.books.size }})")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEach { t -> Choice(t, section == t) { section = if (section == t) null else t; book = null; riwaya = null } }
                }
                when (val sec = section) {
                    null -> Sub("اختر ما تريد: أسباب النزول، والأحاديث، والإعراب، والقراءات، والمتشابهات، وسائر التفاسير. لا يُحمَّل شيء حتى تختاره.", size = 12.sp)
                    "إعراب الكلمات" -> QpText(s, a, "iraab_tree", mapOf("sura" to s, "aya" to a))
                    "المتشابهات" -> QpText(s, a, "similar_ayat", mapOf("sura" to s, "aya" to a), empty = "لا نظائر لفظية لهذه الآية.")
                    "الروايات" -> {
                        Wrap(gap = 6.dp) { Qp.riwayat.forEach { (k, n) -> Choice(n, riwaya == k) { riwaya = k } } }
                        riwaya?.let { r -> QpText(s, a, "riwaya", mapOf("riwaya" to r, "sura" to s, "aya" to a)) }
                    }
                    else -> {
                        val list = if (sec.startsWith("التفاسير")) tafsirs else kinds.filter { it.title == sec }
                        list.forEach { g ->
                            if (list.size > 1) Sub(g.title, size = 12.sp)
                            Wrap(gap = 6.dp) { g.books.forEach { b -> Choice(b.name, book?.slug == b.slug) { book = b } } }
                        }
                        book?.let { b -> QpText(s, a, "tafsir", mapOf("book" to b.slug, "sura" to s, "aya" to a)) }
                    }
                }
            }
        }
        Text("المصدر: الموسوعة القرآنية الشاملة ↗", color = P.muted, fontSize = 11.sp,
            modifier = Modifier.clip(L.shape(6.dp)).clickable { Qp.open(ctx, "${Qp.SITE}/tafseer/saadi/sura$s-aya$a.html") }.padding(4.dp))
    }
}

/** One answer of the encyclopedia, loaded when shown, in plain readable form (its markdown made into lines). */
@Composable
private fun QpText(s: Int, a: Int, tool: String, args: Map<String, Any>, empty: String = "لا مادة لهذه الآية.") {
    val ctx = LocalContext.current
    var text by remember(tool, args) { mutableStateOf<String?>(null) }
    var failed by remember(tool, args) { mutableStateOf(false) }
    LaunchedEffect(tool, args) { failed = false; text = null; text = withContext(Dispatchers.IO) { Qp.call(ctx, tool, args) }; failed = text == null }
    val t = text
    when {
        t == null && !failed -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), color = P.accent, strokeWidth = 2.dp); Sub("  جارٍ التحميل…") }
        t == null -> Sub("تعذّر التحميل، تحقق من الإنترنت.")
        else -> {
            var link: String? = null
            val lines = ArrayList<Pair<Int, String>>()   // 0 body, 1 heading, 2 small
            val max = 24000
            var total = 0
            for (raw in t.lines()) {
                val l = raw.trim()
                if (l.isEmpty()) continue
                if (l.startsWith("الرابط:")) { link = l.removePrefix("الرابط:").trim(); continue }
                if (l.startsWith("الصفحة في الموقع:") || l.matches(Regex("^\\|[-| ]+\\|$"))) continue
                if (l.startsWith("# ")) continue   // the title repeats what the sheet already shows
                if (tool != "riwaya" && l.startsWith("﴿")) continue   // so does the verse itself (a riwaya's verse IS the answer)
                val kind = if (l.startsWith("## ")) 1 else if (l.startsWith("ملاحظة") || l.startsWith("الرقم العام")) 2 else 0
                val clean = l.removePrefix("## ").removePrefix("- ").replace("**", "")
                    .let { if (it.startsWith("|")) it.trim('|').split('|').map { c -> c.trim() }.filter { c -> c.isNotEmpty() }.joinToString(" · ") else it }
                if (total > max) { lines += 2 to "… (البقية في الموسوعة)"; break }
                total += clean.length
                lines += kind to (if (raw.trimStart().startsWith("- ")) "• $clean" else clean)
            }
            if (lines.isEmpty()) Sub(empty)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lines.forEach { (k, l) ->
                    when (k) {
                        1 -> Text(l, color = P.accent, fontSize = (14 * Data.textScale).sp, fontWeight = FontWeight.Bold, fontFamily = Fonts.naskh)
                        2 -> Sub(l, size = 11.sp)
                        else -> Text(l, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (26 * Data.textScale).sp, fontFamily = Fonts.naskh)
                    }
                }
                link?.let { u -> Text("اقرأه في الموسوعة ↗", color = P.accent, fontSize = 12.sp, modifier = Modifier.clip(L.shape(6.dp)).clickable { Qp.open(ctx, u) }.padding(4.dp)) }
            }
        }
    }
}
