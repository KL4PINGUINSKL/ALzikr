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
    private fun pick(n: Int, size: Int, salt: Int): List<Int> {
        val d = today(); val start = (d.dayOfYear * 7 + d.year * 3 + salt) % size
        val step = listOf(5, 7, 11, 13).first { size % it != 0 }
        return (0 until n).map { (start + it * step) % size }.distinct()
    }

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
    )
    fun tasks(): List<Task> = pick(3, allTasks.size, 1).map { allTasks[it] }
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
    )
    fun challenges(): List<String> = pick(3, challengeList.size, 0).map { challengeList[it] }
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
    )
    fun question(): Question = questions[pick(1, questions.size, 2).first()]
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
