package com.alzikr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The friendship themes' living backgrounds, drawn behind the app (never over the text, always faint):
 * a lantern's glow, a garden's vines, zellige stars, rain, the aurora, an illuminated manuscript's gilded frame, a sky that
 * follows the real prayer times, twinkling stars of the last third of the night, and rays of light.
 * They move slowly at about 12 frames a second, and stand still when "reduce motion" or "save battery" is on.
 */
enum class Backdrop(val moving: Boolean) {
    NONE(false), LANTERN(true), VINES(true), GIRIH(false), RAIN(true), AURORA(true), ILLUMINATED(false), SKY(true), STARS(true), RAYS(true),
    SNOW(true), PETALS(true), FIREFLIES(true), BUBBLES(true), DUNES(true), ROSETTE(true), SKYLINE(true),
    BUNTING(true), MESA(true), DAMASK(false), ORBS(true), INK(true), CACTI(true),
}

@Composable
fun ThemeBackdrop(b: Backdrop, p: Pal) {
    if (b == Backdrop.NONE) return
    val still = com.alzikr.app.pets.PetPerf.userReduceMotion || com.alzikr.app.pets.PetPerf.userSaveBattery
    var t by remember { mutableFloatStateOf(0f) }
    if (b.moving && !still) LaunchedEffect(b) { val start = System.nanoTime(); while (true) { delay(83); t = (System.nanoTime() - start) / 1e9f } }
    // Its own layer: the slow motion redraws the background only, never the app over it.
    Canvas(Modifier.fillMaxSize().graphicsLayer()) { drawBackdrop(b, p, t) }
}

/** Draws [b] at time [t] (seconds); the theme gallery uses it for a still preview. */
fun DrawScope.drawBackdrop(b: Backdrop, p: Pal, t: Float) {
        if (size.minDimension < 40f) return          // nothing to draw on yet (and the tiled patterns need room)
        when (b) {
            Backdrop.LANTERN -> lantern(p, t)
            Backdrop.VINES -> vines(p, t)
            Backdrop.GIRIH -> girih(p)
            Backdrop.RAIN -> rain(p, t)
            Backdrop.AURORA -> aurora(p, t)
            Backdrop.ILLUMINATED -> illuminated(p)
            Backdrop.SKY -> sky(p, t)
            Backdrop.STARS -> stars(p, t)
            Backdrop.RAYS -> rays(p, t)
            Backdrop.SNOW -> snow(p, t)
            Backdrop.PETALS -> petals(p, t)
            Backdrop.FIREFLIES -> fireflies(p, t)
            Backdrop.BUBBLES -> bubbles(p, t)
            Backdrop.DUNES -> dunes(p, t)
            Backdrop.ROSETTE -> rosette(p, t)
            Backdrop.SKYLINE -> skyline(p, t)
            Backdrop.BUNTING -> bunting(p, t)
            Backdrop.MESA -> mesa(p, t)
            Backdrop.DAMASK -> damask(p)
            Backdrop.ORBS -> orbs(p, t)
            Backdrop.INK -> ink(p, t)
            Backdrop.CACTI -> cacti(p, t)
            Backdrop.NONE -> Unit
        }
}

private fun rnd(i: Int, k: Int): Float { val x = sin(i * 12.9898f + k * 78.233f) * 43758.547f; return x - kotlin.math.floor(x) }

/** «مشكاة»: a hanging lamp's warm glow at the top, breathing like a flame. */
private fun DrawScope.lantern(p: Pal, t: Float) {
    val flick = .16f + .03f * sin(t * 2.3f) + .015f * sin(t * 5.1f)
    val c = Offset(size.width / 2, size.height * .06f)
    drawCircle(Brush.radialGradient(listOf(p.gold.copy(alpha = flick), Color.Transparent), c, size.width * .85f), size.width * .85f, c)
    // the lamp itself: a chain and a small glowing glass
    drawLine(p.gold.copy(alpha = .25f), Offset(c.x, 0f), Offset(c.x, size.height * .045f), 2f)
    val lamp = Path().apply {
        val y = size.height * .045f; val w = size.width * .035f
        moveTo(c.x - w, y); lineTo(c.x + w, y); lineTo(c.x + w * .55f, y + w * 2.2f); lineTo(c.x - w * .55f, y + w * 2.2f); close()
    }
    drawPath(lamp, p.gold.copy(alpha = .22f + flick * .4f))
    for (i in 0 until 7) {          // embers drifting up
        val ph = (t * .05f + rnd(i, 1)) % 1f
        drawCircle(p.gold.copy(alpha = .18f * (1 - ph)), 2.2f, Offset(c.x + (rnd(i, 2) - .5f) * size.width * .5f + sin(t + i) * 8f, size.height * (.35f - ph * .3f)))
    }
}

/** «روضة»: vines with leaves and small blossoms climbing the two edges, swaying a little. */
private fun DrawScope.vines(p: Pal, t: Float) {
    val leaf = p.accent.copy(alpha = .14f); val stem = p.accent.copy(alpha = .12f); val bloom = p.gold.copy(alpha = .2f)
    for (side in 0..1) {
        val x0 = if (side == 0) 0f else size.width
        val dir = if (side == 0) 1f else -1f
        val path = Path(); path.moveTo(x0, size.height)
        val n = 9
        for (i in 1..n) {
            val y = size.height * (1 - i / n.toFloat() * .85f)
            val sway = sin(t * .6f + i + side) * 6f
            path.quadraticTo(x0 + dir * (40f + 30f * (i % 2)) + sway, y + size.height * .045f, x0 + dir * (18f + 14f * (i % 3)) + sway, y)
            val lx = x0 + dir * (26f + 14f * (i % 3)) + sway; val r = 13f
            drawOval(leaf, Offset(lx - r, y - r * .55f), Size(r * 2, r * 1.1f))
            if (i % 3 == 0) drawCircle(bloom, 6f + 1.5f * sin(t + i), Offset(lx + dir * 16f, y - 12f))
        }
        drawPath(path, stem, style = Stroke(3f))
    }
}

/** «زليج»: a lattice of eight-pointed stars (two squares turned 45°), as on Andalusian tiles. */
private fun DrawScope.girih(p: Pal) {
    val s = size.width / 6f; val line = p.accent.copy(alpha = .07f); val dot = p.gold.copy(alpha = .1f)
    var y = 0f; var row = 0
    while (y < size.height + s) {
        var x = if (row % 2 == 0) 0f else s / 2
        while (x < size.width + s) {
            val r = s * .36f
            val sq = Path().apply { moveTo(x - r, y - r); lineTo(x + r, y - r); lineTo(x + r, y + r); lineTo(x - r, y + r); close() }
            drawPath(sq, line, style = Stroke(1.6f))
            rotate(45f, Offset(x, y)) { drawPath(sq, line, style = Stroke(1.6f)) }
            drawCircle(dot, r * .22f, Offset(x, y))
            x += s
        }
        y += s * .87f; row++
    }
}

/** «غيث»: a gentle rain, and rings spreading where it lands. */
private fun DrawScope.rain(p: Pal, t: Float) {
    val c = p.accent.copy(alpha = .13f)
    for (i in 0 until 46) {
        val speed = .22f + rnd(i, 3) * .18f
        val ph = (t * speed + rnd(i, 1)) % 1f
        val x = rnd(i, 2) * size.width - ph * 30f
        val y = ph * size.height * 1.1f - 40f
        drawLine(c, Offset(x, y), Offset(x - 5f, y + 22f), 1.8f)
    }
    for (i in 0 until 5) {
        val ph = (t * .35f + i / 5f) % 1f
        drawCircle(c.copy(alpha = .14f * (1 - ph)), 6f + ph * 40f, Offset(rnd(i, 7) * size.width, size.height * (.86f + rnd(i, 8) * .1f)), style = Stroke(1.5f))
    }
}

/** «شفق»: three ribbons of the northern lights waving over the top of the screen. */
private fun DrawScope.aurora(p: Pal, t: Float) {
    val colors = listOf(Color(0xFF3CF0B0), Color(0xFF7A8CFF), Color(0xFFC77DFF))
    colors.forEachIndexed { k, col ->
        val path = Path(); val base = size.height * (.12f + k * .07f)
        path.moveTo(0f, base)
        var x = 0f
        while (x <= size.width + 20f) {
            path.lineTo(x, base + sin(x / size.width * 2 * PI.toFloat() * (1.2f + k * .3f) + t * (.35f + k * .1f)) * size.height * .035f); x += 20f
        }
        path.lineTo(size.width, 0f); path.lineTo(0f, 0f); path.close()
        drawPath(path, Brush.verticalGradient(listOf(Color.Transparent, col.copy(alpha = .10f), Color.Transparent), 0f, base + size.height * .12f))
    }
}

/** «مخطوطة مذهّبة»: a gilded double frame with rosettes at the corners, like an illuminated page. */
private fun DrawScope.illuminated(p: Pal) {
    val g = p.gold.copy(alpha = .28f); val m = 10f
    drawRect(g, Offset(m, m), Size(size.width - 2 * m, size.height - 2 * m), style = Stroke(2.2f))
    drawRect(g.copy(alpha = .16f), Offset(m + 7, m + 7), Size(size.width - 2 * m - 14, size.height - 2 * m - 14), style = Stroke(1f))
    listOf(Offset(m, m), Offset(size.width - m, m), Offset(m, size.height - m), Offset(size.width - m, size.height - m)).forEach { c ->
        for (k in 0 until 8) { val a = k * PI.toFloat() / 4; drawCircle(g, 4f, Offset(c.x + cos(a) * 11f, c.y + sin(a) * 11f)) }
        drawCircle(g, 6f, c)
    }
    // a band of small diamonds along the top
    var x = m + 30f
    while (x < size.width - m - 30f) {
        val d = Path().apply { moveTo(x, m + 18f); lineTo(x + 5f, m + 23f); lineTo(x, m + 28f); lineTo(x - 5f, m + 23f); close() }
        drawPath(d, g.copy(alpha = .2f)); x += 22f
    }
}

/** «سماء الوقت»: the sky of this hour: the sun's arc by day, the moon and stars by night (see [Sky]). */
private fun DrawScope.sky(p: Pal, t: Float) {
    val ph = Sky.phase()
    val top = when (ph) { Sky.Phase.NIGHT -> Color(0xFF0B1230); Sky.Phase.DAWN -> Color(0xFF6A5AA8); Sky.Phase.MORNING -> Color(0xFF8FD0F5)
        Sky.Phase.NOON -> Color(0xFF6BC3F2); Sky.Phase.AFTERNOON -> Color(0xFFF2C27A); Sky.Phase.SUNSET -> Color(0xFFF07A55); Sky.Phase.DUSK -> Color(0xFF4A3A78) }
    drawRect(Brush.verticalGradient(listOf(top.copy(alpha = .30f), Color.Transparent), 0f, size.height * .45f))
    val f = Sky.dayFraction()
    if (ph == Sky.Phase.NIGHT || ph == Sky.Phase.DUSK) {
        for (i in 0 until 40) {
            val tw = .5f + .5f * sin(t * (1f + rnd(i, 4) * 2f) + i)
            drawCircle(Color.White.copy(alpha = .10f + .18f * tw), 1.2f + rnd(i, 5) * 1.4f, Offset(rnd(i, 1) * size.width, rnd(i, 2) * size.height * .4f))
        }
        val mc = Offset(size.width * .78f, size.height * .09f)
        drawCircle(Color(0xFFF4EBC8).copy(alpha = .5f), 22f, mc)
        drawCircle(top.copy(alpha = .9f), 20f, Offset(mc.x + 9f, mc.y - 5f))
    } else {
        val x = size.width * (1 - f); val y = size.height * (.2f - sin(f * PI.toFloat()) * .14f)
        val sun = if (ph == Sky.Phase.SUNSET || ph == Sky.Phase.DAWN) Color(0xFFFF9A5A) else Color(0xFFFFD45A)
        drawCircle(Brush.radialGradient(listOf(sun.copy(alpha = .35f), Color.Transparent), Offset(x, y), 120f), 120f, Offset(x, y))
        drawCircle(sun.copy(alpha = .55f), 18f, Offset(x, y))
        for (i in 0 until 3) {           // clouds drifting
            val cx = ((rnd(i, 1) * size.width + t * (6f + i * 3f)) % (size.width + 160f)) - 80f; val cy = size.height * (.06f + i * .07f)
            for (k in 0..2) drawOval(Color.White.copy(alpha = .16f), Offset(cx + k * 26f, cy - (if (k == 1) 10f else 0f)), Size(54f, 30f))
        }
    }
}

/** «سَحَر»: a field of stars twinkling, and now and then a shooting star. */
private fun DrawScope.stars(p: Pal, t: Float) {
    for (i in 0 until 70) {
        val tw = .5f + .5f * sin(t * (.8f + rnd(i, 4) * 2.4f) + i * 1.7f)
        val c = if (i % 9 == 0) p.gold else Color.White
        drawCircle(c.copy(alpha = .06f + .22f * tw), .9f + rnd(i, 5) * 1.6f, Offset(rnd(i, 1) * size.width, rnd(i, 2) * size.height))
    }
    val cycle = t % 9f
    if (cycle < 1.2f) {
        val k = (t / 9f).toInt(); val q = cycle / 1.2f
        val sx = size.width * (.2f + rnd(k, 6) * .6f); val sy = size.height * (.05f + rnd(k, 7) * .25f)
        val e = Offset(sx - q * 220f, sy + q * 110f)
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = .5f * (1 - abs(q - .5f) * 2))), Offset(e.x + 90f, e.y - 45f), e), Offset(e.x + 90f, e.y - 45f), e, 2f)
    }
}

/** «نور»: soft rays of light turning slowly from above. */
private fun DrawScope.rays(p: Pal, t: Float) {
    val c = Offset(size.width / 2, -size.height * .05f)
    rotate(sin(t * .08f) * 8f, c) {
        for (i in 0 until 12) {
            val a = (PI.toFloat() * (.15f + i / 11f * .7f))
            val len = size.height * 1.1f; val w = .045f
            val path = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + cos(a - w) * len, c.y + sin(a - w) * len); lineTo(c.x + cos(a + w) * len, c.y + sin(a + w) * len); close()
            }
            drawPath(path, Brush.radialGradient(listOf(p.gold.copy(alpha = .10f + .04f * sin(t * .7f + i)), Color.Transparent), c, len))
        }
    }
}

/** «ثلج»: snowflakes falling and swaying, small and large. */
private fun DrawScope.snow(p: Pal, t: Float) {
    val c = if (p.dark) Color.White else Color(0xFF9CC4E4)
    for (i in 0 until 55) {
        val speed = .04f + rnd(i, 3) * .06f; val r = 1.5f + rnd(i, 4) * 3.5f
        val ph = (t * speed + rnd(i, 1)) % 1f
        val x = rnd(i, 2) * size.width + sin(t * .7f + i) * 14f
        drawCircle(c.copy(alpha = .14f + rnd(i, 5) * .16f), r, Offset(x, ph * (size.height + 20f) - 10f))
    }
}

/** «بتلات»: petals drifting down on the breeze, turning as they fall. */
private fun DrawScope.petals(p: Pal, t: Float) {
    val cols = listOf(Color(0xFFF4A7C0), Color(0xFFF8C8D6), Color(0xFFE88AA8))
    for (i in 0 until 26) {
        val ph = (t * (.035f + rnd(i, 3) * .04f) + rnd(i, 1)) % 1f
        val x = ((rnd(i, 2) * size.width + ph * size.width * .35f + sin(t * .5f + i) * 20f) % (size.width + 40f)) - 20f
        val y = ph * (size.height + 30f) - 15f
        rotate(t * 40f * (if (i % 2 == 0) 1 else -1) + i * 30f, Offset(x, y)) {
            drawOval(cols[i % 3].copy(alpha = .22f), Offset(x - 7f, y - 4f), Size(14f, 8f))
        }
    }
}

/** «يراعات»: fireflies wandering slowly, each glowing and fading on its own. */
private fun DrawScope.fireflies(p: Pal, t: Float) {
    val c = Color(0xFFE8F57A)
    for (i in 0 until 22) {
        val x = size.width * (.5f + .45f * sin(t * (.05f + rnd(i, 3) * .06f) + i * 2.1f))
        val y = size.height * (.5f + .45f * sin(t * (.04f + rnd(i, 4) * .05f) + i * 1.3f + 1f))
        val glow = (.5f + .5f * sin(t * (1.2f + rnd(i, 5)) + i)).let { it * it }
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = .30f * glow), Color.Transparent), Offset(x, y), 26f), 26f, Offset(x, y))
        drawCircle(c.copy(alpha = .35f + .5f * glow), 2.4f, Offset(x, y))
    }
}

/** «أعماق»: bubbles rising and light rippling like the sea floor. */
private fun DrawScope.bubbles(p: Pal, t: Float) {
    for (k in 0 until 5) {         // light bands (caustics)
        val path = Path(); val y0 = size.height * (.15f + k * .18f)
        path.moveTo(0f, y0); var x = 0f
        while (x <= size.width + 20f) { path.lineTo(x, y0 + sin(x / 60f + t * .8f + k) * 10f); x += 20f }
        drawPath(path, Color(0xFF7FE0F0).copy(alpha = .06f), style = Stroke(10f))
    }
    for (i in 0 until 30) {
        val ph = (t * (.05f + rnd(i, 3) * .07f) + rnd(i, 1)) % 1f
        val x = rnd(i, 2) * size.width + sin(t * 1.3f + i) * 8f; val y = size.height * (1.05f - ph * 1.1f)
        val r = 3f + rnd(i, 4) * 7f
        drawCircle(Color.White.copy(alpha = .16f), r, Offset(x, y), style = Stroke(1.4f))
        drawCircle(Color.White.copy(alpha = .22f), r * .25f, Offset(x - r * .35f, y - r * .35f))
    }
}

/** «كثبان»: layered dunes at the bottom and sand blown across them. */
private fun DrawScope.dunes(p: Pal, t: Float) {
    val tones = listOf(p.gold.copy(alpha = .10f), p.accent.copy(alpha = .10f), p.gold.copy(alpha = .16f))
    tones.forEachIndexed { k, c ->
        val path = Path(); val base = size.height * (.78f + k * .07f)
        path.moveTo(0f, size.height); path.lineTo(0f, base); var x = 0f
        while (x <= size.width + 20f) { path.lineTo(x, base + sin(x / size.width * PI.toFloat() * (1.4f + k * .5f) + k * 1.7f) * size.height * .035f); x += 16f }
        path.lineTo(size.width, size.height); path.close()
        drawPath(path, c)
    }
    for (i in 0 until 14) {       // wind
        val ph = (t * (.12f + rnd(i, 3) * .1f) + rnd(i, 1)) % 1f
        val x = ph * (size.width + 120f) - 60f; val y = size.height * (.55f + rnd(i, 2) * .3f)
        drawLine(p.gold.copy(alpha = .14f * (1 - abs(ph - .5f) * 2)), Offset(x, y), Offset(x + 40f, y - 3f), 1.5f)
    }
}

/** «وردة هندسية»: a twelve-fold Islamic rosette, large and faint, turning very slowly. */
private fun DrawScope.rosette(p: Pal, t: Float) {
    val c = Offset(size.width / 2, size.height * .42f); val R = size.width * .62f
    rotate(t * 2f, c) {
        for (ring in 0 until 3) {
            val r = R * (1f - ring * .28f); val n = 12
            val star = Path()
            for (k in 0..n * 2) {
                val a = k * PI.toFloat() / n
                val rr = if (k % 2 == 0) r else r * .62f
                val pt = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr)
                if (k == 0) star.moveTo(pt.x, pt.y) else star.lineTo(pt.x, pt.y)
            }
            star.close()
            drawPath(star, (if (ring == 1) p.gold else p.accent).copy(alpha = .08f), style = Stroke(1.8f))
        }
        for (k in 0 until 12) { val a = k * PI.toFloat() / 6; drawLine(p.accent.copy(alpha = .05f), c, Offset(c.x + cos(a) * R, c.y + sin(a) * R), 1f) }
        drawCircle(p.gold.copy(alpha = .1f), R * .1f, c, style = Stroke(2f))
    }
}

/** «مآذن»: domes and minarets on the horizon; at night their windows light up one by one. */
private fun DrawScope.skyline(p: Pal, t: Float) {
    val base = size.height * .9f; val c = p.accent.copy(alpha = .13f); val w = size.width
    val path = Path().apply {
        moveTo(0f, size.height); lineTo(0f, base)
        fun dome(cx: Float, r: Float) { lineTo(cx - r, base); cubicTo(cx - r, base - r * 1.25f, cx + r, base - r * 1.25f, cx + r, base); lineTo(cx + r, base) }
        fun minaret(cx: Float, h: Float) {
            val mw = w * .018f
            lineTo(cx - mw, base); lineTo(cx - mw, base - h); lineTo(cx - mw * 1.6f, base - h); lineTo(cx - mw * 1.6f, base - h - 6f)
            lineTo(cx, base - h - mw * 4f); lineTo(cx + mw * 1.6f, base - h - 6f); lineTo(cx + mw * 1.6f, base - h); lineTo(cx + mw, base - h); lineTo(cx + mw, base)
        }
        minaret(w * .08f, size.height * .2f); dome(w * .25f, w * .09f); minaret(w * .4f, size.height * .26f)
        dome(w * .55f, w * .13f); dome(w * .72f, w * .07f); minaret(w * .85f, size.height * .18f); dome(w * .95f, w * .05f)
        lineTo(w, base); lineTo(w, size.height); close()
    }
    drawRect(c, Offset(0f, base), Size(w, size.height - base))
    drawPath(path, c)
    if (p.dark) for (i in 0 until 9) {        // windows
        val on = .5f + .5f * sin(t * .4f + i * 1.9f)
        drawRect(p.gold.copy(alpha = .1f + .25f * on), Offset(w * (.12f + i * .095f), base + 8f + (i % 2) * 10f), Size(5f, 8f))
    }
    drawCircle(p.gold.copy(alpha = .25f), 16f, Offset(w * .8f, size.height * .58f))      // the crescent above
    drawCircle(p.bg.copy(alpha = .9f), 14f, Offset(w * .8f + 7f, size.height * .58f - 4f))
}

/** «فانوس رمضان»: Ramadan's string of little lanterns and stars swaying across the top, and a crescent. */
private fun DrawScope.bunting(p: Pal, t: Float) {
    for (row in 0..1) {
        val y0 = size.height * (.03f + row * .09f); val sag = size.height * .05f
        val cord = Path().apply { moveTo(0f, y0); quadraticTo(size.width / 2, y0 + sag * 2, size.width, y0) }
        drawPath(cord, p.gold.copy(alpha = .3f), style = Stroke(1.5f))
        val n = 9
        for (i in 1 until n) {
            val f = i / n.toFloat(); val x = size.width * f
            val y = y0 + 4 * sag * f * (1 - f)
            val swing = sin(t * 1.2f + i + row) * 4f
            val hang = 10f + (i % 3) * 6f
            drawLine(p.gold.copy(alpha = .3f), Offset(x, y), Offset(x + swing, y + hang), 1f)
            val c = Offset(x + swing, y + hang)
            if ((i + row) % 2 == 0) {
                val col = listOf(Color(0xFFE85A4F), Color(0xFFF2C14E), Color(0xFF3FB8B0))[(i + row) % 3]
                drawCircle(Brush.radialGradient(listOf(col.copy(alpha = .35f), Color.Transparent), c, 30f), 30f, Offset(c.x, c.y + 12f))
                drawRoundRect(col.copy(alpha = .55f), Offset(c.x - 7f, c.y), Size(14f, 22f), androidx.compose.ui.geometry.CornerRadius(6f))
                drawRect(p.gold.copy(alpha = .6f), Offset(c.x - 4f, c.y - 3f), Size(8f, 4f))
            } else {
                val st = Path(); for (k in 0..10) { val a = -PI.toFloat() / 2 + k * PI.toFloat() / 5; val rr = if (k % 2 == 0) 8f else 3.5f
                    if (k == 0) st.moveTo(c.x + cos(a) * rr, c.y + 8f + sin(a) * rr) else st.lineTo(c.x + cos(a) * rr, c.y + 8f + sin(a) * rr) }
                drawPath(st, p.gold.copy(alpha = .45f + .2f * sin(t * 2 + i)))
            }
        }
    }
    val mc = Offset(size.width * .15f, size.height * .26f)
    drawCircle(p.gold.copy(alpha = .3f), 26f, mc); drawCircle(p.bg, 23f, Offset(mc.x + 11f, mc.y - 6f))
}

/** «أريزونا»: red mesas and saguaros under a low desert sun. */
private fun DrawScope.mesa(p: Pal, t: Float) {
    val sun = Offset(size.width * .7f, size.height * .74f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFF9A5A).copy(alpha = .35f), Color.Transparent), sun, size.width * .5f), size.width * .5f, sun)
    drawCircle(Color(0xFFF2B05A).copy(alpha = .35f), 34f, sun)
    val far = Color(0xFFC0582E).copy(alpha = .16f); val near = Color(0xFF8E3A1E).copy(alpha = .2f)
    val m = Path().apply {
        val b = size.height * .8f; moveTo(0f, size.height); lineTo(0f, b)
        lineTo(size.width * .08f, b); lineTo(size.width * .12f, b - 70f); lineTo(size.width * .34f, b - 70f); lineTo(size.width * .38f, b)
        lineTo(size.width * .55f, b); lineTo(size.width * .58f, b - 40f); lineTo(size.width * .66f, b - 40f); lineTo(size.width * .69f, b)
        lineTo(size.width * .8f, b); lineTo(size.width * .84f, b - 95f); lineTo(size.width * .97f, b - 95f); lineTo(size.width, b - 60f); lineTo(size.width, size.height); close()
    }
    drawPath(m, far)
    drawRect(near, Offset(0f, size.height * .86f), Size(size.width, size.height * .14f))
    listOf(.2f to 1f, .47f to .7f, .9f to .85f).forEach { (fx, s) ->           // saguaros
        val x = size.width * fx; val base = size.height * .88f; val h = 90f * s; val w = 12f * s; val c = Color(0xFF3E6B3A).copy(alpha = .3f)
        drawRoundRect(c, Offset(x - w / 2, base - h), Size(w, h), androidx.compose.ui.geometry.CornerRadius(w / 2))
        drawRoundRect(c, Offset(x - w * 2.2f, base - h * .7f), Size(w * .8f, h * .35f), androidx.compose.ui.geometry.CornerRadius(w / 2))
        drawLine(c, Offset(x - w * 1.8f, base - h * .4f), Offset(x, base - h * .4f), w * .7f)
        drawRoundRect(c, Offset(x + w * 1.3f, base - h * .8f), Size(w * .8f, h * .4f), androidx.compose.ui.geometry.CornerRadius(w / 2))
        drawLine(c, Offset(x, base - h * .5f), Offset(x + w * 1.7f, base - h * .5f), w * .7f)
    }
    for (i in 0 until 3) { val x = ((t * 8f + i * 140f) % (size.width + 60f)) - 30f; drawLine(p.ink.copy(alpha = .12f), Offset(x, size.height * (.2f + i * .05f)), Offset(x + 10f, size.height * (.2f + i * .05f) - 5f), 1.5f) }
}

/** «المخمل الملكي»: a faint damask pattern repeating like woven silk. */
private fun DrawScope.damask(p: Pal) {
    val s = size.width / 3.2f; val c = p.gold.copy(alpha = .06f)
    var y = 0f; var row = 0
    while (y < size.height + s) {
        var x = if (row % 2 == 0) 0f else s / 2
        while (x < size.width + s) {
            val motif = Path().apply {
                moveTo(x, y - s * .42f)
                cubicTo(x + s * .32f, y - s * .25f, x + s * .2f, y + s * .05f, x, y + s * .42f)
                cubicTo(x - s * .2f, y + s * .05f, x - s * .32f, y - s * .25f, x, y - s * .42f); close()
            }
            drawPath(motif, c, style = Stroke(2f))
            drawCircle(c, s * .07f, Offset(x, y))
            drawLine(c, Offset(x - s * .22f, y), Offset(x + s * .22f, y), 1.5f)
            x += s
        }
        y += s * .9f; row++
    }
}

/** «زجاج»: large soft lights of color drifting behind frosted glass. */
private fun DrawScope.orbs(p: Pal, t: Float) {
    val cols = listOf(Color(0xFF7A5CFF), Color(0xFF00C2B8), Color(0xFFFF6FA8), Color(0xFFFFB347))
    cols.forEachIndexed { i, c ->
        val x = size.width * (.5f + .4f * sin(t * (.05f + i * .013f) + i * 1.7f)); val y = size.height * (.45f + .38f * cos(t * (.04f + i * .011f) + i))
        val r = size.width * (.45f + .1f * i % 2)
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = if (p.dark) .28f else .22f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
    }
}

/** «حبر وقلم»: drops of ink falling and spreading on paper, and a reed pen's stroke. */
private fun DrawScope.ink(p: Pal, t: Float) {
    for (i in 0 until 6) {
        val ph = (t * .08f + i / 6f) % 1f
        val c = Offset(size.width * (.12f + (i * .37f) % .8f), size.height * (.15f + (i * .29f) % .75f))
        drawCircle(p.ink.copy(alpha = .07f * (1 - ph)), 6f + ph * 70f, c)
        drawCircle(p.ink.copy(alpha = .10f * (1 - ph)), 6f + ph * 70f, c, style = Stroke(1.2f))
    }
    val stroke = Path().apply { moveTo(size.width * .1f, size.height * .93f); cubicTo(size.width * .35f, size.height * .86f, size.width * .6f, size.height * .98f, size.width * .9f, size.height * .9f) }
    drawPath(stroke, p.accent.copy(alpha = .12f), style = Stroke(9f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
}

/** «صبّار»: little cacti in pots along the bottom, a warm sun, and a flower opening now and then. */
private fun DrawScope.cacti(p: Pal, t: Float) {
    val sun = Offset(size.width * .82f, size.height * .1f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC85A).copy(alpha = .3f), Color.Transparent), sun, 110f), 110f, sun)
    val green = Color(0xFF4E9A5A).copy(alpha = .22f); val pot = Color(0xFFB0603A).copy(alpha = .22f)
    listOf(.08f, .3f, .62f, .88f).forEachIndexed { i, fx ->
        val x = size.width * fx; val base = size.height * .97f; val h = 50f + (i % 2) * 26f; val w = 18f
        drawRoundRect(pot, Offset(x - 16f, base - 18f), Size(32f, 18f), androidx.compose.ui.geometry.CornerRadius(3f))
        drawRoundRect(green, Offset(x - w / 2, base - 18f - h), Size(w, h), androidx.compose.ui.geometry.CornerRadius(w / 2))
        if (i % 2 == 0) drawRoundRect(green, Offset(x + w / 2, base - 18f - h * .7f), Size(10f, h * .35f), androidx.compose.ui.geometry.CornerRadius(5f))
        val open = (.5f + .5f * sin(t * .5f + i * 1.6f)).coerceIn(0f, 1f)
        for (k in 0 until 5) { val a = k * 2 * PI.toFloat() / 5; drawCircle(Color(0xFFF06A9A).copy(alpha = .3f * open), 4f * open + 1f, Offset(x + cos(a) * 5f * open, base - 20f - h + sin(a) * 5f * open)) }
    }
}

/**
 * «سماء الوقت»: the hour of the day by the real prayer times of the place (fajr, sunrise, dhuhr, asr, maghrib, isha),
 * so the theme's colors and sky follow the day. [tick] moves each minute while the theme is shown.
 */
object Sky {
    enum class Phase { NIGHT, DAWN, MORNING, NOON, AFTERNOON, SUNSET, DUSK }
    var tick by mutableIntStateOf(0)
    private fun now() = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    private fun times(): Map<Prayer, Int> = runCatching { Store.times() }.getOrNull()
        ?: mapOf(Prayer.FAJR to 300, Prayer.SUNRISE to 380, Prayer.DHUHR to 730, Prayer.ASR to 930, Prayer.MAGHRIB to 1080, Prayer.ISHA to 1160)
    fun phase(): Phase {
        @Suppress("UNUSED_VARIABLE") val v = tick
        val m = now(); val x = times()
        val fajr = x.getValue(Prayer.FAJR); val rise = x.getValue(Prayer.SUNRISE); val dhuhr = x.getValue(Prayer.DHUHR)
        val asr = x.getValue(Prayer.ASR); val mag = x.getValue(Prayer.MAGHRIB); val isha = x.getValue(Prayer.ISHA)
        return when {
            m < fajr || m >= isha -> Phase.NIGHT
            m < rise + 20 -> Phase.DAWN
            m < dhuhr - 30 -> Phase.MORNING
            m < asr -> Phase.NOON
            m < mag - 35 -> Phase.AFTERNOON
            m < mag + 10 -> Phase.SUNSET
            else -> Phase.DUSK
        }
    }
    /** 0 at sunrise … 1 at maghrib (the sun's way across). */
    fun dayFraction(): Float { val x = times(); val r = x.getValue(Prayer.SUNRISE); val s = x.getValue(Prayer.MAGHRIB); return ((now() - r).toFloat() / (s - r).coerceAtLeast(1)).coerceIn(0f, 1f) }

    private fun pal(dark: Boolean, bg: Long, surface: Long, card: Long, line: Long, ink: Long, muted: Long, accent: Long, gold: Long) =
        Pal(dark, Color(bg), Color(surface), Color(card), Color(line), Color(ink), Color(muted), Color(accent), Tints.on(Color(accent)), Color(gold), Color(0xFFD9534F))
    /** The palette of this hour. */
    fun pal(): Pal = when (phase()) {
        Phase.NIGHT -> pal(true, 0xFF0A0F24, 0xFF111832, 0xFF18213F, 0xFF27315A, 0xFFE8ECFF, 0xFF97A0C8, 0xFF8FA8FF, 0xFFF2D27A)
        Phase.DAWN -> pal(true, 0xFF1C1830, 0xFF241F3D, 0xFF2E284C, 0xFF443A68, 0xFFF4EAF6, 0xFFB9A9C9, 0xFFF39BB0, 0xFFF4B860)
        Phase.MORNING -> pal(false, 0xFFF1F8FD, 0xFFFFFFFF, 0xFFE1F0FA, 0xFFC8E1F2, 0xFF12324A, 0xFF587A90, 0xFF1E7FC0, 0xFFD89A2A)
        Phase.NOON -> pal(false, 0xFFF6FAFD, 0xFFFFFFFF, 0xFFE6F2FA, 0xFFCFE3F1, 0xFF0E2C44, 0xFF56758C, 0xFF0B6FB0, 0xFFC98A1E)
        Phase.AFTERNOON -> pal(false, 0xFFFBF4E8, 0xFFFFFFFF, 0xFFF5E6CC, 0xFFE9D3AE, 0xFF3D2A12, 0xFF85694A, 0xFFB86A16, 0xFF2F8F7A)
        Phase.SUNSET -> pal(false, 0xFFFDEDE4, 0xFFFFFFFF, 0xFFF9DACB, 0xFFF0C3AE, 0xFF43201A, 0xFF8E5F52, 0xFFD0502C, 0xFFB57A1E)
        Phase.DUSK -> pal(true, 0xFF19132A, 0xFF211A37, 0xFF2B2246, 0xFF3F3363, 0xFFF0E8FA, 0xFFADA0C8, 0xFFC59BFF, 0xFFF0B860)
    }
}
