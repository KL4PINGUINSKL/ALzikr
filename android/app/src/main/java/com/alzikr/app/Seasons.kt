package com.alzikr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Guides for the seasons of worship: virtues, authentic adhkar and duas, and a summary of the
 * rulings, each with its source (Sunni sources only). Plain letters, like the rest of Content.
 */
object Seasons {
    data class G(val text: String, val src: String = "")
    data class Sec(val title: String, val items: List<G>)
    data class Guide(val id: String, val title: String, val icon: String, val about: String, val sections: List<Sec>, val related: List<String> = emptyList())

    private const val TAKBIR = "الله أكبر الله أكبر، لا إله إلا الله، والله أكبر الله أكبر ولله الحمد"
    private const val TALBIYA = "لبيك اللهم لبيك، لبيك لا شريك لك لبيك، إن الحمد والنعمة لك والملك، لا شريك لك"
    private const val NOTE = "هذا ملخص للتذكير، وفي المسائل تفصيل وخلاف بين أهل العلم؛ فاسأل أهل العلم في بلدك عما يخصك."

    val all = listOf(
        Guide("ramadan", "رمضان", "🌙", "شهر الصيام والقيام والقرآن، فيه أُنزل القرآن وفيه ليلة القدر.", listOf(
            Sec("فضله", listOf(
                G("من صام رمضان إيمانا واحتسابا غفر له ما تقدم من ذنبه", "متفق عليه"),
                G("من قام رمضان إيمانا واحتسابا غفر له ما تقدم من ذنبه", "متفق عليه"),
                G("إذا جاء رمضان فتحت أبواب الجنة، وغلقت أبواب النار، وصفدت الشياطين", "متفق عليه"),
                G("﴿شهر رمضان الذي أنزل فيه القرآن هدى للناس وبينات من الهدى والفرقان﴾", "البقرة: 185"),
            )),
            Sec("أذكار وأدعية", listOf(
                G("عند رؤية الهلال: اللهم أهلله علينا باليمن والإيمان، والسلامة والإسلام، ربي وربك الله", "الترمذي، وحسّنه"),
                G("عند الإفطار: ذهب الظمأ، وابتلت العروق، وثبت الأجر إن شاء الله", "أبو داود، وحسّنه الألباني"),
                G("إذا أفطر عند قوم: أفطر عندكم الصائمون، وأكل طعامكم الأبرار، وصلت عليكم الملائكة", "أبو داود، وصححه الألباني"),
                G("إذا شاتمه أحد أو قاتله فليقل: إني صائم", "متفق عليه"),
            )),
            Sec("أحكام مختصرة", listOf(
                G("تجب نية صيام الفرض من الليل قبل الفجر، ويكفي العزم على الصيام.", "لحديث: من لم يجمع الصيام قبل الفجر فلا صيام له (أصحاب السنن)"),
                G("يفسد الصوم: الأكل والشرب والجماع عمدا، وتعمد القيء، وخروج دم الحيض والنفاس.", ""),
                G("من أكل أو شرب ناسيا فليتم صومه، فإنما أطعمه الله وسقاه.", "متفق عليه"),
                G("المريض والمسافر يفطران ويقضيان عدة من أيام أخر. والكبير العاجز والمريض الذي لا يرجى برؤه يطعم عن كل يوم مسكينا.", "البقرة: 184"),
                G("الحائض والنفساء تفطران وتقضيان الصيام، ولا تقضيان الصلاة.", "قالت عائشة: كنا نؤمر بقضاء الصوم ولا نؤمر بقضاء الصلاة (مسلم)"),
            )),
            Sec("سنن وأعمال", listOf(
                G("تسحروا فإن في السحور بركة", "متفق عليه"),
                G("لا يزال الناس بخير ما عجلوا الفطر", "متفق عليه"),
                G("كان يفطر على رطبات، فإن لم تكن فتمرات، فإن لم تكن حسا حسوات من ماء", "أبو داود والترمذي"),
                G("من فطر صائما كان له مثل أجره، غير أنه لا ينقص من أجر الصائم شيئا", "الترمذي، وصححه"),
                G("عمرة في رمضان تعدل حجة", "متفق عليه"),
                G("صلاة التراويح، والإكثار من تلاوة القرآن ومدارسته، والجود والصدقة؛ كان رسول الله صلى الله عليه وسلم أجود ما يكون في رمضان.", "البخاري"),
            )),
        ), listOf("lastten", "fitr")),

        Guide("lastten", "العشر الأواخر وليلة القدر", "✨", "أفضل ليالي السنة، وفيها ليلة القدر خير من ألف شهر.", listOf(
            Sec("فضلها", listOf(
                G("﴿ليلة القدر خير من ألف شهر﴾", "القدر: 3"),
                G("من قام ليلة القدر إيمانا واحتسابا غفر له ما تقدم من ذنبه", "متفق عليه"),
                G("تحروا ليلة القدر في الوتر من العشر الأواخر من رمضان", "البخاري"),
                G("كان النبي صلى الله عليه وسلم إذا دخل العشر شد مئزره، وأحيا ليله، وأيقظ أهله", "متفق عليه"),
            )),
            Sec("الدعاء", listOf(
                G("قالت عائشة: يا رسول الله، أرأيت إن علمت أي ليلة ليلة القدر ما أقول فيها؟ قال: قولي: اللهم إنك عفو تحب العفو فاعف عني", "الترمذي وابن ماجه، وصححه الألباني"),
            )),
            Sec("أعمال", listOf(
                G("الاعتكاف: كان النبي صلى الله عليه وسلم يعتكف العشر الأواخر من رمضان حتى توفاه الله", "متفق عليه"),
                G("إحياء الليل بالصلاة والقرآن والذكر والدعاء، والصدقة ولو بالقليل كل ليلة.", ""),
            )),
        ), listOf("ramadan", "fitr")),

        Guide("fitr", "عيد الفطر وزكاة الفطر", "🎉", "يوم الجائزة، يفرح فيه المسلمون بإتمام الصيام.", listOf(
            Sec("زكاة الفطر", listOf(
                G("فرض رسول الله صلى الله عليه وسلم زكاة الفطر صاعا من تمر أو صاعا من شعير، على العبد والحر، والذكر والأنثى، والصغير والكبير من المسلمين، وأمر بها أن تؤدى قبل خروج الناس إلى الصلاة", "متفق عليه"),
                G("تُخرج عن كل فرد صاعا من غالب قوت البلد، ويجوز إخراجها قبل العيد بيوم أو يومين، ولا تؤخر عن صلاة العيد.", "كان ابن عمر يعطيها قبل الفطر بيوم أو يومين (البخاري)"),
            )),
            Sec("التكبير", listOf(
                G(TAKBIR, "ثابت عن ابن مسعود رضي الله عنه (ابن أبي شيبة)؛ يُكبَّر من غروب شمس آخر يوم من رمضان حتى صلاة العيد"),
                G("﴿ولتكملوا العدة ولتكبروا الله على ما هداكم ولعلكم تشكرون﴾", "البقرة: 185"),
            )),
            Sec("سنن العيد", listOf(
                G("كان لا يغدو يوم الفطر حتى يأكل تمرات، ويأكلهن وترا", "البخاري"),
                G("كان إذا كان يوم عيد خالف الطريق", "البخاري"),
                G("الاغتسال والتطيب ولبس أحسن الثياب، وصلاة العيد مع المسلمين.", ""),
                G("التهنئة: تقبل الله منا ومنكم", "ثابت عن الصحابة رضي الله عنهم"),
            )),
            Sec("بعد العيد", listOf(
                G("من صام رمضان ثم أتبعه ستا من شوال كان كصيام الدهر", "مسلم"),
            )),
        ), listOf("ramadan")),

        Guide("dhul", "عشر ذي الحجة", "🕋", "أفضل أيام الدنيا، والعمل الصالح فيها أحب إلى الله.", listOf(
            Sec("فضلها", listOf(
                G("ما من أيام العمل الصالح فيها أحب إلى الله من هذه الأيام، يعني أيام العشر. قالوا: يا رسول الله، ولا الجهاد في سبيل الله؟ قال: ولا الجهاد في سبيل الله، إلا رجل خرج بنفسه وماله فلم يرجع من ذلك بشيء", "البخاري"),
                G("﴿والفجر وليال عشر﴾", "الفجر: 1–2"),
            )),
            Sec("الذكر والتكبير", listOf(
                G(TAKBIR, "التكبير المطلق من أول العشر في كل وقت، والمقيد بعد الصلوات من فجر يوم عرفة إلى عصر آخر أيام التشريق"),
                G("سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر: أكثروا فيهن من التهليل والتكبير والتحميد", "أحمد"),
            )),
            Sec("أحكام وأعمال", listOf(
                G("إذا دخلت العشر وأراد أحدكم أن يضحي فلا يمس من شعره وبشره شيئا", "مسلم"),
                G("صيام ما تيسر منها، وآكدها صيام يوم عرفة لغير الحاج، ولا يصام يوم العيد.", ""),
                G("الحج والعمرة لمن استطاع، والصدقة، وصلة الرحم، والإكثار من النوافل.", ""),
            )),
        ), listOf("arafah", "hajj", "adha")),

        Guide("arafah", "يوم عرفة", "⛰", "اليوم التاسع من ذي الحجة، فيه الوقوف بعرفة ركن الحج الأعظم، وهو يوم العتق من النار.", listOf(
            Sec("فضله", listOf(
                G("ما من يوم أكثر من أن يعتق الله فيه عبدا من النار من يوم عرفة", "مسلم"),
                G("صيام يوم عرفة أحتسب على الله أن يكفر السنة التي قبله والسنة التي بعده", "مسلم"),
                G("﴿اليوم أكملت لكم دينكم وأتممت عليكم نعمتي ورضيت لكم الإسلام دينا﴾ نزلت يوم عرفة", "متفق عليه"),
            )),
            Sec("الدعاء", listOf(
                G("خير الدعاء دعاء يوم عرفة، وخير ما قلت أنا والنبيون من قبلي: لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير", "الترمذي، وحسّنه الألباني"),
                G("أكثر من الدعاء لنفسك ولوالديك وللمسلمين، خاصة آخر النهار.", ""),
            )),
            Sec("أحكام", listOf(
                G("يستحب صيامه لغير الحاج، أما الحاج فالسنة له الفطر ليتقوى على الدعاء.", "أفطر النبي صلى الله عليه وسلم بعرفة (متفق عليه)"),
                G("يبدأ التكبير المقيد بعد الصلوات من فجر يوم عرفة.", ""),
            )),
        ), listOf("dhul", "hajj", "adha")),

        Guide("adha", "عيد الأضحى والأضحية", "🐑", "يوم النحر، أعظم الأيام عند الله، وتليه أيام التشريق.", listOf(
            Sec("سنن العيد", listOf(
                G("كان النبي صلى الله عليه وسلم لا يطعم يوم الأضحى حتى يصلي", "الترمذي"),
                G(TAKBIR, "يستمر التكبير إلى عصر آخر أيام التشريق"),
                G("أيام التشريق أيام أكل وشرب وذكر لله", "مسلم"),
                G("التهنئة: تقبل الله منا ومنكم", "ثابت عن الصحابة رضي الله عنهم"),
            )),
            Sec("الأضحية", listOf(
                G("وقتها بعد صلاة العيد إلى غروب شمس آخر أيام التشريق (13 ذي الحجة). من ذبح قبل الصلاة فإنما يذبح لنفسه", "متفق عليه"),
                G("السن المجزئة: الإبل خمس سنين، والبقر سنتان، والمعز سنة، والضأن ما تم له ستة أشهر.", ""),
                G("أربع لا تجوز في الأضاحي: العوراء البين عورها، والمريضة البين مرضها، والعرجاء البين ظلعها، والكسيرة التي لا تنقي", "أبو داود والترمذي، وصححه"),
                G("الشاة تجزئ عن الرجل وأهل بيته، والبدنة والبقرة عن سبعة.", "مسلم"),
                G("عند الذبح: بسم الله والله أكبر", "متفق عليه"),
                G("ويستحب: اللهم هذا منك ولك", "أبو داود"),
                G("يأكل منها ويهدي ويتصدق، ولا يعطي الجزار أجرته منها.", "متفق عليه"),
            )),
        ), listOf("dhul", "hajj")),

        Guide("hajj", "الحج والعمرة خطوة بخطوة", "🕋", "ملخص الأعمال بالترتيب، للتذكير قبل الرحلة وأثناءها.", listOf(
            Sec("التلبية", listOf(G(TALBIYA, "متفق عليه"))),
            Sec("العمرة", listOf(
                G("1. الإحرام من الميقات: الاغتسال والتطيب في البدن، ولبس الإزار والرداء للرجل، ثم نية العمرة: لبيك عمرة، والتلبية حتى البدء بالطواف.", ""),
                G("2. الطواف: سبعة أشواط حول الكعبة تبدأ من الحجر الأسود وتنتهي به، والكعبة عن يسارك.", ""),
                G("بين الركن اليماني والحجر الأسود: ﴿ربنا آتنا في الدنيا حسنة وفي الآخرة حسنة وقنا عذاب النار﴾", "أبو داود"),
                G("3. ركعتان خلف مقام إبراهيم إن تيسر، يقرأ فيهما الكافرون والإخلاص.", "مسلم"),
                G("4. السعي: سبعة أشواط من الصفا إلى المروة، يبدأ بالصفا ويقرأ: ﴿إن الصفا والمروة من شعائر الله﴾ ويقول: أبدأ بما بدأ الله به", "مسلم"),
                G("5. الحلق أو التقصير للرجل، والمرأة تقصر قدر أنملة، وبه تتم العمرة.", ""),
            )),
            Sec("أيام الحج", listOf(
                G("يوم التروية (8 ذي الحجة): الإحرام بالحج والتوجه إلى منى، وصلاة الظهر والعصر والمغرب والعشاء والفجر فيها، تُقصر الرباعية بلا جمع.", ""),
                G("يوم عرفة (9): الوقوف بعرفة بعد الزوال إلى غروب الشمس، وهو ركن الحج الأعظم: الحج عرفة", "أصحاب السنن، وصححه"),
                G("ليلة المزدلفة: المبيت بها وصلاة المغرب والعشاء جمعا، والذكر عند المشعر الحرام بعد الفجر.", ""),
                G("يوم النحر (10): رمي جمرة العقبة بسبع حصيات، ثم الهدي للمتمتع والقارن، ثم الحلق أو التقصير، ثم طواف الإفاضة والسعي.", ""),
                G("أيام التشريق (11–13): المبيت بمنى، ورمي الجمرات الثلاث كل يوم بعد الزوال: الصغرى ثم الوسطى ثم العقبة، بسبع حصيات لكل منها. ومن تعجل في يومين فلا إثم عليه.", "البقرة: 203"),
                G("طواف الوداع عند الخروج من مكة، ويسقط عن الحائض والنفساء.", "متفق عليه"),
            )),
            Sec("أركان الحج", listOf(
                G("الإحرام، والوقوف بعرفة، وطواف الإفاضة، والسعي بين الصفا والمروة.", ""),
                G("من حج فلم يرفث ولم يفسق رجع كيوم ولدته أمه", "متفق عليه"),
            )),
        ), listOf("dhul", "arafah", "adha")),

        Guide("muharram", "شهر الله المحرّم وعاشوراء", "📅", "أول السنة الهجرية، وأحد الأشهر الحرم.", listOf(
            Sec("فضله", listOf(
                G("أفضل الصيام بعد رمضان شهر الله المحرم", "مسلم"),
                G("﴿إن عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والأرض منها أربعة حرم﴾", "التوبة: 36"),
            )),
            Sec("عاشوراء", listOf(
                G("صيام يوم عاشوراء أحتسب على الله أن يكفر السنة التي قبله", "مسلم"),
                G("لئن بقيت إلى قابل لأصومن التاسع", "مسلم؛ فيُستحب صيام التاسع مع العاشر"),
                G("صامه النبي صلى الله عليه وسلم لأنه اليوم الذي نجى الله فيه موسى وقومه من فرعون.", "متفق عليه"),
            )),
            Sec("تنبيه", listOf(
                G("لم يثبت عن النبي صلى الله عليه وسلم دعاء أو ذكر خاص بأول العام الهجري؛ ويكفي حمد الله وسؤاله الخير فيما يستقبل.", ""),
            )),
        )),

        Guide("white", "الأيام البيض", "🌕", "الثالث عشر والرابع عشر والخامس عشر من كل شهر هجري.", listOf(
            Sec("فضلها", listOf(
                G("إذا صمت من الشهر ثلاثا فصم ثلاث عشرة، وأربع عشرة، وخمس عشرة", "الترمذي والنسائي، وحسّنه"),
                G("صوم ثلاثة أيام من كل شهر صوم الدهر كله", "متفق عليه"),
                G("ويُستثنى منها في ذي الحجة اليوم الثالث عشر لأنه من أيام التشريق.", ""),
            )),
        )),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** The guide for a calendar occasion. */
    fun forOccasion(name: String): String? = when {
        "رمضان" in name -> "ramadan"
        "العشر الأواخر" in name || "القدر" in name -> "lastten"
        "الفطر" in name -> "fitr"
        "عشر ذي الحجة" in name -> "dhul"
        "عرفة" in name -> "arafah"
        "الأضحى" in name -> "adha"
        "عاشوراء" in name || "رأس السنة" in name -> "muharram"
        else -> null
    }

    const val DISCLAIMER = NOTE
}

/** All the guides. */
@Composable
fun SeasonsHome(open: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Hint("seasons", "🌙", "لكل موسم: فضله، وأذكاره وأدعيته الثابتة، وأحكامه باختصار مع المصدر. تفتحها أيضًا بالضغط على المناسبة في التقويم.") }
        items(Seasons.all) { g ->
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { open(g.id) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(g.icon, tint = P.accent, size = 28.dp, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.title, color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Sub(g.about, size = 12.sp)
                }
            }
        }
    }
}

/** One guide: its sections, each item with its source; a long text copies on tap. */
@Composable
fun SeasonPage(id: String, open: (String) -> Unit) {
    val g = Seasons.byId(id) ?: return
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Panel(color = P.card, pad = 14.dp) {
                GText("${g.icon} ${g.title}", color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Sub(g.about, size = 13.sp)
            }
        }
        if (id == "ramadan") item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile("📜", "ختمة: ٢٠ صفحة يوميًا", Modifier.weight(1f)) {
                    Reading.goal = 20; val (s, a) = Quran.last ?: (1 to 1); Nav.reading = s to a
                }
                Tile("🟢", "السبحة", Modifier.weight(1f)) { Nav.adhkar = 2; Nav.go("adhkar") }
            }
        }
        g.sections.forEach { sec ->
            item { Title(sec.title, Modifier.padding(top = 8.dp)) }
            items(sec.items) { it ->
                Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable {
                    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("ALzikr", it.text + if (it.src.isNotEmpty()) "\n[${it.src}]" else ""))
                    Toast.makeText(ctx, "نُسخ النص", Toast.LENGTH_SHORT).show()
                }.padding(14.dp)) {
                    Text(it.text, color = P.ink, fontSize = (16 * Data.textScale).sp, lineHeight = (28 * Data.textScale).sp, fontFamily = Fonts.naskh)
                    if (it.src.isNotEmpty()) Text(it.src, color = P.gold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (g.related.isNotEmpty()) {
            item { Title("مواسم مرتبطة", Modifier.padding(top = 8.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    g.related.mapNotNull { Seasons.byId(it) }.forEach { r -> Tile(r.icon, r.title.substringBefore(" ").take(14), Modifier.weight(1f)) { open(r.id) } }
                }
            }
        }
        item { Sub(Seasons.DISCLAIMER + " اضغط على أي نص لنسخه.", Modifier.padding(top = 8.dp), size = 11.sp) }
    }
}
