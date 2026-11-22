package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

// ------------------------------------------------------------------ prayer alarms

@Composable
fun AlarmPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    var resumed by remember { mutableLongStateOf(0L) }
    // Coming back from the settings screen: check the permission again.
    @Suppress("DEPRECATION") val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumed = System.currentTimeMillis() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Hint("alarms", "⏰", "هذا منبه حقيقي مثل منبه الهاتف، غير تذكير الصلاة: يرنّ بصوت المنبه حتى لو كان الهاتف صامتًا، ويظهر على شاشة القفل، ويستمر حتى تضغط «إيقاف» أو «غفوة». فعّله للصلاة التي تريد، واختر قبلها أو بعدها بكم دقيقة.")
        if (!Store.hasLocation) { Sub("حدّد موقعك أولًا من صفحة الصلاة لتُحسب الأوقات."); return@Column }

        @Suppress("UNUSED_VARIABLE") val r = resumed
        if (PrayerAlarm.anyOn() && !PrayerAlarm.canFullScreen(ctx))
            Panel(Modifier.clickable { PrayerAlarm.fullScreenSettings(ctx) }, color = P.danger.copy(alpha = .14f), pad = 12.dp) {
                Text("⚠ اسمح للمنبه بالظهور على شاشة القفل", color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Sub("بدون هذا الإذن يرنّ المنبه لكن يظهر كإشعار فقط. اضغط هنا ثم فعّل «السماح».", size = 12.sp)
            }
        if (PrayerAlarm.anyOn() && !Alerts.canPost(ctx))
            Sub("⚠ الإشعارات متوقفة لهذا التطبيق، فلن تظهر أزرار المنبه. فعّلها من الإعدادات.", color = P.danger, size = 12.sp)

        val next = PrayerAlarm.next()
        val snoozed = PrayerAlarm.snoozedUntil()
        Panel(color = P.card, pad = 14.dp) {
            Sub("المنبه القادم", size = 12.sp)
            if (next == null) Text("لا يوجد منبه مفعّل", color = P.muted, fontSize = 18.sp)
            else {
                val (p, at) = next
                val z = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
                val day = when (z.toLocalDate()) { LocalDate.now() -> "اليوم"; LocalDate.now().plusDays(1) -> "غدًا"; else -> PrayerAlarm.dayNames.first { it.first == z.dayOfWeek.value }.second }
                Text("${p.ar} · ${Alerts.timeText(z.hour * 60 + z.minute)}", color = P.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Sub("$day · بعد ${durationText(at - System.currentTimeMillis())}", color = P.accent)
            }
            if (snoozed > 0) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Sub("💤 غفوة حتى ${Instant.ofEpochMilli(snoozed).atZone(ZoneId.systemDefault()).let { Alerts.timeText(it.hour * 60 + it.minute) }}", Modifier.weight(1f))
                Choice("إلغاء الغفوة", false) { PrayerAlarm.cancelSnooze(ctx); Store.bump() }
            }
        }

        val times = Store.times()
        Prayer.entries.forEach { p ->
            val on = PrayerAlarm.on(p)
            val off = PrayerAlarm.offset(p)
            Column(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(if (on) P.accent.copy(alpha = .10f) else P.surface).border(1.dp, if (on) P.accent.copy(alpha = .5f) else P.line, L.shape(16.dp)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.ar, color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Sub("الوقت ${Alerts.timeText(times.getValue(p))}" + if (on) " · يرنّ ${Alerts.timeText(((times.getValue(p) + off) % 1440 + 1440) % 1440)}" else "", size = 12.sp)
                    }
                    Text(if (on) Alerts.timeText(((times.getValue(p) + off) % 1440 + 1440) % 1440) else "", color = P.gold, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 10.dp))
                    Switch(on, { PrayerAlarm.setOn(ctx, p, it) })
                }
                if (on) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Choice("− ٥", false) { PrayerAlarm.setOffset(ctx, p, off - 5) }
                        Text(when { off < 0 -> "قبل الوقت بـ ${-off} د"; off > 0 -> "بعد الوقت بـ $off د"; else -> "عند دخول الوقت" },
                            color = P.ink, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(130.dp))
                        Choice("+ ٥", false) { PrayerAlarm.setOffset(ctx, p, off + 5) }
                    }
                    val days = PrayerAlarm.days(p)
                    Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PrayerAlarm.dayNames.forEach { (d, n) ->
                            Choice(n, d in days) { val nd = if (d in days) days - d else days + d; if (nd.isNotEmpty()) PrayerAlarm.setDays(ctx, p, nd) }
                        }
                    }
                }
            }
        }

        Title("الصوت والغفوة")
        Sub("الصوت")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Choice("نغمة منبه الهاتف", PrayerAlarm.sound == 0) { PrayerAlarm.sound = 0 }
            Choice("الأذان ثم النغمة", PrayerAlarm.sound == 1) { PrayerAlarm.sound = 1 }
            Choice("اهتزاز فقط", PrayerAlarm.sound == 2) { PrayerAlarm.sound = 2 }
        }
        if (PrayerAlarm.sound == 1 && !Adhan.has(ctx, Adhan.voice()))
            Sub("لم يُنزَّل صوت الأذان بعد (الإعدادات › الأذان)، فستُستخدم نغمة المنبه.", color = P.danger, size = 12.sp)
        Sub("مدة الغفوة")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { PrayerAlarm.snoozes.forEach { m -> Choice("$m د", PrayerAlarm.snooze == m) { PrayerAlarm.snooze = m } } }
        SettingRow("اهتزاز مع الصوت", null) { Switch(PrayerAlarm.vibrate, { PrayerAlarm.vibrate = it }) }
        SettingRow("يبدأ هادئًا ثم يعلو", "يرتفع الصوت تدريجيًا خلال نصف دقيقة") { Switch(PrayerAlarm.ramp, { PrayerAlarm.ramp = it }) }
        Sub("يتوقف وحده بعد ${PrayerAlarm.RING_MIN} دقائق إن لم تُجب. وأثناء المكالمة يهتز فقط.", size = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("🔔", "جرّب المنبه الآن", Modifier.weight(1f)) { PrayerAlarm.ring(ctx, null, test = true) }
        }
    }
}

// ------------------------------------------------------------------ world clock

object WorldClock {
    data class Place(val zone: String, val name: String)
    val places = listOf(
        Place("Asia/Riyadh", "مكة المكرمة"), Place("Asia/Riyadh", "المدينة المنورة"), Place("Asia/Riyadh", "الرياض"),
        Place("Asia/Jerusalem", "القدس"), Place("Africa/Cairo", "القاهرة"), Place("Asia/Amman", "عمّان"), Place("Asia/Damascus", "دمشق"),
        Place("Asia/Beirut", "بيروت"), Place("Asia/Baghdad", "بغداد"), Place("Asia/Kuwait", "الكويت"), Place("Asia/Qatar", "الدوحة"),
        Place("Asia/Bahrain", "المنامة"), Place("Asia/Dubai", "دبي"), Place("Asia/Muscat", "مسقط"), Place("Asia/Aden", "صنعاء"),
        Place("Africa/Khartoum", "الخرطوم"), Place("Africa/Tripoli", "طرابلس"), Place("Africa/Tunis", "تونس"), Place("Africa/Algiers", "الجزائر"),
        Place("Africa/Casablanca", "الدار البيضاء"), Place("Africa/Nouakchott", "نواكشوط"), Place("Africa/Mogadishu", "مقديشو"),
        Place("Europe/Istanbul", "إسطنبول"), Place("Asia/Tehran", "طهران"), Place("Asia/Karachi", "كراتشي"), Place("Asia/Kolkata", "دلهي"),
        Place("Asia/Dhaka", "دكا"), Place("Asia/Kuala_Lumpur", "كوالالمبور"), Place("Asia/Jakarta", "جاكرتا"), Place("Asia/Tashkent", "طشقند"),
        Place("Asia/Almaty", "ألماتي"), Place("Asia/Baku", "باكو"), Place("Africa/Lagos", "لاغوس"), Place("Africa/Dakar", "داكار"),
        Place("Africa/Nairobi", "نيروبي"), Place("Africa/Johannesburg", "جوهانسبرغ"),
        Place("Europe/Berlin", "برلين"), Place("Europe/Paris", "باريس"), Place("Europe/London", "لندن"), Place("Europe/Madrid", "مدريد"),
        Place("Europe/Rome", "روما"), Place("Europe/Amsterdam", "أمستردام"), Place("Europe/Brussels", "بروكسل"), Place("Europe/Vienna", "فيينا"),
        Place("Europe/Stockholm", "ستوكهولم"), Place("Europe/Oslo", "أوسلو"), Place("Europe/Copenhagen", "كوبنهاغن"), Place("Europe/Athens", "أثينا"),
        Place("Europe/Sarajevo", "سراييفو"), Place("Europe/Moscow", "موسكو"),
        Place("America/New_York", "نيويورك"), Place("America/Toronto", "تورونتو"), Place("America/Chicago", "شيكاغو"), Place("America/Denver", "دنفر"),
        Place("America/Los_Angeles", "لوس أنجلوس"), Place("America/Mexico_City", "مكسيكو"), Place("America/Sao_Paulo", "ساو باولو"),
        Place("America/Argentina/Buenos_Aires", "بوينس آيرس"),
        Place("Asia/Shanghai", "بكين"), Place("Asia/Tokyo", "طوكيو"), Place("Asia/Seoul", "سول"), Place("Asia/Singapore", "سنغافورة"),
        Place("Australia/Sydney", "سيدني"), Place("Pacific/Auckland", "أوكلاند"),
    )
    private val defaults = listOf("مكة المكرمة", "القاهرة", "إسطنبول", "لندن", "نيويورك", "جاكرتا")

    var chosen: List<String>
        get() = Store.prefs.getString("world_places", null)?.split('|')?.filter { n -> places.any { it.name == n } } ?: defaults
        set(v) { Store.prefs.edit().putString("world_places", v.joinToString("|")).apply(); Store.bump() }

    fun place(name: String) = places.firstOrNull { it.name == name }
}

@Composable
fun WorldClockPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    var adding by remember { mutableStateOf(false) }
    val here = ZoneId.systemDefault()
    val local = Instant.ofEpochMilli(now).atZone(here)
    fun clock(z: ZonedDateTime) = "%02d:%02d".format(z.hour, z.minute)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Hint("world", "🌍", "الساعة الآن في مدن العالم، والفرق عن وقتك. اضغط «+ إضافة مدينة» لاختيار غيرها، و✕ لإزالتها.")
        Panel(color = P.card, pad = 14.dp) {
            Sub("عندك الآن (${Store.place.ifBlank { here.id }})", size = 12.sp)
            Text("%02d:%02d:%02d".format(local.hour, local.minute, local.second), color = P.ink, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Sub(PrayerAlarm.dayNames.first { it.first == local.dayOfWeek.value }.second + " · " + Data.hijri().let { "${it.day} ${it.monthName} ${it.year} هـ" }, color = P.gold)
        }
        WorldClock.chosen.mapNotNull { WorldClock.place(it) }.forEach { pl ->
            val z = runCatching { Instant.ofEpochMilli(now).atZone(ZoneId.of(pl.zone)) }.getOrNull() ?: return@forEach
            val diffMin = (z.offset.totalSeconds - local.offset.totalSeconds) / 60
            val dayDiff = z.toLocalDate().toEpochDay() - local.toLocalDate().toEpochDay()
            val diff = when {
                diffMin == 0 -> "نفس وقتك"
                else -> (if (diffMin > 0) "+" else "−") + (Math.abs(diffMin) / 60).let { h -> if (Math.abs(diffMin) % 60 == 0) "$h س" else "$h:%02d س".format(Math.abs(diffMin) % 60) }
            }
            val icon = when (z.hour) { in 5..6 -> "🌅"; in 7..16 -> "☀"; in 17..18 -> "🌇"; else -> "🌙" }
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 22.sp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(pl.name, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Sub(diff + when (dayDiff) { 1L -> " · غدًا"; -1L -> " · أمس"; else -> " · اليوم" }, size = 12.sp)
                }
                Text(clock(z), color = P.accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("✕", color = P.muted, fontSize = 16.sp, modifier = Modifier.padding(start = 6.dp).clip(L.shape(8.dp)).clickable { WorldClock.chosen = WorldClock.chosen - pl.name }.padding(10.dp))
            }
        }
        Action("+ إضافة مدينة", Modifier.fillMaxWidth()) { adding = true }
    }
    if (adding) WorldAddDialog { adding = false }
}

@Composable
private fun WorldAddDialog(close: () -> Unit) {
    var q by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(onDismissRequest = close, containerColor = P.surface,
        confirmButton = { androidx.compose.material3.TextButton(onClick = close) { Text("تم", color = P.accent) } },
        title = { Text("اختر مدينة", color = P.ink) },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("ابحث…") }, modifier = Modifier.fillMaxWidth())
                Column(Modifier.padding(top = 8.dp).verticalScroll(rememberScrollState())) {
                    val chosen = WorldClock.chosen
                    WorldClock.places.filter { q.isBlank() || it.name.contains(q.trim()) }.forEach { pl ->
                        val on = pl.name in chosen
                        Row(Modifier.fillMaxWidth().clip(L.shape(10.dp)).clickable { WorldClock.chosen = if (on) chosen - pl.name else chosen + pl.name }.padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (on) "✓" else "＋", color = if (on) P.accent else P.muted, fontSize = 16.sp, modifier = Modifier.width(28.dp))
                            Text(pl.name, color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            val z = Instant.now().atZone(ZoneId.of(pl.zone))
                            Sub("%02d:%02d".format(z.hour, z.minute))
                        }
                    }
                }
            }
        })
}
