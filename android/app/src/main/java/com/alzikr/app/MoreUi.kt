package com.alzikr.app

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MoreScreen(openSettings: () -> Unit) {
    var page by remember { mutableStateOf<String?>(null) }
    page?.let { p ->
        Subpage(title = when (p) { "qibla" -> "القبلة"; "month" -> "مواقيت الشهر"; "calendar" -> "التقويم والمناسبات"; "names" -> "أسماء الله الحسنى"; "qada" -> "قضاء الصلوات"; "themes" -> "الثيمات"; else -> "نسخ احتياطي" }, back = { page = null }) {
            when (p) {
                "qibla" -> QiblaCompass()
                "month" -> MonthTable()
                "calendar" -> CalendarPage()
                "names" -> NamesPage()
                "qada" -> QadaPage()
                "themes" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { ModeChoice(); ColorChoice(); ThemeGallery() }
                else -> BackupPage()
            }
        }
        return
    }
    val h = Data.hijri()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel(color = P.card) {
            Sub("اليوم")
            Text("${h.day} ${h.monthName} ${h.year} هـ", color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            nextOccasion()?.let { (o, d) -> Sub("${o.name} ${if (d == 0L) "اليوم" else "بعد $d يومًا"}", color = P.gold) }
        }
        listOf(
            Triple("themes", "🎨  الثيمات", "${Looks.byId(Data.look).name} · ${Looks.all.size} ثيمًا، ولكل ثيم سبعة ألوان"),
            Triple("qibla", "🧭  القبلة", "بوصلة حية باتجاه الكعبة"),
            Triple("month", "📅  مواقيت الشهر", "جدول الصلوات لكل أيام الشهر"),
            Triple("calendar", "🌙  التقويم والمناسبات", "رمضان والعيدان وعرفة وعاشوراء والأيام البيض"),
            Triple("names", "✨  أسماء الله الحسنى", "التسعة والتسعون اسمًا"),
            Triple("qada", "🕌  قضاء الصلوات", "عدّ ما فاتك وما قضيت"),
            Triple("backup", "💾  نسخ احتياطي", "حفظ كل بياناتك في ملف واستعادتها"),
        ).forEach { (id, title, sub) -> MenuRow(title, sub) { page = id } }
        MenuRow("⚙️  الإعدادات", "الموقع والحساب والتنبيهات والمظهر", openSettings)
    }
}

@Composable
private fun MenuRow(title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(P.surface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Sub(sub, size = 12.sp)
        }
        Text("‹", color = P.muted, fontSize = 22.sp)
    }
}

/** A full page over the tabs, with a title and a back arrow. */
@Composable
fun Subpage(title: String, back: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize().background(P.bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("→", color = P.accent, fontSize = 22.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable(onClick = back).padding(horizontal = 12.dp, vertical = 6.dp))
            Text(title, color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

private fun nextOccasion(): Pair<Content.Occasion, Long>? =
    Content.occasions.mapNotNull { o -> Data.nextDate(o.month, o.day)?.let { o to ChronoUnit.DAYS.between(LocalDate.now(), it) } }.minByOrNull { it.second }

// ------------------------------------------------------------------------------- qibla

@Composable
private fun QiblaCompass() {
    val ctx = LocalContext.current
    var heading by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var accuracyLow by remember { mutableStateOf(false) }
    val qibla = PrayTimes.qibla(Store.lat, Store.lng).toFloat()
    val declination = remember { GeomagneticField(Store.lat.toFloat(), Store.lng.toFloat(), 0f, System.currentTimeMillis()).declination }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) { hasSensor = false; return@DisposableEffect onDispose { } }
        val rot = FloatArray(9); val ori = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, ori)
                val az = (Math.toDegrees(ori[0].toDouble()).toFloat() + declination + 360f) % 360f
                // Smooth: move a fraction of the way, the short way round.
                val diff = ((az - heading + 540f) % 360f) - 180f
                heading = (heading + diff * 0.2f + 360f) % 360f
            }
            override fun onAccuracyChanged(s: Sensor, a: Int) { accuracyLow = a < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM }
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    val turn = ((qibla - heading + 540f) % 360f) - 180f // -180..180: how far to turn
    val aligned = abs(turn) < 4f
    val vibrator = remember { ctx.getSystemService(Vibrator::class.java) }
    var buzzed by remember { mutableStateOf(false) }
    if (aligned && !buzzed) { buzzed = true; runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(40, 160)) } } else if (!aligned && abs(turn) > 10f) buzzed = false

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!hasSensor) { Sub("لا يوجد حساس بوصلة في هذا الهاتف. القبلة ${qibla.roundToInt()}° من الشمال."); return@Column }
        Text(if (aligned) "أنت باتجاه القبلة ✓" else if (turn > 0) "استدر يمينًا ${abs(turn).roundToInt()}°" else "استدر يسارًا ${abs(turn).roundToInt()}°",
            color = if (aligned) P.accent else P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        val ring = P.line; val ink = P.muted; val arrow = if (aligned) P.accent else P.gold; val north = P.danger
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(290.dp)) {
                val r = size.minDimension / 2; val c = center
                drawCircle(ring, r - 4.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                // The dial turns with the phone: north mark and ticks.
                rotate(-heading, c) {
                    for (i in 0 until 72) {
                        val len = if (i % 18 == 0) 16.dp.toPx() else if (i % 6 == 0) 10.dp.toPx() else 5.dp.toPx()
                        rotate(i * 5f, c) { drawLine(if (i == 0) north else ink, Offset(c.x, c.y - r + 6.dp.toPx()), Offset(c.x, c.y - r + 6.dp.toPx() + len), strokeWidth = if (i % 18 == 0) 3.dp.toPx() else 1.5.dp.toPx()) }
                    }
                    // Qibla arrow on the dial.
                    rotate(qibla, c) {
                        val p = Path().apply {
                            moveTo(c.x, c.y - r + 26.dp.toPx())
                            lineTo(c.x - 16.dp.toPx(), c.y - r + 62.dp.toPx())
                            lineTo(c.x + 16.dp.toPx(), c.y - r + 62.dp.toPx()); close()
                        }
                        drawPath(p, arrow)
                        drawLine(arrow, c, Offset(c.x, c.y - r + 60.dp.toPx()), strokeWidth = 5.dp.toPx())
                    }
                }
                drawCircle(arrow, 7.dp.toPx(), c)
                // Fixed marker: the top of the phone.
                drawLine(P_INK_MARK, Offset(c.x, 0f), Offset(c.x, 14.dp.toPx()), strokeWidth = 4.dp.toPx())
            }
            Text("🕋", fontSize = 26.sp)
        }
        Sub("القبلة ${qibla.roundToInt()}° من الشمال · وجّه أعلى الهاتف حتى يصل السهم إلى الأعلى", Modifier.fillMaxWidth(), size = 13.sp)
        if (accuracyLow) Sub("البوصلة تحتاج معايرة: حرّك الهاتف على شكل الرقم 8 عدة مرات، وابتعد عن المعادن والمغناطيس.", color = P.gold, size = 12.sp)
    }
}
private val P_INK_MARK = androidx.compose.ui.graphics.Color(0xFF8899AA)

// ------------------------------------------------------------------------------- month table

@Composable
private fun MonthTable() {
    var month by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    val ar = Locale("ar")
    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Choice("‹ السابق", false) { month = month.minusMonths(1) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM y", ar)), color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Choice("التالي ›", false) { month = month.plusMonths(1) }
        }
        val cols = listOf("اليوم") + Prayer.entries.map { it.ar }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { cols.forEachIndexed { i, t -> Text(t, color = P.muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(if (i == 0) 1.3f else 1f)) } }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items((0 until month.lengthOfMonth()).toList()) { i ->
                val d = month.plusDays(i.toLong()); val t = Store.times(d); val today = d == LocalDate.now()
                val h = Data.hijri(d)
                Row(Modifier.fillMaxWidth().clip(L.shape(8.dp)).background(if (today) P.accent.copy(alpha = .14f) else if (i % 2 == 0) P.surface else P.bg).padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${d.dayOfMonth} ${d.format(DateTimeFormatter.ofPattern("EEE", ar))}", color = if (today) P.accent else P.ink, fontSize = 12.sp)
                        Text("${h.day} ${h.monthName.take(5)}", color = P.muted, fontSize = 9.sp)
                    }
                    Prayer.entries.forEach { p -> Text(Alerts.timeText(t.getValue(p)).replace(" ص", "").replace(" م", ""), color = if (p.alertable) P.ink else P.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------- calendar

@Composable
private fun CalendarPage() {
    val h = Data.hijri()
    val ar = Locale("ar")
    val list = Content.occasions.mapNotNull { o -> Data.nextDate(o.month, o.day)?.let { o to it } }.sortedBy { it.second }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Panel(color = P.card) {
                Text("${h.day} ${h.monthName} ${h.year} هـ", color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Sub(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM y", ar)))
                val white = (13..15).map { d -> Data.nextDate(h.month, d) ?: Data.nextDate(h.month % 12 + 1, d) }.filterNotNull().firstOrNull()
                if (white != null) Sub("الأيام البيض القادمة: ${white.format(DateTimeFormatter.ofPattern("d MMMM", ar))} (13–15 من الشهر الهجري)", color = P.gold, size = 12.sp)
                Sub("يُحسب التاريخ بتقويم أم القرى. إن اختلف عن رؤية بلدك فعدّله من الإعدادات.", size = 11.sp)
            }
        }
        items(list) { (o, date) ->
            val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(o.name, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Sub("${o.day} ${Data.months[o.month - 1]} · ${date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", ar))}" + if (o.note.isNotEmpty()) "\n${o.note}" else "", size = 12.sp)
                }
                Text(if (days == 0L) "اليوم" else "$days يوم", color = if (days <= 7) P.gold else P.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ------------------------------------------------------------------------------- names

@Composable
private fun NamesPage() {
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(Content.names) { i, n ->
            Column(Modifier.clip(L.shape(14.dp)).background(if (i == 0) P.card else P.surface).padding(vertical = 14.dp, horizontal = 6.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${i + 1}", color = P.gold, fontSize = 11.sp)
                Text(n, color = P.ink, fontSize = (17 * Data.textScale).sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
        }
    }
}

// ------------------------------------------------------------------------------- qada

@Composable
private fun QadaPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Sub("سجّل عدد الصلوات التي فاتتك، وأنقص منها كلما قضيت واحدة.", size = 13.sp)
        Prayer.entries.filter { it.alertable }.forEach { p ->
            Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.ar, color = P.ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Choice("−1 قضيت", false) { Data.setQada(p, Data.qada(p) - 1) }
                Text("${Data.qada(p)}", color = if (Data.qada(p) > 0) P.gold else P.muted, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
                Choice("+1", false) { Data.setQada(p, Data.qada(p) + 1) }
            }
        }
        val total = Prayer.entries.filter { it.alertable }.sumOf { Data.qada(it) }
        Sub(if (total == 0) "لا شيء عليك مسجّل. تقبل الله منك." else "المجموع: $total صلاة", color = if (total == 0) P.accent else P.ink)
    }
}

// ------------------------------------------------------------------------------- backup

@Composable
private fun BackupPage() {
    val ctx = LocalContext.current
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(Data.export().toByteArray()) } }.isSuccess
        Toast.makeText(ctx, if (ok) "حُفظت النسخة الاحتياطية" else "تعذّر الحفظ", Toast.LENGTH_LONG).show()
    }
    val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = runCatching { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()
        val ok = json != null && Data.import(json)
        if (ok) Alerts.reschedule(ctx)
        Toast.makeText(ctx, if (ok) "استُعيدت بياناتك" else "هذا الملف ليس نسخة من ALzikr", Toast.LENGTH_LONG).show()
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Sub("تُحفظ كل بياناتك في ملف واحد: الأوراد والأذكار المخصصة، المهام والملاحظات، العادات والختمة، سجل الصلوات والإعدادات. احفظه في Google Drive أو في الهاتف.", size = 13.sp)
        Action("حفظ نسخة احتياطية", Modifier.fillMaxWidth()) { save.launch("ALzikr-${LocalDate.now()}.json") }
        Action("استعادة من ملف", Modifier.fillMaxWidth(), filled = false) { load.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }
        Sub("الاستعادة تستبدل كل ما في التطبيق الآن بما في الملف.", color = P.gold, size = 12.sp)
    }
}

