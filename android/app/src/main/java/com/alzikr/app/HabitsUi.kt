package com.alzikr.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Daily good deeds: tap to mark today, a streak per habit and the last 7 days as dots. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitsList() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Data.Habit?>(null) }
    val habits = Data.habits()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Hint("habits", "🔥", "اضغط على العادة عندما تنجزها اليوم. كل يوم متتالٍ يزيد سلسلتك 🔥. اضغط مطولًا للحذف.") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Title("أعمالي اليومية")
                    Sub("أنجزت ${habits.count { it.doneToday }} من ${habits.size} اليوم")
                }
                Text("+ عادة", color = P.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(L.shape(10.dp)).combinedClickable(onClick = { adding = true }).padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        items(habits, key = { it.id }) { h ->
            Row(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(if (h.doneToday) P.accent.copy(alpha = .12f) else P.surface)
                .border(1.dp, P.line, L.shape(16.dp)).combinedClickable(onClick = { Data.toggleHabit(h.id) }, onLongClick = { deleting = h })
                .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(L.shape(8.dp)).background(if (h.doneToday) P.accent else P.surface).border(2.dp, if (h.doneToday) P.accent else P.muted, L.shape(8.dp)),
                    contentAlignment = Alignment.Center) { if (h.doneToday) Text("✓", color = P.onAccent, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name, color = P.ink, fontSize = 16.sp)
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (6 downTo 0).forEach { i ->
                            val on = LocalDate.now().minusDays(i.toLong()).toString() in h.days
                            Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(if (on) P.accent else P.line))
                        }
                    }
                }
                if (h.streak > 1) GText("${h.streak} 🔥", color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        item { Sub("اضغط لتسجيل اليوم، واضغط مطولًا للحذف.", Modifier.padding(6.dp), size = 12.sp) }
    }
    if (adding) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { adding = false }, containerColor = P.surface,
            title = { Text("عادة يومية جديدة", color = P.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, placeholder = { Text("مثل: صلة الرحم") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("الوتر", "السنن الرواتب", "الصيام").forEach { s -> Choice(s, name == s) { name = s } } }
                }
            },
            confirmButton = { TextButton({ if (name.isNotBlank()) Data.addHabit(name.trim()); adding = false }) { Text("إضافة") } },
            dismissButton = { TextButton({ adding = false }) { Text("إلغاء") } })
    }
    deleting?.let { h ->
        AlertDialog(onDismissRequest = { deleting = null }, containerColor = P.surface,
            title = { Text("حذف «${h.name}»؟", color = P.ink) }, text = { Sub("يُحذف سجلها أيضًا.") },
            confirmButton = { TextButton({ Data.deleteHabit(h.id); deleting = null }) { Text("حذف", color = P.danger) } },
            dismissButton = { TextButton({ deleting = null }) { Text("إلغاء") } })
    }
}

/** Quran khatma by pages of the Madinah mushaf: where you are, today's portion, and the plan. */
@Composable
fun KhatmaPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var custom by remember { mutableStateOf(false) }
    var askDays by remember { mutableStateOf(false) }
    val page = Data.khatmaPage
    val wird = Data.wirdToday()
    val read = Data.pagesToday()
    val from = (page - read + 1).coerceAtLeast(1)
    val to = (page - read + wird).coerceAtMost(Data.PAGES)
    val end = Data.khatmaEnd()
    fun start() { if (Data.khatmaStarted.isEmpty()) Data.khatmaStarted = LocalDate.now().toString() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint("khatma2", "📗", "وردك هو نصيبك اليومي من ختمتك: عدد الصفحات التي تقرؤها اليوم لتختم في الموعد الذي تختاره. اضغط «اقرأ وردك» (أو «وردي» في الصفحة الرئيسية) فتظهر صفحات وردك فقط من المصحف على خلفية سوداء، وتُحسب كل صفحة تقلبها. أو سجّل ما قرأته من مصحف ورقي بالأزرار.")

        // Today's wird.
        if (page < Data.PAGES) Panel(color = P.accent.copy(alpha = .12f), pad = 14.dp) {
            Sub("ورد اليوم", color = P.gold, size = 12.sp)
            Text("من الصفحة $from إلى $to", color = P.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Sub("$wird صفحة (${"%.1f".format(wird / 20.0)} جزء) · قرأت منها ${read.coerceAtMost(wird)}" + if (read >= wird) " ✓ أتممت وردك، تقبّل الله" else "", color = if (read >= wird) P.accent else P.ink)
            Action(if (read >= wird) "📖 راجع ورد اليوم" else "📖 اقرأ وردك (${wird - read.coerceAtMost(wird)} صفحة)", Modifier.fillMaxWidth().padding(top = 8.dp)) { Nav.wird = true }
        }

        // The whole khatma: a tap opens the mushaf at the page reached (in the reading style chosen for the mushaf).
        Panel(Modifier.clickable {
            val at = Quran.firstOfPageSafe(ctx, page.coerceIn(1, Data.PAGES)).split(':').map { it.toInt() }
            MushafView.open(at[0], at[1])
        }, color = P.card) {
            Sub(if (Data.khatmaCount > 0) "الختمة ${Data.khatmaCount + 1}" else "ختمتك")
            Text(if (page == 0) "لم تبدأ بعد" else "الصفحة $page · الجزء ${Data.juzOf(page)}", color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(P.line)) {
                Box(Modifier.fillMaxWidth(page.toFloat() / Data.PAGES).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (page >= Data.PAGES) P.gold else P.accent))
            }
            Sub("${(page * 100 / Data.PAGES)}% · بقي ${Data.PAGES - page} صفحة" + if (page < Data.PAGES) " · تختم يوم ${end.dayOfMonth}/${end.monthValue} (بعد ${Data.daysLeft()} يومًا)" else "", Modifier.padding(top = 6.dp), size = 12.sp)
            Text(if (page == 0) "اضغط لتفتح المصحف من أوله ›" else "اضغط لتفتح المصحف عند الصفحة $page ›", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        }
        if (page >= Data.PAGES) {
            GText("ختمت القرآن، تقبّل الله منك 🌙", color = P.gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Action("ابدأ ختمة جديدة", Modifier.fillMaxWidth()) { Data.newKhatma() }
        } else {
            Sub("سجّل ما قرأت خارج وضع القراءة")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("+1 صفحة", Modifier.weight(1f)) { start(); Data.khatmaPage = page + 1 }
                if (read < wird) Action("+${wird - read} (باقي وردي)", Modifier.weight(1f), filled = false) { start(); Data.khatmaPage = page + (wird - read) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("+جزء", Modifier.weight(1f), filled = false) { start(); Data.khatmaPage = page + 20 }
                Action("رقم الصفحة…", Modifier.weight(1f), filled = false) { custom = true }
            }
        }

        // The plan: any length, or pages a day.
        Panel(pad = 14.dp) {
            Title("متى تختم؟")
            Sub("أتمّها خلال (من اليوم):", Modifier.padding(top = 6.dp))
            val left = Data.daysLeft()
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                PickOne(listOf(3, 7, 10, 15, 20, 30, 40, 60, 90).map { d -> d.toString() to (if (d == 7) "أسبوع" else if (d == 30) "شهر" else "$d يومًا") }, left.toString(), "أختم في", modifier = Modifier.weight(1f, fill = false)) { Data.finishIn(it.toInt()) }
                Choice("مدة أخرى…", false) { askDays = true }
            }
            Sub("أو اختر وردك اليومي بالصفحات:", Modifier.padding(top = 10.dp))
            PickOne(listOf(1, 2, 4, 5, 10, 20, 40).map { n -> n.toString() to (if (n == 20) "جزء" else if (n == 40) "جزآن" else "$n صفحات") }, wird.toString(), "الورد اليومي", shown = 3, modifier = Modifier.padding(top = 6.dp)) { Data.setPagesPerDay(it.toInt()) }
            Sub("تذكير يومي بالورد", Modifier.padding(top = 10.dp))
            val remind = Data.khatmaRemindAt
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice(if (remind < 0) "متوقف" else "⏰ " + Alerts.timeText(remind), remind >= 0) {
                    val m0 = if (remind >= 0) remind else 20 * 60
                    android.app.TimePickerDialog(ctx, { _, h, m -> Data.khatmaRemindAt = h * 60 + m; Alerts.reschedule(ctx) }, m0 / 60, m0 % 60, android.text.format.DateFormat.is24HourFormat(ctx)).show()
                }
                Text(if (remind < 0) "اضغط لاختيار الوقت" else "اضغط لتغيير الوقت", color = P.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (remind >= 0) Choice("إيقاف", false) { Data.khatmaRemindAt = -1; Alerts.reschedule(ctx) }
            }
            Text("البدء من الصفحة الأولى", color = P.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp).clip(L.shape(8.dp)).combinedClickableSafe { Data.newKhatma() }.padding(6.dp))
        }
    }
    if (custom) {
        var text by remember { mutableStateOf(if (page > 0) "$page" else "") }
        AlertDialog(onDismissRequest = { custom = false }, containerColor = P.surface,
            title = { Text("وصلت إلى الصفحة", color = P.ink) },
            text = { OutlinedTextField(text, { text = it.filter(Char::isDigit).take(3) }, singleLine = true, placeholder = { Text("1 – 604") }) },
            confirmButton = { TextButton({ text.toIntOrNull()?.let { start(); Data.khatmaPage = it }; custom = false }) { Text("حفظ") } },
            dismissButton = { TextButton({ custom = false }) { Text("إلغاء") } })
    }
    if (askDays) {
        var text by remember { mutableStateOf("${Data.daysLeft()}") }
        AlertDialog(onDismissRequest = { askDays = false }, containerColor = P.surface,
            title = { Text("أختم خلال كم يومًا؟", color = P.ink) },
            text = { OutlinedTextField(text, { text = it.filter(Char::isDigit).take(3) }, singleLine = true, placeholder = { Text("1 – 365") }) },
            confirmButton = { TextButton({ text.toIntOrNull()?.takeIf { it in 1..365 }?.let { Data.finishIn(it) }; askDays = false }) { Text("حفظ") } },
            dismissButton = { TextButton({ askDays = false }) { Text("إلغاء") } })
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSafe(onClick: () -> Unit) = this.combinedClickable(onClick = onClick)
