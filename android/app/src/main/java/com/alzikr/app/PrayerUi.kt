package com.alzikr.app

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

data class City(val name: String, val lat: Double, val lng: Double, val method: Method)

val CITIES = listOf(
    City("مكة المكرمة", 21.4225, 39.8262, Method.MAKKAH), City("المدينة المنورة", 24.4672, 39.6024, Method.MAKKAH),
    City("الرياض", 24.7136, 46.6753, Method.MAKKAH), City("جدة", 21.4858, 39.1925, Method.MAKKAH),
    City("القاهرة", 30.0444, 31.2357, Method.EGYPT), City("الإسكندرية", 31.2001, 29.9187, Method.EGYPT),
    City("عمّان", 31.9454, 35.9284, Method.JORDAN), City("القدس", 31.7683, 35.2137, Method.JORDAN),
    City("دمشق", 33.5138, 36.2765, Method.EGYPT), City("بيروت", 33.8938, 35.5018, Method.EGYPT),
    City("بغداد", 33.3152, 44.3661, Method.MWL), City("الكويت", 29.3759, 47.9774, Method.KUWAIT),
    City("دبي", 25.2048, 55.2708, Method.DUBAI), City("أبوظبي", 24.4539, 54.3773, Method.DUBAI),
    City("الدوحة", 25.2854, 51.5310, Method.QATAR), City("المنامة", 26.2285, 50.5860, Method.MAKKAH),
    City("مسقط", 23.5880, 58.3829, Method.MAKKAH), City("صنعاء", 15.3694, 44.1910, Method.MWL),
    City("الخرطوم", 15.5007, 32.5599, Method.EGYPT), City("طرابلس", 32.8872, 13.1913, Method.EGYPT),
    City("تونس", 36.8065, 10.1815, Method.MWL), City("الجزائر", 36.7538, 3.0588, Method.MWL),
    City("الرباط", 34.0209, -6.8416, Method.MWL), City("إسطنبول", 41.0082, 28.9784, Method.TURKEY),
    City("برلين", 52.5200, 13.4050, Method.MWL), City("لندن", 51.5074, -0.1278, Method.MWL),
)

/** The prayer that is due now (the last one that started) and the next one, with their epoch times. */
data class Window(val current: Prayer?, val next: Prayer, val nextAt: Long, val prevAt: Long)

fun window(now: LocalDateTime = LocalDateTime.now()): Window {
    val zone = ZoneId.systemDefault()
    fun at(d: LocalDate, m: Int) = d.atStartOfDay(zone).plusMinutes(m.toLong()).toInstant().toEpochMilli()
    val today = now.toLocalDate()
    val t = Store.times(today)
    val nowMs = now.atZone(zone).toInstant().toEpochMilli()
    val order = Prayer.entries
    val next = order.firstOrNull { at(today, t.getValue(it)) > nowMs }
    return if (next != null) {
        val i = order.indexOf(next)
        val prev = if (i > 0) order[i - 1] else null
        val prevAt = if (prev != null) at(today, t.getValue(prev)) else at(today.minusDays(1), Store.times(today.minusDays(1)).getValue(Prayer.ISHA))
        Window(prev ?: Prayer.ISHA, next, at(today, t.getValue(next)), prevAt)
    } else {
        val tomorrow = today.plusDays(1)
        Window(Prayer.ISHA, Prayer.FAJR, at(tomorrow, Store.times(tomorrow).getValue(Prayer.FAJR)), at(today, t.getValue(Prayer.ISHA)))
    }
}

@Composable
fun PrayerScreen(openSettings: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    if (!Store.hasLocation) { LocationSetup(); return }
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    var options by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var sheetFor by remember { mutableStateOf<Prayer?>(null) }
    val pick: (Prayer) -> Unit = { sheetFor = it }
    Nav.prayerSheet?.let { sheetFor = it; Nav.prayerSheet = null }
    val w = window()
    val times = Store.times()
    val view = when (Data.prayerView) { 1 -> 1; 2 -> 2; 3 -> 3; 4 -> 4; else -> when (L.prayers) { PrayerLayout.CARDS -> 1; PrayerLayout.LIST -> 2; PrayerLayout.TIMELINE -> 4 } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UpdateBanner()
        Hint("prayer3", "🕌", "اضغط على أي صلاة لصفحتها: ✓ صلّيتها، والمنبه، والتذكير، والأذان، وسننها وأذكارها. والموقع وطريقة الحساب في السطر الأخير أسفل الصفحة. اضغط التاريخ في الأعلى للتقويم. علّم ✓ بعد أن تصلي، و🔔 لتنبيه كل صلاة.")
        Hint("home1", "✎", "«ذكر الوقت» يتغير وحده: الصباح والمساء والنوم، والكهف يوم الجمعة، والمواسم. ورتّب هذه الصفحة كما تحب من «✎ تخصيص هذه الصفحة» في أسفلها.")
        val hijri = Data.hijri()
        if (hijri.month == 9) Panel(color = P.gold.copy(alpha = .16f), pad = 12.dp) {
            Text("رمضان ${hijri.day} · تقبّل الله صيامك", color = P.gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val iftar = times.getValue(Prayer.MAGHRIB); val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
            Sub("الإمساك ${Alerts.timeText(times.getValue(Prayer.FAJR) - 10)} · الإفطار ${Alerts.timeText(iftar)}" +
                if (nowMin < iftar && nowMin > times.getValue(Prayer.FAJR)) " · بعد ${durationText((iftar - nowMin) * 60_000L)}" else "", color = P.ink)
        }
        val open = { options = true }
        val parts = mapOf<String, @Composable () -> Unit>(
            "next" to {
            // The next prayer, the countdown, and how far the current time has gone.
            Panel(Modifier.clickable { sheetFor = w.next }, color = P.card, pad = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Sub(if (w.next == Prayer.SUNRISE) "الشروق" else "الصلاة القادمة", size = 12.sp)
                        Text(w.next.ar, color = P.ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Alerts.timeText(Store.times(java.time.Instant.ofEpochMilli(w.nextAt).atZone(ZoneId.systemDefault()).toLocalDate()).getValue(w.next)), color = P.gold, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text("بعد " + durationText(w.nextAt - now), color = P.accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(10.dp))
                val frac = ((now - w.prevAt).toFloat() / (w.nextAt - w.prevAt).coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.line)) {
                    Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.accent))
                }
                w.current?.takeIf { it.alertable && it != w.next }?.let { cur ->
                    Sub("الوقت الحالي: ${cur.ar}" + if (Data.prayed(LocalDate.now(), cur)) " ✓ صلّيتها" else " · لم تُعلَّم بعد", Modifier.padding(top = 6.dp), size = 12.sp)
                }
            }
            },
            "moment" to { MomentCard() },
            "forbid" to { ForbidBanner() },
            "tomorrow" to { TomorrowCard { Nav.daySheet = it } },
            "tiles" to { HomeShortcuts() },
            "times" to {
            // Today's times, in the chosen view.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("مواقيت اليوم", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (Data.viewChips) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1 to "بطاقات", 2 to "قائمة", 3 to "سطر", 4 to "خط زمني").forEach { (n, l) -> Choice(l, view == n) { Data.prayerView = n } }
                }
                // A small button shows or hides the view choice.
                androidx.compose.foundation.layout.Box(Modifier.padding(start = 4.dp).clip(L.shape(10.dp)).clickable { Data.viewChips = !Data.viewChips }.padding(6.dp)) {
                    Glyph(if (Data.viewChips) "✕" else "🎨", tint = P.muted, size = 20.dp)
                }
            }
            when (view) {
                1 -> PrayerCards(w, times, pick)
                3 -> PrayerLine(w, times, pick)
                4 -> PrayerTimeline(w, times, pick)
                else -> Panel(pad = 6.dp) { Prayer.entries.forEach { p -> PrayerRow(p, p == w.next, times, pick) } }
            }
            },
            "extra" to { ExtraTimes(times, open) },
            "stats" to {
            // This week, and the next occasion.
            val (prayedWeek, possible) = Data.week()
            val today = Prayer.entries.count { it.alertable && Data.prayed(LocalDate.now(), it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Panel(Modifier.weight(1f), pad = 12.dp) {
                    Sub("صلوات اليوم", size = 12.sp)
                    Text("$today / 5", color = if (today == 5) P.gold else P.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                if (possible > 0) Panel(Modifier.weight(1f), pad = 12.dp) {
                    Sub("آخر 7 أيام", size = 12.sp)
                    Text("$prayedWeek / $possible", color = if (prayedWeek == possible) P.gold else P.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                nextOccasion()?.let { (o, days) ->
                    Panel(Modifier.weight(1f).clickable { Nav.morePage = Seasons.forOccasion(o.name)?.let { "season:$it" } ?: "calendar"; Nav.go("more") }, pad = 12.dp) {
                        Sub(o.name, size = 12.sp)
                        Text(if (days == 0L) "اليوم" else "بعد $days يومًا", color = P.gold, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            },
            "notice" to {
            // The lasting notification.
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { TasbihNotice.on = !TasbihNotice.on; TasbihNotice.update(ctx) }
                .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GText("📌 إشعار دائم في قائمة الإشعارات", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Sub("الصلاة القادمة بعدّ تنازلي وزر «سبّح +١»، يبقى حتى تضغط «إخفاء»", size = 11.sp)
                }
                androidx.compose.material3.Switch(TasbihNotice.on, { TasbihNotice.on = it; TasbihNotice.update(ctx) })
            }
            },
            "words" to { DailyWords() },
            "place" to {
            val qibla = PrayTimes.qibla(Store.lat, Store.lng).roundToInt()
            Sub("📍 ${Store.place.ifBlank { "%.3f، %.3f".format(Store.lat, Store.lng) }} · ${Store.method.ar}${if (Store.hanafi) " · العصر حنفي" else ""} · 🕋 $qibla°",
                Modifier.clip(L.shape(10.dp)).clickable { options = true }.padding(6.dp), size = 12.sp)
            },
        )
        Home.order.filter { it !in Home.hidden }.forEach { parts[it]?.invoke() }
        GText("✎ تخصيص هذه الصفحة", color = P.accent, fontSize = 14.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable { editing = true }.padding(8.dp))
    }
    if (options) TimesOptions(openSettings) { options = false }
    if (editing) HomeEditor { editing = false }
    sheetFor?.let { p -> PrayerSheet(p, { options = true }, openSettings) { sheetFor = null } }
}

/** Other useful times: imsak, duha, the middle of the night and its last third. */
@Composable
private fun ExtraTimes(times: Map<Prayer, Int>, open: () -> Unit) {
    val fajrTomorrow = Store.times(LocalDate.now().plusDays(1)).getValue(Prayer.FAJR) + 1440
    val maghrib = times.getValue(Prayer.MAGHRIB)
    val night = fajrTomorrow - maghrib
    val items = listOf("الإمساك" to times.getValue(Prayer.FAJR) - 10, "الضحى" to times.getValue(Prayer.SUNRISE) + 15,
        "منتصف الليل" to (maghrib + night / 2) % 1440, "الثلث الأخير" to (maghrib + night * 2 / 3) % 1440)
    Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable(onClick = open).padding(vertical = 8.dp)) {
        items.forEach { (n, m) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(n, color = P.muted, fontSize = 11.sp, maxLines = 1)
                Text(Alerts.timeText(m), color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Tapping a time: location, method, Asr, small corrections, the Hijri date, and the calendars. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TimesOptions(openSettings: () -> Unit, close: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val state = androidx.compose.material3.rememberModalBottomSheetState()
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = close, sheetState = state, containerColor = P.surface) {
        Column(Modifier.fillMaxWidth().quickDismiss(state, close).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Title("خيارات الأوقات")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile("📅", "التقويم", Modifier.weight(1f)) { close(); Nav.morePage = "calendar"; Nav.go("more") }
                Tile("🗓", "مواقيت الشهر", Modifier.weight(1f)) { close(); Nav.morePage = "month"; Nav.go("more") }
                Tile("🧭", "القبلة", Modifier.weight(1f)) { close(); Nav.morePage = "qibla"; Nav.go("more") }
            }
            Sub("الموقع: ${Store.place.ifBlank { "%.3f، %.3f".format(Store.lat, Store.lng) }}", Modifier.padding(top = 6.dp))
            LocationSetup(compact = true)
            Sub("طريقة الحساب", Modifier.padding(top = 6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Method.entries.forEach { m -> Choice(m.ar, Store.method == m) { Store.method = m; Store.methodChosen = true; Alerts.reschedule(ctx) } }
            }
            Sub("وقت العصر")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("الجمهور (الظل مثله)", !Store.hanafi) { Store.hanafi = false; Alerts.reschedule(ctx) }
                Choice("الحنفي (مثلاه)", Store.hanafi) { Store.hanafi = true; Alerts.reschedule(ctx) }
            }
            Sub("تعديل الأوقات بالدقائق، والتنبيه لكل صلاة", Modifier.padding(top = 6.dp))
            Prayer.entries.forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.ar, color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(Alerts.timeText(Store.times().getValue(p)), color = P.muted, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                    Choice("−", false) { Store.setOffset(p, Store.offset(p) - 1); Alerts.reschedule(ctx) }
                    Text(Store.offset(p).let { if (it > 0) "+$it" else "$it" }, color = P.ink, fontSize = 14.sp, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Choice("+", false) { Store.setOffset(p, Store.offset(p) + 1); Alerts.reschedule(ctx) }
                    Box(Modifier.width(44.dp), contentAlignment = Alignment.CenterEnd) { Bell(p) }
                }
            }
            Sub("تعديل التاريخ الهجري (إن خالف رؤية بلدك)", Modifier.padding(top = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("−", false) { Data.hijriAdjust = Data.hijriAdjust - 1 }
                Text(Data.hijriAdjust.let { if (it > 0) "+$it" else "$it" } + " يوم", color = P.ink, fontSize = 14.sp)
                Choice("+", false) { Data.hijriAdjust = Data.hijriAdjust + 1 }
                Sub(Data.hijri().let { "${it.day} ${it.monthName} ${it.year} هـ" }, size = 12.sp)
            }
            Text("مزيد من الإعدادات (الأذان والتذكير والصوت) ›", color = P.accent, fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp).clip(L.shape(10.dp)).clickable { close(); openSettings() }.padding(8.dp))
        }
    }
}

/** First start: where are you? GPS, or a city from the list. */
@Composable
fun LocationSetup(compact: Boolean = false, done: () -> Unit = {}) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    fun useGps() {
        busy = true
        Alerts.locate(ctx) { loc ->
            if (loc == null) { busy = false; Toast.makeText(ctx, "تعذّر تحديد الموقع. فعّل الموقع أو اختر مدينة.", Toast.LENGTH_LONG).show(); return@locate }
            Store.setLocation(loc.latitude, loc.longitude, "")
            Alerts.reschedule(ctx)
            Alerts.resolvePlace(ctx, loc.latitude, loc.longitude)
            busy = false; done()
        }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) useGps() else Toast.makeText(ctx, "بدون إذن الموقع اختر مدينتك من القائمة", Toast.LENGTH_LONG).show() }
    Column(if (compact) Modifier.fillMaxWidth() else Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!compact) Panel(color = P.card) {
            Title("أهلًا بك في ALzikr")
            Sub("لحساب أوقات الصلاة بدقة نحتاج موقعك التقريبي. يُحسب كل شيء على هاتفك، ولا يُرسل موقعك إلى أي مكان.", size = 14.sp)
        }
        Action(if (busy) "جارٍ تحديد الموقع…" else "📍 استخدم موقعي الحالي", Modifier.fillMaxWidth()) {
            if (busy) return@Action
            if (ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) useGps() else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        Action("اختيار مدينة", Modifier.fillMaxWidth(), filled = false) { manual = true }
    }
    if (manual) CityDialog(onDismiss = { manual = false }) { city ->
        Store.setLocation(city.lat, city.lng, city.name)
        if (city.name.isNotEmpty()) { Store.method = city.method; Store.methodChosen = true }
        Alerts.reschedule(ctx); manual = false; done()
    }
}

@Composable
fun CityDialog(onDismiss: () -> Unit, onPick: (City) -> Unit) {
    val ctx = LocalContext.current
    var coords by remember { mutableStateOf(false) }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = P.surface,
        title = { Text(if (coords) "إحداثيات الموقع" else "اختر مدينتك", color = P.ink) },
        text = {
            if (coords) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(lat, { lat = it }, label = { Text("خط العرض (مثل 24.71)") }, singleLine = true)
                OutlinedTextField(lng, { lng = it }, label = { Text("خط الطول (مثل 46.67)") }, singleLine = true)
            } else LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(CITIES) { c ->
                    Text(c.name, color = P.ink, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).clickable { onPick(c) }.padding(horizontal = 10.dp, vertical = 11.dp))
                }
            }
        },
        confirmButton = {
            if (coords) TextButton({
                val a = lat.trim().replace('،', '.').toDoubleOrNull(); val b = lng.trim().replace('،', '.').toDoubleOrNull()
                if (a == null || b == null || a !in -66.0..66.0 || b !in -180.0..180.0) Toast.makeText(ctx, "أدخل أرقامًا صحيحة", Toast.LENGTH_SHORT).show()
                else onPick(City("", a, b, Store.method))
            }) { Text("حفظ") }
            else TextButton({ coords = true }) { Text("إدخال إحداثيات") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

/** The "prayed" check: only once the prayer's time has come (or to undo). */
@Composable
private fun PrayedBox(p: Prayer, times: Map<Prayer, Int>) {
    if (!p.alertable) { Box(Modifier.size(26.dp)); return }
    val today = LocalDate.now()
    val started = times.getValue(p) <= java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val done = Data.prayed(today, p)
    Box(Modifier.size(26.dp).clip(L.shape(8.dp)).background(if (done) P.accent else P.surface)
        .border(2.dp, if (done) P.accent else if (started) P.muted else P.line, L.shape(8.dp))
        .clickable(enabled = started || done) { Data.setPrayed(today, p, !done) }, contentAlignment = Alignment.Center) {
        if (done) Text("✓", color = P.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Bell(p: Prayer) {
    if (!p.alertable) return
    val on = Store.alert(p)
    val ctx = LocalContext.current
    GText(if (on) "🔔" else "🔕", fontSize = 16.sp, color = if (on) P.ink else P.muted,
        modifier = Modifier.clip(L.shape(8.dp)).clickable { Store.setAlert(p, !on); Alerts.reschedule(ctx) }.padding(horizontal = 8.dp, vertical = 2.dp))
}

@Composable
private fun PrayerRow(p: Prayer, isNext: Boolean, times: Map<Prayer, Int>, open: (Prayer) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(if (isNext) P.accent.copy(alpha = .12f) else P.surface).clickable { open(p) }
        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        PrayedBox(p, times)
        Spacer(Modifier.size(12.dp))
        Text(p.ar, color = if (isNext) P.accent else if (p.alertable) P.ink else P.muted, fontSize = 17.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(Alerts.timeText(times.getValue(p)), color = if (isNext) P.accent else P.ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.size(width = 48.dp, height = 32.dp), contentAlignment = Alignment.CenterEnd) { Bell(p) }
    }
}

/** Two columns of cards; the next prayer's card is filled with the accent. */
@Composable
private fun PrayerCards(w: Window, times: Map<Prayer, Int>, open: (Prayer) -> Unit) {
    Prayer.entries.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { p ->
                val next = p == w.next
                Column(Modifier.weight(1f).clip(L.shape()).background(if (next) P.accent else P.card).border(1.dp, if (next) P.accent else P.line, L.shape())
                    .clickable { open(p) }.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.ar, color = if (next) P.onAccent else if (p.alertable) P.ink else P.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = L.head ?: L.body, modifier = Modifier.weight(1f))
                        if (!next) Bell(p)
                    }
                    Text(Alerts.timeText(times.getValue(p)), color = if (next) P.onAccent else P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    if (p.alertable && !next) PrayedBox(p, times) else if (next) Text("القادمة", color = P.onAccent.copy(alpha = .8f), fontSize = 12.sp)
                }
            }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** A vertical line through the day; passed prayers are filled dots, the next one glows. */
@Composable
private fun PrayerTimeline(w: Window, times: Map<Prayer, Int>, open: (Prayer) -> Unit) {
    val now = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val line = P.line; val accent = P.accent
    Panel(pad = 10.dp) {
        Prayer.entries.forEachIndexed { i, p ->
            val passed = times.getValue(p) <= now
            val next = p == w.next
            Row(Modifier.fillMaxWidth().height(52.dp).clickable { open(p) }, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(28.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val x = size.width / 2
                        if (i > 0) drawLine(if (passed) accent else line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height / 2), strokeWidth = 3.dp.toPx())
                        if (i < Prayer.entries.size - 1) drawLine(if (passed && !next) accent else line, androidx.compose.ui.geometry.Offset(x, size.height / 2), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 3.dp.toPx())
                        if (next) drawCircle(accent.copy(alpha = .25f), 13.dp.toPx())
                        drawCircle(if (passed || next) accent else line, if (next) 8.dp.toPx() else 6.dp.toPx())
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.ar, color = if (next) P.accent else if (p.alertable) P.ink else P.muted, fontSize = if (next) 19.sp else 16.sp, fontWeight = if (next) FontWeight.Bold else FontWeight.Medium, fontFamily = L.head ?: L.body)
                    if (next) Sub("القادمة", size = 11.sp)
                }
                Text(Alerts.timeText(times.getValue(p)), color = if (next) P.accent else P.ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                PrayedBox(p, times)
                Box(Modifier.width(44.dp), contentAlignment = Alignment.CenterEnd) { Bell(p) }
            }
        }
    }
}

/** All six times in one line; the next is filled, a ✓ under each prayed one. */
@Composable
private fun PrayerLine(w: Window, times: Map<Prayer, Int>, open: (Prayer) -> Unit) {
    val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val shape = L.shape(16.dp)
    // One bar: six equal columns with thin dividers, the next prayer tinted, and the day's progress along the bottom.
    Column(Modifier.fillMaxWidth().clip(shape).background(P.surface).border(1.dp, P.line, shape)) {
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            Prayer.entries.forEachIndexed { i, p ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp).background(P.line))
                val next = p == w.next
                val passed = !next && times.getValue(p) <= nowMin
                val done = p.alertable && Data.prayed(LocalDate.now(), p)
                val t = Alerts.timeText(times.getValue(p))
                Column(Modifier.weight(1f).fillMaxHeight().background(if (next) P.accent.copy(alpha = .16f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { open(p) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.ar, color = if (next) P.accent else P.muted, fontSize = 11.sp, fontWeight = if (next) FontWeight.Bold else FontWeight.Normal, maxLines = 1, softWrap = false)
                    Text(t.substringBefore(' '), color = if (next) P.accent else if (passed) P.ink.copy(alpha = .55f) else P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                        modifier = Modifier.padding(top = 2.dp))
                    Text(t.substringAfter(' ', ""), color = P.muted, fontSize = 10.sp, maxLines = 1)
                    Box(Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                        when {
                            done -> Text("✓", color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            next -> Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(P.accent))
                        }
                    }
                }
            }
        }
        val fajr = times.getValue(Prayer.FAJR); val isha = times.getValue(Prayer.ISHA)
        val frac = ((nowMin - fajr).toFloat() / (isha - fajr).coerceAtLeast(1)).coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(3.dp).background(P.line.copy(alpha = .6f))) {
            Box(Modifier.fillMaxWidth(frac).height(3.dp).background(P.accent))
        }
    }
}
