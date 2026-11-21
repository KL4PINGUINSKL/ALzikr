package com.alzikr.app

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

data class City(val name: String, val lat: Double, val lng: Double, val method: Method)

val CITIES = listOf(
    City("مكة المكرمة", 21.4225, 39.8262, Method.MAKKAH), City("المدينة المنورة", 24.4672, 39.6024, Method.MAKKAH),
    City("الرياض", 24.7136, 46.6753, Method.MAKKAH), City("جدة", 21.4858, 39.1925, Method.MAKKAH),
    City("القاهرة", 30.0444, 31.2357, Method.EGYPT), City("الإسكندرية", 31.2001, 29.9187, Method.EGYPT),
    City("عمّان", 31.9454, 35.9284, Method.JORDAN), City("القدس", 31.7683, 35.2137, Method.JORDAN),
    City("دمشق", 33.5138, 36.2765, Method.EGYPT), City("بيروت", 33.8938, 35.5018, Method.EGYPT),
    City("بغداد", 33.3152, 44.3661, Method.MWL), City("الكويت", 29.3759, 47.9774, Method.KUWAIT),
    City("دبي", 25.2048, 55.2708, Method.DUBAI), City("أبوظبي", 24.4539, 54.3773, Method.DUBAI),
    City("الدوحة", 25.2854, 51.5310, Method.QATAR), City("المنامة", 26.2285, 50.5860, Method.MAKKAH),
    City("مسقط", 23.5880, 58.3829, Method.MAKKAH), City("صنعاء", 15.3694, 44.1910, Method.MWL),
    City("الخرطوم", 15.5007, 32.5599, Method.EGYPT), City("طرابلس", 32.8872, 13.1913, Method.EGYPT),
    City("تونس", 36.8065, 10.1815, Method.MWL), City("الجزائر", 36.7538, 3.0588, Method.MWL),
    City("الرباط", 34.0209, -6.8416, Method.MWL), City("إسطنبول", 41.0082, 28.9784, Method.TURKEY),
    City("برلين", 52.5200, 13.4050, Method.MWL), City("لندن", 51.5074, -0.1278, Method.MWL),
)

/** The prayer that is due now (the last one that started) and the next one, with their epoch times. */
data class Window(val current: Prayer?, val next: Prayer, val nextAt: Long, val prevAt: Long)

fun window(now: LocalDateTime = LocalDateTime.now()): Window {
    val zone = ZoneId.systemDefault()
    fun at(d: LocalDate, m: Int) = d.atStartOfDay(zone).plusMinutes(m.toLong()).toInstant().toEpochMilli()
    val today = now.toLocalDate()
    val t = Store.times(today)
    val nowMs = now.atZone(zone).toInstant().toEpochMilli()
    val order = Prayer.entries
    val next = order.firstOrNull { at(today, t.getValue(it)) > nowMs }
    return if (next != null) {
        val i = order.indexOf(next)
        val prev = if (i > 0) order[i - 1] else null
        val prevAt = if (prev != null) at(today, t.getValue(prev)) else at(today.minusDays(1), Store.times(today.minusDays(1)).getValue(Prayer.ISHA))
        Window(prev ?: Prayer.ISHA, next, at(today, t.getValue(next)), prevAt)
    } else {
        val tomorrow = today.plusDays(1)
        Window(Prayer.ISHA, Prayer.FAJR, at(tomorrow, Store.times(tomorrow).getValue(Prayer.FAJR)), at(today, t.getValue(Prayer.ISHA)))
    }
}

@Composable
fun PrayerScreen(openSettings: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    if (!Store.hasLocation) { LocationSetup(); return }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val w = window()
    val times = Store.times()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(color = P.card) {
            Sub(if (w.next == Prayer.SUNRISE) "الشروق" else "الصلاة القادمة")
            Row(verticalAlignment = Alignment.Bottom) {
                Text(w.next.ar, color = P.ink, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(Alerts.timeText(Store.times(java.time.Instant.ofEpochMilli(w.nextAt).atZone(ZoneId.systemDefault()).toLocalDate()).getValue(w.next)), color = P.gold, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("بعد " + durationText(w.nextAt - now), color = P.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            val frac = ((now - w.prevAt).toFloat() / (w.nextAt - w.prevAt).coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.line)) {
                Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.accent))
            }
        }

        Panel(pad = 6.dp) {
            Prayer.entries.forEach { p ->
                val isNext = p == w.next
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isNext) P.accent.copy(alpha = .12f) else P.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.ar, color = if (isNext) P.accent else if (p.alertable) P.ink else P.muted, fontSize = 17.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(Alerts.timeText(times.getValue(p)), color = if (isNext) P.accent else P.ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Box(Modifier.size(width = 48.dp, height = 32.dp), contentAlignment = Alignment.CenterEnd) {
                        if (p.alertable) {
                            val on = Store.alert(p)
                            val ctx = LocalContext.current
                            Text(if (on) "🔔" else "🔕", fontSize = 18.sp, color = if (on) P.ink else P.muted,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { Store.setAlert(p, !on); Alerts.reschedule(ctx) }.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        val qibla = PrayTimes.qibla(Store.lat, Store.lng).roundToInt()
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = openSettings).padding(horizontal = 6.dp, vertical = 4.dp)) {
            Sub("📍 ${Store.place.ifBlank { "%.3f، %.3f".format(Store.lat, Store.lng) }} · ${Store.method.ar}${if (Store.hanafi) " · العصر حنفي" else ""}", size = 12.sp)
            Sub("🕋 القبلة: $qibla° من الشمال باتجاه عقارب الساعة", size = 12.sp)
        }
    }
}

/** First start: where are you? GPS, or a city from the list. */
@Composable
fun LocationSetup(compact: Boolean = false, done: () -> Unit = {}) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    fun useGps() {
        busy = true
        Alerts.locate(ctx) { loc ->
            if (loc == null) { busy = false; Toast.makeText(ctx, "تعذّر تحديد الموقع. فعّل الموقع أو اختر مدينة.", Toast.LENGTH_LONG).show(); return@locate }
            Store.setLocation(loc.latitude, loc.longitude, "")
            Alerts.reschedule(ctx)
            Alerts.resolvePlace(ctx, loc.latitude, loc.longitude)
            busy = false; done()
        }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) useGps() else Toast.makeText(ctx, "بدون إذن الموقع اختر مدينتك من القائمة", Toast.LENGTH_LONG).show() }
    Column(if (compact) Modifier.fillMaxWidth() else Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!compact) Panel(color = P.card) {
            Title("أهلًا بك في ALzikr")
            Sub("لحساب أوقات الصلاة بدقة نحتاج موقعك التقريبي. يُحسب كل شيء على هاتفك، ولا يُرسل موقعك إلى أي مكان.", size = 14.sp)
        }
        Action(if (busy) "جارٍ تحديد الموقع…" else "📍 استخدم موقعي الحالي", Modifier.fillMaxWidth()) {
            if (busy) return@Action
            if (ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) useGps() else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        Action("اختيار مدينة", Modifier.fillMaxWidth(), filled = false) { manual = true }
    }
    if (manual) CityDialog(onDismiss = { manual = false }) { city ->
        Store.setLocation(city.lat, city.lng, city.name)
        if (city.name.isNotEmpty()) { Store.method = city.method; Store.methodChosen = true }
        Alerts.reschedule(ctx); manual = false; done()
    }
}

@Composable
fun CityDialog(onDismiss: () -> Unit, onPick: (City) -> Unit) {
    val ctx = LocalContext.current
    var coords by remember { mutableStateOf(false) }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = P.surface,
        title = { Text(if (coords) "إحداثيات الموقع" else "اختر مدينتك", color = P.ink) },
        text = {
            if (coords) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(lat, { lat = it }, label = { Text("خط العرض (مثل 24.71)") }, singleLine = true)
                OutlinedTextField(lng, { lng = it }, label = { Text("خط الطول (مثل 46.67)") }, singleLine = true)
            } else LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(CITIES) { c ->
                    Text(c.name, color = P.ink, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onPick(c) }.padding(horizontal = 10.dp, vertical = 11.dp))
                }
            }
        },
        confirmButton = {
            if (coords) TextButton({
                val a = lat.trim().replace('،', '.').toDoubleOrNull(); val b = lng.trim().replace('،', '.').toDoubleOrNull()
                if (a == null || b == null || a !in -66.0..66.0 || b !in -180.0..180.0) Toast.makeText(ctx, "أدخل أرقامًا صحيحة", Toast.LENGTH_SHORT).show()
                else onPick(City("", a, b, Store.method))
            }) { Text("حفظ") }
            else TextButton({ coords = true }) { Text("إدخال إحداثيات") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}
