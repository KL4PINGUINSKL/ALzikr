package com.alzikr.app.pets

import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Regression, migration, validation and safety tests for the Pet Engine (JVM, no device). */
class PetEngineTest {
    private val assets = File("src/main/assets/pets")
    private val core by lazy { PackParser.parse(File(assets, "pack.json").readText()) }
    private val baseline by lazy { JSONObject(javaClass.classLoader!!.getResource("pet_baseline.json")!!.readText()) }
    /** Looks removed in 1.4.0 for being unclear (design/pet-engine/removed_skins.json). */
    private val removed by lazy { JSONObject(javaClass.classLoader!!.getResource("removed_skins.json")!!.readText()).getJSONArray("ids").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } }

    // ---------------------------------------------------------------- migration: nothing visible may change

    @Test fun coreKeepsEveryExistingSpeciesAndSkinInOrder() {
        val sp = baseline.getJSONArray("species")
        val byId = core.species.associateBy { it.id }
        assertEquals("species count", sp.length(), core.species.size)
        for (i in 0 until sp.length()) {
            val b = sp.getJSONObject(i); val c = byId[b.getString("id")] ?: return fail("species ${b.getString("id")} missing")
            assertEquals(b.getString("id"), core.species[i].id)                       // same order as the grid
            assertEquals(b.getString("ar"), c.ar)
            assertEquals(b.getString("defaultName"), c.defaultName)
            assertEquals("unlock level of ${c.id}", b.getInt("unlockLevel"), c.unlock.level)
            val bs = b.getJSONArray("skins")
            // The old skins come first, in their old order (new ones may follow); removed ones are gone and nothing else moved.
            var n = 0
            for (j in 0 until bs.length()) {
                val k = bs.getJSONObject(j)
                if (k.getString("id") in removed) { assertTrue("${k.getString("id")} removed", c.skins.none { it.id == k.getString("id") }); continue }
                val s = c.skins[n++]
                assertEquals("skin $j of ${c.id}", k.getString("id"), s.id)
                assertEquals(k.getString("name"), s.name)
                assertEquals(k.getString("by"), s.by)
                assertEquals(k.getDouble("zoom").toFloat(), s.fit.zoom, 1e-4f)
                assertEquals(k.getDouble("lift").toFloat(), s.fit.lift, 1e-4f)
                assertEquals(k.getBoolean("darkLines"), s.fit.darkLines)
                assertEquals("look level of ${s.id}", if (j < 3) 1 else j - 1, s.unlock.level)
                assertEquals("${s.id}.json", s.states[PetState.IDLE]?.path)
            }
        }
        assertEquals("existing skins", 110, (0 until sp.length()).sumOf { sp.getJSONObject(it).getJSONArray("skins").length() })
    }

    @Test fun everySkinHasAVerifiedLicenseAndSource() {
        core.skins().forEach { s ->
            assertEquals("${s.id} license", "lottie-simple", s.license)
            assertTrue("${s.id} page", s.page.startsWith("https://lottiefiles.com/"))
        }
        assertFalse(core.licenses.getValue("lottie-simple").attribution)
    }

    // ---------------------------------------------------------------- asset validation

    @Test fun everyCoreAssetIsAValidSelfContainedLottie() {
        val issues = AssetValidator.validate(core, { p -> File(assets, p).takeIf { it.exists() }?.readBytes() }, checkHashes = false)
        assertTrue(issues.joinToString("\n") { "${it.path}: ${it.problem}" }, issues.isEmpty())
    }

    @Test fun validatorCatchesBrokenFiles() {
        assertNotNull(AssetValidator.lottieProblem("{}".toByteArray()))
        assertNotNull(AssetValidator.lottieProblem("not json".toByteArray()))
        assertNotNull(AssetValidator.lottieProblem("""{"v":"5","fr":30,"w":10,"h":10,"ip":0,"op":10,"layers":[{}],"assets":[{"id":"i","p":"img.png","u":"images/","e":0}]}""".toByteArray()))
        assertNull(AssetValidator.lottieProblem("""{"v":"5","fr":30,"w":10,"h":10,"ip":0,"op":10,"layers":[{}]}""".toByteArray()))
    }

    // ---------------------------------------------------------------- states and fallbacks

    @Test fun everyStateResolvesForEverySkinWithoutLoops() {
        PetState.entries.forEach { s ->
            val chain = PetState.chain(s)
            assertEquals("chain of $s ends at idle", PetState.IDLE, chain.last())
            assertEquals("no repeats in $s", chain.size, chain.toSet().size)
        }
        core.skins().forEach { k -> PetState.entries.forEach { s -> assertNotNull("${k.id}/$s", StateResolver.resolve(k, s)) } }
        val k = core.skins().first()
        val jump = StateResolver.resolve(k, PetState.HAPPY)!!
        assertTrue(jump.fellBack); assertEquals(Motion.HOP, jump.motion)       // falls back to idle but still hops
        assertEquals(Motion.NONE, StateResolver.resolve(k, PetState.IDLE)!!.motion)
    }

    @Test fun stillFallbackIsUsedWhenNoStateExists() {
        val s = Skin("x", "CAT", "x", fallback = Source(SourceKind.PNG, "x.png"))
        val r = StateResolver.resolve(s, PetState.HAPPY)!!
        assertEquals(SourceKind.PNG, r.source.kind); assertEquals(Motion.HOP, r.motion)
    }

    @Test fun stateMachineReturnsToBaseAndKeepsPriorities() {
        val m = PetStateMachine()
        m.request(PetState.HAPPY, now = 0, durationMs = 1000, prio = 5)
        assertFalse("lower priority ignored", m.request(PetState.IDLE, now = 500, durationMs = 500, prio = 1))
        assertEquals(PetState.HAPPY, m.tick(900))
        assertEquals(PetState.IDLE, m.tick(1000))
        m.setBase(PetState.SLEEP, 1100); assertEquals(PetState.SLEEP, m.tick(1200))
    }

    // ---------------------------------------------------------------- behaviour and evolution

    @Test fun behaviourFollowsTimeOfDayAndPersonality() {
        val calm = Personality(energy = .1f, sleepy = .9f, playful = .1f, chatty = .1f, curious = .1f)
        val stage = Evolution.defaultStages.last()
        val night = (0 until 400).map { Behavior.next(calm, 1, 50, asleep = true, stage = stage, rnd = Random(it)).state }
        assertTrue(night.all { it == PetState.SLEEP })
        val r = Random(7)
        val playful = Personality(energy = .9f, playful = .9f)
        val day = (0 until 2000).map { Behavior.next(playful, 12, 50, false, stage, r).state }
        assertTrue("an energetic friend walks or plays at noon", day.count { it == PetState.WALK || it == PetState.HAPPY } > day.count { it == PetState.IDLE })
        val baby = Evolution.defaultStages.first()
        assertTrue((0 until 500).map { Behavior.next(playful, 18, 90, false, baby, Random(it)).state }.all { Evolution.allowed(baby, it) })
    }

    @Test fun evolutionStagesByLevel() {
        assertEquals("baby", Evolution.stage(null, 1).id); assertEquals("baby", Evolution.stage(null, 2).id)
        assertEquals("curious", Evolution.stage(null, 3).id); assertEquals("tame", Evolution.stage(null, 7).id)
        assertEquals("special", Evolution.stage(null, 8).id); assertEquals("elder", Evolution.stage(null, 40).id)
    }

    @Test fun memorySpeaksOnlyFromRealEvents() {
        val now = 1_000_000_000L
        assertEquals("صار لنا شهر مع بعض!", PetMemory.line(emptyList(), 30, "x", now, emptyList()))
        assertNull(PetMemory.line(emptyList(), 12, "x", now, listOf(9, 10)))
        assertEquals("لاحظت أنك تحب القراءة صباحًا", PetMemory.line(emptyList(), 12, "x", now, listOf(6, 7, 8, 9, 6, 7, 21)))
        val ev = listOf(PetMemory.Event("khatma", now - 1000, 1))
        assertEquals(ev, PetMemory.decode(PetMemory.encode(ev)))
        assertTrue(PetMemory.line(ev, 12, "x", now, emptyList())!!.contains("الأولى"))
    }

    @Test fun seasonWindowsWrapTheYear() {
        assertTrue(Season(9).contains(9, 15)); assertFalse(Season(9).contains(10, 1))
        assertTrue(Season(12, 25, 1, 10).contains(1, 5)); assertFalse(Season(12, 25, 1, 10).contains(6, 1))
    }

    // ---------------------------------------------------------------- manifest safety

    @Test fun parserRefusesUnsafeOrNewerManifests() {
        fun bad(json: String) = try { PackParser.parse(json); fail("accepted: $json") } catch (_: PackFormatException) {}
        bad("""{"schema":99,"id":"x","version":1}""")
        bad("""{"schema":1,"id":"../x","version":1}""")
        bad("""{"schema":1,"id":"x","version":1,"species":[{"id":"CAT","skins":[{"id":"a","states":{"idle":{"kind":"lottie","path":"../../evil.json"}}}]}]}""")
        bad("""{"schema":1,"id":"x","version":1,"species":[{"id":"CAT","skins":[{"id":"a","states":{"idle":{"kind":"lottie","path":"code.dex"}}}]}]}""")
        bad("""{"schema":1,"id":"x","version":1,"species":[{"id":"CAT","skins":[{"id":"a"},{"id":"a","fallback":{"kind":"png","path":"a.png"}}]}]}""")
    }

    // ---------------------------------------------------------------- downloadable packs: install, integrity, rollback, removal

    private val lottie = """{"v":"5.7","fr":30,"w":100,"h":100,"ip":0,"op":30,"layers":[{"ty":4}]}""".toByteArray()
    private fun manifest(version: Int, files: Map<String, ByteArray>, extra: String = "") = """{"schema":1,"id":"spring","version":$version,"title":"t",
        "licenses":[{"id":"cc0","name":"CC0"}],
        "species":[{"id":"CAT","ar":"قطة","skins":[{"id":"spring-cat","name":"قطة الربيع","by":"A","license":"cc0","states":{"idle":{"kind":"lottie","path":"a/cat.json"}}}]}],
        "files":{${files.entries.joinToString(",") { "\"${it.key}\":\"${AssetValidator.sha256(it.value)}\"" }}}$extra}"""
    private fun zip(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { b -> ZipOutputStream(b).use { z -> entries.forEach { (n, d) -> z.putNextEntry(ZipEntry(n)); z.write(d); z.closeEntry() } } }.toByteArray()
    private fun store() = PackStore(File.createTempFile("packs", "").also { it.delete(); it.mkdirs(); it.deleteOnExit() })

    @Test fun installsUpdatesRollsBackAndRemoves() {
        val st = store()
        val f = mapOf("a/cat.json" to lottie)
        val z1 = zip(f + ("pack.json" to manifest(1, f).toByteArray()))
        assertEquals(1, st.install(z1.inputStream(), AssetValidator.sha256(z1)).version)
        val f2 = mapOf("a/cat.json" to String(lottie).replace("\"op\":30", "\"op\":45").toByteArray())
        val z2 = zip(f2 + ("pack.json" to manifest(2, f2).toByteArray()))
        st.install(z2.inputStream(), AssetValidator.sha256(z2))
        assertEquals(2, st.activeVersion("spring"))
        assertTrue(st.rollback("spring")); assertEquals(1, st.activeVersion("spring"))
        assertNotNull(st.file("spring", "a/cat.json"))
        assertTrue(st.remove("spring")); assertNull(st.activeVersion("spring"))
    }

    @Test fun refusesTamperedUnsafeOrExecutablePacks() {
        val st = store()
        val f = mapOf("a/cat.json" to lottie)
        fun refused(z: ByteArray, sha: String = AssetValidator.sha256(z)) = try { st.install(z.inputStream(), sha); fail("installed") } catch (_: PackStore.InstallException) {}
        val good = zip(f + ("pack.json" to manifest(1, f).toByteArray()))
        refused(good, sha = "00".repeat(32))                                                     // hash differs from the catalog
        refused(zip(f + ("pack.json" to manifest(1, f).toByteArray()) + ("../../escape.json" to lottie)))   // zip slip
        refused(zip(f + ("pack.json" to manifest(1, f).toByteArray()) + ("classes.dex" to byteArrayOf(1, 2))))  // code
        refused(zip(f + ("pack.json" to manifest(1, f).toByteArray()) + ("a/extra.json" to lottie)))      // unlisted file
        refused(zip(mapOf("a/cat.json" to "{}".toByteArray(), "pack.json" to manifest(1, f).toByteArray())))  // hash of a file differs
        refused(zip(f + ("pack.json" to manifest(1, f, ""","builtin":true""").toByteArray())))        // pretends to be built in
        assertNull("nothing half-installed", st.activeVersion("spring"))
        st.install(good.inputStream(), AssetValidator.sha256(good))
        refused(good)                                                                            // same version again
        assertEquals(1, st.activeVersion("spring"))
    }

    @Test fun catalogSignedByThePublishScriptVerifiesWithTheAppKey() {
        val dir = "catalogtest/"
        val body = javaClass.classLoader!!.getResource(dir + "catalog.json")!!.readBytes()
        val sig = javaClass.classLoader!!.getResource(dir + "catalog.json.sig")!!.readText()
        assertTrue("openssl signature with ALzikr's key", Catalog.verify(body, sig, PetPacks.PUBLIC_KEY))
        assertFalse("tampered catalog", Catalog.verify(String(body).replace("engine-test", "evil-test").toByteArray(), sig, PetPacks.PUBLIC_KEY))
        assertEquals("engine-test", Catalog.parse(body).single().id)
    }

    @Test fun catalogMustBeSignedAndPointToTrustedHosts() {
        val kp = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.genKeyPair()
        val body = """{"packs":[{"id":"spring","version":1,"url":"https://github.com/KL4PINGUINSKL/ALzikr/releases/download/p/spring.zip","sha256":"ab"},
            {"id":"evil","version":1,"url":"http://example.com/x.zip","sha256":"ab"}]}""".toByteArray()
        val sig = Base64.getEncoder().encodeToString(Signature.getInstance("SHA256withECDSA").run { initSign(kp.private); update(body); sign() })
        val pub = Base64.getEncoder().encodeToString(kp.public.encoded)
        assertTrue(Catalog.verify(body, sig, pub))
        assertFalse(Catalog.verify(body + 32, sig, pub))
        assertEquals(listOf("spring"), Catalog.parse(body).map { it.id })
    }
}
