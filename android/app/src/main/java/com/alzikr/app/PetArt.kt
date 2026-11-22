package com.alzikr.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** The seven friends. Each is drawn by hand from simple shapes (no pictures, no emoji). */
enum class Species(val ar: String, val about: String) {
    CAT("قطة", "هادئة تحب الجلوس قربك"),
    PENGUIN("بطريق", "يتمايل ويرفرف بجناحيه الصغيرين"),
    ALBATROSS("طائر القطرس", "طائر البحر ذو الجناحين الطويلين"),
    SWALLOW("سنونو", "صغير خفيف بذيل مشقوق"),
    RABBIT("أرنب", "يقفز ويحرّك أذنيه"),
    SHEEP("غنمة", "صوفها كالغيمة"),
    CHICKEN("دجاجة", "تنقر الأرض بلطف"),
}

/** How the friend stands right now. */
data class Pose(
    val t: Float = 0f,          // seconds, drives breathing, tail and ears
    val blink: Boolean = false,
    val walk: Float = 0f,       // walking phase (0 = standing)
    val hop: Float = 0f,        // 0..1 height of a hop
    val flap: Float = 0f,       // 0..1 wings up
    val sleep: Boolean = false,
    val happy: Boolean = false,
    val left: Boolean = false,  // facing left
    val tilt: Float = 0f,       // degrees, rocking around the feet (a penguin's waddle)
    val squash: Float = 0f,     // 0..1, a soft squash on landing
)

/** Line drawing: shapes filled with [paper] and outlined in [ink] (the theme's color); null = the colorful drawing. */
data class PetLine(val ink: Color, val paper: Color, val width: Float = 2.4f)

/** A clock for the drawing: frame-accurate in the app, gentler (about 12 fps) over other apps. */
@Composable
fun rememberPetClock(gentle: Boolean = false): () -> Long {
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(gentle) {
        if (gentle) while (true) { now = System.currentTimeMillis(); delay(85) }
        else while (true) { withFrameMillis { now = it } }
    }
    return { now }
}

/** The friend drawn in [modifier]'s box; [pose] is read in the draw phase only, so animating it is cheap. */
@Composable
fun PetSprite(species: Species, pose: () -> Pose, modifier: Modifier, line: PetLine? = null) {
    Canvas(modifier) { drawPet(species, pose(), line) }
}

/** Set while one friend is drawn (drawing happens on the main thread, one at a time). */
private var lineMode: PetLine? = null

private fun c(hex: Long) = Color(hex)

fun DrawScope.drawPet(s: Species, p: Pose, line: PetLine? = null) {
    lineMode = line
    val u = size.minDimension / 100f
    val breathe = sin(p.t * 2.2f) * 0.9f
    // Walking bobs gently; the rabbit hops instead of walking.
    val bob = if (p.walk == 0f) 0f else if (s == Species.RABBIT) abs(sin(p.walk * PI.toFloat())) * 9f else abs(sin(p.walk * PI.toFloat() * 2f)) * 1.6f
    val lift = p.hop * 14f + bob
    // Ground shadow (smaller in the air).
    val sh = 1f - (lift / 14f).coerceIn(0f, 1f) * 0.5f
    drawOval((line?.ink ?: Color.Black).copy(alpha = if (line != null) .16f else .13f * sh), Offset((50 - 22 * sh) * u, 93.5f * u), Size(44 * sh * u, 5 * u))
    val waddle = if (s == Species.PENGUIN && p.walk != 0f) sin(p.walk * PI.toFloat() * 2f) * 7f else 0f
    scale(if (p.left) -1f else 1f, 1f, Offset(size.width / 2, size.height / 2)) {
        scale(1f + p.squash * .08f, 1f - p.squash * .12f, Offset(50 * u, 94 * u)) {
        rotate(p.tilt + waddle, Offset(50 * u, 92 * u)) {
        translate(0f, (-lift - breathe * .4f) * u) {
            when (s) {
                Species.CAT -> cat(u, p)
                Species.PENGUIN -> penguin(u, p)
                Species.ALBATROSS -> albatross(u, p)
                Species.SWALLOW -> swallow(u, p)
                Species.RABBIT -> rabbit(u, p)
                Species.SHEEP -> sheep(u, p)
                Species.CHICKEN -> chicken(u, p)
            }
        }
        }
        }
    }
    if (p.happy && !p.sleep) hearts(u, p.t)
    if (p.sleep) zzz(u, p.t)
    lineMode = null
}

// ------------------------------------------------------------------ helpers (units of 1/100 of the box)

private fun DrawScope.oval(u: Float, cx: Float, cy: Float, w: Float, h: Float, col: Color) {
    val o = Offset((cx - w / 2) * u, (cy - h / 2) * u); val sz = Size(w * u, h * u)
    val l = lineMode
    if (l == null) drawOval(col, o, sz) else { drawOval(l.paper, o, sz); drawOval(l.ink, o, sz, style = Stroke(l.width * u * .5f)) }
}
private fun DrawScope.dot(u: Float, cx: Float, cy: Float, r: Float, col: Color) {
    val l = lineMode
    if (l == null) drawCircle(col, r * u, Offset(cx * u, cy * u)) else { drawCircle(l.paper, r * u, Offset(cx * u, cy * u)); drawCircle(l.ink, r * u, Offset(cx * u, cy * u), style = Stroke(l.width * u * .5f)) }
}
private fun DrawScope.poly(u: Float, col: Color, vararg pts: Float) {
    val path = Path().apply { moveTo(pts[0] * u, pts[1] * u); var i = 2; while (i < pts.size) { lineTo(pts[i] * u, pts[i + 1] * u); i += 2 }; close() }
    val l = lineMode
    if (l == null) drawPath(path, col) else { drawPath(path, l.paper); drawPath(path, l.ink, style = Stroke(l.width * u * .5f, join = androidx.compose.ui.graphics.StrokeJoin.Round)) }
}
private fun DrawScope.line(u: Float, col: Color, w: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
    val l = lineMode
    drawLine(l?.ink ?: col, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), (if (l != null) minOf(w, l.width * .5f).coerceAtLeast(l.width * .4f) else w) * u, StrokeCap.Round)
}
/** A stroked curve (a tail): thick colour, or a thin ink line. */
private fun DrawScope.curve(u: Float, path: Path, col: Color, w: Float) {
    val l = lineMode
    drawPath(path, l?.ink ?: col, style = Stroke((if (l != null) l.width * .55f else w) * u, cap = StrokeCap.Round))
}
private fun DrawScope.rrect(u: Float, x: Float, y: Float, w: Float, h: Float, r: Float, col: Color) {
    val l = lineMode
    val cr = androidx.compose.ui.geometry.CornerRadius(r * u)
    if (l == null) drawRoundRect(col, Offset(x * u, y * u), Size(w * u, h * u), cr)
    else { drawRoundRect(l.paper, Offset(x * u, y * u), Size(w * u, h * u), cr); drawRoundRect(l.ink, Offset(x * u, y * u), Size(w * u, h * u), cr, style = Stroke(l.width * u * .5f)) }
}

/** Eyes: open with a shine, closed as a soft arc (blinking or asleep). */
private fun DrawScope.eye(u: Float, cx: Float, cy: Float, r: Float, closed: Boolean, ink0: Color = c(0xFF2B2B2B)) {
    val ink = lineMode?.ink ?: ink0
    if (closed) {
        val path = Path().apply { moveTo((cx - r) * u, cy * u); quadraticTo(cx * u, (cy + r * .9f) * u, (cx + r) * u, cy * u) }
        drawPath(path, ink, style = Stroke(r * .55f * u, cap = StrokeCap.Round))
    } else {
        drawCircle(ink, r * u, Offset(cx * u, cy * u))
        drawCircle(lineMode?.paper ?: Color.White, r * .35f * u, Offset((cx + r * .35f) * u, (cy - r * .35f) * u))
    }
}
private fun DrawScope.cheek(u: Float, cx: Float, cy: Float, on: Boolean) {
    if (!on) return
    drawOval((lineMode?.ink ?: c(0xFFFF8FA3)).copy(alpha = if (lineMode != null) .25f else .55f), Offset((cx - 3f) * u, (cy - 1.7f) * u), Size(6f * u, 3.4f * u))
}
private fun legSwing(p: Pose, phase: Float) = if (p.walk == 0f) 0f else sin(p.walk * 2f * PI.toFloat() + phase) * 3.2f

private fun DrawScope.hearts(u: Float, t: Float) {
    val y = 14f - (t * 6f % 10f)
    val a = (1f - abs((t * 6f % 10f) / 10f - .5f) * 2f).coerceIn(0f, 1f)
    heart(u, 74f, y + 4f, 4.2f, c(0xFFFF5C7A).copy(alpha = a))
    heart(u, 84f, y + 10f, 3f, c(0xFFFF8FA3).copy(alpha = a * .8f))
}
private fun DrawScope.heart(u: Float, cx: Float, cy: Float, r: Float, col0: Color) {
    val col = lineMode?.ink?.copy(alpha = col0.alpha) ?: col0
    drawCircle(col, r * .6f * u, Offset((cx - r * .5f) * u, cy * u)); drawCircle(col, r * .6f * u, Offset((cx + r * .5f) * u, cy * u))
    drawPath(Path().apply { moveTo((cx - r * 1.05f) * u, (cy + r * .2f) * u); lineTo((cx + r * 1.05f) * u, (cy + r * .2f) * u); lineTo(cx * u, (cy + r * 1.4f) * u); close() }, col)
}
private fun DrawScope.zzz(u: Float, t: Float) {
    val k = (t * .6f) % 1f
    val col = (lineMode?.ink ?: c(0xFF8FA3B8)).copy(alpha = 1f - k)
    val x = 74f + k * 8f; val y = 22f - k * 12f; val s = 5f + k * 3f
    line(u, col, 1.6f, x, y, x + s, y); line(u, col, 1.6f, x + s, y, x, y + s); line(u, col, 1.6f, x, y + s, x + s, y + s)
}

// ------------------------------------------------------------------ the animals (facing right)

private fun DrawScope.cat(u: Float, p: Pose) {
    val fur = c(0xFFF2A65A); val dark = c(0xFFD9822B); val light = c(0xFFFBD9B0)
    // Tail sways.
    val sw = sin(p.t * 1.6f) * 7f
    val tail = Path().apply { moveTo(30 * u, 74 * u); cubicTo(14 * u, (70 + sw * .3f) * u, (10 + sw * .2f) * u, 52 * u, (18 + sw) * u, 42 * u) }
    curve(u, tail, fur, 6.5f)
    // Legs.
    for ((x, ph) in listOf(36f to 0f, 44f to PI.toFloat(), 58f to PI.toFloat(), 64f to 0f)) {
        val d = legSwing(p, ph)
        rrect(u, x - 3 + d, 80f, 6.5f, 13f, 3f, if (x < 50) dark else fur)
    }
    oval(u, 50f, 72f, 46f, 26f, fur)
    oval(u, 54f, 76f, 26f, 14f, light)
    // Stripes.
    line(u, dark, 2.4f, 40f, 62f, 42f, 70f); line(u, dark, 2.4f, 48f, 60f, 49f, 68f)
    // Head.
    dot(u, 68f, 50f, 17f, fur)
    poly(u, fur, 55f, 42f, 57f, 25f, 66f, 37f); poly(u, fur, 70f, 36f, 80f, 25f, 82f, 42f)
    poly(u, c(0xFFFFB3C1), 57.5f, 39f, 58.5f, 30f, 63.5f, 37f); poly(u, c(0xFFFFB3C1), 72f, 36f, 78.5f, 30f, 79.5f, 39f)
    oval(u, 72f, 56f, 16f, 10f, light)
    eye(u, 63f, 48f, 2.6f, p.blink || p.sleep); eye(u, 75f, 48f, 2.6f, p.blink || p.sleep)
    poly(u, c(0xFFFF8FA3), 68f, 53f, 72f, 53f, 70f, 55.5f)
    line(u, c(0xFF8A5A2B), .8f, 75f, 56f, 86f, 54f); line(u, c(0xFF8A5A2B), .8f, 75f, 58f, 86f, 59f)
    cheek(u, 61f, 55f, p.happy); cheek(u, 79f, 55f, p.happy)
}

private fun DrawScope.penguin(u: Float, p: Pose) {
    val black = c(0xFF263238); val white = c(0xFFF5F7F8); val orange = c(0xFFFFA726)
    // Feet waddle.
    val w = legSwing(p, 0f)
    oval(u, 43f + w, 92f, 12f, 5f, orange); oval(u, 58f - w, 92f, 12f, 5f, orange)
    // Flippers.
    val f = -20f - p.flap * 45f - (if (p.walk != 0f) sin(p.walk * 6.28f) * 8f else 0f)
    oval(u, 50f, 60f, 44f, 64f, black)
    rotate(f, Offset(31 * u, 52 * u)) { oval(u, 29f, 64f, 9f, 26f, black) }
    rotate(-f, Offset(69 * u, 52 * u)) { oval(u, 71f, 64f, 9f, 26f, black) }
    oval(u, 51f, 66f, 30f, 46f, white)
    // Face.
    oval(u, 51f, 42f, 26f, 18f, white)
    eye(u, 45f, 41f, 2.4f, p.blink || p.sleep); eye(u, 57f, 41f, 2.4f, p.blink || p.sleep)
    poly(u, orange, 47f, 46f, 55f, 46f, 51f, 51.5f)
    cheek(u, 41f, 47f, p.happy); cheek(u, 61f, 47f, p.happy)
}

private fun DrawScope.albatross(u: Float, p: Pose) {
    val white = c(0xFFFAFAFA); val wing = c(0xFF4A4F57); val beak = c(0xFFF2C46D); val leg = c(0xFFE6B8A8)
    val w = legSwing(p, 0f)
    line(u, leg, 2.2f, 44f + w, 76f, 43f + w, 92f); line(u, leg, 2.2f, 54f - w, 76f, 55f - w, 92f)
    oval(u, 42f + w, 93f, 9f, 3f, leg); oval(u, 56f - w, 93f, 9f, 3f, leg)
    // Tail.
    poly(u, wing, 22f, 66f, 30f, 62f, 32f, 72f)
    oval(u, 48f, 66f, 50f, 26f, white)
    // Long wing: folded, lifts when flapping.
    rotate(-8f - p.flap * 38f, Offset(60 * u, 62 * u)) { oval(u, 42f, 62f, 50f, 13f, wing); oval(u, 46f, 60f, 30f, 7f, c(0xFF6B717A)) }
    // Neck and head.
    oval(u, 66f, 54f, 14f, 20f, white)
    dot(u, 71f, 42f, 11f, white)
    line(u, c(0xFF9AA0A6), 1.6f, 67f, 37.5f, 74f, 37f)
    eye(u, 72f, 40f, 2f, p.blink || p.sleep)
    poly(u, beak, 79f, 40f, 97f, 43f, 96f, 46f, 79f, 46f)
    dot(u, 96.5f, 45f, 1.3f, c(0xFFD9A441))
    cheek(u, 70f, 46f, p.happy)
}

private fun DrawScope.swallow(u: Float, p: Pose) {
    val navy = c(0xFF1E3A6E); val red = c(0xFFE2603B); val cream = c(0xFFF7EBDD)
    // Short legs on the ground.
    line(u, c(0xFF3B3B3B), 1.4f, 50f, 72f, 49f, 92f); line(u, c(0xFF3B3B3B), 1.4f, 56f, 72f, 57f, 92f)
    // Forked tail.
    poly(u, navy, 34f, 64f, 8f, 54f, 26f, 66f, 6f, 76f, 34f, 70f)
    oval(u, 48f, 64f, 36f, 20f, navy)
    oval(u, 52f, 69f, 26f, 11f, cream)
    // Wing.
    rotate(-6f - p.flap * 50f, Offset(54 * u, 60 * u)) { poly(u, c(0xFF15305C), 60f, 58f, 22f, 60f, 30f, 66f, 58f, 66f) }
    dot(u, 64f, 54f, 10f, navy)
    dot(u, 68f, 60f, 5.5f, red)
    eye(u, 67f, 51f, 2f, p.blink || p.sleep)
    poly(u, c(0xFF2B2B2B), 73f, 53f, 80f, 55f, 73f, 57f)
    cheek(u, 62f, 58f, p.happy)
}

private fun DrawScope.rabbit(u: Float, p: Pose) {
    val fur = c(0xFFEDE3D6); val shade = c(0xFFD9CBB8); val pink = c(0xFFFFB3C1)
    dot(u, 27f, 72f, 7f, Color.White)
    val w = legSwing(p, 0f)
    oval(u, 40f + w, 89f, 16f, 8f, shade); oval(u, 58f - w, 90f, 12f, 7f, fur)
    oval(u, 46f, 74f, 40f, 28f, fur)
    // Ears sway.
    val e = sin(p.t * 1.3f) * 4f
    rotate(-8f + e, Offset(62 * u, 46 * u)) { oval(u, 60f, 28f, 9f, 30f, fur); oval(u, 60f, 29f, 4.5f, 22f, pink) }
    rotate(10f + e * .6f, Offset(70 * u, 46 * u)) { oval(u, 72f, 28f, 9f, 30f, fur); oval(u, 72f, 29f, 4.5f, 22f, pink) }
    dot(u, 66f, 56f, 15f, fur)
    eye(u, 70f, 53f, 2.6f, p.blink || p.sleep)
    eye(u, 60f, 53f, 2.2f, p.blink || p.sleep)
    poly(u, pink, 76f, 58f, 80f, 58f, 78f, 60.5f)
    line(u, c(0xFFB09A86), .7f, 79f, 61f, 88f, 60f); line(u, c(0xFFB09A86), .7f, 79f, 62f, 88f, 64f)
    cheek(u, 72f, 61f, p.happy)
}

private fun DrawScope.sheep(u: Float, p: Pose) {
    val wool = c(0xFFF4F1EA); val face = c(0xFF3A3A3A); val woolShade = c(0xFFE3DED2)
    val w = legSwing(p, 0f)
    for ((x, ph) in listOf(36f to 0f, 44f to 3.14f, 58f to 3.14f, 64f to 0f)) line(u, face, 4f, x + legSwing(p, ph), 76f, x + legSwing(p, ph), 92f)
    for ((x, y, r) in listOf(Triple(34f, 66f, 12f), Triple(46f, 58f, 13f), Triple(58f, 60f, 12f), Triple(42f, 72f, 12f), Triple(56f, 72f, 12f), Triple(66f, 66f, 10f))) dot(u, x, y + 1.5f, r, woolShade)
    for ((x, y, r) in listOf(Triple(34f, 64f, 12f), Triple(46f, 56f, 13f), Triple(58f, 58f, 12f), Triple(42f, 70f, 12f), Triple(56f, 70f, 12f), Triple(66f, 64f, 10f))) dot(u, x, y, r, wool)
    @Suppress("UNUSED_VARIABLE") val unused = w
    // Head with a woolly cap.
    oval(u, 76f, 58f, 17f, 22f, face)
    oval(u, 67f, 54f, 8f, 5f, face); oval(u, 86f, 54f, 8f, 5f, face)
    dot(u, 75f, 46f, 7f, wool); dot(u, 80f, 47f, 6f, wool)
    eye(u, 73f, 56f, 2.2f, p.blink || p.sleep, Color.White); eye(u, 80f, 56f, 2.2f, p.blink || p.sleep, Color.White)
    oval(u, 76.5f, 64f, 6f, 3f, c(0xFF555555))
    cheek(u, 70f, 61f, p.happy); cheek(u, 83f, 61f, p.happy)
}

private fun DrawScope.chicken(u: Float, p: Pose) {
    val body = c(0xFFFFFBF2); val shade = c(0xFFEFE3CC); val red = c(0xFFE53935); val yel = c(0xFFFFB300)
    val w = legSwing(p, 0f)
    line(u, yel, 2f, 46f + w, 78f, 45f + w, 92f); line(u, yel, 2f, 56f - w, 78f, 57f - w, 92f)
    line(u, yel, 1.6f, 41f + w, 92f, 49f + w, 92f); line(u, yel, 1.6f, 53f - w, 92f, 61f - w, 92f)
    // Tail feathers.
    poly(u, shade, 24f, 50f, 34f, 62f, 22f, 64f); poly(u, body, 28f, 46f, 38f, 60f, 26f, 58f)
    oval(u, 48f, 66f, 42f, 30f, body)
    rotate(-p.flap * 35f, Offset(52 * u, 62 * u)) { oval(u, 46f, 66f, 22f, 13f, shade) }
    // Head pecks now and then.
    val peck = if (p.walk == 0f && (p.t % 6f) in 4.6f..5.1f) 6f else 0f
    translate(0f, peck * u) {
        dot(u, 66f, 46f, 11f, body)
        dot(u, 61f, 34.5f, 3.2f, red); dot(u, 66f, 33f, 3.6f, red); dot(u, 71f, 35f, 3.2f, red)
        poly(u, yel, 75f, 45f, 83f, 48f, 75f, 51f)
        oval(u, 74f, 54f, 3.6f, 6f, red)
        eye(u, 69f, 44f, 2.1f, p.blink || p.sleep)
        cheek(u, 64f, 50f, p.happy)
    }
}
