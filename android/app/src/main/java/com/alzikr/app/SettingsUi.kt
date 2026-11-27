package com.alzikr.app

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
/** [start] opens one section directly (from «المزيد»); then «back» closes instead of showing the list. */
fun SettingsSheet(start: String? = null, close: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val reschedule = { Alerts.reschedule(ctx) }
    var section by remember { mutableStateOf(start) }
    val back = { if (section != null && start == null) section = null else close() }
    BackHandler(onBack = back)
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        @Suppress("DEPRECATION") val uri = r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (r.resultCode == android.app.Activity.RESULT_OK) { Store.sound = uri?.toString(); Store.silent = uri == null }
    }

    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (section != null) Text("→", color = P.accent, fontSize = 21.sp, modifier = Modifier.clip(L.shape(10.dp)).clickable { back() }.padding(horizontal = 10.dp, vertical = 6.dp))
            Text(when (section) { "times" -> "الموقع والحساب"; "alerts" -> "التنبيهات والأذان"; "prayer" -> "الصلاة والتركيز"; "look" -> "راحة العين والقراءة"; "recite" -> "التلاوة"; "help" -> "المساعدة"; "mode" -> "شكل التطبيق"; "about" -> "عن التطبيق"; else -> "الإعدادات" },
                color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("تم", color = P.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(L.shape(10.dp)).clickable(onClick = close).padding(horizontal = 14.dp, vertical = 8.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (section == null) {
                SimpleModePanel()
                Sub("اختر قسمًا:", size = 13.sp)
                listOf(
                    listOf(Triple("📍", "الموقع والحساب", "times"), Triple("🔔", "التنبيهات والأذان", "alerts"), Triple("🕌", "الصلاة والتركيز", "prayer")),
                    listOf(Triple("👁", "راحة العين", "look"), Triple("🎙", "التلاوة", "recite"), Triple("💡", "المساعدة", "help")),
                    listOf(Triple("ℹ", "عن التطبيق", "about")),
                ).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (icon, label, id) -> Tile(icon, label, Modifier.weight(1f)) { section = id } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Panel(color = P.card, pad = 12.dp) {
                    Sub("📍 " + (if (Store.hasLocation) Store.place.ifBlank { "%.3f، %.3f".format(Store.lat, Store.lng) } else "لم يُحدَّد الموقع") + " · " + Store.method.ar, size = 12.sp)
                    Sub("الثيم: " + Looks.byId(Data.look).name, size = 12.sp)
                }
            }
            if (section == "times") {
                Panel {
                    Title("الموقع")
                    Sub(if (Store.hasLocation) "📍 " + Store.place.ifBlank { "%.4f، %.4f".format(Store.lat, Store.lng) } else "لم يُحدَّد بعد", Modifier.padding(bottom = 10.dp))
                    LocationSetup(compact = true)
                }
            }

            if (section == "times") {
                Panel {
                    Title("طريقة الحساب")
                    Sub("اختر ما يعتمده بلدك أو مسجدك.", Modifier.padding(bottom = 6.dp), size = 12.sp)
                    Method.entries.forEach { m ->
                        val on = Store.method == m
                        Row(Modifier.fillMaxWidth().clip(L.shape(12.dp)).background(if (on) P.accent.copy(alpha = .12f) else P.surface)
                            .clickable { Store.method = m; Store.methodChosen = true; reschedule() }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(m.ar, color = if (on) P.accent else P.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Sub(if (m.ishaMinutes > 0) "${m.fajr}° · العشاء +${m.ishaMinutes} د" else "${m.fajr}° / ${m.isha}°", size = 11.sp)
                        }
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Sub("العصر:"); Choice("الجمهور", !Store.hanafi) { Store.hanafi = false; reschedule() }; Choice("الحنفي", Store.hanafi) { Store.hanafi = true; reschedule() }
                    }
                }
            }

            if (section == "times") {
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
            }

            if (section == "alerts") {
                Panel {
                    Title("التنبيهات")
                    Sub("تنبيه قبل الصلاة", Modifier.padding(top = 6.dp, bottom = 6.dp))
                    Wrap(gap = 6.dp) {
                        listOf(0, 5, 10, 15, 20, 30).forEach { m -> Choice(if (m == 0) "بدون" else "$m د", Store.before == m) { Store.before = m; reschedule() } }
                    }
                    AdhanChoice()
                    Sub(if (Adhan.mode == 0) "صوت التنبيه" else "صوت التنبيه (إن لم يُشغَّل الأذان)", Modifier.padding(top = 12.dp, bottom = 6.dp))
                    val soundName = remember(Store.sound, Store.silent) {
                        when { Store.silent -> "اهتزاز فقط"; Store.sound == null -> "صوت الهاتف الافتراضي"
                            else -> runCatching { RingtoneManager.getRingtone(ctx, Uri.parse(Store.sound)).getTitle(ctx) }.getOrDefault("صوت مختار") }
                    }
                    Text(soundName, color = P.ink, fontSize = 15.sp)
                    Wrap(Modifier.padding(top = 8.dp), gap = 6.dp) {
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
            }

            if (section == "alerts") {
                Panel {
                    Title("تذكير الأذكار")
                    Sub("أذكار الصباح بعد الفجر بـ", Modifier.padding(top = 6.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(-1, 15, 30, 60).forEach { m -> Choice(if (m < 0) "إيقاف" else "$m د", Store.morningAfter == m) { Store.morningAfter = m; reschedule() } } }
                    Sub("أذكار المساء بعد العصر بـ", Modifier.padding(top = 10.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(-1, 15, 30, 60).forEach { m -> Choice(if (m < 0) "إيقاف" else "$m د", Store.eveningAfter == m) { Store.eveningAfter = m; reschedule() } } }
                }
            }

            if (section == "alerts") {
                Panel {
                    Title("تذكيرات أخرى")
                    Sub("أذكار النوم", Modifier.padding(top = 6.dp, bottom = 6.dp))
                    PickOne(listOf(-1, 21 * 60 + 30, 22 * 60, 22 * 60 + 30, 23 * 60, 23 * 60 + 30, 0).map { m -> m.toString() to (if (m < 0) "إيقاف" else Alerts.timeText(m)) },
                        Data.sleepAt.toString(), "وقت النوم", shown = 3) { Data.sleepAt = it.toInt(); reschedule() }
                    Sub("ذكر خلال اليوم (من 9 صباحًا إلى 9 مساءً)", Modifier.padding(top = 10.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "إيقاف", 1 to "كل ساعة", 2 to "كل ساعتين", 3 to "كل 3 ساعات").forEach { (n, l) -> Choice(l, Data.salawatEvery == n) { Data.salawatEvery = n; reschedule() } }
                    }
                    SettingRow("سورة الكهف يوم الجمعة", "تذكير الساعة 10 صباحًا") { Switch(Data.kahf, { Data.kahf = it; reschedule() }) }
                    SettingRow("تنبيه المناسبة القادمة", "مساء قبل الجمعة والاثنين والخميس والأيام البيض وعرفة والعيدين وعاشوراء ورمضان… وإذا اجتمع فضلان في يوم قيل لك") { Switch(Data.fastReminder, { Data.fastReminder = it; reschedule() }) }
                    SettingRow("تنبيه يوم المناسبة", "صباح اليوم نفسه بعد الفجر بعشرين دقيقة") { Switch(Data.dayReminder, { Data.dayReminder = it; reschedule() }) }
                    if (Data.fastReminder) Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Sub("وقته:")
                        Choice("⏰ " + Alerts.timeText(DayInfo.eveAt), true) {
                            android.app.TimePickerDialog(ctx, { _, hh, mm -> DayInfo.eveAt = hh * 60 + mm; reschedule() }, DayInfo.eveAt / 60, DayInfo.eveAt % 60, android.text.format.DateFormat.is24HourFormat(ctx)).show()
                        }
                    }
                    Sub("السحور في رمضان، قبل الفجر بـ", Modifier.padding(top = 6.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "إيقاف", 30 to "30 د", 45 to "45 د", 60 to "ساعة").forEach { (n, l) -> Choice(l, Data.suhoorBefore == n) { Data.suhoorBefore = n; reschedule() } }
                    }
                }
            }

            if (section == "prayer") {
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
            }

            if (section == "look") {
                Panel {
                    Title("🪟 شفافية البطاقات")
                    Sub("في الثيمات ذات الخلفية الحية: كم تظهر الحياة من خلف البطاقات. ثيمات الزجاج لها زجاجها، والثيمات الساكنة تبقى مصمتة.", size = 12.sp)
                    Wrap(gap = 6.dp) {
                        listOf(0 to "مصمتة", 1 to "شفافة قليلًا", 2 to "شفافة أكثر").forEach { (k, l) -> Choice(l, Data.cardSee == k) { Data.cardSee = k } }
                    }
                    Title("🌙 نور الليل")
                    Sub("ضوء دافئ كضوء القنديل يغطي التطبيق من العشاء حتى الفجر حسب مواقيت مدينتك، فتريح عينيك في قراءة القرآن والأذكار ليلًا.", Modifier.padding(bottom = 8.dp), size = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "متوقف", 1 to "من العشاء إلى الفجر", 2 to "دائمًا").forEach { (n, l) -> Choice(l, Comfort.nightLight == n) { Comfort.nightLight = n } }
                    }
                    if (Comfort.nightLight > 0) {
                        Sub("الدفء", Modifier.padding(top = 10.dp, bottom = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1 to "خفيف", 2 to "متوسط", 3 to "قوي").forEach { (n, l) -> Choice(l, Comfort.warmth == n) { Comfort.warmth = n } }
                        }
                        Sub("تعتيم إضافي (أخفض من أقل سطوع للهاتف)", Modifier.padding(top = 10.dp, bottom = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0 to "بدون", 1 to "قليل", 2 to "أكثر").forEach { (n, l) -> Choice(l, Comfort.dim == n) { Comfort.dim = n } }
                        }
                        Text(Comfort.status(), color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Panel {
                    Title("👁 استراحة العين")
                    Sub("بعد مدة من النظر إلى التطبيق تظهر بطاقة صغيرة في الأعلى تذكّرك أن تنظر إلى شيء بعيد عشرين ثانية، تعدّها لك ثم تختفي. والابتعاد عن التطبيق خمس دقائق يُحسب استراحة.", Modifier.padding(bottom = 8.dp), size = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "متوقف", 20 to "كل ٢٠ دقيقة", 30 to "كل ٣٠", 45 to "كل ٤٥").forEach { (n, l) -> Choice(l, Comfort.breakEvery == n) { Comfort.breakEvery = n } }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("تقليل الحركة", color = P.ink, fontSize = 15.sp)
                            Sub("تسكن الخلفيات الحيّة وحركة صديقك، أهدأ للعين وأوفر للبطارية", size = 12.sp)
                        }
                        Switch(Comfort.calm, { Comfort.calm = it })
                    }
                }
                Panel {
                    Title("🔤 خط التطبيق")
                    Sub("خط واحد لكل النصوص في التطبيق، أو خط الثيم كما صُمّم.", Modifier.padding(bottom = 6.dp), size = 12.sp)
                    Wrap(gap = 6.dp) {
                        AppFont.choices.forEach { (id, name) ->
                            Text(name, color = if (AppFont.id == id) P.onAccent else P.ink, fontSize = 15.sp, fontFamily = AppFont.family(id) ?: L.body,
                                modifier = Modifier.clip(L.shape(10.dp)).background(if (AppFont.id == id) P.accent else P.surface).then(Modifier.border(1.dp, P.line, L.shape(10.dp))).clickable { AppFont.id = id }.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                    Text("بسم الله الرحمن الرحيم · ١٢٣ · 123", color = P.ink, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Panel {
                    Title("📅 شكل التاريخ في الأعلى")
                    var dates by remember { mutableStateOf(false) }
                    Box(Modifier.padding(vertical = 6.dp)) { DateBadge({ dates = true }, { dates = true }) }
                    Action("اختر شكلًا", filled = false) { dates = true }
                    if (dates) DateStyleSheet { dates = false }
                }
                Panel {
                    Title("القراءة")
                    Sub("حجم خط القراءة (الأذكار والأحاديث وشروحها والتفسير)", Modifier.padding(top = 4.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "عادي", 1 to "كبير", 2 to "أكبر").forEach { (n, l) -> Choice(l, Data.textSize == n) { Data.textSize = n } }
                    }
                    Text("نموذج: سبحان الله وبحمده، سبحان الله العظيم", color = P.ink, fontSize = (17 * Data.textScale).sp, fontFamily = Fonts.naskh, modifier = Modifier.padding(top = 6.dp))
                    Sub("عرض الخيارات الكثيرة (التفسير، القارئ، الخط…)", Modifier.padding(top = 10.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Choice("بعضها وزر «المزيد»", !Data.pickScroll) { Data.pickScroll = false }
                        Choice("كلها في شريط يُمرَّر", Data.pickScroll) { Data.pickScroll = true }
                    }
                    Sub("الأرقام في التطبيق", Modifier.padding(top = 10.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Labels without digits: every digit on screen follows this very choice.
                        Choice("هندية (المشرق)", Quran.easternDigits) { Quran.easternDigits = true }
                        Choice("عربية (المغرب)", !Quran.easternDigits) { Quran.easternDigits = false }
                    }
                    Sub("مثال: " + Quran.digits(1234567890), size = 12.sp)
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("تعديل التاريخ الهجري", color = P.ink, fontSize = 15.sp)
                            Sub("اليوم: ${Data.hijri().day} ${Data.hijri().monthName}", size = 12.sp)
                        }
                        Stepper(Data.hijriAdjust) { Data.hijriAdjust = it; reschedule() }
                    }
                }
                // The theme, its colours and day / night live in «الثيمات» (they used to be repeated here).
                Text("🎨 الثيم والألوان والوضع النهاري والليلي في «الثيمات» ›", color = P.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(L.shape(10.dp)).clickable { Nav.morePage = "themes"; if (Nav.tab != "more") Nav.go("more"); close() }.padding(8.dp))
            }

            if (section == "recite") {
                Panel {
                    Title("التلاوة")
                    Sub("القارئ ومدى القراءة والتكرار عند الاستماع للقرآن.", Modifier.padding(bottom = 8.dp), size = 12.sp)
                    var pick by remember { mutableStateOf(false) }
                    if (pick) ReciterDialog { pick = false }
                    ReciterLine { pick = true }
                    Sub("مدى القراءة", Modifier.padding(top = 10.dp, bottom = 6.dp))
                    RangeChoice()
                    Sub("تكرار كل آية", Modifier.padding(top = 10.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 3, 5, 10).forEach { n -> Choice(if (n == 1) "بلا تكرار" else "×$n", Recitation.repeat == n) { Recitation.repeat = n } }
                    }
                }
            }

            if (section == "help") {
                Panel {
                    Title("المساعدة")
                    Sub("في أول زيارة لكل صفحة تظهر تلميحة تشرح ما فيها وأين تضغط.", Modifier.padding(bottom = 8.dp), size = 12.sp)
                    Action("إظهار التلميحات من جديد", Modifier.fillMaxWidth(), filled = false) {
                        Hints.resetAll(); android.widget.Toast.makeText(ctx, "ستظهر التلميحات في كل صفحة من جديد", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    Action("👋 صفحة الترحيب: جولة في أهم المميزات", Modifier.fillMaxWidth().padding(top = 8.dp), filled = false) { Welcome.showAgain() }
                }
            }

            if (section == "mode") SimpleModePanel()

            if (section == "about") {
                Panel { AboutSection(ink = P.ink, muted = P.muted, accent = P.accent, card = P.card) }
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, set: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(L.shape(10.dp)).background(P.card).clickable { set(value - 1) }, contentAlignment = Alignment.Center) { Text("−", color = P.ink, fontSize = 18.sp) }
        Text(if (value > 0) "+$value" else "$value", color = if (value == 0) P.muted else P.accent, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.width(44.dp))
        Box(Modifier.size(34.dp).clip(L.shape(10.dp)).background(P.card).clickable { set(value + 1) }, contentAlignment = Alignment.Center) { Text("+", color = P.ink, fontSize = 18.sp) }
    }
}

@Composable
private fun Warn(text: String, onClick: () -> Unit) {
    Text(text, color = P.danger, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(L.shape(10.dp))
        .background(P.danger.copy(alpha = .1f)).clickable(onClick = onClick).padding(10.dp))
}

/** The adhan at prayer time: off, the whole adhan, or the takbirs only; the voice, its download and a sample. */
@Composable
fun AdhanChoice() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Sub("الأذان عند دخول الوقت", Modifier.padding(top = 12.dp, bottom = 6.dp))
    Wrap(gap = 6.dp) {
        listOf(0 to "تنبيه فقط", 1 to "الأذان كاملًا", 2 to "التكبيرات فقط").forEach { (m, l) -> Choice(l, Adhan.mode == m) { Adhan.mode = m } }
    }
    if (Adhan.mode == 0) return
    Adhan.voices.forEach { voice ->
        val on = Adhan.voiceId == voice.id
        val has = Adhan.has(ctx, voice)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(12.dp)).background(if (on) P.accent.copy(alpha = .10f) else P.card)
            .clickable { Adhan.voiceId = voice.id }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text((if (on) "● " else "○ ") + voice.name, color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Sub(voice.credit, size = 10.sp)
            }
            when {
                Adhan.downloading == voice.id -> Text("${(Adhan.progress * 100).toInt()}٪", color = P.accent, fontSize = 13.sp)
                has -> Text("▶ تجربة", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Adhan.voiceId = voice.id; Adhan.start(ctx, null) }.padding(6.dp))
                else -> Text("تنزيل · ${"%.1f".format(voice.bytes / 1_048_576f)} م.ب", color = P.accent, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable {
                    Adhan.voiceId = voice.id
                    scope.launch {
                        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Adhan.download(ctx, voice) }
                        if (!ok) android.widget.Toast.makeText(ctx, "تعذّر التنزيل، تحقق من الإنترنت", android.widget.Toast.LENGTH_LONG).show()
                    }
                }.padding(6.dp))
            }
        }
    }
    if (!Adhan.has(ctx, Adhan.voice())) Sub("نزّل صوت الأذان المختار مرة واحدة ليعمل دون إنترنت. حتى ذلك الحين يُستعمل صوت التنبيه.", Modifier.padding(top = 6.dp), size = 11.sp, color = P.gold)
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("الفجر: التكبيرات فقط", color = P.ink, fontSize = 14.sp)
            Sub("التسجيلان ليس فيهما «الصلاة خير من النوم»", size = 11.sp)
        }
        androidx.compose.material3.Switch(Adhan.fajrTakbir, { Adhan.fajrTakbir = it })
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("لا تشغّل الأذان والهاتف صامت", color = P.ink, fontSize = 14.sp)
            Sub("في وضع الصامت أو الاهتزاز يكتفي بالتنبيه", size = 11.sp)
        }
        androidx.compose.material3.Switch(Adhan.respectSilent, { Adhan.respectSilent = it })
    }
    Sub("يُشغَّل الأذان بصوت المنبّه، ولا يُشغَّل أثناء المكالمات. لإيقافه اضغط «إيقاف» في الإشعار.", Modifier.padding(top = 4.dp), size = 11.sp)
}
