package com.alzikr.app

import android.app.AlarmManager
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSheet(close: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val reschedule = { Alerts.reschedule(ctx) }
    BackHandler(onBack = close)
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        @Suppress("DEPRECATION") val uri = r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (r.resultCode == android.app.Activity.RESULT_OK) { Store.sound = uri?.toString(); Store.silent = uri == null }
    }

    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الإعدادات", color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("تم", color = P.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = close).padding(horizontal = 14.dp, vertical = 8.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel {
                Title("الموقع")
                Sub(if (Store.hasLocation) "📍 " + Store.place.ifBlank { "%.4f، %.4f".format(Store.lat, Store.lng) } else "لم يُحدَّد بعد", Modifier.padding(bottom = 10.dp))
                LocationSetup(compact = true)
            }

            Panel {
                Title("طريقة الحساب")
                Sub("اختر ما يعتمده بلدك أو مسجدك.", Modifier.padding(bottom = 6.dp), size = 12.sp)
                Method.entries.forEach { m ->
                    val on = Store.method == m
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) P.accent.copy(alpha = .12f) else P.surface)
                        .clickable { Store.method = m; Store.methodChosen = true; reschedule() }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.ar, color = if (on) P.accent else P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Sub(if (m.ishaMinutes > 0) "${m.fajr}° · العشاء +${m.ishaMinutes} د" else "${m.fajr}° / ${m.isha}°", size = 11.sp)
                    }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Sub("العصر:"); Choice("الجمهور", !Store.hanafi) { Store.hanafi = false; reschedule() }; Choice("الحنفي", Store.hanafi) { Store.hanafi = true; reschedule() }
                }
            }

            Panel {
                Title("تعديل الأوقات")
                Sub("أضف أو أنقص دقائق لتوافق أذان مسجدك.", Modifier.padding(bottom = 6.dp), size = 12.sp)
                Prayer.entries.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.ar, color = P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Stepper(Store.offset(p)) { Store.setOffset(p, it); reschedule() }
                    }
                }
            }

            Panel {
                Title("التنبيهات")
                Sub("تنبيه قبل الصلاة", Modifier.padding(top = 6.dp, bottom = 6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 5, 10, 15, 20, 30).forEach { m -> Choice(if (m == 0) "بدون" else "$m د", Store.before == m) { Store.before = m; reschedule() } }
                }
                Sub("صوت الأذان", Modifier.padding(top = 12.dp, bottom = 6.dp))
                val soundName = remember(Store.sound, Store.silent) {
                    when { Store.silent -> "اهتزاز فقط"; Store.sound == null -> "صوت الهاتف الافتراضي"
                        else -> runCatching { RingtoneManager.getRingtone(ctx, Uri.parse(Store.sound)).getTitle(ctx) }.getOrDefault("صوت مختار") }
                }
                Text(soundName, color = P.ink, fontSize = 15.sp)
                Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Choice("اختيار صوت…", false) {
                        pickSound.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "صوت الأذان")
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Store.sound?.let(Uri::parse)))
                    }
                    Choice("الافتراضي", Store.sound == null && !Store.silent) { Store.sound = null; Store.silent = false }
                    Choice("اهتزاز فقط", Store.silent) { Store.silent = true }
                    Choice("تجربة ▶", false) { Alerts.testSound(ctx) }
                }
                Sub("لاستخدام صوت أذان لديك: أضفه في إعدادات الهاتف كنغمة إشعار، ثم اختره هنا.", Modifier.padding(top = 6.dp), size = 11.sp)
                if (!Alerts.canPost(ctx)) Warn("الإشعارات متوقفة لـ ALzikr، فلن تصلك التنبيهات. اضغط للسماح.") {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }
                if (Build.VERSION.SDK_INT >= 31 && !ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) Warn("اسمح بالمنبّهات الدقيقة ليصل التنبيه في وقته تمامًا. اضغط هنا.") {
                    ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                }
            }

            Panel {
                Title("تذكير الأذكار")
                Sub("أذكار الصباح بعد الفجر بـ", Modifier.padding(top = 6.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(-1, 15, 30, 60).forEach { m -> Choice(if (m < 0) "إيقاف" else "$m د", Store.morningAfter == m) { Store.morningAfter = m; reschedule() } } }
                Sub("أذكار المساء بعد العصر بـ", Modifier.padding(top = 10.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(-1, 15, 30, 60).forEach { m -> Choice(if (m < 0) "إيقاف" else "$m د", Store.eveningAfter == m) { Store.eveningAfter = m; reschedule() } } }
            }

            Panel {
                Title("تذكيرات أخرى")
                Sub("أذكار النوم", Modifier.padding(top = 6.dp, bottom = 6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-1, 21 * 60 + 30, 22 * 60, 22 * 60 + 30, 23 * 60, 23 * 60 + 30, 0).forEach { m ->
                        Choice(if (m < 0) "إيقاف" else Alerts.timeText(m), Data.sleepAt == m) { Data.sleepAt = m; reschedule() }
                    }
                }
                Sub("ذكر خلال اليوم (من 9 صباحًا إلى 9 مساءً)", Modifier.padding(top = 10.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "إيقاف", 1 to "كل ساعة", 2 to "كل ساعتين", 3 to "كل 3 ساعات").forEach { (n, l) -> Choice(l, Data.salawatEvery == n) { Data.salawatEvery = n; reschedule() } }
                }
                SettingRow("سورة الكهف يوم الجمعة", "تذكير الساعة 10 صباحًا") { Switch(Data.kahf, { Data.kahf = it; reschedule() }) }
                SettingRow("صيام التطوع", "مساء الأحد والأربعاء، وقبل الأيام البيض") { Switch(Data.fastReminder, { Data.fastReminder = it; reschedule() }) }
                Sub("السحور في رمضان، قبل الفجر بـ", Modifier.padding(top = 6.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "إيقاف", 30 to "30 د", 45 to "45 د", 60 to "ساعة").forEach { (n, l) -> Choice(l, Data.suhoorBefore == n) { Data.suhoorBefore = n; reschedule() } }
                }
            }

            Panel {
                Title("الصلاة والتركيز")
                Sub("صمت الهاتف بعد الأذان (وضع عدم الإزعاج)", Modifier.padding(top = 6.dp, bottom = 6.dp))
                val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "إيقاف", 10 to "10 د", 15 to "15 د", 20 to "20 د", 30 to "30 د").forEach { (n, l) ->
                        Choice(l, Data.silentAfterAdhan == n) {
                            Data.silentAfterAdhan = n
                            if (n > 0 && !nm.isNotificationPolicyAccessGranted) runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                        }
                    }
                }
                if (Data.silentAfterAdhan > 0 && !nm.isNotificationPolicyAccessGranted) Warn("اسمح لـ ALzikr بالتحكم في «عدم الإزعاج» ليصمت الهاتف وقت الصلاة. اضغط هنا.") {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
                SettingRow("إيقاف جلسة التركيز عند الأذان", "تُحسب الدقائق التي أنجزتها") { Switch(Data.focusStopsAtAdhan, { Data.focusStopsAtAdhan = it }) }
            }

            Panel {
                Title("المظهر")
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "حسب الهاتف", 1 to "نهاري", 2 to "ليلي").forEach { (n, l) -> Choice(l, Data.theme == n) { Data.theme = n } }
                }
                Sub("حجم خط الأذكار", Modifier.padding(top = 10.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "عادي", 1 to "كبير", 2 to "أكبر").forEach { (n, l) -> Choice(l, Data.textSize == n) { Data.textSize = n } }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("تعديل التاريخ الهجري", color = P.ink, fontSize = 15.sp)
                        Sub("اليوم: ${Data.hijri().day} ${Data.hijri().monthName}", size = 12.sp)
                    }
                    Stepper(Data.hijriAdjust) { Data.hijriAdjust = it; reschedule() }
                }
            }

            Panel { AboutSection(ink = P.ink, muted = P.muted, accent = P.accent, card = P.card) }
        }
    }
}

@Composable
private fun Stepper(value: Int, set: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(P.card).clickable { set(value - 1) }, contentAlignment = Alignment.Center) { Text("−", color = P.ink, fontSize = 18.sp) }
        Text(if (value > 0) "+$value" else "$value", color = if (value == 0) P.muted else P.accent, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.width(44.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(P.card).clickable { set(value + 1) }, contentAlignment = Alignment.Center) { Text("+", color = P.ink, fontSize = 18.sp) }
    }
}

@Composable
private fun Warn(text: String, onClick: () -> Unit) {
    Text(text, color = P.danger, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(P.danger.copy(alpha = .1f)).clickable(onClick = onClick).padding(10.dp))
}
