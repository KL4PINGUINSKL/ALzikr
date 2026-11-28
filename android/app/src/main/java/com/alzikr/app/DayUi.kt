package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The «يومي» sections, by id (the ids of the older tabs are kept, so shortcuts still land right). */
object Daily {
    const val TASKS = 0; const val HABITS = 1; const val FOCUS = 2; const val KHATMA = 3; const val NOTES = 4; const val TODAY = 5
}

/**
 * «اليوم»: one page that ties the day together — how far each part is (tasks, habits, the wird, focus), the day's
 * timeline in order (reminders, tasks after a prayer, the wird), and quick ticks; every part opens its own section.
 */
@Composable
fun DayOverview() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val tasks = Store.tasks().filter { it.on(LocalDate.now()) }
    val open = tasks.filter { !it.done }
    val doneToday = tasks.count { it.done && Instant.ofEpochMilli(it.doneAt).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now() }
    val habits = Data.habits()
    val wird = Data.wirdToday(); val read = Data.pagesToday()
    val (_, focusMin) = Store.focusToday()
    val parts = listOf(doneToday to (doneToday + open.size), habits.count { it.doneToday } to habits.size, read.coerceAtMost(wird) to wird).filter { it.second > 0 }
    val pct = if (parts.isEmpty()) 0 else parts.sumOf { it.first } * 100 / parts.sumOf { it.second }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint("today", "☀", "«اليوم» يجمع يومك في مكان واحد: ما أنجزت، وجدول اليوم بالترتيب، وتأشير سريع. اضغط أي بطاقة لتفتح قسمها.")
        // How the day is going, in one line and one bar.
        Panel(color = P.accent.copy(alpha = .10f), pad = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Sub("يومك", color = P.gold, size = 12.sp)
                    Text(if (parts.isEmpty()) "ابدأ يومك بمهمة أو عادة" else "أنجزت ${Quran.digits(pct)}٪ من يومك", color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Glyph(if (pct >= 100) "🌟" else if (pct >= 50) "🌤" else "🌱", tint = P.gold, size = 32.dp)
            }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(L.shape(4.dp)).background(P.line)) {
                Box(Modifier.fillMaxWidth(pct / 100f).height(8.dp).clip(L.shape(4.dp)).background(P.accent))
            }
        }
        // The four parts, each opening its section.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("✅", "المهام", "${Quran.digits(doneToday)} / ${Quran.digits(doneToday + open.size)}", Modifier.weight(1f)) { Nav.daily = Daily.TASKS }
            Stat("🔁", "العادات", "${Quran.digits(habits.count { it.doneToday })} / ${Quran.digits(habits.size)}", Modifier.weight(1f)) { Nav.daily = Daily.HABITS }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("📖", "الورد", if (Data.khatmaPage >= Data.PAGES) "ختمت 🌙" else "${Quran.digits(read.coerceAtMost(wird))} / ${Quran.digits(wird)} ص", Modifier.weight(1f)) { Nav.daily = Daily.KHATMA }
            Stat("🎯", "التركيز", "${Quran.digits(focusMin)} دقيقة", Modifier.weight(1f)) { Nav.daily = Daily.FOCUS }
        }

        // The day's timeline: what has a time, in order.
        val today = LocalDate.now()
        val times = if (Store.hasLocation) runCatching { Store.times(today) }.getOrNull() else null
        data class Slot(val minute: Int, val icon: String, val text: String, val done: Boolean, val tick: (() -> Unit)?, val open: () -> Unit)
        val slots = ArrayList<Slot>()
        tasks.filter { it.remindAt > 0 && Instant.ofEpochMilli(it.remindAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }.forEach { t ->
            val z = Instant.ofEpochMilli(t.remindAt).atZone(ZoneId.systemDefault())
            slots += Slot(z.hour * 60 + z.minute, "⏰", t.text, t.done, { Store.toggleTask(t.id) }) { Nav.daily = Daily.TASKS }
        }
        if (times != null) tasks.filter { it.after != null && it.remindAt == 0L }.forEach { t ->
            slots += Slot(times.getValue(t.after!!), "🕌", "${t.text} · بعد ${t.after.ar}", t.done, { Store.toggleTask(t.id) }) { Nav.daily = Daily.TASKS }
        }
        if (wird > 0 && Data.khatmaPage < Data.PAGES) slots += Slot(Data.khatmaRemindAt.takeIf { it >= 0 } ?: (times?.getValue(Prayer.FAJR)?.plus(30) ?: 6 * 60), "📖",
            "ورد القرآن: ${Quran.digits(wird)} صفحات", read >= wird, null) { Nav.wird = true }
        if (slots.isNotEmpty()) {
            Title("جدول اليوم")
            val now = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
            slots.sortedBy { it.minute }.forEach { s ->
                Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(if (s.done) P.accent.copy(alpha = .08f) else P.surface).border(1.dp, if (!s.done && s.minute <= now) P.gold.copy(alpha = .6f) else P.line, L.shape(14.dp))
                    .clickable(onClick = s.open).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Alerts.timeText(s.minute), color = if (!s.done && s.minute <= now) P.gold else P.muted, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                    Glyph(s.icon, tint = P.accent, size = 18.dp)
                    Text(s.text, color = if (s.done) P.muted else P.ink, fontSize = 15.sp, textDecoration = if (s.done) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 2)
                    s.tick?.let { tick ->
                        Box(Modifier.size(26.dp).clip(L.shape(8.dp)).background(if (s.done) P.accent else P.surface).border(2.dp, if (s.done) P.accent else P.muted, L.shape(8.dp)).clickable(onClick = tick),
                            contentAlignment = Alignment.Center) { if (s.done) Text("✓", color = P.onAccent, fontWeight = FontWeight.Bold) }
                    } ?: Text(if (s.done) "✓" else "اقرأ ›", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Tasks without a time, ticked from here; and the habits still open today.
        val loose = open.filter { it.remindAt == 0L && it.after == null }
        if (loose.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Title("مهام بلا وقت", Modifier.weight(1f))
                Text("كل المهام ›", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Nav.daily = Daily.TASKS }.padding(6.dp))
            }
            loose.take(5).forEach { t ->
                Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.surface).clickable { Store.toggleTask(t.id) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).clip(L.shape(6.dp)).border(2.dp, P.muted, L.shape(6.dp)))
                    Text(t.text + if (t.repeat) "  🔁" else "", color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                    Glyph("🎯", tint = P.accent, size = 17.dp, modifier = Modifier.clip(L.shape(8.dp)).clickable { focusOn(t) }.padding(6.dp))
                }
            }
        }
        val habitsLeft = habits.filter { !it.doneToday }
        if (habitsLeft.isNotEmpty()) {
            Title("عادات اليوم")
            Wrap(gap = 6.dp) { habitsLeft.forEach { h -> Choice("◻ ${h.name}" + if (h.streak > 1) " · ${Quran.digits(h.streak)}🔥" else "", false) { Data.toggleHabit(h.id) } } }
        }
        EveningReview()
        if (parts.isEmpty() && slots.isEmpty()) Sub("أضف مهمة من «المهام»، أو عادة من «العادات»، أو اضبط خطة ختمتك من «الختمة»؛ وستجتمع كلها هنا.", size = 13.sp)
    }
}

/** Starts a focus session on a task (its name on the timer; its end offers «أنجزت المهمة»). */
fun focusOn(t: Store.Task) { Store.focusTask = t.id; Store.focusTitle = t.text; Nav.daily = Daily.FOCUS }

@Composable
private fun Stat(icon: String, label: String, value: String, modifier: Modifier, open: () -> Unit) {
    Column(modifier.clip(L.shape(16.dp)).background(P.surface).border(1.dp, P.line, L.shape(16.dp)).clickable(onClick = open).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Glyph(icon, tint = P.accent, size = 18.dp); Sub("  $label", size = 13.sp) }
        Text(value, color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Seven small bars, one per day (today last), with the day's initial under each. */
@Composable
fun WeekBars(days: List<Pair<LocalDate, Int>>, unit: String, color: Color = P.accent) {
    val max = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val names = listOf("ن", "ث", "ر", "خ", "ج", "س", "ح")   // Monday … Sunday
    Column {
        Row(Modifier.fillMaxWidth().height(70.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            days.forEach { (_, n) ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (n > 0) Text(Quran.digits(n), color = P.muted, fontSize = 10.sp)
                    Box(Modifier.fillMaxWidth().height((56f * n / max).coerceAtLeast(if (n > 0) 4f else 2f).dp).clip(L.shape(4.dp)).background(if (n > 0) color else P.line))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { (d, _) -> Text(if (d == LocalDate.now()) "اليوم" else names[d.dayOfWeek.value - 1], color = if (d == LocalDate.now()) P.accent else P.muted, fontSize = 10.sp,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Sub("آخر ٧ أيام · المجموع ${Quran.digits(days.sumOf { it.second })} $unit", Modifier.padding(top = 4.dp), size = 11.sp)
    }
}

