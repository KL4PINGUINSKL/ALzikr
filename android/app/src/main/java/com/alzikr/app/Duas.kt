package com.alzikr.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/** «أدعيتي»: your own duas by topic, the people you promised to pray for, and reminders at the times of answer. */
object Duas {
    data class Dua(val id: Long, val text: String, val cat: Int)
    data class Person(val id: Long, val name: String, val note: String)
    val cats = listOf("لنفسي", "لوالديّ", "لأهلي وذريتي", "للمسلمين", "أخرى")

    private val sp get() = Store.prefs
    fun duas(): List<Dua> = runCatching { JSONArray(sp.getString("my_duas", "[]")).let { a -> (0 until a.length()).map { a.getJSONObject(it).let { o -> Dua(o.getLong("id"), o.getString("t"), o.getInt("c")) } } } }.getOrDefault(emptyList())
    private fun saveDuas(l: List<Dua>) { val a = JSONArray(); l.forEach { a.put(JSONObject().put("id", it.id).put("t", it.text).put("c", it.cat)) }; sp.edit().putString("my_duas", a.toString()).apply(); Store.bump() }
    fun add(text: String, cat: Int) = saveDuas(duas() + Dua(System.currentTimeMillis(), text.trim(), cat))
    fun update(d: Dua) = saveDuas(duas().map { if (it.id == d.id) d else it })
    fun delete(id: Long) = saveDuas(duas().filterNot { it.id == id })

    fun people(): List<Person> = runCatching { JSONArray(sp.getString("pray_for", "[]")).let { a -> (0 until a.length()).map { a.getJSONObject(it).let { o -> Person(o.getLong("id"), o.getString("n"), o.optString("note")) } } } }.getOrDefault(emptyList())
    private fun savePeople(l: List<Person>) { val a = JSONArray(); l.forEach { a.put(JSONObject().put("id", it.id).put("n", it.name).put("note", it.note)) }; sp.edit().putString("pray_for", a.toString()).apply(); Store.bump() }
    fun addPerson(name: String, note: String) = savePeople(people() + Person(System.currentTimeMillis(), name.trim(), note.trim()))
    fun deletePerson(id: Long) = savePeople(people().filterNot { it.id == id })

    /** Reminders at the times of answer. */
    var remindLastThird: Boolean
        get() = sp.getBoolean("dua_third", false)
        set(v) { sp.edit().putBoolean("dua_third", v).apply(); Store.bump() }
    var remindFriday: Boolean
        get() = sp.getBoolean("dua_friday", true)
        set(v) { sp.edit().putBoolean("dua_friday", v).apply(); Store.bump() }
    /** Between the adhan and the iqama of this prayer (null = off). */
    var remindAfterAdhan: Prayer?
        get() = sp.getString("dua_adhan", null)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        set(v) { sp.edit().putString("dua_adhan", v?.name).apply(); Store.bump() }

    /** The reminder events of [date] (minutes from midnight), for Alerts. */
    fun events(date: LocalDate, t: Map<Prayer, Int>): List<Int> {
        val out = ArrayList<Int>()
        remindAfterAdhan?.let { out += t.getValue(it) + 3 }
        if (remindFriday && date.dayOfWeek == java.time.DayOfWeek.FRIDAY) out += t.getValue(Prayer.MAGHRIB) - 45
        if (remindLastThird) {
            val fajrNext = Store.times(date.plusDays(1)).getValue(Prayer.FAJR) + 1440
            val m = t.getValue(Prayer.MAGHRIB); out += (m + (fajrNext - m) * 2 / 3) % 1440
        }
        return out
    }
    fun reminderText(): String {
        val names = people().map { it.name }
        val d = duas().randomOrNull()?.text
        return listOfNotNull(if (names.isNotEmpty()) "ادعُ لـ: " + names.take(6).joinToString("، ") else null, d).joinToString(" · ").ifBlank { "وقت يُرجى فيه قبول الدعاء، فادعُ الله" }
    }
    fun whichTime(): String {
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        val t = Store.times()
        return when {
            LocalDate.now().dayOfWeek == java.time.DayOfWeek.FRIDAY && now in t.getValue(Prayer.ASR)..t.getValue(Prayer.MAGHRIB) -> "آخر ساعة من يوم الجمعة"
            now < t.getValue(Prayer.FAJR) || now > t.getValue(Prayer.ISHA) + 120 -> "الثلث الأخير من الليل"
            else -> "بين الأذان والإقامة"
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DuasPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    var adding by remember { mutableStateOf<Duas.Dua?>(null) }
    var addingPerson by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<Duas.Dua?>(null) }
    var cat by remember { mutableStateOf(-1) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Hint("duas", "🤲", "اكتب أدعيتك الخاصة، وأسماء من وعدتهم بالدعاء، ويذكّرك التطبيق في أوقات الإجابة: الثلث الأخير، وآخر ساعة يوم الجمعة، وبين الأذان والإقامة. ضغطة مطوّلة على الدعاء لتعديله أو حذفه.")

        // The people.
        Panel(pad = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Title("وعدتهم بالدعاء", Modifier.weight(1f))
                Text("+ اسم", color = P.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(L.shape(10.dp)).clickable { addingPerson = true }.padding(8.dp))
            }
            val people = Duas.people()
            if (people.isEmpty()) Sub("من طلب منك الدعاء، اكتب اسمه هنا حتى لا تنساه.", size = 12.sp)
            people.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        if (p.note.isNotBlank()) Sub(p.note, size = 12.sp)
                    }
                    Text("✕", color = P.muted, fontSize = 15.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Duas.deletePerson(p.id) }.padding(8.dp))
                }
            }
        }

        // My duas, by topic.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Title("أدعيتي", Modifier.weight(1f))
            Text("+ دعاء", color = P.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(L.shape(10.dp)).clickable { adding = Duas.Dua(0, "", maxOf(cat, 0)) }.padding(8.dp))
        }
        PickOne(listOf("-1" to "الكل") + Duas.cats.mapIndexed { i, c -> i.toString() to c }, cat.toString(), "التصنيف", shown = 3) { cat = it.toInt() }
        val list = Duas.duas().filter { cat == -1 || it.cat == cat }
        if (list.isEmpty()) Sub("لا أدعية هنا بعد. اضغط «+ دعاء»، أو خذ من «أدعية من القرآن» و«أدعية من السنة» في الأذكار.", size = 12.sp)
        list.forEach { d ->
            Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card.seeThrough()).border(1.dp, P.line, L.shape(14.dp))
                .combinedClickable(onClick = {}, onLongClick = { menu = d }).padding(14.dp)) {
                Sub(Duas.cats.getOrElse(d.cat) { "" }, color = P.gold, size = 11.sp)
                Text(d.text, color = P.ink, fontSize = (17 * Data.textScale).sp, lineHeight = (29 * Data.textScale).sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("📖", "أدعية من القرآن", Modifier.weight(1f)) { Nav.collection = "quran_duas"; Nav.go("adhkar") }
            Tile("📚", "أدعية من السنة", Modifier.weight(1f)) { Nav.collection = "sunnah_duas"; Nav.go("adhkar") }
        }

        // Reminders.
        Panel(pad = 14.dp) {
            Title("التذكير في أوقات الإجابة")
            val re = { Alerts.reschedule(ctx) }
            SettingRow("الثلث الأخير من الليل", "«ينزل ربنا… فيقول: من يدعوني فأستجيب له» (البخاري ومسلم)") { Switch(Duas.remindLastThird, { Duas.remindLastThird = it; re() }) }
            SettingRow("آخر ساعة يوم الجمعة", "قبل المغرب بـ 45 دقيقة (أبو داود والنسائي)") { Switch(Duas.remindFriday, { Duas.remindFriday = it; re() }) }
            Sub("بين الأذان والإقامة (الترمذي وأبو داود): لصلاة", Modifier.padding(top = 8.dp))
            Wrap(Modifier.padding(top = 6.dp), gap = 6.dp) {
                Choice("لا", Duas.remindAfterAdhan == null) { Duas.remindAfterAdhan = null; re() }
                Prayer.entries.filter { it.alertable }.forEach { p -> Choice(p.base, Duas.remindAfterAdhan == p) { Duas.remindAfterAdhan = p; re() } }
            }
        }
    }

    adding?.let { d ->
        var text by remember(d.id) { mutableStateOf(d.text) }
        var c by remember(d.id) { mutableStateOf(d.cat) }
        AlertDialog(onDismissRequest = { adding = null }, containerColor = P.surface,
            title = { Text(if (d.id == 0L) "دعاء جديد" else "تعديل الدعاء", color = P.ink) },
            text = {
                Column {
                    OutlinedTextField(text, { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth(), placeholder = { Text("اللهم…") })
                    PickOne(Duas.cats.mapIndexed { i, n -> i.toString() to n }, c.toString(), "التصنيف", modifier = Modifier.padding(top = 8.dp)) { c = it.toInt() }
                }
            },
            confirmButton = { TextButton({ if (text.isNotBlank()) { if (d.id == 0L) Duas.add(text, c) else Duas.update(d.copy(text = text.trim(), cat = c)) }; adding = null }) { Text("حفظ") } },
            dismissButton = { TextButton({ adding = null }) { Text("إلغاء") } })
    }
    menu?.let { d ->
        AlertDialog(onDismissRequest = { menu = null }, containerColor = P.surface, title = { Text(d.text.take(60), color = P.ink, fontSize = 15.sp) },
            text = {
                Column {
                    listOf("تعديل" to { adding = d; menu = null }, "نسخ" to { copy(ctx, d.text); menu = null },
                        "مشاركة صورةً" to { ShareCard.share(ctx, "دعاء", d.text, ""); menu = null }, "حذف" to { Duas.delete(d.id); menu = null }).forEach { (l, act) ->
                        Text(l, color = if (l == "حذف") P.danger else P.ink, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).clickable { act() }.padding(10.dp))
                    }
                }
            }, confirmButton = { TextButton({ menu = null }) { Text("إغلاق") } })
    }
    if (addingPerson) {
        var name by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { addingPerson = false }, containerColor = P.surface, title = { Text("وعدتُ بالدعاء لـ", color = P.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(note, { note = it }, singleLine = true, placeholder = { Text("حاجته (اختياري): الشفاء، التوفيق…") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton({ if (name.isNotBlank()) Duas.addPerson(name, note); addingPerson = false }) { Text("حفظ") } },
            dismissButton = { TextButton({ addingPerson = false }) { Text("إلغاء") } })
    }
}

/** A verse, hadith or dua as a picture in the theme's colors (Amiri, centered, with its source), then the share sheet. */
object ShareCard {
    /** [note]: an optional explanation under the text (e.g. a verse's tafsir, with [noteTitle]). */
    fun share(c: Context, title: String, text: String, src: String, quran: Boolean = false, note: String = "", noteTitle: String = "") = runCatching {
        val pal = ThemeColors.of(c)
        val w = 1080
        val res = c.resources
        val face = runCatching { androidx.core.content.res.ResourcesCompat.getFont(c, if (quran) R.font.amiri_quran else R.font.amiri) }.getOrNull() ?: android.graphics.Typeface.SERIF
        val body = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { typeface = face; textSize = if (text.length > 400) 40f else if (text.length > 200) 48f else 58f; color = pal.ink.toArgb() }
        val layout = android.text.StaticLayout.Builder.obtain(text, 0, text.length, body, w - 200)
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, if (quran) 1.55f else 1.35f)
            .setTextDirection(android.text.TextDirectionHeuristics.RTL).build()
        val notePaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { typeface = runCatching { androidx.core.content.res.ResourcesCompat.getFont(c, R.font.amiri) }.getOrNull() ?: android.graphics.Typeface.SERIF; textSize = 34f; color = pal.ink.copy(alpha = .85f).toArgb() }
        val noteLayout = note.takeIf { it.isNotBlank() }?.let { n -> android.text.StaticLayout.Builder.obtain(n, 0, n.length, notePaint, w - 200)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.3f).setTextDirection(android.text.TextDirectionHeuristics.RTL).build() }
        val extra = noteLayout?.let { it.height + 140 } ?: 0
        val h = (layout.height + 560 + extra).coerceAtLeast(900)
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(bmp)
        cv.drawColor(pal.bg.toArgb())
        val card = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = pal.surface.toArgb() }
        cv.drawRoundRect(40f, 40f, w - 40f, h - 40f, 48f, 48f, card)
        val frame = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = pal.gold.copy(alpha = .55f).toArgb(); style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f }
        cv.drawRoundRect(70f, 70f, w - 70f, h - 70f, 36f, 36f, frame)
        val small = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textAlign = android.graphics.Paint.Align.CENTER; typeface = face }
        small.color = pal.gold.toArgb(); small.textSize = 44f
        cv.drawText(title, w / 2f, 190f, small)
        cv.save(); cv.translate(100f, 270f); layout.draw(cv); cv.restore()
        small.color = pal.muted.toArgb(); small.textSize = 34f
        if (src.isNotBlank()) cv.drawText(src, w / 2f, 270f + layout.height + 90f, small)
        noteLayout?.let { nl ->
            val top = 270f + layout.height + 150f
            small.color = pal.gold.toArgb(); small.textSize = 34f
            cv.drawText(noteTitle, w / 2f, top, small)
            cv.save(); cv.translate(100f, top + 30f); nl.draw(cv); cv.restore()
            small.color = pal.muted.toArgb()
        }
        small.textSize = 30f
        cv.drawText("ALzikr", w / 2f, h - 110f, small)
        val dir = java.io.File(c.cacheDir, "share").apply { mkdirs() }
        val f = java.io.File(dir, "alzikr-card.png")
        f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(c, c.packageName + ".files", f)
        c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text + if (src.isNotBlank()) "\n[$src]" else "").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "مشاركة صورة"))
        @Suppress("UNUSED_VARIABLE") val unused = res
    }
}
