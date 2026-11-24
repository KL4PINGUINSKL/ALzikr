package com.alzikr.app.pets

import android.graphics.Bitmap
import com.alzikr.app.Progress
import com.alzikr.app.Store
import org.json.JSONObject

/**
 * What a legendary friend wears and the optional effects around it (ALzikr Pet Studio legendary/collection.json, exported
 * to assets/pets/legendary/wear.json by design/pet-engine/export_legendary.py). Choices are kept per animal, like the
 * Studio (members of a species share them): prefs pet_wear_<SPECIES> and pet_fx_<SPECIES>. Cosmetic only.
 */
object PetWear {
    class Item(val id: String, val name: String, val slot: String, val level: Int, val width: Float, val only: List<String>, val path: String)
    class Effect(val id: String, val name: String, val level: Int, val only: List<String>)
    private class Data(val items: List<Item>, val effects: List<Effect>, val conflicts: List<Pair<String, String>>)

    private val data: Data by lazy {
        val o = runCatching { JSONObject(PetPacks.open("wear", "legendary/wear.json")!!.use { it.readBytes().toString(Charsets.UTF_8) }) }.getOrNull()
            ?: return@lazy Data(emptyList(), emptyList(), emptyList())
        fun strs(a: org.json.JSONArray?) = a?.let { x -> (0 until x.length()).map { x.getString(it) } }.orEmpty()
        val a = o.getJSONArray("accessories"); val e = o.getJSONArray("effects"); val c = o.optJSONArray("conflicts") ?: org.json.JSONArray()
        Data((0 until a.length()).map { a.getJSONObject(it) }.map { j -> Item(j.getString("id"), j.optString("name"), j.getString("slot"), j.optInt("level", 1),
                j.getDouble("width").toFloat(), strs(j.optJSONArray("only")), j.getString("path")) },
            (0 until e.length()).map { e.getJSONObject(it) }.map { j -> Effect(j.getString("id"), j.optString("name"), j.optInt("level", 1), strs(j.optJSONArray("only"))) },
            (0 until c.length()).map { c.getJSONArray(it) }.map { it.getString(0) to it.getString(1) })
    }

    fun items(sp: String): List<Item> = data.items.filter { sp in it.only }.sortedBy { it.level }
    fun effectsFor(sp: String): List<Effect> = data.effects.filter { sp in it.only }.sortedBy { it.level }
    fun open(level: Int) = Progress.level() >= level

    private fun set(key: String): Set<String> = runCatching { Store.prefs.getStringSet(key, emptySet()) ?: emptySet() }.getOrDefault(emptySet())
    private fun put(key: String, s: Set<String>) { Store.prefs.edit().putStringSet(key, s).apply(); Store.bump() }

    fun wearing(sp: String, id: String) = id in set("pet_wear_$sp")
    fun showing(sp: String, id: String) = id in set("pet_fx_$sp")

    /** Pieces that cannot be worn together with [item]: the same place on the body, or a pair the Studio marks as clashing. */
    fun clashes(item: Item, other: Item) = item.id != other.id && (item.slot == other.slot ||
        data.conflicts.any { (a, b) -> (a == item.id && b == other.id) || (b == item.id && a == other.id) })
    fun replaces(sp: String, item: Item): List<Item> = items(sp).filter { wearing(sp, it.id) && clashes(item, it) }

    fun toggleWear(sp: String, item: Item) {
        val s = set("pet_wear_$sp").toMutableSet()
        if (item.id in s) s -= item.id else { s -= replaces(sp, item).map { it.id }.toSet(); s += item.id }
        put("pet_wear_$sp", s)
    }
    fun toggleEffect(sp: String, id: String) { val s = set("pet_fx_$sp").toMutableSet(); if (!s.remove(id)) s += id; put("pet_fx_$sp", s) }

    /** What the friend wears now, with the pictures (unlocked only: lowering a level hides a piece, it is not lost). */
    fun worn(sp: String): List<Pair<Item, Bitmap>> {
        val on = set("pet_wear_$sp"); if (on.isEmpty()) return emptyList()
        // Draw order: things on the back first, then the body, the neck, and the head last.
        val order = listOf("back", "tail", "leg", "wing", "torso", "neck", "crest", "head")
        return items(sp).filter { it.id in on && open(it.level) }.sortedBy { order.indexOf(it.slot) }
            .mapNotNull { i -> PetBitmaps.get("wear", i.path, 256)?.let { i to it } }
    }
    fun effects(sp: String): List<String> {
        val on = set("pet_fx_$sp"); if (on.isEmpty()) return emptyList()
        return effectsFor(sp).filter { it.id in on && open(it.level) }.map { it.id }
    }
}
