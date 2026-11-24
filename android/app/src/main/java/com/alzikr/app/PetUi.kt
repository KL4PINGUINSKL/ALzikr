package com.alzikr.app

import androidx.compose.foundation.layout.width

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.graphics.compositeOver

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
import androidx.compose.ui.draw.alpha
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
 * The friend's little world, simulated smoothly every frame: it eases into a walk and out of it, leans as it goes,
 * turns gradually, its tail and ears trail behind, it squashes on landing, and now and then a yarn ball rolls in —
 * it chases it and pats it with a paw, and the ball rolls and slows by itself.
 */
private class PetSim {
    var t = 0f
    var x = .5f; var v = 0f; var target = .5f
    var face = 1f; var faceTo = 1f
    var walk = 0f
    var hop = 0f; var hopV = 0f
    var sq = 0f; var sqV = 0f
    var lean = 0f
    var paw = 0f; var pawT = 0f
    var flap = 0f; var flapT = 0f
    var look = 0f; var lookTo = 0f
    var tilt = 0f
    var timer = 2f
    var mode = 0           // 0 idle, 1 wander, 2 play
    var playLeft = 0f
    var asleep = false
    // the yarn ball
    var ball = false; var bx = 0f; var bv = 0f; var spin = 0f
    var cooldown = 0f
    /** The yarn ball game (only for drawn friends that can pat it). */
    var toys = false

    fun jump(h: Float = 1f) { if (hop <= 0.001f) { sq = -.55f * h; hopV = 3.2f * h } }
    fun patBall() {
        if (cooldown > 0f) return
        pawT = .38f; cooldown = .9f
        val dir = if (bx >= x) 1f else -1f
        bv += dir * (0.35f + kotlin.random.Random.nextFloat() * .35f)
        if (kotlin.random.Random.nextInt(3) == 0) jump(.45f)
    }

    fun step(dt: Float) {
        t += dt
        cooldown = (cooldown - dt).coerceAtLeast(0f)
        // ---- choose what to do
        timer -= dt
        if (asleep) { mode = 0; target = x }
        else if (timer <= 0f) {
            val r = kotlin.random.Random.nextFloat()
            when {
                mode == 2 && playLeft > 0f -> {}
                r < .42f -> { mode = 1; target = kotlin.random.Random.nextFloat() * .86f + .07f; timer = 3f + kotlin.random.Random.nextFloat() * 3f }
                r < .62f -> { mode = 0; timer = 2f + kotlin.random.Random.nextFloat() * 3f; lookTo = kotlin.random.Random.nextFloat() * 1.4f - .7f }
                r < .72f -> { jump(); timer = 1.5f }
                r < .80f -> { flapT = .7f; timer = 1.5f }
                r < .88f -> { faceTo = -faceTo; timer = 1.2f }
                else -> if (!ball && toys) {
                    mode = 2; playLeft = 14f + kotlin.random.Random.nextFloat() * 8f; ball = true
                    bx = if (x < .5f) (x + .45f).coerceAtMost(.95f) else (x - .45f).coerceAtLeast(.05f); bv = if (bx > x) -.25f else .25f
                    timer = 1f
                } else timer = 1f
            }
        }
        // ---- play: run to the ball and pat it
        if (mode == 2) {
            playLeft -= dt
            val side = if (bx >= x) -1f else 1f
            target = (bx + side * .09f).coerceIn(.03f, .97f)
            if (kotlin.math.abs(x - target) < .03f && kotlin.math.abs(bv) < .25f) { faceTo = if (bx >= x) 1f else -1f; patBall() }
            if (playLeft <= 0f) { mode = 0; timer = 2f; bv = if (bx > .5f) .5f else -.5f }
        }
        // ---- motion: a soft spring toward the target, capped speed
        val d = target - x
        val a = (d * 7f - v * 4.2f).coerceIn(-1.6f, 1.6f)
        v = (v + a * dt).coerceIn(-.38f, .38f)
        if (kotlin.math.abs(d) < .004f && kotlin.math.abs(v) < .01f) v = 0f
        x = (x + v * dt).coerceIn(0f, 1f)
        if (kotlin.math.abs(v) > .025f) faceTo = if (v > 0) 1f else -1f
        face += (faceTo - face) * (dt * 7f).coerceAtMost(1f)
        val speed = (kotlin.math.abs(v) / .3f).coerceIn(0f, 1f)
        walk += dt * (1.1f + speed * 1.6f) * speed.coerceAtLeast(.0f)
        lean += ((speed * 7f) - lean) * (dt * 5f).coerceAtMost(1f)
        // ---- hop with gravity; squash and stretch on a damped spring
        if (hopV != 0f || hop > 0f) {
            hop += hopV * dt; hopV -= 11f * dt
            if (hop <= 0f) { hop = 0f; if (hopV < -.5f) sqV += -hopV * 2.4f; hopV = 0f }
        }
        val sqA = -170f * sq - 11f * sqV
        sqV += sqA * dt; sq += sqV * dt
        // ---- paw, wings, head
        if (pawT > 0f) { pawT -= dt; paw = kotlin.math.sin((1f - (pawT / .38f).coerceIn(0f, 1f)) * Math.PI.toFloat()) } else paw = 0f
        if (flapT > 0f) { flapT -= dt; flap = (kotlin.math.sin(flapT * 14f) * .5f + .5f) * (flapT / .7f).coerceIn(0f, 1f) } else flap = 0f
        if (kotlin.random.Random.nextFloat() < dt * .25f) lookTo = kotlin.random.Random.nextFloat() * 1.2f - .6f
        if (mode == 2) lookTo = -.35f   // eyes on the ball
        look += (lookTo - look) * (dt * 3f).coerceAtMost(1f)
        // ---- the ball rolls and slows; it bounces softly at the edges, and leaves when play is over
        if (ball) {
            bx += bv * dt; bv *= kotlin.math.exp(-1.1f * dt); spin += bv * dt * 40f
            if (mode == 2) { if (bx < .02f) { bx = .02f; bv = -bv * .55f }; if (bx > .98f) { bx = .98f; bv = -bv * .55f } }
            else if (bx < -.15f || bx > 1.15f || kotlin.math.abs(bv) < .02f) ball = false
        }
    }
    fun pose(happy: Boolean) = Pose(t = t, blink = !asleep && (t % 4.3f) < .13f, walk = walk, speed = (kotlin.math.abs(v) / .3f).coerceIn(0f, 1f),
        hop = hop, flap = flap, sleep = asleep, happy = happy, face = face, lean = lean, squash = sq, paw = paw, look = look, tilt = tilt)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PetStage(modifier: Modifier, height: Int = 52, bubbleBelow: Boolean = true, paper: androidx.compose.ui.graphics.Color = P.bg) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val sp = Pet.species ?: return
    val line = Pet.lineStyle(P.accent, paper)
    val sim = remember { PetSim() }
    var frame by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var said by remember { mutableStateOf<Pet.Say?>(null) }
    var n by remember { mutableIntStateOf(Random.nextInt(100)) }
    val happy = Pet.happy()
    // One smooth simulation, stepped every frame (paused by Android when the app is not on screen).
    // Paused under reduced motion; fewer steps when saving battery (Performance Manager).
    LaunchedEffect(sp) {
        var last = 0L
        while (true) {
            if (!com.alzikr.app.pets.PetPerf.codeMotion(com.alzikr.app.pets.Place.APP)) { last = 0L; delay(500); continue }
            androidx.compose.runtime.withFrameMillis { now ->
                val dt = if (last == 0L) .016f else ((now - last) / 1000f).coerceIn(0f, .05f)
                last = now; sim.step(dt); frame = now
            }
            // At most the place's frame budget (30 fps, 15 when saving), instead of every display frame (up to 120 Hz).
            val step = 1000L / com.alzikr.app.pets.PetPerf.fps(com.alzikr.app.pets.Place.APP)
            delay((step - frame % step - 2).coerceAtLeast(1))   // the same shared boundaries as the friends' clocks
        }
    }
    // Behaviour engine: what the friend does by itself (personality, time of day, the day's mood, its evolution stage).
    val def = com.alzikr.app.pets.PetPacks.registry().species(sp.name)
    val stage = com.alzikr.app.pets.Evolution.stage(def, Progress.level())
    var act by remember { mutableStateOf(com.alzikr.app.pets.PetState.IDLE) }
    LaunchedEffect(sp, stage.id) {
        delay(800)
        while (true) {
            // An interaction from the menu, or quiet reading, takes precedence.
            val asked = PetActions.request?.second ?: 0L
            if (PetQuiet.active || System.currentTimeMillis() - asked < 22_000) { delay(3_000); continue }
            val pick = com.alzikr.app.pets.Behavior.next(def?.personality ?: com.alzikr.app.pets.Personality(), java.time.LocalTime.now().hour, Pet.mood(), Pet.asleep(), stage)
            act = pick.state; delay(pick.durationMs)
        }
    }
    LaunchedEffect(Unit) { while (true) { sim.asleep = Pet.asleep(); delay(30_000) } }
    fun speak() {
        said = Pet.say(n++)
        when (n % 3) { 0 -> sim.jump(); 1 -> sim.flapT = .7f; else -> { sim.faceTo = -sim.faceTo; sim.timer = 1f } }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(150_000, 300_000))
            if (Pet.talks && !Pet.asleep() && !PetQuiet.active) speak()
        }
    }
    // The interactions menu: the four states (كلّمني = calm + a word, العب معي = happy, تمشَّ = walk, نم = sleep).
    val req = PetActions.request
    LaunchedEffect(req) {
        val (want, at) = req ?: return@LaunchedEffect
        if (System.currentTimeMillis() - at > 3_000) return@LaunchedEffect
        when (want) {
            com.alzikr.app.pets.PetState.IDLE -> { act = want; speak() }
            com.alzikr.app.pets.PetState.HAPPY -> { act = want; sim.mode = 2; sim.playLeft = 12f; sim.ball = true; sim.bx = sim.x; sim.bv = .4f; sim.jump(.6f) }
            com.alzikr.app.pets.PetState.WALK -> { act = want; sim.mode = 1; sim.target = if (sim.x > .5f) .15f else .85f; sim.timer = 6f }
            com.alzikr.app.pets.PetState.SLEEP -> { act = want; said = Pet.Say("سأغفو قليلًا… 💤") }
        }
        delay(if (want == com.alzikr.app.pets.PetState.SLEEP) 20_000 else 10_000)
        act = com.alzikr.app.pets.PetState.IDLE
    }
    val cheer = Pet.cheerNow()
    LaunchedEffect(cheer) { if (cheer != null) { said = Pet.Say(cheer); act = com.alzikr.app.pets.PetState.HAPPY; sim.jump(); delay(650); sim.jump(.7f) } }
    LaunchedEffect(said) { if (said != null) { delay(7000); said = null } }

    BoxWithConstraints(modifier.height(height.dp)) {
        val box = height.dp
        val boxPx = with(LocalDensity.current) { box.toPx() }
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        // The yarn ball, on the same ground line.
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE") val f = frame
            if (sim.ball) {
                val r = boxPx * .13f
                val cx = r + sim.bx * (size.width - 2 * r)
                drawYarn(cx, size.height * .93f - r, r, sim.spin, line)
            }
        }
        Box(Modifier.offset { IntOffset((sim.x * (widthPx - boxPx)).toInt().also { @Suppress("UNUSED_VARIABLE") val f = frame }, 0) }.size(box)
            .combinedClickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null,
                onLongClick = { Nav.morePage = "pet"; Nav.go("more") }) { Progress.bond("tap" + System.currentTimeMillis() / 1_800_000L); speak() }) {
            // The animation plays inside; the simulation moves it: glide, hop, a soft squash, a lean, and a turn through the middle.
            PetLottie(sp, Modifier.fillMaxSize().graphicsLayer {
                @Suppress("UNUSED_VARIABLE") val f = frame
                val turn = (kotlin.math.abs(sim.face) * .85f + .15f) * (if (sim.face < 0) -1f else 1f)
                scaleX = turn * (1f + sim.sq * .08f); scaleY = 1f - sim.sq * .1f
                translationY = -sim.hop * size.height * .18f
                rotationZ = sim.lean * .4f * (if (sim.face < 0) -1f else 1f)
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, .95f)
            }, speed = if (sim.asleep || act == com.alzikr.app.pets.PetState.SLEEP) .35f else 1f, scale = stage.scale,
                // Quiet while reading: a still, calm friend.
                playing = !PetQuiet.active,
                state = when { PetQuiet.active -> com.alzikr.app.pets.PetState.IDLE; sim.asleep -> com.alzikr.app.pets.PetState.SLEEP; else -> act })
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
    GText(text, color = P.ink, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
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
    AppSheet(onDismissRequest = close) {
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
    val line = Pet.lineStyle(P.accent, P.card)
    @Suppress("UNUSED_VARIABLE") val unused = clock to line
    val ctx = LocalContext.current
    // The same even tiles as the looks: four per row, ordered by the level they open at, all shown.
    val all = Species.entries.sortedBy { Progress.speciesLevel(it) }
    TileGrid(all, cols = 4, rows = (all.size + 3) / 4) { s, m ->
        val open = Progress.speciesOpen(s)
        PictureTile(s.ar, s == chosen, open, Progress.speciesLevel(s), m, onClick = {
            if (open) pick(s) else android.widget.Toast.makeText(ctx, "يُفتح ${s.ar} في المستوى ${Progress.speciesLevel(s)}", android.widget.Toast.LENGTH_SHORT).show()
        }) { pm -> PetLottie(s, pm, playing = s == chosen, place = com.alzikr.app.pets.Place.GRID) }  // only the chosen one moves
    }
}

/** A tab to open the pet page on (e.g. "room"). */
object PetTabs { var start: String? = null }

/** The friend's page, in tabs: the friend, its look, its room, memories, bag, and settings. */
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
        Hint("pet2", "🐾", "صفحة صديقك في أقسام: «صديقي» لحاله وتحدّيه واللعب معه، و«المظهر» لاختياره وشكله، و«الغرفة» و«الذكريات» و«الحقيبة»، و«الإعدادات». اضغط عليه ليقول شيئًا، ومطولًا لتفتح هذه الصفحة.")
        // The page in tabs (organised); without a friend yet, it opens on choosing one.
        var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(PetTabs.start ?: "friend") }
        LaunchedEffect(Unit) { PetTabs.start = null }
        if (sp == null) tab = "look"
        TabGrid(listOf(Triple("friend", "🐾", "صديقي"), Triple("look", "🎨", "المظهر"), Triple("room", "🏠", "الغرفة"),
            Triple("album", "📔", "الذكريات"), Triple("bag", "🎒", "الحقيبة"), Triple("settings", "⚙", "الإعدادات")), tab) { tab = it }
        if (tab == "friend" && sp != null) Panel(color = P.card, pad = 12.dp) {
            Box(Modifier.fillMaxWidth()) { PetStage(Modifier.fillMaxWidth(), height = 110, paper = P.card) }
            Text(Pet.name, color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            val mood = Pet.mood()
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                repeat(5) { k -> val on = mood >= (k + 1) * 20 - 10; Glyph(if (on) Friendship.heart else "♡", tint = if (on) (if (Friendship.has("golden")) P.gold else P.accent) else P.line, size = 26.dp, modifier = Modifier.padding(horizontal = 3.dp)) }
            }
            Sub(if (Pet.asleep()) "نائم الآن" else when { mood >= 80 -> "فرحان جدًا بيومك، بارك الله فيك"; mood >= 50 -> "سعيد، أكمل يومك بخير"; else -> "ينتظر صلواتك وأذكارك ليفرح" }, Modifier.fillMaxWidth(), size = 12.sp)
            Sub("يفرح كلما علّمت صلاة ✓، وأتممت أذكار الصباح والمساء، ووردك، وسبّحت", Modifier.fillMaxWidth(), size = 11.sp)
        }
        if (tab == "friend" && sp != null && petHasMoves(sp)) Panel(pad = 12.dp) {
            Title("العب معه")
            Sub("للمتعة والصداقة فقط: ليست عبادة ولا تزيد الخبرة.", size = 11.sp)
            Box(Modifier.padding(top = 8.dp)) { PetInteractions() }
        }
        if (tab == "friend" && sp != null) Panel(pad = 14.dp) {
            LaunchedEffect(Unit) { Pet.noteGoodDay(); Progress.bond("visit", 2) }
            Sub(if (Pet.daysTogether() > 0) "معًا منذ ${Quran.digits(Pet.daysTogether())} ${if (Pet.daysTogether() in 3..10) "أيام" else "يوم"}" else "صداقتكما تبدأ اليوم", size = 12.sp)
            // Evolution: the friend grows through stages with the level (it looks a little bigger and does more by itself).
            run {
                val st = com.alzikr.app.pets.Evolution.stages(com.alzikr.app.pets.PetPacks.registry().species(sp.name))
                val cur = com.alzikr.app.pets.Evolution.stage(com.alzikr.app.pets.PetPacks.registry().species(sp.name), Progress.level())
                val next = st.firstOrNull { it.fromLevel > Progress.level() }
                Sub("🌱 مرحلته: ${cur.name}" + (next?.let { " · يصير «${it.name}» في المستوى ${Quran.digits(it.fromLevel)}" } ?: ""), size = 12.sp)
            }
            if (Progress.showLevels) {
                // ✨ experience: Quran, hadith, learning and the day's worship
                val lv = Progress.level(); val x = Progress.xp
                val from = Progress.need(lv); val to = Progress.need(lv + 1)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✨ ${Friendship.title()} · المستوى ${Quran.digits(lv)}", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(Quran.digits(x), color = P.gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                androidx.compose.material3.LinearProgressIndicator(progress = { ((x - from).toFloat() / (to - from).coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(L.shape(4.dp)), color = P.gold, trackColor = P.line)
                Sub("بقي ${Quran.digits(to - x)} ✨ للمستوى ${Quran.digits(lv + 1)}: من قراءة القرآن والأحاديث وشروحها، والحفظ والتجويد، والصلوات والأذكار.", size = 11.sp)
                var how by remember { mutableStateOf(false) }
                Text(if (how) "▴ كيف أكسب النقاط؟" else "❓ كيف أكسب النقاط؟", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp).clip(L.shape(8.dp)).clickable { how = !how }.padding(vertical = 4.dp))
                if (how) Column(Modifier.fillMaxWidth().padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Progress.Kind.entries.forEach { k ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(k.ar, color = P.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Sub(Progress.how(k), size = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                                Text("+${Quran.digits(k.xp)} ✨", color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Sub("اليوم ${Quran.digits(Progress.todayXp(k))} من ${Quran.digits(k.cap)}", size = 10.sp)
                            }
                        }
                    }
                    Text("مجموع اليوم: ${Quran.digits(Progress.todayTotal())} من ${Quran.digits(Progress.DAY_MAX)} ✨ (الحد اليومي لكل شيء معًا، والختمة تكمّل إليه ولا تتجاوزه)",
                        color = P.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Sub("كل شيء يُحسب مرة في اليوم، ولكل نوع حدّ يومي. هذه مكافآت داخل التطبيق فقط، لا مقياس للأجر عند الله.", size = 11.sp)
                }
                Sub("كل مستوى يحتاج ${Quran.digits(Progress.stepGrowth(lv + 1))} ✨ أكثر من سابقه (٧٥ حتى المستوى ٢٥، ثم ١٢٥ حتى ٥٠، ثم ١٥٠): المستوى ${Quran.digits(lv + 1)} يحتاج ${Quran.digits(Progress.step(lv + 1))}، والذي بعده ${Quran.digits(Progress.step(lv + 2))}.", size = 11.sp)
                Action("🎁 مكافآتي وأوسمتي", Modifier.fillMaxWidth().padding(top = 10.dp)) { Nav.morePage = "rewards"; Nav.go("more") }
            }
            val st = Pet.streak()
            Text("🔥 ${if (st > 0) "${Quran.digits(st)} ${if (st in 3..10) "أيام" else "يوم"} طيبة متتالية" else "ابدأ سلسلة أيامك الطيبة اليوم"}" +
                (Pet.bestStreak().takeIf { it > st }?.let { " · أفضل سلسلة ${Quran.digits(it)}" } ?: ""),
                color = P.ink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Sub("اليوم الطيب: أن تمتلئ قلوب ${Pet.name} ثلاثة على الأقل", size = 11.sp)
        }
        if (tab == "friend" && sp != null && Progress.showLevels) Panel(pad = 14.dp) { FriendshipPanel() }
        if (tab == "friend" && sp != null && !Pet.kids) Panel(pad = 14.dp) { MissionsPanel() }
        if (tab == "friend" && sp != null && Pet.kids) Panel(color = if (Pet.challengeDone) P.card else P.accent.copy(alpha = .10f), pad = 14.dp) {
            Text("🎯 تحدي اليوم", color = P.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(Pet.challenge(), color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            if (Pet.challengeDone) Sub("أتممته، تقبّل الله منك ✓ (تحدٍّ جديد غدًا)", size = 12.sp)
            else Action("فعلتُه ✓", Modifier.fillMaxWidth().padding(top = 8.dp)) { Pet.doChallenge() }
        }
        if (tab == "settings" && sp != null) Panel(pad = 14.dp) {
            Title("على الشاشة الرئيسية")
            Sub("ضع ${Pet.name} على شاشة هاتفك الرئيسية: صورته وقلوب يومه وكلمة منه، وزرّا «كلّمني» و«سبّح». أو اختصارًا في رئيسية التطبيق.", size = 12.sp)
            val onPhone = remember(r) { PetWidget.has(ctx) }
            Action(if (onPhone) "✓ على شاشة الهاتف (أضف نسخة أخرى)" else "🏠 ضعه على شاشة الهاتف", Modifier.fillMaxWidth().padding(top = 8.dp), filled = !onPhone) {
                if (!PetWidget.pin(ctx)) android.widget.Toast.makeText(ctx, "اضغط مطولًا على الشاشة الرئيسية ← الأدوات ← ALzikr ← «صديقي»", android.widget.Toast.LENGTH_LONG).show()
            }
            val inApp = "pet" in Home.shortcuts
            Action(if (inApp) "✓ في رئيسية التطبيق (إزالة)" else "➕ اختصار في رئيسية التطبيق", Modifier.fillMaxWidth().padding(top = 6.dp), filled = false) {
                Home.shortcuts = if (inApp) Home.shortcuts - "pet" else listOf("pet") + Home.shortcuts
            }
        }
        if (tab == "room" && sp != null) { PetRoom(); Sub("لكل صديق بيته، يتبدّل ضوؤه مع وقت يومك، وتزيد أغراضه مع المستوى (أضواء معلّقة في الخامس، وحديقة في العاشر). تُظهر الأغراض وتخفيها من «الحقيبة».", size = 12.sp) }
        if (tab == "album") PetAlbumPage()
        if (tab == "bag") PetBag()
        if (tab == "look") {
        Panel(pad = 12.dp) {
            Title("اختر صديقك")
            Sub("مرتّبة بحسب المستوى الذي تُفتح فيه.", size = 11.sp)
            Box(Modifier.padding(top = 8.dp)) { SpeciesGrid(sp) { Pet.species = it } }
        }
        if (sp == null) return@Column
        val looks = PetLooks.of(sp)
        if (looks.size > 1) Panel(pad = 12.dp) {
            val cur = PetLooks.chosen(sp)
            val opened = looks.indices.count { Progress.lookOpen(sp, it) }
            Title("شكل ${Pet.nameOf(sp)}")
            Sub("${Quran.digits(looks.size)} شكلًا · المفتوح لك ${Quran.digits(opened)}", size = 11.sp)
            // An even grid, three per row; two rows, then «عرض كل الأشكال». Opens fully when the chosen look is further down.
            Box(Modifier.padding(top = 10.dp)) {
                TileGrid(looks.withIndex().toList(), cols = 3, rows = 2, startOpen = looks.indexOfFirst { it.id == cur?.id } >= 6, noun = "الأشكال") { (i, lk), m ->
                    val open = Progress.lookOpen(sp, i)
                    PictureTile(lk.name, lk.id == cur?.id, open, Progress.lookLevel(sp, i), m, onClick = {
                        if (open) PetLooks.choose(sp, lk.id)
                        else android.widget.Toast.makeText(ctx, "يُفتح هذا الشكل في المستوى ${Progress.lookLevel(sp, i)}", android.widget.Toast.LENGTH_SHORT).show()
                    }) { pm -> PetLottie(sp, pm, look = lk, playing = lk.id == cur?.id, place = com.alzikr.app.pets.Place.GRID) }  // only the chosen look moves
                }
            }
        }
        Panel(pad = 12.dp) {
        Title("الاسم")
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var name by remember(sp) { mutableStateOf(Pet.nameOf(sp)) }
            OutlinedTextField(name, { name = it; if (it.isNotBlank()) Pet.setName(sp, it) }, singleLine = true, label = { Text("اسم ${sp.ar}") }, modifier = Modifier.weight(1f))
            var me by remember { mutableStateOf(Pet.userName) }
            OutlinedTextField(me, { me = it; Pet.userName = it }, singleLine = true, label = { Text("بماذا يناديك؟") }, modifier = Modifier.weight(1f))
        }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("معاينة وحالات", Modifier.weight(1f), filled = false) { Nav.morePage = "petpreview"; Nav.go("more") }
            Action("الحزم والحركة", Modifier.weight(1f), filled = false) { Nav.morePage = "petpacks"; Nav.go("more") }
        }
        Sub("رسوم ALzikr Pet Studio أصلية لنا، وغيرها من LottieFiles (رخصة Lottie Simple License).", size = 11.sp)
        }
        if (tab == "settings") {

        Panel(pad = 14.dp) {
            Title("لمن؟")
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("للكبار: يذكّرني", !Pet.kids) { Pet.kids = false }
                Choice("للصغار: يتعلّم معي", Pet.kids) { Pet.kids = true }
            }
            if (Pet.kids) Action("⭐ تعلّم مع ${Pet.name}", Modifier.fillMaxWidth().padding(top = 10.dp)) { Nav.morePage = "kids"; Nav.go("more") }
        }
        Panel(pad = 14.dp) {
            Title("الهدوء")
            SettingRow("هادئ أثناء القراءة", "لا يتكلم ولا يتحرك وأنت تقرأ القرآن أو الحديث") {
                Switch(Store.prefs.getBoolean("pet_quiet_reading", true), { Store.prefs.edit().putBoolean("pet_quiet_reading", it).apply(); Store.bump() })
            }
            SettingRow("يقترح عليك شرح الحديث", "بعد قراءة حديث له شرح في الموسوعة، سطر هادئ تحته") {
                Switch(Store.prefs.getBoolean("pet_suggest", true), { Store.prefs.edit().putBoolean("pet_suggest", it).apply(); Store.bump() })
            }
            Action("الحركة والبطارية والحزم", Modifier.fillMaxWidth().padding(top = 8.dp), filled = false) { Nav.morePage = "petpacks"; Nav.go("more") }
        }
        Panel(pad = 14.dp) {
            Title("أين يعيش في التطبيق؟")
            Wrap(Modifier.padding(top = 6.dp), gap = 6.dp) {
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
            Sub("طريقة ظهوره", Modifier.padding(top = 8.dp))
            Wrap(gap = 6.dp) {
                listOf("normal" to "🐾 عادي", "mini" to "🫧 مصغّر", "quiet" to "💤 هادئ").forEach { (k, l) -> Choice(l, Pet.floatMode == k) { Pet.floatMode = k; PetOverlay.refresh(ctx) } }
            }
            Sub(when (Pet.floatMode) { "mini" -> "أيقونة صغيرة ثابتة على الحافة، اضغطها فيكبر لحظات"; "quiet" -> "ثابت بلا فقاعات ولا تجوّل ولا تنبيهات، ولا يتدخل في استخدامك"; else -> "بحجمه، يتحرك ويتكلم حسب الإعدادات أدناه" }, size = 11.sp)
            if (Pet.floating && !can) Text("اضغط هنا لمنح الإذن، ثم ارجع", color = P.danger, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }.padding(6.dp))
            if (Pet.floating && can) LaunchedEffect(Unit) { PetOverlay.start(ctx) }
            Sub("يذكّرك كل", Modifier.padding(top = 8.dp))
            Wrap(gap = 6.dp) {
                listOf(15, 30, 60, 120, 180).forEach { m -> Choice(if (m < 60) "$m دقيقة" else "${m / 60} ساعة", Pet.every == m) { Pet.every = m; PetOverlay.refresh(ctx) } }
            }
            Sub("بماذا؟", Modifier.padding(top = 8.dp))
            Wrap(gap = 6.dp) {
                listOf("mix" to "منوّع", "dhikr" to "ذكر", "ayah" to "آية", "hadith" to "حديث").forEach { (k, l) -> Choice(l, Pet.content == k) { Pet.content = k } }
            }
            Sub("الحجم", Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(48 to "صغير", 64 to "متوسط", 84 to "كبير").forEach { (s, l) -> Choice(l, Pet.floatSize == s) { Pet.floatSize = s; PetOverlay.refresh(ctx) } }
            }
            Sub("الشفافية", Modifier.padding(top = 8.dp))
            var a by remember { mutableFloatStateOf(Pet.floatAlpha) }
            Slider(a, { a = it }, valueRange = .35f..1f, onValueChangeFinished = { Pet.floatAlpha = a; PetOverlay.refresh(ctx) })
            SettingRow("يتحرك دائمًا", "متوقف: صورة ثابتة تتحرك فقط حين يتكلم (أوفر للبطارية)") { Switch(Pet.floatMove, { Pet.floatMove = it }) }
            SettingRow("يتجوّل على حافة الشاشة", "يمشي قليلًا صعودًا ونزولًا كل نصف دقيقة تقريبًا ثم يستريح") { Switch(Pet.wander, { Pet.wander = it }) }
            SettingRow("ضغطتان = تسبيحة", "تُحسب مع سبحتك: ٣٣ سبحان الله، ٣٣ الحمد لله، ٣٤ الله أكبر") { Switch(Pet.floatTasbih, { Pet.floatTasbih = it }) }
            SettingRow("ينبّهك قبل الصلاة", "كلمة قبل كل صلاة بعشر دقائق") { Switch(Pet.prayerNudge, { Pet.prayerNudge = it }) }
            SettingRow("ساعات الهدوء", if (Pet.quietFrom < 0) "متوقفة" else "لا فقاعات من ${Alerts.timeText(Pet.quietFrom)} إلى ${Alerts.timeText(Pet.quietTo)}") {
                Switch(Pet.quietFrom >= 0, { Pet.quietFrom = if (it) 23 * 60 else -1 })
            }
            Sub("اضغط عليه ليقول شيئًا، وضغطتين ليسبّح، ومطولًا لفتح التطبيق، واسحبه إلى ✕ لإخفائه. ينام بعد العشاء. لا يتكلم أثناء المكالمات. ومن إشعاره: «إخفاء ساعة» أو «إيقاف».", size = 11.sp)
            if (Pet.floating && can) Action("أرني فقاعة الآن", Modifier.fillMaxWidth().padding(top = 8.dp), filled = false) { PetOverlay.poke(ctx) }
        }
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
        Wrap(gap = 6.dp) {
            Pet.lessons.forEachIndexed { k, (n, _) -> Choice(n, topic == k) { topic = k } }
        }
        PetLottie(sp, place = com.alzikr.app.pets.Place.KIDS, modifier = Modifier.size(170.dp).graphicsLayer { translationY = -hop.value * 60f; val sq = 1f + hop.value * .06f; scaleX = sq; scaleY = sq })
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

/** ❤ Friendship: its own points and its own gifts (frames, themes, badges, perks), never added to the level. */
@Composable
fun FriendshipPanel() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val bp = Progress.bondPoints; val (bf, bt) = Progress.bondRange()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${Friendship.heart} الصداقة: ${Progress.bondName()}", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("${Quran.digits(bp)} ❤", color = P.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
    if (bt > 0) androidx.compose.material3.LinearProgressIndicator(progress = { ((bp - bf).toFloat() / (bt - bf)).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(L.shape(4.dp)), color = P.accent, trackColor = P.line)
    Sub("نقاط منفصلة عن نقاط المستوى ✨: تكبر بوقتك مع صديقك فقط، ولها جوائزها الخاصة.", size = 11.sp)
    Friendship.next()?.let { g ->
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(L.shape(12.dp)).background(P.card).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(g.icon, fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f)) {
                Text("الجائزة التالية: ${g.name}", color = P.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Sub("${g.about} · بقي ${Quran.digits(g.bond - bp)} ❤", size = 11.sp)
            }
        }
    }
    var how by remember { mutableStateOf(false) }
    Text(if (how) "▴ كيف تكبر الصداقة؟" else "❓ كيف تكبر الصداقة؟", color = P.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp).clip(L.shape(8.dp)).clickable { how = !how }.padding(vertical = 4.dp))
    if (how) Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf("زيارة صفحة صديقك" to "+٢ مرة في اليوم", "لمسه أو الحديث معه (في التطبيق أو العائم أو الأداة)" to "+١ كل نصف ساعة",
            "اللعب معه: كلّمني، العب معي، تمشَّ، نم" to "+١ كل نصف ساعة", "كل وظيفة يومية" to "+٤", "كل تحدٍّ (ثلاثة كل يوم)" to "+٥",
            "سؤال اليوم: تقرأ جوابه في التطبيق" to "+٦", "صندوق الأسبوع: خمسة أيام كاملة" to "+٢٠ فوق الحد").forEach { (a, b) ->
            Row(Modifier.fillMaxWidth()) { Sub(a, Modifier.weight(1f), size = 12.sp); Text(b, color = P.accent, fontSize = 12.sp) }
        }
        Sub("الحد اليومي ${Quran.digits(Progress.BOND_DAY)} ❤، وصندوق الأسبوع فوقه.", size = 11.sp)
    }
    if (Friendship.has("nick")) {
        var edit by remember { mutableStateOf(false) }
        SettingRow("🏷 لقب صديقك", Friendship.nick.ifBlank { "«${Progress.title()}» (لقب المستوى)" }) { Action("تغيير", filled = false) { edit = true } }
        if (edit) {
            var text by remember { mutableStateOf(Friendship.nick) }
            androidx.compose.material3.AlertDialog(onDismissRequest = { edit = false }, containerColor = P.surface,
                title = { Text("لقب صديقك", color = P.ink) },
                text = { androidx.compose.material3.OutlinedTextField(text, { text = it.take(24) }, singleLine = true, placeholder = { Text(Progress.title()) }) },
                confirmButton = { androidx.compose.material3.TextButton({ Friendship.nick = text; edit = false }) { Text("حفظ", color = P.accent) } },
                dismissButton = { androidx.compose.material3.TextButton({ Friendship.nick = ""; edit = false }) { Text("لقب المستوى", color = P.muted) } })
        }
    }
}
