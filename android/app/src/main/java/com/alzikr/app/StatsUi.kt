package com.alzikr.app

import androidx.compose.foundation.clickable

import androidx.compose.foundation.border

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** «إحصائياتي»: prayers, adhkar, khatma and tasbih over the last days, so progress is visible. */
object Stats {
    private val five = Prayer.entries.filter { it.alertable }
    fun prayedOn(d: LocalDate) = five.count { Data.prayed(d, it) }
    /** Days in a row (ending today or yesterday) with all five prayers marked. */
    fun prayerStreak(): Int {
        var d = LocalDate.now(); if (prayedOn(d) < 5) d = d.minusDays(1)
        var n = 0
        while (n < 3650 && prayedOn(d) == 5) { n++; d = d.minusDays(1) }
        return n
    }
    /** A collection finished on [d]. */
    fun colDoneOn(id: String, d: LocalDate): Boolean {
        if (id == "prayer") return Content.afterPrayer.any { colDoneOn(it.id, d) } || colDoneOn0("prayer", d)
        return colDoneOn0(id, d)
    }
    private fun colDoneOn0(id: String, d: LocalDate): Boolean {
        val items = Content.collection(id)?.items ?: return false
        val a = Store.prefs.getString("col_${id}_$d", null)?.let { runCatching { org.json.JSONArray(it) }.getOrNull() } ?: return false
        return items.indices.all { a.optInt(it) >= items[it].count }
    }
    fun rawatibOn(d: LocalDate) = listOf("r_FAJR", "r_DHUHR", "r_MAGHRIB", "r_ISHA").count { Data.extra(d, it) }
    fun fastsThisMonth(): Int {
        val h = Data.hijri(); var n = 0
        for (i in 0 until 31) { val d = LocalDate.now().minusDays(i.toLong()); val hd = Data.hijri(d); if (hd.month != h.month || hd.year != h.year) break; if (Data.extra(d, "fast")) n++ }
        return n
    }
    fun fastsThisYear(): Int {
        val y = Data.hijri().year; var n = 0
        for (i in 0 until 360) { val d = LocalDate.now().minusDays(i.toLong()); if (Data.hijri(d).year != y) break; if (Data.extra(d, "fast")) n++ }
        return n
    }

    /** A plain summary to send to someone. */
    fun text(): String {
        val (w, p) = Data.week()
        val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        val col = listOf("morning" to "الصباح", "evening" to "المساء", "sleep" to "النوم").joinToString("، ") { (id, n) -> "$n ${days.count { colDoneOn(id, it) }}/7" }
        return buildString {
            appendLine("إحصائياتي في ALzikr 🌙")
            appendLine("• الصلوات في آخر 7 أيام: $w من $p")
            appendLine("• أيام متتالية بالصلوات الخمس: ${prayerStreak()}")
            appendLine("• الرواتب هذا الأسبوع: ${days.sumOf { rawatibOn(it) }} من 28 · الوتر: ${days.count { Data.extra(it, "witr") }}/7")
            appendLine("• الأذكار هذا الأسبوع: $col")
            appendLine("• الختمة: ${Data.khatmaPage * 100 / Data.PAGES}٪" + if (Data.khatmaCount > 0) " (ختمات مكتملة: ${Data.khatmaCount})" else "")
            appendLine("• صيام هذا الشهر الهجري: ${fastsThisMonth()} يومًا")
            appendLine("• التسبيح هذا الأسبوع: ${Data.tasbihHistory(7).sumOf { it.second }}")
            append("\nALzikr: $APP_LINK")
        }
    }

    /** The same summary as a picture in the theme's colors, then the share sheet. */
    fun share(c: Context) {
        val pal = ThemeColors.of(c)
        val w = 1080; val h = 1350
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(bmp)
        cv.drawColor(pal.bg.toArgb())
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textAlign = android.graphics.Paint.Align.RIGHT }
        fun text(t: String, y: Float, size: Float, color: Int, bold: Boolean = false) {
            paint.color = color; paint.textSize = size; paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            cv.drawText(t, w - 80f, y, paint)
        }
        val card = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = pal.surface.toArgb() }
        cv.drawRoundRect(40f, 40f, w - 40f, h - 40f, 48f, 48f, card)
        text("إحصائياتي", 160f, 76f, pal.ink.toArgb(), true)
        val hj = Data.hijri()
        text("${hj.day} ${hj.monthName} ${hj.year} هـ", 225f, 36f, pal.gold.toArgb())
        val (wk, pos) = Data.week()
        val rows = listOf(
            "الصلوات في آخر 7 أيام" to "$wk / $pos", "أيام متتالية بالصلوات الخمس" to "${prayerStreak()}",
            "الختمة" to "${Data.khatmaPage * 100 / Data.PAGES}٪", "صيام هذا الشهر" to "${fastsThisMonth()}",
        )
        var y = 330f
        rows.forEach { (k, v) ->
            text(k, y, 40f, pal.muted.toArgb())
            paint.textAlign = android.graphics.Paint.Align.LEFT; paint.color = pal.accent.toArgb(); paint.textSize = 52f; paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            cv.drawText(v, 80f, y, paint); paint.textAlign = android.graphics.Paint.Align.RIGHT
            y += 105f
        }
        // This week's prayers as bars (right to left, today on the left end).
        text("الصلوات هذا الأسبوع", y + 30f, 40f, pal.ink.toArgb(), true)
        val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        val base = y + 330f; val bw = 90f; val gap = (w - 160f - bw * 7) / 6
        val bar = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        days.forEachIndexed { i, d ->
            val n = prayedOn(d)
            val x = w - 80f - bw - i * (bw + gap)
            bar.color = (if (n == 5) pal.gold else pal.accent).toArgb()
            cv.drawRoundRect(x, base - 40f * n.coerceAtLeast(0) - 8f, x + bw, base, 16f, 16f, bar)
            paint.textAlign = android.graphics.Paint.Align.CENTER; paint.color = pal.muted.toArgb(); paint.textSize = 30f; paint.typeface = android.graphics.Typeface.DEFAULT
            cv.drawText(PrayerAlarm.dayNames.first { it.first == d.dayOfWeek.value }.second.take(3), x + bw / 2, base + 44f, paint)
            paint.textAlign = android.graphics.Paint.Align.RIGHT
        }
        paint.textAlign = android.graphics.Paint.Align.CENTER; paint.color = pal.muted.toArgb(); paint.textSize = 32f
        cv.drawText("ALzikr", w / 2f, h - 90f, paint)
        val dir = java.io.File(c.cacheDir, "share").apply { mkdirs() }
        val f = java.io.File(dir, "alzikr-stats.png")
        f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(c, c.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, text())
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        c.startActivity(Intent.createChooser(send, "مشاركة إحصائياتي"))
    }

    fun perPrayer(days: Int): List<Pair<Prayer, Int>> = five.map { p -> p to (0 until days).count { Data.prayed(LocalDate.now().minusDays(it.toLong()), p) } }
}

@Composable
fun StatsPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    fun dayName(d: LocalDate) = PrayerAlarm.dayNames.first { it.first == d.dayOfWeek.value }.second.take(3)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint("stats", "📊", "تقدّمك في الصلاة والأذكار والختمة والتسبيح. علّم ✓ بعد كل صلاة، وأتمّ الأذكار، لتكتمل الصورة.")
        val ctx = androidx.compose.ui.platform.LocalContext.current
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("↗ مشاركة صورة الإحصائيات", Modifier.weight(1f)) { runCatching { Stats.share(ctx) } }
        }

        // Headline numbers.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Big("🔥", "${Stats.prayerStreak()}", "أيام متتالية بالصلوات الخمس", Modifier.weight(1f))
            val (w, p) = Data.week()
            Big("🕌", "$w/$p", "صلوات آخر 7 أيام", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Big("📗", "${Data.khatmaPage * 100 / Data.PAGES}٪", "من ختمتك" + if (Data.khatmaCount > 0) " · ختمات: ${Data.khatmaCount}" else "", Modifier.weight(1f))
            Big("📿", "${Data.tasbihHistory(7).sumOf { it.second }}", "تسبيحة هذا الأسبوع", Modifier.weight(1f))
        }

        // Prayers: each day of the week.
        Panel(pad = 14.dp) {
            Title("الصلوات هذا الأسبوع")
            Row(Modifier.fillMaxWidth().height(120.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                days.forEach { d ->
                    val n = Stats.prayedOn(d)
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("$n", color = if (n == 5) P.gold else P.muted, fontSize = 11.sp)
                        Box(Modifier.width(20.dp).height((14 * n).coerceAtLeast(3).dp).clip(RoundedCornerShape(5.dp)).background(if (n == 5) P.gold else if (d == today) P.accent else P.accent.copy(alpha = .5f)))
                        Spacer(Modifier.height(4.dp))
                        Text(dayName(d), color = if (d == today) P.ink else P.muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Which prayer is missed most (last 30 days).
        Panel(pad = 14.dp) {
            Title("كل صلاة في آخر 30 يومًا")
            Stats.perPrayer(30).forEach { (p, n) ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.ar, color = P.ink, fontSize = 14.sp, modifier = Modifier.width(64.dp))
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(P.line)) {
                        Box(Modifier.fillMaxWidth(n / 30f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(if (n >= 28) P.gold else P.accent))
                    }
                    Text("$n", color = P.muted, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                }
            }
            Sub("الأيام التي لم تُعلَّم فيها الصلاة تُحسب غير مصلّاة.", Modifier.padding(top = 8.dp), size = 11.sp)
        }

        // The sunnah prayers of the week.
        Panel(pad = 14.dp) {
            Title("السنن هذا الأسبوع")
            listOf("rawatib" to "الرواتب", "witr" to "الوتر", "duha" to "الضحى", "qiyam" to "القيام").forEach { (k, name) ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = P.ink, fontSize = 14.sp, modifier = Modifier.width(80.dp))
                    days.forEach { d ->
                        val n = if (k == "rawatib") Stats.rawatibOn(d) else if (Data.extra(d, k)) 1 else 0
                        val full = if (k == "rawatib") n == 4 else n == 1
                        Box(Modifier.weight(1f).padding(horizontal = 2.dp).height(22.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (full) P.gold else if (n > 0) P.accent.copy(alpha = .6f) else P.line.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
                            if (k == "rawatib" && n > 0) Text("$n", color = P.onAccent, fontSize = 10.sp) else if (full) Text("✓", color = P.onAccent, fontSize = 11.sp)
                        }
                    }
                }
            }
            Sub("علّمها من صفحة كل صلاة (اضغط على الصلاة في الرئيسية).", Modifier.padding(top = 8.dp), size = 11.sp)
        }

        // Fasting.
        Panel(pad = 14.dp) {
            Title("صيامي")
            val fasted = Data.extra(today, "fast")
            Text(if (fasted) "✓ صمت اليوم، تقبّل الله" else "صمت اليوم؟ اضغط هنا", color = if (fasted) P.onAccent else P.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(14.dp)).background(if (fasted) P.accent else P.card).border(1.dp, P.accent, L.shape(14.dp))
                    .clickable { Data.setExtra(today, "fast", !fasted) }.padding(vertical = 12.dp))
            Sub("هذا الشهر الهجري: ${Stats.fastsThisMonth()} يومًا · هذا العام: ${Stats.fastsThisYear()} يومًا", Modifier.padding(top = 8.dp))
            Data.isFastingEve(today)?.let { Sub("غدًا: $it", color = P.gold, size = 12.sp) }
        }

        // Morning, evening and sleep adhkar on each day.
        Panel(pad = 14.dp) {
            Title("أذكار هذا الأسبوع")
            listOf("morning" to "الصباح", "evening" to "المساء", "sleep" to "النوم", "prayer" to "بعد الصلاة").forEach { (id, name) ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = P.ink, fontSize = 14.sp, modifier = Modifier.width(80.dp))
                    days.forEach { d ->
                        val ok = Stats.colDoneOn(id, d)
                        Box(Modifier.weight(1f).padding(horizontal = 2.dp).height(22.dp).clip(RoundedCornerShape(6.dp)).background(if (ok) P.accent else P.line.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
                            if (ok) Text("✓", color = P.onAccent, fontSize = 11.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Spacer(Modifier.width(80.dp))
                days.forEach { d -> Text(dayName(d), color = P.muted, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Big(icon: String, value: String, label: String, modifier: Modifier) {
    Panel(modifier, color = P.card, pad = 14.dp) {
        Glyph(icon, tint = P.accent, size = 24.dp)
        Text(value, color = P.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Sub(label, size = 12.sp)
    }
}

/** Home-screen widget: «ذكر الوقت» — the adhkar that fit now; a tap opens them. */
class MomentWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) { update(c) }

    companion object {
        fun update(c: Context) {
            val manager = AppWidgetManager.getInstance(c)
            val ids = manager.getAppWidgetIds(ComponentName(c, MomentWidget::class.java))
            if (ids.isEmpty()) return
            Store.init(c)
            val m = runCatching { Moments.now().firstOrNull { !Moments.done(it) } }.getOrNull()
            val pal = ThemeColors.of(c)
            val v = RemoteViews(c.packageName, R.layout.widget_moment)
            ThemeColors.background(v, R.id.m_root, pal.surface)
            v.setTextColor(R.id.m_label, pal.gold.toArgb()); v.setTextColor(R.id.m_title, pal.ink.toArgb()); v.setTextColor(R.id.m_text, pal.muted.toArgb())
            v.setTextViewText(R.id.m_label, "ذكر الوقت")
            v.setTextViewText(R.id.m_title, m?.title ?: "أتممت ذكر الوقت ✓")
            v.setTextViewText(R.id.m_text, m?.text ?: "تقبّل الله منك")
            val open = Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (m?.coll != null) open.putExtra("coll", m.coll) else open.putExtra("tab", "prayer")
            v.setOnClickPendingIntent(R.id.m_root, PendingIntent.getActivity(c, 90, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            ids.forEach { manager.updateAppWidget(it, v) }
        }
    }
}
