package com.alzikr.app.pets

import org.json.JSONArray
import org.json.JSONObject

/*
 * The ALzikr Pet Engine: data model and parser.
 *
 * Everything about a friend is data, read from a versioned pack manifest (pack.json): species, skins, evolution
 * stages, personality, animation states, accessories, licenses and unlock requirements. No code ships in a pack.
 * This file is plain Kotlin + org.json, so it runs in JVM unit tests too.
 */

/** Manifest schema this app understands. A pack with a higher schema is refused (the app must be updated first). */
const val PET_SCHEMA = 1

/** The four states, the same as ALzikr Pet Studio's (other names in a pack are ignored). [fallback] is used when a skin has no source for a state; [motion] is the movement the
 *  engine adds by code when it falls back (so "jump" on a skin without a jump still hops). idle is the root. */
enum class PetState(val id: String, private val fallbackId: String?, val motion: Motion, val ar: String) {
    IDLE("idle", null, Motion.NONE, "هدوء"),
    WALK("walk", "idle", Motion.BOB, "مشي"),
    HAPPY("happy", "idle", Motion.HOP, "فرح"),
    SLEEP("sleep", "idle", Motion.SLOW, "نوم");

    companion object {
        fun of(id: String): PetState? = entries.firstOrNull { it.id == id }
        /** The chain tried for [s]: s, its fallback, …, idle. Never loops (checked by a unit test). */
        fun chain(s: PetState): List<PetState> { val out = ArrayList<PetState>(); var c: PetState? = s; while (c != null && c !in out) { out += c; c = c.fallback }; return out }
    }
    /** The state tried next when a skin has no source for this one (by id, so a state may fall back to one declared later). */
    val fallback: PetState? get() = fallbackId?.let { of(it) }
}

/** Movement added by code on top of any animation (or alone, on a still picture). */
enum class Motion { NONE, BOB, FAST, SLOW, HOP, SWAY, TILT, FADE_IN, FADE_OUT }

enum class Rarity(val ar: String) { COMMON("عادي"), RARE("نادر"), EPIC("نادر جدًا");
    companion object { fun of(s: String?) = entries.firstOrNull { it.name.equals(s, true) } ?: COMMON } }

/** How a picture is stored. RIVE is reserved: refused until its renderer passes the performance benchmark. */
enum class SourceKind(val ext: Set<String>) {
    LOTTIE(setOf("json")), DOTLOTTIE(setOf("lottie")), SPRITE(setOf("png", "webp")), FRAMES(setOf("png", "webp")),
    WEBP(setOf("webp")), PNG(setOf("png", "webp")), RIVE(setOf("riv")),
    /** A painting animated by the app (mesh warp around landmarks): [Source.path] is the rig JSON, [Source.anim] the motion. */
    RIG(setOf("json"));
    companion object { fun of(s: String) = entries.firstOrNull { it.name.equals(s, true) } }
}

/**
 * One animation (or still) for a state.
 * SPRITE: one sheet [path] with [cols]×[rows] cells, [frames] used, played at [fps].
 * FRAMES: [paths] played in order at [fps].
 * LOTTIE/DOTLOTTIE: optional [from]..[to] segment (0..1) of the timeline.
 * RIG: [path] is the rig (pictures + landmarks), [anim] one of idle, walk, happy, sleep.
 */
data class Source(
    val kind: SourceKind, val path: String = "", val paths: List<String> = emptyList(),
    val fps: Int = 12, val frames: Int = 0, val cols: Int = 1, val rows: Int = 1,
    val from: Float = 0f, val to: Float = 1f, val speed: Float = 1f, val loop: Boolean = true, val motion: Motion? = null, val anim: String = "",
) { fun files(): List<String> = if (kind == SourceKind.FRAMES) paths else listOf(path) }

data class Fit(val zoom: Float = 1f, val lift: Float = 0f, val darkLines: Boolean = false)
data class Unlock(val level: Int = 1, val badge: String? = null)
/** A Hijri window (inclusive). A seasonal skin can be chosen only inside it; once chosen it stays the user's. */
data class Season(val fromMonth: Int, val fromDay: Int = 1, val toMonth: Int = fromMonth, val toDay: Int = 30) {
    fun contains(month: Int, day: Int): Boolean { val v = month * 100 + day; val a = fromMonth * 100 + fromDay; val b = toMonth * 100 + toDay; return if (a <= b) v in a..b else v >= a || v <= b }
}
data class License(val id: String, val name: String, val url: String = "", val attribution: Boolean = false, val commercial: Boolean = true)
data class Personality(val energy: Float = .5f, val sleepy: Float = .5f, val playful: Float = .5f, val chatty: Float = .5f, val curious: Float = .5f)
data class Stage(val id: String, val name: String, val fromLevel: Int, val scale: Float = 1f)

data class Skin(
    val id: String, val species: String, val name: String, val by: String = "", val license: String = "", val page: String = "",
    val rarity: Rarity = Rarity.COMMON, val unlock: Unlock = Unlock(), val fit: Fit = Fit(),
    val states: Map<PetState, Source> = emptyMap(), val fallback: Source? = null, val season: Season? = null,
    /** Kept for whoever already chose it, but not offered (e.g. a file that does not draw on Android). */
    val hidden: Boolean = false,
)
data class SpeciesDef(
    val id: String, val ar: String, val about: String = "", val defaultName: String = "", val unlock: Unlock = Unlock(),
    val personality: Personality = Personality(), val stages: List<Stage> = emptyList(), val skins: List<Skin> = emptyList(),
)
data class Accessory(val id: String, val name: String, val slot: String, val source: Source, val unlock: Unlock = Unlock(), val license: String = "", val by: String = "")

data class Pack(
    val id: String, val version: Int, val schema: Int, val title: String, val builtin: Boolean = false,
    val licenses: Map<String, License> = emptyMap(), val species: List<SpeciesDef> = emptyList(),
    val accessories: List<Accessory> = emptyList(), val season: Season? = null,
    /** relative path → lowercase hex SHA-256; every file a downloaded pack ships must be listed. */
    val files: Map<String, String> = emptyMap(),
) {
    fun skins(): List<Skin> = species.flatMap { it.skins }
    /** Every file the pack's sources refer to. */
    fun referencedFiles(): Set<String> = (skins().flatMap { s -> s.states.values.flatMap { it.files() } + (s.fallback?.files() ?: emptyList()) } +
        accessories.flatMap { it.source.files() }).filter { it.isNotBlank() }.toSet()
}

class PackFormatException(msg: String) : Exception(msg)

/** Reads and checks a pack manifest. Unknown fields are ignored (forward compatible); missing required ones are errors. */
object PackParser {
    private val idRe = Regex("^[a-z0-9][a-z0-9._-]{0,63}$")
    private val speciesRe = Regex("^[A-Z][A-Z0-9_]{0,31}$")

    fun parse(text: String): Pack {
        val o = try { JSONObject(text) } catch (e: Exception) { throw PackFormatException("not JSON: ${e.message}") }
        val schema = o.optInt("schema", -1)
        if (schema < 1) throw PackFormatException("missing schema")
        if (schema > PET_SCHEMA) throw PackFormatException("schema $schema is newer than this app ($PET_SCHEMA)")
        val id = o.optString("id"); if (!idRe.matches(id)) throw PackFormatException("bad pack id '$id'")
        val version = o.optInt("version", 0); if (version < 1) throw PackFormatException("bad version")
        val licenses = o.optJSONArray("licenses").objs().associate { l -> l.getString("id") to License(l.getString("id"), l.optString("name"), l.optString("url"), l.optBoolean("attribution"), l.optBoolean("commercial", true)) }
        val species = o.optJSONArray("species").objs().map { s ->
            val sid = s.getString("id"); if (!speciesRe.matches(sid)) throw PackFormatException("bad species id '$sid'")
            SpeciesDef(
                sid, s.optString("ar", sid), s.optString("about"), s.optString("defaultName"), unlock(s.optJSONObject("unlock")),
                s.optJSONObject("personality")?.let { p -> Personality(p.f("energy"), p.f("sleepy"), p.f("playful"), p.f("chatty"), p.f("curious")) } ?: Personality(),
                s.optJSONArray("stages").objs().map { g -> Stage(g.getString("id"), g.optString("name"), g.optInt("fromLevel", 1), g.optDouble("scale", 1.0).toFloat()) },
                s.optJSONArray("skins").objs().map { k -> skin(k, sid, licenses) },
            )
        }
        val ids = species.flatMap { it.skins }.map { it.id }
        ids.groupBy { it }.filter { it.value.size > 1 }.keys.firstOrNull()?.let { throw PackFormatException("duplicate skin id '$it'") }
        val acc = o.optJSONArray("accessories").objs().map { a ->
            Accessory(a.getString("id"), a.optString("name"), a.optString("slot", "head"), source(a.getJSONObject("source")), unlock(a.optJSONObject("unlock")), a.optString("license"), a.optString("by"))
        }
        val files = o.optJSONObject("files")?.let { f -> f.keys().asSequence().associateWith { f.getString(it).lowercase() } } ?: emptyMap()
        return Pack(id, version, schema, o.optString("title", id), o.optBoolean("builtin"), licenses, species, acc, o.optJSONObject("season")?.let(::season), files)
    }

    private fun skin(k: JSONObject, sid: String, licenses: Map<String, License>): Skin {
        val id = k.getString("id"); if (!idRe.matches(id)) throw PackFormatException("bad skin id '$id'")
        val lic = k.optString("license")
        if (lic.isNotEmpty() && licenses.isNotEmpty() && lic !in licenses) throw PackFormatException("skin $id: unknown license '$lic'")
        val states = HashMap<PetState, Source>()
        k.optJSONObject("states")?.let { st -> st.keys().forEach { key -> PetState.of(key)?.let { states[it] = source(st.getJSONObject(key)) } } }
        if (states.isEmpty() && k.optJSONObject("fallback") == null) throw PackFormatException("skin $id has no picture")
        val f = k.optJSONObject("fit")
        return Skin(id, sid, k.optString("name", id), k.optString("by"), lic, k.optString("page"), Rarity.of(k.optString("rarity")), unlock(k.optJSONObject("unlock")),
            if (f == null) Fit() else Fit(f.optDouble("zoom", 1.0).toFloat(), f.optDouble("lift", 0.0).toFloat(), f.optBoolean("darkLines")),
            states, k.optJSONObject("fallback")?.let(::source), k.optJSONObject("season")?.let(::season), k.optBoolean("hidden"))
    }
    fun source(s: JSONObject): Source {
        val kind = SourceKind.of(s.optString("kind")) ?: throw PackFormatException("unknown source kind '${s.optString("kind")}'")
        val src = Source(kind, s.optString("path"), s.optJSONArray("paths")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            s.optInt("fps", 12).coerceIn(1, 60), s.optInt("frames", 0), s.optInt("cols", 1).coerceAtLeast(1), s.optInt("rows", 1).coerceAtLeast(1),
            s.optDouble("from", 0.0).toFloat().coerceIn(0f, 1f), s.optDouble("to", 1.0).toFloat().coerceIn(0f, 1f), s.optDouble("speed", 1.0).toFloat(),
            s.optBoolean("loop", true), s.optString("motion").takeIf { it.isNotEmpty() }?.let { m -> Motion.entries.firstOrNull { it.name.equals(m, true) } }, s.optString("anim"))
        src.files().forEach { p -> checkPath(p, kind) }
        if (kind == SourceKind.FRAMES && src.paths.isEmpty()) throw PackFormatException("FRAMES without paths")
        return src
    }
    /** Relative, no "..", no absolute paths, and an extension that fits the kind. */
    fun checkPath(p: String, kind: SourceKind) {
        if (p.isBlank() || p.startsWith("/") || p.startsWith("\\") || p.contains("..") || p.contains(':') || p.contains('\\')) throw PackFormatException("unsafe path '$p'")
        val ext = p.substringAfterLast('.', "").lowercase()
        if (ext !in kind.ext) throw PackFormatException("'$p' is not a ${kind.name} file")
    }
    private fun unlock(u: JSONObject?) = if (u == null) Unlock() else Unlock(u.optInt("level", 1).coerceAtLeast(1), u.optString("badge").takeIf { it.isNotEmpty() })
    private fun season(s: JSONObject) = Season(s.getInt("fromMonth"), s.optInt("fromDay", 1), s.optInt("toMonth", s.getInt("fromMonth")), s.optInt("toDay", 30))
    private fun JSONObject.f(k: String) = optDouble(k, .5).toFloat().coerceIn(0f, 1f)
    private fun JSONArray?.objs(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
}
