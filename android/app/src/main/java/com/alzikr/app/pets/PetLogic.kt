package com.alzikr.app.pets

import org.json.JSONObject
import kotlin.random.Random

/* Engine logic with no Android dependency: state resolution, the state machine, behaviour, evolution, memory, validation. */

/** What to draw for a state: the source found, the state it really belongs to, and the code motion to add. */
data class Resolved(val source: Source, val state: PetState, val motion: Motion, val fellBack: Boolean)

object StateResolver {
    /**
     * The state's own source if the skin has one; otherwise the next in its fallback chain, with the requested state's
     * motion added by code (so a "jump" still hops on a skin that only loops). Last resort: the skin's still fallback.
     */
    fun resolve(skin: Skin, want: PetState): Resolved? {
        for (s in PetState.chain(want)) skin.states[s]?.let { src -> return Resolved(src, s, src.motion ?: if (s == want) Motion.NONE else want.motion, s != want) }
        return skin.fallback?.let { Resolved(it, PetState.IDLE, want.motion, true) }
    }
}

/**
 * The animation state machine: one state at a time; a timed state (a hop, a wave) returns to the base state when it ends.
 * Requests of lower priority than the running timed state are ignored, so a celebration is not cut by a stray "curious".
 */
class PetStateMachine(private var base: PetState = PetState.IDLE) {
    var state: PetState = base; private set
    private var until = 0L
    private var priority = 0
    fun setBase(s: PetState, now: Long) { base = s; if (now >= until) state = s }
    fun request(s: PetState, now: Long, durationMs: Long = 0, prio: Int = 1): Boolean {
        if (now < until && prio < priority) return false
        state = s; priority = prio; until = if (durationMs > 0) now + durationMs else 0L
        if (durationMs <= 0) base = s
        return true
    }
    /** Call every frame (or tick): ends a finished timed state. */
    fun tick(now: Long): PetState { if (until in 1..now) { state = base; until = 0; priority = 0 }; return state }
}

/** Evolution: the stage that fits a level. Species may define their own; otherwise these five. */
object Evolution {
    val defaultStages = listOf(
        Stage("baby", "صغير", 1, .86f), Stage("curious", "فضولي", 3, .92f), Stage("tame", "أليف", 5, 1f),
        Stage("special", "مميّز", 8, 1.04f), Stage("elder", "رفيق قديم", 11, 1.06f),
    )
    fun stages(sp: SpeciesDef?) = sp?.stages?.takeIf { it.isNotEmpty() }?.sortedBy { it.fromLevel } ?: defaultStages
    fun stage(sp: SpeciesDef?, level: Int): Stage = stages(sp).last { level >= it.fromLevel }
    /** States a stage may show by itself (a little friend does not "read" yet; an old friend has every behaviour). */
    fun allowed(stage: Stage, s: PetState): Boolean = when (stage.id) {
        "baby" -> s in setOf(PetState.IDLE, PetState.WALK, PetState.SLEEP, PetState.HAPPY, PetState.JUMP, PetState.YAWN, PetState.CURIOUS, PetState.APPEAR, PetState.DISAPPEAR, PetState.TALK, PetState.WAKE_UP)
        "curious" -> s != PetState.READING && s != PetState.THINKING && s != PetState.CELEBRATE
        else -> true
    }
}

/** Behaviour: what the friend does next by itself, from its personality, the time of day, the day's mood and its stage. */
object Behavior {
    data class Pick(val state: PetState, val durationMs: Long)
    /** [hour] 0..23, [mood] 0..100, [asleep] the app's sleep window (after Isha). */
    fun next(p: Personality, hour: Int, mood: Int, asleep: Boolean, stage: Stage, rnd: Random = Random): Pick {
        if (asleep) return Pick(if (rnd.nextFloat() < .08f) PetState.YAWN else PetState.SLEEP, 20_000)
        val w = LinkedHashMap<PetState, Float>()
        w[PetState.IDLE] = 3f
        w[PetState.WALK] = 1f + 2f * p.energy
        w[PetState.RUN] = .6f * p.energy
        w[PetState.SIT] = 1f + (1 - p.energy)
        w[PetState.PLAY] = 1.5f * p.playful
        w[PetState.CURIOUS] = 1.2f * p.curious
        w[PetState.THINKING] = .5f * p.curious
        w[PetState.TALK] = .8f * p.chatty
        w[PetState.YAWN] = .3f + .6f * p.sleepy
        when (hour) {
            in 4..8 -> { w[PetState.STRETCH] = 2f; w[PetState.WAKE_UP] = 1f; w[PetState.YAWN] = (w[PetState.YAWN] ?: 0f) + 1f }
            in 9..16 -> { w[PetState.WALK] = (w[PetState.WALK] ?: 0f) * 1.4f; w[PetState.PLAY] = (w[PetState.PLAY] ?: 0f) * 1.4f }
            in 17..20 -> { w[PetState.SIT] = (w[PetState.SIT] ?: 0f) * 1.5f; w[PetState.READING] = 1.2f; w[PetState.RUN] = 0f }
            else -> { w[PetState.SLEEP] = 1f + 2f * p.sleepy; w[PetState.RUN] = 0f; w[PetState.PLAY] = (w[PetState.PLAY] ?: 0f) * .4f }
        }
        if (mood >= 80) { w[PetState.HAPPY] = 2f; w[PetState.LAUGH] = .8f }
        // A rare move now and then, so it is never fully predictable.
        if (rnd.nextFloat() < .04f) w[PetState.WAVE] = 6f
        val allowed = w.filter { (s, v) -> v > 0 && Evolution.allowed(stage, s) }
        var r = rnd.nextFloat() * allowed.values.sum()
        val s = allowed.entries.first { r -= it.value; r <= 0 }.key
        val ms = when (s) { PetState.SLEEP -> 30_000L; PetState.SIT, PetState.READING -> 12_000L; PetState.WALK -> 6_000L; PetState.RUN -> 2_500L; else -> 3_500L }
        return Pick(s, ms)
    }
}

/**
 * The friend's memory: a local list of real events (no network, no guessing about feelings or intentions) and the
 * plain sentences they lead to. It never quotes Quran or hadith; that stays with the app's sourced content.
 */
object PetMemory {
    data class Event(val kind: String, val at: Long, val value: Int = 0)
    fun encode(e: List<Event>): String = org.json.JSONArray(e.takeLast(300).map { JSONObject().put("k", it.kind).put("t", it.at).put("v", it.value) }).toString()
    fun decode(s: String?): List<Event> = runCatching { val a = org.json.JSONArray(s ?: "[]"); (0 until a.length()).map { a.getJSONObject(it).let { o -> Event(o.getString("k"), o.getLong("t"), o.optInt("v")) } } }.getOrDefault(emptyList())

    /** Something worth saying today from the shared history, or null. [daysTogether] counts today as day 1. */
    fun line(events: List<Event>, daysTogether: Int, name: String, now: Long, pageHours: List<Int>): String? {
        val milestones = mapOf(7 to "صار لنا أسبوع مع بعض!", 30 to "صار لنا شهر مع بعض!", 100 to "مئة يوم ونحن معًا، الحمد لله!", 365 to "سنة كاملة معًا! بارك الله فيك")
        milestones[daysTogether]?.let { return it }
        val day = 86_400_000L
        events.lastOrNull { it.kind == "khatma" && now - it.at < day }?.let { return "مبروك ختمتك${if (it.value <= 1) " الأولى" else ""}! تقبّل الله منك" }
        events.lastOrNull { it.kind == "level" && now - it.at < day }?.let { return "كبرت صداقتنا! صرت في المستوى ${it.value}" }
        events.lastOrNull { it.kind == "streak" && now - it.at < day }?.let { return "${it.value} أيام طيبة متتالية، ما شاء الله!" }
        // A pattern seen in at least six reading days: when the pages are usually read.
        if (pageHours.size >= 6) {
            val morning = pageHours.count { it in 4..11 }; val night = pageHours.count { it >= 19 || it < 4 }
            if (morning * 3 >= pageHours.size * 2) return "لاحظت أنك تحب القراءة صباحًا"
            if (night * 3 >= pageHours.size * 2) return "لاحظت أنك تقرأ غالبًا في الليل"
        }
        return null
    }
}

/** One problem found by the validator. */
data class Issue(val path: String, val problem: String)

/**
 * Asset validation: a manifest's files exist, are the format they claim, are within size limits, match their hashes,
 * and contain nothing executable. [read] returns a file's bytes (null if missing).
 */
object AssetValidator {
    const val MAX_FILE = 3 * 1024 * 1024
    const val MAX_PACK = 40 * 1024 * 1024
    val allowedExt = setOf("json", "lottie", "png", "webp", "riv", "txt", "md")

    fun validate(pack: Pack, read: (String) -> ByteArray?, checkHashes: Boolean = pack.files.isNotEmpty()): List<Issue> {
        val out = ArrayList<Issue>()
        var total = 0L
        for (p in pack.referencedFiles()) {
            val b = read(p); if (b == null) { out += Issue(p, "missing"); continue }
            total += b.size
            if (b.size > MAX_FILE) out += Issue(p, "larger than ${MAX_FILE / 1024 / 1024} MB (${b.size / 1024} KB)")
            when (p.substringAfterLast('.').lowercase()) {
                "json" -> lottieProblem(b)?.let { out += Issue(p, it) }
                "png" -> if (!(b.size > 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte())) out += Issue(p, "not a PNG")
                "webp" -> if (!(b.size > 12 && String(b, 0, 4) == "RIFF" && String(b, 8, 4) == "WEBP")) out += Issue(p, "not a WebP")
                "lottie" -> if (!(b.size > 4 && b[0] == 'P'.code.toByte() && b[1] == 'K'.code.toByte())) out += Issue(p, "not a dotLottie (zip)")
                "riv" -> if (!(b.size > 4 && String(b, 0, 4) == "RIVE")) out += Issue(p, "not a Rive file")
            }
            if (checkHashes) {
                val want = pack.files[p]
                if (want == null) out += Issue(p, "not listed in files")
                else if (sha256(b) != want) out += Issue(p, "hash mismatch")
            }
        }
        if (total > MAX_PACK) out += Issue(pack.id, "pack larger than ${MAX_PACK / 1024 / 1024} MB")
        pack.skins().forEach { s ->
            if (StateResolver.resolve(s, PetState.IDLE) == null) out += Issue(s.id, "no idle picture and no fallback")
            if (s.license.isEmpty() && !pack.builtin) out += Issue(s.id, "no license")
            if (s.license.isNotEmpty() && pack.licenses[s.license]?.attribution == true && s.by.isBlank()) out += Issue(s.id, "license needs attribution but the author is empty")
            s.states.values.forEach { if (it.kind == SourceKind.RIVE) out += Issue(s.id, "Rive renderer not enabled") }
        }
        return out
    }

    /** A Lottie file this app can draw: version, frame rate, size, a timeline and layers; images embedded, not linked. */
    fun lottieProblem(b: ByteArray): String? = try {
        val o = JSONObject(String(b, Charsets.UTF_8))
        when {
            !o.has("v") -> "no Lottie version"
            o.optDouble("fr", 0.0) <= 0 -> "no frame rate"
            o.optDouble("w", 0.0) <= 0 || o.optDouble("h", 0.0) <= 0 -> "no size"
            o.optDouble("op", 0.0) <= o.optDouble("ip", 0.0) -> "empty timeline"
            (o.optJSONArray("layers")?.length() ?: 0) == 0 -> "no layers"
            o.optJSONArray("assets")?.let { a -> (0 until a.length()).any { i -> a.getJSONObject(i).let { x -> x.has("p") && x.optString("p").isNotEmpty() && !x.optString("p").startsWith("data:") && x.optInt("e", 0) != 1 } } } == true -> "image linked outside the file"
            else -> null
        }
    } catch (e: Exception) { "not valid JSON" }

    fun sha256(b: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
}
