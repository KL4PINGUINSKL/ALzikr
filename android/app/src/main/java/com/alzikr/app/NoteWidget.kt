package com.alzikr.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.RemoteViews
import java.time.LocalDate

/** «ملاحظتي» on the home screen: the note chosen for it, as a paper scrap with a pin. A tap opens the notes. */
class NoteWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) { Store.init(c); update(c) }

    companion object {
        fun update(c: Context) {
            Store.init(c)
            val manager = AppWidgetManager.getInstance(c)
            val ids = manager.getAppWidgetIds(ComponentName(c, NoteWidget::class.java))
            if (ids.isEmpty()) return
            val n = Store.notes().firstOrNull { it.widget } ?: Store.notes().firstOrNull { it.pinned }
            val v = RemoteViews(c.packageName, R.layout.widget_note)
            val paper = Md.paper(n?.paper ?: 0)
            if (Build.VERSION.SDK_INT >= 31) v.setColorStateList(R.id.n_root, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(ThemeColors.argb(paper)))
            v.setTextViewText(R.id.n_text, n?.let { Md.spanned(it.text, 0xFF2A2A2A.toInt()) } ?: "اختر ملاحظة لهذه الأداة: افتحها في «يومي ‹ ملاحظات» وفعّل «على الشاشة الرئيسية».")
            val open = PendingIntent.getActivity(c, 81, Intent(c, MainActivity::class.java).putExtra("tab", "notes").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            v.setOnClickPendingIntent(R.id.n_frame, open)
            manager.updateAppWidget(ids, v)
        }
    }
}

/** «يومي اليوم»: how far the day is, and up to five open tasks for today; a tap on one ticks it. */
class DayWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) { Store.init(c); update(c) }

    companion object {
        private val rows = intArrayOf(R.id.d_t0, R.id.d_t1, R.id.d_t2, R.id.d_t3, R.id.d_t4)
        fun update(c: Context) {
            Store.init(c)
            val manager = AppWidgetManager.getInstance(c)
            val ids = manager.getAppWidgetIds(ComponentName(c, DayWidget::class.java))
            if (ids.isEmpty()) return
            val pal = ThemeColors.of(c)
            val v = RemoteViews(c.packageName, R.layout.widget_day)
            ThemeColors.background(v, R.id.d_root, pal.surface)
            val today = LocalDate.now()
            val tasks = Store.tasks().filter { it.on(today) }
            val open = tasks.filter { !it.done }
            val habits = Data.habits()
            v.setTextViewText(R.id.d_title, "☀ يومي · أنجزت ${tasks.size - open.size} من ${tasks.size}")
            v.setTextColor(R.id.d_title, ThemeColors.argb(pal.ink))
            v.setTextViewText(R.id.d_sub, "العادات ${habits.count { it.doneToday }}/${habits.size} · الورد ${Data.pagesToday().coerceAtMost(Data.wirdToday())}/${Data.wirdToday()} ص")
            v.setTextColor(R.id.d_sub, ThemeColors.argb(pal.gold))
            rows.forEachIndexed { i, r ->
                val t = open.getOrNull(i)
                if (t == null) { v.setViewVisibility(r, View.GONE); return@forEachIndexed }
                v.setViewVisibility(r, View.VISIBLE)
                v.setTextViewText(r, "◻  ${t.text}" + (t.after?.let { " · بعد ${it.ar}" } ?: ""))
                v.setTextColor(r, ThemeColors.argb(pal.ink))
                v.setOnClickPendingIntent(r, PendingIntent.getBroadcast(c, ("dw" + t.id).hashCode(),
                    Intent(c, TaskReceiver::class.java).setAction(TaskReminders.ACTION_DONE).putExtra("id", t.id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            }
            if (open.isEmpty()) { v.setViewVisibility(rows[0], View.VISIBLE); v.setTextViewText(rows[0], if (tasks.isEmpty()) "لا مهام لليوم" else "أنجزت مهام اليوم كلها 🌟"); v.setTextColor(rows[0], ThemeColors.argb(pal.muted)) }
            val app = PendingIntent.getActivity(c, 82, Intent(c, MainActivity::class.java).putExtra("tab", "today").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            v.setOnClickPendingIntent(R.id.d_title, app); v.setOnClickPendingIntent(R.id.d_sub, app)
            manager.updateAppWidget(ids, v)
        }
    }
}

/**
 * The note bubble over other apps: a small pin at the screen's edge (drag it anywhere); a tap opens the note as a
 * paper card, another tap folds it. Plain Android views, so it stays light. Needs «display over other apps».
 */
class NoteBubble : Service() {
    private var wm: WindowManager? = null
    private var view: LinearLayout? = null
    private var card: View? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Store.init(this)
        if (intent?.action == STOP) { Store.notes().firstOrNull { it.bubble }?.let { Store.noteInBubble(it.id, false) }; stopSelf(); return START_NOT_STICKY }
        val note = Store.notes().firstOrNull { it.bubble }
        if (note == null || !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "ملاحظة فوق التطبيقات", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        val stop = PendingIntent.getService(this, 83, Intent(this, NoteBubble::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(this, CH).setSmallIcon(R.drawable.ic_stat_zikr).setContentTitle("📌 ${Md.title(note.text)}").setContentText("ملاحظتك فوق التطبيقات")
            .setOngoing(true).setColor(0xFF1A3274.toInt()).addAction(Notification.Action.Builder(null, "إزالة الفقاعة", stop).build()).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(ID, n)
        show(note)
        return START_STICKY
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun show(note: Store.Note) {
        wm = wm ?: getSystemService(WindowManager::class.java)
        view?.let { runCatching { wm?.removeView(it) } }; view = null; card = null
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; clipChildren = false; clipToPadding = false; setPadding(dp(12), dp(10), dp(12), dp(14)) }
        // The closed bubble: a round slip of the note's paper with a drawn push-pin and a soft shadow.
        val pin = PinBadge(this, Md.paper(note.paper)).apply { elevation = dp(6).toFloat() }
        root.addView(pin, LinearLayout.LayoutParams(dp(54), dp(54)))
        params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            val sp = getSharedPreferences("alzikr", MODE_PRIVATE); x = sp.getInt("bubble_x", dp(6)); y = sp.getInt("bubble_y", dp(180))
        }
        // Drag to move (it settles at the nearer side); a tap (no real move) opens or folds the note.
        var sx = 0f; var sy = 0f; var px = 0; var py = 0; var moved = false
        pin.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = params.x; py = params.y; moved = false; pin.animate().scaleX(.92f).scaleY(.92f).setDuration(90).start() }
                MotionEvent.ACTION_MOVE -> { val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (moved || kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(8)) { moved = true; params.x = px + dx.toInt(); params.y = py + dy.toInt(); runCatching { wm?.updateViewLayout(root, params) } } }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    pin.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    if (moved) {
                        val w = resources.displayMetrics.widthPixels
                        if (card == null) params.x = if (params.x + dp(31) < w / 2) dp(6) else w - dp(66)
                        runCatching { wm?.updateViewLayout(root, params) }
                        getSharedPreferences("alzikr", MODE_PRIVATE).edit().putInt("bubble_x", params.x).putInt("bubble_y", params.y).apply()
                    } else if (e.action == MotionEvent.ACTION_UP) toggle(root, note)
                }
            }
            true
        }
        runCatching { wm?.addView(root, params) }.onSuccess { view = root }
    }

    private fun toggle(root: LinearLayout, note: Store.Note) {
        card?.let { root.removeView(it); card = null; return }
        val fresh = Store.notes().firstOrNull { it.id == note.id } ?: note
        val paper = Md.paper(fresh.paper).let { android.graphics.Color.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt()) }
        val ink = 0xFF2A2A2A.toInt()
        val text = TextView(this).apply {
            setText(Md.spanned(fresh.text, ink)); textSize = 14.5f; setTextColor(ink); setLineSpacing(dp(4).toFloat(), 1f)
            textDirection = View.TEXT_DIRECTION_RTL; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
        val scroll = ScrollView(this).apply { addView(text); isVerticalScrollBarEnabled = false }
        // The open note: a paper card hanging from the pin, rounded a little, with a shadow and two quiet buttons.
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(10)); elevation = dp(10).toFloat()
            background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(paper); setStroke(dp(1), 0x22000000) }
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(scroll, LinearLayout.LayoutParams(dp(250), if (fresh.text.lines().size > 10 || fresh.text.length > 400) dp(280) else LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(View(this@NoteBubble).apply { setBackgroundColor(0x1F000000) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(10) })
            addView(LinearLayout(this@NoteBubble).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                fun btn(label: String, color: Int, act: () -> Unit) = TextView(this@NoteBubble).apply {
                    setText(label); setTextColor(color); textSize = 13f; setPadding(dp(8), dp(8), dp(8), dp(4)); setOnClickListener { act() }
                }
                addView(btn("فتح في ALzikr", 0xFF1A3274.toInt()) { toggle(root, note); startActivity(Intent(this@NoteBubble, MainActivity::class.java).putExtra("tab", "notes").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) })
                addView(View(this@NoteBubble), LinearLayout.LayoutParams(0, 1, 1f))
                addView(btn("طيّ", 0x992A2A2A.toInt()) { toggle(root, note) })
            })
        }
        root.addView(box, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = -dp(14) })
        // Keep the card on screen.
        val w = resources.displayMetrics.widthPixels
        if (params.x + dp(300) > w) { params.x = (w - dp(300)).coerceAtLeast(dp(4)); runCatching { wm?.updateViewLayout(root, params) } }
        card = box
        box.alpha = 0f; box.translationY = -dp(8).toFloat(); box.animate().alpha(1f).translationY(0f).setDuration(160).start()
    }

    override fun onDestroy() { view?.let { runCatching { wm?.removeView(it) } }; view = null; super.onDestroy() }

    companion object {
        private const val CH = "note_bubble"
        private const val ID = 31
        private const val STOP = "com.alzikr.app.NOTE_BUBBLE_STOP"
        /** Starts, refreshes or stops the bubble to match the note chosen for it. */
        fun refresh(c: Context) {
            Store.init(c)
            if (Store.notes().any { it.bubble } && Settings.canDrawOverlays(c)) runCatching { c.startForegroundService(Intent(c, NoteBubble::class.java)) }
            else c.stopService(Intent(c, NoteBubble::class.java))
        }
    }
}

/** The bubble's face: a round slip of the note's paper, with a red push-pin drawn in it (head, shine, needle, shadow). */
class PinBadge(c: Context, paper: androidx.compose.ui.graphics.Color) : View(c) {
    private val d = c.resources.displayMetrics.density
    private val paperArgb = android.graphics.Color.argb(255, (paper.red * 255).toInt(), (paper.green * 255).toInt(), (paper.blue * 255).toInt())
    private val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    init {
        outlineProvider = object : android.view.ViewOutlineProvider() { override fun getOutline(v: View, o: android.graphics.Outline) { o.setOval(0, 0, v.width, v.height) } }
    }
    override fun onDraw(cv: android.graphics.Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val r = minOf(w, h) / 2f
        fill.shader = null; fill.style = android.graphics.Paint.Style.FILL
        fill.color = paperArgb; cv.drawCircle(w / 2, h / 2, r, fill)
        // A navy rim, so it reads on any wallpaper.
        fill.style = android.graphics.Paint.Style.STROKE; fill.strokeWidth = d * 2.2f; fill.color = 0xCC1A3274.toInt(); cv.drawCircle(w / 2, h / 2, r - d * 1.1f, fill)
        fill.style = android.graphics.Paint.Style.FILL
        // Ruled lines, like a slip of paper.
        fill.color = 0x331A3274
        for (k in 0..2) { val y = h * (.62f + k * .09f); cv.drawRoundRect(w * .26f, y, w * (.74f - k * .08f), y + d * 1.4f, d, d, fill) }
        // The push-pin: shadow, needle, a solid red head with a darker rim and one shine.
        val cx = w * .48f; val cy = h * .38f; val hr = r * .30f
        fill.color = 0x40000000; cv.drawOval(cx - hr * .7f + d * 2.5f, cy + hr * .2f + d * 2.5f, cx + hr * 1.3f + d * 2.5f, cy + hr * 1.5f + d * 2.5f, fill)
        fill.style = android.graphics.Paint.Style.STROKE; fill.strokeWidth = d * 2f; fill.strokeCap = android.graphics.Paint.Cap.ROUND; fill.color = 0xFF5F6368.toInt()
        cv.drawLine(cx + hr * .3f, cy + hr * .7f, cx + hr * 1.05f, cy + hr * 2.0f, fill)
        fill.style = android.graphics.Paint.Style.FILL
        fill.shader = android.graphics.RadialGradient(cx - hr * .3f, cy - hr * .35f, hr * 1.3f, intArrayOf(0xFFFF6E66.toInt(), 0xFFE53935.toInt(), 0xFFA51717.toInt()), floatArrayOf(0f, .5f, 1f), android.graphics.Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, hr, fill)
        fill.shader = null; fill.style = android.graphics.Paint.Style.STROKE; fill.strokeWidth = d; fill.color = 0xFF8E1414.toInt(); cv.drawCircle(cx, cy, hr - d / 2, fill)
        fill.style = android.graphics.Paint.Style.FILL; fill.color = 0xE6FFFFFF.toInt(); cv.drawCircle(cx - hr * .38f, cy - hr * .4f, hr * .22f, fill)
    }
}
