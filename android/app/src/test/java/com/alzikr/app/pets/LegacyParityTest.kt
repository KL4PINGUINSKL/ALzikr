package com.alzikr.app.pets

import com.alzikr.app.PetLooks
import com.alzikr.app.Progress
import com.alzikr.app.Species
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** The app's live tables (PetLooks, Progress) and the core pack must agree: same looks, fit, order and unlock levels. */
class LegacyParityTest {
    private val core = PackParser.parse(File("src/main/assets/pets/pack.json").readText())

    @Test fun liveLooksEqualCorePack() {
        Species.entries.forEach { sp ->
            // Core looks first, in their old order; the built-in originals follow them.
            val live = PetLooks.of(sp).filter { !it.id.startsWith("originals-") && !it.id.startsWith("legend-") }
            val pack = core.species.firstOrNull { it.id == sp.name }?.skins.orEmpty().filter { !it.hidden }   // hidden looks are not offered
            assertEquals("${sp.name} count", live.size, pack.size)
            live.zip(pack).forEachIndexed { i, (l, s) ->
                assertEquals(l.id, s.id); assertEquals(l.name, s.name); assertEquals(l.by, s.by)
                assertEquals(l.zoom, s.fit.zoom, 1e-4f); assertEquals(l.lift, s.fit.lift, 1e-4f); assertEquals(l.darkLines, s.fit.darkLines)
                assertEquals("pack level of ${l.id}", Progress.lookLevel(sp, i), l.level)
            }
            core.species.firstOrNull { it.id == sp.name }?.let { assertEquals("species level ${sp.name}", Progress.speciesLevel(sp), it.unlock.level) }
        }
    }
}
