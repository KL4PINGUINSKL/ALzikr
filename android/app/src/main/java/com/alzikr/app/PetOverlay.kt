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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
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
    /** What the window was built with; if the friend changed while ALzikr was open, it is rebuilt. */
    private var shown: String? = null
    private fun key() = "${Pet.species}|${Pet.species?.let { PetLooks.chosen(it)?.id }}|${Pet.floatSize}|${Pet.floatAlpha}|${Pet.floatMode}"
    /** Mini mode opened by a tap, for a moment. */
    val expanded = mutableStateOf(false)
    private val mini get() = Pet.floatMode == "mini" && !expanded.value
    private val quiet get() = Pet.floatMode == "quiet"
    /** The sprite's size now: 34 dp in mini mode, otherwise the chosen size. */
    private fun sz(): Int = if (mini) 34 else Pet.floatSize
    /** Hearts after a tap (bumped to replay), walking while it strolls, the tasbih count shown briefly. */
    val hearts = mutableStateOf(0)
    val walking = mutableStateOf(false)
    val count = mutableStateOf<Int?>(null)
    private val margin get() = (6 * resources.displayMetrics.density).toInt()
    private val screenW get() = resources.displayMetrics.widthPixels
    private val petPx get() = (sz() * resources.displayMetrics.density).toInt()

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
            APP_CLOSED -> { appOpen = false; if (shown != key()) { removeView(); show() } else view?.visibility = android.view.View.VISIBLE }
        }
        if (!Pet.floating || !Settings.canDrawOverlays(this) || Pet.species == null || Pet.hidden) { stopSelf(); return START_NOT_STICKY }
        val n = notice()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(ID, n)
        if (view == null && System.currentTimeMillis() >= Pet.hiddenUntil) show()
        if (intent?.action == null) schedule()
        handler.removeCallbacks(minute); handler.postDelayed(minute, 30_000L)
        handler.removeCallbacks(stroll); handler.postDelayed(stroll, 20_000L)
        registry.currentState = Lifecycle.State.RESUMED
        return START_STICKY
    }

    private fun notice(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "الصديق فوق التطبيقات", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        fun act(a: String, code: Int) = PendingIntent.getService(this, code, Intent(this, PetOverlay::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(this, 95, Intent(this, MainActivity::class.java).putExtra("page", "pet").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CH).setSmallIcon(R.drawable.ic_stat_zikr)
            .setContentTitle("${Pet.name} يرافقك · ${Friendship.title()}").setContentText("يذكّرك كل ${if (Pet.every < 60) "${Pet.every} دقيقة" else "${Pet.every / 60} ساعة"}")
            .setContentIntent(open).setOngoing(true).setColor(0xFF1A3274.toInt())
            .addAction(Notification.Action.Builder(null, "إخفاء ساعة", act(HIDE, 96)).build())
            .addAction(Notification.Action.Builder(null, "إيقاف", act(STOP, 97)).build())
            .build()
    }

    private fun show() {
        if (view != null || !Settings.canDrawOverlays(this)) return
        if (Pet.species == null) return
        shown = key()
        wm =getSystemService(WindowManager::class.java)
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
            // Left-to-right for the layout only, so the sprite stays on its edge and the bubble opens inward (the text inside stays Arabic).
            setContent { AppTheme { androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) { Floating() } } }
        }
        v.setOnTouchListener(touch)
        if (appOpen) v.visibility = android.view.View.GONE
        runCatching { wm?.addView(v, params) }.onSuccess { view = v }
    }

    @androidx.compose.runtime.Composable
    private fun Floating() {
        // Read the store, so choosing another friend or look in ALzikr changes this one at once.
        @Suppress("UNUSED_VARIABLE") val v = Store.version
        val species = Pet.species ?: return
        val b = bubble.value
        val right = onRight.value
        val asleep = Pet.asleep()
        val hop = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
        val h = hearts.value
        androidx.compose.runtime.LaunchedEffect(h) {
            if (h == 0) return@LaunchedEffect
            repeat(2) { hop.animateTo(1f, androidx.compose.animation.core.tween(150)); hop.animateTo(0f, androidx.compose.animation.core.tween(190)) }
        }
        Row(Modifier.onSizeChanged { fit(it.width) }, verticalAlignment = Alignment.CenterVertically) {
            if (right && b != null) Box(Modifier.padding(end = 4.dp)) { Bubble(b.first) { openApp(b.second) } }
            Box(Modifier.size(sz().dp).alpha((if (asleep) Pet.floatAlpha * .75f else Pet.floatAlpha) * (if (mini) .7f else 1f))) {
                val look = PetLooks.chosen(species)
                androidx.compose.runtime.key(species, look?.id) {
                    // Keeps moving (unless turned off or asleep); always while it speaks or strolls.
                    PetLottie(species, Modifier.size(sz().dp).hopping { hop.value }, look = look,
                        playing = b != null || walking.value || hop.isRunning || (Pet.floatMove && !asleep && !mini && !quiet),
                        speed = if (asleep) .45f else if (walking.value) 1.4f else 1f, place = com.alzikr.app.pets.Place.OVERLAY,
                        // The engine's states: a skin with its own walk/sleep/talk animation uses it; others fall back to idle.
                        state = when { asleep -> com.alzikr.app.pets.PetState.SLEEP; walking.value -> com.alzikr.app.pets.PetState.WALK; b != null -> com.alzikr.app.pets.PetState.IDLE; else -> com.alzikr.app.pets.PetState.IDLE })
                }
                if (asleep && b == null) androidx.compose.material3.Text("💤", fontSize = 14.sp, modifier = Modifier.align(if (right) Alignment.TopStart else Alignment.TopEnd))
                if (h > 0) FloatingHearts(h, Modifier.align(Alignment.TopCenter))
                count.value?.let { c ->
                    androidx.compose.material3.Text(Quran.digits(c), color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).background(P.accent, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 6.dp))
                }
            }
            if (!right && b != null) Box(Modifier.padding(start = 4.dp)) { Bubble(b.first) { openApp(b.second) } }
        }
    }

    /** Touches on the sprite: drag (raw screen coordinates, so it follows the finger), tap to speak, long press for the app. Touches on the bubble go to it. */
    private val touch by lazy { object : android.view.View.OnTouchListener {
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var downAt = 0L; var dragging = false; var mine = false
        var lastTap = 0L
        /** One tap waits a moment: a second tap makes it a tasbih instead of a word. */
        val single = Runnable {
            if (mini) { expanded.value = true; handler.postDelayed({ expanded.value = false }, 15_000L); return@Runnable }
            hearts.value++; Progress.bond("ftap" + System.currentTimeMillis() / 1_800_000L); speak(force = true)
        }
        val slop = android.view.ViewConfiguration.get(this@PetOverlay).scaledTouchSlop
        val long = Runnable { if (!dragging && mine) { mine = false; openApp(null) } }
        @android.annotation.SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: android.view.View, e: android.view.MotionEvent): Boolean {
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // On the sprite? (it sits at the window's outer edge; the bubble is the rest)
                    mine = if (onRight.value) e.x >= v.width - petPx else e.x <= petPx
                    if (!mine) return false
                    downX = e.rawX; downY = e.rawY; startX = params.x; startY = params.y; downAt = System.currentTimeMillis(); dragging = false
                    handler.postDelayed(long, 650)
                    return true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (!mine) return false
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (!dragging && kotlin.math.hypot(dx, dy) > slop) { dragging = true; stopStroll(); handler.removeCallbacks(long); bubble.value = null; showTrash() }
                    if (dragging) moveTo(startX + dx.toInt(), startY + dy.toInt())
                    return true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(long)
                    if (!mine) return false
                    mine = false
                    if (dragging) dropped() else if (e.actionMasked == android.view.MotionEvent.ACTION_UP && System.currentTimeMillis() - downAt < 600) {
                        val now = System.currentTimeMillis()
                        if (!Pet.floatTasbih) { hearts.value++; speak(force = true) }
                        else if (now - lastTap < 320) { handler.removeCallbacks(single); lastTap = 0L; tasbih() }
                        else { lastTap = now; handler.postDelayed(single, 320) }
                    }
                    return true
                }
            }
            return false
        }
    } }

    /** Keeps the sprite on its edge when the bubble widens the window. */
    private fun fit(width: Int) {
        if (view == null) return
        params.x = if (onRight.value) (screenW - width - margin).coerceAtLeast(0) else margin
        runCatching { wm?.updateViewLayout(view, params) }
    }
    private fun moveTo(x: Int, y: Int) {
        if (view == null) return
        params.x = x.coerceIn(0, screenW - petPx)
        params.y = y.coerceIn(0, resources.displayMetrics.heightPixels - petPx)
        runCatching { wm?.updateViewLayout(view, params) }
        // Near the ✕ at the bottom?
        val d = resources.displayMetrics.density
        val loc = IntArray(2); view?.getLocationOnScreen(loc)
        val tloc = IntArray(2); trash?.getLocationOnScreen(tloc)
        val tx = if (trash != null) tloc[0] + (trash?.width ?: 0) / 2f else screenW / 2f
        val ty = if (trash != null) tloc[1] + 30 * d else resources.displayMetrics.heightPixels - 110 * d
        val cx = (if (onRight.value) loc[0] + (view?.width ?: petPx) - petPx / 2f else loc[0] + petPx / 2f); val cy = loc[1] + petPx / 2f
        overTrash.value = kotlin.math.hypot(cx - tx, cy - ty) < 95 * d
    }

    // ---- the ✕ that appears at the bottom while dragging: drop the friend on it to hide it
    val overTrash = mutableStateOf(false)
    private var trash: ComposeView? = null
    private fun showTrash() {
        if (trash != null) return
        val p = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = (72 * resources.displayMetrics.density).toInt()
        }
        val v = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PetOverlay); setViewTreeSavedStateRegistryOwner(this@PetOverlay)
            setContent { AppTheme { TrashTarget(overTrash.value) } }
        }
        runCatching { wm?.addView(v, p) }.onSuccess { trash = v }
    }
    private fun hideTrash() { trash?.let { runCatching { wm?.removeView(it) } }; trash = null }
    private fun dropped() {
        hideTrash()
        if (overTrash.value) {
            overTrash.value = false
            Pet.floating = false
            android.widget.Toast.makeText(this, "أُخفي ${Pet.name}. تعيده من صفحة «صديقي»", android.widget.Toast.LENGTH_LONG).show()
            stopSelf()
        } else snap()
    }
    /** After a drag: to the nearest edge, out of the way. */
    private fun snap() {
        onRight.value = params.x + petPx / 2 > screenW / 2
        Pet.floatX = if (onRight.value) 1 else 0; Pet.floatY = params.y
        fit(petPx)
    }

    private fun speak(force: Boolean): Boolean {
        if (view == null || appOpen) return false
        val audio = getSystemService(AudioManager::class.java)
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        if (!force && (quiet || audio.mode != AudioManager.MODE_NORMAL || !screenOn || Pet.quietNow())) return false
        bubble.value = Pet.floatText(this, n++)
        handler.removeCallbacks(clear)
        handler.postDelayed(clear, if ((bubble.value?.first?.length ?: 0) > 80) 12_000L else 8_000L)
        return true
    }
    private val clear = Runnable { bubble.value = null }

    /** Two taps: one tasbih, counted with the app's tasbih (and its notification); every 33 it cheers. */
    private fun tasbih() {
        val c = Data.tasbihTap()
        Haptics.tap(this, c % 33 == 0)
        runCatching { CounterNotice.update(this) }
        count.value = c
        val word = "${Pet.tasbihWord(c)} · ${Quran.digits(c)}"
        bubble.value = (if (c % 33 == 0) "$word\nما شاء الله، أتممت ${Quran.digits(33)}" else word) to null
        if (c % 33 == 0) { hearts.value++; Pet.gain("tasbih_${c / 33}", 5) }
        handler.removeCallbacks(clear); handler.postDelayed(clear, 2_500L)
        handler.removeCallbacks(hideCount); handler.postDelayed(hideCount, 4_000L)
    }
    private val hideCount = Runnable { count.value = null }

    /** Every half minute: a word before the prayer (once for each prayer). */
    private val minute = object : Runnable { override fun run() { nudge(); due(); joy(); handler.postDelayed(this, 30_000L) } }
    /** From level 6: when the day's hearts are full it hops and hearts fly, at most once an hour. */
    private var joyAt = 0L
    private fun joy() {
        if (view == null || appOpen || mini || quiet || bubble.value != null || !Friendship.has("joy") || Pet.asleep()) return
        if (System.currentTimeMillis() - joyAt < 3_600_000L || Pet.mood() < 80) return
        if (!getSystemService(PowerManager::class.java).isInteractive) return
        joyAt = System.currentTimeMillis(); hearts.value++
    }
    private fun nudge() {
        if (!Pet.prayerNudge || quiet || !Store.hasLocation || view == null || appOpen || Pet.kids) return
        val w = runCatching { window() }.getOrNull() ?: return
        if (!w.next.alertable) return
        val left = ((w.nextAt - System.currentTimeMillis()) / 60_000L).toInt()
        if (left !in 1..10 || Store.prefs.getLong("pet_nudged", 0L) == w.nextAt) return
        val audio = getSystemService(AudioManager::class.java)
        if (audio.mode != AudioManager.MODE_NORMAL || !getSystemService(PowerManager::class.java).isInteractive) return
        Store.prefs.edit().putLong("pet_nudged", w.nextAt).apply()
        hearts.value++
        bubble.value = "بقي ${Quran.digits(left)} ${if (left in 3..10) "دقائق" else "دقيقة"} على صلاة ${w.next.ar}، استعد لها 🕌" to null
        handler.removeCallbacks(clear); handler.postDelayed(clear, 12_000L)
    }

    /** A stroll: now and then it walks a little up or down its edge, then rests. */
    private var walk: android.animation.ValueAnimator? = null
    private val stroll = object : Runnable { override fun run() { strollOnce(); handler.postDelayed(this, (25_000L..55_000L).random()) } }
    private fun strollOnce() {
        if (!Pet.wander || mini || quiet || view == null || appOpen || bubble.value != null || walk != null || Pet.asleep()) return
        if (!getSystemService(PowerManager::class.java).isInteractive) return
        val d = resources.displayMetrics.density
        val h = resources.displayMetrics.heightPixels
        val top = (90 * d).toInt(); val bottom = h - petPx - (160 * d).toInt()
        if (bottom <= top) return
        val step = ((40..150).random() * d).toInt() * (if (Math.random() < .5) -1 else 1)
        val to = (if (params.y + step in top..bottom) params.y + step else params.y - step).coerceIn(top, bottom)
        val from = params.y
        if (to == from) return
        walking.value = true
        walk = android.animation.ValueAnimator.ofInt(from, to).apply {
            duration = (kotlin.math.abs(to - from) / d * 14).toLong().coerceIn(900L, 2600L)
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { a -> if (view != null) { params.y = a.animatedValue as Int; runCatching { wm?.updateViewLayout(view, params) } } }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) { walking.value = false; walk = null; Pet.floatY = params.y }
            })
            start()
        }
    }
    private fun stopStroll() { walk?.cancel(); walk = null; walking.value = false }
    /**
     * The reminder is due once [Pet.every] minutes (wall clock, kept across restarts) have passed since the last bubble.
     * Checked every half minute, so a reminder missed while ALzikr was open or the screen was off comes as soon as it
     * can (user, 2026-11-28: the friend over the apps had stopped reminding; a one-shot timer skipped a whole period
     * whenever it fell while the app was open, and every restart or settings change started it again from zero).
     */
    private fun due() {
        val last = Store.prefs.getLong("pet_said_at", 0L)
        val now = System.currentTimeMillis()
        if (last == 0L || last > now) { Store.prefs.edit().putLong("pet_said_at", now).apply(); return }
        if (now - last < Pet.every * 60_000L || bubble.value != null) return
        if (speak(force = false)) Store.prefs.edit().putLong("pet_said_at", now).apply()
    }
    private fun schedule() {}

    private fun openApp(verse: String?) {
        bubble.value = null
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (verse != null) i.putExtra("quran", verse) else i.putExtra("page", "pet")
        runCatching { startActivity(i) }
    }

    private fun removeView() { stopStroll(); hideTrash(); view?.let { runCatching { wm?.removeView(it) } }; view = null }

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
        fun start(c: Context) { if (Pet.floating && !Pet.hidden && Settings.canDrawOverlays(c)) send(c, null) }
        fun stop(c: Context) { c.stopService(Intent(c, PetOverlay::class.java)) }
        fun refresh(c: Context) { instance?.get()?.let { s -> s.handler.post { s.removeView(); s.show(); s.schedule() } } }
        fun poke(c: Context) { instance?.get()?.let { s -> s.handler.post { s.speak(force = true) } } }
        /** ALzikr on screen: the floating twin steps aside (the one in the app is there). Same process, so no service start from the background. */
        fun appVisible(c: Context, visible: Boolean) { instance?.get()?.let { s -> s.handler.post {
            s.appOpen = visible
            com.alzikr.app.pets.PetPerf.setVisible(com.alzikr.app.pets.Place.OVERLAY, !visible)
            if (visible) { s.stopStroll(); s.view?.visibility = android.view.View.GONE }
            // Back from ALzikr: if the friend, its look or its size changed there, build it anew.
            else if (s.view != null && s.shown != s.key()) { s.removeView(); s.show() }
            else s.view?.visibility = android.view.View.VISIBLE
        } } }
    }
}

/** The drop target: a soft round ✕ that grows and turns red when the friend is over it. */
@androidx.compose.runtime.Composable
private fun TrashTarget(over: Boolean) {
    val size = if (over) 76.dp else 60.dp
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size).androidx_clipCircle().background(if (over) P.danger else P.surface.copy(alpha = .92f)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text("✕", color = if (over) androidx.compose.ui.graphics.Color.White else P.ink, fontSize = if (over) 30.sp else 24.sp)
        }
        androidx.compose.material3.Text("اسحبه هنا لإخفائه", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .45f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
private fun Modifier.androidx_clipCircle() = this.clip(androidx.compose.foundation.shape.CircleShape)

/** A little hop: [lift] 0..1 raises it and squashes it a touch. */
private fun Modifier.hopping(lift: () -> Float) = this.graphicsLayer { val v = lift(); translationY = -v * size.height * .22f; scaleX = 1f + v * .05f; scaleY = 1f - v * .03f }

/** Three hearts rising and fading after a tap. */
@androidx.compose.runtime.Composable
private fun FloatingHearts(key: Int, modifier: Modifier) {
    val t = androidx.compose.runtime.remember(key) { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(key) { t.animateTo(1f, androidx.compose.animation.core.tween(1300)) }
    if (t.value >= 1f) return
    Row(modifier.graphicsLayer { val v = t.value; translationY = -v * 60f; alpha = 1f - v }) {
        listOf("❤", "💚", "❤").forEach { androidx.compose.material3.Text(it, fontSize = 12.sp, color = P.danger) }
    }
}
