package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** What suits this moment: the adhkar of the time of day, Friday's Kahf, the fasting days, the seasons. Sunni sources only. */
object Moments {
    data class Moment(val icon: String, val title: String, val text: String, val src: String, val button: String,
                      val coll: String? = null, val go: () -> Unit = { Nav.collection = coll; Nav.go("adhkar") }) {
        /** The day it belongs to (the evening counts with the next day) and its title. */
        var key: String = ""
    }

    /** The Islamic day: after maghrib it is already the next day. */
    private fun day(): LocalDate {
        val min = LocalTime.now().let { it.hour * 60 + it.minute }
        val maghrib = if (Store.hasLocation) Store.times().getValue(Prayer.MAGHRIB) else 1080
        return if (min >= maghrib) LocalDate.now().plusDays(1) else LocalDate.now()
    }

    /** Marked done by the user, or its adhkar collection finished today. */
    fun done(m: Moment): Boolean {
        if (m.key in Store.prefs.getStringSet("moment_done", emptySet())!!) return true
        val c = m.coll?.let { Content.collection(it) } ?: return false
        val counts = Data.colCounts(c.id)
        return c.items.indices.all { counts[it] >= c.items[it].count }
    }
    /** Undo every «✓ أتممته» of today. */
    fun undoDone() { Store.prefs.edit().remove("moment_done").apply(); Store.bump() }
    fun anyMarked() = Store.prefs.getStringSet("moment_done", emptySet())!!.any { it.startsWith(day().toString()) }
    fun markDone(m: Moment) {
        val today = day().toString()
        val keep = Store.prefs.getStringSet("moment_done", emptySet())!!.filter { it.startsWith(today) || it.startsWith(LocalDate.now().toString()) }.toSet()
        Store.prefs.edit().putStringSet("moment_done", keep + m.key).apply(); Store.bump()
    }
    private fun guide(id: String): () -> Unit = { Nav.morePage = "season:$id"; Nav.go("more") }

    /** The moments for now, the most fitting first. */
    fun now(): List<Moment> {
        val out = mutableListOf<Moment>()
        val date = LocalDate.now()
        val min = LocalTime.now().let { it.hour * 60 + it.minute }
        val t = if (Store.hasLocation) Store.times() else null
        fun at(p: Prayer) = t?.getValue(p) ?: when (p) { Prayer.FAJR -> 300; Prayer.SUNRISE -> 400; Prayer.DHUHR -> 750; Prayer.ASR -> 930; Prayer.MAGHRIB -> 1080; Prayer.ISHA -> 1170 }
        val fajr = at(Prayer.FAJR); val sunrise = at(Prayer.SUNRISE); val dhuhr = at(Prayer.DHUHR)
        val asr = at(Prayer.ASR); val maghrib = at(Prayer.MAGHRIB); val isha = at(Prayer.ISHA)
        val fajrNext = if (t != null) Store.times(date.plusDays(1)).getValue(Prayer.FAJR) + 1440 else fajr + 1440
        val lastThird = (maghrib + (fajrNext - maghrib) * 2 / 3) % 1440
        val dow = date.dayOfWeek
        // Friday runs from Thursday's maghrib to Friday's maghrib.
        val friday = (dow == DayOfWeek.FRIDAY && min < maghrib) || (dow == DayOfWeek.THURSDAY && min >= maghrib)
        val h = Data.hijri(if (min >= maghrib) date.plusDays(1) else date)

        // The season, if any (the big days go first, the rest after the time of day).
        val season: Moment? = when {
            h.month == 9 && h.day >= 21 -> Moment("🌙", "العشر الأواخر من رمضان", "«كان رسول الله ﷺ إذا دخل العشر أحيا الليل، وأيقظ أهله، وجدّ وشدّ المئزر». وتحرَّ ليلة القدر في الوتر منها، وأكثر من: «اللهم إنك عفو تحب العفو فاعفُ عني».", "البخاري ومسلم؛ والدعاء رواه الترمذي وصححه", "دليل العشر الأواخر", go = guide("lastten"))
            h.month == 9 -> Moment("🌙", "رمضان", "«من صام رمضان إيمانًا واحتسابًا غُفر له ما تقدم من ذنبه».", "البخاري ومسلم", "دليل رمضان", go = guide("ramadan"))
            h.month == 10 && h.day == 1 -> Moment("🎉", "عيد الفطر", "كبّر الله: «الله أكبر، الله أكبر، لا إله إلا الله، والله أكبر، الله أكبر، ولله الحمد»، وأدِّ زكاة الفطر قبل الصلاة.", "التكبير ثابت عن ابن مسعود رضي الله عنه (ابن أبي شيبة)", "دليل العيد", go = guide("fitr"))
            h.month == 12 && h.day == 9 -> Moment("🤲", "يوم عرفة", "«خير الدعاء دعاء يوم عرفة، وخير ما قلت أنا والنبيون من قبلي: لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير». وصيامه يكفّر سنتين.", "الترمذي وحسّنه الألباني؛ والصيام رواه مسلم", "دليل يوم عرفة", go = guide("arafah"))
            h.month == 12 && h.day in 10..13 -> Moment("🐑", if (h.day == 10) "عيد الأضحى" else "أيام التشريق", "«أيام التشريق أيام أكل وشرب وذكر لله». أكثر من التكبير.", "مسلم", "دليل الأضحى", go = guide("adha"))
            h.month == 12 && h.day <= 8 -> Moment("🕋", "عشر ذي الحجة", "«ما من أيام العمل الصالح فيهن أحب إلى الله من هذه الأيام». أكثر من التكبير والتهليل والتحميد والصيام.", "البخاري", "دليل العشر", go = guide("dhul"))
            h.month == 1 && h.day in 9..10 -> Moment("📿", "عاشوراء", "«صيام يوم عاشوراء أحتسب على الله أن يكفّر السنة التي قبله»، وصم يومًا قبله.", "مسلم", "دليل محرم", go = guide("muharram"))
            h.month != 9 && h.day in 13..15 -> Moment("🌕", "الأيام البيض", "«صوم ثلاثة أيام من كل شهر صوم الدهر كله». هي ١٣ و١٤ و١٥ من الشهر الهجري.", "البخاري ومسلم", "عن الأيام البيض", go = guide("white"))
            else -> null
        }
        val bigDay = season != null && (season.title.startsWith("عيد") || season.title == "يوم عرفة" || season.title == "عاشوراء")

        // Right after the adhan, and after the prayer.
        val prayers = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
        val justCalled = prayers.firstOrNull { min - at(it) in 0 until 12 }
        val justPrayed = prayers.firstOrNull { min - at(it) in 12 until 45 }
        if (justCalled != null) out += (
            Moment("📣", "بين الأذان والإقامة", "«الدعاء لا يُرد بين الأذان والإقامة». وبعد الأذان قل: «اللهم رب هذه الدعوة التامة…».", "أبو داود والترمذي، وصححه الألباني", "أذكار الأذان", "mosque"))
        if (justPrayed != null) out += (
            Moment("🤲", "أذكار بعد صلاة ${justPrayed.ar}", "«من سبّح الله في دبر كل صلاة ثلاثًا وثلاثين، وحمد الله ثلاثًا وثلاثين، وكبّر الله ثلاثًا وثلاثين… غُفرت خطاياه».", "مسلم", "أذكار بعد الصلاة", "prayer"))
        if (bigDay) out += season!!
        // Friday.
        if (friday) {
            out += Moment("🕌", "سورة الكهف", "«من قرأ سورة الكهف في يوم الجمعة أضاء له من النور ما بين الجمعتين». وقتها من مغرب الخميس إلى مغرب الجمعة.", "الحاكم والبيهقي، وصححه الألباني", "اقرأ سورة الكهف") { Nav.reading = 18 to 1 }
            if (dow == DayOfWeek.FRIDAY && min in asr until maghrib)
                out += Moment("🤲", "ساعة الإجابة يوم الجمعة", "«التمسوها آخر ساعة بعد العصر». أكثر من الدعاء قبل المغرب.", "أبو داود والنسائي، وصححه الألباني", "أدعية من السنة", "sunnah_duas")
            out += Moment("ﷺ", "الصلاة على النبي ﷺ", "«إن من أفضل أيامكم يوم الجمعة… فأكثروا عليّ من الصلاة فيه، فإن صلاتكم معروضة عليّ».", "أبو داود، وصححه الألباني", "السبحة") { Nav.adhkar = 2; Nav.go("adhkar") }
        }

        val inLastThird = if (lastThird < fajr) min in lastThird until fajr else (min >= lastThird || min < fajr)
        val daytime = when {
            min in fajr until dhuhr - 30 -> Moment("🌅", "أذكار الصباح", "﴿وَسَبِّحْ بِحَمْدِ رَبِّكَ قَبْلَ طُلُوعِ الشَّمْسِ وَقَبْلَ غُرُوبِهَا﴾. وقتها من الفجر إلى الضحى.", "طه: ١٣٠", "ابدأ أذكار الصباح", "morning")
            min in asr until isha -> Moment("🌇", "أذكار المساء", "﴿فَسُبْحَانَ اللَّهِ حِينَ تُمْسُونَ وَحِينَ تُصْبِحُونَ﴾. وقتها من العصر إلى الليل.", "الروم: ١٧", "ابدأ أذكار المساء", "evening")
            inLastThird -> Moment("✨", "الثلث الأخير من الليل", "«ينزل ربنا تبارك وتعالى كل ليلة إلى السماء الدنيا حين يبقى ثلث الليل الآخر، يقول: من يدعوني فأستجيب له، من يسألني فأعطيه، من يستغفرني فأغفر له».", "البخاري ومسلم", "أذكار الاستيقاظ", "wake")
            min >= isha + 45 || min < fajr -> Moment("🌙", "أذكار النوم", "«إذا أتيت مضجعك فتوضأ وضوءك للصلاة، ثم اضطجع على شقك الأيمن…».", "البخاري ومسلم", "أذكار النوم", "sleep")
            else -> Moment("📿", "ذكر الله", "«ألا أنبئكم بخير أعمالكم… ذكر الله». قل: سبحان الله وبحمده، سبحان الله العظيم.", "الترمذي وابن ماجه، وصححه الألباني؛ والتسبيح في البخاري ومسلم", "السبحة") { Nav.adhkar = 2; Nav.go("adhkar") }
        }
        out += daytime
        if (!bigDay && season != null) out += season
        if (min in sunrise + 15 until dhuhr - 10 && daytime.title == "أذكار الصباح")
            out += Moment("☀", "صلاة الضحى", "«ويُجزئ من ذلك ركعتان يركعهما من الضحى».", "مسلم", "السبحة") { Nav.adhkar = 2; Nav.go("adhkar") }

        // Monday and Thursday fasts: on the eve, and on the day.
        val tomorrow = date.plusDays(1).dayOfWeek
        if (h.month != 9 && min >= maghrib && (tomorrow == DayOfWeek.MONDAY || tomorrow == DayOfWeek.THURSDAY))
            out += Moment("🍽", "غدًا ${if (tomorrow == DayOfWeek.MONDAY) "الاثنين" else "الخميس"}: صيام سنة", "«تُعرض الأعمال يوم الاثنين والخميس، فأحب أن يُعرض عملي وأنا صائم». وتسحّر فإن في السحور بركة.", "الترمذي، وصححه الألباني؛ والسحور في البخاري ومسلم", "فضل الغد وسننه") { Nav.daySheet = date.plusDays(1) }
        val d = day().toString()
        return out.distinctBy { it.title }.onEach { it.key = d + "|" + it.title }
    }
}

/** The home page: which parts show, in what order, which shortcuts, and the tab's name. */
object Home {
    data class Part(val id: String, val label: String)
    val parts = listOf(
        Part("tiles", "الاختصارات"),
        Part("next", "الصلاة القادمة والعدّ التنازلي"),
        Part("forbid", "وقت النهي عن الصلاة (يظهر عند دخوله فقط)"),
        Part("times", "مواقيت اليوم"),
        Part("moment", "ذكر الوقت (الصباح، المساء، النوم، الكهف…)"),
        Part("extra", "الإمساك والضحى ومنتصف الليل"),
        Part("words", "آية اليوم وحديث اليوم"),
        Part("tomorrow", "المناسبة القادمة (غدًا الجمعة، الاثنين، الأيام البيض…)"),
        Part("stats", "صلوات اليوم والأسبوع والمناسبة القادمة"),
        Part("place", "الموقع وطريقة الحساب"),
        Part("notice", "مفتاح الإشعار الدائم"),
    )

    data class Short(val id: String, val icon: String, val label: String, val go: () -> Unit)
    val shorts = listOf(
        Short("qibla", "🧭", "القبلة") { Nav.morePage = "qibla"; Nav.go("more") },
        Short("adhkar", "📿", "الأذكار") { Nav.adhkar = 1; Nav.go("adhkar") },
        Short("quran", "📖", "القرآن") { Nav.library = 0; Nav.go("library") },
        Short("tasbih", "🟢", "السبحة") { Nav.adhkar = 2; Nav.go("adhkar") },
        Short("wird", "📜", "وردي") { Nav.wird = true },
        Short("morning", "🌅", "الصباح") { Nav.collection = "morning"; Nav.go("adhkar") },
        Short("evening", "🌇", "المساء") { Nav.collection = "evening"; Nav.go("adhkar") },
        Short("sleep", "🌙", "النوم") { Nav.collection = "sleep"; Nav.go("adhkar") },
        Short("kahf", "🕌", "الكهف") { Nav.reading = 18 to 1 },
        Short("awrad", "⭐", "أورادي") { Nav.adhkar = 0; Nav.go("adhkar") },
        Short("hadith", "📚", "الحديث") { Nav.library = 3; Nav.go("library") },
        Short("calendar", "📅", "التقويم") { Nav.morePage = "calendar"; Nav.go("more") },
        Short("seasons", "🌙", "المواسم") { Nav.morePage = "seasons"; Nav.go("more") },
        Short("alarm", "⏰", "المنبه") { Nav.morePage = "alarms"; Nav.go("more") },
        Short("world", "🌍", "الساعات") { Nav.morePage = "world"; Nav.go("more") },
        Short("hifz", "🧠", "الحفظ") { Nav.morePage = "hifz"; Nav.go("more") },
        Short("duas", "🤲", "أدعيتي") { Nav.adhkar = 3; Nav.go("adhkar") },
        Short("pet", "🐾", "صديقي") { Nav.morePage = "pet"; Nav.go("more") },
        Short("stats", "📊", "إحصائياتي") { Nav.morePage = "stats"; Nav.go("more") },
        Short("tasks", "☑", "المهام") { Nav.daily = 0; Nav.go("daily") },
        Short("focus", "⏱", "التركيز") { Nav.daily = 2; Nav.go("daily") },
        Short("khatma", "📗", "الختمة") { Nav.daily = 3; Nav.go("daily") },
    )
    private val defaultShorts = listOf("qibla", "adhkar", "quran", "khatma", "tasbih")
    val names = listOf("الصلاة", "الرئيسية", "وقتي", "البداية")

    var order: List<String>
        get() {
            // 1.1.37: «ذكر الوقت» moves right under today's times, once, in a saved layout too (the user asked for it).
            if (!Store.prefs.getBoolean("home_mig_137", false)) {
                Store.prefs.getString("home_order", null)?.split(',')?.toMutableList()?.let { l ->
                    if ("moment" in l && "times" in l) { l.remove("moment"); l.add(l.indexOf("times") + 1, "moment") }
                    Store.prefs.edit().putString("home_order", l.joinToString(",")).apply()
                }
                Store.prefs.edit().putBoolean("home_mig_137", true).apply()
            }
            val saved = Store.prefs.getString("home_order", null)?.split(',')?.filter { id -> parts.any { it.id == id } } ?: emptyList()
            val out = saved.toMutableList()
            parts.map { it.id }.filter { it !in saved }.forEach { id ->
                // A new part goes where it belongs (after the one before it in the default order).
                val before = parts.map { it.id }.let { all -> all.getOrNull(all.indexOf(id) - 1) }
                val at = out.indexOf(before)
                if (at >= 0) out.add(at + 1, id) else out += id
            }
            return out
        }
        set(v) { Store.prefs.edit().putString("home_order", v.joinToString(",")).apply(); Store.bump() }
    var hidden: Set<String>
        get() = Store.prefs.getStringSet("home_hidden", emptySet())!!.toSet()
        set(v) { Store.prefs.edit().putStringSet("home_hidden", v).apply(); Store.bump() }
    var shortcuts: List<String>
        get() = Store.prefs.getString("home_shorts", null)?.split(',')?.filter { id -> shorts.any { it.id == id } } ?: defaultShorts
        set(v) { Store.prefs.edit().putString("home_shorts", v.joinToString(",")).apply(); Store.bump() }
    var name: String
        get() = Store.prefs.getString("home_name", null) ?: names.first()
        set(v) { Store.prefs.edit().putString("home_name", v).apply(); Store.bump() }

    fun move(id: String, by: Int) {
        val l = order.toMutableList(); val i = l.indexOf(id); val j = i + by
        if (i < 0 || j !in l.indices) return
        l.add(j, l.removeAt(i)); order = l
    }
    fun reset() { Store.prefs.edit().remove("home_order").remove("home_hidden").remove("home_shorts").remove("home_name").remove("prayer_view").remove("view_chips").apply(); Store.bump() }
}

/** «ذكر الوقت»: the main moment, with the others as small chips under it. Refreshes every minute. */
@Composable
fun MomentCard() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); tick++ } }
    val list = remember(tick, v) { Moments.now().filterNot { Moments.done(it) } }
    val m = list.firstOrNull() ?: run {
        if (Moments.anyMarked()) Text("أتممت ذكر الوقت ✓ · إظهاره من جديد", color = P.muted, fontSize = 13.sp,
            modifier = Modifier.clip(L.shape(10.dp)).clickable { Moments.undoDone() }.padding(8.dp))
        return
    }
    Panel(Modifier.clickable { m.go() }, color = P.accent.copy(alpha = .12f), pad = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Glyph(m.icon, tint = P.accent, size = 30.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Sub("ذكر الوقت" + if (L.ornament.isNotEmpty()) " ${L.ornament}" else "", color = P.gold, size = 12.sp)
                Text(m.title, color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(m.text, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (25 * Data.textScale).sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 6.dp))
        Sub(m.src, size = 11.sp)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(m.button + " ›", color = P.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(L.shape(12.dp)).background(P.accent).clickable { m.go() }.padding(horizontal = 14.dp, vertical = 8.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text("✓ أتممته", color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(L.shape(12.dp)).border(1.dp, P.accent, L.shape(12.dp)).clickable { Moments.markDone(m); runCatching { MomentWidget.update(ctx) } }.padding(horizontal = 14.dp, vertical = 8.dp))
        }
        if (list.size > 1) {
            Sub("ويناسب الآن أيضًا:", Modifier.padding(top = 10.dp), size = 12.sp)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                list.drop(1).forEach { o -> Choice(o.icon + " " + o.title, false) { o.go() } }
            }
        }
    }
}

/** The user's shortcuts, five to a row. */
@Composable
fun HomeShortcuts() {
    val chosen = Home.shortcuts.mapNotNull { id -> Home.shorts.firstOrNull { it.id == id } }
    if (chosen.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chosen.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { s -> Tile(s.icon, s.label, Modifier.weight(1f)) { s.go() } }
                repeat(5 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Customizing the home page: the tab's name, the parts (show, hide, move), and the shortcuts. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeEditor(close: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = close, sheetState = state, containerColor = P.surface) {
        Column(Modifier.fillMaxWidth().quickDismiss(state, close).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Title("تخصيص الصفحة الرئيسية")
            Sub("اختر ما يظهر وترتيبه. التغيير يُحفظ فورًا.")

            Sub("اسم الصفحة في شريط التنقل", Modifier.padding(top = 6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Home.names.forEach { n -> Choice(n, Home.name == n) { Home.name = n } }
            }

            Sub("الأقسام (اضغط ▲ ▼ للترتيب)", Modifier.padding(top = 6.dp))
            val order = Home.order; val hidden = Home.hidden
            order.forEachIndexed { i, id ->
                val part = Home.parts.first { it.id == id }
                val on = id !in hidden
                Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.card).border(1.dp, P.line, L.shape(12.dp)).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(part.label, color = if (on) P.ink else P.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("▲", color = if (i > 0) P.accent else P.line, fontSize = 16.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable(enabled = i > 0) { Home.move(id, -1) }.padding(8.dp))
                    Text("▼", color = if (i < order.size - 1) P.accent else P.line, fontSize = 16.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable(enabled = i < order.size - 1) { Home.move(id, 1) }.padding(8.dp))
                    androidx.compose.material3.Switch(on, { Home.hidden = if (it) hidden - id else hidden + id })
                }
            }

            Sub("الاختصارات (حتى عشرة، بالترتيب الذي تختاره)", Modifier.padding(top = 6.dp))
            val mine = Home.shortcuts
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Home.shorts.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { s ->
                            val n = mine.indexOf(s.id)
                            Tile(s.icon, if (n >= 0) "${n + 1}· ${s.label}" else s.label, Modifier.weight(1f), on = n >= 0) {
                                Home.shortcuts = if (n >= 0) mine - s.id else if (mine.size < 10) mine + s.id else mine
                            }
                        }
                        repeat(4 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Text("↺ الإعدادات الافتراضية", color = P.accent, fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp).clip(L.shape(10.dp)).clickable { Home.reset() }.padding(8.dp))
        }
    }
}

/** A sheet that goes away with a short pull down: once its content is at the top, a small downward drag or fling closes it. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Modifier.quickDismiss(state: androidx.compose.material3.SheetState, close: () -> Unit): Modifier {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val conn = remember(state) {
        val limit = with(density) { 36.dp.toPx() }
        var pulled = 0f
        var closing = false
        fun shut() {
            if (closing) return; closing = true
            scope.launch { runCatching { state.hide() }; close() }
        }
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < 0) pulled = 0f
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                // Content is at its top and the finger keeps pulling down.
                if (available.y > 0 && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                    pulled += available.y
                    if (pulled > limit) shut()
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity { pulled = 0f; return androidx.compose.ui.unit.Velocity.Zero }
            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (available.y > 600f) { shut(); return available }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    return this.nestedScroll(conn)
}
