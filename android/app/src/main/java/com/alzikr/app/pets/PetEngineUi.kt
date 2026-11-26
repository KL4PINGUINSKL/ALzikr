package com.alzikr.app.pets

import com.alzikr.app.Wrap

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import com.alzikr.app.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alzikr.app.Action
import com.alzikr.app.Choice
import com.alzikr.app.L
import com.alzikr.app.P
import com.alzikr.app.Panel
import com.alzikr.app.Pet
import com.alzikr.app.PetLooks
import com.alzikr.app.Progress
import com.alzikr.app.Quran
import com.alzikr.app.SettingRow
import com.alzikr.app.Species
import com.alzikr.app.Store
import com.alzikr.app.Sub
import com.alzikr.app.Title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pet Preview: any look in any of the twenty states, with how the engine resolved it, its pack, license and unlock. */
/** A look to open the preview on (deep link extra "petskin"). */
object PetPreview { var start: String? = null; var state: String? = null }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetPreviewPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val first = remember { PetPreview.start?.let { PetPacks.registry().skin(it) }.also { PetPreview.start = null } }
    var species by remember { mutableStateOf(first?.let { f -> Species.entries.firstOrNull { it.name == f.species } } ?: Pet.species ?: Species.CAT) }
    var lookId by remember(species) { mutableStateOf(first?.takeIf { it.species == species.name }?.id ?: PetLooks.chosen(species)?.id ?: PetLooks.of(species).firstOrNull()?.id) }
    var state by remember { mutableStateOf(PetPreview.state?.let { PetState.of(it) } ?: PetState.IDLE).also { PetPreview.state = null } }
    val reg = PetPacks.registry()
    val skin = lookId?.let { reg.skin(it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        com.alzikr.app.PickOne(Species.entries.map { it.name to it.ar }, species.name, "الصديق", shown = 3) { species = Species.valueOf(it) }
        PetLooks.of(species).let { ls -> com.alzikr.app.PickOne(ls.map { it.id to it.name }, lookId ?: ls.firstOrNull()?.id ?: "", "الشكل", shown = 2) { lookId = it } }
        if (skin == null) return@Column
        Panel(color = P.card, pad = 12.dp) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(220.dp).clip(L.shape(16.dp)).background(P.surface)) {
                    PetView(skin, state, Modifier.fillMaxSize(), Place.PREVIEW, scale = Evolution.stage(reg.species(species.name), Progress.level()).scale)
                }
            }
            val r = StateResolver.resolve(skin, state)
            Text(skin.name, color = P.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Sub(if (r == null) "لا صورة" else if (r.fellBack) "«${state.ar}» غير موجودة في هذا الشكل، فيُعرض «${r.state.ar}» مع حركة «${r.motion.name.lowercase()}»" else "«${state.ar}»: حركة خاصة بهذا الشكل", size = 12.sp)
            Sub("${r?.source?.kind?.name ?: "-"} · ${skin.rarity.ar} · النموّ ${Quran.digits(skin.unlock.level)} · حزمة ${reg.packOf(skin.id)}", size = 12.sp)
            Sub("${skin.by} · ${reg.licenses[skin.license]?.name ?: skin.license}", size = 12.sp)
        }
        Title("الحالات")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PetState.entries.forEach { s ->
                val own = skin.states.containsKey(s)
                Choice((if (own) "● " else "") + s.ar, s == state) { state = s }
            }
        }
        Sub("● حالة لها حركة خاصة في هذا الشكل؛ والبقية تُعرض بأقرب حالة مع حركة من الكود.", size = 11.sp)
    }
}

/** Pack management: the built-in pack, downloaded packs (update, roll back, remove), and the motion/battery settings. */
@Composable
fun PetPacksPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var rev by remember { mutableStateOf(PetPacks.revision) }
    var catalog by remember { mutableStateOf<List<PetPacks.Available>?>(null) }
    var status by remember { mutableStateOf("") }
    @Suppress("UNUSED_VARIABLE") val r = rev
    val reg = PetPacks.registry()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(pad = 14.dp) {
            Title("الحركة والبطارية")
            SettingRow("حركة أقل", if (PetPerf.systemReduceMotion) "مفعّلة من إعدادات الهاتف" else "صور ثابتة للأصدقاء بلا حركة") {
                Switch(PetPerf.userReduceMotion, { PetPerf.userReduceMotion = it; Store.prefs.edit().putBoolean("pet_reduce_motion", it).apply(); Store.bump() })
            }
            SettingRow("توفير البطارية", if (PetPerf.powerSave) "الهاتف في وضع توفير الطاقة: الحركة أبطأ" else "نصف عدد الإطارات، وبلا حركات إضافية") {
                Switch(PetPerf.userSaveBattery, { PetPerf.userSaveBattery = it; Store.prefs.edit().putBoolean("pet_save_battery", it).apply(); Store.bump() })
            }
            Sub("تتوقف الحركة تمامًا والشاشة مطفأة، وحين لا يكون الصديق ظاهرًا.", size = 11.sp)
        }
        Panel(pad = 14.dp) {
            Title(reg.core.title)
            Sub("مدمجة في التطبيق، تعمل بلا إنترنت · الإصدار ${Quran.digits(reg.core.version)} · ${Quran.digits(reg.core.skins().size)} شكلًا لـ${Quran.digits(reg.core.species.size)} حيوانًا", size = 12.sp)
        }
        reg.packs.forEach { p ->
            Panel(pad = 14.dp) {
                Title(p.title)
                Sub("الإصدار ${Quran.digits(p.version)} · ${Quran.digits(p.skins().size)} شكلًا · ${Quran.digits((PetPacks.sizeOf(p.id) / 1024).toInt())} ك.ب", size = 12.sp)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Action("الإصدار السابق", Modifier.weight(1f), filled = false) { if (PetPacks.rollback(p.id)) { rev = PetPacks.revision; Store.bump() } else status = "لا يوجد إصدار سابق" }
                    Action("حذف", Modifier.weight(1f), filled = false, color = P.danger) { PetPacks.remove(p.id); rev = PetPacks.revision; Store.bump() }
                }
            }
        }
        Panel(pad = 14.dp) {
            Title("حزم جديدة")
            Sub("الحزم صور ورسوم فقط، تُنزَّل من مصدر ALzikr الموقَّع ويُتحقق من كل ملف فيها قبل تثبيتها، وتبقى بلا إنترنت بعد تنزيلها.", size = 12.sp)
            Action("البحث عن حزم", Modifier.fillMaxWidth().padding(top = 8.dp)) {
                status = "جارٍ البحث…"
                scope.launch { val c = withContext(Dispatchers.IO) { PetPacks.fetchCatalog() }; catalog = c; status = when { c == null -> "تعذّر الوصول إلى قائمة الحزم أو لم يُتحقق من توقيعها"; c.isEmpty() -> "لا حزم منشورة بعد"; else -> "" } }
            }
            catalog?.forEach { a ->
                val newer = a.installed == null || a.entry.version > a.installed
                SettingRow(a.entry.title, "الإصدار ${a.entry.version}" + (a.installed?.let { " · المثبّت $it" } ?: "") + " · ${a.entry.size / 1024} ك.ب") {
                    if (newer) Action(if (a.installed == null) "تنزيل" else "تحديث") {
                        status = "جارٍ التنزيل والتحقق…"
                        scope.launch { val res = withContext(Dispatchers.IO) { PetPacks.install(a.entry) }; status = res.fold({ "ثُبّتت «${it.title}» ✓" }, { "لم تُثبَّت: ${it.message}" }); rev = PetPacks.revision; Store.bump() }
                    }
                }
            }
            if (status.isNotEmpty()) Sub(status, Modifier.padding(top = 6.dp), size = 12.sp)
        }
        Action("معاينة الأصدقاء والحالات", Modifier.fillMaxWidth(), filled = false) { com.alzikr.app.Nav.morePage = "petpreview"; com.alzikr.app.Nav.go("more") }
        Action("الشكر والرخص", Modifier.fillMaxWidth(), filled = false) { com.alzikr.app.Nav.morePage = "petcredits"; com.alzikr.app.Nav.go("more") }
        @Suppress("UNUSED_VARIABLE") val c = ctx
    }
}

/** The asset license registry: every look with its author, license and source page (attribution where a license asks for it). */
@Composable
fun PetCreditsPage() {
    val ctx = LocalContext.current
    val reg = PetPacks.registry()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Sub("رسوم الأصدقاء من مصمّمين على LottieFiles وغيرها، كلٌّ برخصته. شكرًا لهم.", size = 13.sp)
        reg.licenses.values.forEach { l -> Sub("• ${l.name}" + (if (l.attribution) " (يلزم ذكر المصمم)" else "") + (if (l.url.isNotEmpty()) " — ${l.url}" else ""), size = 12.sp) }
        reg.species.forEach { s ->
            Title(s.ar)
            s.skins.forEach { k ->
                Row(Modifier.fillMaxWidth().clip(L.shape(10.dp)).clickable(enabled = k.page.isNotEmpty()) {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(k.page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(k.name, color = P.ink, fontSize = 14.sp)
                        Sub("${k.by.ifBlank { "—" }} · ${reg.licenses[k.license]?.name ?: k.license}", size = 11.sp)
                    }
                    if (k.page.isNotEmpty()) Text("↗", color = P.accent, fontSize = 16.sp, modifier = Modifier.width(24.dp))
                }
            }
        }
    }
}
