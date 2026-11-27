package com.alzikr.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * «التسلسل التاريخي»: the prophets in their order (no dates — the Quran gives none), then the life of the Prophet ﷺ
 * and the great events of Islamic history to 1900. A day is given only where the date is well known;
 * «في مثل هذا اليوم» uses those days (Hijri for Islamic events, Gregorian for the others).
 */
object Timeline {
    /** [hm]/[hd]: Hijri month/day; [gm]/[gd]: Gregorian month/day — only when the day is well known. */
    data class Ev(val era: String, val when_: String, val title: String, val text: String, val ref: String = "",
                  val hm: Int = 0, val hd: Int = 0, val gm: Int = 0, val gd: Int = 0)

    val eras = listOf("prophets" to "الأنبياء", "sira" to "السيرة النبوية", "rashidun" to "الخلافة الراشدة", "umayyad" to "الأموية",
        "abbasid" to "العباسية والعلوم", "crusades" to "الصليبيون والمغول", "andalus" to "الأندلس", "ottoman" to "العثمانيون")

    val all: List<Ev> = listOf(
        // ---------------------------------------------------------------- the prophets, in their order
        Ev("prophets", "أبو البشر", "آدم عليه السلام", "خلقه الله بيده ونفخ فيه من روحه، وأسجد له الملائكة، وعلّمه الأسماء كلها، وتاب عليه بعد أن أكل من الشجرة.", "البقرة 30–37"),
        Ev("prophets", "بعد آدم", "إدريس عليه السلام", "«وَاذْكُرْ فِي الْكِتَابِ إِدْرِيسَ إِنَّهُ كَانَ صِدِّيقًا نَبِيًّا • وَرَفَعْنَاهُ مَكَانًا عَلِيًّا».", "مريم 56–57"),
        Ev("prophets", "أول الرسل", "نوح عليه السلام", "أول رسول إلى أهل الأرض، دعا قومه ألف سنة إلا خمسين عامًا، وأنجاه الله ومن آمن معه في السفينة من الطوفان.", "العنكبوت 14، هود 25–48"),
        Ev("prophets", "إلى عاد", "هود عليه السلام", "أُرسل إلى قوم عاد في الأحقاف، فكذّبوه، فأهلكهم الله بريح صرصر عاتية.", "هود 50–60، الحاقة 6–7"),
        Ev("prophets", "إلى ثمود", "صالح عليه السلام", "أُرسل إلى ثمود الذين نحتوا الجبال بيوتًا، وجعل الله لهم الناقة آية، فعقروها فأخذتهم الصيحة.", "هود 61–68"),
        Ev("prophets", "خليل الرحمن", "إبراهيم عليه السلام", "حطّم الأصنام فنجّاه الله من النار، وبنى الكعبة مع ابنه إسماعيل، وأذّن في الناس بالحج، وهو أبو الأنبياء.", "الأنبياء 51–70، البقرة 127"),
        Ev("prophets", "إلى سدوم", "لوط عليه السلام", "آمن بإبراهيم وهاجر معه، وأُرسل إلى قوم يأتون الفاحشة، فأنجاه الله وأهله إلا امرأته.", "هود 77–83"),
        Ev("prophets", "الذبيح", "إسماعيل عليه السلام", "ابن إبراهيم، كان صادق الوعد، صبر حين أُمر أبوه بذبحه ففداه الله بذبح عظيم، ومن ذريته محمد ﷺ.", "مريم 54–55، الصافات 101–107"),
        Ev("prophets", "ابن إبراهيم", "إسحاق عليه السلام", "بشّرت الملائكةُ إبراهيمَ وسارة به على الكبر، ومن ورائه يعقوب.", "هود 71–72، الصافات 112"),
        Ev("prophets", "إسرائيل", "يعقوب عليه السلام", "ابن إسحاق وأبو الأسباط، وصّى بنيه: «فَلَا تَمُوتُنَّ إِلَّا وَأَنتُم مُّسْلِمُونَ».", "البقرة 132–133"),
        Ev("prophets", "أحسن القصص", "يوسف عليه السلام", "ألقاه إخوته في الجبّ، ثم صبر في السجن، فمكّن الله له في الأرض وجمعه بأبيه وإخوته.", "سورة يوسف"),
        Ev("prophets", "الصابر", "أيوب عليه السلام", "ابتلاه الله في جسده وأهله وماله فصبر، ونادى ربه: «أَنِّي مَسَّنِيَ الضُّرُّ وَأَنتَ أَرْحَمُ الرَّاحِمِينَ»، فكشف الله ضرّه.", "الأنبياء 83–84"),
        Ev("prophets", "إلى مدين", "شعيب عليه السلام", "دعا أهل مدين إلى التوحيد والوفاء بالكيل والميزان وترك بخس الناس أشياءهم.", "هود 84–95"),
        Ev("prophets", "كليم الله", "موسى عليه السلام", "أُرسل إلى فرعون بالآيات، وفلق الله له البحر فنجّاه وأغرق فرعون، وكلّمه ربه وأنزل عليه التوراة.", "طه، القصص"),
        Ev("prophets", "وزير موسى", "هارون عليه السلام", "أخو موسى، دعا موسى ربه أن يجعله وزيرًا له يشدّ به أزره.", "طه 29–36"),
        Ev("prophets", "من الصابرين", "ذو الكفل عليه السلام", "«وَإِسْمَاعِيلَ وَإِدْرِيسَ وَذَا الْكِفْلِ ۖ كُلٌّ مِّنَ الصَّابِرِينَ».", "الأنبياء 85"),
        Ev("prophets", "الملك النبي", "داود عليه السلام", "قتل جالوت، وآتاه الله الملك والحكمة والزبور، وألان له الحديد، وكانت الجبال تسبّح معه.", "البقرة 251، سبأ 10"),
        Ev("prophets", "ملك لا ينبغي لأحد", "سليمان عليه السلام", "ابن داود، سخّر الله له الريح والجن، وعلّمه منطق الطير، وجاءته ملكة سبأ فأسلمت.", "النمل 15–44"),
        Ev("prophets", "إلى بعلبك", "إلياس عليه السلام", "دعا قومه إلى ترك عبادة الصنم «بعل» وعبادة الله أحسن الخالقين.", "الصافات 123–132"),
        Ev("prophets", "من الأخيار", "اليسع عليه السلام", "ذكره الله مع إسماعيل ويونس ولوط: «وَكُلًّا فَضَّلْنَا عَلَى الْعَالَمِينَ».", "الأنعام 86، ص 48"),
        Ev("prophets", "ذو النون", "يونس عليه السلام", "التقمه الحوت فنادى في الظلمات: «لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ»، فنجّاه الله.", "الأنبياء 87–88"),
        Ev("prophets", "كافل مريم", "زكريا عليه السلام", "كفل مريم، ودعا ربه خفيًّا فوهب له يحيى على الكبر.", "مريم 2–11، آل عمران 37–41"),
        Ev("prophets", "الحَصور", "يحيى عليه السلام", "ابن زكريا، آتاه الله الحكم صبيًّا، وكان برًّا بوالديه.", "مريم 12–15"),
        Ev("prophets", "كلمة الله", "عيسى ابن مريم عليه السلام", "وُلد من غير أب، وكلّم الناس في المهد، وآتاه الله الإنجيل والمعجزات بإذنه، ورفعه الله إليه وما قتلوه وما صلبوه.", "مريم 16–36، النساء 157–158"),
        Ev("prophets", "خاتم النبيين", "محمد ﷺ", "رسول الله إلى الناس كافة، وخاتم الأنبياء والمرسلين، أنزل الله عليه القرآن. وتفصيل سيرته في «السيرة النبوية».", "الأحزاب 40"),

        // ---------------------------------------------------------------- the life of the Prophet ﷺ
        Ev("sira", "عام الفيل · نحو 571م", "مولد النبي ﷺ بمكة", "وُلد ﷺ في ربيع الأول عام الفيل، يتيم الأب، وكفله جده عبد المطلب ثم عمه أبو طالب."),
        Ev("sira", "نحو 610م", "البعثة ونزول الوحي", "نزل عليه جبريل في غار حراء في رمضان بأول ما نزل: «اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ»، وهو ابن أربعين سنة.", "العلق 1–5"),
        Ev("sira", "نحو 615م", "الهجرة إلى الحبشة", "هاجر نفر من المسلمين إلى النجاشي فرارًا بدينهم من أذى قريش."),
        Ev("sira", "نحو 619م", "عام الحزن", "توفيت خديجة رضي الله عنها وتوفي أبو طالب في عام واحد."),
        Ev("sira", "1هـ · 622م", "الهجرة إلى المدينة", "هاجر ﷺ مع أبي بكر إلى المدينة، فبنى المسجد وآخى بين المهاجرين والأنصار. ومن الهجرة بدأ التقويم الهجري."),
        Ev("sira", "17 رمضان 2هـ · 624م", "غزوة بدر الكبرى", "يوم الفرقان: نصر الله المسلمين وهم قلة على قريش.", "الأنفال", hm = 9, hd = 17),
        Ev("sira", "شوال 3هـ · 625م", "غزوة أحد", "استُشهد فيها حمزة رضي الله عنه وسبعون من الصحابة، ونزلت فيها آيات آل عمران.", "آل عمران 121–175"),
        Ev("sira", "شوال 5هـ · 627م", "غزوة الخندق (الأحزاب)", "حفر المسلمون الخندق بمشورة سلمان، وردّ الله الأحزاب بريح وجنود لم يروها.", "الأحزاب 9–27"),
        Ev("sira", "ذو القعدة 6هـ · 628م", "صلح الحديبية", "صلح بين المسلمين وقريش سمّاه الله فتحًا مبينًا، وفيه بيعة الرضوان.", "الفتح 1، 18"),
        Ev("sira", "7هـ · 628م", "فتح خيبر", "فتح المسلمون حصون خيبر، وفيها قال ﷺ: «لأعطينّ الراية غدًا رجلًا يحب الله ورسوله»."),
        Ev("sira", "20 رمضان 8هـ · 630م", "فتح مكة", "دخل ﷺ مكة فاتحًا، فطهّر الكعبة من الأصنام وعفا عن أهلها.", "النصر", hm = 9, hd = 20),
        Ev("sira", "شوال 8هـ · 630م", "غزوة حنين", "أعجبت المسلمين كثرتهم ثم أنزل الله سكينته ونصرهم.", "التوبة 25–26"),
        Ev("sira", "رجب 9هـ · 630م", "غزوة تبوك", "غزوة العسرة إلى تخوم الروم في حرّ شديد، ولم يقع فيها قتال."),
        Ev("sira", "9 ذو الحجة 10هـ · 632م", "حجة الوداع ويوم عرفة", "خطب ﷺ في عرفة خطبته الجامعة، ونزل فيه: «الْيَوْمَ أَكْمَلْتُ لَكُمْ دِينَكُمْ».", "المائدة 3", hm = 12, hd = 9),
        Ev("sira", "ربيع الأول 11هـ · 632م", "وفاة النبي ﷺ", "توفي ﷺ في المدينة يوم الاثنين، على المشهور في الثاني عشر من ربيع الأول، ودُفن في حجرة عائشة رضي الله عنها.", hm = 3, hd = 12),

        // ---------------------------------------------------------------- the rightly guided caliphs
        Ev("rashidun", "11هـ · 632م", "خلافة أبي بكر الصديق", "قاتل المرتدين ومانعي الزكاة، وجُمع القرآن في عهده في صحف بعد استشهاد كثير من القراء في اليمامة."),
        Ev("rashidun", "رجب 15هـ · 636م", "معركة اليرموك", "انتصر المسلمون بقيادة خالد بن الوليد على الروم، فانفتحت الشام."),
        Ev("rashidun", "نحو 15هـ · 636م", "معركة القادسية", "انتصر المسلمون بقيادة سعد بن أبي وقاص على الفرس."),
        Ev("rashidun", "نحو 16هـ · 637م", "عمر يتسلّم بيت المقدس", "قدم عمر بن الخطاب رضي الله عنه وتسلّم مفاتيح القدس، وكتب لأهلها الأمان."),
        Ev("rashidun", "17هـ · 638م", "اعتماد التقويم الهجري", "جعل عمر رضي الله عنه هجرة النبي ﷺ بداية التأريخ، والمحرّم أول الشهور."),
        Ev("rashidun", "23هـ · 644م", "خلافة عثمان بن عفان", "جمع الناس على مصحف واحد ونسخه إلى الأمصار، فبقي القرآن كما هو إلى اليوم."),
        Ev("rashidun", "35هـ · 656م", "خلافة علي بن أبي طالب", "رابع الخلفاء الراشدين، تولّى الخلافة في زمن فتنة."),
        Ev("rashidun", "41هـ · 661م", "عام الجماعة", "تنازل الحسن بن علي لمعاوية حقنًا للدماء، فاجتمعت الكلمة، وبدأت الدولة الأموية."),

        // ---------------------------------------------------------------- the Umayyads
        Ev("umayyad", "92هـ · 711م", "فتح الأندلس", "عبر طارق بن زياد إلى الأندلس، فبدأ فيها حكم إسلامي دام نحو ثمانية قرون."),
        Ev("umayyad", "99–101هـ · 717–720م", "خلافة عمر بن عبد العزيز", "الخليفة العادل الذي ملأ الأرض عدلًا في سنتين ونصف، وأمر بتدوين السنة."),
        Ev("umayyad", "114هـ · 732م", "معركة بلاط الشهداء", "توقف عندها تقدم المسلمين في بلاد الفرنجة قرب تور."),
        Ev("umayyad", "132هـ · 750م", "قيام الدولة العباسية", "سقطت الدولة الأموية في المشرق وقامت العباسية."),

        // ---------------------------------------------------------------- the Abbasids and the sciences
        Ev("abbasid", "145هـ · 762م", "بناء بغداد", "بنى أبو جعفر المنصور مدينة السلام، فصارت حاضرة العلم في العالم."),
        Ev("abbasid", "نحو 830م", "بيت الحكمة", "ازدهرت الترجمة والعلوم في عهد المأمون، وكتب الخوارزمي كتابه في الجبر."),
        Ev("abbasid", "256هـ · 870م", "وفاة الإمام البخاري", "صاحب «الجامع الصحيح» أصح كتاب بعد كتاب الله."),
        Ev("abbasid", "361هـ · 972م", "تأسيس الأزهر", "بُني الجامع الأزهر بالقاهرة، وصار من أقدم الجامعات في العالم."),
        Ev("abbasid", "نحو 1025م", "ابن سينا والقانون في الطب", "كتابٌ دُرِّس في جامعات أوروبا قرونًا."),
        Ev("abbasid", "463هـ · 26 أغسطس 1071م", "معركة ملاذكرد", "انتصر السلاجقة بقيادة ألب أرسلان على الروم، فانفتحت الأناضول.", gm = 8, gd = 26),

        // ---------------------------------------------------------------- Crusaders and Mongols
        Ev("crusades", "492هـ · 15 يوليو 1099م", "سقوط القدس بيد الصليبيين", "احتلت الحملة الصليبية الأولى بيت المقدس.", gm = 7, gd = 15),
        Ev("crusades", "24 ربيع الآخر 583هـ · 4 يوليو 1187م", "معركة حطين", "انتصر صلاح الدين الأيوبي على الصليبيين انتصارًا حاسمًا.", hm = 4, hd = 24, gm = 7, gd = 4),
        Ev("crusades", "27 رجب 583هـ · 2 أكتوبر 1187م", "تحرير بيت المقدس", "دخل صلاح الدين القدس بعد ثمانٍ وثمانين سنة، وعامل أهلها بالرحمة.", hm = 7, hd = 27, gm = 10, gd = 2),
        Ev("crusades", "656هـ · 10 فبراير 1258م", "سقوط بغداد بيد المغول", "دمّر هولاكو بغداد وقتل الخليفة، وأُلقيت الكتب في دجلة.", gm = 2, gd = 10),
        Ev("crusades", "25 رمضان 658هـ · 3 سبتمبر 1260م", "معركة عين جالوت", "أوقف المماليك بقيادة قطز وبيبرس زحف المغول في فلسطين.", hm = 9, hd = 25, gm = 9, gd = 3),
        Ev("crusades", "690هـ · 18 مايو 1291م", "تحرير عكا", "أنهى المماليك آخر وجود صليبي كبير في الشام.", gm = 5, gd = 18),

        // ---------------------------------------------------------------- al-Andalus
        Ev("andalus", "138هـ · 756م", "عبد الرحمن الداخل", "أقام الدولة الأموية في الأندلس، فصارت قرطبة منارة للعلم."),
        Ev("andalus", "479هـ · 23 أكتوبر 1086م", "معركة الزلاقة", "انتصر المرابطون بقيادة يوسف بن تاشفين ومعهم أهل الأندلس على قشتالة.", gm = 10, gd = 23),
        Ev("andalus", "897هـ · 2 يناير 1492م", "سقوط غرناطة", "سلّم أبو عبد الله الصغير آخر ممالك المسلمين في الأندلس.", gm = 1, gd = 2),

        // ---------------------------------------------------------------- the Ottomans
        Ev("ottoman", "نحو 699هـ · 1299م", "قيام الدولة العثمانية", "أسسها عثمان الأول في الأناضول، ودامت أكثر من ستة قرون."),
        Ev("ottoman", "725هـ · 1325م", "رحلة ابن بطوطة", "خرج من طنجة في رحلة دامت نحو ثلاثين سنة جاب فيها العالم المعروف."),
        Ev("ottoman", "20 جمادى الأولى 857هـ · 29 مايو 1453م", "فتح القسطنطينية", "فتحها السلطان محمد الفاتح فصارت إسطنبول عاصمة الدولة العثمانية.", hm = 5, hd = 20, gm = 5, gd = 29),
        Ev("ottoman", "932هـ · 29 أغسطس 1526م", "معركة موهاكس", "انتصر العثمانيون بقيادة سليمان القانوني على المجر.", gm = 8, gd = 29),
        Ev("ottoman", "926–974هـ · 1520–1566م", "عهد سليمان القانوني", "بلغت الدولة العثمانية في عهده أوج قوتها واتساعها."),
        Ev("ottoman", "1094هـ · 12 سبتمبر 1683م", "معركة فيينا", "انتهى الحصار العثماني الثاني لفيينا بهزيمة، وبدأ تراجع الدولة في أوروبا.", gm = 9, gd = 12),
        Ev("ottoman", "1213هـ · 1 يوليو 1798م", "الحملة الفرنسية على مصر", "نزل نابليون بالإسكندرية واحتل مصر ثلاث سنوات حتى خرجت الحملة.", gm = 7, gd = 1),
        Ev("ottoman", "1220هـ · 1805م", "محمد علي يحكم مصر", "تولّى حكم مصر وبدأ نهضة في التعليم والصناعة والجيش."),
        Ev("ottoman", "1246هـ · 5 يوليو 1830م", "احتلال فرنسا للجزائر", "بدأ احتلال دام 132 عامًا حتى الاستقلال.", gm = 7, gd = 5),
        Ev("ottoman", "1255هـ · 3 نوفمبر 1839م", "التنظيمات العثمانية", "إصلاحات في الإدارة والقضاء والجيش.", gm = 11, gd = 3),
        Ev("ottoman", "1299هـ · 13 سبتمبر 1882م", "الاحتلال البريطاني لمصر", "بعد معركة التل الكبير.", gm = 9, gd = 13),
        Ev("ottoman", "1318هـ · 1 سبتمبر 1900م", "بدء سكة حديد الحجاز", "بدأ العمل في خط يربط دمشق بالمدينة المنورة لخدمة الحجاج.", gm = 9, gd = 1),

    )

    /** What happened on this day: Islamic events only, by Hijri day or by Gregorian day where that is the known date. */
    fun onThisDay(date: LocalDate = LocalDate.now()): List<Ev> {
        val h = Data.hijri(date)
        return all.filter { (it.hm == h.month && it.hd == h.day) || (it.gm == date.monthValue && it.gd == date.dayOfMonth) }
    }
    /** Nothing on this very day: what happened this month (Hijri or Gregorian). */
    fun thisMonth(date: LocalDate = LocalDate.now()): List<Ev> {
        val h = Data.hijri(date)
        return all.filter { (it.hm == h.month && it.hd > 0) || (it.gm == date.monthValue && it.gd > 0) }
    }
}

/** «التسلسل التاريخي»: an interactive line through time; each event shows its details when asked. */
@Composable
fun TimelinePage() {
    var era by rememberSaveable { mutableStateOf("prophets") }
    val opened = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var today by rememberSaveable { mutableStateOf(false) }
    var quiz by remember { mutableStateOf<Pair<Timeline.Ev, List<String>>?>(null) }
    var answer by remember { mutableStateOf<String?>(null) }
    fun newQuiz() {
        val pool = Timeline.all.filter { it.era != "prophets" }
        val e = pool.random(); val others = pool.filter { it.when_ != e.when_ }.shuffled().take(2).map { it.when_ }
        quiz = e to (others + e.when_).shuffled(); answer = null
    }
    val list = Timeline.all.filter { it.era == era }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // On this day: shown when asked.
        item {
            Column(Modifier.fillMaxWidth().clip(L.shape(18.dp)).background(Brush.linearGradient(listOf(P.accent.copy(alpha = .22f), P.gold.copy(alpha = .14f)))).padding(14.dp)) {
                Text("📅 ماذا حدث في مثل هذا اليوم؟", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (!today) Action("اكشف", Modifier.padding(top = 8.dp)) { today = true }
                androidx.compose.animation.AnimatedVisibility(today, enter = fadeIn() + expandVertically()) {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val day = Timeline.onThisDay()
                        if (day.isNotEmpty()) day.forEach { e -> EventLine(e) }
                        else {
                            Sub("لا حدث مشهور في مثل هذا اليوم بعينه. ومن أحداث هذا الشهر:", size = 13.sp)
                            Timeline.thisMonth().take(4).forEach { e -> EventLine(e) }
                        }
                    }
                }
            }
        }
        // A quick question.
        item {
            Column(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(P.surface).border(1.dp, P.line, L.shape(16.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯 اختبر نفسك", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(if (quiz == null) "ابدأ" else "سؤال آخر", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { newQuiz() }.padding(6.dp))
                }
                quiz?.let { (e, opts) ->
                    Text("متى كان: «${e.title}»؟", color = P.ink, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                    opts.forEach { o ->
                        val right = o == e.when_; val picked = answer == o
                        Text((if (answer != null && right) "✓ " else if (picked) "✗ " else "") + o, color = when { answer != null && right -> P.accent; picked -> P.danger; else -> P.ink }, fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clip(L.shape(10.dp)).background(P.card.seeThrough()).clickable(enabled = answer == null) { answer = o }.padding(10.dp))
                    }
                    if (answer != null) Sub(e.text, Modifier.padding(top = 6.dp), size = 13.sp)
                }
            }
        }
        item { Wrap(gap = 6.dp) { Timeline.eras.forEach { (id, name) -> Choice(name, era == id) { era = id } } } }
        item { Sub(if (era == "prophets") "بترتيبهم كما ذكرهم العلماء، دون تواريخ (لم يذكرها القرآن). اضغط أي نبي لتعرف عنه." else "اضغط أي حدث لتعرف تفاصيله.", size = 12.sp) }
        items(list, key = { it.era + it.title }) { e ->
            val open = (e.era + e.title) in opened
            Row(Modifier.fillMaxWidth()) {
                // The line through time with a dot for each event.
                Column(Modifier.width(22.dp).padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(if (open) P.accent else P.gold))
                    Box(Modifier.width(2.dp).height(if (open) 110.dp else 56.dp).background(P.line))
                }
                Column(Modifier.weight(1f).clip(L.shape(14.dp)).background(if (open) P.accent.copy(alpha = .10f) else P.surface).border(1.dp, if (open) P.accent.copy(alpha = .4f) else P.line, L.shape(14.dp))
                    .clickable { if (open) opened.remove(e.era + e.title) else opened.add(e.era + e.title) }.padding(12.dp)) {
                    Text(e.when_, color = P.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(e.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Fonts.naskh)
                    AnimatedVisibility(open, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Column(Modifier.padding(top = 6.dp)) {
                            Text(e.text, color = P.ink, fontSize = 14.sp, lineHeight = 23.sp, fontFamily = Fonts.naskh)
                            if (e.ref.isNotEmpty()) Sub("📖 ${e.ref}", Modifier.padding(top = 4.dp), size = 12.sp)
                        }
                    }
                    if (!open) Text("اضغط لتعرف ›", color = P.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun EventLine(e: Timeline.Ev) {
    Column(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.surface).padding(10.dp)) {
        Text(e.when_, color = P.gold, fontSize = 12.sp)
        Text(e.title, color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(e.text, color = P.ink, fontSize = 13.sp, lineHeight = 21.sp)
    }
}

@Suppress("unused") private val keepAlign = TextAlign.Center
@Suppress("unused") private fun keepH() = Modifier.fillMaxHeight()
