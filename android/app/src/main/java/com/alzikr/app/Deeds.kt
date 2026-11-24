package com.alzikr.app

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

// =============================================================================== «صدقاتي»

object Sadaqa {
    data class Entry(val id: Long, val date: String, val amount: String, val note: String)
    private val sp get() = Store.prefs
    fun all(): List<Entry> = runCatching {
        JSONArray(sp.getString("sadaqat", "[]")).let { a -> (0 until a.length()).map { a.getJSONObject(it).let { o -> Entry(o.getLong("id"), o.getString("d"), o.optString("a"), o.optString("n")) } } }
    }.getOrDefault(emptyList()).sortedByDescending { it.date + it.id }
    private fun save(l: List<Entry>) { val a = JSONArray(); l.forEach { a.put(JSONObject().put("id", it.id).put("d", it.date).put("a", it.amount).put("n", it.note)) }; sp.edit().putString("sadaqat", a.toString()).apply(); Store.bump() }
    fun add(amount: String, note: String, date: LocalDate = LocalDate.now()) { save(all() + Entry(System.currentTimeMillis(), date.toString(), amount.trim(), note.trim())); Pet.cheer("صدقة مباركة، تقبّل الله منك") }
    fun delete(id: Long) = save(all().filterNot { it.id == id })
    fun countBetween(from: LocalDate, to: LocalDate) = all().count { val d = LocalDate.parse(it.date); !d.isBefore(from) && !d.isAfter(to) }
    fun thisMonth(): Int { val n = LocalDate.now(); return countBetween(n.withDayOfMonth(1), n) }
    /** Weeks in a row (Saturday to Friday) with at least one sadaqa. */
    fun weekStreak(): Int {
        var start = Goals.weekStart(); var n = 0
        if (countBetween(start, start.plusDays(6)) == 0) start = start.minusDays(7)
        while (n < 520 && countBetween(start, start.plusDays(6)) > 0) { n++; start = start.minusDays(7) }
        return n
    }
    /** 0 off, 1 every day, 2 every Friday. */
    var remind: Int
        get() = sp.getInt("sadaqa_remind", 0)
        set(v) { sp.edit().putInt("sadaqa_remind", v).apply(); Store.bump() }
    var remindAt: Int
        get() = sp.getInt("sadaqa_at", 17 * 60)
        set(v) { sp.edit().putInt("sadaqa_at", v).apply(); Store.bump() }
    fun remindsOn(d: LocalDate) = remind == 1 || (remind == 2 && d.dayOfWeek == DayOfWeek.FRIDAY)
    val virtues = listOf(
        "«ما نقصت صدقة من مال» (مسلم)",
        "«اتقوا النار ولو بشق تمرة» (البخاري ومسلم)",
        "«الصدقة تطفئ الخطيئة كما يطفئ الماء النار» (الترمذي، وصححه الألباني)",
        "«ما من يوم يصبح العباد فيه إلا ملكان ينزلان، فيقول أحدهما: اللهم أعط منفقًا خلفًا» (البخاري ومسلم)",
        "«كل معروف صدقة» (البخاري)",
        "«أحب الأعمال إلى الله أدومها وإن قلّ» (البخاري ومسلم)",
    )
}

@Composable
fun SadaqaPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint("sadaqa", "💰", "سجّل كل صدقة ولو قليلة: مالًا أو طعامًا أو معروفًا. المبلغ اختياري، ويبقى السجل على هاتفك فقط. «كل معروف صدقة».")
        Panel(color = P.card, pad = 14.dp) {
            Text(Sadaqa.virtues[(LocalDate.now().dayOfYear) % Sadaqa.virtues.size], color = P.ink, fontSize = (16 * Data.textScale).sp, lineHeight = (27 * Data.textScale).sp, fontFamily = Fonts.naskh)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Panel(Modifier.weight(1f), color = P.surface, pad = 12.dp) { Sub("هذا الشهر", size = 12.sp); Text("${Sadaqa.thisMonth()}", color = P.accent, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            Panel(Modifier.weight(1f), color = P.surface, pad = 12.dp) { Sub("أسابيع متتالية", size = 12.sp); Text("${Sadaqa.weekStreak()}", color = P.gold, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        }
        Action("+ تصدّقت اليوم", Modifier.fillMaxWidth()) { adding = true }
        Panel(pad = 14.dp) {
            Title("التذكير بالصدقة")
            Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "متوقف", 1 to "كل يوم", 2 to "كل جمعة").forEach { (k, l) -> Choice(l, Sadaqa.remind == k) { Sadaqa.remind = k; Alerts.reschedule(ctx) } }
            }
            if (Sadaqa.remind > 0) Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sub("وقته:")
                Choice("⏰ " + Alerts.timeText(Sadaqa.remindAt), true) {
                    android.app.TimePickerDialog(ctx, { _, h, m -> Sadaqa.remindAt = h * 60 + m; Alerts.reschedule(ctx) }, Sadaqa.remindAt / 60, Sadaqa.remindAt % 60, android.text.format.DateFormat.is24HourFormat(ctx)).show()
                }
            }
        }
        val list = Sadaqa.all()
        if (list.isNotEmpty()) Title("سجلّك")
        list.take(60).forEach { e ->
            Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.surface).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(listOf(e.amount, e.note).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "صدقة" }, color = P.ink, fontSize = 15.sp)
                    val d = LocalDate.parse(e.date); val h = Data.hijri(d)
                    Sub("${DayInfo.dayName(d)} ${h.day} ${h.monthName} · ${d.dayOfMonth}/${d.monthValue}", size = 12.sp)
                }
                Text("✕", color = P.muted, fontSize = 15.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Sadaqa.delete(e.id) }.padding(10.dp))
            }
        }
    }
    if (adding) {
        var amount by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { adding = false }, containerColor = P.surface, title = { Text("صدقة اليوم", color = P.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(amount, { amount = it.take(30) }, singleLine = true, placeholder = { Text("المبلغ (اختياري)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(note, { note = it.take(80) }, singleLine = true, placeholder = { Text("ملاحظة (اختياري): طعام، مسجد، معروف…") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton({ Sadaqa.add(amount, note); adding = false }) { Text("حفظ") } },
            dismissButton = { TextButton({ adding = false }) { Text("إلغاء") } })
    }
}

// =============================================================================== «أهدافي الأسبوعية»

object Goals {
    /** A weekly goal measured from what the app already records. */
    data class Goal(val id: String, val icon: String, val name: String, val target: Int, val count: (LocalDate, LocalDate) -> Int)
    private fun days(from: LocalDate, to: LocalDate) = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
    val all = listOf(
        Goal("five", "🕌", "الصلوات الخمس كل يوم", 35) { a, b -> days(a, b).sumOf { Stats.prayedOn(it) } },
        Goal("witr", "🌙", "الوتر كل ليلة", 7) { a, b -> days(a, b).count { Data.extra(it, "witr") } },
        Goal("rawatib", "🤲", "السنن الرواتب (٤ في اليوم)", 28) { a, b -> days(a, b).sumOf { Stats.rawatibOn(it) } },
        Goal("duha", "☀", "صلاة الضحى ٣ مرات", 3) { a, b -> days(a, b).count { Data.extra(it, "duha") } },
        Goal("qiyam", "✨", "قيام الليل مرتين", 2) { a, b -> days(a, b).count { Data.extra(it, "qiyam") } },
        Goal("fast", "🍽", "صيام يومين", 2) { a, b -> days(a, b).count { Data.extra(it, "fast") } },
        Goal("morning", "🌅", "أذكار الصباح كل يوم", 7) { a, b -> days(a, b).count { Stats.colDoneOn("morning", it) } },
        Goal("evening", "🌇", "أذكار المساء كل يوم", 7) { a, b -> days(a, b).count { Stats.colDoneOn("evening", it) } },
        Goal("quran", "📖", "قراءة القرآن كل يوم", 7) { a, b -> days(a, b).count { d -> runCatching { JSONObject(Store.prefs.getString("khatma_log", "{}")!!).optInt(d.toString()) > 0 }.getOrDefault(false) } },
        Goal("juz", "📗", "جزء في الأسبوع (٢٠ صفحة)", 20) { a, b -> runCatching { val j = JSONObject(Store.prefs.getString("khatma_log", "{}")!!); days(a, b).sumOf { j.optInt(it.toString()) } }.getOrDefault(0) },
        Goal("mulk", "📖", "سورة الملك كل ليلة", 7) { a, b -> days(a, b).count { Data.extra(it, "mulk") } },
        Goal("baqarah", "📗", "خواتيم البقرة كل ليلة", 7) { a, b -> days(a, b).count { Data.extra(it, "baqarah") } },
        Goal("sadaqa", "💰", "صدقة مرة على الأقل", 1) { a, b -> Sadaqa.countBetween(a, b) },
        Goal("kahf", "🕌", "سورة الكهف يوم الجمعة", 1) { a, b -> if (Data.extra(b, "kahf") || Data.extra(b.minusDays(1), "kahf")) 1 else 0 },
    )
    /** The week runs Saturday to Friday. */
    fun weekStart(d: LocalDate = LocalDate.now()): LocalDate { var s = d; while (s.dayOfWeek != DayOfWeek.SATURDAY) s = s.minusDays(1); return s }
    var chosen: Set<String>
        get() = Store.prefs.getStringSet("goals", setOf("five", "witr", "morning", "evening", "quran"))!!.toSet()
        set(v) { Store.prefs.edit().putStringSet("goals", v).apply(); Store.bump() }
    fun progress(g: Goal, weeksAgo: Int = 0): Int { val s = weekStart().minusDays(7L * weeksAgo); return g.count(s, minOf(s.plusDays(6), LocalDate.now())) }
}

@Composable
fun GoalsPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var editing by remember { mutableStateOf(false) }
    val start = Goals.weekStart(); val end = start.plusDays(6)
    val mine = Goals.all.filter { it.id in Goals.chosen }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Hint("goals", "🎯", "اختر أهدافك للأسبوع (من السبت إلى الجمعة). يُحسب تقدّمك وحده مما تسجّله: ✓ الصلوات والسنن والصيام في صفحة كل صلاة، والأذكار، والورد، وصدقاتي.")
        val done = mine.count { Goals.progress(it) >= it.target }
        Panel(color = P.card, pad = 14.dp) {
            Sub("هذا الأسبوع: من ${start.dayOfMonth}/${start.monthValue} إلى ${end.dayOfMonth}/${end.monthValue}", size = 12.sp)
            Text(if (mine.isEmpty()) "لم تختر أهدافًا بعد" else "أتممت $done من ${mine.size}", color = if (done == mine.size && mine.isNotEmpty()) P.gold else P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        mine.forEach { g ->
            val n = Goals.progress(g); val last = Goals.progress(g, 1)
            val frac = (n.toFloat() / g.target).coerceIn(0f, 1f)
            Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, if (n >= g.target) P.gold.copy(alpha = .6f) else P.line, L.shape(14.dp)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Glyph(g.icon, tint = if (n >= g.target) P.gold else P.accent, size = 22.dp)
                    Text(g.name, color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Text(if (n >= g.target) "✓ $n/${g.target}" else "$n/${g.target}", color = if (n >= g.target) P.gold else P.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.line)) {
                    Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (n >= g.target) P.gold else P.accent))
                }
                Sub("الأسبوع الماضي: $last/${g.target}", Modifier.padding(top = 4.dp), size = 11.sp)
            }
        }
        Action(if (editing) "تم" else "اختر أهدافك", Modifier.fillMaxWidth(), filled = editing) { editing = !editing }
        if (editing) Goals.all.forEach { g ->
            val on = g.id in Goals.chosen
            Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(if (on) P.accent.copy(alpha = .1f) else P.surface).clickable { Goals.chosen = if (on) Goals.chosen - g.id else Goals.chosen + g.id }
                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(g.icon, tint = P.accent, size = 20.dp)
                Text(g.name, color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Switch(on, { Goals.chosen = if (it) Goals.chosen + g.id else Goals.chosen - g.id })
            }
        }
    }
}

// =============================================================================== «صفة الوضوء والصلاة»

object SalahGuide {
    data class Step(val title: String, val text: String, val zikr: String = "", val src: String = "")
    data class Part(val id: String, val icon: String, val name: String, val intro: String, val steps: List<Step>)
    val parts = listOf(
        Part("wudu", "💧", "صفة الوضوء", "كما وصفه عثمان بن عفان رضي الله عنه عن النبي ﷺ (البخاري ومسلم).", listOf(
            Step("النية", "ينوي بقلبه رفع الحدث، ولا يتلفظ بها.", "«إنما الأعمال بالنيات»", "البخاري ومسلم"),
            Step("التسمية", "يقول: بسم الله.", "«لا وضوء لمن لم يذكر اسم الله عليه»", "أبو داود وابن ماجه، وحسّنه جماعة"),
            Step("غسل الكفين", "يغسل كفيه ثلاثًا."),
            Step("المضمضة والاستنشاق", "يتمضمض ويستنشق ويستنثر ثلاثًا، من غرفة واحدة أو أكثر."),
            Step("غسل الوجه", "يغسل وجهه ثلاثًا: من منابت شعر الرأس إلى الذقن، ومن الأذن إلى الأذن."),
            Step("غسل اليدين", "يغسل يده اليمنى إلى المرفق ثلاثًا، ثم اليسرى مثل ذلك."),
            Step("مسح الرأس والأذنين", "يمسح رأسه مرة واحدة: يبدأ بمقدّمه إلى قفاه ثم يردّهما، ثم يمسح أذنيه.", "", "البخاري ومسلم"),
            Step("غسل الرجلين", "يغسل رجله اليمنى إلى الكعبين ثلاثًا، ثم اليسرى مثل ذلك، ويخلّل الأصابع."),
            Step("الذكر بعده", "يقول بعد الفراغ:", "«أشهد أن لا إله إلا الله وحده لا شريك له، وأشهد أن محمدًا عبده ورسوله» — فُتحت له أبواب الجنة الثمانية يدخل من أيها شاء", "مسلم"),
        )),
        Part("salah", "🕌", "صفة الصلاة", "«صلّوا كما رأيتموني أصلي» (البخاري). خطوات الركعة من التكبير إلى التسليم.", listOf(
            Step("القيام واستقبال القبلة", "يقف مستقبلًا القبلة، وينوي بقلبه الصلاة التي يصليها."),
            Step("تكبيرة الإحرام", "يقول: الله أكبر، رافعًا يديه حذو منكبيه أو أذنيه.", "", "البخاري ومسلم"),
            Step("وضع اليدين", "يضع يده اليمنى على اليسرى على صدره.", "«كان الناس يؤمرون أن يضع الرجل اليد اليمنى على ذراعه اليسرى في الصلاة»", "البخاري"),
            Step("دعاء الاستفتاح", "يقول سرًّا:", "«سبحانك اللهم وبحمدك، وتبارك اسمك، وتعالى جدّك، ولا إله غيرك»", "أبو داود والترمذي، وصححه الألباني"),
            Step("الاستعاذة والبسملة والفاتحة", "يستعيذ ويبسمل، ثم يقرأ الفاتحة في كل ركعة، ثم سورة في الركعتين الأوليين.", "«لا صلاة لمن لم يقرأ بفاتحة الكتاب»", "البخاري ومسلم"),
            Step("الركوع", "يكبّر رافعًا يديه، ويركع ظهره مستوٍ ويداه على ركبتيه.", "«سبحان ربي العظيم» (ثلاثًا)", "مسلم"),
            Step("الرفع من الركوع", "يرفع رافعًا يديه قائلًا: سمع الله لمن حمده، فإذا استوى قائمًا قال:", "«ربنا ولك الحمد، حمدًا كثيرًا طيبًا مباركًا فيه»", "البخاري"),
            Step("السجود", "يكبّر ويسجد على سبعة أعظم: الجبهة مع الأنف، واليدين، والركبتين، وأطراف القدمين.", "«سبحان ربي الأعلى» (ثلاثًا)، ويكثر من الدعاء", "البخاري ومسلم؛ والذكر رواه مسلم"),
            Step("الجلوس بين السجدتين", "يكبّر ويجلس مفترشًا رجله اليسرى ناصبًا اليمنى.", "«رب اغفر لي، رب اغفر لي»", "أبو داود وابن ماجه، وصححه الألباني"),
            Step("السجدة الثانية", "يكبّر ويسجد كالأولى، ثم يقوم للركعة الثانية مكبّرًا، ويصليها كالأولى."),
            Step("التشهد الأول", "بعد الركعة الثانية في الثلاثية والرباعية يجلس ويقرأ:", "«التحيات لله والصلوات والطيبات، السلام عليك أيها النبي ورحمة الله وبركاته، السلام علينا وعلى عباد الله الصالحين، أشهد أن لا إله إلا الله، وأشهد أن محمدًا عبده ورسوله»", "البخاري ومسلم"),
            Step("التشهد الأخير", "في آخر ركعة يجلس متورّكًا، ويقرأ التشهد ثم الصلاة الإبراهيمية:", "«اللهم صلّ على محمد وعلى آل محمد كما صليت على إبراهيم وعلى آل إبراهيم إنك حميد مجيد، اللهم بارك على محمد وعلى آل محمد كما باركت على إبراهيم وعلى آل إبراهيم إنك حميد مجيد»", "البخاري"),
            Step("الاستعاذة من أربع", "ثم يدعو:", "«اللهم إني أعوذ بك من عذاب جهنم، ومن عذاب القبر، ومن فتنة المحيا والممات، ومن شر فتنة المسيح الدجال»", "مسلم"),
            Step("التسليم", "يلتفت عن يمينه ثم عن يساره قائلًا:", "«السلام عليكم ورحمة الله»", "أبو داود والترمذي، وصححه الألباني"),
        )),
        Part("sahw", "🔁", "سجود السهو", "سجدتان يسجدهما المصلي إذا زاد في صلاته أو نقص أو شكّ.", listOf(
            Step("إذا شكّ", "يبني على الأقل (ما تيقّنه)، ثم يسجد سجدتين قبل أن يسلّم.", "«إذا شكّ أحدكم في صلاته فلم يدر كم صلى… فليطرح الشك وليبن على ما استيقن، ثم يسجد سجدتين قبل أن يسلّم»", "مسلم"),
            Step("إذا زاد أو سلّم قبل تمامها", "يكمل ما بقي، ثم يسجد سجدتين بعد السلام، ثم يسلّم.", "كما فعل النبي ﷺ في حديث ذي اليدين", "البخاري ومسلم"),
            Step("إذا ترك التشهد الأول", "يمضي في صلاته ولا يرجع إن استتمّ قائمًا، ويسجد للسهو قبل السلام.", "", "البخاري ومسلم"),
        )),
        Part("travel", "🧳", "صلاة المسافر", "تخفيف من الله لعباده.", listOf(
            Step("القصر", "تُصلّى الرباعية (الظهر والعصر والعشاء) ركعتين، والمغرب والفجر كما هما.", "«فُرضت الصلاة ركعتين، فأُقرّت صلاة السفر وأُتمّت صلاة الحضر»", "البخاري ومسلم"),
            Step("الجمع", "يجوز جمع الظهر مع العصر، والمغرب مع العشاء، تقديمًا أو تأخيرًا عند الحاجة.", "", "مسلم"),
            Step("السنن", "كان النبي ﷺ لا يدع في السفر ركعتي الفجر والوتر.", "", "ذكره ابن القيم في زاد المعاد، وأصله في البخاري ومسلم"),
        )),
        Part("sick", "🛏", "صلاة المريض", "لا يسقط الفرض، ويصلي على قدر استطاعته.", listOf(
            Step("على قدر الاستطاعة", "يصلي قائمًا، فإن لم يستطع فقاعدًا، فإن لم يستطع فعلى جنبه.", "«صلّ قائمًا، فإن لم تستطع فقاعدًا، فإن لم تستطع فعلى جنب»", "البخاري"),
            Step("الإيماء", "إن عجز عن الركوع والسجود أومأ برأسه، ويجعل السجود أخفض من الركوع."),
            Step("الطهارة", "إن عجز عن الماء أو ضرّه استعماله تيمّم."),
        )),
    )
}

@Composable
fun SalahGuidePage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var part by remember { mutableIntStateOf(0) }
    val p = SalahGuide.parts[part]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SalahGuide.parts.forEachIndexed { i, x -> Choice(x.name, part == i) { part = i } }
        }
        Sub(p.intro, size = 13.sp)
        p.steps.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).padding(12.dp)) {
                Box(Modifier.padding(end = 10.dp).clip(RoundedCornerShape(50)).background(P.accent.copy(alpha = .15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${i + 1}", color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(s.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(s.text, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (24 * Data.textScale).sp, modifier = Modifier.padding(top = 2.dp))
                    if (s.zikr.isNotBlank()) Text(s.zikr, color = P.ink, fontSize = (16 * Data.textScale).sp, lineHeight = (28 * Data.textScale).sp, fontFamily = Fonts.naskh,
                        modifier = Modifier.padding(top = 6.dp).fillMaxWidth().clip(L.shape(10.dp)).background(P.accent.copy(alpha = .08f)).padding(10.dp))
                    if (s.src.isNotBlank()) Sub(s.src, Modifier.padding(top = 4.dp), size = 11.sp)
                }
            }
        }
    }
}
