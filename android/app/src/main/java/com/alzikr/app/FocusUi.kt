package com.alzikr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FocusScreen() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(500); now = System.currentTimeMillis() } }
    val end = Store.focusEnd
    val running = end > now
    var minutes by rememberSaveable { mutableStateOf(Store.focusMinutes) }
    var title by rememberSaveable { mutableStateOf(Store.focusTitle) }
    val (sessions, total) = Store.focusToday()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Hint("focus", "⏳", "اختر المدة ثم اضغط «ابدأ التركيز». يبقى المؤقت في الإشعارات حتى لو أغلقت التطبيق، ويتوقف عند الأذان.")
        Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
            val totalMs = (if (Store.focusIsBreak) 5 else Store.focusMinutes) * 60_000L
            val frac = if (running) ((end - now).toFloat() / totalMs).coerceIn(0f, 1f) else 1f
            val ring = P.line; val fill = if (Store.focusIsBreak && running) P.gold else P.accent
            Canvas(Modifier.size(240.dp)) {
                val w = 14.dp.toPx(); val s = Size(size.width - w, size.height - w); val o = Offset(w / 2, w / 2)
                drawArc(ring, 0f, 360f, false, o, s, style = Stroke(w))
                drawArc(fill, -90f, 360f * frac, false, o, s, style = Stroke(w, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (running) clockText(end - now) else "$minutes:00", color = P.ink, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                Sub(when { running && Store.focusIsBreak -> "استراحة"; running -> Store.focusTitle.ifBlank { "جلسة تركيز" }; else -> "جاهز" })
            }
        }

        if (!running) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(15, 25, 45, 60).forEach { m -> Choice("$m د", minutes == m) { minutes = m } } }
            // Focus on one of the open tasks: its name goes on the timer and its end offers to tick it.
            val openTasks = Store.tasks().filter { !it.done }
            if (openTasks.isNotEmpty()) Column(Modifier.fillMaxWidth()) {
                Sub("ركّز على مهمة:", size = 12.sp)
                Wrap(Modifier.padding(top = 4.dp), gap = 6.dp) {
                    openTasks.take(8).forEach { t -> Choice((if (Store.focusTask == t.id) "✓ " else "") + t.text.take(30), Store.focusTask == t.id) {
                        if (Store.focusTask == t.id) { Store.focusTask = 0; title = "" } else { Store.focusTask = t.id; title = t.text }
                    } }
                }
            }
            OutlinedTextField(title, { title = it; if (Store.focusTask != 0L && Store.tasks().none { t -> t.id == Store.focusTask && t.text == it }) Store.focusTask = 0 }, placeholder = { Text("على ماذا ستركّز؟ (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            // The adhan comes first? Say so before starting.
            if (Store.hasLocation) {
                val w = window()
                val inMs = w.nextAt - now
                if (w.next.alertable && inMs < minutes * 60_000L) Sub("تنبيه: يحين ${w.next.ar} بعد ${durationText(inMs)}، أثناء الجلسة.", color = P.gold)
            }
            // Quiet phone for the session (Do Not Disturb, alarms only); the earlier mode comes back at the end.
            val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🔕 إسكات الهاتف أثناء الجلسة", color = P.ink, fontSize = 15.sp)
                    Sub("بلا رنين ولا صوت إشعارات ولا اهتزاز حتى تنتهي الجلسة أو توقفها، ثم يعود الهاتف كما كان", size = 12.sp)
                }
                androidx.compose.material3.Switch(Data.focusSilent, {
                    Data.focusSilent = it; Store.bump()
                    if (it && !nm.isNotificationPolicyAccessGranted) runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                })
            }
            if (Data.focusSilent && !nm.isNotificationPolicyAccessGranted)
                Text("اسمح لـ ALzikr بالوصول إلى «عدم الإزعاج» ليُسكت الهاتف ›", color = P.gold, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp))
                    .clickable { runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) } }.padding(6.dp))
            Action("ابدأ التركيز", Modifier.fillMaxWidth()) { Alerts.startFocus(ctx, minutes, false, title.trim()) }
        } else {
            Action("إيقاف", Modifier.fillMaxWidth(), filled = false, color = P.danger) { Alerts.stopFocus(ctx) }
            if (!Store.focusIsBreak && ctx.getSharedPreferences("alzikr_alarm", android.content.Context.MODE_PRIVATE).getBoolean("focus_quiet", false)) Sub("🔕 الهاتف صامت حتى تنتهي الجلسة", color = P.accent, size = 13.sp)
            Sub("يبقى المؤقت في قائمة الإشعارات، ويعمل حتى لو أغلقت التطبيق.", size = 12.sp)
        }

        // The task this session was for: tick it when done.
        val forTask = Store.tasks().firstOrNull { it.id == Store.focusTask && !it.done }
        if (!running && forTask != null && sessions > 0) Panel(pad = 14.dp, color = P.accent.copy(alpha = .10f)) {
            Text("هل أنجزت «${forTask.text}»؟", color = P.ink, fontSize = 15.sp)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("✓ أنجزتها") { Store.toggleTask(forTask.id); Store.focusTask = 0; title = "" }
                Action("ليس بعد", filled = false) { Store.focusTask = 0 }
            }
        }
        Panel(pad = 14.dp) {
            Title("اليوم")
            Sub(if (sessions == 0) "لم تبدأ أي جلسة بعد. ابدأ بخمس وعشرين دقيقة." else "$sessions جلسات · $total دقيقة من التركيز", size = 14.sp)
            Box(Modifier.padding(top = 10.dp)) { WeekBars(Store.focusWeek(), "دقيقة", P.gold) }
        }
    }
}
