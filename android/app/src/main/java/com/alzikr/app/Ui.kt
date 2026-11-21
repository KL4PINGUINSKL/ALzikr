package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** ALzikr's colors: calm night green with emerald and a touch of gold; a warm paper look by day. */
data class Pal(
    val dark: Boolean, val bg: Color, val surface: Color, val card: Color, val line: Color,
    val ink: Color, val muted: Color, val accent: Color, val onAccent: Color, val gold: Color, val danger: Color,
)

val Night = Pal(true, Color(0xFF0D1613), Color(0xFF14201C), Color(0xFF1B2A25), Color(0xFF263832),
    Color(0xFFEDF2EE), Color(0xFF9AAEA6), Color(0xFF3FBF8F), Color(0xFF05130D), Color(0xFFD9B45A), Color(0xFFE5675F))
val Day = Pal(false, Color(0xFFF5F1E6), Color(0xFFFFFDF7), Color(0xFFEDE7D6), Color(0xFFDDD5C1),
    Color(0xFF1C2925), Color(0xFF5D6C66), Color(0xFF1F7A5A), Color(0xFFFFFFFF), Color(0xFFA27A22), Color(0xFFC0443C))

val LocalPal = staticCompositionLocalOf { Night }
val P: Pal @Composable get() = LocalPal.current

/** A rounded panel. */
@Composable
fun Panel(modifier: Modifier = Modifier, pad: Dp = 16.dp, color: Color = P.surface, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(color).border(1.dp, P.line, shape).padding(pad), content = content)
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier) = Text(text, color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = modifier)

@Composable
fun Sub(text: String, modifier: Modifier = Modifier, size: TextUnit = 13.sp, color: Color = P.muted) = Text(text, color = color, fontSize = size, modifier = modifier)

/** A selectable choice: rounded rectangle, filled when on. */
@Composable
fun Choice(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(modifier.clip(shape).background(if (on) P.accent else P.card).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (on) P.onAccent else P.ink, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/** The main button. */
@Composable
fun Action(label: String, modifier: Modifier = Modifier, filled: Boolean = true, color: Color = P.accent, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.clip(shape).background(if (filled) color else Color.Transparent).border(1.dp, if (filled) color else P.line, shape)
        .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (filled) P.onAccent else color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingRow(title: String, sub: String?, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = P.ink, fontSize = 15.sp)
            if (sub != null) Sub(sub, size = 12.sp)
        }
        right()
    }
}

/** "3 ساعات و 12 دقيقة" / "12 دقيقة" / "45 ثانية". */
fun durationText(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60
    return when {
        h > 0 && m > 0 -> "$h ساعة و $m دقيقة"
        h > 0 -> "$h ساعة"
        m > 0 -> "$m دقيقة"
        else -> "${s % 60} ثانية"
    }
}

fun clockText(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
