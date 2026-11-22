package com.alzikr.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The newer features' data, in the same "alzikr" preferences as [Store]: prayer log and qada,
 * progress in the built-in collections, the free tasbih, daily habits, the Quran khatma, and the
 * look and reminder settings.
 */
object Data {
    private val sp get() = Store.prefs
    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) { sp.edit().apply(block).apply(); Store.bump() }
    private fun today() = LocalDate.now().toString()

    // ----------------------------------------------------------------- look

    /** Each theme keeps its own color: 0 its original, 1–6 the presets, 7 a free hue. */
    fun lookColor(id: String) = sp.getInt("look_color_$id", 0)
    fun lookHue(id: String) = sp.getFloat("look_hue_$id", 160f)
    fun setLookColor(id: String, index: Int, hue: Float = lookHue(id)) = edit { putInt("look_color_$id", index).putFloat("look_hue_$id", hue) }
    /** Each theme's own background ([Backs], 0 = the theme's) and text color ([Inks], 0 = automatic). */
    fun lookBack(id: String) = sp.getInt("look_back_$id", 0)
    fun setLookBack(id: String, index: Int) = edit { putInt("look_back_$id", index) }
    fun lookInk(id: String) = sp.getInt("look_ink_$id", 0)
    fun setLookInk(id: String, index: Int) = edit { putInt("look_ink_$id", index) }

    /** How the prayer times are shown: 0 the theme's way, 1 cards, 2 list, 3 one line, 4 timeline. */
    var prayerView: Int
        get() = sp.getInt("prayer_view", 0)
        set(v) = edit { putInt("prayer_view", v) }

    /** The chosen theme (see [Looks]). */
    var look: String
        get() = sp.getString("look", "emerald") ?: "emerald"
        set(v) = edit { putString("look", v) }
    /** 0 follow the phone, 1 day, 2 night. */
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) = edit { putInt("theme", v) }
    /** Size of adhkar text: 0 normal, 1 large, 2 larger. */
    var textSize: Int
        get() = sp.getInt("text_size", 0)
        set(v) = edit { putInt("text_size", v) }
    val textScale get() = when (textSize) { 1 -> 1.18f; 2 -> 1.36f; else -> 1f }
    /** Days added to the calculated Hijri date (local moon sighting). */
    var hijriAdjust: Int
        get() = sp.getInt("hijri_adjust", 0)
        set(v) = edit { putInt("hijri_adjust", v.coerceIn(-2, 2)) }

    // ----------------------------------------------------------------- more reminders

    /** Sleep adhkar reminder, minutes after midnight (-1 = off). */
    var sleepAt: Int
        get() = sp.getInt("sleep_at", 22 * 60 + 30)
        set(v) = edit { putInt("sleep_at", v) }
    /** A short dhikr every N hours between 9:00 and 21:00 (0 = off). */
    var salawatEvery: Int
        get() = sp.getInt("salawat_every", 0)
        set(v) = edit { putInt("salawat_every", v) }
    var kahf: Boolean
        get() = sp.getBoolean("kahf", true)
        set(v) = edit { putBoolean("kahf", v) }
    /** The evening before Monday, Thursday and the white days (13–15). */
    var fastReminder: Boolean
        get() = sp.getBoolean("fast_reminder", true)
        set(v) = edit { putBoolean("fast_reminder", v) }
    /** Minutes of Do Not Disturb after the adhan (0 = off). */
    var silentAfterAdhan: Int
        get() = sp.getInt("silent_after", 0)
        set(v) = edit { putInt("silent_after", v) }
    var focusStopsAtAdhan: Boolean
        get() = sp.getBoolean("focus_stop_adhan", true)
        set(v) = edit { putBoolean("focus_stop_adhan", v) }
    /** During Ramadan: a suhoor reminder N minutes before Fajr (0 = off). */
    var suhoorBefore: Int
        get() = sp.getInt("suhoor_before", 45)
        set(v) = edit { putInt("suhoor_before", v) }

    // ----------------------------------------------------------------- prayer log and qada

    fun prayed(date: LocalDate, p: Prayer) = sp.getStringSet("log_$date", emptySet())!!.contains(p.name)
    fun setPrayed(date: LocalDate, p: Prayer, on: Boolean) {
        val set = sp.getStringSet("log_$date", emptySet())!!.toMutableSet()
        if (on) set += p.name else set -= p.name
        edit { putStringSet("log_$date", set) }
    }
    /** (prayed, possible) over the last 7 days including today, counting only prayers whose time has come. */
    fun week(): Pair<Int, Int> {
        var done = 0; var total = 0
        val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        for (i in 0..6) {
            val d = LocalDate.now().minusDays(i.toLong())
            val times = if (i == 0) Store.times(d) else null
            for (p in Prayer.entries.filter { it.alertable }) {
                if (i == 0 && times!!.getValue(p) > nowMin) continue
                total++; if (prayed(d, p)) done++
            }
        }
        return done to total
    }
    fun qada(p: Prayer) = sp.getInt("qada_${p.name}", 0)
    fun setQada(p: Prayer, n: Int) = edit { putInt("qada_${p.name}", n.coerceAtLeast(0)) }

    // ----------------------------------------------------------------- built-in collections

    fun colCounts(id: String): IntArray {
        val size = Content.collection(id)?.items?.size ?: 0
        val a = sp.getString("col_${id}_${today()}", null)?.let { JSONArray(it) }
        return IntArray(size) { a?.optInt(it) ?: 0 }
    }
    fun colTap(id: String, index: Int): IntArray {
        val c = colCounts(id)
        if (index in c.indices) c[index]++
        edit { putString("col_${id}_${today()}", JSONArray(c.toList()).toString()) }
        return c
    }
    fun colReset(id: String) = edit { remove("col_${id}_${today()}") }
    fun colDone(id: String): Boolean {
        val items = Content.collection(id)?.items ?: return false
        val c = colCounts(id)
        return items.indices.all { c[it] >= items[it].count }
    }

    // ----------------------------------------------------------------- free tasbih

    var tasbih: Int
        get() = if (sp.getString("tasbih_day", "") == today()) sp.getInt("tasbih", 0) else 0
        private set(v) = edit { putInt("tasbih", v).putString("tasbih_day", today()) }
    var tasbihSet: Int
        get() = sp.getInt("tasbih_set", 33)
        set(v) = edit { putInt("tasbih_set", v) }
    fun tasbihTap(): Int {
        val n = tasbih + 1
        val days = JSONObject(sp.getString("tasbih_days", "{}")!!)
        days.put(today(), days.optInt(today()) + 1)
        // Keep 60 days.
        val keep = LocalDate.now().minusDays(60).toString()
        days.keys().asSequence().toList().filter { it < keep }.forEach { days.remove(it) }
        sp.edit().putString("tasbih_days", days.toString()).apply()
        tasbih = n
        return n
    }
    fun tasbihReset() = edit { putInt("tasbih", 0).putString("tasbih_day", today()) }
    /** Count per day, oldest first, for the last [n] days. */
    fun tasbihHistory(n: Int = 7): List<Pair<LocalDate, Int>> {
        val days = JSONObject(sp.getString("tasbih_days", "{}")!!)
        return (n - 1 downTo 0).map { LocalDate.now().minusDays(it.toLong()).let { d -> d to days.optInt(d.toString()) } }
    }

    // ----------------------------------------------------------------- habits

    data class Habit(val id: Long, val name: String, val days: Set<String>) {
        val doneToday get() = LocalDate.now().toString() in days
        /** Days in a row ending today (or yesterday, if today is not done yet). */
        val streak: Int get() {
            var d = if (doneToday) LocalDate.now() else LocalDate.now().minusDays(1)
            var n = 0
            while (d.toString() in days) { n++; d = d.minusDays(1) }
            return n
        }
    }
    private val defaultHabits = listOf("قيام الليل", "صلاة الضحى", "ورد القرآن", "صدقة", "بر الوالدين")
    fun habits(): List<Habit> {
        val raw = sp.getString("habits", null)
        if (raw == null) {
            val list = defaultHabits.mapIndexed { i, n -> Habit(i + 1L, n, emptySet()) }
            saveHabits(list); return list
        }
        val a = JSONArray(raw)
        return (0 until a.length()).map { a.getJSONObject(it).let { o ->
            val d = o.optJSONArray("days") ?: JSONArray()
            Habit(o.getLong("id"), o.getString("name"), (0 until d.length()).map(d::getString).toSet())
        } }
    }
    private fun saveHabits(list: List<Habit>) {
        val keep = LocalDate.now().minusDays(400).toString()
        val a = JSONArray(); list.forEach { h -> a.put(JSONObject().put("id", h.id).put("name", h.name).put("days", JSONArray(h.days.filter { it >= keep }.sorted()))) }
        edit { putString("habits", a.toString()) }
    }
    fun toggleHabit(id: Long) {
        val t = today()
        saveHabits(habits().map { if (it.id == id) it.copy(days = if (t in it.days) it.days - t else it.days + t) else it })
    }
    fun addHabit(name: String) = saveHabits(habits() + Habit(System.currentTimeMillis(), name, emptySet()))
    fun deleteHabit(id: Long) = saveHabits(habits().filterNot { it.id == id })

    // ----------------------------------------------------------------- Quran khatma (Madinah mushaf, 604 pages)

    const val PAGES = 604
    var khatmaPage: Int
        get() = sp.getInt("khatma_page", 0)
        set(v) {
            val old = khatmaPage
            val n = v.coerceIn(0, PAGES)
            val log = JSONObject(sp.getString("khatma_log", "{}")!!)
            if (n > old) log.put(today(), log.optInt(today()) + (n - old))
            edit { putInt("khatma_page", n).putString("khatma_log", log.toString()) }
        }
    /** Days to finish the khatma (30 = a juz a day). */
    var khatmaDays: Int
        get() = sp.getInt("khatma_days", 30)
        set(v) = edit { putInt("khatma_days", v) }
    var khatmaStarted: String
        get() = sp.getString("khatma_started", "") ?: ""
        set(v) = edit { putString("khatma_started", v) }
    var khatmaCount: Int
        get() = sp.getInt("khatma_count", 0)
        set(v) = edit { putInt("khatma_count", v) }
    /** Daily reminder of today's portion, minutes after midnight (-1 = off). */
    var khatmaRemindAt: Int
        get() = sp.getInt("khatma_remind", -1)
        set(v) = edit { putInt("khatma_remind", v) }
    fun pagesToday() = JSONObject(sp.getString("khatma_log", "{}")!!).optInt(today())
    fun juzOf(page: Int) = if (page <= 1) 1 else ((page - 2) / 20 + 1).coerceIn(1, 30)
    fun newKhatma() { khatmaCount = khatmaCount + if (khatmaPage >= PAGES) 1 else 0; edit { putInt("khatma_page", 0).putString("khatma_started", today()) } }

    // ----------------------------------------------------------------- dates

    fun isFastingEve(date: LocalDate): String? {
        val tomorrow = date.plusDays(1)
        val h = hijri(tomorrow)
        if (h.month == 9) return null // Ramadan: every day is a fast already
        val white = h.day in 13..15
        val weekday = tomorrow.dayOfWeek == DayOfWeek.MONDAY || tomorrow.dayOfWeek == DayOfWeek.THURSDAY
        return when {
            white && weekday -> "غدًا ${dayName(tomorrow)} ومن الأيام البيض"
            white -> "غدًا من الأيام البيض (${h.day} ${h.monthName})"
            weekday -> "غدًا ${dayName(tomorrow)}"
            else -> null
        }
    }
    private fun dayName(d: LocalDate) = if (d.dayOfWeek == DayOfWeek.MONDAY) "الاثنين" else if (d.dayOfWeek == DayOfWeek.THURSDAY) "الخميس" else ""

    /** How many times «آية أخرى» / «حديث آخر» was pressed today (kind = ayah or hadith). */
    fun dailyShift(kind: String, date: LocalDate = LocalDate.now()) = Store.prefs.getInt("shift_${kind}_$date", 0)
    fun nextDaily(kind: String) {
        val today = LocalDate.now().toString()
        val e = Store.prefs.edit()
        Store.prefs.all.keys.filter { it.startsWith("shift_${kind}_") && !it.endsWith(today) }.forEach(e::remove)
        e.putInt("shift_${kind}_$today", dailyShift(kind) + 1).apply(); Store.bump()
    }

    data class Hijri(val year: Int, val month: Int, val day: Int, val monthName: String)
    val months = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    fun hijri(date: LocalDate = LocalDate.now()): Hijri = runCatching {
        val h = java.time.chrono.HijrahDate.from(date.plusDays(hijriAdjust.toLong()))
        val y = h.get(java.time.temporal.ChronoField.YEAR); val m = h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR); val d = h.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
        Hijri(y, m, d, months[m - 1])
    }.getOrDefault(Hijri(0, 1, 1, ""))

    /** Gregorian date of a Hijri (month, day) at or after today (this year or next). */
    fun nextDate(month: Int, day: Int): LocalDate? = runCatching {
        val now = hijri()
        for (year in listOf(now.year, now.year + 1)) {
            val g = LocalDate.from(java.time.chrono.HijrahChronology.INSTANCE.date(year, month, day)).minusDays(hijriAdjust.toLong())
            if (!g.isBefore(LocalDate.now())) return g
        }
        null
    }.getOrNull()

    // ----------------------------------------------------------------- backup

    /** Everything ALzikr keeps, as one JSON object (for a file the user saves). Each value keeps its type. */
    fun export(): String {
        val data = JSONObject()
        sp.all.forEach { (k, v) ->
            val (t, value) = when (v) {
                is Set<*> -> "set" to JSONArray(v.map { it.toString() })
                is Boolean -> "b" to v
                is Int -> "i" to v
                is Long -> "l" to v
                is Float -> "f" to v.toDouble()
                else -> "s" to v.toString()
            }
            data.put(k, JSONObject().put("t", t).put("v", value))
        }
        return JSONObject().put("app", "ALzikr").put("version", 2).put("data", data).toString()
    }

    fun import(json: String): Boolean = runCatching {
        val o = JSONObject(json)
        if (o.optString("app") != "ALzikr") return false
        val data = o.getJSONObject("data")
        val e = sp.edit().clear()
        data.keys().forEach { k ->
            val x = data.getJSONObject(k)
            when (x.getString("t")) {
                "set" -> x.getJSONArray("v").let { a -> e.putStringSet(k, (0 until a.length()).map(a::getString).toSet()) }
                "b" -> e.putBoolean(k, x.getBoolean("v"))
                "i" -> e.putInt(k, x.getInt("v"))
                "l" -> e.putLong(k, x.getLong("v"))
                "f" -> e.putFloat(k, x.getDouble("v").toFloat())
                else -> e.putString(k, x.getString("v"))
            }
        }
        e.commit(); Store.bump(); true
    }.getOrDefault(false)
}
