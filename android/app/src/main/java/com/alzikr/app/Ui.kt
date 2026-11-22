package com.alzikr.app

import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
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
    val look = L
    if (look.flat) {
        // "Lines only" themes: no card, a thin rule under the content.
        Column(modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = pad * 0.6f), content = content)
            Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
        }
        return
    }
    val shape = look.shape()
    Column(modifier.fillMaxWidth().clip(shape).background(color).border(1.dp, P.line, shape).padding(pad), content = content)
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier) = GText(text, color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body, modifier = modifier)

@Composable
fun Sub(text: String, modifier: Modifier = Modifier, size: TextUnit = 13.sp, color: Color = P.muted) = GText(text, color = color, fontSize = size, modifier = modifier)

/** A selectable choice: rounded rectangle, filled when on. */
@Composable
fun Choice(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = L.shape(10.dp)
    Box(modifier.clip(shape).background(if (on) P.accent else P.card).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        GText(label, color = if (on) P.onAccent else P.ink, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, softWrap = false)
    }
}

/** The main button. */
@Composable
fun Action(label: String, modifier: Modifier = Modifier, filled: Boolean = true, color: Color = P.accent, onClick: () -> Unit) {
    val shape = L.shape(14.dp)
    Box(modifier.clip(shape).background(if (filled) color else Color.Transparent).border(1.dp, if (filled) color else P.line, shape)
        .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        GText(label, color = if (filled) P.onAccent else color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

/**
 * The dhikr counter in the theme's style: a ring, prayer beads (up to 33 around a circle, the
 * larger "imam" bead on top), or a large numeral with a thin bar.
 */
@Composable
fun Counter(count: Int, target: Int, done: Boolean, label: String, size: Dp) {
    val frac = if (target > 0) (count.toFloat() / target).coerceIn(0f, 1f) else 0f
    val ring = P.line; val fill = if (done) P.gold else P.accent; val imam = P.gold
    when (L.counter) {
        CounterStyle.PLAIN -> Column(Modifier.height(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("$count", color = if (done) P.gold else P.ink, fontSize = (size.value / 2.8f).sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body)
            Box(Modifier.width(size * 0.8f).height(4.dp).background(ring)) { Box(Modifier.fillMaxWidth(frac).height(4.dp).background(fill)) }
            Sub(label, Modifier.padding(top = 8.dp))
        }
        else -> Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            if (L.counter == CounterStyle.BEADS) {
                val n = if (target in 1..33) target else 33
                val lit = if (target in 1..33) count.coerceAtMost(n) else (count % n).let { if (it == 0 && count > 0 && frac >= 1f) n else it }
                androidx.compose.foundation.Canvas(Modifier.size(size)) {
                    val r = this.size.minDimension / 2 - 12.dp.toPx()
                    val bead = (2 * Math.PI * r / n * 0.34).toFloat().coerceAtMost(9.dp.toPx())
                    for (i in 0 until n) {
                        val ang = Math.toRadians(-90.0 + 360.0 * i / n)
                        val c = androidx.compose.ui.geometry.Offset(center.x + r * Math.cos(ang).toFloat(), center.y + r * Math.sin(ang).toFloat())
                        drawCircle(if (i < lit) fill else ring, if (i == 0) bead * 1.45f else bead, c)
                    }
                    drawCircle(imam, bead * 0.7f, androidx.compose.ui.geometry.Offset(center.x, center.y - r - bead * 2.2f))
                }
            } else {
                androidx.compose.foundation.Canvas(Modifier.size(size - 10.dp)) {
                    val w = 12.dp.toPx(); val s = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w); val o = androidx.compose.ui.geometry.Offset(w / 2, w / 2)
                    drawArc(ring, 0f, 360f, false, o, s, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
                    if (frac > 0) drawArc(fill, -90f, 360f * frac, false, o, s, style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", color = if (done) P.gold else P.ink, fontSize = (size.value / 3.6f).sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body)
                Sub(label, size = 12.sp)
            }
        }
    }
}

/** First-visit guidance: what this page is for and where to tap. Dismissed once with «فهمت». */
object Hints {
    fun seen(id: String) = Store.prefs.getBoolean("hint_$id", false)
    fun dismiss(id: String) { Store.prefs.edit().putBoolean("hint_$id", true).apply(); Store.bump() }
    fun resetAll() { val e = Store.prefs.edit(); Store.prefs.all.keys.filter { it.startsWith("hint_") }.forEach(e::remove); e.apply(); Store.bump() }
}

@Composable
fun Hint(id: String, icon: String, text: String, modifier: Modifier = Modifier) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    if (Hints.seen(id)) return
    val shape = L.shape(16.dp)
    Row(modifier.fillMaxWidth().clip(shape).background(P.accent.copy(alpha = .10f)).border(1.dp, P.accent.copy(alpha = .45f), shape).padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Glyph(icon, tint = P.accent, size = 24.dp)
        GText(text, color = P.ink, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        Text("فهمت", color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(L.shape(10.dp)).clickable { Hints.dismiss(id) }.padding(horizontal = 10.dp, vertical = 8.dp))
    }
}

/** «⬆ يتوفر تحديث» while a newer version is known, until the app is updated. */
@Composable
fun UpdateBanner() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val ver = androidx.compose.runtime.remember(v) { AppUpdate.available(ctx) } ?: return
    Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.accent).clickable { (ctx as? android.app.Activity)?.let { AppUpdate.checkNow(it) } }
        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        GText("⬆", color = P.onAccent, fontSize = 18.sp)
        Text("يتوفر تحديث ALzikr $ver · اضغط للتحديث", color = P.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
    }
}

/** A big labelled action: icon on top, a clear word under it. */
@Composable
fun Tile(icon: String, label: String, modifier: Modifier = Modifier, on: Boolean = false, onClick: () -> Unit) {
    val shape = L.shape(16.dp)
    Column(modifier.clip(shape).background(if (on) P.accent else P.card).border(1.dp, if (on) P.accent else P.line, shape).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Glyph(icon, tint = if (on) P.onAccent else P.accent, size = 26.dp)
        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        GText(label, color = if (on) P.onAccent else P.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** «تحديث متوفر» in the app's own theme: your version and the new one, what is new in each version since yours, then the download. */
@Composable
fun UpdateDialog() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val activity = ctx as? android.app.Activity
    AppUpdate.progress?.let { f ->
        androidx.compose.material3.AlertDialog(onDismissRequest = {}, containerColor = P.surface,
            title = { Text("تحديث ALzikr", color = P.ink, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(AppUpdate.stage, color = P.ink, fontSize = 15.sp)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                    if (f < 0f) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth(), color = P.accent, trackColor = P.line)
                    else androidx.compose.material3.LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth(), color = P.accent, trackColor = P.line)
                }
            }, confirmButton = {})
        return
    }
    val o = AppUpdate.offer ?: return
    androidx.compose.material3.AlertDialog(onDismissRequest = { AppUpdate.later(ctx) }, containerColor = P.surface,
        title = {
            Column {
                Text("تحديث متوفر", color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body)
                Text("ALzikr ${o.version} · لديك ${o.current}", color = P.gold, fontSize = 14.sp)
            }
        },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Text("ما الجديد", color = P.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                o.news.forEach { (ver, items) ->
                    Text(ver, color = P.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                    items.forEach { line ->
                        Row(Modifier.padding(top = 4.dp)) {
                            Text("•", color = P.accent, fontSize = 15.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(line, color = P.ink, fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { Action("تحديث الآن") { activity?.let { AppUpdate.accept(it) } } },
        dismissButton = { androidx.compose.material3.TextButton({ AppUpdate.later(ctx) }) { Text("لاحقًا", color = P.muted) } })
}
