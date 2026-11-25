package com.alzikr.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * The second set of living backgrounds: «رواق النور» (arches over a garden that follows the prayer times, a fountain and a
 * clock), «المخطوطة السلطانية», «الواحة الحية», «زجاج معشّق», «عالم من الورق», «المرصد» (with the real moon phase),
 * «حديقة الفصول» (spring, summer, autumn, winter, Ramadan and Eid) and «الرخام والذهب».
 */
private fun r2(i: Int, k: Int): Float { val x = sin(i * 12.9898f + k * 78.233f) * 43758.547f; return x - floor(x) }
private fun mix(a: Color, b: Color, t: Float) = Tints.mix(a, b, t.coerceIn(0f, 1f))

private fun pal9(dark: Boolean, bg: Long, surface: Long, card: Long, line: Long, ink: Long, muted: Long, accent: Long, gold: Long) =
    Pal(dark, Color(bg), Color(surface), Color(card), Color(line), Color(ink), Color(muted), Color(accent), Tints.on(Color(accent)), Color(gold), Color(0xFFD9534F))

private fun nowMin() = LocalTime.now().let { it.hour * 60 + it.minute }
private fun times(): Map<Prayer, Int> = runCatching { Store.times() }.getOrNull()
    ?: mapOf(Prayer.FAJR to 300, Prayer.SUNRISE to 380, Prayer.DHUHR to 730, Prayer.ASR to 930, Prayer.MAGHRIB to 1080, Prayer.ISHA to 1160)

/** «رواق النور»: seven moments of the day by the real prayer times; the sky's colors move gradually between them. */
object Riwaq {
    enum class Moment { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA, MIDNIGHT }
    private fun points(): List<Pair<Int, Moment>> {
        val x = times(); val fajr = x.getValue(Prayer.FAJR); val isha = x.getValue(Prayer.ISHA)
        val midnight = ((isha + (fajr + 1440)) / 2) % 1440
        return listOf(fajr to Moment.FAJR, x.getValue(Prayer.SUNRISE) to Moment.SUNRISE, x.getValue(Prayer.DHUHR) - 90 to Moment.DHUHR,
            x.getValue(Prayer.ASR) to Moment.ASR, x.getValue(Prayer.MAGHRIB) - 20 to Moment.MAGHRIB, isha to Moment.ISHA, midnight to Moment.MIDNIGHT).sortedBy { it.first }
    }
    fun moment(): Moment {
        @Suppress("UNUSED_VARIABLE") val v = Sky.tick
        val m = nowMin(); val p = points()
        return (p.lastOrNull { it.first <= m } ?: p.last()).second
    }
    /** The sky's top and horizon colors, blended over the half hour before the next moment. */
    fun sky(): Pair<Color, Color> {
        val m = nowMin(); val p = points()
        val i = p.indexOfLast { it.first <= m }.let { if (it < 0) p.lastIndex else it }
        val next = p[(i + 1) % p.size]
        val until = ((next.first - m) + 1440) % 1440
        val t = if (until < 30) 1f - until / 30f else 0f
        val a = colors(p[i].second); val b = colors(next.second)
        return mix(a.first, b.first, t) to mix(a.second, b.second, t)
    }
    fun colors(m: Moment): Pair<Color, Color> = when (m) {
        Moment.FAJR -> Color(0xFF3B3A78) to Color(0xFF8E7BC0)
        Moment.SUNRISE -> Color(0xFF6C8FC8) to Color(0xFFF2C47A)
        Moment.DHUHR -> Color(0xFF7FC2E8) to Color(0xFFDDEFE0)
        Moment.ASR -> Color(0xFF8AB0CC) to Color(0xFFF0C890)
        Moment.MAGHRIB -> Color(0xFF6A4A88) to Color(0xFFF08A5A)
        Moment.ISHA -> Color(0xFF0E1830) to Color(0xFF233A5E)
        Moment.MIDNIGHT -> Color(0xFF070C1A) to Color(0xFF15213A)
    }
    fun pal(): Pal = palOf(moment())
    fun palOf(m: Moment): Pal = when (m) {
        Moment.FAJR -> pal9(true, 0xFF15162E, 0xFF1D1F3C, 0xFF262A4C, 0xFF3A3F68, 0xFFF0ECF8, 0xFFB0A8CC, 0xFF9FB4F0, 0xFFC9A66B)
        Moment.SUNRISE -> pal9(false, 0xFFF8F4EA, 0xFFFFFCF5, 0xFFF0E7D2, 0xFFE0D2B0, 0xFF174D43, 0xFF5E7A70, 0xFF174D43, 0xFFB0883E)
        Moment.DHUHR -> pal9(false, 0xFFF8F7F4, 0xFFFFFFFF, 0xFFEDF2EE, 0xFFD5E0D8, 0xFF123E36, 0xFF587268, 0xFF174D43, 0xFFC9A66B)
        Moment.ASR -> pal9(false, 0xFFF5EFE3, 0xFFFFFAF0, 0xFFEFE3CC, 0xFFDDCBA6, 0xFF2E2414, 0xFF7A6A4E, 0xFF498E91, 0xFFB0883E)
        Moment.MAGHRIB -> pal9(true, 0xFF21141C, 0xFF2B1A24, 0xFF36212E, 0xFF523246, 0xFFF8E8E4, 0xFFC8A0A4, 0xFFF0A070, 0xFFC9A66B)
        Moment.ISHA -> pal9(true, 0xFF101D35, 0xFF172743, 0xFF1F3152, 0xFF2E4570, 0xFFF5EFE3, 0xFFA8B4C8, 0xFFC9A66B, 0xFFC9A66B)
        Moment.MIDNIGHT -> pal9(true, 0xFF0A1224, 0xFF111B33, 0xFF182441, 0xFF26365C, 0xFFE8ECF4, 0xFF98A4BC, 0xFFC8D2E4, 0xFFC9A66B)
    }
}

/** Spring, summer, autumn, winter (by the month, as in the north), Ramadan and the two Eids (by the Hijri date). */
object YearSeason {
    enum class S(val ar: String) { SPRING("الربيع"), SUMMER("الصيف"), AUTUMN("الخريف"), WINTER("الشتاء"), RAMADAN("رمضان"), EID("العيد") }
    fun now(): S {
        val h = runCatching { Data.hijri() }.getOrNull()
        if (h != null) {
            if (h.month == 9) return S.RAMADAN
            if ((h.month == 10 && h.day <= 3) || (h.month == 12 && h.day in 10..13)) return S.EID
        }
        return when (LocalDate.now().monthValue) { 3, 4, 5 -> S.SPRING; 6, 7, 8 -> S.SUMMER; 9, 10, 11 -> S.AUTUMN; else -> S.WINTER }
    }
    fun pal(): Pal = palOf(now())
    fun palOf(s: S): Pal = when (s) {
        S.SPRING -> pal9(false, 0xFFF7FAF2, 0xFFFFFFFF, 0xFFEAF3E2, 0xFFD2E4C4, 0xFF22381C, 0xFF63785A, 0xFF4E9A3E, 0xFFE0709A)
        S.SUMMER -> pal9(false, 0xFFF0F8F8, 0xFFFFFFFF, 0xFFDDF0EE, 0xFFC2E0DC, 0xFF0E3238, 0xFF4E7074, 0xFF0E8A8A, 0xFFE0A030)
        S.AUTUMN -> pal9(false, 0xFFFBF3E8, 0xFFFFFAF2, 0xFFF3E2CC, 0xFFE3C8A4, 0xFF3E2410, 0xFF85664A, 0xFFC0561E, 0xFF8A6A1A)
        S.WINTER -> pal9(true, 0xFF0E1520, 0xFF15202E, 0xFF1C293A, 0xFF2C3C52, 0xFFEAF0F8, 0xFF9AAABE, 0xFF8EC4F0, 0xFFF0B860)
        S.RAMADAN -> pal9(true, 0xFF120E24, 0xFF1A1432, 0xFF231B42, 0xFF382C62, 0xFFF6EEDC, 0xFFB4A8C8, 0xFFF2C14E, 0xFFE85A4F)
        S.EID -> pal9(false, 0xFFFDF6F8, 0xFFFFFFFF, 0xFFF8E6EC, 0xFFEECBD6, 0xFF3A1A2A, 0xFF8A6070, 0xFFB0407A, 0xFF2E9A8A)
    }
}

/** The moon's age in days (0 new … 14.8 full … 29.5), from a known new moon. Only to draw it as it looks tonight. */
fun moonAge(): Float {
    val known = 947182440000L       // 2000-01-06 18:14 UTC, a new moon
    val days = (System.currentTimeMillis() - known) / 86_400_000.0
    return ((days % 29.530588) + 29.530588).toFloat() % 29.530588f
}

/** Draws the moon as it is: the lit part from the age (waxing lit on the right). */
fun DrawScope.moon(c: Offset, r: Float, lit: Color, dark: Color) {
    val age = moonAge(); val f = age / 29.530588f                    // 0..1
    drawCircle(dark, r, c)
    val k = cos(f * 2 * PI.toFloat())                                 // 1 new, -1 full
    val waxing = f < .5f
    val path = Path().apply {
        // the outer half circle on the lit side, then the terminator as a half ellipse
        val steps = 24
        for (s in 0..steps) {
            val a = -PI.toFloat() / 2 + PI.toFloat() * s / steps
            val x = c.x + (if (waxing) 1 else -1) * cos(a) * r; val y = c.y + sin(a) * r
            if (s == 0) moveTo(x, y) else lineTo(x, y)
        }
        for (s in steps downTo 0) {
            val a = -PI.toFloat() / 2 + PI.toFloat() * s / steps
            val x = c.x + (if (waxing) 1 else -1) * cos(a) * r * k; val y = c.y + sin(a) * r
            lineTo(x, y)
        }
        close()
    }
    drawPath(path, lit)
}

// ------------------------------------------------------------------------------- «رواق النور»

fun DrawScope.riwaq(p: Pal, t: Float) {
    val (top, horizon) = Riwaq.sky(); val moment = Riwaq.moment()
    val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(listOf(top.copy(alpha = .55f), horizon.copy(alpha = .45f), Color.Transparent), 0f, h * .62f))
    val night = moment == Riwaq.Moment.ISHA || moment == Riwaq.Moment.MIDNIGHT
    if (night || moment == Riwaq.Moment.FAJR) for (i in 0 until 36) {
        val tw = .5f + .5f * sin(t * (1f + r2(i, 4) * 2f) + i)
        drawCircle((if (moment == Riwaq.Moment.MIDNIGHT) Color(0xFFDDE6F4) else Color.White).copy(alpha = (.12f + .22f * tw) * (if (moment == Riwaq.Moment.FAJR) .5f else 1f)),
            1f + r2(i, 5) * 1.6f, Offset(r2(i, 1) * w, r2(i, 2) * h * .36f))
    }
    // the garden behind: trees whose leaves sway, lit by the moment
    val leaf = when (moment) { Riwaq.Moment.DHUHR, Riwaq.Moment.SUNRISE -> Color(0xFF2E7A52); Riwaq.Moment.ASR -> Color(0xFF5A7A3A)
        Riwaq.Moment.MAGHRIB -> Color(0xFF6A4A3A); else -> Color(0xFF1C3A34) }
    for (i in 0 until 7) {
        val x = w * (i + .5f) / 7f; val base = h * .9f; val r = w * (.07f + r2(i, 3) * .04f)
        drawLine(Color(0xFF4A3A28).copy(alpha = .25f), Offset(x, base), Offset(x, base - r * 1.6f), r * .18f)
        for (k in 0 until 4) {
            val sway = sin(t * .6f + i + k) * 3f
            drawCircle(leaf.copy(alpha = .22f), r * (.7f + k * .1f), Offset(x + (k - 1.5f) * r * .5f + sway, base - r * 1.7f - (k % 2) * r * .4f))
        }
    }
    // the fountain: a basin and arcs of water, and the light on it
    val fc = Offset(w / 2, h * .93f)
    drawOval(Color.White.copy(alpha = .18f), Offset(fc.x - w * .16f, fc.y - 10f), Size(w * .32f, 22f))
    drawOval(Color(0xFF498E91).copy(alpha = .35f), Offset(fc.x - w * .14f, fc.y - 6f), Size(w * .28f, 14f))
    for (k in 0 until 6) {
        val ph = (t * .5f + k / 6f) % 1f
        val dir = if (k % 2 == 0) 1 else -1
        val x = fc.x + dir * ph * w * .1f; val y = fc.y - 18f - sin(ph * PI.toFloat()) * 60f
        drawCircle(Color.White.copy(alpha = .35f * (1 - ph)), 2.4f, Offset(x, y))
    }
    drawLine(Color.White.copy(alpha = .3f), Offset(fc.x, fc.y - 10f), Offset(fc.x, fc.y - 70f - sin(t * 3) * 4f), 3f)
    // the colonnade in front: a wall with three pointed arches, gilded edges
    val wallH = h * .34f
    val wall = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, wallH))
        for (i in 0 until 3) {
            val cx = w * (i + .5f) / 3f; val aw = w / 3f * .72f; val spring = wallH * .52f
            moveTo(cx - aw / 2, wallH); lineTo(cx - aw / 2, spring)
            cubicTo(cx - aw / 2, spring - aw * .35f, cx - aw * .08f, spring - aw * .5f, cx, wallH * .08f)
            cubicTo(cx + aw * .08f, spring - aw * .5f, cx + aw / 2, spring - aw * .35f, cx + aw / 2, spring)
            lineTo(cx + aw / 2, wallH); close()
        }
    }
    drawPath(wall, (if (p.dark) Color(0xFF0C1528) else Color(0xFFF5EFE3)).copy(alpha = .62f))
    drawPath(wall, Color(0xFFC9A66B).copy(alpha = .45f), style = Stroke(1.5f))
    // light through the arches in the day; lanterns in them at night
    if (moment == Riwaq.Moment.SUNRISE || moment == Riwaq.Moment.DHUHR || moment == Riwaq.Moment.ASR) for (i in 0 until 3) {
        val cx = w * (i + .5f) / 3f; val slant = if (moment == Riwaq.Moment.ASR) 120f else if (moment == Riwaq.Moment.SUNRISE) -120f else 0f
        val ray = Path().apply { moveTo(cx - 40f, wallH * .5f); lineTo(cx + 40f, wallH * .5f); lineTo(cx + 120f + slant, h * .8f); lineTo(cx - 120f + slant, h * .8f); close() }
        drawPath(ray, Brush.verticalGradient(listOf(Color(0xFFFFE6A8).copy(alpha = .16f + .03f * sin(t * .5f + i)), Color.Transparent), wallH * .5f, h * .8f))
    }
    if (night || moment == Riwaq.Moment.MAGHRIB) for (i in 0 until 3) {
        val cx = w * (i + .5f) / 3f; val y = wallH * .62f
        drawLine(Color(0xFFC9A66B).copy(alpha = .5f), Offset(cx, wallH * .3f), Offset(cx, y), 1.2f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD27A).copy(alpha = .45f + .1f * sin(t * 2 + i)), Color.Transparent), Offset(cx, y + 14f), 60f), 60f, Offset(cx, y + 14f))
        drawRoundRect(Color(0xFFE8B850).copy(alpha = .8f), Offset(cx - 8f, y), Size(16f, 26f), androidx.compose.ui.geometry.CornerRadius(6f))
    }
    // the clock in the middle arch's spandrel
    val cc = Offset(w / 2, wallH * .26f); val cr = min(w, h) * .055f
    drawCircle((if (p.dark) Color(0xFF101D35) else Color(0xFFF8F7F4)).copy(alpha = .85f), cr, cc)
    drawCircle(Color(0xFFC9A66B), cr, cc, style = Stroke(2.5f))
    for (k in 0 until 12) { val a = k * PI.toFloat() / 6; drawCircle(Color(0xFFC9A66B), if (k % 3 == 0) 2.6f else 1.4f, Offset(cc.x + cos(a) * cr * .8f, cc.y + sin(a) * cr * .8f)) }
    val now = LocalTime.now(); val ha = ((now.hour % 12) + now.minute / 60f) / 12f * 2 * PI.toFloat() - PI.toFloat() / 2; val ma = now.minute / 60f * 2 * PI.toFloat() - PI.toFloat() / 2
    drawLine(Color(0xFF174D43).let { if (p.dark) Color(0xFFF5EFE3) else it }, cc, Offset(cc.x + cos(ha) * cr * .5f, cc.y + sin(ha) * cr * .5f), 3.5f)
    drawLine(Color(0xFFC9A66B), cc, Offset(cc.x + cos(ma) * cr * .75f, cc.y + sin(ma) * cr * .75f), 2f)
    // leaves drifting down slowly
    for (i in 0 until 6) {
        val ph = (t * (.02f + r2(i, 3) * .02f) + r2(i, 1)) % 1f
        val x = r2(i, 2) * w + sin(t * .7f + i) * 20f; val y = wallH + ph * (h - wallH)
        rotate(t * 30f + i * 50f, Offset(x, y)) { drawOval(leaf.copy(alpha = .3f), Offset(x - 6f, y - 3f), Size(12f, 6f)) }
    }
}

// ------------------------------------------------------------------------------- «المخطوطة السلطانية»

fun DrawScope.manuscript(p: Pal) {
    val g = p.gold; val w = size.width; val h = size.height; val m = 12f
    drawRect(Brush.horizontalGradient(listOf(g.copy(alpha = .22f), Color.Transparent), 0f, 26f), Offset(0f, 0f), Size(26f, h))
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, g.copy(alpha = .22f)), w - 26f, w), Offset(w - 26f, 0f), Size(26f, h))
    drawRect(g.copy(alpha = .35f), Offset(m, m), Size(w - 2 * m, h - 2 * m), style = Stroke(2f))
    drawRect(g.copy(alpha = .2f), Offset(m + 8, m + 8), Size(w - 2 * m - 16, h - 2 * m - 16), style = Stroke(1f))
    // a shamsa (a gilded sunburst) at the top, as on a manuscript's opening page
    val c = Offset(w / 2, h * .1f)
    for (k in 0 until 32) {
        val a = k * PI.toFloat() / 16
        drawLine(g.copy(alpha = if (k % 2 == 0) .3f else .16f), Offset(c.x + cos(a) * 26f, c.y + sin(a) * 26f), Offset(c.x + cos(a) * (if (k % 2 == 0) 58f else 44f), c.y + sin(a) * (if (k % 2 == 0) 58f else 44f)), 2f)
    }
    drawCircle(g.copy(alpha = .35f), 24f, c, style = Stroke(2f)); drawCircle(g.copy(alpha = .18f), 16f, c)
    // corner medallions
    listOf(Offset(m, m), Offset(w - m, m), Offset(m, h - m), Offset(w - m, h - m)).forEach { o ->
        rotate(45f, o) { drawRect(g.copy(alpha = .4f), Offset(o.x - 9f, o.y - 9f), Size(18f, 18f), style = Stroke(1.5f)) }
        drawCircle(g.copy(alpha = .5f), 4f, o)
    }
    // faint ink ruling, as on the page
    var y = h * .2f
    while (y < h * .95f) { drawLine(p.ink.copy(alpha = .035f), Offset(w * .08f, y), Offset(w * .92f, y), 1f); y += 44f }
}

// ------------------------------------------------------------------------------- «الواحة الحية»

fun DrawScope.oasis(p: Pal, t: Float) {
    val w = size.width; val h = size.height
    val ph = Sky.phase()
    val (skyTop, skyLow) = when (ph) { Sky.Phase.NIGHT -> Color(0xFF0B1230) to Color(0xFF1E2A50); Sky.Phase.DAWN -> Color(0xFF5A4A8A) to Color(0xFFF2B48A)
        Sky.Phase.SUNSET -> Color(0xFF6A3A6A) to Color(0xFFF08A50); Sky.Phase.DUSK -> Color(0xFF2A2050) to Color(0xFF6A4A7A)
        Sky.Phase.AFTERNOON -> Color(0xFF7AAAD0) to Color(0xFFF2D49A); else -> Color(0xFF6BB8E8) to Color(0xFFCFEAF2) }
    drawRect(Brush.verticalGradient(listOf(skyTop.copy(alpha = .4f), skyLow.copy(alpha = .3f), Color.Transparent), 0f, h * .7f))
    if (ph == Sky.Phase.NIGHT || ph == Sky.Phase.DUSK) {
        for (i in 0 until 30) drawCircle(Color.White.copy(alpha = .1f + .2f * (.5f + .5f * sin(t + i))), 1.3f, Offset(r2(i, 1) * w, r2(i, 2) * h * .35f))
        moon(Offset(w * .8f, h * .1f), 18f, Color(0xFFF4EBC8).copy(alpha = .7f), Color(0xFF1E2A50).copy(alpha = .5f))
    } else drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD45A).copy(alpha = .4f), Color.Transparent), Offset(w * .25f, h * .14f), 110f), 110f, Offset(w * .25f, h * .14f))
    // layers moving at different speeds (depth): far mountains, near dunes, the water, the palms in front
    fun ridge(base: Float, amp: Float, speed: Float, freq: Float, col: Color) {
        val shift = (t * speed) % w
        val path = Path().apply {
            moveTo(0f, h); var x = 0f
            while (x <= w + 20f) { lineTo(x, base - (sin((x + shift) / w * PI.toFloat() * freq) * .5f + .5f) * amp); x += 18f }
            lineTo(w, h); close()
        }
        drawPath(path, col)
    }
    ridge(h * .78f, h * .12f, 2f, 3f, Color(0xFF7A6A8A).copy(alpha = .14f))
    ridge(h * .86f, h * .06f, 6f, 2f, Color(0xFFD8B070).copy(alpha = .2f))
    val pool = Offset(w * .5f, h * .9f)
    drawOval(Color(0xFF3FA8C0).copy(alpha = .3f), Offset(pool.x - w * .3f, pool.y - 16f), Size(w * .6f, 32f))
    for (k in 0 until 5) { val x = pool.x - w * .25f + ((t * 14f + k * 60f) % (w * .5f)); drawLine(Color.White.copy(alpha = .25f), Offset(x, pool.y - 4f + k % 2 * 6f), Offset(x + 18f, pool.y - 4f + k % 2 * 6f), 1.5f) }
    listOf(.14f to 1f, .82f to .85f, .7f to .6f).forEachIndexed { i, (fx, s) ->       // palms
        val bx = w * fx; val by = h * .9f; val th = h * .22f * s; val sway = sin(t * .6f + i) * 6f
        val trunk = Path().apply { moveTo(bx, by); quadraticTo(bx + 14f * s, by - th * .5f, bx + 6f * s + sway, by - th) }
        drawPath(trunk, Color(0xFF6A4A2A).copy(alpha = .35f), style = Stroke(9f * s))
        val top = Offset(bx + 6f * s + sway, by - th)
        for (k in 0 until 7) {
            val a = PI.toFloat() * (1.05f + k * .15f) + sin(t * .8f + k) * .05f
            val tip = Offset(top.x + cos(a) * 60f * s, top.y + sin(a) * 34f * s + 18f * s)
            val frond = Path().apply { moveTo(top.x, top.y); quadraticTo((top.x + tip.x) / 2, top.y - 22f * s, tip.x, tip.y) }
            drawPath(frond, Color(0xFF3E8A4A).copy(alpha = .35f), style = Stroke(5f * s))
        }
    }
    // the weather: a light rain in winter, mist in the morning
    val month = LocalDate.now().monthValue
    if (month in listOf(12, 1, 2) && LocalDate.now().dayOfMonth % 3 == 0) for (i in 0 until 30) {
        val q = (t * (.25f + r2(i, 3) * .15f) + r2(i, 1)) % 1f; val x = r2(i, 2) * w
        drawLine(Color.White.copy(alpha = .12f), Offset(x, q * h), Offset(x - 4f, q * h + 16f), 1.4f)
    }
    if (ph == Sky.Phase.DAWN || ph == Sky.Phase.MORNING) drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .12f), Color.Transparent), h * .7f, h * .95f), Offset(0f, h * .7f), Size(w, h * .25f))
}

// ------------------------------------------------------------------------------- «زجاج معشّق»

fun DrawScope.stained(p: Pal, t: Float) {
    val w = size.width; val h = size.height
    val cols = listOf(Color(0xFFE0453A), Color(0xFF2E7DD8), Color(0xFFF2B53A), Color(0xFF2EAA6A), Color(0xFF8A4AD0), Color(0xFF20B8C0))
    val c = Offset(w / 2, h * .16f); val R = w * .3f
    // the window: a rosette of colored panes in lead lines
    for (ring in 0 until 3) {
        val r0 = R * ring / 3f; val r1 = R * (ring + 1) / 3f; val n = 8 * (ring + 1)
        for (k in 0 until n) {
            val a0 = k * 2 * PI.toFloat() / n + ring * .2f; val a1 = (k + 1) * 2 * PI.toFloat() / n + ring * .2f
            val pane = Path().apply {
                moveTo(c.x + cos(a0) * r0, c.y + sin(a0) * r0); lineTo(c.x + cos(a0) * r1, c.y + sin(a0) * r1)
                lineTo(c.x + cos(a1) * r1, c.y + sin(a1) * r1); lineTo(c.x + cos(a1) * r0, c.y + sin(a1) * r0); close()
            }
            val glow = .22f + .08f * sin(t * .6f + k + ring)
            drawPath(pane, cols[(k + ring) % cols.size].copy(alpha = glow))
            drawPath(pane, Color.Black.copy(alpha = .35f), style = Stroke(2f))
        }
    }
    drawCircle(Color(0xFFC9A66B).copy(alpha = .5f), R, c, style = Stroke(3f))
    // light falling from it, its colors laid softly on the floor and moving with the sun
    val drift = sin(t * .05f) * w * .08f
    for (k in 0 until 6) {
        val x0 = c.x + (k - 2.5f) * R * .25f; val x1 = w * (.1f + k * .16f) + drift
        val beam = Path().apply { moveTo(x0 - 10f, c.y + R * .6f); lineTo(x0 + 10f, c.y + R * .6f); lineTo(x1 + 50f, h * .95f); lineTo(x1 - 50f, h * .95f); close() }
        drawPath(beam, Brush.verticalGradient(listOf(cols[k].copy(alpha = .10f), cols[k].copy(alpha = .03f)), c.y, h))
        drawOval(cols[k].copy(alpha = .10f), Offset(x1 - 60f, h * .93f - 14f), Size(120f, 28f))
    }
}

// ------------------------------------------------------------------------------- «عالم من الورق»

fun DrawScope.paper(p: Pal, t: Float) {
    val w = size.width; val h = size.height
    val tones = if (p.dark) listOf(Color(0xFF2A3350), Color(0xFF323C5E), Color(0xFF3C486C), Color(0xFF46537A)) else listOf(Color(0xFFE8DCCB), Color(0xFFDCCBB2), Color(0xFFCDB89A), Color(0xFFBDA684))
    val shadow = Color.Black.copy(alpha = if (p.dark) .35f else .14f)
    fun shifted(path: Path, dx: Float, dy: Float) = Path().apply { addPath(path, Offset(dx, dy)) }
    for (i in 0 until 3) {                         // paper clouds
        val cx = ((r2(i, 1) * w + t * (5f + i * 3f)) % (w + 200f)) - 100f; val cy = h * (.06f + i * .06f)
        val cloud = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx, cy, cx + 90f, cy + 30f)); addOval(androidx.compose.ui.geometry.Rect(cx + 25f, cy - 16f, cx + 70f, cy + 20f)) }
        drawPath(shifted(cloud, 4f, 5f), shadow); drawPath(cloud, (if (p.dark) Color(0xFF4A5680) else Color.White).copy(alpha = .9f))
    }
    val mountains = Path().apply { moveTo(0f, h); lineTo(0f, h * .74f); lineTo(w * .2f, h * .62f); lineTo(w * .38f, h * .72f); lineTo(w * .6f, h * .58f); lineTo(w * .82f, h * .7f); lineTo(w, h * .64f); lineTo(w, h); close() }
    val hills = Path().apply { moveTo(0f, h); lineTo(0f, h * .82f); quadraticTo(w * .25f, h * .74f, w * .5f, h * .8f); quadraticTo(w * .78f, h * .86f, w, h * .78f); lineTo(w, h); close() }
    val mosque = Path().apply {
        val b = h * .88f; val cx = w * .55f
        moveTo(cx - w * .2f, b); lineTo(cx - w * .2f, b - 50f); lineTo(cx - w * .12f, b - 50f)
        cubicTo(cx - w * .12f, b - 130f, cx + w * .12f, b - 130f, cx + w * .12f, b - 50f)
        lineTo(cx + w * .2f, b - 50f); lineTo(cx + w * .2f, b); close()
        val mx = cx + w * .26f; moveTo(mx - 10f, b); lineTo(mx - 10f, b - 160f); lineTo(mx, b - 185f); lineTo(mx + 10f, b - 160f); lineTo(mx + 10f, b); close()
    }
    val front = Path().apply { moveTo(0f, h); lineTo(0f, h * .9f); quadraticTo(w * .3f, h * .86f, w * .6f, h * .92f); quadraticTo(w * .85f, h * .96f, w, h * .9f); lineTo(w, h); close() }
    listOf(mountains to tones[0], hills to tones[1], mosque to tones[2], front to tones[3]).forEach { (path, col) ->
        drawPath(shifted(path, 5f, 6f), shadow); drawPath(path, col.copy(alpha = .75f))
    }
    for (i in 0 until 4) {                            // paper trees
        val x = w * (.08f + i * .27f); val b = h * .93f
        val tree = Path().apply { moveTo(x, b - 70f); lineTo(x + 22f, b - 20f); lineTo(x - 22f, b - 20f); close(); addRect(androidx.compose.ui.geometry.Rect(x - 4f, b - 22f, x + 4f, b)) }
        drawPath(shifted(tree, 4f, 4f), shadow); drawPath(tree, (if (p.dark) Color(0xFF3E6A5A) else Color(0xFF8AB89A)).copy(alpha = .8f))
    }
}

// ------------------------------------------------------------------------------- «المرصد»

fun DrawScope.observatory(p: Pal, t: Float) {
    val w = size.width; val h = size.height
    for (i in 0 until 60) drawCircle(Color.White.copy(alpha = .06f + .16f * (.5f + .5f * sin(t * (.6f + r2(i, 4)) + i))), .8f + r2(i, 5) * 1.4f, Offset(r2(i, 1) * w, r2(i, 2) * h))
    // an astrolabe: graduated rings turning slowly at different speeds, and its rete's pointers
    val c = Offset(w / 2, h * .42f); val R = w * .44f
    val silver = Color(0xFFC8D0E0); val gold = Color(0xFFD8B060)
    drawCircle(silver.copy(alpha = .14f), R, c, style = Stroke(2f))
    rotate(t * 1.5f, c) {
        for (k in 0 until 72) { val a = k * PI.toFloat() / 36; val l = if (k % 6 == 0) 16f else 7f
            drawLine(silver.copy(alpha = .18f), Offset(c.x + cos(a) * (R - l), c.y + sin(a) * (R - l)), Offset(c.x + cos(a) * R, c.y + sin(a) * R), 1.2f) }
    }
    rotate(-t * 3f, c) {
        drawCircle(gold.copy(alpha = .16f), R * .72f, c, style = Stroke(1.5f))
        drawCircle(gold.copy(alpha = .12f), R * .45f, Offset(c.x, c.y - R * .15f), style = Stroke(1.5f))
        for (k in 0 until 8) { val a = k * PI.toFloat() / 4
            val tip = Offset(c.x + cos(a) * R * .68f, c.y + sin(a) * R * .68f)
            drawLine(gold.copy(alpha = .2f), Offset(c.x + cos(a) * R * .3f, c.y + sin(a) * R * .3f), tip, 1.5f); drawCircle(gold.copy(alpha = .35f), 3f, tip) }
    }
    drawLine(silver.copy(alpha = .2f), Offset(c.x - R, c.y), Offset(c.x + R, c.y), 1f); drawLine(silver.copy(alpha = .2f), Offset(c.x, c.y - R), Offset(c.x, c.y + R), 1f)
    drawCircle(gold.copy(alpha = .4f), 6f, c)
    // the moon as it really looks tonight
    moon(Offset(w * .82f, h * .09f), 24f, Color(0xFFF2EAD0).copy(alpha = .8f), Color(0xFF2A2F55).copy(alpha = .6f))
}

// ------------------------------------------------------------------------------- «حديقة الفصول»

fun DrawScope.seasons(p: Pal, t: Float) {
    val w = size.width; val h = size.height
    when (YearSeason.now()) {
        YearSeason.S.SPRING -> {
            for (i in 0 until 5) { val x = w * (i + .5f) / 5f; val b = h * .96f
                drawLine(Color(0xFF6A4A2A).copy(alpha = .25f), Offset(x, b), Offset(x, b - 90f), 6f)
                for (k in 0 until 9) drawCircle(Color(0xFFF4A7C0).copy(alpha = .3f), 10f, Offset(x + cos(k * .7f) * 30f, b - 100f + sin(k * 1.3f) * 20f)) }
            for (i in 0 until 6) {          // butterflies
                val x = w * (.5f + .4f * sin(t * (.1f + r2(i, 3) * .1f) + i * 2f)); val y = h * (.45f + .3f * cos(t * (.08f + r2(i, 4) * .1f) + i))
                val flap = abs(sin(t * 6f + i)) * 8f + 2f; val col = listOf(Color(0xFFF2B53A), Color(0xFFE0709A), Color(0xFF6AB0E0))[i % 3].copy(alpha = .45f)
                drawOval(col, Offset(x - flap - 2f, y - 6f), Size(flap, 12f)); drawOval(col, Offset(x + 2f, y - 6f), Size(flap, 12f))
            }
        }
        YearSeason.S.SUMMER -> {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF2EAAB0).copy(alpha = .18f)), h * .8f, h), Offset(0f, h * .8f), Size(w, h * .2f))
            for (i in 0 until 26) { val tw = (sin(t * 2f + i * 1.7f) * .5f + .5f).let { it * it }
                drawCircle(Color.White.copy(alpha = .5f * tw), 2.5f, Offset(r2(i, 1) * w, h * (.82f + r2(i, 2) * .16f))) }
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD45A).copy(alpha = .35f), Color.Transparent), Offset(w * .8f, h * .1f), 140f), 140f, Offset(w * .8f, h * .1f))
        }
        YearSeason.S.AUTUMN -> for (i in 0 until 26) {
            val q = (t * (.03f + r2(i, 3) * .04f) + r2(i, 1)) % 1f
            val x = r2(i, 2) * w + sin(t * .8f + i) * 30f; val y = q * (h + 30f) - 15f
            val col = listOf(Color(0xFFE08A2E), Color(0xFFC0561E), Color(0xFFD8B040), Color(0xFF8A3A1A))[i % 4].copy(alpha = .4f)
            rotate(t * 50f + i * 40f, Offset(x, y)) { drawOval(col, Offset(x - 9f, y - 5f), Size(18f, 10f)) }
        }
        YearSeason.S.WINTER -> {
            for (i in 0 until 3) { val x = w * (.15f + i * .32f); val b = h * .96f      // houses with warm windows
                drawRect(Color(0xFF2A3446).copy(alpha = .55f), Offset(x, b - 70f), Size(90f, 70f))
                drawPath(Path().apply { moveTo(x - 8f, b - 70f); lineTo(x + 45f, b - 110f); lineTo(x + 98f, b - 70f); close() }, Color(0xFFEAF0F8).copy(alpha = .4f))
                drawRect(Color(0xFFFFC85A).copy(alpha = .5f + .15f * sin(t + i)), Offset(x + 30f, b - 50f), Size(28f, 24f)) }
            for (i in 0 until 50) { val q = (t * (.04f + r2(i, 3) * .05f) + r2(i, 1)) % 1f
                drawCircle(Color.White.copy(alpha = .3f), 1.5f + r2(i, 4) * 2.5f, Offset(r2(i, 2) * w + sin(t + i) * 10f, q * h)) }
        }
        YearSeason.S.RAMADAN -> for (i in 1 until 9) {
            val x = w * i / 9f; val y = h * .05f + 20f * sin(PI.toFloat() * i / 9f); val sw = sin(t * 1.2f + i) * 4f
            drawLine(Color(0xFFF2C14E).copy(alpha = .3f), Offset(x, 0f), Offset(x + sw, y), 1f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFF2C14E).copy(alpha = .35f), Color.Transparent), Offset(x + sw, y + 14f), 30f), 30f, Offset(x + sw, y + 14f))
            drawRoundRect(listOf(Color(0xFFE85A4F), Color(0xFFF2C14E), Color(0xFF3FB8B0))[i % 3].copy(alpha = .6f), Offset(x + sw - 7f, y), Size(14f, 22f), androidx.compose.ui.geometry.CornerRadius(6f))
        }
        YearSeason.S.EID -> {
            for (i in 0 until 40) { val q = (t * (.06f + r2(i, 3) * .05f) + r2(i, 1)) % 1f
                val col = listOf(Color(0xFFE0709A), Color(0xFFF2B53A), Color(0xFF2E9A8A), Color(0xFF8A6AD0))[i % 4].copy(alpha = .45f)
                rotate(t * 90f + i * 20f, Offset(r2(i, 2) * w, q * h)) { drawRect(col, Offset(r2(i, 2) * w - 4f, q * h - 2f), Size(8f, 4f)) } }
            for (i in 0 until 6) { val x = w * (i + .5f) / 6f; val b = h * .97f
                for (k in 0 until 5) { val a = k * 2 * PI.toFloat() / 5; drawCircle(Color(0xFFE0709A).copy(alpha = .35f), 7f, Offset(x + cos(a) * 8f, b - 30f + sin(a) * 8f)) }
                drawLine(Color(0xFF4E9A5A).copy(alpha = .35f), Offset(x, b), Offset(x, b - 24f), 2f) }
        }
    }
}

// ------------------------------------------------------------------------------- «الرخام والذهب»

fun DrawScope.marble(p: Pal) {
    val w = size.width; val h = size.height
    val vein = if (p.dark) Color.White.copy(alpha = .06f) else Color(0xFF6A6A6A).copy(alpha = .07f)
    for (i in 0 until 9) {
        val y0 = h * r2(i, 1); val path = Path().apply {
            moveTo(-20f, y0)
            cubicTo(w * .3f, y0 + (r2(i, 2) - .5f) * 300f, w * .6f, y0 + (r2(i, 3) - .5f) * 300f, w + 20f, y0 + (r2(i, 4) - .5f) * 200f)
        }
        drawPath(path, vein, style = Stroke(1f + r2(i, 5) * 3f))
        if (i % 3 == 0) drawPath(path, Color(0xFFC9A66B).copy(alpha = .10f), style = Stroke(1f))
    }
    // emerald inlay with a gold edge, top and bottom, like a palace floor's border
    val emerald = Color(0xFF174D43).copy(alpha = if (p.dark) .5f else .3f); val gold = Color(0xFFC9A66B).copy(alpha = .55f)
    listOf(0f, h - 16f).forEach { y ->
        drawRect(emerald, Offset(0f, y), Size(w, 16f))
        drawLine(gold, Offset(0f, if (y == 0f) 16f else y), Offset(w, if (y == 0f) 16f else y), 1.5f)
        var x = 10f
        while (x < w) { rotate(45f, Offset(x, y + 8f)) { drawRect(gold, Offset(x - 3f, y + 5f), Size(6f, 6f)) }; x += 30f }
    }
}
