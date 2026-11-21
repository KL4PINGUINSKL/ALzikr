package com.alzikr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material3.Text
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
private fun copy(c: Context, text: String) {
    c.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("ALzikr", text))
    Toast.makeText(c, "نُسخ", Toast.LENGTH_SHORT).show()
}

/** The Library tab: the Quran and the hadith books. Opens a reader when [Nav] asks for one. */
@Composable
fun LibraryScreen() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    Nav.quran?.let { (s, a) -> if (Quran.ready) QuranReader(s, a) { Nav.quran = null } else Loading(); return }
    Nav.hadith?.let { (b, n) -> HadithReader(b, n) { Nav.hadith = null }; return }
    var section by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("القرآن الكريم", "الحديث", "المفضلة").forEachIndexed { i, t -> Choice(t, section == i) { section = i } }
        }
        Box(Modifier.weight(1f)) {
            when (section) {
                0 -> if (Quran.ready) QuranHome() else Loading()
                1 -> HadithHome()
                else -> Favorites()
            }
        }
    }
}

@Composable private fun Loading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.accent) }

// =============================================================================== Quran

@Composable
private fun QuranHome() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var query by rememberSaveable { mutableStateOf("") }
    var byJuz by rememberSaveable { mutableStateOf(false) }
    val q = Quran.normalize(query)
    val surahs = if (q.isEmpty()) Quran.surahs else Quran.surahs.filter { Quran.normalize(it.name).contains(q) || it.n.toString() == q }
    val hits = remember(q) { if (q.length >= 3) Quran.search(q) else emptyList() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Quran.last?.let { (s, a) ->
                Panel(Modifier.clickable { Nav.openQuran(s, a) }, color = P.card) {
                    Sub("متابعة القراءة")
                    Text("سورة ${Quran.surahs[s - 1].name} · الآية ${Quran.digits(a)}", color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = L.head)
                    Quran.at(s, a)?.let { Sub("صفحة ${it.page} · الجزء ${it.juz}", size = 12.sp) }
                }
            }
        }
        item {
            OutlinedTextField(query, { query = it }, placeholder = { Text("ابحث عن سورة أو كلمة في القرآن") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("السور", !byJuz) { byJuz = false }; Choice("الأجزاء", byJuz) { byJuz = true }
            }
        }
        if (byJuz && q.isEmpty()) items((1..30).toList()) { j ->
            val a = Quran.firstOfJuz(j)
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { Nav.openQuran(a.s, a.a) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("الجزء ${Quran.digits(j)}", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Sub("${Quran.surahs[a.s - 1].name} ${Quran.digits(a.a)} · ص ${a.page}", size = 12.sp)
            }
        } else items(surahs, key = { it.n }) { s ->
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { Nav.openQuran(s.n, 1) }.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun QuranReader(surah: Int, startAyah: Int, back: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val scope = rememberCoroutineScope()
    var s by remember(surah) { mutableStateOf(surah) }
    val info = Quran.surahs[s - 1]
    val list = Quran.ofSurah(s)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = if (s == surah) (startAyah - 1).coerceAtLeast(0) else 0)
    var chosen by remember { mutableStateOf<Quran.Ayah?>(null) }
    val size = Quran.fontSize
    // Remember where reading stopped.
    LaunchedEffect(s) { snapshotFlow { state.firstVisibleItemIndex }.collect { i -> list.getOrNull(i)?.let { Quran.last = it.s to it.a } } }
    Subpage("سورة ${info.name}", back) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Sub("${if (info.meccan) "مكية" else "مدنية"} · ${Quran.digits(info.count)} آية", Modifier.weight(1f), size = 12.sp)
                Choice("ا−", false) { Quran.fontSize = size - 2 }
                Box(Modifier.width(6.dp))
                Choice("ا+", false) { Quran.fontSize = size + 2 }
            }
            LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                if (s != 1 && s != 9) item {
                    Text(Quran.BASMALA, color = P.accent, fontSize = (size + 2).sp, fontFamily = Fonts.quran, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                }
                itemsIndexed(list, key = { _, a -> a.a }) { i, a ->
                    if (i > 0 && list[i - 1].page != a.page) Text("— صفحة ${Quran.digits(a.page)} · الجزء ${Quran.digits(a.juz)} —", color = P.muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    val marked = Quran.marked(a.s, a.a)
                    Text(a.text + (if (a.sajda) " ۩" else "") + " ﴿${Quran.digits(a.a)}﴾", color = P.ink, fontSize = size.sp, lineHeight = (size * 1.9f).sp, fontFamily = Fonts.quran,
                        textAlign = if (L.id == "mushaf") TextAlign.Justify else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).background(if (marked) P.gold.copy(alpha = .14f) else P.bg).clickable { chosen = a }.padding(horizontal = 6.dp, vertical = 4.dp))
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (s > 1) Choice("‹ ${Quran.surahs[s - 2].name}", false) { s -= 1; scope.launch { state.scrollToItem(0) } } else Box(Modifier)
                        if (s < 114) Choice("${Quran.surahs[s].name} ›", false) { s += 1; scope.launch { state.scrollToItem(0) } }
                    }
                }
            }
        }
    }
    chosen?.let { a -> AyahDialog(a) { chosen = null } }
}

@Composable
private fun AyahDialog(a: Quran.Ayah, close: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ref = "سورة ${Quran.surahs[a.s - 1].name}، الآية ${a.a}"
    var tafsir by remember { mutableStateOf(Quran.tafsir(ctx, "jalalayn", a.s, a.a)) }
    AlertDialog(onDismissRequest = close, containerColor = P.surface,
        title = { Text(ref, color = P.ink, fontSize = 16.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(a.text, color = P.ink, fontSize = 20.sp, fontFamily = Fonts.quran, lineHeight = 36.sp)
                Sub("صفحة ${a.page} · الجزء ${a.juz}${if (a.sajda) " · موضع سجدة" else ""}", Modifier.padding(top = 6.dp), size = 12.sp)
                Title("تفسير الجلالين", Modifier.padding(top = 12.dp))
                when {
                    tafsir != null -> Text(tafsir!!, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = 26.sp, fontFamily = Fonts.naskh)
                    Quran.downloading != null -> Sub("جارٍ التنزيل… ${(Quran.progress * 100).toInt()}%")
                    else -> Text("تنزيل التفسير (مرة واحدة، نحو 2 ميغابايت)", color = P.accent, fontSize = 14.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { Quran.downloadTafsir(ctx, Quran.tafasir.first()) }
                            if (ok) tafsir = Quran.tafsir(ctx, "jalalayn", a.s, a.a) else Toast.makeText(ctx, "تعذّر التنزيل، تحقق من الإنترنت", Toast.LENGTH_LONG).show()
                        }
                    }.padding(vertical = 6.dp))
                }
                Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Choice(if (Quran.marked(a.s, a.a)) "★ إزالة العلامة" else "☆ علامة", false) { Quran.toggleMark(a.s, a.a) }
                    Choice("نسخ", false) { copy(ctx, "${a.text}\n[$ref]") }
                    Choice("مشاركة", false) { share(ctx, "${a.text}\n[$ref]") }
                    Choice("وصلت هنا في الختمة", false) { Data.khatmaPage = a.page; if (Data.khatmaStarted.isEmpty()) Data.khatmaStarted = java.time.LocalDate.now().toString(); Toast.makeText(ctx, "الختمة: الصفحة ${a.page}", Toast.LENGTH_SHORT).show() }
                }
            }
        },
        confirmButton = { TextButton(close) { Text("إغلاق") } })
}

// =============================================================================== Hadith

@Composable
private fun HadithHome() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE") val v = Hadith.version
    var remove by remember { mutableStateOf<Hadith.Info?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        AlertDialog(onDismissRequest = { remove = null }, containerColor = P.surface, title = { Text("حذف «${b.title}» من الهاتف؟", color = P.ink) },
            text = { Sub("يمكنك تنزيله مرة أخرى متى شئت.") },
            confirmButton = { TextButton({ Hadith.delete(ctx, b.id); remove = null }) { Text("حذف", color = P.danger) } },
            dismissButton = { TextButton({ remove = null }) { Text("إلغاء") } })
    }
}

@Composable
private fun HadithReader(bookId: String, number: Int, back: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var book by remember(bookId) { mutableStateOf<Hadith.Book?>(null) }
    LaunchedEffect(bookId) { book = withContext(Dispatchers.IO) { Hadith.load(ctx, bookId) } }
    var chapter by rememberSaveable(bookId) { mutableStateOf<String?>(null) }
    var query by rememberSaveable(bookId) { mutableStateOf("") }
    val b = book
    Subpage(Hadith.info(bookId)?.title ?: "", { if (chapter != null && query.isEmpty()) chapter = null else back() }) {
        if (b == null) { Loading(); return@Subpage }
        val q = Hadith.normalize(query)
        val manyChapters = b.chapters.size > 1
        val shown = when {
            q.length >= 2 -> remember(q, b) { b.items.filter { Hadith.normalize(it.text).contains(q) }.take(300) }
            number > 0 && chapter == null && !manyChapters -> b.items
            chapter != null -> b.items.filter { it.chapter == chapter }
            !manyChapters -> b.items
            else -> null
        }
        val start = remember(b, number) { if (number > 0) b.items.indexOfFirst { it.number == number.toString() }.coerceAtLeast(0) else 0 }
        val state = rememberLazyListState(initialFirstVisibleItemIndex = if (number > 0 && shown === b.items) start + 1 else 0)
        LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(query, { query = it }, placeholder = { Text("ابحث في الكتاب") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
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
                items(shown, key = { "${it.chapter}-${it.number}-${it.text.length}" }) { h -> HadithCard(b, h) }
            }
        }
    }
}

@Composable
private fun HadithCard(b: Hadith.Book, h: Hadith.Item) {
    val ctx = LocalContext.current
    val fav = Hadith.isFavorite(b.info.id, h.number)
    val ref = "${b.info.title}، رقم ${h.number}"
    Panel(pad = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Quran.digits(h.number.toIntOrNull() ?: 0).takeIf { h.number.toIntOrNull() != null } ?: h.number, color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (fav) "★" else "☆", color = if (fav) P.gold else P.muted, fontSize = 18.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Hadith.toggleFavorite(b.info.id, h.number) }.padding(horizontal = 8.dp))
            Text("نسخ", color = P.muted, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { copy(ctx, "${h.text}\n[$ref]") }.padding(horizontal = 8.dp, vertical = 4.dp))
            Text("مشاركة", color = P.muted, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { share(ctx, "${h.text}\n[$ref]") }.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Text(h.text, color = P.ink, fontSize = (17 * Data.textScale).sp, lineHeight = (30 * Data.textScale).sp, fontFamily = Fonts.naskh)
    }
}

// =============================================================================== favorites

@Composable
private fun Favorites() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val marks = if (Quran.ready) Quran.bookmarks() else emptyList()
    val favs = Hadith.favorites()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Title("علامات المصحف") }
        if (marks.isEmpty()) item { Sub("اضغط على أي آية ثم «علامة» لتجدها هنا.", size = 12.sp) }
        items(marks) { (s, a) ->
            val ay = Quran.at(s, a) ?: return@items
            Column(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).clickable { Nav.openQuran(s, a) }.padding(12.dp)) {
                Text(ay.text, color = P.ink, fontSize = 18.sp, fontFamily = Fonts.quran, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 32.sp)
                Sub("${Quran.surahs[s - 1].name} ${Quran.digits(a)}", size = 12.sp, color = P.gold)
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

/** "Verse of the day" and "hadith of the day", for the prayer screen. */
@Composable
fun DailyWords() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Quran.load(ctx) } }
    val ayah = if (Quran.ready) Quran.ofTheDay() else null
    val hadith = remember { runCatching { Hadith.ofTheDay(ctx) }.getOrNull() }
    if (ayah != null) Panel(Modifier.clickable { Nav.openQuran(ayah.s, ayah.a) }) {
        Sub("آية اليوم" + if (L.ornament.isNotEmpty()) " ${L.ornament}" else "", color = P.gold)
        Text(ayah.text, color = P.ink, fontSize = 20.sp, fontFamily = Fonts.quran, lineHeight = 36.sp, modifier = Modifier.padding(top = 4.dp))
        Sub("${Quran.surahs[ayah.s - 1].name} ${Quran.digits(ayah.a)}", size = 12.sp)
    }
    if (hadith != null) Panel(Modifier.clickable { Nav.openHadith(hadith.first.id, hadith.second.number.toIntOrNull() ?: 0) }) {
        Sub("حديث اليوم · ${hadith.first.title}", color = P.gold)
        Text(hadith.second.text, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = 26.sp, fontFamily = Fonts.naskh, maxLines = 6, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

