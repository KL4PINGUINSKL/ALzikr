package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ================================================================================ the app's font

/** One font for the whole app (or the theme's own): the user's choice in «راحة العين». */
object AppFont {
    val choices = listOf("" to "خط الثيم", "system" to "خط الهاتف", "naskh" to "النسخ (أميري)", "modern" to "الحديث (تجوال)", "kufi" to "الكوفي", "messiri" to "المسيري", "changa" to "تشانغا")
    var id: String
        get() = Store.prefs.getString("app_font", "") ?: ""
        set(v) { Store.prefs.edit().putString("app_font", v).apply(); Store.bump() }
    fun family(id: String): FontFamily? = when (id) { "system" -> FontFamily.Default; "naskh" -> Fonts.naskh; "modern" -> Fonts.modern; "kufi" -> Fonts.kufi; "messiri" -> Fonts.messiri; "changa" -> Fonts.changa; else -> null }
    /** The look with the chosen font for its words and headings. */
    fun apply(look: Look): Look { val f = family(id) ?: return look; return look.copy(body = f, head = f) }
}

// ================================================================================ the date in the corner

/** Ways to show the date under the app's name; some open with friendship points. */
object DateStyle {
    data class Style(val id: String, val name: String, val bond: Int)
    val styles = listOf(
        Style("full", "كامل", 0), Style("hijri", "هجري فقط", 0), Style("greg", "ميلادي فقط", 0), Style("short", "مختصر بالأرقام", 0),
        Style("stack", "سطران", 0), Style("page", "ورقة التقويم", 150), Style("moon", "الهلال", 300), Style("bar", "شريط الشهر", 600),
        Style("next", "المناسبة القادمة", 1000), Style("ornate", "المزخرف", 1500), Style("digital", "الرقمي", 2500),
    )
    fun open(s: Style) = Progress.bondPoints >= s.bond
    var id: String
        get() = (Store.prefs.getString("date_style", "full") ?: "full").takeIf { i -> styles.any { it.id == i && open(it) } } ?: "full"
        set(v) { Store.prefs.edit().putString("date_style", v).apply(); Store.bump() }
    /** The moon's shape for a Hijri day. */
    fun moon(day: Int) = when (day) { in 1..3 -> "🌒"; in 4..6 -> "🌓"; in 7..11 -> "🌔"; in 12..16 -> "🌕"; in 17..21 -> "🌖"; in 22..25 -> "🌗"; in 26..28 -> "🌘"; else -> "🌑" }
}

/** The date under the app's name, in the chosen style. A tap opens the calendar; a long press, the styles. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DateBadge(open: () -> Unit, pick: () -> Unit, style: String = DateStyle.id) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val d = LocalDate.now(); val ar = Locale("ar")
    val h = Data.hijri(d)
    val day = d.format(DateTimeFormatter.ofPattern("EEEE", ar))
    val greg = d.format(DateTimeFormatter.ofPattern("d MMMM", ar))
    val mod = Modifier.clip(L.shape(10.dp)).combinedClickable(onClick = open, onLongClick = pick)
    when (style) {
        "hijri" -> Sub("🌙 $day ${Quran.digits(h.day)} ${h.monthName} ${Quran.digits(h.year)} هـ", mod, size = 12.sp)
        "greg" -> Sub("📅 $day ${d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", ar))}", mod, size = 12.sp)
        "short" -> Sub("${Quran.digits(h.day)}/${Quran.digits(h.month)}/${Quran.digits(h.year)} هـ · ${Quran.digits(d.dayOfMonth)}/${Quran.digits(d.monthValue)}", mod, size = 12.sp)
        "stack" -> Column(mod) {
            Text("$day ${Quran.digits(h.day)} ${h.monthName}", color = P.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${Quran.digits(h.year)} هـ · $greg", color = P.gold, fontSize = 11.sp)
        }
        "page" -> Row(mod, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(44.dp).clip(L.shape(8.dp)).background(P.surface).border(1.dp, P.line, L.shape(8.dp)), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().background(P.accent).padding(vertical = 1.dp), contentAlignment = Alignment.Center) { Text(h.monthName.take(6), color = P.onAccent, fontSize = 9.sp, maxLines = 1) }
                Text(Quran.digits(h.day), color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(start = 8.dp)) { Text(day, color = P.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("${Quran.digits(h.year)} هـ · $greg", color = P.muted, fontSize = 11.sp) }
        }
        "moon" -> Row(mod, verticalAlignment = Alignment.CenterVertically) {
            Text(DateStyle.moon(h.day), fontSize = 24.sp)
            Column(Modifier.padding(start = 6.dp)) { Text("${Quran.digits(h.day)} ${h.monthName}", color = P.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("$day · $greg", color = P.muted, fontSize = 11.sp) }
        }
        "bar" -> Column(mod.width(170.dp)) {
            Text("$day ${Quran.digits(h.day)} ${h.monthName}", color = P.ink, fontSize = 12.sp, maxLines = 1)
            Box(Modifier.fillMaxWidth().padding(top = 3.dp).height(5.dp).clip(L.shape(3.dp)).background(P.line)) {
                Box(Modifier.fillMaxWidth(h.day / 30f).height(5.dp).clip(L.shape(3.dp)).background(Brush.horizontalGradient(listOf(P.accent, P.gold))))
            }
            Text("مضى ${Quran.digits(h.day)} من الشهر · $greg", color = P.muted, fontSize = 10.sp)
        }
        "next" -> Column(mod) {
            Text("$day ${Quran.digits(h.day)} ${h.monthName}", color = P.ink, fontSize = 12.sp)
            val n = runCatching { nextOccasion() }.getOrNull()
            Text(n?.let { (o, k) -> if (k == 0L) "✨ اليوم ${o.name}" else "⏳ ${o.name} بعد ${Quran.digits(k.toInt())} يومًا" } ?: greg, color = P.gold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        "ornate" -> Text("۞ ${Quran.digits(h.day)} ${h.monthName} ${Quran.digits(h.year)} ۞", color = P.gold, fontSize = 13.sp, fontFamily = Fonts.naskh, fontWeight = FontWeight.Bold, modifier = mod)
        "digital" -> Row(mod, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Quran.digits(h.day), Quran.digits(h.month), Quran.digits(h.year)).forEach { t ->
                Text(t, color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(L.shape(6.dp)).background(P.surface).border(1.dp, P.accent.copy(alpha = .5f), L.shape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Text(" هـ", color = P.muted, fontSize = 11.sp)
        }
        else -> Sub("📅 " + dateLine(), mod, size = 12.sp)
    }
}

/** Choosing how the date shows (long press on it, or «راحة العين»). */
@Composable
fun DateStyleSheet(close: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = androidx.compose.ui.platform.LocalContext.current
    AppSheet(onDismissRequest = close) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📅 شكل التاريخ في الأعلى", color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Sub("بعضها يُفتح بنقاط الصداقة ❤. اضغط التاريخ لتفتح التقويم، واضغطه مطولًا لتعود إلى هنا.", size = 12.sp)
            DateStyle.styles.forEach { s ->
                val open = DateStyle.open(s); val on = DateStyle.id == s.id
                Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(if (on) P.accent.copy(alpha = .12f) else P.surface).border(1.dp, if (on) P.accent else P.line, L.shape(14.dp))
                    .clickable { if (open) DateStyle.id = s.id else android.widget.Toast.makeText(ctx, "يُفتح عند ${Quran.digits(s.bond)} ❤ (معك ${Quran.digits(Progress.bondPoints)})", android.widget.Toast.LENGTH_SHORT).show() }
                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (open) s.name else "🔒 ${s.name}", color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (!open) Text("${Quran.digits(s.bond)} ❤", color = P.gold, fontSize = 12.sp)
                    else Box { DateBadge({}, {}, s.id) }
                }
            }
        }
    }
}

// ================================================================================ quick access

/**
 * «الوصول السريع»: what the user opens most (counted with a gentle fading, so recent habits weigh more). On the home
 * page these are suggested as its shortcuts, until the user picks the shortcuts himself.
 */
object Usage {
    private fun counts(): JSONObject = runCatching { JSONObject(Store.prefs.getString("usage", "{}")!!) }.getOrDefault(JSONObject())
    /** One more use of [id] (a home shortcut's id). */
    fun hit(id: String) {
        val o = counts()
        // Each use counts 1; everything fades by 2 % a use, so last month's habits do not hold the first places forever.
        o.keys().asSequence().toList().forEach { k -> o.put(k, o.optDouble(k) * .98) }
        o.put(id, o.optDouble(id, 0.0) + 1.0)
        Store.prefs.edit().putString("usage", o.toString()).apply()
    }
    /** The most used, best first. */
    fun top(n: Int): List<String> { val o = counts(); return o.keys().asSequence().toList().filter { o.optDouble(it) >= 1.5 }.sortedByDescending { o.optDouble(it) }.take(n) }
    /** A More page → its home shortcut (if it has one). */
    val morePage = mapOf("qibla" to "qibla", "calendar" to "calendar", "seasons" to "seasons", "alarms" to "alarm", "world" to "world", "hifz" to "hifz", "pet" to "pet",
        "today" to "today", "ramadan" to "ramadan", "goals" to "goals", "sadaqa" to "sadaqa", "salah" to "salah", "tajweed" to "tajweed", "stats" to "stats", "timeline" to "timeline", "history" to "history")
}

// ================================================================================ «سجلاتي»

/** Everything searched and read, newest first; a tap opens it again where it was. */
object History {
    data class Entry(val icon: String, val title: String, val sub: String, val key: String, val at: Long)
    fun all(): List<Entry> = runCatching {
        val a = JSONArray(Store.prefs.getString("history", "[]")); (0 until a.length()).map { a.getJSONObject(it).let { o -> Entry(o.getString("i"), o.getString("t"), o.optString("s"), o.getString("k"), o.getLong("at")) } }
    }.getOrDefault(emptyList())
    /** Adds (or moves to the top) one item; the last 300 are kept. */
    fun add(icon: String, title: String, sub: String, key: String) {
        if (title.isBlank()) return
        val list = listOf(Entry(icon, title.take(140), sub, key, System.currentTimeMillis())) + all().filter { it.key != key }
        val a = JSONArray(); list.take(300).forEach { e -> a.put(JSONObject().put("i", e.icon).put("t", e.title).put("s", e.sub).put("k", e.key).put("at", e.at)) }
        Store.prefs.edit().putString("history", a.toString()).apply()
    }
    fun clear() { Store.prefs.edit().remove("history").apply(); Store.bump() }
    fun remove(key: String) { val a = JSONArray(); all().filter { it.key != key }.forEach { e -> a.put(JSONObject().put("i", e.icon).put("t", e.title).put("s", e.sub).put("k", e.key).put("at", e.at)) }; Store.prefs.edit().putString("history", a.toString()).apply(); Store.bump() }

    /** Opens what an entry points to. */
    fun open(c: android.content.Context, key: String) {
        val p = key.split(':')
        when (p[0]) {
            "quran" -> Nav.openQuran(p[1].toInt(), p[2].toInt())
            "read" -> Nav.reading = p[1].toInt() to p[2].toInt()
            "hadith" -> Nav.openHadith(p[1], p.getOrNull(2)?.toIntOrNull() ?: 0)
            "sharh" -> { Nav.sharh = p[1]; Nav.go("library") }
            "book" -> (Books.shelf(c).firstOrNull { it.id == p[1] } ?: Books.index(c).firstOrNull { it.id == p[1] })?.let { Nav.openBook(it) }
            "story" -> { Nav.story = p[1].toInt(); Nav.library = 1; Nav.go("library") }
            "collection" -> { Nav.collection = p[1]; Nav.go("adhkar") }
            "page" -> { Nav.morePage = key.removePrefix("page:"); Nav.go("more") }
            "search" -> { Nav.searchQuery = key.removePrefix("search:"); Nav.search = true }
        }
    }
}

/** «سجلاتي»: what you searched and read, by day; a tap opens it again. */
@Composable
fun HistoryPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var filter by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val all = History.all()
    val shown = all.filter { e -> when (filter) { "search" -> e.key.startsWith("search:"); "read" -> !e.key.startsWith("search:"); else -> true } }
    val fmt = remember { java.text.SimpleDateFormat("h:mm a", Locale("ar")) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Hint("history", "🕘", "كل ما قرأته وبحثت عنه في التطبيق، الأحدث أولًا. اضغط أي سطر لتعود إليه حيث كنت.") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Wrap(Modifier.weight(1f), gap = 6.dp) { Choice("الكل", filter == "") { filter = "" }; Choice("📖 ما قرأته", filter == "read") { filter = "read" }; Choice("🔍 ما بحثت عنه", filter == "search") { filter = "search" } }
                if (all.isNotEmpty()) Text("مسح", color = P.danger, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { confirm = true }.padding(8.dp))
            }
        }
        if (shown.isEmpty()) item { Sub("لا شيء بعد. ما تقرؤه من القرآن والحديث والشروح والكتب والقصص والأذكار، وما تبحث عنه، يظهر هنا.", Modifier.padding(8.dp)) }
        var lastDay = ""
        shown.forEach { e ->
            val d = java.time.Instant.ofEpochMilli(e.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val label = when (d) { LocalDate.now() -> "اليوم"; LocalDate.now().minusDays(1) -> "أمس"; else -> d.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("ar"))) }
            if (label != lastDay) { lastDay = label; item(key = "d" + e.at) { Text(label, color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) } }
            item(key = e.key + e.at) {
                Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.surface).clickable { History.open(ctx, e.key) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GText(e.icon, fontSize = 18.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(e.title, color = P.ink, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, fontFamily = Fonts.naskh)
                        Sub(listOf(e.sub, fmt.format(java.util.Date(e.at))).filter { it.isNotBlank() }.joinToString(" · "), size = 11.sp)
                    }
                    Text("✕", color = P.muted, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { History.remove(e.key) }.padding(6.dp))
                }
            }
        }
    }
    if (confirm) androidx.compose.material3.AlertDialog(onDismissRequest = { confirm = false }, containerColor = P.surface, title = { Text("مسح السجل كله؟", color = P.ink) },
        confirmButton = { androidx.compose.material3.TextButton({ History.clear(); confirm = false }) { Text("مسح", color = P.danger) } },
        dismissButton = { androidx.compose.material3.TextButton({ confirm = false }) { Text("إلغاء") } })
}

@Suppress("unused") private val keepAlign = TextAlign.Center
@Suppress("unused") private val keepColor = Color.Transparent
@Suppress("unused") private fun keepSize() = Modifier.size(1.dp)
