package com.alzikr.app

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun describe(l: Look): String = listOf(
    when (l.nav) { NavStyle.BAR -> "شريط سفلي"; NavStyle.DOCK -> "شريط عائم"; NavStyle.TOP -> "تبويبات علوية"; NavStyle.WORDS -> "كلمات بلا أيقونات" },
    when (l.prayers) { PrayerLayout.LIST -> "قائمة الصلوات"; PrayerLayout.CARDS -> "بطاقات الصلوات"; PrayerLayout.TIMELINE -> "خط زمني" },
    when (l.counter) { CounterStyle.RING -> "عدّاد حلقة"; CounterStyle.BEADS -> "سبحة خرز"; CounterStyle.PLAIN -> "عدّاد رقمي" },
    when (l.corners) { Corners.ROUND -> "زوايا مستديرة"; Corners.SOFT -> "زوايا ناعمة"; Corners.SQUARE -> "زوايا حادة"; Corners.CUT -> "زوايا مقصوصة" },
).joinToString(" · ")

/** Every theme as a small card drawn in its own colors, corners and fonts. Tap to apply. */
@Composable
fun ThemeGallery() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val dark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Looks.all.forEach { look ->
            val pal = look.pal(dark, Data.theme)
            val chosen = Data.look == look.id
            CompositionLocalProvider(LocalPal provides pal, LocalLook provides look) {
                Column(Modifier.fillMaxWidth().clip(look.shape()).background(pal.bg).border(if (chosen) 3.dp else 1.dp, if (chosen) pal.accent else pal.line, look.shape())
                    .clickable { Data.look = look.id }.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text((if (look.ornament.isNotEmpty()) look.ornament + " " else "") + look.name, color = pal.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = look.head ?: look.body, modifier = Modifier.weight(1f))
                        if (chosen) Text("✓ المستخدم", color = pal.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(look.about, color = pal.muted, fontSize = 12.sp, fontFamily = look.body)
                    Spacer(Modifier.height(10.dp))
                    // A miniature: a panel, a choice, a button and the counter's shape.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clip(look.shape()).background(if (look.flat) pal.bg else pal.surface).border(1.dp, pal.line, look.shape()).padding(10.dp)) {
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
    }
}

/** Day / night / follow the phone, when the chosen theme has both. */
@Composable
fun ModeChoice() {
    val look = Looks.byId(Data.look)
    if (look.day == null || look.night == null) { Sub("هذا الثيم ${if (look.day == null) "ليلي" else "نهاري"} دائمًا.", size = 12.sp); return }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0 to "حسب الهاتف", 1 to "نهاري", 2 to "ليلي").forEach { (n, l) -> Choice(l, Data.theme == n) { Data.theme = n } }
    }
}
