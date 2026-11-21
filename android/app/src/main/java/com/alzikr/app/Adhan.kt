package com.alzikr.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The adhan at prayer time: the whole adhan or only its opening takbirs, in a chosen voice.
 * Recordings are downloaded once (hosted on the ALzikr GitHub release "adhan-v1", from Wikimedia
 * Commons) and played as an alarm by [AdhanService], with «إيقاف» in its notification.
 */
object Adhan {
    data class Voice(val id: String, val name: String, val credit: String, val bytes: Long, val takbirMs: Long)
    private const val IA = "مجموعة «أذانات مساجد العالم» على أرشيف الإنترنت · وسمها رافعها بالملك العام"
    val voices = listOf(
        Voice("makkah-mulla", "عبد الملك الملا · المسجد الحرام", IA, 4_044_886, 28_500),
        Voice("makkah-hakim", "ماجد حكيم · المسجد الحرام", IA, 1_480_707, 32_000),
        Voice("madinah-bukhari", "عبد العزيز بخاري · المسجد النبوي", IA, 5_532_821, 43_000),
        Voice("aaqib-azeez", "عاقب عزيز", "تسجيل Atcovi · رخصة CC BY-SA 4.0 · ويكيميديا كومنز", 1_448_294, 11_200),
        Voice("sabah-fakhri", "صباح فخري", "ملك عام · ويكيميديا كومنز", 2_530_742, 30_000),
    )
    private const val BASE = "https://github.com/KL4PINGUINSKL/ALzikr/releases/download/adhan-v1/adhan-"

    /** 0 = the notification sound only, 1 = the whole adhan, 2 = the takbirs only. */
    var mode: Int
        get() = Store.prefs.getInt("adhan_mode", 0)
        set(v) { Store.prefs.edit().putInt("adhan_mode", v).apply(); Store.bump() }
    var voiceId: String
        get() = Store.prefs.getString("adhan_voice", voices.first().id) ?: voices.first().id
        set(v) { Store.prefs.edit().putString("adhan_voice", v).apply(); Store.bump() }
    /** Fajr: the takbirs only (the recordings have no «الصلاة خير من النوم»). */
    var fajrTakbir: Boolean
        get() = Store.prefs.getBoolean("adhan_fajr_takbir", true)
        set(v) { Store.prefs.edit().putBoolean("adhan_fajr_takbir", v).apply(); Store.bump() }
    /** Stay quiet while the phone is on silent or vibrate. */
    var respectSilent: Boolean
        get() = Store.prefs.getBoolean("adhan_respect_silent", false)
        set(v) { Store.prefs.edit().putBoolean("adhan_respect_silent", v).apply(); Store.bump() }

    fun voice() = voices.firstOrNull { it.id == voiceId } ?: voices.first()
    fun file(c: Context, v: Voice) = File(File(c.filesDir, "adhan").apply { mkdirs() }, "${v.id}.mp3")
    fun has(c: Context, v: Voice) = file(c, v).let { it.exists() && it.length() == v.bytes }

    var downloading by mutableStateOf<String?>(null)
    var progress by mutableFloatStateOf(0f)

    /** Downloads a recording. Blocking. */
    fun download(c: Context, v: Voice): Boolean {
        downloading = v.id; progress = 0f
        val tmp = File(file(c, v).parentFile, "${v.id}.part")
        return try {
            var u = URL(BASE + v.id + ".mp3"); var conn: HttpURLConnection; var hops = 0
            while (true) {
                conn = u.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false; conn.connectTimeout = 15_000; conn.readTimeout = 60_000
                val code = conn.responseCode
                if (code in 300..399 && hops++ < 5) { u = URL(u, conn.getHeaderField("Location")); conn.disconnect(); continue }
                if (code != 200) { conn.disconnect(); return false }
                break
            }
            var done = 0L
            conn.inputStream.use { input -> tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                while (true) { val n = input.read(buf); if (n < 0) break; out.write(buf, 0, n); done += n; progress = done.toFloat() / v.bytes }
            } }
            conn.disconnect()
            (tmp.length() == v.bytes && tmp.renameTo(file(c, v))).also { Store.bump() }
        } catch (e: Exception) {
            android.util.Log.w("ALzikr", "adhan ${v.id}", e); false
        } finally { tmp.delete(); downloading = null }
    }

    /** Will the adhan sound for this prayer now? (Not during a call, not when muted if asked.) */
    fun willPlay(c: Context, p: Prayer): Boolean {
        if (mode == 0 || !p.alertable || !has(c, voice())) return false
        val am = c.getSystemService(AudioManager::class.java)
        if (am.mode != AudioManager.MODE_NORMAL) return false
        if (respectSilent && am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
        return true
    }

    /** Plays the adhan for [p] (or a short sample when [p] is null). */
    fun start(c: Context, p: Prayer?) {
        val takbirOnly = p == null || mode == 2 || (p == Prayer.FAJR && fajrTakbir)
        val i = Intent(c, AdhanService::class.java).putExtra("prayer", p?.name).putExtra("takbir", takbirOnly)
        runCatching { c.startForegroundService(i) }.onFailure { android.util.Log.w("ALzikr", "adhan start", it) }
    }

    fun stop(c: Context) { c.stopService(Intent(c, AdhanService::class.java)) }
}

class AdhanService : Service() {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Store.init(this)
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        val prayer = runCatching { Prayer.valueOf(intent?.getStringExtra("prayer") ?: "") }.getOrNull()
        val takbirOnly = intent?.getBooleanExtra("takbir", true) ?: true
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "الأذان", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false); setSound(null, null) })
        val stop = PendingIntent.getService(this, 70, Intent(this, AdhanService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(this, CH).setSmallIcon(R.drawable.ic_stat_zikr)
            .setContentTitle(if (prayer != null) "الأذان · صلاة ${prayer.ar}" else "تجربة الأذان")
            .setContentText(Adhan.voice().name + if (takbirOnly) " · التكبيرات" else "")
            .setOngoing(true).setDeleteIntent(stop).setColor(0xFF1F7A5A.toInt())
            .addAction(Notification.Action.Builder(null, "إيقاف", stop).build()).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(ID, n)
        val v = Adhan.voice()
        val f = Adhan.file(this, v)
        if (!f.exists()) { stopSelf(); return START_NOT_STICKY }
        player?.release()
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                setDataSource(f.absolutePath)
                setOnCompletionListener { stopSelf() }
                prepare(); start()
            }
        }.getOrNull()
        if (player == null) { stopSelf(); return START_NOT_STICKY }
        handler.removeCallbacksAndMessages(null)
        if (takbirOnly) handler.postDelayed({ fadeAndStop() }, v.takbirMs)
        return START_NOT_STICKY
    }

    /** A short fade so the cut after the takbirs is gentle. */
    private fun fadeAndStop() {
        var vol = 1f
        val step = object : Runnable {
            override fun run() {
                vol -= .1f
                if (vol <= 0f) { stopSelf(); return }
                runCatching { player?.setVolume(vol, vol) }
                handler.postDelayed(this, 60)
            }
        }
        handler.post(step)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.stop() }; player?.release(); player = null
        super.onDestroy()
    }

    companion object { const val STOP = "com.alzikr.app.ADHAN_STOP"; private const val CH = "adhan_play"; private const val ID = 43 }
}
