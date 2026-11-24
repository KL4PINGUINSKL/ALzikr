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
        Gift("frame_night", 50, "🖼", Kind.FRAME, "إطار «الليل المرصّع بالنجوم»", "لأداة صديقك على الشاشة الرئيسية"),
        Gift("greet", 100, "🌅", Kind.PERK, "تحية الصباح باسمك", "يحيّيك صديقك صباحًا باسمك ويذكّرك بأذكار الصباح"),
        Gift("theme_breeze", 150, "🎨", Kind.THEME, "ثيم «نسيم»", "نعناعي وسماوي هادئ"),
        Gift("frame_gold", 225, "🖼", Kind.FRAME, "إطار «الذهبي»", "لأداة صديقك"),
        Gift("joy", 300, "😊", Kind.PERK, "الفرح فوق التطبيقات", "يقفز صديقك العائم وتطير القلوب حين تمتلئ قلوب يومك"),
        Gift("certificate", 375, "📜", Kind.PERK, "شهادة الصداقة", "صورة باسم صديقك وأيامكما تشاركها إن شئت"),
        Gift("theme_dusk", 500, "🎨", Kind.THEME, "ثيم «غسق»", "بنفسجي المغيب وذهب خافت"),
        Gift("frame_pattern", 625, "🖼", Kind.FRAME, "إطار «الزخرفة»", "لأداة صديقك"),
        Gift("letter", 750, "💌", Kind.PERK, "رسالة الجمعة", "كل جمعة يكتب لك صديقك كلمة عن أسبوعكما معًا"),
        Gift("frame_dawn", 1000, "🖼", Kind.FRAME, "إطار «الفجر»", "لأداة صديقك"),
        Gift("anniversary", 1250, "🎂", Kind.PERK, "ذكرى صداقتكما", "يحتفل صديقك بكل شهر يمرّ على صداقتكما"),
        Gift("theme_moon", 1500, "🎨", Kind.THEME, "ثيم «ضوء القمر»", "فضّي ليلي وذهب القمر"),
        Gift("nick", 2000, "🏷", Kind.PERK, "لقب من اختيارك", "تكتب لصديقك لقبًا يظهر مكان لقبه في الأداة وصفحته"),
        Gift("golden", 2500, "💛", Kind.PERK, "القلوب الذهبية", "قلوب صديقك في الأداة وصفحته تصير ذهبية"),
    )
    fun gift(id: String) = gifts.firstOrNull { it.id == id }
    /** Is this gift open? (Unknown ids are open, so nothing breaks.) */
    fun has(id: String) = gift(id)?.let { Progress.bondPoints >= it.bond } ?: true
    fun next(): Gift? = gifts.firstOrNull { Progress.bondPoints < it.bond }

    /** Themes that friendship opens (the other themes are open to everyone). */
    fun themeGift(lookId: String): Gift? = gift("theme_$lookId")
    fun themeOpen(lookId: String) = themeGift(lookId)?.let { Progress.bondPoints >= it.bond } ?: true

    /** The widget frame's gift. */
    fun frameGift(frameId: String): Gift? = gift("frame_$frameId")
    fun frameOpen(frameId: String) = frameGift(frameId)?.let { Progress.bondPoints >= it.bond } ?: true

    // ---- the nickname
    var nick: String
        get() = Store.prefs.getString("pet_nick", "") ?: ""
        set(v) { Store.prefs.edit().putString("pet_nick", v.trim().take(24)).apply(); Store.bump() }
    /** The title under the friend's name: the one you chose (when open), or the level's title. */
    fun title(): String = nick.takeIf { it.isNotBlank() && has("nick") } ?: Progress.title()

    val heart: String get() = if (has("golden")) "💛" else "❤"

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
