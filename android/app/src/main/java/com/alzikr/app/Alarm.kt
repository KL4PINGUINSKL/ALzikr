package com.alzikr.app

import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A real alarm for each prayer, separate from the prayer reminder: it rings like the phone's alarm clock
 * (alarm volume, repeating until stopped, over the lock screen, with snooze), set with setAlarmClock so the
 * phone shows it as its next alarm and Doze never delays it.
 */
object PrayerAlarm {
    fun on(p: Prayer) = Store.prefs.getBoolean("alarm_on_${p.name}", false)
    fun setOn(c: Context, p: Prayer, v: Boolean) { Store.prefs.edit().putBoolean("alarm_on_${p.name}", v).apply(); Store.bump(); schedule(c) }
    /** Minutes from the prayer time: negative = before it. */
    fun offset(p: Prayer) = Store.prefs.getInt("alarm_off_${p.name}", 0)
    fun setOffset(c: Context, p: Prayer, v: Int) { Store.prefs.edit().putInt("alarm_off_${p.name}", v.coerceIn(-120, 120)).apply(); Store.bump(); schedule(c) }
    /** Days it rings (1 = Monday … 7 = Sunday). */
    fun days(p: Prayer): Set<Int> = Store.prefs.getStringSet("alarm_days_${p.name}", null)?.mapNotNull { it.toIntOrNull() }?.toSet() ?: (1..7).toSet()
    fun setDays(c: Context, p: Prayer, v: Set<Int>) { Store.prefs.edit().putStringSet("alarm_days_${p.name}", v.map { it.toString() }.toSet()).apply(); Store.bump(); schedule(c) }

    /** 0 = the phone's alarm tone, 1 = the adhan (then the tone), 2 = vibration only. */
    var sound: Int
        get() = Store.prefs.getInt("alarm_sound", 0)
        set(v) { Store.prefs.edit().putInt("alarm_sound", v).apply(); Store.bump() }
    var snooze: Int
        get() = Store.prefs.getInt("alarm_snooze", 10)
        set(v) { Store.prefs.edit().putInt("alarm_snooze", v).apply(); Store.bump() }
    var vibrate: Boolean
        get() = Store.prefs.getBoolean("alarm_vibrate", true)
        set(v) { Store.prefs.edit().putBoolean("alarm_vibrate", v).apply(); Store.bump() }
    /** Start softly and grow louder over half a minute. */
    var ramp: Boolean
        get() = Store.prefs.getBoolean("alarm_ramp", true)
        set(v) { Store.prefs.edit().putBoolean("alarm_ramp", v).apply(); Store.bump() }
    /** Stops by itself after this many minutes if nobody answers. */
    const val RING_MIN = 10
    val snoozes = listOf(5, 10, 15, 20)
    val dayNames = listOf(6 to "السبت", 7 to "الأحد", 1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة")

    fun anyOn() = Prayer.entries.any { on(it) }

    /** When the alarm for [p] rings on [d] (epoch ms). */
    fun at(p: Prayer, d: LocalDate): Long {
        val m = Store.times(d).getValue(p) + offset(p)
        return d.atStartOfDay(ZoneId.systemDefault()).plusMinutes(m.toLong()).toInstant().toEpochMilli()
    }

    /** The next alarm (prayer, time), looking a week ahead. */
    fun next(after: Long = System.currentTimeMillis()): Pair<Prayer, Long>? {
        if (!Store.hasLocation) return null
        val today = LocalDate.now()
        for (i in -1..8) {
            val d = today.plusDays(i.toLong())
            val list = Prayer.entries.filter { on(it) && d.dayOfWeek.value in days(it) }.map { it to at(it, d) }.filter { it.second > after + 1_000 }.sortedBy { it.second }
            list.firstOrNull()?.let { return it }
        }
        return null
    }

    private fun pi(c: Context, code: Int, p: Prayer?, at: Long) = PendingIntent.getBroadcast(c, code,
        Intent(c, AlarmReceiver::class.java).putExtra("prayer", p?.name).putExtra("at", at),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun show(c: Context) = PendingIntent.getActivity(c, 83, Intent(c, MainActivity::class.java).putExtra("page", "alarms"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(c: Context) {
        Store.init(c)
        val am = c.getSystemService(AlarmManager::class.java)
        am.cancel(pi(c, 81, null, 0))
        val (p, at) = next() ?: return
        set(am, at, pi(c, 81, p, at), show(c))
    }

    fun snoozeNow(c: Context, p: Prayer?) {
        val at = System.currentTimeMillis() + snooze * 60_000L
        Store.prefs.edit().putLong("alarm_snooze_at", at).apply()
        set(c.getSystemService(AlarmManager::class.java), at, pi(c, 82, p, at), show(c))
    }
    fun cancelSnooze(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(pi(c, 82, null, 0))
        Store.prefs.edit().remove("alarm_snooze_at").apply()
    }
    fun snoozedUntil(): Long = Store.prefs.getLong("alarm_snooze_at", 0L).takeIf { it > System.currentTimeMillis() } ?: 0L

    private fun set(am: AlarmManager, at: Long, op: PendingIntent, show: PendingIntent) {
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        else am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), op)
    }

    /** Android 14+ may need the user's OK to show the alarm over the lock screen. */
    fun canFullScreen(c: Context) = Build.VERSION.SDK_INT < 34 || c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    fun fullScreenSettings(c: Context) {
        if (Build.VERSION.SDK_INT >= 34) runCatching {
            c.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${c.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun ring(c: Context, p: Prayer?, test: Boolean = false) {
        val i = Intent(c, AlarmService::class.java).putExtra("prayer", p?.name).putExtra("test", test)
        runCatching { c.startForegroundService(i) }.onFailure { android.util.Log.w("ALzikr", "alarm start", it) }
    }
    fun stop(c: Context) { c.startService(Intent(c, AlarmService::class.java).setAction(AlarmService.STOP)) }
    fun snoozeRinging(c: Context) { c.startService(Intent(c, AlarmService::class.java).setAction(AlarmService.SNOOZE)) }

    fun title(p: Prayer?) = if (p == null) "تجربة المنبه" else "منبه صلاة ${p.ar}"
    fun line(p: Prayer?): String {
        if (p == null) return "هكذا سيرنّ المنبه"
        val o = offset(p)
        return when {
            o < 0 -> "${p.ar} بعد ${-o} دقيقة"
            o > 0 -> "دخل وقت ${p.ar} قبل $o دقيقة"
            p == Prayer.FAJR -> "حان وقت الفجر · الصلاة خير من النوم"
            p == Prayer.SUNRISE -> "الشروق الآن"
            else -> "حان وقت صلاة ${p.ar}"
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Store.init(c)
        val p = runCatching { Prayer.valueOf(i.getStringExtra("prayer") ?: "") }.getOrNull()
        val at = i.getLongExtra("at", 0L)
        // A late delivery (phone was off) older than the ringing time is not rung.
        if (at == 0L || System.currentTimeMillis() - at < PrayerAlarm.RING_MIN * 60_000L) PrayerAlarm.ring(c, p)
        Store.prefs.edit().remove("alarm_snooze_at").apply()
        PrayerAlarm.schedule(c)
    }
}

/** Rings: the sound (alarm volume, looping), vibration, a full-screen page over the lock screen, stop and snooze. */
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var prayer: Prayer? = null
    private var focus: android.media.AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Store.init(this)
        when (intent?.action) {
            STOP -> { quit(); return START_NOT_STICKY }
            SNOOZE -> { PrayerAlarm.snoozeNow(this, prayer); quit(); return START_NOT_STICKY }
        }
        prayer = runCatching { Prayer.valueOf(intent?.getStringExtra("prayer") ?: "") }.getOrNull()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "منبه الصلوات", NotificationManager.IMPORTANCE_HIGH).apply {
            setShowBadge(false); setSound(null, null); enableVibration(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
        val page = Intent(this, AlarmActivity::class.java).putExtra("prayer", prayer?.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        val full = PendingIntent.getActivity(this, 84, page, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 85, Intent(this, AlarmService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val snooze = PendingIntent.getService(this, 86, Intent(this, AlarmService::class.java).setAction(SNOOZE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(this, CH).setSmallIcon(R.drawable.ic_stat_zikr)
            .setContentTitle(PrayerAlarm.title(prayer)).setContentText(PrayerAlarm.line(prayer))
            .setCategory(Notification.CATEGORY_ALARM).setOngoing(true).setColor(0xFF1F7A5A.toInt())
            .setFullScreenIntent(full, true).setContentIntent(full).setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(Notification.Action.Builder(null, "إيقاف", stop).build())
            .addAction(Notification.Action.Builder(null, "غفوة ${PrayerAlarm.snooze} د", snooze).build())
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(ID, n)
        // When the screen is on and unlocked Android shows a heads-up instead; try the page directly too.
        runCatching { startActivity(page) }
        startSound()
        startVibration()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ quit() }, PrayerAlarm.RING_MIN * 60_000L)
        return START_NOT_STICKY
    }

    private fun attrs() = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    private fun startSound() {
        player?.release(); player = null
        if (PrayerAlarm.sound == 2) return
        val am = getSystemService(AudioManager::class.java)
        // In a call the alarm only vibrates, as the phone's own alarm stays out of the way.
        if (am.mode != AudioManager.MODE_NORMAL) return
        val req = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs()).build()
        focus = req; am.requestAudioFocus(req)
        val adhan = Adhan.voice().takeIf { PrayerAlarm.sound == 1 && Adhan.has(this, it) }
        if (adhan != null) play(Uri.fromFile(Adhan.file(this, adhan)), loop = false) { play(tone(), loop = true) }
        else play(tone(), loop = true)
        if (PrayerAlarm.ramp) {
            var vol = .12f
            val up = object : Runnable { override fun run() { vol = (vol + .04f).coerceAtMost(1f); runCatching { player?.setVolume(vol, vol) }; if (vol < 1f) handler.postDelayed(this, 1_000) } }
            runCatching { player?.setVolume(vol, vol) }
            handler.postDelayed(up, 1_000)
        }
    }

    private fun tone(): Uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    private fun play(uri: Uri, loop: Boolean, then: (() -> Unit)? = null) {
        val old = player?.let { runCatching { it.stop() }; it.release(); null }
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attrs())
                setDataSource(this@AlarmService, uri)
                isLooping = loop
                if (then != null) setOnCompletionListener { then() }
                prepare(); start()
            }
        }.getOrNull() ?: old
        // A tone that cannot play (a deleted file): fall back to the built-in alarm.
        if (player == null && uri != RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) play(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), true)
    }

    private fun startVibration() {
        if (!PrayerAlarm.vibrate && PrayerAlarm.sound != 2) return
        val v = getSystemService(Vibrator::class.java) ?: return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 600), 1)
        if (Build.VERSION.SDK_INT >= 33) v.vibrate(effect, android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_ALARM))
        else @Suppress("DEPRECATION") v.vibrate(effect, attrs())
    }

    private fun quit() {
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.stop() }; player?.release(); player = null
        getSystemService(Vibrator::class.java)?.cancel()
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }; focus = null
        AlarmActivity.current?.get()?.finish()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.stop() }; player?.release(); player = null
        getSystemService(Vibrator::class.java)?.cancel()
        super.onDestroy()
    }

    companion object {
        const val STOP = "com.alzikr.app.ALARM_STOP"
        const val SNOOZE = "com.alzikr.app.ALARM_SNOOZE"
        private const val CH = "prayer_alarm"
        private const val ID = 44
    }
}

/** The ringing page over the lock screen: the time, the prayer, a big «إيقاف» and «غفوة». */
class AlarmActivity : ComponentActivity() {
    companion object { var current: WeakReference<Activity>? = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        warmStates()
        current = WeakReference(this)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val p = runCatching { Prayer.valueOf(intent?.getStringExtra("prayer") ?: "") }.getOrNull()
        setContent {
            AppTheme {
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
                val t = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(P.surface, P.bg))), contentAlignment = Alignment.Center) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Glyph("⏰", tint = P.accent, size = 60.dp)
                        Text(Alerts.timeText(t.hour * 60 + t.minute), color = P.ink, fontSize = 76.sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body)
                        Text(PrayerAlarm.title(p), color = P.gold, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, fontFamily = L.head ?: L.body)
                        Text(PrayerAlarm.line(p), color = P.muted, fontSize = 17.sp, textAlign = TextAlign.Center)
                        Box(Modifier.height(36.dp))
                        Text("إيقاف", color = P.onAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clip(L.shape(40.dp)).background(P.accent).clickable { PrayerAlarm.stop(this@AlarmActivity); finish() }.padding(vertical = 20.dp))
                        Text("غفوة ${PrayerAlarm.snooze} دقائق", color = P.ink, fontSize = 18.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clip(L.shape(40.dp)).background(P.card).border(1.dp, P.line, L.shape(40.dp)).clickable { PrayerAlarm.snoozeRinging(this@AlarmActivity); finish() }.padding(vertical = 16.dp))
                    }
                }
            }
        }
    }

    override fun onDestroy() { if (current?.get() === this) current = null; super.onDestroy() }
}
