package com.alzikr.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One exact alarm at a time: the next prayer alert, "prayer in N minutes" heads-up, or morning/evening
 * adhkar reminder. When it fires, the notification is shown and the following event is scheduled.
 * Rescheduled on app start, settings changes, reboot, time and time-zone changes.
 */
object Alerts {
    private const val CH_REMIND = "remind"
    private const val CH_FOCUS = "focus_run"
    private const val CH_FOCUS_DONE = "focus_done"
    private const val ID_PRAYER = 10
    private const val ID_REMIND = 11
    /** Occasions have their own channel and slots, so a later reminder does not replace them. */
    private const val CH_OCCASION = "occasions"
    private const val ID_EVE = 12
    private const val ID_DAY = 13
    const val ID_FOCUS = 12

    enum class Kind { PRAYER, BEFORE, MORNING, EVENING, SLEEP, DHIKR, KAHF, FAST, SUHOOR, KHATMA, DUA, SADAQA, OCCASION }
    data class Event(val at: Long, val kind: Kind, val prayer: Prayer)

    fun timeText(minutes: Int): String {
        val t = LocalTime.of((minutes / 60).mod(24), minutes.mod(60))
        return t.format(DateTimeFormatter.ofPattern("h:mm a", Locale("ar")))
    }

    private fun epoch(date: LocalDate, minutes: Int) =
        date.atStartOfDay(ZoneId.systemDefault()).plusMinutes(minutes.toLong()).toInstant().toEpochMilli()

    /** All alert events today and tomorrow, sorted. */
    fun upcoming(): List<Event> {
        if (!Store.hasLocation) return emptyList()
        val list = ArrayList<Event>()
        for (date in listOf(LocalDate.now(), LocalDate.now().plusDays(1))) {
            val t = Store.times(date)
            for (p in Prayer.entries) {
                if (!p.alertable || !Store.alert(p)) continue
                val m = t.getValue(p)
                list += Event(epoch(date, m), Kind.PRAYER, p)
                if (Store.before > 0) list += Event(epoch(date, m - Store.before), Kind.BEFORE, p)
            }
            if (Store.morningAfter >= 0) list += Event(epoch(date, t.getValue(Prayer.FAJR) + Store.morningAfter), Kind.MORNING, Prayer.FAJR)
            if (Store.eveningAfter >= 0) list += Event(epoch(date, t.getValue(Prayer.ASR) + Store.eveningAfter), Kind.EVENING, Prayer.ASR)
            if (Data.sleepAt >= 0) list += Event(epoch(date, Data.sleepAt), Kind.SLEEP, Prayer.ISHA)
            if (Data.khatmaRemindAt >= 0 && Data.khatmaPage < Data.PAGES) list += Event(epoch(date, Data.khatmaRemindAt), Kind.KHATMA, Prayer.DHUHR)
            if (Data.salawatEvery > 0) for (m in 9 * 60..21 * 60 step Data.salawatEvery * 60) list += Event(epoch(date, m), Kind.DHIKR, Prayer.DHUHR)
            if (Data.kahf && date.dayOfWeek == java.time.DayOfWeek.FRIDAY) list += Event(epoch(date, 10 * 60), Kind.KAHF, Prayer.DHUHR)
            if (Data.fastReminder && DayInfo.of(date.plusDays(1)) != null) list += Event(epoch(date, DayInfo.eveAt), Kind.FAST, Prayer.ISHA)
            // The day itself, after Fajr (Friday alone has its own reminder).
            if (Data.dayReminder && DayInfo.of(date)?.parts?.any { !it.kahf } == true) list += Event(epoch(date, t.getValue(Prayer.FAJR) + 20), Kind.OCCASION, Prayer.FAJR)
            if (Sadaqa.remindsOn(date)) list += Event(epoch(date, Sadaqa.remindAt), Kind.SADAQA, Prayer.ASR)
            Duas.events(date, t).forEach { m -> list += Event(epoch(date, m), Kind.DUA, Prayer.ISHA) }
            if (Data.suhoorBefore > 0 && Data.hijri(date).month == 9) list += Event(epoch(date, t.getValue(Prayer.FAJR) - Data.suhoorBefore), Kind.SUHOOR, Prayer.FAJR)
        }
        return list.sortedBy { it.at }
    }

    fun reschedule(c: Context) {
        Store.init(c)
        runCatching { PrayerAlarm.schedule(c) }
        runCatching { MomentWidget.update(c) }
        val am = c.getSystemService(AlarmManager::class.java)
        val next = upcoming().firstOrNull { it.at > System.currentTimeMillis() + 2_000 }
        val pi = PendingIntent.getBroadcast(c, 1, Intent(c, AlertReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.cancel(pi)
        TasbihNotice.update(c)
        if (next == null) { PrayerWidget.updateAll(c); return }
        c.getSharedPreferences("alzikr_alarm", Context.MODE_PRIVATE).edit().putLong("at", next.at).putString("kind", next.kind.name).putString("prayer", next.prayer.name).apply()
        exact(am, next.at, pi)
        PrayerWidget.updateAll(c)
    }

    @SuppressLint("MissingPermission")
    fun exact(am: AlarmManager, at: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** The prayer channel carries the chosen sound; Android fixes a channel's sound, so a new sound means a new channel. */
    private fun prayerChannel(c: Context): String {
        val nm = c.getSystemService(NotificationManager::class.java)
        val sound = Store.sound
        val id = if (Store.silent) "prayer_silent" else "prayer_" + (sound?.hashCode()?.toUInt()?.toString(16) ?: "default")
        if (nm.getNotificationChannel(id) == null) {
            nm.notificationChannels.filter { it.id.startsWith("prayer_") }.forEach { nm.deleteNotificationChannel(it.id) }
            val ch = NotificationChannel(id, "أوقات الصلاة", NotificationManager.IMPORTANCE_HIGH)
            if (Store.silent) ch.setSound(null, null)
            else ch.setSound(sound?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
        return id
    }

    /** The prayer notice while the adhan itself is playing: no sound of its own. */
    private fun adhanNoticeChannel(c: Context): String {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel("prayer_notice_adhan") == null)
            nm.createNotificationChannel(NotificationChannel("prayer_notice_adhan", "أوقات الصلاة (مع الأذان)", NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null); enableVibration(true) })
        return "prayer_notice_adhan"
    }

    fun channels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REMIND, "تذكير قبل الصلاة والأذكار", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_FOCUS, "جلسة التركيز الجارية", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        nm.createNotificationChannel(NotificationChannel(CH_FOCUS_DONE, "انتهاء جلسة التركيز", NotificationManager.IMPORTANCE_HIGH))
    }

    fun canPost(c: Context) = Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun open(c: Context, tab: String) = PendingIntent.getActivity(c, tab.hashCode(),
        Intent(c, MainActivity::class.java).putExtra("tab", tab).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun fire(c: Context, kind: Kind, p: Prayer) {
        if (!canPost(c)) return
        channels(c)
        val nm = c.getSystemService(NotificationManager::class.java)
        val b = when (kind) {
            Kind.PRAYER -> {
                val tasks = Store.openTasksAfter(p)
                var text = if (tasks.isEmpty()) "حيّ على الصلاة" else "بعد ${p.ar}: " + tasks.joinToString("، ") { it.text }
                // A focus session gives way to the prayer.
                if (Data.focusStopsAtAdhan && Store.focusEnd > System.currentTimeMillis() && !Store.focusIsBreak) {
                    val done = ((System.currentTimeMillis() - (Store.focusEnd - Store.focusMinutes * 60_000L)) / 60_000L).toInt()
                    stopFocus(c); if (done > 0) Store.addFocus(done)
                    text += " · أُوقفت جلسة التركيز ($done دقيقة)"
                }
                // The adhan itself plays; the notice then stays quiet (no second sound).
                val adhan = Adhan.willPlay(c, p)
                if (adhan) Adhan.start(c, p)
                silence(c)
                Notification.Builder(c, if (adhan) adhanNoticeChannel(c) else prayerChannel(c)).setContentTitle("حان الآن وقت صلاة ${p.ar}").setContentText(text)
                    .setStyle(Notification.BigTextStyle().bigText(text)).setCategory(Notification.CATEGORY_REMINDER).setContentIntent(open(c, if (tasks.isEmpty()) "prayer" else "tasks"))
                    .addAction(Notification.Action.Builder(null, "صليت ✓", PendingIntent.getBroadcast(c, 40 + p.ordinal,
                        Intent(c, AlertReceiver::class.java).setAction(AlertReceiver.PRAYED).putExtra("prayer", p.name), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build())
            }
            Kind.BEFORE -> Notification.Builder(c, CH_REMIND).setContentTitle("صلاة ${p.ar} بعد ${Store.before} دقائق").setContentText("استعد للصلاة").setContentIntent(open(c, "prayer")).setTimeoutAfter(Store.before * 60_000L)
            Kind.MORNING -> Notification.Builder(c, CH_REMIND).setContentTitle("أذكار الصباح").setContentText(awradLeft()).setContentIntent(open(c, "adhkar"))
            Kind.EVENING -> Notification.Builder(c, CH_REMIND).setContentTitle("أذكار المساء").setContentText(awradLeft()).setContentIntent(open(c, "adhkar"))
            Kind.KHATMA -> {
                val from = Data.khatmaPage + 1
                val to = (Data.khatmaPage + Data.wirdLeftToday()).coerceAtMost(Data.PAGES)
                val read = Data.pagesToday()
                Notification.Builder(c, CH_REMIND).setContentTitle("وِرد القرآن").setContentText(if (read > 0) "قرأت اليوم $read صفحة، بارك الله فيك" else "وردك اليوم: من الصفحة $from إلى $to")
                    .setContentIntent(PendingIntent.getActivity(c, 77, Intent(c, MainActivity::class.java).putExtra("quran", Quran.firstOfPageSafe(c, from)).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            }
            Kind.SLEEP -> Notification.Builder(c, CH_REMIND).setContentTitle("أذكار النوم").setContentText("باسمك اللهم أموت وأحيا").setContentIntent(open(c, "adhkar"))
            Kind.DHIKR -> Content.reminders[(System.currentTimeMillis() / 3_600_000L % Content.reminders.size).toInt()].let { z ->
                Notification.Builder(c, CH_REMIND).setContentTitle("ذكر").setContentText(z).setContentIntent(open(c, "adhkar")).setTimeoutAfter(50 * 60_000L) }
            Kind.KAHF -> Notification.Builder(c, CH_REMIND).setContentTitle("يوم الجمعة المبارك").setContentText("اغتسل وتطيّب وبكّر إلى الصلاة، واقرأ الكهف، وأكثر من الصلاة على النبي ﷺ")
                .setStyle(Notification.BigTextStyle().bigText("• الغسل والطيب ولبس أحسن الثياب\n• التبكير إلى المسجد والإنصات للخطبة\n• قراءة سورة الكهف\n• الإكثار من الصلاة على النبي ﷺ\n• الدعاء في آخر ساعة بعد العصر"))
                .setContentIntent(PendingIntent.getActivity(c, 80, Intent(c, MainActivity::class.java).putExtra("day", LocalDate.now().toString()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            Kind.FAST, Kind.OCCASION -> {
                val today = kind == Kind.OCCASION
                val d = if (today) LocalDate.now() else LocalDate.now().plusDays(1)
                val card = DayInfo.of(d)
                val parts = card?.parts.orEmpty()
                // When two or more meet (a Monday that is a white day, Arafah on a Thursday…), say so.
                val title = (if (today) "اليوم " else "غدًا ") + when {
                    parts.size >= 2 -> "يجتمع فضلان: " + parts.joinToString(" + ") { it.name }
                    else -> card?.title ?: ""
                }
                val text = when {
                    parts.size >= 2 && parts.any { it.sunan.any { s -> "صيام" in s.text || "صوم" in s.text } } -> "صيام واحد يجمع الفضلين، فانوِه من الليل"
                    else -> card?.let { DayInfo.line(it) } ?: ""
                }
                val body = parts.joinToString("\n\n") { p -> p.icon + " " + p.name + "\n" + (p.virtue.firstOrNull()?.text ?: "") + "\n• " + (p.sunan.firstOrNull()?.text ?: "") }
                val tap = PendingIntent.getActivity(c, if (today) 76 else 77, Intent(c, MainActivity::class.java).putExtra("tab", "prayer").putExtra("day", d.toString()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                nm.createNotificationChannel(NotificationChannel(CH_OCCASION, "المناسبات (قبلها بليلة ويومها)", NotificationManager.IMPORTANCE_DEFAULT))
                Notification.Builder(c, CH_OCCASION).setContentTitle(title).setContentText("$text · اضغط لفضله وسننه")
                    .setStyle(Notification.BigTextStyle().bigText(body + "\n\nاضغط لفضله وسننه كاملة")).setContentIntent(tap)
            }
            Kind.SADAQA -> Notification.Builder(c, CH_REMIND).setContentTitle("صدقة اليوم").setContentText(Sadaqa.virtues[LocalDate.now().dayOfYear % Sadaqa.virtues.size])
                .setContentIntent(PendingIntent.getActivity(c, 79, Intent(c, MainActivity::class.java).putExtra("page", "sadaqa").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            Kind.DUA -> Notification.Builder(c, CH_REMIND).setContentTitle("وقت إجابة: " + Duas.whichTime()).setContentText(Duas.reminderText())
                .setStyle(Notification.BigTextStyle().bigText(Duas.reminderText())).setTimeoutAfter(60 * 60_000L)
                .setContentIntent(PendingIntent.getActivity(c, 78, Intent(c, MainActivity::class.java).putExtra("adhkarsec", 3).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            Kind.SUHOOR -> Notification.Builder(c, CH_REMIND).setContentTitle("السحور").setContentText("الفجر بعد ${Data.suhoorBefore} دقيقة · تسحروا فإن في السحور بركة").setContentIntent(open(c, "prayer"))
        }
        b.setSmallIcon(R.drawable.ic_stat_zikr).setAutoCancel(true).setColor(0xFF1F7A5A.toInt())
        nm.notify(when (kind) { Kind.PRAYER -> ID_PRAYER; Kind.FAST -> ID_EVE; Kind.OCCASION -> ID_DAY; else -> ID_REMIND }, b.build())
    }

    /** Do Not Disturb for a few minutes after the adhan, when the user asked for it and allowed ALzikr to. */
    private fun silence(c: Context) {
        val minutes = Data.silentAfterAdhan
        val nm = c.getSystemService(NotificationManager::class.java)
        if (minutes <= 0 || !nm.isNotificationPolicyAccessGranted || nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        c.getSharedPreferences("alzikr_alarm", Context.MODE_PRIVATE).edit().putBoolean("silenced", true).apply()
        exact(c.getSystemService(AlarmManager::class.java), System.currentTimeMillis() + minutes * 60_000L,
            PendingIntent.getBroadcast(c, 3, Intent(c, AlertReceiver::class.java).setAction(AlertReceiver.UNSILENCE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    }

    fun unsilence(c: Context) {
        val sp = c.getSharedPreferences("alzikr_alarm", Context.MODE_PRIVATE)
        if (!sp.getBoolean("silenced", false)) return
        sp.edit().putBoolean("silenced", false).apply()
        val nm = c.getSystemService(NotificationManager::class.java)
        // Only undo our own change: if the mode was changed meanwhile, leave it.
        if (nm.isNotificationPolicyAccessGranted && nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    /** "Try the sound" in Settings: the real prayer channel, with a test title. */
    fun testSound(c: Context) {
        if (!canPost(c)) return
        val n = Notification.Builder(c, prayerChannel(c)).setSmallIcon(R.drawable.ic_stat_zikr).setContentTitle("تجربة تنبيه الصلاة")
            .setContentText(if (Store.silent) "اهتزاز فقط، بلا صوت" else "هكذا سيبدو تنبيه الأذان").setAutoCancel(true).setColor(0xFF1F7A5A.toInt()).build()
        c.getSystemService(NotificationManager::class.java).notify(ID_PRAYER, n)
    }

    private fun awradLeft(): String {
        val left = Store.adhkar().filter { it.target > 0 && !it.done }
        return if (left.isEmpty()) "أتممت أورادك اليوم، بارك الله فيك" else "بقي ${left.size} من أورادك اليوم"
    }

    // ----------------------------------------------------------------- focus sessions

    fun startFocus(c: Context, minutes: Int, isBreak: Boolean, title: String) {
        Store.focusMinutes = if (isBreak) Store.focusMinutes else minutes
        Store.focusIsBreak = isBreak; Store.focusTitle = title
        val end = System.currentTimeMillis() + minutes * 60_000L
        Store.focusEnd = end
        exact(c.getSystemService(AlarmManager::class.java), end, focusIntent(c, FocusReceiver.END))
        focusNotification(c)
    }

    fun stopFocus(c: Context) {
        Store.focusEnd = 0
        c.getSystemService(AlarmManager::class.java).cancel(focusIntent(c, FocusReceiver.END))
        c.getSystemService(NotificationManager::class.java).cancel(ID_FOCUS)
    }

    private fun focusIntent(c: Context, action: String) = PendingIntent.getBroadcast(c, action.hashCode(),
        Intent(c, FocusReceiver::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Ongoing notification with a live countdown (Android draws the timer, no service needed). */
    fun focusNotification(c: Context) {
        if (!canPost(c) || Store.focusEnd <= System.currentTimeMillis()) return
        channels(c)
        val title = if (Store.focusIsBreak) "استراحة" else Store.focusTitle.ifBlank { "جلسة تركيز" }
        val n = Notification.Builder(c, CH_FOCUS).setSmallIcon(R.drawable.ic_stat_zikr).setContentTitle(title)
            .setUsesChronometer(true).setChronometerCountDown(true).setWhen(Store.focusEnd).setShowWhen(true)
            .setOngoing(true).setContentIntent(open(c, "focus")).setColor(0xFF1F7A5A.toInt())
            .addAction(Notification.Action.Builder(null, "إيقاف", focusIntent(c, FocusReceiver.STOP)).build())
            .build()
        c.getSystemService(NotificationManager::class.java).notify(ID_FOCUS, n)
    }

    fun focusEnded(c: Context) {
        val wasBreak = Store.focusIsBreak
        val minutes = Store.focusMinutes
        Store.focusEnd = 0
        if (!wasBreak) Store.addFocus(minutes)
        if (!canPost(c)) return
        channels(c)
        val n = Notification.Builder(c, CH_FOCUS_DONE).setSmallIcon(R.drawable.ic_stat_zikr)
            .setContentTitle(if (wasBreak) "انتهت الاستراحة" else "أحسنت! انتهت جلسة التركيز")
            .setContentText(if (wasBreak) "جاهز لجلسة جديدة؟" else "$minutes دقيقة من التركيز · خذ استراحة قصيرة")
            .setAutoCancel(true).setContentIntent(open(c, "focus")).setColor(0xFF1F7A5A.toInt())
        if (!wasBreak) n.addAction(Notification.Action.Builder(null, "استراحة 5 دقائق", focusIntent(c, FocusReceiver.BREAK)).build())
        c.getSystemService(NotificationManager::class.java).notify(ID_FOCUS, n.build())
    }

    // ----------------------------------------------------------------- location

    /** The phone's position (coarse is enough for prayer times): last known, else one fresh fix. */
    @SuppressLint("MissingPermission")
    fun locate(c: Context, done: (Location?) -> Unit) {
        val lm = c.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 6 * 3600_000L) { done(last); return }
        val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: run { done(last); return }
        val main = Handler(Looper.getMainLooper())
        var answered = false
        fun answer(l: Location?) { if (!answered) { answered = true; done(l ?: last) } }
        if (Build.VERSION.SDK_INT >= 30) lm.getCurrentLocation(provider, null, c.mainExecutor) { answer(it) }
        else @Suppress("DEPRECATION") lm.requestSingleUpdate(provider, { answer(it) }, Looper.getMainLooper())
        main.postDelayed({ answer(null) }, 20_000)
    }

    /** ("Riyadh", "SA") for a position, best effort (needs the internet; empty when unknown). */
    fun placeName(c: Context, lat: Double, lng: Double): Pair<String, String> = runCatching {
        @Suppress("DEPRECATION")
        android.location.Geocoder(c, Locale("ar")).getFromLocation(lat, lng, 1)?.firstOrNull()?.let { (it.locality ?: it.subAdminArea ?: it.adminArea ?: "") to (it.countryCode ?: "") }
    }.getOrNull() ?: ("" to "")

    /** Name the place and, unless the user chose a method, use the one common in that country. Off the main thread. */
    fun resolvePlace(c: Context, lat: Double, lng: Double) = Thread {
        val (name, cc) = placeName(c, lat, lng)
        c.mainExecutor.execute {
            if (name.isNotBlank()) Store.setLocation(lat, lng, name)
            if (cc.isNotBlank()) {
                Store.country = cc
                if (!Store.methodChosen) Store.method = Store.methodFor(cc)
            }
            reschedule(c)
        }
    }.start()
}

class AlertReceiver : BroadcastReceiver() {
    companion object { const val PRAYED = "com.alzikr.app.PRAYED"; const val UNSILENCE = "com.alzikr.app.UNSILENCE" }
    override fun onReceive(c: Context, i: Intent) {
        Store.init(c)
        when (i.action) {
            PRAYED -> {
                runCatching { Prayer.valueOf(i.getStringExtra("prayer") ?: "") }.getOrNull()?.let { Data.setPrayed(LocalDate.now(), it, true) }
                c.getSystemService(NotificationManager::class.java).cancel(10)
                return
            }
            UNSILENCE -> { Alerts.unsilence(c); return }
            // Swiped away: back it comes. «إخفاء»: switched off until turned on again.
            TasbihNotice.BACK, CounterNotice.BACK -> { TasbihNotice.update(c); return }
            CounterNotice.HIDE -> {
                CounterNotice.on = false; CounterNotice.update(c)
                android.widget.Toast.makeText(c, "أُخفيت السبحة من الإشعارات. تعيدها من صفحة السبحة", android.widget.Toast.LENGTH_LONG).show()
                return
            }
            TasbihNotice.HIDE -> {
                TasbihNotice.on = false; TasbihNotice.update(c)
                android.widget.Toast.makeText(c, "أُخفي الإشعار. تعيده من صفحة الصلاة", android.widget.Toast.LENGTH_LONG).show()
                return
            }
        }
        val sp = c.getSharedPreferences("alzikr_alarm", Context.MODE_PRIVATE)
        val kind = runCatching { Alerts.Kind.valueOf(sp.getString("kind", "") ?: "") }.getOrNull()
        val prayer = runCatching { Prayer.valueOf(sp.getString("prayer", "") ?: "") }.getOrNull()
        // Fired late (phone asleep for long)? Skip stale alerts instead of ringing hours later.
        val late = System.currentTimeMillis() - sp.getLong("at", 0)
        if (kind != null && prayer != null && late < 15 * 60_000L) Alerts.fire(c, kind, prayer)
        Alerts.reschedule(c)
    }
}

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Store.init(c)
        when (i.action) {
            END -> Alerts.focusEnded(c)
            STOP -> Alerts.stopFocus(c)
            BREAK -> Alerts.startFocus(c, 5, true, "")
        }
    }
    companion object { const val END = "com.alzikr.app.FOCUS_END"; const val STOP = "com.alzikr.app.FOCUS_STOP"; const val BREAK = "com.alzikr.app.FOCUS_BREAK" }
}

/** Reboot, app update, clock or time-zone change: alarms are gone or wrong, so set them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Store.init(c)
        Alerts.reschedule(c)
        runCatching { PetOverlay.start(c) }
        if (Store.focusEnd > System.currentTimeMillis()) {
            Alerts.exact(c.getSystemService(AlarmManager::class.java), Store.focusEnd, PendingIntent.getBroadcast(c, FocusReceiver.END.hashCode(),
                Intent(c, FocusReceiver::class.java).setAction(FocusReceiver.END), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            Alerts.focusNotification(c)
        } else if (Store.focusEnd != 0L) Store.focusEnd = 0
    }
}
