package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * «سجل صديقي»: what opened and when — each level reached (and the friends and looks it opened), each friendship gift and
 * rank, each badge — with the level and friendship as they stand now, what has opened so far, and what comes next.
 * Kept on the phone (and in the backups, as the rest of the settings); the newest 500 entries.
 */
object Journal {
    data class Entry(val t: Long, val icon: String, val text: String)
    private val sp get() = Store.prefs
    private fun key() = if (Pet.kids) "kid_journal" else "journal"

    fun entries(): List<Entry> = runCatching {
        val a = JSONArray(sp.getString(key(), "[]"))
        (0 until a.length()).map { a.getJSONObject(it) }.map { Entry(it.getLong("t"), it.optString("i"), it.getString("x")) }
    }.getOrDefault(emptyList())

    fun add(icon: String, text: String, t: Long = System.currentTimeMillis()) {
        val all = (entries() + Entry(t, icon, text)).sortedBy { it.t }
        val list = all.filter { it.t == 0L } + all.filter { it.t != 0L }.takeLast(500)
        val a = JSONArray(); list.forEach { a.put(JSONObject().put("t", it.t).put("i", it.icon).put("x", it.text)) }
        sp.edit().putString(key(), a.toString()).apply()
    }

    /** A level reached: the level, then each friend and look it opened. */
    fun level(l: Int) {
        add("✨", "بلغت المستوى ${Quran.digits(l)}")
        Species.entries.filter { Progress.speciesLevel(it) == l }.forEach { add("🐾", "صديق جديد: ${it.ar} — بالمستوى ${Quran.digits(l)}") }
        Species.entries.forEach { s -> PetLooks.of(s).filter { it.level == l && !it.hidden }.forEach { add("🎨", "شكل جديد: ${it.name} (${s.ar}) — بالمستوى ${Quran.digits(l)}") } }
    }

    /** Friendship points went from [before] to [after]: the gifts and ranks crossed. */
    fun bond(before: Int, after: Int, steps: List<Pair<Int, String>>) {
        Friendship.gifts.filter { it.bond in (before + 1)..after }.forEach { add(it.icon, "هدية صداقة: ${it.name} — عند ${Quran.digits(it.bond)} ❤") }
        steps.filter { it.first in (before + 1)..after }.forEach { add("💞", "مرتبة صداقة جديدة: ${it.second} — عند ${Quran.digits(it.first)} ❤") }
    }

    /** Opened before the journal existed (its day unknown): every friend, friendship gift and look, with what it needed. */
    private fun before() {
        if (sp.getBoolean(key() + "_before", false)) return
        sp.edit().putBoolean(key() + "_before", true).apply()
        val l = Progress.level()
        Species.entries.filter { Progress.speciesOpen(it) }.sortedBy { Progress.speciesLevel(it) }.forEach {
            add("🐾", "صديق: ${it.ar} — " + (Progress.speciesLevel(it).let { n -> if (n <= 1) "مفتوح من البداية" else "بالمستوى ${Quran.digits(n)}" }), 0L)
        }
        Friendship.gifts.filter { Progress.bondPoints >= it.bond }.forEach { add(it.icon, "هدية صداقة: ${it.name} — عند ${Quran.digits(it.bond)} ❤", 0L) }
        Species.entries.forEach { s -> PetLooks.of(s).filter { !it.hidden && it.level in 2..l }.forEach { add("🎨", "شكل: ${it.name} (${s.ar}) — بالمستوى ${Quran.digits(it.level)}", 0L) } }
    }

    /** The first time: where things stand, and the badges already earned on their days. */
    fun start() {
        before()
        if (sp.getBoolean(key() + "_started", false)) return
        sp.edit().putBoolean(key() + "_started", true).apply()
        val zone = ZoneId.systemDefault()
        Progress.earned().forEach { (id, day) ->
            val b = Progress.badges.firstOrNull { it.id == id } ?: return@forEach
            val t = runCatching { LocalDate.parse(day).atTime(12, 0).atZone(zone).toInstant().toEpochMilli() }.getOrDefault(System.currentTimeMillis())
            add(b.icon, "وسام: ${b.name} — ${b.how}", t)
        }
        add("📜", "بدأ السجل: المستوى ${Quran.digits(Progress.level())} · ${Quran.digits(Progress.bondPoints)} ❤ صداقة · ${Quran.digits(Species.entries.count { Progress.speciesOpen(it) })} صديقًا مفتوحًا")
    }
}

/** The journal's window. */
@Composable
fun JournalSheet(close: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    LaunchedEffect(Unit) { Journal.start(); Store.bump() }
    AppSheet(onDismissRequest = close) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📜 سجل ${Pet.name.ifBlank { "صديقي" }}", color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            // Now: the level by experience, and the friendship.
            val l = Progress.level(); val xp = Progress.xp
            val from = Progress.need(l); val to = Progress.need(l + 1)
            Meter("✨ المستوى ${Quran.digits(l)}", "${Quran.digits(xp)} ✨ · بقي ${Quran.digits(to - xp)} للمستوى ${Quran.digits(l + 1)}", (xp - from).toFloat() / (to - from).coerceAtLeast(1))
            val b = Progress.bondPoints; val (bf, bt) = Progress.bondRange(b)
            Meter("${Friendship.heart} الصداقة: ${Progress.bondName(b)}", "${Quran.digits(b)} ❤" + if (bt > 0) " · بقي ${Quran.digits(bt - b)} للمرتبة التالية" else " · أعلى المراتب",
                if (bt > 0) (b - bf).toFloat() / (bt - bf) else 1f)
            Sub("اليوم: ${Quran.digits(Progress.todayTotal())} / ${Quran.digits(Progress.DAY_MAX)} ✨ · ${Quran.digits(Progress.bondToday())} / ${Quran.digits(Progress.BOND_DAY)} ❤", size = 12.sp)
            val days = Pet.since.takeIf { it > 0 }?.let { java.time.temporal.ChronoUnit.DAYS.between(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()) + 1 }
            Sub(listOfNotNull(days?.let { "معًا منذ ${Quran.digits(it.toInt())} ${if (it in 3..10) "أيام" else "يومًا"}" },
                "سلسلة الأيام الطيبة ${Quran.digits(Pet.streak())} (أفضلها ${Quran.digits(maxOf(Pet.bestStreak(), Pet.streak()))})").joinToString(" · "), size = 12.sp)

            // What has opened so far.
            Title("ما فُتح حتى الآن")
            val looks = Species.entries.flatMap { s -> PetLooks.of(s).filter { !it.hidden } }
            val rows = listOf(
                "🐾 الأصدقاء" to (Species.entries.count { Progress.speciesOpen(it) } to Species.entries.size),
                "🎨 الأشكال" to (looks.count { it.level <= l } to looks.size),
                "🖌 الثيمات" to (Looks.all.count { Friendship.themeOpen(it.id) } to Looks.all.size),
                "🖼 إطارات الأداة" to (Progress.frames.count { it.open } to Progress.frames.size),
                "💝 هدايا الصداقة" to (Friendship.gifts.count { Progress.bondPoints >= it.bond } to Friendship.gifts.size),
                "🏅 الأوسمة" to (Progress.earned().size to Progress.badges.size),
            )
            var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
            rows.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (name, c) ->
                        Column(Modifier.weight(1f).clip(L.shape(12.dp)).background(if (open == name) P.accent.copy(alpha = .16f) else P.card)
                            .clickable { open = if (open == name) null else name }.padding(10.dp)) {
                            Sub(name, size = 12.sp)
                            Text("${Quran.digits(c.first)} / ${Quran.digits(c.second)}" + if (open == name) "  ▴" else "  ▾", color = if (c.first == c.second) P.gold else P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // The tapped bubble's list: what is open (✓) and what is not yet (🔒), with what each needs.
                pair.firstOrNull { it.first == open }?.let { (name, _) -> OpenedList(name, l) }
            }

            // What comes next.
            Title("القادم")
            val nextFriend = Species.entries.filter { !Progress.speciesOpen(it) }.minByOrNull { Progress.speciesLevel(it) }
            nextFriend?.let { Sub("🐾 ${it.ar} في المستوى ${Quran.digits(Progress.speciesLevel(it))}", size = 13.sp, color = P.ink) }
            looks.filter { it.level > l }.minByOrNull { it.level }?.let { lk -> Sub("🎨 الشكل التالي «${lk.name}» في المستوى ${Quran.digits(lk.level)}", size = 13.sp, color = P.ink) }
            Friendship.next()?.let { g -> Sub("${g.icon} ${g.name} عند ${Quran.digits(g.bond)} ❤ (بقي ${Quran.digits(g.bond - Progress.bondPoints)})", size = 13.sp, color = P.ink) }

            // The journal itself, newest first, by day.
            Title("اليوميات")
            val zone = ZoneId.systemDefault()
            val fmtDay = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale("ar"))
            val fmtTime = DateTimeFormatter.ofPattern("h:mm a", Locale("ar"))
            val list = Journal.entries().sortedByDescending { it.t }
            if (list.isEmpty()) Sub("يبدأ السجل من الآن: كل مستوى تبلغه وكل ما يُفتح يُكتب هنا بتاريخه.", size = 13.sp)
            list.groupBy { if (it.t == 0L) null else Instant.ofEpochMilli(it.t).atZone(zone).toLocalDate() }.forEach { (day, items) ->
                Text(day?.let { Quran.numerals(fmtDay.format(it)) } ?: "قبل بدء السجل (تاريخه غير معروف)", color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                items.forEach { e ->
                    Row(Modifier.fillMaxWidth().clip(L.shape(10.dp)).background(P.card).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(e.icon, fontSize = 18.sp)
                        Text(e.text, color = P.ink, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                        if (e.t != 0L) Sub(Quran.numerals(fmtTime.format(Instant.ofEpochMilli(e.t).atZone(zone))), size = 11.sp)
                    }
                }
            }
        }
    }
}

/** One kind's things: open first (✓), then the rest (🔒), each with what it needs. */
@Composable
private fun OpenedList(kind: String, l: Int) {
    data class Item(val open: Boolean, val text: String, val need: String, val order: Int)
    val earned = Progress.earned()
    val items: List<Item> = when {
        kind.contains("الأصدقاء") -> Species.entries.map { s -> val n = Progress.speciesLevel(s); Item(Progress.speciesOpen(s), s.ar, if (n <= 1) "من البداية" else "المستوى ${Quran.digits(n)}", n) }
        kind.contains("الأشكال") -> Species.entries.flatMap { s -> PetLooks.of(s).filter { !it.hidden }.map { lk -> Item(lk.level <= l, "${lk.name} · ${s.ar}", if (lk.level <= 1) "من البداية" else "المستوى ${Quran.digits(lk.level)}", lk.level) } }
        kind.contains("الثيمات") -> Looks.all.map { lk -> val g = Friendship.themeGift(lk.id); Item(Friendship.themeOpen(lk.id), (lk.ornament.takeIf { it.isNotEmpty() }?.plus(" ") ?: "") + lk.name, g?.let { "${Quran.digits(it.bond)} ❤" } ?: "مفتوح للجميع", g?.bond ?: 0) }
        kind.contains("إطارات") -> Progress.frames.map { f -> Item(f.open, f.name, if (f.bond == 0) "مفتوح للجميع" else "${Quran.digits(f.bond)} ❤", f.bond) }
        kind.contains("هدايا") -> Friendship.gifts.map { g -> Item(Progress.bondPoints >= g.bond, "${g.icon} ${g.name}", "${Quran.digits(g.bond)} ❤", g.bond) }
        else -> Progress.badges.map { b -> Item(earned.containsKey(b.id), "${b.icon} ${b.name}", earned[b.id]?.let { "نلته ${Quran.numerals(it)}" } ?: b.how, 0) }
    }.sortedWith(compareBy({ !it.open }, { it.order }))
    Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(L.shape(12.dp)).background(P.surface).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { it ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (it.open) "✓" else "🔒", color = if (it.open) P.accent else P.muted, fontSize = 13.sp)
                Text(it.text, color = if (it.open) P.ink else P.muted, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Text(it.need, color = P.gold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Meter(title: String, sub: String, frac: Float) {
    Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card).padding(12.dp)) {
        Text(title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(8.dp).clip(L.shape(4.dp)).background(P.line)) {
            Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(8.dp).background(P.accent))
        }
        Sub(sub, size = 12.sp)
    }
}
