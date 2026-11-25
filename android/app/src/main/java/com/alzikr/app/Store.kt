package com.alzikr.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Everything ALzikr keeps, in one SharedPreferences file ("alzikr"): settings, adhkar, tasks, notes
 * and focus statistics. Small JSON lists, so the app stays light with no database.
 * [version] changes on every write so Compose screens re-read.
 */
object Store {
    private lateinit var sp: SharedPreferences
    var version by mutableIntStateOf(0)
        private set

    /** The application, so a change can refresh the home-screen widgets and the note bubble. */
    private var app: Context? = null
    val appContext: Context? get() = app
    fun init(c: Context) {
        if (app == null) app = c.applicationContext
        if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("alzikr", Context.MODE_PRIVATE)
        // The Pet Engine: packs and the performance policy (both idempotent).
        com.alzikr.app.pets.PetPacks.init(c); com.alzikr.app.pets.PetPerf.init(c)
        com.alzikr.app.pets.PetPerf.userReduceMotion = sp.getBoolean("pet_reduce_motion", false)
        com.alzikr.app.pets.PetPerf.userSaveBattery = sp.getBoolean("pet_save_battery", false)
    }
    private fun changed() { version++ }
    /** For the other stores that share this file (Data.kt). */
    internal val prefs: SharedPreferences get() = sp
    internal fun bump() = changed()

    // ----------------------------------------------------------------- location and calculation

    val hasLocation get() = sp.contains("lat")
    val lat get() = sp.getFloat("lat", 21.4225f).toDouble()
    val lng get() = sp.getFloat("lng", 39.8262f).toDouble()
    val place get() = sp.getString("place", "") ?: ""
    fun setLocation(lat: Double, lng: Double, place: String) { sp.edit().putFloat("lat", lat.toFloat()).putFloat("lng", lng.toFloat()).putString("place", place).apply(); changed() }

    var method: Method
        get() = runCatching { Method.valueOf(sp.getString("method", null) ?: "") }.getOrDefault(Method.MWL)
        set(v) { sp.edit().putString("method", v.name).apply(); changed() }
    /** True once the user picked a method (or a city); until then it follows the country. */
    var methodChosen: Boolean
        get() = sp.getBoolean("method_chosen", false)
        set(v) { sp.edit().putBoolean("method_chosen", v).apply() }
    var country: String
        get() = sp.getString("country", "") ?: ""
        set(v) { sp.edit().putString("country", v).apply() }

    /** The method most mosques use in a country (ISO code). */
    fun methodFor(cc: String): Method = when (cc.uppercase()) {
        "SA", "BH", "OM", "YE" -> Method.MAKKAH
        "EG", "SY", "LB", "SD", "LY", "IQ", "MY", "ID", "SG", "BN" -> Method.EGYPT
        "JO", "PS" -> Method.JORDAN
        "AE" -> Method.DUBAI
        "QA" -> Method.QATAR
        "KW" -> Method.KUWAIT
        "TR" -> Method.TURKEY
        "PK", "IN", "BD", "AF" -> Method.KARACHI
        "US", "CA" -> Method.ISNA
        else -> Method.MWL
    }
    var hanafi: Boolean
        get() = sp.getBoolean("hanafi", false)
        set(v) { sp.edit().putBoolean("hanafi", v).apply(); changed() }

    /** Minutes added to a prayer's calculated time (to match the local mosque). */
    fun offset(p: Prayer) = sp.getInt("off_${p.name}", 0)
    fun setOffset(p: Prayer, m: Int) { sp.edit().putInt("off_${p.name}", m.coerceIn(-30, 30)).apply(); changed() }

    fun times(date: LocalDate = LocalDate.now()): Map<Prayer, Int> =
        PrayTimes.compute(date, lat, lng, method, if (hanafi) 2 else 1).mapValues { (p, m) -> m + offset(p) }

    // ----------------------------------------------------------------- alerts

    fun alert(p: Prayer) = sp.getBoolean("alert_${p.name}", p.alertable)
    fun setAlert(p: Prayer, on: Boolean) { sp.edit().putBoolean("alert_${p.name}", on).apply(); changed() }
    /** Minutes before each prayer for a heads-up (0 = none). */
    var before: Int
        get() = sp.getInt("before", 10)
        set(v) { sp.edit().putInt("before", v).apply(); changed() }
    /** Chosen alert sound (a ringtone / notification sound URI), null = the phone's default. */
    var sound: String?
        get() = sp.getString("sound", null)
        set(v) { sp.edit().putString("sound", v).apply(); changed() }
    var silent: Boolean
        get() = sp.getBoolean("silent", false)
        set(v) { sp.edit().putBoolean("silent", v).apply(); changed() }

    /** Morning adhkar reminder: minutes after Fajr (-1 = off). Evening: minutes after Asr. */
    var morningAfter: Int
        get() = sp.getInt("morning_after", 30)
        set(v) { sp.edit().putInt("morning_after", v).apply(); changed() }
    var eveningAfter: Int
        get() = sp.getInt("evening_after", 30)
        set(v) { sp.edit().putInt("evening_after", v).apply(); changed() }

    // ----------------------------------------------------------------- adhkar and awrad

    data class Zikr(val id: Long, val text: String, val target: Int, val count: Int, val day: String, val total: Long) {
        val today get() = if (day == LocalDate.now().toString()) count else 0
        val done get() = target > 0 && today >= target
    }

    private val defaults = listOf(
        "أستغفر الله وأتوب إليه" to 100,
        "سبحان الله وبحمده" to 100,
        "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير" to 10,
        "اللهم صلِّ وسلِّم على نبينا محمد" to 10,
        "سبحان الله" to 33,
        "الحمد لله" to 33,
        "الله أكبر" to 34,
        "لا حول ولا قوة إلا بالله" to 10,
    )

    fun adhkar(): List<Zikr> {
        val raw = sp.getString("adhkar", null)
        if (raw == null) {
            val list = defaults.mapIndexed { i, (t, n) -> Zikr(i + 1L, t, n, 0, "", 0) }
            saveAdhkar(list); return list
        }
        val a = JSONArray(raw)
        return (0 until a.length()).map { a.getJSONObject(it).let { o -> Zikr(o.getLong("id"), o.getString("text"), o.optInt("target"), o.optInt("count"), o.optString("day"), o.optLong("total")) } }
    }

    private fun saveAdhkar(list: List<Zikr>) {
        val a = JSONArray(); list.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("target", it.target).put("count", it.count).put("day", it.day).put("total", it.total)) }
        sp.edit().putString("adhkar", a.toString()).apply(); changed()
    }

    fun tap(id: Long): Zikr? {
        val today = LocalDate.now().toString()
        var hit: Zikr? = null
        saveAdhkar(adhkar().map { if (it.id == id) it.copy(count = it.today + 1, day = today, total = it.total + 1).also { z -> hit = z } else it })
        markStreak()
        return hit
    }
    fun resetToday(id: Long) = saveAdhkar(adhkar().map { if (it.id == id) it.copy(count = 0, day = LocalDate.now().toString()) else it })
    fun upsertZikr(id: Long?, text: String, target: Int) {
        val list = adhkar()
        saveAdhkar(if (id == null) list + Zikr(System.currentTimeMillis(), text, target, 0, "", 0) else list.map { if (it.id == id) it.copy(text = text, target = target) else it })
    }
    fun deleteZikr(id: Long) = saveAdhkar(adhkar().filterNot { it.id == id })
    fun moveZikr(id: Long, up: Boolean) {
        val list = adhkar().toMutableList(); val i = list.indexOfFirst { it.id == id }; val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        list[i] = list[j].also { list[j] = list[i] }; saveAdhkar(list)
    }

    /** Days in a row on which every daily wird (with a target) was completed. */
    val streak: Int get() {
        val last = sp.getString("streak_day", null)?.let(LocalDate::parse) ?: return 0
        val n = sp.getInt("streak", 0)
        return if (last == LocalDate.now() || last == LocalDate.now().minusDays(1)) n else 0
    }
    private fun markStreak() {
        val withTarget = adhkar().filter { it.target > 0 }
        if (withTarget.isEmpty() || !withTarget.all { it.done }) return
        val today = LocalDate.now()
        val last = sp.getString("streak_day", null)?.let(LocalDate::parse)
        if (last == today) return
        val n = if (last == today.minusDays(1)) sp.getInt("streak", 0) + 1 else 1
        sp.edit().putString("streak_day", today.toString()).putInt("streak", n).apply()
    }

    // ----------------------------------------------------------------- tasks

    /** One step of a task (subtasks). */
    data class Step(val text: String, val done: Boolean)
    /**
     * [after]: a prayer the task belongs after ("after Asr"), or null. [rule]: how it repeats ("" once, "daily",
     * "days:1,4" ISO weekdays, "monthly:15", "white" = the white days 13-15 of the Hijri month); a repeating task done on an
     * earlier day is open again. [due]: the day a one-off task is for ("" = any day). [cat]: its category.
     */
    data class Task(val id: Long, val text: String, val done: Boolean, val after: Prayer?, val doneAt: Long, val remindAt: Long = 0,
                    val rule: String = "", val steps: List<Step> = emptyList(), val cat: String = "", val due: String = "") {
        val repeat: Boolean get() = rule.isNotEmpty()
        /** Is it a task for [date]? */
        fun on(date: LocalDate): Boolean = when {
            rule == "daily" -> true
            rule.startsWith("days:") -> date.dayOfWeek.value.toString() in rule.removePrefix("days:").split(',')
            rule.startsWith("monthly:") -> date.dayOfMonth == rule.removePrefix("monthly:").toIntOrNull()
            rule == "white" -> runCatching { Data.hijri(date).day in 13..15 }.getOrDefault(false)
            else -> due.isEmpty() || due <= date.toString()
        }
    }

    fun tasks(): List<Task> {
        val a = JSONArray(sp.getString("tasks", "[]"))
        val today = LocalDate.now()
        val seen = HashSet<Long>()
        var repaired = false
        return (0 until a.length()).map { a.getJSONObject(it).let { o ->
            // Repair: a repeated id (from 1.5.0 test builds) gets a free one, so the lists never see the same key twice.
            var id = o.getLong("id"); while (!seen.add(id)) id++
            if (id != o.getLong("id")) { o.put("id", id); repaired = true }
            val rule = o.optString("rule").ifEmpty { if (o.optBoolean("repeat")) "daily" else "" }
            val doneAt = o.optLong("doneAt")
            // A repeating task done on an earlier day is open again today (its steps too).
            val stale = rule.isNotEmpty() && java.time.Instant.ofEpochMilli(doneAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() != today
            val done = o.optBoolean("done") && !stale
            val steps = o.optJSONArray("steps")?.let { st -> (0 until st.length()).map { i -> st.getJSONObject(i).let { x -> Step(x.getString("t"), x.optBoolean("d") && !stale) } } } ?: emptyList()
            Task(id, o.getString("text"), done, o.optString("after").takeIf { s -> s.isNotEmpty() }?.let { s -> runCatching { Prayer.valueOf(s) }.getOrNull() }, doneAt,
                o.optLong("remindAt"), rule, steps, o.optString("cat"), o.optString("due"))
        } }.also { if (repaired) sp.edit().putString("tasks", a.toString()).apply() }
    }
    private fun saveTasks(list: List<Task>) {
        val a = JSONArray(); list.forEach { t -> a.put(JSONObject().put("id", t.id).put("text", t.text).put("done", t.done).put("after", t.after?.name ?: "").put("doneAt", t.doneAt)
            .put("remindAt", t.remindAt).put("rule", t.rule).put("cat", t.cat).put("due", t.due)
            .put("steps", JSONArray().also { st -> t.steps.forEach { x -> st.put(JSONObject().put("t", x.text).put("d", x.done)) } })) }
        sp.edit().putString("tasks", a.toString()).apply(); changed()
        app?.let { runCatching { DayWidget.update(it) } }
    }
    fun addTask(text: String, after: Prayer?, remindAt: Long = 0, rule: String = "", cat: String = "", due: String = "") =
        tasks().let { list ->
            // Ids must be unique (they key the lists): a routine adds several in the same millisecond.
            val id = maxOf(System.currentTimeMillis(), (list.maxOfOrNull { it.id } ?: 0L) + 1)
            saveTasks(listOf(Task(id, text, false, after, 0, remindAt, rule, emptyList(), cat, due)) + list)
        }
    fun update(id: Long, f: (Task) -> Task) = saveTasks(tasks().map { if (it.id == id) f(it) else it })
    fun toggleRepeat(id: Long) = update(id) { it.copy(rule = if (it.repeat) "" else "daily") }
    fun renameTask(id: Long, text: String) = update(id) { it.copy(text = text) }
    /** A to-do reminder at [at] (epoch ms; 0 = none). Call TaskReminders.schedule afterwards. */
    fun setTaskReminder(id: Long, at: Long) = update(id) { it.copy(remindAt = at) }
    fun toggleTask(id: Long) = update(id) { it.copy(done = !it.done, doneAt = if (!it.done) System.currentTimeMillis() else 0) }
    fun toggleStep(id: Long, i: Int) = update(id) { t -> t.copy(steps = t.steps.mapIndexed { k, x -> if (k == i) x.copy(done = !x.done) else x }, doneAt = if (t.repeat) System.currentTimeMillis() else t.doneAt) }
    fun addStep(id: Long, text: String) = update(id) { it.copy(steps = it.steps + Step(text, false)) }
    fun removeStep(id: Long, i: Int) = update(id) { t -> t.copy(steps = t.steps.filterIndexed { k, _ -> k != i }) }
    fun deleteTask(id: Long) = saveTasks(tasks().filterNot { it.id == id })
    fun clearDone() = saveTasks(tasks().filterNot { it.done })
    fun openTasksAfter(p: Prayer) = tasks().filter { !it.done && it.after == p }

    // ----------------------------------------------------------------- notes

    /**
     * A note (Markdown-like text, see NoteText). [pinned]: at the top of the notes. [place]: where it hangs as a paper
     * scrap ("" none, "all", a tab id, or "calendar"). [widget]: shown on the home-screen widget; [bubble]: in the
     * floating bubble over other apps. [paper]: the paper's colour (0..5).
     */
    data class Note(val id: Long, val text: String, val at: Long, val pinned: Boolean = false, val place: String = "", val widget: Boolean = false, val bubble: Boolean = false, val paper: Int = 0)

    fun notes(): List<Note> {
        val a = JSONArray(sp.getString("notes", "[]"))
        return (0 until a.length()).map { a.getJSONObject(it).let { o -> Note(o.getLong("id"), o.getString("text"), o.optLong("at"), o.optBoolean("pinned"),
            o.optString("place"), o.optBoolean("widget"), o.optBoolean("bubble"), o.optInt("paper")) } }.sortedByDescending { it.pinned }
    }
    private fun saveNotes(list: List<Note>) {
        val a = JSONArray(); list.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("at", it.at).put("pinned", it.pinned)
            .put("place", it.place).put("widget", it.widget).put("bubble", it.bubble).put("paper", it.paper)) }
        sp.edit().putString("notes", a.toString()).apply(); changed()
        app?.let { runCatching { NoteWidget.update(it); NoteBubble.refresh(it) } }
    }
    fun saveNote(id: Long?, text: String) {
        val now = System.currentTimeMillis()
        val list = notes()
        val old = list.firstOrNull { it.id == id }
        saveNotes(if (id == null || old == null) listOf(Note(now, text, now)) + list else listOf(old.copy(text = text, at = now)) + list.filterNot { it.id == id })
    }
    fun deleteNote(id: Long) = saveNotes(notes().filterNot { it.id == id })
    fun togglePin(id: Long) = saveNotes(notes().map { if (it.id == id) it.copy(pinned = !it.pinned) else it })
    fun updateNote(id: Long, f: (Note) -> Note) = saveNotes(notes().map { if (it.id == id) f(it) else it })
    /** Only one note on the widget, and one in the bubble: choosing one takes it from the other. */
    fun noteOnWidget(id: Long, on: Boolean) = saveNotes(notes().map { it.copy(widget = if (it.id == id) on else it.widget && !on) })
    fun noteInBubble(id: Long, on: Boolean) = saveNotes(notes().map { it.copy(bubble = if (it.id == id) on else it.bubble && !on) })

    // ----------------------------------------------------------------- focus

    /** End of the running focus session (epoch ms), 0 when none. */
    var focusEnd: Long
        get() = sp.getLong("focus_end", 0)
        set(v) { sp.edit().putLong("focus_end", v).apply(); changed() }
    var focusMinutes: Int
        get() = sp.getInt("focus_minutes", 25)
        set(v) { sp.edit().putInt("focus_minutes", v).apply(); changed() }
    var focusIsBreak: Boolean
        get() = sp.getBoolean("focus_break", false)
        set(v) { sp.edit().putBoolean("focus_break", v).apply(); changed() }
    var focusTitle: String
        get() = sp.getString("focus_title", "") ?: ""
        set(v) { sp.edit().putString("focus_title", v).apply(); changed() }

    fun focusToday(): Pair<Int, Int> { // (sessions, minutes)
        val today = LocalDate.now().toString()
        return if (sp.getString("focus_day", "") == today) sp.getInt("focus_sessions", 0) to sp.getInt("focus_total", 0) else 0 to 0
    }
    fun addFocus(minutes: Int) {
        val (s, m) = focusToday()
        val hist = JSONObject(sp.getString("focus_hist", "{}")!!)
        val day = LocalDate.now().toString()
        hist.put(day, hist.optInt(day) + minutes)
        sp.edit().putString("focus_day", day).putInt("focus_sessions", s + 1).putInt("focus_total", m + minutes).putString("focus_hist", hist.toString()).apply(); changed()
    }
    /** Focus minutes on each of the last 7 days, oldest first. */
    fun focusWeek(): List<Pair<LocalDate, Int>> {
        val hist = JSONObject(sp.getString("focus_hist", "{}")!!)
        return (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }.map { d -> d to (hist.optInt(d.toString()) + if (d == LocalDate.now() && hist.optInt(d.toString()) == 0) focusToday().second else 0) }
    }
    /** The task a focus session is for (0 = none): its end offers «أنجزت المهمة». */
    var focusTask: Long
        get() = sp.getLong("focus_task", 0)
        set(v) { sp.edit().putLong("focus_task", v).apply(); changed() }
}
