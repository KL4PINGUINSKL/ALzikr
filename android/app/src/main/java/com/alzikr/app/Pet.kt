package com.alzikr.app

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime

/**
 * The little friend: which animal, its name, adults or children, where it lives in the app, and its
 * floating twin over other apps. Its words: the prayer close, the dhikr of the moment, the wird,
 * a gentle reminder; for children, encouragement and lessons.
 */
object Pet {
    private val sp get() = Store.prefs
    var species: Species?
        get() = sp.getString("pet_species", null)?.let { runCatching { Species.valueOf(it) }.getOrNull() }
        set(v) { sp.edit().putString("pet_species", v?.name).apply(); if (v != null && since == 0L) since = System.currentTimeMillis(); Store.bump() }
    /** Each friend keeps its own name. */
    fun nameOf(s: Species) = sp.getString("pet_name_${s.name}", null)?.takeIf { it.isNotBlank() }
        // The first test build kept one name for all: it belongs to the friend chosen then.
        ?: sp.getString("pet_name", null)?.takeIf { it.isNotBlank() && s == species }?.also { setName(s, it); sp.edit().remove("pet_name").apply() }
        ?: defaultName(s)
    fun setName(s: Species, v: String) { sp.edit().putString("pet_name_${s.name}", v.trim().take(20)).apply(); Store.bump() }
    var name: String
        get() = species?.let { nameOf(it) } ?: "صديقي"
        set(v) { species?.let { setName(it, v) } }
    /** Line drawing in the theme's color for adults; the colorful drawing for children. */
    fun lineStyle(ink: androidx.compose.ui.graphics.Color, paper: androidx.compose.ui.graphics.Color) = if (kids) null else PetLine(ink, paper)

    /** How the day went, 0..100: prayers marked so far, the morning/evening adhkar, the wird, some tasbih. */
    fun mood(): Int {
        var m = 0
        if (Store.hasLocation) {
            val t = Store.times(); val now = LocalTime.now().let { it.hour * 60 + it.minute }
            val due = Prayer.entries.filter { it.alertable && t.getValue(it) + 20 <= now }
            m += if (due.isEmpty()) 25 else 50 * due.count { Data.prayed(LocalDate.now(), it) } / due.size
        }
        if (Data.colDone("morning")) m += 12
        if (Data.colDone("evening")) m += 13
        if (Data.wirdToday() > 0 && Data.pagesToday() >= Data.wirdToday()) m += 15
        if ((Data.tasbihHistory(1).firstOrNull()?.second ?: 0) >= 33) m += 10
        return m.coerceIn(0, 100)
    }
    /** Something good was just done: the friend hops and says so. */
    fun cheer(text: String = "ما شاء الله، أحسنت!", key: String = text, xp: Boolean = true) {
        sp.edit().putLong("pet_cheer_at", System.currentTimeMillis()).putString("pet_cheer", text).apply()
        if (xp) gain(key, 10)
        runCatching { noteGoodDay() }
        Store.bump()
    }

    // ---- friendship: experience from good deeds (each deed counts once a day), levels and titles
    val xp: Int get() = Progress.xp
    /** Adds [amount] once per day for each [key], so marking and unmarking a prayer does not count twice. */
    @Suppress("UNUSED_PARAMETER")
    fun gain(key: String, amount: Int) { Progress.record(Progress.Kind.DEED, key) }
    fun level(x: Int = xp) = Progress.level(x)
    /** Experience where a level starts. */
    fun levelStart(l: Int) = Progress.need(l)
    fun title(l: Int = level()) = Progress.title(l)
    /** What the friend calls the user (optional; used in its morning greeting from level 4). */
    var userName: String
        get() = sp.getString("pet_user_name", "") ?: ""
        set(v) { sp.edit().putString("pet_user_name", v.trim().take(20)).apply(); Store.bump() }
    /** When the friendship began (the first friend chosen). */
    var since: Long
        get() = sp.getLong("pet_since", 0L)
        set(v) { sp.edit().putLong("pet_since", v).apply() }
    fun daysTogether() = if (since == 0L) 0 else (java.time.temporal.ChronoUnit.DAYS.between(java.time.Instant.ofEpochMilli(since).atZone(java.time.ZoneId.systemDefault()).toLocalDate(), LocalDate.now()) + 1).toInt()
    fun cheerNow(): String? = sp.getString("pet_cheer", null)?.takeIf { System.currentTimeMillis() - sp.getLong("pet_cheer_at", 0) < 6000 }
    fun defaultName(s: Species) = when (s) { Species.CAT -> "لؤلؤة"; Species.PENGUIN -> "بطبوط"; Species.ALBATROSS -> "رحّال"; Species.SWALLOW -> "سنونة"; Species.RABBIT -> "قطن"; Species.SHEEP -> "غيمة"; Species.CHICKEN -> "كتكوتة"; Species.COW -> "زهرة"; Species.ELEPHANT -> "فيلو"; Species.GIRAFFE -> "طويلة"; Species.BIRD -> "زقزوق"; Species.BEAR -> "دبدوب"; Species.PANDA -> "بمبو" }
    /** For children: encouragement and lessons instead of reminders. */
    var kids: Boolean
        get() = Data.look == "kids" || sp.getBoolean("pet_kids", false)
        set(v) { sp.edit().putBoolean("pet_kids", v).apply(); Store.bump() }
    /** "top" (in the header), "ground" (a strip above the tab bar) or "off". */
    var place: String
        get() = sp.getString("pet_place", "top") ?: "top"
        set(v) { sp.edit().putString("pet_place", v).apply(); Store.bump() }
    /** Speaks by itself now and then while the app is open. */
    var talks: Boolean
        get() = sp.getBoolean("pet_talks", true)
        set(v) { sp.edit().putBoolean("pet_talks", v).apply(); Store.bump() }
    var introSeen: Boolean
        get() = sp.getBoolean("pet_intro", false)
        set(v) { sp.edit().putBoolean("pet_intro", v).apply(); Store.bump() }
    var stars: Int
        get() = sp.getInt("pet_stars", 0)
        set(v) { sp.edit().putInt("pet_stars", v).apply(); Store.bump() }

    // ---- over other apps
    var floating: Boolean
        get() = sp.getBoolean("pet_float", false)
        set(v) { sp.edit().putBoolean("pet_float", v).apply(); Store.bump() }
    /** Minutes between bubbles over other apps. */
    var every: Int
        get() = sp.getInt("pet_every", 60)
        set(v) { sp.edit().putInt("pet_every", v.coerceIn(5, 600)).apply(); Store.bump() }
    /** "mix", "dhikr", "ayah", "hadith". */
    var content: String
        get() = sp.getString("pet_content", "mix") ?: "mix"
        set(v) { sp.edit().putString("pet_content", v).apply(); Store.bump() }
    var floatSize: Int
        get() = sp.getInt("pet_fsize", 64)
        set(v) { sp.edit().putInt("pet_fsize", v).apply(); Store.bump() }
    var floatAlpha: Float
        get() = sp.getFloat("pet_falpha", .9f)
        set(v) { sp.edit().putFloat("pet_falpha", v).apply(); Store.bump() }
    var floatX: Int
        get() = sp.getInt("pet_fx", -1)
        set(v) { sp.edit().putInt("pet_fx", v).apply() }
    var floatY: Int
        get() = sp.getInt("pet_fy", 400)
        set(v) { sp.edit().putInt("pet_fy", v).apply() }
    /** Over other apps it keeps moving (off: a still picture that moves only when it speaks). */
    var floatMove: Boolean
        get() = sp.getBoolean("pet_fmove", true)
        set(v) { sp.edit().putBoolean("pet_fmove", v).apply(); Store.bump() }
    /** Now and then it strolls up or down along its edge. */
    var wander: Boolean
        get() = sp.getBoolean("pet_wander", true)
        set(v) { sp.edit().putBoolean("pet_wander", v).apply(); Store.bump() }
    /** Two taps on it over other apps = one tasbih. */
    var floatTasbih: Boolean
        get() = sp.getBoolean("pet_ftasbih", true)
        set(v) { sp.edit().putBoolean("pet_ftasbih", v).apply(); Store.bump() }
    /** Over other apps: a word ten minutes before each prayer. */
    var prayerNudge: Boolean
        get() = sp.getBoolean("pet_pnudge", true)
        set(v) { sp.edit().putBoolean("pet_pnudge", v).apply(); Store.bump() }
    /** The tasbih phrase for count [n]: 33 subhan Allah, 33 al-hamdu lillah, 34 Allahu akbar, then again. */
    fun tasbihWord(n: Int) = ((n - 1) % 100).let { if (it < 33) "سبحان الله" else if (it < 66) "الحمد لله" else "الله أكبر" }
    /** Quiet hours: no bubbles between these minutes (e.g. 23:00–05:00); -1 = none. */
    var quietFrom: Int
        get() = sp.getInt("pet_qfrom", 23 * 60)
        set(v) { sp.edit().putInt("pet_qfrom", v).apply(); Store.bump() }
    var quietTo: Int
        get() = sp.getInt("pet_qto", 5 * 60)
        set(v) { sp.edit().putInt("pet_qto", v).apply(); Store.bump() }
    fun quietNow(): Boolean {
        if (quietFrom < 0) return false
        val m = LocalTime.now().let { it.hour * 60 + it.minute }
        return if (quietFrom <= quietTo) m in quietFrom until quietTo else m >= quietFrom || m < quietTo
    }
    var hiddenUntil: Long
        get() = sp.getLong("pet_hidden_until", 0L)
        set(v) { sp.edit().putLong("pet_hidden_until", v).apply(); Store.bump() }

    // ---- good days in a row: a day counts once its mood reaches 60
    fun noteGoodDay() {
        if (mood() < 60) return
        val d = LocalDate.now().toString()
        val set = sp.getStringSet("pet_good_days", emptySet())!!
        if (d in set) return
        val keep = LocalDate.now().minusDays(400).toString()
        sp.edit().putStringSet("pet_good_days", (set.filter { it >= keep } + d).toSet()).apply()
    }
    /** Consecutive good days up to today (or up to yesterday, if today is not good yet). */
    fun streak(): Int {
        val set = sp.getStringSet("pet_good_days", emptySet())!!
        var d = LocalDate.now(); if (d.toString() !in set) d = d.minusDays(1)
        var n = 0
        while (d.toString() in set) { n++; d = d.minusDays(1) }
        return n
    }
    fun bestStreak(): Int {
        val days = sp.getStringSet("pet_good_days", emptySet())!!.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
        var best = 0; var cur = 0; var prev: LocalDate? = null
        days.forEach { d -> cur = if (prev?.plusDays(1) == d) cur + 1 else 1; best = maxOf(best, cur); prev = d }
        return best
    }

    // ---- the day's small challenge: one good deed suggested each day
    private val challenges = listOf(
        "صلِّ ركعتي الضحى", "تصدّق اليوم ولو بشيء قليل", "صِل أحد أرحامك واسأل عن حاله", "اقرأ صفحة زائدة على وردك",
        "قل «سبحان الله وبحمده» مئة مرة", "استغفر الله مئة مرة", "صلِّ على النبي ﷺ أكثر من عادتك", "صلِّ السنن الرواتب كلها",
        "صلِّ الوتر قبل أن تنام", "ابدأ من تلقاه بالسلام وابتسم في وجهه", "ادعُ لأخ لك بظهر الغيب", "احفظ حديثًا جديدًا من «حديث اليوم»",
        "سبّح وحمّد وكبّر ثلاثًا وثلاثين بعد كل صلاة", "أمِط أذى عن الطريق أو أعِن أحدًا في حاجته", "احفظ لسانك اليوم من الغيبة",
        "اقرأ أذكار الصباح والمساء كاملة", "تعلّم مسألة فقهية جديدة من «الأحكام الفقهية»", "اقرأ تفسير آية من وردك",
    )
    private val kidsChallenges = listOf(
        "ساعد أمك أو أباك في شيء", "قل «بسم الله» قبل الأكل و«الحمد لله» بعده", "رتّب غرفتك وحدك", "قل «السلام عليكم» لأهل البيت",
        "احفظ آية جديدة", "صلِّ الصلوات الخمس في وقتها", "قل «جزاك الله خيرًا» لمن يساعدك", "اقرأ أذكار النوم قبل أن تنام",
        "ابتسم لإخوتك وشاركهم ألعابك", "تعلّم حرفًا جديدًا في قسم الأطفال",
    )
    fun challenge(): String = LocalDate.now().let { d -> if (kids) kidsChallenges[d.dayOfYear % kidsChallenges.size] else challenges[(d.dayOfYear * 7 + d.year) % challenges.size] }
    val challengeDone: Boolean get() = sp.getString("pet_ch_done", "") == LocalDate.now().toString()
    fun doChallenge() {
        sp.edit().putString("pet_ch_done", LocalDate.now().toString()).apply()
        if (kids) stars += 2
        sp.edit().putInt("pet_ch_count", sp.getInt("pet_ch_count", 0) + 1).apply()
        // A challenge grows the friendship (❤), not the experience.
        cheer("ما شاء الله! أتممت تحدي اليوم", key = "challenge", xp = false)
        Progress.bond("challenge", 5)
    }

    // ---- mood and sleep
    /** Happy when today's prayers so far are marked and the adhkar of the time are done. */
    fun happy(): Boolean = mood() >= 60
    fun asleep(): Boolean {
        if (!Store.hasLocation) return false
        val t = Store.times(); val m = LocalTime.now().let { it.hour * 60 + it.minute }
        return m >= t.getValue(Prayer.ISHA) + 150 || m < t.getValue(Prayer.FAJR) - 20
    }

    // ---- what it says
    data class Say(val text: String, val go: (() -> Unit)? = null)
    private val dhikr = listOf(
        "سبحان الله وبحمده، سبحان الله العظيم", "لا تنسَ ذكر الله", "لا إله إلا الله وحده لا شريك له",
        "اللهم صلِّ وسلم على نبينا محمد", "أستغفر الله وأتوب إليه", "لا حول ولا قوة إلا بالله",
        "الحمد لله على كل حال", "سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر", "حسبي الله لا إله إلا هو عليه توكلت",
    )
    val hadiths = listOf(
        "«أحب الأعمال إلى الله أدومها وإن قلّ» (البخاري ومسلم)",
        "«الكلمة الطيبة صدقة» (البخاري ومسلم)",
        "«تبسّمك في وجه أخيك صدقة» (الترمذي، وصححه الألباني)",
        "«من كان يؤمن بالله واليوم الآخر فليقل خيرًا أو ليصمت» (البخاري ومسلم)",
        "«لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه» (البخاري ومسلم)",
        "«المسلم من سلم المسلمون من لسانه ويده» (البخاري ومسلم)",
        "«إنما الأعمال بالنيات» (البخاري ومسلم)",
        "«الطهور شطر الإيمان» (مسلم)",
        "«خيركم من تعلّم القرآن وعلّمه» (البخاري)",
        "«كلمتان خفيفتان على اللسان، ثقيلتان في الميزان: سبحان الله وبحمده، سبحان الله العظيم» (البخاري ومسلم)",
        "«ليس الشديد بالصُّرَعة، إنما الشديد الذي يملك نفسه عند الغضب» (البخاري ومسلم)",
        "«من سلك طريقًا يلتمس فيه علمًا سهّل الله له به طريقًا إلى الجنة» (مسلم)",
        "«اتق الله حيثما كنت، وأتبع السيئة الحسنة تمحها، وخالق الناس بخلق حسن» (الترمذي، وحسّنه)",
        "«الدال على الخير كفاعله» (الترمذي، وصححه الألباني)",
        "«يسّروا ولا تعسّروا، وبشّروا ولا تنفّروا» (البخاري ومسلم)",
    )
    private val kidsWords = listOf(
        "هيا نتعلم شيئًا جديدًا!", "قل: بسم الله قبل أن تأكل", "قل: الحمد لله بعد الأكل",
        "هل صلّيت؟ أنا أحب الذين يصلّون!", "ابتسم، فالابتسامة صدقة", "هل قرأت سورة اليوم؟",
        "قل: سبحان الله، ثلاث مرات", "أطِع والديك، فهذا يحبه الله", "قل: السلام عليكم لمن تلقاه",
    )

    /** Something fitting for now, the most useful first, otherwise a gentle reminder. */
    fun say(n: Int = 0): Say {
        if (kids) {
            if (n % 3 == 0) return Say("هيا نتعلم معًا! عندك ${stars} نجمة ⭐") { Nav.morePage = "kids"; Nav.go("more") }
            return Say(kidsWords[(n + LocalTime.now().minute) % kidsWords.size])
        }
        val hm = LocalTime.now().let { it.hour * 60 + it.minute }
        if (n % 4 == 0 && Progress.has(4) && hm in 300..600 && sp.getString("pet_greet_day", "") != LocalDate.now().toString()) {
            sp.edit().putString("pet_greet_day", LocalDate.now().toString()).apply()
            return Say("صباح الخير${userName.takeIf { it.isNotBlank() }?.let { " يا $it" } ?: ""} ☀ يوم مبارك، ابدأه بأذكار الصباح") { Nav.collection = "morning"; Nav.go("adhkar") }
        }
        if (asleep()) return Say("تصبح على خير… لا تنسَ أذكار النوم") { Nav.collection = "sleep"; Nav.go("adhkar") }
        val opts = ArrayList<Say>()
        if (Store.hasLocation) {
            val w = window()
            val left = ((w.nextAt - System.currentTimeMillis()) / 60_000L).toInt()
            if (w.next.alertable && left in 0..15) opts += Say("بقي $left دقيقة على صلاة ${w.next.ar}، استعد لها") { Nav.prayerSheet = w.next; Nav.go("prayer") }
            val cur = w.current
            val m = LocalTime.now().let { it.hour * 60 + it.minute }
            if (cur != null && cur.alertable && cur != w.next && !Data.prayed(LocalDate.now(), cur) && m - Store.times().getValue(cur) in 20..300)
                opts += Say("هل صلّيت ${cur.ar}؟ علّمها ✓") { Nav.prayerSheet = cur; Nav.go("prayer") }
        }
        Moments.now().firstOrNull { !Moments.done(it) }?.let { m -> opts += Say("حان وقت: ${m.title}", m.go) }
        Data.wirdLeftToday().takeIf { it > 0 && Data.khatmaPage < Data.PAGES }?.let { opts += Say("بقي من وردك اليوم $it صفحات") { Nav.wird = true } }
        opts += Say(dhikr[(n + LocalTime.now().minute) % dhikr.size])
        return opts[n % opts.size]
    }

    /** A bubble over other apps: dhikr, a short verse or a hadith (by the user's choice). */
    fun floatText(c: Context, n: Int): Pair<String, String?> {
        val kind = when (content) { "dhikr" -> 0; "ayah" -> 1; "hadith" -> 2; else -> n % 3 }
        if (kind == 1) runCatching {
            Quran.load(c.applicationContext)
            val short = Quran.ayahs.filter { it.plain.length in 30..120 }
            val a = short[((System.currentTimeMillis() / 60_000L) % short.size).toInt()]
            return "${a.text}\n— ${Quran.surahs[a.s - 1].name} ${Quran.digits(a.a)}" to "${a.s}:${a.a}"
        }
        if (kind == 2) return hadiths[(n + LocalDate.now().dayOfYear) % hadiths.size] to null
        return dhikr[(n + LocalTime.now().hour) % dhikr.size] to null
    }

    // ---- children's lessons
    data class Q(val q: String, val choices: List<String>, val answer: Int, val why: String)
    val lessons: List<Pair<String, List<Q>>> = listOf(
        "ديني" to listOf(
            Q("كم عدد أركان الإسلام؟", listOf("ثلاثة", "خمسة", "سبعة"), 1, "الشهادتان، والصلاة، والزكاة، والصيام، والحج."),
            Q("ما أول أركان الإسلام؟", listOf("الصلاة", "الشهادتان", "الصيام"), 1, "أن تشهد أن لا إله إلا الله وأن محمدًا رسول الله."),
            Q("كم عدد أركان الإيمان؟", listOf("ستة", "أربعة", "عشرة"), 0, "الإيمان بالله، وملائكته، وكتبه، ورسله، واليوم الآخر، والقدر خيره وشره."),
            Q("كم صلاة نصلي في اليوم والليلة؟", listOf("ثلاث", "خمس", "سبع"), 1, "الفجر، والظهر، والعصر، والمغرب، والعشاء."),
            Q("في أي شهر نصوم؟", listOf("رجب", "رمضان", "شعبان"), 1, "صيام شهر رمضان ركن من أركان الإسلام."),
            Q("إلى أين نتجه في الصلاة؟", listOf("الكعبة", "الشمس", "أي جهة"), 0, "القبلة هي الكعبة المشرفة في مكة."),
        ),
        "أذكاري" to listOf(
            Q("ماذا تقول قبل الأكل؟", listOf("الحمد لله", "بسم الله", "سبحان الله"), 1, "«يا غلام، سمِّ الله، وكل بيمينك، وكل مما يليك» (البخاري ومسلم)."),
            Q("ماذا تقول بعد الأكل؟", listOf("الحمد لله", "الله أكبر", "أستغفر الله"), 0, "نحمد الله الذي أطعمنا."),
            Q("ماذا تقول إذا عطست؟", listOf("الحمد لله", "بسم الله", "آمين"), 0, "ويقول لك أخوك: يرحمك الله."),
            Q("بأي يد نأكل؟", listOf("اليسرى", "اليمنى", "لا فرق"), 1, "«وكل بيمينك» (البخاري ومسلم)."),
            Q("ماذا نقول إذا لقينا مسلمًا؟", listOf("مرحبًا فقط", "السلام عليكم", "لا شيء"), 1, "«أفشوا السلام بينكم» (مسلم)."),
            Q("ماذا تقول عند النوم؟", listOf("باسمك اللهم أموت وأحيا", "صباح الخير", "الله أكبر"), 0, "من أذكار النوم (البخاري)."),
        ),
        "قرآني" to listOf(
            Q("ما أول سورة في القرآن؟", listOf("الناس", "الفاتحة", "البقرة"), 1, "الفاتحة هي أم الكتاب."),
            Q("ما آخر سورة في القرآن؟", listOf("الناس", "الإخلاص", "الفلق"), 0, "سورة الناس."),
            Q("كم آية في سورة الفاتحة؟", listOf("خمس", "سبع", "عشر"), 1, "الفاتحة سبع آيات، وهي السبع المثاني."),
            Q("أي سورة تعدل ثلث القرآن؟", listOf("الإخلاص", "الكوثر", "العصر"), 0, "«قل هو الله أحد تعدل ثلث القرآن» (البخاري)."),
            Q("ما أقصر سورة في القرآن؟", listOf("الكوثر", "البقرة", "يس"), 0, "سورة الكوثر ثلاث آيات."),
            Q("كم عدد سور القرآن؟", listOf("١١٤", "١٠٠", "٣٠"), 0, "١١٤ سورة في ٣٠ جزءًا."),
        ),
        "الأنبياء" to listOf(
            Q("من هو خاتم الأنبياء؟", listOf("موسى", "محمد ﷺ", "عيسى"), 1, "محمد ﷺ آخر الأنبياء والرسل."),
            Q("من بنى الكعبة مع ابنه إسماعيل؟", listOf("إبراهيم", "نوح", "يوسف"), 0, "﴿وَإِذْ يَرْفَعُ إِبْرَاهِيمُ الْقَوَاعِدَ مِنَ الْبَيْتِ وَإِسْمَاعِيلُ﴾."),
            Q("من النبي الذي التقمه الحوت؟", listOf("يونس", "يعقوب", "صالح"), 0, "يونس عليه السلام، فسبّح الله فنجّاه."),
            Q("من صنع السفينة الكبيرة؟", listOf("نوح", "هود", "لوط"), 0, "نوح عليه السلام بأمر الله."),
            Q("من النبي الذي كلّمه الله؟", listOf("موسى", "داود", "إدريس"), 0, "﴿وَكَلَّمَ اللَّهُ مُوسَىٰ تَكْلِيمًا﴾."),
            Q("من أول الأنبياء؟", listOf("آدم", "إبراهيم", "سليمان"), 0, "آدم عليه السلام أبو البشر."),
        ),
    )
}
