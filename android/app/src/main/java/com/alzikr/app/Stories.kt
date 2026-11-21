package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A story told in the Quran: where it is (surah, first verse, last verse) and a short introduction. */
data class QStory(val title: String, val intro: String, val s: Int, val from: Int, val to: Int)

object Stories {
    val quran = listOf(
        QStory("آدم عليه السلام", "خلق آدم واستخلافه في الأرض، وسجود الملائكة له وإباء إبليس، ثم سكناه الجنة وهبوطه منها وتوبته.", 2, 30, 39),
        QStory("ابنا آدم", "قربان ابني آدم، وقتل أحدهما أخاه حسدًا، والغراب الذي علّمه كيف يواري سوءة أخيه.", 5, 27, 31),
        QStory("نوح عليه السلام", "دعوة نوح قومه، وصنع السفينة، والطوفان، ونجاة المؤمنين، وابن نوح الذي أبى الركوب.", 11, 25, 49),
        QStory("هود وقوم عاد", "دعوة هود قومه عادًا إلى عبادة الله وحده، وعنادهم، ونجاة هود والذين آمنوا معه.", 11, 50, 60),
        QStory("صالح وناقة ثمود", "دعوة صالح قومه ثمود، وآية الناقة، وعقرهم لها، وأخذهم بالصيحة.", 11, 61, 68),
        QStory("إبراهيم وتحطيم الأصنام", "حوار إبراهيم مع أبيه وقومه، وتحطيمه الأصنام، وإلقاؤه في النار التي جعلها الله بردًا وسلامًا.", 21, 51, 70),
        QStory("إبراهيم والذبيح", "رؤيا إبراهيم ذبح ابنه، واستسلامهما لأمر الله، وفداء الذبيح بذبح عظيم.", 37, 99, 111),
        QStory("ضيف إبراهيم وقوم لوط", "الملائكة تبشّر إبراهيم بإسحاق، ثم تأتي لوطًا، وهلاك قومه الذين أصرّوا على الفاحشة.", 11, 69, 83),
        QStory("شعيب وأهل مدين", "دعوة شعيب قومه إلى التوحيد والعدل في الكيل والميزان، وعاقبة تكذيبهم.", 11, 84, 95),
        QStory("يوسف عليه السلام", "سمّاها الله أحسن القصص: رؤيا يوسف، وكيد إخوته، وصبره في البئر والسجن، ثم تمكينه ولقاؤه بأبيه.", 12, 4, 101),
        QStory("مولد موسى ونشأته", "وحي الله إلى أم موسى، ونشأته في بيت فرعون، وخروجه إلى مدين وزواجه.", 28, 7, 28),
        QStory("موسى وفرعون والسحرة", "نداء موسى عند الشجرة، وإرساله إلى فرعون مع هارون، وإيمان السحرة، ونجاة بني إسرائيل.", 20, 9, 79),
        QStory("بقرة بني إسرائيل", "أمر الله بني إسرائيل أن يذبحوا بقرة، وتشدّدهم في السؤال، وكشف القاتل.", 2, 67, 73),
        QStory("موسى والخضر", "رحلة موسى إلى مجمع البحرين للقاء العبد الصالح، والسفينة والغلام والجدار، وما فيها من الحكمة.", 18, 60, 82),
        QStory("قارون", "قارون الذي آتاه الله الكنوز فبغى وتكبّر، فخسف الله به وبداره الأرض.", 28, 76, 82),
        QStory("أصحاب السبت", "القرية التي اعتدى أهلها في يوم السبت بالحيلة، وعاقبة من عصى ونجاة من نهى عن السوء.", 7, 163, 166),
        QStory("طالوت وجالوت", "ملك طالوت على بني إسرائيل، وابتلاؤهم بالنهر، وقتل داود جالوت.", 2, 246, 251),
        QStory("داود والخصمان", "تسبيح الجبال والطير مع داود، والخصمان اللذان تسوّرا المحراب.", 38, 17, 26),
        QStory("سليمان والنملة والهدهد وملكة سبأ", "منطق الطير، والنملة، والهدهد وخبر سبأ، وعرش الملكة، وإسلامها مع سليمان لله رب العالمين.", 27, 15, 44),
        QStory("أيوب عليه السلام", "صبر أيوب على البلاء ودعاؤه ربه، وكشف الضر عنه.", 38, 41, 44),
        QStory("يونس عليه السلام", "يونس في بطن الحوت وتسبيحه، ونجاته، وإيمان قومه.", 37, 139, 148),
        QStory("زكريا ويحيى", "دعاء زكريا ربه خفية، والبشارة بيحيى.", 19, 2, 15),
        QStory("مريم وعيسى عليهما السلام", "البشارة لمريم، وولادة عيسى، وكلامه في المهد.", 19, 16, 36),
        QStory("أصحاب الكهف", "الفتية الذين آمنوا بربهم وأووا إلى الكهف، ولبثوا فيه سنين.", 18, 9, 26),
        QStory("صاحب الجنتين", "مثل الرجلين: صاحب البستانين المغترّ بماله، وصاحبه المؤمن الناصح.", 18, 32, 44),
        QStory("ذو القرنين", "رحلات ذي القرنين إلى المغرب والمشرق، وبناء السدّ أمام يأجوج ومأجوج.", 18, 83, 98),
        QStory("لقمان يعظ ابنه", "وصايا لقمان الحكيم لابنه في التوحيد وبرّ الوالدين والصلاة والأخلاق.", 31, 12, 19),
        QStory("أصحاب القرية", "الرسل الذين جاؤوا القرية، والرجل الذي جاء من أقصى المدينة يسعى.", 36, 13, 29),
        QStory("مؤمن آل فرعون", "الرجل المؤمن من آل فرعون الذي كتم إيمانه ثم صدع بالنصيحة.", 40, 28, 45),
        QStory("أصحاب الجنة", "أصحاب البستان الذين أقسموا ليقطفنّه ويمنعوا المساكين، فأصبح كالصريم.", 68, 17, 33),
        QStory("أصحاب الأخدود", "المؤمنون الذين عُذّبوا في الأخدود لأنهم آمنوا بالله العزيز الحميد.", 85, 4, 10),
        QStory("أصحاب الفيل", "الجيش الذي جاء بالفيل ليهدم الكعبة، فأرسل الله عليهم طيرًا أبابيل.", 105, 1, 5),
        QStory("الذي مرّ على قرية", "الرجل الذي أماته الله مئة عام ثم بعثه، آيةً على البعث.", 2, 259, 259),
        QStory("إبراهيم والطير", "سؤال إبراهيم ربه أن يريه كيف يحيي الموتى، ليطمئن قلبه.", 2, 260, 260),
    )

    /** Story categories on IslamHouse (their books). */
    val islamic = listOf(
        Triple("5387", "قصص الأنبياء", "سير الأنبياء والرسل عليهم السلام"),
        Triple("795", "السيرة النبوية", "حياة النبي ﷺ من المولد إلى الوفاة"),
        Triple("398195", "الصحابة وآل البيت", "سير الصحابة وآل بيت النبي ﷺ وفضائلهم"),
        Triple("221823", "التراجم وسير الأعلام", "أئمة الإسلام والمصلحون"),
        Triple("398220", "قصص وعبر", "قصص فيها عظة واعتبار"),
        Triple("221824", "لماذا أسلموا؟", "قصص المسلمين الجدد"),
        Triple("200217", "الرقائق والمواعظ", "ما يرقّق القلب ويذكّر بالآخرة"),
    )
}

/** Quran stories and Islamic stories. */
@Composable
fun StoriesHome(openBooks: (String, String) -> Unit) {
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    Nav.story?.let { open = it; Nav.story = null }
    open?.let { i -> StoryReader(Stories.quran[i]) { open = null }; return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Hint("stories", "📖", "قصص القرآن تُقرأ بآياتها وتفسيرها الميسّر، ويمكنك الاستماع إليها. وفي الأسفل كتب القصص الإسلامية: الأنبياء والسيرة والصحابة.") }
        item { Title("قصص القرآن الكريم") }
        items(Stories.quran.indices.toList()) { i ->
            val st = Stories.quran[i]
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, P.line, L.shape(14.dp)).clickable { open = i }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(st.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = L.head ?: L.body)
                    Sub("سورة ${if (Quran.ready) Quran.surahs[st.s - 1].name else st.s.toString()} · ${if (st.to > st.from) "الآيات ${Quran.digits(st.from)}–${Quran.digits(st.to)}" else "الآية ${Quran.digits(st.from)}"}", size = 12.sp)
                }
                Text("‹", color = P.muted, fontSize = 20.sp)
            }
        }
        item { Title("قصص إسلامية (كتب)", Modifier.padding(top = 14.dp)) }
        items(Stories.islamic) { (id, name, about) ->
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card).clickable { openBooks(id, name) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Sub(about, size = 12.sp)
                }
                Text("📚", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun StoryReader(st: QStory, back: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val tv = Tafsir.version
    var withTafsir by rememberSaveable { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(withTafsir) {
        if (withTafsir && !Tafsir.has(ctx, "muyassar", st.s)) { loading = true; withContext(Dispatchers.IO) { Tafsir.fetch(ctx, "muyassar", st.s) }; loading = false }
    }
    var readMode by rememberSaveable { mutableStateOf(false) }
    if (readMode && Quran.ready) {
        val blocks = ArrayList<RBlock>()
        blocks += RBlock(RKind.NOTE, st.intro)
        blocks += RBlock(RKind.SMALL, "سورة ${Quran.surahs[st.s - 1].name} · الآيات ${st.from}–${st.to}")
        (st.from..st.to).mapNotNull { Quran.at(st.s, it) }.forEach { a ->
            blocks += RBlock(RKind.QURAN, a.text + " ﴿${Quran.digits(a.a)}﴾")
            if (withTafsir) Tafsir.get(ctx, "muyassar", a.s, a.a)?.takeIf { it.from == a.a && it.text.isNotBlank() }?.let { blocks += RBlock(RKind.NOTE, it.text) }
        }
        TextReading(st.title, "story:${st.s}:${st.from}", blocks) { readMode = false }
        return
    }
    Subpage(st.title, back) {
        if (!Quran.ready) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Sub("…") }; return@Subpage }
        val verses = (st.from..st.to).mapNotNull { Quran.at(st.s, it) }
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Panel(color = P.card) {
                        Text(st.intro, color = P.ink, fontSize = 15.sp, lineHeight = 24.sp)
                        Sub("سورة ${Quran.surahs[st.s - 1].name} · ${verses.size} آية", Modifier.padding(top = 6.dp), size = 12.sp, color = P.gold)
                    }
                }
                if (loading) item { Sub("جارٍ تحميل التفسير الميسّر…", size = 12.sp, color = P.gold) }
                items(verses, key = { it.a }) { a ->
                    val reading = Recitation.now == (a.s to a.a)
                    Text(a.text + " ﴿${Quran.digits(a.a)}﴾", color = P.ink, fontSize = Quran.fontSize.sp, lineHeight = (Quran.fontSize * 1.9f).sp, fontFamily = Fonts.quran,
                        modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).background(if (reading) P.accent.copy(alpha = .18f) else P.bg).padding(6.dp))
                    if (withTafsir) Tafsir.get(ctx, "muyassar", a.s, a.a)?.takeIf { it.from == a.a && it.text.isNotBlank() }?.let { e ->
                        Text(e.text, color = P.muted, fontSize = (14 * Data.textScale).sp, lineHeight = (23 * Data.textScale).sp, fontFamily = Fonts.naskh,
                            modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).background(P.card).padding(10.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().background(P.surface).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Recitation.now == null) Tile("▶", "استمع للقصة", Modifier.weight(1f)) { Recitation.play(ctx, st.s, st.from, stopAfter = st.to) }
                else Tile("⏹", "إيقاف التلاوة", Modifier.weight(1f), on = true) { Recitation.stop() }
                Tile("📖", if (withTafsir) "إخفاء التفسير" else "إظهار التفسير", Modifier.weight(1f), on = withTafsir) { withTafsir = !withTafsir }
                Tile("📜", "وضع القراءة", Modifier.weight(1f)) { readMode = true }
                Tile("↗", "في المصحف", Modifier.weight(1f)) { Nav.openQuran(st.s, st.from) }
            }
        }
    }
}
