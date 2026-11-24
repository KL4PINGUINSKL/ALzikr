package com.alzikr.app.pets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import org.json.JSONObject

/**
 * Painted friends (ALzikr Pet Studio originals): one painting per pose, bent live around a few landmarks (head, neck,
 * feet, tail, ear/wing tip) by a smooth local field, the same motion the Studio uses. Only pictures and numbers ship,
 * so a friend costs ~100 KB instead of megabytes of frames, and it stays sharp at any size and frame rate.
 *
 * Model space is 400 × 400, facing right, feet near y = 353.
 */
object PetRig {
    /** [g] = gait: cycle seconds, stride, lift, sway, bob. */
    class Rig(
        val gait: String, val g: FloatArray, val art: String, val sleep: String?, val closed: String?,
        // Landmarks: absent while the Studio has not marked them (then only the whole body moves).
        val eyes: List<FloatArray>, val head: FloatArray?, val neck: FloatArray?, val tail: FloatArray?,
        val near: FloatArray?, val far: FloatArray?, val tip: FloatArray?,
        /** The drawn area (standing ∪ sleeping) in model units; small places frame this, not the whole artboard. */
        val view: FloatArray? = null,
        /** Legendary friends: their animal, the body's bounds, eye size and the sleeping pose's points (for what they wear). */
        val species: String? = null, val bounds: FloatArray? = null, val eyeRadius: Float = 0f, val rest: Marks? = null,
    )
    /** The points of one pose that accessories hang from (the Studio's anchors). */
    class Marks(val eyes: List<FloatArray>, val eyeRadius: Float, val neck: FloatArray?, val near: FloatArray?, val tip: FloatArray?, val tail: FloatArray?, val bounds: FloatArray?)
    class Pose(val controls: List<FloatArray>, val dx: Float, val dy: Float, val angle: Float, val sx: Float, val sy: Float, val blink: Float)

    private val rigs = LruCache<String, Rig>(16)

    private fun pts(a: org.json.JSONArray?) = (a ?: org.json.JSONArray()).let { x -> (0 until x.length()).map { i -> x.getJSONArray(i).let { floatArrayOf(it.getDouble(0).toFloat(), it.getDouble(1).toFloat()) } } }
    private fun box(a: org.json.JSONArray?) = a?.takeIf { it.length() == 4 }?.let { b -> FloatArray(4) { b.getDouble(it).toFloat() } }

    fun parse(text: String): Rig {
        val o = JSONObject(text)
        fun pt(k: String, j: JSONObject = o) = j.optJSONArray(k)?.let { floatArrayOf(it.getDouble(0).toFloat(), it.getDouble(1).toFloat()) }
        val ga = o.getJSONArray("g")
        val rest = o.optJSONObject("rest")?.let { j -> Marks(pts(j.optJSONArray("eyes")), j.optDouble("eyeRadius", 4.0).toFloat(), pt("neck", j), pt("near", j), pt("tip", j), pt("tail", j), box(j.optJSONArray("bounds"))) }
        return Rig(o.optString("gait"), FloatArray(ga.length()) { ga.getDouble(it).toFloat() }, o.getString("art"),
            o.optString("sleep").ifEmpty { null }, o.optString("closed").ifEmpty { null },
            pts(o.optJSONArray("eyes")), pt("head"), pt("neck"), pt("tail"), pt("near"), pt("far"), pt("tip"), box(o.optJSONArray("view")),
            o.optString("species").ifEmpty { null }, box(o.optJSONArray("bounds")), o.optDouble("eyeRadius", 0.0).toFloat(), rest)
    }
    fun load(skinId: String, path: String): Rig? {
        val key = "$skinId/$path"; rigs.get(key)?.let { return it }
        val text = PetPacks.open(skinId, path)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return null
        return runCatching { parse(text) }.getOrNull()?.also { rigs.put(key, it) }
    }

    private const val TAU = (2 * Math.PI).toFloat()

    /** The pose at [t] seconds of [anim] (idle, walk, happy, sleep). Plain math, unit-tested. */
    fun motion(r: Rig, anim: String, t: Float): Pose {
        val (seconds, stride, lift, sway, bob) = r.g.let { listOf(it[0], it[1], it[2], it[3], it[4]) }
        val a = t * TAU / seconds; val s = sin(a); val c = cos(a); val slow = sin(t * TAU / 3.8f)
        val ph = (t % 2f) / 2f; val leap = max(0f, sin(ph * TAU)); val settle = max(0f, -sin(ph * TAU))
        var blink = max(0f, 1f - abs((t + .6f) % 4.6f - 3.85f) / .124f)
        if (anim == "sleep") return Pose(emptyList(), 0f, 0f, 0f, 1f, 1f + .012f * slow, 0f)
        val ctl = ArrayList<FloatArray>(8)
        fun add(p: FloatArray?, rx: Float, ry: Float, x: Float, y: Float) { if (p != null) ctl += floatArrayOf(p[0], p[1], rx, ry, x, y) }
        var dx = 0f; var dy = 0f; var angle = 0f; var sx = 1f; var sy = 1f
        add(r.neck, 80f, 83f, slow * 1.3f, -2.1f * slow)
        add(r.tail, 45f, 65f, sin(t * 2.8f + .9f) * 2.7f, sin(t * 2.8f) * 3f)
        add(r.tip, 42f, 45f, sin(t * 1.4f) * 1.8f, sin(t * 2.6f) * 1.5f)
        when (anim) {
            "walk" -> {
                val hopping = r.gait in setOf("bound", "hop", "perch", "dart")
                angle = s * sway * .55f; dy = -(if (hopping) max(0f, c) else abs(c)) * bob; dx = s * (if (r.gait == "waddle") 4f else 1.5f)
                add(r.near, 33f, 58f, stride * s, -lift * max(0f, c))
                add(r.far, 32f, 56f, -stride * s, -lift * max(0f, -c))
                add(r.head, 68f, 73f, (if (r.gait == "peck") 6f else 2f) * s, (if (r.gait == "stride") 3f else 1.5f) * c)
                add(r.tip, 50f, 49f, sin(a - .7f) * 4f, (if (r.gait in setOf("waddle", "glide", "dart")) 7f else 3f) * s)
                add(r.tail, 48f, 64f, 2f * s, 5f * sin(a - .7f))
                if (hopping) { sy = 1f - .018f * max(0f, -c); sx = 1f + .012f * max(0f, -c) }
            }
            "happy" -> {
                dy = -(if (r.gait == "heavy" || r.gait == "crawl") 7f else 18f) * leap * leap
                sx = 1f + .018f * settle; sy = 1f - .032f * settle; angle = 3.3f * sin(t * TAU)
                add(r.head, 74f, 75f, 2.5f * sin(t * 4f), -3f * leap)
                add(r.tip, 53f, 48f, 7f * sin(t * 8f), -7f * leap); add(r.tail, 53f, 55f, 6f * sin(t * 8f - .4f), -6f * leap)
                add(r.near, 35f, 53f, 4f * leap, -6f * leap); blink = max(blink, .85f * settle * settle * settle)
            }
            else -> { sx = 1f + .0035f * slow; sy = 1f + .009f * slow; add(r.head, 64f, 69f, 1.25f * sin(t * 1.31f), -.9f * slow) }
        }
        return Pose(ctl, dx, dy, angle, sx, sy, blink)
    }

    /** How far the painting moves at (x, y): smooth bumps around each landmark. */
    fun field(x: Float, y: Float, controls: List<FloatArray>, out: FloatArray) {
        var dx = 0f; var dy = 0f
        for (c in controls) {
            val u = (x - c[0]) / c[2]; val v = (y - c[1]) / c[3]; val d = u * u + v * v
            if (d >= 2.25f) continue
            val k = (1f - d / 2.25f).let { it * it * it }; dx += k * c[4]; dy += k * c[5]
        }
        out[0] = dx; out[1] = dy
    }

    private const val MESH = 24
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val verts = FloatArray((MESH + 1) * (MESH + 1) * 2)
    private val tmp = FloatArray(2)

    /**
     * Draws the friend into a [w]×[h] area of [cv] (centered, fitted). [art], [sleepArt], [closedArt] are the decoded
     * pictures. Synchronized: the widget renders frames off the main thread with the same buffers.
     */
    @Synchronized fun draw(cv: Canvas, w: Float, h: Float, r: Rig, anim: String, t: Float, art: Bitmap, sleepArt: Bitmap?, closedArt: Bitmap?, effects: Boolean = true) {
        val pose = motion(r, anim, t)
        // Frame the animal itself (plus room for a hop and a sway), feet on the bottom edge, like a tightly cut animation.
        val v = r.view ?: floatArrayOf(0f, 0f, 400f, 400f)
        val x0 = v[0] - 8f; val x1 = v[2] + 8f; val y0 = v[1] - (if (r.view != null) 20f else 0f); val y1 = maxOf(v[3], 357f) + 4f
        val s = minOf(w / (x1 - x0), h / (y1 - y0))
        cv.save()
        cv.translate((w - (x1 - x0) * s) / 2f - x0 * s, h - y1 * s); cv.scale(s, s)
        cv.translate(200f + pose.dx, 353f + pose.dy); cv.rotate(pose.angle); cv.scale(pose.sx, pose.sy); cv.translate(-200f, -353f)
        paint.alpha = 255
        if (anim == "sleep" && sleepArt != null) {
            cv.drawBitmap(sleepArt, null, RectF(0f, 0f, 400f, 400f), paint)
        } else {
            // A source point p is shown at p + field(p) (the Studio samples the inverse; same result for these small moves).
            var i = 0
            for (j in 0..MESH) for (k in 0..MESH) {
                val x = k * 400f / MESH; val y = j * 400f / MESH
                field(x, y, pose.controls, tmp); verts[i++] = x + tmp[0]; verts[i++] = y + tmp[1]
            }
            // The mesh decides where the picture goes, whatever its pixel size (the closed-eye picture is smaller).
            cv.drawBitmapMesh(art, MESH, MESH, verts, 0, null, 0, paint)
            if (pose.blink > 0.01f && closedArt != null) { paint.alpha = (pose.blink.coerceIn(0f, 1f) * 255).toInt(); cv.drawBitmapMesh(closedArt, MESH, MESH, verts, 0, null, 0, paint); paint.alpha = 255 }
        }
        r.species?.let { sp ->
            val asleep = anim == "sleep" && sleepArt != null
            val m = (if (asleep) r.rest else null) ?: Marks(r.eyes, r.eyeRadius, r.neck, r.near, r.tip, r.tail, r.bounds)
            PetWear.worn(sp).forEach { (item, pic) -> wear(cv, item, pic, m, r.neck, pose, asleep) }
            if (effects) PetWear.effects(sp).forEach { effect(cv, it, m, pose, if (asleep) .25f * t else t) }
        }
        cv.restore()
    }

    // ------------------------------------------------------------------------------------------------ what they wear

    /** Where the moving painting shows the model point ([x], [y]) (the same field that bends the picture). */
    private fun project(x: Float, y: Float, pose: Pose): FloatArray { val o = FloatArray(2); field(x, y, pose.controls, o); o[0] += x; o[1] += y; return o }
    private val wearPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    /**
     * The Studio's anchor for one accessory (originals/render.js attachment() with follow): from the eyes, neck, feet,
     * wing tip and tail, carried by the motion and turned with it. Returns x, y, angle (degrees), scale.
     */
    fun anchor(slot: String, id: String, m: Marks, standNeck: FloatArray?, pose: Pose, asleep: Boolean): FloatArray? {
        if (m.eyes.size < 2) return null
        val lx = m.eyes[0][0]; val ly = m.eyes[0][1]; val rx = m.eyes[1][0]; val ry = m.eyes[1][1]
        val b = m.bounds ?: floatArrayOf(0f, 0f, 400f, 400f); val neck = m.neck ?: standNeck ?: return null; val near = m.near ?: neck
        val bw = b[2] - b[0]; val bh = b[3] - b[1]
        val face = maxOf(m.eyeRadius * 3f, kotlin.math.hypot(rx - lx, ry - ly)); val faceScale = (face / 42f).coerceIn(.44f, 1.48f)
        var x: Float; var y: Float; var scale: Float; val angle: Float
        when (slot) {
            "head" -> { x = lx - face * .22f; y = ly - face * .72f - 5f; scale = maxOf(.83f, faceScale); angle = -8f }
            "neck" -> { x = neck[0]; y = neck[1]; scale = faceScale; angle = -5f }
            "torso" -> { x = (b[0] + b[2]) * .5f + 13f; y = (neck[1] + near[1]) * .5f - 8f; scale = bw / 150f; angle = if (asleep) 5f else -4f }
            "back" -> { x = (b[0] + b[2]) * .5f * .51f + neck[0] * .49f - 8f; y = neck[1] + (near[1] - neck[1]) * .25f; scale = minOf(1.19f, bw / 180f); angle = -11f }
            else -> {
                val p = when (slot) { "wing" -> m.tip; "leg" -> m.near; "tail" -> m.tail; "crest" -> floatArrayOf(lx - 9f, ly - m.eyeRadius * 2f); else -> null } ?: neck
                x = p[0]; y = p[1] - (if (slot == "leg") 9f else 0f); angle = if (slot == "wing") -18f else 0f; scale = (bw / 255f).coerceIn(.68f, 1.25f)
                if (slot == "wing") { val t = m.tip ?: neck; x = .75f * t[0] + .25f * neck[0]; y = .75f * t[1] + .25f * neck[1] }
            }
        }
        if (id == "legend-scale-corlet") { x = neck[0] - bw * .065f; y = neck[1] + bh * (if (asleep) .015f else .11f); scale = bw * (if (asleep) .19f else .23f) / 65f }
        val pt = project(x, y, pose); val along = project(x + 10f, y, pose)
        val twist = Math.toDegrees(kotlin.math.atan2((along[1] - pt[1]).toDouble(), (along[0] - pt[0]).toDouble())).toFloat()
        return floatArrayOf(pt[0], pt[1], angle + twist, scale)
    }

    /** Paints one accessory picture at its anchor (the Studio's world.js paintItem). */
    private fun wear(cv: Canvas, item: PetWear.Item, pic: Bitmap, m: Marks, standNeck: FloatArray?, pose: Pose, asleep: Boolean) {
        val a = anchor(item.slot, item.id, m, standNeck, pose, asleep) ?: return
        var y = a[1]; val rad = Math.toRadians(a[2].toDouble())
        val yAnchor = when (item.slot) { "head" -> .80f; "neck" -> .37f; else -> .5f }
        var width = item.width * a[3]
        if (item.slot == "head") {
            // Room above the forehead for the highest point of the happy hop.
            y = maxOf(36f, y)
            val above = abs(sin(rad)).toFloat() * .5f + abs(cos(rad)).toFloat() * pic.height / pic.width * yAnchor
            width = minOf(width, (y - 26f) / above)
        }
        val height = width * pic.height / pic.width
        cv.save(); cv.translate(a[0], y); cv.rotate(a[2])
        cv.drawBitmap(pic, null, RectF(-width * .5f, -height * yAnchor, width * .5f, height * (1 - yAnchor)), wearPaint)
        cv.restore()
    }

    // ------------------------------------------------------------------------------------------------ effects

    private val fxColor = mapOf("stars" to 0xFFFFDF8B, "crystals" to 0xFFB9E9FC, "flames" to 0xFFFFAC6B, "snow" to 0xFFE3F6FF,
        "gold" to 0xFFE9C76C, "trails" to 0xFFA4B6FC, "wings" to 0xFFFFE7B4)
    private val fxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fxPath = android.graphics.Path()

    private fun star(cv: Canvas, x: Float, y: Float, r: Float) {
        fxPath.reset()
        for (i in 0 until 8) {
            val a = i * Math.PI / 4 - Math.PI / 2; val d = if (i % 2 == 1) r * .26f else r
            val px = x + cos(a).toFloat() * d; val py = y + sin(a).toFloat() * d
            if (i == 0) fxPath.moveTo(px, py) else fxPath.lineTo(px, py)
        }
        fxPath.close(); fxPaint.style = Paint.Style.FILL; cv.drawPath(fxPath, fxPaint)
    }

    /** One optional effect around the friend (the Studio's legendary/effects.js: six soft particles); [t] is already slowed in sleep. */
    private fun effect(cv: Canvas, id: String, m: Marks, pose: Pose, t: Float) {
        val b = m.bounds ?: return
        val ap = (if (id == "trails") m.tail else m.tip) ?: m.neck ?: return
        val anchor = project(ap[0], ap[1], pose)
        val color = (fxColor[id] ?: 0xFFFFDF8B).toInt()
        for (i in 0 until 6) {
            val phase = ((t * .37f + i * .173f) % 1f + 1f) % 1f; val angle = i * 2.399f + t * .19f; val r = 3f + 2.2f * (1f + sin(i + t * 2f))
            fxPaint.color = color
            fxPaint.alpha = ((.46f + .5f * sin(Math.PI * phase).toFloat()).coerceIn(0f, 1f) * 255).toInt()
            val x = (b[0] + b[2]) * .5f + cos(angle) * (b[2] - b[0]) * .49f
            val y = b[1] + (b[3] - b[1]) * (1 - phase) * .88f
            when (id) {
                "wings" -> star(cv, anchor[0] + i * 5 - 6, anchor[1] - i * 3, r * .68f)
                "trails" -> {
                    fxPaint.style = Paint.Style.STROKE; fxPaint.strokeWidth = 2f - i * .17f; fxPath.reset()
                    fxPath.moveTo(anchor[0] - i * 4, anchor[1] + i * 2)
                    fxPath.quadTo(anchor[0] - 9 - i * 4, anchor[1] - 6 + sin(t + i) * 3, anchor[0] - 14 - i * 4, anchor[1] + 4 + i * 2)
                    cv.drawPath(fxPath, fxPaint)
                }
                "stars" -> star(cv, x, y, r)
                "flames" -> { fxPaint.style = Paint.Style.FILL; fxPath.reset(); fxPath.moveTo(x, y - r * 2); fxPath.cubicTo(x + 8, y + 4, x - 7, y + 5, x, y - r * 2); cv.drawPath(fxPath, fxPaint) }
                "crystals" -> { fxPaint.style = Paint.Style.FILL; fxPath.reset(); fxPath.moveTo(x, y - 5); fxPath.lineTo(x + 3, y); fxPath.lineTo(x, y + 6); fxPath.lineTo(x - 3, y); fxPath.close(); cv.drawPath(fxPath, fxPaint) }
                "snow" -> {
                    fxPaint.style = Paint.Style.STROKE; fxPaint.strokeWidth = 1f
                    for (j in 0 until 3) { val a = j * Math.PI / 3; val c = cos(a).toFloat() * 4; val s = sin(a).toFloat() * 4; cv.drawLine(x - c, y - s, x + c, y + s, fxPaint) }
                }
                else -> { fxPaint.style = Paint.Style.FILL; cv.drawCircle(x, y, r * .62f, fxPaint) }
            }
        }
        fxPaint.alpha = 255
    }
}
