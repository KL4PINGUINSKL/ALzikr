package com.alzikr.app

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
import androidx.compose.material3.Text
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
                if (h.streak > 1) Text("${h.streak} 🔥", color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
    var custom by remember { mutableStateOf(false) }
    val page = Data.khatmaPage
    val days = Data.khatmaDays
    val perDay = ceil(Data.PAGES.toDouble() / days).toInt()
    val started = Data.khatmaStarted.takeIf { it.isNotEmpty() }?.let(LocalDate::parse)
    val dayNo = started?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() + 1 } ?: 1
    val shouldBe = (dayNo * perDay).coerceAtMost(Data.PAGES)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint("khatma", "📗", "سجّل أين وصلت بالأزرار، أو اضغط «اقرأ في المصحف» لتكمل من صفحتك. واختر مدة الختمة في الأسفل.")
        Panel(color = P.card) {
            Sub(if (Data.khatmaCount > 0) "الختمة ${Data.khatmaCount + 1}" else "ختمتك")
            Text(if (page == 0) "لم تبدأ بعد" else "الصفحة $page · الجزء ${Data.juzOf(page)}", color = P.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(P.line)) {
                Box(Modifier.fillMaxWidth(page.toFloat() / Data.PAGES).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (page >= Data.PAGES) P.gold else P.accent))
            }
            Sub("${(page * 100 / Data.PAGES)}% · بقي ${Data.PAGES - page} صفحة · اليوم قرأت ${Data.pagesToday()} صفحة", Modifier.padding(top = 6.dp), size = 12.sp)
            if (page in 1 until Data.PAGES && started != null) Sub(
                if (page >= shouldBe) "أنت على الخطة أو متقدم عليها، بارك الله فيك" else "لتبقى على الخطة: اقرأ حتى الصفحة $shouldBe اليوم",
                color = if (page >= shouldBe) P.accent else P.gold, size = 13.sp)
        }
        if (Quran.ready && page < Data.PAGES) Action("📖 اقرأ في المصحف من الصفحة ${page + 1}", Modifier.fillMaxWidth()) {
            val a = Quran.firstOfPage(page + 1); Nav.openQuran(a.s, a.a)
        }
        if (page >= Data.PAGES) {
            Text("ختمت القرآن، تقبّل الله منك 🌙", color = P.gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Action("ابدأ ختمة جديدة", Modifier.fillMaxWidth()) { Data.newKhatma() }
        } else {
            Sub("أين وصلت؟")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("+1 صفحة", Modifier.weight(1f)) { if (Data.khatmaStarted.isEmpty()) Data.khatmaStarted = LocalDate.now().toString(); Data.khatmaPage = page + 1 }
                Action("+$perDay (ورد اليوم)", Modifier.weight(1f), filled = false) { if (Data.khatmaStarted.isEmpty()) Data.khatmaStarted = LocalDate.now().toString(); Data.khatmaPage = page + perDay }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("+جزء", Modifier.weight(1f), filled = false) { if (Data.khatmaStarted.isEmpty()) Data.khatmaStarted = LocalDate.now().toString(); Data.khatmaPage = page + 20 }
                Action("رقم الصفحة…", Modifier.weight(1f), filled = false) { custom = true }
            }
        }
        Panel(pad = 14.dp) {
            Title("الخطة")
            Sub("أختم في: ", Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(7 to "أسبوع", 15 to "15 يومًا", 30 to "شهر", 60 to "شهرين").forEach { (d, l) -> Choice(l, days == d) { Data.khatmaDays = d } }
            }
            Sub("تذكير يومي بالورد", Modifier.padding(top = 10.dp))
            val ctx = androidx.compose.ui.platform.LocalContext.current
            Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(-1, 6 * 60, 8 * 60, 13 * 60, 17 * 60, 20 * 60, 21 * 60 + 30).forEach { m ->
                    Choice(if (m < 0) "إيقاف" else Alerts.timeText(m), Data.khatmaRemindAt == m) { Data.khatmaRemindAt = m; Alerts.reschedule(ctx) }
                }
            }
            Sub("$perDay صفحة يوميًا تقريبًا (${"%.1f".format(perDay / 20.0)} جزء)", Modifier.padding(top = 8.dp), size = 12.sp)
            Text("البدء من الصفحة الأولى", color = P.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp).clip(L.shape(8.dp)).combinedClickableSafe { Data.newKhatma() }.padding(6.dp))
        }
    }
    if (custom) {
        var text by remember { mutableStateOf(if (page > 0) "$page" else "") }
        AlertDialog(onDismissRequest = { custom = false }, containerColor = P.surface,
            title = { Text("وصلت إلى الصفحة", color = P.ink) },
            text = { OutlinedTextField(text, { text = it.filter(Char::isDigit).take(3) }, singleLine = true, placeholder = { Text("1 – 604") }) },
            confirmButton = { TextButton({ text.toIntOrNull()?.let { if (Data.khatmaStarted.isEmpty()) Data.khatmaStarted = LocalDate.now().toString(); Data.khatmaPage = it }; custom = false }) { Text("حفظ") } },
            dismissButton = { TextButton({ custom = false }) { Text("إلغاء") } })
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSafe(onClick: () -> Unit) = this.combinedClickable(onClick = onClick)
