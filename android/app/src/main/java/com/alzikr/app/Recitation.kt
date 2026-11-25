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
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Verse-by-verse recitation streamed from everyayah.com. Each verse can repeat (for memorizing),
 * the basmala is read before each surah, and reading can go on into the next surah. A small
 * foreground service keeps it playing with the screen off and shows pause / stop.
 */
object Recitation {
    /** style: 0 murattal, 1 mujawwad, 2 teaching (for memorizing). */
    data class Reciter(val folder: String, val name: String, val style: Int = 0)
    val styles = listOf("مرتّل", "مجوّد", "معلّم (للتحفيظ)")
    val reciters = listOf(
        Reciter("Alafasy_128kbps", "مشاري العفاسي"),
        Reciter("Abdul_Basit_Murattal_192kbps", "عبد الباسط عبد الصمد (مرتل)"),
        Reciter("Husary_128kbps", "محمود خليل الحصري"),
        Reciter("Husary_Muallim_128kbps", "محمود خليل الحصري (المعلّم)", 2),
        Reciter("Minshawy_Teacher_128kbps", "محمد صديق المنشاوي (المعلّم)", 2),
        Reciter("Minshawy_Mujawwad_192kbps", "محمد صديق المنشاوي (مجوّد)", 1),
        Reciter("Abdul_Basit_Mujawwad_128kbps", "عبد الباسط عبد الصمد (مجوّد)", 1),
        Reciter("Husary_128kbps_Mujawwad", "محمود خليل الحصري (مجوّد)", 1),
        Reciter("Mohammad_al_Tablaway_128kbps", "محمد محمود الطبلاوي", 1),
        Reciter("Mahmoud_Ali_Al_Banna_32kbps", "محمود علي البنا", 1),
        Reciter("Minshawy_Murattal_128kbps", "محمد صديق المنشاوي (مرتل)"),
        Reciter("MaherAlMuaiqly128kbps", "ماهر المعيقلي"),
        Reciter("Abdurrahmaan_As-Sudais_192kbps", "عبد الرحمن السديس"),
        Reciter("Saood_ash-Shuraym_128kbps", "سعود الشريم"),
        Reciter("Ghamadi_40kbps", "سعد الغامدي"),
        Reciter("Yasser_Ad-Dussary_128kbps", "ياسر الدوسري"),
        Reciter("Nasser_Alqatami_128kbps", "ناصر القطامي"),
        Reciter("Abu_Bakr_Ash-Shaatree_128kbps", "أبو بكر الشاطري"),
        Reciter("Hudhaify_128kbps", "علي الحذيفي"),
        Reciter("Muhammad_Ayyoub_128kbps", "محمد أيوب"),
        Reciter("Muhammad_Jibreel_128kbps", "محمد جبريل"),
        Reciter("Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net", "أحمد بن علي العجمي"),
        Reciter("Fares_Abbad_64kbps", "فارس عباد"),
        Reciter("Hani_Rifai_192kbps", "هاني الرفاعي"),
        Reciter("Ali_Jaber_64kbps", "علي جابر"),
        Reciter("Abdullah_Basfar_192kbps", "عبد الله بصفر"),
        Reciter("Salah_Al_Budair_128kbps", "صلاح البدير"),
        Reciter("Khaalid_Abdullaah_al-Qahtaanee_192kbps", "خالد القحطاني"),
        Reciter("Abdullaah_3awwaad_Al-Juhaynee_128kbps", "عبد الله الجهني"),
        Reciter("Muhsin_Al_Qasim_192kbps", "محسن القاسم"),
        Reciter("Salaah_AbdulRahman_Bukhatir_128kbps", "صلاح بو خاطر"),
        Reciter("Abdullah_Matroud_128kbps", "عبد الله المطرود"),
        Reciter("Ibrahim_Akhdar_32kbps", "إبراهيم الأخضر"),
        Reciter("Yaser_Salamah_128kbps", "ياسر سلامة"),
        Reciter("Akram_AlAlaqimy_128kbps", "أكرم العلاقمي"),
    )

    var reciter: String
        get() = Store.prefs.getString("reciter", reciters.first().folder) ?: reciters.first().folder
        set(v) { Store.prefs.edit().putString("reciter", v).apply(); Store.bump() }
    var repeat: Int
        get() = Store.prefs.getInt("recite_repeat", 1)
        set(v) { Store.prefs.edit().putInt("recite_repeat", v).apply(); Store.bump() }
    /** How far to read from the chosen verse: AYAH (that verse), COUNT (N verses), SURAH (to its end), ON (into the next surahs). */
    enum class Range(val label: String) { AYAH("الآية فقط"), COUNT("عدد من الآيات"), SURAH("حتى نهاية السورة"), ON("متابعة السور") }
    var range: Range
        get() = runCatching { Range.valueOf(Store.prefs.getString("recite_range", null) ?: "") }.getOrDefault(Range.SURAH)
        set(v) { Store.prefs.edit().putString("recite_range", v.name).apply(); Store.bump() }
    var count: Int
        get() = Store.prefs.getInt("recite_count", 5)
        set(v) { Store.prefs.edit().putInt("recite_count", v).apply(); Store.bump() }
    val counts = listOf(3, 5, 7, 10, 20)
    fun rangeText(r: Range = range) = if (r == Range.COUNT) "${Quran.digits(count)} آيات" else r.label
    fun reciterName() = reciters.firstOrNull { it.folder == reciter }?.name ?: ""

    /** The verse being read (surah, ayah), or null when stopped. */
    var now by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    var paused by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    /** How many full players are on screen now (the small bar hides while one is). */
    var bars by mutableStateOf(0)

    private var player: MediaPlayer? = null
    private var played = 0
    private var basmalaFirst = false
    private lateinit var app: Context
    private var focus: AudioFocusRequest? = null

    private fun url(s: Int, a: Int) = "https://everyayah.com/data/$reciter/%03d%03d.mp3".format(s, a)

    /** Stop after this verse of the same surah, or null to read on; [story] keeps a story's own end. */
    private var stopAt: Int? = null
    private var story: Int? = null
    private var crossSurahs = false
    /** The range in use now (a one-off "this verse only" does not change the saved choice). */
    var playing by mutableStateOf(Range.SURAH)
        private set

    /** Where to stop when reading from verse [a] of surah [s] with range [r]. */
    private fun stopFor(s: Int, a: Int, r: Range): Int? {
        val last = Quran.surahs.getOrNull(s - 1)?.count ?: a
        return when (r) { Range.AYAH -> a; Range.COUNT -> (a + count - 1).coerceAtMost(last); Range.SURAH, Range.ON -> null }
    }

    fun play(c: Context, s: Int, a: Int, stopAfter: Int? = null, only: Range? = null) {
        Quran.load(c.applicationContext)
        story = stopAfter
        val r = only ?: range
        playing = if (stopAfter != null) Range.SURAH else r
        stopAt = stopAfter ?: stopFor(s, a, r)
        crossSurahs = stopAfter == null && r == Range.ON
        app = c.applicationContext
        Quran.load(app)
        played = 0
        basmalaFirst = a == 1 && s != 1 && s != 9
        now = s to a; paused = false
        if (!requestFocus()) { Toast.makeText(app, "الصوت مشغول الآن (مكالمة؟)", Toast.LENGTH_SHORT).show(); now = null; return }
        runCatching { app.startForegroundService(Intent(app, RecitationService::class.java)) }
        load()
    }

    private fun load() {
        val (s, a) = now ?: return
        player?.release()
        loading = true
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            setDataSource(if (basmalaFirst) url(1, 1) else url(s, a))
            setOnPreparedListener { loading = false; if (!paused) it.start() }
            setOnCompletionListener { advance() }
            setOnErrorListener { _, _, _ ->
                loading = false
                Toast.makeText(app, "تعذّر تشغيل التلاوة، تحقق من الإنترنت", Toast.LENGTH_LONG).show()
                stop(); true
            }
            prepareAsync()
        }
        RecitationService.refresh(app)
    }

    private fun advance() {
        val (s, a) = now ?: return
        if (basmalaFirst) { basmalaFirst = false; load(); return }
        // The verse was heard to its end: a point for the friend (once a day per verse).
        if (played == 0) runCatching { Progress.record(Progress.Kind.LISTEN, "$s:$a") }
        played++
        if (played < repeat) { player?.seekTo(0); player?.start(); return }
        played = 0
        if (stopAt != null && a >= stopAt!!) { stop(); return }
        val verses = Quran.surahs.getOrNull(s - 1)?.count ?: 0
        when {
            a < verses -> now = s to a + 1
            crossSurahs && s < 114 -> { now = s + 1 to 1; basmalaFirst = s + 1 != 9 }
            else -> { stop(); return }
        }
        load()
    }

    /** Jump to the previous or next verse of the surah being read. */
    fun skip(c: Context, delta: Int) {
        val (s, a) = now ?: return
        val verses = Quran.surahs.getOrNull(s - 1)?.count ?: return
        val na = (a + delta).coerceIn(1, verses)
        if (story != null) play(c, s, na, story) else play(c, s, na, only = playing)
    }

    /** Change how far to read while reading, counted from the verse being read now. */
    fun changeRange(r: Range) {
        range = r
        val (s, a) = now ?: return
        if (story != null) return
        playing = r; stopAt = stopFor(s, a, r); crossSurahs = r == Range.ON
        RecitationService.refresh(app)
    }

    fun pause() { player?.takeIf { it.isPlaying }?.pause(); paused = true; RecitationService.refresh(app) }
    fun resume() { if (now == null) return; paused = false; if (!loading) player?.start(); RecitationService.refresh(app) }
    fun stop() {
        player?.release(); player = null
        now = null; paused = false; loading = false
        focus?.let { f -> app.getSystemService(AudioManager::class.java).abandonAudioFocusRequest(f) }; focus = null
        if (::app.isInitialized) app.stopService(Intent(app, RecitationService::class.java))
    }

    /** Pause for calls and other audio; carry on when it ends. */
    private fun requestFocus(): Boolean {
        // Still holding focus from the verse before (changing the reciter or the verse while reading): asking again would
        // make Android send our own old request a LOSS, which paused the new reciter before it started.
        if (focus != null) return true
        val am = app.getSystemService(AudioManager::class.java)
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> { pause(); focus?.let { f -> am.abandonAudioFocusRequest(f) }; focus = null }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { player?.takeIf { it.isPlaying }?.pause() }
                    AudioManager.AUDIOFOCUS_GAIN -> if (!paused && now != null) player?.start()
                }
            }.build()
        focus = req
        return am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
}

/** Keeps the recitation alive in the background, with pause / stop in the notification. */
class RecitationService : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        when (i?.action) {
            PAUSE -> Recitation.pause()
            RESUME -> Recitation.resume()
            STOP -> { Recitation.stop(); return START_NOT_STICKY }
        }
        val n = build(this)
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(ID, n)
        if (Recitation.now == null) stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        private const val ID = 21
        private const val CH = "recitation"
        const val PAUSE = "com.alzikr.app.RECITE_PAUSE"
        const val RESUME = "com.alzikr.app.RECITE_RESUME"
        const val STOP = "com.alzikr.app.RECITE_STOP"

        private fun action(c: Context, a: String, code: Int) = PendingIntent.getService(c, code, Intent(c, RecitationService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        fun build(c: Context): Notification {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH, "تلاوة القرآن", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
            val (s, a) = Recitation.now ?: (1 to 1)
            val name = Quran.surahs.getOrNull(s - 1)?.name ?: ""
            val open = PendingIntent.getActivity(c, 30, Intent(c, MainActivity::class.java).putExtra("quran", "$s:$a").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            return Notification.Builder(c, CH).setSmallIcon(R.drawable.ic_stat_zikr)
                .setContentTitle("سورة $name · الآية ${Quran.digits(a)}")
                .setContentText(Recitation.reciterName() + " · " + Recitation.rangeText(Recitation.playing) + if (Recitation.repeat > 1) " · تكرار ×${Recitation.repeat}" else "")
                .setContentIntent(open).setOngoing(true).setShowWhen(false).setColor(0xFF1A3274.toInt())
                .addAction(Notification.Action.Builder(null, if (Recitation.paused) "متابعة" else "إيقاف مؤقت", action(c, if (Recitation.paused) RESUME else PAUSE, 31)).build())
                .addAction(Notification.Action.Builder(null, "إنهاء", action(c, STOP, 32)).build())
                .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1))
                .build()
        }

        fun refresh(c: Context) {
            if (Recitation.now == null) return
            if (Alerts.canPost(c)) c.getSystemService(NotificationManager::class.java).notify(ID, build(c))
        }
    }
}
