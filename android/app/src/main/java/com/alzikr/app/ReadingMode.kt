package com.alzikr.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Reading-mode preferences shared by the mushaf and every book, plus the daily wird and the khatma. */
object Reading {
    enum class Paper(val label: String, val bg: Long, val ink: Long, val accent: Long, val muted: Long) {
        WHITE("أبيض", 0xFFFFFDFA, 0xFF1B1B1B, 0xFF1F7A5A, 0xFF777777),
        CREAM("كريمي", 0xFFF5ECD5, 0xFF3A2E1C, 0xFF8A5A1E, 0xFF8C7A5C),
        GREEN("أخضر هادئ", 0xFFE6F0E8, 0xFF1D2F25, 0xFF2F6B4C, 0xFF6F8577),
        DUSK("رمادي داكن", 0xFF26292D, 0xFFE2DED4, 0xFFC9A85C, 0xFF8E8A82),
        NIGHT("ليلي", 0xFF000000, 0xFFBDB6A6, 0xFFB08A45, 0xFF6E6A62);

        /** The same paper over a PDF page: tinted, or inverted for the dark papers. */
        fun pdfFilter(): ColorFilter? = when (this) {
            WHITE -> null
            CREAM -> scale(.96f, .91f, .78f)
            GREEN -> scale(.90f, .96f, .91f)
            DUSK -> invert(.82f, 38f)
            NIGHT -> invert(.72f, 0f)
        }
        private fun scale(r: Float, g: Float, b: Float) = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(r, 0f, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
        private fun invert(k: Float, base: Float) = (255f * k + base).let { t ->
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(-k, 0f, 0f, 0f, t, 0f, -k, 0f, 0f, t, 0f, 0f, -k, 0f, t, 0f, 0f, 0f, 1f, 0f)))
        }
    }

    private val p get() = Store.prefs
    var paper: Paper
        get() = runCatching { Paper.valueOf(p.getString("read_paper", null) ?: "") }.getOrDefault(Paper.CREAM)
        set(v) { p.edit().putString("read_paper", v.name).apply(); Store.bump() }
    var quranSize: Int
        get() = p.getInt("read_qsize", 26)
        set(v) { p.edit().putInt("read_qsize", v.coerceIn(18, 48)).apply(); Store.bump() }
    var textSize: Int
        get() = p.getInt("read_tsize", 19)
        set(v) { p.edit().putInt("read_tsize", v.coerceIn(14, 34)).apply(); Store.bump() }
    var line: Float
        get() = p.getFloat("read_line", 1f)
        set(v) { p.edit().putFloat("read_line", v.coerceIn(.8f, 1.5f)).apply(); Store.bump() }
    /** Screen light while reading: -1 = the phone's own. */
    var bright: Float
        get() = p.getFloat("read_bright", -1f)
        set(v) { p.edit().putFloat("read_bright", v).apply(); Store.bump() }
    var speed: Int
        get() = p.getInt("read_speed", 3)
        set(v) { p.edit().putInt("read_speed", v.coerceIn(1, 8)).apply(); Store.bump() }
    /** The mushaf as pages turned sideways (true) or one continuous scroll. */
    var pages: Boolean
        get() = p.getBoolean("read_pager", false)
        set(v) { p.edit().putBoolean("read_pager", v).apply(); Store.bump() }
    var goal: Int
        get() = p.getInt("read_goal", 5)
        set(v) { p.edit().putInt("read_goal", v).apply(); Store.bump() }

    /** Where a book was left: item index and offset. */
    fun pos(key: String): Pair<Int, Int> = p.getString("read_pos_$key", null)?.split(':')?.let { (it[0].toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0) } ?: (0 to 0)
    fun setPos(key: String, i: Int, off: Int) = p.edit().putString("read_pos_$key", "$i:$off").apply()

    // ---- the daily wird (pages read today) and the khatma (how far, in order)
    private fun today() = LocalDate.now().toString()
    fun todayPages(): Int = if (p.getString("wird_day", "") == today()) p.getStringSet("wird_pages", emptySet())?.size ?: 0 else 0
    var far: Int
        get() = p.getInt("khatma_far", 0)
        set(v) { p.edit().putInt("khatma_far", v.coerceIn(0, 604)).apply(); Store.bump() }
    val khatmas get() = p.getInt("khatmas", 0)

    /** A page stayed open long enough to be read. */
    fun visit(c: Context, page: Int) {
        val d = today()
        val set = (if (p.getString("wird_day", "") == d) p.getStringSet("wird_pages", emptySet()) ?: emptySet() else emptySet()).toMutableSet()
        val before = set.size
        set += page.toString()
        val e = p.edit().putString("wird_day", d).putStringSet("wird_pages", set)
        var msg: String? = null
        if (before < goal && set.size >= goal) msg = "أتممت وردك اليوم، تقبّل الله منك"
        val f = far
        if (f == 0 || page in (f + 1)..(f + 3)) {
            if (page >= 604) { e.putInt("khatma_far", 0).putInt("khatmas", khatmas + 1); msg = "أتممت ختمة القرآن الكريم، تقبّل الله منك" }
            else e.putInt("khatma_far", maxOf(page, if (f == 0) 0 else f))
        }
        e.apply(); Store.bump()
        msg?.let { Toast.makeText(c, it, Toast.LENGTH_LONG).show() }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * The reading frame: the whole screen in the paper's color, the screen kept on, the bars hidden.
 * One tap shows or hides the tools; «Aa» holds paper, font, spacing, light and auto-scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingShell(
    title: String, info: String, back: () -> Unit,
    progress: Float? = null, status: String? = null, scroll: ScrollableState? = null,
    textual: Boolean = true, quran: Boolean = false,
    tiles: @Composable RowScope.() -> Unit = {},
    settings: @Composable ColumnScope.() -> Unit = {},
    content: @Composable (toggle: () -> Unit) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val paper = Reading.paper
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window }
    var controls by remember { mutableStateOf(true) }
    var auto by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    BackHandler(onBack = back)
    // The tools hide by themselves (longer the very first time, when the explanation shows; it then never returns).
    LaunchedEffect(controls, sheet) {
        if (controls && !sheet) {
            val first = !Hints.seen("readmode")
            delay(if (first) 8_000 else 4_000)
            controls = false
            if (first) Hints.dismiss("readmode")
        }
    }
    var pillDim by remember { mutableStateOf(false) }
    LaunchedEffect(auto, controls) { pillDim = false; if (auto && !controls) { delay(3_000); pillDim = true } }
    DisposableEffect(window) {
        val ctl = window?.let { WindowCompat.getInsetsController(it, view) }
        ctl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctl?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            ctl?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
            window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
        }
    }
    val bright = Reading.bright
    LaunchedEffect(bright, window) {
        window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = if (bright < 0f) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else bright.coerceIn(.01f, 1f) } }
    }
    val density = LocalDensity.current.density
    val speed = Reading.speed
    LaunchedEffect(auto, speed, scroll) {
        if (!auto || scroll == null) return@LaunchedEffect
        val px = speed * .22f * density
        while (isActive) {
            withFrameNanos { }
            runCatching { scroll.scrollBy(px) }
            if (!scroll.canScrollForward) auto = false
        }
    }
    Box(Modifier.fillMaxSize().background(Color(paper.bg))) {
        content { controls = !controls }
        AnimatedVisibility(controls, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxWidth().background(P.surface).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("→", color = P.accent, fontSize = 22.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable(onClick = back).padding(horizontal = 12.dp, vertical = 6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (info.isNotBlank()) Text(info, color = P.muted, fontSize = 12.sp, maxLines = 1)
                    }
                }
                if (status != null) Text(status, color = P.gold, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
                if (progress != null) Box(Modifier.fillMaxWidth().height(3.dp).background(P.line)) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(P.accent))
                }
            }
        }
        AnimatedVisibility(controls, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxWidth().background(P.surface).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)).padding(10.dp)) {
                Hint("readmode", "📜", "اضغط مرة في الصفحة لإخفاء الأدوات أو إظهارها. اضغط مطوّلًا على " + (if (quran) "آية لخياراتها" else "فقرة لنسخها") + ". «Aa» لتغيير لون الورق والخط والإضاءة. الشاشة لا تنطفئ أثناء القراءة.", Modifier.padding(bottom = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (scroll != null) Tile(if (auto) "⏸" else "⏬", if (auto) "إيقاف التمرير" else "تمرير تلقائي", Modifier.weight(1f), on = auto) { auto = !auto; if (auto) controls = false }
                    tiles()
                    Tile("Aa", "المظهر", Modifier.weight(1f)) { sheet = true }
                }
            }
        }
        // While scrolling by itself with the tools hidden: a small quiet control.
        if (auto && !controls) Row(Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)).padding(bottom = 14.dp)
            .alpha(if (pillDim) .35f else 1f).clip(RoundedCornerShape(50)).background(Color(paper.ink).copy(alpha = .10f)).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            val ink = Color(paper.muted)
            Text("−", color = ink, fontSize = 20.sp, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { Reading.speed = speed - 1 }.padding(horizontal = 14.dp, vertical = 6.dp))
            Text("السرعة ${Quran.digits(speed)}", color = ink, fontSize = 13.sp)
            Text("+", color = ink, fontSize = 20.sp, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { Reading.speed = speed + 1 }.padding(horizontal = 14.dp, vertical = 6.dp))
            Text("⏸", color = ink, fontSize = 16.sp, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { auto = false }.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = P.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Title("المظهر وراحة القراءة")
            Sub("لون الورق")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Reading.Paper.entries.forEach { pp ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(L.shape(12.dp)).clickable { Reading.paper = pp }.padding(4.dp)) {
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(Color(pp.bg))
                            .border(if (pp == paper) 3.dp else 1.dp, if (pp == paper) P.accent else P.line, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Text("بِسۡمِ", color = Color(pp.ink), fontSize = 15.sp, fontFamily = Fonts.quran)
                        }
                        Text(pp.label, color = if (pp == paper) P.accent else P.ink, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (textual) {
                val size = if (quran) Reading.quranSize else Reading.textSize
                Sub("حجم الخط: ${Quran.digits(size)}", Modifier.padding(top = 8.dp))
                Box(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(Color(paper.bg)).padding(12.dp)) {
                    Text(if (quran) Quran.BASMALA else "الحمد لله الذي بنعمته تتم الصالحات", color = Color(paper.ink), fontSize = size.sp,
                        lineHeight = (size * (if (quran) 1.95f else 1.75f) * Reading.line).sp, fontFamily = if (quran) Fonts.quran else Fonts.naskh, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                if (quran) Slider(size.toFloat(), { Reading.quranSize = it.toInt() }, valueRange = 18f..48f)
                else Slider(size.toFloat(), { Reading.textSize = it.toInt() }, valueRange = 14f..34f)
                Sub("تباعد الأسطر")
                Slider(Reading.line, { Reading.line = it }, valueRange = .8f..1.5f)
            }
            Sub("إضاءة الشاشة أثناء القراءة")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Choice("إضاءة الهاتف", bright < 0f) { Reading.bright = -1f }
                Slider(if (bright < 0f) .5f else bright, { Reading.bright = it.coerceAtLeast(.01f) }, valueRange = .01f..1f, modifier = Modifier.weight(1f).padding(start = 10.dp))
            }
            if (scroll != null) {
                Sub("سرعة التمرير التلقائي")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..8).forEach { n -> Choice(Quran.digits(n), speed == n) { Reading.speed = n } }
                }
            }
            settings()
        }
    }
}

// =============================================================================== the mushaf

private fun pageStarts(): IntArray {
    val r = IntArray(605)
    var p = 1
    Quran.ayahs.forEachIndexed { i, a -> while (p < a.page && p <= 604) { r[p] = i; p++ } }
    while (p <= 604) { r[p] = Quran.ayahs.size; p++ }
    return r
}

/** The mushaf in reading mode: page by page, mushaf-like, continuing across surahs. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranReading(s0: Int, a0: Int, back: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    if (!Quran.ready) { Box(Modifier.fillMaxSize().background(Color(Reading.paper.bg))); return }
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val scope = rememberCoroutineScope()
    val starts = remember { pageStarts() }
    val startPage = remember { Quran.at(s0, a0)?.page ?: 1 }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = startPage - 1)
    val pager = rememberPagerState(initialPage = startPage - 1) { 604 }
    val paged = Reading.pages
    var cur by remember { mutableIntStateOf(startPage) }
    var chosen by remember { mutableStateOf<Quran.Ayah?>(null) }
    var index by remember { mutableStateOf(false) }
    fun firstOf(p: Int) = Quran.ayahs[starts[p - 1].coerceAtMost(Quran.ayahs.size - 1)]
    fun go(p: Int) = scope.launch { val i = (p - 1).coerceIn(0, 603); if (Reading.pages) pager.scrollToPage(i) else list.scrollToItem(i) }

    LaunchedEffect(paged) {
        if (paged) { pager.scrollToPage(cur - 1); snapshotFlow { pager.currentPage }.collect { cur = it + 1 } }
        else { list.scrollToItem(cur - 1); snapshotFlow { list.firstVisibleItemIndex }.collect { cur = it + 1 } }
    }
    LaunchedEffect(cur) {
        if (cur != startPage) firstOf(cur).let { Quran.last = it.s to it.a } else Quran.last = s0 to a0
        delay(10_000)
        Reading.visit(ctx, cur)
    }
    LaunchedEffect(Recitation.now) {
        val (rs, ra) = Recitation.now ?: return@LaunchedEffect
        val pg = Quran.at(rs, ra)?.page ?: return@LaunchedEffect
        if (pg != cur) runCatching { if (paged) pager.animateScrollToPage(pg - 1) else list.animateScrollToItem(pg - 1) }
    }

    val top = firstOf(cur)
    val mark = Quran.marked(top.s, top.a)
    ReadingShell(
        title = "سورة ${Quran.surahs[top.s - 1].name}",
        info = "الجزء ${Quran.digits(top.juz)} · صفحة ${Quran.digits(cur)} من ٦٠٤",
        back = back, progress = cur / 604f,
        status = "ورد اليوم: ${Quran.digits(Reading.todayPages())} من ${Quran.digits(Reading.goal)} صفحات · الختمة ${Quran.digits(Reading.far * 100 / 604)}٪",
        scroll = if (paged) null else list, quran = true,
        tiles = {
            if (Recitation.now == null) Tile("▶", "استماع", Modifier.weight(1f)) { Recitation.play(ctx, top.s, top.a) }
            else Tile("⏹", "إيقاف", Modifier.weight(1f), on = true) { Recitation.stop() }
            Tile("📑", "الفهرس", Modifier.weight(1f)) { index = true }
            Tile("🔖", if (mark) "مُعلَّمة" else "علامة", Modifier.weight(1f), on = mark) { Quran.toggleMark(top.s, top.a) }
        },
        settings = {
            Sub("طريقة العرض", Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("تمرير متصل", !paged) { Reading.pages = false }
                Choice("صفحات كالمصحف", paged) { Reading.pages = true }
            }
            Sub("الورد اليومي", Modifier.padding(top = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2, 4, 5, 10, 20).forEach { n -> Choice(if (n == 20) "جزء (٢٠ صفحة)" else "${Quran.digits(n)} صفحات", Reading.goal == n) { Reading.goal = n } }
            }
            val left = 604 - Reading.far
            Sub("الختمة: وصلت الصفحة ${Quran.digits(Reading.far)}" + (if (Reading.khatmas > 0) " · ختمات مكتملة: ${Quran.digits(Reading.khatmas)}" else "") +
                " · بوردك الحالي تتمها في نحو ${Quran.digits((left + Reading.goal - 1) / Reading.goal)} يومًا", size = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("موضع ختمتي هنا", false) { Reading.far = cur }
                Choice("ختمة جديدة", false) { Reading.far = 0 }
            }
            Sub("تُحسب الصفحة مقروءة إذا بقيت أمامك عشر ثوانٍ.", size = 11.sp)
        },
    ) { toggle ->
        val long: (Quran.Ayah) -> Unit = { chosen = it }
        if (paged) HorizontalPager(pager, Modifier.fillMaxSize(), key = { it }) { i ->
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 34.dp, bottom = 40.dp)) { QuranPage(i + 1, starts, toggle, long) }
        }
        else LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(top = 34.dp, bottom = 80.dp)) {
            items(604, key = { it }) { i ->
                QuranPage(i + 1, starts, toggle, long)
                if (i < 603) Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 6.dp).height(1.dp).background(Color(Reading.paper.muted).copy(alpha = .3f)))
            }
        }
    }
    chosen?.let { a -> AyahSheet(a) { chosen = null } }
    if (index) {
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { index = false }, containerColor = P.surface, title = { Text("الفهرس", color = P.ink) },
            text = {
                Column {
                    OutlinedTextField(text, { text = it.filter(Char::isDigit).take(3) }, singleLine = true, placeholder = { Text("رقم الصفحة ١ – ٦٠٤") }, modifier = Modifier.fillMaxWidth())
                    Sub("الأجزاء", Modifier.padding(top = 8.dp, bottom = 4.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..30).forEach { j -> Choice(Quran.digits(j), top.juz == j) { go(Quran.firstOfJuz(j).page); index = false } }
                    }
                    Sub("السور", Modifier.padding(top = 8.dp, bottom = 4.dp))
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(114) { i ->
                            val su = Quran.surahs[i]
                            Row(Modifier.fillMaxWidth().clip(L.shape(10.dp)).background(if (su.n == top.s) P.card else P.surface).clickable { Quran.at(su.n, 1)?.let { go(it.page) }; index = false }
                                .padding(horizontal = 10.dp, vertical = 9.dp)) {
                                Text("${Quran.digits(su.n)}. سورة ${su.name}", color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Sub("ص ${Quran.digits(Quran.at(su.n, 1)?.page ?: 0)}", size = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton({ text.toIntOrNull()?.let { go(it) }; index = false }) { Text("انتقل") } },
            dismissButton = { TextButton({ index = false }) { Text("إغلاق") } })
    }
}

/** One mushaf page: its surah and juz on top, surah banners, the verses as one flowing text, the page number. */
@Composable
private fun QuranPage(p: Int, starts: IntArray, toggle: () -> Unit, long: (Quran.Ayah) -> Unit) {
    val paper = Reading.paper
    val muted = Color(paper.muted)
    val ayahs = Quran.ayahs.subList(starts[p - 1], starts[p].coerceAtLeast(starts[p - 1]))
    if (ayahs.isEmpty()) return
    val runs = remember(p) {
        val out = ArrayList<List<Quran.Ayah>>(); var curRun = ArrayList<Quran.Ayah>()
        ayahs.forEach { a -> if (a.a == 1 && curRun.isNotEmpty()) { out += curRun; curRun = ArrayList() }; curRun += a }
        if (curRun.isNotEmpty()) out += curRun
        out
    }
    Column(Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onTap = { toggle() }) }.padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("سورة ${Quran.surahs[ayahs.first().s - 1].name}", color = muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("الجزء ${Quran.digits(ayahs.first().juz)}", color = muted, fontSize = 12.sp)
        }
        runs.forEach { run ->
            if (run.first().a == 1) SurahBanner(run.first().s)
            AyahRun(run, toggle, long)
        }
        Text(Quran.digits(p), color = muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
    }
}

@Composable
private fun SurahBanner(s: Int) {
    val paper = Reading.paper
    val acc = Color(paper.accent)
    val info = Quran.surahs[s - 1]
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp).clip(RoundedCornerShape(14.dp)).background(acc.copy(alpha = .08f))
        .border(1.5.dp, acc.copy(alpha = .6f), RoundedCornerShape(14.dp)).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("سورة ${info.name}", color = Color(paper.ink), fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = Fonts.naskh)
        Text("${if (info.meccan) "مكية" else "مدنية"} · آياتها ${Quran.digits(info.count)}", color = Color(paper.muted), fontSize = 11.sp)
    }
    if (s != 1 && s != 9) Text(Quran.BASMALA, color = Color(paper.ink), fontSize = Reading.quranSize.sp, fontFamily = Fonts.quran,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
}

/** The verses of one surah on a page as one justified text; a long press on a verse opens its options. */
@Composable
private fun AyahRun(run: List<Quran.Ayah>, toggle: () -> Unit, long: (Quran.Ayah) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val paper = Reading.paper
    val acc = Color(paper.accent)
    val now = Recitation.now
    val haptic = LocalHapticFeedback.current
    val ranges = ArrayList<Pair<IntRange, Quran.Ayah>>()
    val text = buildAnnotatedString {
        run.forEach { a ->
            val st = length
            val hi = when { now == (a.s to a.a) -> acc.copy(alpha = .22f); Quran.marked(a.s, a.a) -> Color(0xFFC9A85C).copy(alpha = .25f); else -> null }
            if (hi != null) pushStyle(SpanStyle(background = hi))
            append(a.text); if (a.sajda) append(" ۩")
            if (hi != null) pop()
            withStyle(SpanStyle(color = acc)) { append(" ﴿${Quran.digits(a.a)}﴾ ") }
            ranges += (st until length) to a
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val size = Reading.quranSize
    Text(text, color = Color(paper.ink), fontSize = size.sp, lineHeight = (size * 1.95f * Reading.line).sp, fontFamily = Fonts.quran, textAlign = TextAlign.Justify,
        onTextLayout = { layout = it },
        modifier = Modifier.fillMaxWidth().pointerInput(run.first().s, run.first().a) {
            detectTapGestures(onTap = { toggle() }, onLongPress = { pos ->
                val o = layout?.getOffsetForPosition(pos) ?: return@detectTapGestures
                ranges.firstOrNull { o in it.first }?.second?.let { haptic.performHapticFeedback(HapticFeedbackType.LongPress); long(it) }
            })
        })
}

// =============================================================================== books

enum class RKind { HEAD, BODY, QURAN, NOTE, SMALL }
data class RBlock(val kind: RKind, val text: String)

/** Any text book in reading mode: headings, paragraphs, verses, notes; it reopens where you left it. */
@Composable
fun TextReading(title: String, key: String, blocks: List<RBlock>, startAt: Int = -1, back: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val scope = rememberCoroutineScope()
    val saved = remember(key) { Reading.pos(key) }
    val last = (blocks.size - 1).coerceAtLeast(0)
    val state = rememberLazyListState(if (startAt >= 0) startAt.coerceIn(0, last) else saved.first.coerceIn(0, last), if (startAt >= 0) 0 else saved.second)
    LaunchedEffect(key) { snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }.collect { (i, o) -> Reading.setPos(key, i, o) } }
    var toc by remember { mutableStateOf(false) }
    val heads = remember(blocks) { blocks.withIndex().filter { it.value.kind == RKind.HEAD } }
    val i = state.firstVisibleItemIndex
    val head = heads.lastOrNull { it.index <= i }?.value?.text ?: ""
    val pct = if (last > 0) i * 100 / last else 100
    ReadingShell(title, listOf(head, "${Quran.digits(pct)}٪").filter { it.isNotBlank() }.joinToString(" · "), back, progress = pct / 100f, scroll = state,
        tiles = { if (heads.size > 1) Tile("📑", "العناوين", Modifier.weight(1f)) { toc = true } }) { toggle ->
        LazyColumn(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { toggle() }) }, state = state,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 90.dp)) {
            itemsIndexed(blocks) { _, b -> BlockView(b, toggle) { copyBlock(ctx, b.text) } }
        }
    }
    if (toc) AlertDialog(onDismissRequest = { toc = false }, containerColor = P.surface, title = { Text("العناوين", color = P.ink) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(heads.size, key = { it }) { n ->
                    val h = heads[n]
                    Text(h.value.text, color = if (h.value.text == head) P.accent else P.ink, fontSize = 15.sp, fontFamily = Fonts.naskh,
                        modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).clickable { scope.launch { state.scrollToItem(h.index) }; toc = false }.padding(horizontal = 8.dp, vertical = 9.dp))
                }
            }
        },
        confirmButton = { TextButton({ toc = false }) { Text("إغلاق") } })
}

private fun copyBlock(c: Context, text: String) {
    c.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("ALzikr", text))
    Toast.makeText(c, "نُسخ النص", Toast.LENGTH_SHORT).show()
}

@Composable
private fun BlockView(b: RBlock, toggle: () -> Unit, long: () -> Unit) {
    val paper = Reading.paper
    val ink = Color(paper.ink); val acc = Color(paper.accent); val muted = Color(paper.muted)
    val t = Reading.textSize; val ln = Reading.line
    val haptic = LocalHapticFeedback.current
    val tap = Modifier.pointerInput(b) { detectTapGestures(onTap = { toggle() }, onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); long() }) }
    when (b.kind) {
        RKind.HEAD -> Text(b.text, color = acc, fontSize = (t + 3).sp, lineHeight = ((t + 3) * 1.6f).sp, fontWeight = FontWeight.Bold, fontFamily = Fonts.naskh,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp).then(tap))
        RKind.BODY -> Text(b.text, color = ink, fontSize = t.sp, lineHeight = (t * 1.75f * ln).sp, fontFamily = Fonts.naskh, textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).then(tap))
        RKind.QURAN -> Text(b.text, color = ink, fontSize = (t + 5).sp, lineHeight = ((t + 5) * 1.95f * ln).sp, fontFamily = Fonts.quran, textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).then(tap))
        RKind.NOTE -> Text(b.text, color = ink.copy(alpha = .85f), fontSize = (t - 2).sp, lineHeight = ((t - 2) * 1.7f * ln).sp, fontFamily = Fonts.naskh, textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(acc.copy(alpha = .07f)).then(tap).padding(12.dp))
        RKind.SMALL -> Text(b.text, color = muted, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp).then(tap))
    }
}
