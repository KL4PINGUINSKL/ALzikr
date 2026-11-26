package com.alzikr.app

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Touch each object that holds Compose state, so it is created on the main thread. */
fun warmStates() {
    Store.version; Nav.tab; Quran.ready; Recitation.now; AppUpdate.offer; AppUpdate.progress; Tafsir.version; Hadith.version; Books.version; Adhan.downloading
}

class MainActivity : ComponentActivity() {
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        val tap = Nav.volumeTap
        if (tap != null && (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event?.repeatCount == 0) tap()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        AutoBackup.onLaunch(this)
        runCatching { Journal.start() }
        // Create every shared Compose state on the main thread first. If a background thread (the Quran
        // loading below) is the first to touch one of these objects, its state is born outside the
        // composition's snapshot and Compose can crash with "Reading a state that was created after the snapshot was taken".
        warmStates()
        Data.mergeOldKhatma()
        PetOverlay.start(this)
        enableEdgeToEdge()
        if (savedInstanceState == null) Nav.tab = Nav.home
        intent?.getStringExtra("tab")?.let(::openTab)
        runCatching { NoteBubble.refresh(this) }
        // A test build of 1.5.1 moved the full app to the emerald when entering the simple app and kept the theme
        // to bring back; bring it back now (the simple app has its own themes since).
        Store.prefs.getString("simple_prev_look", null)?.let { prev ->
            Store.prefs.edit().putString("look", prev).putInt("theme", Store.prefs.getInt("simple_prev_theme", 0))
                .remove("simple_prev_look").remove("simple_prev_theme").apply()
        }
        runCatching { PetLooks.keepOldDefaults(existing = !Store.prefs.contains("welcomed") && (Store.hasLocation || Pet.species != null || Pet.introSeen)) }
        openPlace(intent)
        AppUpdate.onFound = { Store.bump() }
        // Installed before methods followed the country: look the country up once.
        if (Store.hasLocation && Store.country.isEmpty() && !Store.methodChosen) Alerts.resolvePlace(this, Store.lat, Store.lng)
        if (Build.VERSION.SDK_INT >= 33 && !Alerts.canPost(this)) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        Thread { Quran.load(applicationContext); Tafsir.cleanup(applicationContext) }.start()
        setContent { Root() }
    }

    /** "quran" = "2:255" opens the mushaf there; "hadith" = "bukhari:1" opens a book (shortcuts and links). */
    private fun openPlace(i: Intent?) {
        if (i?.getBooleanExtra("darktasbih", false) == true) TasbihDarkActivity.open(this)
        i?.getStringExtra("read")?.split(':')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }?.let { Nav.reading = it[0] to it[1] }
        // Opens a surah at a verse in the library's reader ("3:1"; also for testing by adb).
        i?.getStringExtra("quran")?.split(':')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }?.let { Nav.openQuran(it[0], it[1]) }
        i?.getStringExtra("hsearch")?.let { Nav.hadithQuery = it; Nav.tab = "library" }
        i?.getIntExtra("lib", -1)?.takeIf { it >= 0 }?.let { Nav.library = it; Nav.tab = "library" }
        i?.getIntExtra("story", -1)?.takeIf { it >= 0 }?.let { Nav.story = it; Nav.library = 1; Nav.tab = "library" }
        i?.getStringExtra("petskin")?.let { com.alzikr.app.pets.PetPreview.start = it }
        i?.getStringExtra("petstate")?.let { com.alzikr.app.pets.PetPreview.state = it }
        i?.getStringExtra("pettab")?.let { PetTabs.start = it }
        // Test switch: show a season's decorations out of its time ("ramadan", or "off").
        i?.getStringExtra("petseason")?.let { Store.prefs.edit().putString("pet_season_preview", if (it == "off") "" else it).apply(); Store.bump() }
        i?.getStringExtra("page")?.let { Nav.morePage = it; Nav.go("more") }
        // Pet packs placed in the app's private files/incoming/ (by adb run-as, for testing): install them through the
        // same checks as a download. Other apps cannot write there, so the name alone is not a risk.
        i?.getStringExtra("petinstall")?.let { name ->
            Thread { val r = com.alzikr.app.pets.PetPacks.installLocal(name)
                runOnUiThread { android.widget.Toast.makeText(this, r.fold({ "ثُبّتت حزمة «${it.title}»" }, { "لم تُثبَّت الحزمة: ${it.message}" }), android.widget.Toast.LENGTH_LONG).show(); Store.bump() } }.start()
        }
        i?.getStringExtra("coll")?.let { Nav.collection = it; Nav.go("adhkar") }
        i?.getIntExtra("adhkarsec", -1)?.takeIf { it >= 0 }?.let { Nav.adhkar = it; Nav.go("adhkar") }
        i?.getStringExtra("day")?.let { d -> runCatching { java.time.LocalDate.parse(d) }.getOrNull()?.let { Nav.daySheet = it } }
        i?.getStringExtra("psheet")?.let { n -> runCatching { Prayer.valueOf(n) }.getOrNull()?.let { Nav.prayerSheet = it; Nav.tab = "prayer" } }
        i?.getStringExtra("quran")?.split(':')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }?.let { Nav.openQuran(it[0], it[1]) }
        i?.getStringExtra("hadith")?.split(':')?.takeIf { it.size == 2 }?.let { Nav.openHadith(it[0], it[1].toIntOrNull() ?: 0) }
    }

    /** Notifications open a tab by name; a few names moved inside other tabs in 1.1. */
    private fun openTab(t: String) {
        when (t) {
            "focus" -> { Nav.tab = "daily"; Nav.daily = 2 }
            "tasks" -> { Nav.tab = "daily"; Nav.daily = 0 }
            "khatma" -> { Nav.tab = "daily"; Nav.daily = 3 }
            "today" -> { Nav.tab = "daily"; Nav.daily = Daily.TODAY }
            "notes" -> { Nav.tab = "daily"; Nav.daily = Daily.NOTES }
            "wird" -> Nav.wird = true
            else -> Nav.tab = t
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("tab")?.let(::openTab)
        openPlace(intent)
    }

    override fun onPause() { super.onPause(); com.alzikr.app.pets.PetPerf.setVisible(com.alzikr.app.pets.Place.APP, false); com.alzikr.app.pets.PetPerf.setVisible(com.alzikr.app.pets.Place.KIDS, false); com.alzikr.app.pets.PetPerf.setVisible(com.alzikr.app.pets.Place.GRID, false); PetOverlay.appVisible(this, false); val app = applicationContext; Thread { runCatching { PetWidget.update(app) }; runCatching { AutoBackup.save(app) } }.start() }

    override fun onResume() {
        PetOverlay.appVisible(this, true)
        listOf(com.alzikr.app.pets.Place.APP, com.alzikr.app.pets.Place.KIDS, com.alzikr.app.pets.Place.GRID, com.alzikr.app.pets.Place.PREVIEW).forEach { com.alzikr.app.pets.PetPerf.setVisible(it, true) }
        super.onResume()
        Alerts.reschedule(this)
        Alerts.focusNotification(this)
        TasbihWidget.update(this)
        AppUpdate.check(this)
    }
}

/** "الجمعة 20 جمادى الأولى 1448 هـ · 21 نوفمبر" */
fun dateLine(date: LocalDate = LocalDate.now()): String {
    val ar = Locale("ar")
    val h = Data.hijri(date)
    val hijri = "${h.day} ${h.monthName} ${h.year}"
    val day = date.format(DateTimeFormatter.ofPattern("EEEE", ar))
    val greg = date.format(DateTimeFormatter.ofPattern("d MMMM", ar))
    return "$day $hijri هـ · $greg"
}

/** Material's text styles with the theme's font everywhere. */
private fun typography(font: FontFamily?): Typography {
    val t = Typography()
    if (font == null) return t
    fun TextStyle.f() = copy(fontFamily = font)
    return Typography(t.displayLarge.f(), t.displayMedium.f(), t.displaySmall.f(), t.headlineLarge.f(), t.headlineMedium.f(), t.headlineSmall.f(),
        t.titleLarge.f(), t.titleMedium.f(), t.titleSmall.f(), t.bodyLarge.f(), t.bodyMedium.f(), t.bodySmall.f(), t.labelLarge.f(), t.labelMedium.f(), t.labelSmall.f())
}

data class Section(val id: String, val label: String, val glyph: String)
val sections get() = if (Data.simple) listOf(Section("prayer", "الصلاة", "🕌"), Section("library", "القرآن", "📖"))
else if (Looks.byId(Data.look).kids) listOf(
    Section("kids", "الأطفال", "⭐"), Section("prayer", Home.name, "🕌"), Section("adhkar", "الأذكار", "📿"),
    Section("library", "المكتبة", "📖"), Section("pet", "صديقي", "🐾"), Section("more", "المزيد", "☰"),
) else listOf(
    Section("prayer", Home.name, "🕌"), Section("adhkar", "الأذكار", "📿"), Section("library", "المكتبة", "📖"),
    Section("daily", "يومي", "☑"), Section("pet", "صديقي", "🐾"), Section("more", "المزيد", "☰"),
)

/** The applied theme (look, colors, fonts, right-to-left) around any screen: the app, the alarm. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val look = AppFont.apply(Looks.byId(Data.look))   // the font chosen in «راحة العين», if any
    val pal = look.pal(isSystemInDarkTheme(), Data.theme)
    val scheme = if (pal.dark) darkColorScheme(primary = pal.accent, onPrimary = pal.onAccent, surface = pal.surface, onSurface = pal.ink, background = pal.bg, onSurfaceVariant = pal.muted, surfaceVariant = pal.card)
        else lightColorScheme(primary = pal.accent, onPrimary = pal.onAccent, surface = pal.surface, onSurface = pal.ink, background = pal.bg, onSurfaceVariant = pal.muted, surfaceVariant = pal.card)
    MaterialTheme(colorScheme = scheme, typography = typography(look.body)) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val scaled = if (look.kids) androidx.compose.ui.unit.Density(density.density, density.fontScale * 1.12f) else density
        CompositionLocalProvider(LocalPal provides pal, LocalLook provides look, LocalLayoutDirection provides LayoutDirection.Rtl, androidx.compose.ui.platform.LocalDensity provides scaled) { content() }
    }
}

@Composable
private fun Root() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val look = Looks.byId(Data.look)
    val pal = look.pal(isSystemInDarkTheme(), Data.theme)
    var settings by remember { mutableStateOf<String?>(null) }   // null = closed, "" = the sections list, else one section
    // The home-screen widgets follow the theme too.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(pal) { runCatching { PrayerWidget.updateAll(ctx); TasbihWidget.update(ctx); MomentWidget.update(ctx); PetWidget.update(ctx) } }
    androidx.compose.runtime.key(Data.simple) { AppTheme {
        run {
            // Back returns to the previous page (inner pages handle back first).
            androidx.activity.compose.BackHandler(enabled = Nav.history.isNotEmpty() || Nav.tab != Nav.home) { Nav.back() }
            Box(Modifier.fillMaxSize().background(pal.bg)) {
                ThemeBackdrop(look.backdrop, pal)
                // «سماء الوقت»: the colors follow the hour, checked each minute.
                if (look.sky || look.palFor != null) androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000); Sky.tick++ } }
                RestoreOffer()
                if (Data.simple && Nav.tab == "more" && Nav.morePage == null) androidx.compose.runtime.LaunchedEffect(Unit) { Nav.go("prayer") }
                // While a reading screen is open the app's header, tabs, bar and friend step aside (full screen).
                val inPage = Nav.pages.isNotEmpty() || Reading.open > 0
                Column(Modifier.fillMaxSize()) {
                    if (inPage) Spacer(Modifier.statusBarsPadding())
                    else Header()
                    if (look.nav == NavStyle.TOP && !inPage) TopTabs()
                    if (look.nav == NavStyle.LANTERNS && !inPage) LanternTabs()
                    Row(Modifier.weight(1f)) {
                    if (look.nav == NavStyle.RAIL && !inPage) SideRail()
                    Box(Modifier.weight(1f).then(if (inPage || Nav.search) Modifier else Modifier.tabSwipe())) {
                        // Notes pinned to this page hang at its top corner (hidden over readers, shown on the calendar).
                        val scrapPlace = if (Nav.tab == "more" && Nav.moreTop == "calendar") "calendar" else Nav.tab
                        when (Nav.tab) {
                            "adhkar" -> AdhkarScreen()
                            "library" -> LibraryScreen(quranOnly = Data.simple)
                            "daily" -> TasksScreen()
                            "more" -> MoreScreen(openSettings = { settings = it })
                            "kids" -> KidsHome()
                            "pet" -> PetPage()   // «صديقي» as its own section (it stays in «المزيد» too)
                            else -> PrayerScreen(openSettings = { settings = it })
                        }
                        if (!Data.simple && (!inPage || scrapPlace == "calendar")) PinnedScraps(scrapPlace, Modifier.align(Alignment.TopEnd))
                    }
                    }
                    if (!inPage && Pet.visible && Pet.place == "ground") Column(Modifier.fillMaxWidth()) {
                        PetStage(Modifier.fillMaxWidth().padding(horizontal = 10.dp), height = 46, bubbleBelow = false)
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(2.dp).clip(L.shape(1.dp)).background(P.line))
                    }
                    MiniPlayer()
                    if (inPage) Spacer(Modifier.navigationBarsPadding())
                    else when (look.nav) {
                        NavStyle.BAR -> BottomBar()
                        NavStyle.WORDS -> WordsBar()
                        NavStyle.DOCK -> Spacer(Modifier.height(86.dp).navigationBarsPadding())
                        NavStyle.TOP, NavStyle.LANTERNS, NavStyle.RAIL -> Spacer(Modifier.navigationBarsPadding())
                        NavStyle.ARC -> Spacer(Modifier.height(92.dp).navigationBarsPadding())
                    }
                }
                if (look.nav == NavStyle.DOCK && !inPage) Box(Modifier.align(Alignment.BottomCenter)) { Dock() }
                if (look.nav == NavStyle.ARC && !inPage) Box(Modifier.align(Alignment.BottomCenter)) { ArcNav() }
                Overlays()
            }
            Nav.daySheet?.let { d -> DaySheet(d) { Nav.daySheet = null } }
            UpdateDialog()
            PetIntro()
            ProgressCelebration()
            settings?.let { s -> SettingsSheet(s.ifEmpty { null }) { settings = null } }
            Nav.settingsReq?.let { s -> settings = s; Nav.settingsReq = null }
            if (Nav.search) GlobalSearchPage { Nav.search = false }
            WelcomePages()     // the first launch
            EyeBreak()         // «استراحة العين»: a short reminder card at the top
            ComfortOverlay()   // «نور الليل»: over everything, letting touches through
        }
    } }
}

/** Pages opened over the current tab; back closes them and you are where you were. */
@Composable
private fun Overlays() {
    if (Nav.wird) { WirdReader { Nav.wird = false }; return }
    Nav.reading?.let { (s, a) -> QuranReading(s, a) { Nav.reading = null }; return }
    val sharh = Nav.overlaySharh; val hadith = Nav.overlayHadith; val quran = Nav.overlayQuran
    if (sharh == null && hadith == null && quran == null) return
    Box(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().navigationBarsPadding()) {
        when {
            sharh != null -> SharhOpen(sharh) { Nav.overlaySharh = null }
            hadith != null -> HadithReader(hadith.first, 0, back = { Nav.overlayHadith = null }, focus = hadith.second)
            quran != null -> QuranOverlay(quran.first, quran.second) { Nav.overlayQuran = null }
        }
    }
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val pet = Pet.visible && Pet.place == "top"
        // The friend gets its own fixed space (it used to get only what the date line left, a few dp on long dates).
        Column(Modifier.weight(1f)) {
            Text(if (L.ornament.isNotEmpty()) "${L.ornament} ALzikr ${L.ornament}" else "ALzikr", color = P.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = L.head)
            var styles by remember { mutableStateOf(false) }
            DateBadge(open = { Nav.morePage = "calendar"; Nav.go("more") }, pick = { styles = true })
            if (styles) DateStyleSheet { styles = false }
        }
        // The settings live in «المزيد» now, so the friend takes the gear's place and is a little bigger.
        // The friend, with its glow if one is chosen («صديقي» › 🖼).
        if (pet) Box(Modifier.width(150.dp).padding(start = 4.dp)) { PetGlow(); PetStage(Modifier.fillMaxWidth(), height = 88) }
        // The simple mode: the settings instead of the search (there is no «المزيد» there).
        if (Data.simple) Box(Modifier.size(40.dp).clip(L.shape(12.dp)).clickable { Nav.settingsReq = "" }, contentAlignment = Alignment.Center) {
            if (L.icons == IconStyle.GLYPH || L.icons == IconStyle.NONE) Glyph("⚙", tint = P.muted, size = 20.dp) else Icon(Icons.Filled.Settings, "الإعدادات", tint = P.muted)
        }
        // The search over everything: one small, quiet button at the header's end.
        else Box(Modifier.size(40.dp).clip(L.shape(12.dp)).clickable { Nav.search = true }, contentAlignment = Alignment.Center) {
            if (L.icons == IconStyle.GLYPH || L.icons == IconStyle.NONE) Glyph("🔍", tint = P.muted, size = 20.dp) else Icon(Icons.Filled.Search, "البحث في كل التطبيق", tint = P.muted)
        }
    }
}

/**
 * A sideways swipe over a section's page moves to the next or previous section, round like a wheel: after the last
 * comes the first. The finger goes the way the bar goes: a swipe to the right reaches the section on the right.
 * Lists that scroll sideways keep their own swipes (they take the gesture first).
 */
@Composable
private fun Modifier.tabSwipe(): Modifier {
    val ids = sections.map { it.id }
    return this.pointerInput(ids) {
        var total = 0f
        detectHorizontalDragGestures(onDragStart = { total = 0f }, onDragCancel = { total = 0f }, onDragEnd = {
            val limit = 90.dp.toPx()
            val i = ids.indexOf(Nav.tab).takeIf { it >= 0 } ?: 0
            if (kotlin.math.abs(total) > limit && ids.isNotEmpty()) {
                val next = if (total > 0) (i + 1) % ids.size else (i - 1 + ids.size) % ids.size
                Nav.go(ids[next])
            }
            total = 0f
        }) { change, dx -> total += dx; change.consume() }
    }
}

/** The section icon in the theme's icon set. */
@Composable
internal fun SectionIcon(s: Section, on: Boolean, size: Int = 22, tint: androidx.compose.ui.graphics.Color = if (on) P.accent else P.muted) {
    when (L.icons) {
        IconStyle.GLYPH -> Glyph(s.glyph, tint = tint, size = size.dp)
        IconStyle.NONE -> {}
        else -> Icon(AppIcons.of(s.id, L.icons), s.label, tint = tint, modifier = Modifier.size(size.dp))
    }
}

@Composable
private fun BottomBar() {
    Column(Modifier.fillMaxWidth().background(P.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            sections.forEach { s ->
                val on = Nav.tab == s.id
                // Equal shares of the width: every section fits, none is cut at the edge.
                Column(Modifier.weight(1f).clip(L.shape(12.dp)).clickable { Nav.go(s.id) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.clip(L.shape(10.dp)).background(if (on) P.accent.copy(alpha = .16f) else P.surface).padding(horizontal = 10.dp, vertical = 4.dp)) { SectionIcon(s, on) }
                    Spacer(Modifier.height(2.dp))
                    Text(s.label, color = if (on) P.accent else P.muted, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}

/** Words only, with a short line over the chosen one. */
@Composable
private fun WordsBar() {
    Column(Modifier.fillMaxWidth().background(P.bg)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
            sections.forEach { s ->
                val on = Nav.tab == s.id
                Column(Modifier.weight(1f).clickable { Nav.go(s.id) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(22.dp).height(3.dp).background(if (on) P.accent else P.bg))
                    Spacer(Modifier.height(6.dp))
                    Text(s.label, color = if (on) P.ink else P.muted, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

/** Tabs under the header, like a book's sections. */
@Composable
private fun TopTabs() {
    Column(Modifier.fillMaxWidth()) {
        // Every section in one even row (no hidden sideways scrolling).
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
            sections.forEach { s ->
                val on = Nav.tab == s.id
                Column(Modifier.weight(1f).clickable { Nav.go(s.id) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionIcon(s, on, 18)
                    Text(s.label, color = if (on) P.accent else P.muted, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(36.dp).height(2.dp).background(if (on) P.accent else P.bg))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
    }
}

/** A floating dock with icons only. */
@Composable
private fun Dock() {
    Row(Modifier.navigationBarsPadding().padding(bottom = 14.dp).shadow(10.dp, L.shape(28.dp)).clip(L.shape(28.dp)).background(P.card).border(1.dp, P.line, L.shape(28.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        sections.forEach { s ->
            val on = Nav.tab == s.id
            Box(Modifier.size(50.dp).clip(L.shape(20.dp)).background(if (on) P.accent else P.card).clickable { Nav.go(s.id) }, contentAlignment = Alignment.Center) {
                when (L.icons) {
                    IconStyle.GLYPH, IconStyle.NONE -> Glyph(s.glyph, tint = if (on) P.onAccent else P.muted, size = 24.dp)
                    else -> Icon(AppIcons.of(s.id, L.icons), s.label, tint = if (on) P.onAccent else P.muted, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
