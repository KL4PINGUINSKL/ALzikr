package com.alzikr.app

import java.time.LocalDate
import org.json.JSONObject

/**
 * One place that records what the user did and turns it into the friend's growth:
 * ✨ experience (Quran, hadith, learning, the day's worship) raises its level and opens rewards;
 * ❤ friendship (time spent with the friend: taps, talks, challenges) changes how close it feels;
 * 🏅 badges mark long-term milestones. Each thing counts once a day, and each kind has a daily cap,
 * so the numbers cannot be farmed. These are in-app rewards only, not a measure of reward with Allah.
 */
object Progress {
    private val sp get() = Store.prefs
    /** Children keep their own progress (experience, friendship, badges), apart from the adult's, on the same phone. */
    private fun pk(name: String) = if (Pet.kids) "kid_$name" else name
    private fun today() = LocalDate.now().toString()

    // ------------------------------------------------------------ experience
    enum class Kind(val xp: Int, val cap: Int, val ar: String) {
        PAGE(3, 60, "صفحات القرآن"), WIRD(15, 15, "إتمام الورد"), KHATMA(200, 200, "ختم القرآن"),
        HADITH(2, 30, "قراءة الأحاديث"), SHARH(3, 15, "شروح الأحاديث"), STUDY(5, 20, "الحفظ والتجويد"),
        DEED(10, 60, "الصلوات والأذكار والصدقة"),
    }

    /** Records one thing done today; returns the experience it earned (0 if counted already or the cap is reached). */
    fun record(kind: Kind, key: String): Int {
        val d = today()
        val day = if (sp.getString(pk("prog_day"), "") == d) JSONObject(sp.getString(pk("prog_today"), "{}")!!) else JSONObject()
        val seen = day.optJSONObject("seen") ?: JSONObject()
        val id = "${kind.name}:$key"
        if (seen.has(id)) return 0
        seen.put(id, 1)
        val got = day.optJSONObject("xp") ?: JSONObject()
        val before = got.optInt(kind.name)
        val add = kind.xp.coerceAtMost(kind.cap - before).coerceAtLeast(0)
        got.put(kind.name, before + add)
        day.put("seen", seen).put("xp", got)
        val e = sp.edit().putString(pk("prog_day"), d).putString(pk("prog_today"), day.toString())
        count(e, kind)
        val oldLevel = level()
        if (add > 0) e.putInt(pk("pet_xp"), xp + add)
        e.apply()
        if (Pet.since == 0L && Pet.species != null) Pet.since = System.currentTimeMillis()
        if (level() > oldLevel) levelUp(level())
        if (add > 0 && kind == Kind.KHATMA) Pet.remember("khatma", maxOf(Data.khatmaCount, total(Kind.KHATMA)))
        if (kind == Kind.PAGE) Pet.readingHour()
        checkBadges()
        Store.bump()
        return add
    }
    /** Lifetime counters behind the badges. */
    private fun count(e: android.content.SharedPreferences.Editor, k: Kind) { e.putInt(pk("prog_n_${k.name}"), total(k) + 1) }
    fun total(k: Kind) = sp.getInt(pk("prog_n_${k.name}"), 0)
    fun todayXp(k: Kind): Int = if (sp.getString(pk("prog_day"), "") == today()) JSONObject(sp.getString(pk("prog_today"), "{}")!!).optJSONObject("xp")?.optInt(k.name) ?: 0 else 0

    val xp: Int get() = sp.getInt(pk("pet_xp"), 0)
    /** Total experience at which level [n] begins: 0, 150, 375, 675, 1050, 1500, 2025, 2625, 3300, 4050… */
    /**
     * Points needed to reach level [n] (total). A fixed arithmetic progression: level 2 needs 150, and every level needs
     * 75 more than the one before it (150, 225, 300, 375…), so need(n) - need(n-1) = 75·(n+1).
     */
    fun need(n: Int): Int = if (n <= 1) 0 else 75 * (n - 1) * (n + 2) / 2
    /** Points this level alone needs (the step from the previous one). */
    fun step(n: Int): Int = need(n) - need(n - 1)
    fun level(x: Int = xp): Int { var n = 1; while (need(n + 1) <= x) n++; return n }
    fun title(l: Int = level()) = when { l >= 12 -> "صديق العمر"; l >= 9 -> "رفيق الدرب"; l >= 7 -> "الخِلّ الوفيّ"; l >= 5 -> "الصاحب"; l >= 3 -> "الرفيق"; else -> "صديق جديد" }

    /** Shows levels, points and rewards; off for those who prefer their worship uncounted (rewards still open quietly). */
    var showLevels: Boolean
        get() = sp.getBoolean("prog_show", true)
        set(v) { sp.edit().putBoolean("prog_show", v).apply(); Store.bump() }

    // ------------------------------------------------------------ level-up
    private fun levelUp(l: Int) { sp.edit().putInt(pk("prog_levelup"), l).apply(); Pet.remember("level", l) }
    /** A level reached and not yet celebrated. */
    fun pendingLevelUp(): Int? = sp.getInt(pk("prog_levelup"), 0).takeIf { it > 1 && showLevels }
    fun celebrated() { sp.edit().remove(pk("prog_levelup")).apply(); Store.bump() }

    // ------------------------------------------------------------ friendship (❤), separate from experience
    /** Time spent with the friend: [amount] once per [key] a day, at most 15 a day. */
    fun bond(key: String, amount: Int = 1) {
        val d = today()
        val keys = if (sp.getString(pk("bond_day"), "") == d) sp.getStringSet(pk("bond_keys"), emptySet())!!.toMutableSet() else mutableSetOf()
        val got = if (sp.getString(pk("bond_day"), "") == d) sp.getInt(pk("bond_today"), 0) else 0
        if (!keys.add(key) || got >= 15) return
        val add = amount.coerceAtMost(15 - got)
        sp.edit().putString(pk("bond_day"), d).putStringSet(pk("bond_keys"), keys).putInt(pk("bond_today"), got + add).putInt(pk("bond"), bondPoints + add).apply()
        checkBadges(); Store.bump()
    }
    val bondPoints: Int get() = sp.getInt(pk("bond"), 0)
    private val bondSteps = listOf(0 to "تعارف", 20 to "ألفة", 60 to "مودّة", 150 to "صحبة", 300 to "أخوّة", 600 to "رفقة العمر")
    fun bondName(b: Int = bondPoints) = bondSteps.last { b >= it.first }.second
    fun bondStage(b: Int = bondPoints) = bondSteps.indexOfLast { b >= it.first }
    /** (from, to) of the current friendship stage, for its bar; to = -1 at the last stage. */
    fun bondRange(b: Int = bondPoints): Pair<Int, Int> { val i = bondStage(b); return bondSteps[i].first to (bondSteps.getOrNull(i + 1)?.first ?: -1) }

    // ------------------------------------------------------------ rewards opened by level (looks for new friends, widget frames, the certificate)
    data class Reward(val level: Int, val icon: String, val name: String)
    fun rewards(): List<Reward> = listOf(
        Reward(1, "🐾", "القطة والبطريق والغنمة، وثلاثة أشكال لكل حيوان"),
        Reward(2, "🎉", "احتفال عند كل مستوى جديد، وشكل جديد لكل حيوان مع كل مستوى بلا حدّ"),
        Reward(3, "🖼", "إطار «الليل المرصّع بالنجوم» للأداة"),
        Reward(4, "🌅", "يحيّيك صديقك باسمك صباحًا، ويقول «تقبّل الله» حين تتم وردك"),
        Reward(5, "✨", "إطار «الذهبي» للأداة"),
        Reward(6, "😊", "وضع الفرح فوق التطبيقات: يقفز وتطير القلوب حين تمتلئ قلوب يومك"),
        Reward(8, "🕌", "إطار «الزخرفة» للأداة"),
        Reward(9, "📜", "شهادة الصداقة: صورة تشاركها باسمك ومستوى صديقك وأيامكما"),
        Reward(10, "🌟", "إطار «الفجر» للأداة"),
        Reward(12, "👑", "لقب «صديق العمر»"),
    ) + speciesLevels.filter { it.value > 1 }.map { (s, l) -> Reward(l, "🐾", "صديق جديد: ${s.ar}") }.sortedBy { it.level }
    /** Animals open with the level too (the cat, the penguin and the sheep from the start): the levels are in the pack data. */
    val speciesLevels: Map<Species, Int> get() = Species.entries.associateWith { speciesLevel(it) }
    fun speciesLevel(s: Species) = com.alzikr.app.pets.PetPacks.registry().species(s.name)?.unlock?.level ?: 1
    /** The friend someone already had before the locks stays theirs. */
    fun speciesOpen(s: Species) = level() >= speciesLevel(s) || s.name == kept("species")
    fun has(levelNeeded: Int) = level() >= levelNeeded

    /**
     * What someone had chosen before the locks (their friend and the look of each friend) stays open for them.
     * Recorded once, the first time the locks are seen.
     */
    private fun kept(what: String): String? {
        if (!sp.getBoolean("prog_kept", false)) {
            val e = sp.edit().putBoolean("prog_kept", true)
            Pet.species?.let { e.putString("prog_keep_species", it.name) }
            Species.entries.forEach { s -> sp.getString("pet_look_${s.name}", null)?.let { e.putString("prog_keep_look_${s.name}", it) } }
            e.apply()
        }
        return sp.getString("prog_keep_$what", null)
    }
    /** The rule the core pack's look levels were made with: three at the start, then one more every level, without end. */
    fun lookLevel(i: Int) = if (i < 3) 1 else i - 1
    /** The level that opens look [i] of [s], from the pack data. */
    fun lookLevel(s: Species, i: Int) = PetLooks.of(s).getOrNull(i)?.level ?: lookLevel(i)
    /**
     * Open when the level is reached and, for a seasonal look, during its Hijri season. What someone had already
     * chosen stays theirs.
     */
    fun lookOpen(s: Species, i: Int): Boolean {
        val l = PetLooks.of(s).getOrNull(i) ?: return false
        if (l.id == kept("look_${s.name}")) return true
        // The look this friend wears now stays open even if its level moved up later (levels were re-arranged in 1.4.0).
        if (l.id == sp.getString("pet_look_${s.name}", null)) return true
        if (level() < l.level) return false
        val season = l.season ?: return true
        // A seasonal look already chosen stays with its friend after the season.
        if (l.id == sp.getString("pet_look_${s.name}", null)) return true
        val h = Data.hijri(); return season.contains(h.month, h.day)
    }

    // ------------------------------------------------------------ widget frames
    data class Frame(val id: String, val name: String, val level: Int, val res: Int?)
    val frames: List<Frame> get() = listOf(
        Frame("plain", "بلون الثيم", 1, null), Frame("night", "الليل المرصّع بالنجوم", 3, R.drawable.frame_night),
        Frame("gold", "الذهبي", 5, R.drawable.frame_gold), Frame("pattern", "الزخرفة", 8, R.drawable.frame_pattern), Frame("dawn", "الفجر", 10, R.drawable.frame_dawn),
    )
    var frame: String
        get() = sp.getString("prog_frame", "plain")!!.takeIf { id -> frames.any { it.id == id && level() >= it.level } } ?: "plain"
        set(v) { sp.edit().putString("prog_frame", v).apply(); Store.bump() }

    // ------------------------------------------------------------ badges (🏅): long-term milestones, kept with the day they were earned
    data class Badge(val id: String, val icon: String, val name: String, val how: String, val target: Int, val now: () -> Int)
    val badges: List<Badge> get() = listOf(
        Badge("khatma1", "📗", "الختمة الأولى", "أتمّ ختمة", 1) { maxOf(Data.khatmaCount, total(Kind.KHATMA)) },
        Badge("khatma10", "📚", "قارئ", "أتمّ عشر ختمات", 10) { maxOf(Data.khatmaCount, total(Kind.KHATMA)) },
        Badge("pages1000", "📖", "ألف صفحة", "اقرأ ١٠٠٠ صفحة مع صديقك", 1000) { total(Kind.PAGE) },
        Badge("hadith100", "🎒", "طالب الحديث", "اقرأ ١٠٠ حديث مختلف", 100) { total(Kind.HADITH) },
        Badge("hadith500", "🌿", "محبّ الحديث", "اقرأ ٥٠٠ حديث", 500) { total(Kind.HADITH) },
        Badge("streak7", "🔥", "أسبوع طيب", "سبعة أيام طيبة متتالية", 7) { Pet.bestStreak() },
        Badge("streak30", "🏵", "صديق الأيام الطيبة", "ثلاثون يومًا طيبًا متتاليًا", 30) { Pet.bestStreak() },
        Badge("days30", "🌙", "شهر معًا", "ثلاثون يومًا على صداقتكما", 30) { Pet.daysTogether() },
        Badge("days100", "🎖", "مئة يوم", "مئة يوم على صداقتكما", 100) { Pet.daysTogether() },
        Badge("days365", "🌳", "سنة معًا", "سنة كاملة على صداقتكما", 365) { Pet.daysTogether() },
        Badge("challenges30", "🎯", "صاحب التحديات", "أتمّ ثلاثين تحديًا يوميًا", 30) { sp.getInt("pet_ch_count", 0) },
        Badge("level10", "⭐", "المستوى العاشر", "بلغ صديقك المستوى ١٠", 10) { level() },
    )
    fun earned(): Map<String, String> = JSONObject(sp.getString(pk("prog_badges"), "{}")!!).let { o -> o.keys().asSequence().associateWith { o.getString(it) } }
    fun checkBadges() {
        val o = JSONObject(sp.getString(pk("prog_badges"), "{}")!!)
        var fresh: Badge? = null
        badges.forEach { b -> if (!o.has(b.id) && runCatching { b.now() }.getOrDefault(0) >= b.target) { o.put(b.id, today()); fresh = b } }
        if (fresh != null) { sp.edit().putString(pk("prog_badges"), o.toString()).putString(pk("prog_newbadge"), fresh!!.id).apply() }
    }
    fun newBadge(): Badge? = sp.getString(pk("prog_newbadge"), null)?.let { id -> badges.firstOrNull { it.id == id } }?.takeIf { showLevels }
    fun badgeSeen() { sp.edit().remove(pk("prog_newbadge")).apply(); Store.bump() }

    // ------------------------------------------------------------ hooks
    /** Pages moved forward in the khatma (reading mode counts a page after ten seconds on it; the buttons are for a paper mushaf). */
    fun khatmaMoved(old: Int, new: Int) {
        if (new <= old) return
        ((old + 1)..new).take(40).forEach { record(Kind.PAGE, "p$it") }
        if (Data.wirdToday() > 0 && Data.pagesToday() >= Data.wirdToday()) { if (record(Kind.WIRD, "wird") > 0 && has(4)) Pet.cheer("تقبّل الله منك وردك 🌙", key = "wird_cheer") }
        if (new >= Data.PAGES) record(Kind.KHATMA, "k${Data.khatmaCount}_${Data.khatmaStarted}")
    }
}
