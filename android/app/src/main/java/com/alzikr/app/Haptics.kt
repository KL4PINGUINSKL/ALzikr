package com.alzikr.app

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * One vibration for every counter (tasbih, adhkar, widget, notification, dark screen, qibla).
 * Long enough to be felt (very short pulses are dropped by many phones), and marked as an alarm-class
 * vibration so Android does not ignore it when the tap comes from the widget or the notification
 * (those taps arrive while the app is in the background, where media vibrations are blocked).
 */
object Haptics {
    /** 0 off, 1 light, 2 medium (default), 3 strong. */
    var level: Int
        get() = Store.prefs.getInt("vib_level", 2)
        set(v) { Store.prefs.edit().putInt("vib_level", v.coerceIn(0, 3)).apply(); Store.bump() }
    val names = listOf("بلا اهتزاز", "خفيف", "متوسط", "قوي")

    private fun vibrator(c: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) c.getSystemService(VibratorManager::class.java)?.defaultVibrator else c.getSystemService(Vibrator::class.java)

    /** A tap; [end] = the end of a round (a double pulse). */
    fun tap(c: Context, end: Boolean = false) {
        Store.init(c)
        val l = level
        if (l == 0) return
        val ms = when (l) { 1 -> 25L; 3 -> 60L; else -> 40L }
        val amp = when (l) { 1 -> 110; 3 -> 255; else -> 190 }
        val effect = if (end) VibrationEffect.createWaveform(longArrayOf(0, ms * 2, 90, ms * 2), intArrayOf(0, amp, 0, amp), -1)
            else VibrationEffect.createOneShot(ms, amp)
        play(c, effect)
    }

    /** A single pulse of [ms] (the qibla when aligned). */
    fun pulse(c: Context, ms: Long = 60) { Store.init(c); if (level > 0) play(c, VibrationEffect.createOneShot(ms, 220)) }

    private fun play(c: Context, effect: VibrationEffect) = runCatching {
        val v = vibrator(c) ?: return@runCatching
        if (!v.hasVibrator()) return@runCatching
        if (Build.VERSION.SDK_INT >= 33) v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        else @Suppress("DEPRECATION") v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
    }
}
