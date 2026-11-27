package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime

/** The sunnah around each prayer, from authentic Sunni sources. */
object Sunan {
    data class S(val text: String, val src: String)
    fun of(p: Prayer): List<S> = when (p) {
        Prayer.FAJR -> listOf(
            S("ركعتان قبل الفجر، وهما آكد السنن: «ركعتا الفجر خير من الدنيا وما فيها».", "مسلم"),
            S("«من صلى الغداة في جماعة، ثم قعد يذكر الله حتى تطلع الشمس، ثم صلى ركعتين، كانت له كأجر حجة وعمرة تامة تامة تامة». وبعد الصلاة أذكار الصباح.", "الترمذي، وحسّنه الألباني"),
        )
        Prayer.SUNRISE -> listOf(
            S("لا صلاة وقت طلوع الشمس حتى ترتفع قِيد رمح (نحو ربع ساعة).", "مسلم"),
            S("ثم صلاة الضحى ركعتان أو أكثر: «ويُجزئ من ذلك ركعتان يركعهما من الضحى».", "مسلم"),
        )
        Prayer.DHUHR -> listOf(
            S("أربع ركعات قبلها وركعتان بعدها، من السنن الرواتب الثنتي عشرة: «من صلى اثنتي عشرة ركعة في يوم وليلة بُني له بهن بيت في الجنة».", "مسلم، والتفصيل عند الترمذي وصححه"),
            S("«من حافظ على أربع ركعات قبل الظهر وأربع بعدها حرّمه الله على النار».", "أبو داود والترمذي، وصححه الألباني"),
        )
        Prayer.ASR -> listOf(
            S("أربع ركعات قبلها (غير مؤكدة): «رحم الله امرأً صلى قبل العصر أربعًا».", "أبو داود والترمذي، وحسّنه الألباني"),
            S("لا نافلة مطلقة بعد العصر حتى تغرب الشمس. وبعدها أذكار المساء.", "البخاري ومسلم"),
        )
        Prayer.MAGHRIB -> listOf(
            S("ركعتان بعدها من الرواتب.", "الترمذي، وصححه"),
            S("وركعتان قبلها لمن شاء: «صلّوا قبل صلاة المغرب… لمن شاء».", "البخاري"),
        )
        Prayer.ISHA -> listOf(
            S("ركعتان بعدها من الرواتب.", "الترمذي، وصححه"),
            S("ثم الوتر آخر الليل أو قبل النوم: «اجعلوا آخر صلاتكم بالليل وترًا».", "البخاري ومسلم"),
        )
    }
}

/** Everything about one prayer: prayed ✓, alarm, reminder, adhan, its sunnah, its adhkar, and its time correction. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PrayerSheet(p: Prayer, timesOptions: () -> Unit, openSettings: () -> Unit, close: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val time = Store.times().getValue(p)
    val now = LocalTime.now().let { it.hour * 60 + it.minute }
    AppSheet(onDismissRequest = close) {
        Column(Modifier.fillMaxWidth().quickDismiss(state, close).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Name, time, and how far it is.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (p.alertable) "صلاة ${p.ar}" else p.ar, color = P.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = L.head ?: L.body)
                    Sub(if (time > now) "بعد ${durationText((time - now) * 60_000L)}" else "دخل وقتها قبل ${durationText((now - time) * 60_000L)}", color = P.accent)
                }
                Text(Alerts.timeText(time), color = P.gold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            // Prayed.
            if (p.alertable) {
                val done = Data.prayed(LocalDate.now(), p)
                val can = time <= now
                Text(if (done) "✓ صلّيتها، تقبّل الله" else if (can) "اضغط هنا بعد أن تصليها ✓" else "لم يدخل وقتها بعد",
                    color = if (done) P.onAccent else if (can) P.accent else P.muted, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(if (done) P.accent else P.card).border(1.dp, if (can || done) P.accent else P.line, L.shape(16.dp))
                        .clickable(enabled = can || done) { Data.setPrayed(LocalDate.now(), p, !done); if (!done) Haptics.tap(ctx) }.padding(vertical = 16.dp))
            }
            // Its sunnah done too (tap again to undo).
            val extras = when (p) {
                Prayer.FAJR -> listOf("r_FAJR" to "سنة الفجر")
                Prayer.SUNRISE -> listOf("duha" to "صلاة الضحى")
                Prayer.DHUHR -> listOf("r_DHUHR" to "سنة الظهر")
                Prayer.ASR -> listOf("r_ASR" to "أربع قبل العصر")
                Prayer.MAGHRIB -> listOf("r_MAGHRIB" to "سنة المغرب")
                Prayer.ISHA -> listOf("r_ISHA" to "سنة العشاء", "witr" to "الوتر", "qiyam" to "قيام الليل")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                extras.forEach { (k, label) ->
                    val on = Data.extra(LocalDate.now(), k)
                    Text((if (on) "✓ " else "") + label, color = if (on) P.onAccent else P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 17.sp,
                        modifier = Modifier.weight(1f).clip(L.shape(12.dp)).background(if (on) P.accent else P.card).border(1.dp, if (on) P.accent else P.line, L.shape(12.dp))
                            .clickable { Data.setExtra(LocalDate.now(), k, !on); if (!on) Haptics.tap(ctx) }.padding(vertical = 10.dp))
                }
            }

            // Alarm, reminder, adhan.
            Column(Modifier.fillMaxWidth().clip(L.shape(16.dp)).background(P.card.seeThrough()).padding(horizontal = 12.dp, vertical = 4.dp)) {
                val alarm = PrayerAlarm.on(p)
                val off = PrayerAlarm.offset(p)
                Line("⏰", "المنبه", if (alarm) "يرنّ ${Alerts.timeText(((time + off) % 1440 + 1440) % 1440)} مثل منبه الهاتف" else "منبه حقيقي يرنّ حتى توقفه") {
                    Switch(alarm, { PrayerAlarm.setOn(ctx, p, it) })
                }
                if (alarm) Row(Modifier.padding(start = 36.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Choice("− 5", false) { PrayerAlarm.setOffset(ctx, p, off - 5) }
                    Text(when { off < 0 -> "قبل الوقت بـ ${-off} د"; off > 0 -> "بعده بـ $off د"; else -> "عند الوقت" }, color = P.ink, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(110.dp))
                    Choice("+ 5", false) { PrayerAlarm.setOffset(ctx, p, off + 5) }
                    Text("المزيد ›", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { close(); Nav.morePage = "alarms"; Nav.go("more") }.padding(6.dp))
                }
                if (p.alertable) {
                    Line("🔔", "تذكير الصلاة", "إشعار عند دخول الوقت") {
                        Switch(Store.alert(p), { Store.setAlert(p, it); Alerts.reschedule(ctx) })
                    }
                    val adhanOn = Adhan.mode != 0
                    Line("📣", "الأذان", if (!adhanOn) "متوقف في الإعدادات" else if (Adhan.skip(p)) "لا يُؤذَّن لهذه الصلاة" else Adhan.voice().name) {
                        if (adhanOn) Switch(!Adhan.skip(p), { Adhan.setSkip(p, !it) })
                        else Text("تشغيل ›", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { close(); openSettings() }.padding(6.dp))
                    }
                }
            }

            // Its sunnah.
            Panel(color = P.card, pad = 12.dp) {
                Sub(if (p.alertable) "السنن مع هذه الصلاة" else "في هذا الوقت", color = P.gold)
                Sunan.of(p).forEach { s ->
                    Text(s.text, color = P.ink, fontSize = (15 * Data.textScale).sp, lineHeight = (25 * Data.textScale).sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 6.dp))
                    Sub(s.src, size = 11.sp)
                }
            }

            // Its adhkar.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (p.alertable) {
                    Tile("🤲", "أذكار بعد ${p.ar}", Modifier.weight(1f)) { close(); Nav.collection = "prayer_${p.name}"; Nav.go("adhkar") }
                    Tile("📣", "أذكار الأذان", Modifier.weight(1f)) { close(); Nav.collection = "mosque"; Nav.go("adhkar") }
                }
                when (p) {
                    Prayer.FAJR, Prayer.SUNRISE -> Tile("🌅", "أذكار الصباح", Modifier.weight(1f)) { close(); Nav.collection = "morning"; Nav.go("adhkar") }
                    Prayer.ASR, Prayer.MAGHRIB -> Tile("🌇", "أذكار المساء", Modifier.weight(1f)) { close(); Nav.collection = "evening"; Nav.go("adhkar") }
                    Prayer.ISHA -> Tile("🌙", "أذكار النوم", Modifier.weight(1f)) { close(); Nav.collection = "sleep"; Nav.go("adhkar") }
                    else -> {}
                }
            }
            if (p == Prayer.ISHA) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val read = Data.extra(Moments.night(), "mulk")
                    val readB = Data.extra(Moments.night(), "baqarah")
                    Tile("📖", if (read) "✓ سورة الملك" else "سورة الملك", Modifier.weight(1f), on = read) { close(); Moments.readMulk() }
                    Tile("📗", if (readB) "✓ خواتيم البقرة" else "خواتيم البقرة", Modifier.weight(1f), on = readB) { close(); Moments.readBaqarah() }
                    Tile("📖", "سورة السجدة", Modifier.weight(1f)) { close(); Nav.reading = 32 to 1 }
                    Tile("📝", "محاسبة اليوم", Modifier.weight(1f)) { close(); Nav.morePage = "today"; Nav.go("more") }
                }
                Sub("«كان النبي ﷺ لا ينام حتى يقرأ ﴿الم تنزيل﴾ و﴿تبارك الذي بيده الملك﴾» (الترمذي، وصححه الألباني) · «من قرأ بالآيتين من آخر سورة البقرة في ليلة كفتاه» (البخاري ومسلم)", size = 11.sp)
            }

            // Its time correction.
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("تعديل الوقت بالدقائق", color = P.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Choice("−", false) { Store.setOffset(p, Store.offset(p) - 1); Alerts.reschedule(ctx) }
                Text(Store.offset(p).let { if (it > 0) "+$it" else "$it" }, color = P.ink, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                Choice("+", false) { Store.setOffset(p, Store.offset(p) + 1); Alerts.reschedule(ctx) }
            }
            Text("حساب الأوقات: الموقع وطريقة الحساب ووقت العصر ›", color = P.accent, fontSize = 14.sp,
                modifier = Modifier.clip(L.shape(10.dp)).clickable { close(); timesOptions() }.padding(8.dp))
        }
    }
}

@Composable
private fun Line(icon: String, title: String, sub: String, right: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(Modifier.width(36.dp)) { Glyph(icon, tint = P.accent, size = 22.dp) }
        Column(Modifier.weight(1f)) {
            Text(title, color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Sub(sub, size = 12.sp)
        }
        right()
    }
}
