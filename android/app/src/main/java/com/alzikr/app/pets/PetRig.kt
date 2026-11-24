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
    )
    class Pose(val controls: List<FloatArray>, val dx: Float, val dy: Float, val angle: Float, val sx: Float, val sy: Float, val blink: Float)

    private val rigs = LruCache<String, Rig>(16)

    fun parse(text: String): Rig {
        val o = JSONObject(text)
        fun pt(k: String) = o.optJSONArray(k)?.let { floatArrayOf(it.getDouble(0).toFloat(), it.getDouble(1).toFloat()) }
        val ga = o.getJSONArray("g")
        return Rig(o.optString("gait"), FloatArray(ga.length()) { ga.getDouble(it).toFloat() }, o.getString("art"),
            o.optString("sleep").ifEmpty { null }, o.optString("closed").ifEmpty { null },
            (o.optJSONArray("eyes") ?: org.json.JSONArray()).let { a -> (0 until a.length()).map { i -> a.getJSONArray(i).let { floatArrayOf(it.getDouble(0).toFloat(), it.getDouble(1).toFloat()) } } },
            pt("head"), pt("neck"), pt("tail"), pt("near"), pt("far"), pt("tip"),
            o.optJSONArray("view")?.takeIf { it.length() == 4 }?.let { a -> FloatArray(4) { a.getDouble(it).toFloat() } })
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
    @Synchronized fun draw(cv: Canvas, w: Float, h: Float, r: Rig, anim: String, t: Float, art: Bitmap, sleepArt: Bitmap?, closedArt: Bitmap?) {
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
        cv.restore()
    }
}
