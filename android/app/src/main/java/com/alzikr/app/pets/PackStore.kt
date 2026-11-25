package com.alzikr.app.pets

import java.io.File
import java.io.InputStream
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.ZipInputStream
import org.json.JSONObject

/**
 * Downloaded packs on disk (plain JVM, unit-tested):
 *
 *   root/<packId>/<version>/pack.json + files     one directory per installed version
 *   root/<packId>/active                          the version in use (written atomically)
 *   root/.staging/…                                 extraction area, cleared on start
 *
 * Install = verify the zip's SHA-256 against the signed catalog → extract safely into staging (no "..", no absolute
 * paths, only picture/data extensions, size and count limits) → parse and validate the manifest and every file hash →
 * move the directory into place → switch "active". The previous version is kept for rollback; older ones are removed.
 * Nothing executable is ever accepted, and nothing is loaded as code.
 */
class PackStore(private val root: File) {
    class InstallException(msg: String) : Exception(msg)
    private val staging = File(root, ".staging")
    private val allowed = AssetValidator.allowedExt

    init { root.mkdirs(); staging.deleteRecursively() }

    fun installed(): List<Pack> = root.listFiles().orEmpty().filter { it.isDirectory && !it.name.startsWith(".") }.mapNotNull { active(it.name) }
    fun activeVersion(id: String): Int? = File(File(root, id), "active").takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()
    fun dir(id: String, version: Int = activeVersion(id) ?: -1) = File(File(root, id), version.toString())
    fun active(id: String): Pack? = activeVersion(id)?.let { v -> runCatching { PackParser.parse(File(dir(id, v), "pack.json").readText()) }.getOrNull() }
    fun file(id: String, path: String): File? = File(dir(id), path).takeIf { it.exists() && it.canonicalPath.startsWith(dir(id).canonicalPath) }

    /** Installs a zip whose SHA-256 must equal [sha256] (from the signed catalog). Returns the installed pack. */
    fun install(zip: InputStream, sha256: String, expectId: String? = null): Pack {
        // Each install has its own staging directory; stale ones from a crash were cleared when the store was created.
        val tmp = File(staging, "dl-${System.nanoTime()}").apply { mkdirs() }
        try {
            val bytes = zip.readBytes()
            if (bytes.size > AssetValidator.MAX_PACK + 1_000_000) throw InstallException("pack too large")
            if (AssetValidator.sha256(bytes) != sha256.lowercase()) throw InstallException("download does not match the catalog (hash)")
            extract(bytes, tmp)
            val manifest = File(tmp, "pack.json").takeIf { it.exists() } ?: throw InstallException("no pack.json")
            val pack = try { PackParser.parse(manifest.readText()) } catch (e: PackFormatException) { throw InstallException("manifest: ${e.message}") }
            if (pack.builtin) throw InstallException("a downloaded pack cannot claim to be built in")
            if (expectId != null && pack.id != expectId) throw InstallException("pack id ${pack.id} != $expectId")
            if (pack.files.isEmpty()) throw InstallException("manifest lists no file hashes")
            // Every extracted file must be listed and hashed; listed files must exist.
            val onDisk = tmp.walkTopDown().filter { it.isFile && it.name != "pack.json" }.map { it.relativeTo(tmp).invariantSeparatorsPath }.toSet()
            (onDisk - pack.files.keys).firstOrNull()?.let { throw InstallException("unlisted file $it") }
            val issues = AssetValidator.validate(pack, { p -> File(tmp, p).takeIf { it.exists() }?.readBytes() }, checkHashes = true)
            if (issues.isNotEmpty()) throw InstallException(issues.joinToString("; ") { "${it.path}: ${it.problem}" })
            (pack.files.keys - onDisk).firstOrNull()?.let { throw InstallException("missing file $it") }
            val prev = activeVersion(pack.id)
            if (prev != null && prev >= pack.version) throw InstallException("version ${pack.version} is not newer than installed $prev")
            val target = dir(pack.id, pack.version)
            target.parentFile!!.mkdirs(); target.deleteRecursively()
            if (!tmp.renameTo(target)) { tmp.copyRecursively(target, overwrite = true); tmp.deleteRecursively() }
            setActive(pack.id, pack.version)
            // Keep the previous version for rollback; anything older goes.
            File(root, pack.id).listFiles().orEmpty().filter { it.isDirectory && it.name.toIntOrNull()?.let { v -> v != pack.version && v != prev } == true }.forEach { it.deleteRecursively() }
            return pack
        } finally { tmp.deleteRecursively() }
    }

    /** Back to the version kept before the last update. */
    fun rollback(id: String): Boolean {
        val cur = activeVersion(id) ?: return false
        val prev = File(root, id).listFiles().orEmpty().mapNotNull { it.name.toIntOrNull() }.filter { it < cur }.maxOrNull() ?: return false
        if (!File(dir(id, prev), "pack.json").exists()) return false
        setActive(id, prev); dir(id, cur).deleteRecursively(); return true
    }
    fun remove(id: String): Boolean = File(root, id).let { it.exists() && it.deleteRecursively() }

    private fun setActive(id: String, v: Int) {
        val f = File(File(root, id), "active"); val t = File(f.parentFile, "active.tmp")
        t.writeText(v.toString())
        if (!t.renameTo(f)) { f.delete(); if (!t.renameTo(f)) throw InstallException("cannot switch version") }
    }

    private fun extract(zip: ByteArray, into: File) {
        var count = 0; var total = 0L
        val base = into.canonicalPath + File.separator
        ZipInputStream(zip.inputStream()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val name = e.name.replace('\\', '/')
                if (e.isDirectory) continue
                if (name.startsWith("/") || name.contains("..") || name.contains(':')) throw InstallException("unsafe path $name")
                val ext = name.substringAfterLast('.', "").lowercase()
                if (name != "pack.json" && ext !in allowed) throw InstallException("file type not allowed: $name")
                if (++count > 2000) throw InstallException("too many files")
                val out = File(into, name)
                if (!out.canonicalPath.startsWith(base)) throw InstallException("unsafe path $name")
                out.parentFile!!.mkdirs()
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    while (true) { val n = z.read(buf); if (n < 0) break; total += n; if (total > AssetValidator.MAX_PACK) throw InstallException("pack too large"); o.write(buf, 0, n) }
                }
            }
        }
    }
}

/**
 * The pack catalog is trusted only if signed with ALzikr's key (ECDSA P-256, SHA-256). The public key is in the app;
 * the private key never leaves the developer's machine. The catalog carries each pack's version, download URL and zip hash.
 */
object Catalog {
    data class Entry(val id: String, val version: Int, val title: String, val url: String, val sha256: String, val size: Long, val schema: Int)
    /** Hosts a pack may be downloaded from. */
    val hosts = setOf("alzikr.alpharmasy.com", "github.com", "objects.githubusercontent.com", "raw.githubusercontent.com", "cdn.jsdelivr.net")

    fun verify(catalog: ByteArray, signatureB64: String, publicKeyB64: String): Boolean = runCatching {
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyB64.trim())))
        Signature.getInstance("SHA256withECDSA").run { initVerify(key); update(catalog); verify(Base64.getDecoder().decode(signatureB64.trim())) }
    }.getOrDefault(false)

    fun parse(catalog: ByteArray): List<Entry> {
        val o = JSONObject(String(catalog, Charsets.UTF_8)); val a = o.getJSONArray("packs")
        return (0 until a.length()).map { a.getJSONObject(it) }.map { p ->
            Entry(p.getString("id"), p.getInt("version"), p.optString("title", p.getString("id")), p.getString("url"), p.getString("sha256").lowercase(), p.optLong("size"), p.optInt("schema", 1))
        }.filter { e -> runCatching { java.net.URI(e.url).let { it.scheme == "https" && it.host in hosts } }.getOrDefault(false) && e.schema <= PET_SCHEMA }
    }
}
