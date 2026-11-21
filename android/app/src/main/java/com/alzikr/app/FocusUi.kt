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
import androidx.compose.material3.Text
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
            OutlinedTextField(title, { title = it }, placeholder = { Text("على ماذا ستركّز؟ (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            // The adhan comes first? Say so before starting.
            if (Store.hasLocation) {
                val w = window()
                val inMs = w.nextAt - now
                if (w.next.alertable && inMs < minutes * 60_000L) Sub("تنبيه: يحين ${w.next.ar} بعد ${durationText(inMs)}، أثناء الجلسة.", color = P.gold)
            }
            Action("ابدأ التركيز", Modifier.fillMaxWidth()) { Alerts.startFocus(ctx, minutes, false, title.trim()) }
        } else {
            Action("إيقاف", Modifier.fillMaxWidth(), filled = false, color = P.danger) { Alerts.stopFocus(ctx) }
            Sub("يبقى المؤقت في قائمة الإشعارات، ويعمل حتى لو أغلقت التطبيق.", size = 12.sp)
        }

        Panel(pad = 14.dp) {
            Title("اليوم")
            Sub(if (sessions == 0) "لم تبدأ أي جلسة بعد. ابدأ بخمس وعشرين دقيقة." else "$sessions جلسات · $total دقيقة من التركيز", size = 14.sp)
        }
    }
}
