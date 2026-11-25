package com.alzikr.app

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * One search over everything in the app: its pages and settings, the surahs and every verse, the adhkar and duas,
 * the stories, Fiqh and the seasons, the names of Allah, the hadith books on the phone, the explained-hadith
 * encyclopedia, and every Islamic book title. Opened from the 🔍 in the header.
 */
object GlobalSearch {
    enum class Kind(val label: String, val icon: String) {
        PAGE("في التطبيق", "🧭"), SURAH("السور", "📖"), AYAH("في آيات القرآن", "📖"), DHIKR("الأذكار والأدعية", "📿"),
        STORY("القصص", "🌿"), FIQH("الأحكام الفقهية", "⚖"), SEASON("المواسم", "🌙"), NAME("الأسماء الحسنى", "✨"),
        SHARH("الأحاديث المشروحة", "💡"), HADITH("في كتب الحديث", "📜"), BOOK("الكتب", "📚"),
    }
    class Hit(val kind: Kind, val title: String, val sub: String = "", val go: () -> Unit)

    private fun more(page: String) = { Nav.morePage = page; Nav.go("more") }
    private fun settings(section: String) = { Nav.settingsReq = section }

    /** The app's own pages, with the words people may use for them. */
    private val pages: List<Triple<String, String, () -> Unit>> = listOf(
        Triple("القبلة", "قبله بوصله اتجاه الكعبه", more("qibla")), Triple("مواقيت الشهر", "جدول اوقات الصلاه شهر", more("month")),
        Triple("التقويم والمناسبات", "تقويم هجري مناسبات", more("calendar")), Triple("أسماء الشهور ومعانيها", "شهور اشهر", more("months")),
        Triple("مواسم الطاعات", "مواسم رمضان الحج عرفه", more("seasons")), Triple("الأسماء الحسنى", "اسماء الله", more("names")),
        Triple("قضاء الصلوات", "قضاء فوائت", more("qada")), Triple("حاسبة الزكاة", "زكاه ذهب فضه مال", more("zakat")),
        Triple("مسابقة القرآن", "مسابقه اسئله", more("quiz")), Triple("محوّل التاريخ", "تحويل هجري ميلادي", more("convert")),
        Triple("المنبه", "منبه تنبيه استيقاظ", more("alarms")), Triple("إحصائياتي", "احصائيات", more("stats")),
        Triple("حفظ القرآن", "حفظ مراجعه", more("hifz")), Triple("صديقي", "صديق حيوان مستوى", more("pet")),
        Triple("صدقاتي", "صدقه", more("sadaqa")), Triple("رمضان", "صيام سحور افطار", more("ramadan")),
        Triple("محاسبة اليوم", "محاسبه", more("today")), Triple("أهدافي", "اهداف", more("goals")),
        Triple("صفة الوضوء والصلاة", "وضوء صلاه صفه", more("salah")), Triple("تعلّم التجويد", "تجويد احكام", more("tajweed")),
        Triple("الأحكام الفقهية", "فقه احكام", more("fiqh")), Triple("قسم الأطفال", "اطفال صغار حروف", more("kidszone")),
        Triple("الساعة العالمية", "ساعه مدن", more("world")), Triple("الثيمات", "ثيم الوان شكل ليلي نهاري", more("themes")),
        Triple("نسخ احتياطي", "نسخ احتياطي استعاده", more("backup")), Triple("البحث عن حديث", "حديث بحث", more("hsearch")),
        Triple("يومي: اليوم", "يوم جدول انجاز ملخص", { Nav.daily = Daily.TODAY; Nav.go("daily") }), Triple("المهام", "مهام مهمه قائمه تذكير", { Nav.daily = Daily.TASKS; Nav.go("daily") }),
        Triple("العادات", "عادات عاده", { Nav.daily = Daily.HABITS; Nav.go("daily") }), Triple("الختمة والورد", "ختمه ورد صفحات", { Nav.daily = Daily.KHATMA; Nav.go("daily") }),
        Triple("جلسة التركيز", "تركيز مؤقت جلسه", { Nav.daily = Daily.FOCUS; Nav.go("daily") }), Triple("ملاحظاتي", "ملاحظات ملاحظه", { Nav.daily = Daily.NOTES; Nav.go("daily") }),
        Triple("الموقع وطريقة الحساب", "موقع مدينه حساب طريقه تعديل الاوقات", settings("times")),
        Triple("التنبيهات والأذان", "تنبيه اذان مؤذن صوت اشعار", settings("alerts")),
        Triple("الصلاة والتركيز", "صلاه تركيز", settings("prayer")),
        Triple("راحة العين", "نور الليل خط حجم ارقام هجري عين", settings("look")),
        Triple("التلاوة والقارئ", "قارئ تلاوه استماع", settings("recite")),
        Triple("المساعدة", "مساعده شرح", settings("help")), Triple("عن التطبيق", "تحديث نسخه اصدار", settings("about")),
    )

    /** Everything kept in the app itself (instant). */
    fun quick(q: String): List<Hit> {
        val m = HadithSearch.matcher(q)
        val out = ArrayList<Hit>()
        pages.filter { (t, k, _) -> m(t + " " + k) }.forEach { (t, _, go) -> out += Hit(Kind.PAGE, t, "", go) }
        if (Quran.ready) Quran.jumps(q).forEach { j ->
            out += Hit(Kind.SURAH, j.label, if (j.surahOnly) "${Quran.surahs[j.s - 1].count} آية" else "") { Nav.openQuran(j.s, j.a) }
        }
        if (Quran.ready && Quran.normalize(q).length >= 2) Quran.surahs.filter { m(it.name) && out.none { h -> h.kind == Kind.SURAH && h.title.contains(it.name) } }.forEach { s ->
            out += Hit(Kind.SURAH, "سورة ${s.name}", "${if (s.meccan) "مكية" else "مدنية"} · ${Quran.digits(s.count)} آية") { Nav.openQuran(s.n, 1) }
        }
        (Content.collections + Content.afterPrayer).forEach { c ->
            if (m(c.title + " " + c.subtitle)) out += Hit(Kind.DHIKR, c.title, c.subtitle) { Nav.collection = c.id; Nav.go("adhkar") }
            c.items.filter { m(it.text) }.take(3).forEach { i -> out += Hit(Kind.DHIKR, i.text, "من «${c.title}»") { Nav.collection = c.id; Nav.go("adhkar") } }
        }
        Stories.quran.forEachIndexed { i, st -> if (m(st.title + " " + st.intro)) out += Hit(Kind.STORY, st.title, "قصص القرآن") { Nav.story = i; Nav.library = 1; Nav.go("library") } }
        Stories.islamic.forEach { (_, name, about) -> if (m("$name $about")) out += Hit(Kind.STORY, name, about) { Nav.library = 1; Nav.go("library") } }
        Fiqh.topics.filter { m(it.title + " " + it.intro) }.forEach { t -> out += Hit(Kind.FIQH, t.title, t.intro.take(80), more("fiqh:${t.id}")) }
        Fiqh.search(q).take(20).forEach { h -> out += Hit(Kind.FIQH, h.item.text, "${h.topic.title} · ${h.sec.title}", more("fiqh:${h.topic.id}")) }
        Seasons.all.filter { m(it.title + " " + it.about) }.forEach { g -> out += Hit(Kind.SEASON, g.title, g.about.take(80), more("season:${g.id}")) }
        Content.names.forEachIndexed { i, n -> if (m(n)) out += Hit(Kind.NAME, n, "من الأسماء الحسنى (${Quran.digits(i + 1)})", more("names")) }
        return out
    }

    fun ayahs(q: String): List<Hit> = if (!Quran.ready || Quran.normalize(q).length < 3) emptyList() else Quran.search(q, 200).map { a ->
        Hit(Kind.AYAH, a.text, "سورة ${Quran.surahs[a.s - 1].name} · الآية ${Quran.digits(a.a)}") { Nav.openQuran(a.s, a.a) }
    }

    fun sharh(c: Context, q: String): List<Hit> = HadithSearch.searchSharh(c, q, 100).mapNotNull { h ->
        val id = h.sharhId ?: return@mapNotNull null
        Hit(Kind.SHARH, h.text, "موسوعة الأحاديث المشروحة") { Nav.sharh = id; Nav.go("library") }
    }

    fun hadith(c: Context, q: String): List<Hit> = HadithSearch.searchBooks(c, q, 150).map { h ->
        Hit(Kind.HADITH, h.text.replace('\n', ' ').take(220), "${h.bookTitle} · ${h.number}") { Nav.openHadith(h.book, h.number.toIntOrNull() ?: 0) }
    }

    fun books(c: Context, q: String): List<Hit> {
        val m = HadithSearch.matcher(q)
        val mine = Books.shelf(c).filter { m(it.title + " " + it.authors) }
        val all = Books.index(c).filter { m(it.title + " " + it.authors) && mine.none { s -> s.id == it.id } }
        return (mine + all).take(150).map { b -> Hit(Kind.BOOK, b.title, listOf(b.authors, if (Books.has(c, b.id)) "على الهاتف" else "").filter { it.isNotBlank() }.joinToString(" · ")) { Nav.openBook(b) } }
    }

    // ------------------------------------------------------------------ recent searches
    fun recent(): List<String> = Store.prefs.getString("search_recent", "")!!.split('\n').filter { it.isNotBlank() }
    fun remember(q: String) {
        val list = (listOf(q.trim()) + recent().filter { it != q.trim() }).take(8)
        Store.prefs.edit().putString("search_recent", list.joinToString("\n")).apply()
    }
    fun forget() = Store.prefs.edit().remove("search_recent").apply()
}

/** The search page over everything; results come section by section (the hadith books take a moment). */
@Composable
fun GlobalSearchPage(close: () -> Unit) {
    val ctx = LocalContext.current
    BackHandler(onBack = close)
    var query by remember { mutableStateOf("") }
    val found = remember { mutableStateMapOf<GlobalSearch.Kind, List<GlobalSearch.Hit>>() }
    val busy = remember { mutableStateMapOf<GlobalSearch.Kind, Boolean>() }
    val open = remember { mutableStateMapOf<GlobalSearch.Kind, Boolean>() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() }; withContext(Dispatchers.IO) { Quran.load(ctx) } }
    LaunchedEffect(query) {
        found.clear(); busy.clear(); open.clear()
        val q = query.trim()
        if (HadithSearch.words(q).isEmpty()) return@LaunchedEffect
        delay(350)   // typing: wait for a pause
        GlobalSearch.quick(q).groupBy { it.kind }.forEach { (k, v) -> found[k] = v }
        coroutineScope {
            fun slow(k: GlobalSearch.Kind, work: () -> List<GlobalSearch.Hit>) = async {
                busy[k] = true
                val r = withContext(Dispatchers.IO) { runCatching(work).getOrDefault(emptyList()) }
                if (r.isNotEmpty()) found[k] = r
                busy[k] = false
            }
            slow(GlobalSearch.Kind.AYAH) { GlobalSearch.ayahs(q) }
            slow(GlobalSearch.Kind.SHARH) { GlobalSearch.sharh(ctx, q) }
            slow(GlobalSearch.Kind.BOOK) { GlobalSearch.books(ctx, q) }
            slow(GlobalSearch.Kind.HADITH) { GlobalSearch.hadith(ctx, q) }
        }
    }
    fun go(h: GlobalSearch.Hit) { GlobalSearch.remember(query); close(); h.go() }

    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("→", color = P.accent, fontSize = 22.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable(onClick = close).padding(horizontal = 12.dp, vertical = 8.dp))
            OutlinedTextField(query, { query = it }, singleLine = true, modifier = Modifier.weight(1f).focusRequester(focus),
                placeholder = { Text("ابحث في كل التطبيق: آية، حديث، كتاب، ذكر…") },
                trailingIcon = { if (query.isNotEmpty()) Text("✕", color = P.muted, fontSize = 18.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable { query = "" }.padding(10.dp)) })
        }
        val q = query.trim()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (HadithSearch.words(q).isEmpty()) {
                item { Sub("ابحث بكلمة أو أكثر، والحركات وأشكال الهمزة لا تهم. أمثلة:", size = 13.sp) }
                item {
                    Wrap(gap = 6.dp) { listOf("الكهف 10", "آية الكرسي", "إنما الأعمال بالنيات", "الصبر", "رياض الصالحين", "أذكار النوم", "الزكاة", "القبلة").forEach { e -> Choice(e, false) { query = e } } }
                }
                val recent = GlobalSearch.recent()
                if (recent.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("بحثت مؤخرًا", color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("مسح", color = P.muted, fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { GlobalSearch.forget(); query = " "; query = "" }.padding(6.dp))
                        }
                    }
                    items(recent) { r -> Text("🕘 $r", color = P.ink, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).clickable { query = r }.padding(horizontal = 8.dp, vertical = 10.dp)) }
                }
                return@LazyColumn
            }
            val shownKinds = GlobalSearch.Kind.entries.filter { (found[it]?.isNotEmpty() == true) || busy[it] == true }
            if (shownKinds.isEmpty()) item { Sub("لا نتائج بعد…", Modifier.padding(8.dp)) }
            shownKinds.forEach { k ->
                val list = found[k].orEmpty()
                item(key = "h-$k") {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${k.icon} ${k.label}" + if (list.isNotEmpty()) " (${Quran.digits(list.size)}${if (list.size >= 150) "+" else ""})" else "", color = P.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (busy[k] == true) CircularProgressIndicator(Modifier.size(16.dp), color = P.accent, strokeWidth = 2.dp)
                    }
                }
                val all = open[k] == true
                items(if (all) list else list.take(4), key = { h -> "${k}-${list.indexOf(h)}" }) { h ->
                    Column(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(P.surface).clickable { go(h) }.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(h.title, color = P.ink, fontSize = 15.sp, lineHeight = 24.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                            fontFamily = if (k == GlobalSearch.Kind.AYAH) Fonts.quran else Fonts.naskh)
                        if (h.sub.isNotBlank()) Sub(h.sub, size = 12.sp)
                    }
                }
                if (list.size > 4) item(key = "m-$k") {
                    Text(if (all) "أقل ▴" else "عرض الكل (${Quran.digits(list.size)}) ▾", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(L.shape(8.dp)).clickable { open[k] = !all }.padding(horizontal = 8.dp, vertical = 6.dp))
                }
            }
        }
    }
}
