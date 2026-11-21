package com.alzikr.app

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

/** The Adhkar tab: my daily awrad, the full collections, and a free tasbih. */
@Composable
fun AdhkarScreen(openCollection: String? = null) {
    var section by rememberSaveable { mutableStateOf(if (openCollection != null) 1 else 0) }
    var reading by rememberSaveable { mutableStateOf(openCollection) }
    reading?.let { id -> Content.collection(id)?.let { CollectionReader(it) { reading = null }; return } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("أورادي", "الأذكار", "السبحة").forEachIndexed { i, t -> Choice(t, section == i) { section = i } }
        }
        Box(Modifier.weight(1f)) {
            when (section) {
                0 -> WirdList()
                1 -> CollectionsList { reading = it }
                else -> TasbihScreen()
            }
        }
    }
}

/** Morning first in the morning, evening first after Asr, sleep first at night. */
private fun suggested(): String? {
    if (!Store.hasLocation) return null
    val t = Store.times(); val now = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    return when {
        now in t.getValue(Prayer.FAJR)..(t.getValue(Prayer.DHUHR) - 30) -> "morning"
        now in t.getValue(Prayer.ASR)..(t.getValue(Prayer.ISHA) + 60) -> "evening"
        now > t.getValue(Prayer.ISHA) + 60 || now < t.getValue(Prayer.FAJR) -> "sleep"
        else -> null
    }
}

@Composable
private fun CollectionsList(open: (String) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val hint = remember { suggested() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(Content.collections.sortedByDescending { it.id == hint }) { _, c ->
            val counts = Data.colCounts(c.id)
            val done = c.items.indices.count { counts[it] >= c.items[it].count }
            val complete = done == c.items.size
            Row(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(if (c.id == hint && !complete) P.accent.copy(alpha = .12f) else P.surface)
                .border(1.dp, P.line, L.shape(16.dp)).clickable { open(c.id) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.title, color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Sub(if (c.id == hint && !complete) "${c.subtitle} · وقتها الآن" else c.subtitle, size = 12.sp, color = if (c.id == hint && !complete) P.accent else P.muted)
                }
                Text(if (complete) "✓ تمّت" else if (done > 0) "$done/${c.items.size}" else "${c.items.size} ذكرًا", color = if (complete) P.gold else P.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        item { Sub("يبدأ العدّ من جديد كل يوم. اضغط على الذكر نفسه لتعدّ.", Modifier.padding(6.dp), size = 12.sp) }
    }
}

@Composable
private fun CollectionReader(c: Collection, back: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val vibrator = remember { ctx.getSystemService(Vibrator::class.java) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val counts = Data.colCounts(c.id)
    val done = c.items.indices.count { counts[it] >= c.items[it].count }
    Subpage(c.title, back) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(P.line)) {
                Box(Modifier.fillMaxWidth(done.toFloat() / c.items.size).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (done == c.items.size) P.gold else P.accent))
            }
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(c.items) { i, item ->
                    val n = counts[i]; val finished = n >= item.count
                    Column(Modifier.fillMaxWidth().clip(L.shape(18.dp)).background(if (finished) P.surface else P.card)
                        .border(1.dp, if (finished) P.line else P.accent.copy(alpha = .35f), L.shape(18.dp))
                        .clickable(enabled = !finished) {
                            val after = Data.colTap(c.id, i)[i]
                            val now = after >= item.count
                            runCatching { vibrator?.vibrate(if (now) VibrationEffect.createWaveform(longArrayOf(0, 50, 70, 50), -1) else VibrationEffect.createOneShot(15, 110)) }
                            if (now) scope.launch { runCatching { list.animateScrollToItem((i + 1).coerceAtMost(c.items.size)) } }
                        }.padding(16.dp)) {
                        if (item.note.isNotEmpty()) Text(item.note, color = P.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                        Text(item.text, color = if (finished) P.muted else P.ink, fontSize = (18 * Data.textScale).sp, lineHeight = (30 * Data.textScale).sp)
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Sub(if (item.count > 1) "${item.count} مرات" else "مرة واحدة", Modifier.weight(1f), size = 12.sp)
                            Box(Modifier.clip(L.shape(10.dp)).background(if (finished) P.gold.copy(alpha = .2f) else P.accent).padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text(if (finished) "✓" else "${n} / ${item.count}", color = if (finished) P.gold else P.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (done == c.items.size) Text("تقبّل الله منك ✓", color = P.gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("البدء من جديد", color = P.muted, fontSize = 13.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable { Data.colReset(c.id); scope.launch { list.scrollToItem(0) } }.padding(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TasbihScreen() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val vibrator = remember { ctx.getSystemService(Vibrator::class.java) }
    val count = Data.tasbih; val set = Data.tasbihSet
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(33, 99, 100, 1000).forEach { n -> Choice("$n", set == n) { Data.tasbihSet = n } } }
        Box(Modifier.size(270.dp).clip(androidx.compose.foundation.shape.CircleShape).background(P.card)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                val n = Data.tasbihTap(); TasbihWidget.update(ctx)
                runCatching { vibrator?.vibrate(if (n % set == 0) VibrationEffect.createWaveform(longArrayOf(0, 70, 90, 70), -1) else VibrationEffect.createOneShot(14, 100)) }
            }, contentAlignment = Alignment.Center) {
            Counter(count % set, set, false, "الدورة ${count / set + 1} · المجموع $count", 250.dp)
        }
        Sub("اضغط على الدائرة للتسبيح", size = 12.sp)
        Text("تصفير", color = P.muted, fontSize = 14.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable { Data.tasbihReset() }.padding(horizontal = 14.dp, vertical = 8.dp))

        Panel(pad = 14.dp) {
            Title("آخر سبعة أيام")
            val h = Data.tasbihHistory(7); val max = (h.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(120.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                h.forEach { (d, n) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize().weight(1f)) {
                        Text(if (n > 0) "$n" else "", color = P.muted, fontSize = 9.sp)
                        Box(Modifier.width(18.dp).height((80f * n / max).coerceAtLeast(2f).dp).clip(RoundedCornerShape(4.dp)).background(if (d == java.time.LocalDate.now()) P.accent else P.accent.copy(alpha = .45f)))
                        Spacer(Modifier.height(4.dp))
                        Text(d.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale("ar"))).take(3), color = P.muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
