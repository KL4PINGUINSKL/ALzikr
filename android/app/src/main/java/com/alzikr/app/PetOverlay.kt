package com.alzikr.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.lang.ref.WeakReference

/**
 * The friend over other apps: a small sprite snapped to a screen edge (drag it anywhere; it only takes
 * touches on itself). Every [Pet.every] minutes a short bubble — dhikr, verse or hadith — shows for a few
 * seconds and goes. Quiet during calls, with the screen off, in quiet hours, and while ALzikr itself is open.
 */
class PetOverlay : Service(), LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    private val handler = Handler(Looper.getMainLooper())
    private var wm: WindowManager? = null
    private var view: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    /** (text, verse "s:a" or null) on screen now. */
    val bubble = mutableStateOf<Pair<String, String?>?>(null)
    val onRight = mutableStateOf(true)
    private var n = 0
    private var appOpen = false
    private val margin get() = (6 * resources.displayMetrics.density).toInt()
    private val screenW get() = resources.displayMetrics.widthPixels
    private val petPx get() = (Pet.floatSize * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        saved.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
        instance = WeakReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            STOP -> { Pet.floating = false; stopSelf(); return START_NOT_STICKY }
            HIDE -> { Pet.hiddenUntil = System.currentTimeMillis() + 3_600_000L; removeView(); handler.postDelayed({ show() }, 3_600_000L) }
            POKE -> { speak(force = true) }
            REFRESH -> { removeView(); show(); schedule() }
            APP_OPEN -> { appOpen = true; view?.visibility = android.view.View.GONE }
            APP_CLOSED -> { appOpen = false; view?.visibility = android.view.View.VISIBLE }
        }
        if (!Pet.floating || !Settings.canDrawOverlays(this) || Pet.species == null) { stopSelf(); return START_NOT_STICKY }
        val n = notice()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(ID, n)
        if (view == null && System.currentTimeMillis() >= Pet.hiddenUntil) show()
        if (intent?.action == null) schedule()
        registry.currentState = Lifecycle.State.RESUMED
        return START_STICKY
    }

    private fun notice(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "الصديق فوق التطبيقات", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        fun act(a: String, code: Int) = PendingIntent.getService(this, code, Intent(this, PetOverlay::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(this, 95, Intent(this, MainActivity::class.java).putExtra("page", "pet").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CH).setSmallIcon(R.drawable.ic_stat_zikr)
            .setContentTitle("${Pet.name} يرافقك").setContentText("يذكّرك كل ${if (Pet.every < 60) "${Pet.every} دقيقة" else "${Pet.every / 60} ساعة"}")
            .setContentIntent(open).setOngoing(true).setColor(0xFF1F7A5A.toInt())
            .addAction(Notification.Action.Builder(null, "إخفاء ساعة", act(HIDE, 96)).build())
            .addAction(Notification.Action.Builder(null, "إيقاف", act(STOP, 97)).build())
            .build()
    }

    private fun show() {
        if (view != null || !Settings.canDrawOverlays(this)) return
        val species = Pet.species ?: return
        wm = getSystemService(WindowManager::class.java)
        onRight.value = Pet.floatX != 0
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START; x = if (onRight.value) screenW - petPx - margin else margin; y = Pet.floatY }
        val v = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PetOverlay)
            setViewTreeSavedStateRegistryOwner(this@PetOverlay)
            setContent { AppTheme { Floating(species) } }
        }
        if (appOpen) v.visibility = android.view.View.GONE
        runCatching { wm?.addView(v, params) }.onSuccess { view = v }
    }

    @androidx.compose.runtime.Composable
    private fun Floating(species: Species) {
        val clock = rememberPetClock(gentle = true)
        val b = bubble.value
        val right = onRight.value
        Row(Modifier.onSizeChanged { fit(it.width) }, verticalAlignment = Alignment.CenterVertically) {
            if (right && b != null) Box(Modifier.padding(end = 4.dp)) { Bubble(b.first) { openApp(b.second) } }
            Box(Modifier.size(Pet.floatSize.dp).alpha(Pet.floatAlpha)
                .pointerInput(Unit) { detectDragGestures(onDragEnd = { snap() }) { ch, d -> ch.consume(); moveBy(d.x, d.y) } }
                .pointerInput(Unit) { detectTapGestures(onTap = { speak(force = true) }, onLongPress = { openApp(null) }) }) {
                PetSprite(species, {
                    val t = clock() / 1000f
                    Pose(t = t, blink = (t % 4.4f) < .15f, sleep = Pet.asleep(), happy = Pet.happy(), left = right, flap = if (b != null && (t % 1.2f) < .3f) .6f else 0f)
                }, Modifier.size(Pet.floatSize.dp))
            }
            if (!right && b != null) Box(Modifier.padding(start = 4.dp)) { Bubble(b.first) { openApp(b.second) } }
        }
    }

    /** Keeps the sprite on its edge when the bubble widens the window. */
    private fun fit(width: Int) {
        if (view == null) return
        params.x = if (onRight.value) (screenW - width - margin).coerceAtLeast(0) else margin
        runCatching { wm?.updateViewLayout(view, params) }
    }
    private fun moveBy(dx: Float, dy: Float) {
        if (view == null) return
        bubble.value = null
        params.x = (params.x + dx.toInt()).coerceIn(0, screenW - petPx)
        params.y = (params.y + dy.toInt()).coerceIn(0, resources.displayMetrics.heightPixels - petPx)
        runCatching { wm?.updateViewLayout(view, params) }
    }
    /** After a drag: to the nearest edge, out of the way. */
    private fun snap() {
        onRight.value = params.x + petPx / 2 > screenW / 2
        Pet.floatX = if (onRight.value) 1 else 0; Pet.floatY = params.y
        fit(petPx)
    }

    private fun speak(force: Boolean) {
        if (view == null || appOpen) return
        val audio = getSystemService(AudioManager::class.java)
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        if (!force && (audio.mode != AudioManager.MODE_NORMAL || !screenOn || Pet.quietNow())) return
        bubble.value = Pet.floatText(this, n++)
        handler.removeCallbacks(clear)
        handler.postDelayed(clear, if ((bubble.value?.first?.length ?: 0) > 80) 12_000L else 8_000L)
    }
    private val clear = Runnable { bubble.value = null }
    private val tick = object : Runnable { override fun run() { speak(force = false); handler.postDelayed(this, Pet.every * 60_000L) } }
    private fun schedule() { handler.removeCallbacks(tick); handler.postDelayed(tick, Pet.every * 60_000L) }

    private fun openApp(verse: String?) {
        bubble.value = null
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (verse != null) i.putExtra("quran", verse) else i.putExtra("page", "pet")
        runCatching { startActivity(i) }
    }

    private fun removeView() { view?.let { runCatching { wm?.removeView(it) } }; view = null }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeView()
        registry.currentState = Lifecycle.State.DESTROYED
        if (instance?.get() === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val CH = "pet_float"; private const val ID = 46
        private const val STOP = "com.alzikr.app.PET_STOP"; private const val HIDE = "com.alzikr.app.PET_HIDE"
        private const val POKE = "com.alzikr.app.PET_POKE"; private const val REFRESH = "com.alzikr.app.PET_REFRESH"
        private const val APP_OPEN = "com.alzikr.app.PET_APP_OPEN"; private const val APP_CLOSED = "com.alzikr.app.PET_APP_CLOSED"
        private var instance: WeakReference<PetOverlay>? = null
        private fun send(c: Context, a: String?) = runCatching { c.startForegroundService(Intent(c, PetOverlay::class.java).setAction(a)) }
        fun start(c: Context) { if (Pet.floating && Settings.canDrawOverlays(c)) send(c, null) }
        fun stop(c: Context) { c.stopService(Intent(c, PetOverlay::class.java)) }
        fun refresh(c: Context) { instance?.get()?.let { s -> s.handler.post { s.removeView(); s.show(); s.schedule() } } }
        fun poke(c: Context) { instance?.get()?.let { s -> s.handler.post { s.speak(force = true) } } }
        /** ALzikr on screen: the floating twin steps aside (the one in the app is there). Same process, so no service start from the background. */
        fun appVisible(c: Context, visible: Boolean) { instance?.get()?.let { s -> s.handler.post { s.appOpen = visible; s.view?.visibility = if (visible) android.view.View.GONE else android.view.View.VISIBLE } } }
    }
}
