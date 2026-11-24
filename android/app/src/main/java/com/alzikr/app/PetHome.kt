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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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

/** «كلّمني، العب معي، تمشَّ، نم» (the four states): for fun and friendship only (❤, capped daily), never experience. */
@Composable
fun PetInteractions() {
    val acts = listOf(
        Triple("💬", "كلّمني", PetState.IDLE), Triple("🧶", "العب معي", PetState.HAPPY), Triple("🐾", "تمشَّ", PetState.WALK),
        Triple("💤", "نم", PetState.SLEEP),
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

/**
 * The friend's home, painted by ALzikr Pet Studio (assets/pets/originals/room.json): one room per animal, furniture
 * opened by the level (one special piece per animal), and upgrades (string lights at 5, a garden canopy at 10).
 * Each piece can be shown or put away from the bag. Positions are percentages of a 640×440 room.
 */
data class RoomItem(val id: String, val name: String, val level: Int, val only: List<String>?, val x: Float, val y: Float, val scale: Float, val size: Float, val path: String) {
    val icon: String get() = when (id) { "floor" -> "🟫"; "bed" -> "🛏"; "toy" -> "🧶"; "plant" -> "🪴"; "window", "curtain" -> "🪟"; "lamp" -> "🏮"; "table" -> "📚"
        "print" -> "🖼"; "garden" -> "🌷"; "mobile" -> "✨"; "cushion" -> "🛋"; else -> "⭐" }
}
object PetRoomItems {
    /** [stages]: a home that changes with the level (legendary friends): level → (name, picture). */
    class Room(val homes: Map<String, Pair<String, String>>, val items: List<RoomItem>, val upgrades: List<Pair<Int, List<String>>>,
               val stages: Map<String, List<Triple<Int, String, String>>> = emptyMap())
    val room: Room by lazy { listOf("originals/room.json", "legendary/room.json").mapNotNull(::read).reduce { a, b ->
        Room(a.homes + b.homes, a.items + b.items, a.upgrades + b.upgrades, a.stages + b.stages) } }
    private fun read(path: String): Room? {
        val o = runCatching { org.json.JSONObject(com.alzikr.app.pets.PetPacks.open("room", path)!!.use { it.readBytes().toString(Charsets.UTF_8) }) }.getOrNull()
        return if (o == null) null else {
            val h = o.getJSONObject("homes"); val f = o.getJSONArray("furniture"); val u = o.getJSONArray("upgrades")
            Room(h.keys().asSequence().associateWith { k -> h.getJSONObject(k).let { it.optString("name") to it.getString("path") } },
                (0 until f.length()).map { f.getJSONObject(it) }.map { j -> RoomItem(j.getString("id"), j.optString("name"), j.optInt("level", 1),
                    j.optJSONArray("only")?.let { a -> (0 until a.length()).map { a.getString(it) } }, j.getDouble("x").toFloat(), j.getDouble("y").toFloat(),
                    j.getDouble("scale").toFloat(), j.getDouble("size").toFloat(), j.getString("path")) },
                (0 until u.length()).map { u.getJSONObject(it) }.map { j -> j.getInt("level") to j.getJSONArray("layers").let { a -> (0 until a.length()).map { a.getString(it) } } },
                h.keys().asSequence().mapNotNull { k -> h.getJSONObject(k).optJSONArray("stages")?.let { a -> k to (0 until a.length()).map { a.getJSONObject(it) }.map { j -> Triple(j.getInt("level"), j.optString("name"), j.getString("path")) } } }.toMap())
        }
    }
    /** The home for this friend now: for a home that grows, the last stage the level has reached. */
    fun home(sp: Species): Pair<String, String>? = room.stages[sp.name]?.lastOrNull { Progress.level() >= it.first }?.let { it.second to it.third } ?: room.homes[sp.name]
    /** The pieces this animal can have (the special one belongs to its own animal). */
    fun items(sp: Species? = Pet.species): List<RoomItem> = room.items.filter { it.only == null || (sp != null && sp.name in it.only) }
    fun owned(i: RoomItem) = Progress.level() >= i.level
    fun shown(i: RoomItem) = owned(i) && i.id !in (Store.prefs.getStringSet("pet_room_hidden", emptySet()) ?: emptySet())
    fun toggle(i: RoomItem, show: Boolean) {
        val s = (Store.prefs.getStringSet("pet_room_hidden", emptySet()) ?: emptySet()).toMutableSet()
        if (show) s -= i.id else s += i.id
        Store.prefs.edit().putStringSet("pet_room_hidden", s).apply(); Store.bump()
    }
    fun layers(): Set<String> = room.upgrades.filter { Progress.level() >= it.first }.flatMap { it.second }.toSet()
    fun homeName(sp: Species?) = sp?.let { home(it)?.first }
}

/** «غرفة صديقي»: the friend's painted home; its light follows the real time of day. */
@Composable
fun PetRoom() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val sp = Pet.species ?: return
    val home = PetRoomItems.home(sp)?.second
    val items = PetRoomItems.items(sp).filter { PetRoomItems.shown(it) }
    val layers = PetRoomItems.layers()
    val paths = (listOfNotNull(home) + items.map { it.path } + (if ("canopy" in layers) listOf("originals/items/curtain.webp") else emptyList()) +
        (if ("planters" in layers) listOf("originals/items/garden.webp") else emptyList())).distinct()
    val pics = androidx.compose.runtime.produceState<Map<String, android.graphics.Bitmap>>(emptyMap(), paths) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { paths.mapNotNull { p -> com.alzikr.app.pets.PetBitmaps.get("room", p, 1024)?.let { p to it } }.toMap() }
    }.value
    val hour = LocalTime.now().hour
    // Daylight, morning/evening glow, or night (the Studio's own light colors).
    val tint = when (hour) { in 7..16 -> null; 5, 6, 17, 18 -> Color(0xFFD48268).copy(alpha = .2f); else -> Color(0xFF142948).copy(alpha = .34f) }
    val paint = remember { android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG) }
    fun drawItem(c: android.graphics.Canvas, b: android.graphics.Bitmap, id: String, x: Float, y: Float, scale: Float, size: Float, W: Float, H: Float) {
        val w = size * scale * W / 640f; val hh = w * b.height / b.width * (if (id == "floor") .55f else 1f)
        val cx = W * x / 100f; val cy = H * y / 100f
        c.drawBitmap(b, null, android.graphics.RectF(cx - w / 2, cy - hh / 2, cx + w / 2, cy + hh / 2), paint)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().clip(L.shape(18.dp))) {
        val roomH = maxWidth * (440f / 640f)
        Box(Modifier.fillMaxWidth().height(roomH)) {
            // Behind the friend: the room, its light, upgrades and the furniture that stands further back.
            Canvas(Modifier.fillMaxSize()) {
                val W = size.width; val H = size.height
                drawIntoCanvas { cv -> val c = cv.nativeCanvas
                    home?.let { pics[it] }?.let { b ->
                        val r = maxOf(W / b.width, H / b.height); val dw = b.width * r; val dh = b.height * r
                        c.drawBitmap(b, null, android.graphics.RectF((W - dw) / 2, (H - dh) * .66f, (W - dw) / 2 + dw, (H - dh) * .66f + dh), paint)
                    }
                }
                tint?.let { drawRect(it) }
                drawIntoCanvas { cv -> val c = cv.nativeCanvas
                    if ("canopy" in layers) pics["originals/items/curtain.webp"]?.let { b -> paint.alpha = 235; c.drawBitmap(b, null, android.graphics.RectF(0f, -H * .035f, W, H * .425f), paint); paint.alpha = 255 }
                    if ("planters" in layers) pics["originals/items/garden.webp"]?.let { b -> listOf(1f, 90f).forEach { x -> drawItem(c, b, "garden", x, 67f, .75f, 123f, W, H) } }
                }
                if ("stringlights" in layers) {
                    val sx = W / 640f; val sy = H / 440f
                    val path = Path().apply { moveTo(0f, 27f * sy); quadraticBezierTo(310f * sx, 93f * sy, 640f * sx, 20f * sy) }
                    drawPath(path, Color(0xFF635541), style = Stroke(1.7f * sx))
                    for (i in 0 until 13) { val t = (i + .45f) / 13f; val x = t * 640f; val y = 27f * (1 - t) * (1 - t) + 186f * t * (1 - t) + 20f * t * t
                        drawOval(Color(0xFFFBDD97), Offset((x - 3.4f) * sx, (y + 3.7f - 5.4f) * sy), Size(6.8f * sx, 10.8f * sy)) }
                }
                drawIntoCanvas { cv -> val c = cv.nativeCanvas
                    items.filter { it.id == "floor" || it.y <= 84f }.sortedBy { if (it.id == "floor") -100f else it.y }
                        .forEach { i -> pics[i.path]?.let { drawItem(c, it, i.id, i.x, i.y, i.scale, i.size, W, H) } }
                }
            }
            // The friend stands on the rug (feet at 87 % of the room's height, like the Studio).
            val petH = roomH * .62f
            Box(Modifier.fillMaxWidth().height(petH).offset(y = roomH * .87f - petH * .9f)) {
                PetStage(Modifier.fillMaxWidth(), height = petH.value.toInt(), paper = Color.Transparent)
            }
            // In front of the friend: the pieces nearest to the viewer.
            Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { cv -> items.filter { it.id != "floor" && it.y > 84f }.forEach { i -> pics[i.path]?.let { drawItem(cv.nativeCanvas, it, i.id, i.x, i.y, i.scale, i.size, size.width, size.height) } } }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ the bag

/** «حقيبتي»: what the friend has earned (room things, widget frames, rare looks, badges) and what is still locked. */
@Composable
fun PetBag() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    Panel(pad = 14.dp) {
        Title("أغراض الغرفة")
        PetRoomItems.homeName(Pet.species)?.let { Sub("بيت صديقك: $it", size = 13.sp) }
        PetRoomItems.items().forEach { i ->
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
