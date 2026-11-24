package com.alzikr.app.pets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Renderers: one composable, [PetView], draws any skin in any state with any supported format, at the frame rate the
 * Performance Manager allows, adding the fallback's code motion. [PetFrames] turns the same sources into bitmaps for
 * places that cannot animate by themselves (the home-screen widget, the certificate).
 */

/** Decoded pictures, bounded: about 1/16 of the app's memory, at most 24 MB. */
object PetBitmaps {
    private val max = (Runtime.getRuntime().maxMemory() / 16).coerceAtMost(24L * 1024 * 1024).toInt()
    val cache = object : LruCache<String, Bitmap>(max) { override fun sizeOf(k: String, v: Bitmap) = v.byteCount }
    fun get(skinId: String, path: String, maxPx: Int = 1024): Bitmap? {
        val key = "$skinId/$path@$maxPx"
        cache.get(key)?.let { return it }
        val bytes = PetPacks.open(skinId, path)?.use { it.readBytes() } ?: return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var s = 1; while (o.outWidth / (s * 2) >= maxPx && o.outHeight / (s * 2) >= maxPx) s *= 2
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s }) ?: return null
        cache.put(key, b); return b
    }
}

/** dotLottie (.lottie zip): the first animation, with its images embedded, as plain Lottie JSON. */
object DotLottie {
    private val cache = LruCache<String, String>(8)
    fun json(skinId: String, path: String): String? {
        val key = "$skinId/$path"; cache.get(key)?.let { return it }
        val bytes = PetPacks.open(skinId, path)?.use { it.readBytes() } ?: return null
        val entries = HashMap<String, ByteArray>()
        java.util.zip.ZipInputStream(bytes.inputStream()).use { z -> while (true) { val e = z.nextEntry ?: break; if (!e.isDirectory) entries[e.name] = z.readBytes() } }
        val manifest = entries["manifest.json"]?.let { org.json.JSONObject(String(it)) }
        val first = manifest?.optJSONArray("animations")?.optJSONObject(0)?.optString("id")
        val anim = (first?.let { entries["animations/$it.json"] ?: entries["a/$it.json"] } ?: entries.entries.firstOrNull { (n, _) -> (n.startsWith("animations/") || n.startsWith("a/")) && n.endsWith(".json") }?.value) ?: return null
        val o = org.json.JSONObject(String(anim))
        o.optJSONArray("assets")?.let { a -> for (i in 0 until a.length()) { val x = a.getJSONObject(i); val p = x.optString("p"); if (p.isNotEmpty() && !p.startsWith("data:")) {
            val img = entries.entries.firstOrNull { it.key.endsWith("/$p") }?.value ?: continue
            val mime = if (img.size > 4 && img[0] == 0x89.toByte()) "png" else if (img.size > 12 && String(img, 8, 4) == "WEBP") "webp" else "jpeg"
            x.put("p", "data:image/$mime;base64," + android.util.Base64.encodeToString(img, android.util.Base64.NO_WRAP)).put("u", "").put("e", 1) } } }
        return o.toString().also { cache.put(key, it) }
    }
}

/**
 * One shared clock per frame rate: every friend on screen reads the same value, so they all advance in the same frame
 * (one redraw per tick instead of one per friend). A clock runs only while something uses it.
 */
object PetClock {
    class Clock(val fps: Int) { val t = androidx.compose.runtime.mutableLongStateOf(0L); var users = 0; var job: kotlinx.coroutines.Job? = null }
    private val clocks = HashMap<Int, Clock>()
    private val scope = kotlinx.coroutines.CoroutineScope(androidx.compose.ui.platform.AndroidUiDispatcher.Main + kotlinx.coroutines.SupervisorJob())
    fun acquire(fps: Int): Clock {
        val c = clocks.getOrPut(fps) { Clock(fps) }
        if (c.users++ == 0) c.job = scope.launch {
            val start = withFrameMillis { it } - c.t.longValue
            val step = 1000L / fps.coerceAtLeast(1)
            while (true) { c.t.longValue = withFrameMillis { it } - start; delay((step - 4).coerceAtLeast(1)) }
        }
        return c
    }
    fun release(c: Clock) { if (--c.users <= 0) { c.users = 0; c.job?.cancel(); c.job = null } }
}

/** Elapsed milliseconds of the shared clock for [fps] while [running]; frozen (and no clock held) otherwise. */
@Composable
private fun rememberTicker(running: Boolean, fps: Int): () -> Long {
    var frozen by remember { mutableLongStateOf(0L) }
    if (!running) return { frozen }
    val c = remember(fps) { PetClock.acquire(fps) }
    androidx.compose.runtime.DisposableEffect(c) { onDispose { frozen = c.t.longValue; PetClock.release(c) } }
    return { c.t.longValue }
}

/**
 * Draws [skin] in [state]. [playing] false (or the Performance Manager saying no) shows a still frame.
 * [speed] scales the animation; the state's fallback motion (hop, sway, slow…) is added by code when allowed.
 */
@Composable
fun PetView(skin: Skin, state: PetState, modifier: Modifier, place: Place = Place.APP, playing: Boolean = true, speed: Float = 1f, scale: Float = 1f) {
    val r = remember(skin, state) { StateResolver.resolve(skin, state) } ?: return
    val anim = playing && PetPerf.animate(place)
    val fps = PetPerf.fps(place)
    val motion = if (PetPerf.codeMotion(place) && playing) r.motion else Motion.NONE
    val clock = rememberTicker(anim, fps)
    val sp = speed * r.source.speed * when (r.motion) { Motion.SLOW -> .45f; Motion.FAST -> 1.6f; else -> 1f }
    Box(modifier.graphicsLayer {
        val z = skin.fit.zoom * scale
        scaleX = z; scaleY = z; translationY = -skin.fit.lift * size.height
        if (motion != Motion.NONE) {
            val t = clock() / 1000f
            when (motion) {
                Motion.HOP -> { val h = kotlin.math.abs(kotlin.math.sin(t * 5.5f)); translationY -= h * size.height * .12f; scaleY = z * (1f - h * .04f) }
                Motion.BOB, Motion.FAST -> translationY -= kotlin.math.sin(t * (if (motion == Motion.FAST) 14f else 8f)) * size.height * .025f
                Motion.SWAY -> rotationZ = kotlin.math.sin(t * 4f) * 8f
                Motion.TILT -> rotationZ = kotlin.math.sin(t * 1.6f) * 6f
                Motion.SLOW -> { val b = 1f + kotlin.math.sin(t * 1.4f) * .02f; scaleX = z * b; scaleY = z * b }
                Motion.FADE_IN -> alpha = (t * 2f).coerceIn(0f, 1f)
                Motion.FADE_OUT -> alpha = (1f - t * 2f).coerceIn(0f, 1f)
                Motion.NONE -> {}
            }
        }
    }) {
        val src = r.source
        when (src.kind) {
            SourceKind.LOTTIE, SourceKind.DOTLOTTIE -> LottieSource(skin, src, anim, clock, sp)
            SourceKind.SPRITE, SourceKind.FRAMES -> SpriteSource(skin, src, if (anim) clock else { { 0L } }, sp)
            SourceKind.WEBP -> WebpSource(skin, src, anim, clock)
            SourceKind.PNG -> StillSource(skin, src.path)
            SourceKind.RIVE -> skin.fallback?.let { StillSource(skin, it.path) }     // Rive is not enabled yet: its still fallback
        }
    }
}

@Composable
private fun LottieSource(skin: Skin, src: Source, anim: Boolean, clock: () -> Long, speed: Float) {
    val ctx = LocalContext.current
    val spec = remember(skin.id, src.path) {
        val f = PetPacks.file(skin.id, src.path)
        when {
            src.kind == SourceKind.DOTLOTTIE -> null
            f != null -> LottieCompositionSpec.File(f.absolutePath)
            else -> LottieCompositionSpec.Asset("pets/${src.path}")
        }
    }
    val comp: LottieComposition? = if (spec != null) rememberLottieComposition(spec).value else {
        produceState<LottieComposition?>(null, skin.id, src.path) {
            value = withContext(Dispatchers.IO) { DotLottie.json(skin.id, src.path)?.let { LottieCompositionFactory.fromJsonStringSync(it, "dl:${skin.id}/${src.path}").value } }
        }.value
    }
    @Suppress("UNUSED_VARIABLE") val unused = ctx
    val c = comp ?: return
    LottieAnimation(c, {
        val d = (c.duration / speed.coerceAtLeast(.05f)).coerceAtLeast(1f)
        val p = if (!anim) 0f else ((clock() % d.toLong()) / d)
        src.from + p * (src.to - src.from)
    }, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, enableMergePaths = true)  // some looks need merge paths (off by default)
}

@Composable
private fun SpriteSource(skin: Skin, src: Source, clock: () -> Long, speed: Float) {
    val frames = produceState<List<Bitmap>>(emptyList(), skin.id, src) {
        value = withContext(Dispatchers.IO) { spriteFrames(skin, src, 512) }
    }.value
    if (frames.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        val n = frames.size
        val i = ((clock() * src.fps * speed / 1000f).toLong().let { if (src.loop) (it % n) else it.coerceAtMost(n - 1L) }).toInt()
        val b = frames[i]
        val s = minOf(size.width / b.width, size.height / b.height)
        val w = (b.width * s).toInt(); val h = (b.height * s).toInt()
        drawImage(b.asImageBitmap(), IntOffset.Zero, IntSize(b.width, b.height), IntOffset(((size.width - w) / 2).toInt(), ((size.height - h) / 2).toInt()), IntSize(w, h))
    }
}

/** A sprite sheet cut into cells, or a list of frame files. */
internal fun spriteFrames(skin: Skin, src: Source, maxPx: Int): List<Bitmap> = when (src.kind) {
    SourceKind.FRAMES -> src.paths.mapNotNull { PetBitmaps.get(skin.id, it, maxPx) }
    else -> PetBitmaps.get(skin.id, src.path, maxPx * maxOf(src.cols, src.rows))?.let { sheet ->
        val cw = sheet.width / src.cols; val ch = sheet.height / src.rows
        val n = (if (src.frames > 0) src.frames else src.cols * src.rows).coerceAtMost(src.cols * src.rows)
        (0 until n).map { i -> Bitmap.createBitmap(sheet, (i % src.cols) * cw, (i / src.cols) * ch, cw, ch) }
    } ?: emptyList()
}

@Composable
private fun WebpSource(skin: Skin, src: Source, anim: Boolean, clock: () -> Long) {
    if (Build.VERSION.SDK_INT < 28) { StillSource(skin, src.path); return }   // Android 8: the first frame
    val drawable = produceState<android.graphics.drawable.Drawable?>(null, skin.id, src.path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = PetPacks.open(skin.id, src.path)?.use { it.readBytes() } ?: return@runCatching null
                android.graphics.ImageDecoder.decodeDrawable(android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes)))
            }.getOrNull()
        }
    }.value ?: return
    LaunchedEffect(drawable, anim) { (drawable as? android.graphics.drawable.AnimatedImageDrawable)?.let { if (anim) it.start() else it.stop() } }
    Canvas(Modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE") val tick = clock()      // redraw at the allowed frame rate
        val w = drawable.intrinsicWidth.coerceAtLeast(1); val h = drawable.intrinsicHeight.coerceAtLeast(1)
        val s = minOf(size.width / w, size.height / h)
        val dw = (w * s).toInt(); val dh = (h * s).toInt()
        drawable.setBounds(((size.width - dw) / 2).toInt(), ((size.height - dh) / 2).toInt(), ((size.width + dw) / 2).toInt(), ((size.height + dh) / 2).toInt())
        drawIntoCanvas { drawable.draw(it.nativeCanvas) }
    }
}

@Composable
private fun StillSource(skin: Skin, path: String) {
    val b = produceState<Bitmap?>(null, skin.id, path) { value = withContext(Dispatchers.IO) { PetBitmaps.get(skin.id, path, 512) } }.value ?: return
    androidx.compose.foundation.Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
}

/** Still frames of a skin for the widget and the certificate. Asleep: one frame. */
object PetFrames {
    /** One frame at [at] (0..1) of the skin's idle (or its fallback), [px] square, with the skin's fit applied. */
    fun frame(c: Context, skin: Skin, at: Float, px: Int, state: PetState = PetState.IDLE): Bitmap? {
        val r = StateResolver.resolve(skin, state) ?: return null
        val src = r.source
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(out)
        when (src.kind) {
            SourceKind.LOTTIE, SourceKind.DOTLOTTIE -> {
                val comp = (if (src.kind == SourceKind.DOTLOTTIE) DotLottie.json(skin.id, src.path)?.let { LottieCompositionFactory.fromJsonStringSync(it, "dl:${skin.id}").value }
                    else PetPacks.file(skin.id, src.path)?.let { f -> f.inputStream().use { LottieCompositionFactory.fromJsonInputStreamSync(it, "f:${f.path}").value } }
                        ?: LottieCompositionFactory.fromAssetSync(c, "pets/${src.path}").value) ?: return null
                val d = LottieDrawable().apply { enableMergePathsForKitKatAndAbove(true); composition = comp; progress = src.from + at * (src.to - src.from) }
                val w = comp.bounds.width().toFloat().coerceAtLeast(1f); val h = comp.bounds.height().toFloat().coerceAtLeast(1f)
                val s = minOf(px / w, px / h) * skin.fit.zoom
                cv.translate((px - w * s) / 2f, (px - h * s) / 2f - skin.fit.lift * px); cv.scale(s, s)
                d.setBounds(0, 0, w.toInt(), h.toInt()); d.draw(cv)
            }
            SourceKind.SPRITE, SourceKind.FRAMES -> spriteFrames(skin, src, px).takeIf { it.isNotEmpty() }?.let { fs -> drawFit(cv, fs[((at * fs.size).toInt()).coerceIn(0, fs.size - 1)], px, skin.fit) } ?: return null
            SourceKind.WEBP, SourceKind.PNG -> (PetBitmaps.get(skin.id, src.path, px) ?: return null).let { drawFit(cv, it, px, skin.fit) }
            SourceKind.RIVE -> skin.fallback?.let { f -> PetBitmaps.get(skin.id, f.path, px)?.let { drawFit(cv, it, px, skin.fit) } } ?: return null
        }
        return out
    }
    /** Can this skin give more than one frame (so the widget animates)? */
    fun animated(skin: Skin) = StateResolver.resolve(skin, PetState.IDLE)?.source?.kind.let { it == SourceKind.LOTTIE || it == SourceKind.DOTLOTTIE || it == SourceKind.SPRITE || it == SourceKind.FRAMES }
    private fun drawFit(cv: android.graphics.Canvas, b: Bitmap, px: Int, fit: Fit) {
        val s = minOf(px.toFloat() / b.width, px.toFloat() / b.height) * fit.zoom
        val w = b.width * s; val h = b.height * s
        cv.drawBitmap(b, null, android.graphics.RectF((px - w) / 2, (px - h) / 2 - fit.lift * px, (px + w) / 2, (px + h) / 2 - fit.lift * px), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
    }
    /** Lottie timeline length (ms) for frame counting; sprites use their own count. */
    fun durationMs(c: Context, skin: Skin): Float {
        val src = StateResolver.resolve(skin, PetState.IDLE)?.source ?: return 0f
        return when (src.kind) {
            SourceKind.LOTTIE -> (PetPacks.file(skin.id, src.path)?.let { f -> f.inputStream().use { LottieCompositionFactory.fromJsonInputStreamSync(it, "f:${f.path}").value } }
                ?: LottieCompositionFactory.fromAssetSync(c, "pets/${src.path}").value)?.duration?.times(src.to - src.from) ?: 0f
            SourceKind.DOTLOTTIE -> DotLottie.json(skin.id, src.path)?.let { LottieCompositionFactory.fromJsonStringSync(it, "dl:${skin.id}").value?.duration } ?: 0f
            SourceKind.SPRITE, SourceKind.FRAMES -> (if (src.kind == SourceKind.FRAMES) src.paths.size else (if (src.frames > 0) src.frames else src.cols * src.rows)) * 1000f / src.fps
            else -> 0f
        }
    }
}
