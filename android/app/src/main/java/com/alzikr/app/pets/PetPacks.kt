package com.alzikr.app.pets

import android.content.Context
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pet Pack Manager: the built-in core pack (assets/pets/pack.json, always there, works offline) plus any downloaded packs
 * (filesDir/petpacks, see [PackStore]). Skins from packs join the existing animals; a pack cannot add a new animal in
 * schema 1 (the animals are part of the app). Everything a renderer needs to open a file goes through [open]/[file].
 */
object PetPacks {
    private var ctx: Context? = null
    @Volatile private var cache: Registry? = null
    /** Set when a pack is installed, updated or removed, so screens re-read. */
    @Volatile var revision = 0; private set

    class Registry(val core: Pack, val packs: List<Pack>) {
        /** Every animal with its skins: core skins first in their order, then pack skins. */
        val species: List<SpeciesDef> = core.species.map { s -> s.copy(skins = s.skins + packs.flatMap { p -> p.species.filter { it.id == s.id }.flatMap { it.skins } }) }
        private val bySpecies = species.associateBy { it.id }
        private val owner: Map<String, String> = (listOf(core) + packs).flatMap { p -> p.skins().map { it.id to p.id } }.toMap()
        val licenses: Map<String, License> = (listOf(core) + packs).flatMap { it.licenses.values }.associateBy { it.id }
        fun species(id: String) = bySpecies[id]
        fun skins(speciesId: String) = bySpecies[speciesId]?.skins.orEmpty()
        fun skin(id: String) = species.firstNotNullOfOrNull { s -> s.skins.firstOrNull { it.id == id } }
        fun packOf(skinId: String) = owner[skinId] ?: "core"
    }

    fun init(c: Context) { if (ctx == null) ctx = c.applicationContext }
    private fun store(): PackStore? = ctx?.let { PackStore(File(it.filesDir, "petpacks")) }

    /** The registry; in JVM tests (no Context) the core pack is read from the source tree. */
    fun registry(): Registry = cache ?: synchronized(this) {
        cache ?: run {
            val text = ctx?.assets?.open("pets/pack.json")?.use { it.readBytes().toString(Charsets.UTF_8) } ?: File("src/main/assets/pets/pack.json").readText()
            val core = PackParser.parse(text)
            val packs = runCatching { store()?.installed().orEmpty() }.getOrDefault(emptyList()).filter { p -> p.id != "core" && p.schema <= PET_SCHEMA }
            Registry(core, packs).also { cache = it }
        }
    }
    private fun changed() { cache = null; revision++ }

    /** Opens a file of a skin's pack. Core files live in assets/pets/, pack files in their installed directory. */
    fun open(skinId: String, path: String): InputStream? {
        val pack = registry().packOf(skinId)
        return if (pack == "core") runCatching { ctx?.assets?.open("pets/$path") }.getOrNull() else store()?.file(pack, path)?.inputStream()
    }
    /** A real file for a downloaded pack's asset (null for core assets, which are inside the APK). */
    fun file(skinId: String, path: String): File? = registry().packOf(skinId).takeIf { it != "core" }?.let { store()?.file(it, path) }
    fun isCore(skinId: String) = registry().packOf(skinId) == "core"

    // ------------------------------------------------------------ downloadable packs

    /**
     * The catalog is published next to the app (signed with the developer's private key). [PUBLIC_KEY] is ALzikr's
     * catalog-signing key (ECDSA P-256, X.509, Base64). Until a catalog is published, the list is simply empty.
     */
    const val CATALOG_URL = "https://raw.githubusercontent.com/KL4PINGUINSKL/ALzikr/main/petpacks/catalog.json"
    var PUBLIC_KEY = ""

    data class Available(val entry: Catalog.Entry, val installed: Int?)
    /** Fetches and verifies the catalog; null when offline, unsigned or tampered. */
    fun fetchCatalog(): List<Available>? = runCatching {
        if (PUBLIC_KEY.isBlank()) return emptyList()
        val body = get(CATALOG_URL) ?: return null
        val sig = get("$CATALOG_URL.sig")?.toString(Charsets.UTF_8) ?: return null
        if (!Catalog.verify(body, sig, PUBLIC_KEY)) return null
        val st = store()
        Catalog.parse(body).map { Available(it, st?.activeVersion(it.id)) }
    }.getOrNull()

    /** Downloads, checks and installs a pack from the verified catalog (atomic; the previous version is kept). */
    fun install(e: Catalog.Entry): Result<Pack> = runCatching {
        val st = store() ?: error("no storage")
        val bytes = get(e.url) ?: error("download failed")
        st.install(bytes.inputStream(), e.sha256, expectId = e.id).also { changed() }
    }
    fun rollback(id: String) = (store()?.rollback(id) == true).also { if (it) changed() }
    fun remove(id: String) = (store()?.remove(id) == true).also { if (it) changed() }
    fun installedPacks(): List<Pack> = registry().packs
    fun sizeOf(id: String): Long = store()?.dir(id)?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    /**
     * Installs a zip placed in the app's own private folder (files/incoming/<name>.zip). Only this app (or the developer
     * through adb's run-as) can write there; used to test packs on a device before they are published.
     */
    fun installLocal(name: String): Result<Pack> = runCatching {
        val c = ctx ?: error("no context")
        if (!Regex("^[a-z0-9._-]{1,64}$").matches(name)) error("bad name")
        val f = File(File(c.filesDir, "incoming"), "$name.zip").takeIf { it.exists() } ?: error("no such file")
        val bytes = f.readBytes()
        (store() ?: error("no storage")).install(bytes.inputStream(), PackStoreHash.of(bytes)).also { changed(); f.delete() }
    }

    private fun get(url: String): ByteArray? {
        val u = URL(url)
        if (u.protocol != "https" || u.host !in Catalog.hosts) return null
        val c = u.openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 30_000; c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "ALzikr")
        return try { if (c.responseCode == 200) c.inputStream.use { it.readBytes() } else null } finally { c.disconnect() }
    }
}

/** SHA-256 for a local file (the local install has no catalog; the zip's own manifest hashes still apply). */
object PackStoreHash { fun of(b: ByteArray) = AssetValidator.sha256(b) }
