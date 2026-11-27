package com.alzikr.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drawn decorations for the friend's card and its widget: frames that hang from the edge (Ramadan lanterns,
 * Eid lanterns, crescents and stars) and festive scenes (the Ramadan crescent, the Eid crescent, round orange lanterns).
 */
object Decor {
    /** Frames drawn here (not XML shapes): they hang from the top edge. */
    val drawn = setOf("lanterns", "eid", "crescents", "hearts", "butterflies", "sakura", "rainbow", "snowflakes", "bunting")
    fun isDrawn(id: String) = id in drawn
    /** Frames that also carry their own ornaments (drawFrameExtras): planets round the galaxy, roses at the rose's corners… */
    val extras = setOf("night", "gold", "pattern", "dawn", "rose", "ocean", "desert", "lantern", "zellige", "aurora", "pearl", "royal", "crystal", "galaxy")
    fun hasExtras(id: String) = id in extras
    /** Festive scenes (card ids; the widget's are the same with "w_"). */
    val scenes = setOf("ramadan_moon", "eid_moon", "orange_lanterns")
}

private fun DrawScope.dp(v: Float) = v * density

/** A crescent: a disc minus a shifted disc (no need to know the colour behind it). */
fun DrawScope.crescent(cx: Float, cy: Float, r: Float, color: Color, tilt: Float = .42f) {
    val a = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r)) }
    val b = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - r + r * tilt, cy - r - r * .12f, cx + r + r * tilt, cy + r - r * .12f)) }
    drawPath(Path.combine(PathOperation.Difference, a, b), color)
}

/** A five-pointed star. */
fun DrawScope.star(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path()
    for (k in 0 until 10) {
        val rr = if (k % 2 == 0) r else r * .45f
        val a = (-PI / 2 + k * PI / 5).toFloat()
        val x = cx + cos(a) * rr; val y = cy + sin(a) * rr
        if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); drawPath(p, color)
}

/** A Ramadan fanous hanging on a string from [top] (string length [len]); [s] its scale; glowing glass. */
fun DrawScope.fanous(x: Float, top: Float, len: Float, s: Float, metal: Color, glass: Color, glow: Float = 1f) {
    val u = dp(s)
    val y = top + len
    drawLine(metal.copy(alpha = .8f), Offset(x, top), Offset(x, y), 1.2f * density)
    drawCircle(metal, 2.2f * u, Offset(x, y + 1.5f * u), style = Stroke(1.2f * density))            // ring
    // the cap: a small dome and a band
    drawPath(Path().apply { moveTo(x - 6 * u, y + 7 * u); quadraticBezierTo(x, y + 1 * u, x + 6 * u, y + 7 * u); close() }, metal)
    drawRect(metal, Offset(x - 7 * u, y + 7 * u), Size(14 * u, 2.4f * u))
    // the glow, then the body: a hexagonal glass between two bands
    drawCircle(Brush.radialGradient(listOf(glass.copy(alpha = .55f * glow), Color.Transparent), Offset(x, y + 18 * u), 22 * u), 22 * u, Offset(x, y + 18 * u))
    val body = Path().apply {
        moveTo(x - 6 * u, y + 9.4f * u); lineTo(x + 6 * u, y + 9.4f * u); lineTo(x + 8.5f * u, y + 16 * u)
        lineTo(x + 6 * u, y + 26 * u); lineTo(x - 6 * u, y + 26 * u); lineTo(x - 8.5f * u, y + 16 * u); close()
    }
    drawPath(body, Brush.verticalGradient(listOf(glass, glass.copy(alpha = .75f)), y + 9 * u, y + 26 * u))
    drawLine(metal, Offset(x, y + 9.4f * u), Offset(x, y + 26 * u), 1.1f * density)
    drawLine(metal, Offset(x - 8.5f * u, y + 16 * u), Offset(x + 8.5f * u, y + 16 * u), 1f * density)
    drawPath(body, metal, style = Stroke(1.3f * density))
    drawRect(metal, Offset(x - 6.5f * u, y + 26 * u), Size(13 * u, 2.4f * u))
    drawPath(Path().apply { moveTo(x - 4 * u, y + 28.4f * u); lineTo(x + 4 * u, y + 28.4f * u); lineTo(x, y + 33 * u); close() }, metal)
}

/** A round, ribbed orange lantern (like a little pumpkin, but a lantern of Ramadan nights). */
fun DrawScope.roundLantern(x: Float, y: Float, s: Float, color: Color, glow: Float = 1f) {
    val u = dp(s)
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .45f * glow), Color.Transparent), Offset(x, y), 30 * u), 30 * u, Offset(x, y))
    val w = 15 * u; val h = 12 * u
    drawOval(Brush.radialGradient(listOf(Color(0xFFFFE2A8), color, color.copy(red = color.red * .75f, green = color.green * .7f)), Offset(x - w * .2f, y - h * .3f), w * 1.3f),
        Offset(x - w, y - h), Size(w * 2, h * 2))
    // the ribs
    for (k in listOf(-.55f, 0f, .55f)) {
        val p = Path().apply { moveTo(x + w * k * .3f, y - h); quadraticBezierTo(x + w * k * 1.25f, y, x + w * k * .3f, y + h) }
        drawPath(p, Color(0x66803800), style = Stroke(1.2f * density))
    }
    drawRect(Color(0xFF6B3A12), Offset(x - 4 * u, y - h - 3 * u), Size(8 * u, 3.5f * u))      // the cap
    drawRect(Color(0xFF6B3A12), Offset(x - 3 * u, y + h - .5f * u), Size(6 * u, 2.5f * u))    // the base
}

/** The frame's band in its two colours at the very edge (as for the XML frames), then what hangs from it. */
fun DrawScope.drawDecorFrame(id: String, t: Float, top: Color, bottom: Color, radius: Float) {
    val w = size.width; val h = size.height
    val band = 3f * density
    drawRoundRect(Brush.verticalGradient(listOf(top, bottom)), Offset(band / 2, band / 2), Size(w - band, h - band), CornerRadius(radius), style = Stroke(band))
    val sway = { k: Int -> sin((t * 2 * PI + k * 1.3).toFloat()) * 2f * density }
    when (id) {
        "lanterns" -> {
            val gold = Color(0xFFE9C46A)
            drawLine(gold.copy(alpha = .7f), Offset(radius, band), Offset(w - radius, band), 1.4f * density)
            val n = 5
            for (k in 0 until n) {
                val fx = .12f + k * (.76f / (n - 1)); val len = dp(listOf(6f, 18f, 11f, 20f, 8f)[k])
                fanous(w * fx + sway(k), band, len, listOf(.95f, 1.15f, 1f, 1.2f, .95f)[k], gold, listOf(Color(0xFFFFB347), Color(0xFFFFD27A), Color(0xFFFF8A5C), Color(0xFFFFD27A), Color(0xFFFFB347))[k])
            }
        }
        "eid" -> {
            // a garland of little flags, then colourful lanterns and stars
            val cols = listOf(Color(0xFFFF6B9A), Color(0xFF4FD1C5), Color(0xFFFFD166), Color(0xFF9B7BFF), Color(0xFF6BCB77))
            val g = Path(); g.moveTo(radius, band); g.quadraticBezierTo(w / 2, dp(16f), w - radius, band)
            drawPath(g, Color(0xCCFFFFFF), style = Stroke(1.2f * density))
            val flags = 14
            for (k in 1 until flags) {
                val f = k / flags.toFloat(); val x = radius + (w - 2 * radius) * f
                val y = band + (1 - (2 * f - 1) * (2 * f - 1)) * (dp(16f) - band) * .5f * 2 * .5f
                drawPath(Path().apply { moveTo(x - dp(4f), y); lineTo(x + dp(4f), y); lineTo(x, y + dp(8f)); close() }, cols[k % cols.size])
            }
            listOf(.08f, .3f, .7f, .92f).forEachIndexed { k, fx -> fanous(w * fx + sway(k), band, dp(listOf(10f, 20f, 18f, 8f)[k]), .9f, Color(0xFFF2F2F2), cols[(k * 2) % cols.size]) }
            listOf(.19f, .5f, .81f).forEachIndexed { k, fx -> star(w * fx, dp(32f) + sway(k + 3), dp(6f), cols[(k + 2) % cols.size]) }
        }
        "hearts" -> {
            // A string of hearts hanging at different lengths, beating softly.
            val cols = listOf(Color(0xFFFF6B9A), Color(0xFFFFB3C8), Color(0xFFE2588C), Color(0xFFFF8FB1), Color(0xFFFFC2D6))
            listOf(.1f, .27f, .44f, .61f, .78f, .92f).forEachIndexed { k, fx ->
                val x = w * fx + sway(k); val len = dp(listOf(8f, 18f, 12f, 20f, 10f, 15f)[k])
                drawLine(Color(0xAAFFFFFF), Offset(x, band), Offset(x, band + len), 1f * density)
                val beat = 1f + .08f * sin((t * 4 * PI + k).toFloat())
                heart(Offset(x, band + len + dp(6f)), dp(6.5f) * beat, cols[k % cols.size])
            }
        }
        "butterflies" -> {
            // Butterflies flying slowly round the frame, wings beating.
            val cols = listOf(Color(0xFFFFB347), Color(0xFF7FD8FF), Color(0xFFE59BFF), Color(0xFFFF7A9A))
            cols.forEachIndexed { k, c ->
                val f = (t * .2f + k * .25f) % 1f
                val pw = w - 2 * radius; val x = radius + pw * f; val y = if (k % 2 == 0) dp(10f) + sin((t * 2 * PI * 2 + k).toFloat()) * dp(4f) else h - dp(10f) + sin((t * 2 * PI * 2 + k).toFloat()) * dp(4f)
                val flap = .35f + .65f * kotlin.math.abs(sin((t * 2 * PI * 6 + k).toFloat()))
                drawOval(c, Offset(x - dp(7f) * flap, y - dp(5f)), Size(dp(7f) * flap, dp(6f)))
                drawOval(c, Offset(x, y - dp(5f)), Size(dp(7f) * flap, dp(6f)))
                drawOval(c.copy(alpha = .8f), Offset(x - dp(5f) * flap, y), Size(dp(5f) * flap, dp(4.5f)))
                drawOval(c.copy(alpha = .8f), Offset(x, y), Size(dp(5f) * flap, dp(4.5f)))
                drawLine(Color(0xFF3A2A20), Offset(x, y - dp(4f)), Offset(x, y + dp(4f)), 1.4f * density)
            }
        }
        "sakura" -> {
            // A blossoming branch along the top, and petals drifting down the sides.
            val br = Path(); br.moveTo(radius * .6f, dp(6f)); br.quadraticBezierTo(w * .35f, dp(16f), w * .6f, dp(7f)); br.quadraticBezierTo(w * .8f, dp(2f), w - radius * .6f, dp(9f))
            drawPath(br, Color(0xFF6B4A3A), style = Stroke(2f * density))
            listOf(.12f, .25f, .4f, .55f, .7f, .86f).forEachIndexed { k, fx ->
                val x = w * fx; val y = dp(listOf(8f, 12f, 13f, 9f, 5f, 8f)[k])
                for (q in 0 until 5) { val a = (q * 72f + k * 20f) * PI.toFloat() / 180f; drawCircle(Color(0xFFFFC8DA), dp(2.6f), Offset(x + cos(a) * dp(3f), y + sin(a) * dp(3f))) }
                drawCircle(Color(0xFFE2588C), dp(1.4f), Offset(x, y))
            }
            repeat(6) { k ->
                val ph = (t + k / 6f) % 1f; val x = if (k % 2 == 0) dp(8f) + sin(ph * 9f) * dp(3f) else w - dp(8f) + sin(ph * 9f) * dp(3f)
                drawOval(Color(0xFFFFB3C8).copy(alpha = 1f - ph * .7f), Offset(x, dp(22f) + ph * (h - dp(30f))), Size(dp(5f), dp(3f)))
            }
        }
        "rainbow" -> {
            // A small rainbow between two clouds at the top.
            val cols = listOf(Color(0xFFFF6B6B), Color(0xFFFFB347), Color(0xFFFFE066), Color(0xFF6BCB77), Color(0xFF4FC3F7), Color(0xFF9B7BFF))
            val cx = w / 2; val r0 = dp(22f)
            cols.forEachIndexed { k, c -> drawArc(c, 180f, 180f, false, Offset(cx - (r0 - k * dp(2.6f)), dp(26f) - (r0 - k * dp(2.6f))), Size(2 * (r0 - k * dp(2.6f)), 2 * (r0 - k * dp(2.6f))), style = Stroke(dp(2.4f))) }
            for (sx in listOf(cx - r0, cx + r0)) {
                val y = dp(25f); val drift = sin((t * 2 * PI).toFloat()) * dp(1.5f)
                drawCircle(Color.White, dp(6f), Offset(sx - dp(5f) + drift, y)); drawCircle(Color.White, dp(7.5f), Offset(sx + drift, y - dp(3f))); drawCircle(Color.White, dp(6f), Offset(sx + dp(6f) + drift, y))
            }
        }
        "snowflakes" -> {
            // Snowflakes hanging and turning, and a few falling.
            fun flake(c: Offset, r: Float, a: Float) {
                for (q in 0 until 6) { val ang = (a + q * 60f) * PI.toFloat() / 180f
                    val e = Offset(c.x + cos(ang) * r, c.y + sin(ang) * r); drawLine(Color.White, c, e, 1.3f * density)
                    val m2 = Offset(c.x + cos(ang) * r * .6f, c.y + sin(ang) * r * .6f)
                    for (s2 in listOf(-35f, 35f)) { val b2 = (a + q * 60f + s2) * PI.toFloat() / 180f; drawLine(Color.White, m2, Offset(m2.x + cos(b2) * r * .35f, m2.y + sin(b2) * r * .35f), 1f * density) } }
            }
            listOf(.15f, .38f, .62f, .85f).forEachIndexed { k, fx ->
                val x = w * fx + sway(k); val len = dp(listOf(10f, 18f, 13f, 8f)[k])
                drawLine(Color(0xAAFFFFFF), Offset(x, band), Offset(x, band + len), 1f * density)
                flake(Offset(x, band + len + dp(7f)), dp(6.5f), t * 120f + k * 20f)
            }
            repeat(8) { k -> val ph = (t + k / 8f) % 1f; drawCircle(Color.White.copy(alpha = .8f - ph * .6f), dp(1.6f), Offset(w * ((k * .13f + .05f) % 1f), dp(30f) + ph * (h - dp(36f)))) }
        }
        "bunting" -> {
            // Two strings of little pennants across the top, gently swinging.
            val cols = listOf(Color(0xFFFF6B9A), Color(0xFFFFD166), Color(0xFF4FD1C5), Color(0xFF9B7BFF), Color(0xFF6BCB77))
            for (row in 0 until 2) {
                val a = if (row == 0) radius * .5f else w * .5f; val b = if (row == 0) w * .5f else w - radius * .5f
                val dip = dp(14f) + sway(row)
                val str = Path(); str.moveTo(a, band); str.quadraticBezierTo((a + b) / 2, band + dip * 2, b, band)
                drawPath(str, Color(0xCCFFFFFF), style = Stroke(1.1f * density))
                for (k in 1 until 7) {
                    val f = k / 7f; val x = a + (b - a) * f; val y = band + dip * 4 * f * (1 - f)
                    drawPath(Path().apply { moveTo(x - dp(4.5f), y); lineTo(x + dp(4.5f), y); lineTo(x, y + dp(9f)); close() }, cols[(k + row * 2) % cols.size])
                }
            }
        }
        "crescents" -> {
            val gold = Color(0xFFF3D27A)
            listOf(.15f, .38f, .62f, .85f).forEachIndexed { k, fx ->
                val x = w * fx + sway(k); val len = dp(listOf(12f, 22f, 16f, 10f)[k])
                drawLine(gold.copy(alpha = .7f), Offset(x, band), Offset(x, band + len), 1.2f * density)
                if (k % 2 == 0) crescent(x, band + len + dp(10f), dp(10.5f), gold) else star(x, band + len + dp(8f), dp(8f), gold)
            }
        }
    }
}

/** The festive scenes (the card animates with [t]; the widget passes 0). */
fun DrawScope.drawFestiveScene(id: String, t: Float) {
    val w = size.width; val h = size.height
    fun stars(n: Int, seed: Int) { val r = java.util.Random(seed.toLong())
        repeat(n) { val tw = .45f + .55f * ((sin((t * 2 * PI + r.nextFloat() * 6.28).toFloat()) + 1) / 2)
            drawCircle(Color.White.copy(alpha = tw), (r.nextFloat() * 1.6f + .6f) * density, Offset(r.nextFloat() * w, r.nextFloat() * h * .8f)) } }
    when (id) {
        "ramadan_moon" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0B0A2A), Color(0xFF2A1A55), Color(0xFF4A2A6A))))
            stars(40, 81)
            val c = Offset(w * .72f, h * .34f); val r = h * .2f
            drawCircle(Brush.radialGradient(listOf(Color(0x55FFE7A0), Color.Transparent), c, r * 2.6f), r * 2.6f, c)
            crescent(c.x, c.y, r, Color(0xFFFFE08A))
            star(c.x - r * .15f, c.y - r * .05f, r * .22f, Color(0xFFFFE08A))
            // a mosque's outline below
            val base = h * .86f; val mc = Color(0xFF140F30)
            drawRect(mc, Offset(0f, base), Size(w, h - base))
            drawCircle(mc, h * .13f, Offset(w * .3f, base))
            drawRect(mc, Offset(w * .3f - h * .13f, base - h * .02f), Size(h * .26f, h * .1f))
            drawRect(mc, Offset(w * .12f, base - h * .32f), Size(w * .025f, h * .32f)); drawCircle(mc, w * .02f, Offset(w * .1325f, base - h * .32f))
            drawRect(mc, Offset(w * .48f, base - h * .26f), Size(w * .022f, h * .26f)); drawCircle(mc, w * .018f, Offset(w * .491f, base - h * .26f))
            fanous(w * .9f + sin((t * 2 * PI).toFloat()) * 2 * density, 0f, h * .12f, .9f, Color(0xFFE9C46A), Color(0xFFFFC35C))
        }
        "eid_moon" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF06283D), Color(0xFF0E4D64), Color(0xFF137C84))))
            stars(30, 83)
            val c = Offset(w * .25f, h * .32f); val r = h * .18f
            drawCircle(Brush.radialGradient(listOf(Color(0x55FFFFFF), Color.Transparent), c, r * 2.4f), r * 2.4f, c)
            crescent(c.x, c.y, r, Color(0xFFFFF4D6))
            val cols = listOf(Color(0xFFFF6B9A), Color(0xFFFFD166), Color(0xFF4FD1C5), Color(0xFF9B7BFF), Color(0xFF6BCB77))
            val rnd = java.util.Random(7)
            repeat(26) { k ->
                val x = rnd.nextFloat() * w; val y = ((rnd.nextFloat() + t * .4f) % 1f) * h
                drawRect(cols[k % cols.size], Offset(x, y), Size(3.5f * density, 6f * density))
            }
            listOf(.55f, .75f, .92f).forEachIndexed { k, fx -> fanous(w * fx + sin((t * 2 * PI + k).toFloat()) * 2 * density, 0f, h * (.08f + k * .05f), .8f, Color(0xFFF2F2F2), cols[k]) }
        }
        "orange_lanterns" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1238), Color(0xFF6B2C3A), Color(0xFFB4583A))))
            stars(18, 85)
            drawCircle(Brush.radialGradient(listOf(Color(0x44FFE0A0), Color.Transparent), Offset(w * .8f, h * .25f), h * .3f), h * .3f, Offset(w * .8f, h * .25f)); crescent(w * .8f, h * .25f, h * .11f, Color(0xFFFFE0A0))
            val wire = Path(); wire.moveTo(0f, h * .12f); wire.quadraticBezierTo(w * .5f, h * .38f, w, h * .1f)
            drawPath(wire, Color(0x99FFD27A), style = Stroke(1.2f * density))
            listOf(.12f to .17f, .3f to .26f, .5f to .3f, .7f to .25f, .88f to .15f).forEachIndexed { k, (fx, fy) ->
                val x = w * fx; val y = h * fy + sin((t * 2 * PI + k).toFloat()) * 2 * density
                drawLine(Color(0x99FFD27A), Offset(x, y - dp(8f)), Offset(x, y + dp(4f)), 1f * density)
                roundLantern(x, y + dp(16f), listOf(.8f, 1f, 1.1f, 1f, .85f)[k], listOf(Color(0xFFFF8C1A), Color(0xFFFFA62B), Color(0xFFFF7A00), Color(0xFFFFA62B), Color(0xFFFF8C1A))[k],
                    .8f + .2f * sin((t * 4 * PI + k).toFloat()))
            }
            drawPath(Path().apply { moveTo(0f, h * .88f); quadraticBezierTo(w * .5f, h * .78f, w, h * .9f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF2A1020))
        }
    }
}


/** An eight-pointed star (two squares), the girih's star. */
fun DrawScope.star8(cx: Float, cy: Float, r: Float, color: Color, stroke: Float = 0f) {
    for (a in listOf(0f, 45f)) {
        val p = Path()
        for (k in 0 until 4) {
            val ang = ((a + k * 90f + 45f) * PI / 180).toFloat()
            val x = cx + cos(ang) * r; val y = cy + sin(ang) * r
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        if (stroke > 0f) drawPath(p, color, style = Stroke(stroke)) else drawPath(p, color)
    }
}

/** A cut gem: a diamond with a lighter top facet. */
private fun DrawScope.gem(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path().apply { moveTo(cx, cy - r); lineTo(cx + r * .8f, cy); lineTo(cx, cy + r); lineTo(cx - r * .8f, cy); close() }
    drawPath(p, color)
    drawPath(Path().apply { moveTo(cx, cy - r); lineTo(cx + r * .8f, cy); lineTo(cx - r * .8f, cy); close() }, Color.White.copy(alpha = .35f))
    drawPath(p, Color.White.copy(alpha = .6f), style = Stroke(.8f * density))
}

/**
 * Each frame's own ornaments over its band (user, 2026-11-27: «اجعل الإطارات أكثر تميزًا، مثلًا للمجرّة كواكب»):
 * planets travelling round the galaxy, roses at the rose's corners, a sun rising on the dawn, pearls hanging… [t] 0..1 moves them.
 */
fun DrawScope.drawFrameExtras(id: String, t: Float, radius: Float) {
    if (!Decor.hasExtras(id)) return
    val w = size.width; val h = size.height; val u = density
    val tau = (2 * PI).toFloat()
    val inset = 3f * u
    fun along(f0: Float): Offset {          // a point on the band, clockwise from the top left (0..1)
        val pw = w - 2 * inset; val ph = h - 2 * inset
        val d = (((f0 % 1f) + 1f) % 1f) * 2 * (pw + ph)
        return when {
            d < pw -> Offset(inset + d, inset)
            d < pw + ph -> Offset(inset + pw, inset + d - pw)
            d < 2 * pw + ph -> Offset(inset + pw - (d - pw - ph), inset + ph)
            else -> Offset(inset, inset + ph - (d - 2 * pw - ph))
        }
    }
    val m = 12f * u
    val corners = listOf(Offset(m, m), Offset(w - m, m), Offset(w - m, h - m), Offset(m, h - m))
    fun twinkle(k: Int) = .45f + .55f * ((sin(t * tau + k * 1.7f) + 1) / 2)
    fun hang(x: Float, len: Float, color: Color) = drawLine(color, Offset(x, inset), Offset(x, inset + len), 1f * u)
    when (id) {
        "galaxy" -> {
            // Four planets travel round the frame; a ringed planet and a star hang from the top.
            listOf(0xFFFFB86B, 0xFF7FD8FF, 0xFFE59BFF, 0xFFFF7A7A).forEachIndexed { k, c ->
                val p = along(t * .25f + k * .25f); val r = (3f + (k % 2) * 1.4f) * u
                drawCircle(Brush.radialGradient(listOf(Color.White, Color(c)), Offset(p.x - r * .4f, p.y - r * .4f), r * 1.6f), r, p)
            }
            repeat(14) { k -> drawCircle(Color.White.copy(alpha = twinkle(k)), 1.2f * u, along(k / 14f + .03f)) }
            val x = w * .8f; val len = 14f * u; hang(x, len, Color(0x99C8B8FF))
            val c = Offset(x + sin(t * tau) * 2f * u, inset + len + 8f * u)
            drawOval(Color(0x88F3D9A0), Offset(c.x - 13f * u, c.y - 2.5f * u), Size(26f * u, 5f * u), style = Stroke(1.4f * u))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE9B8), Color(0xFFD98F3E)), Offset(c.x - 2f * u, c.y - 2f * u), 9f * u), 7f * u, c)
            drawArc(Color(0xDDF3D9A0), 200f, 140f, false, Offset(c.x - 13f * u, c.y - 2.5f * u), Size(26f * u, 5f * u), style = Stroke(1.4f * u))
            val x2 = w * .2f; hang(x2, 8f * u, Color(0x99C8B8FF)); star(x2, inset + 14f * u, 5f * u, Color(0xFFFFF1B8))
        }
        "night" -> {
            val gold = Color(0xFFF3D27A)
            hang(w * .18f, 10f * u, gold.copy(alpha = .6f)); crescent(w * .18f, inset + 20f * u, 8f * u, gold)
            listOf(.42f to 16f, .62f to 8f, .84f to 13f).forEachIndexed { k, (fx, l) ->
                hang(w * fx, l * u, gold.copy(alpha = .5f)); star(w * fx, inset + (l + 6f) * u, 4.5f * u, gold.copy(alpha = twinkle(k)))
            }
        }
        "gold" -> {
            val g = Color(0xFFC9962E)
            corners.forEach { c -> gem(c.x, c.y, 5f * u, Color(0xFFE9C46A)); listOf(-1f, 1f).forEach { s -> drawCircle(g, 1.6f * u, Offset(c.x + s * 8f * u, c.y)); drawCircle(g, 1.6f * u, Offset(c.x, c.y + s * 8f * u)) } }
            val sw = Path(); sw.moveTo(w * .3f, inset); sw.quadraticBezierTo(w * .5f, inset + 14f * u, w * .7f, inset)
            drawPath(sw, g, style = Stroke(1.2f * u)); repeat(9) { k -> val f = k / 8f; drawCircle(Color(0xFFF6E3A6), 1.8f * u, Offset(w * (.3f + .4f * f), inset + 14f * u * 4 * f * (1 - f) * .5f * 2)) }
        }
        "pattern", "zellige" -> {
            val c1 = if (id == "zellige") Color(0xFF9CC4FF) else Color(0xFFE9C46A); val c2 = if (id == "zellige") Color.White else Color(0xFF8FD3B0)
            corners.forEachIndexed { k, c -> rotate(t * 30f * (if (k % 2 == 0) 1 else -1), c) { star8(c.x, c.y, 8f * u, c1); drawCircle(c2, 2.4f * u, c) } }
            star8(w / 2, inset + 1f * u, 6f * u, c1.copy(alpha = .9f), 1.2f * u)
        }
        "dawn" -> {
            val c = Offset(w / 2, inset)
            drawCircle(Brush.radialGradient(listOf(Color(0xAAFFD08A), Color.Transparent), c, 34f * u), 34f * u, c)
            repeat(9) { k ->
                val a = (PI.toFloat() * (k + 1) / 10f); val r0 = 13f * u; val r1 = (19f + 4f * sin(t * tau + k)) * u
                drawLine(Color(0xCCFFD08A), Offset(c.x - cos(a) * r0, c.y + sin(a) * r0), Offset(c.x - cos(a) * r1, c.y + sin(a) * r1), 1.4f * u)
            }
            drawArc(Color(0xFFFFB067), 0f, 180f, true, Offset(c.x - 10f * u, c.y - 10f * u), Size(20f * u, 20f * u))
        }
        "rose" -> {
            corners.forEachIndexed { k, c -> rotate(k * 40f + sin(t * tau + k) * 6f, c) { rose(c, 7f * u, Color(0xFFF07FA6), Color(0xFFC2185B), Color(0xFF6FAF7F)) } }
            listOf(.35f, .5f, .65f).forEachIndexed { k, fx -> heart(Offset(w * fx, inset + 2f * u + sin(t * tau + k) * 1.5f * u), 3.4f * u, Color(0xFFE2588C).copy(alpha = .85f)) }
        }
        "ocean" -> {
            repeat(10) { k ->
                val ph = (t + k / 10f) % 1f; val side = if (k % 2 == 0) inset + 6f * u else w - inset - 6f * u
                val y = h - ph * h; val r = (2f + (k % 3)) * u
                drawCircle(Color.White.copy(alpha = .55f * (1 - ph)), r, Offset(side + sin(ph * 12f) * 2f * u, y), style = Stroke(1f * u))
            }
            val c = corners[3]; star(c.x + 2f * u, c.y - 2f * u, 7f * u, Color(0xFFFF9E80))
        }
        "desert" -> {
            val c = corners[3]
            drawLine(Color(0xFF8A5A2B), Offset(c.x, c.y + 4f * u), Offset(c.x + 3f * u, c.y - 12f * u), 2f * u)
            repeat(5) { k -> val a = (-160f + k * 35f) * PI.toFloat() / 180f
                drawLine(Color(0xFF4E8A3A), Offset(c.x + 3f * u, c.y - 12f * u), Offset(c.x + 3f * u + cos(a) * 9f * u, c.y - 12f * u + sin(a) * 6f * u), 2f * u) }
            crescent(w - m - 2f * u, m + 2f * u, 7f * u, Color(0xFFC98A3A)); star(w - m - 14f * u, m + 4f * u, 3f * u, Color(0xFFC98A3A))
        }
        "lantern" -> {
            fanous(w * .82f + sin(t * tau) * 2f * u, inset, 10f * u, 1f, Color(0xFFE9C46A), Color(0xFFFFB347))
            fanous(w * .18f - sin(t * tau) * 2f * u, inset, 4f * u, .8f, Color(0xFFE9C46A), Color(0xFFFFD27A))
        }
        "aurora" -> {
            val p = Path(); p.moveTo(inset, inset + 6f * u)
            var x = inset; while (x <= w - inset) { p.lineTo(x, inset + 6f * u + sin(x / (24f * u) + t * tau) * 4f * u); x += 4f * u }
            drawPath(p, Brush.horizontalGradient(listOf(Color(0xAA4FF0B0), Color(0xAA6A8CFF), Color(0xAAC47CFF))), style = Stroke(3f * u))
            repeat(8) { k -> drawCircle(Color.White.copy(alpha = twinkle(k) * .8f), 1.1f * u, along(.55f + k * .05f)) }
        }
        "pearl" -> {
            val sw = { a: Float, b: Float, dip: Float ->
                repeat(11) { k -> val f = k / 10f; val x = w * (a + (b - a) * f); val y = inset + dip * u * 4 * f * (1 - f)
                    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFE6E0F2)), Offset(x - u, y - u), 3f * u), 2.4f * u, Offset(x, y)) } }
            sw(.12f, .5f, 12f); sw(.5f, .88f, 12f)
            val drop = Offset(w * .5f, inset + 10f * u + sin(t * tau) * 1.5f * u)
            hang(w * .5f, 6f * u, Color(0xFFD9D2EA)); drawOval(Brush.radialGradient(listOf(Color.White, Color(0xFFE0D6F0)), drop, 6f * u), Offset(drop.x - 4f * u, drop.y), Size(8f * u, 10f * u))
        }
        "royal" -> {
            val c = Offset(w / 2, inset + 2f * u); val gold = Color(0xFFE9C46A)
            val crown = Path().apply {
                moveTo(c.x - 12f * u, c.y + 9f * u); lineTo(c.x - 12f * u, c.y - 2f * u); lineTo(c.x - 6f * u, c.y + 3f * u); lineTo(c.x, c.y - 6f * u)
                lineTo(c.x + 6f * u, c.y + 3f * u); lineTo(c.x + 12f * u, c.y - 2f * u); lineTo(c.x + 12f * u, c.y + 9f * u); close()
            }
            drawPath(crown, gold); drawCircle(Color(0xFFE0115F), 1.8f * u, Offset(c.x, c.y + 4f * u))
            corners.forEachIndexed { k, cc -> gem(cc.x, cc.y, 4.5f * u, listOf(Color(0xFFE0115F), Color(0xFF2E7DFF), Color(0xFF2EBE6A), Color(0xFFE0115F))[k]) }
        }
        "crystal" -> {
            corners.forEachIndexed { k, c -> gem(c.x, c.y, 6f * u, Color(0xFF9EE3F5)); val s = twinkle(k)
                drawLine(Color.White.copy(alpha = s), Offset(c.x + 6f * u, c.y - 9f * u), Offset(c.x + 6f * u, c.y - 3f * u), 1f * u)
                drawLine(Color.White.copy(alpha = s), Offset(c.x + 3f * u, c.y - 6f * u), Offset(c.x + 9f * u, c.y - 6f * u), 1f * u) }
        }
    }
}
