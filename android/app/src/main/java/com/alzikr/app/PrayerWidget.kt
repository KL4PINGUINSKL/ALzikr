package com.alzikr.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb

/**
 * Home-screen widget: the next prayer, a live countdown (drawn by Android, no updates needed in
 * between), the Hijri date and today's five times. Refreshed at each prayer and whenever alerts
 * are rescheduled.
 */
class PrayerWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) { Store.init(c); updateAll(c) }
    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        if (i.action == TICK) { Store.init(c); updateAll(c) }
    }

    companion object {
        private const val TICK = "com.alzikr.app.WIDGET_TICK"
        private val ids = listOf(R.id.w_t0, R.id.w_t1, R.id.w_t2, R.id.w_t3, R.id.w_t4)

        fun updateAll(c: Context) {
            val manager = AppWidgetManager.getInstance(c)
            val widgets = manager.getAppWidgetIds(ComponentName(c, PrayerWidget::class.java))
            if (widgets.isEmpty()) return
            Store.init(c)
            val v = RemoteViews(c.packageName, R.layout.widget_prayer)
            val pal = ThemeColors.of(c)
            ThemeColors.background(v, R.id.w_root, pal.surface)
            v.setTextColor(R.id.w_name, pal.ink.toArgb()); v.setTextColor(R.id.w_time, pal.gold.toArgb())
            v.setTextColor(R.id.w_count, pal.accent.toArgb()); v.setTextColor(R.id.w_date, pal.muted.toArgb())
            v.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(c, 7, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            if (!Store.hasLocation) {
                v.setTextViewText(R.id.w_name, "ALzikr")
                v.setTextViewText(R.id.w_time, "")
                v.setTextViewText(R.id.w_date, "افتح التطبيق لتحديد موقعك")
                widgets.forEach { manager.updateAppWidget(it, v) }
                return
            }
            val w = window()
            val nextMin = Store.times(java.time.Instant.ofEpochMilli(w.nextAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()).getValue(w.next)
            v.setTextViewText(R.id.w_name, w.next.ar)
            v.setTextViewText(R.id.w_time, Alerts.timeText(nextMin))
            v.setChronometer(R.id.w_count, SystemClock.elapsedRealtime() + (w.nextAt - System.currentTimeMillis()), "بعد %s", true)
            v.setChronometerCountDown(R.id.w_count, true)
            val h = Data.hijri()
            v.setTextViewText(R.id.w_date, "${h.day} ${h.monthName}")
            val times = Store.times()
            Prayer.entries.filter { it.alertable }.forEachIndexed { i, p ->
                v.setTextViewText(ids[i], "${p.ar}\n${Alerts.timeText(times.getValue(p))}")
                v.setTextColor(ids[i], if (p == w.next) pal.gold.toArgb() else pal.muted.toArgb())
            }
            widgets.forEach { manager.updateAppWidget(it, v) }
            // Move on to the following prayer right after this one (not exact: a minute late is fine).
            c.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, w.nextAt + 30_000L,
                PendingIntent.getBroadcast(c, 5, Intent(c, PrayerWidget::class.java).setAction(TICK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
    }
}

/** The applied theme's colors for widgets and other places outside Compose. */
object ThemeColors {
    fun of(c: android.content.Context): Pal {
        Store.init(c)
        val dark = (c.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        return Looks.byId(Data.look).pal(dark, Data.theme)
    }
    fun argb(col: androidx.compose.ui.graphics.Color, alpha: Float = col.alpha) = col.copy(alpha = alpha).toArgb()
    /** Tints a widget's rounded background (Android 12+; older phones keep the default). */
    fun background(v: android.widget.RemoteViews, id: Int, col: androidx.compose.ui.graphics.Color) {
        if (android.os.Build.VERSION.SDK_INT >= 31) v.setColorStateList(id, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(argb(col, .96f)))
    }
}
