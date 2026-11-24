package com.alzikr.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Memorizing the Quran: a few new verses each day, and spaced review of what was learned
 * (after 1, 3, 7, 14, 30 and 60 days) so it stays. Starts from the end of the Quran (Juz 'Amma)
 * or from the beginning, or from any surah.
 */
object Hifz {
    /** One learned portion: surah [s], verses [from]..[to], learned on [day], review [level]. */
    data class Chunk(val id: Long, val s: Int, val from: Int, val to: Int, val day: String, val level: Int, val due: String)
    private val gaps = intArrayOf(1, 3, 7, 14, 30, 60)

    private val sp get() = Store.prefs
    var started: Boolean
        get() = sp.getBoolean("hifz_on", false)
        set(v) { sp.edit().putBoolean("hifz_on", v).apply(); Store.bump() }
    /** true = from An-Nas back toward Al-Baqarah (the usual way to begin). */
    var backward: Boolean
        get() = sp.getBoolean("hifz_back", true)
        set(v) { sp.edit().putBoolean("hifz_back", v).apply(); Store.bump() }
    var perDay: Int
        get() = sp.getInt("hifz_per_day", 5)
        set(v) { sp.edit().putInt("hifz_per_day", v).apply(); Store.bump() }
    /** The next verse to learn. */
    var pos: Pair<Int, Int>
        get() = sp.getString("hifz_pos", null)?.split(':')?.let { it[0].toInt() to it[1].toInt() } ?: (if (backward) 114 to 1 else 1 to 1)
        set(v) { sp.edit().putString("hifz_pos", "${v.first}:${v.second}").apply(); Store.bump() }

    fun chunks(): List<Chunk> = runCatching {
        val a = JSONArray(sp.getString("hifz_chunks", "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Chunk(o.getLong("id"), o.getInt("s"), o.getInt("f"), o.getInt("t"), o.getString("d"), o.getInt("l"), o.getString("due")) } }
    }.getOrDefault(emptyList())
    private fun save(l: List<Chunk>) {
        val a = JSONArray(); l.forEach { a.put(JSONObject().put("id", it.id).put("s", it.s).put("f", it.from).put("t", it.to).put("d", it.day).put("l", it.level).put("due", it.due)) }
        sp.edit().putString("hifz_chunks", a.toString()).apply(); Store.bump()
    }

    /** Today's new verses: from [pos], [perDay] verses, not past the surah's end. Null when everything is done. */
    fun today(): Triple<Int, Int, Int>? {
        val (s, a) = pos
        if (s !in 1..114) return null
        val n = Quran.surahs.getOrNull(s - 1)?.count ?: return null
        return Triple(s, a, (a + perDay - 1).coerceAtMost(n))
    }
    fun learnedToday() = chunks().any { it.day == LocalDate.now().toString() }

    /** Mark today's verses learned and move on to the next ones (the next surah after the last verse). */
    fun learn(s: Int, from: Int, to: Int) {
        val today = LocalDate.now()
        save(chunks() + Chunk(System.currentTimeMillis(), s, from, to, today.toString(), 0, today.plusDays(gaps[0].toLong()).toString()))
        val n = Quran.surahs[s - 1].count
        pos = when {
            to < n -> s to to + 1
            backward -> (s - 1) to 1
            else -> (s + 1) to 1
        }
    }
    /** Undo: forget the last learned portion and go back to it. */
    fun undoLast() {
        val l = chunks(); val last = l.maxByOrNull { it.id } ?: return
        save(l - last); pos = last.s to last.from
    }
    fun due(): List<Chunk> = chunks().filter { !LocalDate.parse(it.due).isAfter(LocalDate.now()) }.sortedBy { it.due }
    /** Reviewed: [good] moves it to a longer gap; otherwise back to tomorrow. */
    fun review(c: Chunk, good: Boolean) {
        val lvl = if (good) (c.level + 1).coerceAtMost(gaps.size - 1) else 0
        runCatching { Progress.record(Progress.Kind.STUDY, "hz:${c.id}") }
        save(chunks().map { if (it.id == c.id) it.copy(level = lvl, due = LocalDate.now().plusDays(gaps[lvl].toLong()).toString()) else it })
    }
    /** Surahs whose every verse is in a learned portion. */
    fun surahsDone(): List<Int> {
        val by = chunks().groupBy { it.s }
        return by.filter { (s, l) -> val n = Quran.surahs.getOrNull(s - 1)?.count ?: return@filter false; (1..n).all { v -> l.any { v in it.from..it.to } } }.keys.sorted()
    }
    fun versesDone() = chunks().sumOf { it.to - it.from + 1 }
    fun reset() { sp.edit().remove("hifz_chunks").remove("hifz_pos").putBoolean("hifz_on", false).apply(); Store.bump() }
    fun label(s: Int, from: Int, to: Int) = "سورة ${Quran.surahs[s - 1].name} · ${if (from == to) "الآية ${Quran.digits(from)}" else "الآيات ${Quran.digits(from)}–${Quran.digits(to)}"}"
}

@Composable
fun HifzPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    if (!Quran.ready) return
    var reading by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var viewing by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var reviewing by remember { mutableStateOf<Hifz.Chunk?>(null) }
    var pickSurah by remember { mutableStateOf(false) }
    reading?.let { (s, f, t) -> HifzReader(s, f, t, review = null) { reading = null }; return }
    viewing?.let { (s, f, t) -> HifzReader(s, f, t, review = null, learn = false) { viewing = null }; return }
    reviewing?.let { c -> HifzReader(c.s, c.from, c.to, review = c) { reviewing = null }; return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint("hifz", "🧠", "كل يوم آيات قليلة جديدة، ثم مراجعة ما حفظته بعد يوم، ثم ٣ أيام، ثم أسبوع، ثم أسبوعين، ثم شهر… حتى يثبت. اضغط «حفظتها» بعد أن تحفظ، و«أتقنتها» أو «أعدها» بعد كل مراجعة.")
        if (!Hifz.started) {
            Panel(color = P.card, pad = 16.dp) {
                Title("ابدأ برنامج الحفظ")
                Sub("من أين تبدأ؟", Modifier.padding(top = 10.dp))
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Choice("من جزء عمّ (الناس ← البقرة)", Hifz.backward) { Hifz.backward = true; Hifz.pos = 114 to 1 }
                    Choice("من الفاتحة", !Hifz.backward) { Hifz.backward = false; Hifz.pos = 1 to 1 }
                }
                Choice("أو اختر سورة: سورة ${Quran.surahs[Hifz.pos.first - 1].name}", false, Modifier.padding(top = 8.dp)) { pickSurah = true }
                Sub("كم آية كل يوم؟", Modifier.padding(top = 10.dp))
                Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 5, 7, 10, 15).forEach { n -> Choice("$n آيات", Hifz.perDay == n) { Hifz.perDay = n } }
                }
                Action("ابدأ على بركة الله", Modifier.fillMaxWidth().padding(top = 14.dp)) { Hifz.started = true }
            }
        } else {
            // Today's new verses.
            val t = Hifz.today()
            Panel(color = P.accent.copy(alpha = .12f), pad = 16.dp) {
                Sub("حفظ اليوم", color = P.gold, size = 12.sp)
                if (t == null) Text("أتممت ما اخترته، بارك الله فيك", color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                else {
                    Text(Hifz.label(t.first, t.second, t.third), color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (Hifz.learnedToday()) Sub("✓ حفظت اليوم، وهذه الآيات التالية إن أردت الزيادة", color = P.accent)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Action("📖 احفظها", Modifier.weight(1f)) { reading = t }
                        Action("✓ حفظتها", Modifier.weight(1f), filled = false) { Hifz.learn(t.first, t.second, t.third); Haptics.tap(ctx, true) }
                    }
                }
            }
            // Review.
            val due = Hifz.due()
            Panel(pad = 14.dp) {
                Title("المراجعة اليوم" + if (due.isNotEmpty()) " (${due.size})" else "")
                if (due.isEmpty()) Sub("لا مراجعة اليوم. ✓", Modifier.padding(top = 6.dp))
                due.forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(12.dp)).background(P.card).clickable { reviewing = c }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Hifz.label(c.s, c.from, c.to), color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Sub("المراجعة رقم ${c.level + 1}", size = 11.sp)
                        }
                        Text("راجع ›", color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            // What is memorized.
            val surahs = Hifz.surahsDone()
            Panel(pad = 14.dp) {
                Title("محفوظاتك")
                Sub("${Quran.digits(Hifz.versesDone())} آية · ${Quran.digits(surahs.size)} سورة كاملة", Modifier.padding(top = 4.dp))
                if (surahs.isNotEmpty()) Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    surahs.forEach { s -> Choice("✓ " + Quran.surahs[s - 1].name, true) { viewing = Triple(s, 1, Quran.surahs[s - 1].count) } }
                }
            }
            // Settings.
            Panel(pad = 14.dp) {
                Sub("كم آية كل يوم؟")
                Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 5, 7, 10, 15).forEach { n -> Choice("$n آيات", Hifz.perDay == n) { Hifz.perDay = n } }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("اختر سورة أخرى", false) { pickSurah = true }
                    if (Hifz.chunks().isNotEmpty()) Choice("↺ تراجع عن آخر حفظ", false) { Hifz.undoLast() }
                }
                Text("بدء البرنامج من جديد", color = P.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp).clip(L.shape(8.dp)).clickable { Hifz.reset() }.padding(6.dp))
            }
        }
    }
    if (pickSurah) AlertDialog(onDismissRequest = { pickSurah = false }, containerColor = P.surface, title = { Text("ابدأ من سورة", color = P.ink) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(Quran.surahs) { su ->
                    Text("${Quran.digits(su.n)}. ${su.name} (${Quran.digits(su.count)} آية)", color = P.ink, fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().clip(L.shape(8.dp)).clickable { Hifz.pos = su.n to 1; pickSurah = false }.padding(10.dp))
                }
            }
        }, confirmButton = { TextButton({ pickSurah = false }) { Text("إغلاق") } })
}

/** The verses to learn or review: big text, listen with repeat, and a test that hides them (first word only, tap to reveal). */
@Composable
private fun HifzReader(s: Int, from: Int, to: Int, review: Hifz.Chunk?, learn: Boolean = true, back: () -> Unit) {
    val ctx = LocalContext.current
    val ayahs = remember(s, from, to) { (from..to).mapNotNull { Quran.at(s, it) } }
    var test by remember { mutableStateOf(review != null) }
    val shown = remember(s, from, to, test) { mutableStateListOf<Int>() }
    Subpage(if (review != null) "مراجعة" else "حفظ اليوم", back) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(Hifz.label(s, from, to), color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("▶ استمع ×${Recitation.repeat}", false) { Recitation.play(ctx, s, from, stopAfter = to) }
                Choice("🔁 التكرار: ${Recitation.repeat}", Recitation.repeat > 1) { Recitation.repeat = listOf(1, 3, 5, 10).let { r -> r[(r.indexOf(Recitation.repeat) + 1) % r.size] } }
                Choice(if (test) "👁 إظهار النص" else "🧠 اختبرني", test) { test = !test }
            }
            if (test) Sub("كل آية تظهر أول كلمة منها فقط: سمّعها، ثم اضغط عليها لتتأكد.", size = 12.sp)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ayahs) { a ->
                    val open = !test || a.a in shown
                    Column(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(P.card).border(1.dp, P.line, L.shape(16.dp))
                        .clickable { if (a.a in shown) shown.remove(a.a) else shown.add(a.a) }.padding(14.dp)) {
                        Text(if (open) a.text else a.text.split(' ').first() + " …", color = if (open) P.ink else P.muted, fontSize = 24.sp, lineHeight = 44.sp,
                            fontFamily = Fonts.quran, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("﴿${Quran.digits(a.a)}﴾", color = P.accent, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (review != null) {
                    Action("✓ أتقنتها", Modifier.weight(1f)) { Hifz.review(review, true); Haptics.tap(ctx, true); back() }
                    Action("↺ أعدها غدًا", Modifier.weight(1f), filled = false) { Hifz.review(review, false); back() }
                } else if (learn) {
                    Action("✓ حفظتها", Modifier.weight(1f)) { Hifz.learn(s, from, to); Haptics.tap(ctx, true); back() }
                }
            }
        }
    }
}
