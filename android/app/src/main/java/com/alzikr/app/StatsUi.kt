package com.alzikr.app

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
import androidx.compose.material3.Text
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
        val items = Content.collection(id)?.items ?: return false
        val a = Store.prefs.getString("col_${id}_$d", null)?.let { runCatching { org.json.JSONArray(it) }.getOrNull() } ?: return false
        return items.indices.all { a.optInt(it) >= items[it].count }
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

        // Headline numbers.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Big("🔥", "${Stats.prayerStreak()}", "أيام متتالية بالصلوات الخمس", Modifier.weight(1f))
            val (w, p) = Data.week()
            Big("🕌", "$w/$p", "صلوات آخر ٧ أيام", Modifier.weight(1f))
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
            Title("كل صلاة في آخر ٣٠ يومًا")
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
