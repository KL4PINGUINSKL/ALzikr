package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

// =============================================================================== Ramadan

object RamadanInfo {
    /** The first day of the next (or current) Ramadan, by the app's Hijri calendar. */
    fun start(): LocalDate? {
        var d = LocalDate.now()
        val h = Data.hijri(d)
        if (h.month == 9) return d.minusDays((h.day - 1).toLong())
        repeat(400) { d = d.plusDays(1); val x = Data.hijri(d); if (x.month == 9 && x.day == 1) return d }
        return null
    }
    fun inRamadan() = Data.hijri().month == 9
    fun dayOf() = Data.hijri().day
    /** The last day (the 29th or 30th): the day before Shawwal 1. */
    fun end(): LocalDate? { val s = start() ?: return null; var d = s; repeat(31) { d = d.plusDays(1); if (Data.hijri(d).month != 9) return d.minusDays(1) }; return d }
}

@Composable
fun RamadanPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!RamadanInfo.inRamadan()) {
            val s = RamadanInfo.start()
            Panel(color = P.card, pad = 16.dp) {
                Sub("رمضان القادم", color = P.gold)
                val days = s?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) } ?: 0
                Text(if (s == null) "—" else "بعد $days يومًا", color = P.ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                if (s != null) Sub("يبدأ تقريبًا ${DayInfo.dayName(s)} ${s.dayOfMonth}/${s.monthValue}/${s.year} (بحسب الحساب، والعبرة برؤية الهلال في بلدك)", size = 12.sp)
            }
            Panel(pad = 14.dp) {
                Title("استعد له من الآن")
                listOf(
                    "صم من شعبان: «ما رأيته أكثر صيامًا منه في شعبان» (البخاري ومسلم)",
                    "اقضِ ما عليك من رمضان الماضي قبل أن يدخل",
                    "عوّد نفسك على ورد يومي من القرآن (من «الختمة»)",
                    "أكثر من الدعاء ببلوغه، وجدّد التوبة",
                ).forEach { Text("• $it", color = P.ink, fontSize = 15.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
            Action("دليل رمضان كاملًا", Modifier.fillMaxWidth(), filled = false) { Nav.morePage = "season:ramadan"; Nav.go("more") }
            return@Column
        }
        // ---- in Ramadan
        val day = RamadanInfo.dayOf()
        val t = Store.times()
        val m = LocalTime.now().let { it.hour * 60 + it.minute + it.second / 60f }
        val imsak = t.getValue(Prayer.FAJR) - 10; val iftar = t.getValue(Prayer.MAGHRIB)
        Panel(color = P.card, pad = 16.dp) {
            Sub("رمضان · اليوم $day", color = P.gold)
            val (label, at) = when {
                m < imsak -> "السحور ينتهي (الإمساك)" to imsak
                m < iftar -> "الإفطار" to iftar
                else -> "سحور الغد (الإمساك)" to imsak + 1440
            }
            val left = ((at - m) * 60_000L).toLong().coerceAtLeast(0)
            Text(label, color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("بعد " + durationText(left), color = P.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Sub("الإمساك ${Alerts.timeText(imsak)} · الإفطار ${Alerts.timeText(iftar)}", size = 12.sp)
            if (m >= iftar - 15 && m < iftar + 30) Text("دعاء الإفطار: «ذهب الظمأ، وابتلّت العروق، وثبت الأجر إن شاء الله» (أبو داود، وحسّنه الألباني)",
                color = P.ink, fontSize = 15.sp, lineHeight = 25.sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 8.dp))
        }
        // The month's nights: fasting and taraweeh, one square per day.
        val start = RamadanInfo.start() ?: today
        Panel(pad = 14.dp) {
            Title("صيامي وقيامي")
            val fastedToday = Data.extra(today, "fast"); val tarToday = Data.extra(today, "taraweeh")
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(if (fastedToday) "✓ صمت اليوم" else "صمت اليوم", fastedToday) { Data.setExtra(today, "fast", !fastedToday) }
                Choice(if (tarToday) "✓ صليت التراويح" else "صليت التراويح", tarToday) { Data.setExtra(today, "taraweeh", !tarToday) }
            }
            val days = (0 until 30).map { start.plusDays(it.toLong()) }.filter { Data.hijri(it).month == 9 }
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                days.chunked(10).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { d ->
                            val f = Data.extra(d, "fast"); val q = Data.extra(d, "taraweeh"); val odd = Data.hijri(d).day >= 21 && Data.hijri(d).day % 2 == 1
                            Box(Modifier.weight(1f).height(30.dp).clip(L.shape(6.dp)).background(if (f && q) P.gold else if (f || q) P.accent.copy(alpha = .55f) else P.line.copy(alpha = .5f))
                                .border(if (d == today) 2.dp else if (odd) 1.dp else 0.dp, if (d == today) P.ink else P.gold, L.shape(6.dp)), contentAlignment = Alignment.Center) {
                                Text("${Data.hijri(d).day}", color = if (f && q) P.onAccent else P.ink, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            Sub("ذهبي: صيام وقيام · بإطار: ليالي الوتر من العشر", Modifier.padding(top = 6.dp), size = 11.sp)
        }
        // A khatma in Ramadan.
        Panel(pad = 14.dp) {
            Title("ختمة رمضان")
            val left = RamadanInfo.end()?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt() + 1 } ?: 1
            Sub("بقي من الشهر $left يومًا. اجعل ختمتك تنتهي معه:", Modifier.padding(top = 4.dp))
            Action("أختم قبل نهاية رمضان (${kotlin.math.ceil((Data.PAGES - Data.khatmaPage).toDouble() / left).toInt()} صفحة يوميًا)", Modifier.fillMaxWidth().padding(top = 8.dp)) { Data.finishIn(left); Nav.daily = 3; Nav.go("daily") }
        }
        // Zakat al-fitr.
        var people by remember { mutableIntStateOf(1) }
        Panel(pad = 14.dp) {
            Title("زكاة الفطر")
            Text("«فرض رسول الله ﷺ زكاة الفطر صاعًا من تمر أو صاعًا من شعير… وأمر بها أن تؤدّى قبل خروج الناس إلى الصلاة» (البخاري ومسلم)", color = P.ink, fontSize = 14.sp, lineHeight = 23.sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("عدد من تخرجها عنهم:", color = P.ink, fontSize = 15.sp)
                Choice("−", false) { people = (people - 1).coerceAtLeast(1) }
                Text("$people", color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                Choice("+", false) { people += 1 }
            }
            Text("${people} صاع ≈ ${"%.1f".format(people * 2.5)} كغ من غالب قوت البلد (أرز أو تمر…)", color = P.accent, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Sub("الصاع نحو ٢٫٥ إلى ٣ كغ بحسب نوع الطعام. وقتها: قبل صلاة العيد، ويجوز قبله بيوم أو يومين.", size = 11.sp)
        }
        Action("دليل العشر الأواخر وليلة القدر", Modifier.fillMaxWidth(), filled = false) { Nav.morePage = "season:lastten"; Nav.go("more") }
    }
}

// =============================================================================== «محاسبة اليوم»

@Composable
fun TodayPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Hint("today", "📝", "قبل النوم: انظر في يومك، احمد الله على ما أحسنت، واستغفره مما قصّرت. «حاسبوا أنفسكم قبل أن تحاسبوا» (من قول عمر رضي الله عنه).")
        val h = Data.hijri()
        Sub("${DayInfo.dayName(today)} ${h.day} ${h.monthName} ${h.year} هـ", color = P.gold)
        val items = mutableListOf<Triple<String, String, Boolean>>()
        val five = Prayer.entries.filter { it.alertable }
        val prayed = five.count { Data.prayed(today, it) }
        items += Triple("🕌", "الصلوات: $prayed من 5" + if (prayed < 5) " (لم تُعلَّم: ${five.filter { !Data.prayed(today, it) }.joinToString("، ") { it.ar }})" else "", prayed == 5)
        items += Triple("🤲", "السنن الرواتب: ${Stats.rawatibOn(today)} من 4", Stats.rawatibOn(today) == 4)
        items += Triple("🌙", "الوتر", Data.extra(today, "witr"))
        items += Triple("🌅", "أذكار الصباح", Stats.colDoneOn("morning", today))
        items += Triple("🌇", "أذكار المساء", Stats.colDoneOn("evening", today))
        items += Triple("📖", "وردك من القرآن: ${Data.pagesToday()} صفحة" + if (Data.wirdToday() > 0) " من ${Data.wirdToday()}" else "", Data.wirdToday() > 0 && Data.pagesToday() >= Data.wirdToday())
        items += Triple("📖", "سورة الملك", Data.extra(today, "mulk"))
        items += Triple("📿", "التسبيح: ${Data.tasbihHistory(1).firstOrNull()?.second ?: 0}", (Data.tasbihHistory(1).firstOrNull()?.second ?: 0) >= 33)
        items += Triple("💰", "صدقة", Sadaqa.countBetween(today, today) > 0)
        if (Data.extra(today, "fast")) items += Triple("🍽", "صيام", true)
        items.forEach { (icon, label, ok) ->
            Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(if (ok) P.accent.copy(alpha = .10f) else P.surface).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(icon, tint = if (ok) P.accent else P.muted, size = 20.dp)
                Text(label, color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                Text(if (ok) "✓" else "—", color = if (ok) P.gold else P.muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        val key = "note_$today"
        var note by remember { mutableStateOf(Store.prefs.getString(key, "") ?: "") }
        Title("ما أحسنت فيه، وما تستغفر منه")
        OutlinedTextField(note, { note = it; Store.prefs.edit().putString(key, it).apply() }, minLines = 3, modifier = Modifier.fillMaxWidth(), placeholder = { Text("اكتب لنفسك… (يبقى على هاتفك فقط)") })
        Panel(color = P.card, pad = 12.dp) {
            Text("«اللهم أعني على ذكرك وشكرك وحسن عبادتك»", color = P.ink, fontSize = 16.sp, fontFamily = Fonts.naskh)
            Sub("أبو داود والنسائي، وصححه الألباني", size = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("📖", "سورة الملك", Modifier.weight(1f)) { Data.setExtra(today, "mulk", true); Nav.reading = 67 to 1 }
            Tile("🌙", "أذكار النوم", Modifier.weight(1f)) { Nav.collection = "sleep"; Nav.go("adhkar") }
        }
    }
}
