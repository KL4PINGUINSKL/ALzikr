package com.alzikr.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A tafsir book for one verse in a card that opens and closes (its surah is downloaded only when opened).
 * Used for comparing tafsirs, the meanings of words (غريب القرآن) and the reflections (تدبّر وعمل).
 */
@Composable
fun TafsirCard(bookId: String, s: Int, a: Int, title: String = Tafsir.book(bookId).name, startOpen: Boolean = false) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var open by remember(bookId, s, a) { mutableStateOf(startOpen) }
    var loading by remember(bookId, s) { mutableStateOf(false) }
    var failed by remember(bookId, s) { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE") val v = Tafsir.version
    LaunchedEffect(open, bookId, s) {
        if (open && !Tafsir.has(ctx, bookId, s)) { loading = true; failed = !withContext(Dispatchers.IO) { Tafsir.fetch(ctx, bookId, s) }; loading = false }
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(14.dp)).background(P.card.seeThrough())) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (open) "▴" else "▾", color = P.muted, fontSize = 14.sp)
        }
        if (open) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
            val e = Tafsir.get(ctx, bookId, s, a)
            when {
                e != null && e.text.isNotBlank() -> {
                    if (e.to > e.from) Sub("يشمل الآيات ${Quran.digits(e.from)}–${Quran.digits(e.to)}", size = 12.sp)
                    TafsirText(e.text, (16 * Data.textScale).sp, (28 * Data.textScale).sp)
                }
                loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = P.accent, strokeWidth = 2.dp); Sub("  جارٍ التحميل…")
                }
                failed -> Text("تعذّر التحميل. اضغط لإعادة المحاولة", color = P.danger, fontSize = 13.sp, modifier = Modifier.clickable {
                    scope.launch { loading = true; failed = !withContext(Dispatchers.IO) { Tafsir.fetch(ctx, bookId, s) }; loading = false }
                })
                else -> Sub("لا شيء في هذا الكتاب لهذه الآية.", size = 12.sp)
            }
        }
    }
}

/** The tafsirs offered side by side when comparing (the chosen one is shown above them). */
val compareSet = listOf("muyassar", "saadi", "ibnkathir", "tabari", "qurtubi", "baghawi", "tanwir", "adwa", "shawkani", "mathoor")

/** Your own notes on verses (kept on the phone, included in the app's backup with the other settings). */
object VerseNotes {
    private fun key(s: Int, a: Int) = "vnote_${s}_$a"
    fun get(s: Int, a: Int): String = Store.prefs.getString(key(s, a), "") ?: ""
    fun set(s: Int, a: Int, text: String) {
        val t = text.trim()
        Store.prefs.edit().apply { if (t.isEmpty()) remove(key(s, a)) else putString(key(s, a), t) }.apply(); Store.bump()
    }
    /** All notes, in mushaf order. */
    fun all(): List<Triple<Int, Int, String>> = Store.prefs.all.mapNotNull { (k, v) ->
        if (!k.startsWith("vnote_")) return@mapNotNull null
        val p = k.removePrefix("vnote_").split('_'); val s = p.getOrNull(0)?.toIntOrNull(); val a = p.getOrNull(1)?.toIntOrNull()
        if (s == null || a == null || v !is String) null else Triple(s, a, v)
    }.sortedWith(compareBy({ it.first }, { it.second }))
}

@Composable
fun VerseNoteDialog(s: Int, a: Int, close: () -> Unit) {
    var text by remember { mutableStateOf(VerseNotes.get(s, a)) }
    AlertDialog(onDismissRequest = close, containerColor = P.surface,
        title = { GText("ملاحظتي على ${Quran.surahs[s - 1].name} ${Quran.digits(a)}", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = { OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 4, placeholder = { Text("ما وقفت عنده في هذه الآية…") }) },
        confirmButton = { TextButton({ VerseNotes.set(s, a, text); close() }) { GText("حفظ", color = P.accent) } },
        dismissButton = { TextButton(close) { GText("إلغاء", color = P.muted) } })
}

/** Searching the tafsirs that are on the phone (no internet needed). */
object TafsirSearch {
    data class Hit(val book: String, val s: Int, val a: Int, val snippet: String)
    private fun norm(t: String) = t.replace(Regex("[\\u064B-\\u0652\\u0670\\u0640]"), "").replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ة', 'ه').replace('ى', 'ي')
    fun run(c: Context, q: String, limit: Int = 200): List<Hit> {
        val nq = norm(q.trim()); if (nq.length < 2) return emptyList()
        val out = ArrayList<Hit>()
        for (b in Tafsir.books) {
            val dir = java.io.File(c.filesDir, "tafsir/${b.id}"); if (!dir.isDirectory) continue
            for (f in dir.listFiles().orEmpty().filter { it.name.endsWith(".txt") }.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: 0 }) {
                val s = f.nameWithoutExtension.toIntOrNull() ?: continue
                f.forEachLine { l ->
                    if (out.size >= limit) return@forEachLine
                    val p = l.split('\t', limit = 2); if (p.size < 2 || p[1].startsWith("=")) return@forEachLine
                    val text = p[1].replace("\\n", " "); val n = norm(text); val i = n.indexOf(nq)
                    if (i >= 0) out += Hit(b.id, s, p[0].toIntOrNull() ?: 0, n.substring((i - 60).coerceAtLeast(0), (i + nq.length + 90).coerceAtMost(n.length)))
                }
                if (out.size >= limit) return out
            }
        }
        return out
    }
}

@Composable
fun TafsirSearchPanel() {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<TafsirSearch.Hit>?>(null) }
    LaunchedEffect(q) { kotlinx.coroutines.delay(350); hits = if (q.trim().length < 2) null else withContext(Dispatchers.IO) { TafsirSearch.run(ctx, q) } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("ابحث في التفاسير المحمّلة على هاتفك…") })
        hits?.let { h ->
            Sub(if (h.isEmpty()) "لا نتائج في التفاسير المحمّلة." else "${Quran.digits(h.size)} نتيجة", size = 12.sp)
            h.take(60).forEach { x ->
                Column(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.surface).clickable { Nav.openQuran(x.s, x.a) }.padding(12.dp)) {
                    Sub("${Tafsir.book(x.book).name} · ${Quran.surahs[x.s - 1].name} ${Quran.digits(x.a)}", size = 12.sp, color = P.gold)
                    Text("…${x.snippet}…", color = P.ink, fontSize = 14.sp, fontFamily = Fonts.naskh, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** A verse for today with an invitation to reflect (the same verse all day). */
@Composable
fun DailyTadabburCard() {
    if (!Quran.ready) return
    val day = java.time.LocalDate.now().toEpochDay()
    // Walk the mushaf a verse at a time through the year, skipping very short verses.
    val (s, a) = remember(day) {
        var i = Math.floorMod(day * 97, 6236L).toInt(); var r: Pair<Int, Int>? = null
        while (r == null) { val ay = Quran.ayahs.getOrNull(i); if (ay != null && ay.text.length > 40) r = ay.s to ay.a else i = (i + 1) % 6236 }
        r
    }
    val ay = Quran.at(s, a) ?: return
    Panel(pad = 14.dp) {
        Text("🌱 آية للتدبّر اليوم", color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(ay.text, color = P.ink, fontSize = 20.sp, fontFamily = Fonts.quran, lineHeight = 36.sp, modifier = Modifier.padding(top = 6.dp))
        Sub("${Quran.surahs[s - 1].name} ${Quran.digits(a)}", size = 12.sp)
        Action("تدبّرها: التفسير ووقفات «تدبّر وعمل»", Modifier.fillMaxWidth().padding(top = 8.dp), filled = false) { Nav.openQuran(s, a, sheet = true, tadabbur = true) }
    }
}
