package com.alzikr.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tasbih with a "switched-off" screen: pure black at the lowest brightness, shown over the lock
 * screen (no unlocking); a tap anywhere or a volume key counts, with a small vibration.
 * A completely dark screen takes no touches at all, so the screen stays on but looks off.
 */
class TasbihDarkActivity : ComponentActivity() {
    private val count = mutableIntStateOf(0)
    private val showHelp = mutableStateOf(true)
    private val levels = floatArrayOf(0.02f, 0.12f, 0.35f)
    private val level = mutableIntStateOf(0)

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        warmStates()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = levels[level.intValue] }
        count.intValue = Data.tasbih
        setContent {
            val c = count.intValue
            val set = Data.tasbihSet
            Box(Modifier.fillMaxSize().background(Color.Black)
                .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { tap() }, onLongClick = { finish() }),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${c % set}", color = Color(0xFFB8B8B8), fontSize = 110.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("الدورة ${c / set + 1} من $set · المجموع $c", color = Color(0xFF8A8A8A), fontSize = 16.sp)
                }
                // ☀ changes the screen light (its own tap does not count).
                Text("☀ ${level.intValue + 1}", color = Color(0xFF8A8A8A), fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 20.dp).combinedClickable(onClick = {
                        level.intValue = (level.intValue + 1) % levels.size
                        window.attributes = window.attributes.apply { screenBrightness = levels[level.intValue] }
                    }).padding(12.dp))
                if (showHelp.value) Text("اضغط في أي مكان للتسبيح، أو بأزرار الصوت.\nضغطة مطوّلة للخروج.", color = Color(0xFF7A7A7A), fontSize = 14.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp))
            }
        }
    }

    private fun tap() {
        val n = Data.tasbihTap()
        count.intValue = n
        if (n >= 3) showHelp.value = false
        runCatching { getSystemService(Vibrator::class.java)?.vibrate(if (n % Data.tasbihSet == 0) VibrationEffect.createWaveform(longArrayOf(0, 80, 100, 80), -1) else VibrationEffect.createOneShot(16, 110)) }
        TasbihWidget.update(this)
    }

    /** The volume keys count too (the phone can stay in a pocket). */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) { if (event?.repeatCount == 0) tap(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() { super.onResume(); count.intValue = Data.tasbih }

    companion object {
        fun open(c: Context) = c.startActivity(Intent(c, TasbihDarkActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * ALzikr's lasting notification: the next prayer with a live countdown, and the tasbih («سبّح +١»)
 * usable on the lock screen. Swiping it away brings it back; only «إخفاء» (or the switch) removes it.
 */
object TasbihNotice {
    private const val CH = "tasbih"
    private const val ID = 41
    const val BACK = "com.alzikr.app.NOTICE_BACK"
    const val HIDE = "com.alzikr.app.NOTICE_HIDE"
    var on: Boolean
        get() = Store.prefs.getBoolean("tasbih_notice", false)
        set(v) { Store.prefs.edit().putBoolean("tasbih_notice", v).apply(); Store.bump() }

    /** The next prayer (not the sunrise) and its time. */
    private fun next(): Pair<Prayer, Long> {
        val zone = java.time.ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = java.time.LocalDate.now()
        for (d in listOf(today, today.plusDays(1))) {
            val t = Store.times(d)
            Prayer.entries.filter { it.alertable }.forEach { p ->
                val at = d.atStartOfDay(zone).plusMinutes(t.getValue(p).toLong()).toInstant().toEpochMilli()
                if (at > now) return p to at
            }
        }
        return Prayer.FAJR to now
    }

    fun update(c: Context) {
        Store.init(c)
        val nm = c.getSystemService(NotificationManager::class.java)
        if (!on || !Alerts.canPost(c) || !Store.hasLocation) { nm.cancel(ID); return }
        nm.createNotificationChannel(NotificationChannel(CH, "إشعار ALzikr الدائم", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
        val set = Data.tasbihSet; val n = Data.tasbih
        val (p, at) = next()
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val tap = PendingIntent.getBroadcast(c, 62, Intent(c, TasbihWidget::class.java).setAction(TasbihWidget.TAP), flags)
        val dark = PendingIntent.getActivity(c, 63, Intent(c, TasbihDarkActivity::class.java), flags)
        val hide = PendingIntent.getBroadcast(c, 64, Intent(c, AlertReceiver::class.java).setAction(HIDE), flags)
        val back = PendingIntent.getBroadcast(c, 65, Intent(c, AlertReceiver::class.java).setAction(BACK), flags)
        val open = PendingIntent.getActivity(c, 66, Intent(c, MainActivity::class.java).putExtra("tab", "prayer").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), flags)
        val time = Alerts.timeText(java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).let { it.hour * 60 + it.minute })
        val notice = Notification.Builder(c, CH).setSmallIcon(R.drawable.ic_stat_zikr)
            .setContentTitle("${p.ar} $time")
            .setContentText("السبحة: ${n % set} من $set · المجموع $n")
            .setWhen(at).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
            .setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC).setColor(0xFF1F7A5A.toInt())
            .setContentIntent(open).setDeleteIntent(back)
            .addAction(Notification.Action.Builder(null, "سبّح +١", tap).build())
            .addAction(Notification.Action.Builder(null, "الشاشة المظلمة", dark).build())
            .addAction(Notification.Action.Builder(null, "إخفاء", hide).build())
            .build()
        nm.notify(ID, notice)
    }
}
