package com.alzikr.app.pets

import com.alzikr.app.PetLooks
import com.alzikr.app.Progress
import com.alzikr.app.Species
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The built-in ALzikr Pet Studio originals: every animal has two looks, every file is valid, the rig moves sensibly. */
class OriginalsTest {
    private val dir = File("src/main/assets/pets")
    private val pack = PackParser.parse(File(dir, "originals/pack.json").readText())

    @Test fun everyAnimalHasItsOriginals() {
        assertTrue(pack.builtin)
        Species.entries.filter { PetPacks.registry().species(it.name)?.skins?.none { k -> k.id.startsWith("legend-") } != false }.forEach { sp ->
            val o = PetLooks.of(sp).filter { it.id.startsWith("originals-") }
            assertTrue("${sp.name} originals", o.size >= 2)
            assertTrue("${sp.name} Arabic names", o.all { l -> l.name.any { it in '\u0600'..'\u06FF' } })
        }
        // The three new animals open after the existing ones.
        listOf(Species.TURTLE to 12, Species.FOX to 13, Species.OWL to 14).forEach { (s, l) -> assertEquals(l, Progress.speciesLevel(s)) }
        // Legendary friends open from level 50, each with looks of its own.
        listOf(Species.CELESTIAL_DRAGON to 50, Species.WINGED_UNICORN to 60, Species.GRIFFIN to 70, Species.PHOENIX to 80, Species.COSMIC_DRAGON to 100).forEach { (s, l) ->
            assertEquals(l, Progress.speciesLevel(s)); assertTrue(PetLooks.of(s).isNotEmpty() && PetLooks.of(s).all { it.level >= 50 })
        }
        // Every step is 75 more than the one before, at every level.
        assertEquals(150, Progress.step(2))
        (3..120).forEach { n -> assertEquals(75, Progress.step(n) - Progress.step(n - 1)) }
        assertEquals(24_300, Progress.need(25)); assertEquals(95_550, Progress.need(50)); assertEquals(378_675, Progress.need(100))
        assertEquals(49, Progress.level(95_549)); assertEquals(50, Progress.level(95_550))
        // Every level from 2 to 125 opens a friend or a look (levels.json), and each friend's looks open in order.
        val opened = Species.entries.flatMap { s -> listOf(Progress.speciesLevel(s)) + PetLooks.of(s).filter { !it.hidden }.map { it.level } }.toSet()
        (2..125).forEach { l -> assertTrue("level $l opens nothing", l in opened) }
        Species.entries.forEach { s -> PetLooks.of(s).filter { !it.hidden }.map { it.level }.let { assertEquals("${s.name} in order", it.sorted(), it) } }
    }

    @Test fun filesAreValid() {
        val issues = AssetValidator.validate(pack, { p -> File(dir, p).takeIf { it.exists() }?.readBytes() }, checkHashes = false)
        assertTrue(issues.joinToString("\n"), issues.isEmpty())
        pack.skins().forEach { s ->
            PetState.entries.forEach { st -> assertNotNull("${s.id} $st", StateResolver.resolve(s, st)) }
            val rig = PetRig.parse(File(dir, s.states.getValue(PetState.IDLE).path).readText())
            listOfNotNull(rig.art, rig.sleep, rig.closed).forEach { assertTrue("${s.id}: $it", File(dir, it).exists()) }
        }
        val room = org.json.JSONObject(File(dir, "originals/room.json").readText())
        assertEquals(16, room.getJSONObject("homes").length())
        val f = room.getJSONArray("furniture"); (0 until f.length()).forEach { assertTrue(File(dir, f.getJSONObject(it).getString("path")).exists()) }
    }

    @Test fun motionStaysSmallAndLoops() {
        val rig = PetRig.parse(File(dir, "originals/rigs/pip.json").readText())
        val out = FloatArray(2)
        for (anim in listOf("idle", "walk", "happy")) for (i in 0 until 50) {
            val p = PetRig.motion(rig, anim, i * .1f)
            // No landmark is pushed further than ~20 model units (of 400): the painting bends, it does not tear.
            PetRig.field(rig.head!![0], rig.head!![1], p.controls, out); assertTrue("$anim head", kotlin.math.hypot(out[0], out[1]) < 20f)
            assertTrue(p.blink in 0f..1f)
        }
        // A walk cycle returns to where it started.
        val a = PetRig.motion(rig, "walk", 0f); val b = PetRig.motion(rig, "walk", rig.g[0])
        assertEquals(a.dx, b.dx, 1e-3f); assertEquals(a.angle, b.angle, 1e-3f)
        assertEquals(0, PetRig.motion(rig, "sleep", 1f).controls.size)
    }
}
