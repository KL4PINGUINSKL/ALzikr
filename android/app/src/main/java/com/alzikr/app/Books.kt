package com.alzikr.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Islamic books in Arabic from IslamHouse.com (the official library of the Islamic Guidance offices,
 * Ahl as-Sunnah wal-Jama'ah), through its public API: browse its category tree, download a book's
 * PDF once and read it in the app.
 */
object Books {
    private const val API = "https://api3.islamhouse.com/v3/paV29H2gm56kvLPy"
    data class Category(val id: String, val title: String)
    data class Book(val id: String, val title: String, val description: String, val authors: String, val pdf: String?, val size: String)
    data class Page(val books: List<Book>, val page: Int, val pages: Int)

    var downloading by mutableStateOf<String?>(null)
    var progress by mutableFloatStateOf(0f)
    var version by mutableIntStateOf(0)

    private fun dir(c: Context) = File(c.filesDir, "books").apply { mkdirs() }
    private fun cacheDir(c: Context) = File(c.cacheDir, "islamhouse").apply { mkdirs() }
    fun file(c: Context, id: String) = File(dir(c), "$id.pdf")
    fun has(c: Context, id: String) = file(c, id).exists()

    private fun get(url: String): String? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000; conn.readTimeout = 30_000
        try { if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null } finally { conn.disconnect() }
    }.getOrNull()

    /** A cached answer for an hour, then asked again (so new books appear on their own). */
    private fun cachedGet(c: Context, key: String, url: String, refresh: Boolean): String? {
        val f = File(cacheDir(c), key)
        if (!refresh && f.exists() && System.currentTimeMillis() - f.lastModified() < 3_600_000L) return f.readText()
        return get(url)?.also { f.writeText(it) } ?: f.takeIf { it.exists() }?.readText()
    }

    /** Sub-categories of a category ("397053" is the root of the tree). Blocking. */
    fun categories(c: Context, parent: String = "397053", refresh: Boolean = false): List<Category> {
        val text = cachedGet(c, "cat_$parent.json", "$API/categories/viewcat/$parent/ar/showall/json", refresh) ?: return emptyList()
        val a = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it) }.filter { it.optString("datatype", "category") in setOf("category", "multicategories") }
            .map { Category(it.optString("id"), it.optString("title")) }
    }

    /** Books of a category (or the newest books when [category] is null). Blocking. */
    fun books(c: Context, category: String?, page: Int, refresh: Boolean = false): Page? {
        val url = if (category == null) "$API/main/books/ar/ar/$page/50/json" else "$API/main/get-category-items/$category/books/ar/ar/$page/50/json"
        val text = cachedGet(c, "books_${category ?: "new"}_$page.json", url, refresh) ?: return null
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val a = o.optJSONArray("data") ?: JSONArray()
        val links = o.optJSONObject("links")
        return Page((0 until a.length()).mapNotNull { a.optJSONObject(it) }.map(::parse), links?.optInt("current_page") ?: page, links?.optInt("pages_number") ?: page)
    }

    private fun parse(o: JSONObject): Book {
        val authors = o.optJSONArray("prepared_by")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("title") }.filter { it.isNotBlank() }.joinToString("، ") } ?: ""
        val pdf = o.optJSONArray("attachments")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.firstOrNull { it.optString("extension_type").equals("PDF", true) } }
        return Book(o.optString("id"), o.optString("title"), o.optString("description").trim(), authors, pdf?.optString("url"), pdf?.optString("size") ?: "")
    }

    /** Downloads a book's PDF. Blocking. */
    fun download(c: Context, b: Book): Boolean {
        val url = b.pdf ?: return false
        downloading = b.id; progress = 0f
        val tmp = File(dir(c), "${b.id}.part")
        return try {
            var u = URL(url); var conn: HttpURLConnection; var hops = 0
            while (true) {
                conn = u.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false; conn.connectTimeout = 15_000; conn.readTimeout = 60_000
                val code = conn.responseCode
                if (code in 300..399 && hops++ < 5) { u = URL(u, conn.getHeaderField("Location")); conn.disconnect(); continue }
                if (code != 200) { conn.disconnect(); return false }
                break
            }
            val total = conn.contentLengthLong
            var done = 0L
            conn.inputStream.use { input -> tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                while (true) { val n = input.read(buf); if (n < 0) break; out.write(buf, 0, n); done += n; if (total > 0) progress = done.toFloat() / total }
            } }
            conn.disconnect()
            // Remember the title for the "my books" shelf.
            File(dir(c), "${b.id}.json").writeText(JSONObject().put("id", b.id).put("title", b.title).put("authors", b.authors).toString())
            tmp.renameTo(file(c, b.id)).also { version++ }
        } catch (e: Exception) {
            android.util.Log.w("ALzikr", "book ${b.id}", e); false
        } finally { tmp.delete(); downloading = null }
    }

    /** Books on the phone. */
    fun shelf(c: Context): List<Book> = dir(c).listFiles { f -> f.name.endsWith(".json") }?.mapNotNull { f ->
        runCatching { JSONObject(f.readText()).let { Book(it.getString("id"), it.optString("title"), "", it.optString("authors"), null, "") } }.getOrNull()
    }?.filter { has(c, it.id) } ?: emptyList()

    fun delete(c: Context, id: String) { file(c, id).delete(); File(dir(c), "$id.json").delete(); version++ }
    fun clearCache(c: Context) { cacheDir(c).listFiles()?.forEach { it.delete() } }

    fun lastPage(id: String) = Store.prefs.getInt("book_page_$id", 0)
    fun setLastPage(id: String, p: Int) = Store.prefs.edit().putInt("book_page_$id", p).apply()
}
