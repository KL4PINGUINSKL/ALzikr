package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The friend's day with you (❤ friendship only, never the level): three daily tasks the app checks by itself, three
 * small challenges you mark yourself, a question whose answer is inside the app, and a chest at the end of a full week.
 * Adults only (children keep their own challenge and stars).
 */
object Missions {
    private val sp get() = Store.prefs
    private fun today() = LocalDate.now()
    private fun bag(kind: String, ids: List<String>, n: Int) = DailyBag.pick(kind, ids, n)

    // ------------------------------------------------------------ daily tasks (checked by the app)
    class Task(val id: String, val icon: String, val text: String, val done: () -> Boolean, val go: () -> Unit)
    private fun xp(k: Progress.Kind) = Progress.todayXp(k)
    private val five = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
    val allTasks: List<Task> = listOf(
        Task("prayers", "🕌", "علّم صلواتك الخمس ✓", { five.all { Data.prayed(today(), it) } }) { Nav.go("prayer") },
        Task("morning", "🌅", "أتمّ أذكار الصباح", { Data.colDone("morning") }) { Nav.collection = "morning"; Nav.go("adhkar") },
        Task("evening", "🌇", "أتمّ أذكار المساء", { Data.colDone("evening") }) { Nav.collection = "evening"; Nav.go("adhkar") },
        Task("wird", "📖", "اقرأ ورد اليوم", { xp(Progress.Kind.WIRD) > 0 }) { Nav.wird = true },
        Task("pages", "📄", "اقرأ خمس صفحات من القرآن", { xp(Progress.Kind.PAGE) >= 10 }) { Nav.go("library") },
        Task("listen", "🎧", "استمع إلى عشر آيات", { xp(Progress.Kind.LISTEN) >= 10 }) { Nav.go("library") },
        Task("hadith", "📜", "اقرأ حديثًا", { xp(Progress.Kind.HADITH) > 0 }) { Nav.go("library") },
        Task("sharh", "🔎", "اقرأ شرح حديث", { xp(Progress.Kind.SHARH) > 0 }) { Nav.go("library") },
        Task("tasbih", "📿", "سبّح مئة تسبيحة", { (Data.tasbihHistory(1).firstOrNull()?.second ?: 0) >= 100 }) { Nav.go("adhkar") },
        Task("study", "🎓", "ادرس درسًا في التجويد أو راجع حفظك", { xp(Progress.Kind.STUDY) > 0 }) { Nav.morePage = "tajweed"; Nav.go("more") },
        Task("sleep", "🌙", "اقرأ أذكار النوم", { Data.colDone("sleep") }) { Nav.collection = "sleep"; Nav.go("adhkar") },
        Task("duha", "☀", "صلِّ الضحى وعلّمها", { Data.extra(today(), "duha") }) { Nav.go("prayer") },
        Task("witr", "✨", "صلِّ الوتر وعلّمه", { Data.extra(today(), "witr") }) { Nav.go("prayer") },
        Task("pages10", "📚", "اقرأ عشر صفحات من القرآن", { xp(Progress.Kind.PAGE) >= 20 }) { Nav.go("library") },
        Task("listen30", "🎧", "استمع إلى ثلاثين آية", { xp(Progress.Kind.LISTEN) >= 30 }) { Nav.go("library") },
        Task("hadith3", "📜", "اقرأ ثلاثة أحاديث", { xp(Progress.Kind.HADITH) >= 6 }) { Nav.go("library") },
        Task("sadaqa", "🤲", "تصدّق وسجّل صدقتك", { runCatching { Sadaqa.all().any { it.date == today().toString() } }.getOrDefault(false) }) { Nav.morePage = "sadaqa"; Nav.go("more") },
        Task("tasbih300", "📿", "سبّح ثلاثمئة تسبيحة", { (Data.tasbihHistory(1).firstOrNull()?.second ?: 0) >= 300 }) { Nav.go("adhkar") },
    )
    fun tasks(): List<Task> = bag("task", allTasks.map { it.id }, 3).mapNotNull { id -> allTasks.firstOrNull { it.id == id } }
    const val TASK_BOND = 4
    /** Gives the friendship points of the tasks done (once a day each). Called whenever the panel shows. */
    fun collectTasks() { tasks().filter { runCatching { it.done() }.getOrDefault(false) }.forEach { Progress.bond("task_${it.id}", TASK_BOND) } }

    // ------------------------------------------------------------ challenges (marked by you)
    private val challengeList = listOf(
        "صلِّ ركعتي الضحى", "تصدّق اليوم ولو بشيء قليل", "صِل أحد أرحامك واسأل عن حاله", "اقرأ صفحة زائدة على وردك",
        "قل «سبحان الله وبحمده» مئة مرة", "استغفر الله مئة مرة", "صلِّ على النبي ﷺ أكثر من عادتك", "صلِّ السنن الرواتب كلها",
        "صلِّ الوتر قبل أن تنام", "ابدأ من تلقاه بالسلام وابتسم في وجهه", "ادعُ لأخ لك بظهر الغيب", "احفظ حديثًا جديدًا",
        "سبّح وحمّد وكبّر ثلاثًا وثلاثين بعد كل صلاة", "أمِط أذى عن الطريق أو أعِن أحدًا في حاجته", "احفظ لسانك اليوم من الغيبة",
        "اقرأ تفسير آية من وردك", "تعلّم مسألة فقهية جديدة", "توضّأ قبل النوم ونم على جنبك الأيمن",
        "قل «لا إله إلا الله وحده لا شريك له…» عشر مرات", "اشكر أحدًا أحسن إليك وادعُ له", "اتصل بوالديك أو زُرهما وأدخل عليهما السرور",
        "أطعم طعامًا أو اسقِ ماءً", "اعفُ عمّن أساء إليك اليوم", "اجلس بعد الفجر تذكر الله حتى تطلع الشمس",
        "صلِّ قبل الظهر أربعًا", "ردّد مع المؤذن وادعُ بعد الأذان", "اقرأ سورة الملك قبل النوم", "تفكّر في آية واحدة دقيقتين",
        "أكثِر من «لا حول ولا قوة إلا بالله»", "اقرأ آية الكرسي بعد كل صلاة", "اكتب ثلاث نِعَم تشكر الله عليها",
        "ابتعد عن الهاتف نصف ساعة واذكر الله فيها", "علّم أحدًا آية أو ذكرًا", "صُم إن كان يوم صيام مستحب أو انوِه لغد",
        "ادعُ لوالديك بعد كل صلاة", "اقرأ دعاء الخروج من البيت ودخوله", "رتّب مصلّاك أو مكان قراءتك",
        "صلِّ ركعتين بعد الوضوء", "صلِّ ركعتين قبل الفجر ولا تفوّتهما", "صلِّ أربعًا قبل العصر", "صلِّ ركعتين بعد المغرب",
        "قم من الليل ولو ركعتين", "بكّر إلى المسجد وانتظر الصلاة", "قل «سبحان الله وبحمده، سبحان الله العظيم» مئة مرة",
        "قل «لا إله إلا أنت سبحانك إني كنت من الظالمين» مئة مرة", "قل «رضيت بالله ربًّا وبالإسلام دينًا وبمحمد ﷺ نبيًّا» صباحًا ومساءً",
        "اقرأ سيد الاستغفار صباحًا ومساءً", "قل «حسبي الله لا إله إلا هو…» سبعًا صباحًا ومساءً", "ادعُ بدعاء «اللهم إنك عفوّ تحب العفو فاعفُ عني»",
        "اقرأ سورة الإخلاص عشر مرات", "اقرأ المعوّذات بعد كل صلاة", "اقرأ خواتيم سورة البقرة الليلة", "اقرأ سورة الكهف أو جزءًا منها",
        "تدبّر سورة قصيرة واقرأ تفسيرها", "احفظ آية جديدة اليوم", "راجع ما تحفظ من القرآن", "استمع إلى سورة كاملة بخشوع",
        "اقرأ حديثًا وشرحه وطبّقه اليوم", "تعلّم اسمًا من أسماء الله الحسنى ومعناه", "اقرأ عن صحابي من الصحابة",
        "اكتب دعاءً تحبه وادعُ به في سجودك", "أطِل سجودك في صلاة واحدة وادعُ فيه", "صلِّ صلاة بخشوع وكأنها آخر صلاة",
        "أخفِ عملًا صالحًا لا يعلمه إلا الله", "أصلح بين اثنين أو قل كلمة طيبة لمتخاصمين", "زُر مريضًا أو اتصل به",
        "أهدِ هدية ولو صغيرة", "ساعد في أعمال البيت دون أن يُطلب منك", "أحسِن إلى جارك ولو بالسلام والبشاشة",
        "اقضِ حاجة لأحد إخوانك", "انصح أحدًا برفق في أمر خير", "اشكر والديك وقبّل يديهما",
        "أكرِم ضيفًا أو أطعم صائمًا", "كفّ عن الجدال اليوم ولو كنت محقًّا", "لا تغضب اليوم، وإن غضبت فاستعذ بالله وتوضّأ",
        "قل خيرًا أو اصمت طوال اليوم", "اجتنب النظر إلى ما لا يحلّ", "لا تتكلم في أحد غائب إلا بخير",
        "قلّل من الشكوى وأكثِر من الحمد", "ابتسم في وجه ثلاثة أشخاص", "سامح أحدًا في قلبك قبل أن تنام",
        "نم مبكرًا لتقوم لصلاة الفجر بنشاط", "استيقظ قبل الفجر بعشر دقائق وادعُ", "اشرب جالسًا وكُل بيمينك وسمِّ الله",
        "تسوّك قبل الصلاة", "البس ثيابًا نظيفة وتطيّب للصلاة", "حافظ على الوضوء طوال اليوم",
        "ادخل المسجد برجلك اليمنى وقل دعاءه", "قل دعاء الركوب في كل مرة تركب فيها", "اذكر الله عند دخول السوق",
        "تصدّق سرًّا", "ضع مالًا في صندوق صدقة أو تبرّع لمحتاج", "علّم طفلًا ذكرًا أو سورة قصيرة",
        "شارك آية أو حديثًا نافعًا مع أهلك", "حدّث أهلك بنعمة أنعم الله بها عليك", "اسأل الله الجنة واستعذ به من النار ثلاثًا",
        "صلِّ على النبي ﷺ عشر مرات صباحًا وعشرًا مساءً", "اذكر الله بعد كل صلاة قبل أن تقوم", "تأمّل في خلق الله خمس دقائق وسبّحه",
    )
    fun challenges(): List<String> = bag("ch", challengeList.distinct(), 3)
    const val CHALLENGE_BOND = 5
    fun challengeDone(i: Int) = sp.getString("pet_ch${i}_done", "") == today().toString()
    fun doChallenge(i: Int) {
        if (challengeDone(i)) return
        sp.edit().putString("pet_ch${i}_done", today().toString()).putInt("pet_ch_count", sp.getInt("pet_ch_count", 0) + 1).apply()
        if (i == 0) sp.edit().putString("pet_ch_done", today().toString()).apply()   // the first one is the widget's challenge
        Pet.cheer("ما شاء الله! أتممت تحديًا", key = "challenge$i", xp = false)
        Progress.bond("challenge$i", CHALLENGE_BOND)
        checkFullDay()
    }

    // ------------------------------------------------------------ the question of the day (the answer is in the app)
    class Question(val id: String, val q: String, val a: String, val where: String, val open: () -> Unit)
    private fun ayah(s: Int, a: Int): () -> Unit = { Nav.openQuran(s, a) }
    private fun page(p: String): () -> Unit = { Nav.morePage = p; Nav.go("more") }
    val questions: List<Question> = listOf(
        Question("kursi", "ما أعظم آية في كتاب الله؟", "آية الكرسي: ﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ﴾ (البقرة ٢٥٥)", "سورة البقرة، الآية ٢٥٥", ayah(2, 255)),
        Question("ikhlas", "ما السورة التي تعدل ثلث القرآن؟", "سورة الإخلاص ﴿قُلْ هُوَ اللَّهُ أَحَدٌ﴾", "سورة الإخلاص", ayah(112, 1)),
        Question("fatiha", "كم عدد آيات سورة الفاتحة؟", "سبع آيات، وهي السبع المثاني", "سورة الفاتحة", ayah(1, 1)),
        Question("tawba", "ما السورة الوحيدة التي لا تبدأ بالبسملة؟", "سورة التوبة", "سورة التوبة", ayah(9, 1)),
        Question("naml", "في أي سورة جاءت البسملة في أثنائها، في كتاب سليمان عليه السلام؟", "سورة النمل: ﴿إِنَّهُ مِن سُلَيْمَانَ وَإِنَّهُ بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ﴾ (٣٠)", "سورة النمل، الآية ٣٠", ayah(27, 30)),
        Question("kawthar", "ما أقصر سورة في القرآن؟", "سورة الكوثر، ثلاث آيات", "سورة الكوثر", ayah(108, 1)),
        Question("maryam", "ما السورة التي سُمّيت باسم امرأة؟", "سورة مريم", "سورة مريم", ayah(19, 16)),
        Question("dayn", "ما أطول آية في القرآن؟", "آية الدَّين في سورة البقرة (٢٨٢)", "سورة البقرة، الآية ٢٨٢", ayah(2, 282)),
        Question("alaq", "ما أول ما نزل من القرآن؟", "﴿اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ﴾ أول سورة العلق", "سورة العلق", ayah(96, 1)),
        Question("yunus", "ما دعاء ذي النون (يونس عليه السلام) في بطن الحوت؟", "﴿لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ﴾ (الأنبياء ٨٧)", "سورة الأنبياء، الآية ٨٧", ayah(21, 87)),
        Question("zayd", "من الصحابي الوحيد الذي ذُكر اسمه في القرآن؟", "زيد بن حارثة رضي الله عنه (الأحزاب ٣٧)", "سورة الأحزاب، الآية ٣٧", ayah(33, 37)),
        Question("luqman", "بماذا بدأ لقمان وصيته لابنه؟", "﴿يَا بُنَيَّ لَا تُشْرِكْ بِاللَّهِ ۖ إِنَّ الشِّرْكَ لَظُلْمٌ عَظِيمٌ﴾ (لقمان ١٣)", "سورة لقمان، الآية ١٣", ayah(31, 13)),
        Question("qadr", "ليلة القدر خير من كم شهر؟", "﴿لَيْلَةُ الْقَدْرِ خَيْرٌ مِّنْ أَلْفِ شَهْرٍ﴾", "سورة القدر", ayah(97, 3)),
        Question("fil", "في أي سورة قصة أصحاب الفيل؟", "سورة الفيل", "سورة الفيل", ayah(105, 1)),
        Question("ukhdud", "في أي سورة قصة أصحاب الأخدود؟", "سورة البروج (٤–٨)", "سورة البروج، الآية ٤", ayah(85, 4)),
        Question("nur", "في أي سورة آية ﴿اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ﴾؟", "سورة النور، الآية ٣٥", "سورة النور، الآية ٣٥", ayah(24, 35)),
        Question("baqarah_end", "بماذا تُختم سورة البقرة من الدعاء؟", "﴿رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا…﴾ (البقرة ٢٨٦)", "سورة البقرة، الآية ٢٨٦", ayah(2, 286)),
        Question("kahf_cave", "كم لبث أصحاب الكهف في كهفهم؟", "﴿ثَلَاثَ مِائَةٍ سِنِينَ وَازْدَادُوا تِسْعًا﴾ (الكهف ٢٥)", "سورة الكهف، الآية ٢٥", ayah(18, 25)),
        Question("rahman", "ما الآية التي تكرّرت في سورة الرحمن؟", "﴿فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ﴾", "سورة الرحمن، الآية ١٣", ayah(55, 13)),
        Question("ibrahim_dua", "بماذا دعا إبراهيم عليه السلام لذريته في الصلاة؟", "﴿رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِن ذُرِّيَّتِي﴾ (إبراهيم ٤٠)", "سورة إبراهيم، الآية ٤٠", ayah(14, 40)),
        Question("nun", "ما أحكام النون الساكنة والتنوين؟", "أربعة: الإظهار، والإدغام، والإقلاب، والإخفاء", "تعلّم التجويد", page("tajweed")),
        Question("madd", "كم مقدار المدّ الطبيعي؟", "حركتان", "تعلّم التجويد", page("tajweed")),
        Question("sahw", "إذا شكّ المصلي كم صلّى، فماذا يفعل؟", "يبني على اليقين، وهو الأقل، ثم يسجد للسهو قبل أن يسلّم (حديث أبي سعيد في صحيح مسلم)", "صفة الوضوء والصلاة", page("salah")),
        Question("umm", "ما السورة التي تُسمّى أمّ الكتاب؟", "سورة الفاتحة", "سورة الفاتحة", ayah(1, 2)),
        Question("ilm", "ما الدعاء الذي أُمر النبي ﷺ أن يطلب فيه الزيادة؟", "﴿وَقُل رَّبِّ زِدْنِي عِلْمًا﴾ (طه ١١٤)", "سورة طه، الآية ١١٤", ayah(20, 114)),
        Question("walidayn_dua", "ما الدعاء للوالدين الذي علّمناه الله؟", "﴿رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا﴾ (الإسراء ٢٤)", "سورة الإسراء، الآية ٢٤", ayah(17, 24)),
        Question("uff", "ما الكلمة التي نهانا الله أن نقولها لوالدينا؟", "﴿فَلَا تَقُل لَّهُمَا أُفٍّ وَلَا تَنْهَرْهُمَا﴾ (الإسراء ٢٣)", "سورة الإسراء، الآية ٢٣", ayah(17, 23)),
        Question("yusr", "ما الذي يأتي مع العسر؟", "﴿فَإِنَّ مَعَ الْعُسْرِ يُسْرًا﴾ (الشرح ٥–٦)", "سورة الشرح، الآية ٥", ayah(94, 5)),
        Question("kawkab", "كم كوكبًا رأى يوسف عليه السلام في رؤياه؟", "أحد عشر كوكبًا والشمس والقمر (يوسف ٤)", "سورة يوسف، الآية ٤", ayah(12, 4)),
        Question("sabr", "ماذا قال يعقوب عليه السلام حين جاؤوه بقميص يوسف؟", "﴿فَصَبْرٌ جَمِيلٌ﴾ (يوسف ١٨)", "سورة يوسف، الآية ١٨", ayah(12, 18)),
        Question("isra", "من أين إلى أين كان الإسراء بالنبي ﷺ؟", "من المسجد الحرام إلى المسجد الأقصى (الإسراء ١)", "سورة الإسراء، الآية ١", ayah(17, 1)),
        Question("siyam", "في أي سورة فُرض الصيام؟", "سورة البقرة: ﴿كُتِبَ عَلَيْكُمُ الصِّيَامُ﴾ (١٨٣)", "سورة البقرة، الآية ١٨٣", ayah(2, 183)),
        Question("ramadan", "في أي شهر أُنزل القرآن؟", "شهر رمضان (البقرة ١٨٥)", "سورة البقرة، الآية ١٨٥", ayah(2, 185)),
        Question("qarib", "ماذا قال الله لمن سأل عنه؟", "﴿فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ﴾ (البقرة ١٨٦)", "سورة البقرة، الآية ١٨٦", ayah(2, 186)),
        Question("qawaid", "من رفع قواعد البيت الحرام؟", "إبراهيم وإسماعيل عليهما السلام (البقرة ١٢٧)", "سورة البقرة، الآية ١٢٧", ayah(2, 127)),
        Question("sharh_musa", "بماذا دعا موسى عليه السلام حين أُرسل إلى فرعون؟", "﴿رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي﴾ (طه ٢٥–٢٦)", "سورة طه، الآية ٢٥", ayah(20, 25)),
        Question("judi", "على أي جبل استوت سفينة نوح عليه السلام؟", "الجوديّ (هود ٤٤)", "سورة هود، الآية ٤٤", ayah(11, 44)),
        Question("nuh_years", "كم لبث نوح عليه السلام في قومه؟", "ألف سنة إلا خمسين عامًا (العنكبوت ١٤)", "سورة العنكبوت، الآية ١٤", ayah(29, 14)),
        Question("ayyub", "بماذا دعا أيوب عليه السلام ربه؟", "﴿أَنِّي مَسَّنِيَ الضُّرُّ وَأَنتَ أَرْحَمُ الرَّاحِمِينَ﴾ (الأنبياء ٨٣)", "سورة الأنبياء، الآية ٨٣", ayah(21, 83)),
        Question("zakariya", "بماذا دعا زكريا عليه السلام حين طلب الولد؟", "﴿رَبِّ لَا تَذَرْنِي فَرْدًا وَأَنتَ خَيْرُ الْوَارِثِينَ﴾ (الأنبياء ٨٩)", "سورة الأنبياء، الآية ٨٩", ayah(21, 89)),
        Question("yahya", "ما اسم الغلام الذي بُشّر به زكريا عليه السلام؟", "يحيى (مريم ٧)", "سورة مريم، الآية ٧", ayah(19, 7)),
        Question("mann", "ما الطعام الذي أنزله الله على بني إسرائيل؟", "المنّ والسلوى (البقرة ٥٧)", "سورة البقرة، الآية ٥٧", ayah(2, 57)),
        Question("naml_ant", "ماذا قالت النملة لقومها حين رأت جيش سليمان؟", "﴿ادْخُلُوا مَسَاكِنَكُمْ لَا يَحْطِمَنَّكُمْ سُلَيْمَانُ وَجُنُودُهُ﴾ (النمل ١٨)", "سورة النمل، الآية ١٨", ayah(27, 18)),
        Question("hudhud", "أيّ طائر تفقّده سليمان عليه السلام فلم يجده؟", "الهدهد (النمل ٢٠)", "سورة النمل، الآية ٢٠", ayah(27, 20)),
        Question("ankabut", "ما أوهن البيوت كما في القرآن؟", "بيت العنكبوت (العنكبوت ٤١)", "سورة العنكبوت، الآية ٤١", ayah(29, 41)),
        Question("hud_qawm", "إلى أي قوم أُرسل هود عليه السلام؟", "إلى عاد (هود ٥٠)", "سورة هود، الآية ٥٠", ayah(11, 50)),
        Question("salih_qawm", "إلى أي قوم أُرسل صالح عليه السلام؟", "إلى ثمود (هود ٦١)", "سورة هود، الآية ٦١", ayah(11, 61)),
        Question("shuayb_qawm", "إلى أي قوم أُرسل شعيب عليه السلام؟", "إلى مدين (هود ٨٤)", "سورة هود، الآية ٨٤", ayah(11, 84)),
        Question("khushu", "ما أول صفة للمؤمنين المفلحين في سورة المؤمنون؟", "﴿الَّذِينَ هُمْ فِي صَلَاتِهِمْ خَاشِعُونَ﴾ (٢)", "سورة المؤمنون، الآية ٢", ayah(23, 2)),
        Question("khuluq", "بماذا وصف الله خُلق نبيه ﷺ؟", "﴿وَإِنَّكَ لَعَلَىٰ خُلُقٍ عَظِيمٍ﴾ (القلم ٤)", "سورة القلم، الآية ٤", ayah(68, 4)),
        Question("salat_nabi", "ما الآية التي تأمرنا بالصلاة على النبي ﷺ؟", "﴿إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ…﴾ (الأحزاب ٥٦)", "سورة الأحزاب، الآية ٥٦", ayah(33, 56)),
        Question("nas", "ما آخر سورة في المصحف؟", "سورة الناس", "سورة الناس", ayah(114, 1)),
        Question("yasin_man", "في سورة يس، بماذا نادى الرجل الذي جاء من أقصى المدينة قومه؟", "﴿يَا قَوْمِ اتَّبِعُوا الْمُرْسَلِينَ﴾ (يس ٢٠)", "سورة يس، الآية ٢٠", ayah(36, 20)),
        Question("dhulqarnayn", "في أي سورة قصة ذي القرنين؟", "سورة الكهف (٨٣)", "سورة الكهف، الآية ٨٣", ayah(18, 83)),
        Question("khidr", "مع من سافر موسى عليه السلام ليتعلّم في سورة الكهف؟", "مع عبد صالح آتاه الله رحمة وعلمًا، وهو الخضر كما في صحيح البخاري (الكهف ٦٥)", "سورة الكهف، الآية ٦٥", ayah(18, 65)),
        Question("rukub", "ما الدعاء القرآني عند الركوب؟", "﴿سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ﴾ (الزخرف ١٣)", "سورة الزخرف، الآية ١٣", ayah(43, 13)),
        Question("abwab", "كم عدد أبواب جهنم كما في سورة الحجر؟", "سبعة أبواب (الحجر ٤٤)", "سورة الحجر، الآية ٤٤", ayah(15, 44)),
        Question("muhammad", "ما السورة التي سُمّيت باسم النبي ﷺ؟", "سورة محمد (٤٧)", "سورة محمد، الآية ٢", ayah(47, 2)),
        Question("ahmad", "على لسان من ذُكر اسم «أحمد» في القرآن؟", "على لسان عيسى عليه السلام (الصف ٦)", "سورة الصف، الآية ٦", ayah(61, 6)),
        Question("mahd", "ماذا قال عيسى عليه السلام وهو في المهد؟", "﴿إِنِّي عَبْدُ اللَّهِ آتَانِيَ الْكِتَابَ وَجَعَلَنِي نَبِيًّا﴾ (مريم ٣٠)", "سورة مريم، الآية ٣٠", ayah(19, 30)),
        Question("ghiba", "ما الآية التي تنهى عن الغيبة؟", "﴿وَلَا يَغْتَب بَّعْضُكُم بَعْضًا﴾ (الحجرات ١٢)", "سورة الحجرات، الآية ١٢", ayah(49, 12)),
        Question("akram", "من أكرم الناس عند الله؟", "﴿إِنَّ أَكْرَمَكُمْ عِندَ اللَّهِ أَتْقَاكُمْ﴾ (الحجرات ١٣)", "سورة الحجرات، الآية ١٣", ayah(49, 13)),
        Question("itmi", "بماذا تطمئن القلوب؟", "﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾ (الرعد ٢٨)", "سورة الرعد، الآية ٢٨", ayah(13, 28)),
        Question("shukr", "ما وعد الله لمن شكر؟", "﴿لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ﴾ (إبراهيم ٧)", "سورة إبراهيم، الآية ٧", ayah(14, 7)),
        Question("qunut", "ما الآية التي تنهى عن اليأس من رحمة الله؟", "﴿لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ ۚ إِنَّ اللَّهَ يَغْفِرُ الذُّنُوبَ جَمِيعًا﴾ (الزمر ٥٣)", "سورة الزمر، الآية ٥٣", ayah(39, 53)),
        Question("shuhur", "كم عدد الشهور عند الله، وكم منها حُرُم؟", "اثنا عشر شهرًا، منها أربعة حُرُم (التوبة ٣٦)", "سورة التوبة، الآية ٣٦", ayah(9, 36)),
        Question("taklim", "أي الأنبياء كلّمه الله تكليمًا؟", "موسى عليه السلام (النساء ١٦٤)", "سورة النساء، الآية ١٦٤", ayah(4, 164)),
        Question("zabur", "أي الأنبياء آتاه الله الزبور؟", "داود عليه السلام (النساء ١٦٣)", "سورة النساء، الآية ١٦٣", ayah(4, 163)),
        Question("rih", "لمن سخّر الله الريح تجري بأمره؟", "لسليمان عليه السلام (ص ٣٦)", "سورة ص، الآية ٣٦", ayah(38, 36)),
        Question("nakhla", "ماذا أُمرت مريم أن تفعل بجذع النخلة؟", "﴿وَهُزِّي إِلَيْكِ بِجِذْعِ النَّخْلَةِ تُسَاقِطْ عَلَيْكِ رُطَبًا جَنِيًّا﴾ (مريم ٢٥)", "سورة مريم، الآية ٢٥", ayah(19, 25)),
        Question("taawun", "على ماذا أمرنا الله أن نتعاون؟", "﴿وَتَعَاوَنُوا عَلَى الْبِرِّ وَالتَّقْوَىٰ﴾ (المائدة ٢)", "سورة المائدة، الآية ٢", ayah(5, 2)),
        Question("akmal", "في أي آية أخبر الله بإكمال الدين؟", "﴿الْيَوْمَ أَكْمَلْتُ لَكُمْ دِينَكُمْ﴾ (المائدة ٣)", "سورة المائدة، الآية ٣", ayah(5, 3)),
        Question("layl", "بماذا أُمر النبي ﷺ في أول سورة المزمل؟", "بقيام الليل: ﴿قُمِ اللَّيْلَ إِلَّا قَلِيلًا﴾ (المزمل ٢)", "سورة المزمل، الآية ٢", ayah(73, 2)),
        Question("hasanat", "ما الذي يُذهب السيئات؟", "﴿إِنَّ الْحَسَنَاتِ يُذْهِبْنَ السَّيِّئَاتِ﴾ (هود ١١٤)", "سورة هود، الآية ١١٤", ayah(11, 114)),
        Question("warid", "الله أقرب إلى الإنسان من ماذا؟", "﴿وَنَحْنُ أَقْرَبُ إِلَيْهِ مِنْ حَبْلِ الْوَرِيدِ﴾ (ق ١٦)", "سورة ق، الآية ١٦", ayah(50, 16)),
        Question("ibada", "لماذا خلق الله الجن والإنس؟", "﴿وَمَا خَلَقْتُ الْجِنَّ وَالْإِنسَ إِلَّا لِيَعْبُدُونِ﴾ (الذاريات ٥٦)", "سورة الذاريات، الآية ٥٦", ayah(51, 56)),
        Question("saba", "في أي سورة قصة ملكة سبأ مع سليمان عليه السلام؟", "سورة النمل (٢٣ وما بعدها)", "سورة النمل، الآية ٢٣", ayah(27, 23)),
        Question("sajda_last", "في أي سورة آخر سجدة تلاوة في المصحف؟", "سورة العلق: ﴿وَاسْجُدْ وَاقْتَرِب﴾ (١٩)", "سورة العلق، الآية ١٩", ayah(96, 19)),
        Question("samad", "ما معنى «الصمد» في سورة الإخلاص؟", "السيّد الذي يُقصد في الحوائج كلها (من كتب التفسير)", "تفسير سورة الإخلاص، الآية ٢", { Nav.openQuran(112, 2, sheet = true) }),
        Question("kawthar_mean", "ما «الكوثر» الذي أُعطيه النبي ﷺ؟", "نهر في الجنة وعده الله نبيه ﷺ، كما في صحيح مسلم (من كتب التفسير)", "تفسير سورة الكوثر، الآية ١", { Nav.openQuran(108, 1, sheet = true) }),
        Question("meem_ba", "ما حكم الميم الساكنة إذا جاء بعدها باء؟", "الإخفاء الشفوي", "تعلّم التجويد: أحكام الميم الساكنة", page("tajweed")),
        Question("qalqala", "ما حروف القلقلة؟", "خمسة يجمعها «قطب جد»", "تعلّم التجويد: القلقلة", page("tajweed")),
        Question("halq", "ما حروف الإظهار الحلقي؟", "ستة: الهمزة والهاء والعين والحاء والغين والخاء", "تعلّم التجويد: النون الساكنة والتنوين", page("tajweed")),
        Question("lazim", "كم مقدار المدّ اللازم؟", "ست حركات", "تعلّم التجويد: المدود", page("tajweed")),
        Question("ruku", "ماذا يقول المصلي في ركوعه؟", "«سبحان ربي العظيم»", "صفة الوضوء والصلاة", page("salah")),
        Question("sujud", "ماذا يقول المصلي في سجوده؟", "«سبحان ربي الأعلى»", "صفة الوضوء والصلاة", page("salah")),
    )
    fun question(): Question = bag("q", questions.map { it.id }, 1).firstOrNull()?.let { id -> questions.firstOrNull { it.id == id } } ?: questions.first()
    const val QUESTION_BOND = 6
    private const val READ_MS = 15_000L
    fun answered() = sp.getString("q_done", "") == today().toString()
    /** Opens where the answer is; reading it there (15 seconds or more) shows the answer and gives the points. */
    fun seek() { sp.edit().putLong("q_open_at", System.currentTimeMillis()).putString("q_open_id", question().id).apply(); question().open() }
    /** Back from looking: was it long enough? */
    fun checkAnswer(): Boolean {
        if (answered()) return true
        val at = sp.getLong("q_open_at", 0L); if (at == 0L || sp.getString("q_open_id", "") != question().id) return false
        if (System.currentTimeMillis() - at < READ_MS) return false
        sp.edit().putString("q_done", today().toString()).remove("q_open_at").putInt("q_count", sp.getInt("q_count", 0) + 1).apply()
        Progress.bond("question", QUESTION_BOND)
        Pet.cheer("أحسنت! عرفت الجواب 💡", key = "question", xp = false)
        checkFullDay()
        return true
    }
    fun lookedTooShort() = !answered() && sp.getLong("q_open_at", 0L) > 0L && sp.getString("q_open_id", "") == question().id

    // ------------------------------------------------------------ the week's chest
    const val CHEST_BOND = 20
    private fun weekStart() = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
    /** A full day: every task, every challenge and the question. */
    fun fullToday() = tasks().all { runCatching { it.done() }.getOrDefault(false) } && (0..2).all { challengeDone(it) } && answered()
    fun fullDays(): Int { val ws = weekStart(); return (sp.getStringSet("miss_full", emptySet()) ?: emptySet()).count { runCatching { !LocalDate.parse(it).isBefore(ws) }.getOrDefault(false) } }
    fun checkFullDay() {
        if (!fullToday()) return
        val s = (sp.getStringSet("miss_full", emptySet()) ?: emptySet()).toMutableSet(); if (!s.add(today().toString())) return
        sp.edit().putStringSet("miss_full", s.filter { runCatching { LocalDate.parse(it).isAfter(today().minusDays(30)) }.getOrDefault(false) }.toSet()).apply()
        if (fullDays() >= 5 && sp.getString("miss_chest", "") != weekStart().toString()) {
            sp.edit().putString("miss_chest", weekStart().toString()).putInt("miss_chests", sp.getInt("miss_chests", 0) + 1).apply()
            Progress.bond("chest", CHEST_BOND, capped = false)
            Pet.cheer("🎁 فتحت صندوق الأسبوع! أسبوع جميل معًا", key = "chest", xp = false)
        }
        Store.bump()
    }
    fun chestOpened() = sp.getString("miss_chest", "") == weekStart().toString()
}

/** «يومي مع صديقي»: the daily tasks, the challenges, the question and the week's chest (❤ only). */
@Composable
fun MissionsPanel() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    LaunchedEffect(v) { Missions.collectTasks(); Missions.checkAnswer(); Missions.checkFullDay() }
    Text("${Friendship.heart} يومي مع ${Pet.name}", color = P.gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Sub("كلها تزيد نقاط الصداقة فقط. تتجدّد كل يوم.", size = 11.sp)

    Text("الوظائف اليومية · +${Quran.digits(Missions.TASK_BOND)} ❤ لكل واحدة", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
    Sub("يتحقق منها التطبيق بنفسه.", size = 11.sp)
    Missions.tasks().forEach { t ->
        val done = runCatching { t.done() }.getOrDefault(false)
        MissionRow(t.icon, t.text, done, if (done) "✓" else "اذهب ←") { if (!done) t.go() }
    }

    Text("التحديات · +${Quran.digits(Missions.CHALLENGE_BOND)} ❤ لكل تحدٍّ", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
    Missions.challenges().forEachIndexed { i, c ->
        val done = Missions.challengeDone(i)
        MissionRow("🎯", c, done, if (done) "✓" else "فعلتُه") { Missions.doChallenge(i) }
    }

    val q = Missions.question()
    Text("سؤال اليوم · +${Quran.digits(Missions.QUESTION_BOND)} ❤", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
    Column(Modifier.fillMaxWidth().padding(top = 4.dp).clip(L.shape(12.dp)).background(P.card).padding(10.dp)) {
        Text("💡 ${q.q}", color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (Missions.answered()) {
            Text(q.a, color = P.accent, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        } else {
            Sub("ابحث عن الجواب في: ${q.where}. اقرأه هناك ربع دقيقة ثم ارجع هنا.", size = 12.sp)
            if (Missions.lookedTooShort()) Sub("لم تكمل ربع دقيقة هناك؛ ارجع واقرأ قليلًا.", size = 11.sp, color = P.danger)
            Action("🔎 ابحث عن الجواب", Modifier.fillMaxWidth().padding(top = 8.dp)) { Missions.seek() }
        }
    }

    val n = Missions.fullDays()
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).clip(L.shape(12.dp)).background(if (Missions.chestOpened()) P.gold.copy(alpha = .14f) else P.card).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (Missions.chestOpened()) "🎁" else "🧰", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
        Column(Modifier.weight(1f)) {
            Text("صندوق الأسبوع · +${Quran.digits(Missions.CHEST_BOND)} ❤", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Sub(if (Missions.chestOpened()) "فُتح هذا الأسبوع ✓" else "أتمم يومك كاملًا (الوظائف والتحديات والسؤال) خمسة أيام في الأسبوع: ${Quran.digits(n.coerceAtMost(5))} من ٥", size = 11.sp)
        }
    }
}

@Composable
private fun MissionRow(icon: String, text: String, done: Boolean, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp).clip(L.shape(12.dp)).background(if (done) P.accent.copy(alpha = .10f) else P.card)
        .clickable(enabled = !done, onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(icon, fontSize = 18.sp)
        Text(text, color = if (done) P.muted else P.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(action, color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
