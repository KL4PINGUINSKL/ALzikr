package com.alzikr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun share(c: Context, text: String) = c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "مشاركة"))
internal fun copy(c: Context, text: String) {
    c.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("ALzikr", text))
    Toast.makeText(c, "نُسخ", Toast.LENGTH_SHORT).show()
}

/** The Library tab: the Quran and the hadith books. Opens a reader when [Nav] asks for one. */
@Composable
fun LibraryScreen() {
    val keep = androidx.compose.runtime.saveable.rememberSaveableStateHolder()   // the list below keeps its place while a page covers it
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    // The verse asked for is framed; with Nav.quranSheet its sheet opens at once.
    Nav.quran?.let { (s, a) -> if (Quran.ready) QuranReader(s, a, back = { Nav.quran = null }, mark = true) else Loading(); return }
    Nav.hadith?.let { (b, n) -> HadithReader(b, n, back = { Nav.hadith = null }); return }
    Nav.sharh?.let { id -> SharhOpen(id) { Nav.sharh = null }; return }
    Nav.hadithQuery?.let { q -> HadithSearchPage(initial = q) { Nav.hadithQuery = null }; return }
    keep.SaveableStateProvider("main") {
    var section by rememberSaveable { mutableStateOf(0) }
    Nav.library?.let { section = it; Nav.library = null }
    // A books section opened from the stories or the sciences ("" = all books).
    var booksAt by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    // The sections (e.g. the Islamic stories list) keep their place while a books page covers them.
    val keepTabs = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    booksAt?.let { (id, name) ->
        Subpage(if (id.isEmpty()) "الكتب الإسلامية" else name, { booksAt = null }) { BooksHome(root = id.ifEmpty { null }, rootTitle = name) }
        return@SaveableStateProvider
    }
    keepTabs.SaveableStateProvider("tabs") {
    Column(Modifier.fillMaxSize()) {
        // The sections as an even grid of tiles that slides away with the finger as you scroll down and slides back as
        // you scroll up, anywhere in the list (no jump: it moves exactly as far as the content does). Hidden in reading mode.
        val tabs = listOf("📖" to "القرآن", "🌿" to "القصص", "📚" to "التفاسير", "📜" to "الحديث", "💡" to "الشروح", "🎓" to "الكتب", "★" to "المفضلة", "🔍" to "بحث الحديث")
        val headerH = remember { androidx.compose.runtime.mutableIntStateOf(0) }
        val hidden = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        val fold = remember {
            object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    val before = hidden.floatValue
                    hidden.floatValue = (before - available.y).coerceIn(0f, headerH.intValue.toFloat())
                    return androidx.compose.ui.geometry.Offset(0f, before - hidden.floatValue)
                }
            }
        }
        if (Reading.open == 0) Box(Modifier.fillMaxWidth().clipToBounds().layout { m, c ->
            val pl = m.measure(c.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
            headerH.intValue = pl.height
            val h = (pl.height - hidden.floatValue).toInt().coerceIn(0, pl.height)
            layout(pl.width, h) { pl.place(0, h - pl.height) }
        }) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                TabGrid(tabs.mapIndexed { i, (ic, t) -> Triple(i.toString(), ic, t) }, section.toString(), cols = 4) {
                    // The eighth tile opens the hadith search (two even rows of four).
                    if (it == "7") Nav.hadithQuery = "" else section = it.toInt()
                }
            }
        }
        Box(Modifier.weight(1f).nestedScroll(fold)) {
            when (section) {
                0 -> if (Quran.ready) QuranHome() else Loading()
                1 -> StoriesHome(openBooks = { id, name -> booksAt = id to name })
                2 -> TafsirHome()
                3 -> HadithHome()
                4 -> SharhHome()
                5 -> SciencesHome(openBooks = { id, name -> booksAt = id to name }, openAll = { booksAt = "" to "" })
                else -> Favorites()
            }
        }
    }
    }
    }
}

/**
 * A list's scroll position that survives leaving the screen: opening a surah (or a hadith) replaces the list, and its
 * ordinary state was lost, so «back» showed the top. Kept per [key] for as long as the app runs.
 */
private val keptLists = HashMap<String, Pair<Int, Int>>()
internal fun keptPosition(key: String): Pair<Int, Int> = keptLists[key] ?: (0 to 0)
@Composable
internal fun rememberKeptListState(key: String): androidx.compose.foundation.lazy.LazyListState {
    val saved = keptLists[key] ?: (0 to 0)
    val state = remember(key) { androidx.compose.foundation.lazy.LazyListState(saved.first, saved.second) }
    LaunchedEffect(state) { snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }.collect { keptLists[key] = it } }
    return state
}

@Composable private fun Loading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent) }

// =============================================================================== Quran

@Composable
private fun QuranHome() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var query by rememberSaveable { mutableStateOf("") }
    var byJuz by rememberSaveable { mutableStateOf(false) }
    val q = Quran.normalize(query)
    val jumps = remember(q) { Quran.jumps(query) }
    val named = jumps.filter { it.surahOnly }.map { it.s }.toSet()
    val surahs = if (q.isEmpty()) Quran.surahs else Quran.surahs.filter { it.n in named || Quran.normalize(it.name).contains(q) || it.n.toString() == Quran.latinDigits(q) }
    val hits = remember(q) { if (q.length >= 3) Quran.search(q) else emptyList() }
    val listState = rememberKeptListState("quran:" + (if (byJuz) "juz" else "surah"))
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Hint("quran", "📖", "اضغط «متابعة القراءة» لتكمل من حيث توقفت، أو اختر سورة أو جزءًا، أو ابحث بكلمة من آية.") }
        item { MushafViewCard() }
        item {
            val (ls, la) = Quran.last ?: (1 to 1)
            Panel(Modifier.clickable { Nav.reading = ls to la }, color = P.accent.copy(alpha = .10f)) {
                GText("📜 وضع القراءة", color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = L.head)
                Sub("صفحات كاملة بلون ورق مريح وخط كبير، دون أزرار تشغلك، وتتابع من حيث توقفت. ورد اليوم: ${Quran.digits(Reading.todayPages())} من ${Quran.digits(Reading.goal)} صفحات · الختمة ${Quran.digits(Reading.far * 100 / 604)}٪", size = 12.sp)
            }
        }
        item {
            Quran.last?.let { (s, a) ->
                Panel(Modifier.clickable { MushafView.open(s, a) }, color = P.card) {
                    Sub("متابعة القراءة")
                    Text("سورة ${Quran.surahs[s - 1].name} · الآية ${Quran.digits(a)}", color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = L.head)
                    Quran.at(s, a)?.let { Sub("صفحة ${it.page} · الجزء ${it.juz}", size = 12.sp) }
                }
            }
        }
        item {
            OutlinedTextField(query, { query = it }, placeholder = { Text("سورة الكهف، الكهف 10، آية الكرسي، أو كلمة…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("السور", !byJuz) { byJuz = false }; Choice("الأجزاء", byJuz) { byJuz = true }
            }
        }
        // A verse or a juz the query names: «الكهف 10», «2:255», «آية الكرسي», «جزء عم».
        items(jumps.filter { !it.surahOnly }) { j ->
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.accent.copy(alpha = .12f)).clickable { Nav.openQuran(j.s, j.a) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(j.label, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = Fonts.naskh, modifier = Modifier.weight(1f))
                Text("انتقل ›", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (q.isNotEmpty() && surahs.isEmpty() && hits.isEmpty() && jumps.isEmpty() && q.length >= 2)
            item { Sub("لا نتائج. اكتب اسم السورة (مثل: الكهف، يس) أو رقمها، أو «الكهف 10»، أو كلمة من آية.", Modifier.padding(8.dp)) }
        if (byJuz && q.isEmpty()) items((1..30).toList()) { j ->
            val a = Quran.firstOfJuz(j)
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { MushafView.open(a.s, a.a) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("الجزء ${Quran.digits(j)}", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Sub("${Quran.surahs[a.s - 1].name} ${Quran.digits(a.a)} · ص ${a.page}", size = 12.sp)
            }
        } else items(surahs, key = { it.n }) { s ->
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { MushafView.open(s.n, 1) }.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(L.shape(10.dp)).background(P.card), contentAlignment = Alignment.Center) { Text(Quran.digits(s.n), color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("سورة ${s.name}", color = P.ink, fontSize = 17.sp, fontFamily = Fonts.naskh)
                    Sub("${if (s.meccan) "مكية" else "مدنية"} · ${Quran.digits(s.count)} آية", size = 12.sp)
                }
                Sub("ص ${Quran.ofSurah(s.n).first().page}", size = 11.sp)
            }
        }
        if (hits.isNotEmpty()) {
            item { Title("في الآيات (${hits.size}${if (hits.size >= 300) "+" else ""})", Modifier.padding(top = 10.dp)) }
            items(hits) { a ->
                Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { Nav.openQuran(a.s, a.a) }.padding(12.dp)) {
                    Text(a.text, color = P.ink, fontSize = 18.sp, fontFamily = Fonts.quran, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 32.sp)
                    Sub("${Quran.surahs[a.s - 1].name} ${Quran.digits(a.a)}", size = 12.sp, color = P.gold)
                }
            }
        }
        item { Sub("نص المصحف: رواية حفص، بخط أميري قرآن. المصدر: tanzil.net عبر quran-api.", Modifier.padding(6.dp), size = 11.sp) }
    }
}

/** The mushaf opened over another page (the verse of the day), loaded first if needed. */
@Composable
fun QuranOverlay(surah: Int, ayah: Int, back: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    if (Quran.ready) QuranReader(surah, ayah, back, mark = true) else Loading()
}

@Composable
private fun QuranReader(surah: Int, startAyah: Int, back: () -> Unit, mark: Boolean = false) {
    QuietWhileReading()  // the friend stays calm and silent while reading
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    @Suppress("UNUSED_VARIABLE") val tv = Tafsir.version
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var s by remember(surah) { mutableStateOf(surah) }
    val info = Quran.surahs[s - 1]
    val list = Quran.ofSurah(s)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = if (s == surah) (startAyah - 1).coerceAtLeast(0) else 0)
    var chosen by remember { mutableStateOf<Quran.Ayah?>(null) }
    // The verse shown framed: the one opened, then any verse tapped; the frame fades after five seconds.
    var focus by remember { mutableStateOf(if (mark) surah to startAyah else null) }
    LaunchedEffect(focus) { if (focus != null) { kotlinx.coroutines.delay(5_000); focus = null } }
    LaunchedEffect(Unit) { if (Nav.quranSheet) { Nav.quranSheet = false; chosen = Quran.at(surah, startAyah) } }
    // Defaults from «طريقة عرض المصحف» in the library.
    var study by rememberSaveable { mutableStateOf(MushafView.studyDefault) }
    var hifz by rememberSaveable { mutableStateOf(MushafView.hifzDefault) }
    val shown = remember(s, hifz) { androidx.compose.runtime.mutableStateListOf<Int>() }
    var sheet by remember { mutableStateOf<String?>(null) } // "reciter" | "font"
    var fetching by remember { mutableStateOf(false) }
    val size = Quran.fontSize
    val header = if (s != 1 && s != 9) 1 else 0
    fun visibleAyah() = list.getOrNull((state.firstVisibleItemIndex - header).coerceAtLeast(0)) ?: list.first()
    // A mushaf page counts for the friend after twenty seconds on it (once a day).
    val readPage = visibleAyah().page
    LaunchedEffect(s, readPage) {
        kotlinx.coroutines.delay(20_000); Progress.record(Progress.Kind.PAGE, "p$readPage")
        // The next pages of the khatma, read in order here, move it on (and complete the wird), as in reading mode.
        val k = Data.khatmaPage
        if (k < Data.PAGES && readPage in (k + 1)..(k + 3)) { if (Data.khatmaStarted.isEmpty()) Data.khatmaStarted = java.time.LocalDate.now().toString(); Data.khatmaPage = readPage }
    }
    LaunchedEffect(s) { snapshotFlow { state.firstVisibleItemIndex }.collect { i -> list.getOrNull((i - header).coerceAtLeast(0))?.let { Quran.last = it.s to it.a } } }
    // Follows the recitation as it moves on, while you read its surah (and into the next surah when it goes on there);
    // opening the reader elsewhere, or reading another surah meanwhile, never pulls you back to the verse being recited.
    var lastNow by remember { mutableStateOf(Recitation.now) }
    LaunchedEffect(Recitation.now) {
        val now = Recitation.now; val prev = lastNow; lastNow = now
        if (now == null || prev == null || prev == now) return@LaunchedEffect
        if (prev.first != s && now.first != s) return@LaunchedEffect
        val (rs, ra) = now
        if (rs != s) { s = rs }
        runCatching { state.animateScrollToItem((ra - 1 + header).coerceAtLeast(0)) }
    }
    LaunchedEffect(study, s, Tafsir.selected) {
        if (study && !Tafsir.has(ctx, Tafsir.selected, s)) {
            fetching = true
            val ok = withContext(Dispatchers.IO) { Tafsir.fetch(ctx, Tafsir.selected, s) }
            fetching = false
            if (!ok) Toast.makeText(ctx, "تعذّر تحميل التفسير، تحقق من الإنترنت", Toast.LENGTH_LONG).show()
        }
    }
    Subpage("سورة ${info.name}", back) {
        Column(Modifier.fillMaxSize()) {
            Sub("${if (info.meccan) "مكية" else "مدنية"} · ${Quran.digits(info.count)} آية · تبدأ في الصفحة ${Quran.digits(list.first().page)}", Modifier.padding(horizontal = 16.dp), size = 12.sp)
            if (hifz) Hint("hifz", "🧠", "وضع الحفظ: تظهر أول كلمة من كل آية فقط. سمّع الآية من حفظك ثم اضغط عليها لتتأكد. استخدم «تكرار» في الاستماع لتثبيتها.", Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            Hint("reader", "👆", "اضغط على أي آية لتظهر خياراتها: الاستماع منها، تفسيرها، العلامة والمشاركة. والأزرار في الأسفل للسورة كلها.", Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            if (study) {
                // Which tafsir is shown under the verses, with a clear label.
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("التفسير:", color = P.muted, fontSize = 13.sp)
                    PickOne(Tafsir.books.map { it.id to it.name }, Tafsir.selected, "اختر التفسير", shown = 1, modifier = Modifier.padding(start = 8.dp)) { Tafsir.selected = it }
                }
                if (fetching) Sub("جارٍ تحميل ${Tafsir.book(Tafsir.selected).name} لهذه السورة…", Modifier.padding(horizontal = 16.dp), size = 12.sp, color = P.gold)
            }
            LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                if (header == 1) item {
                    Text(Quran.BASMALA, color = P.accent, fontSize = (size + 2).sp, fontFamily = Fonts.quran, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                }
                itemsIndexed(list, key = { _, a -> a.a }) { i, a ->
                    if (i > 0 && list[i - 1].page != a.page) Text("— صفحة ${Quran.digits(a.page)} · الجزء ${Quran.digits(a.juz)} —", color = P.muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    val marked = Quran.marked(a.s, a.a)
                    val reading = Recitation.now == (a.s to a.a)
                    val focused = focus == (a.s to a.a)
                    val hidden = hifz && a.a !in shown
                    // Memorization mode: only the first word shows until you tap to check yourself.
                    @Suppress("UNUSED_VARIABLE") val mv = Mushafs.version
                    Text(if (hidden) androidx.compose.ui.text.AnnotatedString(a.text.substringBefore(' ') + " … ﴿${Quran.digits(a.a)}﴾")
                        else androidx.compose.ui.text.buildAnnotatedString { append(Mushafs.verse(ctx, a)); append((if (a.sajda) " ۩" else "") + " ﴿${Quran.digits(a.a)}﴾") },
                        color = if (hidden) P.muted else P.ink, fontSize = size.sp, lineHeight = (size * 1.9f).sp, fontFamily = Fonts.quran,
                        textAlign = if (L.id == "mushaf" || L.id == "kaaba") TextAlign.Justify else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp))
                            .background(when { hidden -> P.card; reading -> P.accent.copy(alpha = .18f); focused -> P.accent.copy(alpha = .12f); marked -> P.gold.copy(alpha = .14f); else -> P.bg })
                            .then(if (focused) Modifier.border(2.dp, P.accent, L.shape(10.dp)) else Modifier)
                            .clickable { if (hidden) shown += a.a else { chosen = a; focus = a.s to a.a } }.padding(horizontal = 6.dp, vertical = 4.dp))
                    if (study) Tafsir.get(ctx, Tafsir.selected, a.s, a.a)?.takeIf { it.from == a.a && it.text.isNotBlank() }?.let { e ->
                        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(L.shape(12.dp)).background(P.card).padding(12.dp)) {
                            Sub(Tafsir.book(Tafsir.selected).name + if (e.to > e.from) " · الآيات ${Quran.digits(e.from)}–${Quran.digits(e.to)}" else "", color = P.gold, size = 11.sp)
                            TafsirText(e.text, (15 * Data.textScale).sp, (26 * Data.textScale).sp)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (s > 1) Choice("‹ السورة السابقة: ${Quran.surahs[s - 2].name}", false) { s -= 1; scope.launch { state.scrollToItem(0) } } else Box(Modifier)
                        if (s < 114) Choice("السورة التالية: ${Quran.surahs[s].name} ›", false) { s += 1; scope.launch { state.scrollToItem(0) } }
                    }
                }
            }
            // The bar at the bottom: three clear actions, or the player while reciting.
            Column(Modifier.fillMaxWidth().background(P.surface)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
                if (Recitation.now != null) PlayerBar { sheet = "reciter" }
                else Column {
                Wrap(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 6.dp), gap = 6.dp) {
                    Choice("🎙 ${Recitation.reciterName()} ▾", false) { sheet = "reciter" }
                    RangeChoice(scroll = false)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tile("▶", "استماع", Modifier.weight(1f)) { Recitation.play(ctx, s, visibleAyah().a) }
                    Tile("📖", if (study) "إخفاء التفسير" else "التفسير", Modifier.weight(1f), on = study) { study = !study }
                    Tile("🧠", if (hifz) "إنهاء الحفظ" else "وضع الحفظ", Modifier.weight(1f), on = hifz) { hifz = !hifz }
                    Tile("📄", "صفحات", Modifier.weight(1f)) { visibleAyah().let { Nav.reading = it.s to it.a } }
                    Tile("Aa", "الخط", Modifier.weight(1f)) { sheet = "font" }
                }
                }
            }
        }
    }
    chosen?.let { a -> AyahSheet(a) { chosen = null } }
    when (sheet) {
        "reciter" -> ReciterDialog { sheet = null }
        "font" -> AppSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(horizontal = 18.dp)) { Title("المصحف والخط"); Box(Modifier.padding(top = 8.dp)) { MushafSettings() } }
        }
    }
}

/** While reciting: where we are, and play / pause / previous / next / stop, the reciter and the repeat. */
@Composable
private fun PlayerBar(chooseReciter: () -> Unit) {
    val ctx = LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) { Recitation.bars++; onDispose { Recitation.bars-- } }
    val (s, a) = Recitation.now ?: return
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("سورة ${Quran.surahs[s - 1].name} · الآية ${Quran.digits(a)}", color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("القارئ: ${Recitation.reciterName()} ▾", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable(onClick = chooseReciter).padding(vertical = 2.dp))
            }
            if (Recitation.loading) CircularProgressIndicator(Modifier.size(22.dp), color = P.accent, strokeWidth = 2.dp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("⏮", "السابقة", Modifier.weight(1f)) { Recitation.skip(ctx, -1) }
            Tile(if (Recitation.paused) "▶" else "⏸", if (Recitation.paused) "متابعة" else "إيقاف مؤقت", Modifier.weight(1.2f), on = true) { if (Recitation.paused) Recitation.resume() else Recitation.pause() }
            Tile("⏭", "التالية", Modifier.weight(1f)) { Recitation.skip(ctx, +1) }
            Tile("🔁", "تكرار ×${Recitation.repeat}", Modifier.weight(1f), on = Recitation.repeat > 1) { Recitation.repeat = listOf(1, 3, 5, 10).let { r -> r[(r.indexOf(Recitation.repeat) + 1) % r.size] } }
            Tile("⏹", "إنهاء", Modifier.weight(1f)) { Recitation.stop() }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("المدى:", color = P.muted, fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
            RangeChoice(live = true)
        }
    }
}

@Composable
internal fun ReciterDialog(close: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(onDismissRequest = close, containerColor = P.surface,
        title = { Text("اختر القارئ", color = P.ink) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                Recitation.styles.forEachIndexed { st, label ->
                item { Text(label, color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (st == 0) 0.dp else 12.dp, bottom = 4.dp, start = 4.dp)) }
                items(Recitation.reciters.filter { it.style == st }) { r ->
                    val on = r.folder == Recitation.reciter
                    Row(Modifier.fillMaxWidth().clip(L.shape(10.dp)).background(if (on) P.accent.copy(alpha = .12f) else P.surface).clickable {
                        Recitation.reciter = r.folder; close()
                        Recitation.now?.let { (rs, ra) -> Recitation.play(ctx, rs, ra) }
                    }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.name, color = if (on) P.accent else P.ink, fontSize = 16.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        if (on) Text("✓", color = P.accent, fontWeight = FontWeight.Bold)
                    }
                }
                }
                item {
                    Text("مدى القراءة", color = P.ink, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                    RangeChoice()
                    Sub("التلاوة من everyayah.com وتحتاج إلى الإنترنت.", Modifier.padding(top = 6.dp), size = 11.sp)
                }
            }
        },
        confirmButton = { TextButton(close) { Text("إغلاق") } })
}

/** Tapping a verse: the verse, six clear actions, then its tafsir in its own section. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun AyahSheet(first: Quran.Ayah, inContext: (() -> Unit)? = null, close: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // The sheet can walk to the next or previous verse (buttons, or a sideways swipe) without closing.
    var a by remember(first) { mutableStateOf(first) }
    val scroll = rememberScrollState()
    fun step(d: Int) {
        val next = if (d > 0) Quran.at(a.s, a.a + 1) ?: Quran.at(a.s + 1, 1) else if (a.a > 1) Quran.at(a.s, a.a - 1) else if (a.s > 1) Quran.ofSurah(a.s - 1).lastOrNull() else null
        if (next != null) { a = next; scope.launch { scroll.scrollTo(0) } }
    }
    @Suppress("UNUSED_VARIABLE") val tv = Tafsir.version
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ref = "سورة ${Quran.surahs[a.s - 1].name}، الآية ${a.a}"
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val sel = Tafsir.selected
    LaunchedEffect(sel, a.s) {
        failed = false
        if (!Tafsir.has(ctx, sel, a.s)) {
            loading = true
            failed = !withContext(Dispatchers.IO) { Tafsir.fetch(ctx, sel, a.s) }
            loading = false
        }
    }
    val entry = Tafsir.get(ctx, sel, a.s, a.a)
    var pickReciter by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf(false) }
    var compare by remember { mutableStateOf(false) }
    if (pickReciter) ReciterDialog { pickReciter = false }
    // Opens fully, so the tafsir is in view right away.
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppSheet(onDismissRequest = close) {
        var drag by remember { mutableStateOf(0f) }
        Column(Modifier.fillMaxWidth().quickDismiss(sheetState, close)
            .pointerInput(Unit) {
                // A sideways swipe: as in the mushaf, a swipe to the right goes on to the next verse, to the left goes back.
                detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragCancel = { drag = 0f },
                    onDragEnd = { val limit = 70.dp.toPx(); if (drag > limit) step(1) else if (drag < -limit) step(-1); drag = 0f }) { ch, dx -> drag += dx; ch.consume() }
            }
            .verticalScroll(scroll).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val hasPrev = a.s > 1 || a.a > 1; val hasNext = a.s < 114 || Quran.at(a.s, a.a + 1) != null
                Text("› الآية السابقة", color = if (hasPrev) P.accent else P.muted.copy(alpha = .4f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(L.shape(10.dp)).clickable(enabled = hasPrev) { step(-1) }.padding(horizontal = 8.dp, vertical = 6.dp))
                Spacer(Modifier.weight(1f))
                Sub("اسحب جانبًا للتنقل", size = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("الآية التالية ‹", color = if (hasNext) P.accent else P.muted.copy(alpha = .4f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(L.shape(10.dp)).clickable(enabled = hasNext) { step(1) }.padding(horizontal = 8.dp, vertical = 6.dp))
            }
            Text(ref + " · صفحة ${Quran.digits(a.page)} · الجزء ${Quran.digits(a.juz)}" + if (a.sajda) " · موضع سجدة" else "", color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(a.text, color = P.ink, fontSize = 22.sp, fontFamily = Fonts.quran, lineHeight = 40.sp, modifier = Modifier.padding(vertical = 4.dp))
            // Listening first: play it, from here, memorize it, the reciter and how far «من هنا» goes.
            Wrap(Modifier.fillMaxWidth(), gap = 6.dp) {
                Choice("▶ هذه الآية", true) { Recitation.play(ctx, a.s, a.a, only = Recitation.Range.AYAH); if (inContext == null) close() }
                Choice("⏩ من هنا: " + Recitation.rangeText(), false) { Recitation.play(ctx, a.s, a.a); if (inContext == null) close() }
                Choice("🔁 حفظها ×5", false) { Recitation.repeat = 5; Recitation.play(ctx, a.s, a.a); if (inContext == null) close() }
                if (inContext != null) Choice("📖 في سياقها", false) { close(); inContext() }
            }
            ReciterLine(Modifier.padding(top = 8.dp)) { pickReciter = true }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("مدى «من هنا»: ", color = P.muted, fontSize = 13.sp)
                RangeChoice()
            }
            Recitation.now?.takeIf { inContext != null }?.let { (ps, pa) ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(12.dp)).background(P.accent.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GText(if (Recitation.loading) "⏳ جارٍ التحميل…" else "🎙 ${Quran.surahs[ps - 1].name} ${Quran.digits(pa)}", color = P.ink, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    Choice(if (Recitation.paused) "▶ متابعة" else "⏸ إيقاف مؤقت", false) { if (Recitation.paused) Recitation.resume() else Recitation.pause() }
                    Choice("■ إنهاء", false) { Recitation.stop() }
                }
            }

            // The tafsir, right under the verse. It folds away with one tap (a long one, such as Ibn Kathir's, need not be
            // scrolled past to reach the verse's actions); the choice is remembered.
            var tafsirShown by remember { mutableStateOf(!Store.prefs.getBoolean("sheet_tafsir_hidden", false)) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                GText("📖 التفسير:", color = P.muted, fontSize = 13.sp)
                PickOne(Tafsir.books.map { it.id to it.name }, sel, "اختر التفسير", shown = 1, modifier = Modifier.padding(start = 8.dp)) { Tafsir.selected = it; tafsirShown = true }
            }
            val fold = { tafsirShown = !tafsirShown; Store.prefs.edit().putBoolean("sheet_tafsir_hidden", !tafsirShown).apply() }
            // Folded: a short bubble across the width, just its name and «إظهار».
            if (!tafsirShown) Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(14.dp)).background(P.card).clickable(onClick = fold).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(Tafsir.book(sel).name, color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("إظهار ▾", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (tafsirShown) Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(14.dp)).background(P.card).padding(14.dp)) {
                // The name, and «إخفاء» at the bubble's top left.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Tafsir.book(sel).name, color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("إخفاء ▴", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(L.shape(8.dp)).clickable(onClick = fold).padding(horizontal = 8.dp, vertical = 4.dp))
                }
                when {
                    entry != null && entry.text.isNotBlank() -> {
                        if (entry.to > entry.from) Sub("يشمل الآيات ${Quran.digits(entry.from)}–${Quran.digits(entry.to)}", size = 12.sp)
                        TafsirText(entry.text, (16 * Data.textScale).sp, (28 * Data.textScale).sp, Modifier.padding(top = 6.dp))
                    }
                    loading -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = P.accent, strokeWidth = 2.dp)
                        Sub("  جارٍ تحميل تفسير هذه السورة…")
                    }
                    failed -> Text("تعذّر التحميل، تحقق من الإنترنت. اضغط هنا لإعادة المحاولة", color = P.danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp).clickable {
                        scope.launch { loading = true; failed = !withContext(Dispatchers.IO) { Tafsir.fetch(ctx, sel, a.s) }; loading = false }
                    })
                    else -> Sub("لا يوجد في هذا الكتاب تفسير خاص بهذه الآية.", Modifier.padding(top = 8.dp))
                }
            }
            // The meanings of its words, a reflection, and other tafsirs side by side (each opens on a tap).
            if (sel != "gharib") TafsirCard("gharib", a.s, a.a, "🔤 معاني الكلمات (غريب القرآن)")
            val openT = remember { Nav.openTadabbur.also { Nav.openTadabbur = false } }
            if (sel != "tadabbur") TafsirCard("tadabbur", a.s, a.a, "🌱 وقفات للتدبّر والعمل", startOpen = openT)
            if (!compare) Action("📚 قارن التفاسير", Modifier.fillMaxWidth().padding(top = 8.dp), filled = false) { compare = true }
            else compareSet.filter { it != sel }.forEach { TafsirCard(it, a.s, a.a) }

            // The verse's other actions, after its tafsir.
            Text("خيارات الآية", color = P.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            Wrap(Modifier.fillMaxWidth(), gap = 6.dp) {
                Choice(if (Quran.marked(a.s, a.a)) "★ مُعلَّمة" else "☆ علامة", Quran.marked(a.s, a.a)) { Quran.toggleMark(a.s, a.a) }
                Choice("📍 وصلت هنا", false) {
                    Data.khatmaPage = a.page; if (Data.khatmaStarted.isEmpty()) Data.khatmaStarted = java.time.LocalDate.now().toString()
                    Toast.makeText(ctx, "سُجّلت في ختمتك: الصفحة ${a.page}", Toast.LENGTH_SHORT).show()
                }
                Choice("⧉ نسخ", false) { copy(ctx, "${a.text}\n[$ref]") }
                Choice("↗ مشاركة", false) { share(ctx, "${a.text}\n[$ref]") }
                Choice("🖼 صورة", false) { ShareCard.share(ctx, ref, a.text, "", quran = true) }
                // The verse with the tafsir shown below it (footnotes left out, at most ~700 letters).
                entry?.text?.takeIf { it.isNotBlank() }?.let { t ->
                    Choice("🖼 صورة مع التفسير", false) {
                        val clean = t.replace(Regex("""\[\[.*?\]\]""", RegexOption.DOT_MATCHES_ALL), "").trim().let { if (it.length > 700) it.take(700).substringBeforeLast(' ') + "…" else it }
                        ShareCard.share(ctx, ref, a.text, "", quran = true, note = clean, noteTitle = Tafsir.book(sel).name)
                    }
                }
                Choice(if (VerseNotes.get(a.s, a.a).isEmpty()) "📝 ملاحظة" else "📝 ملاحظتي", VerseNotes.get(a.s, a.a).isNotEmpty()) { noteFor = true }
            }
            if (noteFor) VerseNoteDialog(a.s, a.a) { noteFor = false }
            VerseNotes.get(a.s, a.a).takeIf { it.isNotEmpty() }?.let { n ->
                Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(12.dp)).background(P.accent.copy(alpha = .10f)).clickable { noteFor = true }.padding(12.dp)) {
                    Sub("📝 ملاحظتي", size = 12.sp, color = P.gold)
                    Text(n, color = P.ink, fontSize = 15.sp, fontFamily = Fonts.naskh)
                }
            }
        }
    }
}

/** The tafsir books: which surahs are on the phone, download the short ones whole, delete, choose the default. */
@Composable
private fun TafsirHome() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE") val tv = Tafsir.version
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { TafsirSearchPanel() }
        item { Hint("tafsirs", "📚", "اختر تفسيرك ليظهر أولًا. لتقرأ تفسير آية: افتح المصحف واضغط على الآية، أو اختر «التفسير تحت الآيات».") }
        item { Sub("اختر التفسير الافتراضي. كل سورة تُحمَّل عند فتح تفسيرها أول مرة ثم تبقى على الهاتف. التفاسير المختصرة يمكن تنزيلها كاملة.", size = 12.sp) }
        items(Tafsir.books, key = { it.id }) { b ->
            val have = Tafsir.count(ctx, b.id)
            val busy = Tafsir.busy == b.id
            Column(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(if (Tafsir.selected == b.id) P.accent.copy(alpha = .12f) else P.surface)
                .border(1.dp, P.line, L.shape(16.dp)).clickable { Tafsir.selected = b.id }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.name, color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = Fonts.naskh)
                        Sub(b.about, size = 12.sp)
                    }
                    if (Tafsir.selected == b.id) Text("✓ الافتراضي", color = P.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sub(when { busy -> "جارٍ التنزيل… ${Tafsir.done}/114"; have == 114 -> "كامل على الهاتف · ${Tafsir.bytes(ctx, b.id) / 1_000_000} م.ب"; have > 0 -> "$have سورة على الهاتف"; else -> "يُحمَّل عند الحاجة" }, Modifier.weight(1f), size = 12.sp)
                    // Any book can be kept whole on the phone (for travel, without internet); the long ones are marked.
                    if (have < 114 && !busy && Tafsir.busy == null) Text(if (b.light) "تنزيل كامل" else "تنزيل كامل (كبير)", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { Tafsir.fetchAll(ctx, b.id) }
                            Toast.makeText(ctx, if (ok) "نُزّل «${b.name}» كاملًا" else "توقف التنزيل، تحقق من الإنترنت ثم أعد المحاولة", Toast.LENGTH_LONG).show()
                        }
                    }.padding(6.dp))
                    if (have > 0 && !busy) Text("حذف", color = P.danger, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Tafsir.delete(ctx, b.id) }.padding(6.dp))
                }
            }
        }
        item { Sub("المصدر: tafsir_api (رخصة MIT) من quran.com و Tarteel. في وضع «📖 تفسير» داخل المصحف يظهر التفسير تحت كل آية.", Modifier.padding(6.dp), size = 11.sp) }
    }
}

// =============================================================================== Hadith

@Composable
private fun HadithHome() {
    val keep = androidx.compose.runtime.saveable.rememberSaveableStateHolder()   // the list below keeps its place while a page covers it
    val ctx = LocalContext.current
    var searching by rememberSaveable { mutableStateOf(false) }
    if (searching) { HadithSearchPage { searching = false }; return }
    keep.SaveableStateProvider("main") {
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE") val v = Hadith.version
    var remove by remember { mutableStateOf<Hadith.Info?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SearchBar("ابحث عن حديث في كل الكتب…") { searching = true } }
        item { Hint("hadith", "📜", "اضغط على كتاب لتفتحه. الكتب التي بجانبها «تنزيل» تُنزَّل مرة واحدة ثم تعمل بلا إنترنت.") }
        items(Hadith.books, key = { it.id }) { b ->
            val have = Hadith.available(ctx, b.id)
            val busy = Hadith.downloading == b.id
            Row(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(P.surface).border(1.dp, P.line, L.shape(16.dp))
                .clickable {
                    when {
                        have -> Nav.openHadith(b.id, 0)
                        Hadith.downloading == null -> scope.launch {
                            val ok = withContext(Dispatchers.IO) { Hadith.download(ctx, b.id) }
                            Toast.makeText(ctx, if (ok) "نُزّل «${b.title}»، يعمل الآن بلا إنترنت" else "تعذّر التنزيل، تحقق من الإنترنت", Toast.LENGTH_LONG).show()
                        }
                    }
                }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(b.title, color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = Fonts.naskh)
                    Sub(b.author, size = 12.sp)
                }
                when {
                    busy -> Text("${(Hadith.progress * 100).toInt()}%", color = P.accent, fontSize = 13.sp)
                    have -> if (b.path != null) Text("✓", color = P.gold, fontSize = 15.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { remove = b }.padding(6.dp)) else Text("مضمّن", color = P.muted, fontSize = 12.sp)
                    else -> Text("تنزيل · ${b.mb} م.ب", color = P.accent, fontSize = 12.sp)
                }
            }
        }
        item { Sub("الكتب الكبيرة تُنزّل مرة واحدة ثم تعمل بلا إنترنت. المصدر: hadith-json و hadith-api على GitHub. اضغط ✓ لحذف كتاب من الهاتف.", Modifier.padding(6.dp), size = 11.sp) }
    }
    remove?.let { b ->
        AlertDialog(onDismissRequest = { remove = null }, containerColor = P.surface, title = { Text("«${b.title}» على هاتفك", color = P.ink) },
            text = { Sub("«تحديث» يعيد تنزيله من المصدر بأحدث نسخة، و«حذف» يوفّر المساحة ويمكنك تنزيله لاحقًا.") },
            confirmButton = {
                TextButton({
                    remove = null
                    scope.launch {
                        Hadith.delete(ctx, b.id)
                        val ok = withContext(Dispatchers.IO) { Hadith.download(ctx, b.id) }
                        Toast.makeText(ctx, if (ok) "حُدّث «${b.title}»" else "تعذّر التحديث، تحقق من الإنترنت", Toast.LENGTH_LONG).show()
                    }
                }) { Text("تحديث") }
            },
            dismissButton = {
                Row {
                    TextButton({ Hadith.delete(ctx, b.id); remove = null }) { Text("حذف", color = P.danger) }
                    TextButton({ remove = null }) { Text("إلغاء") }
                }
            })
    }
    }
}

@Composable
internal fun HadithReader(bookId: String, number: Int, back: () -> Unit, focus: String? = number.takeIf { it > 0 }?.toString()) {
    val keep = androidx.compose.runtime.saveable.rememberSaveableStateHolder()   // the list below keeps its place while a page covers it
    QuietWhileReading()  // the friend stays calm and silent while reading
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var book by remember(bookId) { mutableStateOf<Hadith.Book?>(null) }
    LaunchedEffect(bookId) { book = withContext(Dispatchers.IO) { Hadith.load(ctx, bookId) } }
    var chapter by rememberSaveable(bookId) { mutableStateOf<String?>(null) }
    var query by rememberSaveable(bookId) { mutableStateOf("") }
    var sharhId by rememberSaveable(bookId) { mutableStateOf<String?>(null) }
    // A hadith opened by itself counts after fifteen seconds on it.
    LaunchedEffect(bookId, focus) { if (focus != null) { kotlinx.coroutines.delay(15_000); Progress.record(Progress.Kind.HADITH, "$bookId:$focus") } }
    sharhId?.let { id -> SharhOpen(id) { sharhId = null }; return }
    // An explanation opened from the list walks the hadiths around it (next / previous).
    var walk by remember(bookId) { mutableStateOf<Triple<List<Hadith.Item>, Int, String>?>(null) }
    // Back from an explanation: the list returns to the hadith last read there, not to the top of the chapter.
    var returnTo by remember(bookId) { mutableStateOf<Hadith.Item?>(null) }
    walk?.let { (list, i, id) -> BookSharh(Hadith.info(bookId)?.title ?: "", list, i, id) { at -> returnTo = list.getOrNull(at); walk = null }; return }
    val b = book
    var readMode by rememberSaveable(bookId) { mutableStateOf(false) }
    if (readMode && b != null) {
        val (blocks, at) = remember(b) {
            val out = ArrayList<RBlock>(); val heads = HashMap<String, Int>()
            val many = b.chapters.size > 1
            b.items.groupBy { it.chapter }.forEach { (ch, hs) ->
                if (many) { heads[ch] = out.size; out += RBlock(RKind.HEAD, b.chapters[ch] ?: ch) }
                hs.forEach { h -> out += RBlock(RKind.SMALL, "حديث ${h.number}"); out += RBlock(RKind.BODY, h.text) }
            }
            out to heads
        }
        TextReading(Hadith.info(bookId)?.title ?: "", "hadith:$bookId", blocks, chapter?.let { at[it] } ?: -1) { readMode = false }
        return
    }
    keep.SaveableStateProvider("main") {
    // Opened on one hadith: go to its chapter.
    LaunchedEffect(b, focus) { if (b != null && focus != null && b.chapters.size > 1) b.items.firstOrNull { it.number == focus }?.let { chapter = it.chapter } }
    Subpage(Hadith.info(bookId)?.title ?: "", { if (chapter != null && query.isEmpty()) chapter = null else back() }) {
        if (b == null) { Loading(); return@Subpage }
        val q = Hadith.normalize(query)
        val manyChapters = b.chapters.size > 1
        val shown = when {
            q.length >= 2 -> remember(q, b) { val m = HadithSearch.matcher(query); b.items.filter { m(it.text) }.take(300) }
            focus != null && chapter == null && !manyChapters -> b.items
            chapter != null -> b.items.filter { it.chapter == chapter }
            !manyChapters -> b.items
            else -> null
        }
        val start = remember(shown, focus) { if (focus != null && shown != null) shown.indexOfFirst { it.number == focus }.coerceAtLeast(0) else 0 }
        val state = rememberLazyListState()
        LaunchedEffect(shown, focus) {
            val back = returnTo
            if (back != null && shown != null) {
                returnTo = null
                val k = shown.indexOf(back)
                // Items above the hadiths: the hint, the search field, «read» (without a query) and the chapter title.
                if (k >= 0) runCatching { state.scrollToItem(k + 2 + (if (query.isEmpty()) 1 else 0) + (if (chapter != null && q.isEmpty()) 1 else 0)) }
            } else if (focus != null && shown != null && q.isEmpty()) runCatching { state.scrollToItem(start + 2 + (if (chapter != null) 1 else 0)) }
        }
        LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Hint("hadithreader", "🔍", "اختر بابًا لتقرأ أحاديثه، أو ابحث في الكتاب كله. اضغط ☆ لحفظ حديث في «المفضلة».") }
            item { OutlinedTextField(query, { query = it }, placeholder = { Text("ابحث في الكتاب") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            if (query.isEmpty()) item { Tile("📜", if (chapter != null) "اقرأ في وضع القراءة من هذا الباب" else "اقرأ الكتاب في وضع القراءة", Modifier.fillMaxWidth()) { readMode = true } }
            if (shown == null) {
                items(b.chapters.entries.toList()) { (id, name) ->
                    val count = b.items.count { it.chapter == id }
                    Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { chapter = id }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, color = P.ink, fontSize = 16.sp, fontFamily = Fonts.naskh, modifier = Modifier.weight(1f))
                        Sub("$count", size = 12.sp)
                    }
                }
            } else {
                if (chapter != null && q.isEmpty()) item { Title(b.chapters[chapter] ?: "") }
                if (shown.isEmpty()) item { Sub("لا نتائج") }
                items(shown, key = { "${it.chapter}-${it.number}-${it.text.length}" }) { h -> HadithCard(b, h, focused = h.number == focus, openSharh = { id -> val k = shown.indexOf(h); if (k >= 0) walk = Triple(shown, k, id) else sharhId = id }) }
            }
        }
    }
    }
}

@Composable
private fun HadithCard(b: Hadith.Book, h: Hadith.Item, focused: Boolean = false, openSharh: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var finding by remember { mutableStateOf(false) }
    val fav = Hadith.isFavorite(b.info.id, h.number)
    val ref = "${b.info.title}، رقم ${h.number}"
    Panel(if (focused) Modifier.border(2.dp, P.accent, L.shape()) else Modifier, pad = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Quran.digits(h.number.toIntOrNull() ?: 0).takeIf { h.number.toIntOrNull() != null } ?: h.number, color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (fav) "★" else "☆", color = if (fav) P.gold else P.muted, fontSize = 18.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Hadith.toggleFavorite(b.info.id, h.number) }.padding(horizontal = 8.dp))
            Text("نسخ", color = P.muted, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { copy(ctx, "${h.text}\n[$ref]") }.padding(horizontal = 8.dp, vertical = 4.dp))
            Text("مشاركة", color = P.muted, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { share(ctx, "${h.text}\n[$ref]") }.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Text(h.text, color = P.ink, fontSize = (17 * Data.textScale).sp, lineHeight = (30 * Data.textScale).sp, fontFamily = Fonts.naskh)
        Text(if (finding) "جارٍ البحث عن الشرح…" else "💡 الشرح والدرجة", color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp).clip(L.shape(10.dp)).background(P.accent.copy(alpha = .10f)).clickable(enabled = !finding) {
                finding = true
                scope.launch {
                    val id = withContext(Dispatchers.IO) { HadithSearch.findSharh(ctx, h.text) }
                    finding = false
                    if (id != null) openSharh(id)
                    else Toast.makeText(ctx, if (HadithSearch.hasIndex(ctx)) "لا يوجد شرح لهذا الحديث في موسوعة الأحاديث المشروحة" else "تحتاج المرة الأولى إلى الإنترنت لتحميل فهرس الشروح", Toast.LENGTH_LONG).show()
                }
            }.padding(horizontal = 12.dp, vertical = 8.dp))
        // The friend's gentle suggestion: after a while on a hadith opened by itself, if the encyclopedia explains it.
        // Quiet text only (no motion or sound), once per hadith, and it can be closed or turned off in the friend's settings.
        if (focused && Pet.species != null && Store.prefs.getBoolean("pet_suggest", true)) {
            var tip by remember(h.number) { mutableStateOf<String?>(null) }
            var closed by remember(h.number) { mutableStateOf(false) }
            LaunchedEffect(h.number) {
                kotlinx.coroutines.delay(15_000)
                if (HadithSearch.hasIndex(ctx)) tip = withContext(Dispatchers.IO) { HadithSearch.findSharh(ctx, h.text) }
            }
            val id = tip
            if (id != null && !closed) Row(Modifier.fillMaxWidth().padding(top = 10.dp).clip(L.shape(12.dp)).background(P.card).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🐾 ${Pet.name}: لهذا الحديث شرح في الموسوعة، أتحب أن تقرأه؟", color = P.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("اقرأ", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(L.shape(8.dp)).clickable { openSharh(id) }.padding(horizontal = 8.dp, vertical = 4.dp))
                Text("✕", color = P.muted, fontSize = 14.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { closed = true }.padding(horizontal = 6.dp, vertical = 4.dp))
            }
        }
    }
}

// =============================================================================== favorites

@Composable
private fun Favorites() {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val marks = if (Quran.ready) Quran.bookmarks() else emptyList()
    val favs = Hadith.favorites()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val notes = VerseNotes.all()
        if (notes.isNotEmpty()) {
            item { Title("ملاحظاتي على الآيات") }
            items(notes) { (s, a, n) ->
                Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { Nav.openQuran(s, a) }.padding(12.dp)) {
                    Sub("${Quran.surahs[s - 1].name} ${Quran.digits(a)}", size = 12.sp, color = P.gold)
                    Text(n, color = P.ink, fontSize = 15.sp, fontFamily = Fonts.naskh, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item { Title("علامات المصحف", Modifier.padding(top = if (notes.isNotEmpty()) 12.dp else 0.dp)) }
        if (marks.isEmpty()) item { Sub("اضغط على أي آية ثم «علامة» لتجدها هنا.", size = 12.sp) }
        items(marks) { (s, a) ->
            val ay = Quran.at(s, a) ?: return@items
            Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { Nav.openQuran(s, a) }.padding(12.dp)) {
                Text(ay.text, color = P.ink, fontSize = 18.sp, fontFamily = Fonts.quran, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 32.sp)
                Sub("${Quran.surahs[s - 1].name} ${Quran.digits(a)}", size = 12.sp, color = P.gold)
            }
        }
        val sharhFav = Sharh.favorites()
        if (sharhFav.isNotEmpty()) {
            item { Title("أحاديث مشروحة", Modifier.padding(top = 12.dp)) }
            items(sharhFav.toList()) { id ->
                val title = remember(id) { runCatching { java.io.File(ctx.filesDir, "sharh/h_$id.json").readText().let { org.json.JSONObject(it).optString("title") } }.getOrDefault("حديث $id") }
                Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { Nav.sharh = id }.padding(14.dp)) {
                    Text(title, color = P.ink, fontSize = 15.sp, fontFamily = Fonts.naskh, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item { Title("أحاديث مفضلة", Modifier.padding(top = 12.dp)) }
        if (favs.isEmpty()) item { Sub("اضغط ☆ على أي حديث ليُحفظ هنا.", size = 12.sp) }
        items(favs) { (book, number) ->
            val info = Hadith.info(book) ?: return@items
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { Nav.openHadith(book, number.toIntOrNull() ?: 0) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(info.title, color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Sub("رقم $number", size = 12.sp)
            }
        }
    }
}

/** "Verse of the day" and "hadith of the day", for the prayer screen. Both are fixed for the whole day. */
@Composable
fun DailyWords() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val shiftH = Data.dailyShift("hadith")
    val ayah = if (Quran.ready) Quran.ofTheDay() else null
    // Decided once (explained hadith when its index is on the phone, else one of the Forty), so the card never changes under the finger.
    val daily by androidx.compose.runtime.produceState<Pair<Sharh.Brief?, Pair<Hadith.Info, Hadith.Item>?>?>(null, shiftH) {
        value = withContext(Dispatchers.IO) {
            val e = runCatching { HadithSearch.sharhIndex(ctx); HadithSearch.ofTheDay(ctx) }.getOrNull()
            e to (if (e == null) runCatching { Hadith.ofTheDay(ctx) }.getOrNull() else null)
        }
    }
    var shown by remember { mutableStateOf<Quran.Ayah?>(null) }
    shown?.let { a -> AyahSheet(a, inContext = { Nav.overlayQuran = a.s to a.a }) { shown = null } }
    if (ayah != null) Panel(Modifier.clickable { shown = ayah }) {
        Sub("آية اليوم" + if (L.ornament.isNotEmpty()) " ${L.ornament}" else "", color = P.gold)
        Text(ayah.text, color = P.ink, fontSize = 20.sp, fontFamily = Fonts.quran, lineHeight = 36.sp, modifier = Modifier.padding(top = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sub("${Quran.surahs[ayah.s - 1].name} ${Quran.digits(ayah.a)} · اضغط لتفسيرها وسماعها", Modifier.weight(1f), size = 12.sp)
            Choice("🔀 آية أخرى", false) { Data.nextDaily("ayah") }
        }
    }
    val d = daily ?: return
    d.first?.let { e ->
        Panel(Modifier.clickable { Nav.overlaySharh = e.id }) {
            Sub("حديث اليوم" + if (L.ornament.isNotEmpty()) " ${L.ornament}" else "", color = P.gold)
            Text(e.title, color = P.ink, fontSize = (17 * Data.textScale).sp, lineHeight = 28.sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                GText("💡 اضغط لقراءة الشرح والدرجة والفوائد", color = P.accent, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Choice("🖼", false) { ShareCard.share(ctx, "حديث اليوم", e.title, "") }
                Choice("🔀 حديث آخر", false) { Data.nextDaily("hadith") }
            }
        }
        return
    }
    d.second?.let { (info, item) ->
        Panel(Modifier.clickable { Nav.overlayHadith = info.id to item.number }) {
            Sub("حديث اليوم · ${info.title}", color = P.gold)
            Text(item.text, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = 26.sp, fontFamily = Fonts.naskh, maxLines = 6, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)); Choice("🔀 حديث آخر", false) { Data.nextDaily("hadith") } }
        }
    }
}


/** A tap-to-search bar that opens the hadith search. */
@Composable
fun SearchBar(text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(P.card).border(1.dp, P.accent.copy(alpha = .5f), L.shape(16.dp)).clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        GText("🔍", fontSize = 18.sp)
        Text(text, color = P.muted, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

/** How far to read: this verse only, N verses (tap again to change N), to the end of the surah, or on. */
@Composable
fun RangeChoice(live: Boolean = false, scroll: Boolean = true) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val current = if (live) Recitation.playing else Recitation.range
    // Wraps onto new lines (no sideways scrolling); [scroll] is kept for old call sites.
    @Suppress("UNUSED_PARAMETER") val unused = scroll
    // The chosen range, then «المزيد» for the others; tapping «n آيات» again changes the count.
    PickOne(Recitation.Range.entries.map { r -> r.name to (if (r == Recitation.Range.COUNT) "${Quran.digits(Recitation.count)} آيات" else r.label) },
        current.name, "إلى أين يستمع؟", shown = 1) { k ->
        val r = Recitation.Range.valueOf(k)
        if (r == Recitation.Range.COUNT && current == r) Recitation.count = Recitation.counts[(Recitation.counts.indexOf(Recitation.count) + 1) % Recitation.counts.size]
        if (live) Recitation.changeRange(r) else Recitation.range = r
    }
}

/** «🎙 القارئ: … ▾» — always visible where listening starts. */
@Composable
fun ReciterLine(modifier: Modifier = Modifier, onClick: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    Row(modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.card).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        GText("🎙", fontSize = 16.sp)
        Text("القارئ: ", color = P.muted, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
        Text(Recitation.reciterName(), color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("تغيير ▾", color = P.accent, fontSize = 13.sp)
    }
}

/** While a recitation plays and its page is closed: the verse, the surah, the reciter, pause and stop, at the bottom of every page. */
@Composable
fun MiniPlayer() {
    val (s, a) = Recitation.now ?: return
    if (Recitation.bars > 0) return
    Column(Modifier.fillMaxWidth().background(P.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph("🎙", tint = P.accent, size = 22.dp)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp).clip(L.shape(8.dp)).clickable { Nav.overlayQuran = s to a }) {
                Text("سورة ${Quran.surahs[s - 1].name} · الآية ${Quran.digits(a)}", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(if (Recitation.loading) "جارٍ التحميل…" else Recitation.reciterName() + if (Recitation.paused) " · متوقفة مؤقتًا" else "", color = P.muted, fontSize = 12.sp, maxLines = 1)
            }
            Box(Modifier.size(44.dp).clip(L.shape(12.dp)).background(P.accent).clickable { if (Recitation.paused) Recitation.resume() else Recitation.pause() }, contentAlignment = Alignment.Center) {
                Glyph(if (Recitation.paused) "▶" else "⏸", tint = P.onAccent, size = 24.dp)
            }
            Box(Modifier.padding(start = 6.dp).size(44.dp).clip(L.shape(12.dp)).background(P.card).clickable { Recitation.stop() }, contentAlignment = Alignment.Center) {
                Glyph("⏹", tint = P.ink, size = 24.dp)
            }
        }
    }
}
