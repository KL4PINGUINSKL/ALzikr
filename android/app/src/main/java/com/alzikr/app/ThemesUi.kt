package com.alzikr.app

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun describe(l: Look): String = listOf(
    when (l.nav) { NavStyle.BAR -> "شريط سفلي"; NavStyle.DOCK -> "شريط عائم"; NavStyle.TOP -> "تبويبات علوية"; NavStyle.WORDS -> "كلمات بلا أيقونات"; NavStyle.LANTERNS -> "فوانيس معلّقة للأقسام"; NavStyle.RAIL -> "شريط جانبي"; NavStyle.ARC -> "أقسام على قوس" },
    when (l.prayers) { PrayerLayout.LIST -> "قائمة الصلوات"; PrayerLayout.CARDS -> "بطاقات الصلوات"; PrayerLayout.TIMELINE -> "خط زمني"; PrayerLayout.LANTERNS -> "فوانيس الصلوات"; PrayerLayout.SUNDIAL -> "مزولة"; PrayerLayout.GARDEN -> "حديقة تُزهر" },
    when (l.counter) { CounterStyle.RING -> "عدّاد حلقة"; CounterStyle.BEADS -> "سبحة خرز"; CounterStyle.PLAIN -> "عدّاد رقمي"; CounterStyle.LANTERN -> "فانوس يمتلئ نورًا"; CounterStyle.FLOWER -> "زهرة تتفتّح"; CounterStyle.CRESCENT -> "هلال يكتمل" },
    when (l.corners) { Corners.ROUND -> "زوايا مستديرة"; Corners.SOFT -> "زوايا ناعمة"; Corners.SQUARE -> "زوايا حادة"; Corners.CUT -> "زوايا مقصوصة" },
).joinToString(" · ")

/** The kinds of themes, to find one quickly. */
private val filters = listOf("all" to "الكل", "open" to "المفتوحة لي", "living" to "خلفيات حيّة", "form" to "تغيّر الشكل", "time" to "تتبدّل مع الوقت", "scene" to "بمشهد مرسوم", "locked" to "المقفلة")
private fun Look.reshapes() = panel != PanelStyle.CARD || nav in setOf(NavStyle.LANTERNS, NavStyle.RAIL, NavStyle.ARC) ||
    prayers in setOf(PrayerLayout.LANTERNS, PrayerLayout.SUNDIAL, PrayerLayout.GARDEN) || counter in setOf(CounterStyle.LANTERN, CounterStyle.FLOWER, CounterStyle.CRESCENT)

/**
 * Choosing a theme: the chosen (or the tapped) theme in full at the top, then every theme as a small tile in a grid of
 * three, drawn in its own colors and background, with filters by kind. A tap on an open tile applies it at once; a tap
 * on a locked one shows it above with the friendship points it needs. No long scrolling to reach the last themes.
 */
@Composable
fun ThemeGallery() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val dark = isSystemInDarkTheme()
    var filter by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("all") }
    var shown by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(Data.look) }
    // «زمرّد» (the default) first, then «فارس» and «بنوتة», then the other open themes, then the locked ones by the hearts they need.
    val first = listOf("emerald", "faris", "girls")
    val list = Looks.all.sortedWith(compareBy<Look>({ l -> first.indexOf(l.id).takeIf { it >= 0 } ?: (if (Friendship.themeOpen(l.id)) 10 else 20) },
        { Friendship.themeGift(it.id)?.bond ?: 0 })).filter { l ->
        val open = Friendship.themeOpen(l.id)
        when (filter) { "open" -> open; "locked" -> !open; "living" -> l.backdrop != Backdrop.NONE; "form" -> l.reshapes(); "time" -> l.sky || l.palFor != null || l.hero != null; "scene" -> l.hero != null; else -> true }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Sub("الثيمات هنا بألوانها الأصلية. لتغيير ألوان ثيمك أو جعله يتبدّل بين الليل والنهار: «تخصيص الثيم».", size = 12.sp)
        val focus = Looks.byId(shown)
        ThemeCard(focus, dark, original = true) { Data.look = focus.id }
        val open = Friendship.themeOpen(focus.id)
        if (!open) Sub("🔒 يُفتح بـ ${Quran.digits(Friendship.themeGift(focus.id)!!.bond)} ❤ صداقة · معك الآن ${Quran.digits(Progress.bondPoints)} ❤", size = 12.sp, color = P.gold)
        else if (Data.look != focus.id) Action("استعمل «${focus.name}»", Modifier.fillMaxWidth()) { Data.look = focus.id }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            filters.forEach { (id, name) -> Choice(name, filter == id) { filter = id } }
        }
        Sub("${Quran.digits(list.size)} ثيمًا · المفتوحة ${Quran.digits(Looks.all.count { Friendship.themeOpen(it.id) })} من ${Quran.digits(Looks.all.size)}", size = 11.sp)
        list.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { look -> ThemeTile(look, dark, Modifier.weight(1f), shown == look.id) {
                    shown = look.id
                    if (Friendship.themeOpen(look.id)) Data.look = look.id
                } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A small tile: the theme's background, its name in its font, its colors, and ✓ or 🔒. */
@Composable
private fun ThemeTile(look: Look, dark: Boolean, modifier: Modifier, focused: Boolean, onTap: () -> Unit) {
    val pal = look.pal(dark, Data.theme, 0, 160f, 0, 0)   // the theme as it was made (its colours are changed in «تخصيص الثيم»)
    val chosen = Data.look == look.id; val open = Friendship.themeOpen(look.id)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Column(modifier.height(104.dp).clip(shape).background(pal.bg).drawBehind { drawBackdrop(look.backdrop, pal, 4f) }
        .border(if (chosen) 3.dp else if (focused) 2.dp else 1.dp, if (chosen) pal.accent else if (focused) pal.gold else pal.line, shape)
        .clickable(onClick = onTap).padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(look.ornament.ifEmpty { "◦" }, color = pal.gold, fontSize = 14.sp, maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (chosen) Text("✓", color = pal.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            else if (!open) Text("🔒${Quran.digits(Friendship.themeGift(look.id)!!.bond)}", color = pal.ink, fontSize = 10.sp, maxLines = 1, softWrap = false,
                modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).background(pal.surface.copy(alpha = .85f)).padding(horizontal = 4.dp, vertical = 1.dp))
        }
        Text(look.name, color = pal.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = look.head ?: look.body, maxLines = 2, lineHeight = 17.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(pal.surface, pal.accent, pal.gold).forEach { c -> Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c).border(1.dp, pal.line, androidx.compose.foundation.shape.CircleShape)) }
        }
    }
}

/** One theme in full: drawn in its own colors, corners and fonts, with a miniature of a panel, a button and the counter. */
@Composable
private fun ThemeCard(look: Look, dark: Boolean, original: Boolean = false, onPick: () -> Unit) {
    val pal = if (original) look.pal(dark, Data.theme, 0, 160f, 0, 0) else look.pal(dark, Data.theme)
    val chosen = Data.look == look.id
    val open = Friendship.themeOpen(look.id)
    CompositionLocalProvider(LocalPal provides pal, LocalLook provides look) {
        Column(Modifier.fillMaxWidth().alpha(if (open) 1f else .6f).clip(look.shape()).background(pal.bg).drawBehind { drawBackdrop(look.backdrop, pal, 4f) }.border(if (chosen) 3.dp else 1.dp, if (chosen) pal.accent else pal.line, look.shape())
            .clickable(enabled = open) { onPick() }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text((if (look.ornament.isNotEmpty()) look.ornament + " " else "") + look.name, color = pal.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = look.head ?: look.body, modifier = Modifier.weight(1f))
                if (chosen) Text("✓ المستخدم", color = pal.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (!open) Text("🔒 يُفتح بـ ${Quran.digits(Friendship.themeGift(look.id)!!.bond)} ❤ صداقة", color = pal.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(look.about, color = pal.muted, fontSize = 12.sp, fontFamily = look.body)
            Spacer(Modifier.height(10.dp))
            // A miniature: a panel, a choice, a button and the counter's shape.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val ps = panelShape(look.panel, look.radius)
                Column(Modifier.weight(1f).clip(ps).background(if (look.flat) pal.bg else pal.surface).border(1.dp, if (look.panel == PanelStyle.CARD) pal.line else pal.gold.copy(alpha = .6f), ps)
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = if (look.panel == PanelStyle.ARCH) 22.dp else 10.dp)) {
                    Text("العصر", color = pal.ink, fontSize = 14.sp, fontFamily = look.head ?: look.body, fontWeight = FontWeight.SemiBold)
                    Text("3:12 م", color = pal.gold, fontSize = 16.sp, fontFamily = look.body, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.clip(look.shape(10.dp)).background(pal.accent).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("سبحان الله", color = pal.onAccent, fontSize = 13.sp, fontFamily = look.body)
                }
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    when (look.counter) {
                        CounterStyle.BEADS -> Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(5) { Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (it < 3) pal.accent else pal.line)) } }
                        CounterStyle.PLAIN -> Text("33", color = pal.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = look.head ?: look.body)
                        CounterStyle.LANTERN -> Text("🏮", fontSize = 26.sp)
                        CounterStyle.FLOWER -> Text("🌸", fontSize = 26.sp)
                        CounterStyle.CRESCENT -> Text("🌙", fontSize = 26.sp)
                        CounterStyle.RING -> Box(Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape).border(5.dp, pal.accent, androidx.compose.foundation.shape.CircleShape))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(describe(look), color = pal.muted, fontSize = 11.sp, fontFamily = look.body)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(pal.bg, pal.surface, pal.card, pal.accent, pal.gold, pal.ink).forEach { c -> Box(Modifier.width(22.dp).height(10.dp).clip(look.shape(3.dp)).background(c).border(1.dp, pal.line, look.shape(3.dp))) }
            }
        }
    }
}

/** Day / night / follow the phone, when the chosen theme has both. */
@Composable
fun ModeChoice() {
    val look = Looks.byId(Data.look)
    if (look.palFor != null || look.sky) { Sub("ألوان هذا الثيم تتبدّل وحدها مع ${if (look.id == "seasons") "الفصول والمناسبات" else "أوقات الصلاة"}.", size = 12.sp); return }
    if (look.day == null || look.night == null) { Sub("هذا الثيم ${if (look.day == null) "ليلي" else "نهاري"} دائمًا.", size = 12.sp); return }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0 to "حسب الهاتف", 1 to "نهاري", 2 to "ليلي").forEach { (n, l) -> Choice(l, Data.theme == n) { Data.theme = n } }
    }
}

/** The current theme's seven colors, and a free one from the color wheel. */
@Composable
fun ColorChoice() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val look = Looks.byId(Data.look)
    val dark = isSystemInDarkTheme()
    val chosen = Data.lookColor(look.id)
    val now = look.pal(dark, Data.theme)
    val base = look.basePal(dark, Data.theme).copy(dark = now.dark)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Sub("لون الأزرار والعناوين في «${look.name}»", size = 13.sp)
        Wrap(gap = 8.dp) {
            Tints.presets.forEach { i ->
                val t = Tints.tint(i)!!
                val c = Tints.accentFor(i, 0f, base.dark) ?: base.accent
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { Data.setLookColor(look.id, i) }) {
                    Box(Modifier.size(34.dp).clip(look.shape(10.dp)).background(c).border(if (chosen == i) 3.dp else 1.dp, if (chosen == i) P.ink else P.line, look.shape(10.dp)),
                        contentAlignment = Alignment.Center) { if (chosen == i) Text("✓", color = if (c.luminance() > .5f) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold) }
                    Text(t.name, color = P.muted, fontSize = 9.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Choice("لون حر", chosen == Tints.CUSTOM) { Data.setLookColor(look.id, Tints.CUSTOM) }
            if (chosen == Tints.CUSTOM) {
                var hue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(Data.lookHue(look.id)) }
                androidx.compose.material3.Slider(hue, { hue = it }, valueRange = 0f..359f, onValueChangeFinished = { Data.setLookColor(look.id, Tints.CUSTOM, hue) },
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = androidx.compose.ui.graphics.Color.hsv(hue, .7f, .6f), activeTrackColor = androidx.compose.ui.graphics.Color.hsv(hue, .7f, .6f)))
            }
        }
        // The background, then the text color: mix any of them with any buttons color.
        val back = Data.lookBack(look.id)
        Sub("الخلفية", Modifier.padding(top = 6.dp), size = 13.sp)
        Wrap(gap = 8.dp) {
            Backs.list.forEachIndexed { i, b ->
                val bg = if (i == 0) look.basePal(dark, Data.theme).bg else androidx.compose.ui.graphics.Color(b.bg)
                val card = if (i == 0) look.basePal(dark, Data.theme).card else androidx.compose.ui.graphics.Color(b.card)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { Data.setLookBack(look.id, i) }) {
                    Box(Modifier.size(width = 46.dp, height = 34.dp).clip(look.shape(10.dp)).background(bg).border(if (back == i) 3.dp else 1.dp, if (back == i) P.accent else P.line, look.shape(10.dp)),
                        contentAlignment = Alignment.Center) {
                        Box(Modifier.size(width = 24.dp, height = 10.dp).clip(look.shape(4.dp)).background(card))
                    }
                    Text(b.name, color = if (back == i) P.accent else P.muted, fontSize = 9.sp)
                }
            }
        }
        val ink = Data.lookInk(look.id)
        Sub("لون الخط", Modifier.padding(top = 6.dp), size = 13.sp)
        Wrap(gap = 8.dp) {
            Inks.list.forEachIndexed { i, k ->
                val c = if (i == 0) (if (now.dark) androidx.compose.ui.graphics.Color(0xFFF2F2F2) else androidx.compose.ui.graphics.Color(0xFF151515)) else androidx.compose.ui.graphics.Color(k.c)
                val readable = i == 0 || Tints.contrast(c, now.bg) >= 3f
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(enabled = readable) { Data.setLookInk(look.id, i) }) {
                    Box(Modifier.size(34.dp).clip(look.shape(10.dp)).background(now.bg).border(if (ink == i) 3.dp else 1.dp, if (ink == i) P.accent else P.line, look.shape(10.dp)),
                        contentAlignment = Alignment.Center) { Text("أ", color = c.copy(alpha = if (readable) 1f else .25f), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    Text(k.name, color = if (ink == i) P.accent else P.muted.copy(alpha = if (readable) 1f else .4f), fontSize = 9.sp)
                }
            }
        }
        Sub("امزج أي خلفية مع أي لون للأزرار ولون للخط. الخلفيات السوداء تبقى سوداء نقية مهما غيّرت اللون، والألوان التي لا تُقرأ على الخلفية تظهر باهتة ولا تُختار.", size = 11.sp)
    }
}


/**
 * «تخصيص الثيم» (user, 2026-11-28): the theme in use as it will look, then its colours (buttons, background, text),
 * day / night, and «ثيم الليل والنهار». «الثيمات» keeps the themes as they were made.
 */
@Composable
fun ThemeCustomPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val dark = isSystemInDarkTheme()
    val look = Looks.byId(Data.look)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Sub("هكذا يبدو ثيمك «${look.name}» بعد التخصيص:", size = 13.sp)
        ThemeCard(look, dark) {}
        Panel(pad = 12.dp) {
            Text("☀🌙 النهاري والليلي", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.padding(top = 6.dp)) { ModeChoice() }
        }
        Panel(pad = 12.dp) {
            Text("🎨 الألوان", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
            ColorChoice()
            if (Data.lookColor(look.id) != 0 || Data.lookBack(look.id) != 0 || Data.lookInk(look.id) != 0)
                Action("↺ أرجع ألوان «${look.name}» الأصلية", Modifier.fillMaxWidth().padding(top = 10.dp), filled = false) {
                    Data.setLookColor(look.id, 0); Data.setLookBack(look.id, 0); Data.setLookInk(look.id, 0)
                }
        }
        DayNightPanel()
    }
}

/** «ثيم الليل والنهار»: a theme from Fajr to Maghrib and another from Maghrib to Fajr, each chosen here. */
@Composable
private fun DayNightPanel() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    Panel(pad = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🌗 ثيم الليل والنهار", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Sub("ثيم من الفجر إلى المغرب، وآخر من المغرب إلى الفجر، يتبدّلان وحدهما مع أوقات الصلاة.", size = 12.sp)
            }
            androidx.compose.material3.Switch(Data.dayNight, { Data.dayNight = it })
        }
        if (Data.dayNight) {
            val now = Data.isNight()
            val open = Looks.all.filter { Friendship.themeOpen(it.id) }.map { it.id to it.name }
            listOf(false to "☀ النهار", true to "🌙 الليل").forEach { (n, label) ->
                Text(label + if (n == now) " (الآن)" else "", color = if (n == now) P.accent else P.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                PickOne(open, if (n) Data.lookNight else Data.lookDay, if (n) "ثيم الليل" else "ثيم النهار", shown = 3, modifier = Modifier.padding(top = 4.dp)) {
                    if (n) Data.lookNight = it else Data.lookDay = it
                }
            }
            Sub("التخصيص (الألوان) يخص كل ثيم وحده، فخصّص كلًّا منهما في وقته.", Modifier.padding(top = 8.dp), size = 11.sp)
        }
    }
}
