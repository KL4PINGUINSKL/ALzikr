package com.alzikr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun shareText(c: Context, text: String) = c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "مشاركة"))
private fun copyText(c: Context, text: String) {
    c.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("ALzikr", text))
    Toast.makeText(c, "نُسخ", Toast.LENGTH_SHORT).show()
}

/** A small rounded-rectangle label (grade, size…). */
@Composable
private fun Badge(text: String, color: androidx.compose.ui.graphics.Color) =
    GText(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(L.shape(6.dp)).background(color.copy(alpha = .14f)).padding(horizontal = 8.dp, vertical = 3.dp))

// =============================================================================== explained hadith

/** Explained hadith: categories → hadiths → one hadith with its explanation. */
@Composable
fun SharhHome() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var cats by remember { mutableStateOf<List<Sharh.Category>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var parent by rememberSaveable { mutableStateOf<String?>(null) }
    var openCat by rememberSaveable { mutableStateOf<String?>(null) }
    var openHadith by rememberSaveable { mutableStateOf<String?>(null) }
    fun load(refresh: Boolean) = scope.launch {
        failed = false
        cats = withContext(Dispatchers.IO) { Sharh.categories(ctx, refresh) }
        failed = cats.isNullOrEmpty()
    }
    LaunchedEffect(Unit) { load(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    if (searching) { HadithSearchPage { searching = false }; return }
    openHadith?.let { id -> SharhPage(id) { openHadith = null }; return }
    openCat?.let { id -> SharhList(cats?.firstOrNull { it.id == id }?.title ?: "", id, open = { openHadith = it }) { openCat = null }; return }
    val all = cats
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SearchBar("ابحث عن حديث وشرحه…") { searching = true } }
        item { Hint("sharh", "💡", "أحاديث مشروحة من «موسوعة الأحاديث النبوية»: اختر موضوعًا، ثم حديثًا لترى شرحه وفوائده ودرجته.") }
        if (parent != null) item { Choice("‹ رجوع إلى المواضيع", false) { parent = null } }
        when {
            all == null -> item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent) } }
            failed -> item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Sub("تحتاج المرة الأولى إلى الإنترنت لتحميل المواضيع.")
                    Action("إعادة المحاولة", Modifier.padding(top = 10.dp)) { load(true) }
                }
            }
            else -> {
                val shown = all.filter { it.parent == parent }
                items(shown, key = { it.id }) { cat ->
                    val hasChildren = all.any { it.parent == cat.id }
                    Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, P.line, L.shape(14.dp))
                        .clickable { if (hasChildren) parent = cat.id else openCat = cat.id }.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Sub("${cat.count} حديث", size = 12.sp)
                        Text(if (hasChildren) "  ‹" else "", color = P.muted, fontSize = 18.sp)
                    }
                }
                if (parent != null) item { Action("كل أحاديث هذا الموضوع", Modifier.fillMaxWidth(), filled = false) { openCat = parent } }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Sub("المصدر: hadeethenc.com", Modifier.weight(1f), size = 11.sp)
                        Text("⟳ تحديث", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Sharh.clearCache(ctx); cats = null; load(true) }.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SharhList(title: String, category: String, open: (String) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val items = remember(category) { androidx.compose.runtime.mutableStateListOf<Sharh.Brief>() }
    var page by remember(category) { mutableStateOf(0) }
    var last by remember(category) { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    suspend fun more() {
        if (loading || page >= last) return
        loading = true
        withContext(Dispatchers.IO) { Sharh.list(ctx, category, page + 1) }?.let { items += it.items; page = it.page; last = it.last } ?: run { last = page }
        loading = false
    }
    LaunchedEffect(category) { more() }
    Subpage(title, back) {
        val shown = if (query.isBlank()) items else items.filter { Quran.normalize(it.title).contains(Quran.normalize(query)) }
        val state = rememberLazyListState()
        LaunchedEffect(state, query) { snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { if (query.isBlank() && it >= items.size - 5) more() } }
        LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(query, { query = it }, placeholder = { Text("ابحث في العناوين المحمّلة") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            items(shown, key = { it.id }) { h ->
                Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { open(h.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(h.title, color = P.ink, fontSize = 15.sp, fontFamily = Fonts.naskh, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                    if (Sharh.isFavorite(h.id)) Text(" ★", color = P.gold)
                }
            }
            if (loading) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent) } }
            if (!loading && items.isEmpty()) item { Sub("تعذّر التحميل. تحقق من الإنترنت وارجع.") }
        }
    }
}

/** Opens one explained hadith on its own (from favorites). */
@Composable
fun SharhOpen(id: String, back: () -> Unit) = SharhPage(id, back)

@Composable
private fun SharhPage(id: String, back: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var h by remember(id) { mutableStateOf<Sharh.Hadith?>(null) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) { h = withContext(Dispatchers.IO) { Sharh.hadith(ctx, id) }; failed = h == null }
    var readMode by remember(id) { mutableStateOf(false) }
    h?.takeIf { readMode }?.let { x ->
        val blocks = ArrayList<RBlock>()
        blocks += RBlock(RKind.HEAD, "الحديث")
        blocks += RBlock(RKind.BODY, x.text)
        listOf(x.grade.takeIf { it.isNotBlank() }?.let { "الدرجة: $it" }, x.attribution.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · ").takeIf { it.isNotBlank() }?.let { blocks += RBlock(RKind.SMALL, it) }
        blocks += RBlock(RKind.HEAD, "الشرح")
        x.explanation.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.forEach { blocks += RBlock(RKind.BODY, it) }
        if (x.hints.isNotEmpty()) { blocks += RBlock(RKind.HEAD, "من فوائد الحديث"); x.hints.forEach { blocks += RBlock(RKind.BODY, "• $it") } }
        x.words.filter { it.first.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { w -> blocks += RBlock(RKind.HEAD, "معاني الكلمات"); w.forEach { (a, m) -> blocks += RBlock(RKind.BODY, "$a: $m") } }
        if (x.reference.isNotBlank()) { blocks += RBlock(RKind.HEAD, "المراجع"); blocks += RBlock(RKind.SMALL, x.reference) }
        TextReading("شرح الحديث", "sharh:$id", blocks, 0) { readMode = false }
        return
    }
    Subpage("شرح الحديث", back) {
        val x = h
        if (x == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (failed) Sub("تعذّر التحميل، تحقق من الإنترنت.") else CircularProgressIndicator(color = P.accent) }; return@Subpage }
        val full = "${x.text}\n[${x.attribution} · ${x.grade}]\n\nالشرح: ${x.explanation}"
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(color = P.card) {
                Text(x.text, color = P.ink, fontSize = (19 * Data.textScale).sp, lineHeight = (34 * Data.textScale).sp, fontFamily = Fonts.naskh)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (x.grade.isNotBlank()) Badge("الدرجة: ${x.grade}", P.accent)
                    if (x.attribution.isNotBlank()) Badge(x.attribution, P.gold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(if (Sharh.isFavorite(x.id)) "★" else "☆", if (Sharh.isFavorite(x.id)) "في المفضلة" else "أضف للمفضلة", Modifier.weight(1f)) { Sharh.toggleFavorite(x.id) }
                Tile("📜", "القراءة", Modifier.weight(1f)) { readMode = true }
                Tile("⧉", "نسخ", Modifier.weight(1f)) { copyText(ctx, full) }
                Tile("↗", "مشاركة", Modifier.weight(1f)) { shareText(ctx, full) }
            }
            Title("الشرح")
            Text(x.explanation, color = P.ink, fontSize = (16 * Data.textScale).sp, lineHeight = (28 * Data.textScale).sp, fontFamily = Fonts.naskh)
            if (x.hints.isNotEmpty()) {
                Title("من فوائد الحديث")
                x.hints.forEach { Text("• $it", color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (26 * Data.textScale).sp, fontFamily = Fonts.naskh) }
            }
            val words = x.words.filter { it.first.isNotBlank() }
            if (words.isNotEmpty()) {
                Title("معاني الكلمات")
                words.forEach { (w, m) -> Text("$w: $m", color = P.ink, fontSize = 15.sp, lineHeight = 24.sp, fontFamily = Fonts.naskh) }
            }
            if (x.reference.isNotBlank()) {
                Title("المراجع")
                Sub(x.reference, size = 12.sp)
            }
        }
    }
}

// =============================================================================== Islamic books

/** Islamic books: my shelf, the newest books, and the category tree; download a PDF and read it here. */
@Composable
fun BooksHome(root: String? = null, rootTitle: String = "") {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE") val bv = Books.version
    var path by rememberSaveable { mutableStateOf(listOf<String>()) }       // category ids from the root
    var titles by rememberSaveable { mutableStateOf(listOf<String>()) }
    var cats by remember { mutableStateOf<List<Books.Category>?>(null) }
    var reading by rememberSaveable { mutableStateOf<String?>(null) }
    var readingTitle by rememberSaveable { mutableStateOf("") }
    var refreshTick by remember { mutableStateOf(0) }
    val current = path.lastOrNull() ?: root
    val currentTitle = titles.lastOrNull() ?: rootTitle.ifBlank { null }
    LaunchedEffect(current, refreshTick) { cats = null; cats = withContext(Dispatchers.IO) { Books.categories(ctx, current ?: "397053", refreshTick > 0) } }
    reading?.let { id -> PdfReader(id, readingTitle) { reading = null }; return }
    val list = remember(current) { androidx.compose.runtime.mutableStateListOf<Books.Book>() }
    var page by remember(current) { mutableStateOf(0) }
    var pages by remember(current) { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Books.Book?>(null) }
    suspend fun more() {
        if (loading || page >= pages) return
        loading = true
        withContext(Dispatchers.IO) { Books.books(ctx, current, page + 1, refreshTick > 0) }?.let { list += it.books; page = it.page; pages = it.pages } ?: run { pages = page }
        loading = false
    }
    LaunchedEffect(current, refreshTick) { list.clear(); page = 0; pages = 1; more() }
    val state = rememberLazyListState()
    LaunchedEffect(state, current, query) { snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { if (query.isBlank() && it >= state.layoutInfo.totalItemsCount - 4) more() } }
    val shelf = remember(Books.version) { Books.shelf(ctx) }
    LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (root == null) item { Hint("books", "📚", "كتب أهل السنة والجماعة من «دار الإسلام». اختر تصنيفًا أو كتابًا، ثم «تنزيل» لقراءته داخل التطبيق، ويبقى في «كتبي» بلا إنترنت.") }
        if (path.isEmpty() && root == null) item { ImportPdfTile(Modifier.fillMaxWidth()) }
        if (path.isEmpty() && root == null && shelf.isNotEmpty()) {
            item { Title("كتبي") }
            items(shelf, key = { "shelf" + it.id }) { b ->
                Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card).clickable { readingTitle = b.title; reading = b.id }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    GText("📖", fontSize = 20.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(b.title, color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Sub((if (Books.lastPage(b.id) > 0) "وصلت إلى الصفحة ${Books.lastPage(b.id) + 1} · " else "") + b.authors, size = 11.sp)
                    }
                    Text("حذف", color = P.danger, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Books.delete(ctx, b.id) }.padding(6.dp))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Title(if (currentTitle != null) "أقسام «$currentTitle»" else "التصنيفات", Modifier.weight(1f))
                Text("⟳ تحديث", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Books.clearCache(ctx); refreshTick++ }.padding(8.dp))
            }
            if (path.isNotEmpty()) Choice("‹ رجوع", false) { path = path.dropLast(1); titles = titles.dropLast(1) }
        }
        val cs = cats
        if (cs == null) item { Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent) } }
        else if (cs.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cs.forEach { c -> Choice(c.title, false) { path = path + c.id; titles = titles + c.title } }
            }
        }
        item {
            Title(if (current == null) "أحدث الكتب" else "الكتب في «$currentTitle»", Modifier.padding(top = 8.dp))
            OutlinedTextField(query, { query = it }, placeholder = { Text("ابحث في الكتب المعروضة") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        val shown = if (query.isBlank()) list else list.filter { Quran.normalize(it.title + " " + it.authors).contains(Quran.normalize(query)) }
        items(shown, key = { it.id }) { b ->
            Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, P.line, L.shape(14.dp)).clickable { chosen = b }.padding(14.dp)) {
                Text(b.title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = Fonts.naskh)
                if (b.authors.isNotBlank()) Sub(b.authors, size = 12.sp, color = P.gold)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (Books.has(ctx, b.id)) Badge("✓ على الهاتف", P.accent) else if (b.pdf != null) Badge("PDF · ${b.size}", P.muted)
                }
            }
        }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent) } }
        if (!loading && list.isEmpty() && cs != null) item { Sub("لا توجد كتب هنا، أو لا يوجد اتصال بالإنترنت.") }
        item { Sub("المصدر: IslamHouse.com (دار الإسلام).", Modifier.padding(6.dp), size = 11.sp) }
    }
    chosen?.let { b ->
        val have = Books.has(ctx, b.id)
        AlertDialog(onDismissRequest = { chosen = null }, containerColor = P.surface,
            title = { Text(b.title, color = P.ink, fontSize = 17.sp) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (b.authors.isNotBlank()) Sub(b.authors, color = P.gold)
                    Text(b.description, color = P.ink, fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp))
                    if (Books.downloading == b.id) Sub("جارٍ التنزيل… ${(Books.progress * 100).toInt()}%", Modifier.padding(top = 8.dp), color = P.accent)
                    if (b.pdf == null) Sub("لا يوجد ملف PDF لهذا الكتاب.", Modifier.padding(top = 8.dp), color = P.danger)
                }
            },
            confirmButton = {
                if (have) TextButton({ readingTitle = b.title; reading = b.id; chosen = null }) { Text("اقرأ الآن") }
                else if (b.pdf != null && Books.downloading == null) TextButton({
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { Books.download(ctx, b) }
                        if (ok) { readingTitle = b.title; reading = b.id; chosen = null } else Toast.makeText(ctx, "تعذّر التنزيل، تحقق من الإنترنت", Toast.LENGTH_LONG).show()
                    }
                }) { Text("تنزيل (${b.size}) ثم القراءة") }
            },
            dismissButton = { TextButton({ chosen = null }) { Text("إغلاق") } })
    }
}

/** Reads a downloaded PDF: pages drawn by Android's PdfRenderer, zoom, and the last page remembered. */
@Composable
private fun PdfReader(id: String, title: String, back: () -> Unit) {
    val ctx = LocalContext.current
    val renderer = remember(id) { runCatching { PdfRenderer(ParcelFileDescriptor.open(Books.file(ctx, id), ParcelFileDescriptor.MODE_READ_ONLY)) }.getOrNull() }
    DisposableEffect(id) { onDispose { runCatching { renderer?.close() } } }
    var zoom by rememberSaveable { mutableStateOf(1f) }
    var goTo by remember { mutableStateOf(false) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = Books.lastPage(id))
    val scope = rememberCoroutineScope()
    LaunchedEffect(id) { snapshotFlow { state.firstVisibleItemIndex }.collect { Books.setLastPage(id, it) } }
    var readMode by rememberSaveable(id) { mutableStateOf(false) }
    if (readMode && renderer != null) { PdfReading(renderer, id, title) { readMode = false }; return }
    Subpage(title, back) {
        if (renderer == null) { Sub("تعذّر فتح الملف. احذفه ونزّله من جديد.", Modifier.padding(16.dp)); return@Subpage }
        val count = renderer.pageCount
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("📜 وضع القراءة", false) { readMode = true }
                Choice("ص ${state.firstVisibleItemIndex + 1} / $count", false) { goTo = true }
                Choice("−", false) { zoom = (zoom - .25f).coerceAtLeast(1f) }
                Choice("${(zoom * 100).toInt()}٪", false) { zoom = 1f }
                Choice("+", false) { zoom = (zoom + .25f).coerceAtMost(3f) }
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val widthPx = with(LocalDensity.current) { (maxWidth * zoom).roundToPx() }
                val pageWidth = maxWidth * zoom
                LazyColumn(Modifier.fillMaxSize().let { if (zoom > 1f) it.horizontalScroll(rememberScrollState()) else it }, state = state,
                    contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(count) { i -> PdfPage(renderer, i, widthPx, Modifier.width(pageWidth)) }
                }
            }
        }
    }
    if (goTo) {
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { goTo = false }, containerColor = P.surface, title = { Text("انتقل إلى صفحة", color = P.ink) },
            text = { OutlinedTextField(text, { text = it.filter(Char::isDigit).take(5) }, singleLine = true, placeholder = { Text("1 – ${renderer?.pageCount ?: 1}") }) },
            confirmButton = { TextButton({ text.toIntOrNull()?.let { n -> scope.launch { state.scrollToItem((n - 1).coerceIn(0, (renderer?.pageCount ?: 1) - 1)) } }; goTo = false }) { Text("انتقل") } },
            dismissButton = { TextButton({ goTo = false }) { Text("إلغاء") } })
    }
}

private val renderLock = Any()

@Composable
private fun PdfPage(renderer: PdfRenderer, index: Int, widthPx: Int, modifier: Modifier, filter: androidx.compose.ui.graphics.ColorFilter? = null, paper: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White) {
    var bitmap by remember(index, widthPx) { mutableStateOf<Bitmap?>(null) }
    var ratio by remember(index) { mutableStateOf(1.414f) }
    LaunchedEffect(index, widthPx) {
        bitmap = withContext(Dispatchers.IO) {
            synchronized(renderLock) {
                runCatching {
                    renderer.openPage(index).use { p ->
                        ratio = p.height.toFloat() / p.width
                        val w = widthPx.coerceAtMost(2400)
                        Bitmap.createBitmap(w, (w * ratio).toInt(), Bitmap.Config.ARGB_8888).also { b ->
                            b.eraseColor(android.graphics.Color.WHITE)
                            p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }.getOrNull()
            }
        }
    }
    Box(modifier.aspectRatio(1f / ratio).background(paper), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), "صفحة ${index + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth, colorFilter = filter) } ?: CircularProgressIndicator(Modifier.size(24.dp), color = P.accent)
    }
}


/** The Islamic sciences, each opening its books on IslamHouse; plus all books and my shelf. */
data class Science(val id: String, val name: String, val icon: String, val about: String)
val sciences = listOf(
    Science("192525", "العقيدة", "🕋", "التوحيد والإيمان وأركانه ومنهج أهل السنة والجماعة"),
    Science("728011", "التفسير", "📖", "تفاسير القرآن وأصول التفسير"),
    Science("192742", "علوم القرآن", "✨", "نزول القرآن وجمعه وإعجازه والقراءات"),
    Science("152181", "التجويد", "🎙", "أحكام التلاوة ومتونها"),
    Science("144409", "كتب السنة", "📜", "دواوين الحديث"),
    Science("344947", "شروح السنة", "💡", "شروح كتب الحديث"),
    Science("728012", "علوم الحديث", "🔎", "مصطلح الحديث والتخريج وتراجم الرجال"),
    Science("199946", "الفقه", "⚖", "العبادات والمعاملات والأسرة والفتاوى"),
    Science("206212", "أصول الفقه", "🧭", "قواعد استنباط الأحكام"),
    Science("795", "السيرة النبوية", "🌙", "حياة النبي ﷺ"),
    Science("192527", "الآداب والأخلاق", "🤝", "الآداب الإسلامية والأخلاق"),
    Science("200217", "الرقائق والتزكية", "💚", "ما يرقّق القلب ويزكّي النفس"),
    Science("276190", "اللغة العربية", "ض", "النحو والصرف وعلوم العربية"),
    Science("192529", "المتون العلمية", "📘", "متون العقيدة والفقه والحديث والنحو للحفظ والدراسة"),
    Science("5301", "التاريخ الإسلامي", "🏛", "الحضارة والبلدان والأعلام"),
)

@Composable
fun SciencesHome(openBooks: (String, String) -> Unit, openAll: () -> Unit) {
    val ctx = LocalContext.current
    val shelf = remember(Books.version) { Books.shelf(ctx) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Hint("sciences", "🎓", "اختر علمًا لترى كتبه، أو «كل الكتب» لتصفح المكتبة كاملة. الكتب المنزّلة تجدها في «كتبي».") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile("📚", "كل الكتب", Modifier.weight(1f)) { openAll() }
                Tile("📖", if (shelf.isEmpty()) "كتبي (فارغة)" else "كتبي (${shelf.size})", Modifier.weight(1f)) { openAll() }
            }
            ImportPdfTile(Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        item { Title("العلوم الإسلامية", Modifier.padding(top = 6.dp)) }
        items(sciences.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { sc ->
                    Column(Modifier.weight(1f).clip(L.shape(16.dp)).background(P.surface).border(1.dp, P.line, L.shape(16.dp)).clickable { openBooks(sc.id, sc.name) }.padding(12.dp)) {
                        Text(sc.icon, fontSize = 22.sp)
                        Text(sc.name, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = L.head ?: L.body)
                        Text(sc.about, color = P.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
        item { Sub("الكتب من دار الإسلام (IslamHouse.com) لأهل السنة والجماعة.", Modifier.padding(6.dp), size = 11.sp) }
    }
}

/** A PDF book in reading mode: full screen, the paper's tint (or night inversion), auto-scroll, resume. */
@Composable
private fun PdfReading(renderer: PdfRenderer, id: String, title: String, back: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = Books.lastPage(id))
    LaunchedEffect(id) { snapshotFlow { state.firstVisibleItemIndex }.collect { Books.setLastPage(id, it) } }
    val count = renderer.pageCount
    var goTo by remember { mutableStateOf(false) }
    val paper = Reading.paper
    val bg = androidx.compose.ui.graphics.Color(paper.bg)
    ReadingShell(title, "صفحة ${state.firstVisibleItemIndex + 1} من $count", back, progress = (state.firstVisibleItemIndex + 1f) / count, scroll = state, textual = false,
        tiles = { Tile("📑", "انتقل إلى", Modifier.weight(1f)) { goTo = true } }) { toggle ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            LazyColumn(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { toggle() }) }, state = state,
                contentPadding = PaddingValues(vertical = 30.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(count) { i -> PdfPage(renderer, i, widthPx, Modifier.fillMaxWidth(), paper.pdfFilter(), bg) }
            }
        }
    }
    if (goTo) {
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { goTo = false }, containerColor = P.surface, title = { Text("انتقل إلى صفحة", color = P.ink) },
            text = { OutlinedTextField(text, { text = it.filter(Char::isDigit).take(5) }, singleLine = true, placeholder = { Text("1 – $count") }) },
            confirmButton = { TextButton({ text.toIntOrNull()?.let { n -> scope.launch { state.scrollToItem((n - 1).coerceIn(0, count - 1)) } }; goTo = false }) { Text("انتقل") } },
            dismissButton = { TextButton({ goTo = false }) { Text("إلغاء") } })
    }
}

/** «➕ إضافة كتاب PDF من هاتفك»: pick a PDF, it joins "my books" and opens in the reader and reading mode. */
@Composable
fun ImportPdfTile(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val title = withContext(Dispatchers.IO) { Books.importPdf(ctx, uri) }
            busy = false
            Toast.makeText(ctx, if (title != null) "أُضيف إلى «كتبي»: $title" else "هذا الملف ليس PDF أو تعذّرت قراءته", Toast.LENGTH_LONG).show()
        }
    }
    Column(modifier) {
        Tile("➕", if (busy) "جارٍ الإضافة…" else "إضافة كتاب PDF من هاتفك", Modifier.fillMaxWidth()) { if (!busy) pick.launch(arrayOf("application/pdf")) }
        Sub("نزّل الكتاب من موقع ناشره (مثل موقع مؤسسة الشيخ ابن عثيمين) ثم أضفه هنا، فيظهر في «كتبي» ويُفتح في القارئ ووضع القراءة.", Modifier.padding(top = 4.dp), size = 11.sp)
    }
}
