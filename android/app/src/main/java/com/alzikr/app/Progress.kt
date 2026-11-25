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
        PAGE(2, DAY_MAX, "صفحات القرآن"), WIRD(15, 15, "إتمام الورد"), KHATMA(200, 200, "ختم القرآن"),
        HADITH(2, 30, "قراءة الأحاديث"), SHARH(3, 15, "شروح الأحاديث"), STUDY(5, 20, "الحفظ والتجويد"),
        DEED(10, 60, "الصلوات والأذكار والصدقة"), LISTEN(1, 60, "الاستماع للتلاوة"),
    }
    /** All experience of one day together never passes this; a finished khatma fills what is left of it (up to 200). */
    const val DAY_MAX = 300
    /** Experience earned today, all kinds together (at most [DAY_MAX]). */
    fun todayTotal(): Int = Kind.entries.sumOf { todayXp(it) }

    /** How each kind is earned, in plain words (shown under «كيف أكسب النقاط؟»). */
    fun how(k: Kind): String = when (k) {
        Kind.PAGE -> "كل صفحة قرآن تقرؤها، من وردك أو خارجه: تبقى عليها 20 ثانية في المصحف، أو تقلّبها في وضع القراءة أو في «اقرأ وردك»"
        Kind.LISTEN -> "كل آية تسمعها حتى آخرها بصوت أي قارئ"
        Kind.WIRD -> "إتمام ورد اليوم من ختمتك (يُحسب عند آخر صفحة منه، أينما قرأته بالترتيب)"
        Kind.KHATMA -> "ختم القرآن كاملًا (يكمّل نقاط يومك حتى 300 ولا يتجاوزها)"
        Kind.HADITH -> "كل حديث تقرؤه 15 ثانية"
        Kind.SHARH -> "كل شرح حديث تبقى عليه دقيقة"
        Kind.STUDY -> "كل درس تجويد تتمّه، وكل مقطع تحفظه أو تراجعه"
        Kind.DEED -> "كل صلاة تعلّمها ✓، وإتمام أذكار، وصدقة، وكل 33 تسبيحة"
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
        val dayTotal = Kind.entries.sumOf { got.optInt(it.name) }
        val add = minOf(kind.xp, kind.cap - before, DAY_MAX - dayTotal).coerceAtLeast(0)
        got.put(kind.name, before + add)
        val amt = day.optJSONObject("amt") ?: JSONObject(); amt.put(id, add)
        day.put("seen", seen).put("xp", got).put("amt", amt)
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
    /** Takes back what [record] gave today for this thing (it was undone: a prayer unmarked…). */
    fun unrecord(kind: Kind, key: String) {
        val d = today(); if (sp.getString(pk("prog_day"), "") != d) return
        val day = JSONObject(sp.getString(pk("prog_today"), "{}")!!)
        val seen = day.optJSONObject("seen") ?: return
        val id = "${kind.name}:$key"; if (!seen.has(id)) return
        seen.remove(id)
        val amt = day.optJSONObject("amt") ?: JSONObject(); val back = amt.optInt(id, 0); amt.remove(id)
        val got = day.optJSONObject("xp") ?: JSONObject(); got.put(kind.name, (got.optInt(kind.name) - back).coerceAtLeast(0))
        day.put("seen", seen).put("xp", got).put("amt", amt)
        sp.edit().putString(pk("prog_today"), day.toString()).putInt(pk("pet_xp"), (xp - back).coerceAtLeast(0))
            .putInt(pk("prog_n_${kind.name}"), (total(kind) - 1).coerceAtLeast(0)).apply()
        Store.bump()
    }
    /** Lifetime counters behind the badges. */
    private fun count(e: android.content.SharedPreferences.Editor, k: Kind) { e.putInt(pk("prog_n_${k.name}"), total(k) + 1) }
    fun total(k: Kind) = sp.getInt(pk("prog_n_${k.name}"), 0)
    fun todayXp(k: Kind): Int = if (sp.getString(pk("prog_day"), "") == today()) JSONObject(sp.getString(pk("prog_today"), "{}")!!).optJSONObject("xp")?.optInt(k.name) ?: 0 else 0

    val xp: Int get() = sp.getInt(pk("pet_xp"), 0)
    /**
     * Points this level alone needs (the step from the previous one). Level 2 needs 150; each level then needs more
     * than the one before it: 75 more up to level 25, 125 more from 26 to 50, and 150 more after 50.
     * Steps: 150, 225 … 1875 (L25), 2000 … 5000 (L50), 5150 … 12500 (L100).
     */
    fun stepGrowth(n: Int): Int = 75
    fun step(n: Int): Int = when {
        n <= 1 -> 0
        else -> 75 * n
    }
    /** Total points at which level [n] begins: 0, 150, 375 … 24 300 (L25), 95 550 (L50), 378 675 (L100). */
    private val needs: IntArray by lazy { IntArray(1001).also { a -> for (n in 2..1000) a[n] = a[n - 1] + step(n) } }
    fun need(n: Int): Int = if (n <= 1) 0 else if (n <= 1000) needs[n] else needs[1000] + (1001..n).sumOf { step(it) }
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
    /** Friendship points a day at most (the week's chest comes on top). */
    const val BOND_DAY = 40
    /** Time spent with the friend: [amount] once per [key] a day, at most [BOND_DAY] a day ([capped] = false: on top). */
    fun bond(key: String, amount: Int = 1, capped: Boolean = true) {
        val d = today()
        val keys = if (sp.getString(pk("bond_day"), "") == d) sp.getStringSet(pk("bond_keys"), emptySet())!!.toMutableSet() else mutableSetOf()
        val got = if (sp.getString(pk("bond_day"), "") == d) sp.getInt(pk("bond_today"), 0) else 0
        if (!keys.add(key) || (capped && got >= BOND_DAY)) return
        val add = if (capped) amount.coerceAtMost(BOND_DAY - got) else amount
        val before = bondPoints
        val amt = runCatching { JSONObject(sp.getString(pk("bond_amt"), "{}")!!) }.getOrDefault(JSONObject()).let { if (sp.getString(pk("bond_day"), "") == d) it else JSONObject() }
        amt.put(key, add)
        sp.edit().putString(pk("bond_day"), d).putStringSet(pk("bond_keys"), keys).putInt(pk("bond_today"), got + add).putInt(pk("bond"), before + add)
            .putString(pk("bond_amt"), amt.toString()).apply()
        if (!Pet.kids) Friendship.crossed(before, before + add)
        checkBadges(); Store.bump()
    }
    /** Takes back the friendship points [bond] gave today for [key] (the thing was undone). */
    fun unbond(key: String) {
        val d = today(); if (sp.getString(pk("bond_day"), "") != d) return
        val keys = sp.getStringSet(pk("bond_keys"), emptySet())!!.toMutableSet(); if (!keys.remove(key)) return
        val amt = runCatching { JSONObject(sp.getString(pk("bond_amt"), "{}")!!) }.getOrDefault(JSONObject())
        val back = amt.optInt(key, 0); amt.remove(key)
        sp.edit().putStringSet(pk("bond_keys"), keys).putInt(pk("bond_today"), (sp.getInt(pk("bond_today"), 0) - back).coerceAtLeast(0))
            .putInt(pk("bond"), (bondPoints - back).coerceAtLeast(0)).putString(pk("bond_amt"), amt.toString()).apply()
        Store.bump()
    }
    /** What [bond] really gave today for [key] (less than asked when the day's limit was near), or null if nothing. */
    fun bondGot(key: String): Int? = if (!bonded(key)) null else runCatching { JSONObject(sp.getString(pk("bond_amt"), "{}")!!) }.getOrNull()?.takeIf { it.has(key) }?.optInt(key)
    fun bondToday(): Int = if (sp.getString(pk("bond_day"), "") == today()) sp.getInt(pk("bond_today"), 0) else 0
    /** Today's friendship points by where they came from (label → points), largest first. */
    fun bondSources(): List<Pair<String, Int>> {
        if (sp.getString(pk("bond_day"), "") != today()) return emptyList()
        val amt = runCatching { JSONObject(sp.getString(pk("bond_amt"), "{}")!!) }.getOrDefault(JSONObject())
        fun label(k: String) = when {
            k == "visit" -> "زيارة صفحة صديقك"
            k.startsWith("tap") || k.startsWith("ftap") || k.startsWith("wtalk") -> "لمسه والحديث معه"
            k.startsWith("act_") -> "اللعب معه"
            k.startsWith("challenge") -> "التحديات"
            k.startsWith("task_") -> "الوظائف اليومية"
            k.startsWith("now_") -> "«الآن»"
            k == "question" -> "سؤال اليوم"
            k == "chest" -> "صندوق الأسبوع"
            k == "learn" -> "تعلّم اليوم"
            else -> "أخرى"
        }
        val m = LinkedHashMap<String, Int>()
        amt.keys().forEach { k -> val v = amt.optInt(k); if (v > 0) m[label(k)] = (m[label(k)] ?: 0) + v }
        val known = m.values.sum(); val all = bondToday()
        if (all > known) m["قبل هذا التحديث"] = all - known
        return m.entries.map { it.key to it.value }.sortedByDescending { it.second }
    }
    fun bonded(key: String) = sp.getString(pk("bond_day"), "") == today() && key in (sp.getStringSet(pk("bond_keys"), emptySet()) ?: emptySet())
    val bondPoints: Int get() = sp.getInt(pk("bond"), 0)
    private val bondSteps = listOf(0 to "تعارف", 50 to "ألفة", 150 to "مودّة", 375 to "صحبة", 750 to "أخوّة", 1500 to "رفقة العمر", 2500 to "صفاء الودّ")
    fun bondName(b: Int = bondPoints) = bondSteps.last { b >= it.first }.second
    fun bondStage(b: Int = bondPoints) = bondSteps.indexOfLast { b >= it.first }
    /** (from, to) of the current friendship stage, for its bar; to = -1 at the last stage. */
    fun bondRange(b: Int = bondPoints): Pair<Int, Int> { val i = bondStage(b); return bondSteps[i].first to (bondSteps.getOrNull(i + 1)?.first ?: -1) }

    // ------------------------------------------------------------ rewards opened by level (looks for new friends, widget frames, the certificate)
    data class Reward(val level: Int, val icon: String, val name: String)
    fun rewards(): List<Reward> = listOf(
        Reward(1, "🐾", "القطة والبطريق والغنمة، وأشكال لكل منها"),
        Reward(2, "🎉", "احتفال عند كل مستوى جديد، وكل مستوى حتى 40 يفتح صديقًا جديدًا، ثم صديق كل مستويين أو ثلاثة وبينها أشكال"),
        Reward(12, "👑", "لقب «صديق العمر»"),
        Reward(15, "🌊", "أصدقاء الاستوديو: الملكيون والعناصر وما قبل التاريخ والبحر والآليون والحلوى والكون، آخرهم في المستوى 110"),
        Reward(25, "🐉", "الأصدقاء الأسطوريون بينهم، ولهم زينة ومؤثرات تُفتح مع المستوى"),
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
    /** Widget frames: friendship gifts ([bond] = the friendship points that open one; see [Friendship]). */
    data class Frame(val id: String, val name: String, val bond: Int, val res: Int?) { val open: Boolean get() = Friendship.frameOpen(id) }
    val frames: List<Frame> get() = listOf(
        Frame("plain", "بلون الثيم", 0, null), Frame("night", "الليل المرصّع بالنجوم", Friendship.frameGift("night")!!.bond, R.drawable.frame_night),
        Frame("gold", "الذهبي", Friendship.frameGift("gold")!!.bond, R.drawable.frame_gold), Frame("pattern", "الزخرفة", Friendship.frameGift("pattern")!!.bond, R.drawable.frame_pattern),
        Frame("dawn", "الفجر", Friendship.frameGift("dawn")!!.bond, R.drawable.frame_dawn),
    )
    var frame: String
        get() = sp.getString("prog_frame", "plain")!!.takeIf { id -> frames.any { it.id == id && it.open } } ?: "plain"
        set(v) { sp.edit().putString("prog_frame", v).apply(); Store.bump() }

    // ------------------------------------------------------------ badges (🏅): long-term milestones, kept with the day they were earned
    data class Badge(val id: String, val icon: String, val name: String, val how: String, val target: Int, val now: () -> Int)
    val badges: List<Badge> get() = listOf(
        Badge("khatma1", "📗", "الختمة الأولى", "أتمّ ختمة", 1) { maxOf(Data.khatmaCount, total(Kind.KHATMA)) },
        Badge("khatma10", "📚", "قارئ", "أتمّ عشر ختمات", 10) { maxOf(Data.khatmaCount, total(Kind.KHATMA)) },
        Badge("pages1000", "📖", "ألف صفحة", "اقرأ 1000 صفحة مع صديقك", 1000) { total(Kind.PAGE) },
        Badge("hadith100", "🎒", "طالب الحديث", "اقرأ 100 حديث مختلف", 100) { total(Kind.HADITH) },
        Badge("hadith500", "🌿", "محبّ الحديث", "اقرأ 500 حديث", 500) { total(Kind.HADITH) },
        Badge("streak7", "🔥", "أسبوع طيب", "سبعة أيام طيبة متتالية", 7) { Pet.bestStreak() },
        Badge("streak30", "🏵", "صديق الأيام الطيبة", "ثلاثون يومًا طيبًا متتاليًا", 30) { Pet.bestStreak() },
        Badge("days30", "🌙", "شهر معًا", "ثلاثون يومًا على صداقتكما", 30) { Pet.daysTogether() },
        Badge("days100", "🎖", "مئة يوم", "مئة يوم على صداقتكما", 100) { Pet.daysTogether() },
        Badge("days365", "🌳", "سنة معًا", "سنة كاملة على صداقتكما", 365) { Pet.daysTogether() },
        Badge("challenges30", "🎯", "صاحب التحديات", "أتمّ ثلاثين تحديًا يوميًا", 30) { sp.getInt("pet_ch_count", 0) },
        Badge("level10", "⭐", "المستوى العاشر", "بلغ صديقك المستوى 10", 10) { level() },
        // Friendship badges (time together, not worship).
        Badge("bond20", "🤝", "أول ألفة", "اجمع 50 ❤ من وقتك مع صديقك", 50) { bondPoints },
        Badge("bond150", "💞", "صحبة", "اجمع 375 ❤", 375) { bondPoints },
        Badge("bond600", "🫶", "رفقة العمر", "اجمع 1500 ❤", 1500) { bondPoints },
        Badge("bond1000", "💛", "صفاء الودّ", "اجمع 2500 ❤", 2500) { bondPoints },
        Badge("chest4", "🎁", "أسابيع مكتملة", "افتح صندوق الأسبوع أربع مرات", 4) { sp.getInt("miss_chests", 0) },
        Badge("questions30", "💡", "الباحث", "أجب عن ثلاثين سؤالًا من أسئلة اليوم", 30) { sp.getInt("q_count", 0) },
        Badge("learn30", "📚", "طالب العلم", "أتمم «تعلّم اليوم» ثلاثين يومًا", 30) { sp.getInt("learn_count", 0) },
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
    /** Today's wird is read (from the khatma count, or the wird reader's last page): once a day, and the friend says so. */
    fun wirdDone() { if (record(Kind.WIRD, "wird") > 0) Pet.cheer("تقبّل الله منك وردك 🌙", key = "wird_cheer") }

    /** Pages moved forward in the khatma (reading mode counts a page after ten seconds on it; the buttons are for a paper mushaf). */
    fun khatmaMoved(old: Int, new: Int) {
        if (new <= old) return
        ((old + 1)..new).take(40).forEach { record(Kind.PAGE, "p$it") }
        if (Data.wirdToday() > 0 && Data.pagesToday() >= Data.wirdToday()) wirdDone()
        if (new >= Data.PAGES) record(Kind.KHATMA, "k${Data.khatmaCount}_${Data.khatmaStarted}")
    }
}
