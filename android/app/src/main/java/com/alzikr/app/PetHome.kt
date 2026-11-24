package com.alzikr.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alzikr.app.pets.PetState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A request from the interactions menu to the friend on screen (PetStage picks it up). */
object PetActions {
    var request by mutableStateOf<Pair<PetState, Long>?>(null)
    fun ask(s: PetState) { request = s to System.currentTimeMillis() }
}

/**
 * Quiet time: while the Quran or a hadith is being read, the friend in the app stays still and silent
 * (a counter, so nested readers work). The switch is «هادئ أثناء القراءة» (on by default).
 */
object PetQuiet {
    var readers by mutableIntStateOf(0)
    val active: Boolean get() = readers > 0 && Store.prefs.getBoolean("pet_quiet_reading", true)
}
/** Call inside a reader: the friend is quiet while it is on screen. */
@Composable
fun QuietWhileReading() {
    DisposableEffect(Unit) { PetQuiet.readers++; onDispose { PetQuiet.readers = (PetQuiet.readers - 1).coerceAtLeast(0) } }
}

// ------------------------------------------------------------------------------------------------ interactions

/** «كلّمني، العب معي، اجلس، نم، حيّني»: for fun and friendship only (❤, capped daily), never experience. */
@Composable
fun PetInteractions() {
    val acts = listOf(
        Triple("💬", "كلّمني", PetState.TALK), Triple("🧶", "العب معي", PetState.PLAY), Triple("🪑", "اجلس", PetState.SIT),
        Triple("💤", "نم", PetState.SLEEP), Triple("👋", "حيّني", PetState.WAVE),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        acts.forEach { (icon, label, st) ->
            Column(Modifier.weight(1f).clip(L.shape(14.dp)).background(P.card).border(1.dp, P.line, L.shape(14.dp))
                .clickable { PetActions.ask(st); Progress.bond("act_${st.id}_" + System.currentTimeMillis() / 1_800_000L) }
                .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, fontSize = 20.sp)
                Text(label, color = P.ink, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ seasons

/**
 * Seasons for the friend (Hijri): Ramadan brings its own words (room decorations will come with the room's new design); seasonal looks in packs use the
 * same windows (Skin.season). "pet_season_preview" (a test switch) shows a season out of its time.
 */
object PetSeason {
    val ramadan = com.alzikr.app.pets.Season(9, 1, 9, 30)
    fun now(): String? {
        Store.prefs.getString("pet_season_preview", null)?.takeIf { it.isNotBlank() }?.let { return it }
        val h = Data.hijri()
        return if (h.year > 0 && ramadan.contains(h.month, h.day)) "ramadan" else null
    }
    val isRamadan: Boolean get() = now() == "ramadan"
    /** Plain greetings for the season (no quotations). */
    fun line(n: Int): String? = if (!isRamadan) null else listOf(
        "رمضان مبارك! 🌙", "تقبّل الله صيامك وقيامك", "هل قرأت وردك اليوم؟ رمضان شهر القرآن", "لا تنسَ السحور، ففيه بركة", "اللهم بلّغنا ليلة القدر",
    )[Math.floorMod(n, 5)]
}

// ------------------------------------------------------------------------------------------------ the room

/** Things for the friend's room, opened by the level. Drawn by code (no outside art); each can be shown or put away. */
data class RoomItem(val id: String, val icon: String, val name: String, val level: Int)
object PetRoomItems {
    val items = listOf(
        RoomItem("rug", "🟫", "سجادة مزخرفة", 1), RoomItem("cushion", "🛋", "وسادة مريحة", 2), RoomItem("plant", "🪴", "نبتة", 3),
        RoomItem("lantern", "🏮", "فانوس", 4), RoomItem("shelf", "📚", "رف كتب", 5), RoomItem("yarn", "🧶", "كرة صوف", 6),
        RoomItem("clock", "🕰", "ساعة حائط", 7), RoomItem("pattern", "✴", "زخرفة هندسية", 8), RoomItem("garden", "🌷", "حديقة صغيرة عند النافذة", 10),
        RoomItem("stars", "✨", "نجوم السقف", 12),
    )
    fun owned(i: RoomItem) = Progress.level() >= i.level
    fun shown(i: RoomItem) = owned(i) && i.id !in (Store.prefs.getStringSet("pet_room_hidden", emptySet()) ?: emptySet())
    fun toggle(i: RoomItem, show: Boolean) {
        val s = (Store.prefs.getStringSet("pet_room_hidden", emptySet()) ?: emptySet()).toMutableSet()
        if (show) s -= i.id else s += i.id
        Store.prefs.edit().putStringSet("pet_room_hidden", s).apply(); Store.bump()
    }
}

/** «غرفة صديقي»: a small room whose window follows the real time of day, with the items the friend has earned. */
@Composable
fun PetRoom() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val h = LocalTime.now().hour
    // Morning, day, evening, night: the sky in the window and the light on the wall.
    val (sky1, sky2, wall) = when (h) {
        in 5..9 -> Triple(Color(0xFFFFD9A8), Color(0xFFB8E0F5), Color(0xFFF4E6D4))
        in 10..15 -> Triple(Color(0xFF7CC4F0), Color(0xFFCFEFFF), Color(0xFFF1EBDD))
        in 16..18 -> Triple(Color(0xFFF2A65A), Color(0xFF8E6FB5), Color(0xFFE9D6C2))
        else -> Triple(Color(0xFF0E1A3A), Color(0xFF2A2F5E), Color(0xFF3A3550))
    }
    val night = h !in 5..18
    val show = { id: String -> PetRoomItems.items.firstOrNull { it.id == id }?.let { PetRoomItems.shown(it) } == true }
    BoxWithConstraints(Modifier.fillMaxWidth().height(300.dp).clip(L.shape(18.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val W = size.width; val H = size.height; val floorY = H * .68f
            drawRect(Brush.verticalGradient(listOf(wall, wall.copy(alpha = .92f))), size = Size(W, floorY))
            if (show("pattern")) {
                // An eight-pointed star repeated faintly on the wall.
                val c = if (night) Color.White.copy(alpha = .07f) else Color(0xFF1F7A5A).copy(alpha = .08f)
                var y = 30f
                while (y < floorY - 20) { var x = 30f; while (x < W) { star8(this, Offset(x, y), 16f, c); x += 70f }; y += 70f }
            }
            // Window with the real sky.
            val wx = W * .58f; val wy = H * .1f; val ww = W * .32f; val wh = H * .36f
            drawRoundRect(Brush.verticalGradient(listOf(sky1, sky2), startY = wy, endY = wy + wh), Offset(wx, wy), Size(ww, wh), CornerRadius(14f))
            if (night) {
                drawCircle(Color(0xFFF3E7B0), 14f, Offset(wx + ww * .72f, wy + wh * .3f))
                drawCircle(sky1, 12f, Offset(wx + ww * .66f, wy + wh * .26f))   // crescent
                listOf(.2f to .2f, .35f to .55f, .5f to .3f, .15f to .7f, .85f to .75f).forEach { (a, b) -> drawCircle(Color.White.copy(alpha = .8f), 2.2f, Offset(wx + ww * a, wy + wh * b)) }
            } else drawCircle(Color(0xFFFFE07A).copy(alpha = .9f), 18f, Offset(wx + ww * .3f, wy + wh * .32f))
            drawRoundRect(Color(0xFF8A6A4A), Offset(wx, wy), Size(ww, wh), CornerRadius(14f), style = Stroke(6f))
            drawLine(Color(0xFF8A6A4A), Offset(wx + ww / 2, wy), Offset(wx + ww / 2, wy + wh), 4f)
            if (show("stars")) listOf(.1f, .3f, .45f, .8f, .92f).forEachIndexed { i, a -> star8(this, Offset(W * a, 14f + (i % 2) * 14f), 6f, Color(0xFFE7C66B).copy(alpha = .8f)) }
            // Floor and rug.
            drawRect(Brush.verticalGradient(listOf(Color(0xFFB98A5E), Color(0xFF9C7048)), startY = floorY, endY = H), Offset(0f, floorY), Size(W, H - floorY))
            if (show("rug")) {
                drawOval(Color(0xFF8E2F3C), Offset(W * .18f, floorY + (H - floorY) * .28f), Size(W * .64f, (H - floorY) * .58f))
                drawOval(Color(0xFFE7C66B), Offset(W * .23f, floorY + (H - floorY) * .36f), Size(W * .54f, (H - floorY) * .42f), style = Stroke(3f))
            }
            if (night) drawRect(Color(0xFF000814).copy(alpha = .25f), size = size)
        }
        val wpx = maxWidth
        // Furniture and things, placed around the room.
        @Composable fun item(id: String, x: Float, y: Float, sizeSp: Int) { if (show(id)) Text(PetRoomItems.items.first { it.id == id }.icon, fontSize = sizeSp.sp, modifier = Modifier.offset(wpx * x, 300.dp * y)) }
        item("shelf", .06f, .14f, 34); item("clock", .38f, .08f, 26); item("lantern", .44f, .3f, 26)
        item("garden", .62f, .42f, 22); item("plant", .86f, .5f, 34); item("cushion", .06f, .6f, 34); item("yarn", .72f, .78f, 22)
        // The friend lives on the rug.
        Box(Modifier.fillMaxWidth().height(130.dp).align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
            PetStage(Modifier.fillMaxWidth(), height = 118, paper = Color.Transparent)
        }
    }
}
private fun star8(d: androidx.compose.ui.graphics.drawscope.DrawScope, c: Offset, r: Float, col: Color) {
    val p = Path()
    for (k in 0 until 16) {
        val a = Math.PI * k / 8; val rr = if (k % 2 == 0) r else r * .55f
        val x = c.x + (rr * kotlin.math.cos(a)).toFloat(); val y = c.y + (rr * kotlin.math.sin(a)).toFloat()
        if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); d.drawPath(p, col)
}

// ------------------------------------------------------------------------------------------------ the bag

/** «حقيبتي»: what the friend has earned (room things, widget frames, rare looks, badges) and what is still locked. */
@Composable
fun PetBag() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    Panel(pad = 14.dp) {
        Title("أغراض الغرفة")
        PetRoomItems.items.forEach { i ->
            val owned = PetRoomItems.owned(i)
            SettingRow("${if (owned) i.icon else "🔒"} ${i.name}", if (owned) null else "يُفتح في المستوى ${Quran.digits(i.level)}") {
                if (owned) Switch(PetRoomItems.shown(i), { PetRoomItems.toggle(i, it) })
            }
        }
    }
    Panel(pad = 14.dp) {
        Title("إطارات الأداة")
        Progress.frames.forEach { f ->
            val open = Progress.level() >= f.level
            SettingRow("${if (open) "🖼" else "🔒"} ${f.name}", if (open) (if (Progress.frame == f.id) "مستعمل الآن ✓" else null) else "المستوى ${Quran.digits(f.level)}") {
                if (open && Progress.frame != f.id) Action("استعمال") { Progress.frame = f.id }
            }
        }
    }
    Panel(pad = 14.dp) {
        val sp = Pet.species
        val looks = sp?.let { PetLooks.of(it) }.orEmpty()
        val open = sp?.let { s -> looks.indices.count { Progress.lookOpen(s, it) } } ?: 0
        Title("المجموعة")
        Sub("أشكال ${sp?.ar ?: "صديقك"}: ${Quran.digits(open)} من ${Quran.digits(looks.size)} · النادرة المفتوحة: ${Quran.digits(sp?.let { s -> looks.withIndex().count { (i, l) -> l.rarity != com.alzikr.app.pets.Rarity.COMMON && Progress.lookOpen(s, i) } } ?: 0)}", size = 13.sp)
        Sub("الأوسمة: ${Quran.digits(Progress.earned().size)} من ${Quran.digits(Progress.badges.size)}", size = 13.sp)
        Action("🎁 كل المكافآت والأوسمة", Modifier.fillMaxWidth().padding(top = 8.dp), filled = false) { Nav.morePage = "rewards"; Nav.go("more") }
    }
}

// ------------------------------------------------------------------------------------------------ the album

/** One page of the friendship album: a real moment and its day. */
data class AlbumMoment(val icon: String, val title: String, val date: LocalDate)
object PetAlbum {
    fun moments(): List<AlbumMoment> {
        val out = ArrayList<AlbumMoment>()
        val zone = ZoneId.systemDefault()
        fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        if (Pet.since > 0) {
            val start = day(Pet.since)
            out += AlbumMoment("🐾", "اليوم الذي صار فيه ${Pet.name} صديقك", start)
            listOf(7 to "أسبوعنا الأول", 30 to "شهر معًا", 100 to "مئة يوم معًا", 365 to "سنة معًا").forEach { (d, t) ->
                if (Pet.daysTogether() >= d) out += AlbumMoment("🎈", t, start.plusDays(d - 1L))
            }
        }
        com.alzikr.app.pets.PetMemory.decode(Store.prefs.getString("pet_memory", null)).forEach { e ->
            when (e.kind) {
                "level" -> out += AlbumMoment("✨", "المستوى ${Quran.digits(e.value)}", day(e.at))
                "khatma" -> out += AlbumMoment("📗", if (e.value <= 1) "أول ختمة لنا" else "الختمة رقم ${Quran.digits(e.value)}", day(e.at))
                "streak" -> out += AlbumMoment("🔥", "${Quran.digits(e.value)} أيام طيبة متتالية", day(e.at))
            }
        }
        Progress.earned().forEach { (id, d) -> Progress.badges.firstOrNull { it.id == id }?.let { b -> runCatching { out += AlbumMoment(b.icon, "وسام «${b.name}»", LocalDate.parse(d)) } } }
        return out.distinctBy { it.title }.sortedByDescending { it.date }
    }

    /** A moment as a picture to share: the friend, the moment and its date only (no worship details). */
    fun share(c: Context, m: AlbumMoment) {
        val sp = Pet.species ?: return
        val pal = ThemeColors.of(c)
        val w = 1080; val h = 1080
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(bmp)
        cv.drawColor(pal.surface.toArgb())
        val frame = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = pal.gold.toArgb(); style = android.graphics.Paint.Style.STROKE; strokeWidth = 5f }
        cv.drawRoundRect(50f, 50f, w - 50f, h - 50f, 40f, 40f, frame)
        PetWidget.frameBitmap(c, sp, .3f, 420)?.let { cv.drawBitmap(it, (w - 420) / 2f, 130f, null) }
        val t = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textAlign = android.graphics.Paint.Align.CENTER }
        t.textSize = 90f; cv.drawText(m.icon, w / 2f, 660f, t)
        t.color = pal.ink.toArgb(); t.textSize = 56f; t.isFakeBoldText = true
        cv.drawText(m.title, w / 2f, 770f, t)
        t.isFakeBoldText = false; t.textSize = 38f; t.color = pal.muted.toArgb()
        cv.drawText("${Pet.name} · ${m.date}", w / 2f, 840f, t)
        t.textSize = 30f; cv.drawText("ALzikr", w / 2f, h - 90f, t)
        val dir = java.io.File(c.cacheDir, "share").apply { mkdirs() }
        val f = java.io.File(dir, "alzikr-moment.png")
        f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(c, c.packageName + ".files", f)
        c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "مشاركة الذكرى").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** «ذكرياتنا»: the friendship's moments, newest first; each can be shared as a picture. */
@Composable
fun PetAlbumPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val ms = PetAlbum.moments()
    if (ms.isEmpty()) { Sub("تظهر هنا لحظاتكما: يوم صار صديقك، وأول ختمة، والمستويات، والأوسمة، والأيام الطيبة.", size = 13.sp); return }
    Sub("لحظاتكما معًا. اضغط «مشاركة» لتشارك صورة فيها اللحظة وتاريخها فقط.", size = 12.sp)
    ms.forEach { m ->
        Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.card).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(m.icon, fontSize = 28.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
            Column(Modifier.weight(1f)) {
                Text(m.title, color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Sub(m.date.toString(), size = 12.sp)
            }
            Text("مشاركة", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { PetAlbum.share(ctx, m) }.padding(6.dp))
        }
    }
}
