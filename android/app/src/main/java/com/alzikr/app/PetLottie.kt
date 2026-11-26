package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alzikr.app.pets.PetPacks
import com.alzikr.app.pets.PetState
import com.alzikr.app.pets.PetView
import com.alzikr.app.pets.Place
import com.alzikr.app.pets.Rarity
import com.alzikr.app.pets.Season

/**
 * The friends' looks, now read from the Pet Engine's packs (assets/pets/pack.json + downloaded packs) instead of a
 * table in code. The ids, names, fit and order are the same as before (checked by LegacyParityTest), so every saved
 * choice and unlock stays valid. Each look's author, page and license are in the pack.
 */
object PetLooks {
    /** Open from the first day, shown first, and the look a new friend of this kind starts with. */
    /** Open for everyone from the first day. */
    val OPEN_FROM_START = setOf("penguin-7")
    val FEATURED = mapOf(Species.CAT to "originals-mishmish", Species.PENGUIN to "originals-luulu")
    /** [zoom] enlarges a drawing that leaves empty space around the animal; [lift] moves it up (fraction of the box). */
    data class Look(val id: String, val name: String, val by: String, val zoom: Float = 1f, val lift: Float = 0f, val darkLines: Boolean = false,
                    val level: Int = 1, val rarity: Rarity = Rarity.COMMON, val season: Season? = null, val license: String = "", val page: String = "", val hidden: Boolean = false)
    @Volatile private var memo: Pair<Int, Map<Species, List<Look>>>? = null
    val looks: Map<Species, List<Look>> get() {
        val rev = PetPacks.revision
        memo?.takeIf { it.first == rev }?.let { return it.second }
        val reg = PetPacks.registry()
        val m = Species.entries.associateWith { s ->
            val list = reg.skins(s.name).map { k -> Look(k.id, k.name, k.by, k.fit.zoom, k.fit.lift, k.fit.darkLines, k.unlock.level, k.rarity, k.season, k.license, k.page, k.hidden) }
            val list0 = list.map { if (it.id in OPEN_FROM_START) it.copy(level = 1) else it }
            val star = FEATURED[s]
            val f = list0.firstOrNull { it.id == star }
            if (f == null) list0 else listOf(f.copy(level = 1, hidden = false)) + (list0 - f)
        }
        memo = rev to m
        return m
    }
    /** The looks offered for [s]: hidden ones only when this friend already uses it. */
    fun of(s: Species): List<Look> {
        val mine = runCatching { Store.prefs.getString("pet_look_${s.name}", null) }.getOrNull()
        return looks[s].orEmpty().filter { !it.hidden || it.id == mine }
    }
    /** The look chosen for this friend (each friend keeps its own). */
    fun chosen(s: Species): Look? {
        val id = Store.prefs.getString("pet_look_${s.name}", null)
        // A look not opened yet (a new friend's rare look) falls back to the first one.
        val all = of(s); val i = all.indexOfFirst { it.id == id }
        return if (i >= 0 && Progress.lookOpen(s, i)) all[i] else all.firstOrNull()
    }
    /** Once, on the update to 1.5.1: a friend already living with someone keeps the look it wore. */
    fun keepOldDefaults(existing: Boolean) {
        val sp = Store.prefs
        if (sp.getBoolean("featured_kept", false)) return
        val e = sp.edit().putBoolean("featured_kept", true)
        if (existing) FEATURED.keys.forEach { s ->
            if (!sp.contains("pet_look_${s.name}")) PetPacks.registry().skins(s.name).firstOrNull { !it.hidden }?.let { e.putString("pet_look_${s.name}", it.id) }
        }
        e.apply()
    }
    fun choose(s: Species, id: String) { Store.prefs.edit().putString("pet_look_${s.name}", id).apply(); Store.bump() }
}

/**
 * One friend, drawn by the Pet Engine in [state] at [place]'s frame-rate budget. [playing] false keeps a still frame.
 * Works for every format a pack may use (Lottie, dotLottie, sprite sheets, frames, animated WebP, still PNG).
 */
@Composable
fun PetLottie(species: Species, modifier: Modifier, look: PetLooks.Look? = PetLooks.chosen(species), playing: Boolean = true, speed: Float = 1f,
              place: Place = Place.APP, state: PetState = PetState.IDLE, scale: Float = 1f) {
    val l = look ?: return
    val skin = PetPacks.registry().skin(l.id) ?: return
    // A drawing made of dark lines needs light behind it on a dark theme.
    val halo = if (l.darkLines && P.dark) Modifier.background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = .55f), androidx.compose.ui.graphics.Color.Transparent))) else Modifier
    PetView(skin, state, modifier.then(halo), place, playing, speed, scale)
}

/** Developer check: every look at the size it has in the header (More page "pettest"). */
@Composable
fun PetLooksTest() {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(androidx.compose.foundation.lazy.grid.GridCells.Fixed(4), Modifier.fillMaxSize()) {
        PetLooks.looks.forEach { (s, ls) ->
            ls.forEach { l ->
                item {
                    androidx.compose.foundation.layout.Column(Modifier.padding(4.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        androidx.compose.foundation.layout.Box(Modifier.size(80.dp).background(P.card)) { PetLottie(s, Modifier.fillMaxSize(), look = l, place = Place.GRID) }
                        androidx.compose.material3.Text(l.id, color = P.ink, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
