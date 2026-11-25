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
            val paper = Md.papers[(n?.paper ?: 0).coerceIn(0, 5)]
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
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        val pin = TextView(this).apply {
            text = "📌"; textSize = 24f; setPadding(dp(8), dp(6), dp(8), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(0xEEFFF4B8.toInt()); setStroke(dp(1), 0x55000000) }
            elevation = dp(4).toFloat()
        }
        root.addView(pin)
        params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = getSharedPreferences("alzikr", MODE_PRIVATE).getInt("bubble_x", dp(8)); y = getSharedPreferences("alzikr", MODE_PRIVATE).getInt("bubble_y", dp(160))
        }
        // Drag to move; a tap (no real move) opens or folds the note.
        var sx = 0f; var sy = 0f; var px = 0; var py = 0; var moved = false
        pin.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = params.x; py = params.y; moved = false }
                MotionEvent.ACTION_MOVE -> { val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (moved || kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(8)) { moved = true; params.x = px + dx.toInt(); params.y = py + dy.toInt(); runCatching { wm?.updateViewLayout(root, params) } } }
                MotionEvent.ACTION_UP -> if (moved) getSharedPreferences("alzikr", MODE_PRIVATE).edit().putInt("bubble_x", params.x).putInt("bubble_y", params.y).apply() else toggle(root, note)
            }
            true
        }
        runCatching { wm?.addView(root, params) }.onSuccess { view = root }
    }

    private fun toggle(root: LinearLayout, note: Store.Note) {
        card?.let { root.removeView(it); card = null; return }
        val fresh = Store.notes().firstOrNull { it.id == note.id } ?: note
        val text = TextView(this).apply {
            setText(Md.spanned(fresh.text, 0xFF2A2A2A.toInt())); textSize = 14f; setTextColor(0xFF2A2A2A.toInt()); setLineSpacing(dp(3).toFloat(), 1f)
            textDirection = View.TEXT_DIRECTION_RTL
        }
        val scroll = ScrollView(this).apply { addView(text); isVerticalScrollBarEnabled = true }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); elevation = dp(6).toFloat()
            background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(Md.papers[fresh.paper.coerceIn(0, 5)].let { android.graphics.Color.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt()) }) }
            addView(scroll, LinearLayout.LayoutParams(dp(240), LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(TextView(this@NoteBubble).apply {
                setText("فتح في ALzikr ›"); setTextColor(0xFF1A3274.toInt()); textSize = 12f; setPadding(0, dp(8), 0, 0)
                setOnClickListener { startActivity(Intent(this@NoteBubble, MainActivity::class.java).putExtra("tab", "notes").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
            })
        }
        // A long note scrolls inside a card of fixed height; a short one takes only what it needs.
        scroll.layoutParams = LinearLayout.LayoutParams(dp(240), if (fresh.text.lines().size > 10 || fresh.text.length > 400) dp(260) else LinearLayout.LayoutParams.WRAP_CONTENT)
        root.addView(box); card = box
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
