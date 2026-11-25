package com.alzikr.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Themes that change how the app is laid out, not only its colors:
 *  - panels shaped as a mihrab arch, a rose petal, a desert pebble, a gilded frame, frosted glass or a parchment scroll;
 *  - navigation as lanterns hanging from a string, a rail on the side, or an arc of buttons like a flower's stem;
 *  - the five prayers as lanterns that light up, a sundial the sun travels, or a garden that blooms with each prayer;
 *  - the dhikr counter as a lantern filling with light, a flower opening petal by petal, or a crescent growing full.
 */
enum class PanelStyle { CARD, ARCH, PETAL, PEBBLE, FRAMED, GLASS, SCROLL }

/** The panel's outline. The arch rises to a point at the top, like a mihrab's window. */
fun panelShape(s: PanelStyle, r: Dp): Shape = when (s) {
    PanelStyle.ARCH -> GenericShape { size, _ ->
        val w = size.width; val a = min(w * .09f, size.height * .3f)
        moveTo(0f, size.height); lineTo(0f, a)
        cubicTo(0f, a * .35f, w * .38f, a * .3f, w / 2, 0f)
        cubicTo(w * .62f, a * .3f, w, a * .35f, w, a)
        lineTo(w, size.height); close()
    }
    PanelStyle.PETAL -> RoundedCornerShape(topStart = 34.dp, topEnd = 6.dp, bottomEnd = 34.dp, bottomStart = 6.dp)
    PanelStyle.PEBBLE -> RoundedCornerShape(topStart = 28.dp, topEnd = 44.dp, bottomEnd = 24.dp, bottomStart = 38.dp)
    PanelStyle.FRAMED -> RoundedCornerShape(3.dp)
    PanelStyle.SCROLL -> RoundedCornerShape(2.dp)
    PanelStyle.GLASS -> RoundedCornerShape(r.coerceAtLeast(18.dp))
    PanelStyle.CARD -> RoundedCornerShape(r)
}

/** A panel in the theme's own form (Panel calls this for every style but the plain card). */
@Composable
fun FancyPanel(modifier: Modifier, pad: Dp, color: Color, content: @Composable ColumnScope.() -> Unit) {
    val style = L.panel; val shape = panelShape(style, L.radius)
    val gold = P.gold; val line = P.line; val accent = P.accent; val dark = P.dark
    val top = when (style) { PanelStyle.ARCH -> 20.dp; PanelStyle.SCROLL -> 12.dp; else -> 0.dp }
    val bottom = if (style == PanelStyle.SCROLL) 12.dp else 0.dp
    val back: Brush = when (style) {
        PanelStyle.GLASS -> Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) .10f else .55f), color.copy(alpha = if (dark) .45f else .55f)))
        PanelStyle.PETAL -> Brush.linearGradient(listOf(color, Tints.mix(color, accent, .08f)))
        PanelStyle.PEBBLE -> Brush.radialGradient(listOf(Tints.mix(color, Color.White, if (dark) .03f else .25f), color))
        else -> Brush.verticalGradient(listOf(color, color))
    }
    val edge = when (style) { PanelStyle.FRAMED -> gold.copy(alpha = .8f); PanelStyle.GLASS -> Color.White.copy(alpha = if (dark) .22f else .7f)
        PanelStyle.ARCH -> gold.copy(alpha = .55f); PanelStyle.PEBBLE -> accent.copy(alpha = .3f); else -> line }
    Column(modifier.fillMaxWidth().clip(shape).background(back).border(if (style == PanelStyle.FRAMED) 1.5.dp else 1.dp, edge, shape)
        .drawBehind { decorate(style, gold, accent) }
        .padding(start = pad, end = pad, top = pad + top, bottom = pad + bottom), content = content)
}

private fun DrawScope.decorate(style: PanelStyle, gold: Color, accent: Color) {
    when (style) {
        PanelStyle.ARCH -> {           // a keystone at the arch's point and a thin inner arch
            val w = size.width; val a = min(w * .09f, size.height * .3f)
            val d = Path().apply { moveTo(w / 2, 5f); lineTo(w / 2 + 6f, 13f); lineTo(w / 2, 21f); lineTo(w / 2 - 6f, 13f); close() }
            drawPath(d, gold.copy(alpha = .7f))
            val inner = Path().apply { moveTo(9f, a + 14f); cubicTo(9f, a * .35f + 12f, w * .38f, a * .3f + 10f, w / 2, 28f); cubicTo(w * .62f, a * .3f + 10f, w - 9f, a * .35f + 12f, w - 9f, a + 14f) }
            drawPath(inner, gold.copy(alpha = .25f), style = Stroke(1f))
        }
        PanelStyle.FRAMED -> {         // a second gilded line inside and a small square at each corner
            val m = 5.dp.toPx()
            drawRect(gold.copy(alpha = .45f), Offset(m, m), Size(size.width - 2 * m, size.height - 2 * m), style = Stroke(.8.dp.toPx()))
            val q = 4.dp.toPx()
            listOf(Offset(m, m), Offset(size.width - m, m), Offset(m, size.height - m), Offset(size.width - m, size.height - m)).forEach { c ->
                rotate(45f, c) { drawRect(gold.copy(alpha = .8f), Offset(c.x - q / 2, c.y - q / 2), Size(q, q)) }
            }
        }
        PanelStyle.SCROLL -> {         // the paper rolled at the top and the bottom
            val h = 8.dp.toPx()
            listOf(0f, size.height - h).forEach { y ->
                drawRect(Brush.verticalGradient(listOf(gold.copy(alpha = .35f), gold.copy(alpha = .12f), gold.copy(alpha = .35f)), y, y + h), Offset(0f, y), Size(size.width, h))
                drawCircle(gold.copy(alpha = .5f), h / 2, Offset(h / 2, y + h / 2)); drawCircle(gold.copy(alpha = .5f), h / 2, Offset(size.width - h / 2, y + h / 2))
            }
        }
        PanelStyle.PETAL -> drawLine(accent.copy(alpha = .18f), Offset(size.width * .08f, size.height * .92f), Offset(size.width * .92f, size.height * .08f), 1.dp.toPx())
        PanelStyle.GLASS -> drawLine(Color.White.copy(alpha = .35f), Offset(size.width * .1f, 1.dp.toPx()), Offset(size.width * .6f, 1.dp.toPx()), 1.5.dp.toPx())
        else -> Unit
    }
}

// ------------------------------------------------------------------------------- navigation

/** Lanterns hanging from a string under the header; the chosen one glows. */
@Composable
fun LanternTabs() {
    val gold = P.gold; val line = P.line
    Box(Modifier.fillMaxWidth().height(92.dp)) {
        Canvas(Modifier.fillMaxWidth().height(20.dp)) {
            val p = Path().apply { moveTo(0f, 4f); quadraticTo(size.width / 2, 22f, size.width, 4f) }
            drawPath(p, line, style = Stroke(1.5f))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
            sections.forEachIndexed { i, s ->
                val on = Nav.tab == s.id
                val drop = (8 + (if (i % 2 == 0) 0 else 10) + (if (i == 0 || i == sections.lastIndex) 0 else 4)).dp
                Column(Modifier.weight(1f).clickable { Nav.go(s.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(1.5.dp).height(drop).background(line))
                    Box(Modifier.size(38.dp, 44.dp).drawBehind { lantern(this, if (on) gold else line, on, 1f) }, contentAlignment = Alignment.Center) {
                        Glyph(s.glyph, tint = if (on) P.onAccent else P.muted, size = 16.dp)
                    }
                    Text(s.label, color = if (on) gold else P.muted, fontSize = 10.sp, maxLines = 1, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

/** A lantern: a cap, a six-sided glass body and a point below; [fill] 0..1 of light from the bottom. */
fun lantern(d: DrawScope, c: Color, lit: Boolean, fill: Float) = with(d) {
    val w = size.width; val h = size.height
    val body = Path().apply {
        moveTo(w * .3f, h * .14f); lineTo(w * .7f, h * .14f); lineTo(w * .92f, h * .38f); lineTo(w * .86f, h * .78f)
        lineTo(w * .62f, h * .9f); lineTo(w * .38f, h * .9f); lineTo(w * .14f, h * .78f); lineTo(w * .08f, h * .38f); close()
    }
    if (lit) drawCircle(Brush.radialGradient(listOf(c.copy(alpha = .45f), Color.Transparent), Offset(w / 2, h * .5f), w), w, Offset(w / 2, h * .5f))
    drawRect(c, Offset(w * .36f, 0f), Size(w * .28f, h * .14f))
    if (fill > 0f) clipPath(body) {
        drawRect(Brush.verticalGradient(listOf(c.copy(alpha = if (lit) .95f else .5f), c.copy(alpha = if (lit) .7f else .3f))), Offset(0f, h * (.9f - .76f * fill)), Size(w, h))
    }
    drawPath(body, c, style = Stroke(1.6f))
    drawLine(c.copy(alpha = .6f), Offset(w * .5f, h * .14f), Offset(w * .5f, h * .9f), 1f)
    drawLine(c, Offset(w / 2, h * .9f), Offset(w / 2, h), 2f)
}

/** A slim rail on the side (the right, in Arabic) with every section. */
@Composable
fun SideRail() {
    Column(Modifier.width(66.dp).fillMaxHeight().padding(vertical = 8.dp).clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)).background(P.surface)
        .padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        sections.forEach { s ->
            val on = Nav.tab == s.id
            Column(Modifier.width(56.dp).clip(RoundedCornerShape(18.dp)).background(if (on) P.accent else Color.Transparent).clickable { Nav.go(s.id) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                SectionIcon(s, on, 22, tint = if (on) P.onAccent else P.muted)
                Text(s.label, color = if (on) P.onAccent else P.muted, fontSize = 10.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

/** The sections on an arc at the bottom, the middle ones higher, like petals on a stem. */
@Composable
fun ArcNav() {
    val n = sections.size; val card = P.card; val line = P.line
    Box(Modifier.fillMaxWidth().navigationBarsPadding().height(92.dp)) {
        Canvas(Modifier.fillMaxWidth().height(92.dp)) {
            val p = Path().apply { moveTo(0f, size.height); lineTo(0f, size.height * .55f); quadraticTo(size.width / 2, -size.height * .1f, size.width, size.height * .55f); lineTo(size.width, size.height); close() }
            drawPath(p, card); drawPath(p, line, style = Stroke(1.2f))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.Top) {
            sections.forEachIndexed { i, s ->
                val on = Nav.tab == s.id
                val x = (i + .5f) / n * 2 - 1                       // -1 … 1
                val lift = ((1 - x * x) * 26).dp
                Column(Modifier.weight(1f).padding(top = (34.dp - lift).coerceAtLeast(4.dp)).clickable { Nav.go(s.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(if (on) 46.dp else 38.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (on) P.accent else P.surface)
                        .border(1.dp, if (on) P.gold else line, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                        SectionIcon(s, on, 20, tint = if (on) P.onAccent else P.muted)
                    }
                    Text(s.label, color = if (on) P.accent else P.muted, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------- the five prayers

private val five get() = Prayer.entries.filter { it.alertable }

@Composable
private fun pulse(): androidx.compose.runtime.State<Float> {
    // Read only while drawing (.value inside the draw code), so the pulse redraws the lantern, never recomposes the screen.
    val t = rememberInfiniteTransition(label = "pulse")
    return t.animateFloat(.55f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "p")
}

/** Five lanterns hanging at different heights: lit gold for a prayer prayed, glowing for the next one. */
@Composable
fun PrayerLanterns(w: Window, times: Map<Prayer, Int>, open: (Prayer) -> Unit) {
    val today = LocalDate.now(); val glow = pulse(); val gold = P.gold; val accent = P.accent; val line = P.line
    Panel(pad = 8.dp) {
        Row(Modifier.fillMaxWidth()) {
            five.forEachIndexed { i, p ->
                val prayed = Data.prayed(today, p); val next = p == w.next
                Column(Modifier.weight(1f).clip(L.shape(10.dp)).clickable { open(p) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(1.5.dp).height((6 + (i % 3) * 10).dp).background(line))
                    Box(Modifier.size(42.dp, 52.dp).drawBehind { val g = glow.value; lantern(this, when { prayed -> gold; next -> accent.copy(alpha = g); else -> line }, prayed || next, if (prayed) 1f else if (next) .45f * g else 0f) })
                    Text(p.ar, color = if (next) accent else P.ink, fontSize = 13.sp, fontWeight = if (next) FontWeight.Bold else FontWeight.Normal)
                    Text(Alerts.timeText(times.getValue(p)), color = P.gold, fontSize = 12.sp)
                }
            }
        }
        Sub("كل صلاة تصلّيها تُضيء فانوسها", Modifier.padding(top = 6.dp).align(Alignment.CenterHorizontally), size = 11.sp)
    }
}

/** A sundial: the day's arc from fajr (right) to isha (left), the prayers on it, and the sun where it is now. */
@Composable
fun PrayerSundial(w: Window, times: Map<Prayer, Int>, open: (Prayer) -> Unit) {
    val today = LocalDate.now(); val now = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val gold = P.gold; val accent = P.accent; val line = P.line; val ink = P.muted; val surface = P.surface
    val start = times.getValue(Prayer.FAJR); val end = times.getValue(Prayer.ISHA)
    fun frac(m: Int) = ((m - start).toFloat() / (end - start).coerceAtLeast(1)).coerceIn(0f, 1f)
    Panel(pad = 12.dp) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val r = min(size.width / 2 - 18f, size.height - 18f); val c = Offset(size.width / 2, size.height - 6f)
            val tl = Offset(c.x - r, c.y - r); val sz = Size(2 * r, 2 * r)
            for (k in 0..12) { val a = (180f + k * 15f) * PI.toFloat() / 180f; drawLine(line, Offset(c.x + cos(a) * (r - 10f), c.y + sin(a) * (r - 10f)), Offset(c.x + cos(a) * r, c.y + sin(a) * r), 1.5f) }
            drawArc(line, 180f, 180f, false, tl, sz, style = Stroke(5f))
            val f = frac(now)
            if (now in start..end) drawArc(accent, 360f - 180f * f, 180f * f, false, tl, sz, style = Stroke(5f))
            Prayer.entries.forEach { p ->
                val a = (360f - 180f * frac(times.getValue(p))) * PI.toFloat() / 180f
                val pt = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
                val prayed = p.alertable && Data.prayed(today, p)
                drawCircle(if (prayed) gold else if (p == w.next) accent else line, if (p.alertable) 8f else 5f, pt)
                if (p == w.next) drawCircle(accent, 13f, pt, style = Stroke(2f))
            }
            if (now in start..end) {          // the sun now
                val a = (360f - 180f * f) * PI.toFloat() / 180f
                val s = Offset(c.x + cos(a) * (r - 30f), c.y + sin(a) * (r - 30f))
                drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = .5f), Color.Transparent), s, 34f), 34f, s); drawCircle(gold, 10f, s)
            } else {                          // the moon at night
                drawCircle(ink.copy(alpha = .6f), 12f, Offset(c.x, c.y - r * .45f)); drawCircle(surface, 11f, Offset(c.x + 6f, c.y - r * .45f - 3f))
            }
            drawLine(line, Offset(c.x, c.y), Offset(c.x, c.y - r * .25f), 3f)    // the gnomon
        }
        Row(Modifier.fillMaxWidth()) {
            Prayer.entries.forEach { p ->
                Column(Modifier.weight(1f).clip(L.shape(8.dp)).clickable { open(p) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.ar, color = if (p == w.next) P.accent else P.ink, fontSize = 12.sp, fontWeight = if (p == w.next) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    Text(Alerts.timeText(times.getValue(p)), color = P.gold, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

/** A garden of five plants (roses, cacti or crystals, by the theme): each prayer prayed makes its plant bloom. */
@Composable
fun PrayerGarden(w: Window, times: Map<Prayer, Int>, open: (Prayer) -> Unit) {
    val today = LocalDate.now(); val glow = pulse(); val motif = L.motif
    val accent = P.accent; val gold = P.gold; val line = P.line
    Panel(pad = 8.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            five.forEach { p ->
                val prayed = Data.prayed(today, p); val next = p == w.next
                Column(Modifier.weight(1f).clip(L.shape(10.dp)).clickable { open(p) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(52.dp, 72.dp)) { plant(motif, prayed, next, glow.value, accent, gold, line) }
                    Text(p.ar, color = if (next) accent else P.ink, fontSize = 13.sp, fontWeight = if (next) FontWeight.Bold else FontWeight.Normal)
                    Text(Alerts.timeText(times.getValue(p)), color = P.gold, fontSize = 12.sp)
                }
            }
        }
        Sub(when (motif) { "cactus" -> "كل صلاة تصلّيها تُزهر صبّارتها"; "crystal" -> "كل صلاة تصلّيها تُضيء بلّورتها"; else -> "كل صلاة تصلّيها تتفتّح وردتها" },
            Modifier.padding(top = 6.dp).align(Alignment.CenterHorizontally), size = 11.sp)
    }
}

private fun DrawScope.plant(motif: String, prayed: Boolean, next: Boolean, glow: Float, accent: Color, gold: Color, line: Color) {
    val w = size.width; val h = size.height; val green = Color(0xFF4E9A5A)
    when (motif) {
        "cactus" -> {
            drawRoundRect(Color(0xFFB0603A).copy(alpha = .8f), Offset(w * .28f, h * .82f), Size(w * .44f, h * .18f), androidx.compose.ui.geometry.CornerRadius(4f))   // the pot
            val body = if (prayed || next) green else green.copy(alpha = .45f)
            drawRoundRect(body, Offset(w * .4f, h * .3f), Size(w * .2f, h * .54f), androidx.compose.ui.geometry.CornerRadius(w * .1f))
            drawRoundRect(body, Offset(w * .18f, h * .45f), Size(w * .14f, h * .24f), androidx.compose.ui.geometry.CornerRadius(w * .07f))
            drawRoundRect(body, Offset(w * .68f, h * .38f), Size(w * .14f, h * .22f), androidx.compose.ui.geometry.CornerRadius(w * .07f))
            drawLine(body, Offset(w * .3f, h * .62f), Offset(w * .4f, h * .62f), w * .08f); drawLine(body, Offset(w * .6f, h * .55f), Offset(w * .7f, h * .55f), w * .08f)
            if (prayed) for (k in 0 until 6) { val a = k * PI.toFloat() / 3; drawCircle(Color(0xFFF06A9A), w * .07f, Offset(w * .5f + cos(a) * w * .08f, h * .27f + sin(a) * w * .08f)) }
            if (prayed) drawCircle(gold, w * .05f, Offset(w * .5f, h * .27f))
            if (next) drawCircle(accent.copy(alpha = .4f * glow), w * .3f, Offset(w * .5f, h * .5f), style = Stroke(2f))
        }
        "crystal" -> {
            val shard = Path().apply { moveTo(w * .5f, h * .08f); lineTo(w * .74f, h * .36f); lineTo(w * .66f, h * .9f); lineTo(w * .34f, h * .9f); lineTo(w * .26f, h * .36f); close() }
            if (prayed) {
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .4f), Color.Transparent), Offset(w / 2, h / 2), w), w, Offset(w / 2, h / 2))
                drawPath(shard, Brush.verticalGradient(listOf(Color.White.copy(alpha = .9f), accent.copy(alpha = .8f), gold.copy(alpha = .7f))))
            }
            drawPath(shard, if (next) accent.copy(alpha = glow) else if (prayed) Color.White.copy(alpha = .8f) else line, style = Stroke(2f))
            drawLine((if (prayed) Color.White else line).copy(alpha = .6f), Offset(w * .5f, h * .08f), Offset(w * .5f, h * .9f), 1f)
        }
        else -> {          // a rose
            drawLine(green, Offset(w / 2, h), Offset(w / 2, h * .35f), 3f)
            drawOval(green, Offset(w * .5f, h * .6f), Size(w * .3f, h * .1f)); drawOval(green, Offset(w * .2f, h * .72f), Size(w * .3f, h * .1f))
            val c = Offset(w / 2, h * .3f)
            if (prayed) {
                for (k in 0 until 6) { val a = k * PI.toFloat() / 3 + .3f; drawCircle(accent.copy(alpha = .85f), w * .14f, Offset(c.x + cos(a) * w * .13f, c.y + sin(a) * w * .13f)) }
                drawCircle(Tints.mix(accent, Color.Black, .25f), w * .12f, c); drawCircle(gold.copy(alpha = .8f), w * .04f, c)
            } else {
                val bud = Path().apply { moveTo(c.x, c.y - h * .12f); quadraticTo(c.x + w * .14f, c.y, c.x, c.y + h * .06f); quadraticTo(c.x - w * .14f, c.y, c.x, c.y - h * .12f); close() }
                drawPath(bud, (if (next) accent else line).copy(alpha = if (next) glow else .8f))
            }
        }
    }
}

// ------------------------------------------------------------------------------- the dhikr counter

/** The new counters: a lantern filling with light, a flower opening petal by petal, a crescent growing to a full moon. */
@Composable
fun FancyCounter(style: CounterStyle, count: Int, target: Int, done: Boolean, label: String, size: Dp) {
    val frac = if (target > 0) (count.toFloat() / target).coerceIn(0f, 1f) else 0f
    val fill = if (done) P.gold else P.accent; val line = P.line; val gold = P.gold; val bg = P.surface
    when (style) {
        CounterStyle.FLOWER -> Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            val n = if (target in 1..33) target else 33
            val lit = if (target in 1..33) count.coerceAtMost(n) else (count % n).let { if (it == 0 && count > 0 && frac >= 1f) n else it }
            Canvas(Modifier.size(size)) {
                val c = center; val R = this.size.minDimension / 2
                for (i in 0 until n) {
                    val deg = -90f + 360f * i / n
                    rotate(deg + 90f, c) {
                        drawOval(if (i < lit) fill.copy(alpha = .85f) else line.copy(alpha = .7f), Offset(c.x - R * .07f, c.y - R * .98f), Size(R * .14f, R * .5f))
                    }
                }
                drawCircle(gold.copy(alpha = .25f), R * .42f, c)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", color = if (done) P.gold else P.ink, fontSize = (size.value / 4.2f).sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body)
                Sub(label, size = 11.sp)
            }
        }
        CounterStyle.LANTERN -> Column(Modifier.height(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val flick = pulse()
            Box(Modifier.size(size * .5f, size * .62f).drawBehind {
                lantern(this, if (count > 0) fill else line, count > 0, frac)
                if (count > 0) drawOval(Color(0xFFFFE08A).copy(alpha = .9f * flick.value), Offset(this.size.width * .44f, this.size.height * (.86f - .7f * frac).coerceAtLeast(.3f)), Size(this.size.width * .12f, this.size.height * .1f))
            })
            Text("$count", color = if (done) P.gold else P.ink, fontSize = (size.value / 5f).sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body)
            Sub(label, size = 11.sp)
        }
        else -> Column(Modifier.height(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {   // CRESCENT
            Canvas(Modifier.size(size * .55f)) {
                val r = this.size.minDimension / 2 * .8f; val c = center
                for (k in 0 until 6) { val a = k * 1.1f; drawCircle(gold.copy(alpha = .35f), 2f, Offset(c.x + cos(a) * r * 1.2f, c.y + sin(a) * r * 1.15f)) }
                drawCircle(line.copy(alpha = .5f), r, c)
                val moon = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
                val shade = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x - 2 * r * frac.coerceAtLeast(.08f), c.y), r)) }
                val lit = Path().apply { op(moon, shade, PathOperation.Difference) }
                drawPath(lit, Brush.radialGradient(listOf(Color(0xFFFFF4C8), fill), c, r * 1.2f))
                if (frac >= 1f) drawCircle(Brush.radialGradient(listOf(fill.copy(alpha = .35f), Color.Transparent), c, r * 1.6f), r * 1.6f, c)
            }
            Text("$count", color = if (done) P.gold else P.ink, fontSize = (size.value / 5f).sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body)
            Sub(label, size = 11.sp)
        }
    }
}
