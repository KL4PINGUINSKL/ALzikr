package com.alzikr.app

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * ❤ Friendship rewards: what time spent with the friend opens (visits, talks, play, the daily challenge), kept apart
 * from the ✨ level, which grows with worship and opens friends and looks. Friendship points never add to the level,
 * and the level never adds to friendship.
 *
 * Gifts are app things only: widget frames, themes, badges, and small ways the friend shows it knows you.
 */
object Friendship {
    enum class Kind(val ar: String) { FRAME("إطار"), THEME("ثيم"), BADGE("وسام"), PERK("ميزة") }
    data class Gift(val id: String, val bond: Int, val icon: String, val kind: Kind, val name: String, val about: String)

    val gifts: List<Gift> = listOf(
Gift("theme_madinah", 25, "🕌", Kind.THEME, "ثيم «المدينة»", "أخضر القبة وبياض الحرم"),
        Gift("frame_night", 50, "🖼", Kind.FRAME, "إطار «الليل المرصّع بالنجوم»", "لأداة صديقك على الشاشة الرئيسية"),
        Gift("theme_sea", 60, "🌊", Kind.THEME, "ثيم «البحر»", "أزرق عميق وزبد أبيض"),
        Gift("theme_lantern", 75, "🏮", Kind.THEME, "ثيم «مشكاة»", "قنديل معلّق يتوهّج ويتنفّس كاللهب، وجمرات صاعدة"),
        Gift("theme_fajr", 90, "🌅", Kind.THEME, "ثيم «الفجر»", "ألوان الشروق الهادئة"),
        Gift("greet", 100, "🌅", Kind.PERK, "تحية الصباح باسمك", "يحيّيك صديقك صباحًا باسمك ويذكّرك بأذكار الصباح"),
        Gift("theme_rose", 110, "🌸", Kind.THEME, "ثيم «ورد»", "وردي وعنابي بسبحة خرز"),
        Gift("frame_rose", 125, "🖼", Kind.FRAME, "إطار «الورد»", "وردي فاتح بإطار مزدوج"),
        Gift("theme_breeze", 150, "🎨", Kind.THEME, "ثيم «نسيم»", "نعناعي وسماوي هادئ"),
        Gift("theme_kaaba", 175, "🕋", Kind.THEME, "ثيم «الكعبة»", "سواد الكسوة وذهب الحزام"),
        Gift("badge_first", 200, "🎗", Kind.BADGE, "وسام «بداية الطريق»", "أول خطوات صداقتكما"),
        Gift("frame_gold", 225, "🖼", Kind.FRAME, "إطار «الذهبي»", "لأداة صديقك"),
        Gift("theme_garden", 250, "🌿", Kind.THEME, "ثيم «روضة»", "كروم وأوراق تتسلّق جانبي الشاشة وتتمايل"),
        Gift("theme_minimal", 275, "◻", Kind.THEME, "ثيم «بسيط»", "أبيض وأسود بخطوط فاصلة فقط"),
        Gift("joy", 300, "😊", Kind.PERK, "الفرح فوق التطبيقات", "يقفز صديقك العائم وتطير القلوب حين تمتلئ قلوب يومك"),
        Gift("theme_snow", 325, "❄", Kind.THEME, "ثيم «ثلج»", "ندف ثلج تتساقط على أزرق جليدي"),
        Gift("frame_ocean", 350, "🖼", Kind.FRAME, "إطار «البحر»", "أزرق عميق بموج متقطّع"),
        Gift("certificate", 375, "📜", Kind.PERK, "شهادة الصداقة", "صورة باسم صديقك وأيامكما تشاركها إن شئت"),
        Gift("theme_cactus", 400, "🌵", Kind.THEME, "ثيم «صبّار»", "تصميم جديد كليًا: صبّارات تُزهر بصلاتك وأقسام على قوس"),
        Gift("theme_amoled", 425, "⚫", Kind.THEME, "ثيم «أسود نقي»", "أسود حقيقي يوفّر البطارية"),
        Gift("nightdua", 450, "🌙", Kind.PERK, "دعاء المساء باسمك", "بعد العشاء يدعو لك صديقك باسمك ويذكّرك بأذكار النوم"),
        Gift("theme_dusk", 500, "🎨", Kind.THEME, "ثيم «غسق»", "بنفسجي المغيب وذهب خافت"),
        Gift("frame_desert", 525, "🖼", Kind.FRAME, "إطار «الكثبان»", "رملي دافئ"),
        Gift("theme_zellige", 575, "✵", Kind.THEME, "ثيم «زليج»", "شبكة نجوم ثمانية كبلاط الأندلس"),
        Gift("theme_paperworld", 600, "✂", Kind.THEME, "ثيم «عالم من الورق»", "طبقات ورق مقصوص بظلال حقيقية"),
        Gift("frame_pattern", 625, "🖼", Kind.FRAME, "إطار «الزخرفة»", "لأداة صديقك"),
        Gift("theme_c_arches", 650, "🕌", Kind.THEME, "ثيم «حديقة الأقواس»", "أقواس عالية وحديقة ونافورة وفوانيس"),
        Gift("theme_petals", 675, "🌸", Kind.THEME, "ثيم «بتلات»", "بتلات وردية تنساب مع النسيم"),
        Gift("frame_lantern", 700, "🖼", Kind.FRAME, "إطار «القنديل»", "كهرماني دافئ بنقاط ذهبية"),
        Gift("letter", 750, "💌", Kind.PERK, "رسالة الجمعة", "كل جمعة يكتب لك صديقك كلمة عن أسبوعكما معًا"),
        Gift("theme_ramadan", 800, "🏮", Kind.THEME, "ثيم «فانوس رمضان»", "تصميم جديد كليًا: فوانيس معلّقة للأقسام والصلوات والسبحة"),
        Gift("theme_rain", 875, "🌧", Kind.THEME, "ثيم «غيث»", "مطر لطيف يهطل ودوائر تتّسع حيث يسقط"),
        Gift("frame_zellige", 950, "🖼", Kind.FRAME, "إطار «الزليج»", "كوبالتي بحافة بيضاء كالبلاط"),
        Gift("frame_dawn", 1000, "🖼", Kind.FRAME, "إطار «الفجر»", "لأداة صديقك"),
        Gift("theme_fireflies", 1025, "✨", Kind.THEME, "ثيم «يراعات»", "يراعات مضيئة تحوم في ليل أخضر"),
        Gift("theme_oasis", 1050, "🌴", Kind.THEME, "ثيم «الواحة الحية»", "نخيل وماء وجبال تتحرك بعمق"),
        Gift("badge_heart", 1100, "💗", Kind.BADGE, "وسام «القلب الحاضر»", "لمن لا يغيب عن صديقه"),
        Gift("theme_aurora", 1125, "🌌", Kind.THEME, "ثيم «شفق»", "أشرطة الشفق القطبي تتموّج فوق ليل عميق"),
        Gift("theme_c_diwan", 1150, "📜", Kind.THEME, "ثيم «ديوان الخطّاط»", "رقّ عاجي وحبر عنّابي وتذهيب"),
        Gift("theme_damask", 1200, "🌹", Kind.THEME, "ثيم «الوردة الدمشقية»", "تصميم جديد كليًا: لوحات كالبتلات وورود تتفتّح بصلاتك"),
        Gift("anniversary", 1250, "🎂", Kind.PERK, "ذكرى صداقتكما", "يحتفل صديقك بكل شهر يمرّ على صداقتكما"),
        Gift("theme_sultani", 1300, "❈", Kind.THEME, "ثيم «المخطوطة السلطانية»", "ورق عاجي مذهّب وأغلفة جلدية"),
        Gift("memory", 1375, "🏅", Kind.PERK, "ذاكرة الصديق", "يتذكّر صديقك أطول سلسلة أيام طيبة لكما ويحتفل حين تبلغها من جديد"),
        Gift("theme_c_palms", 1400, "🌴", Kind.THEME, "ثيم «نخيل الواحة»", "نخيل وجبال وماء فيروزي"),
        Gift("theme_bubbles", 1450, "🫧", Kind.THEME, "ثيم «أعماق»", "فقاعات تصعد وضوء يتراقص كقاع البحر"),
        Gift("theme_moon", 1500, "🎨", Kind.THEME, "ثيم «ضوء القمر»", "فضّي ليلي وذهب القمر"),
        Gift("theme_observatory", 1550, "✧", Kind.THEME, "ثيم «المرصد»", "أسطرلاب يدور والقمر كما هو الليلة"),
        Gift("frame_aurora", 1625, "🖼", Kind.FRAME, "إطار «الشفق»", "ليلي بحافة خضراء وبنفسجية"),
        Gift("theme_c_lamps", 1700, "🏮", Kind.THEME, "ثيم «قناديل المسجد»", "نوافذ ملوّنة وقناديل دافئة"),
        Gift("theme_illuminated", 1750, "📖", Kind.THEME, "ثيم «مخطوطة مذهّبة»", "إطار مذهّب بوريدات كصفحة مخطوطة مزخرفة"),
        Gift("theme_arizona", 1800, "🏜", Kind.THEME, "ثيم «أريزونا»", "تصميم جديد كليًا: هضاب حمراء ومزولة وشريط جانبي"),
        Gift("theme_dunes", 1900, "🏜", Kind.THEME, "ثيم «كثبان»", "كثبان متموّجة ورمال تسفوها الريح"),
        Gift("nick", 2000, "🏷", Kind.PERK, "لقب من اختيارك", "تكتب لصديقك لقبًا يظهر مكان لقبه في الأداة وصفحته"),
        Gift("theme_c_cutouts", 2050, "✂", Kind.THEME, "ثيم «قصاصات ورقية»", "مسجد وتلال وسحب من الورق"),
        Gift("frame_pearl", 2125, "🖼", Kind.FRAME, "إطار «اللؤلؤ»", "أبيض لؤلؤي بحافة فضية"),
        Gift("theme_stained", 2200, "❖", Kind.THEME, "ثيم «زجاج معشّق»", "نافذة زجاج ملوّن يتسلّل منها الضوء"),
        Gift("hilal", 2250, "🌙", Kind.PERK, "تهنئة الهلال والعيد", "يهنّئك صديقك بأول كل شهر هجري بدعاء الهلال، وبالعيدين"),
        Gift("theme_c_spheres", 2350, "🪐", Kind.THEME, "ثيم «ساعة الأفلاك»", "أسطرلاب نحاسي وفضة ونجوم"),
        Gift("theme_ink", 2400, "✒", Kind.THEME, "ثيم «حبر وقلم»", "تصميم جديد كليًا: لفائف رقّ وقطرات حبر"),
        Gift("golden", 2500, "💛", Kind.PERK, "القلوب الذهبية", "قلوب صديقك في الأداة وصفحته تصير ذهبية"),
        Gift("badge_star", 2600, "🌟", Kind.BADGE, "وسام «نجم الصحبة»", "صداقة تضيء"),
        Gift("theme_seasons", 2700, "❀", Kind.THEME, "ثيم «حديقة الفصول»", "حديقة تتبدّل مع الفصول ورمضان والعيد"),
        Gift("frame_royal", 2750, "🖼", Kind.FRAME, "إطار «الملكي»", "بنفسجي ملكي بإطار ذهبي مزدوج"),
        Gift("theme_rosette", 2900, "🏵", Kind.THEME, "ثيم «وردة هندسية»", "وردة إسلامية من اثني عشر ضلعًا تدور ببطء"),
        Gift("theme_sky", 3000, "🌤", Kind.THEME, "ثيم «سماء الوقت»", "ثيم حيّ تتبدّل ألوانه وسماؤه مع أوقات الصلاة في مدينتك"),
        Gift("theme_c_home", 3100, "🏡", Kind.THEME, "ثيم «بيت الفصول»", "بيت وأشجار تتبدّل مع الفصول"),
        Gift("theme_glass", 3250, "◈", Kind.THEME, "ثيم «زجاج»", "تصميم جديد كليًا: زجاج مصنفر وبلّورات تُضيء"),
        Gift("badge_long", 3500, "🎖", Kind.BADGE, "وسام «الرفقة الطويلة»", "لصداقة ثبتت على الأيام"),
        Gift("theme_riwaq", 3750, "۞", Kind.THEME, "ثيم «رواق النور»", "أقواس وحديقة تتبدّل مع أوقات الصلاة"),
        Gift("theme_stars", 4000, "✨", Kind.THEME, "ثيم «سَحَر»", "نجوم الثلث الأخير من الليل تتلألأ وشهاب يعبر"),
        Gift("theme_c_palace", 4250, "🏛", Kind.THEME, "ثيم «قصر الرخام»", "رخام بعروق وزمرد وإطارات معدنية"),
        Gift("frame_crystal", 4500, "🖼", Kind.FRAME, "إطار «البلّور»", "سماوي شفاف"),
        Gift("badge_moon", 4750, "🌕", Kind.BADGE, "وسام «بدر الوفاء»", "وفاء تمّ كالبدر"),
        Gift("diamond", 5000, "💎", Kind.PERK, "قلوب الماس", "قلوب صديقك في الأداة وصفحته تصير ماسية"),
        Gift("theme_skyline", 5500, "🕌", Kind.THEME, "ثيم «مآذن»", "قباب ومآذن في الأفق تضيء نوافذها ليلًا"),
        Gift("badge_loyal", 6000, "🏆", Kind.BADGE, "وسام «الخِلّ الوفيّ»", "لمن لم يترك صديقه"),
        Gift("theme_velvet", 6500, "♛", Kind.THEME, "ثيم «المخمل الملكي»", "تصميم جديد كليًا: مخمل قرمزي وإطارات مذهّبة ومزولة ذهبية"),
        Gift("badge_garden", 7000, "🌺", Kind.BADGE, "وسام «روضة الأُنس»", "أُنس لا يذبل"),
        Gift("theme_light", 7500, "🌟", Kind.THEME, "ثيم «نور»", "أشعّة نور ذهبية تنسكب من أعلى وتدور ببطء"),
        Gift("badge_eternal", 8000, "♾", Kind.BADGE, "وسام «العهد الدائم»", "عهد لم يُنقض"),
        Gift("frame_lanterns", 400, "🏮", Kind.FRAME, "إطار «فوانيس رمضان»", "فوانيس ذهبية تتدلّى من حافة البطاقة والأداة"),
        Gift("frame_eid", 800, "🎉", Kind.FRAME, "إطار «فوانيس العيد»", "رايات ملوّنة وفوانيس ونجوم العيد"),
        Gift("frame_crescents", 1200, "🌙", Kind.FRAME, "إطار «أهلّة ونجوم»", "أهلّة ونجوم ذهبية معلّقة"),
        Gift("frame_galaxy", 8500, "🖼", Kind.FRAME, "إطار «المجرّة»", "أسود وبنفسجي بنجوم صغيرة على الحافة"),
        Gift("theme_marble", 9000, "◆", Kind.THEME, "ثيم «الرخام والذهب»", "رخام وزمرد وذهب"),
        Gift("badge_pure", 10000, "🥇", Kind.BADGE, "وسام «صفاء الودّ الذهبي»", "عشرة آلاف ❤ من الصحبة"),
        Gift("badge_crown", 12500, "👑", Kind.BADGE, "تاج الصداقة", "أعلى ما تبلغه صداقة"),
    ).sortedBy { it.bond }
    fun gift(id: String) = gifts.firstOrNull { it.id == id }
    /** Is this gift open? (Unknown ids are open, so nothing breaks.) */
    fun has(id: String) = gift(id)?.let { Progress.bondPoints >= it.bond } ?: true
    fun next(): Gift? = gifts.firstOrNull { Progress.bondPoints < it.bond }

    /** Themes that friendship opens (the other themes are open to everyone). */
    fun themeGift(lookId: String): Gift? = gift("theme_$lookId")
    /** TEMPORARY, for trying the themes before publishing: every theme open. Must be false in a published version. */
    const val TRY_ALL_THEMES = false
    fun themeOpen(lookId: String) = TRY_ALL_THEMES || (themeGift(lookId)?.let { Progress.bondPoints >= it.bond } ?: true)

    /** The widget frame's gift. */
    fun frameGift(frameId: String): Gift? = gift("frame_$frameId")
    fun frameOpen(frameId: String) = frameGift(frameId)?.let { Progress.bondPoints >= it.bond } ?: true

    // ---- the nickname
    var nick: String
        get() = Store.prefs.getString("pet_nick", "") ?: ""
        set(v) { Store.prefs.edit().putString("pet_nick", v.trim().take(24)).apply(); Store.bump() }
    /** The title under the friend's name: the one you chose (when open), or the level's title. */
    fun title(): String = nick.takeIf { it.isNotBlank() && has("nick") } ?: Progress.title()

    val heart: String get() = if (has("diamond")) "💎" else if (has("golden")) "💛" else "❤"

    // ---- words that only friendship opens (one a day each)
    fun line(): Pet.Say? {
        val sp = Store.prefs; val today = LocalDate.now()
        if (has("letter") && today.dayOfWeek == DayOfWeek.FRIDAY && sp.getString("pet_letter_day", "") != today.toString()) {
            sp.edit().putString("pet_letter_day", today.toString()).apply()
            val good = Pet.streak()
            return Pet.Say("💌 جمعة مباركة${Pet.userName.takeIf { it.isNotBlank() }?.let { " يا $it" } ?: ""}! " +
                (if (good > 0) "أيامنا الطيبة متتالية منذ ${Quran.digits(good)} ${if (good in 3..10) "أيام" else "يوم"}. " else "") +
                "لا تنسَ سورة الكهف والصلاة على النبي ﷺ")
        }
        val who = Pet.userName.takeIf { it.isNotBlank() }?.let { " يا $it" } ?: ""
        val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        if (has("hilal") && sp.getString("pet_hilal_day", "") != today.toString()) {
            val h = Data.hijri(today)
            val say = when {
                h.month == 10 && h.day == 1 -> "🌙 عيد مبارك$who! تقبّل الله منا ومنك"
                h.month == 12 && h.day == 10 -> "🐑 عيد أضحى مبارك$who! تقبّل الله منا ومنك"
                h.day == 1 -> "🌙 هلّ هلال ${h.monthName}: اللهم أهلّه علينا باليُمن والإيمان، والسلامة والإسلام، ربي وربك الله"
                else -> null
            }
            if (say != null) { sp.edit().putString("pet_hilal_day", today.toString()).apply(); return Pet.Say(say) }
        }
        if (has("nightdua") && sp.getString("pet_nightdua_day", "") != today.toString() &&
            runCatching { nowMin >= Store.times().getValue(Prayer.ISHA) }.getOrDefault(nowMin >= 20 * 60)) {
            sp.edit().putString("pet_nightdua_day", today.toString()).apply()
            return Pet.Say("🌙 تصبح على خير$who. اللهم احفظه في نومه ويقظته، وبارك له في يومه. لا تنسَ أذكار النوم وآية الكرسي")
        }
        if (has("memory") && sp.getString("pet_memory_day", "") != today.toString()) {
            val st = Pet.streak()
            if (st >= 3 && st >= Pet.bestStreak()) {
                sp.edit().putString("pet_memory_day", today.toString()).apply()
                return Pet.Say("🏅 أتذكّر كل أيامنا: هذه أطول سلسلة أيام طيبة لنا، ${Quran.digits(st)} ${if (st in 3..10) "أيام" else "يومًا"} متتالية! بارك الله فيك$who")
            }
        }
        val since = Pet.since
        if (has("anniversary") && since > 0 && sp.getString("pet_anniv_day", "") != today.toString()) {
            val start = Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDate()
            val months = java.time.temporal.ChronoUnit.MONTHS.between(start, today).toInt()
            if (months > 0 && start.plusMonths(months.toLong()) == today) {
                sp.edit().putString("pet_anniv_day", today.toString()).apply()
                return Pet.Say("🎂 اليوم ${if (months % 12 == 0) "${Quran.digits(months / 12)} ${if (months == 12) "سنة" else "سنوات"}" else "${Quran.digits(months)} ${if (months in 3..10) "أشهر" else "شهر"}"} على صداقتنا، الحمد لله عليك!")
            }
        }
        return null
    }

    // ---- a gift just opened: celebrated once
    /** Called after friendship points grow from [before]: notes the newest gift crossed. */
    fun crossed(before: Int, after: Int) {
        gifts.lastOrNull { it.bond in (before + 1)..after }?.let { Store.prefs.edit().putString("bond_newgift", it.id).apply() }
    }
    fun newGift(): Gift? = Store.prefs.getString("bond_newgift", null)?.let { gift(it) }
    fun giftSeen() { Store.prefs.edit().remove("bond_newgift").apply(); Store.bump() }
}
