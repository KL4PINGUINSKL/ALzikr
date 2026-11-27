package com.alzikr.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

/** Task categories, each with its colour. */
object Cats {
    val all = listOf("عبادة" to Color(0xFF2E9E6A), "عمل" to Color(0xFF3B6FD8), "بيت" to Color(0xFFC9962E), "أسرة" to Color(0xFFD6577A), "دراسة" to Color(0xFF8A4FD1), "صحة" to Color(0xFF1F9AA8))
    fun color(c: String) = all.firstOrNull { it.first == c }?.second
}

/** How a task repeats, in words. */
object Rules {
    private val dayNames = mapOf(1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة", 6 to "السبت", 7 to "الأحد")
    fun label(rule: String): String = when {
        rule == "daily" -> "كل يوم"
        rule.startsWith("days:") -> "كل " + rule.removePrefix("days:").split(',').mapNotNull { it.toIntOrNull()?.let(dayNames::get) }.joinToString(" و")
        rule.startsWith("monthly:") -> "كل شهر يوم ${rule.removePrefix("monthly:")}"
        rule == "white" -> "الأيام البيض"
        else -> ""
    }
    fun dayName(d: Int) = dayNames[d] ?: ""
}

/** Ready-made routines: a set of tasks added together for today, and the user's own. */
object Routines {
    data class Routine(val id: String, val icon: String, val title: String, val items: List<Pair<String, Prayer?>>)
    val builtIn = listOf(
        Routine("morning", "🌅", "روتين الصباح", listOf("أذكار الصباح" to Prayer.FAJR, "ورد القرآن" to Prayer.FAJR, "صلاة الضحى" to null, "دعاء الاستفتاح ليومك" to Prayer.FAJR)),
        Routine("evening", "🌇", "روتين المساء", listOf("أذكار المساء" to Prayer.ASR, "الاستغفار ١٠٠" to Prayer.MAGHRIB, "محاسبة اليوم" to Prayer.ISHA)),
        Routine("sleep", "🌙", "روتين النوم", listOf("الوتر" to Prayer.ISHA, "أذكار النوم" to Prayer.ISHA, "سورة الملك" to Prayer.ISHA, "النوم على طهارة" to Prayer.ISHA)),
        Routine("friday", "🕌", "روتين الجمعة", listOf("غسل الجمعة والطيب" to Prayer.FAJR, "سورة الكهف" to null, "الصلاة على النبي ﷺ" to null, "الدعاء في ساعة الإجابة" to Prayer.ASR)),
        Routine("house", "🏠", "روتين البيت", listOf("ترتيب البيت" to null, "صلة الرحم (اتصال)" to null, "جلسة مع الأهل" to Prayer.MAGHRIB)),
    )
    fun mine(): List<Routine> = runCatching {
        val a = org.json.JSONArray(Store.prefs.getString("routines", "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
            val it2 = o.getJSONArray("items")
            Routine(o.getString("id"), "⭐", o.getString("title"), (0 until it2.length()).map { k -> it2.getJSONArray(k).let { x -> x.getString(0) to x.optString(1).takeIf { s -> s.isNotEmpty() }?.let { s -> runCatching { Prayer.valueOf(s) }.getOrNull() } } })
        } }
    }.getOrDefault(emptyList())
    fun save(title: String, items: List<Pair<String, Prayer?>>) {
        val a = org.json.JSONArray(Store.prefs.getString("routines", "[]"))
        a.put(org.json.JSONObject().put("id", "r" + System.currentTimeMillis()).put("title", title).put("items", org.json.JSONArray().also { arr -> items.forEach { (t, p) -> arr.put(org.json.JSONArray().put(t).put(p?.name ?: "")) } }))
        Store.prefs.edit().putString("routines", a.toString()).apply(); Store.bump()
    }
    fun delete(id: String) {
        val a = org.json.JSONArray(Store.prefs.getString("routines", "[]")); val out = org.json.JSONArray()
        for (i in 0 until a.length()) a.getJSONObject(i).takeIf { it.getString("id") != id }?.let { out.put(it) }
        Store.prefs.edit().putString("routines", out.toString()).apply(); Store.bump()
    }
    /** Adds the routine's tasks for today (skipping ones already open with the same name). */
    fun apply(r: Routine): Int {
        val open = Store.tasks().filter { !it.done }.map { it.text }.toSet()
        val add = r.items.filter { it.first !in open }
        add.reversed().forEach { (t, p) -> Store.addTask(t, p, due = LocalDate.now().toString(), cat = "عبادة".takeIf { r.id != "house" } ?: "بيت") }
        return add.size
    }
}

/**
 * Quick entry in everyday words: «اتصل بأمي غدًا بعد العصر», «اجتماع الخميس الساعة ٥ مساء», «الوتر كل يوم»,
 * «صيام الاثنين والخميس كل أسبوع». The day, the prayer, the hour and the repetition are read from the text and taken out.
 */
object QuickParse {
    data class Parsed(val text: String, val after: Prayer?, val due: LocalDate?, val minute: Int?, val rule: String)
    private val days = mapOf("الاثنين" to 1, "الإثنين" to 1, "الثلاثاء" to 2, "الأربعاء" to 3, "الاربعاء" to 3, "الخميس" to 4, "الجمعة" to 5, "الجمعه" to 5, "السبت" to 6, "الأحد" to 7, "الاحد" to 7,
        "اثنين" to 1, "إثنين" to 1, "ثلاثاء" to 2, "أربعاء" to 3, "اربعاء" to 3, "خميس" to 4, "جمعة" to 5, "جمعه" to 5, "سبت" to 6)
    private val prayers = mapOf("الفجر" to Prayer.FAJR, "الظهر" to Prayer.DHUHR, "العصر" to Prayer.ASR, "المغرب" to Prayer.MAGHRIB, "العشاء" to Prayer.ISHA)

    fun parse(input: String): Parsed {
        // Marks off (غدًا → غدا), digits Western, single spaces, a space at each end so whole words match.
        var t = " " + Quran.latinDigits(input).replace(Regex("[ً-ْ]"), "").replace(Regex("\\s+"), " ").trim() + " "
        // A phrase taken out leaves a space, so the words around it stay apart.
        fun cut(r: Regex): MatchResult? = r.find(t)?.also { m -> t = " " + t.replaceRange(m.range, " ").replace(Regex("\\s+"), " ").trim() + " " }
        var rule = ""; var due: LocalDate? = null; var after: Prayer? = null; var minute: Int? = null
        if (cut(Regex(" (كل يوم|يوميا|يوميًا) ")) != null) rule = "daily"
        if (cut(Regex(" (الايام البيض|الأيام البيض) ")) != null) rule = "white"
        if (cut(Regex(" كل شهر ")) != null) rule = "monthly:${LocalDate.now().dayOfMonth}"
        // «كل اثنين وخميس», «الاثنين والخميس كل أسبوع»
        val many = Regex(" (?:كل |يوم )?((?:${days.keys.joinToString("|")})(?: ?و ?(?:${days.keys.joinToString("|")}))+)(?: كل (?:أسبوع|اسبوع))? ").find(t)
        if (many != null) { rule = "days:" + days.filterKeys { it in many.groupValues[1] }.values.distinct().sorted().joinToString(","); cut(Regex(Regex.escape(many.value))) }
        Regex(" (?:كل )((?:${days.keys.joinToString("|")}))(?: كل (?:أسبوع|اسبوع))? ").find(t)?.let { m -> if (m.value.contains("كل")) { rule = "days:${days[m.groupValues[1]]}"; cut(Regex(Regex.escape(m.value))) } }
        when {
            cut(Regex(" بعد غد ")) != null -> due = LocalDate.now().plusDays(2)
            cut(Regex(" (غدا|غدًا|غداً|بكرة|بكره) ")) != null -> due = LocalDate.now().plusDays(1)
            cut(Regex(" اليوم ")) != null -> due = LocalDate.now()
        }
        if (rule.isEmpty()) Regex(" (?:يوم )?(${days.keys.joinToString("|")}) ").find(t)?.let { m ->
            val d = days[m.groupValues[1]]!!; var x = LocalDate.now().plusDays(1)
            while (x.dayOfWeek != DayOfWeek.of(d)) x = x.plusDays(1)
            due = x; cut(Regex(Regex.escape(m.value)))
        }
        Regex(" (?:بعد|عند) (?:صلاة )?(${prayers.keys.joinToString("|")}) ").find(t)?.let { m -> after = prayers[m.groupValues[1]]; cut(Regex(Regex.escape(m.value))) }
        Regex(" (?:الساعة |ساعة )?(\\d{1,2})(?:[:.](\\d{2}))? ?(صباحا|صباحًا|صباحاً|مساء|مساءً|ص|م|ظهرا|ظهرًا|عصرا|ليلا)? ").find(t)?.let { m ->
            val hasWord = m.value.contains("الساعة") || m.value.contains("ساعة") || m.groupValues[2].isNotEmpty() || m.groupValues[3].isNotEmpty()
            val h = m.groupValues[1].toInt()
            if (hasWord && h in 0..23) {
                val pm = m.groupValues[3].let { it.startsWith("م") || it.startsWith("ع") || it.startsWith("ل") || (it.startsWith("ظ") && h < 12) }
                var hh = h; if (pm && hh < 12) hh += 12; if (!pm && m.groupValues[3].isEmpty() && hh in 1..6) hh += 12
                minute = hh * 60 + (m.groupValues[2].toIntOrNull() ?: 0); cut(Regex(Regex.escape(m.value)))
            }
        }
        if (minute != null && due == null && rule.isEmpty()) due = LocalDate.now().let { if (minute!! <= java.time.LocalTime.now().let { n -> n.hour * 60 + n.minute }) it.plusDays(1) else it }
        return Parsed(t.trim(), after, due, minute, rule)
    }
}

/** The tasks section: quick entry, routines, categories, flexible repeats, steps, reminders, focus. */
@Composable
fun TaskListPlus() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var after by rememberSaveable { mutableStateOf<String?>(null) }
    var remindAt by rememberSaveable { mutableStateOf(0L) }
    var rule by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("") }
    var showDone by rememberSaveable { mutableStateOf(false) }
    var ruleFor by remember { mutableStateOf<Long?>(null) }   // -1 = the new task
    var saveRoutine by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val parsed = remember(text) { QuickParse.parse(text) }
    val all = Store.tasks().filter { filter.isEmpty() || it.cat == filter }
    val now = all.filter { !it.done && it.on(today) }
    val later = all.filter { !it.done && !it.on(today) }
    val done = all.filter { it.done }

    fun add() {
        if (text.isBlank()) return
        val p = parsed
        val atPick = remindAt
        val at = when {
            atPick > 0 -> atPick
            p.minute != null -> (p.due ?: today).atStartOfDay(java.time.ZoneId.systemDefault()).plusMinutes(p.minute.toLong()).toInstant().toEpochMilli()
            else -> 0L
        }
        Store.addTask(p.text.ifBlank { text.trim() }, after?.let(Prayer::valueOf) ?: p.after, at, rule.ifEmpty { p.rule }, cat, if (rule.isEmpty() && p.rule.isEmpty()) (p.due?.toString() ?: "") else "")
        text = ""; remindAt = 0; rule = ""; after = null
        TaskReminders.schedule(ctx)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Hint("tasks4", "✅", "اكتب المهمة كما تقولها: «اتصل بأمي غدًا بعد العصر»، «اجتماع الخميس الساعة ٥ مساء»، «الوتر كل يوم»؛ فيفهم اليوم والوقت والتكرار. أو أضف روتينًا كاملًا بلمسة. اضغط المهمة لتفتح خطواتها وخياراتها.") }
        item {
            Panel(pad = 12.dp) {
                OutlinedTextField(text, { text = it }, placeholder = { Text("مهمة جديدة… (مثل: زيارة جدتي الجمعة بعد العصر)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { add() }))
                // What the words were understood as.
                val understood = listOfNotNull(parsed.due?.let { dueLabel(it) }, parsed.after?.let { "بعد ${it.ar}" }, parsed.minute?.let { "⏰ " + Alerts.timeText(it) }, Rules.label(parsed.rule).takeIf { it.isNotEmpty() }?.let { "🔁 $it" })
                if (text.isNotBlank() && understood.isNotEmpty()) Text("فهمت: " + understood.joinToString(" · "), color = P.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                Wrap(Modifier.padding(top = 8.dp), gap = 6.dp) {
                    Prayer.entries.filter { it.alertable }.forEach { p -> Choice("بعد ${p.base}", after == p.name) { after = if (after == p.name) null else p.name } }
                }
                Wrap(Modifier.padding(top = 6.dp), gap = 6.dp) {
                    Choice(if (remindAt > 0) "⏰ " + TaskReminders.label(remindAt) else "⏰ وقت…", remindAt > 0) { pickReminder2(ctx, remindAt) { remindAt = it } }
                    Choice(if (rule.isNotEmpty()) "🔁 " + Rules.label(rule) else "🔁 تكرار…", rule.isNotEmpty()) { ruleFor = -1L }
                    Cats.all.forEach { (n, c) -> Choice((if (cat == n) "● " else "○ ") + n, cat == n) { cat = if (cat == n) "" else n } }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) { Action("إضافة") { add() } }
            }
        }
        // Routines: a whole set in one tap.
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sub("روتين بلمسة:", Modifier.weight(1f), size = 13.sp)
                    if (now.isNotEmpty()) Text("＋ احفظ مهام اليوم كروتين", color = P.accent, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { saveRoutine = true }.padding(6.dp))
                }
                Wrap(Modifier.padding(top = 4.dp), gap = 6.dp) {
                    (Routines.builtIn + Routines.mine()).forEach { r ->
                        Choice("${r.icon} ${r.title}", false) { val n = Routines.apply(r); Toast.makeText(ctx, if (n == 0) "مهامه موجودة اليوم" else "أُضيف ${Quran.digits(n)} مهام لليوم", Toast.LENGTH_SHORT).show() }
                    }
                }
                Routines.mine().takeIf { it.isNotEmpty() }?.let { mine -> Sub("روتيناتك: اضغط مطولًا في «حذف» لحذفها", size = 10.sp)
                    Wrap(gap = 6.dp) { mine.forEach { r -> Text("✕ ${r.title}", color = P.muted, fontSize = 11.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Routines.delete(r.id) }.padding(4.dp)) } } }
            }
        }
        // Categories as a filter.
        if (Store.tasks().any { it.cat.isNotEmpty() }) item {
            Wrap(gap = 6.dp) {
                Choice("الكل", filter.isEmpty()) { filter = "" }
                Cats.all.filter { (n, _) -> Store.tasks().any { it.cat == n } }.forEach { (n, _) -> Choice(n, filter == n) { filter = n } }
            }
        }
        item { Title("اليوم (${Quran.digits(now.size)})", Modifier.padding(top = 4.dp)) }
        if (now.isEmpty()) item { Sub("لا مهام لليوم. أضف مهمة أو روتينًا.", Modifier.padding(8.dp)) }
        items(now, key = { "n" + it.id }) { TaskCard(it) { ruleFor = it.id } }
        if (later.isNotEmpty()) {
            item { Title("قادمة (${Quran.digits(later.size)})", Modifier.padding(top = 8.dp)) }
            items(later, key = { "l" + it.id }) { TaskCard(it) { ruleFor = it.id } }
        }
        if (done.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${if (showDone) "▾" else "◂"} المنجزة (${done.size})", color = P.muted, fontSize = 14.sp, modifier = Modifier.weight(1f).clip(L.shape(8.dp)).clickable { showDone = !showDone }.padding(6.dp))
                if (showDone) Text("حذف المنجزة", color = P.danger, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Store.clearDone() }.padding(6.dp))
            }
        }
        if (showDone) items(done, key = { "d" + it.id }) { TaskCard(it) { ruleFor = it.id } }
    }
    ruleFor?.let { id ->
        val current = if (id == -1L) rule else Store.tasks().firstOrNull { it.id == id }?.rule ?: ""
        RuleDialog(current, { r -> if (id == -1L) rule = r else { Store.update(id) { it.copy(rule = r, due = if (r.isNotEmpty()) "" else it.due) }; TaskReminders.schedule(ctx) } }) { ruleFor = null }
    }
    if (saveRoutine) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { saveRoutine = false }, containerColor = P.surface, title = { Text("روتين جديد من مهام اليوم", color = P.ink) },
            text = { Column { Sub(now.joinToString("، ") { it.text }, size = 12.sp); OutlinedTextField(name, { name = it }, placeholder = { Text("اسم الروتين") }, singleLine = true, modifier = Modifier.padding(top = 8.dp)) } },
            confirmButton = { TextButton({ if (name.isNotBlank()) Routines.save(name.trim(), now.map { it.text to it.after }); saveRoutine = false }) { Text("حفظ") } },
            dismissButton = { TextButton({ saveRoutine = false }) { Text("إلغاء") } })
    }
}

fun dueLabel(d: LocalDate): String = when (d) {
    LocalDate.now() -> "اليوم"; LocalDate.now().plusDays(1) -> "غدًا"
    else -> "${Rules.dayName(d.dayOfWeek.value)} ${d.dayOfMonth}/${d.monthValue}"
}

/** Choosing how a task repeats. */
@Composable
private fun RuleDialog(current: String, set: (String) -> Unit, close: () -> Unit) {
    var r by remember { mutableStateOf(current) }
    val picked = if (r.startsWith("days:")) r.removePrefix("days:").split(',').mapNotNull { it.toIntOrNull() }.toSet() else emptySet()
    AlertDialog(onDismissRequest = close, containerColor = P.surface, title = { Text("التكرار", color = P.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Wrap(gap = 6.dp) {
                    Choice("مرة واحدة", r.isEmpty()) { r = "" }; Choice("كل يوم", r == "daily") { r = "daily" }
                    Choice("كل شهر يوم ${LocalDate.now().dayOfMonth}", r.startsWith("monthly:")) { r = "monthly:${LocalDate.now().dayOfMonth}" }
                    Choice("الأيام البيض (١٣–١٥ هجري)", r == "white") { r = "white" }
                }
                Sub("أو أيام من الأسبوع:", size = 12.sp)
                Wrap(gap = 6.dp) {
                    listOf(6, 7, 1, 2, 3, 4, 5).forEach { d ->
                        Choice(Rules.dayName(d), d in picked) {
                            val s = if (d in picked) picked - d else picked + d
                            r = if (s.isEmpty()) "" else "days:" + s.sorted().joinToString(",")
                        }
                    }
                }
                if (r.isNotEmpty()) Text("🔁 " + Rules.label(r), color = P.accent, fontSize = 13.sp)
            }
        },
        confirmButton = { TextButton({ set(r); close() }) { Text("تم") } },
        dismissButton = { TextButton(close) { Text("إلغاء") } })
}

/** One task: tick, its details in a line, and — opened with a tap — its steps and options. */
@Composable
private fun TaskCard(t: Store.Task, editRule: () -> Unit) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf("") }
    var rename by remember { mutableStateOf<String?>(null) }
    val catColor = Cats.color(t.cat)
    Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, if (open) P.accent.copy(alpha = .5f) else P.line, L.shape(14.dp))) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).clip(L.shape(7.dp)).background(if (t.done) P.accent else P.surface).border(2.dp, if (t.done) P.accent else (catColor ?: P.muted), L.shape(7.dp))
                .clickable { Store.toggleTask(t.id); if (!t.done) TaskReminders.cancelNotice(ctx, t.id) }, contentAlignment = Alignment.Center) {
                if (t.done) Text("✓", color = P.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.text, color = if (t.done) P.muted else P.ink, fontSize = 15.sp, textDecoration = if (t.done) TextDecoration.LineThrough else null)
                val meta = listOfNotNull(t.cat.takeIf { it.isNotEmpty() }, t.after?.let { "بعد ${it.ar}" }, t.remindAt.takeIf { it > 0 && !t.done }?.let { "⏰ " + TaskReminders.label(it) },
                    Rules.label(t.rule).takeIf { it.isNotEmpty() }?.let { "🔁 $it" }, t.due.takeIf { it.isNotEmpty() && t.rule.isEmpty() }?.let { runCatching { dueLabel(LocalDate.parse(it)) }.getOrNull() },
                    t.steps.takeIf { it.isNotEmpty() }?.let { s -> "☑ ${Quran.digits(s.count { it.done })}/${Quran.digits(s.size)}" })
                if (meta.isNotEmpty()) Text(meta.joinToString(" · "), color = catColor ?: P.muted, fontSize = 12.sp)
            }
            Text(if (open) "▴" else "▾", color = P.muted, fontSize = 14.sp)
        }
        if (open) Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Steps.
            t.steps.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(L.shape(8.dp)).clickable { Store.toggleStep(t.id, i) }.padding(vertical = 4.dp)) {
                    Box(Modifier.size(18.dp).clip(L.shape(5.dp)).background(if (s.done) P.accent else Color.Transparent).border(1.5.dp, if (s.done) P.accent else P.muted, L.shape(5.dp)), contentAlignment = Alignment.Center) {
                        if (s.done) Text("✓", color = P.onAccent, fontSize = 11.sp) }
                    Text(s.text, color = if (s.done) P.muted else P.ink, fontSize = 14.sp, textDecoration = if (s.done) TextDecoration.LineThrough else null, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Text("✕", color = P.muted, fontSize = 12.sp, modifier = Modifier.clip(L.shape(6.dp)).clickable { Store.removeStep(t.id, i) }.padding(4.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(step, { step = it }, placeholder = { Text("خطوة…") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (step.isNotBlank()) { Store.addStep(t.id, step.trim()); step = "" } }))
                Text("＋", color = P.accent, fontSize = 22.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { if (step.isNotBlank()) { Store.addStep(t.id, step.trim()); step = "" } }.padding(10.dp))
            }
            // Options.
            Wrap(gap = 6.dp) {
                if (!t.done) Choice("🎯 ركّز عليها", false) { focusOn(t) }
                Choice(if (t.remindAt > 0) "⏰ " + TaskReminders.label(t.remindAt) else "⏰ تذكير", t.remindAt > 0) { pickReminder2(ctx, t.remindAt) { at -> Store.setTaskReminder(t.id, at); TaskReminders.schedule(ctx) } }
                if (t.remindAt > 0) Choice("بلا تذكير", false) { Store.setTaskReminder(t.id, 0); TaskReminders.schedule(ctx) }
                Choice(if (t.rule.isNotEmpty()) "🔁 " + Rules.label(t.rule) else "🔁 تكرار", t.rule.isNotEmpty(), onClick = editRule)
                if (t.rule.isEmpty()) Choice("→ غدًا", false) { Store.update(t.id) { it.copy(due = LocalDate.now().plusDays(1).toString()) } }
                Choice("✎ تعديل", false) { rename = t.text }
                Choice("🗑 حذف", false) { Store.deleteTask(t.id); TaskReminders.cancelNotice(ctx, t.id) }
            }
            Wrap(gap = 6.dp) { Cats.all.forEach { (n, c) -> Choice((if (t.cat == n) "● " else "○ ") + n, t.cat == n) { Store.update(t.id) { it.copy(cat = if (it.cat == n) "" else n) } } } }
        }
    }
    rename?.let { r ->
        var s by remember { mutableStateOf(r) }
        AlertDialog(onDismissRequest = { rename = null }, containerColor = P.surface, title = { Text("تعديل المهمة", color = P.ink) },
            text = { OutlinedTextField(s, { s = it }, singleLine = true) },
            confirmButton = { TextButton({ if (s.isNotBlank()) Store.renameTask(t.id, s.trim()); rename = null }) { Text("حفظ") } },
            dismissButton = { TextButton({ rename = null }) { Text("إلغاء") } })
    }
}

/** Date, then time (Android's own pickers); [done] gets the moment in epoch ms. */
fun pickReminder2(c: android.content.Context, current: Long, done: (Long) -> Unit) {
    val z = java.time.Instant.ofEpochMilli(if (current > 0) current else System.currentTimeMillis() + 3_600_000L).atZone(java.time.ZoneId.systemDefault())
    android.app.DatePickerDialog(c, { _, y, m, d ->
        android.app.TimePickerDialog(c, { _, h, min ->
            val at = java.time.LocalDateTime.of(y, m + 1, d, h, min).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (at <= System.currentTimeMillis()) Toast.makeText(c, "اختر وقتًا لم يمضِ بعد", Toast.LENGTH_SHORT).show() else done(at)
        }, z.hour, z.minute, android.text.format.DateFormat.is24HourFormat(c)).show()
    }, z.year, z.monthValue - 1, z.dayOfMonth).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
}

/**
 * «مراجعة المساء» (in «اليوم», from Maghrib): what was done, and what is left moved to tomorrow in one tap or one by
 * one; a link to «محاسبة اليوم»; and an optional reminder at a chosen time.
 */
@Composable
fun EveningReview() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val maghrib = if (Store.hasLocation) runCatching { Store.times(today).getValue(Prayer.MAGHRIB) }.getOrDefault(18 * 60) else 18 * 60
    val now = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val tasks = Store.tasks().filter { it.on(today) }
    val left = tasks.filter { !it.done && it.rule.isEmpty() }
    val evening = now >= maghrib
    Panel(color = Color(0xFF1A3274).copy(alpha = .10f), pad = 14.dp) {
        Text("🌙 مراجعة المساء", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (!evening) Sub("تظهر بعد المغرب: ما أنجزت، ونقل ما بقي إلى الغد.", size = 12.sp)
        else {
            Sub("أنجزت ${Quran.digits(tasks.count { it.done })} من ${Quran.digits(tasks.size)} مهام اليوم" + if (left.isEmpty()) " · لا شيء باقٍ، بارك الله في يومك" else "", size = 13.sp)
            left.forEach { t ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("◻ ${t.text}", color = P.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("→ غدًا", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Store.update(t.id) { it.copy(due = today.plusDays(1).toString()) } }.padding(6.dp))
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (left.size > 1) Action("انقل الباقي كله إلى الغد") { left.forEach { t -> Store.update(t.id) { it.copy(due = today.plusDays(1).toString()) } } }
                Action("محاسبة اليوم", filled = false) { Nav.morePage = "today"; Nav.go("more") }
            }
        }
        val at = Data.reviewAt
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Choice(if (at < 0) "⏰ ذكّرني بالمراجعة" else "⏰ " + Alerts.timeText(at), at >= 0) {
                val m0 = if (at >= 0) at else 21 * 60
                android.app.TimePickerDialog(ctx, { _, h, m -> Data.reviewAt = h * 60 + m; TaskReminders.scheduleReview(ctx) }, m0 / 60, m0 % 60, android.text.format.DateFormat.is24HourFormat(ctx)).show()
            }
            if (at >= 0) Text("  إيقاف", color = P.muted, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Data.reviewAt = -1; TaskReminders.scheduleReview(ctx) }.padding(6.dp))
        }
    }
}

@Suppress("unused") private val keepCircle = CircleShape
@Suppress("unused") private fun keepHeight() = Modifier.height(1.dp)
