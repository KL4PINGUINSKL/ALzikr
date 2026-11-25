package com.alzikr.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Task reminders, to-do-list style: a task can carry a date and time; at that time a notification says it, with
 * «تم ✓» and «بعد ١٠ دقائق». One exact alarm for the nearest reminder, set again after each one and after a reboot.
 * It does not need a location (unlike the prayer alerts).
 */
object TaskReminders {
    private const val CH = "task_remind"
    private const val ACTION_FIRE = "com.alzikr.app.TASK_FIRE"
    const val ACTION_REVIEW = "com.alzikr.app.TASK_REVIEW"
    const val ACTION_DONE = "com.alzikr.app.TASK_DONE"
    const val ACTION_SNOOZE = "com.alzikr.app.TASK_SNOOZE"

    /** «اليوم 4:30 م», «غدًا 9:00 ص», «الخميس 27/11 8:00 م». */
    fun label(at: Long): String {
        val z = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
        val day = when (z.toLocalDate()) {
            LocalDate.now() -> "اليوم"
            LocalDate.now().plusDays(1) -> "غدًا"
            else -> z.format(DateTimeFormatter.ofPattern("EEEE d/M", Locale("ar")))
        }
        return "$day ${Alerts.timeText(z.hour * 60 + z.minute)}"
    }

    private fun pi(c: Context, action: String, id: Long = 0) = PendingIntent.getBroadcast(c, (action + id).hashCode(),
        Intent(c, TaskReceiver::class.java).setAction(action).putExtra("id", id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Sets the alarm for the nearest open task's reminder (or clears it). */
    fun schedule(c: Context) {
        Store.init(c)
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pi(c, ACTION_FIRE)
        am.cancel(p)
        val next = Store.tasks().filter { !it.done && it.remindAt > System.currentTimeMillis() }.minOfOrNull { it.remindAt } ?: return
        Alerts.exact(am, next, p)
    }

    /** The alarm went off: every task now due gets its notification, and the next alarm is set. */
    fun fire(c: Context) {
        Store.init(c)
        val now = System.currentTimeMillis()
        Store.tasks().filter { !it.done && it.remindAt in 1..(now + 30_000) }.forEach { t ->
            notify(c, t)
            // Said once; a daily task is reminded again tomorrow at the same time (a snooze sets it again too).
            var next = 0L
            if (t.repeat) {
                // The next day the rule applies, at the same hour.
                next = t.remindAt
                var guard = 0
                do { next += 86_400_000L; guard++ } while (guard < 400 && (next <= now || !t.on(java.time.Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault()).toLocalDate())))
            }
            Store.setTaskReminder(t.id, next)
        }
        schedule(c)
    }

    private fun notify(c: Context, t: Store.Task) {
        if (!Alerts.canPost(c)) return
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH) == null) nm.createNotificationChannel(NotificationChannel(CH, "تذكير المهام", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(c, 71, Intent(c, MainActivity::class.java).putExtra("tab", "tasks").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(c, CH).setSmallIcon(R.drawable.ic_stat_zikr).setContentTitle("✅ ${t.text}")
            .setContentText("تذكير بمهمتك" + (t.after?.let { " · بعد ${it.ar}" } ?: "")).setAutoCancel(true).setContentIntent(open).setColor(0xFF1A3274.toInt())
            .addAction(Notification.Action.Builder(null, "تم ✓", pi(c, ACTION_DONE, t.id)).build())
            .addAction(Notification.Action.Builder(null, "بعد ١٠ دقائق", pi(c, ACTION_SNOOZE, t.id)).build())
            .build()
        nm.notify(("task" + t.id).hashCode(), n)
    }

    /** The daily «مراجعة المساء» reminder at [Data.reviewAt] (or none). */
    fun scheduleReview(c: Context) {
        Store.init(c)
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pi(c, ACTION_REVIEW)
        am.cancel(p)
        val m = Data.reviewAt
        if (m < 0) return
        var at = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).plusMinutes(m.toLong()).toInstant().toEpochMilli()
        if (at <= System.currentTimeMillis()) at += 86_400_000L
        Alerts.exact(am, at, p)
    }

    fun review(c: Context) {
        Store.init(c)
        scheduleReview(c)
        if (!Alerts.canPost(c)) return
        val today = LocalDate.now()
        val tasks = Store.tasks().filter { it.on(today) }
        val left = tasks.count { !it.done }
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH) == null) nm.createNotificationChannel(NotificationChannel(CH, "تذكير المهام", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(c, 72, Intent(c, MainActivity::class.java).putExtra("tab", "today").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(c, CH).setSmallIcon(R.drawable.ic_stat_zikr).setContentTitle("🌙 مراجعة يومك")
            .setContentText(if (tasks.isEmpty()) "كيف كان يومك؟ حاسب نفسك قبل النوم" else "أنجزت ${tasks.size - left} من ${tasks.size}" + if (left > 0) " · انقل الباقي إلى الغد" else " · بارك الله في يومك")
            .setAutoCancel(true).setContentIntent(open).setColor(0xFF1A3274.toInt()).build()
        nm.notify(73, n)
    }

    fun cancelNotice(c: Context, id: Long) = c.getSystemService(NotificationManager::class.java).cancel(("task$id").hashCode())
}

class TaskReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Store.init(c)
        val id = i.getLongExtra("id", 0)
        when (i.action) {
            TaskReminders.ACTION_DONE -> { if (Store.tasks().any { it.id == id && !it.done }) Store.toggleTask(id); TaskReminders.cancelNotice(c, id) }
            TaskReminders.ACTION_REVIEW -> TaskReminders.review(c)
            TaskReminders.ACTION_SNOOZE -> { Store.setTaskReminder(id, System.currentTimeMillis() + 10 * 60_000L); TaskReminders.cancelNotice(c, id); TaskReminders.schedule(c) }
            else -> TaskReminders.fire(c)
        }
    }
}
