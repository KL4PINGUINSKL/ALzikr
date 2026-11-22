package com.alzikr.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * The friend living in the app: it walks gently left and right, stops, looks around, hops now and then,
 * sleeps at night and is happy when the prayers are marked. A tap makes it speak; it also speaks by itself
 * every few minutes (if allowed). A long press opens its page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PetStage(modifier: Modifier, height: Int = 52, bubbleBelow: Boolean = true) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val sp = Pet.species ?: return
    val clock = rememberPetClock()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val x = remember { Animatable(0.5f) }
    var left by remember { mutableStateOf(false) }
    var walking by remember { mutableStateOf(false) }
    val hop = remember { Animatable(0f) }
    val flap = remember { Animatable(0f) }
    var said by remember { mutableStateOf<Pet.Say?>(null) }
    var n by remember { mutableIntStateOf(Random.nextInt(100)) }
    val asleep = Pet.asleep(); val happy = Pet.happy() || said != null && Pet.kids
    fun speak() { said = Pet.say(n++); scope.launch { hop.animateTo(1f, tween(160)); hop.animateTo(0f, tween(220)) } }

    // A gentle life: walk somewhere, rest, sometimes hop or flap; asleep at night it stays put.
    LaunchedEffect(sp, asleep) {
        while (true) {
            if (asleep) { walking = false; delay(4000); continue }
            delay(Random.nextLong(2500, 6500))
            when (Random.nextInt(6)) {
                0, 1, 2 -> {
                    val to = Random.nextFloat() * .9f + .05f
                    left = to < x.value; walking = true
                    x.animateTo(to, tween((kotlin.math.abs(to - x.value) * 9000).toInt().coerceAtLeast(600), easing = LinearEasing))
                    walking = false
                }
                3 -> { hop.animateTo(1f, tween(180)); hop.animateTo(0f, tween(240)) }
                4 -> { flap.animateTo(1f, tween(200)); flap.animateTo(0f, tween(300)) }
                else -> left = !left
            }
        }
    }
    // Speaks by itself now and then.
    LaunchedEffect(sp) {
        while (true) {
            delay(Random.nextLong(150_000, 300_000))
            if (Pet.talks && !Pet.asleep()) speak()
        }
    }
    LaunchedEffect(said) { if (said != null) { delay(7000); said = null } }

    BoxWithConstraints(modifier.height(height.dp)) {
        val box = height.dp
        val maxX = maxWidth - box
        Box(Modifier.offset(x = maxX * x.value).size(box)
            .combinedClickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null,
                onLongClick = { Nav.morePage = "pet"; Nav.go("more") }) { speak() }) {
            PetSprite(sp, {
                val t = clock() / 1000f
                Pose(t = t, blink = !asleep && (t % 4.2f) < .13f, walk = if (walking) t * 1.6f else 0f, hop = hop.value, flap = flap.value, sleep = asleep, happy = happy, left = left)
            }, Modifier.fillMaxSize())
            said?.let { s ->
                val px = with(LocalDensity.current) { box.roundToPx() }
                Popup(alignment = if (bubbleBelow) Alignment.TopCenter else Alignment.BottomCenter, offset = IntOffset(0, if (bubbleBelow) px else -px)) {
                    Bubble(s.text) { s.go?.invoke(); said = null }
                }
            }
        }
    }
}

/** The speech bubble: small, soft, closes by itself; a tap goes where it points. */
@Composable
fun Bubble(text: String, onClick: () -> Unit) {
    Text(text, color = P.ink, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 240.dp).clip(L.shape(16.dp)).background(P.card).border(1.dp, P.accent.copy(alpha = .5f), L.shape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp))
}

/** One-time welcome: meet the new friend and choose one (or later). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PetIntro() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    if (Pet.introSeen || Pet.species != null) return
    val state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val close = { Pet.introSeen = true }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = close, sheetState = state, containerColor = P.surface) {
        Column(Modifier.fillMaxWidth().quickDismiss(state, close).verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Title("صديق جديد يرافقك 🌙")
            Sub("اختر صديقًا صغيرًا يعيش في أعلى التطبيق بهدوء: يذكّرك بالصلاة والأذكار بكلمة لطيفة، ويفرح حين تصلّي. وللصغار يتعلّم معهم. ويمكن أن يرافقك فوق التطبيقات الأخرى.", size = 14.sp)
            SpeciesGrid(null) { Pet.species = it; Pet.introSeen = true }
            Text("لاحقًا (تجده في «المزيد»)", color = P.muted, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(L.shape(10.dp)).clickable(onClick = close).padding(10.dp))
        }
    }
}

/** The seven friends, each alive in its own little box. */
@Composable
fun SpeciesGrid(chosen: Species?, pick: (Species) -> Unit) {
    val clock = rememberPetClock()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Species.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { s ->
                    val on = s == chosen
                    Column(Modifier.weight(1f).clip(L.shape(16.dp)).background(if (on) P.accent.copy(alpha = .14f) else P.card)
                        .border(if (on) 2.dp else 1.dp, if (on) P.accent else P.line, L.shape(16.dp)).clickable { pick(s) }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        PetSprite(s, { val t = clock() / 1000f + s.ordinal; Pose(t = t, blink = (t % 3.7f) < .12f, flap = if ((t % 5f) < .4f) .8f else 0f, happy = on) }, Modifier.size(78.dp))
                        Text(s.ar, color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The friend's page: choose, name, adults or children, where it lives, and its twin over other apps. */
@Composable
fun PetPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val ctx = LocalContext.current
    var resumed by remember { mutableIntStateOf(0) }
    @Suppress("DEPRECATION") val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumed++ }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    @Suppress("UNUSED_VARIABLE") val r = resumed
    val sp = Pet.species
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint("pet", "🐾", "اختر صديقك وسمّه. يعيش في أعلى التطبيق أو على أرضية في أسفله، يمشي بهدوء ويتكلم أحيانًا. اضغط عليه ليقول شيئًا، واضغط مطولًا لتفتح هذه الصفحة.")
        if (sp != null) Panel(color = P.card, pad = 12.dp) {
            Box(Modifier.fillMaxWidth()) { PetStage(Modifier.fillMaxWidth(), height = 110) }
            Text(Pet.name, color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Sub(if (Pet.asleep()) "نائم الآن 💤" else if (Pet.happy()) "سعيد لأنك صلّيت ما مضى من صلوات اليوم" else "اضغط عليه ليتكلم", Modifier.fillMaxWidth(), size = 12.sp)
        }
        Title("اختر صديقك")
        SpeciesGrid(sp) { Pet.species = it }
        if (sp == null) return@Column
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var name by remember(sp) { mutableStateOf(Pet.name) }
            OutlinedTextField(name, { name = it; if (it.isNotBlank()) Pet.name = it }, singleLine = true, label = { Text("اسمه") }, modifier = Modifier.weight(1f))
        }

        Panel(pad = 14.dp) {
            Title("لمن؟")
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("للكبار: يذكّرني", !Pet.kids) { Pet.kids = false }
                Choice("للصغار: يتعلّم معي", Pet.kids) { Pet.kids = true }
            }
            if (Pet.kids) Action("⭐ تعلّم مع ${Pet.name}", Modifier.fillMaxWidth().padding(top = 10.dp)) { Nav.morePage = "kids"; Nav.go("more") }
        }
        Panel(pad = 14.dp) {
            Title("أين يعيش في التطبيق؟")
            Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("أعلى الشاشة", Pet.place == "top") { Pet.place = "top" }
                Choice("أرضية في الأسفل", Pet.place == "ground") { Pet.place = "ground" }
                Choice("مخفي", Pet.place == "off") { Pet.place = "off" }
            }
            SettingRow("يتكلم وحده أحيانًا", "كلمة لطيفة كل بضع دقائق وأنت في التطبيق") { Switch(Pet.talks, { Pet.talks = it }) }
        }

        // Over other apps.
        Panel(pad = 14.dp) {
            Title("يرافقك فوق التطبيقات")
            Sub("صديق صغير على حافة الشاشة، تسحبه حيث تشاء، ولا يلمس ما تحته. كل مدة تختارها تظهر منه فقاعة قصيرة بذكر أو آية أو حديث ثم تختفي.", size = 12.sp)
            val can = Settings.canDrawOverlays(ctx)
            SettingRow("تشغيله", if (can) null else "يحتاج إذن «الظهور فوق التطبيقات»") {
                Switch(Pet.floating && can, {
                    if (it && !Settings.canDrawOverlays(ctx)) runCatching { ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    Pet.floating = it
                    if (it && Settings.canDrawOverlays(ctx)) PetOverlay.start(ctx) else PetOverlay.stop(ctx)
                })
            }
            if (Pet.floating && !can) Text("اضغط هنا لمنح الإذن، ثم ارجع", color = P.danger, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }.padding(6.dp))
            if (Pet.floating && can) LaunchedEffect(Unit) { PetOverlay.start(ctx) }
            Sub("يذكّرك كل", Modifier.padding(top = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15, 30, 60, 120, 180).forEach { m -> Choice(if (m < 60) "$m دقيقة" else "${m / 60} ساعة", Pet.every == m) { Pet.every = m; PetOverlay.refresh(ctx) } }
            }
            Sub("بماذا؟", Modifier.padding(top = 8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("mix" to "منوّع", "dhikr" to "ذكر", "ayah" to "آية", "hadith" to "حديث").forEach { (k, l) -> Choice(l, Pet.content == k) { Pet.content = k } }
            }
            Sub("الحجم", Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(48 to "صغير", 64 to "متوسط", 84 to "كبير").forEach { (s, l) -> Choice(l, Pet.floatSize == s) { Pet.floatSize = s; PetOverlay.refresh(ctx) } }
            }
            Sub("الشفافية", Modifier.padding(top = 8.dp))
            var a by remember { mutableFloatStateOf(Pet.floatAlpha) }
            Slider(a, { a = it }, valueRange = .35f..1f, onValueChangeFinished = { Pet.floatAlpha = a; PetOverlay.refresh(ctx) })
            SettingRow("ساعات الهدوء", if (Pet.quietFrom < 0) "متوقفة" else "لا فقاعات من ${Alerts.timeText(Pet.quietFrom)} إلى ${Alerts.timeText(Pet.quietTo)}") {
                Switch(Pet.quietFrom >= 0, { Pet.quietFrom = if (it) 23 * 60 else -1 })
            }
            Sub("لا يتكلم أثناء المكالمات. ومن إشعاره: «إخفاء ساعة» أو «إيقاف».", size = 11.sp)
            if (Pet.floating && can) Action("أرني فقاعة الآن", Modifier.fillMaxWidth().padding(top = 8.dp), filled = false) { PetOverlay.poke(ctx) }
        }
    }
}

/** Children learn with their friend: short questions by topic, a star for each right answer, and the friend celebrates. */
@Composable
fun KidsPage() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val sp = Pet.species ?: Species.CAT
    val ctx = LocalContext.current
    var topic by remember { mutableIntStateOf(0) }
    var i by remember(topic) { mutableIntStateOf(0) }
    var picked by remember(topic, i) { mutableStateOf<Int?>(null) }
    val clock = rememberPetClock()
    val hop = remember { Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val (name, qs) = Pet.lessons[topic]
    val q = qs[i % qs.size]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pet.lessons.forEachIndexed { k, (n, _) -> Choice(n, topic == k) { topic = k } }
        }
        PetSprite(sp, {
            val t = clock() / 1000f
            Pose(t = t, blink = (t % 3.5f) < .12f, hop = hop.value, flap = hop.value, happy = picked == q.answer)
        }, Modifier.size(150.dp))
        Text("⭐ ${Pet.stars}", color = P.gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Panel(color = P.card, pad = 16.dp) {
            Sub("$name · سؤال ${i % qs.size + 1} من ${qs.size}", color = P.gold, size = 12.sp)
            Text(q.q, color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        q.choices.forEachIndexed { k, ch ->
            val right = k == q.answer
            val bg = when { picked == null -> P.surface; right -> P.accent; picked == k -> P.danger.copy(alpha = .25f); else -> P.surface }
            Text(ch, color = if (picked != null && right) P.onAccent else P.ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(L.shape(18.dp)).background(bg).border(1.dp, P.line, L.shape(18.dp)).clickable(enabled = picked == null) {
                    picked = k
                    if (right) { Pet.stars = Pet.stars + 1; Haptics.tap(ctx, true); scope.launch { repeat(2) { hop.animateTo(1f, tween(170)); hop.animateTo(0f, tween(200)) } } }
                }.padding(vertical = 16.dp))
        }
        picked?.let {
            Text(if (it == q.answer) "أحسنت! ${Pet.name} فرحان بك 🎉" else "حاول مرة أخرى في المرة القادمة", color = if (it == q.answer) P.accent else P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Sub(q.why, size = 14.sp)
            Action("السؤال التالي ›", Modifier.fillMaxWidth()) { i++ }
        }
    }
}
