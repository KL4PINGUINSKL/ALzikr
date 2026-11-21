package com.alzikr.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.RemoteViews

/** A tasbih on the home screen: tap to count (the same count as the app's free tasbih). */
class TasbihWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) { Store.init(c); update(c) }
    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        when (i.action) {
            TAP -> {
                Store.init(c)
                val n = Data.tasbihTap()
                runCatching { c.getSystemService(Vibrator::class.java)?.vibrate(if (n % Data.tasbihSet == 0) VibrationEffect.createWaveform(longArrayOf(0, 70, 90, 70), -1) else VibrationEffect.createOneShot(14, 100)) }
                update(c)
            }
            RESET -> { Store.init(c); Data.tasbihReset(); update(c) }
        }
    }

    companion object {
        const val TAP = "com.alzikr.app.TASBIH_TAP"
        private const val RESET = "com.alzikr.app.TASBIH_RESET"

        fun update(c: Context) {
            TasbihNotice.update(c)
            val manager = AppWidgetManager.getInstance(c)
            val ids = manager.getAppWidgetIds(ComponentName(c, TasbihWidget::class.java))
            if (ids.isEmpty()) return
            val set = Data.tasbihSet; val count = Data.tasbih
            val v = RemoteViews(c.packageName, R.layout.widget_tasbih)
            v.setTextViewText(R.id.t_count, "${count % set}")
            v.setTextViewText(R.id.t_round, "الدورة ${count / set + 1} من $set · المجموع $count")
            v.setOnClickPendingIntent(R.id.t_root, PendingIntent.getBroadcast(c, 60, Intent(c, TasbihWidget::class.java).setAction(TAP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            v.setOnClickPendingIntent(R.id.t_reset, PendingIntent.getBroadcast(c, 61, Intent(c, TasbihWidget::class.java).setAction(RESET), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            ids.forEach { manager.updateAppWidget(it, v) }
        }
    }
}
