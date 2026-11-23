@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.alzikr.app

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.layout.heightIn

import androidx.compose.animation.togetherWith

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
    var readAt by rememberSaveable { mutableStateOf(-1) }
    Nav.adhkar?.let { section = it; reading = null; Nav.adhkar = null }
    Nav.collection?.let { section = 1; reading = it; readAt = -1; Nav.collection = null }
    reading?.let { id -> Content.collection(id)?.let { CollectionReader(it, readAt) { reading = null; readAt = -1 }; return } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "أورادي", 1 to "الأذكار", 3 to "أدعيتي", 2 to "السبحة").forEach { (i, t) -> Choice(t, section == i) { section = i } }
        }
        Box(Modifier.weight(1f)) {
            when (section) {
                0 -> WirdList()
                1 -> CollectionsList { id, at -> reading = id; readAt = at }
                3 -> DuasPage()
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
private fun CollectionsList(openAt: (String, Int) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val hint = remember { suggested() }
    val open: (String) -> Unit = { openAt(it, -1) }
    var q by rememberSaveable { mutableStateOf("") }
    // Letters only, without marks, so «سبحان» finds «سُبْحَانَ».
    fun plain(t: String) = t.filter { it.isLetter() || it == ' ' }.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ة', 'ه').replace('ى', 'ي')
    val hits = remember(q) {
        val words = plain(q).split(' ').filter { it.length > 1 }
        if (words.isEmpty()) emptyList() else Content.collections.flatMap { c -> c.items.mapIndexedNotNull { i, it -> if (plain(it.text).let { t -> words.all { w -> t.contains(w) } }) Triple(c, i, it) else null } }.take(60)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            androidx.compose.material3.OutlinedTextField(q, { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث في كل الأذكار… (مثلًا: سبحان، الكرب، السفر)") }, shape = L.shape(14.dp))
        }
        if (q.isNotBlank()) {
            item { Sub(if (hits.isEmpty()) "لا نتائج" else "${hits.size} نتيجة", size = 12.sp) }
            itemsIndexed(hits) { _, (c, i, it) ->
                Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, P.line, L.shape(14.dp)).clickable { openAt(c.id, i) }.padding(12.dp)) {
                    Sub(c.title, color = P.gold, size = 12.sp)
                    Text(it.text, color = P.ink, fontSize = 15.sp, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            return@LazyColumn
        }
        item { Hint("collections", "📖", "اختر مجموعة، ثم اضغط على كل ذكر لتعدّه، فينقلك إلى الذكر التالي حتى تُتمّها. مجموعة الوقت الحالي تظهر أولًا.") }
        itemsIndexed(Content.collections.flatMap { if (it.id == "prayer") Content.afterPrayer else listOf(it) }.sortedByDescending { it.id == hint }) { _, c ->
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

/**
 * One dhikr at a time, in the middle of the screen: the one before it (✓ done) small above, the next one faint below.
 * Tap the dhikr (or the big button) to count; when its number is reached it moves on by itself.
 */
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
private fun CollectionReader(c: Collection, startAt: Int = -1, back: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val counts = Data.colCounts(c.id)
    fun finished(i: Int) = counts[i] >= c.items[i].count
    val done = c.items.indices.count { finished(it) }
    var cur by rememberSaveable(c.id) { mutableStateOf(startAt.takeIf { it in c.items.indices } ?: c.items.indices.firstOrNull { counts[it] < c.items[it].count } ?: c.items.size) }
    var whole by rememberSaveable { mutableStateOf(false) }
    fun nextOpen(from: Int) = (from + 1 until c.items.size).firstOrNull { Data.colCounts(c.id)[it] < c.items[it].count }
        ?: (0 until c.items.size).firstOrNull { Data.colCounts(c.id)[it] < c.items[it].count } ?: c.items.size
    fun tap() {
        if (cur !in c.items.indices || Data.colCounts(c.id)[cur] >= c.items[cur].count) return
        val after = Data.colTap(c.id, cur)[cur]
        val now = after >= c.items[cur].count
        Haptics.tap(ctx, now)
        if (now) { MomentWidget.update(ctx); if (Data.colDone(c.id)) Pet.cheer("أتممت ${c.title}، أحسنت!"); val at = cur; scope.launch { kotlinx.coroutines.delay(450); if (cur == at) cur = nextOpen(at) } }
    }
    // The volume keys count too, and the screen stays on while reading.
    androidx.compose.runtime.DisposableEffect(c.id) {
        Nav.volumeTap = { tap() }
        val w = (ctx as? android.app.Activity)?.window
        w?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { Nav.volumeTap = null; w?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val swipe = with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.toPx() }
    Subpage(c.title, back) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Where you are.
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (cur < c.items.size) "الذكر ${cur + 1} من ${c.items.size}" else "أتممتها", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Sub("أُنجز $done", size = 12.sp)
                Choice(if (whole) "ذكر واحد" else "القائمة كاملة", whole, Modifier.padding(start = 8.dp)) { whole = !whole }
            }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(P.line)) {
                Box(Modifier.fillMaxWidth(done.toFloat() / c.items.size).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (done == c.items.size) P.gold else P.accent))
            }
            if (whole) { Box(Modifier.weight(1f)) { CollectionList(c, cur) { cur = it } }; return@Column }
            Hint("dhikr_focus", "👆", "اسحب على الذكر للأعلى أو نحو اليمين للتالي، وللأسفل أو نحو اليسار للسابق. تعدّ بالضغط عليه أو بأزرار الصوت. ضغطة مطوّلة تنسخه. و«أعد قراءته» يجعله غير منجز.", Modifier.padding(top = 8.dp))

            // The one before (done ✓), small.
            val prev = (cur - 1).takeIf { it >= 0 }
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.BottomCenter) {
                if (prev != null) Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).clickable { cur = prev }.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (finished(prev)) "✓" else "•", color = if (finished(prev)) P.gold else P.muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(c.items[prev].text, color = P.muted, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
                }
            }

            // The current one, in the middle.
            Box(Modifier.weight(1f).fillMaxWidth().pointerInput(c.id) {
                var dx = 0f; var dy = 0f
                detectDragGestures(onDragStart = { dx = 0f; dy = 0f }, onDragEnd = {
                    val next = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) (if (dx > swipe) 1 else if (dx < -swipe) -1 else 0) else (if (dy < -swipe) 1 else if (dy > swipe) -1 else 0)
                    if (next == 1 && cur < c.items.size) cur += 1
                    if (next == -1 && cur > 0) cur -= 1
                }) { change, d -> change.consume(); dx += d.x; dy += d.y }
            }, contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedContent(cur, transitionSpec = {
                    (androidx.compose.animation.slideInVertically { it / 3 } + androidx.compose.animation.fadeIn()) togetherWith
                        (androidx.compose.animation.slideOutVertically { -it / 3 } + androidx.compose.animation.fadeOut())
                }, label = "dhikr") { i ->
                    if (i >= c.items.size) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✓", color = P.gold, fontSize = 64.sp)
                        Text("أتممت ${c.title}، تقبّل الله منك", color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("البدء من جديد", color = P.accent, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp).clip(L.shape(10.dp)).clickable { Data.colReset(c.id); cur = 0 }.padding(10.dp))
                    } else {
                        val item = c.items[i]; val n = Data.colCounts(c.id)[i]; val fin = n >= item.count
                        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).clip(L.shape(22.dp)).background(if (fin) P.surface else P.card)
                            .border(2.dp, if (fin) P.gold.copy(alpha = .6f) else P.accent.copy(alpha = .7f), L.shape(22.dp))
                            .combinedClickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null,
                                onLongClick = { copy(ctx, item.text) }) { tap() }
                            .then(if (item.text.length > 380) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(horizontal = 20.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (item.note.isNotEmpty()) Text(item.note, color = P.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 10.dp))
                            Text(item.text, color = if (fin) P.muted else P.ink, fontSize = (21 * Data.textScale).sp, lineHeight = (36 * Data.textScale).sp,
                                fontFamily = Fonts.naskh, textAlign = TextAlign.Center)
                            Sub(if (item.count > 1) "يُقال ${item.count} مرات" else "يُقال مرة واحدة", Modifier.padding(top = 12.dp), size = 12.sp)
                            // Done by mistake, or to read it again.
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (fin) Choice("↺ أعد قراءته (غير منجز)", false) { Data.colSet(c.id, i, 0) }
                                else if (n > 0) Choice("− تراجع عن عدّة", false) { Data.colSet(c.id, i, n - 1) }
                            }
                        }
                    }
                }
            }

            // The next one, faint.
            val next = (cur + 1).takeIf { it < c.items.size }
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                if (next != null) Text("التالي: " + c.items[next].text, color = P.muted.copy(alpha = .7f), fontSize = 13.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.clip(L.shape(10.dp)).clickable { cur = next }.padding(horizontal = 10.dp, vertical = 6.dp))
            }

            // Count, and move by hand.
            if (cur < c.items.size) {
                val item = c.items[cur]; val n = counts[cur]; val fin = n >= item.count
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(52.dp).clip(L.shape(14.dp)).background(P.surface).border(1.dp, P.line, L.shape(14.dp)).clickable(enabled = cur > 0) { cur -= 1 }, contentAlignment = Alignment.Center) {
                        Text("›", color = if (cur > 0) P.ink else P.line, fontSize = 26.sp)
                    }
                    Box(Modifier.weight(1f).height(60.dp).clip(L.shape(18.dp)).background(if (fin) P.gold.copy(alpha = .25f) else P.accent).clickable(enabled = !fin) { tap() }, contentAlignment = Alignment.Center) {
                        Text(if (fin) "✓ تمّ" else if (item.count > 1) "${n} / ${item.count}" else "اضغط بعد قراءته", color = if (fin) P.gold else P.onAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.size(52.dp).clip(L.shape(14.dp)).background(P.surface).border(1.dp, P.line, L.shape(14.dp)).clickable(enabled = cur < c.items.size - 1) { cur += 1 }, contentAlignment = Alignment.Center) {
                        Text("‹", color = if (cur < c.items.size - 1) P.ink else P.line, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionList(c: Collection, focus: Int, pick: (Int) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val vibrator = remember { ctx.getSystemService(Vibrator::class.java) }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = focus.coerceAtLeast(0))
    val scope = rememberCoroutineScope()
    val counts = Data.colCounts(c.id)
    val done = c.items.indices.count { counts[it] >= c.items[it].count }
    run {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(P.line)) {
                Box(Modifier.fillMaxWidth(done.toFloat() / c.items.size).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (done == c.items.size) P.gold else P.accent))
            }
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(c.items) { i, item ->
                    val n = counts[i]; val finished = n >= item.count
                    Column(Modifier.fillMaxWidth().clip(L.shape(18.dp)).background(if (finished) P.surface else if (i == focus) P.accent.copy(alpha = .12f) else P.card)
                        .border(if (i == focus) 2.dp else 1.dp, if (finished) P.line else if (i == focus) P.accent else P.line, L.shape(18.dp))
                        .combinedClickable(onLongClick = { if (finished) Data.colSet(c.id, i, 0) }) {
                            if (finished) return@combinedClickable
                            pick(i)
                            val after = Data.colTap(c.id, i)[i]
                            val now = after >= item.count
                            Haptics.tap(ctx, now)
                            if (now) scope.launch { runCatching { list.animateScrollToItem((i + 1).coerceAtMost(c.items.size)) } }
                        }.padding(16.dp)) {
                        if (item.note.isNotEmpty()) Text(item.note, color = P.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                        Text(item.text, color = if (finished) P.muted else P.ink, fontSize = (18 * Data.textScale).sp, lineHeight = (30 * Data.textScale).sp)
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Sub(if (item.count > 1) "${item.count} مرات" else "مرة واحدة", Modifier.weight(1f), size = 12.sp)
                            Box(Modifier.clip(L.shape(10.dp)).background(if (finished) P.gold.copy(alpha = .2f) else P.accent).padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text(if (finished) "✓" else "${n} / ${item.count}", color = if (finished) P.gold else P.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            if (finished) Text("↺", color = P.accent, fontSize = 18.sp, modifier = Modifier.padding(start = 6.dp).clip(L.shape(8.dp)).clickable { Data.colSet(c.id, i, 0) }.padding(horizontal = 8.dp, vertical = 2.dp))
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
        Hint("tasbih", "🟢", "اضغط على الدائرة لتسبّح، واختر عدد الدورة من الأعلى. يمكنك أيضًا وضع ويدجت «السبحة» على الشاشة الرئيسية.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(33, 99, 100, 1000).forEach { n -> Choice("$n", set == n) { Data.tasbihSet = n } } }
        Box(Modifier.size(270.dp).clip(androidx.compose.foundation.shape.CircleShape).background(P.card)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                val n = Data.tasbihTap(); TasbihWidget.update(ctx)
                Haptics.tap(ctx, n % set == 0)
            }, contentAlignment = Alignment.Center) {
            Counter(count % set, set, false, "الدورة ${count / set + 1} · المجموع $count", 250.dp)
        }
        Sub("اضغط على الدائرة للتسبيح", size = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("🌙", "سبحة الشاشة المظلمة", Modifier.weight(1f)) { TasbihDarkActivity.open(ctx) }
            Tile("🔔", if (CounterNotice.on) "إزالتها من الإشعارات" else "في الإشعارات وعلى القفل", Modifier.weight(1f), on = CounterNotice.on) { CounterNotice.on = !CounterNotice.on; CounterNotice.update(ctx) }
        }
        Sub("«الشاشة المظلمة»: شاشة سوداء خافتة تظهر فوق قفل الهاتف، تسبّح بلمسها أو بأزرار الصوت. «في الإشعارات»: إشعار مستقل للسبحة بعدّادها وزر «سبّح +١» يعمل دون فتح القفل، منفصل عن إشعار الصلاة القادمة.", size = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GText("📳", fontSize = 16.sp)
            Haptics.names.forEachIndexed { i, n -> Choice(n, Haptics.level == i) { Haptics.level = i; Haptics.tap(ctx) } }
        }
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
