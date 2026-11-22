package com.alzikr.app

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WirdList() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val list = Store.adhkar()
    var selected by rememberSaveable { mutableStateOf(list.firstOrNull { !it.done }?.id ?: list.firstOrNull()?.id ?: 0L) }
    var editing by remember { mutableStateOf<Store.Zikr?>(null) }
    var adding by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<Store.Zikr?>(null) }
    val current = list.firstOrNull { it.id == selected } ?: list.firstOrNull()
    val vibrator = remember { ctx.getSystemService(Vibrator::class.java) }

    fun tap() {
        val z = current ?: return
        val after = Store.tap(z.id) ?: return
        val reached = after.target > 0 && after.today == after.target
        Haptics.tap(ctx, reached)
        // Wird finished: move on to the next unfinished one.
        if (reached) Store.adhkar().firstOrNull { !it.done && it.target > 0 }?.let { selected = it.id }
    }

    androidx.compose.runtime.DisposableEffect(current?.id) { Nav.volumeTap = { tap() }; onDispose { Nav.volumeTap = null } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Hint("wird", "📿", "اختر ذكرًا من القائمة، ثم اضغط على البطاقة الكبيرة لتعدّ. اضغط مطولًا على ذكر لتعديله. وفي «الأذكار» تجد أذكار الصباح والمساء كاملة.") }
        item {
            val daily = list.filter { it.target > 0 }
            val doneCount = daily.count { it.done }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Title("أورادك اليوم")
                    Sub("أتممت $doneCount من ${daily.size}" + (Store.streak.takeIf { it > 1 }?.let { " · $it أيام متتالية" } ?: ""))
                }
                Text("+ ذكر", color = P.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(L.shape(10.dp)).clickable { adding = true }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        if (current != null) item {
            // The counter: tap anywhere on the panel.
            Panel(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tap() }, color = P.card) {
                Text(current.text, color = P.ink, fontSize = (22 * Data.textScale).sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 34.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Counter(current.today, current.target, current.done,
                        if (current.target > 0) (if (current.done) "تم ✓ من ${current.target}" else "من ${current.target}") else "تسبيح حر", 190.dp)
                }
                Sub("اضغط في أي مكان هنا للعدّ، أو بأزرار الصوت", Modifier.fillMaxWidth(), size = 12.sp)
                if (current.today > 0) Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                    Choice(if (current.done) "↺ أعد قراءته (غير منجز)" else "↺ تصفير", false) { Store.resetToday(current.id) }
                }
            }
        }
        items(list, key = { it.id }) { z ->
            val on = z.id == current?.id
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(if (on) P.accent.copy(alpha = .12f) else P.surface)
                .combinedClickable(onClick = { selected = z.id }, onLongClick = { menu = z }).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(z.text, color = P.ink, fontSize = 15.sp, maxLines = 2, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text(if (z.target > 0) "${z.today}/${z.target}" else "${z.today}", color = if (z.done) P.gold else P.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (z.done) Text(" ✓", color = P.gold, fontSize = 14.sp)
            }
        }
        item { Sub("اضغط مطولًا على أي ذكر لتعديله أو تصفيره أو حذفه. يبدأ العدّ من جديد كل يوم.", size = 12.sp) }
    }

    menu?.let { z ->
        AlertDialog(onDismissRequest = { menu = null }, containerColor = P.surface,
            title = { Text(z.text, color = P.ink, fontSize = 16.sp, maxLines = 3) },
            text = {
                Column {
                    listOf("تعديل" to { editing = z; menu = null }, "تصفير عدّاد اليوم" to { Store.resetToday(z.id); menu = null },
                        "نقل للأعلى" to { Store.moveZikr(z.id, true); menu = null }, "نقل للأسفل" to { Store.moveZikr(z.id, false); menu = null },
                        "حذف" to { Store.deleteZikr(z.id); menu = null }).forEach { (label, act) ->
                        Text(label, color = if (label == "حذف") P.danger else P.ink, fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).clickable { act() }.padding(horizontal = 8.dp, vertical = 12.dp))
                    }
                    Sub("المجموع منذ البداية: ${z.total}", Modifier.padding(top = 6.dp, start = 8.dp), size = 12.sp)
                }
            },
            confirmButton = { TextButton({ menu = null }) { Text("إغلاق") } })
    }
    if (adding || editing != null) ZikrDialog(editing) { adding = false; editing = null }
}

@Composable
private fun ZikrDialog(z: Store.Zikr?, close: () -> Unit) {
    var text by remember { mutableStateOf(z?.text ?: "") }
    var target by remember { mutableStateOf(z?.target?.toString() ?: "33") }
    AlertDialog(onDismissRequest = close, containerColor = P.surface,
        title = { Text(if (z == null) "ذكر أو ورد جديد" else "تعديل الذكر", color = P.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text("نص الذكر") }, minLines = 2)
                OutlinedTextField(target, { target = it.filter(Char::isDigit).take(5) }, label = { Text("العدد اليومي (0 = تسبيح حر)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(0, 3, 7, 10, 33, 100).forEach { n -> Choice(if (n == 0) "حر" else "$n", target == "$n") { target = "$n" } } }
            }
        },
        confirmButton = { TextButton({ if (text.isNotBlank()) { Store.upsertZikr(z?.id, text.trim(), target.toIntOrNull() ?: 0); close() } }) { Text("حفظ") } },
        dismissButton = { TextButton(close) { Text("إلغاء") } })
}
