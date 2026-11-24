package com.alzikr.app

import java.time.LocalTime

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.remember

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What a day holds: Friday, Monday and Thursday, the white days, Arafah, the Eids, Ashura, the start of Ramadan,
 * the last ten, the ten of Dhul-Hijjah, the six of Shawwal — each with its virtue and its sunan, from authentic Sunni sources.
 */
object DayInfo {
    data class G(val text: String, val src: String)
    data class Part(val icon: String, val name: String, val virtue: List<G>, val sunan: List<G>, val guide: String? = null, val kahf: Boolean = false)
    data class Card(val date: LocalDate, val parts: List<Part>) {
        val title get() = parts.joinToString(" · ") { it.name }
    }

    private val days = listOf("", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    fun dayName(d: LocalDate) = days[d.dayOfWeek.value]

    fun of(d: LocalDate): Card? {
        val h = Data.hijri(d)
        val out = mutableListOf<Part>()
        // The Hijri occasions first.
        when {
            h.month == 9 && h.day == 1 -> out += Part("🌙", "أول رمضان", listOf(
                G("«إذا دخل رمضان فُتّحت أبواب الجنة، وغُلّقت أبواب جهنم، وسُلسلت الشياطين».", "البخاري ومسلم"),
                G("«من صام رمضان إيمانًا واحتسابًا غُفر له ما تقدم من ذنبه».", "البخاري ومسلم")), listOf(
                G("تبييت نية الصيام من الليل، والسحور: «تسحّروا فإن في السحور بركة».", "البخاري ومسلم"),
                G("صلاة التراويح: «من قام رمضان إيمانًا واحتسابًا غُفر له ما تقدم من ذنبه».", "البخاري ومسلم")), "ramadan")
            h.month == 9 && h.day == 21 -> out += Part("✨", "أول العشر الأواخر", listOf(
                G("«تحرّوا ليلة القدر في الوتر من العشر الأواخر من رمضان».", "البخاري"),
                G("﴿لَيْلَةُ الْقَدْرِ خَيْرٌ مِّنْ أَلْفِ شَهْرٍ﴾.", "القدر: ٣")), listOf(
                G("«كان رسول الله ﷺ إذا دخل العشر أحيا الليل، وأيقظ أهله، وجدّ وشدّ المئزر».", "البخاري ومسلم"),
                G("الاعتكاف، والإكثار من: «اللهم إنك عفوّ تحب العفو فاعفُ عني».", "الترمذي، وصححه الألباني")), "lastten")
            h.month == 9 && h.day in listOf(23, 25, 27, 29) -> out += Part("✨", "ليلة ${h.day} من رمضان (وترية)", listOf(
                G("«تحرّوا ليلة القدر في الوتر من العشر الأواخر من رمضان».", "البخاري"),
                G("«من قام ليلة القدر إيمانًا واحتسابًا غُفر له ما تقدم من ذنبه».", "البخاري ومسلم")), listOf(
                G("القيام والدعاء في ليلتها (وهي الليلة التي تسبق هذا اليوم)، والإكثار من: «اللهم إنك عفوّ تحب العفو فاعفُ عني».", "الترمذي، وصححه الألباني")), "lastten")
            h.month == 1 && h.day == 1 -> out += Part("🌙", "أول المحرّم", listOf(
                G("«أفضل الصيام بعد رمضان شهر الله المحرّم».", "مسلم"),
                G("المحرّم من الأشهر الحرم: ﴿منها أربعة حرم فلا تظلموا فيهن أنفسكم﴾.", "التوبة: ٣٦")), listOf(
                G("الإكثار من الصيام في الشهر، ولا يُخصّ أول العام بعبادة لم ترد.", "")), "muharram")
            h.month == 10 && h.day == 1 -> out += Part("🎉", "عيد الفطر", listOf(
                G("يوم فرح بإتمام الصيام، ولا يُصام: «نهى رسول الله ﷺ عن صيام يومين: يوم الفطر ويوم النحر».", "البخاري ومسلم")), listOf(
                G("إخراج زكاة الفطر قبل الصلاة.", "البخاري ومسلم"),
                G("«كان رسول الله ﷺ لا يغدو يوم الفطر حتى يأكل تمرات… ويأكلهن وترًا».", "البخاري"),
                G("التكبير، ومخالفة الطريق في الذهاب والرجوع.", "البخاري")), "fitr")
            h.month == 10 && h.day == 2 -> out += Part("🌙", "ستّ من شوال", listOf(
                G("«من صام رمضان ثم أتبعه ستًّا من شوال كان كصيام الدهر».", "مسلم")), listOf(
                G("تُصام متتابعة أو متفرقة في شوال، بعد يوم العيد.", "")))
            h.month == 12 && h.day == 1 -> out += Part("🕋", "أول عشر ذي الحجة", listOf(
                G("«ما من أيام العمل الصالح فيهن أحب إلى الله من هذه الأيام»، يعني أيام العشر.", "أبو داود والترمذي بهذا اللفظ، وأصله في البخاري")), listOf(
                G("الإكثار من التكبير والتهليل والتحميد، والصيام، والصدقة.", "أحمد، وصححه أحمد شاكر"),
                G("«من أراد أن يضحّي فلا يأخذ من شعره وأظفاره شيئًا» حتى يذبح.", "مسلم")), "dhul")
            h.month == 12 && h.day == 9 -> out += Part("🤲", "يوم عرفة", listOf(
                G("«ما من يوم أكثر من أن يعتق الله فيه عبدًا من النار من يوم عرفة».", "مسلم"),
                G("«صيام يوم عرفة أحتسب على الله أن يكفّر السنة التي قبله والسنة التي بعده».", "مسلم")), listOf(
                G("صيامه لغير الحاج.", "مسلم"),
                G("«خير الدعاء دعاء يوم عرفة، وخير ما قلت أنا والنبيون من قبلي: لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير».", "الترمذي، وحسّنه الألباني")), "arafah")
            h.month == 12 && h.day == 10 -> out += Part("🐑", "عيد الأضحى", listOf(
                G("«إن أعظم الأيام عند الله تبارك وتعالى يوم النحر».", "أبو داود، وصححه الألباني")), listOf(
                G("صلاة العيد، والتكبير، وذبح الأضحية بعد الصلاة.", "البخاري ومسلم"),
                G("لا يُصام يوم العيد.", "البخاري ومسلم")), "adha")
            h.month == 12 && h.day in 11..13 -> out += Part("🐑", "أيام التشريق", listOf(
                G("«أيام التشريق أيام أكل وشرب وذكر لله».", "مسلم")), listOf(
                G("التكبير بعد الصلوات وفي كل وقت، ولا تُصام.", "مسلم")), "adha")
            h.month == 1 && h.day == 9 -> out += Part("📿", "تاسوعاء", listOf(
                G("«لئن بقيت إلى قابل لأصومنّ التاسع».", "مسلم")), listOf(
                G("صيامه مع عاشوراء مخالفةً لأهل الكتاب.", "مسلم")), "muharram")
            h.month == 1 && h.day == 10 -> out += Part("📿", "عاشوراء", listOf(
                G("«صيام يوم عاشوراء أحتسب على الله أن يكفّر السنة التي قبله».", "مسلم"),
                G("فيه نجّى الله موسى عليه السلام وقومه.", "البخاري ومسلم")), listOf(
                G("صيامه، ويُصام معه التاسع.", "مسلم")), "muharram")
        }
        if (h.month != 9 && h.day in 13..15) out += Part("🌕", "من الأيام البيض (${h.day} ${h.monthName})", listOf(
            G("«صيام ثلاثة أيام من كل شهر صيام الدهر كله».", "البخاري ومسلم")), listOf(
            G("«إذا صمت من الشهر ثلاثًا فصم ثلاث عشرة وأربع عشرة وخمس عشرة».", "الترمذي والنسائي، وحسّنه الألباني")), "white")

        // The days of the week.
        when (d.dayOfWeek) {
            DayOfWeek.FRIDAY -> out += Part("🕌", "يوم الجمعة", listOf(
                G("«خير يوم طلعت عليه الشمس يوم الجمعة: فيه خُلق آدم، وفيه أُدخل الجنة، وفيه أُخرج منها».", "مسلم"),
                G("«فيه ساعة لا يوافقها عبد مسلم وهو قائم يصلي يسأل الله شيئًا إلا أعطاه إياه».", "البخاري ومسلم")), listOf(
                G("الغسل والطيب والسواك ولبس أحسن الثياب: «غُسل يوم الجمعة واجب على كل محتلم».", "البخاري ومسلم"),
                G("التبكير إلى المسجد: «من راح في الساعة الأولى فكأنما قرّب بدنة».", "البخاري ومسلم"),
                G("قراءة سورة الكهف: «أضاء له من النور ما بين الجمعتين».", "الحاكم والبيهقي، وصححه الألباني"),
                G("الإكثار من الصلاة على النبي ﷺ: «فإن صلاتكم معروضة عليّ».", "أبو داود، وصححه الألباني"),
                G("الإنصات للخطبة: «إذا قلت لصاحبك يوم الجمعة: أنصت، والإمام يخطب، فقد لغوت».", "البخاري ومسلم"),
                G("الدعاء في آخر ساعة بعد العصر.", "أبو داود والنسائي، وصححه الألباني"),
                G("كان النبي ﷺ يقرأ في فجر الجمعة: ﴿الم تنزيل﴾ السجدة و﴿هل أتى على الإنسان﴾.", "البخاري ومسلم")), kahf = true)
            DayOfWeek.MONDAY, DayOfWeek.THURSDAY -> if (h.month != 9) out += Part("🍽", "يوم ${dayName(d)}", listOf(
                G("«تُعرض الأعمال يوم الاثنين والخميس، فأحب أن يُعرض عملي وأنا صائم».", "الترمذي، وصححه الألباني"),
                G("«تُفتح أبواب الجنة يوم الاثنين ويوم الخميس، فيُغفر لكل عبد لا يشرك بالله شيئًا، إلا رجلًا كانت بينه وبين أخيه شحناء».", "مسلم")) +
                (if (d.dayOfWeek == DayOfWeek.MONDAY) listOf(G("سُئل ﷺ عن صوم الاثنين فقال: «ذاك يوم وُلدت فيه، ويوم بُعثت أو أُنزل عليّ فيه».", "مسلم")) else emptyList()), listOf(
                G("صيامه تطوعًا، وتبييت النية من الليل، والسحور.", "الترمذي؛ والسحور في البخاري ومسلم"),
                G("إصلاح ما بينك وبين الناس، حتى تُغفر لك.", "مسلم")))
            else -> {}
        }
        return if (out.isEmpty()) null else Card(d, out)
    }

    /** The evening reminder's time (minutes from midnight). */
    var eveAt: Int
        get() = Store.prefs.getInt("eve_at", 20 * 60 + 30)
        set(v) { Store.prefs.edit().putInt("eve_at", v).apply(); Store.bump() }

    fun line(c: Card) = c.parts.first().let { p -> p.virtue.firstOrNull()?.text ?: "" }
}

/** A day's card: its virtue and its sunan, with a link to its guide and the Kahf on Friday. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(date: LocalDate, close: () -> Unit) {
    val card = DayInfo.of(date) ?: run { close(); return }
    val state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rel = when (date) { LocalDate.now() -> "اليوم"; LocalDate.now().plusDays(1) -> "غدًا"; else -> "" }
    val h = Data.hijri(date)
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = close, sheetState = state, containerColor = P.surface) {
        Column(Modifier.fillMaxWidth().quickDismiss(state, close).verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Sub("$rel ${DayInfo.dayName(date)} ${h.day} ${h.monthName} ${h.year} هـ".trim(), color = P.gold)
            card.parts.forEach { part ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Glyph(part.icon, tint = P.accent, size = 30.dp)
                    Text(part.name, color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body, modifier = Modifier.padding(start = 10.dp))
                }
                Panel(color = P.card, pad = 14.dp) {
                    Text("فضله", color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    part.virtue.forEach { g -> Said(g) }
                }
                Panel(color = P.card, pad = 14.dp) {
                    Text("سننه وأعماله", color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    part.sunan.forEach { g -> Said(g) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (part.kahf) Tile("📖", "سورة الكهف", Modifier.weight(1f)) { close(); Data.setExtra(LocalDate.now(), "kahf", true); Nav.reading = 18 to 1 }
                    if (part.kahf) Tile("📿", "الصلاة على النبي ﷺ", Modifier.weight(1f)) { close(); Nav.adhkar = 2; Nav.go("adhkar") }
                    part.guide?.let { g -> Tile("🌙", "الدليل كاملًا", Modifier.weight(1f)) { close(); Nav.morePage = "season:$g"; Nav.go("more") } }
                }
            }
        }
    }
}

@Composable
private fun Said(g: DayInfo.G) {
    Text(g.text, color = P.ink, fontSize = (16 * Data.textScale).sp, lineHeight = (27 * Data.textScale).sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 8.dp))
    if (g.src.isNotEmpty()) Sub(g.src, size = 11.sp)
}

/** «غدًا …» on the home page when tomorrow has something; tap for its card. */
@Composable
fun TomorrowCard(open: (LocalDate) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val now = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val maghrib = if (Store.hasLocation) Store.times().getValue(Prayer.MAGHRIB) else 1080
    // Before maghrib show today's day if it has something; after it, tomorrow's.
    val date = if (now < maghrib && DayInfo.of(LocalDate.now()) != null) LocalDate.now() else LocalDate.now().plusDays(1)
    val card = DayInfo.of(date) ?: return
    val rel = if (date == LocalDate.now()) "اليوم" else "غدًا"
    Row(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(P.gold.copy(alpha = .14f)).clickable { open(date) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Glyph(card.parts.first().icon, tint = P.gold, size = 28.dp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text("$rel ${card.title}", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Sub("اضغط لفضله وسننه", size = 12.sp)
        }
        Text("‹", color = P.gold, fontSize = 22.sp)
    }
}

/** The times when voluntary prayer is not prayed: after Fajr until the sun is up a spear's length, at the zenith, after Asr until sunset. */
object Forbid {
    /** (what, until minute) when now is one of them. */
    fun now(): Pair<String, Int>? {
        if (!Store.hasLocation) return null
        val t = Store.times(); val m = LocalTime.now().let { it.hour * 60 + it.minute }
        val fajr = t.getValue(Prayer.FAJR); val sun = t.getValue(Prayer.SUNRISE); val dhuhr = t.getValue(Prayer.DHUHR)
        val asr = t.getValue(Prayer.ASR); val maghrib = t.getValue(Prayer.MAGHRIB)
        val prayedFajr = Data.prayed(LocalDate.now(), Prayer.FAJR) || m >= fajr + 25
        val prayedAsr = Data.prayed(LocalDate.now(), Prayer.ASR) || m >= asr + 25
        return when {
            m in sun until sun + 15 -> "طلوع الشمس حتى ترتفع" to sun + 15
            m in fajr until sun && prayedFajr -> "بعد صلاة الفجر حتى ترتفع الشمس" to sun + 15
            m in dhuhr - 10 until dhuhr -> "قيام الشمس في وسط السماء حتى تزول" to dhuhr
            m in asr until maghrib && prayedAsr -> "بعد صلاة العصر حتى تغرب الشمس" to maghrib
            else -> null
        }
    }
    val proofs = listOf(
        DayInfo.G("«لا صلاة بعد الصبح حتى ترتفع الشمس، ولا صلاة بعد العصر حتى تغيب الشمس».", "البخاري ومسلم"),
        DayInfo.G("عن عقبة بن عامر: «ثلاث ساعات كان رسول الله ﷺ ينهانا أن نصلي فيهن… حين تطلع الشمس بازغة حتى ترتفع، وحين يقوم قائم الظهيرة حتى تميل الشمس، وحين تضيّف الشمس للغروب حتى تغرب».", "مسلم"),
        DayInfo.G("ويُقضى الفرض الفائت في أي وقت: «من نام عن صلاة أو نسيها فليصلها إذا ذكرها».", "البخاري ومسلم"),
        DayInfo.G("واختلف العلماء في ذوات الأسباب كتحية المسجد وسنة الوضوء في هذه الأوقات.", ""),
    )
}

/** A slim line on the home page while a forbidden time lasts; tap for its evidence. */
@Composable
fun ForbidBanner() {
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); tick++ } }
    @Suppress("UNUSED_VARIABLE") val t0 = tick
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val (what, until) = Forbid.now() ?: return
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.danger.copy(alpha = .10f)).clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Glyph("⛔", tint = P.danger, size = 22.dp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text("وقت نهي عن صلاة النافلة حتى ${Alerts.timeText(until)}", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Sub(what + " · اضغط للدليل", size = 12.sp)
        }
    }
    if (open) androidx.compose.material3.AlertDialog(onDismissRequest = { open = false }, containerColor = P.surface,
        title = { Text("أوقات النهي عن الصلاة", color = P.ink) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Forbid.proofs.forEach { g -> Text(g.text, color = P.ink, fontSize = 15.sp, lineHeight = 25.sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 8.dp)); if (g.src.isNotEmpty()) Sub(g.src, size = 11.sp) } } },
        confirmButton = { androidx.compose.material3.TextButton({ open = false }) { Text("حسنًا") } })
}
