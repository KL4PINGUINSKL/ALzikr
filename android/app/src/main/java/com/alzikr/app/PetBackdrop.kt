package com.alzikr.app

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The scene behind the friend in its card («صديقي»): five open to everyone, five opened by friendship points, or a
 * picture from the phone. And «الوهج»: a soft coloured glow around the friend at the top of the app.
 * The scenes are drawn, not pictures, so the app stays light.
 */
object PetBackdrop {
    data class Scene(val id: String, val name: String, val bond: Int)
    val scenes = listOf(
        Scene("night", "سماء الليل", 0), Scene("dawn", "الفجر", 0), Scene("garden", "الحديقة", 0), Scene("sea", "البحر", 0), Scene("dunes", "الصحراء", 0),
        Scene("spring", "الربيع", 0), Scene("pinkclouds", "غيوم وردية", 0), Scene("mountains", "الجبال", 0),
        Scene("moon", "الهلال", 150), Scene("aurora", "الشفق", 400), Scene("rain", "المطر", 600), Scene("zellige", "الزليج", 900),
        Scene("skyclouds", "السماء والغيوم", 0), Scene("sunclouds", "شمس بين الغيوم", 0), Scene("sunsetclouds", "غيوم الغروب", 0), Scene("snowfall", "الثلج", 0),
        Scene("cotton", "غيوم القطن", 300), Scene("aboveclouds", "فوق الغيوم", 800), Scene("nightclouds", "غيوم الليل", 1500), Scene("goldclouds", "غيوم ذهبية", 2500),
        Scene("sakura", "الكرز", 1200), Scene("lanterns", "الفوانيس", 2000), Scene("meteors", "الشهب", 3000), Scene("galaxy", "المجرّة", 4000),
    )
    /** A picture from the phone (for the card or the widget) opens with friendship too. */
    const val PHOTO_BOND = 1000
    fun photoOpen() = Progress.bondPoints >= PHOTO_BOND
    fun open(s: Scene) = Progress.bondPoints >= s.bond

    /** "" none, a scene id, or "photo". */
    var chosen: String
        get() = Store.prefs.getString("pet_card_bg", "") ?: ""
        set(v) { Store.prefs.edit().putString("pet_card_bg", v).apply(); Store.bump() }
    fun photo(c: android.content.Context) = File(c.filesDir, "pet_bg.jpg")

    /** Copies (and shrinks) a picture from the phone to be the card's background. Blocking. */
    fun setPhoto(c: android.content.Context, uri: Uri): Boolean = runCatching {
        val bytes = c.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var sample = 1; while (o.outWidth / sample > 1600 || o.outHeight / sample > 1600) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })!!
        photo(c).outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
        Store.prefs.edit().putLong("pet_bg_photo_at", System.currentTimeMillis()).apply()
        chosen = "photo"; true
    }.getOrDefault(false)

    // ---------------------------------------------------------------- the home-screen widget's own backgrounds
    /** Scenes for the «صديقي» widget (not the card's): [light] = a light scene, so the widget's words turn dark on it. */
    data class WScene(val id: String, val name: String, val bond: Int, val light: Boolean)
    val widgetScenes = listOf(
        WScene("w_sunset", "الغروب", 0, false), WScene("w_forest", "الغابة", 0, false), WScene("w_snow", "الثلج", 0, true),
        WScene("w_clouds", "السماء الصافية", 0, true), WScene("w_calm", "الليل الهادئ", 0, false),
        WScene("w_lamp", "المشكاة", 250, false), WScene("w_palms", "النخيل", 700, true), WScene("w_waves", "أمواج الليل", 1500, false),
        WScene("w_stars", "المرصد", 3000, false), WScene("w_gold", "الذهب", 5000, true),
        WScene("w_lavender", "حقل البنفسج", 0, false), WScene("w_river", "النهر", 0, true),
        WScene("w_pinkclouds", "غيوم وردية", 0, true), WScene("w_sunsetclouds", "غيوم الغروب", 0, false), WScene("w_cotton", "غيوم القطن", 0, true),
        WScene("w_above", "فوق الغيوم", 400, true), WScene("w_desertnight", "ليل الصحراء", 600, false), WScene("w_nightclouds", "غيوم الليل", 1000, false),
        WScene("w_dawnclouds", "غيوم الفجر", 1800, false), WScene("w_goldclouds", "غيوم ذهبية", 3500, true),
        WScene("w_rainbow", "قوس المطر", 2000, true), WScene("w_moonsea", "القمر والبحر", 4000, false),
    )
    fun open(s: WScene) = Progress.bondPoints >= s.bond
    /** "" = the theme or a frame (as before), a widget scene id, or "photo". */
    var widgetBg: String
        get() = Store.prefs.getString("pet_widget_bg", "") ?: ""
        set(v) { Store.prefs.edit().putString("pet_widget_bg", v).apply(); Store.bump() }
    fun widgetPhoto(c: android.content.Context) = File(c.filesDir, "pet_widget_bg.jpg")
    fun setWidgetPhoto(c: android.content.Context, uri: Uri): Boolean = runCatching {
        val bytes = c.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var sample = 1; while (o.outWidth / sample > 1200 || o.outHeight / sample > 1200) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })!!
        widgetPhoto(c).outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
        widgetBg = "photo"; true
    }.getOrDefault(false)

    /**
     * The widget's background as a picture (900 × 450 px, rounded corners, a light veil so the words read), or null
     * for the theme / frame. The second value: whether the words should be dark.
     */
    fun widgetBitmap(c: android.content.Context): Pair<android.graphics.Bitmap, Boolean>? {
        val id = widgetBg; if (id.isEmpty()) return null
        val w = 900; val h = 450
        val out = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(out)
        val r = 22f * c.resources.displayMetrics.density
        val clip = android.graphics.Path().apply { addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, android.graphics.Path.Direction.CW) }
        // A frame chosen too: drawn first, then the scene inset inside it (the frame stays as a border).
        val frame = Progress.frames.firstOrNull { it.id == Progress.frame }?.res
        if (frame != null) runCatching {
            androidx.core.content.ContextCompat.getDrawable(c, frame)?.let { d -> d.setBounds(0, 0, w, h); d.draw(cv) }
            val inset = 9f * c.resources.displayMetrics.density
            cv.clipPath(android.graphics.Path().apply { addRoundRect(inset, inset, w - inset, h - inset, r - inset / 2, r - inset / 2, android.graphics.Path.Direction.CW) })
        } else cv.clipPath(clip)
        var light = false
        if (id == "photo") {
            val src = runCatching { BitmapFactory.decodeFile(widgetPhoto(c).path) }.getOrNull() ?: return null
            val scale = maxOf(w / src.width.toFloat(), h / src.height.toFloat())
            val dw = src.width * scale; val dh = src.height * scale
            cv.drawBitmap(src, null, android.graphics.RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
            // A darker veil on the words' side (left half, right-to-left layout) so white words read on any photo.
            cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), android.graphics.Paint().apply { shader = android.graphics.LinearGradient(w.toFloat(), 0f, 0f, 0f, 0x22000000, 0x99000000.toInt(), android.graphics.Shader.TileMode.CLAMP) })
        } else {
            val s = widgetScenes.firstOrNull { it.id == id } ?: return null
            light = s.light
            val img = androidx.compose.ui.graphics.ImageBitmap(w, h)
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(c.resources.displayMetrics.density), androidx.compose.ui.unit.LayoutDirection.Rtl,
                androidx.compose.ui.graphics.Canvas(img), Size(w.toFloat(), h.toFloat())) { drawWidgetScene(id) }
            cv.drawBitmap(img.asAndroidBitmapCompat(), 0f, 0f, null)
        }
        return out to light
    }

    /** The glow's colour: "" none, a name below, or "rainbow" (its colours turn slowly). */
    var glow: String
        get() = Store.prefs.getString("pet_glow", "") ?: ""
        set(v) { Store.prefs.edit().putString("pet_glow", v).apply(); Store.bump() }
    val glows = listOf("gold" to Color(0xFFFFC94D), "green" to Color(0xFF4CD39A), "blue" to Color(0xFF5BA8FF), "pink" to Color(0xFFFF7EB6), "purple" to Color(0xFFB07CFF), "white" to Color(0xFFFFF6E0), "rainbow" to Color(0xFFFF5F5F))
    val glowNames = mapOf("gold" to "ذهبي", "green" to "أخضر", "blue" to "أزرق", "pink" to "وردي", "purple" to "بنفسجي", "white" to "نور", "rainbow" to "قوس قزح")
}

private fun androidx.compose.ui.graphics.ImageBitmap.asAndroidBitmapCompat(): android.graphics.Bitmap = asAndroidBitmap()

/** A soft, fluffy cloud centred at (x, y); [s] scales it, [shade] draws its underside a little lower. */
fun DrawScope.cloud(x: Float, y: Float, s: Float, c: Color, shade: Color? = null) {
    val d = density * s
    fun puff(dy: Float, col: Color) {
        drawCircle(col, 17 * d, Offset(x - 27 * d, y + dy)); drawCircle(col, 25 * d, Offset(x - 5 * d, y - 9 * d + dy))
        drawCircle(col, 19 * d, Offset(x + 20 * d, y - 3 * d + dy)); drawCircle(col, 13 * d, Offset(x + 38 * d, y + 5 * d + dy))
        drawRoundRect(col, Offset(x - 44 * d, y + dy), Size(95 * d, 18 * d), androidx.compose.ui.geometry.CornerRadius(9 * d))
    }
    if (shade != null) puff(5 * d, shade)
    puff(0f, c)
}

/** The widget's own scenes (still: a widget cannot animate a drawing). */
fun DrawScope.drawWidgetScene(id: String) {
    val w = size.width; val h = size.height
    fun stars(n: Int, seed: Int) { val r = java.util.Random(seed.toLong()); repeat(n) { drawCircle(Color.White.copy(alpha = .35f + r.nextFloat() * .6f), (r.nextFloat() * 1.8f + .6f) * density, Offset(r.nextFloat() * w, r.nextFloat() * h * .8f)) } }
    when (id) {
        "w_sunset" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF2B1B4F), Color(0xFF8E3B6B), Color(0xFFE9744A), Color(0xFFF6B05A))))
            drawCircle(Color(0xFFFFD58A), h * .2f, Offset(w * .28f, h * .78f))
            drawRect(Brush.verticalGradient(listOf(Color(0x00000000), Color(0x66200A2A)), 0f, h), Offset(0f, h * .55f), Size(w, h * .45f)) }
        "w_forest" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF0F3B2E), Color(0xFF1E6B4C))))
            val r = java.util.Random(4); repeat(14) { k -> val x = r.nextFloat() * w; val th = h * (.35f + r.nextFloat() * .4f); val c = listOf(Color(0xFF0B2A20), Color(0xFF134434), Color(0xFF1A5A43))[k % 3]
                drawPath(Path().apply { moveTo(x, h - th); lineTo(x - th * .28f, h); lineTo(x + th * .28f, h); close() }, c) } }
        "w_snow" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFDCEBFA), Color(0xFFF7FBFF))))
            drawPath(Path().apply { moveTo(0f, h * .75f); quadraticBezierTo(w * .4f, h * .6f, w, h * .78f); lineTo(w, h); lineTo(0f, h); close() }, Color.White)
            val r = java.util.Random(9); repeat(60) { drawCircle(Color(0xFF9EC3E6).copy(alpha = .5f), (1 + r.nextFloat() * 2.5f) * density, Offset(r.nextFloat() * w, r.nextFloat() * h)) } }
        "w_clouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF8FCBF4), Color(0xFFDDF1FF))))
            listOf(.2f to .3f, .65f to .2f, .45f to .62f, .85f to .55f).forEach { (fx, fy) -> val x = w * fx; val y = h * fy
                listOf(-30f to 0f, 0f to -14f, 30f to 0f, 12f to 8f).forEach { (dx, dy) -> drawCircle(Color.White.copy(alpha = .85f), 26 * density, Offset(x + dx * density, y + dy * density)) } } }
        "w_calm" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF070D24), Color(0xFF16245A)))); stars(70, 17) }
        "w_lamp" -> { drawRect(Brush.radialGradient(listOf(Color(0xFF6B3A12), Color(0xFF1C0E05)), Offset(w * .25f, h * .45f), w * .6f))
            drawCircle(Brush.radialGradient(listOf(Color(0xCCFFC765), Color(0x00FFC765)), Offset(w * .25f, h * .5f), h * .7f), h * .7f, Offset(w * .25f, h * .5f))
            drawRoundRect(Color(0xFFFFC765), Offset(w * .25f - 22 * density, h * .5f - 30 * density), Size(44 * density, 56 * density), androidx.compose.ui.geometry.CornerRadius(14 * density)) }
        "w_palms" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE6B8), Color(0xFFF2C98A))))
            drawPath(Path().apply { moveTo(0f, h * .85f); quadraticBezierTo(w * .5f, h * .72f, w, h * .86f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFFD9A45E))
            listOf(.16f, .3f).forEach { fx -> val x = w * fx
                drawLine(Color(0xFF7A5230), Offset(x, h * .85f), Offset(x + 10 * density, h * .32f), 7 * density)
                for (k in 0..5) { val a = (PI + k * PI / 5).toFloat(); drawLine(Color(0xFF3E7D3A), Offset(x + 10 * density, h * .32f), Offset(x + 10 * density + cos(a) * 60 * density, h * .32f + sin(a) * -26 * density + 22 * density), 6 * density) } } }
        "w_waves" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF041A33), Color(0xFF0C3B66)))); stars(30, 21)
            drawCircle(Color(0xFFFFF3C4), h * .1f, Offset(w * .22f, h * .22f))
            for (k in 0..4) { val y = h * (.55f + k * .1f); val p = Path(); p.moveTo(0f, y); var x = 0f
                while (x <= w) { p.lineTo(x, y + sin((x / w * 8 * PI + k).toFloat()) * 6 * density); x += 8 * density }
                drawPath(p, Color(0xFF8FD3FF).copy(alpha = .35f - k * .05f), style = Stroke(2.5f * density)) } }
        "w_stars" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF02040F), Color(0xFF101B3D)))); stars(90, 31)
            val pts = listOf(.12f to .3f, .22f to .2f, .32f to .34f, .41f to .25f, .5f to .4f).map { Offset(w * it.first, h * it.second) }
            pts.zipWithNext().forEach { (a, b) -> drawLine(Color(0x99A9C8FF), a, b, 1.5f * density) }; pts.forEach { drawCircle(Color.White, 3 * density, it) } }
        "w_lavender" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF4A2D7A), Color(0xFFB58BD9), Color(0xFFF3C6A8))))
            for (k in 0..7) { val y = h * (.6f + k * .06f); drawLine(Color(0xFF7A4FB5).copy(alpha = .8f - k * .05f), Offset(0f, y), Offset(w, y + (k - 3) * 6 * density), (5 + k) * density) } }
        "w_river" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFBFE6C8), Color(0xFFE6F5D8))))
            drawPath(Path().apply { moveTo(w * .35f, 0f); quadraticBezierTo(w * .2f, h * .5f, w * .45f, h); lineTo(w * .7f, h); quadraticBezierTo(w * .4f, h * .5f, w * .5f, 0f); close() }, Color(0xFF7CC6E8)) }
        "w_rainbow" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF9FD3F2), Color(0xFFE8F6FF))))
            listOf(0xFFFF6B6B, 0xFFFFA94D, 0xFFFFE066, 0xFF69DB7C, 0xFF4DABF7, 0xFF9775FA).forEachIndexed { k, c ->
                drawCircle(Color(c).copy(alpha = .75f), h * (.95f - k * .07f), Offset(w * .3f, h * 1.15f), style = Stroke(h * .07f)) } }
        "w_moonsea" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF050B24), Color(0xFF0E2A55)))); stars(50, 51)
            drawCircle(Color(0x33FFF3C4), h * .3f, Offset(w * .22f, h * .3f)); drawCircle(Color(0xFFFFF3C4), h * .14f, Offset(w * .22f, h * .3f))
            for (k in 0..6) { val y = h * (.62f + k * .055f); drawLine(Color(0xFFFFF3C4).copy(alpha = .5f - k * .06f), Offset(w * .22f - (30 - k * 3) * density, y), Offset(w * .22f + (30 - k * 3) * density, y), 2 * density) } }
        "w_pinkclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFFFC9D9), Color(0xFFFFE6D6), Color(0xFFE0D4FF))))
            listOf(Triple(.18f, .3f, 1.1f), Triple(.62f, .2f, .9f), Triple(.42f, .66f, 1.3f), Triple(.88f, .58f, 1f)).forEach { (fx, fy, k) -> cloud(w * fx, h * fy, k, Color.White.copy(alpha = .92f), Color(0x55E59AB8)) } }
        "w_sunsetclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2466), Color(0xFFC0507A), Color(0xFFF59A5A), Color(0xFFFFD08A))))
            drawCircle(Color(0xFFFFE0A0), h * .16f, Offset(w * .7f, h * .72f))
            listOf(Triple(.2f, .35f, 1.1f), Triple(.55f, .5f, .9f), Triple(.85f, .3f, 1f), Triple(.35f, .8f, 1.3f)).forEach { (fx, fy, k) -> cloud(w * fx, h * fy, k, Color(0xFFFFB8A0), Color(0xFF9A4A7A)) } }
        "w_cotton" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFCFE0FF), Color(0xFFF0E6FF))))
            val cs = listOf(Color(0xFFFFD6EC), Color(0xFFD6E8FF), Color(0xFFE9DCFF), Color(0xFFFFF1D6))
            listOf(Triple(.15f, .28f, 1f), Triple(.5f, .22f, 1.2f), Triple(.85f, .35f, .9f), Triple(.3f, .7f, 1.3f), Triple(.75f, .75f, 1.1f)).forEachIndexed { i, (fx, fy, k) -> cloud(w * fx, h * fy, k, cs[i % 4], Color(0x33A08CD0)) } }
        "w_above" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF4A9EE8), Color(0xFFA8D8FF))))
            drawCircle(Color(0x55FFFFFF), h * .3f, Offset(w * .8f, h * .25f)); drawCircle(Color(0xFFFFF6D0), h * .12f, Offset(w * .8f, h * .25f))
            for (k in 0..8) cloud(w * (k / 8f), h * (.78f + (k % 2) * .06f), 1.5f, Color.White, Color(0xFFC9DDF0)) }
        "w_desertnight" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0F2C), Color(0xFF2A2350), Color(0xFF5A3E5E)))); stars(60, 61)
            drawCircle(Color(0xFFFFF1C4), h * .1f, Offset(w * .2f, h * .22f)); drawCircle(Color(0xFF14183A), h * .085f, Offset(w * .2f + h * .045f, h * .2f))
            drawPath(Path().apply { moveTo(0f, h * .78f); quadraticBezierTo(w * .35f, h * .6f, w * .7f, h * .8f); quadraticBezierTo(w * .9f, h * .88f, w, h * .76f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF3A2A40)) }
        "w_nightclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF0A1236), Color(0xFF26346E)))); stars(45, 71)
            drawCircle(Color(0x33FFF3C4), h * .28f, Offset(w * .72f, h * .32f)); drawCircle(Color(0xFFFFF3C4), h * .13f, Offset(w * .72f, h * .32f))
            listOf(Triple(.25f, .45f, 1.1f), Triple(.62f, .55f, .9f), Triple(.9f, .75f, 1.2f), Triple(.12f, .82f, 1f)).forEach { (fx, fy, k) -> cloud(w * fx, h * fy, k, Color(0xFF5A6AA8), Color(0xFF2E3A72)) } }
        "w_dawnclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF2E3A7A), Color(0xFF8A6AB8), Color(0xFFF3A9A0), Color(0xFFFFE1B0))))
            drawCircle(Brush.radialGradient(listOf(Color(0xAAFFF1C2), Color(0x00FFF1C2)), Offset(w * .5f, h), h * .6f), h * .6f, Offset(w * .5f, h))
            listOf(Triple(.2f, .3f, 1f), Triple(.7f, .22f, 1.1f), Triple(.45f, .55f, .9f), Triple(.9f, .6f, 1f)).forEach { (fx, fy, k) -> cloud(w * fx, h * fy, k, Color(0xFFFFD2C4), Color(0xFF9C7AC0)) } }
        "w_goldclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE7A8), Color(0xFFFFF6DC))))
            drawCircle(Brush.radialGradient(listOf(Color(0xCCFFFFFF), Color(0x00FFFFFF)), Offset(w * .5f, h * .3f), h * .6f), h * .6f, Offset(w * .5f, h * .3f))
            listOf(Triple(.18f, .4f, 1.1f), Triple(.5f, .7f, 1.3f), Triple(.84f, .38f, 1f)).forEach { (fx, fy, k) -> cloud(w * fx, h * fy, k, Color(0xFFFFF4D6), Color(0xFFE9C46A)) }
            val r = java.util.Random(77); repeat(40) { drawCircle(Color.White.copy(alpha = .5f + r.nextFloat() * .5f), (1 + r.nextFloat() * 2f) * density, Offset(r.nextFloat() * w, r.nextFloat() * h)) } }
        "w_gold" -> { drawRect(Brush.linearGradient(listOf(Color(0xFFFFF1C4), Color(0xFFE9C46A), Color(0xFFF8DE9A)), Offset.Zero, Offset(w, h)))
            val r = java.util.Random(13); repeat(70) { drawCircle(Color.White.copy(alpha = .4f + r.nextFloat() * .5f), (1 + r.nextFloat() * 2.2f) * density, Offset(r.nextFloat() * w, r.nextFloat() * h)) } }
    }
}

/** One scene, drawn to fill its box; [t] 0..1 moves the living parts (stars, waves, lights) when motion is on. */
fun DrawScope.drawScene(id: String, t: Float) {
    val w = size.width; val h = size.height
    fun stars(n: Int, seed: Int, color: Color = Color.White) { val r = java.util.Random(seed.toLong())
        repeat(n) { val x = r.nextFloat() * w; val y = r.nextFloat() * h * .75f; val tw = .45f + .55f * ((sin((t * 2 * PI + r.nextFloat() * 6.28).toFloat()) + 1) / 2)
            drawCircle(color.copy(alpha = tw), (r.nextFloat() * 1.6f + .6f) * density, Offset(x, y)) } }
    when (id) {
        "night" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1638), Color(0xFF1E2F6B), Color(0xFF3B3F8F)))); stars(38, 7)
            drawCircle(Color(0x33FFFFFF), h * .9f, Offset(w * .5f, h * 1.55f)) }
        "dawn" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF3B2A5C), Color(0xFFB4567A), Color(0xFFF2A25C), Color(0xFFFFD79A))))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1C2), Color(0x00FFF1C2)), Offset(w * .5f, h * .92f), h * .55f), h * .55f, Offset(w * .5f, h * .92f))
            drawCircle(Color(0xFFFFE6A6), h * .17f, Offset(w * .5f, h * .9f)) }
        "garden" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF9ED6F5), Color(0xFFD9F2FF))))
            drawCircle(Color(0xFFFFF3B0), h * .1f, Offset(w * .82f, h * .2f))
            drawPath(Path().apply { moveTo(0f, h * .7f); quadraticBezierTo(w * .3f, h * .5f, w * .6f, h * .68f); quadraticBezierTo(w * .85f, h * .8f, w, h * .62f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF7BC67E))
            drawPath(Path().apply { moveTo(0f, h * .85f); quadraticBezierTo(w * .4f, h * .7f, w, h * .86f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF4FA35A)) }
        "sea" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF7FD3E8), Color(0xFF2E8FB8), Color(0xFF0E5C86))))
            for (k in 0..3) { val y = h * (.55f + k * .12f); val p = Path(); p.moveTo(0f, y)
                var x = 0f; while (x <= w) { p.lineTo(x, y + sin((x / w * 6 * PI + t * 2 * PI + k).toFloat()) * 5 * density); x += 6 * density }
                drawPath(p, Color.White.copy(alpha = .28f - k * .05f), style = Stroke(2 * density)) } }
        "dunes" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE3B0), Color(0xFFF7C77B))))
            drawCircle(Color(0xFFFFF6D8), h * .13f, Offset(w * .22f, h * .25f))
            drawPath(Path().apply { moveTo(0f, h * .72f); quadraticBezierTo(w * .35f, h * .5f, w * .7f, h * .74f); quadraticBezierTo(w * .9f, h * .84f, w, h * .7f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFFE0A85E))
            drawPath(Path().apply { moveTo(0f, h * .88f); quadraticBezierTo(w * .5f, h * .72f, w, h * .9f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFFC98A44)) }
        "spring" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFBDE7FF), Color(0xFFE9F8FF))))
            drawPath(Path().apply { moveTo(0f, h * .72f); quadraticBezierTo(w * .5f, h * .6f, w, h * .74f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF8FD18A))
            val r = java.util.Random(12); repeat(26) { val x = r.nextFloat() * w; val y = h * (.76f + r.nextFloat() * .22f); val c = listOf(Color(0xFFFF8FB1), Color(0xFFFFE066), Color(0xFFFFFFFF), Color(0xFFC79BFF))[it % 4]
                for (k in 0..4) { val a = (k * 2 * PI / 5).toFloat(); drawCircle(c, 3.2f * density, Offset(x + cos(a) * 3.5f * density, y + sin(a) * 3.5f * density)) }; drawCircle(Color(0xFFFFC94D), 2f * density, Offset(x, y)) } }
        "pinkclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFFFD1DC), Color(0xFFFFE9D6), Color(0xFFE3D7FF))))
            listOf(.2f to .3f, .7f to .22f, .45f to .55f, .9f to .6f, .1f to .75f).forEachIndexed { k, (fx, fy) -> val x = w * fx; val y = h * fy + sin((t * 2 * PI + k).toFloat()) * 3 * density
                listOf(-28f to 0f, 0f to -12f, 28f to 0f, 10f to 8f).forEach { (dx, dy) -> drawCircle(Color.White.copy(alpha = .8f), 22 * density, Offset(x + dx * density, y + dy * density)) } } }
        "mountains" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF6FA8DC), Color(0xFFCFE8F7))))
            drawPath(Path().apply { moveTo(0f, h * .8f); lineTo(w * .22f, h * .38f); lineTo(w * .42f, h * .72f); lineTo(w * .62f, h * .3f); lineTo(w * .85f, h * .66f); lineTo(w, h * .5f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF5A6E8C))
            drawPath(Path().apply { moveTo(w * .62f, h * .3f); lineTo(w * .56f, h * .4f); lineTo(w * .6f, h * .38f); lineTo(w * .64f, h * .42f); lineTo(w * .68f, h * .38f); close() }, Color.White)
            drawPath(Path().apply { moveTo(0f, h * .9f); quadraticBezierTo(w * .5f, h * .76f, w, h * .9f); lineTo(w, h); lineTo(0f, h); close() }, Color(0xFF3E7D5A)) }
        "rain" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF2B3A4F), Color(0xFF4A5D78))))
            val r = java.util.Random(8); repeat(70) { val x = r.nextFloat() * w; val y0 = r.nextFloat(); val y = ((y0 + t * 3f) % 1f) * h
                drawLine(Color(0x88CFE6FF), Offset(x, y), Offset(x - 3 * density, y + 12 * density), 1.3f * density) } }
        "sakura" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE4EC), Color(0xFFFFF6F0))))
            drawLine(Color(0xFF6B4A3A), Offset(w * .9f, h), Offset(w * .78f, h * .35f), 8 * density); drawLine(Color(0xFF6B4A3A), Offset(w * .8f, h * .5f), Offset(w * .55f, h * .25f), 5 * density)
            listOf(.78f to .3f, .62f to .22f, .9f to .45f, .7f to .42f).forEach { (fx, fy) -> drawCircle(Color(0xFFFFB7CC), 34 * density, Offset(w * fx, h * fy)) }
            val r = java.util.Random(3); repeat(24) { val x = (r.nextFloat() * w - t * w * .3f + w) % w; val y = (r.nextFloat() * h + t * h * .5f) % h; drawCircle(Color(0xFFFF9EBB), 3 * density, Offset(x, y)) } }
        "meteors" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF02030A), Color(0xFF14183A)))); stars(50, 41)
            for (k in 0..3) { val p = ((t + k * .25f) % 1f); val x = w * (1.1f - p * 1.2f) - k * 40 * density; val y = h * (p * .9f) + k * 12 * density
                drawLine(Brush.linearGradient(listOf(Color(0x00FFFFFF), Color(0xEEFFFFFF)), Offset(x + 60 * density, y - 30 * density), Offset(x, y)), Offset(x + 60 * density, y - 30 * density), Offset(x, y), 2 * density) } }
        "moon" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF050B1F), Color(0xFF14204A)))); stars(30, 3)
            val c = Offset(w * .78f, h * .3f); val r = h * .2f
            drawCircle(Color(0x33FFE9A8), r * 2.2f, c); drawCircle(Color(0xFFFFE9A8), r, c); drawCircle(Color(0xFF0A1430), r * .88f, Offset(c.x + r * .42f, c.y - r * .12f)) }
        "aurora" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF041019), Color(0xFF0C2A3A)))); stars(20, 11)
            for (k in 0..2) { val p = Path(); val base = h * (.32f + k * .12f); p.moveTo(0f, base)
                var x = 0f; while (x <= w) { p.lineTo(x, base + sin((x / w * 3 * PI + t * 2 * PI * (k + 1) * .5f).toFloat()) * 16 * density); x += 8 * density }
                drawPath(p, Brush.horizontalGradient(listOf(Color(0x0044FFB0), listOf(Color(0xAA44FFB0), Color(0xAA4FC3FF), Color(0xAAB07CFF))[k], Color(0x0044FFB0))), style = Stroke((14 - k * 3) * density)) } }
        "zellige" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF0E2D63), Color(0xFF1C4C9C))))
            val s = 34 * density; var y = 0f; var row = 0
            while (y < h + s) { var x = if (row % 2 == 0) 0f else s / 2; while (x < w + s) {
                for (k in 0..7) { val a = k * PI / 4; drawLine(Color(0x55FFD97A), Offset(x, y), Offset(x + cos(a).toFloat() * s * .38f, y + sin(a).toFloat() * s * .38f), 1.4f * density) }
                drawCircle(Color(0x66FFFFFF), 2.2f * density, Offset(x, y)); x += s }; y += s * .86f; row++ } }
        "lanterns" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1405), Color(0xFF5A2E0C))))
            listOf(.15f to .3f, .38f to .18f, .62f to .26f, .86f to .16f).forEachIndexed { k, (fx, fy) ->
                val x = w * fx; val y = h * fy + sin((t * 2 * PI + k).toFloat()) * 3 * density
                drawLine(Color(0x88E8C27A), Offset(x, 0f), Offset(x, y - 14 * density), 1.2f * density)
                drawCircle(Brush.radialGradient(listOf(Color(0x88FFC765), Color(0x00FFC765)), Offset(x, y), 38 * density), 38 * density, Offset(x, y))
                drawRoundRect(Color(0xFFFFB84D), Offset(x - 9 * density, y - 14 * density), Size(18 * density, 26 * density), androidx.compose.ui.geometry.CornerRadius(6 * density))
                drawRoundRect(Color(0xFF8A4B10), Offset(x - 11 * density, y - 17 * density), Size(22 * density, 4 * density)) } }
        "skyclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF5DB3F0), Color(0xFFB9E2FF), Color(0xFFE8F6FF))))
            listOf(Triple(.1f, .25f, 1.1f), Triple(.5f, .18f, .8f), Triple(.35f, .55f, 1.3f), Triple(.8f, .45f, 1f), Triple(.65f, .8f, 1.2f)).forEachIndexed { i, (fx, fy, k) ->
                val x = (((fx + t * (.12f + i * .03f)) % 1.3f) - .15f) * w; cloud(x, h * fy, k, Color.White, Color(0x44A6C8E6)) } }
        "sunclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF7CC4F2), Color(0xFFD7F0FF))))
            val c = Offset(w * .3f, h * .32f)
            for (k in 0..11) { val a = (k * PI / 6 + t * PI / 3).toFloat(); drawLine(Color(0x66FFE38A), Offset(c.x + cos(a) * h * .2f, c.y + sin(a) * h * .2f), Offset(c.x + cos(a) * h * .34f, c.y + sin(a) * h * .34f), 4 * density) }
            drawCircle(Color(0xFFFFE38A), h * .16f, c)
            listOf(Triple(.4f, .42f, 1.2f), Triple(.8f, .25f, .9f), Triple(.65f, .7f, 1.3f), Triple(.12f, .78f, 1f)).forEachIndexed { i, (fx, fy, k) ->
                val x = (((fx + t * (.1f + i * .02f)) % 1.3f) - .15f) * w; cloud(x, h * fy, k, Color.White, Color(0x44A6C8E6)) } }
        "sunsetclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2466), Color(0xFFC0507A), Color(0xFFF59A5A), Color(0xFFFFD08A))))
            drawCircle(Brush.radialGradient(listOf(Color(0xAAFFE0A0), Color(0x00FFE0A0)), Offset(w * .7f, h * .85f), h * .5f), h * .5f, Offset(w * .7f, h * .85f))
            drawCircle(Color(0xFFFFE0A0), h * .15f, Offset(w * .7f, h * .85f))
            listOf(Triple(.15f, .3f, 1.1f), Triple(.55f, .45f, .9f), Triple(.85f, .22f, 1f), Triple(.35f, .7f, 1.2f)).forEachIndexed { i, (fx, fy, k) ->
                val x = (((fx + t * (.08f + i * .02f)) % 1.3f) - .15f) * w; cloud(x, h * fy, k, Color(0xFFFFB8A0), Color(0xFF9A4A7A)) } }
        "snowfall" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFB8D4EE), Color(0xFFEAF4FF))))
            drawPath(Path().apply { moveTo(0f, h * .8f); quadraticBezierTo(w * .4f, h * .66f, w, h * .82f); lineTo(w, h); lineTo(0f, h); close() }, Color.White)
            val r = java.util.Random(19); repeat(60) { val x = r.nextFloat() * w + sin((t * 2 * PI + it).toFloat()) * 6 * density; val y = ((r.nextFloat() + t) % 1f) * h
                drawCircle(Color.White.copy(alpha = .9f), (1.5f + r.nextFloat() * 2.5f) * density, Offset(x, y)) } }
        "cotton" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFCFE0FF), Color(0xFFF0E6FF), Color(0xFFFFE6F0))))
            val cs = listOf(Color(0xFFFFD6EC), Color(0xFFD6E8FF), Color(0xFFE9DCFF), Color(0xFFFFF1D6), Color(0xFFD9FFF0))
            listOf(Triple(.1f, .25f, 1f), Triple(.45f, .18f, 1.2f), Triple(.8f, .3f, .9f), Triple(.28f, .62f, 1.3f), Triple(.72f, .7f, 1.1f)).forEachIndexed { i, (fx, fy, k) ->
                val y = h * fy + sin((t * 2 * PI + i).toFloat()) * 4 * density; val x = (((fx + t * .06f) % 1.3f) - .15f) * w; cloud(x, y, k, cs[i], Color(0x33A08CD0)) } }
        "aboveclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF3F8FE0), Color(0xFF9FD0FF))))
            drawCircle(Color(0x44FFFFFF), h * .32f, Offset(w * .78f, h * .25f)); drawCircle(Color(0xFFFFF6D0), h * .12f, Offset(w * .78f, h * .25f))
            for (k in 0..9) { val x = (((k / 8f + t * .05f) % 1.25f) - .1f) * w; cloud(x, h * (.8f + (k % 2) * .07f), 1.6f, Color.White, Color(0xFFC9DDF0)) } }
        "nightclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF0A1236), Color(0xFF26346E)))); stars(40, 71)
            drawCircle(Color(0x33FFF3C4), h * .3f, Offset(w * .75f, h * .3f)); drawCircle(Color(0xFFFFF3C4), h * .13f, Offset(w * .75f, h * .3f))
            listOf(Triple(.2f, .45f, 1.1f), Triple(.6f, .58f, .9f), Triple(.9f, .78f, 1.2f), Triple(.05f, .82f, 1f)).forEachIndexed { i, (fx, fy, k) ->
                val x = (((fx + t * (.07f + i * .02f)) % 1.3f) - .15f) * w; cloud(x, h * fy, k, Color(0xFF5A6AA8), Color(0xFF2E3A72)) } }
        "goldclouds" -> { drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE7A8), Color(0xFFFFF6DC))))
            drawCircle(Brush.radialGradient(listOf(Color(0xCCFFFFFF), Color(0x00FFFFFF)), Offset(w * .5f, h * .3f), h * .6f), h * .6f, Offset(w * .5f, h * .3f))
            listOf(Triple(.15f, .4f, 1.1f), Triple(.5f, .72f, 1.3f), Triple(.85f, .36f, 1f)).forEachIndexed { i, (fx, fy, k) ->
                val x = (((fx + t * (.06f + i * .02f)) % 1.3f) - .15f) * w; cloud(x, h * fy, k, Color(0xFFFFF4D6), Color(0xFFE9C46A)) }
            val r = java.util.Random(77); repeat(40) { val tw = .4f + .6f * ((sin((t * 2 * PI + r.nextFloat() * 6.28).toFloat()) + 1) / 2)
                drawCircle(Color.White.copy(alpha = tw), (1 + r.nextFloat() * 2f) * density, Offset(r.nextFloat() * w, r.nextFloat() * h)) } }
        "galaxy" -> { drawRect(Brush.radialGradient(listOf(Color(0xFF3A1E6A), Color(0xFF0A0518)), Offset(w * .5f, h * .45f), w * .7f)); stars(60, 23)
            val r = java.util.Random(5); repeat(140) { val a = r.nextFloat() * 6.28f + t * 1.2f; val d = r.nextFloat(); val x = w * .5f + cos(a + d * 4) * d * w * .45f; val y = h * .45f + sin(a + d * 4) * d * h * .35f
                drawCircle(listOf(Color(0xAAFFB0F0), Color(0xAA9AD8FF), Color(0xAAFFFFFF))[it % 3], (1 + r.nextFloat()) * density, Offset(x, y)) } }
    }
}

/** The friend's stage in its card, with the chosen scene (or picture) behind it. */
@Composable
fun PetStageWithBackdrop(height: Int) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val id = PetBackdrop.chosen
    val calm = com.alzikr.app.pets.PetPerf.reduceMotion
    val tr = rememberInfiniteTransition(label = "scene")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val stamp = Store.prefs.getLong("pet_bg_photo_at", 0)
    val pic = remember(id, stamp) { if (id == "photo") runCatching { BitmapFactory.decodeFile(PetBackdrop.photo(ctx).path)?.asImageBitmap() }.getOrNull() else null }
    Box(Modifier.fillMaxWidth().height((height + 40).dp).clip(L.shape(16.dp))) {
        when {
            pic != null -> Image(pic, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            id.isNotEmpty() && id != "photo" -> Canvas(Modifier.fillMaxSize()) { drawScene(id, if (calm) 0f else t) }
        }
        Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 6.dp)) { PetStage(Modifier.fillMaxWidth(), height = height, paper = Color.Transparent) }
    }
}

/**
 * The friend's card in «صديقي» with the chosen scene (or picture) behind ALL of it; a soft veil at the bottom keeps the
 * words readable. [content] gets true when a scene is on (then its words are light).
 */
@Composable
fun PetSceneCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.(Boolean) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val id = PetBackdrop.chosen
    val calm = com.alzikr.app.pets.PetPerf.reduceMotion
    val tr = rememberInfiniteTransition(label = "scene")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val stamp = Store.prefs.getLong("pet_bg_photo_at", 0)
    val pic = remember(id, stamp) { if (id == "photo") runCatching { BitmapFactory.decodeFile(PetBackdrop.photo(ctx).path)?.asImageBitmap() }.getOrNull() else null }
    val on = pic != null || (id.isNotEmpty() && id != "photo")
    Box(Modifier.fillMaxWidth().clip(L.shape(18.dp)).background(P.card)) {
        if (pic != null) Image(pic, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        else if (on) Canvas(Modifier.matchParentSize()) { drawScene(id, if (calm) 0f else t) }
        if (on) Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x55000000), Color(0xAA000000)))))
        Column(Modifier.fillMaxWidth().padding(12.dp)) { content(on) }
    }
}

/** A soft glow behind the friend at the top of the app (pulsing gently; still when motion is reduced). */
@Composable
fun BoxScope.PetGlow() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val g = PetBackdrop.glow
    if (g.isEmpty()) return
    val calm = com.alzikr.app.pets.PetPerf.reduceMotion
    val tr = rememberInfiniteTransition(label = "glow")
    val pulse by tr.animateFloat(.75f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    val hue by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "h")
    val color = if (g == "rainbow") Color.hsv(if (calm) 40f else hue, .65f, 1f) else PetBackdrop.glows.firstOrNull { it.first == g }?.second ?: return
    val k = if (calm) 1f else pulse
    Canvas(Modifier.matchParentSize()) {
        val c = Offset(size.width / 2, size.height * .58f); val r = minOf(size.width, size.height) * .62f * k
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .55f), color.copy(alpha = .18f), Color.Transparent), c, r), r, c)
    }
}

/** Choosing the card's scene (or a picture from the phone) and the glow. */
@Composable
fun PetLookSheet(close: () -> Unit) {
    AppSheet(onDismissRequest = close) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PetDecorContent(null)
        }
    }
}

/** The friend's decorations: [part] "card" (its card's background), "widget" (the home-screen widget: frame and background), "glow", or null for all. */
@Composable
fun PetDecorContent(part: String?) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) Thread { val ok = PetBackdrop.setPhoto(ctx, uri); android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (!ok) android.widget.Toast.makeText(ctx, "تعذّر فتح الصورة", android.widget.Toast.LENGTH_SHORT).show() } }.start()
    }
    val pickWidget = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) Thread { val ok = PetBackdrop.setWidgetPhoto(ctx, uri); android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (ok) PetWidget.update(ctx) else android.widget.Toast.makeText(ctx, "تعذّر فتح الصورة", android.widget.Toast.LENGTH_SHORT).show() } }.start()
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (part == null || part == "card") {
            Text("🖼 خلفية بطاقة ${Pet.name.ifBlank { "صديقك" }}", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Sub("ثمانٍ مفتوحة لك، وثمانٍ تفتحها نقاط الصداقة ❤، وصورة من هاتفك عند ${Quran.digits(PetBackdrop.PHOTO_BOND)} ❤.", size = 12.sp)
            val items = listOf(PetBackdrop.Scene("", "بلا خلفية", 0)) + PetBackdrop.scenes.sortedBy { it.bond }
            items.chunked(3).forEach { row ->
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { s ->
                        val open = PetBackdrop.open(s); val on = PetBackdrop.chosen == s.id
                        Column(Modifier.weight(1f).clip(L.shape(12.dp)).border(if (on) 3.dp else 1.dp, if (on) P.accent else P.line, L.shape(12.dp))
                            .clickable { if (open) PetBackdrop.chosen = s.id else android.widget.Toast.makeText(ctx, "تُفتح عند ${Quran.digits(s.bond)} ❤ (معك ${Quran.digits(Progress.bondPoints)})", android.widget.Toast.LENGTH_SHORT).show() }) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1.4f).background(P.card), contentAlignment = Alignment.Center) {
                                if (s.id.isNotEmpty()) Canvas(Modifier.fillMaxSize()) { drawScene(s.id, .2f) }
                                if (!open) Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) { Text("🔒 ${Quran.digits(s.bond)} ❤", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                            Text(s.name, color = if (on) P.accent else P.ink, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp))
                        }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
            Choice(if (!PetBackdrop.photoOpen()) "🔒 📷 صورة من هاتفك · ${Quran.digits(PetBackdrop.PHOTO_BOND)} ❤" else if (PetBackdrop.chosen == "photo") "📷 صورتك ✓ (اختر غيرها)" else "📷 صورة من هاتفك", PetBackdrop.chosen == "photo") {
                if (PetBackdrop.photoOpen()) pick.launch("image/*") else android.widget.Toast.makeText(ctx, "تُفتح الصورة من هاتفك عند ${Quran.digits(PetBackdrop.PHOTO_BOND)} ❤ (معك ${Quran.digits(Progress.bondPoints)})", android.widget.Toast.LENGTH_SHORT).show() }

        }
        if (part == null || part == "widget") {
            Text("🖼 إطار الأداة", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Sub("يحيط بالأداة، ومع الخلفية يصير حولها كبرواز.", size = 12.sp)
            WidgetFramesGrid()
            Text("📱 خلفية أداة «صديقي» على الشاشة الرئيسية", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Sub("خلفيات للأداة غير خلفيات بطاقته، وصورة من هاتفك عند ${Quran.digits(PetBackdrop.PHOTO_BOND)} ❤. «بلا خلفية» يُبقي لون الثيم أو الإطار وحده.", size = 12.sp)
            val wItems = listOf(PetBackdrop.WScene("", "بلا خلفية", 0, false)) + PetBackdrop.widgetScenes.sortedBy { it.bond }
            wItems.chunked(3).forEach { row ->
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { s ->
                        val open = PetBackdrop.open(s); val on = PetBackdrop.widgetBg == s.id
                        Column(Modifier.weight(1f).clip(L.shape(12.dp)).border(if (on) 3.dp else 1.dp, if (on) P.accent else P.line, L.shape(12.dp))
                            .clickable { if (open) { PetBackdrop.widgetBg = s.id; PetWidget.update(ctx) } else android.widget.Toast.makeText(ctx, "تُفتح عند ${Quran.digits(s.bond)} ❤ (معك ${Quran.digits(Progress.bondPoints)})", android.widget.Toast.LENGTH_SHORT).show() }) {
                            Box(Modifier.fillMaxWidth().aspectRatio(2f).background(P.card), contentAlignment = Alignment.Center) {
                                if (s.id.isNotEmpty()) Canvas(Modifier.fillMaxSize()) { drawWidgetScene(s.id) } else Text("🖼", fontSize = 18.sp)
                                if (!open) Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) { Text("🔒 ${Quran.digits(s.bond)} ❤", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                            Text(s.name, color = if (on) P.accent else P.ink, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp))
                        }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
            Choice(if (!PetBackdrop.photoOpen()) "🔒 📷 صورة من هاتفك للأداة · ${Quran.digits(PetBackdrop.PHOTO_BOND)} ❤" else if (PetBackdrop.widgetBg == "photo") "📷 صورتك للأداة ✓ (اختر غيرها)" else "📷 صورة من هاتفك للأداة", PetBackdrop.widgetBg == "photo") {
                if (PetBackdrop.photoOpen()) pickWidget.launch("image/*") else android.widget.Toast.makeText(ctx, "تُفتح الصورة من هاتفك عند ${Quran.digits(PetBackdrop.PHOTO_BOND)} ❤ (معك ${Quran.digits(Progress.bondPoints)})", android.widget.Toast.LENGTH_SHORT).show() }

        }
        if (part == null || part == "glow") {
            Text("✨ وهج حوله في أعلى التطبيق", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Wrap(gap = 8.dp) {
                Choice("بلا وهج", PetBackdrop.glow.isEmpty()) { PetBackdrop.glow = "" }
                PetBackdrop.glows.forEach { (k, c) ->
                    androidx.compose.foundation.layout.Row(Modifier.clip(L.shape(20.dp)).background(if (PetBackdrop.glow == k) c.copy(alpha = .25f) else P.surface).border(1.dp, if (PetBackdrop.glow == k) c else P.line, L.shape(20.dp))
                        .clickable { PetBackdrop.glow = k }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(if (k == "rainbow") Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)) else Brush.linearGradient(listOf(c, c))))
                        Text("  " + (PetBackdrop.glowNames[k] ?: k), color = P.ink, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Suppress("unused") private val keepWidth = Modifier.width(1.dp)
