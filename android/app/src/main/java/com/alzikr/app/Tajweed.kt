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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * «تعلّم التجويد»: the rules of reading for Hafs from Asim by way of ash-Shatibiyyah, as taught from
 * Tuhfat al-Atfal and al-Jazariyyah, each with verses from the mushaf to hear and open, and a test per unit.
 * Examples are written in plain letters and looked up in the verse; the mushaf's own spelling is shown.
 */
object Tajweed {
    /** A word or words in verse [s]:[a]. */
    data class Ex(val word: String, val s: Int, val a: Int, val note: String = "")
    data class Lesson(val id: String, val title: String, val text: String, val letters: String = "", val examples: List<Ex> = emptyList(), val src: String = "")
    data class Unit(val id: String, val icon: String, val title: String, val intro: String, val lessons: List<Lesson>, val quiz: Boolean = false)

    val units = listOf(
        Unit("intro", "🎓", "مدخل إلى التجويد", "ما التجويد، وما حكمه، وكيف تتعلمه.", listOf(
            Lesson("def", "تعريفه", "التجويد لغةً: التحسين. واصطلاحًا: إخراج كل حرف من مخرجه، وإعطاؤه حقّه من الصفات اللازمة له (كالهمس والجهر)، ومستحقّه من الأحكام التي تنشأ عن تلك الصفات (كالتفخيم والترقيق والإدغام والمد)."),
            Lesson("rule", "حكمه", "تعلّم أحكامه نظريًّا فرض كفاية، والعمل به عند القراءة واجب على كل من يقرأ القرآن بقدر استطاعته؛ لأن القرآن نزل به ونُقل إلينا به متواترًا.\n\nقال ابن الجزري:\nوالأخذُ بالتجويد حتمٌ لازمُ — من لم يجوّد القرآنَ آثمُ\nلأنه به الإلهُ أنزلا — وهكذا منه إلينا وصلا",
                src = "متن الجزرية"),
            Lesson("virtue", "فضله", "قال تعالى: ﴿ورتّل القرآن ترتيلًا﴾ [المزمل: 4]، ويُروى عن علي رضي الله عنه (ذكره ابن الجزري في النشر، ولا يُعرف له إسناد ثابت): الترتيل تجويد الحروف ومعرفة الوقوف.\nوقال ﷺ: «الماهر بالقرآن مع السفرة الكرام البررة، والذي يقرأ القرآن ويتتعتع فيه وهو عليه شاق له أجران».",
                examples = listOf(Ex("ورتل القرآن ترتيلا", 73, 4)), src = "البخاري ومسلم"),
            Lesson("lahn", "اللحن", "اللحن هو الخطأ في القراءة، وهو نوعان:\n• اللحن الجلي: خطأ ظاهر يغيّر الحرف أو الحركة، كقراءة «الحمدُ» بفتح الدال، أو إبدال حرف بحرف. وهو حرام بالإجماع.\n• اللحن الخفي: خطأ لا يعرفه إلا المتقنون، كترك الغنّة أو الإخفاء أو تقصير المد. وهو مكروه وقيل محرّم على من قدر على تعلّمه."),
            Lesson("levels", "مراتب القراءة", "• التحقيق: قراءة بطيئة جدًّا مع الطمأنينة، للتعليم.\n• الترتيل: قراءة بتمهّل وطمأنينة مع تدبّر المعاني، وهي أفضلها.\n• التدوير: قراءة متوسطة بين الترتيل والحدر.\n• الحدر: قراءة سريعة مع المحافظة على الأحكام.\nوكلها جائزة ما دامت الأحكام محفوظة."),
            Lesson("how", "كيف تتعلّم", "التجويد يؤخذ بالتلقي والمشافهة: اسمع من قارئ متقن وقلّده، واعرض قراءتك على معلّم. هذه الدروس تعرّفك بالقواعد، والأمثلة كلها من المصحف: اضغط على المثال لتسمعه، واختر «الحصري المعلّم» أو «المنشاوي المعلّم» قارئًا فهما يقرآن الآية ببطء للتعليم."),
        )),
        Unit("isti", "🛡", "الاستعاذة والبسملة", "ما يقوله القارئ قبل أن يبدأ، وكيف يصل بين السور.", listOf(
            Lesson("isti_rule", "الاستعاذة", "صيغتها المشهورة: «أعوذ بالله من الشيطان الرجيم». وهي مطلوبة عند ابتداء القراءة: ﴿فإذا قرأت القرآن فاستعذ بالله من الشيطان الرجيم﴾. ويجهر بها إذا قرأ جهرًا وبين الناس، ويسرّ بها إذا قرأ سرًّا أو في الصلاة.",
                examples = listOf(Ex("فإذا قرأت القرآن فاستعذ بالله", 16, 98))),
            Lesson("basmala", "البسملة", "يُبسمل القارئ في أول كل سورة إلا سورة التوبة (براءة)؛ فليس في أولها بسملة. وفي أثناء السورة يجوز له أن يبسمل وأن يترك."),
            Lesson("start4", "أوجه البدء بأول السورة", "إذا جمع بين الاستعاذة والبسملة وأول السورة فله أربعة أوجه كلها جائزة:\n1. قطع الجميع: يقف على الاستعاذة، ثم على البسملة، ثم يبدأ السورة.\n2. قطع الأول ووصل الثاني بالثالث.\n3. وصل الأول بالثاني وقطع الثالث.\n4. وصل الجميع بنَفَس واحد."),
            Lesson("between", "بين السورتين", "إذا وصل آخر سورة بأول التي بعدها فله ثلاثة أوجه جائزة:\n1. قطع الجميع.\n2. قطع آخر السورة، ثم وصل البسملة بأول التالية.\n3. وصل الجميع.\nويمتنع وجه رابع: وصل آخر السورة بالبسملة ثم الوقف عليها؛ لئلا يُتوهّم أن البسملة من آخر السورة.\nوبين الأنفال والتوبة ثلاثة أوجه بلا بسملة: الوقف، أو السكت، أو الوصل."),
        )),
        Unit("makharij", "👄", "مخارج الحروف", "المخرج هو موضع خروج الحرف. ولمعرفة مخرج حرف: سكّنه وأدخل عليه همزة متحركة (أَبْ، أَتْ) وانظر أين ينقطع الصوت.", listOf(
            Lesson("jawf", "الجوف", "الفراغ الممتد داخل الحلق والفم، وتخرج منه حروف المد الثلاثة:\n• الألف الساكنة المفتوح ما قبلها.\n• الواو الساكنة المضموم ما قبلها.\n• الياء الساكنة المكسور ما قبلها.\nويجمعها مثال: «نوحيها».", letters = "ا و ي", examples = listOf(Ex("نوحيها", 11, 49))),
            Lesson("halq", "الحلق", "فيه ثلاثة مخارج لستة حروف:\n• أقصى الحلق (أبعده عن الفم): الهمزة والهاء.\n• وسط الحلق: العين والحاء.\n• أدنى الحلق (أقربه إلى الفم): الغين والخاء.", letters = "ء هـ  ع ح  غ خ"),
            Lesson("lisan", "اللسان", "فيه عشرة مخارج لثمانية عشر حرفًا:\n• أقصى اللسان مع ما فوقه من الحنك الأعلى: القاف، وتحته قليلًا: الكاف.\n• وسط اللسان مع ما فوقه: الجيم والشين والياء غير المدّية.\n• إحدى حافتي اللسان مع ما يحاذيها من الأضراس العليا: الضاد (وهو أصعب الحروف).\n• أدنى حافة اللسان إلى منتهى طرفه: اللام.\n• طرف اللسان تحت مخرج اللام قليلًا: النون.\n• طرف اللسان مع ظهره قريبًا من مخرج النون: الراء.\n• طرف اللسان مع أصول الثنايا العليا: الطاء والدال والتاء.\n• طرف اللسان مع ما فوق الثنايا السفلى: الصاد والزاي والسين (حروف الصفير).\n• طرف اللسان مع أطراف الثنايا العليا: الظاء والذال والثاء.", letters = "ق ك  ج ش ي  ض  ل  ن  ر  ط د ت  ص ز س  ظ ذ ث"),
            Lesson("shafatan", "الشفتان", "فيهما مخرجان:\n• بطن الشفة السفلى مع أطراف الثنايا العليا: الفاء.\n• بين الشفتين: الباء والميم بانطباقهما، والواو غير المدّية بانفتاحهما.", letters = "ف  ب م و"),
            Lesson("khayshum", "الخيشوم", "أقصى الأنف من الداخل، ومنه تخرج الغنّة: صوت أغنّ مركّب في جسم النون والميم. ولو أمسكت أنفك لانقطعت الغنّة.", letters = "ن م", examples = listOf(Ex("إنا أعطيناك", 108, 1), Ex("ثم", 2, 28))),
        )),
        Unit("sifat", "🔤", "صفات الحروف", "الصفة كيفية تعرض للحرف عند خروجه، فتميّز الحروف المتحدة في المخرج (كالطاء والدال والتاء).", listOf(
            Lesson("hams", "الهمس والجهر", "• الهمس: جريان النَّفَس عند النطق بالحرف لضعف الاعتماد عليه. حروفه عشرة: «فحثّه شخص سكت».\n• الجهر: انحباس النَّفَس لقوة الاعتماد على الحرف. وهو في باقي الحروف.", letters = "فحثه شخص سكت"),
            Lesson("shidda", "الشدة والتوسط والرخاوة", "• الشدة: انحباس الصوت عند النطق بالحرف. حروفها: «أجد قط بكت».\n• التوسط (البينية): لا يجري الصوت كله ولا ينحبس كله. حروفه: «لن عمر».\n• الرخاوة: جريان الصوت مع الحرف. وهي في باقي الحروف.", letters = "أجد قط بكت  ·  لن عمر"),
            Lesson("istila", "الاستعلاء والاستفال", "• الاستعلاء: ارتفاع أقصى اللسان إلى الحنك الأعلى عند النطق، فيخرج الحرف مفخّمًا. حروفه سبعة: «خُصّ ضغطٍ قِظ».\n• الاستفال: انخفاض اللسان عن الحنك، فيخرج الحرف مرقّقًا. وهو في باقي الحروف.", letters = "خص ضغط قظ"),
            Lesson("itbaq", "الإطباق والانفتاح", "• الإطباق: انطباق اللسان على الحنك الأعلى وانحصار الصوت بينهما. حروفه أربعة: الصاد والضاد والطاء والظاء، وهي أقوى حروف التفخيم.\n• الانفتاح: تجافي اللسان عن الحنك. وهو في باقي الحروف.", letters = "ص ض ط ظ"),
            Lesson("idhlaq", "الإذلاق والإصمات", "• الإذلاق: خفّة الحرف وسرعة النطق به لخروجه من طرف اللسان أو الشفة. حروفه: «فرّ من لُبّ».\n• الإصمات: ثقل الحرف. وهو في باقي الحروف.", letters = "فر من لب"),
            Lesson("safir", "الصفير", "صوت زائد يشبه صوت الطائر يخرج مع الصاد والزاي والسين.", letters = "ص ز س"),
            Lesson("qalqala_s", "القلقلة", "اضطراب المخرج عند النطق بالحرف ساكنًا حتى يُسمع له نبرة قوية. حروفها: «قطب جد». ولها باب خاص بعد قليل.", letters = "قطب جد"),
            Lesson("lin", "اللين", "إخراج الحرف بسهولة من غير كلفة، وهو في الواو والياء الساكنتين المفتوح ما قبلهما، مثل: «خوف» و«البيت».", letters = "و ي", examples = listOf(Ex("من خوف", 106, 4), Ex("البيت", 106, 3))),
            Lesson("inhiraf", "الانحراف والتكرير", "• الانحراف: ميل الحرف بعد خروجه حتى يتصل بمخرج غيره، وهو في اللام والراء.\n• التكرير: ارتعاد طرف اللسان عند النطق بالراء. والمطلوب معرفته لاجتنابه، لا للمبالغة فيه، خاصة في الراء المشددة.", letters = "ل ر"),
            Lesson("tafashshi", "التفشي والاستطالة", "• التفشي: انتشار الريح في الفم عند النطق بالشين.\n• الاستطالة: امتداد الصوت من أول حافة اللسان إلى آخرها عند النطق بالضاد.", letters = "ش  ض"),
        )),
        Unit("noon", "ن", "أحكام النون الساكنة والتنوين", "للنون الساكنة والتنوين أربعة أحكام بحسب الحرف الذي بعدهما. والتنوين نون ساكنة تُلفظ ولا تُكتب.", listOf(
            Lesson("izhar", "الإظهار الحلقي", "إذا جاء بعد النون الساكنة أو التنوين حرف من حروف الحلق الستة أُظهرت النون واضحة بلا غنّة زائدة. الحروف: «أخي هاك علمًا حازه غير خاسر» (أوائل الكلمات).", letters = "ء هـ ع ح غ خ",
                examples = listOf(Ex("من آمن", 2, 62), Ex("من هاد", 13, 33), Ex("أنعمت", 1, 7), Ex("عذاب أليم", 2, 10), Ex("من عمل", 16, 97), Ex("عليم حكيم", 4, 26), Ex("فسينغضون", 17, 51), Ex("من خوف", 106, 4)), src = "تحفة الأطفال"),
            Lesson("idgham_g", "الإدغام بغنّة", "إذا جاء بعدها حرف من حروف «ينمو» في كلمة أخرى أُدغمت النون فيه مع غنّة بمقدار حركتين.\nويُستثنى ما جاء في كلمة واحدة فيُظهر: الدنيا، صنوان، قنوان، بنيان. وكذلك «يس • والقرآن» و«ن • والقلم» تُظهر النون عند حفص.", letters = "ي ن م و",
                examples = listOf(Ex("من يقول", 2, 8), Ex("من نور", 24, 40), Ex("من مال", 23, 55), Ex("من وال", 13, 11), Ex("يومئذ ناعمة", 88, 8), Ex("رحيم ودود", 11, 90))),
            Lesson("idgham_ng", "الإدغام بغير غنّة", "إذا جاء بعدها لام أو راء أُدغمت فيهما إدغامًا كاملًا بلا غنّة، مثل: «من ربهم».", letters = "ل ر",
                examples = listOf(Ex("من ربهم", 2, 5), Ex("من لدنه", 4, 40), Ex("هدى للمتقين", 2, 2), Ex("غفور رحيم", 2, 173))),
            Lesson("iqlab", "الإقلاب", "إذا جاء بعدها باء قُلبت النون أو التنوين ميمًا مخفاة بغنّة، وتُرسم في المصحف ميمًا صغيرة فوقها.", letters = "ب",
                examples = listOf(Ex("من بعد", 2, 27), Ex("أنبئهم", 2, 33), Ex("سميع بصير", 22, 61), Ex("لينبذن", 104, 4))),
            Lesson("ikhfa", "الإخفاء الحقيقي", "إذا جاء بعدها أحد الحروف الخمسة عشر الباقية أُخفيت النون: يُنطق بها بين الإظهار والإدغام مع غنّة بمقدار حركتين، ويتهيأ اللسان لمخرج الحرف التالي. وحروفه أوائل كلمات هذا البيت:\nصِفْ ذا ثنا كم جاد شخصٌ قد سما — دُمْ طيّبًا زِدْ في تقى ضَعْ ظالما", letters = "ص ذ ث ك ج ش ق س د ط ز ف ت ض ظ",
                examples = listOf(Ex("وأنتم تعلمون", 2, 22), Ex("من قبل", 2, 25), Ex("ينفقون", 2, 3), Ex("منضود", 56, 29), Ex("من ثمرة", 2, 25), Ex("الإنسان", 103, 2), Ex("انظر", 6, 24), Ex("ريحا صرصرا", 41, 16), Ex("جنات تجري", 2, 25)), src = "تحفة الأطفال"),
        ), quiz = true),
        Unit("meem", "م", "أحكام الميم الساكنة", "للميم الساكنة ثلاثة أحكام بحسب الحرف الذي بعدها.", listOf(
            Lesson("ikhfa_sh", "الإخفاء الشفوي", "إذا جاء بعد الميم الساكنة باء أُخفيت الميم بغنّة بمقدار حركتين، مع انطباق خفيف للشفتين.", letters = "ب",
                examples = listOf(Ex("ترميهم بحجارة", 105, 4), Ex("وهم بالآخرة", 7, 45))),
            Lesson("idgham_sh", "إدغام المثلين الصغير", "إذا جاء بعدها ميم متحركة أُدغمت الأولى في الثانية بغنّة بمقدار حركتين، وتُنطقان ميمًا واحدة مشددة.", letters = "م",
                examples = listOf(Ex("ولهم ما", 16, 57), Ex("كم من فئة", 2, 249))),
            Lesson("izhar_sh", "الإظهار الشفوي", "إذا جاء بعدها أي حرف غير الباء والميم أُظهرت الميم بلا غنّة. ويُعتنى بإظهارها عند الواو والفاء خاصة؛ لقرب مخرجهما منها، فلا تُخفى.",
                examples = listOf(Ex("ألم تر", 105, 1), Ex("عليهم ولا", 1, 7), Ex("هم فيها", 2, 39), Ex("لكم دينكم", 109, 6))),
        ), quiz = true),
        Unit("ghunna", "〰", "النون والميم المشددتان", "الغنّة صوت يخرج من الخيشوم.", listOf(
            Lesson("mushaddad", "الغنّة في المشدد", "كل نون أو ميم مشددة تُغنّ غنّة كاملة بمقدار حركتين، وتُسمّى «حرف غنّة مشدد»، سواء كانت في وسط الكلمة أو آخرها، في الوصل والوقف.", letters = "نّ مّ",
                examples = listOf(Ex("إنا أعطيناك", 108, 1), Ex("ثم إليه ترجعون", 2, 28), Ex("عم يتساءلون", 78, 1), Ex("الجنة", 2, 35), Ex("الجنة والناس", 114, 6))),
            Lesson("ghunna_levels", "مراتب الغنّة", "الغنّة أكمل ما تكون في المشدد، ثم في المدغم بغنّة، ثم في المخفى، ثم في الساكن المظهر، ثم في المتحرك. ومقدارها في الثلاث الأولى حركتان."),
        )),
        Unit("lam", "ل", "أحكام اللامات", "اللام الساكنة إما لام التعريف «ال»، أو لام في فعل أو حرف أو اسم، ولكلٍّ حكمه. ومعها لام لفظ الجلالة.", listOf(
            Lesson("qamari", "اللام القمرية", "تُظهر لام «ال» إذا جاء بعدها حرف من حروف «ابغِ حجّك وخَف عقيمه» (أربعة عشر حرفًا)، وتظهر عليها السكون في المصحف.", letters = "ا ب غ ح ج ك و خ ف ع ق ي م هـ",
                examples = listOf(Ex("الحمد", 1, 2), Ex("القمر", 54, 1), Ex("الكتاب", 2, 2), Ex("العالمين", 1, 2), Ex("في الأرض", 2, 11))),
            Lesson("shamsi", "اللام الشمسية", "تُدغم لام «ال» في الحرف الذي بعدها إذا كان من الأربعة عشر الباقية، فلا تُنطق، ويُشدَّد الحرف الذي بعدها.", letters = "ط ث ص ر ت ض ذ ن د س ظ ز ش ل",
                examples = listOf(Ex("والشمس", 91, 1), Ex("الرحمن", 1, 3), Ex("والنجم", 53, 1), Ex("الصراط", 1, 6), Ex("الذين", 1, 7))),
            Lesson("lam_fil", "لام الفعل ولام الحرف", "لام الفعل (قُل، التقى) ولام الحرف (هل، بل) تُظهران دائمًا، إلا إذا جاء بعدهما لام أو راء فتُدغمان، مثل: «قل ربي» و«بل رفعه». و«بل ران» يسكت فيها حفص على اللام سكتة لطيفة فلا تُدغم.",
                examples = listOf(Ex("قل نعم", 37, 18), Ex("فالتقى", 54, 12), Ex("وقل رب", 17, 24), Ex("بل رفعه", 4, 158))),
            Lesson("jalala", "لام لفظ الجلالة", "لام اسم «الله» تُفخَّم إذا سبقها فتح أو ضم، وتُرقَّق إذا سبقها كسر، أصليًّا كان أو عارضًا.",
                examples = listOf(Ex("قال الله", 3, 55, "تفخيم بعد فتح"), Ex("محمد رسول الله", 48, 29, "تفخيم بعد ضم"), Ex("بسم الله", 1, 1, "ترقيق بعد كسر"), Ex("قل اللهم", 3, 26, "ترقيق بعد كسر عارض"))),
        ), quiz = true),
        Unit("ra", "ر", "التفخيم والترقيق", "التفخيم: تسمين صوت الحرف فيمتلئ الفم بصداه. والترقيق: تنحيفه.", listOf(
            Lesson("always", "ما يُفخَّم دائمًا وما يُرقَّق", "• حروف الاستعلاء «خُصّ ضغطٍ قِظ» مفخّمة دائمًا، وأقواها حروف الإطباق (ص ض ط ظ). ومراتب تفخيمها خمس: المفتوح بعده ألف، ثم المفتوح، ثم المضموم، ثم الساكن، ثم المكسور.\n• حروف الاستفال مرقّقة دائمًا، إلا الراء ولام لفظ الجلالة ففيهما تفصيل، والألف تتبع ما قبلها تفخيمًا وترقيقًا.", letters = "خص ضغط قظ",
                examples = listOf(Ex("قال", 2, 30, "الألف بعد حرف مفخم تُفخَّم"), Ex("الصراط", 1, 6))),
            Lesson("ra_tafkhim", "تفخيم الراء", "تُفخَّم الراء:\n• إذا كانت مفتوحة أو مضمومة: «ربنا»، «رزقناهم».\n• إذا كانت ساكنة وقبلها فتح أو ضم: «القرآن».\n• إذا كانت ساكنة وقبلها كسر عارض: «ارجعي».\n• إذا كانت ساكنة بعد كسر أصلي وبعدها حرف استعلاء غير مكسور في الكلمة نفسها: «قرطاس»، «مرصادا»، «فرقة».",
                examples = listOf(Ex("ربنا", 2, 127), Ex("رزقناهم", 2, 3), Ex("ارجعي", 89, 28), Ex("قرطاس", 6, 7), Ex("مرصادا", 78, 21), Ex("فرقة", 9, 122))),
            Lesson("ra_tarqiq", "ترقيق الراء", "تُرقَّق الراء:\n• إذا كانت مكسورة: «رجال».\n• إذا كانت ساكنة بعد كسر أصلي وليس بعدها حرف استعلاء: «فرعون».\n• إذا وُقف عليها وقبلها ياء ساكنة: «خير»، «قدير».\n• إذا وُقف عليها وقبلها كسر أو ساكن قبله كسر: «الذكر»، «مستمر».",
                examples = listOf(Ex("رجال", 9, 108), Ex("فرعون", 2, 49), Ex("قدير", 2, 20, "عند الوقف"), Ex("الذكر", 15, 9, "عند الوقف"), Ex("مستمر", 54, 2, "عند الوقف"))),
            Lesson("ra_both", "ما يجوز فيه الوجهان", "كلمات يجوز فيها التفخيم والترقيق:\n• «فرقٍ» (الشعراء 63): الترقيق أولى لكسر القاف.\n• «مصر» و«القطر» عند الوقف: يُختار تفخيم راء «مصر» وترقيق راء «القطر» اعتبارًا بحال الوصل.\n• «يسرِ» (الفجر 4) و«أسرِ» و«فأسرِ» عند الوقف: الترقيق أولى.",
                examples = listOf(Ex("كل فرق", 26, 63), Ex("بمصر", 10, 87), Ex("القطر", 34, 12))),
        ), quiz = true),
        Unit("qalqala", "📳", "القلقلة", "حروفها «قطب جد»، وتكون في الساكن منها، أصليًّا كان السكون أو عارضًا للوقف.", listOf(
            Lesson("sughra", "القلقلة الصغرى", "حرف القلقلة ساكن سكونًا أصليًّا في وسط الكلمة أو آخرها وصلًا، فتكون القلقلة خفيفة.", letters = "ق ط ب ج د",
                examples = listOf(Ex("يقطعون", 2, 27), Ex("قد أفلح", 23, 1), Ex("يدخلون", 4, 124), Ex("والفجر", 89, 1))),
            Lesson("kubra", "القلقلة الكبرى", "حرف القلقلة في آخر الكلمة وسُكّن للوقف، فتكون القلقلة أوضح.",
                examples = listOf(Ex("الفلق", 113, 1), Ex("محيط", 85, 20), Ex("أحد", 112, 1), Ex("البروج", 85, 1))),
            Lesson("akbar", "القلقلة الأكبر", "حرف القلقلة مشدد في آخر الكلمة ووُقف عليه، فهي أقوى المراتب، مثل: «الحق» و«تبّ».",
                examples = listOf(Ex("وتب", 111, 1), Ex("بالحق", 2, 71))),
        ), quiz = true),
        Unit("madd", "〰️", "المدود", "المد: إطالة الصوت بحرف من حروف المد (ا و ي). والحركة مقدار قبض الإصبع أو بسطه. والمد أصلي وفرعي.", listOf(
            Lesson("tabii", "المد الطبيعي", "يُمدّ حركتين، وهو ما لا يقوم ذات الحرف إلا به، ولا يأتي بعده همز ولا سكون. ومنه مد حروف «حيٌّ طَهُر» في فواتح السور: حا، يا، طا، ها، را.",
                examples = listOf(Ex("قال", 2, 30), Ex("نوحيها", 11, 49), Ex("طه", 20, 1))),
            Lesson("muttasil", "المد المتصل (الواجب)", "حرف مد بعده همز في كلمة واحدة، ويُمدّ أربع أو خمس حركات وجوبًا، وست عند الوقف إذا كانت الهمزة آخر الكلمة.",
                examples = listOf(Ex("جاء", 110, 1), Ex("السماء", 2, 19), Ex("سوء", 2, 49))),
            Lesson("munfasil", "المد المنفصل (الجائز)", "حرف مد في آخر كلمة وهمز في أول الكلمة التي بعدها، ويُمدّ أربع أو خمس حركات لحفص من طريق الشاطبية (ويجوز القصر حركتين من طريق الطيبة).",
                examples = listOf(Ex("بما أنزل", 2, 4), Ex("قالوا آمنا", 2, 14), Ex("يا أيها", 2, 21))),
            Lesson("badal", "مد البدل", "همز قبل حرف مد، ويُمدّ حركتين.",
                examples = listOf(Ex("آمنوا", 2, 9), Ex("إيمانا", 3, 173), Ex("أوتوا", 2, 101))),
            Lesson("arid", "المد العارض للسكون", "حرف مد بعده حرف سُكّن للوقف، ويجوز فيه حركتان أو أربع أو ست.",
                examples = listOf(Ex("العالمين", 1, 2), Ex("نستعين", 1, 5), Ex("الرحيم", 1, 3))),
            Lesson("lin_madd", "مد اللين", "واو أو ياء ساكنة قبلها فتح، وبعدها حرف سُكّن للوقف، ويجوز فيه حركتان أو أربع أو ست.",
                examples = listOf(Ex("قريش", 106, 1), Ex("البيت", 106, 3), Ex("من خوف", 106, 4))),
            Lesson("lazim_k", "المد اللازم الكلمي", "حرف مد بعده سكون أصلي في كلمة واحدة، ويُمدّ ست حركات لزومًا.\n• مثقّل: إذا كان الساكن مشددًا: «الضالين»، «الحاقة»، «دابة».\n• مخفّف: إذا لم يُشدَّد، وليس في القرآن منه إلا «آلآن» في موضعي يونس.",
                examples = listOf(Ex("الضالين", 1, 7), Ex("الحاقة", 69, 1), Ex("الصاخة", 80, 33), Ex("آلآن", 10, 51))),
            Lesson("lazim_h", "المد اللازم الحرفي", "في فواتح السور، الحرف الذي هجاؤه ثلاثة أحرف أوسطها حرف مد، وحروفه «نقص عسلكم»، فيُمدّ ست حركات (والعين يجوز فيها أربع أو ست).\n• مثقّل: إذا أُدغم في ما بعده، كاللام في «الم» (لام ميم).\n• مخفّف: إذا لم يُدغم، كالميم في «الم» و«ق» و«ن» و«ص».\nوالألف في «الم» لا تُمد لأنه ليس في هجائها حرف مد.", letters = "ن ق ص ع س ل ك م",
                examples = listOf(Ex("الم", 2, 1), Ex("كهيعص", 19, 1), Ex("ق", 50, 1), Ex("ص", 38, 1))),
            Lesson("sila", "مد الصلة", "هاء الضمير المفرد الغائب إذا وقعت بين متحركين تُمدّ:\n• صغرى: إذا لم يأتِ بعدها همز، فتُمدّ حركتين: «إنه هو».\n• كبرى: إذا جاء بعدها همز، فتُمدّ أربعًا أو خمسًا كالمنفصل: «عنده إلا».\nوتُستثنى «فيهِ مهانا» (الفرقان) فتمد، و«يرضَهُ لكم» (الزمر) فلا تمد.",
                examples = listOf(Ex("إنه هو", 2, 37), Ex("عنده إلا", 2, 255), Ex("ماله أخلده", 104, 3))),
            Lesson("iwad", "مد العوض", "عند الوقف على تنوين النصب يُبدل ألفًا وتُمدّ حركتين، إلا تاء التأنيث المربوطة فيُوقف عليها هاءً ساكنة.",
                examples = listOf(Ex("عليما حكيما", 4, 11))),
            Lesson("tamkin", "مد التمكين والفرق", "• مد التمكين: ياءان أولاهما مشددة مكسورة والثانية ساكنة، فتُمدّ حركتين مع التمكين: «حُيّيتم»، «النبيين».\n• مد الفرق: همزة استفهام دخلت على همزة وصل في «ال»، فتُبدل ألفًا وتُمدّ ست حركات؛ للتفريق بين الاستفهام والخبر.",
                examples = listOf(Ex("حييتم", 4, 86), Ex("النبيين", 2, 61), Ex("آلذكرين", 6, 143), Ex("آلله أذن لكم", 10, 59))),
        ), quiz = true),
        Unit("idgham", "🔗", "الإدغام بين الحروف", "إدخال حرف ساكن في متحرك بحيث يصيران حرفًا واحدًا مشددًا. وله ثلاثة أنواع بحسب العلاقة بين الحرفين.", listOf(
            Lesson("mithlayn", "المتماثلان", "حرفان اتفقا مخرجًا وصفة، الأول ساكن والثاني متحرك، فيجب إدغامهما.",
                examples = listOf(Ex("اضرب بعصاك", 2, 60), Ex("فما ربحت تجارتهم", 2, 16), Ex("وقد دخلوا", 5, 61))),
            Lesson("mutajanisayn", "المتجانسان", "حرفان اتفقا مخرجًا واختلفا صفة. وتُدغم عند حفص في: التاء في الدال والطاء، والدال في التاء، والثاء في الذال، والذال في الظاء، والباء في الميم. والطاء في التاء إدغامًا ناقصًا يبقى معه الإطباق: «بسطت»، «أحطت»، «فرطتم».",
                examples = listOf(Ex("قد تبين", 2, 256), Ex("أثقلت دعوا", 7, 189), Ex("يلهث ذلك", 7, 176), Ex("إذ ظلموا", 4, 64), Ex("اركب معنا", 11, 42), Ex("ودت طائفة", 3, 69), Ex("أحطت", 27, 22))),
            Lesson("mutaqaribayn", "المتقاربان", "حرفان تقاربا مخرجًا أو صفة. ويُدغم منه عند حفص: اللام في الراء «قل رب»، والقاف في الكاف «ألم نخلقكم» (إدغامًا كاملًا، ويجوز الناقص).",
                examples = listOf(Ex("وقل رب", 23, 118), Ex("ألم نخلقكم", 77, 20))),
        ), quiz = true),
        Unit("waqf", "✋", "الوقف والابتداء وعلاماته", "الوقف: قطع الصوت عند آخر الكلمة زمنًا يُتنفّس فيه عادة بنية استئناف القراءة. والسكت: قطعه دون تنفس.", listOf(
            Lesson("waqf_kinds", "أقسام الوقف", "• التام: على كلام تمّ معناه ولا تعلّق له بما بعده لفظًا ولا معنى، كأواخر القصص.\n• الكافي: تمّ معناه وتعلّق بما بعده معنًى لا لفظًا.\n• الحسن: تمّ معناه وتعلّق بما بعده لفظًا، كالوقف على «الحمد لله» ثم يُعاد.\n• القبيح: لا يتمّ معناه أو يُفسده، كالوقف على «لا تقربوا الصلاة»، فيعود القارئ ويصل."),
            Lesson("signs", "علامات الوقف في المصحف", "• مـ : وقف لازم، يجب الوقف لئلا يتغير المعنى.\n• لا : وقف ممنوع.\n• ج : وقف جائز، يستوي الوقف والوصل.\n• صلى : الوصل أولى مع جواز الوقف.\n• قلى : الوقف أولى مع جواز الوصل.\n• ∴ ∴ : وقف التعانق، يُوقف على أحد الموضعين لا على كليهما، مثل: ﴿ذلك الكتاب لا ريب ∴ فيه ∴ هدى للمتقين﴾.",
                examples = listOf(Ex("ذلك الكتاب لا ريب فيه", 2, 2))),
            Lesson("waqf_how", "كيف تقف", "• يُوقف على المتحرك بالسكون، ويجوز الروم (الإتيان بثلث الحركة) في المضموم والمكسور، والإشمام (ضمّ الشفتين بلا صوت) في المضموم.\n• تنوين النصب يُوقف عليه ألفًا: «عليمًا» ← «عليما».\n• تاء التأنيث المربوطة يُوقف عليها هاءً ساكنة: «رحمة» ← «رحمه».\n• لا يُوقف على وسط الكلمة، ولا على ما كُتب موصولًا في المصحف."),
            Lesson("sakt", "السكتات الأربع لحفص", "يسكت حفص سكتة لطيفة بلا تنفس في أربعة مواضع، وعليها حرف «سـ» صغير:\n• «عوجا ۜ قيما» (الكهف 1)\n• «من مرقدنا ۜ هذا» (يس 52)\n• «من ۜ راق» (القيامة 27)\n• «بل ۜ ران» (المطففين 14)",
                examples = listOf(Ex("عوجا", 18, 1), Ex("مرقدنا", 36, 52), Ex("من راق", 75, 27), Ex("بل ران", 83, 14))),
        )),
        Unit("hamza", "ء", "همزة الوصل والقطع", "همزة القطع تثبت في الابتداء والوصل (أنعمت، أكرم). وهمزة الوصل تثبت في الابتداء فقط وتسقط في الوصل.", listOf(
            Lesson("wasl_fil", "همزة الوصل في الأفعال", "يُبدأ بها مضمومة إذا كان ثالث الفعل مضمومًا ضمًّا لازمًا: «اُنظر»، «اُدعوني». ويُبدأ بها مكسورة إذا كان ثالثه مفتوحًا أو مكسورًا: «اِذهب»، «اِرجعي»، «اِهدنا».",
                examples = listOf(Ex("انظر", 6, 24, "تُبدأ بالضم"), Ex("ادعوني", 40, 60, "تُبدأ بالضم"), Ex("اذهب", 20, 24, "تُبدأ بالكسر"), Ex("اهدنا", 1, 6, "تُبدأ بالكسر"))),
            Lesson("wasl_ism", "همزة الوصل في الأسماء و«ال»", "• في «ال» التعريف يُبدأ بها مفتوحة: «اَلحمد».\n• في الأسماء العشرة يُبدأ بها مكسورة: ابن، ابنة، امرؤ، امرأة، اثنان، اثنتان، اسم، است، ابنم، وفي مصادر الأفعال الخماسية والسداسية: «اِستغفار».",
                examples = listOf(Ex("عيسى ابن مريم", 4, 157), Ex("امرأت عمران", 3, 35), Ex("اسمه المسيح", 3, 45))),
        )),
        Unit("hafs", "⭐", "كلمات خاصة برواية حفص", "كلمات تُقرأ بطريقة خاصة في رواية حفص عن عاصم، يحسن بكل قارئ أن يعرفها.", listOf(
            Lesson("imala", "الإمالة في «مجريها»", "الإمالة الكبرى في راء «مجريها» (هود 41)، فتُنطق الفتحة قريبة من الكسرة والألف قريبة من الياء. وهي الإمالة الوحيدة لحفص.", examples = listOf(Ex("مجراها", 11, 41))),
            Lesson("tashil", "التسهيل في «ءأعجمي»", "تُسهَّل الهمزة الثانية بين الهمزة والألف في «ءأعجميّ» (فصلت 44).", examples = listOf(Ex("أأعجمي", 41, 44))),
            Lesson("ishmam", "الإشمام في «لا تأمنّا»", "أصلها «تأمنُنا»، فتُدغم النون في النون مع الإشمام: ضمّ الشفتين عند النطق بالنون المدغمة إشارة إلى الضمة (يوسف 11). ويجوز الروم.", examples = listOf(Ex("لا تأمنا", 12, 11))),
            Lesson("daf", "«ضعف» في الروم", "في الروم (54) ثلاث كلمات «ضعف»، يجوز في ضادها الفتح والضم لحفص، والفتح مقدّم.", examples = listOf(Ex("من ضعف", 30, 54))),
            Lesson("sad_sin", "السين والصاد", "• «يبسط» (البقرة 245) و«بسطة» (الأعراف 69): تُقرآن بالسين وإن كُتبت عليهما صاد صغيرة.\n• «المصيطرون» (الطور 37): بالصاد والسين، والصاد مقدّمة.\n• «بمصيطر» (الغاشية 22): بالصاد فقط.",
                examples = listOf(Ex("ويبسط", 2, 245), Ex("بسطة", 7, 69), Ex("المصيطرون", 52, 37), Ex("بمصيطر", 88, 22))),
            Lesson("alifs", "ألفات تُكتب ولا تُقرأ وصلًا", "• «أنا» تُحذف ألفها وصلًا وتثبت وقفًا.\n• «لكنّا هو الله» (الكهف 38) و«الظنونا» و«الرسولا» و«السبيلا» (الأحزاب) و«قواريرا» الأولى (الإنسان 15): تُحذف ألفها وصلًا وتثبت وقفًا.\n• «سلاسلا» (الإنسان 4): يجوز الوقف بالألف وبدونها.",
                examples = listOf(Ex("أنا خير منه", 7, 12), Ex("لكنا هو الله ربي", 18, 38), Ex("سلاسل", 76, 4), Ex("قواريرا", 76, 15))),
            Lesson("atani", "«آتانِ الله» في النمل", "«فما آتانِ الله» (النمل 36): تُحذف الياء وصلًا، ويجوز عند الوقف حذفها وإثباتها ساكنة.", examples = listOf(Ex("فما آتاني الله", 27, 36))),
        )),
        Unit("adab", "🤲", "آداب التلاوة وسجودها", "ما يُستحب لقارئ القرآن، ومواضع سجود التلاوة.", listOf(
            Lesson("adab_list", "آداب التلاوة", "• الإخلاص لله، والطهارة، واستقبال القبلة.\n• الاستعاذة عند البدء، والبسملة أول السور.\n• الترتيل والتدبّر والخشوع: ﴿كتاب أنزلناه إليك مبارك ليدّبّروا آياته﴾.\n• تحسين الصوت: «زيّنوا القرآن بأصواتكم».\n• سؤال الله الرحمة عند آية الرحمة، والاستعاذة عند آية العذاب، كما كان يفعل النبي ﷺ.",
                examples = listOf(Ex("ليدبروا آياته", 38, 29)), src = "«زيّنوا القرآن بأصواتكم»: أبو داود والنسائي، وصححه الألباني؛ والسؤال والاستعاذة: مسلم"),
            Lesson("sujud", "سجود التلاوة", "سنّة للقارئ والمستمع عند آية السجدة، يكبّر ويسجد سجدة واحدة، ويقول: «سبحان ربي الأعلى»، ومما ورد: «سجد وجهي للذي خلقه، وشقّ سمعه وبصره بحوله وقوته».\nمواضعه في المصحف خمسة عشر (ومن العلماء من عدّها أربعة عشر: فالشافعية لا يعدّون سجدة «ص» من العزائم، والحنفية لا يعدّون الثانية من الحج)، ويُعلَّم عليها في المصحف بـ ۩. اضغط على أي موضع لتسمعه أو تفتحه:",
                examples = listOf(Ex("", 7, 206), Ex("", 13, 15), Ex("", 16, 50), Ex("", 17, 109), Ex("", 19, 58), Ex("", 22, 18), Ex("", 22, 77), Ex("", 25, 60), Ex("", 27, 26), Ex("", 32, 15), Ex("", 38, 24), Ex("", 41, 38), Ex("", 53, 62), Ex("", 84, 21), Ex("", 96, 19)),
                src = "الدعاء: أبو داود والترمذي، وصححه الترمذي"),
        )),
    )

    val lessons get() = units.flatMap { it.lessons }
    fun done(id: String) = id in (Store.prefs.getStringSet("tajweed_done", emptySet()) ?: emptySet())
    fun setDone(id: String, on: Boolean) {
        val set = (Store.prefs.getStringSet("tajweed_done", emptySet()) ?: emptySet()).toMutableSet()
        if (on) set += id else set -= id
        Store.prefs.edit().putStringSet("tajweed_done", set).apply(); Store.bump()
        if (on) runCatching { Progress.record(Progress.Kind.STUDY, "tj:$id") }
    }
    fun doneCount() = lessons.count { done(it.id) }
    fun best(unit: String) = Store.prefs.getInt("tajweed_best_$unit", -1)
    fun setBest(unit: String, score: Int) { if (score > best(unit)) { Store.prefs.edit().putInt("tajweed_best_$unit", score).apply(); Store.bump() } }

    /** The words of [e] as the mushaf writes them (the plain letters when the verse's words do not line up). */
    fun shown(e: Ex): String {
        val a = Quran.at(e.s, e.a) ?: return e.word
        if (e.word.isBlank()) return a.text
        val letter = Regex("[ء-يٱ]")
        val t = a.text.split(' ').filter { letter.containsMatchIn(it) }
        val p = a.plain.split(' ').filter { letter.containsMatchIn(it) }
        val want = Quran.normalize(e.word).split(' ')
        if (t.size != p.size) return e.word
        val np = p.map { Quran.normalize(it) }
        for (i in 0..np.size - want.size) {
            if (want.indices.all { k -> np[i + k] == want[k] || (k == want.lastIndex && np[i + k].startsWith(want[k])) || (k == 0 && np[i + k].endsWith(want[k])) })
                return t.subList(i, i + want.size).joinToString(" ")
        }
        return e.word
    }

    data class Question(val ex: Ex, val answer: String, val options: List<String>)
    /** Ten questions: which rule does this word show? Built from the unit's own examples. */
    fun quiz(u: Unit): List<Question> {
        val withEx = u.lessons.filter { it.examples.isNotEmpty() }
        val names = withEx.map { it.title }
        return withEx.flatMap { l -> l.examples.map { l to it } }.shuffled().take(10).map { (l, e) ->
            val others = (names - l.title).shuffled().take(3)
            Question(e, l.title, (others + l.title).shuffled())
        }
    }
}

@Composable
fun TajweedPage() {
    val keep = androidx.compose.runtime.saveable.rememberSaveableStateHolder()   // the list below keeps its place while a page covers it
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    var unit by rememberSaveable { mutableStateOf<String?>(null) }
    var quiz by rememberSaveable { mutableStateOf<String?>(null) }
    var reciters by remember { mutableStateOf(false) }
    if (reciters) ReciterDialog { reciters = false }
    if (!Quran.ready) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = P.accent) }; return }
    quiz?.let { id -> Tajweed.units.firstOrNull { it.id == id }?.let { TajweedQuiz(it) { quiz = null }; return } }
    unit?.let { id ->
        Tajweed.units.firstOrNull { it.id == id }?.let { u ->
            androidx.activity.compose.BackHandler { unit = null }
            TajweedUnit(u, openQuiz = { quiz = u.id }, pickReciter = { reciters = true }, back = { unit = null })
            return
        }
    }
    keep.SaveableStateProvider("main") {
    val all = Tajweed.lessons.size
    val done = Tajweed.doneCount()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel(color = P.accent.copy(alpha = .10f)) {
            Text("﴿ورتّل القرآن ترتيلًا﴾", color = P.ink, fontSize = 22.sp, fontFamily = Fonts.quran)
            Sub("دروس التجويد برواية حفص عن عاصم، بأمثلة من المصحف تسمعها وتفتحها، واختبار لكل باب.", size = 13.sp)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { if (all == 0) 0f else done / all.toFloat() }, modifier = Modifier.weight(1f).height(6.dp).clip(L.shape(3.dp)), color = P.accent, trackColor = P.line)
                Text("  $done من $all درسًا", color = P.muted, fontSize = 12.sp)
            }
        }
        ReciterLine { reciters = true }
        Tajweed.units.forEachIndexed { i, u ->
            val d = u.lessons.count { Tajweed.done(it.id) }
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { unit = u.id }.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(L.shape(10.dp)).background(if (d == u.lessons.size) P.accent else P.card).padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(if (d == u.lessons.size) "✓" else Quran.digits(i + 1), color = if (d == u.lessons.size) P.onAccent else P.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(u.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Sub("${Quran.digits(d)} من ${Quran.digits(u.lessons.size)} دروس" + (if (u.quiz && Tajweed.best(u.id) >= 0) " · أفضل نتيجة ${Quran.digits(Tajweed.best(u.id))}/10" else if (u.quiz) " · فيه اختبار" else ""), size = 12.sp)
                }
                Text("›", color = P.muted, fontSize = 20.sp)
            }
        }
        Sub("المرجع: تحفة الأطفال للجمزوري، والمقدمة الجزرية لابن الجزري، برواية حفص عن عاصم من طريق الشاطبية. التجويد يُتلقّى بالمشافهة، فاعرض قراءتك على معلّم متقن.", Modifier.padding(4.dp), size = 11.sp)
    }
    }
}

@Composable
private fun TajweedUnit(u: Tajweed.Unit, openQuiz: () -> Unit, pickReciter: () -> Unit, back: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var open by rememberSaveable { mutableStateOf(u.lessons.firstOrNull { !Tajweed.done(it.id) }?.id ?: u.lessons.first().id) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ الأبواب", color = P.accent, fontSize = 14.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable(onClick = back).padding(6.dp))
            Spacer1()
        }
        Title(u.title)
        Sub(u.intro, size = 13.sp)
        if (u.quiz) Action("📝 اختبر نفسك في هذا الباب", Modifier.fillMaxWidth(), filled = false) { openQuiz() }
        u.lessons.forEach { l ->
            val isOpen = open == l.id
            val ok = Tajweed.done(l.id)
            Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, if (isOpen) P.accent.copy(alpha = .5f) else P.line, L.shape(14.dp))) {
                Row(Modifier.fillMaxWidth().clickable { open = if (isOpen) "" else l.id }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ok) "✓" else "•", color = if (ok) P.accent else P.muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(l.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                    Text(if (isOpen) "▴" else "▾", color = P.muted, fontSize = 14.sp)
                }
                if (isOpen) Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(l.text, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (26 * Data.textScale).sp)
                    if (l.letters.isNotBlank()) Text(l.letters, color = P.accent, fontSize = 24.sp, fontFamily = Fonts.naskh, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).background(P.accent.copy(alpha = .08f)).padding(10.dp))
                    if (l.examples.isNotEmpty()) {
                        Sub("أمثلة من المصحف — اضغط 🔊 لتسمع، أو 📖 لتفتح الآية:", size = 12.sp)
                        l.examples.forEach { e -> ExampleRow(e) }
                        Sub("القارئ الحالي: ${Recitation.reciterName()} · تغيير", Modifier.clip(L.shape(8.dp)).clickable(onClick = pickReciter).padding(4.dp), size = 11.sp, color = P.accent)
                    }
                    if (l.src.isNotBlank()) Sub(l.src, size = 11.sp)
                    Text(if (ok) "✓ فهمت هذا الدرس (اضغط للتراجع)" else "فهمت هذا الدرس ✓", color = if (ok) P.accent else P.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(L.shape(12.dp)).background(if (ok) P.accent.copy(alpha = .12f) else P.accent).clickable {
                            Tajweed.setDone(l.id, !ok)
                            if (!ok) { Pet.cheer("أحسنت! درس جديد في التجويد", xp = false); u.lessons.dropWhile { it.id != l.id }.drop(1).firstOrNull { !Tajweed.done(it.id) }?.let { open = it.id } }
                        }.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }
        if (u.quiz) Action("📝 اختبر نفسك في هذا الباب", Modifier.fillMaxWidth()) { openQuiz() }
        val i = Tajweed.units.indexOf(u)
        Tajweed.units.getOrNull(i + 1)?.let { n -> Sub("الباب التالي: ${n.title}", size = 12.sp) }
    }
}

@Composable private fun Spacer1() = androidx.compose.foundation.layout.Spacer(Modifier.height(0.dp))

@Composable
internal fun ExampleRow(e: Tajweed.Ex) {
    val ctx = LocalContext.current
    val shown = remember(e) { Tajweed.shown(e) }
    val playing = Recitation.now == (e.s to e.a)
    Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.card).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(shown, color = P.ink, fontSize = if (e.word.isBlank()) 17.sp else 22.sp, fontFamily = Fonts.quran, lineHeight = if (e.word.isBlank()) 32.sp else 38.sp)
            Sub("${Quran.surahs[e.s - 1].name} ${Quran.digits(e.a)}" + if (e.note.isNotBlank()) " · ${e.note}" else "", size = 11.sp, color = P.gold)
        }
        Text(if (playing) "⏹" else "🔊", fontSize = 20.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable {
            if (playing) Recitation.stop() else Recitation.play(ctx, e.s, e.a, only = Recitation.Range.AYAH)
        }.padding(10.dp))
        Text("📖", fontSize = 20.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable { Nav.overlayQuran = e.s to e.a }.padding(10.dp))
    }
}

@Composable
private fun TajweedQuiz(u: Tajweed.Unit, back: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = back)
    val ctx = LocalContext.current
    var qs by remember { mutableStateOf(Tajweed.quiz(u)) }
    var i by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("‹ ${u.title}", color = P.accent, fontSize = 14.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable(onClick = back).padding(6.dp))
        if (i >= qs.size) {
            LaunchedEffect(Unit) { Tajweed.setBest(u.id, score * 10 / qs.size.coerceAtLeast(1)); if (score * 10 >= qs.size * 8) Pet.cheer("ما شاء الله! أتقنت ${u.title}", xp = false) }
            Panel(color = P.accent.copy(alpha = .10f)) {
                Title("نتيجتك: ${Quran.digits(score)} من ${Quran.digits(qs.size)}")
                Sub(when { score == qs.size -> "ممتاز! أتقنت هذا الباب."; score * 10 >= qs.size * 7 -> "أحسنت! راجع ما أخطأت فيه."; else -> "راجع دروس الباب ثم أعد الاختبار." })
            }
            Action("أعد الاختبار بأسئلة أخرى", Modifier.fillMaxWidth()) { qs = Tajweed.quiz(u); i = 0; score = 0; picked = null }
            Action("عُد إلى الدروس", Modifier.fillMaxWidth(), filled = false) { back() }
            return@Column
        }
        val q = qs[i]
        LinearProgressIndicator(progress = { i / qs.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(L.shape(3.dp)), color = P.accent, trackColor = P.line)
        Sub("السؤال ${Quran.digits(i + 1)} من ${Quran.digits(qs.size)} · ما الحكم في هذا المثال؟")
        ExampleRow(q.ex)
        q.options.forEach { o ->
            val bg = when { picked == null -> P.surface; o == q.answer -> P.accent.copy(alpha = .22f); o == picked -> androidx.compose.ui.graphics.Color(0x33D9534F); else -> P.surface }
            Text(o, color = P.ink, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(bg).border(1.dp, P.line, L.shape(12.dp))
                .clickable(enabled = picked == null) { picked = o; if (o == q.answer) { score++; Haptics.tap(ctx) } }.padding(14.dp))
        }
        if (picked != null) {
            Sub(if (picked == q.answer) "✓ صحيح" else "✗ الصواب: ${q.answer}", color = if (picked == q.answer) P.accent else P.gold, size = 14.sp)
            Action(if (i + 1 < qs.size) "التالي" else "النتيجة", Modifier.fillMaxWidth()) { i++; picked = null }
        }
    }
}
