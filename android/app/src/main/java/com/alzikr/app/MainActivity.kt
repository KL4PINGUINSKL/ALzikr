package com.alzikr.app

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    Store.version; Nav.tab; Quran.ready; Recitation.now; Tafsir.version; Hadith.version; Books.version; Adhan.downloading
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
        // Create every shared Compose state on the main thread first. If a background thread (the Quran
        // loading below) is the first to touch one of these objects, its state is born outside the
        // composition's snapshot and Compose can crash with "Reading a state that was created after the snapshot was taken".
        warmStates()
        Data.mergeOldKhatma()
        enableEdgeToEdge()
        intent?.getStringExtra("tab")?.let(::openTab)
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
        i?.getStringExtra("hsearch")?.let { Nav.hadithQuery = it; Nav.tab = "library" }
        i?.getIntExtra("lib", -1)?.takeIf { it >= 0 }?.let { Nav.library = it; Nav.tab = "library" }
        i?.getIntExtra("story", -1)?.takeIf { it >= 0 }?.let { Nav.story = it; Nav.library = 1; Nav.tab = "library" }
        i?.getStringExtra("page")?.let { Nav.morePage = it; Nav.go("more") }
        i?.getStringExtra("coll")?.let { Nav.collection = it; Nav.go("adhkar") }
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
            "wird" -> Nav.wird = true
            else -> Nav.tab = t
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("tab")?.let(::openTab)
        openPlace(intent)
    }

    override fun onResume() {
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
val sections get() = listOf(
    Section("prayer", Home.name, "🕌"), Section("adhkar", "الأذكار", "📿"), Section("library", "المكتبة", "📖"),
    Section("daily", "يومي", "☑"), Section("more", "المزيد", "☰"),
)

/** The applied theme (look, colors, fonts, right-to-left) around any screen: the app, the alarm. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val look = Looks.byId(Data.look)
    val pal = look.pal(isSystemInDarkTheme(), Data.theme)
    val scheme = if (pal.dark) darkColorScheme(primary = pal.accent, onPrimary = pal.onAccent, surface = pal.surface, onSurface = pal.ink, background = pal.bg, onSurfaceVariant = pal.muted, surfaceVariant = pal.card)
        else lightColorScheme(primary = pal.accent, onPrimary = pal.onAccent, surface = pal.surface, onSurface = pal.ink, background = pal.bg, onSurfaceVariant = pal.muted, surfaceVariant = pal.card)
    MaterialTheme(colorScheme = scheme, typography = typography(look.body)) {
        CompositionLocalProvider(LocalPal provides pal, LocalLook provides look, LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
    }
}

@Composable
private fun Root() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val look = Looks.byId(Data.look)
    val pal = look.pal(isSystemInDarkTheme(), Data.theme)
    var settings by remember { mutableStateOf(false) }
    // The home-screen widgets follow the theme too.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(pal) { runCatching { PrayerWidget.updateAll(ctx); TasbihWidget.update(ctx); MomentWidget.update(ctx) } }
    AppTheme {
        run {
            // Back returns to the previous page (inner pages handle back first).
            androidx.activity.compose.BackHandler(enabled = Nav.history.isNotEmpty() || Nav.tab != "prayer") { Nav.back() }
            Box(Modifier.fillMaxSize().background(pal.bg)) {
                val inPage = Nav.pages.isNotEmpty()
                Column(Modifier.fillMaxSize()) {
                    if (inPage) Spacer(Modifier.statusBarsPadding())
                    else Header { settings = true }
                    if (look.nav == NavStyle.TOP && !inPage) TopTabs()
                    Box(Modifier.weight(1f)) {
                        when (Nav.tab) {
                            "adhkar" -> AdhkarScreen()
                            "library" -> LibraryScreen()
                            "daily" -> TasksScreen()
                            "more" -> MoreScreen(openSettings = { settings = true })
                            else -> PrayerScreen(openSettings = { settings = true })
                        }
                    }
                    MiniPlayer()
                    if (inPage) Spacer(Modifier.navigationBarsPadding())
                    else when (look.nav) {
                        NavStyle.BAR -> BottomBar()
                        NavStyle.WORDS -> WordsBar()
                        NavStyle.DOCK -> Spacer(Modifier.height(86.dp).navigationBarsPadding())
                        NavStyle.TOP -> Spacer(Modifier.navigationBarsPadding())
                    }
                }
                if (look.nav == NavStyle.DOCK && !inPage) Box(Modifier.align(Alignment.BottomCenter)) { Dock() }
                Overlays()
            }
            if (settings) SettingsSheet { settings = false }
        }
    }
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
private fun Header(openSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (L.ornament.isNotEmpty()) "${L.ornament} ALzikr ${L.ornament}" else "ALzikr", color = P.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = L.head)
            Sub("📅 " + dateLine(), Modifier.clip(L.shape(8.dp)).clickable { Nav.morePage = "calendar"; Nav.go("more") }, size = 12.sp)
        }
        Box(Modifier.size(44.dp).clip(L.shape(12.dp)).clickable(onClick = openSettings), contentAlignment = Alignment.Center) {
            if (L.icons == IconStyle.GLYPH || L.icons == IconStyle.NONE) Glyph("⚙", tint = P.muted, size = 22.dp) else Icon(Icons.Filled.Settings, "الإعدادات", tint = P.muted)
        }
    }
}

/** The section icon in the theme's icon set. */
@Composable
private fun SectionIcon(s: Section, on: Boolean, size: Int = 22) {
    val tint = if (on) P.accent else P.muted
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
                Column(Modifier.clip(L.shape(12.dp)).clickable { Nav.go(s.id) }.padding(horizontal = 4.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.clip(L.shape(10.dp)).background(if (on) P.accent.copy(alpha = .16f) else P.surface).padding(horizontal = 12.dp, vertical = 4.dp)) { SectionIcon(s, on) }
                    Spacer(Modifier.height(2.dp))
                    Text(s.label, color = if (on) P.accent else P.muted, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
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
                Column(Modifier.clickable { Nav.go(s.id) }.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            sections.forEach { s ->
                val on = Nav.tab == s.id
                Column(Modifier.clickable { Nav.go(s.id) }.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionIcon(s, on, 18); if (L.icons != IconStyle.NONE) Spacer(Modifier.width(4.dp))
                        Text(s.label, color = if (on) P.accent else P.muted, fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                    }
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
