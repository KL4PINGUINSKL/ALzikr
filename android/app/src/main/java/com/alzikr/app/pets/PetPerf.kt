package com.alzikr.app.pets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Where a friend is drawn; each place has its own frame-rate budget. */
enum class Place(val fps: Int) { APP(30), OVERLAY(24), KIDS(30), PREVIEW(15), GRID(12) }

/**
 * Performance Manager: how much motion is allowed right now.
 * - Frame rate per place, halved in battery-saving mode (the phone's or the app's own), never below 8.
 * - Reduced motion (the phone's "remove animations" or the app's own switch): a still picture, no code motion.
 * - Suspended while the screen is off, and per place while it is not visible (the overlay while ALzikr is open, the
 *   app while it is in the background): no frames are produced at all.
 * Values are snapshot state, so every friend on screen follows a change at once.
 */
object PetPerf {
    private var ctx: Context? = null
    var screenOn by mutableStateOf(true); private set
    var powerSave by mutableStateOf(false); private set
    var systemReduceMotion by mutableStateOf(false); private set
    private val hidden = mutableStateOf(setOf<Place>())

    /** The app's own switches (stored with the other pet settings). */
    var userReduceMotion by mutableStateOf(false)
    var userSaveBattery by mutableStateOf(false)

    fun init(c: Context) {
        if (ctx != null) return
        ctx = c.applicationContext
        refresh()
        val r = object : BroadcastReceiver() { override fun onReceive(x: Context, i: Intent) { refresh(i.action) } }
        val f = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) }
        // System broadcasts still arrive with NOT_EXPORTED; nothing outside the app can poke this receiver.
        runCatching { if (android.os.Build.VERSION.SDK_INT >= 33) ctx!!.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED) else ctx!!.registerReceiver(r, f) }
    }
    fun refresh(action: String? = null) {
        val c = ctx ?: return
        val pm = c.getSystemService(PowerManager::class.java)
        screenOn = when (action) { Intent.ACTION_SCREEN_OFF -> false; Intent.ACTION_SCREEN_ON -> true; else -> pm?.isInteractive ?: true }
        powerSave = pm?.isPowerSaveMode == true
        systemReduceMotion = runCatching { Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    fun setVisible(p: Place, visible: Boolean) { hidden.value = if (visible) hidden.value - p else hidden.value + p }

    val reduceMotion: Boolean get() = systemReduceMotion || userReduceMotion
    val saving: Boolean get() = powerSave || userSaveBattery
    /** May this place animate at all now? */
    fun animate(p: Place): Boolean = screenOn && !reduceMotion && p !in hidden.value
    fun fps(p: Place): Int = if (saving) (p.fps / 2).coerceAtLeast(8) else p.fps
    /** Code motion (hops, sways) is decoration: off when saving or reduced. */
    fun codeMotion(p: Place): Boolean = animate(p) && !saving
}
