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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** The seven friends, drawn as soft continuous silhouettes (curves through points, no corners). */
enum class Species(val ar: String, val about: String) {
    CAT("قطة", "هادئة تحب الجلوس قربك"),
    PENGUIN("بطريق", "يتمايل ويرفرف بجناحيه الصغيرين"),
    ALBATROSS("طائر القطرس", "طائر البحر ذو الجناحين الطويلين"),
    SWALLOW("سنونو", "صغير خفيف بذيل مشقوق"),
    RABBIT("أرنب", "يقفز ويحرّك أذنيه"),
    SHEEP("غنمة", "صوفها كالغيمة"),
    CHICKEN("دجاجة", "تنقر الأرض بلطف"),
    COW("بقرة", "هادئة تمضغ العشب"),
    ELEPHANT("فيل", "يحرّك خرطومه وأذنيه الكبيرتين"),
    GIRAFFE("زرافة", "عنقها طويل تطلّ من فوق"),
    BIRD("عصفور", "يغرّد ويرفرف بجناحيه"),
}

/** How the friend stands right now. Everything is continuous, so motion can be smooth. */
data class Pose(
    val t: Float = 0f,          // seconds: breathing, idle sway
    val blink: Boolean = false,
    val walk: Float = 0f,       // walking phase in cycles (legs, bob)
    val speed: Float = 0f,      // 0..1 how fast it moves now (stride size)
    val hop: Float = 0f,        // 0..1 height above ground
    val flap: Float = 0f,       // 0..1 wings up
    val sleep: Boolean = false,
    val happy: Boolean = false,
    val left: Boolean = false,  // facing left (used when [face] is not given)
    val tilt: Float = 0f,       // degrees, rocking around the feet
    val squash: Float = 0f,     // -1..1: >0 squashed (landing), <0 stretched (jumping)
    val face: Float = Float.NaN, // -1 (left) .. 1 (right): turning passes smoothly through the middle
    val lean: Float = 0f,       // degrees, leaning into the walk
    val paw: Float = 0f,        // 0..1 front paw raised (batting the ball)
    val look: Float = 0f,       // -1..1 head turned down/up a little
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

@Composable
fun PetSprite(species: Species, pose: () -> Pose, modifier: Modifier, line: PetLine? = null) {
    Canvas(modifier) { drawPet(species, pose(), line) }
}

private var lineMode: PetLine? = null
private fun c(hex: Long) = Color(hex)
private const val TAU = (PI * 2).toFloat()

fun DrawScope.drawPet(s: Species, p: Pose, line: PetLine? = null) {
    lineMode = line
    val u = size.minDimension / 100f
    val face = if (p.face.isNaN()) (if (p.left) -1f else 1f) else p.face
    val breathe = sin(p.t * 2.1f)
    val stride = p.speed.coerceIn(0f, 1f)
    val bob = if (s == Species.RABBIT) abs(sin(p.walk * PI.toFloat())) * 10f * stride else abs(sin(p.walk * TAU)) * 1.8f * stride
    val lift = p.hop * 16f + bob
    // A soft shadow that shrinks in the air.
    val sh = 1f - (lift / 16f).coerceIn(0f, 1f) * .45f
    drawOval((line?.ink ?: Color.Black).copy(alpha = if (line != null) .14f else .12f * sh), Offset((50 - 21 * sh) * u, 93f * u), Size(42 * sh * u, 4.6f * u))
    // Turning: the width passes through a narrow middle (like a body turning), never a hard flip.
    val turnW = (abs(face) * .85f + .15f).coerceIn(.15f, 1f) * (if (face < 0) -1f else 1f)
    scale(turnW, 1f, Offset(50 * u, 50 * u)) {
        // Squash and stretch from the feet, breathing in the chest.
        val sq = p.squash.coerceIn(-1f, 1f)
        scale(1f + sq * .10f, 1f - sq * .14f + breathe * .012f, Offset(50 * u, 93 * u)) {
            val wad = if (s == Species.PENGUIN) sin(p.walk * TAU) * 8f * stride else 0f
            rotate(p.tilt + wad + p.lean, Offset(50 * u, 92 * u)) {
                translate(0f, -lift * u) {
                    when (s) {
                        Species.CAT -> cat(u, p, stride)
                        Species.PENGUIN -> penguin(u, p, stride)
                        Species.ALBATROSS -> albatross(u, p, stride)
                        Species.SWALLOW -> swallow(u, p, stride)
                        Species.RABBIT -> rabbit(u, p, stride)
                        Species.SHEEP -> sheep(u, p, stride)
                        Species.CHICKEN -> chicken(u, p, stride)
                        // These three are drawn only by their Lottie looks.
                        Species.COW, Species.ELEPHANT, Species.GIRAFFE -> sheep(u, p, stride)
                        Species.BIRD -> swallow(u, p, stride)
                    }
                }
            }
        }
    }
    if (p.happy && !p.sleep) hearts(u, p.t)
    if (p.sleep) zzz(u, p.t)
    lineMode = null
}

// ------------------------------------------------------------------ smooth shapes

/** A closed curve through the points (Catmull-Rom → cubic Bézier): always round, never a corner. */
private fun blob(u: Float, vararg xy: Float): Path {
    val n = xy.size / 2
    fun px(i: Int) = xy[((i % n + n) % n) * 2] * u
    fun py(i: Int) = xy[((i % n + n) % n) * 2 + 1] * u
    return Path().apply {
        moveTo(px(0), py(0))
        for (i in 0 until n) {
            val c1x = px(i) + (px(i + 1) - px(i - 1)) / 6f; val c1y = py(i) + (py(i + 1) - py(i - 1)) / 6f
            val c2x = px(i + 1) - (px(i + 2) - px(i)) / 6f; val c2y = py(i + 1) - (py(i + 2) - py(i)) / 6f
            cubicTo(c1x, c1y, c2x, c2y, px(i + 1), py(i + 1))
        }
        close()
    }
}
/** An open smooth curve through the points (tails, whiskers, mouths). */
private fun curve(u: Float, vararg xy: Float): Path {
    val n = xy.size / 2
    fun px(i: Int) = xy[i.coerceIn(0, n - 1) * 2] * u
    fun py(i: Int) = xy[i.coerceIn(0, n - 1) * 2 + 1] * u
    return Path().apply {
        moveTo(px(0), py(0))
        for (i in 0 until n - 1) {
            cubicTo(px(i) + (px(i + 1) - px(i - 1)) / 6f, py(i) + (py(i + 1) - py(i - 1)) / 6f,
                px(i + 1) - (px(i + 2) - px(i)) / 6f, py(i + 1) - (py(i + 2) - py(i)) / 6f, px(i + 1), py(i + 1))
        }
    }
}
/** A wavy cloud (wool): an ellipse whose edge rises and falls softly. */
private fun cloud(u: Float, cx: Float, cy: Float, rx: Float, ry: Float, bumps: Int, depth: Float, phase: Float = 0f): Path {
    val pts = FloatArray(bumps * 4)
    for (i in 0 until bumps * 2) {
        val a = i * TAU / (bumps * 2) + phase
        val r = if (i % 2 == 0) 1f + depth else 1f
        pts[i * 2] = cx + cos(a) * rx * r; pts[i * 2 + 1] = cy + sin(a) * ry * r
    }
    return blob(u, *pts)
}

/** Fill (colorful) or paper with an ink outline (line drawing). */
private fun DrawScope.shape(path: Path, col: Color, u: Float) {
    val l = lineMode
    if (l == null) drawPath(path, col)
    else { drawPath(path, l.paper); drawPath(path, l.ink, style = Stroke(l.width * u * .55f, join = StrokeJoin.Round, cap = StrokeCap.Round)) }
}
/** Colorful-only detail (a belly patch, inner ears): skipped in line drawing so the lines stay few and clean. */
private fun DrawScope.tint(path: Path, col: Color) { if (lineMode == null) drawPath(path, col) }
/** A soft stroke: tails and whiskers. */
private fun DrawScope.stroke(path: Path, col: Color, w: Float, u: Float) {
    val l = lineMode
    drawPath(path, l?.ink ?: col, style = Stroke((if (l != null) l.width * .55f else w) * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
/** A thick soft band (a tail feather): the colour, or an outlined band in line drawing. */
private fun DrawScope.band(path: Path, col: Color, w: Float, u: Float) {
    val l = lineMode
    if (l == null) drawPath(path, col, style = Stroke(w * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    else {
        drawPath(path, l.ink, style = Stroke(w * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, l.paper, style = Stroke((w - l.width * 1.1f).coerceAtLeast(.5f) * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
/** A rounded limb hanging from [x],[y], swung by [deg]. */
private fun DrawScope.limb(u: Float, x: Float, y: Float, len: Float, w: Float, deg: Float, col: Color) {
    rotate(deg, Offset(x * u, y * u)) {
        val r = w / 2
        shape(blob(u, x - r, y, x - r * .9f, y + len * .55f, x - r * .7f, y + len, x, y + len + r * .5f, x + r * .9f, y + len, x + r, y + len * .5f, x + r, y), col, u)
    }
}

private fun DrawScope.eye(u: Float, cx: Float, cy: Float, r: Float, closed: Boolean, ink0: Color = c(0xFF2B2B2B)) {
    val ink = lineMode?.ink ?: ink0
    if (closed) drawPath(curve(u, cx - r * 1.1f, cy, cx, cy + r * .8f, cx + r * 1.1f, cy), ink, style = Stroke(r * .55f * u, cap = StrokeCap.Round))
    else {
        drawOval(ink, Offset((cx - r * .85f) * u, (cy - r) * u), Size(r * 1.7f * u, r * 2f * u))
        drawCircle(lineMode?.paper ?: Color.White, r * .32f * u, Offset((cx + r * .3f) * u, (cy - r * .4f) * u))
    }
}
private fun DrawScope.cheek(u: Float, cx: Float, cy: Float, on: Boolean) {
    if (!on) return
    drawOval((lineMode?.ink ?: c(0xFFFF8FA3)).copy(alpha = if (lineMode != null) .22f else .5f), Offset((cx - 3f) * u, (cy - 1.6f) * u), Size(6f * u, 3.2f * u))
}
private fun DrawScope.hearts(u: Float, t: Float) {
    val k = (t * .5f) % 1f
    heart(u, 76f + sin(t * 3f) * 1.5f, 20f - k * 12f, 4f, (lineMode?.ink ?: c(0xFFFF5C7A)).copy(alpha = sin(k * PI.toFloat()).coerceIn(0f, 1f)))
    val k2 = (t * .5f + .5f) % 1f
    heart(u, 86f + sin(t * 2.6f) * 1.2f, 26f - k2 * 10f, 2.8f, (lineMode?.ink ?: c(0xFFFF8FA3)).copy(alpha = (sin(k2 * PI.toFloat()) * .8f).coerceIn(0f, 1f)))
}
private fun DrawScope.heart(u: Float, cx: Float, cy: Float, r: Float, col: Color) {
    drawPath(blob(u, cx, cy - r * .35f, cx + r * .55f, cy - r * .95f, cx + r * 1.05f, cy - r * .35f, cx + r * .6f, cy + r * .5f, cx, cy + r * 1.1f, cx - r * .6f, cy + r * .5f, cx - r * 1.05f, cy - r * .35f, cx - r * .55f, cy - r * .95f), col)
}
private fun DrawScope.zzz(u: Float, t: Float) {
    val k = (t * .4f) % 1f
    val col = (lineMode?.ink ?: c(0xFF8FA3B8)).copy(alpha = sin(k * PI.toFloat()).coerceIn(0f, 1f))
    val x = 72f + k * 10f + sin(t * 2f) * 1.5f; val y = 24f - k * 14f; val s = 4.5f + k * 3f
    drawPath(curve(u, x, y, x + s, y + .2f, x + .3f, y + s, x + s, y + s), col, style = Stroke(1.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** A yarn ball (the friend's toy): rolls with its own spin. */
fun DrawScope.drawYarn(cx: Float, cy: Float, r: Float, spin: Float, line: PetLine?) {
    val ink = line?.ink ?: c(0xFFE57373)
    val fill = line?.paper ?: c(0xFFF4A7A7)
    drawCircle(fill, r, Offset(cx, cy))
    rotate(spin * 57.3f, Offset(cx, cy)) {
        for (k in -1..1) drawArc(ink.copy(alpha = if (line != null) 1f else .85f), 200f + k * 25f, 140f, false,
            Offset(cx - r * (.75f - k * .18f), cy - r * .8f), Size(r * (1.5f - k * .36f), r * 1.6f), style = Stroke(r * .12f, cap = StrokeCap.Round))
    }
    drawCircle(ink, r, Offset(cx, cy), style = Stroke(r * .14f))
}

// ------------------------------------------------------------------ the animals (facing right; units of 1/100)

private fun DrawScope.cat(u: Float, p: Pose, k: Float) {
    val fur = c(0xFFF2A65A); val dark = c(0xFFDB8A3C); val light = c(0xFFFCE0BC)
    val swing = { ph: Float -> sin((p.walk + ph) * TAU) * 22f * k }
    // Tail: a soft S that sways, and trails behind when walking.
    val sw = sin(p.t * 1.3f) * 5f - p.lean * .6f
    stroke(curve(u, 30f, 72f, 21f, 70f + sw * .2f, 15f + sw * .3f, 60f, 17f + sw, 49f, 23f + sw * 1.2f, 45f), fur, 6.5f, u)
    limb(u, 38f, 76f, 14f, 6.5f, swing(.5f), dark); limb(u, 62f, 76f, 14f, 6.5f, swing(0f), dark)
    shape(blob(u, 29f, 72f, 33f, 63f, 44f, 59f, 58f, 59f, 68f, 63f, 71f, 72f, 66f, 80f, 52f, 83f, 38f, 82f, 30f, 78f), fur, u)
    tint(blob(u, 42f, 76f, 52f, 71f, 63f, 73f, 60f, 80f, 48f, 81f), light)
    limb(u, 34f, 77f, 14f, 6.8f, swing(0f), fur)
    limb(u, 66f, 77f, 14f, 6.8f, swing(.5f) - p.paw * 70f, fur)
    rotate(p.look * 10f, Offset(66f * u, 60f * u)) {
        val ear = sin(p.t * .9f) * 1.2f
        shape(blob(u, 56f, 56f, 55.5f, 46f, 57f + ear * .3f, 33f + ear, 63f, 40f, 70f, 38.5f, 77f, 40f, 82.5f - ear * .3f, 33f - ear * .5f, 84f, 46f, 84f, 55f, 78f, 62f, 70f, 64.5f, 61f, 62f), fur, u)
        tint(blob(u, 58.2f, 41f, 58.8f, 36.5f, 62f, 40.5f), c(0xFFFFB3C1)); tint(blob(u, 77.5f, 40.5f, 81f, 36f, 81.5f, 41.5f), c(0xFFFFB3C1))
        tint(blob(u, 63f, 57f, 70f, 54f, 78f, 57f, 76f, 62.5f, 70f, 63.5f, 64f, 62f), light)
        eye(u, 65f, 50f, 2.5f, p.blink || p.sleep); eye(u, 75.5f, 50f, 2.5f, p.blink || p.sleep)
        shape(blob(u, 69f, 55f, 72.5f, 55f, 70.8f, 57.4f), c(0xFFFF8FA3), u)
        stroke(curve(u, 68.5f, 59f, 70.8f, 60.3f, 73f, 59f), c(0xFF8A5A2B), .9f, u)
        stroke(curve(u, 76f, 57.5f, 82f, 56f, 88f, 56.5f), c(0xFFB0773D), .6f, u)
        stroke(curve(u, 76f, 59.5f, 82f, 60f, 87.5f, 61.5f), c(0xFFB0773D), .6f, u)
        cheek(u, 63f, 56.5f, p.happy); cheek(u, 79f, 56.5f, p.happy)
    }
}

private fun DrawScope.penguin(u: Float, p: Pose, k: Float) {
    val black = c(0xFF2B3A42); val white = c(0xFFF7F8F8); val orange = c(0xFFFFA94D)
    val step = sin(p.walk * TAU) * 3f * k
    shape(blob(u, 38f + step, 90f, 44f + step, 88f, 50f + step, 90.5f, 44f + step, 92.5f), orange, u)
    shape(blob(u, 52f - step, 90f, 58f - step, 88f, 64f - step, 90.5f, 58f - step, 92.5f), orange, u)
    shape(blob(u, 50f, 25f, 62f, 29f, 70f, 42f, 72.5f, 60f, 69f, 78f, 59f, 89f, 41f, 89f, 31f, 78f, 27.5f, 60f, 30f, 42f, 38f, 29f), black, u)
    val f = 14f + p.flap * 50f + sin(p.walk * TAU) * 6f * k + sin(p.t * 1.7f) * 2f
    rotate(f, Offset(30f * u, 50f * u)) { shape(blob(u, 30f, 48f, 26f, 58f, 25f, 70f, 28f, 76f, 31.5f, 66f, 32.5f, 54f), black, u) }
    rotate(-f, Offset(70f * u, 50f * u)) { shape(blob(u, 70f, 48f, 74f, 58f, 75f, 70f, 72f, 76f, 68.5f, 66f, 67.5f, 54f), black, u) }
    shape(blob(u, 50f, 42f, 59f, 47f, 63f, 61f, 60f, 77f, 50f, 84f, 40f, 77f, 37f, 61f, 41f, 47f), white, u)
    rotate(p.look * 8f, Offset(50f * u, 45f * u)) {
        shape(blob(u, 50f, 33.5f, 57f, 31.5f, 62f, 37f, 59f, 44f, 50f, 46f, 41f, 44f, 38f, 37f, 43f, 31.5f), white, u)
        eye(u, 45.5f, 38.5f, 2.3f, p.blink || p.sleep); eye(u, 54.5f, 38.5f, 2.3f, p.blink || p.sleep)
        shape(blob(u, 47f, 43f, 50f, 41.8f, 53f, 43f, 50f, 47.5f), orange, u)
        cheek(u, 42f, 44f, p.happy); cheek(u, 58f, 44f, p.happy)
    }
}

private fun DrawScope.albatross(u: Float, p: Pose, k: Float) {
    val white = c(0xFFFBFBFA); val wing = c(0xFF4E555E); val beak = c(0xFFF4C977); val leg = c(0xFFE8BBAA)
    val swing = { ph: Float -> sin((p.walk + ph) * TAU) * 18f * k }
    limb(u, 46f, 74f, 17f, 3.2f, swing(.5f), leg); limb(u, 54f, 74f, 17f, 3.2f, swing(0f), leg)
    shape(blob(u, 19f, 63f, 30f, 59f, 30f, 66f), wing, u)
    shape(blob(u, 24f, 64f, 34f, 57f, 52f, 56f, 64f, 59f, 70f, 66f, 64f, 75f, 46f, 78f, 31f, 73f), white, u)
    rotate(-p.flap * 48f - sin(p.t * 1.1f) * 2f, Offset(58f * u, 60f * u)) {
        shape(blob(u, 26f, 63f, 40f, 57f, 58f, 56.5f, 66f, 59f, 60f, 64f, 44f, 67f, 31f, 67f), wing, u)
    }
    rotate(p.look * 10f, Offset(66f * u, 58f * u)) {
        shape(blob(u, 62f, 61f, 63f, 50f, 67f, 41f, 75f, 37.5f, 82f, 41f, 82.5f, 48f, 77f, 53f, 70f, 60f), white, u)
        stroke(curve(u, 71f, 40.5f, 75f, 39f, 79.5f, 41f), c(0xFF9AA0A6), 1.2f, u)
        eye(u, 76f, 43.5f, 1.9f, p.blink || p.sleep)
        shape(blob(u, 81f, 42.5f, 89f, 43f, 96f, 45f, 97f, 48f, 90f, 48.5f, 81.5f, 48.5f), beak, u)
        cheek(u, 74f, 49f, p.happy)
    }
}

private fun DrawScope.swallow(u: Float, p: Pose, k: Float) {
    val navy = c(0xFF22407A); val red = c(0xFFE56B45); val cream = c(0xFFF8EEDF)
    val swing = { ph: Float -> sin((p.walk + ph) * TAU) * 15f * k }
    limb(u, 50f, 70f, 21f, 2.4f, swing(.5f), c(0xFF4A4A4A)); limb(u, 56f, 70f, 21f, 2.4f, swing(0f), c(0xFF4A4A4A))
    val tw = sin(p.t * 1.5f) * 2f
    band(curve(u, 33f, 65f, 21f, 61f + tw * .3f, 7f, 54f + tw), navy, 3.6f, u)
    band(curve(u, 33f, 67f, 22f, 70f - tw * .3f, 8f, 76f - tw), navy, 3.6f, u)
    shape(blob(u, 28f, 65f, 40f, 58f, 54f, 55f, 60f, 47f, 68f, 43.5f, 75f, 47f, 75.5f, 55f, 70f, 62f, 58f, 70f, 43f, 72f, 31f, 69f), navy, u)
    tint(blob(u, 46f, 66f, 58f, 63f, 68f, 61f, 62f, 68.5f, 50f, 70.5f), cream)
    tint(blob(u, 66f, 56f, 72f, 54.5f, 73f, 59f, 68.5f, 61.5f), red)
    rotate(-p.flap * 55f - sin(p.t * 1.2f) * 2f, Offset(58f * u, 57f * u)) {
        shape(blob(u, 62f, 56f, 48f, 56.5f, 30f, 59.5f, 20f, 62f, 34f, 64f, 52f, 63f), c(0xFF1A3466), u)
    }
    eye(u, 68.5f, 49.5f, 1.9f, p.blink || p.sleep)
    shape(blob(u, 75f, 50f, 80f, 51.5f, 75.5f, 53f), c(0xFF2B2B2B), u)
    cheek(u, 66f, 55f, p.happy)
}

private fun DrawScope.rabbit(u: Float, p: Pose, k: Float) {
    val fur = c(0xFFEEE5D8); val shade = c(0xFFDCCDB9); val pink = c(0xFFFFB8C6)
    val kick = sin(p.walk * PI.toFloat()) * 30f * k
    shape(blob(u, 22f, 72f, 26f, 66f, 32f, 68f, 31f, 75f, 25f, 77f), Color.White, u)
    rotate(-kick, Offset(38f * u, 82f * u)) { shape(blob(u, 30f, 86f, 38f, 82f, 50f, 85f, 52f, 90f, 38f, 91f), shade, u) }
    shape(blob(u, 28f, 76f, 33f, 65f, 46f, 60f, 60f, 63f, 66f, 72f, 61f, 82f, 46f, 86f, 33f, 84f), fur, u)
    rotate(kick * .5f, Offset(58f * u, 80f * u)) { shape(blob(u, 54f, 82f, 60f, 80f, 64f, 88f, 57f, 90f), fur, u) }
    rotate(p.look * 10f, Offset(66f * u, 58f * u)) {
        val e = sin(p.t * 1.1f) * 4f - p.lean * .8f + (if (p.hop > .1f) -10f * p.hop else 0f)
        rotate(-10f + e, Offset(62f * u, 47f * u)) {
            shape(blob(u, 59f, 47f, 56.5f, 33f, 58f, 19f, 62f, 15f, 65.5f, 21f, 65.5f, 34f, 64f, 47f), fur, u)
            tint(blob(u, 60.3f, 43f, 59f, 30f, 61.5f, 20f, 63.5f, 30f, 62.6f, 43f), pink)
        }
        rotate(8f + e * .7f, Offset(70f * u, 47f * u)) {
            shape(blob(u, 67f, 47f, 66f, 33f, 69f, 19f, 73f, 16f, 76f, 22f, 74.5f, 35f, 72.5f, 47f), fur, u)
            tint(blob(u, 68.8f, 43f, 69f, 30f, 72f, 21f, 73.4f, 31f, 71.2f, 43f), pink)
        }
        shape(blob(u, 54f, 58f, 57f, 49f, 66f, 44.5f, 76f, 47f, 80f, 55f, 77f, 63f, 68f, 66.5f, 59f, 64f), fur, u)
        eye(u, 70f, 53.5f, 2.5f, p.blink || p.sleep); eye(u, 61.5f, 54f, 2.1f, p.blink || p.sleep)
        shape(blob(u, 76.5f, 57.5f, 79.5f, 57.5f, 78f, 59.6f), pink, u)
        stroke(curve(u, 79f, 60f, 84.5f, 59.5f, 89f, 60.5f), c(0xFFB29C88), .6f, u)
        stroke(curve(u, 79f, 61.5f, 84f, 62.5f, 88.5f, 64.5f), c(0xFFB29C88), .6f, u)
        cheek(u, 72.5f, 61f, p.happy)
    }
}

private fun DrawScope.sheep(u: Float, p: Pose, k: Float) {
    val wool = c(0xFFF6F3EC); val woolShade = c(0xFFE6E0D3); val face = c(0xFF3D3B3A)
    val swing = { ph: Float -> sin((p.walk + ph) * TAU) * 20f * k }
    limb(u, 37f, 72f, 20f, 4.2f, swing(.5f), face); limb(u, 61f, 72f, 20f, 4.2f, swing(0f), face)
    if (lineMode == null) drawPath(cloud(u, 48f, 65.5f, 22f, 14f, 7, .13f, .3f), woolShade)
    shape(cloud(u, 48f, 64f, 22f, 14f, 7, .13f, sin(p.t * .7f) * .04f), wool, u)
    limb(u, 41f, 74f, 18f, 4.4f, swing(0f), face); limb(u, 57f, 74f, 18f, 4.4f, swing(.5f), face)
    rotate(p.look * 10f + sin(p.t * .8f) * 2f, Offset(70f * u, 58f * u)) {
        shape(blob(u, 66f, 54f, 64f, 49f, 61f, 47.5f, 63f, 52f), face, u)
        shape(blob(u, 70f, 50f, 76f, 47.5f, 82f, 50f, 84f, 58f, 80f, 66f, 75f, 68.5f, 70f, 66f, 68f, 58f), face, u)
        shape(cloud(u, 76f, 49f, 7.5f, 4.5f, 4, .2f), wool, u)
        eye(u, 74f, 57f, 2f, p.blink || p.sleep, Color.White); eye(u, 80.5f, 57f, 2f, p.blink || p.sleep, Color.White)
        if (lineMode == null) drawOval(c(0xFF5A5654), Offset(74.5f * u, 63f * u), Size(5.5f * u, 2.6f * u))
        cheek(u, 71.5f, 62f, p.happy)
    }
}

private fun DrawScope.chicken(u: Float, p: Pose, k: Float) {
    val body = c(0xFFFFFCF4); val shade = c(0xFFF0E4CC); val red = c(0xFFE84B3C); val yel = c(0xFFFFB627)
    val swing = { ph: Float -> sin((p.walk + ph) * TAU) * 20f * k }
    for ((x, ph) in listOf(45f to .5f, 55f to 0f)) {
        rotate(swing(ph), Offset(x * u, 78f * u)) {
            stroke(curve(u, x, 78f, x - .5f, 86f, x, 92f), yel, 2.2f, u)
            stroke(curve(u, x - 4.5f, 92.5f, x, 91.8f, x + 4.5f, 92.5f), yel, 1.8f, u)
        }
    }
    val sway = sin(p.t * 1.2f) * 2f
    shape(blob(u, 33f, 58f, 24f + sway, 47f, 22f + sway, 40f, 28f + sway * .6f, 43f, 36f, 52f), shade, u)
    shape(blob(u, 28f, 63f, 37f, 55f, 52f, 53f, 64f, 56f, 69f, 66f, 63f, 77f, 48f, 81f, 35f, 77f), body, u)
    rotate(-p.flap * 40f, Offset(52f * u, 62f * u)) { shape(blob(u, 38f, 64f, 47f, 59f, 58f, 61f, 54f, 69f, 43f, 71f), shade, u) }
    // Pecks now and then: the head dips smoothly.
    val peck = if (p.speed < .05f) { val ph = (p.t % 5f) / 5f; if (ph > .82f) sin((ph - .82f) / .18f * PI.toFloat()) * 9f else 0f } else 0f
    rotate(p.look * 10f + peck * 2.5f, Offset(62f * u, 56f * u)) {
        shape(blob(u, 60f, 49f, 61f, 36f, 64f, 33.5f, 67f, 31.5f, 70.5f, 33f, 73.5f, 35f, 75f, 41f, 76f, 47f, 71f, 55f, 64f, 56f), red, u)
        shape(blob(u, 59f, 48f, 61f, 40f, 67f, 36.5f, 74f, 39f, 76f, 46f, 72f, 53f, 64f, 55f), body, u)
        shape(blob(u, 75.5f, 44f, 82f, 46.3f, 75.5f, 48.8f), yel, u)
        shape(blob(u, 72f, 50f, 75.5f, 50f, 75f, 56.5f, 72.2f, 56f), red, u)
        eye(u, 69.5f, 43.5f, 2f, p.blink || p.sleep)
        cheek(u, 66f, 49.5f, p.happy)
    }
}
